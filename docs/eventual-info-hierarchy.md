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

**`GetSetHelper` guard-tolerance — DONE (2026-07-22).** Getter/setter recognition now sees through a *leading
inert-guard* prefix: `doGetSetAnalysis` runs the return/assignment recognition on the first **non-inert**
statement, not literally the first, so `if (ConsumptionEdgeRecorder.ENABLED) { record(this); } return
propertyValueMap;` (the `InfoImpl.analysis()` / `ParameterInfoImpl.analysis()` shape) is recognised as the getter
it plainly is, routing `x.analysis()` through the same unified `FieldReference` machinery (`ExpressionVisitor`
call-site rewrite) as the other 694 getters. Engine/recognition only — no CST edits, no contact with the
modification-convergence invariant; the single entry point keeps prepwork + link in agreement (both read
`GET_SET_FIELD`).

*Tolerance (see `GetSetHelper`'s class comment for the full soundness argument).* The check is purely syntactic
(it sits in `PrepAnalyzer`, below any computed verdict, and its output is trusted stack-wide, incl.
`ExpressionVisitor`, which *replaces* `x.m()` with a read of `x.field`, bypassing the body). A leading statement
is an inert guard iff, across its whole subtree, it: (1) falls through — no return/yield/throw/break/continue;
(2) writes no field of `this`; (3) references no field of `this` at all; (4) makes no call *on* `this`. A call
that merely *passes* `this` to a static method (`record(this)`) is allowed — its only possible effect is on state
the object does not own, a disclaimed static side effect (road §050) that cannot cap the object's immutability,
so dropping it at the call site corrupts no verdict. Deliberately **not** tolerated: a guard that reads/writes an
own field, calls a method on `this`, or can exit early (a second behaviour, not a getter with a benign prelude).
`TestGetSetGuardTolerance` (positive: guarded getter/setter/fluent; negative: the four boundary violations).
Suites: common 52/0, prepwork 209/0, link 402/0, analyzer 233/0. Consistent with (b)/(c): it closes the
equivalence gap and removes the latent non-confluence source but does **not** by itself lift `ParameterInfoImpl`
(the owned-store cap, already handled by the ignore-mod route).

*Golden-rule corpus A/B (Fernflower FPDUMP, baseline vs change).* Not byte-identical, but **no verdict moved**:
with the `getset=` classification tokens stripped the diff is **0 lines**. The single delta is one genuinely
guarded setter that baseline missed — `ConstExprent.setConstType(VarType)`:
`if (constType == null) { constType = VARTYPE_UNKNOWN; } this.constType = constType;` — now correctly classified
`getset=constType(set)` (the guard reassigns the *parameter*, a local, which rule 2 permits; then the strict
setter shape matches). Its `nonModifying=false` verdict is identical in both runs. So the A/B caught exactly the
intended broadened classification, and it is correct and non-regressive — the feature working as designed, not a
verdict regression.

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

**AAPI safe-surface — DONE (2026-07-22).** The callee-annotation half of the global-escape arm: a static
*method* call that reconfigures global state (`System.setOut(other)` replacing the process-wide `System.out`)
is invisible from JDK source, so it is recorded as a **contract** on the library's safe surface. Five pieces:
1. **`@StaticSideEffects` annotation** (`maddi-support/.../annotation/rare/StaticSideEffects.java`) — contracted
   on a library surface, `@Target(METHOD, CONSTRUCTOR)`, like `@IgnoreModifications`.
2. **`AnnotationToProperty`** parses it → `STATIC_SIDE_EFFECTS_METHOD` (method map), so the shallow/AAPI path
   materialises it automatically.
3. **`SourceContractMaterializer.materialize(MethodInfo)`** materialises it on SOURCE methods too (parallel to
   `IGNORE_MODIFICATIONS_FIELD` on fields) — a source author may assert it; ungated, a no-op where absent.
4. **`StaticSideEffectAnalyzerImpl`** propagation: a call to a `@StaticSideEffects` callee makes the caller a
   static-side-effect method too, transitively. `calleeStaticSideEffect` resolves the callee's verdict with a
   has-body discriminator — a source callee not yet decided is UNDECIDED (wait, like the modifying-call case on
   `NON_MODIFYING_METHOD`); a shallow/abstract callee with no contract is a decided FALSE (never stalls). This
   is what makes the contract *bite*: it is how `System.setOut` reaches the guard.
5. **AAPI declarations** — `System$.setOut/setErr/setIn` in `maddi-aapi-archive/.../jdk/JavaLang.java` annotated
   `@StaticSideEffects`; the archive JSON regenerated via `./gradlew :maddi-aapi-parser:compileAnalysisHints`
   (surgical 3-line diff in `JavaLang.json` — each setter gains `"staticSideEffectsMethod":1` — plus the
   repackaged `openjdk.jar`; no other JSON changed).

The `guardIgnoreModificationsContainment` arm needed no change — it already reads `STATIC_SIDE_EFFECTS_METHOD`
on the direct callee, so propagation composes: `sink.reconfigure()` on an `@IgnoreModifications` field, where
`reconfigure()` calls the contracted leaf, is flagged. Tests: `TestStaticSideEffects.testPropagation` (direct +
transitive), `TestGuardIgnoreModifications.testGlobalEscapeViaContractIsWarned` (contract → propagate → guard).
Analyzer suite 233/0, modification-common 52/0. Road §050 gained a "Recognising an invisible escape: the
safe-surface contract" subsection. **Golden-rule corpus A/B: Fernflower FPDUMP byte-identical** off the SSE gate
(the JSON delta is 3 JDK methods gaining a property no gate-off run reads).

**`@StaticSideEffects` in the IDE — DONE (2026-07-22).** `DecoratorImpl.annotationAndProperties()` now emits
`@StaticSideEffects` on a method whose `STATIC_SIDE_EFFECTS_METHOD` is true (mirroring the `@IgnoreModifications`
emission), and `AnnotationTagger` tags it the `NEGATIVE` attention polarity — not a missing safety guarantee, but
a genuine outward effect the designer should always see, rendered like the baseline cautions. Feeds
DecoratorImpl → AnnotationTagger → all three front-ends + decorated-source printing. Not in the FPDUMP path and a
no-op with the SSE gate off (no source method carries the property), so no corpus A/B needed.
`TestDecorateStaticSideEffects` (prepwork, decorator seam), `TestStaticSideEffectPolarity` (daemon, end-to-end:
gate on → decorate → tag NEGATIVE). prepwork 207/0, daemon 12/0.

## Greatest-fixpoint removal pass (the remaining engine investment)

The `EventualCluster` prototype supplies only the **optimistic seed** of the greatest fixpoint: it assumes each
cluster candidate is eventually immutable and lets the two analyzers use that before the verdict is proven
(`TypeImmutableAnalyzerImpl.immutableSuper` for a supertype contribution, `TypeEventualAnalyzerImpl.fieldHolds
CommittableContent` for a cross-reference field type). It never does the other half — **contract**: remove any
member whose verdict does not hold once its dependencies are restricted to the survivors, iterating to
convergence. Today the result "happens to be" self-consistent (stable reruns; every member genuinely checks out),
but that is proof-by-observation, not proof-by-construction. The removal pass makes it sound for arbitrary code
and is the prerequisite to taking the whole cluster result (4→17 eventual, the five core `Info` types at
eventual-HC) off the `EVENTUALCLUSTER` gate.

Two architectural obstacles make this its own funded step, not an increment: (a) `EVENTUALLY_IMMUTABLE_TYPE` is
**write-once** — set once in `computeTypeLevel` and NOT in `IteratingAnalyzerImpl.clearDerivedFamily`, so the
outer loop never clears/recomputes it; retraction needs either adding it to the clearable family or a distinct
post-convergence phase with its own clear. (b) analysis() writes are **monotone (strengthen-only)**; a retraction
is a weakening the `TolerantWrite` guard refuses (the trap that killed "wait-on-pending-callee"), so the removal
must run outside the monotone discipline as a controlled clear-and-recompute. It also overlaps the engine's
existing immutability cycle-breaking and should extend it rather than run a parallel fixpoint — the most-guarded
region, gated behind a byte-identical corpus A/B.

**Step 1 — witness the optimism — IMPLEMENTED (2026-07-22, build/test pending gradle go-ahead).** For the
contraction to have something to run on, every optimistic decision is now recorded. `EventualCluster.
treatAsEventuallyImmutable(member, candidate, actual)` (signature extended with `member`) records the edge
`member → candidate` in a new `assumptions()` ledger whenever it answers `true` only because of the seed
(candidate not yet proven); both call sites thread the member (the subtype for `immutableSuper`, the field owner
for the field-type check). Recording is a pure side effect read by nobody yet, so it changes no verdict — additive
and gated (`ENABLED` made non-final, mirroring `StaticSideEffectAnalyzerImpl`, so tests can flip it).
`TestEventualClusterAssumptions` pins: an optimistic call records the edge; a proven verdict and the gate-off case
record nothing. *No corpus A/B needed (no verdict path touched).*

**Step 2 — the contraction — IMPLEMENTED (2026-07-22, gate-ON dogfood validation pending gradle go-ahead).**
`EventualClusterContraction` runs once at the terminal certification point (in `IteratingAnalyzerImpl`, before the
verdict fingerprint and guard): it computes the largest subset of the eventual-verdict holders closed under "every
candidate I assumed is retained" (`membersToRetract`, pure/generic so it is unit-testable), then **retracts**
`EVENTUALLY_IMMUTABLE_TYPE` on the members that did not survive — dropping any that leaned on a candidate which did
not itself prove eventually immutable, cascading to a fixpoint. Retraction is a `removeIf` on the property: it
runs *outside* the monotone loop (a weakening `TolerantWrite` would refuse) as a post-convergence phase, which is
sound precisely because the seed only ever influenced `EVENTUALLY_IMMUTABLE_TYPE` (the optimistic contribution
fires solely in the after-mark branch of `immutableSuper` and in `fieldHoldsCommittableContent`), so clearing that
one property is the whole retraction — no derived-family recompute. Conservative: the ledger is a superset of the
final structural dependencies, so the pass never keeps an unsound verdict, though it could drop a justifiable one;
on a self-consistent cluster it retracts nothing. Double-gated on `EVENTUALCLUSTER` (call site + early return) →
off the gate the ledger is empty and it is a complete no-op, so the gate-off corpus A/B is byte-identical **by
construction**. `TestEventualClusterContraction` (self-consistent cycle survives whole; broken assumption drops;
cascade; independent verdict kept; mixed core-kept/sibling-dropped). analyzer 240/0.

**Gate-ON dogfood — the contraction is NOT a no-op: it retracts 12 (2026-07-22).** Run with `EVENTUALCLUSTER=1`
on maddi's own CST, the contraction retracted **12** of the 17 optimistic eventual verdicts — the *entire* `Info`
flagship family (`TypeInfoImpl`, `MethodInfoImpl`, `FieldInfoImpl`, `ParameterInfoImpl`, `TypeParameterImpl`,
`CompilationUnitImpl`, …). Only 5 self-contained verdicts survive (`ModuleInfo.Provides`/`Uses`, `Variable`,
`ModuleInfoImpl.ProvidesImpl`/`UsesImpl`). This is the contraction *working correctly*, not a bug: `InfoImpl` and
the `*Info` interfaces are all `eventual=null` — they never obtain a verdict (no `@Mark` of their own; the mark
lives on the subclasses' `inspection` field, and the **subclass→superclass mark inheritance is the deferred
piece**). Every flagship leans on `InfoImpl` (via `immutableSuper`) or an interface (via a cross-reference field)
being eventual, and the greatest-fixpoint contraction soundly refuses to certify a verdict whose premise is never
discharged. **So the seeded "4→17" was resting on undischarged premises** — the earlier "stable across reruns"
observation was self-*consistency* of the optimism, not soundness; step 2 is exactly the tool that exposed it.

**Roadmap, reordered.** The subclass→superclass mark inheritance is no longer optional/deferred — it is the
**critical-path prerequisite**. Once `InfoImpl` (inheriting the shared `inspection` transition from its
subclasses) and the interfaces obtain their own eventual verdicts, the flagships' assumptions discharge, the
contraction retracts nothing, and the 17 survive *soundly* — which is what earns ungating.

**Still open (in order):** (1) **interface eventual verdict (Part B)** — the diagnostic (FPDUMP now emits
`eventuallyNonMod`) pinned the blocker: the interfaces' cross-reference read-through accessors (`isFactoryMethod`,
`primaryType`, `descriptor`, the hierarchy streams) bail in `computeEventuallyNonModifying` because
`receiverAfterLabels` only follows *genuinely* non-modifying chains, not the *eventually*-non-modifying
`this`-accessor chains (`returnType()`, `enclosingMethod()`) the real accessors use. Fix = reframe
`nonModifyingLabels`/`receiverAfterLabels` into a unified `commitLabels(owner, expr)` (commit every `this`-derived
receiver **and** arg, not just root the receiver in a committed field). **Fully specified for handoff in
`docs/handoff-eventual-interface-nonmodification.md`.** (2) **subclass→superclass mark inheritance (Part A)** —
give `InfoImpl` its own eventual verdict from the subclasses' shared `inspection` mark (also in the handoff, §9).
(3) re-run the dogfood → contraction retracts 0. (4) **Step 3 — ungate** behind a byte-identical corpus A/B.

