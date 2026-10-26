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
| dependent field: every constructor stores a dependent parameter in it | `&'a T` (`&'a mut T` when modified), `struct View<'a>` |
| result sharing the receiver's accessible content (dependent) | `&T` |
| result that is the hidden content (independent with hidden content, a type parameter) | `&T` |
| fluent method (`return this`) | `&Self` / `&mut Self` |
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

`TestJavaToRustOwnership` pins the first and the last: the same Java compiles with the right verdicts, fails with
E0596/E0594 when `add` is declared non-modifying, and fails with E0502 where a constructor keeps the caller's list
and the caller modifies it while the holder lives: dependent and modifiable from outside, what Rust refuses without
`RefCell`.

### The table of #167, row by row

`TestJavaToRustMapping` takes the mapping of the issue row by row: a Java example, the verdicts the analysis computes
for it, the Rust they print as, and rustc's judgement, with a counterpart (the neighbouring verdict and rustc's error
for it) where there is one. The same examples were run through the CLI (`--analysis-steps modification --translate
rust`, with `maddi-support` on the class path), so the verdicts are the analyzer's, not only the test's. With the
analyzer's verdicts every example compiled but one: `Client.twice()` calls `Counter.next()`, which modifies its own
type's static state, and the analysis then counted only another type's. Since maddi-mod#65 the analysis gives
`twice()` the verdict, and the printer no longer needs it: `unsafe fn` is the printer's own closure over the
`static mut`s a method touches (`RustUnsafety`, row 13 below). Repeated on 2026-10-25 (the same 14 sources in one
package, one crate, `cargo build`): no error, no warning; also with row 13 extended by a read from another type
(`Client.current()` calling `Counter.peek()`), a call within Counter, and `Counter::next` passed as a `Supplier`.
That run found two printer faults the per-test crates could not: the example class `Box` was printed as `Box`, so
every `Box<dyn Fn…>` of the module meant that struct (a type named as one of Rust's own is now `JBox`); and a
closure parameter borrowed by the analysis's verdict was declared `&mut Box<dyn Fn…>` while the caller passed
`&mut || …` (now `&mut dyn Fn…`). A `dyn Fn` closure is called through `&`: it is `&dyn Fn…` also where the analysis marks the
parameter modified, as it does every functional parameter it applies (the applied marker that the callers resolve,
maddi-mod#69; declaring the SAMs `@NotModified` instead would lose the side effects of a function passed in). A
`Consumer` or `Runnable` is an `FnMut`, `&mut dyn FnMut…`. Which closures are `FnMut` is the open question: a
`Function` whose passed functions modify what they capture is E0596.

| row | status | evidence |
|---|---|---|
| `@NotModified` parameter → `&T` | holds | `parametersAndReceivers` |
| `@Modified` parameter → `&mut T` | holds, also through the elements | `parametersAndReceivers`; called unmodified: E0596 |
| non-modifying method → `&self`, modifying → `&mut self` | holds | `parametersAndReceivers`, `TestJavaToRustOwnership` |
| `@Container` → `&`, never `&mut` | holds | `container`: only the non-container method has a `&mut` |
| independent constructor parameter | holds as `&T`, copied by the constructor (not an owned `T`) | `independentConstructorParameter`; called dependent: E0382 |
| dependent field → lifetime | holds | `dependentField` (outliving the list: E0515), `dependentFieldIsBorrowed` (modified meanwhile: E0502) |
| hidden content → generic `T` | holds: no bounds needed, the hidden content handed out as `&T` | `hiddenContent` |
| `@Immutable(hc=true)` → `Rc<T>` | holds: held as `Rc`, its hidden content cloned under `T: Clone` | `immutableHcShared`: two holders share one pair |
| `@Finalizer` → `self` | holds | `finalizer`: a call after the finalizer is E0382 |
| `@Mark`/`@Only` → typestate | holds | `typestate`: reading before the mark, and a second mark, are E0599 |
| `@IgnoreModifications` → interior mutability | holds: `&self` over a `RefCell` | `ignoreModifications`; without the verdict, the logging `&self` caller is E0596 |
| `@StaticSideEffects` → `static mut`, `unsafe fn` | holds, and wider: Rust guards a read and a type's own state too, so `unsafe fn` is the printer's closure, the verdict one of its seeds | `staticSideEffects` (no verdict needed, a read through `peek` included), `staticSideEffectsVerdict` (a verdict without a `static mut`, as a library contract) |
| nullable → `Option<T>` | holds | `nullability` |

The exceptions are the values for which sharing and copying cannot be told apart: a `String`, and a source class or
record the analysis calls `@Immutable` (without hidden content), which derives `Clone`. A named string is cloned where
it is stored again, a borrowed immutable object (`map.get(k)`) where an owned one is expected, and a stream over them
is `.iter().cloned()`. A type with hidden content (`@Immutable(hc=true)`) is not: its clone would copy what Java
shares, so the mismatch stays for rustc to report.

A dependent result that is a *new* container of the receiver's mutable elements (`orders.stream().toList()`) is
`Vec<&T>` in Rust, not the `&Vec<T>` the signature says: rustc reports it, but that one is the printer's (the
verdict does not say whether the container or only its elements are shared).

rustc catches unsound verdicts, not imprecise ones: a `&mut` where `&` would do compiles. Clippy's
`needless_pass_by_ref_mut` and `unused_mut` give the other direction.

### Findings so far

Analyzer issues the oracle found, each with a minimal repro:

- CodeLaser/maddi-mod#58: `stream().map(Order::customer).toList()` is dependent where the same map with a lambda is
  independent (E0515).
- CodeLaser/maddi-mod#59: modifying a map's value (`products.get(k).put(…)`) leaves the deep
  `NON_MODIFYING_METHOD` true (E0596).
- CodeLaser/maddi-mod#60: `Collections.unmodifiableList(field)` is independent; a read-only view of a mutable
  collection is dependent (E0507). `UNMODIFIABLE_VIEW_METHOD` cannot stand in for it: it marks any unmodifiable
  result, a fresh one too.
- CodeLaser/maddi-mod#62: element modification one level beyond #59 (a map in a list in a map, `values()` iterated,
  a cast `Object` value) is missed by the deep verdict (E0596).
