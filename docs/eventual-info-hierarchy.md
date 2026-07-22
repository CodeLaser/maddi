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
  contracted" path) — **done, see below**.

`CompilationUnitImpl`, `ParameterizedTypeImpl`, `ModuleInfoImpl` remain at FINAL_FIELDS for similar per-type
field reasons, each worth a look but none blocking the headline.

## Plugin fix: transitive source-project edges, and cst-analysis as source (2026-07-22)

The "analyze cst-analysis as source" route was blocked by a plugin gap:
`ComputeSourceSets.dependentProjectResult` built each *transitive* dependency project as a flat leaf source
set with `List.of()` inter-project dependencies, so the cst-analysis→cst-api edge was never wired —
cst-analysis could not resolve cst-api (`package org.e2immu.language.cst.api.info does not exist`) and the
front end dropped it. Fixed: `ComputeSourceSets.collectSourceProjectEdges` reconstructs the dependency DAG
among source projects from the Gradle resolution result, and `ComputeDependencies` adds those edges to the
graph. The dogfood now analyzes `cst-analysis` as a third source subproject.

**Effect, confirmed:** `PropertyValueMapImpl.getOrDefault`/`getOrNull` are **computed** `@NotModified` from
source, the interface methods inherit it, and `ParameterInfoImpl.analysis` can now read `unmodified=true` —
exactly the "computed, not contracted" resolution.

Also fixed here: **after-mark independence is floored at the unconditional verdict** (`immutableAfterMark`,
gated). The mark only relaxes, so after-mark independence can never be below the unconditional — but
`independentAfterMark` under-reported when a plain accessor leaks a not-yet-proven cluster candidate
(`ParameterInfoImpl.parameterizedType`). Flooring lifted `TypeParameterImpl` to IMMUTABLE-HC.

**State with the gate:** `eventuallyImmutableType` 4 → 17, **9 at IMMUTABLE-HC** (`TypeInfoImpl`,
`MethodInfoImpl`, `FieldInfoImpl`, `TypeParameterImpl`, + the 4 base + `CompilationUnitStub`). Gate OFF still
exactly 4 (golden rule intact), analyzer suite 227/0, plugin 6/0.

**`ParameterInfoImpl` still does not reliably land**, for a subtler reason than before: its `analysis` field's
`unmodified` verdict is now *non-deterministic* — it flips true/false across runs (the documented
non-confluence), and because `PropertyValueMap` is `@FinalFields` (not immutable-HC) the field is checked, so
the type reaches HC only when the coin lands right. The clean settle remains the structural one — make
`ParameterInfoImpl extends InfoImpl` so it inherits the store instead of holding it directly — which also
stops it reading the store through a direct field reference in the first place.

## Getter ↔ variable equivalence, and the store is a red herring (2026-07-22, corrected)

`extends InfoImpl` is explicitly off the table until every other means is exhausted. Chasing the store
modification led to three corrections of the record above.

**(a) The `analysis` store is NOT the `ParameterInfoImpl` cap.** Across 8 dogfood runs (gate on/off, with and
without any change) `field unmodified=true …ParameterInfoImpl.analysis` is **stable** — the flip described
above does not reproduce. The plugin fix (cst-analysis as source) made `getOrDefault`'s `NON_MODIFYING`
verdict stably decided, which closes the provisional-`false` window that used to race. So there is no live
modification non-determinism on that field.

