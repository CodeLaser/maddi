# Making the `Info` hierarchy eventually immutable — diagnosis

**Status: note (diagnosis, 2026-07-22). No fix landed yet.** Measured on the dogfood run
(`cst-api` + `cst-impl` co-analyzed as source, with `--preload-analysis-results-dirs`).

The goal: certify `TypeInfoImpl`, `MethodInfoImpl`, `FieldInfoImpl`, `ParameterInfoImpl`,
`TypeParameterImpl` (and their `TypeInfo`/… interfaces) as eventually immutable, the way
`ModuleInfo.Provides`/`Uses` already are. All of them carry their **marks** correctly today
(`commit` = `@Mark("inspection")`, `hasBeenInspected`/`hasBeenCommitted` = `@TestMark`,
`setOnDemandInspection` = `@Only(before)`), but **not one gets a type-level eventual verdict**.

## The verdicts (FPDUMP, in-memory, not the codec)

Every target — impl *and* interface — is concluded **`@Mutable` (0)**, written, not undecided:

```
@Mutable   TypeInfo, MethodInfo, FieldInfo, ParameterInfo, TypeParameter   (interfaces)
@Mutable   TypeInfoImpl, MethodInfoImpl, FieldInfoImpl, ParameterInfoImpl, TypeParameterImpl
@Mutable   InfoImpl                                                          (abstract base)
```

(`immutableType=None` in the JSON is ambiguous: `ImmutableImpl.encode` returns null for
`value <= 0`, so both MUTABLE and undecided serialize as absent. FPDUMP disambiguates: all are
written MUTABLE.)

## The causal chain (verified end to end)

1. **`EventuallyFinalOnDemand.get()` is modifying.** In the *before* state it runs the on-demand
   loader (`onDemand.run()` → `setFinal`, `EventuallyFinalOnDemand.java:37`). It carries no
   `@NotModified`, so both the shallow analyzer and inference conclude it modifies.
2. **Every `*InfoImpl` accessor that reads through `inspection.get()` inherits `@Modified`** —
   `access()`, `javaDoc()`, `fullyQualifiedName()`, `descriptor()`, `isSynthetic()`, `typeInfo()`,
   `hasBeenAnalyzed()`, `translate()`. Verified contrast: `MethodInfoImpl.simpleName()` (returns a
   plain final field) is `nonModifying=true`; `access()` (via `inspection.get()`) is `false`.
3. **The abstract-method analyzer copies those `@Modified` verdicts up to the `Info` interface.**
   `Info.access()`, `Info.fullyQualifiedName()`, … are all `@Modified`.
4. **A type with modifying methods can't be immutable.** `Info` is fieldless and
   `@Independent(hc=true)`, yet rule 1 fails → it caps at **`@FinalFields`**. Its parent `Element`
   reaches `@Immutable(hc=true)` precisely because it has no such lazy-getter method (its
   `analysis()` is a throwing default, `Element.java:188`).
5. **A FINAL_FIELDS supertype counts as mutable** (`ImmutableImpl.isMutable()` is `value <= 1`) →
   `TypeImmutableAnalyzerImpl.computeImmutableType` line 124 returns MUTABLE for every subtype.
   So `InfoImpl` and every `*Info`/`*InfoImpl` are dragged to `@Mutable`.
6. The eventual phase (`TypeEventualAnalyzerImpl.computeTypeLevel`) excuses `@Mark`/`@Only(before)`
   methods but **not** plain `@Modified` accessors, so it cannot rescue the interface.

### What is NOT the blocker

- **The concrete impls' own accessors are not self-blocking.** They modify `inspection`, but
  `inspection` is the *marked* field, and `loopOverFieldsAndMethods` already excuses a marked
  field's modification (`TypeImmutableAnalyzerImpl.java:221`). The impls are dragged down purely by
  their **supertypes** (`InfoImpl` + the `*Info` interface).
- **`InfoImpl`'s own state is fine.** It is `@Independent(hc=true)`, `@Container`,
  `propertyValueMap` is `unmodified=true`. Its only real blocker is the `Info` supertype.
  (Secondary: `InfoImpl.analysis()` reads `nonModifying=false`, from the debug-gated
  `ConsumptionEdgeRecorder.record(this)` call — worth a look, but not the dominant cause.)

