# Definitions: modification, containers, independence, immutability

A condensed reference of the concepts the analyzer computes, summarized from the **Road to Immutability**
(`road-to-immutability/src/docs/asciidoc/`), with each definition mapped onto the `Property` / `Value` it becomes in
the code.

- **This document is a summary, not the normative source.** Where it disagrees with the AsciiDoc, the AsciiDoc wins
  (except where noted in [Wrinkles in the source document](#wrinkles-in-the-source-document), where the code wins).
- Section references like §050 are to the AsciiDoc file with that numeric prefix.
- Written for the guard work (`GuardAnalyzerImpl`), which verifies user-written contracts against computed values, so
  it leans on precisely those definitions. See `guard-mode-analysis.md` at the repository root.

## The chain of definitions

Each concept builds on the previous one; this is the order the document develops them in, and roughly the order the
analyzer computes them (see `README.md` for the phases).

```
final fields → modification → containers → linking/dependence → hidden content → immutability
```

## Final fields (§020)

> **Definition**: a field is **effectively final** when it either has the modifier `final`, or it is not assigned to in
> methods that can be transitively called from non-private (non-constructor) methods.

So a field assigned only from a private method reachable solely from the constructor is still effectively final.
Only the `private` modifier counts as access control; package-private/protected/public are not distinguished (§010).

- Field annotated `@Final` (`FINAL_FIELD`) when effectively final without the modifier — no point annotating what the
  modifier already says. Not effectively final = *variable*, optionally `@Final(absent=true)`.
- Type annotated `@FinalFields` when all fields are effectively final but the type is *not* immutable
  (`IMMUTABLE_TYPE` = `FINAL_FIELDS`). A type with one variable field is never immutable.
- Any `record` is at least `@FinalFields`.

Final fields alone do not give intuitive immutability: `StringsInArray` holds a `String[]` passed in by the caller,
who can keep writing to it. Hence the rest of this document.

## Modification (§030)

> **Definition**: a **method is modifying** if it causes an assignment in the object graph of the fields of the object
> it is applied to.

"Object graph" = the fields of the object, the fields of those fields, and so on to arbitrary depth. A method that only
reads from the object graph is non-modifying. Setters are `@Modified`, getters are `@NotModified`. A method calling a
modifying method on one of its fields is itself modifying. All non-trivial constructors are modifying.

> **Definition**: the analyzer marks a **parameter** as **modified** when the parameter's method applies an assignment
> or modifying methods to the argument, i.e., the object that enters the method via the parameter. This definition
> holds with respect to the argument's entire object graph.

> **Definition**: the analyzer marks a **field** as **modified** when at least one of the type's methods, transitively
> reachable from a non-private non-constructor method, applies at least one assignment or modifying method to this
> field's object graph.

`@Modified` and `@NotModified` are exclusive; one or the other is computed for every method. One annotation pair covers
methods, fields and parameters (rather than `@Modifying`/`@NotModifying` for methods) deliberately.

**Modification is the default; the analyzer must *prove* non-modification.** An abstract method without `@NotModified`
is assumed modifying, and so are the parameters of abstract methods unless their type is immutable (§030, §050,
appendix). In the code: `ShallowMethodAnalyzer.computeMethodNonModifying` returns `DEFAULT_FALSE` unless the method is
annotated, inherits a TRUE from a public override (`commonBooleanFromOverride`), or the type is at least
`@Immutable(hc=true)`; constructors and `@Fluent` methods are modifying outright.

**Indirect modification** (§070): a method is *not* non-modifying merely because it makes no direct assignment. If a
modifying method called on a `@Modified` parameter calls back into one of our own modifying methods, we are modifying
too. These cyclic cases are enforceable only when all code is visible, and the analyzer handles them poorly; an extra
interface breaking the cycle is the recommended workaround.

**Static side effects** (§050): static and instance fields are treated alike *within* a primary type — modifying a
static field of your own type is a modification. But when a static modifying method is called on a field (or type) not
belonging to the primary type, its enclosing types, or its parent types, the modification is classified as a *static
side effect* (`@StaticSideEffects`), not a modification. `@IgnoreModifications` on a field suppresses modifications
explicitly. Modifications inside a *static initializer block* are construction, hence ignored; modifications to static
fields inside a constructor are **not**.

## Containers (§040)

> **Definition**: a type is a **container** when no non-private method or constructor modifies its arguments.

Informally: a type you can safely hand your objects to. It may store them, but it will not change them.

**The container property is exactly "all parameters of non-private methods and constructors are `@NotModified`"** —
and `TypeContainerAnalyzerImpl.go` computes precisely that, `allMatch(ParameterInfo::isUnmodified)` over the parameters
of every non-private method and constructor. Nothing more is needed, because the *parameter* modification it reads has
already absorbed the hard part.

The subtlety is in the word *arguments*, not in the container rule. A parameter is modified when the argument's entire
object graph is modified at any point in the object's life-cycle — **not** when the receiving method body contains a
visible modification. It is not because a method does not visibly modify its argument that no modification exists:

```java
class ErrorRegistry {
    @Modified
    private final List<ErrorMessage> messages = new ArrayList<>();

    @Modified
    public void add(@Modified ErrorMessage message) { messages.add(message); } // nothing here modifies message

    @Modified
    public void changeFirst() {
        if (!messages.isEmpty()) messages.get(0).setMessage("changed!"); // ...but this modifies an object from add()
    }
}
```

`add` only stores its argument, which links `message` to the field `messages`; `changeFirst` modifies that field's
object graph. **It is the linking engine (`maddi-modification-link`) that computes this**, carrying the modification
from `changeFirst` → field `messages` → parameter `message` of `add`, which is therefore `@Modified`. `ErrorRegistry`
is not a container, and the per-parameter check sees it.

This is the same chain as `LinkExample1` in §045, where a modifying `add` marks the field, and the field marks the
linked constructor parameter. It is why containers *can* be checked parameter-by-parameter: by the time
`UNMODIFIED_PARAMETER` is read, linking has already done the propagation.

Deriving from a container does not make you a container (§050): a subtype may add non-private methods that modify their
parameters, as long as the methods *inherited from* the container do not modify theirs, and the implementation does not
modify objects linked to those parameters.

## Linking and dependence (§045, §070)

> **Definition**: two objects are **independent** of each other (or not linked) when no modification to the first can
> imply a modification to the second, or vice versa. Conversely, two objects are **dependent** (or linked) when a
> modification to the first may imply a modification to the second, or vice versa.

Linked objects share a common sub-object — `list.subList(1,5)` is backed by `list`. Linking tracks the underlying
object, not the variable, so intermediate local assignments change nothing. Linking does not apply to objects that
cannot be modified (primitives, `String`, `java.lang.Object`, unbound type parameters — all immutable).

> **Definition**: a method or constructor parameter is **not linked** when it is not linked to any of the fields of the
> type. A method is **not linked** when its return value is not linked to any of the fields of the type.

### How linking is computed (§070)

Four base rules over a dependency graph of variables:

| | Rule | Exceptions |
|---|---|---|
| 1 | `v = w` → `v` links to `w` | |
| 2 | `v = a.method(b)` → `v` potentially links to `a` and `b` | not if `v` immutable; not to `b` if `b` immutable; not to `a` if `method` is `@Independent` |
| 3 | `v = new A(b)` → `v` potentially links to `b` | not if `b` immutable; not if `A` immutable; not if `b` is `@Independent` |
| 4 | `a.method(b)` modifying → `a` potentially links to `b` | as rule 3, with `a` in the role of `v` |

Consequences: `v = cond ? a : b` links to both; casts do not break linking; a pattern variable `a instanceof P p` links
`p` to `a`; binary operators return primitives or `String`, so `v = a + b` links to neither. Links *between* parameters
`b`, `c`, `d` of one call are covered by `@Modified`/`@NotModified` rather than computed individually, since the project
advocates containers where all parameters are `@NotModified`; `@Independent`'s extra attributes let a user be explicit.

## Accessible and hidden content (§045, §080)

> **Definition**: a type `A`, part of the object graph of the fields of type `T`, is **accessible** inside `T` when any
> of its methods or fields is accessed. The methods of `java.lang.Object` are excluded. A type that is part of the
> object graph of the fields, but is not accessible, is **hidden** (when it is an unbound type parameter) or
> **transparent** (when it is not).

> **Definition**: the **accessible content** of a type are those objects of the object graph of the fields that are of
> accessible type. The **hidden content** are those objects that are of hidden (or transparent) type.

A transparent type can be replaced by an unbound type parameter, so "hidden" covers both in practice; the analyzer warns
on transparent types and treats them internally as unbound type parameters (hence `@ImmutableContainer`), even when the
type is obviously modifiable. Type parameters are always treated as the type they extend, `Object` if unbound (§050).

When `C` extends `P`, or `T` implements `I` and `I` is the formal type, the `P`/`I` part is accessible while whatever
`C`/`T` adds stays hidden.

> **A type is not responsible for modifications to its hidden content.**

This is the central tenet. Modifications to hidden content are by definition external to the type.

Refining linking with this split:

> **Definition**: a parameter or method return value is **dependent** on the fields iff it is linked to the *accessible*
> content of the type; **independent** iff it is at most linked to the *hidden* content.

So "independent" throughout mostly means "not dependent": no linking, or linking only to hidden content.
`@Independent(hc=true)` stresses linking to the hidden part ("content linking", §080) — a `T first()` returning hidden
content, a `visit(Consumer<T>)` exposing it as an argument, a constructor storing `T t` in a field. Dependence is the
default where possible and normally goes unannotated; `@Independent(absent=true)` states it explicitly.

**Propagating modifications** (§080): a modification made by a concrete lambda propagates back to the hidden content of
the object it came from — `list.visit(sb -> sb.append("\n"))` marks `list` as `@Modified`. The analyzer does **not**
track the distinction between modification *of* hidden content and modification of accessible content beyond this
(there is no `@Modified(hc=true)` on the receiving parameter); §080 states this limit explicitly.

**Functional interfaces**: a parameter whose formal type is a functional interface from `java.util.function` is
implicitly `@IgnoreModifications`. Without it, `visit(Consumer<T>)` would make every visitor-holding type a
non-container, since `Consumer.accept` is modifying and modifies its parameter.

## Immutability (§050)

Intuitively: *after construction, an immutable type holds a number of objects; the type will not change their content,
nor exchange them for other objects, or allow others to do so. The type is not responsible for what others do to the
content of the objects it was given.*

> **Definition**: a type is **immutable** when
>
> - **Rule 0**: all its fields are effectively final.
> - **Rule 1**: all its fields are `@NotModified`.
> - **Rule 2**: all its fields are either private, or of immutable type themselves.
> - **Rule 3**: no parameters of non-private methods or non-private constructors, and no return values of non-private
>   methods, are dependent on the (accessible part of the) fields.

Rule 2 blocks external modification via direct field access; rule 3 blocks it via references obtained or shared through
parameters and return values. Rule 1 implies every method of an immutable type is `@NotModified`.

Notes:

- All primitives and `java.lang.Object` are immutable, as are `String` and unbound type parameters.
- A field of unbound type parameter type counts as immutable locally (substitute `Object`), so it need not be private.
- Constructor parameters of unbound type parameter type are hidden inside the type, so **rule 3 does not apply** to them.
- `hc=true` is added when the type has hidden content or is extendable; it is instructive only. Abstract types always
  have hidden content, so `hc=true` is implicit and `hc=false` on an abstract type draws a complaint.
- Rule 1 can be reached *eventually* (§060). Eventuality for rules 2 and 3 is considered far-fetched.

The `ArrayContainer1/2/3` and `SetBasedContainer1..6` example series in §050 walk each failure mode: dependent
constructor parameter (rule 3), public modifiable field (rule 2), exposing getter (rule 3), added modifying method
(rule 1).

### Abstract types (§050)

Variants of the rules, obeyed by the abstract methods:

> **Variant of rule 1**: abstract methods must be non-modifying.
>
> **Variant of rule 3**: abstract methods returning values must not be dependent. They cannot expose the fields via
> parameters: parameters of non-primitive, non-immutable type must not be dependent.

These exist so implementations and extensions get the chance to have the same immutability properties. Computing
immutability of interfaces is possible, but contracts are usually more practical. **No implementation of an immutable
interface is guaranteed to be immutable itself**, nor a container, unless it adds no new non-private methods.

For `sealed` types all subtypes are known, so an abstract parent's annotations can be computed rather than contracted.

### Inheritance (§050) — what the guard enforces

- Modification status may **not** go from non-modifying to modifying in a derived type. This blocks a modifying
  `equals()`/`toString()`/`Collection.size()` anywhere. The principle is *consistency of expectation*.
- Deriving cannot **increase** the immutability level. An override of a `@Modified` method may be non-modifying but may
  not be explicitly marked `@NotModified`.
- Deriving from an immutable type does not make you immutable; deriving from a non-immutable type means you can never
  be immutable.
- Permitted directions (appendix): `@Modified` → `@NotModified` yes, reverse no; independence may go
  `@Independent(absent=true)` → `@Independent(hc=true)` → `@Independent`, not right to left.

### Independence of types (§080)

> **Definition**: an **external modification** is a modification, carried out outside the type, (1) on a field, directly
> accessed from the object, or (2) on an argument or return value, executed after the constructor or method call on the
> object.

> **Definitions**: a type is **dependent** when external modifications impact the accessible content of the type. A type
> is **independent** (`@Independent`) when external modifications cannot impact the accessible content. The hidden
> content of the type is mutable or modifiable.

Equivalent to the immutability definition minus rules 0 and 1, with rules 2 and 3 restricted to fields exposed to the
outside via linking or content linking. Immutable types are independent, but a type need not be immutable to be
independent — any type communicating only via immutable types is independent (a `GetterSetter` over an `int`).

| | Mutable, modifiable | Immutable with hidden content | Immutable without hidden content |
|---|---|---|---|
| **Dependent** | `Set` | — | — |
| **Independent with hidden content** | `Iterator<T>` | `Optional<T>`, `Set.of(T)` | — |
| **Independent** | `Writer`, `Iterator<String>` | — | `int`, `String`, `Class` |

## Eventual immutability (§060)

A single state transition inside an object, from a mutable *before* phase to an immutable *after* phase.

- `@Mark("field")` — the method effecting the transition.
- `@Only(before=…)` / `@Only(after=…)` — methods whose precondition confines them to one phase.
- `@TestMark("field")` — computed on methods returning the state as a boolean.
- `@BeforeMark` — a method/parameter/field guaranteed to hold an object in its initial state.
- `after="…"` on `@FinalFields` / `@Immutable` / `@ImmutableContainer` — names the field(s) (comma-separated) carrying
  the transition.

`@Mark` and `@Only` also apply to parameters, when marked methods are called on a parameter of eventually immutable
type. **Contracted eventual immutability is not yet implemented in the analyzer** (§060).

## Other annotations (§120)

- **Not-null**: `@NotNull` / `@Nullable` (default for non-primitive parameters and return values is nullable);
  `@NotNull(content=true)` says none of the object's elements can be null, `content2=true` goes one level deeper (for
  entry sets). Computed in order: context not-null of parameters → field not-null → external not-null of parameters
  linked to fields.
- **`@Identity`**: returns its first parameter. **`@Fluent`**: returns `this`. Fluent methods are always `@Independent`
  (returning the type itself exposes nothing new), and in a container, fluent and void methods are assumed `@Modified`
  unless marked `@NotModified`/`@StaticSideEffects` (appendix).
- **`@Finalizer`**: once called, no other method may be applied to the object. Always contracted, never computed.
  Rules: fields of a type with finalizers must be effectively final; a finalizer may only be called on a field from
  within another finalizer; a finalizer may never be called on a parameter or anything linked to it.
- **`@UtilityClass`**: an immutable class that cannot be instantiated — no non-static fields, a single private unused
  constructor, static fields of immutable type. Implies `@Immutable`.
- **`@ExtensionClass(of=E.class)`**: immutable; all non-private static methods with parameters have a `@NotNull` first
  parameter of type `E` (at least one such method); parameterless non-private static methods return `@NotNull E`. Often
  not a container, since the first parameter is frequently `@Modified`.
- **`@Singleton`**: recognized via a single static field with static construction, or a precondition on the constructor
  using a private static boolean.

## Mapping to the code

### Value lattices

Numeric order matters: property writes are monotonic (`PropertyValueMapImpl.setAllowControlledOverwrite` allows
strictly-increasing overwrites only), and `Value.Immutable`/`Value.Independent` fold with `min`/`max`.

`ValueImpl.BoolImpl` — note `FALSE` is also the *default*, so "absent" and "decided false" are only distinguishable via
`getOrNull` vs `getOrDefault`:

| Constant | Value | Predicate |
|---|---|---|
| `NO_VALUE` | -1 | `hasAValue()` false |
| `FALSE` | 0 | `isFalse()`, `isDefault()` |
| `TRUE` | 1 | `isTrue()` |

`ValueImpl.ImmutableImpl`:

| Constant | Value | Annotation |
|---|---|---|
| `NO_VALUE` | -1 | (the `null` constant) |
| `MUTABLE` | 0 | — (`isMutable()` covers 0 **and** 1) |
| `FINAL_FIELDS` | 1 | `@FinalFields` |
| `IMMUTABLE_HC` | 2 | `@Immutable(hc=true)` |
| `IMMUTABLE` | 3 | `@Immutable` |

`ValueImpl.IndependentImpl` — **a different scale from `Immutable`; do not transpose the numbers**:

| Constant | Value | Annotation |
|---|---|---|
| `INDEPENDENT_DELAYED` | -1 | (undecided) |
| `DEPENDENT` | 0 | `@Independent(absent=true)` |
| `INDEPENDENT_HC` | 1 | `@Independent(hc=true)` |
| `INDEPENDENT` | 2 | `@Independent` |

`isAtLeastIndependentHc()` is `>= 1`; `isDependent()` is `== 0` and so excludes the delayed value.
`IndependentImpl` also carries `linkToParametersReturnValue` (from `dependentParameters`, `hcParameters`,
`dependentReturnValue`, `hcReturnValue`) and `dependentExceptions` (from `except`).

### Concept → property

Written by `AnnotationToProperty.annotationsToMap` on the contract side; computed by the phase analyzers (`README.md`).

| Concept | Type | Method | Parameter | Field |
|---|---|---|---|---|
| Immutability | `IMMUTABLE_TYPE` | `IMMUTABLE_METHOD` (dynamic return type) | `IMMUTABLE_PARAMETER` | `IMMUTABLE_FIELD` |
| Container | `CONTAINER_TYPE` | `CONTAINER_METHOD` | `CONTAINER_PARAMETER` | `CONTAINER_FIELD` |
| Independence | `INDEPENDENT_TYPE` | `INDEPENDENT_METHOD` | `INDEPENDENT_PARAMETER` | `INDEPENDENT_FIELD` |
| Modification | — | `NON_MODIFYING_METHOD` | `UNMODIFIED_PARAMETER` | `UNMODIFIED_FIELD` |
| Finality | — (see below) | — | — | `FINAL_FIELD` |
| Not-null | — | `NOT_NULL_METHOD` | `NOT_NULL_PARAMETER` | `NOT_NULL_FIELD` |
| Ignore modifications | — | `IGNORE_MODIFICATION_METHOD` | `IGNORE_MODIFICATIONS_PARAMETER` | `IGNORE_MODIFICATIONS_FIELD` |

**Watch the polarity**: the annotations are `@Modified`/`@NotModified`, but the properties are
`NON_MODIFYING_METHOD` / `UNMODIFIED_PARAMETER` / `UNMODIFIED_FIELD` — *true means not modified*. A contract violation
is a **decided FALSE**.

Also: `@ImmutableContainer` sets *two* properties (immutable + container); `@UtilityClass` sets three
(`IMMUTABLE`, `INDEPENDENT`, `UTILITY_CLASS`); `@Modified` maps to `UNMODIFIED_* = from(isAbsent)`; `@FinalFields` maps
to `IMMUTABLE_* = FINAL_FIELDS`; and when no independence annotation is present on a type,
`simpleComputeIndependent(typeInfo, immutable)` derives it from the immutability value.

There is **no type-level property for final fields**: `@FinalFields` on a type lands in `IMMUTABLE_TYPE` as the
`FINAL_FIELDS` level.

Three **dead channels**, each dead in a different way — check before building on any property:

- `FINAL_TYPE` ("finalType"): declared in `PropertyImpl`, registered in `PropertyProviderImpl`, and never written or
  read anywhere.
- `ANALYZER_ERROR`: *read* by `DecoratorImpl.comments()` (`DecoratorImpl.java:108`) to render comments into
  analysis-hints output, but never written by anyone.
- `PARAMETER_ASSIGNED_TO_FIELD` (`Value.AssignedToField`): its only writer,
  `TypeModIndyAnalyzerImpl.fromNonFinalFieldToParameter`, has its entire body commented out behind a FIXME. The
  method is still called, so it is a silent no-op, and `ParameterInfo.assignedToField()` always returns EMPTY.

Where a parameter's link to a field is needed, the live source is the field's `LinksImpl.LINKS` (written by
`FieldAnalyzerImpl`, phase 2), which is also what `INDEPENDENT_FIELD` is derived from.

Other properties relevant to the guard: `IMPLEMENTATIONS` (`Value.SetOfMethodInfo`, written by prepwork) is the
abstract-method → implementations edge the guard traverses.

### Defaults (appendix, `ShallowMethodAnalyzer`)

- No independence information on a type → derived from immutability: not immutable → dependent; `@Immutable(hc=true)` →
  `@Independent(hc=true)`; `@Immutable` → `@Independent`.
- Parameters of a **non-modifying** method are `@Independent` by default, regardless of the type's annotation.
  Parameters of a **modifying** method are dependent when the type is not immutable, independent when it is.
- When a type is immutable, `@Independent` becomes the default for its methods and parameters — but `@Independent(hc=true)`
  must still be written to indicate communication of hidden content.
- Factory methods (static, returning the type) and static methods in immutable types: independence is with respect to
  the *method's parameters*, not the type; parameter independence follows the parameter type's immutability.
- Every class never extended is regarded as effectively final in application mode.
- A method with a single statement returning a constant implies `@ImmutableContainer("value")`; an explicitly `final`
  field with an initializer implies `@Final` (and `@ImmutableContainer("value")` where relevant).
- Annotations are generally inherited on types, methods and parameters, within the deviation limits in
  [Inheritance](#inheritance-050--what-the-guard-enforces).

## What this means for the guard

Consequences of the above that shape `GuardAnalyzerImpl`:

1. **Modification is the default, non-modification must be proved.** A contract asserting `@NotModified` / `@Container`
   is therefore always a claim *against* the default, which is precisely why it is worth verifying.
2. **Checking a `@Container` contract per parameter is correct, but blaming it is not a local problem.** Reading
   `UNMODIFIED_PARAMETER` per parameter is exactly what `TypeContainerAnalyzerImpl` does, and it suffices, because
   linking has already propagated modifications into that value. **Detection is local; the explanation is not.** In
   the `ErrorRegistry` shape the offending parameter is `message` of `add`, yet *nothing in `add`'s body modifies
   anything* — the modification is in `changeFirst`, a different method, reached via the field. `blameParameterModified`
   scans only `target`'s own body for a direct site, so it finds nothing and returns `null`: the violation is reported
   without a "where". Blame for a linked parameter has to follow the link chain (parameter → field → the method that
   modifies the field's object graph), which is the link module's data, not the CST's.
3. **Contracts bind implementations only through inherited methods.** A subtype may add modifying methods of its own
   without violating a parent's `@Container` (§050). The guard's traversal must go through `IMPLEMENTATIONS` of the
   contracted type's own methods, not over all methods of the implementation.
4. **Rule 3 does not apply to parameters of unbound type parameter type** — they are hidden content. An independence
   guard must not flag them.
5. **`@Independent` is a three-valued scale.** A contract of full `@Independent` (2) against a computed
   `INDEPENDENT_HC` (1) is a genuine weakening, but not "dependent"; the guard currently flags only decided-DEPENDENT
   (0), so this case is silently accepted. The `@Immutable` scale has the same gap (3 vs 2) plus a trap: `hc=true` is
   implicitly present on an abstract type, so a naive 3-vs-2 diff would fire on every `@ImmutableContainer interface`.
6. **Undecided must stay silent.** `INDEPENDENT_DELAYED` (-1) and `Bool.NO_VALUE` (-1) mean "no information", not
   "violation" — and `FALSE` doubles as the Bool default, so read with `getOrNull`, never `getOrDefault`, before
   blaming. Two convenience accessors bake in the unsafe default and must **not** be used by a guard:
   `FieldInfo.isPropertyFinal()` and `isIgnoreModifications()` read their property with `getOrDefault(…, FALSE)`, so
   an undecided field reads as "not final". The computing analyzers may use them — a wrong guess only delays a fixed
   point — but a guard would blame on incomplete information.
7. **A contract map is not proof the user wrote an annotation.** `ContractReader.contracts(TypeInfo)` *always*
   contains `INDEPENDENT_TYPE`: `AnnotationToProperty` calls `simpleComputeIndependent(typeInfo, immutable)` whenever
   no `@Independent` annotation is present, yielding DEPENDENT — or INDEPENDENT for an unannotated type whose
   non-private methods only speak primitives. Keying a type-level `@Independent` guard off it would fire on
   unannotated code. `IMMUTABLE_TYPE`, `CONTAINER_TYPE` and `UTILITY_CLASS` are safe: only real annotations set them.
   For methods, parameters and fields the independence value is not synthesized, so `INDEPENDENT_METHOD` is safe.
8. **Eventual contracts (`after="…"`) cannot be guarded.** The transition fields are assignable and the marker
   methods modifying by design; the rules hold only after the mark, which the analyzer cannot see (§060). A guard
   must skip these types or it reports their design as a violation.
9. **Blame from links, not from syntax, wherever links exist.** Linking computes *exact* assignments, so
   `this.x = Objects.requireNonNull(x)`, an assignment through a local, a cast, or `list.subList(..)` all link the
   same way — while a CST scan for `this.f = <parameter>` sees only the literal shape and silently blames nothing on
   the rest. Reading the same value the analyzer decided from (a field's `LINKS` → `INDEPENDENT_FIELD`) also keeps
   blame and verdict from drifting apart. Prefer the CST only where no computed link data survives: the modification
   walks re-derive from the CST precisely because the per-call `Result.modified` map is discarded after linking.
10. **The hidden-content limit bounds what blame can say** (§080): the analyzer does not distinguish a modification of
    hidden content from one of accessible content once it has propagated to a parameter. Blame walks can therefore
    report *that* a modification propagated but not always *which part* was modified.
11. **Analysis-hints values are trusted, not verified** — they are contracts about code the analyzer cannot see.
    Only source-code contracts are guard material.

## Known gaps

Noted while summarizing. Three source-document wrinkles found alongside these were fixed in the AsciiDoc on
2026-07-15: the appendix said methods default to `@NotModified` (a typo for `@Modified`, contradicting §030/§050 and
the code); §050 used a non-existent `@Dependent` annotation; and §050's "Dynamic type annotations" example was titled
and named `SetBasedContainer6` when it revisits `SetBasedContainer4`.

1. **Two section files are never included in the book.** `index.adoc` omits `090-further-notes.adoc` (extendability,
   eventual immutability, `@Commutable`) and `100-linking-by-example.adoc` (the linking reference). Both look
   finished, and §050's live `<<extendability>>` cross-reference dangles into `090` — asciidoctor warns "possible
   invalid reference: extendability" on every build. Almost certainly accidental, but restoring them is a decision
   about the book's structure, so it is left open.
2. **The `@Modified` `value()` attribute (named fields) is parsed but ignored** — `AnnotationToProperty` logs
   "Ignoring field list in @Modified" behind a FIXME. A real unimplemented feature, not a typo.
