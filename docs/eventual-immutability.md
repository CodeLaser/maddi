# Eventual immutability — plan and status

**Status: implemented and working on real code.** Contracts, propagation, interface inheritance and
independence-after-mark are all in; measured on maddi's own CST, 30 methods carry a mark and 4 types are
eventually immutable. What is not done is listed under "Not done, on purpose" and "Stage 2, what is not
covered"; none of it blocks use.

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
| 2 | Compute eventuality by propagation over eventually immutable fields | done |
| 3 | Assignment-incompatible-with-precondition, for hand-rolled flag types | probably never needed |
| — | Interface inheritance: implementation → abstract method → interface type-level | done |
| — | Independence after the mark, with the leaked-type-must-be-eventual constraint | done |

## State on real code (dogfood, maddi's own CST)

Measured with `--preload-analysis-results-dirs` (mandatory — see `dogfood/README.md`; without it the
annotated APIs are absent and every figure reads as a zero):

- **30 methods** carry `EVENTUAL_METHOD`. `TypeInfoImpl.commit` is `@Mark("inspection")`,
  `hasBeenInspected()` is `@TestMark("inspection")`, `setOnDemandInspection()` is
  `@Only(before="inspection")`; likewise across `MethodInfoImpl`, `FieldInfoImpl`, `ParameterInfoImpl`,
  `TypeParameterImpl`, `CompilationUnitImpl` and `ModuleInfoImpl`. Six of the 30 sit on **interfaces**,
  reached by the implementation → abstract-method → interface chain.
- **4 types** are eventually immutable: `ModuleInfo.Provides` and `ModuleInfo.Uses` reach
  `@Immutable(hc=true)` after their mark, their two implementations `@FinalFields`.

Nobody annotated any of it; all of it is computed.

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

## Stage 2, as built

`TypeEventualAnalyzer` / `TypeEventualAnalyzerImpl`, phase 4.3, run from `SingleIterationAnalyzerImpl`
after the container analyzer.

**Per method.** Walk the body; for each call whose receiver is `this.f` (with `f` an own field whose type is
eventually immutable) or `this` (an inherited marked method), take the callee's `Value.Eventual` and place the
caller on the same side of the transition: `@Mark` → `@Mark`, `@Only(before)` → `@Only(before)`,
`@Only(after)` → `@Only(after)`. Mixing sides concludes nothing. `@Mark` additionally requires the method to
modify, so a claim can never contradict phase 1. The label is *our* field's name, not the support class's
internal one: what our callers observe is "this.inspection has been set".

`@TestMark` propagates only through a body that is exactly `return <testmark call>` or
`return !<testmark call>`; anything looser would classify every method that merely consults the state — an
`assert`, a guard — as a state test.

**Per type.** Collect the fields marked by the type's own `@Mark` methods, and ask
`TypeImmutableAnalyzer.immutableIgnoringModificationOf` for the level the type reaches once those fields can
no longer change. Same computation as the unconditional verdict with exactly one relaxation, so the two
cannot drift apart. Written only when it beats the unconditional verdict.

**Contracts are materialized.** A hand-written `@Mark` on a source method was previously visible only through
the `ContractReader`; the analyzer now writes it into `analysis()`, because everything downstream (codec,
guard, IDE daemon) looks there. The same reader is the fallback when reading a *called* method's contract:
a compiled type is shallow-analyzed lazily, per method, so a support class usually arrives with an empty
property map.

## Stage 2, what is not covered

- **Field finality is not relaxed.** A type whose transition is a plain assignable flag (`private boolean
  frozen`) still fails rule 0. That is the stage 3 case, and it is why `Freezable` itself is contracted.
- **Guarded fields.** `findFieldsGuardedByEventuallyImmutableFields` from `ComputingTypeAnalyser` is not
  ported yet: a private field assigned only inside methods guarded by the mark should ride along with it
  (this is what lets `EventuallyFinal.value` ride along with `isFinal`). Pure set containment, no dataflow.
  Worth doing once a run over maddi itself shows which types need it.