## Part B in progress: the `commitLabels` reframe (2026-07-22, evening session)

The handoff's §5 reframe is **implemented** (all of it gated on `EVENTUALCLUSTER`; the gate-off visitor path is
the old code verbatim): `computeEventuallyNonModifying` now excuses a call iff every `this`-derived value it
touches — receiver *and* arguments — is committed by the collected marks (`commitExcusedLabels` /
`commitLabels` in `TypeEventualAnalyzerImpl`). Beyond the handoff spec, the session added, each for a reason the
dogfood forced:

- **Local tracking** (`buildLocalCommitMap`): the spec's "a local is not `this`-derived → ∅" is the §6 aliasing
  trap one hop removed (`var l = this.items; l.add(x)`); locals ever assigned a `this`-derived value carry its
  commit labels (flow-insensitive fixpoint, null = poisoned). Fresh `new ArrayList<>()` builder locals stay ∅.
- **`handedOnValueSafe`** replaces the blanket return-type gate on intermediate chain calls: decided by the
  callee's **independence**. `@Independent` shares nothing mutable → safe; `@Dependent` returns accessible
  content → safe iff the receiver is committed (NOT off bare `this` — the `getItems()` trap); `@Independent
  (hc=true)` (Collection.stream, Either.getRight) → the wrapper layer is fresh by contract, safe iff the
  concrete return type's parameters are committable-or-immutable-hc (`Stream<MethodModifier>`); anything
  undecided falls back to `returnTypeHoldsCommittableContent` (the type itself committable — EFOD.get()
  returning a candidate `MethodInspection`).
- **Look-through of same-class single-`return` forwards** (depth-capped): the method boundary erases *which*
  committed field a result was read through — `descriptor()` calling `this.parameters()` — so `commitLabels`
  inlines a `return <expr>;` body one level and evaluates it in place.
- **Abstract label union** (`AbstractMethodAnalyzerImpl.methodEventuallyNonModifying`, gated): implementations
  legitimately name different transitions for the same abstract accessor (`[inspection]` vs `[methodInfo]` for
  `Info.fullyQualifiedName`); unlike a `@Mark`, "non-modifying after L" weakens monotonically, so the union is
  the sound meet.
- **`EventualCluster` negative-cache bug fixed**: `isDirectCandidate` cached a *negative* verdict across
  iterations, freezing `TypeInspectionImpl.Builder` out of the cluster when queried before its `@TestMark` was
  computed. Eventual intent appears monotonically; only the positive answer may be cached.
- **Contraction repairs** (both required *because of* Part B): the handoff's premise "the seed only ever
  influenced `EVENTUALLY_IMMUTABLE_TYPE`" no longer holds — `commitLabels` leans on the seed for **method
  labels** too. (a) *Discharge*: a candidate that ends up **unconditionally** ≥ immutable-hc (`MethodInspection`)
  discharges assumptions on it a fortiori; previously its assumers were wrongly retracted. (b) *Label
  provenance*: when an abstract method inherits enm labels from an implementation, the abstract owner inherits
  the implementation owner's assumption edges (`EventualCluster.noteLabelInheritance` →
  `effectiveAssumptions()`), so a broken assumption under an impl's labels cascades to the interface that
  inherited them.

**Dogfood scoreboard (gate ON), method level — the reframe works.** Both §5.4 worked traces land exactly as
predicted: `isFactoryMethod → eventuallyNonMod=[inspection, typeInfo]`, `TypeInfoImpl.primaryType →
[compilationUnitOrEnclosingType, inspection]`. Holdout counts (nonModifying=false, no label, not eventual/getset):
`ParameterInfo` **19→0**, `Info` 3→2, `MethodInfo` 7→5, `FieldInfo` 3→3, `TypeInfo` ~19→12.
`Info.fullyQualifiedName`, `isFinal`, `isSynchronized`, `isSAMOfStandardFunctionalInterface`, `isEnclosedIn`,
`parameters`, and the whole modifier-Set/Stream chain class are excused.

**Type level — the interfaces do NOT yet surface; retraction is now honest and large (34).** With enm labels
leaning on the seed, the all-or-nothing fixpoint retracts nearly everything until *every* holdout clears — the
optics got worse (survivors 5→2) precisely because the ledger got sound. The remaining blockers, precisely:

1. **Fresh-object rewiring methods** — `withOwner`, `withOwnerVariableBuilder`, `withMethodType`,
   `withSynthetic`, `translate` (blocks `Info`, `MethodInfo`, `FieldInfo`). They construct a fresh `*InfoImpl`
   and call `setVariable`/`setFinal`/builder-commit **on the fresh object's own field** — excusable only with
   freshness tracking: a `ConstructorCall` case in `commitLabels` (fresh, labels = union of args), field reads
   scoped on fresh locals, and a transition-guard exception for marks on provably-fresh receivers. Sketched, not
   implemented.
2. **Streams over the hierarchy with `this`-capturing lambdas** — `constructorAndMethodStream`,
   `recursiveSuperTypeStream`, … (blocks `TypeInfo`). Needs a `Lambda` case in `commitLabels` (the outer visitor
   already excuses calls *inside* lambda bodies; the gap is the lambda **as argument value**).