**(b) Getter ↔ variable equivalence — verified status.** The engine already models `x.f()` == `x.f` *exactly*,
not approximately: `ApplyGetSetTranslation` (prepwork, run on every expression in `MethodAnalyzer.beforeExpression`)
and its link-layer twin in `ExpressionVisitor` **rewrite** a recognised getter call into the field access
before variable data / links are built, so there is a single `FieldReference` afterward — no separate "getter
path" to diverge. Recognition is `GetSetHelper.doGetSetAnalysis`: the body's first statement must be a bare
`return this.field;` (or the array/list-get shapes). Dogfood: 694 getters + 132 setters of 6271 methods.
The **one real gap**: a leading side-effect-only statement defeats recognition. `InfoImpl.analysis()` and
`ParameterInfoImpl.analysis()` open with `if (ConsumptionEdgeRecorder.ENABLED) { record(this); }`, so their
first statement is an `IfStatement` → unrecognised → not rewritten; the accessor's receiver is a return-value
intermediate and the field never surfaces as a `FieldReference` in the reader. (`TypeParameterImpl.analysis()`,
`StatementImpl.analysis()`, … are bare returns → recognised.) This asymmetry is the latent fragility behind
the historical flip, but a probe (making `ParameterInfoImpl.analysis()` a bare, recognised getter) left the
verdict **byte-identical** `@FinalFields(after="inspection,methodInfo,parameterizedType")` — so it is *not*
the cap either.

**PROPOSED — `GetSetHelper` guard-tolerance (documented, not yet implemented).** Make getter/setter
recognition see through a *leading side-effect-only guard*: recognise `if (CONST) { …no field access… } return
this.field;` (and the setter analogue) as the getter it plainly is. This restores exact getter↔variable
equivalence for `analysis()` and any similarly-guarded accessor, routing them through the same unified
`FieldReference` machinery as the other 694 getters instead of the return-value-link fallback. It is an
engine/recognition change only — no CST edits, no contact with the modification-convergence invariant. It does
**not** lift `ParameterInfoImpl` (see (c)); it closes the genuine gap in the equivalence and removes the latent
non-confluence source. Care points: the guard block must be proven not to read or write the field (else the
rewrite is unsound); keep it behind the same recognition entry point so all consumers (prepwork + link) agree.

**Rejected — "wait-on-pending-callee" in `FieldAnalyzerImpl.computeUnmodified` (unsound, do not revisit as-is).**
The idea was: when a `FieldReference`'s `UNMODIFIED_VARIABLE` is `false` only because the called method's
`NON_MODIFYING_METHOD` is still undecided (`MethodModification.go` defaults an undecided callee to *modifying*),
return "undecided" so the field waits and settles monotonically instead of committing the provisional `false`.
It is unsound: an undecided field-modification is optimistically forced to **TRUE** by cycle-breaking
(`FieldAnalyzerImpl.go`, `cycleBreakingActive`), and `TolerantWrite` then *refuses* the later correcting `false`
as a weakening — so genuinely-modified collections (`ModuleInfoImpl.BuilderImpl.requiresList`,
`AndImpl.Builder.expressions`, `ElementImpl.Builder.annotations`, `ExpressionComparator.cache`, …) were wrongly
reported `unmodified=true`. The pessimistic "undecided callee = modifying" default exists precisely to prevent
this. Lesson: converting a provisional-`false` field verdict to "undecided" is equivalent to *optimistically*
assuming unmodified, which cycle-breaking then bakes in.

**(c) The real cap is the directly-owned `analysis` store — probe-confirmed, and it is NOT independence.**
`ParameterInfoImpl` is `@FinalFields(after="inspection,methodInfo,parameterizedType")` with
`INDEPENDENT_TYPE=@Independent` (independence floors fine; the after-mark independence loop exempts the three
mark fields, whose `afterMark.fields()=[parameterizedType, methodInfo, inspection]` — verified). The cap is in
`TypeImmutableAnalyzerImpl.loopOverFieldsAndMethods`: it returns `false` on the **`analysis`** field
(`PropertyValueMap`), read `unmodified=false` *at computation time* even though it settles `true` (FPDUMP
final). That commits FINAL_FIELDS, which then does not upgrade. The four flagship types never face this: their
store is **inherited from `InfoImpl`**, so it is not a declared field of the subtype and never enters
`typeInfo.fields()` in the loop — contrast `MethodInfoImpl.afterMark.fields()=[inspection, typeInfo]`, no store.
A probe that skips the `PropertyValueMap` store field in the loop lifts `ParameterInfoImpl` to
`eventual=@Immutable(hc=true)` **deterministically** (HC count 9→10, stable across reruns). So the store —
owned vs inherited — is the whole difference; `parameterizedType` (`ParameterizedType`, itself only
`eventual=@FinalFields`) is correctly exempted via the mark and is *not* the cap.

