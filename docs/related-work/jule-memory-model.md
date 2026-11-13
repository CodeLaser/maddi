# Jule's memory model and mutability rules, compared with maddi

**Note, 2026-11-13.** Read from the Jule manual (`julelang/manual` at `9a761c3`, 2026-11-07) and the
compiler (`julelang/jule` at `42e5373`, 2026-11-10). jule.dev is a JavaScript app with no text content, so
the sources were read from the repositories. Paths below are relative to those repositories.

**Conclusion.** Jule has no algorithms like ours. It **checks declared** mutability (`mut`), intraprocedurally
and by type, with no interprocedural inference and no alias or escape analysis. The overlap with maddi is in the
*rules* about what may be shared — independence, hidden content, the override direction — not in how they are
computed. Where the Jule manual says the compiler "insists" although there is no real risk, an inference
engine does better.

## 1. Memory management

- **Reference counting, no tracing GC.** Smart pointers (`&T`), slices, heap-allocated strings, channels,
  `any` and traits carry a reference count. The count is maintained through the C++ backend's copy
  constructors and destructors; the manual (`src/memory/memory-model.md`, "Copy Constructors and
  Destructors") says Jule "will never have built-in analysis for it".
- **Cycles leak** and are the programmer's problem (`src/memory/management/smart-pointers.md`, "Reference
  Cycles"). So is the stack overflow from recursively freeing a long chain.
- **Atomic counts only when needed, decided for the whole program at once.** If the program never uses the
  thread keyword `co`, counts are non-atomic; any use of `co` makes all of them atomic
  (`memory-model.md`, "Concurrency and RC").
- **Default initialisation everywhere**: slices to their full capacity, struct fields to their default
  value; external (C/C++) types are exempt.
- **Value semantics**: assignment copies. Slices and heap strings share a main allocation between copies;
  arrays are always copied.

## 2. Immutable by default, but shallow

A variable without `mut` cannot be written through. The guarantee is explicitly **not deep**
(`src/memory/immutability.md`, "Shared Memory"):

```jule
mut x := []byte("hello world")
y := x          // immutable, shares the allocation
x[0] = 'H'
println(string(y))   // Hello world
```

"Immutable" in Jule is therefore close to maddi's *not modified through this variable*, not to any of our
immutability levels.

**Reference pointers** (`&x` locals and parameters, `src/memory/raw-pointers/reference-pointers.md`) are raw
pointers with syntactic restrictions instead of a lifetime analysis: the target must be a variable (not a
slice element, since the slice may reallocate), they cannot be globals, cannot be captured by closures, and
cannot be passed to a thread. Unsafe Jule lifts each restriction.

## 3. The mutability rules (`std/jule/sema`)

### 3.1 Mutable type

`Type.Mutable()` (`std/jule/sema/type.jule:185`): a type is mutable if a copy of it can share writable state —
slices, raw and smart pointers, traits, type enums, `any`, and arrays, maps and channels of a mutable type.
A struct gets `F_MUTABLE` when a field *not* declared `mut` has a mutable type (`struct.jule:105`, refreshed
for generic instances at `struct.jule:268`). This is a single pass over the fields; there is no fixpoint.

### 3.2 The copy rule ≈ independence, decided by type

`checkValidityForInitExpr` (`sema.jule:1309`): a value of a mutable type held in immutable memory may not be
assigned to mutable memory — "immutable data cannot be assigned to a mutable memory because of type …".
The same applies to struct-literal fields (`checkMutRiskOfStructLit`, `type2.jule:1252`).

This is our **independence** question — does handing out a value expose the holder's mutable content? — answered
by type alone. The consequence the manual spells out (`immutability.md`): a function returning a mutable-typed
value must return it from a `mut` variable, and the compiler insists "even if there is no such thing" as a real
risk. maddi computes the independence of each method instead.

### 3.3 Interior mutability ≈ `@IgnoreModifications`

A field declared `mut` may be assigned by a method whose receiver is not `mut`
(`src/memory/mutability.md`, "Interior Mutability"; like C++'s `mutable`). In maddi's terms that method
modifies its object, and the contract hides it: the closest counterpart is `@IgnoreModifications`, i.e. manual
hidden content.

Copying a struct with such a field into mutable memory is refused only when the field is a mutable type
**and accessible** from the copying site (`isMutRiskyStruct`, `sema.jule:1303`); inside the struct's own
methods the risk is removed (`removeInteriorMutRisk`, `scope.jule:1222`). Visibility thus enters the rule,
which is reminiscent of hidden content: a private field the outside cannot name does not count. The manual
recommends keeping interior-mutable fields private for this reason.

### 3.4 "Responsive" mutability

`src/responsiveness/mutability.md`: a struct literal may take an immutable slice when the struct goes into an
immutable variable — the destination decides. One crude shortcut (`structLitChecker.pushMatch`,
`type2.jule`): a struct with **no methods** carries no interior-mutability risk; with one method, even an
empty one, it is risky, "in order not to increase compilation costs".

### 3.5 Contracts in function types and traits ≈ the override rule

A function type or trait method may declare a `mut` parameter or receiver, and an implementation may drop the
`mut` (`responsiveness/mutability.md`, "Functions", "Traits"). An implementation may be *less* modifying than
the contract, never more: the same direction as maddi's rule for an implementation against its abstract method.
Jule also ignores `mut` on a parameter whose type is not mutable, since it cannot affect the caller.

## 4. The optimiser only reads the declarations

`src/julec/opt` keeps per-variable facts for nil checks (`nil.jule`), bounds checks (`boundary.jule`) and type
assertions (`dynamic.jule`). A fact is dropped when the variable is assigned, used in a struct literal, or
passed to a parameter declared `mut &` (`opt/expr.jule:886`, `args`). This is "is this parameter modified" used
exactly as we use it, but taken from the declared `mut`, never from analysing the callee.

## 5. What this means for maddi

- **Nothing algorithmic to borrow.** Jule asks the programmer to write `mut` where maddi infers modification,
  and keeps every check inside one function and shallow.
- **A useful comparison when explaining maddi.** §3.2, §3.3 and §3.5 are type-level, annotation-driven versions
  of independence, hidden content / `@IgnoreModifications`, and the override rule. The places where Jule
  over-approximates (§3.2's forced `mut` returns, §3.4's one-method shortcut) are exactly where inference wins.
- **A possible target for annotations.** Jule's `mut` on parameters and receivers is the kind of annotation
  maddi's verdicts could generate, as with Rust's `&mut` in the Rust printer. Relevant only if Jule ever became
  a translation target.