3. **The `@TestMark` staircase** — `TypeInspectionImpl.Builder.hasBeenCommitted()` forwards
   `typeInfo.hasBeenInspected()` through a **cluster-interface-typed field**; `computeEventual`'s
   `labelsOfReceiver` uses the *strict* eventual check, so the Builder's `@TestMark` (and with it the
   candidacy of `TypeInspection`, and with *that* the excusal of `isSealed`/`isStatic`/…) only materialises in
   whatever late iteration the seeded interface verdict happens to exist — order-dependent, converges partially.
   Either `labelsOfReceiver` learns the seed (+ witnessing, + provenance for `EVENTUAL_METHOD`), or the
   `TypeInspection` candidacy needs a non-circular source.
4. **Part A** (`InfoImpl` subclass→superclass mark inheritance, handoff §9.3) — unchanged, still required.

Unit tests: `TestCommitLabels` (cross-reference receiver/arg shapes, Either + local chains, all bail shapes,
gate-off pins). Full `:maddi-modification-analyzer:test` green. Gate-OFF Fernflower corpus A/B: **0-line diff**
against a base run (the one line that differed against the *first* base run — `StatEdge.EdgeType.<init>`
nonModifying — flips identically between two base runs, i.e. pre-existing run-to-run nondeterminism, not the
change).

## Part B, second wave: METHOD LEVEL COMPLETE — 0 holdouts on all five interfaces (2026-07-22, late)

The wave that followed took every remaining holdout class down; **all five `*Info` interfaces now have zero
unexcused modifying methods**. What it took, in landing order (each verified by a gate-ON dogfood iteration,
all gated):

- **The `@TestMark` staircase, resolved**: `labelsOfReceiver` now uses `isEventuallyImmutableFieldType`
  (seed + witness; off the gate this is the identical strict check), so
  `TypeInspectionImpl.Builder.hasBeenCommitted()` — a `@TestMark` forward through a candidate-typed field —
  classifies deterministically instead of waiting on the interface verdict it itself feeds.
  `methodEventual` records label provenance like its enm twin.
- **Freshness**: `LocalContext` tracks locals whose every assignment is a plain constructor call (through
  ternaries and casts). `ConstructorCall` = fresh with the union of its args' labels; a field read scoped on
  another object commits after the scope's labels; a `@Mark`/`@Only(before)` call whose receiver chain roots
  in a fresh local is that object's lifecycle, not this's transition (`withOwnerVariableBuilder`'s
  `fi.inspection.setVariable(...)`); a chain rooted in fresh skips the handed-on-value check (the fluent
  `newField.builder().setX(..).setY(..)`).
- **Lambdas and method references as argument values**: a lambda's body is walked with the full
  `commitLabels` discipline (calls inside are additionally excused by the enclosing visitor as always); a
  bound method reference mirrors the intermediate-call rules on its scope and declared return type.
- **The owner-candidacy rule** (the deepest cut): when the owner itself is a cluster candidate — witnessed
  as a self-assumption — a this-accessor's result, and even bare `this` handed out (`Stream.of(this)` in
  `innerClassEnclosingStream`), is excusable: accessible content of `this` is committed once its own marks
  pass, the trap shape (accessor handing out an unmarked mutable field) sinks the owner's own type verdict,
  and the contraction cascades that retraction. This is the coinductive step that unlocked
  `interfacesImplemented()`-style chains and with them every hierarchy stream.