- CodeLaser/maddi-mod#63: the reachability pass did not walk a switch expression's `case -> { ... }` arm (E0596).
- CodeLaser/maddi-mod#64: `new HashMap<>(field)` over immutable keys and values is dependent (E0515).

## Shape

- A package is a module (`a.b` → `src/a_b.rs`), declared in `src/lib.rs`; `Cargo.toml` beside `src/`.
- A class is a `struct` and an inherent `impl`; each source interface it implements, and each interface those
  extend, gets an `impl Trait for`. An interface is a `trait`; a value of its type is a `Box<dyn Trait>`, a borrowed
  one `&dyn Trait`. A call of a trait's method from another module imports the trait.
- An interface method's verdict is the union over its implementations, or unknown and so modifying. Rust makes an
  impl repeat the trait's signature, so an implementation with other verdicts (`&self` where the trait has
  `&mut self`) is an inherent method with its own, and the trait's method delegates to it: the body is still judged
  against its own verdict, and `self.m()` resolves to the inherent method first.
- A source superclass is composition (`INHERITANCE_AS_COMPOSITION`): a `base: Parent` field built by `super(…)`,
  and an inherited field or method is reached through `.base` (`super.m()` too). A call from the parent to a method
  the subclass overrides stays in the parent; a subclass is not accepted where the parent is expected.