## The fix is a small tree, not one lever

To certify the family, in dependency order:

1. **Method-level eventual non-modification primitive.** A method that is non-modifying only after
   a mark — `@NotModified(after="isFinal")` — as a first-class, computed property
   (`EVENTUALLY_NON_MODIFYING_METHOD`, mirroring the field-level `EVENTUALLY_FINAL_FIELD`). Today
   `AnnotationToProperty` parses `@NotModified(after=)` on a **method** by *dropping* the `after=`
   and collapsing to an unconditional `@NotModified` (line 180–182; `finalAfter` is consumed only
   for fields at line 317) — which is false in the before state. No method in the tree uses
   `@NotModified(after=)` today, so fixing the parse regresses nothing.
2. **Contract `get()`** as `@NotModified(after="isFinal")` (a jar support leaf — contracting is
   correct; it flows through the shallow path).
3. **Compute** the property on the source impl accessors: a method whose only modification flows
   through `this.<eventually-immutable field>.<eventually-non-modifying call>` is
   `@NotModified(after="<field>")`. Natural extension of `computeEventual`/`labelsOfReceiver`.
   (Per `SourceContractMaterializer`'s philosophy, modification is *computed*, never trusted from a
   source annotation — so the impl accessors must be computed, not hand-annotated.)
4. **Propagate** impl → abstract via `AbstractMethodAnalyzerImpl`, so the `Info` interface's
   abstract accessors become `@NotModified(after="inspection")`.
5. **Type-level eventual verdict from after-labels.** `Info` has **no `@Mark` method** (its mark
   lives on the subclasses' `inspection` field), so `computeTypeLevel` must derive the mark label
   from its eventually-non-modifying methods' `after=` labels, then excuse those methods →
   `Info` = `@Immutable(hc=true, after="inspection")`.
6. **Inherited mark at the type level (subclass → abstract superclass).** `InfoImpl` also has no
   mark of its own (the `inspection` field is in its subclasses), so it needs the subclass's
   eventual verdict propagated onto the abstract base — the inverse of the old
   `approvedPreconditionsFromParent`, the piece `docs/eventual-immutability.md` lists as not ported.
   Without it, `immutableSuper(InfoImpl, afterMark)` returns `@Mutable` and line 124 keeps dragging
   every concrete impl down.

Steps 1–2 are unambiguous groundwork. Steps 3–4 are the `#1` computation/propagation. Steps 5–6
are two *new* capabilities (label-from-non-modification; inherited-mark-from-subclass), step 6
being an explicitly deferred item. The eventuality stays out of the `IMMUTABLE_TYPE` lattice
throughout (`EVENTUALLY_IMMUTABLE_TYPE` only), as today.

## Steps 1–5, as built (2026-07-22) — and the wall they hit

Landed: property `EVENTUALLY_NON_MODIFYING_METHOD` (method-level twin of `EVENTUALLY_FINAL_FIELD`,
codec-registered); `AnnotationToProperty` parses `@NotModified(after=)` on a method into it (was
dropping the `after=` and asserting an unconditional, false, `@NotModified`); `EventuallyFinalOnDemand.get()`
contracted `@NotModified(after="isFinal")`; `TypeEventualAnalyzerImpl.computeEventuallyNonModifying`
computes it on source methods (a `@Modified` method whose every modification is a call on `this.<own
eventually immutable field>` or a `this`-forward to another eventually-non-modifying method — never a
`@Mark`/`@Only(before)` call, which *is* the transition); `AbstractMethodAnalyzerImpl.methodEventuallyNonModifying`
propagates impl→abstract; `computeTypeLevel` derives the type mark label from the after-labels when
there is no `@Mark` method (the `Info` case). Pinned by `TestEventualPropagation.test10`/`test11`;
analyzer suite 224/0, modification-common 52/0; no deterministic regression (the `Value.*` interface
cluster flips FINAL_FIELDS↔MUTABLE run-to-run on its own — confirmed by two identical reruns).

Measured on the dogfood: **149 methods** now carry `EVENTUALLY_NON_MODIFYING_METHOD`, and it
propagates to the `Info` interface for the clean read-through accessors — `Info.access()`,
`javaDoc()`, `isSynthetic()` all carry `{inspection}`. `eventualMethod` 30, `eventuallyImmutableType`
4 unchanged.

But the `Info` interface still does **not** reach an eventual verdict, and the reason is *not* step 6.
Five abstract accessors stay `@Modified` because they modify through a **cross-reference to another
`Info` object**, not through `this.inspection`:

- `MethodInfoImpl.descriptor()` → `this.typeInfo.descriptor()` (reads through another `TypeInfo`)
- `ParameterInfoImpl.typeInfo()` → `this.methodInfo.typeInfo()`
- `ParameterInfoImpl.fullyQualifiedName()` → `this.methodInfo.fullyQualifiedName()`
- `translate(...)` genuinely constructs new objects
- `hasBeenAnalyzed()` similar cross-reads

`computeEventuallyNonModifying` excuses a call only on `this.<own eventually immutable field>`
(`inspection`); it does not excuse `this.typeInfo.descriptor()`, because `typeInfo`'s type (`TypeInfo`)
is itself the blocked interface (not yet eventually immutable) and, even if it were, its mark is a
*different object's* lifecycle. The `Info` types form a **mutual-reference cluster** (parameter →
method → type → …) whose eventual immutability is a joint fixpoint: each needs the others.