- **Value-type reasoning**: `handedOnValueSafe` accepts any call on a COMMITTED receiver whose return type's
  parameters are committable (covers direct recursion — `parent.recursiveSuperTypeStream()` — where the
  callee's independence is inherently undecided); `returnTypeHoldsCommittableContent` accepts parameterless
  immutable-hc types (`MethodInspectionImpl`, `MethodType`) and rejects arrays; `commitLabels` short-circuits
  ∅ for any expression whose TYPE cannot carry mutable state (an int arithmetic constructor argument, a
  String concat) — the producing calls are excused independently by the visitor; ternary/cast/parenthesis
  unwrapping; `@IgnoreModifications` reads are disclaimed (road §050).

`translate`, `withMethodBody`, `withSynthetic`, `topOfOverloadingHierarchy`, the `with*` builders, the
hierarchy streams: all excused. Also implemented: **Part A** (subclass→parent mark inheritance for abstract
classes, `EventualCluster.noteHierarchy`/`knownSubclasses` + the shared-label intersection in
`computeTypeLevel`, seeded + witnessed).

**Where the wall is now (type level).** Retraction 37; the interfaces' (seeded) verdicts still retract
because the assumption closure reaches a NEXT RING of candidates that neither prove eventually immutable nor
discharge unconditionally: `ParameterizedTypeImpl` (@Mutable — a **markless carrier**: no transition of its
own, all-final fields of candidate types; needs the §060 field-ride-along decoupled from an own mark, plus
dynamic-immutability-aware field committability for its `List.copyOf`-style fields), `FieldInspectionImpl`
(@Mutable, unlike its three sibling inspections — undiagnosed), `CompilationUnitImpl`, `TypeParameterImpl`.
The all-or-nothing fixpoint holds until that ring closes.

**Ring 2, mapped precisely (2026-07-22, closing).** Landed on top of the above: `FieldInspectionImpl
.analysisOfInitializer` gains the `@IgnoreModifications` every Info analysis store carries (it was the one
without it — the source of its @Mutable); the §060 field-ride-along fires without a prior own mark under the
gate (**markless carriers**: `ParameterizedTypeImpl`); assumed candidates outside the analysis order
(`java.lang.Record`, pulled in as every record's supertype) discharge through their preloaded unconditional
verdict; and the contraction gained an env-gated diagnostic (`EC_RETRACT_DEBUG=1`) that prints, per retracted
member, exactly which assumed candidates broke — use it first in any future session.

The diagnostic shows the closure now spans the **entire CST**, and the remaining broken roots are:

1. **The `Expression` hierarchy** (`api.expression.Expression` + every `*Impl` + `ExpressionImpl`): every
   carrier assumed it; certifying it means the whole expression tree proves out — including the printer
   methods that today CRASH with the known exit-5 `ANALYSER_ERROR` (cycle protection), leaving their
   `NON_MODIFYING` undecided forever. Fixing those crashes is a hard prerequisite.
2. **The API `Builder` interfaces** (`FieldInfo.Builder`, `MethodInfo.Builder`, …): they enter the cluster
   through the upward closure (the `*InspectionImpl.Builder`s implement them and have `@TestMark` intent),
   so `commitLabels`' RTHCC treats them as committable candidates and records assumptions — but a builder
   interface full of plain `@Modified` setters can never prove eventually immutable. Candidacy (or at least
   RTHCC-committability) needs to exclude builder-natured types, or the builders need their own eventual
   story (their `commit()` IS a transition).
3. `api.type.ParameterizedType` (interface): blocked by its default printing methods (see 1).
4. `api.element.CompilationUnit`, `api.element.ModuleInfo`, `api.variable.*` — same pattern, smaller.

So retraction-0 is equivalent to certifying essentially all of cst-api/cst-impl — the full "culmination"
scope. The method-level machinery (this session) appears sufficient; the remaining work is (a) the printer
crashes, (b) the builder-candidacy modeling decision, (c) grinding the expression/statement/variable
hierarchies through the same dogfood loop with `EC_RETRACT_DEBUG` as the compass.

## Ring 3: success-only witnessing; the first cross-reference type SURVIVES (2026-07-22, closing)

Two more pieces, both gated:

- **Success-only witnessing.** The assumption ledger recorded edges from *every* optimistic query — including
  computations that bailed in iteration k and succeeded in iteration k+n via a different path (fresh-rooted,
  look-through), leaving vestigial edges the contraction then cascaded on. Every computation in
  `TypeEventualAnalyzerImpl.go` now runs inside a per-thread **assumption buffer**
  (`EventualCluster.beginAssumptionBuffer`/`commit`/`discard`): edges reach the ledger only when the
  computation lands its property. This removed the `java.lang.Record` and most Builder-interface edges from
  the broken lists without any modeling decision.
- **Throwing-stub compatibility in `methodEventual`** (mirrors the enm clause): an implementation that never
  modifies at all — `CompilationUnitStub`'s throwing `setFingerPrint` — cannot contradict the transition the
  real implementations declare, so it no longer vetoes the abstract method's `@Mark`.

**Three-corpus gate-OFF A/B (2026-07-22, closing, at `54bd9859` vs base `523963fe`):** Fernflower
**0-line diff**; Langchain4j **0-line diff**; Timefold 8 lines, every one of which flips identically
between two BASE runs (`TestdataInvalidConstraintWeightOverridesSolution`,
`TestdataFactorySortableSolution` independence, `ListIterableSelector`) — pre-existing run-to-run
nondeterminism, verified with the A-vs-A2 technique. The whole session's engine surface is gate-off inert
on the full certified proving ground.

**Gate-ON stability (2026-07-22, two consecutive dogfoods at `54bd9859`):** the surviving core of **8** is
identical across runs; ONE type flips in/out — `CompilationUnitPrinterImpl`, a printer-family type, i.e.
exactly the verification-residue boundary (`docs/handoff-verification-residue.md`) — and the
`eventuallyNonMod` method count wobbles (414 vs 402) for the same reason. Full stability is an ungate
criterion and is expected to come with the residue fix, not before.

**Result: `CompilationUnit` is the first cross-reference type to SURVIVE the contraction** —
`eventual=@Immutable(hc=true)(after="fingerPrint,types")`, retained, not seeded-and-retracted. Survivors
5→8. The remaining broken roots, per `EC_RETRACT_DEBUG`: `api.type.ParameterizedType` (18 dependents —
holdouts `print`/`rewire`/`concreteSuperType`/`mostSpecific`/`replaceByTypeBounds`, i.e. the printing and
rewiring machinery), `api.expression.Expression` + `ExpressionImpl` (17+12 — same machinery, plus the
71-method `nonModifying=null` verification residue), `AnnotationExpression`, and the small tail
(`VariableImpl`, `FieldInspection` cascades). The grind continues exactly there.

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

## The residue quest, characterized: recursion pessimism is the fulcrum (2026-07-22, night)

The verification-residue handoff was executed as a characterization pass; the full record now lives in
`docs/handoff-verification-residue.md` §7. The essentials for this arc:

- The assumed buckets dissolved: no crashes anywhere; the "587-element residue" is the expected summary
  fallout of cycle breaking activating mid-verification (genuine residue: 2 elements, a
  `ParameterizedTypeImpl` lambda); exit code 5 is the guard reporting contract violations, not cycle
  protection; and the 71 `nonModifying=null` methods are abstract cst-api methods without in-scope
  implementations — `MODREACH=1` (the gated §14 shadow pass) already decides all 71.
- The real blocker of `ParameterizedType`/`Expression`: **recursive pure methods can never compute
  non-modifying**. `MethodInfoImpl.isNonModifying()` defaults undecided to modifying at call sites, so a
  method's own first evaluation poisons its summary (receiver + receiver-rooted field into the modified
  set), and the monotone write discipline keeps the FALSE forever. Minimal repro pinned in
  `TestRecursionThroughAbstract` (`direct()`, plain self-recursion: FALSE). Through
  `TypeInfoImpl.packageName()/descriptor()/fromPrimaryTypeDownwards()` → `TypeNameImpl.typeName` this
  sinks the entire print family, which is what the eventual contraction keeps tripping over.
- Composed gates measured: `MODREACH=1 EVENTUALCLUSTER=1` gives survivors 8→5, retracted 36→59, enm
  labels 414→522 — the honest downgrades of the shadow pass remove verdicts the optimistic seed leaned
  on. Method-level machinery is healthy; the type level dies exclusively on the recursion pessimism.
- Candidate fixes (decision pending, spelled out in handoff §7.5): (A) shadow-pass primitive seeding +
  reverse upgrade, corpus-inert behind MODREACH, recommended; (B) fixpoint-side optimism, rejected as it
  fights the §14 monotone-write architecture.

Session artifacts, all env-gated and verdict-inert (module suite green; dogfood A/B churn proven equal to
same-state base-vs-base): `FPDUMP_PARAMS` (parameter lines in the FPDUMP), `MODREACH_DEBUG` (reverse-
divergence dump), `MODREACH_EXPLAIN=<substring>` (BFS chain from a reached receiver back to its seed).

## Design A landed: the shadow pass repairs the recursion pessimism (2026-07-22, night, commit 23a01d71)

Bart approved handoff §7.5 design A; it is implemented, tested, and recorded in
`docs/handoff-verification-residue.md` §8. In brief: primitive seeding (walkable bodies no longer seed
receiver-rooted summary entries; assignments, boundary contracts, the undecided-abstract-callee mirror and
the E1/E2/E6 edges carry the evidence), E6-aware abstract seeding, the FALSE→TRUE reverse upgrade at the
cutover, the `@IgnoreModifications` mirror + immutable-variable cut in the projections, and `@NotModified`
on `Either.isLeft()/isRight()` (maddi-support byte-code aapi gap — the one ungated change; Fernflower
gate-off A/B **byte-identical, 0 lines**, suites analyzer/link/prepwork/common all green).

Effects on the dogfood under `MODREACH=1`: reverse-kept 231→0, joint fixpoint clean, ~340 FALSE→TRUE
upgrades, `nonModifying=null` stays 0; `TypeInfoImpl.packageName()`/`fromPrimaryTypeDownwards()` and the
abstract `TypeInfo.packageName()` compute TRUE; `TestRecursionThroughAbstract.testModReach` pins the repro
all-true. `descriptor()` and the `print` family remain FALSE **correctly** — their chains pass through
`inspection.get()`, i.e. the pre-mark modification this whole arc exists to excuse.

**Consequence for the eventual endgame:** composing `MODREACH=1 EVENTUALCLUSTER=1` still nets fewer
survivors (4, retracted 61; `InfoImpl` survives for the first time): modreach's ~1586 honest TRUE→FALSE
downgrades hand `commitLabels` more methods than it currently excuses. The next front is Part B coverage
against the honest modification state (EC_RETRACT_DEBUG + eventuallyNonMod scoreboard on the composed
run), and one open engine question: jar `Stream.map` seeding as boundary-modifying despite the preloaded
jdk aapi (suspected per-sourceSet Info identity mismatch; `MODREACH_EXPLAIN` chains through
`SetOfMethodInfoImpl.nice()`).

## EVENTUALLY_UNMODIFIED_PARAMETER lands: the static-helper hop closes (2026-07-23, follow-up session)

The `docs/spec-eventually-unmodified-parameter.md` mechanism is implemented end-to-end — the commit walk
parameterized by a `WalkRoot` (this-walk unchanged; a `ParameterInfo` root computes the parameter twin of
`@NotModified(after=)`), the abstract-accessor bridge through IMPLEMENTATIONS (interface-typed roots
resolve `p.typeInfo()` to implementation field labels), consumption at bare-root argument sites (with the
same-label-space pass-through for overload forwards), gated batch propagation, annotation/codec twins, and
`TestEventuallyUnmodifiedParameter` on the COMPOSED harness. The full record, measurements and honest
misses live in the spec's §8; the residue handoff §9 carries the closing pointer.

Headline: 265 eup parameters on the composed dogfood; the §7.2 evidence chain closes — the printer's
`parameterizedType` and `TypeNameImpl.typeName`'s `typeInfo` carry labels, and the entire
`ParameterizedType(Impl)` print/fullyQualifiedName/descriptor/mostSpecific family gains
`enm=[typeInfo, typeParameter]` (net +21 enm, two stale optimistic unions honestly dropped). The
scoreboard however does not move (survivors 5, retracted 63; `ModuleInfoImpl.Provides/UsesImpl` strengthen
to `@Immutable(hc=true)(after=…)`): the cascade now stops at three NEWLY EXPOSED shapes — List-of-
candidate-content fields (`typesReferenced` bails on `this.parameters`), the type-parameter-map builders,
and `rewire`/`withNullable` — each of which is its own designed mechanism (spec §8.3). That is the next
front.

## The container ride-along lands: ParameterizedTypeImpl forms its verdict (2026-07-23, same day)

Spec §8.3 item 1 is implemented (record: `docs/spec-eventually-unmodified-parameter.md` §9, gated
`EVENTUALCLUSTER`): the §060 ride-along one indirection deeper, granted per SITE — read position
(non-modifying, non-dependent calls on final container fields of committable content, wrapper
stability proven syntactically), argument position (callee provably neither mutates nor accessibly
retains the wrapper; constructors judged by direct body scan), and ConstructorCall sites now visited
by both walks (closing a genuine capture hole). Composed dogfood: enm 567→663 (+122/−7 vs the
certified baseline; the 7 losses are honest capture-hole corrections), modifying-unlabeled
1382→1273, survivors 6 (the `TypeReference` pair joins). `ParameterizedTypeImpl` reaches 100%
method excusal (ctor + one anonymous supplier aside) and FORMS ITS OWN EVENTUAL VERDICT for the
first time — retracted by its lean on the `MethodInfo` interface. The frontier is now the
interface clique: `api.info.MethodInfo` (19 leans), `ExpressionImpl` (19), `Element` (9),
`TypeInfoImpl` (8), `StatementImpl` (8), the Builders — breadth work over their remaining
rewire/translate/print holdouts, plus the noted look-through gap for concrete superclass-declared
accessors. Gate-off Fernflower A/B: byte-identical, 0 lines.

## The interface clique round: the flagship family FORMS (2026-07-23, continued)

Four changes, chasing the measured roots (`EC_RETRACT_DEBUG` lean counts) one instrument at a time
(new env-gated diagnostics: `EC_TYPE_DEBUG=<fqn-substrings>` traces computeTypeLevel/immutableAfterMark/
the supertype loop; `EC_ASSUME_DEBUG=<substring>` prints DIRECT assumption edges as they are recorded):

1. **Concrete inherited accessor look-through** (gated): the same-class inline in `commitLabels` now
   keys on the DECLARING type's label space, so `StatementImpl.comments()` inlines inside
   `DoStatementImpl.rewire`.
2. **Interface-constant finality** (UNGATED, JLS 9.3): `FieldInfoImpl.isPropertyFinal()` returns true
   for interface fields — `String CONSTRUCTOR_NAME = "<init>"` read as an ASSIGNABLE field and made
   `MethodInfo` (and every constants-carrying interface, in every corpus) unconditionally @Mutable via
   the `fieldsAssignable` early exit. Fernflower A/B: exactly 3 constants-only interfaces strengthen
   (+ the known flake); five suites green with no re-pins.
3. **Markless-carrier container fields** (gated): `computeTypeLevel`'s ride-along loop and
   `resolveExcusedFields` accept `containerContentCommittable` — `ExpressionImpl.comments`-style final
   Lists of committable content join the mark labels. Survivors jumped 6→15 on that step alone
   (all four remaining ModuleInfo members, DetailedSourcesImpl, MethodMapImpl, PerPackage, three
   ValueImpl types).
4. **Weak-verdict deferral** (gated): the `TypeInfo` interface froze `@FinalFields(after="inspection")`
   in ITERATION 1 with 2 of its eventual 33 enm labels present — the verdict is write-once, and
   `immutableSuper`'s `isMutable(@FinalFields)` check then hard-sank every subtype to MUTABLE. A
   final-fields after-mark level computed before the cycle-breaking phase is now deferred to the
   terminal iterations, when the method layer has converged.

**Composed scoreboard (stable across two runs): survivors 11, retracted 92 — and for the first time
the ENTIRE flagship family FORMS eventual verdicts** (TypeInfoImpl, MethodInfoImpl, FieldInfoImpl,
ParameterInfoImpl, the TypeInfo/MethodInfo interfaces, ParameterizedTypeImpl — all now in the
retracted set rather than the never-forms roots). The cascade is wider (four small survivors of the
previous step are currently pulled under by the larger folded assumption sets), and the remaining
ROOTS are precisely measured:

- **The Builder interfaces** (ParameterInfo/MethodInfo/TypeInfo/FieldInfo/TypeParameter.Builder,
  ~26 leans): direct assumers are the flagship impls themselves — their `builder()`-style PRE-MARK
  accessor chains get excused by Builder candidacy, which can never prove (plain setters). The right
  mechanism is @Only(before) classification for pre-mark accessors, not candidacy: a designed
  feature, next quest.
- **Element (9) / Statement (9)**: the print/rewire/variableStream* abstract-union breadth (the ~51
  print implementations), unchanged.
- **VariableImpl (7)**: assignable lazy-cache fields (`cachedFqn`, `cachedHash`) — needs its own
  cache-exemption story (field finality is deliberately not relaxed by the mark).
- FieldInspection (4) and a long interface tail.

## The Builder-lean quest, round 1 (2026-07-23, continued): 10 -> 4 edges, @Only via preconditions

`docs/handoff-builder-leans.md` carries the full characterization and record. Implemented (gated):
the precondition shapes -- leading `assert <state test>` and both if-throw guards -- classify a
method `@Only` on the side the live path requires (23 methods on the dogfood, the `builder()`
family among them, excused at type level through the `@Only(before)` route instead of mislabeled
enm); the transition-callee bail is relaxed for receivers provably not root-derived (another
object's lifecycle -- `other.commit(s)` on a parameter no longer bails the walk); and
`commitArguments` separates call-excuse eup labels from value-commit labels (hygiene; measurably
neutral). Composed dogfood: Builder assumption edges 10 -> 4, `@Only`-classified 23, enm 673 -> 654
(the newly-@Only methods leave the enm layer by design), survivors 10 / retracted 93, the flagship
family still forms; the survivor wobble is pure-cascade (`broken: []`). The four resistant edges
(`rewirePhase1/3`, `handleMethodOrConstructor:0`) are characterized down to the call path in the
handoff §4b, with the conflation theory implemented-and-falsified and the write-once-ordering
theory left as the pointed next investigation. Gate-off Fernflower A/B: 0 lines, twice.

## The Builder-lean quest, round 2 (2026-07-23, continued): 4 -> 0 edges, the Builder root eliminated

The §4b instrumentation mandate produced an iteration-stamped site trace (`EC_SITE_DEBUG`, log-only:
per-computation MC/receiver/gauntlet/track lines, WRITE stamps at every eventual-property landing,
`EventualCluster.ITERATION`), and the trace falsified the write-once-ordering theory on contact: the
abstract `builder()`'s `@Only(before)` is written in iteration 1, BEFORE the leaning enm computations
run. The real mechanism (`handoff-builder-leans.md` §4c): the fluent chains fold ARGUMENT labels into
the chain value, so the outer link runs the handed-on gauntlet committed -- and a MODIFYING Builder
setter never reaches `handedOnValueSafe`'s independence branch, falling through to the return-type
candidacy lean. Two gated fixes: (1) `receiverProvablyNotRoot` consulted BEFORE `handedOnValueSafe`
-- a chain based in another object's graph needs no lean, its inflowing root content is already
committed by the accumulated labels; (2) freshness is now a least fixpoint over the assignment graph,
so `TypeInfo.Builder b = typeInfo.builder(); b.setX(..)` behaves exactly like the inline chain
(`copyAllButConstructorsMethodsFieldsSubTypesAnnotations` was the fifth, new, edge that exposed this).
Composed dogfood: **Builder edges 0** (10 at characterization, 4 after round 1), enm 657, eup 307,
@Only 23, survivors 10 / retracted 94, flagship family forms throughout; the retraction-root list is
now Builder-free -- led by Statement(7), Element(4), FieldInspection(3), VariableImpl: exactly the two
remaining quests. Pins in `TestCommitLabels.INPUT_FLUENT`; gate-off Fernflower byte-identity; suites
green.

## The Element/Statement breadth quest, round 1 (2026-07-23, continued): the abstract unions start landing

Measurement first (`EC_ASSUME_DEBUG` now takes a comma-list; a new `ECSITE "enm batch … blocked by …"`
print in `AbstractMethodAnalyzerImpl` names the implementation that kills each abstract union): the 42
Element/Statement/FieldInspection/VariableImpl edges are mostly typeLevel hierarchy propagation; the
real roots were ~15 impl bodies blocking the abstract enm unions of `Element.complexity/print/rewire/
typesReferenced/variableStream*` and `Statement.translate/rewire/withBlocks/withSource`. The per-body
traces reduced the bails to four mechanisms, three of them now closed (all gated):

1. **The downward interface closure**: candidacy closed upward only, so the markless sub-interfaces of
   `Element` (`Block`, `Comment`, `LocalVariable`) never entered the cluster, and every statement/
   expression field of those types bailed the walk (`CatchClauseImpl.complexity`'s `this.block`).
   `isCandidate` now admits an INTERFACE whose superinterface is a member -- implementations still
   earn membership the strict way. Setter-bearing interfaces (the Builders) are refused: `haveSetters`
   is an unconditional MUTABLE exit, so their membership is pure doomed mass. The same refusal now
   guards both ends of `treatAsEventuallyImmutable` (a setter-bearing member's optimism is wasted, a
   setter-bearing candidate's is doomed) -- safe only since the Builder-lean quest, whose walk fixes
   carry the rewire chains without Builder candidacy.
2. **The accessor spelling of the container ride-along** (`rootContainerField`): the whole statement/
   expression family hands its final lists around as `comments()`/`annotations()`, not `this.comments`
   -- both ride-along positions now unwrap a plain non-setter accessor called on the bare root. This
   is what let the rewire/translate/withX copy-constructor family land (`WhileStatementImpl.withBlocks`
   enm=[annotations, comments, expression], `Statement.translate` union with 17 labels).
3. **Primitive streams** (`isPrimitiveStream` in `returnTypeHoldsCommittableContent`): a `mapToInt`
   reduction hands on VALUES only; parameterless, it had neither the immutable-hc route (streams are
   contractually consumable) nor the type-parameter route. Unblocked `SwitchEntryImpl.complexity`,
   and with it the honest 9-label `Element.complexity` union.

**Composed scoreboard (es6): enm 657 -> 933, eup 307 -> 376; `Element.complexity`,
`Statement.translate` (17 labels), `Statement.rewire` unions land; retracted 160, survivors 5, the
flagship family forms throughout.** Survivors dip (10 -> 5: the `ModuleInfoImpl.*` nested types are
pulled under by the larger folded assumption sets -- formation intact, pure cascade) because the
cluster now spans the whole CST api; they return as the roots prove. Known instability, measured:
the abstract-union write-once still races the modreach downgrade (`Element.complexity` lands the
honest 9-label union in one run, the stale 2-label one in another) -- the race predates this round
and is the natural next target. Residue: `Element.print` <- `TypeInfoImpl.print`, `Element.rewire`
<- `BitwiseNegationImpl.rewire` (per-body chases), one doomed edge `EvalOr -> Or.Builder` (an
adder-only Builder the setter proxy cannot catch -- harmless, witnessed, retracted), and the
`VariableImpl` cache quest untouched. Pins: `TestCommitLabels.INPUT_BREADTH` (closure admit + setter
refusal; the accessor spelling and primitive-stream mechanisms are corpus-validated only -- the
aapi-less unit harness shallow-defaults JDK containers too leniently to discriminate). Gate-off
Fernflower byte-identity; suites green.

## The Element/Statement breadth quest, round 2 (2026-07-23, continued): the race is dead — the honest baseline

The abstract-union write-once vs modreach-downgrade race is closed STRUCTURALLY rather than by
per-batch deferral: the eventual layer (EVENTUAL_METHOD, the enm/eup label layer,
EVENTUALLY_IMMUTABLE_TYPE) now joins the immutability family in the post-cutover
`clearDerivedFamily` (gated), and the cluster resets alongside (`resetForRederivation`: witnessed
edges, label provenance, candidacy caches -- they belong to the cleared computations). The whole
eventual layer re-derives on the honest, frozen modification state; contracts re-materialize on the
next pass. Companion fix: `hasSetters` now consults each abstract method's IMPLEMENTATIONS (an
abstract Builder interface carries no getset marks of its own), which refuses the honest world's
re-appearing Builder leans (`FactoryImpl -> MethodInfo.Builder`).

**The eventual layer is now fully DETERMINISTIC: two composed runs differ by 0 lines across
enm/eup/eventual/after-mark** (the only dump diffs left are the known QualifiedName/SymbolEnum
modification flake). `Element.complexity` lands the honest union every run. The honest price, now
measured instead of raced-over: enm 933 -> 806, eup 376 -> 309 -- that much of the pre-fix layer
rested on pre-cutover optimism -- and survivors drop to 1 (TextBlockFormattingImpl) with 147
retracted, while THE FLAGSHIP FAMILY STILL FORMS in every run. Both re-derivation rounds converge
("done? true"): this is the honest fixpoint, not budget starvation.

**The honest retraction roots re-rank the roadmap:** `Expression` (~25 folded appearances -- the
eval-engine breadth: EvalOr/EvalNegation/... lean on expression interfaces), `TypeInfo`(11),
`Runtime`(12 folded -- the factory interface), `Element` down to 2; the per-body residue
(`TypeInfoImpl.print`, `BitwiseNegationImpl.rewire`) persists in the honest world. Next quests
should start from THIS list, re-measured with `EC_ASSUME_DEBUG=Expression,Runtime`.

## The honest-roots quest, round 1 (2026-07-23, continued): measurement + inherited-field ownership

`EC_ASSUME_DEBUG=Expression,Runtime,TypeInfo` on the honest baseline: 324 direct edges (Expression
187, TypeInfo 81, Runtime 56) -- these are not batch blockers but the WIDESPREAD leans: the whole
label layer rests on these three candidacies, discharged only by their proof or survival. TypeInfo
FORMS (pure cascade victim). The actionable roots:

- **Expression never forms because exactly four abstract unions fail**: `rewire`, `translate`,
  `withSource`, `internalCompareTo` (every other Expression method landed). Blockers, per the batch
  diagnostic: `InstanceOfImpl.translate` (the `translationMap.translateExpression(this)` BARE-THIS
  handout -- a candidacy chicken-and-egg: the owner-seed escape needs the leaf impl to be a
  candidate, which needs one enm to land first), `InstanceOfImpl.rewire`,
  `ConstructorCallImpl.withSource`, `BinaryOperatorImpl.internalCompareTo`,
  `UnaryOperatorImpl.rewire` -- each needs its own chase.
- **Runtime is sunk by its supertypes**: zero modifying methods, zero dependent methods, but
  `Factory`/`Eval` cap at @FinalFields (and `isMutable(@FinalFields)` sinks the extender). Factory
  even carries a mark (`markLabels=[intParameterizedType]`, one excused method) yet
  `afterMark=@FinalFields buys nothing` -- WHY the unconditional computation stops at FinalFields
  for an all-non-modifying interface is the open question (needs a deeper EC_TYPE_DEBUG print
  inside computeImmutableType0's hc rules).

**Landed this round (gated): inherited-field ownership in the commit walk.** The
`UnaryOperatorImpl.operator` shape: `BitwiseNegationImpl.rewire` reads `operator`/`precedence`/
`expression` -- SUPERCLASS fields -- and the FieldReference branch demanded strict own-field
membership, bailing silently. `isOwnOrInheritedField` walks the superclass chain; the label names
the super's field, which the type level tolerates exactly like an inherited mark (the clique
round's precedent for inherited accessors). Composed: enm 806 -> 824, BitwiseNegationImpl leaves
the rewire blocker list. Pin: `INPUT_BREADTH.SubClass.subSize` = [item] (∅ off the gate).
Also observed en route: a parameterless immutable-hc field type (Precedence, Diamond) is already
harmless to the walk -- the flag interfaces were a false suspect.

## The honest-roots quest, round 2 (2026-07-23, continued): two Expression unions land

Two coupled changes crack the bare-this deadlock and the expression family's real poison:

1. **The self-assumption** (gated, `treatAsEventuallyImmutable`): a type may always lean on ITSELF --
   the computation in flight is the very one that would make it a candidate. This breaks the leaf
   impls' chicken-and-egg (`translationMap.translateExpression(this)`, `new CommonType(this)`: the
   owner-seed escape needed candidacy, candidacy needed a first enm). Witnessed like any edge: a type
   that never forms retracts everything that consumed its labels.
2. **`@IgnoreModifications` on the expression trio's analysis maps** (cst-impl source, the InfoImpl
   precedent applied consistently): `InstanceOfImpl`/`MethodCallImpl`'s `propertyValueMap` and
   `ConstructorCallImpl`'s `analysis` are the same manual-hidden-content overlay (road §050) that
   InfoImpl already declares; handed into every copy constructor, the unannotated field poisoned
   every `translate`/`withSource` walk of those types. Only maddi's own source is affected -- corpus
   inputs never see cst-impl annotations.

**`Expression.rewire` and `Expression.withSource` abstract unions LAND** (the full union over the
~80 expression impls); `InstanceOfImpl.translate` clears. Composed: enm 824 -> 839, eup 309 -> 328,
flagships form throughout. Remaining for Expression: `translate` (blocked by the two most complex
bodies, `ConstructorCallImpl.translate` and `MethodCallImpl.translate`) and `internalCompareTo`
(`BinaryOperatorImpl`). The Factory/Eval cap decomposed en route: exactly two abstract methods
(`commonType`, `newInlineConditional`, both funneling into `CommonType.commonType`'s
inspection-reading lattice walk plus FactoryImpl's own lazy caches) -- unresolved, next round.

## The honest-roots quest, round 3 (2026-07-23, continued): Expression.translate lands — container
## aliases, identity transparency, the two-round local context

The remaining translate blockers (`MethodCallImpl`/`ConstructorCallImpl`) decomposed into three
mechanical gaps, all closed (gated):

1. **Container-alias tracking** (`LocalContext.containerAlias`, pass 3 of `buildLocalContext`): the
   `list.isEmpty() ? list : rebuilt` short-circuit assigns the BARE field wrapper to a local in one
   branch -- correctly uncommittable as a VALUE, but the local provably aliases one known container
   field, and the per-site rescues (read-through, argument, constructor-capture) may judge it as the
   field spelled inline. A subset guard on the other branch's labels keeps the minted label sound.
2. **@Identity transparency** (`Objects.requireNonNull`, aapi `identityMethod`): the wrapper-safety
   ctor scan treats an identity forward as the value itself -- `this.f = requireNonNull(p)` is the
   capture, judged at the assignment; the call is not an onward handoff.
3. **The two-round local context**: aliases discovered after the commit fixpoint un-poison DOWNSTREAM
   locals (the copy built FROM the aliased local), so the fixpoint re-derives once with the aliases
   in place.

**Three of Expression's four abstract unions now land** (rewire, translate, withSource -- each the
full union over the ~80 impls). Composed: enm 842, eup 339, flagships form. The LAST Expression
blocker is `internalCompareTo` <- `BinaryOperatorImpl`, which is not mechanical: the walk SUCCEEDS
with EMPTY labels (`rhs.compareTo(x)`: the direct callee is contract-non-modifying, so no excuse is
demanded, while modreach honestly reaches the modification through the `internalCompareTo` dispatch)
-- an ∅-enm on a decided-modifying method is currently unwritable, and deciding what it should MEAN
(write ∅-enm as a first-class value? demand the receiver labels at contract-non-modifying dispatch
sites whose implementations modify?) is a design question for the next session, ideally with Bart.

## The ∅-enm decision (2026-07-23, WITH BART): dispatch-honest excuse sites

**Decision (Bart, option B): the site-level modification view must never be more optimistic than the
dispatch closure.** A contract-non-modifying ABSTRACT callee (`Comparable.compareTo`, `Object.equals`)
whose analyzed implementations honestly modify pre-mark counts as modifying at excuse sites -- the
receiver's labels are demanded exactly as if the honest implementation were called directly. Option A
(first-class ∅-enm) was rejected: it would invert the label lattice at one point ("∅ = after full
commitment" vs everywhere else "fewer labels = weaker requirement") and leave the equals-family gap.

Implementation: `implementationHonestlyModifies` consults IMPLEMENTATIONS -- and, NEW, the additive
`EXTERNAL_IMPLEMENTATIONS` property: prepwork used to skip external-library overrides entirely, so a
jar abstract (Comparable.compareTo) carried no dispatch closure at all. The external key is kept apart
from IMPLEMENTATIONS so the abstract batches never write onto jar methods; the property is inert to
everything but the gated predicate. The underlying FINDING stands on its own: maddi's CST honestly
violates the JDK's "comparing/equality does not modify" contracts BEFORE the mark (comparison forces
lazy inspection) -- something the guard could eventually report as a contract tension.

Measured: the fake ∅-walks are gone (BinaryOperatorImpl.internalCompareTo now demands and honestly
FAILS the rhs excuse instead of vacuously succeeding); +7 enm from dispatch-honest sites that CAN
excuse (849 total, eup 339); zero regressions; flagships form; gate-off Fernflower byte-identity.
The one remaining hop for the internalCompareTo union: `commitLabels(this.rhs)` bails although
Expression is a direct candidate and rhs is an own final field -- next session's first probe is one
print inside `fieldHoldsCommittableContent`'s refusal path for that exact site.

## Session close (2026-07-23): the internalCompareTo probe resolves — convergence, not design

The recorded probe (why `commitLabels(this.rhs)` bailed) resolved into ORDERING, not a rule gap:
`isCandidate(Expression)` was false in the failing attempt and true one attempt later, and
**`BinaryOperatorImpl.internalCompareTo` landed enm=[lhs, rhs]** in the same run under the
dispatch-honest sites. The abstract union's remaining blocker is `GreaterThanZeroImpl.internalCompareTo`,
which decodes to the ordinary eup chain one remove further: `compareBinaryToGt0(binary, this)` hands
bare `this` to a static helper whose eup on parameter 1 has not landed yet -- a chase, not a decision.
Diagnostics added and kept (gated): the `fieldHolds refusal` and `treatAs refusal` prints that made
the probe decisive.

**Where the arc stands at session close** (nine commits, `9a2f5ea3..`): the Builder root eliminated;
the abstract-union race dead and the eventual layer fully deterministic; the honest baseline enm 849
/ eup 339 with the flagship family forming in every run; Expression's rewire/translate/withSource
unions landed and internalCompareTo one eup-chase away; the ∅-enm decision (dispatch-honest sites)
taken with Bart and implemented. Next session: the GreaterThanZeroImpl eup chase, then Factory's
lazy caches, TypeInfoImpl.print, VariableImpl caches -- and the cluster's survivors will start
recovering as Expression forms.

## The GreaterThanZeroImpl eup chase (2026-07-24): the internalCompareTo union completes

The chase decoded into two consumption gaps in the eup fold (`commitArguments`), both mechanical:

1. **The ancestor label space.** `sameLabelSpace` demanded exact type identity between the callee's
   parameter type and the walk's label type. The `GreaterThanZero`-rooted walk of
   `compareBinaryToGt0:1:e2` hands its root to `compareVariables:1` declared as `Expression` -- an
   ANCESTOR interface, the same promise space one level wider -- and fell through to
   `labelsCommittableOnRoot`, which no interface can pass (no fields). Silent skip, walk yields ∅,
   unwritable. The guard diagnostic (`eup guard: unmodified=...`) first ruled out the early exits:
   pre-cutover the plain layer optimistically says `unmodified=true` (guard refuses, correctly);
   post-cutover the walk ran and the `treatAs refusal` print (kept from the probe round) named the
   real site. Fix: interface-rooted walks accept an ancestor parameter type (`isAncestorType`),
   mirroring the downward-interface-closure argument; committability stays the ultimate consumer's
   job, exactly as for the identity case.
2. **Dispatch narrowing at the class-rooted consumer.** With eup(`compareBinaryToGt0:1`) = the full
   Element union landed, `GreaterThanZeroImpl.internalCompareTo`'s enm walk hands bare `this` and
   fails whole-set committability (`lhs` is no field of a GreaterThanZeroImpl). But the union labels
   are per-implementation excuses: a label naming no field anywhere in the argument's runtime cone
   (the root class plus its known subclasses, fields own or inherited) is VACUOUS for this argument
   -- those code paths cannot execute on it. The consumer now folds the committable restriction when
   the entire residue is vacuous (`residueVacuousOnCone`); anything less falls through unchanged.
   This is the receiver analog the spec (§7) asked to mirror: a `this.foo()` call narrows naturally
   via method resolution; the arg-position fold had no narrowing at all.

Outcome (composed dogfood): **the abstract `Expression.internalCompareTo` union LANDS** -- every
implementation plainly non-modifying or labeled (`GreaterThanZeroImpl` = `[expression]`, the
narrowed fold). enm 849 -> 851, eup 339 -> 344; `GreaterThanZero` now forms-and-retracts with named
broken deps instead of never forming. Survivors stay at 1: Expression's ledger entry lists its
remaining lean roots, and the retraction ranking (`TypeInfo` 36, `MethodInfo` 33, `Runtime` 21,
`Element` 20, `VariableImpl` 12) is exactly the recorded roadmap -- the Factory/print/VariableImpl
quests, in that order. Unit pins: `TestCommitLabels.INPUT_CHAIN` (both mechanisms discriminate in
the aapi-less harness; gate-off twin). The silent-skip fallback for a live residue remains, recorded
here as the residual ∅-gap: a walk can still yield [] when a residue label names a real cone field
-- honest per-site, but the spec's bail would be stricter.

## The Factory quest (2026-07-24, continued): the self-call rule + the precedenceMap flip

The `commonType`/`newInlineConditional` cap decoded in three layers:

1. **Direct recursion poisoned the walk.** `CommonType.commonType`'s lattice descent calls itself
   (`commonParameter = commonType(parameter, pt2Parameter)`); the enm walk read the callee's enm --
   the very set under computation, write-once and unwritten -- got ∅, and bailed (the local context
   pass showed it verbatim: `track commonParameter = null`). The SELF-CALL RULE (both root-receiver
   branches, gated): a call to the method under analysis is excused by the fixpoint hypothesis -- its
   excuse set IS the set in flight, it contributes nothing new; the result still runs the ordinary
   handed-on gauntlet. `WalkRoot` now carries `underAnalysis`. Collateral: **+12 enm / +8 eup** --
   the whole Eval recursion family (`EvalSum.expandTerms`, `wrapSum/wrapInSum/wrapInProduct`,
   `EvalInlineConditional.eval`, `Eval.sortAndSimplify`) and the ImportComputer landed at once.
   Unit pin: `TestCommitLabels.INPUT_RECURSION`.
2. **`FactoryImpl.precedenceMap` was mutable by construction style, not by semantics.** The ctor
   filled the `HashMap` field with `put()` calls -- and part-of-construction excludes only
   ASSIGNMENTS, not content-modifying calls, so the plain layer honestly said `unmodified=false`
   and capped FactoryImpl. The cst-impl refactor (build a local map, `Map.copyOf` into the final
   field) flipped it to `unmodified=true independent=@Independent(hc=true)` -- the source now
   follows its own rules. FINDING, recorded: ctor-`put()` maps read as mutable; build immutably.
3. **The remaining cap is the interface-hierarchy cycle.** With the field flip, the ECTYPE trace
   shows `FactoryImpl` REACHING `@Immutable(hc=true)` mid-run (markLabels include `precedenceMap`,
   `excusedF=6`), but rounds with `afterMarkNone=true` collapse it to `@Mutable` via the hierarchy
   rule -- the INTERFACE `Factory`'s `@FinalFields` verdict caps its own implementation through the
   `isMutable(@FinalFields)` lattice quirk, circularly. `FactoryImpl` now forms-and-retracts on
   named deps ([IntConstant, BooleanConstant, MethodInfo, TypeInfo, Factory, VariableImpl...]) --
   the VariableImpl-caches and flagship quests, exactly the roadmap. `CommonType.commonType` itself
   is now an honest ∅ (nothing this-rooted for the walk; the plain FALSE is world-graph reachability
   through `runtime`) -- unwritable by design, waiting on the Runtime family's candidacy.

Scoreboard: enm 851 -> 863, eup 344 -> 352, survivors 1, retractions 152 -> 155 (more of the world
forms before the contraction). Validation: module suites green (16/16 TestCommitLabels), composed
runs identical up to one documented plain-layer flake (`ValueImpl.IndependentImpl`), gate-off
Fernflower identical modulo the 1-line ctor-nonModifying flake -- now known to be a FAMILY, not a
single line: run 2 flipped `Exprent.<init>(int)`, run 3 flipped the documented
`StatEdge.EdgeType.<init>(int)` back and forth. NEW FLAKE observed and cured: after heavy
gradle-daemon reuse, javac emitted a burst of bogus syntax errors on valid Fernflower sources
(`EXIT_PARSER_ERROR`); a daemon restart cleared it -- the parsing-stability.md process-wide-state
story, now seen at corpus scale.

## The VariableImpl cache quest (2026-07-24, continued): the memo disclaimer

The `cachedFqn`/`cachedHash` lazy memos -- a deliberate hot-path optimization (equals/hashCode on
the link-graph probes; the fields cannot be made eager, the base ctor cannot call the virtual
`fullyQualifiedName()`) -- sank `VariableImpl` at the very FIRST exit: non-final fields, MUTABLE
before any after-mark relaxation, and their writes made `hashCode`/`equals`/`fqnForEquality`
plainly modifying. The design: extend `@IgnoreModifications` (road §050, "manual hidden content")
to the SLOT -- on a private idempotent memo, the disclaimer covers the assignment as well as the
content. Four parts:

1. **cst-impl**: `@IgnoreModifications` on both fields (the source annotates its own idiom).
2. **Plain layer, UNGATED (contract-honoring, the `loopOverFieldsAndMethods` precedent)**:
   `ExpressionVisitor.assignment` no longer marks the scope chain modified when the target field is
   `@IgnoreModifications` -- symmetric with the filter `MethodModification.go` already applies to
   call-through modification. No-op without the annotation.
3. **Gated, after-mark only**: `computeImmutableType0`'s assignable-fields exit looks past
   `@IgnoreModifications` fields when `!afterMark.isNone()` -- the UNCONDITIONAL verdict keeps the
   honest `@Mutable` (the slot IS assignable), only the eventual relaxation sees through.
4. **Gated**: the enm walk's own-field-assignment bail skips disclaimed fields.

The unit pin (INPUT_MEMO) exposed part 4 and decoded the dogfood truth: `hashCode` is HONESTLY
modifying through the abstract `fullyQualifiedName()` dispatch (lazy inspection), so the right
outcome is not plain non-modification but a landed enm -- and it landed: all three methods carry
**enm=[methodInfo]**. `VariableImpl` reaches `immutableAfterMark=@Immutable(hc=true)` and is GONE
from the retraction-root ranking (was 12 lean edges): it now retracts only as a cascade victim of
the flagships, `broken: []`. Remaining roots: TypeInfo 38, MethodInfo 33, Comment 24, Runtime 21,
Element 20, Block 16 -- the flagship convergence itself.

Scoreboard: enm 863 -> 866, retractions 155 -> 156, survivors 1. Validation: module suites green
(17/17 TestCommitLabels), three-corpus A/B for the ungated part -- fernflower byte-identical to the
canonical baseline, langchain4j A/B byte-identical, timefold identical modulo a DEMONSTRATED
same-code run-to-run flake (two B-runs differ by 6 lines among `testdomain.*Solution` independence
verdicts; the flipping set wanders). Composed dogfood determinism: 22 diff lines, all within the
documented QualifiedNameImpl/SymbolEnum flake set; the eventual layer 0-diff.

## The print quest (2026-07-24, continued): Element.print lands -- the opaque sink + the wrapper-capture fold

The 51 unlabeled print methods decoded into two mechanisms, and both landed:

1. **The opaque sink** (`contentTypeHarmless`, gated): the printer pipeline's chains hand on
   `Stream<OutputBuilder>` / `OutputBuilder` values, and `handedOnValueSafe` refused them --
   `OutputBuilder` is a genuinely mutable builder. But it is a mutable builder of `OutputElement`s,
   which the analyzer itself certifies `@Immutable(hc=true)`, and its entire method surface exposes
   only elements, Strings, primitives and itself: NOTHING root-typed can be stored into or read out
   of one. Reachability, not commitment, is what the gauntlet protects -- a value through which the
   root provably cannot be reached is safe to hand on. Implemented as `contentTypeHarmless`
   (primitives; immutables; immutable-hc with harmless parameters; shallow containers with harmless
   parameters; interfaces with an entirely-harmless signature -- `signatureOpaque`, cached in the
   cluster and reset at rederivation, since it derives from IMMUTABLE_TYPE). One clause in
   `returnTypeHoldsCommittableContent` + one in `typeParametersHoldCommittableContent`.
   **+45 enm at once**: the whole printer family (`TypePrinterImpl.print`=[typeInfo] x3,
   `MethodPrinterImpl.print`=[methodInfo, typeInfo], `FieldPrinterImpl.print`=[fieldInfo]).
2. **The wrapper-capture fold** (`wrapperCaptureLabels`, gated): `TypeInfoImpl.print` is
   `new TypePrinterImpl(this, false).print(...)` -- a modifying call on a fresh wrapper whose ctor
   captured bare `this`, which the freshness shortcut excused with ∅ (the residual ∅-gap, wrapper
   edition). The wrapper method's own enm labels name the wrapper's fields; translate each through
   the constructor's capture map (computed syntactically, `this.f = pi` -- the
   PARAMETER_ASSIGNED_TO_FIELD property is not reliably present): a bare-root capture owes the
   ROOT'S FULL COMMITMENT (`rootCommitmentLabels`: every field committable -> its label, or
   reachability-harmless / disclaimed -> nothing, or the promise fails -- committability checked
   FIRST, before harmlessness, so lenient shallow verdicts on transition carriers cannot swallow
   their labels: the unit pin caught exactly that ordering bug); any other captured expression owes
   its own commit labels; non-captured labels are fresh-owned, vacuous. The fold only ever ADDS.

