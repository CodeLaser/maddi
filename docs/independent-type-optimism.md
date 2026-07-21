# `INDEPENDENT_TYPE` can be permanently optimistic

**Status: reproduced defect, not fixed.** The obvious fix is correct but unmasks a second, independent
inconsistency; see "Why the obvious fix is not enough". Found while building eventual immutability
(`eventual-immutability.md`), which is how the asymmetry became visible.

## The defect

`TypeIndependentAnalyzerImpl.computeIndependentType` ends with

```java
return indyFromHierarchy.min(fromFieldsAndAbstractMethods);
```

and `Value.min(null)` returns the receiver (`v == null || compareTo(v) <= 0 ? this : v`). So when
`loopOverFieldsAndAbstractMethods` returns `null` — its way of saying *undecided*, set whenever a field's
`INDEPENDENT_FIELD` or an abstract method's `INDEPENDENT_METHOD` has not been decided yet — the result
silently becomes `indyFromHierarchy`, which for a type with no supertypes is `INDEPENDENT`.

"We do not know yet" is thereby read as "independent": the optimistic direction. `go()` writes that value,
and `TolerantWrite.setAllowControlledOverwrite` enforces monotonicity, so the later, correct DEPENDENT is
refused. The type stays independent forever.

## Reproduction

`TestEventualPropagation`'s `INPUT7` fixture, on an unmodified tree:

```java
interface I {
    @Mark("t") void commit(String s);
    @Only(before = "t") List<String> leak();
}
static class Impl implements I {
    private final List<String> list = new ArrayList<>();
    @Override public List<String> leak() { return list; }
}
```

Measured:

```
type I    INDEPENDENT_TYPE=@Independent   IMMUTABLE_TYPE=@FinalFields
  m leak  INDEPENDENT_METHOD=@Dependent
type Impl INDEPENDENT_TYPE=@Dependent     IMMUTABLE_TYPE=@Mutable
  m leak  INDEPENDENT_METHOD=@Dependent
```

`I` is `@Independent` while its own abstract method `I.leak()` is `@Dependent` — an interface that hands
out a `List<String>`. `loopOverFieldsAndAbstractMethods` returns DEPENDENT the moment it sees a dependent
abstract method, so the type-level value cannot have been computed from a settled `leak()`; it was decided
in an earlier iteration, while `leak()` was still undecided, and frozen there.

Note the asymmetry: the class `Impl` is correct. Only the interface is poisoned, because an abstract
method's `INDEPENDENT_METHOD` arrives late — it is copied up from implementations by phase 6
(`AbstractMethodAnalyzerImpl.methodIndependent`), after the type analyzers have already run.

### A self-confirming loop, not just one bad value

The optimistic value feeds back. On `TestModificationFunctionalE7`'s `INPUT4`:

```
type TryData     indep=@Independent  imm=@Immutable(hc=true)
  m throwingFunction  INDEPENDENT_METHOD=@Independent
type TryDataImpl indep=@Independent  imm=@Immutable(hc=true)
  m throwingFunction  INDEPENDENT_METHOD=null        <- never decided at all
```

`TryDataImpl` is a class with a `private final ThrowingFunction` field returned directly by an accessor —
a textbook dependent accessor — yet it is reported `@Immutable(hc=true)`. The type was promoted while its
accessor was undecided; the promotion made the type immutable; an accessor returning an immutable type is
independent; which confirms the type. The accessor's own value is then never needed, and stays `null`.

With the loop broken (see below) the honest values appear: `TryData` and `TryDataImpl` become
`@Dependent`, `TryDataImpl` becomes `@Mutable`, and `TryDataImpl.throwingFunction()` resolves to
`@Dependent`.

## Why the obvious fix is not enough

The obvious fix is to wait — `return null` when `fromFieldsAndAbstractMethods == null` — which is exactly
what `TypeImmutableAnalyzerImpl` does at the corresponding spot (`loopOverFieldsAndMethods == null`), and
`go()` already has the deliberate cycle-breaking answer for a value that never arrives (it writes
INDEPENDENT under `activateCycleBreaking`). A null there costs an iteration, not a verdict.

It produces correct independence, and it breaks two tests, both on E7 shape 4:

- `TestModificationFunctionalE7.test4`: `run`'s modified set grows from
  `run:0:td, run` to `ThrowingFunction.apply:0:o, run:0:td, run`.
- `TestShadowModificationPass.testBuilderCallback`: `unmodifiedParameter ThrowingFunction.apply:0:o`
  becomes a main-vs-shadow divergence, breaking the "correctly-analyzed code must diff clean" invariant.

The chain is: correct (pessimistic) independence → `TryData` no longer immutable → the conservative
marking at the opaque SAM application `td.throwingFunction().apply(td)` is no longer immutable-guarded →
`apply:0:o` enters `run`'s modified set → the shadow pass, walking reachability, concludes `apply:0:o` is
modified, while the main pass's `UNMODIFIED_PARAMETER` says it is *not*.

**Main is right and the new seed is a false positive.** `ThrowingFunction.apply` is realized only by the
method reference `this::methodBody`, and `methodBody` never touches its parameter. The two passes
disagree because the engine holds two contradictory conventions for a SAM with no registered
implementations:

- `AbstractMethodAnalyzerImpl.doMethodWithoutImplementation` forces its parameters unmodified/independent
  (optimistic — nothing is known, so nothing is assumed to happen);
- the link computer conservatively marks the argument of an opaque application modified (pessimistic).

`IMPLEMENTATIONS` is populated from `methodInfo.overrides()` (`MethodAnalyzer`), so a SAM satisfied only
by a lambda or method reference genuinely has none. That is by design, not a bug — which is why this
contradiction cannot be fixed by populating `IMPLEMENTATIONS`.

While `TryData` was wrongly immutable, the contradiction was invisible: the immutable guard suppressed the
conservative marking. Fixing independence removes the mask.

### The narrowing does not dodge it

An attractive narrowing — wait only when an abstract *method* is undecided, keep concluding when only
fields are — does not help. In `TryData` the undecided element *is* the abstract method
`throwingFunction()`, so the narrowing produces the same result and the same fallout.

## Options

1. **Reconcile the two SAM conventions first, then fix independence.** The principled order. Either
   `doMethodWithoutImplementation` stops forcing optimistic values for a SAM that is actually applied
   somewhere, or the link computer stops seeding the formal parameter of an implementation-less abstract
   method. Both change modification results beyond this defect and want the corpus proving ground.
2. **Fix independence and accept the new E7 shape**, updating both tests. Rejected here: it would enshrine
   a false positive (`apply:0:o` is demonstrably not modified) and, worse, would silence a real
   main-vs-shadow inconsistency by editing the invariant that exists to catch it.
3. **Make the optimistic value revisable** rather than frozen — i.e. let this particular write escape
   `TolerantWrite`'s monotonicity. Rejected: monotonic overwrite is a deliberate engine invariant
   (`analysis-rewiring.md`); subverting it locally trades a wrong value for an unstable one.

Option 1 is the recommendation.

## Scope of the damage today

Anything downstream of `INDEPENDENT_TYPE` on an *interface* (or any type whose fields/abstract methods
settle late) can be over-optimistic: `IMMUTABLE_TYPE` via the independence gate in
`computeImmutableType`, and everything derived from it. Concrete types are largely unaffected, as `Impl`
above shows. Eventual immutability is affected only indirectly — it reads the same gate — and its own
after-mark path already waits instead of concluding, precisely to avoid inheriting this bug
(`TypeIndependentAnalyzerImpl.computeIndependentType`, the `!afterMark.isNone()` branch).