## The cluster is an all-or-nothing greatest fixpoint (2026-07-22, deeper dig)

Chasing the five accessors to the bottom (contrary to a first fear, they do **not** modify through a
deep transitive graph — `ParameterInfoImpl.parameterizedType()` returns a plain field, the stream/lambda
calls are non-modifying JDK, so `descriptor()` modifies *only* through `this.inspection` via
`parameters()` and `this.typeInfo` via the cross-ref). The real obstruction is a **circular
recognition** with three interlocking pieces, none of which can resolve first:

1. **Cross-reference field recognition.** `MethodInfoImpl.descriptor()` → `this.typeInfo.descriptor()`,
   `ParameterInfoImpl.typeInfo()` → `this.methodInfo.typeInfo()`, `…fullyQualifiedName()` →
   `this.methodInfo.fullyQualifiedName()`. Each is excusable *iff* the cross-referenced field type
   (`TypeInfo`, `MethodInfo`) is recognized eventually immutable — which is the blocked verdict itself.
2. **Inherited mark, subclass → abstract superclass.** Every `*InfoImpl` extends `InfoImpl`, which has
   **no mark of its own** (its accessors are abstract, implemented in the subclasses; verified: zero
   `EVENTUAL_METHOD`/`EVENTUALLY_NON_MODIFYING_METHOD` on `InfoImpl` itself). So `InfoImpl` can only get
   an eventual verdict *from* its subclasses — but by the hierarchy rule the subclasses need `InfoImpl`
   eventual first (a FINAL_FIELDS/MUTABLE supertype forces them MUTABLE). This is the deferred
   `approvedPreconditionsFromParent`, here in its subclass→parent direction.
3. **Interface propagation** (already built: `methodEventuallyNonModifying`, and the type-level
   label-from-after in `computeTypeLevel`).

All three are mutually circular. A least-fixpoint monotone pass (what the analyzer runs) concludes
nothing from nothing; the cluster only resolves as a **greatest fixpoint**: optimistically assume the
whole cluster is eventually immutable, compute, and keep it iff nothing contradicts. That is sound as a
*coinductive* eventual claim — `@Immutable(after="inspection,typeInfo,…")`, the joint transition of
the cluster — but it cannot be reached by the write-once, monotone outer loop without an
optimistic-seed-and-verify pass, i.e. the same shape as the engine's existing immutability cycle-breaking,
extended to eventual immutability and to the subclass→superclass mark inheritance.

**Assessment.** This is not an incremental extension of steps 1–5; it is one indivisible mechanism
(optimistic cluster seeding + verification + subclass→parent mark inheritance) that has to break all
three circles at once, and it touches the engine's most guarded part (cycle-breaking, whose golden rule
is a byte-identical FPDUMP A/B on the certified corpus). It should be scoped and funded as its own
piece, behind a gate until a corpus A/B clears it. Steps 1–5 are the correct, self-contained substrate
it will build on — they land the entire self-field pattern and the interface method propagation; the
cluster fixpoint is the distinct next investment.