Outcome: **the abstract `Element.print(Qualification)` union LANDS** (47 labels) -- the fourth
flagship union -- and ZERO unlabeled print methods remain. `TypeInfo` left the retraction-root
ranking entirely (was the top root at 38); remaining roots: MethodInfo 37, Comment 31, Runtime 21,
Element 21, Block 18. Scoreboard: enm 866 -> 920, eup 361 -> 392, retractions 162, survivors 1.
Diagnostics kept (gated): the enm guard prints (eventual/nonModifying early exits), the
excuse-position transition-bail print, the wrapper-fold print. Validation: 19/19 TestCommitLabels
(INPUT_WRAPPER pin -- discriminating via a second committable field, since the aapi-less plain layer
does not trace modification through captures; opaque sink corpus-validated per the NOTE precedent);
gate-off Fernflower identical modulo the documented ctor flake; composed determinism -- eventual
layer 0-diff, plain diffs within the known flake family (two adjacent members observed:
`Value.AssignedToField`/`AssignedToFieldImpl`).

## The flagship convergence, round 1 (2026-07-24, continued): survivors 1 -> 42

The remaining retraction roots (MethodInfo 37, Comment 31, Runtime 21, Element 21, Block 18) decoded
into three mechanisms; the third was a one-line semantic bug with outsized reach.