**PROPOSED — exempt the analysis-metadata store from the field-modification cap (gated).** The
`PropertyValueMap analysis()` store is the mechanism of eventual immutability itself: filled during analysis,
frozen after; every Info type has one, and for the four that extend `InfoImpl` it is provably never a
modification cap (it is inherited, never looped). A directly-owned store should get the same treatment — skip a
field of type `PropertyValueMap` in `loopOverFieldsAndMethods` (gated on `EVENTUALCLUSTER`), mirroring the
structural exemption the flagship get for free. Sound: the field's `UNMODIFIED_FIELD` settles `true` (its only
writers, external `set()` calls, are not in the analysed source); the `false` it is read as is a
read-ordering artifact, not a real modification. Care point: key it on the store precisely (type
`PropertyValueMap`), and keep it gated so the corpus A/B stays byte-identical off the gate.

### Diagnostic: FPDUMP extended
`FPDUMP` now also emits, per element: the after-mark `eventual=` verdict on each type
(`EVENTUALLY_IMMUTABLE_TYPE`), `getset=<field>(get|set)` on each recognised getter/setter method
(`GET_SET_FIELD`), and `independent=` / `ignoreMod=` on fields and types. All were essential to the corrections
above — the flagship types read `type immutable=@Mutable` unconditionally, their HC-ness lives only in
`eventual=`. (`maddi-modification-analyzer/.../IteratingAnalyzerImpl`.)

## Resolution: `@IgnoreModifications`-as-hidden-content (2026-07-22, DONE)