## Prototype: `EventualCluster`, gated on `EVENTUALCLUSTER` (2026-07-22)

Built the optimistic half of the greatest fixpoint (`EventualCluster`, injected into the eventual and
immutable analyzers). Cluster identification is candidates + upward hierarchy closure (§"how the cluster
set is identified" above): a *direct* candidate has a `@Mark`/`@Only`/`@TestMark` method, an
`EVENTUALLY_NON_MODIFYING_METHOD`, or an `EVENTUALLY_IMMUTABLE_TYPE`; its supertypes join by closure
(the only way `InfoImpl` and the interfaces, with no eventual method, enter). Under the gate the eventual
analyzer's cross-reference check and the immutable analyzer's `immutableSuper` treat a candidate as
eventually immutable (capped at immutable-HC) before its verdict is proven.

**Result — the circularity does break.** `eventuallyImmutableType` goes **4 → 17**, *stable across two
reruns* (identical set), and the joint marks are exactly the predicted coinductive transitions:
`TypeInfoImpl` after `inspection`, `MethodInfoImpl` after `inspection,typeInfo`, `FieldInfoImpl` after
`inspection,owner`, `ParameterInfoImpl` after `inspection,methodInfo,parameterizedType`,
`CompilationUnitImpl` after `fingerPrint,types`, plus `TypeParameterImpl`, `ModuleInfoImpl`,
`FieldReferenceImpl`, `ParameterizedTypeImpl`, `ThisImpl` … Gate OFF returns exactly 4 (golden rule
intact); analyzer suite 227/0 with the gate compiled in.

**Two ceilings the prototype exposes, both worth the follow-up:**

1. **Level caps at FINAL_FIELDS(1), not IMMUTABLE-HC(2).** `TypeInfoImpl`-after-`inspection` is level 1
   with `independentType=2` — so independence is *not* the cap. The cap is a **cross-reference through a
   wrapper**: `TypeInfoImpl.compilationUnitOrEnclosingType` is an `Either<CompilationUnit,TypeInfo>`, read
   as `this.field.getLeft().method()`. The `nonModifyingLabels` pattern only reaches `this.field.m()`, not
   `this.field.unwrap().m()`, so that field stays modified-content and holds the type at FINAL_FIELDS.
   Generalising the excusal through immutable single-indirection wrappers (`Either`, `Option`) is the next
   lever for the level.
2. **Interfaces don't surface.** FINAL_FIELDS-after-mark does not beat their FINAL_FIELDS-*unconditional*,
   so `computeTypeLevel`'s "must beat unconditional" guard suppresses the write. They would appear once
   ceiling 1 lifts the level to IMMUTABLE-HC.

**Soundness status.** The result is stable (evidence of a self-consistent fixpoint at level 1), but the
greatest-fixpoint **removal pass is not yet implemented** — a member that concluded while relying on a
candidate that ultimately did *not* is not yet retracted. That, plus the wrapper generalisation and
promoting `InfoImpl` via subclass→parent inheritance, is the remaining work to make it default-worthy.
The prototype's contribution is the proof that the cluster is genuinely resolvable and that the marks are
the right ones; it lives behind `EVENTUALCLUSTER` until the removal pass and a corpus A/B clear it.

## Wrapper generalisation → the level lifts to IMMUTABLE-HC (2026-07-22)

Three additions (all under the gate, or A/B-verified neutral off it) took the flagship types from
FINAL_FIELDS to genuine immutable-HC:

1. **Chained reads through an immutable wrapper.** `nonModifyingLabels`/`receiverAfterLabels` now
   follow a chain of *non-modifying* accessors from an own field:
   `this.compilationUnitOrEnclosingType.getRight().descriptor()` resolves to the field
   `compilationUnitOrEnclosingType`. `fieldHoldsCommittableContent` excuses a field that is an immutable
   single-indirection wrapper (`Either`, `Option`) of candidate content. A `ContractReader` fallback
   (`immutableOf`) supplies `Either`'s `@ImmutableContainer` verdict, which as a jar type is not
   materialised into `analysis()` (the same trap `eventualOf` already handles). `Either.getRight()` is
   recognised non-modifying because it is declared on an immutable type.
2. **Immutable-typed fields are exempt from the `UNMODIFIED_FIELD` gate** (in `loopOverFieldsAndMethods`,
   gated). A `private final String` field (`simpleName`, `fullyQualifiedName`, `MethodInfoImpl.name`)
   was recorded `unmodified=false` in the baseline — spurious, since String content is unmodifiable —
   and it was the true cap holding every flagship type at FINAL_FIELDS. An immutable-typed field's
   content cannot be modified whatever the field analyzer recorded, so the check is skipped.
3. **A final field of eventually-immutable/candidate type joins the mark** (§060: "will at some point
   hold objects that are in their after state … act as immutable fields"). `FieldInfoImpl.type`
   (a `ParameterizedType`) rides along even though no accessor reads through it. Only fires when a mark
   was already found, so a type with no transition of its own is unaffected.

**Result:** `eventuallyImmutableType` 4 → 17, **8 at IMMUTABLE-HC** (was 4), *stable across reruns*, gate
OFF still exactly 4 (golden rule intact), suite 227/0. The three flagship types are certified
`@Immutable(hc=true)` after their marks:

- `TypeInfoImpl` after `compilationUnitOrEnclosingType, inspection`
- `MethodInfoImpl` after `inspection, typeInfo`
- `FieldInfoImpl` after `inspection, owner, type`

**Still FINAL_FIELDS:** `ParameterInfoImpl` (after `inspection, methodInfo, parameterizedType`) is capped
by its own `analysis` field — a mutable `PropertyValueMap`. Unlike the other three, `ParameterInfoImpl`
does **not** extend `InfoImpl`; it holds the analysis store directly and reads it *on the field*
(`analysis.getOrDefault(...)` in `isUnmodified`/`isIgnoreModifications`/`assignedToField`), whereas the
three that extend `InfoImpl` can only reach the store through the `analysis()` accessor.

The mechanism, confirmed: `PropertyValueMap.getOrDefault` has no `@NotModified` and its only implementation
(`PropertyValueMapImpl`) is a **jar** here, so its non-modification is never established (`nonModifying=null`).
The identical call is then non-modifying through the accessor (`InfoImpl.hasBeenAnalyzed` = `analysis().getOrDefault`
→ true) but conservatively modifying on the direct field (`ParameterInfoImpl.isUnmodified` = `analysis.getOrDefault`
→ false), because a possibly-mutating call on a *named own field* marks that field's content modified. So the
store reads `unmodified=false` only in `ParameterInfoImpl`.

Three fixes, in increasing scope:
- **Annotate `PropertyValueMap.getOrDefault`/`getOrNull` `@NotModified`** — root cause, tiny, correct, helps
  anywhere these are called on a field.
- **Make `ParameterInfoImpl extends InfoImpl`** like its three siblings — removes the directly-readable store.
- **Analyze `cst-analysis` as source** so `getOrDefault` is *computed* non-modifying (the "computed, not
  contracted" path). Attempted: adding `cst-analysis` as a third dogfood source subproject wires it into the
  input configuration, but the openjdk front end drops it — `package org.e2immu.language.cst.api.info does not
  exist`. Root cause is a plugin gap: `ComputeSourceSets.dependentProjectResult` builds each *transitive*
  dependency project (cst-analysis) as a flat leaf source set with `List.of()` inter-project dependencies, so
  the cst-analysis→cst-api source edge is never wired and cst-analysis cannot resolve cst-api. Fixing it means
  reconstructing the source-project dependency DAG from the resolution result — the general lesson (analyze
  dependencies as source for immutability) is now noted in `road-to-immutability/llm-summary.md`.

`CompilationUnitImpl`, `ParameterizedTypeImpl`, `ModuleInfoImpl`, `TypeParameterImpl` remain at FINAL_FIELDS
for similar per-type field reasons, each worth a look but none blocking the headline.