1. **The bare-root-argument commitment fold** (`commitArguments`, gated): the arg-position sibling of
   the wrapper-capture fold. `ParameterInfoImpl.rewire` is `return infoMap.parameterInfo(this);` --
   bare `this` handed to a plainly-modified parameter carrying NO eup promise was ∅-excused by the
   self-assumption. It now owes the ROOT'S FULL COMMITMENT (`rootCommitmentLabels`); only ever adds.
   `ParameterInfoImpl.rewire` = [inspection, methodInfo, parameterizedType] -> **the abstract
   `Element.rewire` union LANDS (63 labels, the fifth flagship union)** -> `Element.print`'s sibling
   完成 -> `Element` reaches `immutableAfterMark=@Immutable(hc=true)` (excusedM=13) and FORMS. The
   existing `bailBareThis` pin updated: [peer] -> [inspection, peer], the honest named promise.
2. **Part B -- super -> markless member label inheritance** (`computeTypeLevel`, gated): the downward
   interface closure at VERDICT level, and its class twin. A markless cluster member (`Comment`;
   `SingleLineCommentImpl`, a value object of Strings) has no transition of its own -- its only
   blocker is the hierarchy, whose promise is the object's, not one type's. Inherit every eventual
   supertype's labels (witnessed treatAs for the still-circular ones); soundness rests on
   `immutableAfterMark` still checking the member's own fields and methods in full.
