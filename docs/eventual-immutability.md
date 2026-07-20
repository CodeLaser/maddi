# Eventual immutability — plan and status

**Status: stage 1 (contracts) done; stage 2 (propagation) not started.**

Concepts: `road-to-immutability` §060 (`sections/060-eventual.adoc`). This note records *how* we are bringing
that chapter back into the current engine, and what was deliberately left out.

## Why

A very large part of maddi is written in eventually immutable style, mostly by holding a support class:
`TypeInfoImpl.inspection` is an `EventuallyFinalOnDemand<TypeInspection>`, and `MethodInfoImpl`,
`FieldInfoImpl`, the four `*InspectionImpl`s, `FactoryImpl`, `PredefinedImpl`, `InfoMapImpl` and
`TypeParameterImpl` follow the same commit-once shape. Without eventual immutability the analyzer cannot
certify its own code, which makes dogfooding (running the IDE plugins on maddi itself) much less useful.

## The key observation

The old e2immu analyser (`DetectEventual`, `ComputingTypeAnalyser.computeApprovedPreconditions*`) computed
eventuality from **preconditions**. We do not need that machinery, because maddi's own eventual types are
almost all *consumers* rather than *definers*:

- `DetectEventual`'s propagation path — old `eventualFromEventuallyImmutableFields`, step 3 of `detect()` —
  derives `@Mark`/`@Only` from calls on a field of eventually immutable type. It uses **no preconditions at
  all**: only the field type's eventuality, the method's `@Modified`, and which marked method is called.
- The precondition-based paths are needed only to *define* a new eventual type from scratch. There are ~10
  such types (the support classes) and they are already hand-annotated.
- `MethodCallIncompatibleWithPrecondition` additionally needs companion methods and aspects (for the
  `set.isEmpty()` ⇔ `0 == set.size()` style of transition). Nothing in maddi needs it.

So: **contract the leaves, propagate everywhere else, never compute a precondition.**

## Staging

| Stage | What | Status |
|---|---|---|
| 0 | Value model: `Value.Eventual`, `Value.EventuallyImmutable` + properties | done |
| 1 | Read `@Mark`/`@Only`/`@TestMark` and `after="…"` as contracts | done |
| 2 | Compute eventuality by propagation over eventually immutable fields | **next** |
| 3 | Assignment-incompatible-with-precondition, for hand-rolled flag types | probably never needed |

## Stage 0/1, as built

- `Value.Eventual` (`ValueImpl.EventualImpl`) carries all of `@Mark` / `@Only` / `@TestMark` in one record —
  a method is at most one of them and they share the mark label. Properties `EVENTUAL_METHOD` and
  `EVENTUAL_PARAMETER` (`@Mark` travels to a parameter when a marked method is called on it).
- `Value.EventuallyImmutable` (`ValueImpl.EventuallyImmutableImpl`) is the type-level `after="…"` plus the
  level reached after the mark. Property `EVENTUALLY_IMMUTABLE_TYPE`.
- `EVENTUALLY_FINAL_FIELD` holds the mark label of `@Final(after=)` / `@NotModified(after=)`.
- All of it is read in `AnnotationToProperty`, so both the `ShallowAnalyzer` and the `ContractReader` (and
  therefore the guard) see it. `GuardAnalyzerImpl.isEventual` now reads the property instead of scanning
  annotation strings by hand.
- `EventuallyFinalOnDemand` was the one support class without annotations; it now has them. Since
  `TypeInfoImpl.inspection` is exactly that type, it is the linchpin of stage 2.

**Mark labels are field *names*, not `FieldInfo`.** A mark is frequently inherited (`Freezable.frozen` seen
from a subclass), so the field is often not visible in the type carrying the annotation. Names are also
trivially rewireable and language-agnostic.

### Deliberately additive

Stage 1 changes no existing property value. In particular `@ImmutableContainer(after="frozen")` still writes
`IMMUTABLE_HC` into `IMMUTABLE_TYPE`, exactly as before, and `@Final(after=)` still writes `FINAL_FIELD=true`.

That is optimistic — before the mark the type is *not* immutable and the field is *not* final — but the
immutability lattice (`ValueImpl.ImmutableImpl`, a flat 0..3 int) is combined with `min`/`max` throughout the
analyzer, and independence in particular is derived from it. Moving eventuality into that lattice will change
results, so it is a separate, deliberate step, to be taken once stage 2 exists and can be tested.

## Stage 2 — what it needs to do

1. **Per method**: scan for calls on `this.f` (with `f` a final field of eventually immutable type) or on
   `this` (an inherited marked method). Take the callee's contracted `Value.Eventual` and combine with the
   method's own `@Modified`:
   - callee `@Mark` + modifying → `@Mark`
   - callee `@Only(before)` → `@Only(before)`
   - callee `@Only(after)` + non-modifying → `@Only(after)`
   - inconsistent across fields → not eventual

   This is the old `eventualFromEventuallyImmutableFields`, but resolved syntactically over the CST rather
   than through the old `CONTEXT_IMMUTABLE` context-property propagation.

2. **Per type**: eventually immutable at level L when the immutability rules hold once the eventual field
   *and the fields guarded by it* are set aside. Port `findFieldsGuardedByEventuallyImmutableFields` from
   `ComputingTypeAnalyser` — it is pure set containment ("the methods that assign this field ⊆ the methods
   guarded by that field"), no dataflow. This is what lets `EventuallyFinal.value` ride along with `isFinal`,
   and what `TypeInspectionImpl`'s builder fields need.

3. Inheritance: `approvedPreconditionsFromParent` in the old code; a subtype inherits its parent's mark.

## Not done, on purpose

- **`@BeforeMark`** is parsed by nobody yet. It belongs with the "track objects guaranteed to be in the
  after (or before) state" work — a state axis on a value (return value, parameter, field), which is the
  natural follow-on to stage 2 and the thing that should inform the lattice decision above.
- **Preconditions.** `PropertyImpl.PRECONDITION_METHOD` and `Value.Precondition` exist but nothing writes
  them; that remains the case. We are not reviving the precondition/postcondition subsystem.
- **Companion methods and aspects.** Out of scope, and not needed for the patterns maddi uses.

## Reference material

The old implementation lives outside this repo, in the e2immu analyser (`analyser/src/main/java/org/e2immu/
analyser/analyser/util/`): `DetectEventual`, `AssignmentIncompatibleWithPrecondition`,
`MethodCallIncompatibleWithPrecondition`, plus `analyser/impl/computing/ComputingTypeAnalyser` and
`analyser/check/CheckEventual`. It is ~4 years old and written against the old delay model (`CausesOfDelay`,
`DV`, per-statement `VariableInfo`), so it is a design reference, not code to port line by line — with the
exception of `findFieldsGuardedByEventuallyImmutableFields`, which transfers nearly verbatim.
