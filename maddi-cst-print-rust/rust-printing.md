# Printing the CST as Rust

`maddi-cst-print-rust` prints a CST, parsed from Java, as a Rust library crate. It is the third Java-to-X printer,
beside `maddi-cst-print-kotlin` and `maddi-cst-print-csharp`. The work item is CodeLaser/maddi#167. The printer is
**experimental**.

## Aim

The other two printers aim at a translation that reads as if it had been written by hand. This one has a second aim
first: to be an **oracle for the linking engine**. Rust's rule is *aliasing XOR mutability*; the Road to
Immutability splits the same idea over modification (rule 1) and independence (rule 3). So the verdicts become the
ownership modes of the signatures, and rustc's borrow checker judges the bodies against them.

| verdict | Rust |
|---|---|
| non-modifying method | `&self` |
| modifying method | `&mut self` |
| `@Finalizer` | `self` |
| parameter not kept (independent), unmodified | `&T` (`&str` for a string) |
| parameter not kept, modified | `&mut T` |
| parameter kept (dependent, or independent with hidden content) | `T`, moved in |
| result sharing the receiver's accessible content (dependent) | `&T` |
| nullable | `Option<T>` |

`RustFacts` is the seam: `RustFacts.STORED` reads the stored properties, `RustFacts.of` adds the nullability pass's
report, and a test can supply any verdict it likes, a wrong one included.

## Strict on purpose

Nothing is softened to please the borrow checker: no `clone()`, no `Rc<RefCell<…>>`. Where rustc rejects the
output, it says where a verdict and the code disagree:

| rustc says | reading |
|---|---|
| E0596, E0594: cannot borrow or assign behind a `&` reference | a non-modifying (or unmodified) verdict the body contradicts: the **analyzer** is wrong |
| E0382: use of a moved value | a dependent parameter: Java shares the object, Rust does not; the verdict is right, the **printer** needs another representation (`Rc`, a clone) |
| E0597, E0716: does not live long enough | a missed dependence |
| E0502, E0499: overlapping borrows | aliasing plus modification |

`TestJavaToRustOwnership` pins the first two: the same Java compiles with the right verdicts, fails with E0596/E0594
when `add` is declared non-modifying, and fails with E0382 where a constructor keeps the caller's list.

The exceptions are the immutable values: a `String`, and a source record. Sharing and copying them cannot be told
apart, so a named string is cloned where it is stored again, and a borrowed record (`map.get(k)`) is cloned where an
owned one is expected.

rustc catches unsound verdicts, not imprecise ones: a `&mut` where `&` would do compiles. Clippy's
`needless_pass_by_ref_mut` and `unused_mut` give the other direction.

## Shape

- A package is a module (`a.b` → `src/a_b.rs`), declared in `src/lib.rs`; `Cargo.toml` beside `src/`.
- A class is a `struct` and an inherent `impl`; each source interface it implements gets an `impl Trait for`. An
  interface is a `trait`; a value of its type is a `Box<dyn Trait>`, a borrowed one `&dyn Trait`.
- A record is a `struct` with public fields, derived `Debug, Clone, PartialEq`, a positional `new`, and accessors.
- An enum without state is a plain `enum` deriving `Copy`; its constants become camel-case variants.
- A nested type is an item of its own, named after its enclosing types (`Outer.Inner` → `OuterInner`).
- A constructor is an associated `fn new… -> Self`. A body that only assigns fields gives one `Self { … }`; anything
  else builds `this` at the fields' empty values, runs the body on it, and returns it.
- Overloads get a numeric suffix in declaration order (`new`, `new_2`; `add`, `add_2`), the same at declaration and
  call.
- A `static final` constant is an associated `const`; other static state is reported (`STATIC_STATE`).
- The JDK maps onto `std`: the collections (`Vec`, `HashMap`, `HashSet`, `BTreeMap`, `VecDeque`), `Option`,
  `String`, and a table of methods (`RustJdk`). An index is a `usize` in Rust: `i as usize`, and a size goes back to
  `i32`, so the arithmetic around it stays Java's. Java's implicit numeric widening is spelled out.

The printer writes Rust text directly, not through the `OutputElement` IR the Kotlin and C# printers share: one Java
type is several Rust items, which the one-declaration shape of `TypePrinter` does not fit, and `rustfmt` formats
the result better than a generic formatter would.

## Not yet

Class inheritance (`INHERITANCE`), anonymous and local classes, `instanceof`, switch, try/catch (`TRY_BODY_ONLY`),
method references, static mutable state, generic bounds (`T: Clone + PartialEq`), `equals`/`hashCode` as
`PartialEq`/`Hash`, and most of the JDK beyond the collections. Each unsupported construct prints `todo!()` and an
`UNSUPPORTED` message.

## Running it

```bash
maddi --compile-log build.log --translate rust --translate-output rust-crate
cd rust-crate && cargo build
```

The tests compile their samples with `rustc` when it is on the `PATH`, and are skipped otherwise.