3. **The degenerate AfterMark bug**: an inherited-marks type has EMPTY excused sets, and
   `AfterMark(∅,∅).isNone()` read as NONE -- the entire after-mark relaxation silently degraded to
   the unconditional path, letting the plain-@FinalFields super sink the member through the
   `isMutable(@FinalFields)` hierarchy exit. The record gained an `inheritedMarks` component
   (compat 2-arg constructor keeps every off-gate caller identical); the gated flow passes
   `EventualCluster.ENABLED`. This was the dam: with it, `Comment` formed on its inherited labels,
   the Element<->Comment mutual cycle closed inside the greatest fixpoint, and the whole
   comment/module/expression/variable interface universe survived the contraction.

**Survivors 1 -> 42**: the Comment family (Comment, SingleLineComment, MultiLineComment, JavaDoc,
ImportStatement, ModuleInfo.Exports/Opens/Requires + impls), the expression interfaces
(BinaryOperator, And, Or, Cast, ConstantExpression, InlineConditional, InstanceOf, MethodReference,
Numeric, TypeExpression, UnaryOperator, Instance...), the variable interfaces (FieldReference, This,
DependentVariable), CompilationUnit(+Stub, @FinalFields(after)), the CompilationUnitPrinter pair,
TextBlockFormattingImpl. The flagship Impl family and the api.info interfaces still form-and-retract
-- the next frontier (their ledger: MethodInfo, ParameterInfo, Runtime, java.util.Set, the statement
interfaces). Scoreboard: enm 924, eup 398. Validation: 21/21 TestCommitLabels (INPUT_MARKLESS pin;
the harness NOTE for sub-interfaces adding unimplemented abstract methods); gate-off Fernflower
identical modulo the documented ctor flake; composed determinism -- eventual layer 0-diff,
survivors 42 = 42 across runs.