The "skip `PropertyValueMap` by type" proposal above was correctly diagnosed as a hack (it conflates two
regimes and would hide a genuine modification once the analyzer's own sources are in scope). The principled
replacement, worked out with Bart and written into `road-to-immutability` §050 ("Ignoring modifications as
manual hidden content"): **`@IgnoreModifications` *is* the manual form of hidden content** — a field whose
modifications the author disclaims, confined to the *ignored stratum*, so it does not bear on the type's
immutability. `@StaticSideEffects` is the same guard's global-escape arm.

Implemented in two parts:

1. **Annotations** — every `Info` store carries `@IgnoreModifications` with the "analysis overlay is orthogonal
   to CST-structure immutability" rationale at the site: `InfoImpl.propertyValueMap` (inherited by the four
   `InfoImpl`-extending types), `ParameterInfoImpl.analysis` (owned), `TypeParameterImpl.analysis` (owned
   override).
2. **Engine** — (a) `SourceContractMaterializer` now materializes `IGNORE_MODIFICATIONS_FIELD` on source fields
   (it is a pure contract, uncomputable, so a source annotation was previously read by nothing — the exact
   asymmetry that made the annotation fire on shallow-analysed `InfoImpl` but not on source `ParameterInfoImpl`);
   ungated, a no-op off maddi's own annotated code. (b) `TypeImmutableAnalyzerImpl.loopOverFieldsAndMethods`
   treats an `@IgnoreModifications` field as hidden content — its `UNMODIFIED_FIELD` verdict is irrelevant —
   gated on `EVENTUALCLUSTER`; the field still holds the type at `IMMUTABLE_HC` (concrete type not deeply
   immutable), never hc-free.

**Result:** `ParameterInfoImpl` reaches `eventual=@Immutable(hc=true)(after="inspection,methodInfo,
parameterizedType")` **deterministically** — the fourth core `Info` type, and the first **without**
`extends InfoImpl`. All five (`TypeInfoImpl`, `MethodInfoImpl`, `FieldInfoImpl`, `TypeParameterImpl`,
`ParameterInfoImpl`) are now eventual-HC on maddi's own code. Dogfood HC 9→10; gate-off unchanged (4 eventual);
analyzer suite 227/0. **Golden-rule corpus A/B: passed** — the three certified corpus tests
(`TestFernflower`/`TestTimefoldSolver`/`TestLangchain4j`) ran gate-off, forced (`--rerun-tasks`), with real
analysis time (147s / 179s / 36s) and 0 failures / 0 errors (Timefold's 1 skip is the pre-existing assumption).
The ungated materialization is a corpus no-op (no e2immu annotations there) and the field-loop skip is gated off.

**Follow-ons 1 & 2 (2026-07-22, DONE).**
- **Ungated the field-loop skip.** `if (fieldInfo.isIgnoreModifications()) continue;` moved out of the
  `EVENTUALCLUSTER` gate in `loopOverFieldsAndMethods` — honouring the contract is general correctness, and a
  no-op wherever no field carries the annotation. **Corpus A/B green** (Fernflower/Timefold/Langchain4j, forced
  rerun, real analysis time, 0 failures).
- **Confinement guard, separation arm.** `GuardAnalyzerImpl.guardIgnoreModificationsSeparation` **warns**
  (category `ignore-modifications-not-confined`, never caps) when an `@IgnoreModifications` field holds a
  non-decoration link to an accessible (non-ignore-mod) field of the same primary type — content shared with the
  accessible surface, so a modification through the ignored stratum could escape it. Conservative (a
  reference-only/decoration link stays silent, so the analysis overlay's normal use is not flagged);
  method-granularity / global-escape (`@StaticSideEffects`) is the deferred later arm.
  `TestGuardIgnoreModifications`: the overlay shape is silent, a StringBuilder-content share is flagged once.
  Analyzer suite 229/0.

**Confinement guard, global-escape arm — DONE (2026-07-22).** Its mechanical core, `@StaticSideEffects`, did
not exist in the engine (only in road §050) — now implemented as a computed `STATIC_SIDE_EFFECTS_METHOD`
(`StaticSideEffectAnalyzerImpl`): a method has a static side effect when it modifies static/global state of a
type *other* than its own primary type (first cut: assignment to, or a modifying call on, another type's static
field; static-method reconfiguration like `System.setOut` needs an AAPI safe-surface declaration, left for
later). Gated on env `SSE`, additive (writes only its own property). `GuardAnalyzerImpl`.
`guardIgnoreModificationsContainment` then warns when a modifying call on an `@IgnoreModifications` field has a
callee that is `@StaticSideEffects` — the modification reaches global state, so it left the ignored stratum.
`TestStaticSideEffects` + `TestGuardIgnoreModifications.testGlobalEscapeIsWarned`; analyzer suite 231/0; corpus
A/B (SSE off byte-identical / SSE on no-crash) green.

**Still open:** the AAPI safe-surface declarations for static-method reconfiguration (`System.setOut`,
`logger.addAppender`) — the part of the global-escape arm that needs callee annotations; a `DecoratorImpl`
emission of `@StaticSideEffects` so it surfaces in the IDE (Task 4 adjacent); `GetSetHelper` guard-tolerance
(getter↔variable equivalence gap); and the greatest-fixpoint **removal pass** still owed for the cluster
optimism.

## Task 4: surface the eventual verdicts to developers (the IDE path)

The eventual verdicts are the novel output of this arc; today they are visible only via `FPDUMP` and the
results JSON. The goal is to show them *in the editor* ("this type becomes `@Immutable` once `inspection` is
committed"). Reconnaissance (2026-07-22) corrected the framing: **a full IDE stack already ships** — plugins for
IntelliJ (`maddi-intellij`: inlay/gutter/annotator/findings-panel), Eclipse (`maddi-eclipse`: code minings),
and VS Code (`maddi-vscode`: inlay hints + hover + diagnostics), all over a bespoke NDJSON daemon protocol
(`maddi-ide-daemon` / `maddi-ide-client`, `DaemonProtocol`), **not** LSP. So this is not "build IDE
integration"; it is "make the eventual verdicts flow through the surfaces that exist." They flow only
**partially** today: the daemon ships each element's raw `properties` map (so `eventualMethod=…`/
`eventuallyImmutableType=…` are already weakly visible in the **VS Code hover**), but the *rendered*
annotations/inlays on every front-end come from `DecoratorImpl`, which emits **no eventual annotation at all**.

**The single high-leverage seam — `DecoratorImpl.annotationAndProperties()`**
(`maddi-modification-prepwork/.../io/DecoratorImpl.java`, ~128–383). It turns computed `analysis()` properties
back into `@Annotation` decorations and feeds `AnnotationTagger` → `ResultCollector` → **all three front-ends**
*and* decorated-source printing. It covers `@Immutable`/`@Independent`/`@NotModified`/`@Final`/… but no
`EVENTUALLY_*`; for an eventually-immutable type it reads the optimistic unconditional `IMMUTABLE_TYPE` and
prints a plain `@Immutable(hc=true)` with **no `after=`**, losing the eventual nature. Extend it to emit
`EVENTUALLY_IMMUTABLE_TYPE` → `@Immutable(after="…")`, `EVENTUAL_METHOD`/`EVENTUAL_PARAMETER` →
`@Mark`/`@Only`/`@TestMark`, `EVENTUALLY_FINAL_FIELD` → `@Final(after="…")`, `EVENTUALLY_NON_MODIFYING_METHOD` →
`@NotModified(after="…")`. The exact inverse already exists — `AnnotationToProperty` (`maddi-modification-common/.../AnnotationToProperty.java`, ~134–335) parses these same annotations *into* the properties — so mirror its label/field
semantics. Tests to extend: `TestWriteAnalysis2`, `TestAnalysisHintsComposer`.

**Staging** (each step independently shippable, all downstream of the seam):
1. **Decorate — DONE (2026-07-22).** `DecoratorImpl.annotationAndProperties()` now emits
   `EVENTUALLY_IMMUTABLE_TYPE` → `@Immutable(hc?, after="…")`/`@FinalFields(after="…")`, `EVENTUAL_METHOD`/
   `EVENTUAL_PARAMETER` → `@Mark`/`@Only`/`@TestMark`, `EVENTUALLY_NON_MODIFYING_METHOD` → `@NotModified(after="…")`,
   `EVENTUALLY_FINAL_FIELD` → `@Final(after="…")`, mirroring `AnnotationToProperty`. `AnnotationTagger` tags
   `@Immutable`/`@NotModified`/`@Final(after=)` POSITIVE (rendered inlays, `after=` visible) and the
   `@Mark`/`@Only`/`@TestMark`/`@FinalFields` family NEUTRAL (carried, not dropped), so all three front-ends now
   surface them. `TestDecorateEventual`; ungated & additive; prepwork 206/0, link 402/0 (decoration unchanged).
2. **Polarity — DONE (2026-07-22).** `AnnotationTagger` tags `@Mark`/`@Only`/`@TestMark`/`@FinalFields` and the
   `after=` forms of `@Immutable`/`@NotModified`/`@Final` with a new **`EVENTUAL`** polarity (detected
   structurally from the `AnnotationExpression`, not by text-matching), distinct from the plain `POSITIVE` of a
   proven-now verdict; and an eventual verdict is never a context default, so the default filter always shows it.
   Verified safe on all three front-ends (each filters "show unless polarity == one excluded literal"; polarity
   is a free-form String, so an unrecognised value renders everywhere but the explicit NONE mode — it can only
   make eventual verdicts *more* visible). `TestEventualPolarity` (end-to-end: a `SetOnce` holder's
   `@Immutable(after="value")`/`@Mark`/`@Only` come back `EVENTUAL`, unconditional `@Container`/`@NotModified`
   stay `POSITIVE`); daemon 11/0. Front-end styling/filtering *on* `EVENTUAL` is a later refinement.
3. **Typed protocol field** (optional) — `DaemonProtocol.ElementAnnotation` carries `displayAnnotations`,
   `annotations`, and a stringly-typed `properties` map; a typed eventual field would let front-ends style the
   `after="…"` labels rather than parse strings.
4. **Round-trip is already done** — `WriteAnalysisResults` + `PropertyProviderImpl` + `ValueImpl` codecs
   serialize/deserialize every eventual property, so a file-consuming tool needs no new work.

No LSP is involved; the transport is the daemon's NDJSON. See `docs/ide-todo.md` for the separately-tracked IDE
work (partial re-analysis, streaming).