- A record is a `struct` with public fields, derived `Debug, Clone, PartialEq`, a positional `new`, and accessors.
- An enum without state is a plain `enum` deriving `Copy`; its constants become camel-case variants.
- A nested type is an item of its own, named after its enclosing types (`Outer.Inner` → `OuterInner`).
- A dependent field (`RustBorrows`): final, without initializer, of a type that is moved rather than copied, and
  assigned a dependent parameter by every constructor, is a borrow with the struct's lifetime: `struct View<'a> {
  list: &'a Vec<String> }`, `&'a mut` when the object modifies it. The constructor takes that borrow, and the caller
  passes `&l`. Records keep their owned components. A struct with a lifetime, like a generic one, is called by its
  name rather than `Self`, which would fix the impl's lifetime or type arguments.
- An eventually immutable type (`RustTypestate`: a `@Mark`, one transition) is a typestate: `struct Config<S =
  Before>` with a `PhantomData<S>`; constructors, statics and `@Only(before)` methods in `impl Config<Before>`, a
  method of either state in `impl<S> Config<S>`, `@Only(after)` methods in `impl Config<After>`. The mark takes
  `mut self`, runs its body, and moves the fields into a `Config<After>`; a mark called on a local rebinds it (`let
  mut c = c.set_name(..)`), on anything else it is `UNSUPPORTED`. maddi's `SetOnce<T>` is an `Option<T>`.
- `@IgnoreModifications` on a method (`RustCells`): the method takes `&self`, the fields it modifies are
  `RefCell<T>`: `borrow_mut()` in it, `borrow()` in another `&self` method, `get_mut()` in a `&mut self` one. A
  conflicting borrow is a run-time `BorrowMutError`, the price of the annotation.
- `@Immutable(hc=true)`: an `Rc<T>` wherever it is held, `Rc::new` at construction, cloned (shared) where Java
  shares it; its hidden content is cloned under a `T: Clone` bound on the impls.
- Java's mutable static state, of a type with a compile-time empty value, is a module-level `static mut`. A method is
  an `unsafe fn` with an `unsafe` body when it reads or writes one (of any type), calls, constructs or references
  (`Counter::next`) a method that is, transitively, or has the `@StaticSideEffects` verdict (`RustUnsafety`); a
  method call on a non-`Copy` static goes through `&raw mut`, as Rust 2024 refuses a reference to it.
- A constructor is an associated `fn new… -> Self`. A body that only assigns fields gives one `Self { … }`; anything
  else builds `this` at the fields' empty values, runs the body on it, and returns it.
- Overloads get a numeric suffix in declaration order (`new`, `new_2`; `add`, `add_2`), the same at declaration and
  call.
- A `static final` constant is an associated `const`. A `static final` field whose object no method modifies (the
  `UNMODIFIED_FIELD` verdict), of a type Rust can share between threads, and untouched by a static initializer, is a
  module-level `static OWNER_NAME: LazyLock<T>`, built on first use and read-only: a wrong verdict is rustc's E0596.
  Other static state is a `static mut` (see below) or reported (`STATIC_STATE`).
- An enum without state gets `VALUES`, `values()`, `ordinal()` and `name()`, what `java.lang.Enum` gives it. A field
  its one constructor sets from a parameter, and each constant from a primitive or string literal (`EARTH(9.8)`), is
  a match over the variants, `gravity_value()`, which every read of the field calls. Other state is
  `ENUM_WITH_STATE`.
- A switch (old style with its fall-through, new style, expression) is a `match`; a statement over part of the
  cases gets `_ => {}`. A `break` inside an arm would leave an enclosing loop in Rust, and is reported.
- `equals(Object)` is `impl PartialEq`, the usual preamble (`this == o`, `instanceof`, `getClass()`) left out
  because `other: &Self` already guarantees it; `hashCode()` stays a method and `impl Hash` feeds it to the hasher
  (with `impl Eq` when both are there). A call `a.equals(b)` is `a == b`.
- `o instanceof T t` of an `Object`, `T` a class without type parameters, is `if let Some(t) = o.downcast_ref::<T>()`
  (`downcast_mut` when `t` is modified, `Some(&t)` for a copy type); without a pattern variable it is `o.is::<T>()`.
  An interface, a generic type and a record pattern stay `todo!()`: `Any` cannot tell them apart.
- A method reference is a closure (`Order::customer` → `|x| x.customer()`, `out::add` → `|x| { out.push(x.clone()); }`); a stream is an iterator (`map`,
  `filter`, `anyMatch`, `count`, `sum`, `toList`; `distinct` folds into a vector to keep Java's order).
- `sb.append(a).append(b)` is one `push_str`/`push` per append.
- A collection's `iterator()` is a peekable `iter()`: `hasNext()` is `peek().is_some()`, `next()` is
  `next().unwrap()`, a panic where Java throws. `remove()` through it is not mapped.
- A closure's parameter is the element (`map` over `.cloned()`) or a reference to it (`filter`): it is passed as `&x`
  where a borrow is expected and `x.clone()` where an owned string is, which deref coercion and auto-deref accept
  either way. A comparator (`comparing`, `comparingByValue`, `reversed`, `thenComparing`, a two-parameter lambda) is
  the `|a, b| -> Ordering` closure `sort_by`, `max_by`, `min_by` take; its key extractor is applied inline, as a
  closure literal called on the spot leaves rustc without its parameter's type.
- A source interface with one abstract method is a trait that every closure of its method's shape implements
  (`impl<F: Fn(&str)> Listener for F`). A lambda or method reference passed, stored or assigned where a functional
  interface is expected has its parameters typed, which rustc cannot infer for a `dyn` target; a kept one is a
  `Box::new(...)`. A stored listener that captures `self` mutably (an observer registering itself) is aliasing plus
  modification: rustc's E0500/E0501, which the strict printer leaves.
- A `java.util.function` interface is a `dyn Fn…`: `Supplier`, `Function`, `Consumer` (an `FnMut`) and their kin by
  hand, every other one (`IntSupplier`, `IntPredicate`, `ToIntFunction`, `IntBinaryOperator`, `BiPredicate`, …) from
  the abstract method it declares: a primitive by value, a type argument borrowed, a `void` method an `FnMut`. A
  `String` argument is borrowed as `&str`, as a `String` parameter is, and calling the closure borrows what it is
  passed. The in-memory parse of the tests leaves these types lazily loaded, without a single abstract method; the
  printer knows them by package.
- A parameter moved in (`T`) and read once, outside a loop and a closure, is moved at that read: `self.message =
  message;`, and Java's copy of it (`List.copyOf(input)`, `new HashSet<>(input)`) is the value itself. A string or
  an immutable value read more than once is still cloned at each owned use (`RustBodyPrinter.movable`).
- A borrowed array is a slice, `&[T]` or `&mut [T]`: a Java array never changes length. A `Vec` argument coerces;
  a nullable one is `.as_deref()`. Owned, it stays a `Vec<T>`.
- A struct holding itself by value, directly or through other structs, has no size (E0072): such a field is a
  `Box<T>`, `Option<Box<T>>` when nullable (`RustBoxes`), read as `(*self.f)` or `self.f.as_deref()`, written boxed.
  A linked structure that Java shares (`node.next = other`) still fails: a Box owns, and sharing is a dependence.
- Java's `==` on objects of source classes is identity: `std::ptr::eq(a, b)`. Enum constants, strings and boxed
  primitives are values, compared with `==`.
- A nullable value assigned where the verdicts hold the target non-null is unwrapped (`UNWRAP_AT_DEREFERENCE`).
- A `T` parameter is independent whatever the body does with it, so the body decides its passing: stored (returned,
  assigned, `list.add(t)`, `List.of(t)`, a source callee taking it owned) it is moved in, `T`; otherwise `&T`.
- A type parameter carries the trait bounds its uses need (`RustBounds`), on the impl's and the method's generics:
  `Eq + std::hash::Hash` as a `HashSet` element or `HashMap` key, `Ord` for the sorted ones and `compareTo`,
  `PartialEq` for `==`, `equals`, `contains`, `indexOf`, `std::fmt::Display` in a string, `Clone` for a copy of a
  collection of them or as the argument of an `@Immutable(hc=true)` type. A struct declaration carries none.
- A `Stream<T>` (`IntStream`: `i32`, …) as a declared type is the iterator chain the body prints:
  `Box<dyn Iterator<Item = T>>`, `+ '_` as the result of a method on the object, `impl Iterator<Item = T>` as a
  borrowed parameter; a returned chain is boxed.
- Where a result comes from, the links know and the independence verdicts do not (`LinkFacts`, computed in the run
  that analyses the source): a new object is owned, and the parameters it keeps are moved in; a result inside the
  receiver borrows it; one inside a single borrowed parameter borrows that parameter. Whatever either says, a method
  whose every return creates its result (`return new R(list)`) returns it owned, and so does one returning
  `opt.orElse(...)`/`orElseGet(...)`: `unwrap_or` on a borrowed Option's clone, the default a clone of a field or a
  borrow, as is an `orElseGet` supplier's result (`|| alt.clone()`). A borrowed result the verdicts call non-null that is held in an Option is `as_ref().unwrap()`, not `&` of a
  temporary moved out; an untranslated `todo!(...)` is returned without a `&`.
- A place written through (the object of a modifying call, an assignment's target) is reached exclusively: a
  nullable scope is `as_mut().unwrap()`, a map's value `get_mut(k)`; the arguments and indices in it are only read.
- A getter returning one of its object's fields (`return items;`) that some caller modifies through
  (`b.items().clear()`) gets an exclusive twin, `items_mut(&mut self) -> &mut T`, which those callers call
  (`RustGetters`). An interface's getter (`items()` that a default method's `items().add(s)` modifies through) has
  its twin declared in the trait when every implementation is such a getter, each defining its own; not a functional
  interface's method, nor a computed result.
- A function parameter that some call site passes a closure writing what it captures (`this::parse` assigning a
  field) is `&mut dyn FnMut`, for every caller (`RustClosures`); otherwise a `dyn Fn` is called through `&`.
- A `Map.Entry` is the `(K, V)` tuple a map's iteration yields; `new SimpleEntry<>(k, v)` and `Map.entry(k, v)` build
  one, `getKey()`/`getValue()` are `.0`/`.1`.
- `System.exit`, `lineSeparator`, `arraycopy` (`clone_from_slice`, `copy_within` inside one array);
  `Collections.sort`, `addAll`, `singleton`, `synchronized…` (the lock goes: `SYNCHRONIZED`); `Arrays.fill`,
  `toString`, `copyOf`.
- `src/jrt.rs`, written with every crate, holds what `std` has no counterpart for: `String.valueOf(Object)` of a
  `Box<dyn Any>` (the values the translation boxes), `String.hashCode()`.
- A nullable value where `Option<&str>` is expected is `None`, `.as_deref()` or `.map(|s| s.as_str())`; one stored in
  a collection, whose values are never Options, is unwrapped (`UNWRAP_AT_DEREFERENCE`); `x == null` of a value the
  verdicts call non-null is its constant outcome (`NULL_CHECK_OF_NON_NULL`).
- `map.getOrDefault(k, d).put(...)` modifies in place: `get_mut(k).unwrap_or(&mut d)`. A string borrowed from a
  lookup is copied into its local, and into the arguments of a `&mut self` call, which ends the borrow of its
  holder; a string is immutable, so the copy cannot be told from the shared one.
- An unmodifiable view (`Collections.unmodifiableList(x)`) is `x` borrowed: Rust's `&` is a read-only view.
- A value passed or returned where an `Object` is expected is boxed, `Box::new("x".to_string())`: owned as its own
  type first, so the box holds a `String`, as a collection's value or a field's initializer does.
  A class's object where its interface is held (a local, an assignment, an argument, a result) is boxed as the
  `Box<dyn I>` the declaration prints; a closure and an `Rc` are not.
- A string parameter the body assigns (`p = decode(p)`) is taken as its own `String`; a closure passed for a nullable
  parameter is `Some(Box::new(...))`; `Optional.ofNullable(x)` of a nullable `x` is a copy of its `Option`, not `Some(x)`.
- A local is a mutable binding when its object is modified through one of its fields (`a.list1.add(s)`), through a
  local referring to one (`let l = &mut a.list1`), or by a JDK static modifying its first argument
  (`Collections.sort`); a nullable exclusive parameter (`Option<&mut T>`, reached by `as_mut()`) is `mut` too.
- A returned switch expression or conditional converts each arm as the result is (`None`, `self.f.as_ref()`,
  `Some(v)`), not the whole; a `yield` before an arm's last statement is a `break 'yN value` out of the arm's labeled
  block.
- A parameter the body may replace by a new object and returns (`if (flag) s = new Wrapper(s); return s;`) is a
  result new on one path and existing on the other: moved in, returned owned.
- `BigDecimal` is an `f64` (`BIGDECIMAL_AS_F64`), `LocalDateTime` and `Instant` a `SystemTime`
  (`TIME_AS_SYSTEM_TIME`): both change behaviour, and are reported.
- The JDK maps onto `std`: the collections (`Vec`, `HashMap`, `HashSet`, `BTreeMap`, `VecDeque`), `Option`,
  `String`, and a table of methods (`RustJdk`). An index is a `usize` in Rust: `i as usize`, and a size goes back to
  `i32`, so the arithmetic around it stays Java's. Java's implicit numeric widening is spelled out.

The printer writes Rust text directly, not through the `OutputElement` IR the Kotlin and C# printers share: one Java
type is several Rust items, which the one-declaration shape of `TypePrinter` does not fit, and `rustfmt` formats
the result better than a generic formatter would.

## Not yet

Inheritance from a JDK class (`INHERITANCE`), dynamic dispatch through a parent, anonymous and local classes, `instanceof` of an interface or generic type and record patterns,
try/catch (`TRY_BODY_ONLY`), static state of other types (a `HashMap`, an object), generic bounds beyond `Clone`, sorting and collectors
beyond `toList`, and most of the JDK beyond the collections. Each unsupported construct prints `todo!()` and an
`UNSUPPORTED` message.

## Running it

```bash
maddi --compile-log build.log --translate rust --translate-output rust-crate
cd rust-crate && cargo build
```

The tests compile their samples with `rustc` when it is on the `PATH`, and are skipped otherwise.