- **Inherited marks at type level.** A `Freezable` subclass gets its *methods* annotated, but not its own
  type-level verdict; that needs the old `approvedPreconditionsFromParent`.

## Interfaces (the hierarchy blocker)

`TypeInfoImpl` cannot be certified while `TypeInfo` -- the interface -- is plain mutable: it declares
`commit`, `builder`, `setOnDemandInspection`, so the hierarchy rule returns MUTABLE for every
implementation before any field reasoning happens. Eventuality therefore has to reach the interfaces.
Three pieces, all in place:

1. `AbstractMethodAnalyzerImpl.methodEventual` copies `EVENTUAL_METHOD` from implementations to the
   abstract method, the same implementation-to-abstract shape phase 6 already uses for modification and
   independence. All implementations must agree on the side of the transition *and* the mark label.
2. `TypeImmutableAnalyzer.AfterMark` generalizes the relaxation from fields to `(fields, methods)`. An
   interface has no fields, only abstract methods, so excusing the `@Mark` / `@Only(before=)` methods is
   what lets it reach a level at all.
3. `immutableSuper` consults a supertype's `EVENTUALLY_IMMUTABLE_TYPE` when computing an after-mark
   level: after *our* mark the supertype has been marked too -- the transition belongs to the object,
   not to one type -- so it contributes the level it reaches after its own mark.

A fourth change was needed to make the chain actually close: **`@TestMark` now implies
`@NotModified`**. Observing which side of the transition an object is on is not changing it (§060: "these
methods can be called any time"). Without it, `EventuallyFinalOnDemand.isFinal()` defaults to modifying,
so `hasBeenInspected()` is modifying, so the interface fails rule 1 and stays at FINAL_FIELDS -- and a
FINAL_FIELDS supertype counts as mutable (`ImmutableImpl.isMutable()` is `value <= 1`), which drags the
implementation back down to MUTABLE.

`TestEventualPropagation.test5` pins the whole chain: implementation -> abstract method -> interface
type-level -> back down to the implementation.

### Why the dogfood run still shows nothing

`dogfood/` analyzes only `maddi-cst-impl`; `TypeInfo` lives in the **maddi-cst-api jar**. A jar type is
never part of the abstract-method batch, so nothing propagates into it. Exercising interface propagation
on maddi's own code needs the interface module co-analyzed *as source*. The input configuration format
supports several source sets, but the plugin computes one Gradle project's, and a sibling project is
consumed as a jar by design (`ComputeSourceSets`: no cross-project source recursion, which Gradle 9
rejects as unsafe). Options, in increasing order of work: a second Gradle source set inside the dogfood
project; teaching the plugin to emit source sets for included builds; or hand-merging two generated
configurations.

## Not done, on purpose

- **`@BeforeMark`** is parsed by nobody yet. It belongs with the "track objects guaranteed to be in the
  after (or before) state" work — a state axis on a value (return value, parameter, field), which is the
  natural follow-on to stage 2 and the thing that should inform the lattice decision above.
- **Preconditions.** `PropertyImpl.PRECONDITION_METHOD` and `Value.Precondition` exist but nothing writes
  them; that remains the case. We are not reviving the precondition/postcondition subsystem.
- **Companion methods and aspects.** Out of scope, and not needed for the patterns maddi uses.
- **Refutation as a violation.** The guard warns `contract-unverifiable` on a promise it cannot check;
  turning a *refuted* one into an error needs a tri-state from `immutabilityOf`, which today collapses
  "provably mutable" and "undecided" into null.

## Reference material

The old implementation lives outside this repo, in the e2immu analyser (`analyser/src/main/java/org/e2immu/
analyser/analyser/util/`): `DetectEventual`, `AssignmentIncompatibleWithPrecondition`,
`MethodCallIncompatibleWithPrecondition`, plus `analyser/impl/computing/ComputingTypeAnalyser` and
`analyser/check/CheckEventual`. It is ~4 years old and written against the old delay model (`CausesOfDelay`,
`DV`, per-statement `VariableInfo`), so it is a design reference, not code to port line by line — with the
exception of `findFieldsGuardedByEventuallyImmutableFields`, which transfers nearly verbatim.
