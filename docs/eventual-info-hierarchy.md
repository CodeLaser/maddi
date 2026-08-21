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
   methods that today CRASH with the known exit-5 `ANALYZER_ERROR` (cycle protection), leaving their
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

**Three-corpus gate-OFF A/B (2026-07-22, closing, at `e4bea61a` vs base `f092cd6e`):** Fernflower
**0-line diff**; Langchain4j **0-line diff**; Timefold 8 lines, every one of which flips identically
between two BASE runs (`TestdataInvalidConstraintWeightOverridesSolution`,
`TestdataFactorySortableSolution` independence, `ListIterableSelector`) — pre-existing run-to-run
nondeterminism, verified with the A-vs-A2 technique. The whole session's engine surface is gate-off inert
on the full certified proving ground.

**Gate-ON stability (2026-07-22, two consecutive dogfoods at `e4bea61a`):** the surviving core of **8** is
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

## Design A landed: the shadow pass repairs the recursion pessimism (2026-07-22, night, commit 110695ec)

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

**Where the arc stands at session close** (nine commits, `672b48bf..`): the Builder root eliminated;
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

## The drift round (2026-07-31): re-baseline, the Provides adder, doomed external candidates

Resuming after a week in which BOTH the engine and the cst sources moved (merges from the other
worktree arcs): the composed dogfood at devel `c16777e9` read **survivors 26 / retracted 166 /
enm 927 / eup 387** -- down from the recorded 42, deterministic across two runs (identical survivor
set, enm layer 0-diff). So the drop was real drift, not nondeterminism. The `EC_RETRACT_DEBUG` root
ranking against the never-form roots decoded it into two mechanical causes, both fixed:

1. **The `ProvidesImpl` adder (source drift, commit 22bd062a).** `provides X with A, B, C` support
   gave `ModuleInfoImpl.ProvidesImpl` a plain `ArrayList` + `addImplementationResolved` adder --
   genuinely mutable state with no transition, on a type that was one of the original four gate-off
   eventual types. Its `typesReferenced(Predicate)` became the ONE unexcusable implementation
   blocking the `Element.typesReferenced(Predicate)` abstract union; `Element` capped at
   FinalFields-after-mark, and the `isMutable(@FinalFields)` hierarchy rule sank
   Info/MethodInfo/TypeInfo/Comment/Statement/Block -- the bulk of the 166 retractions. Fix:
   commit-once restored -- `SetOnce<List<TypeInfo>>`, api `setImplementationsResolved(List)` (the
   adder removed), `JavaInspectorImpl.resolveModuleInfo` accumulates locally and sets once, the one
   adder call in `TestCallGraph` adapted. Confirmed on the dogfood: `setImplementationsResolved`
   computes `@Mark("implementationsResolved")`, `typesReferenced` lands
   `enm=[apiResolved, implementationsResolved]`, the `Element` union completes, and the whole
   Comment/ModuleInfo.Exports/Opens/Requires/CompilationUnit family returns to the survivor set.
   FINDING (the `precedenceMap` lesson again, adder edition): an adder-filled collection field reads
   as mutable; build locally, commit once. And: **the dogfood corpus is live source -- any teammate
   commit can sink a flagship union. Re-measure before chasing engine mechanisms.**

2. **Doomed external candidates (gated, `EventualCluster.candidateDoomed`).** `java.util.Set`
   (16 leans) and `java.util.function.Function` (4) sat in the cluster although an external-library
   type can never form an eventual verdict and -- below immutable-hc -- never discharge; every lean
   was certain retraction fodder, and worse, it shadowed the honest wrapper/container mechanisms at
   the same sites (`fieldHoldsCommittableContent` consults the seed BEFORE the immutable-wrapper
   path). `treatAsEventuallyImmutable` now refuses an external candidate without an hc verdict.
   Trap recorded: `ImmutableImpl.encode` serializes MUTABLE as absent, so for an external type
   ABSENT counts as doomed, not still-to-come; genuinely eventual support leaves (`SetOnce`) enter
   through the proven path (`actual.isEventual()`, ContractReader-backed) before this check. Pin:
   `TestEventualClusterAssumptions.testDoomedExternalCandidateIsRefused`.

**Composed scoreboard: survivors 26 -> 41, retracted 166 -> 152, enm 908 (-19 formerly-optimistic
labels, honest), eup 404.** Deterministic: two runs, identical survivor set, enm layer 0-diff.
Gate-off Fernflower A/B (stash technique): identical modulo exactly the two documented
ctor-nonModifying flake lines (`Exprent.<init>(int)`, `StatEdge.EdgeType.<init>(int)`), no verdict
moved. Suites: analyzer 270/0, prepwork 218/0 (1 pre-existing skip), common 68/0, cst-impl 73/0,
inspection-integration 435/0.

**The measured next wall: `Info.translate`, and factory-method freshness.** `Element` now forms at
Immutable-HC-after-mark (excusedM=13) and retracts only on the next ring. `Info` freezes
`@FinalFields(after=...)` because `Info.translate(TranslationMap)` is its ONE unexcused abstract
method -- the union is blocked by exactly `MethodInfoImpl.translate` and `TypeInfoImpl.translate` --
and that weak formed verdict sinks every `*Info` interface through `isMutable(@FinalFields)`: a
formed-but-weak supertype verdict is strictly worse than none, which would get the optimistic HC
seed. (`Statement` and `Factory` freeze the same weak-FF shape and sink Block/StatementImpl/Runtime
identically.) Site trace (`EC_SITE_DEBUG=MethodInfoImpl.translate`): the enm walk bails at
`transition bail: MethodInfo.builder()` with poisoned locals `methodInfo`/`builder`/`newPi` -- the
fresh object arrives via the same-class helper
`copyAllButBodyParametersReturnTypeAnnotationsExceptionTypes`, a factory-method return the
constructor-call-only freshness fixpoint cannot see. **Next quest: factory-method freshness** -- a
call to a same-class helper that provably returns a locally-constructed object is fresh at the call
site, labels = the helper's enm labels plus the argument labels; it unblocks `Info.translate` and
with it the `*Info` interface family. A second open design point exposed en route: whether a
formed-but-weak (@FinalFields) eventual verdict on a still-circular cluster member should keep its
subtypes from the optimistic seed, or whether `immutableSuper` should stay seeded (witnessed) until
the contraction settles it. Secondary roots, re-measured: `BinaryOperatorImpl`(11),
`MethodInspection`(10), the markless expression sub-interface ring
(IntConstant/BooleanConstant/Divide/...), `DependentVariableImpl`, `FieldInspection`.

## Factory-method freshness (2026-07-31, same session): Info.translate lands — survivors 42

The quest above, implemented (all gated; `TypeEventualAnalyzerImpl`):

- **`freshReturn(MethodInfo)`** -- purely syntactic, positive-cached, recursion-refusing, depth-capped
  (3): a callee provably returns an object constructed in its own body when every return expression is
  a constructor call, a chain/local rooted in one (the callee's own pass-1 freshness fixpoint, with the
  factory clause), or a nested factory call; an ABSTRACT callee is a factory when every known
  implementation is. CST-only, so the cache needs no rederivation reset. Lambdas/anonymous classes are
  not descended into (their returns are not the method's; effective finality bars them from assigning
  its locals).
- **Consumption**: the pass-1 freshness fixpoint and both transition-bail sites accept factory calls
  (`rootedInFreshOrFactory`), so `T t = copyAllBut(); t.builder()…commit()` and the inline chain both
  read as the fresh copy's lifecycle; the handed-on gauntlet is skipped for a factory value exactly
  like for a fluent fresh chain. A factory call ON THE BARE ROOT additionally owes the root's full
  commitment (`rootCommitmentLabels`, the wrapper-capture precedent one level up) -- the fresh object
  embeds content the callee read from the root, which argument labels cannot see. Care point learned
  on the dogfood: when the root never fully commits (`rc == null`) the fold must FALL THROUGH to the
  ordinary gauntlet, not bail -- a hard bail there silently took down the whole statement print family
  (opaque-sink route) in the first attempt, enm 908 -> 898; with the fall-through the change is purely
  additive, enm 908 -> 916, zero losses.

**The `Info.translate` union LANDS** (`enm=[compilationUnitOrEnclosingType, inspection, owner, type,
typeInfo]`), with `MethodInfo.translate`, `withMethodBody`, and both `copyAllBut…` helpers.
**Composed scoreboard: survivors 42, retracted ~155, enm 916, eup 409** -- `Element`, `Comment`,
`MethodInfo`, `ParameterInfo` all leave the never-form roots. Determinism: survivor set and enm layer
byte-identical across two runs (the retracted COUNT wobbles 155/154 -- a pure-cascade member, the
documented benign wobble). Gate-off Fernflower A/B: identical modulo the two documented ctor-flake
lines. Suites: analyzer 272/0 (`TestCommitLabels` INPUT_FACTORY: bare-root factory + inline chain +
the returns-this negative; gate-off twins). One INPUT_FACTORY lesson: an inline
`copyAllBut().commit(s)` chain is already PLAINLY non-modifying (the plain layer tracks constructor
freshness through the chain) -- the pin needs a `content()` read to keep the method modifying so the
enm walk, not the plain layer, carries it.

**Remaining roots, re-measured (in lean order): `StatementImpl`(24), `Runtime`(23), `Block`(21),
`MethodInspection`(12), `BinaryOperatorImpl`(11), `DependentVariableImpl`, `FieldInspection`, the
constant-expression ring (`IntConstant`/`BooleanConstant`, 5 each).** `Info` and `TypeInfo` now form
(cascade victims only). The statement family (`StatementImpl`/`Block`) and the `Runtime`/`Factory`
weak-FF caps are the next quests.

## The ZipLists contract (2026-07-31, same session): the statement family forms — survivors 55

The `StatementImpl`/`Block` root decoded in one site trace: `Statement.withBlocks(List)` was the ONE
unexcused abstract method on `Statement`, its union blocked by exactly
`SwitchStatementNewStyleImpl.withBlocks` and `TryStatementImpl.withBlocks` — both of which hand their
container field (`entries`, `catchClauses`) to **`ZipLists.zip(List, List)`**, a maddi-util JAR static
with no contract: no `UNMODIFIED_PARAMETER`, so the argument-position container ride-along refuses,
and the zip-stream receiver chain stays uncommitted. The fix is the `Either.isLeft`/EFOD precedent —
contract the leaf: `zip` is `@NotModified` with `@NotModified @Independent(hc=true)` parameters and an
`@Independent(hc=true)` return (it only iterates; the stream shares nothing but elements), mirroring
`ListUtil.joinLists` in the same module.

**Composed scoreboard: survivors 42 -> 55, retracted 141, enm 918, eup 409.** The
`Statement.withBlocks` union lands (`enm=[entries, expression, initializer, initializers, selector,
updaters]` + eup on `tSubBlocks`), zero unexcused `withBlocks` remain, `Block` forms
`@FinalFields(after=…)`, and `StatementImpl`/`Block` leave the roots. Determinism: survivor set, enm
layer and retracted count all identical across two runs. Gate-off Fernflower A/B: **0-line diff**
(the corpus never has maddi-util on its analyzed classpath). Analyzer suite 272/0.

*Operational note:* the dogfood `inputConfiguration.json` still references `maddi-util-0.8.2.jar`
(generated 2026-07-23) while the build now produces 0.9.0 — the 0.8.2 file was refreshed in place for
these runs; regenerate the configuration per `dogfood/README.md` at the next opportunity.
**Closed 2026-08-17**: the pins now follow `gradle.properties`, and `TestEventualRatchet` fails on a
configuration generated at any other version, so this cannot go stale silently again.

**Remaining roots: `Runtime`(23) — sunk by `Factory`'s unconditional `@FinalFields`, whose two stuck
methods (`commonType`, `newInlineConditional`) both funnel into the wrapper-capture shape
`new CommonType(this).commonType(pt1, pt2)` where `CommonType.commonType` carries the recorded
unwritable ∅-enm (world-graph modification through its `runtime` field) — the wrapper fold has no
promise to translate. Then `MethodInspection`(12), `BinaryOperatorImpl`(11), `DependentVariableImpl`,
`FieldInspection`, the constant-expression ring.**

## The independence seed (2026-07-31, same session): the constant ring forms — survivors 63

Three coupled mechanisms (all gated), each found by one diagnostic hop from the previous:

1. **Immutable-hc carrier fields.** `CommonType.commonType`'s honest label is `[runtime]`, but
   `Predefined` (plain `@Immutable(hc=true)`, zero modifying methods) is neither eventual nor a
   candidate, so `fieldHoldsCommittableContent` refused it — and once its verdict decided, the
   harmless shortcut at the TOP of `commitLabels` swallowed the field read into the unwritable ∅.
   Fixes: `isEventuallyImmutableFieldType` accepts an unconditionally immutable-hc type WITH hidden
   content, a fortiori (the contraction's discharge rule at the excuse site; deeply immutable stays
   label-less), and the top-of-walk harmless shortcut no longer preempts a root-scoped FIELD read
   (committability first, the `rootCommitmentLabels` ordering principle — the field branch re-checks
   harmlessness for label-less fields, so String fields still yield ∅). `CommonType.commonType` lands
   `enm=[runtime]`; the wrapper fold translates it into the full `FactoryImpl` commitment on both
   abstract `Factory.commonType`/`newInlineConditional`; **`Factory` forms at `@Immutable(hc=true)`**.

2. **The independence-side cluster seed.** The next ring (`ConstantExpression`, `BinaryOperator`,
   `IntConstant`, `BooleanConstant`, `DependentVariableImpl`, `BinaryOperatorImpl`) was capped by
   after-mark independence `@Dependent` — the recorded under-report, now binding: `computeImmutableType`'s
   dependence cap turns it into FinalFields-after-mark, and `isMutable(@FinalFields)` spreads the sink.
   `TypeIndependentAnalyzerImpl` now takes the `EventualCluster`, and `excused()` gains the wider
   after-mark form: under the joint transition, clause 2 is the load-bearing one for ANY exposure, not
   just a before-mark-only method's — a dependent accessor callable after the mark
   (`ConstantExpression.rewire()` exposing `Expression`) shares content committed once the exposed
   type's own marks pass, and a still-circular exposed type is accepted through the witnessed seed
   exactly as in `immutableSuper`. Dependent FIELD exposures get the same excusal.

3. **Pure type-parameter exposures.** The last cap of the ring: `ConstantExpression.constant()`
   returns `T` with a `@Dependent` method verdict — but a pure type-parameter exposure is hidden
   content by definition, precisely what immutable-hc permits; `excused()` accepts it outright
   (after-mark mode). With that, `ConstantExpression` and the whole constant ring reach
   `@Immutable(hc=true)(after=…)` and SURVIVE.

**Composed scoreboard: survivors 55 -> 63, retracted 148, enm 923, eup 411, zero enm losses at every
step.** Determinism: survivor set and enm layer identical across two runs. Gate-off Fernflower A/B:
the one documented `StatEdge.EdgeType.<init>` flake line. Analyzer suite 272/0. New diagnostic:
`TypeIndependentAnalyzerImpl` joins the `EC_TYPE_DEBUG` family, printing the exact
field/method/parameter a DEPENDENT verdict roots in ("DEPENDENT: method constant returns Type param
T" was the decisive print).

**Remaining roots, re-measured: `Runtime`(22 — still markless, waiting on the `Factory` retraction
chain), `MethodInspection`(12), `FieldInspection`(5), `SumImpl`(5), `UnaryOperatorImpl`(4 —
`hashCode()` + ctor unlabeled), the `YieldStatement/ThrowStatement/SwitchStatementOldStyle` impl
tail (3 each).**

*The `MethodInspection` blocker, diagnosed precisely (next session's first design question):*
`MethodInspectionImpl.Builder.fullyQualifiedName()` is `fullyQualifiedName.isSet() ? …get() :
<compute from parameters>` — deliberately callable on BOTH sides of the transition (the
sv-reconstruction fix that answers the real name meanwhile). The one-sided `computeEventual`
propagation rule sees the `SetOnce.get()` call and stamps the method `@Only(after=
"fullyQualifiedName")`; the abstract `MethodInspection.fullyQualifiedName()` inherits it; and an
`@Only(after)` method that is (pre-mark-)modifying is correctly NOT excusable at type level — so the
interface caps at FinalFields. The honest classification is "no side" (the marked call does not
DOMINATE: the `isSet()` guard provides a before-path), which would free the method for the enm layer
to label instead. The propagation step needs a dominance condition — conclude a side only when every
live path runs the marked call — without breaking the legitimate single-statement `return f.get();`
forwards.

## The dominance quest (2026-07-31, same session): entry-state witnesses — survivors 67

Frequency check first (why this is a general mechanism, not a one-off): the both-sides guard is a CORE
idiom of the eventual style — the support classes are built of it (`SetOnce.getOrDefault`/
`getOrDefaultNull` ARE the guarded fallback, contracted away inside the jar), and user code reproduces
it wherever the canned accessor does not fit: ~50 transition-state-test guard sites across 12 maddi
modules by a crude grep (undercounting the engine modules, which spell it `haveAnalyzedValueFor`). Of
the 11 `@Only(after)` classifications on the dogfood, THREE distinct false-contract families turned
out to hide in them. And the engine already carried four ad-hoc live-path disciplines
(`computeTestMark`'s strict body, `scanPreconditions`' early-exit guards, `GetSetHelper`'s inert
prefix, `freshReturn`'s all-returns rule) — this quest is their unification applied to the marked-call
propagation.

**The mechanism (`SideWalk`, gated; the gate-off visitor is verbatim, pinned).** A sided call
classifies its caller only when it witnesses the method's ENTRY state: it must sit on the **spine**
(every live path — not in a branch, loop, lambda, catch/finally, ternary arm or short-circuit tail,
and not after a statement that can exit the method alive), and none of its labels may be **tainted**
by an earlier — even conditional — `@Mark` (the ensure-then-read `if (!isSet()) commitParameters(); …
get()` must not read as `@Only(after)`). Conditional regions contribute no sides but still taint; a
spine `@Mark` contributes AND taints, so `f.set(x); f.get();` reads `@Mark`, not mixed sides.
`scanPreconditions` is untouched — its guards conclude by the other dominance argument (early exit).

**Result: the classification diff is surgical — 7 removed, 0 added, 0 changed — and every removal is
a verified false contract:**
- `Builder.fullyQualifiedName()` + abstract `MethodInspection.fullyQualifiedName()` (guarded fallback);
- the 4 `CompilationUnitPrinter*.print` `@Only(after="compilationUnit")` — the module-info branch
  answers BEFORE `types()` is set (its own comment says so: "has to be answered before anything that
  walks types()");
- `TypeInfo.hierarchyNotYetDone()` `@Only(before)` — a short-circuit guarded fallback
  (`!hasBeenInspected() && builder().…`), callable both sides.

**Composed scoreboard: survivors 63 -> 67** (the `MethodInspection`/`MethodInspectionImpl` pair — the
12-lean root — and the `CompilationUnitPrinter` pair join), retracted 147, enm 929 (+6: the freed
methods land enm labels instead). Determinism: survivor set, enm layer AND classification layer
identical across two runs. Gate-off Fernflower A/B: the one documented ctor flake line. Analyzer
suite 274/0; `TestEventualDominance` pins the five shapes (honest forward, ternary fallback,
early-return fallback, ensure-then-read, spine set-then-get) with a gate-off twin pinning the old
behaviour, so ungating stays a deliberate step.

**Ungating candidate.** The gate-off visitor still writes the false `@Only(after)`/`@Only(before)`
contracts, which the decorator ships to the IDE. The corpus never exercises the propagation (no
eventual types), so ungating costs only a gate-off dogfood delta that is line-by-line justified
(strictly-better contracts). Left gated for now; flip after the standard three-corpus A/B.

**Remaining roots after this quest: `Runtime`(cascade), `FieldInspection`, `SumImpl`,
`UnaryOperatorImpl.hashCode`, the `YieldStatement/ThrowStatement/SwitchStatementOldStyle` impl tail.**

## The missing disclaimers (2026-07-31, same session): the statement family completes — survivors 72

The statement-impl tail decoded into the same two §050 idioms already solved elsewhere, each with one
member the earlier rounds missed:

1. **`UnaryOperatorImpl.hash`** — the lazy hashCode memo, the exact `VariableImpl.cachedHash` idiom;
   the `@IgnoreModifications` slot disclaimer applied (cst-impl annotation, no engine change needed —
   the memo-disclaimer machinery landed in the VariableImpl quest). `hashCode()` flips to plainly
   non-modifying.
2. **`StatementImpl.propertyValueMap` and `TryStatementImpl.CatchClauseImpl.propertyValueMap`** — the
   TWO analysis stores in cst-impl without the `@IgnoreModifications` every other store carries (audit:
   9 store fields, 7 annotated). The new `TypeIndependentAnalyzerImpl` DEPENDENT-provenance print
   found it in one shot: `StatementImpl DEPENDENT: field propertyValueMap` — the store held the entire
   statement family at FinalFields-after-mark THROUGH THE INDEPENDENCE LOOP (the immutability loop's
   ungated skip never applied there). Companion engine fix, ungated like its twin:
   `loopOverFieldsAndAbstractMethods` skips an `@IgnoreModifications` field — hidden content's
   independence does not bear on the type's; a no-op wherever no field carries the annotation.

**Composed scoreboard: survivors 67 -> 72** (Break/Continue/BreakOrContinue/Empty/Import statement
impls join; `api.expression.BinaryOperator` lifts from `@FinalFields(after=)` to
`@Immutable(hc=true)(after=)`), retracted ~164 (wider formation before contraction), enm 929, zero
losses. Determinism: survivor set + enm layer identical across two runs (retracted 164/163, the
benign cascade wobble). Gate-off Fernflower A/B: the one documented ctor flake line — the ungated
independence skip is corpus-inert as argued. Suites: analyzer 274/0, cst-impl 73/0.

**Remaining roots, re-measured: `Runtime`(22, pure cascade), `FieldInspection`(5),
`SumImpl`(5), then a ring at 3 (`LocalVariableCreationImpl`, `AnnotationExpressionImpl`, and the
markless expression sub-interfaces `Negation`/`EnclosedExpression`/`BitwiseNegation`).**

## The seed completion (2026-07-31, same session): Runtime survives — survivors 75

Two symmetric gaps, closed (both gated):

1. **The `independentSuper` seed.** `immutableSuper` has had the still-circular-candidate seed since
   the prototype; its independence twin did not — a sub-impl's after-mark independence fell to the
   super's honest unconditional `@Dependent` through the hierarchy min, and the dependence cap froze
   `SumImpl`, `LocalVariableCreationImpl`, `AnnotationExpressionImpl` (and the eval family) at
   MUTABLE-after-mark. `independentSuper` now contributes independent-hc for a witnessed candidate
   supertype, exactly as `immutableSuper` contributes immutable-hc.

2. **The a-fortiori skip in Parts A and B.** `ExpressionWrapper` — a MIXIN outside the cluster,
   unconditionally `@Immutable(hc=true)` — failed Part B's admissibility bail and kept the whole
   `Negation`/`EnclosedExpression`/`BitwiseNegation` ring markless. An unconditionally immutable-hc
   supertype (Part B) or subclass (Part A) contributes no labels and blocks nothing — the same
   discharge rule the contraction and `isEventuallyImmutableFieldType` already apply.

**Composed scoreboard: survivors 72 -> 75, and `api.runtime.Runtime` — the 22-lean root — SURVIVES**,
with the negation ring and `EvalCast`/`EvalInstanceOf`/`EvalRemainder`. The retraction-root list is
down to `FieldInspection`(5), `EvalNegation`(2) and `Value.*` singletons; the CompilationUnit
foursome retracts as pure cascade (`broken: []`). Determinism: survivor set + enm layer identical
across two runs. Gate-off Fernflower A/B: **0-line diff**. Analyzer suite 274/0.

**The last named circle, characterized for next session:** `FieldInspectionImpl` forms and retracts
leaning ONLY on its interface `FieldInspection`, which is markless with legitimately label-less
supers (`Inspection` is unconditionally hc) and no enm-carrying methods of its own — the missing
direction is IMPLEMENTATION -> INTERFACE label inheritance at type level (Part A covers
subclass -> abstract class; the interface twin does not exist yet). Everything else in the ledger is
cascade behind it.

## SideWalk UNGATED (2026-08-01)

The dominance discipline is now the only side-collection path (the historical one-sided visitor is
deleted; `scanPreconditions` remains gated as before). Validation per the golden rule:

- **Gate-off dogfood delta: exactly one line** — `MethodInspectionImpl.Builder.fullyQualifiedName()`
  loses its false `@Only(after="fullyQualifiedName")` (32 -> 31 classifications; the other six of the
  seven false contracts only ever arose under gated machinery). Line-justified: strictly-better.
- **Three-corpus A/B:** Fernflower **0-line**, Langchain4j **0-line**, Timefold 4 lines all within
  the documented `testdomain.*Solution` wander — and provably unrelated: all three corpora carry
  **zero** eventual classifications (`grep -c 'eventual=@'` = 0 on both sides), and the ungated code
  can only influence `EVENTUAL_METHOD`.
- Analyzer suite 274/0; `TestEventualDominance`'s gate-off pin now asserts the dominance outcomes on
  both gates.

## Part A'' and the exposure excusals: the cluster closes — survivors 254 (2026-08-01)

The `FieldInspection` circle was the keystone. Three pieces (first two gated, third annotation-driven
and corpus-inert):

1. **Part A'' — implementation -> INTERFACE label inheritance** (`knownImplementors` in the cluster,
   the interface twin of Part A): a markless interface inherits the shared transition of its analyzed
   direct implementors; setter-bearing implementors (the Builders, the before-state face) are skipped.
   **Fired eagerly this minted implementor labels into write-once verdicts in iteration 1 and crashed
   survivors 75 -> 32** — restricted to the TERMINAL phase and cluster candidates, it is clean.
2. **The disclaimed-accessor excusal** in the independence loop: `analysisOfInitializer()` returning
   the `@IgnoreModifications` store is hidden-content sharing, not dependence — the independence twin
   of the eventual walk's `isIgnoreModificationsAccessor`, consulting IMPLEMENTATIONS for abstract
   accessors. Ungated (annotation-driven, corpus-inert).
3. **The container-exposure clause** in `excused()`: `fieldModifiers()` exposing `Set<FieldModifier>`
   — a dependent exposure of a container whose every type parameter is excusable (eventual,
   immutable-hc, or witnessed candidate) is the ride-along carrier the mark labels already name.

**Composed scoreboard: survivors 75 -> 254, retracted 176 -> 2** — the greatest fixpoint closes over
essentially all of cst-api/cst-impl: the full `Expression`/`Statement`/`Element`/`Info` hierarchies
(interfaces AND impls), the eval engine, the printers, the variables, `ParameterizedType(Impl)`,
`Runtime`/`Factory`/`PredefinedImpl`. Flagship levels are the meaningful ones: `Expression`,
`Statement`, `Info` at `@Immutable(hc=true)(after=…)`, `TypeInfoImpl` after
`compilationUnitOrEnclosingType,inspection`. The TWO remaining retractions lean on `EvalNegation`
(never forms — the last named holdout). Determinism: 254 = 254 modulo ONE boundary type
(`ImportComputer.ImportDetails` flips in/out — the verification-residue boundary, the documented
`CompilationUnitPrinterImpl` precedent). Suite 274/0; gate-off Fernflower A/B: **0-line diff**.

What remains for retraction-0 and full stability: the `EvalNegation` holdout, the one-type
determinism wobble (tied to the residue story), and the standing soundness backstop — the witnessed
contraction — stays on until the corpus A/B ungating of the whole cluster.

## THE UNGATING (2026-08-01): default-on, and the honest reckoning

`EVENTUALCLUSTER` and `MODREACH` are now **default-on** (`=0` opts out; `EvalNegation.negationCache`
got its memo disclaimer en route). Running the full unit suite default-on immediately triggered the
soundness pin `TestEventualPropagation.test7` (the leaked-ArrayList shape) and exposed that **the 254
rested substantially on two optimisms that bypassed the witnessed ledger**:

1. **The raw-container after-mark skip**: ride-along container fields entered `AfterMark.fields()`
   and the independence loop skipped them wholesale — but a raw wrapper is frozen by no mark, and if
   it escapes through a dependent accessor, a pre-mark caller mutates our state post-mark. Fixed: the
   skip re-checks its original premise (field type eventual/candidate/hc), with a **verification
   arm** — `fieldWrapperProvablyImmutable`, every write a `copyOf`/`of`-family expression — as the
   sound alternative for copy-backed wrappers.
2. **The independence floor trusted a cycle-broken unconditional**: `INDEPENDENT` written by cycle
   breaking is optimism, not a verdict; flooring after-mark independence on it promoted test7's
   shape. Fixed: the floor uses the honestly recomputed unconditional
   (`independentAfterMark(NONE, false)`), null meaning no floor.

Also landed: `contractedIndependentHc` — the TRUSTED-LEAF route
(docs/eventual-design-improvements.md §4): a hand-written `@Independent(hc=true)` on
`FieldInspection.fieldModifiers()` (Set.copyOf-backed, uncomputable from the declared type) is read
through the ContractReader in the independence loop; and `ignoreModificationsAccessor`, the
independence twin of the eventual walk's disclaimed-store excusal. One legitimate re-pin: a leading
`assert <state test>` now classifies `@Only(after)` by the (default-on) precondition shapes.

**The honest scoreboard: default-on gives 24 sound survivors** (deterministic, identical set across
runs; suite 274/0 with the soundness pin passing) — down from the optimistic 254, up 6× from the old
default (~4). The 230 in between are real but *unproven*: their two supports must be rebuilt on
witnessed/verified ground. The named reconstruction levers, in order: (a) the cst-impl `copyOf`
discipline (conformance rule 4) so `fieldWrapperProvablyImmutable` fires for the comment/annotation
ride-alongs — NOTE a first attempt (copyOf in the `StatementImpl`/`ExpressionImpl` base ctors) made
things WORSE (24 → 10: the ternary in the hot base ctors perturbs more walks than the arm recovers)
and was reverted; the sweep needs the call-site (Builder) end instead; (b) trusted-leaf
`@Independent(hc=true)` contracts on the copy-backed accessors, the `fieldModifiers` route, family by
family; (c) honest unconditional independence for the interfaces.

**Corpus behavior under the new defaults** (this is a semantic change, deliberately commissioned):
Fernflower **passes** (roll-call 1/0/0) in 651 s (~5.4× the gate-off 120 s — the shadow-pass cost at
corpus scale; a performance follow-up). The verdict delta vs gate-off is 536 lines and matches the
documented modreach signature: methods 59 strengthened (the FALSE→TRUE reverse upgrades) vs 36
honestly weakened (`copy()`/`iterator()` reachability corrections), types 4↑/13↓ following their
methods. And one first: `TargetInfo.LocalvarTarget` in Fernflower's own unannotated code is computed
`@Immutable(hc=true)(after="table")` — the machinery generalizing beyond maddi. Timefold and
Langchain4j under the new defaults are still to be run and classified the same way before this is
considered fully validated.

## The enforcement round (2026-08-01): the ratchet, the conformance rules, and what they caught

Implements `docs/eventual-design-improvements.md` §§1–3. The headline is not the machinery — it is
that the machinery earned its keep within an hour of existing, twice.

### The ratchet (§1)

`TestEventualRatchet` (maddi-run-openjdk, `@Tag("slow")`) re-derives the surviving
`EVENTUALLY_IMMUTABLE_TYPE` set from a composed dogfood run and diffs it against
`dogfood/expected-eventual-survivors.txt`, with `dogfood/eventual-survivor-wobble.txt` for boundary
nondeterminism. Three things are deliberate:

- **The baseline is 24, not the doc's 254.** The design note was written before the ungating; the 254
  did not survive the assumptions ledger. A ratchet pinned to numbers that were never sound would
  fail on its first honest run and be deleted as noise.
- **It builds its own pipeline rather than calling `RunAnalyzer`**, so `MODREACH` and
  `EVENTUALCLUSTER` are set programmatically. Both are environment *opt-outs* now; a developer with
  either exported to `0` would otherwise measure a different engine and read the difference as a
  regression.
- **It reads the property, never an FPDUMP.** The dump is a diagnostic and free to change format.
- It fails, loudly, when the dogfood input configuration is missing — the vacuous-green failure mode
  `AGENTS.md` §Commands warns about.

**Known scope limit, now stated in the test and the baseline header:** cst-api / cst-analysis /
cst-impl are analysed as source and are what the ratchet defends. maddi-support and maddi-util arrive
as **jars pinned at 0.8.2 while the project is at 0.9.0**, so a change to either does not reach the
run and reads as "no change". This is why the maddi-util contracts added below are, as of this
commit, *unmeasured on the dogfood*: they are inert until the pins are bumped, the plugin re-published
and the input configuration regenerated — at which point the baseline must be re-derived, since the
jars moving forward will move verdicts with them.

**Closed 2026-08-17.** The pins now read the project version out of `gradle.properties`
(`dogfood/settings.gradle.kts`), and `TestEventualRatchet.assertCoverage` *fails* rather than logs when
the input configuration was generated at any other version — the disclaimer was doing no work, because
the file it describes is generated, uncommitted, and therefore whatever the developer last produced.
The re-derivation at 0.9.0 returned **the same 285 types, membership identical**: the maddi-util
contracts predicted above to "move verdicts with them" moved none. Worth recording as the negative
result it is — the prediction was that bumping the pins would shift the baseline, and it did not.

### The conformance rules (§2)

`TestEventualConformance` lives in **maddi-inspection-openjdk**'s ordinary `test` task, not in
cst-impl: the rules that matter need method *bodies*, so they need maddi's own `JavaInspector`, which
cst-impl's test source set does not have. It parses maddi's own sources, mirroring
`TestJavaInspector6MultiProject`.

Two things had to be got right before the rules said anything useful:

- **Source sets must be named after the jar they resolve to.** The name is not cosmetic — the
  automatic module name derives from it, and a hand-written `"annotations"` leaves the module
  unresolvable, whereupon annotation types fail to convert and the scanner dies with an NPE inside
  `ClassSymbolScanner` rather than reporting an unresolved import. The helper derives the name from
  the artifact URI, which also survives the version bumps a hard-coded `annotations-26.1.0.jar` does
  not.
- **Rules 3 and 4 need a scope, or they drown.** Applied to all of cst-impl they produced 28
  violations, ~20 of them deliberately mutable services (`QualificationImpl` accumulates print state,
  `ImportComputerImpl` accumulates imports, `IsAssignableFrom` memoizes). Those never become part of
  an `Element`, so an adder on them costs the analysis nothing. The scope is now computed: everything
  implementing `Element`, closed over its fields' declared types *and* over implementors of anything
  in scope — the latter because `isMutable(@FinalFields)` is exactly a downward rule. 206 cst-impl
  types, 107 in scope. `ModuleInfo.Provides extends Element`, so the `ProvidesImpl` incident that
  motivated the rule is in; `MethodMapImpl` comes in as the field type of `TypeInspectionImpl`.

Rule 1 (every `PropertyValueMap` store disclaimed) came back **clean** — the audit that closed it last
session holds. The rules found four real things: `AndImpl.hash` and `OrImpl.hash` (lazy memos without
the disclaimer, the `UnaryOperatorImpl` shape), `MethodMapImpl`'s final `HashMap` filled by `put()` in
its constructor (the `precedenceMap` shape), and three uncontracted maddi-util statics
(`ListUtil.joinLists`, `MapUtil.compareMaps`, `MapUtil.nice` — the `ZipLists.zip` gap).

### What the ratchet caught: a weak verdict is worse than none, measured

Applying those fixes dropped the dogfood from **24 survivors to 10** — the entire statement family.
The bisect is the interesting part, and it exonerated the obvious suspects: reverting `MethodMapImpl`
alone left it at 10; the engine change of §3 *alone* measured 24. The cause was the two-line
`@IgnoreModifications` on `AndImpl.hash` / `OrImpl.hash`.

The mechanism is the rule already recorded in this document, now with a price tag on it. Before the
disclaimer, `AndImpl` had a non-final field, no verdict, and therefore the **optimistic HC seed**.
With it, all fields are final-or-disclaimed and the type forms `@FinalFields` — but only
`@FinalFields`, because `expressions` is assigned straight from a constructor parameter and is not a
provably immutable container. That weak formed verdict *replaces* the seed, and every statement type
leaning on the `Expression` union goes down with it.

The fix is the reconstruction lever this document named after the failed base-constructor sweep:
**apply the copy at the call-site end.** `AndImpl.Builder.build()` already committed
`List.copyOf(expressions)`; the two public constructors did not, and they are what most callers use.
Copying there makes the verdict no longer weak, and disclaimer + copy together measure **24,
identical to baseline**. Neither half is shippable alone: the disclaimer alone is a 14-type
regression, and that is precisely what a week of dogfood archaeology used to cost.

The general lesson, sharper than before: **a correct idiom applied halfway is a regression.** An
`@IgnoreModifications` that moves a type from "no verdict" to "weak verdict" must be landed together
with whatever gets it the rest of the way, or not at all. This is also why conformance rule 4 is
folded into rule 3 as "no in-place mutation of a final collection field" rather than the doc's
"constructors must assign defensive copies": the latter, applied to the hot base constructors,
is the sweep that measured 24 → 10 last session.

### The support types (§3)

`io.codelaser.maddi.support.Memo<T>` and `IntMemo`, both carrying a **class-level** `@IgnoreModifications`
(`TYPE` added to the annotation's `@Target`), plus the engine rule that makes it pay: a field whose
*type* carries the class-level disclaimer is materialized as if the field itself were annotated
(`SourceContractMaterializer.materializeIgnoreModificationsFromFieldType`). Writing it into
`analysis()` — rather than special-casing it at each of the dozen read sites — is what makes every
consumer agree. This is not the rejected "skip `PropertyValueMap` by type" hack: that keyed on a type
its author never marked; here the disclaimer is written, deliberately, on the class whose whole
purpose it is. `IntMemo` also fixes a latent bug the hand-written slots have: a computed hash of
exactly 0 is stored as 1, so it is computed once rather than on every call
(`VariableImpl.hashCode` does this by hand; `UnaryOperatorImpl.hashCode` did not).

**Nothing is migrated to them yet, on purpose.** Every memo slot that exists today —
`VariableImpl.cachedFqn`, `VariableImpl.cachedHash`, and the three `hash` slots — is on the analysis
hot path, and a `Memo` costs one object allocation per CST node where a bare field costs none. With a
5.4× corpus slowdown already on the follow-up list, that is the wrong direction to take on
speculation. The mechanism is in place and measured neutral (24 survivors, dogfood); migrating any
particular slot is now a measurement, not a design question.

### The corpus A/B, and a second halfway-idiom

The engine rule of §3 is ungated, so the golden rule applies. Fernflower, default-on, three runs:

| comparison | differing elements |
|---|---|
| A (HEAD, fresh) vs BD (HEAD, previous session) | 2 — `MergeHelper.matchWhile` and `StatEdge.EdgeType.<init>(int)`, both `nonModifying` flips |
| A (HEAD, fresh) vs B (this change set) | 1 — `ConstantPool.pool`, `independent` `@Independent` → `@Dependent` |
| A (HEAD, fresh) vs B2 (change set + the `hasBeenInspected` guard below) | 0, modulo those same two flakes |

The first row is the point of running A twice: two runs of the *same* tree differ by two elements, so
`MergeHelper.matchWhile` joins `StatEdge.EdgeType.<init>(int)` and `Exprent.<init>(int)` in the known
`nonModifying` flake family. Against that noise floor, the change set had exactly one real effect.

One is not zero, and on a corpus containing **no e2immu annotations at all** a rule that fires on
class-level `@IgnoreModifications` should have been provably inert. It was not, and the reason is worth
recording: `TypeInfo.annotations()` goes through `EventuallyFinalOnDemand.get()`, which **runs the lazy
byte-code loader**. Asking every field's type for its annotations inspects types the analyzer would
never otherwise have looked at, and what is inspected changes what is known when a verdict is computed
— here, conservatively, on one field.

So the rule now tests `hasBeenInspected()` first, and the ordering is load-bearing rather than an
optimization; with the guard the corpus diff is the flake pair and nothing else (third row above),
which is the byte-identical outcome the golden rule asks for. This is the §5 trade-off ("lazy inspection … the reason the enm layer had to exist")
biting a caller that merely wanted to read an annotation. **The general form is the same lesson as the
`AndImpl.hash` regression above: on this CST, reading is not free.** Anything that walks types outside
the analyzer's own order must either prove it is not forcing inspection, or measure a corpus A/B and
be prepared for the answer.

## The bistability investigation (2026-08-01): the dogfood is a coin flip

**Symptom.** The composed dogfood run is NOT deterministic: across identical invocations at the same
commit (`12a84540`), the survivor count flips between **24** (the ratchet's baseline: the statement
family forms) and **10** (it never forms), roughly 50/50. Every "deterministic, identical across two
runs" claim in this document was sampling luck from 2-run checks; the enforcement round's ratchet is
pinned to a coin flip. Established with 40+ measured runs, direct `installDist` CLI (no Gradle).

**The two worlds, precisely.**
- The FPDUMPs differ ONLY in the 14 statement-family type lines; every method-level verdict is
  identical. Per-iteration property-change counts are identical through iteration 17 and fork at 18
  (585 vs 570).
- The eventual-cluster ledger (`EC_ASSUME_DEBUG=org.e2immu`) of the 10-world is a strict SUPERSET:
  22 extra statement-family assumption edges (`BlockImpl -> Block`, `TryStatementImpl ->
  TryStatement`, `X -> StatementImpl`, ...) that end up undischarged, so the contraction cascades the
  family away. The same edges appear in both worlds at DIFFERENT iterations (it=20 vs it=28...) —
  the whole trajectory shifts.
- Persisted analysis output is essentially deterministic PER WORLD (two 24-world runs: byte-identical
  result dirs). Cross-world, `methodLinks` on several statement `translate()` methods and `links` on
  `ExplicitConstructorInvocationImpl` fields flip PRESENCE — the write-vs-nested-shallow-not-written
  distinction of the link computer's recursion prevention.
- Upstream of everything: the link engine's per-method work/witness counts vary between EVERY pair of
  runs (`-Dmaddi.workReport=1`; e.g. `MethodPrinterImpl.print` witnesses 16146 vs 16610 on an
  IDENTICAL final closure of 14914 facts) — exploration-order noise from iteration 1 onward, mostly
  harmless, occasionally tipping the recursion-arrival pattern.

**Exonerated by experiment** (each still bistable): `-ea` on/off; `EC_RETRACT_DEBUG`; `--parallel`;
`PARALLEL=1` (verified: zero pool threads, all `[main]`); the prep phase (three prep-only runs:
byte-identical output); Gradle itself (direct CLI reproduces); the JDK `ImmutableCollections` salt
(a `SALTPROBE` in Main: SAME salt order gave both outcomes); `-XX:hashCode=4` — NOTE this mode is
ADDRESS-based, so those runs prove nothing about identity hashing; C1-only JIT + SerialGC; pure
interpreter `-Xint` + SerialGC; `-XX:hashCode=3` (counter) WITH JIT (JIT threads may perturb the
counter, so also not conclusive); filesystem enumeration order (stable md5 across runs). No
`identityHashCode`, no clocks/randomness in any analysis module (grep-verified), no weak/soft refs,
no caught StackOverflowError (fault-tolerance catch sites log; logs clean).

**Found and fixed en route (kept regardless — each closes a real order-sensitivity hole):**
- `WitnessIndex`'s own comment records the PRIOR round of this same disease ("arrival order depends
  on map iteration over identity-hashed variables (LocalVariableImpl has no hashCode override)") —
  the FQN-based `VariableImpl.hashCode` and the canonical witness tie-break were that round's fix.
- **`ParameterizedTypeImpl.hashCode` hashed the `WildcardEnum` CONSTANT — `Enum.hashCode()` is the
  identity hash and FINAL**, so every wildcard-bearing type (and every type recursively containing
  one) had a per-JVM-run hash; hash-keyed collections of types iterated in run-varying order. Fixed
  with a stable per-constant token (`stableWildcardHash`). Measured: NOT sufficient alone — still
  bistable — but exactly the disease the hashCode comment ("keep their iteration order") tried to
  prevent.
- Canonical (FQN-sorted) iteration for `MethodLinkedVariablesImpl.modified` and
  `LinkNatureImpl.pass` (were salted `Set.of`/`Set.copyOf`), creation-order snapshot for
  `VariableDataImpl.Builder.knownVariableNames()` (was salted `Set.copyOf`), and
  `LinkedVariablesImpl.merge` accumulates in a `HashMap` (FQN-hashed keys iterate identically across
  runs) instead of re-wrapping in salted `Map.copyOf`. An FQN-sorted canonical CONSTRUCTOR for
  `LinkedVariablesImpl` was additionally tried and REVERTED: it deterministically re-labels the
  §-face indices and fails `TestForEachLambda`'s ~/∩ pairing pins (4 tests) — if reinstated, those
  pins must be re-derived together with the face-minting order. All surviving changes: full fast
  suites green.
- Reading note: `LinksImpl.equals`/`hashCode` are PRIMARY-ONLY and `LinkImpl.equals` ignores the
  nature — deliberate, but they make the closure first-arrival-sensitive: whichever
  equal-by-key-different-by-content value arrives first wins. This is the amplifier that turns
  exploration noise into semantic divergence.

**Open at the time of writing:** the variance SEED is not yet identified. The decisive experiment —
`-Xint -XX:+UseSerialGC -XX:hashCode=3` (deterministic counter, no JIT threads) × 3 — is in flight:
stable ⇒ identity hash confirmed somewhere (hunt the carrier); still bistable ⇒ every JVM-level
mechanism is excluded and the seed is something genuinely exotic. Until the dogfood is deterministic,
scoreboard claims need ≥4 repeat runs, and the ratchet baseline (24) must be read as "the better of
two worlds", not a stable fact.

### The trace round (2026-08-02): event order is deterministic; INSTANCE selection is not

`LINKTRACE=<fqn substring>` (new, env-gated, zero overhead off) prints every seed, propagation step,
mirror completion and recompute of the matching method's fixpoint engine. Six traced runs of
`EvalInequality.twoTerms` — the recurring within-world wobble site — produced the sharpest fact of the
investigation:

- **All six traces are byte-identical (792 events each).** Seeds, derivations, witness-improved flags,
  recomputes: the same sequence, every run.
- **Yet the persisted `methodLinks` provenance differs**: the retained `terms[0]` DependentVariable
  instance points at source `97-…`, `119-…` or `125-…` across runs — the THREE textually identical
  `runtime.sum(terms[0], terms[1])` call sites each mint an FQN-equal `terms[0]`; which INSTANCE
  survives the dedup flips per run (TRISTABLE, matching the three sites).

So the nondeterminism is not in what the engine DOES — it is in WHICH of several `equals()`-equal
objects ends up representing the value. Instance selection among equals happens wherever a hash
structure deduplicates; the one JDK mechanism that breaks such ties by IDENTITY even when every
`hashCode()` is value-based is **`java.util.HashMap`'s treeified-bin tie-break**
(`HashMap.tieBreakOrder` falls back to `System.identityHashCode` for keys whose hashes collide and
that are not mutually `Comparable`). `Variable` is Comparable (FQN); **`Fact` (the engine's
`HashMap<Fact, Witness>` key, and `history` HashSet element) is a record and NOT Comparable** — a
treeified bin of colliding facts iterates, and resolves equal-key insertion, in per-run order. This
hypothesis survives every experiment run so far, including `-XX:hashCode=3` with JIT (VM threads
perturb the counter) — the clean discriminator (`-Xint -XX:hashCode=3`, where the counter is truly
deterministic) was twice cut short.

Candidate fixes, in order of preference: (a) make `Fact` Comparable (source, target via the vertex
comparator; label by score+symbol) so tie-breaks are value-based everywhere; (b) canonicalize
retained-instance choice at the dedup sites (LinksImpl merge keeps the lexicographically-smallest
provenance); (c) the ledger-level clamp (docs above). Note the encoded PROVENANCE wobble (97/119/125)
is semantically neutral in itself — the harm is that the same instance-selection mechanism decides
which methods' links get written vs nested-shallow, which is where the 24↔10 worlds fork.

### The retention round (2026-08-02 evening): the fork, caught in the act

The overnight campaign's phase A delivered the cross-world retention-trace pair on its fifth run
(A1=24-world, A5=10-world; streams of 138 602 events each, diff = a handful of lines):

- **First divergence:** the first-time `methodLinks` write of `Expression.<init>(int)` occurs ~700
  events earlier in the 10-world — the on-demand recursion pattern differs from iteration 1, at the
  `Expression` union root.
- **The fork itself:** in the 10-world, `io.codelaser.maddi.cst.api.statement.Statement.translate`'s
  stored `methodLinks` is the all-empty `[-] --> -`, and once per iteration a RICH re-derivation
  (`[0:translationMap*.§$→this*.§$, 0:translationMap*.§m≡this*.§m] --> translate.§$s∋this*.§$,…`)
  arrives and is DROPPED — `RT keepEq*`: the two are EQUAL because LinksImpl equality is primary-only,
  and first-arrival freezes. In the 24-world the rich value arrived first (`RT keepEq`, renderings
  identical).
- **The stale-read:** the PERSISTED value is identical in both worlds — the empty heals in a later
  iteration (clear-before-recompute lets the rich value in). But the eventual layer's write-once
  decisions read `Statement.translate`'s links during iterations ~18–20, INSIDE the frozen-empty
  window: no links → no excusal → the 22 extra assumption edges → the contraction kills the family →
  10. Rich in the window → excusals hold → 24.

So the JVM-level seed (still unnamed) only decides WHO WINS A RACE the value model should never have
allowed to matter: equality that is deliberately key-only (primary variables) combined with
first-arrival retention makes the frozen content arrival-order dependent. The fix is content-aware
retention, gated `CANON_MLV_RICH`: `Value.strictlyRicherThan` (default false) is overridden by
`MethodLinkedVariablesImpl` (an equal value with all-empty link sets and empty modified is strictly
poorer), and `TolerantWrite` overwrites an equal-but-strictly-poorer current value. The outcome
becomes a function of the value SET, not the arrival order; the empty placeholder can no longer sit
out the eventual layer's window. Phase B of the campaign (CANON_WITNESS alone) does NOT fix the flip
(B1=10), as this analysis predicts — the witness tie-break is a cosmetic-provenance cousin, not the
fork.

### The overnight ladder (2026-08-02/03): three retention layers closed, the timing layer named

Phases E–J on the campaign dir (`~/git/ws/eventual/bistability-20260802-2135/summary.txt`; binary =
the 2f298f04 lineage, see README-attribution.txt):

| phase | change under test | result |
|---|---|---|
| E ×10 | empty→rich methodLinks retention | **24↔10 DEAD**; new bimodal **39↔53** (+29 types stabilized) |
| F ×7 | + total canonical order (content mass, then rendering) on MethodLinkedVariables | still 39↔53 |
| G ×2 | + same order on the field-level `links` (LinksImpl) | still 39↔53 |
| H ×2 | traced pair | the ENTIRE stream diff = ONE reordered on-demand first-write: `Expression.<init>(int)` (position 3608 vs 4322, both iteration 1) |
| I ×12 | slot-always-recomputes (getOrCreate short-circuit removed under the gate) | still 39↔53 (4/8) |
| J ×3+ | EC_ASSUME_DEBUG pair | **the fork's last carrier is TIMING, not content** |

The J-pair's ledger diff: in the 53-world the statement IMPLS' eventual walks succeed at **it=1**
(`enm AssertStatementImpl.translate`, `typeLevel AssertStatementImpl`) and mint impl-side edges that
later discharge when the family forms; in the 39-world those walks first succeed at **it≈20** and
record the api→`Statement` shape (221 vs 187 edges, the extra 34 undischarged) — the contraction then
drops the family. The link VALUES heal under canonical retention; WHEN a walk first succeeds does
not, and the walk's output is write-once.

**Conclusion.** The retention canonicalizations are real fixes (each killed a measured fork; keep
them), but the residual bistability is the eventual layer's ITERATION-TIMED, WRITE-ONCE minting
consuming inputs that are still settling — the exact shape the Part A'' lesson already named ("fired
eagerly it crashed survivors 75→32 by minting write-once labels in iteration 1; TERMINAL-phase +
candidates only"). The remaining fix is a design decision: defer the eventual walks' candidate
minting and ledger recording to the terminal certification point (where the contraction already
runs), so every walk sees settled links/enm state. Until then the composed dogfood is bimodal 39↔53
under CANON_MLV_RICH — strictly better than 24↔10 (floor +29), not yet a ratchet baseline.

### The discriminator's verdict (2026-08-03, phase C completed)

Four runs, `-Xint -XX:+UseSerialGC -XX:hashCode=3` (interpreter, serial GC, sequential-counter
identity hashes — every controllable JVM variance source pinned): **10, 10, 24, 24 — still bistable.**
Identity hashing is definitively excluded, closing the seed hunt by elimination: the run-to-run
perturbation comes from something outside every JVM mechanism we can control (and after the J-pair,
it does not matter): the defect was never the perturbation but the engine's SENSITIVITY to it —
write-once, iteration-timed eventual minting consuming inputs that are still settling. One bit of
anything tips which iteration a walk first succeeds in, and that bit is frozen forever. The deferral
design fix (terminal-phase minting, the Part A'' pattern) removes the sensitivity; no further seed
forensics are warranted.

### The ungating of canonical retention (2026-08-03): the retention round becomes the default

Bart: "please implement §§retention round" — the three gated retention fixes are now DEFAULT
behavior; the gates `CANON_MLV_RICH` and `CANON_WITNESS` no longer exist:

- **TolerantWrite**: an incoming value EQUAL to the current one (key-only equality) but strictly
  richer (`Value.strictlyRicherThan` — total canonical order: content mass, then rendering)
  overwrites it, unconditionally.
- **LinkComputerImpl.doType**: the analysis-order slot ALWAYS recomputes (`getOrCreate` gone) and
  canonical retention decides — the stored summary is a function of the slot computation, not of the
  on-demand arrival pattern.
- **WitnessIndex.putIfBetter**: equal-quality, canonicalCompare-0 direct witnesses tie-break on
  smallest statementIndex, unconditionally.

**The one observable semantics shift** (two `typelink.TestList` pins updated, test1/test3): a
hand-`set()` METHOD_LINKS summary on a SOURCE method no longer suppresses body derivation — the slot
recomputes, and the key-equal richer value wins. Both tests modeled sublist-style contracts on
methods that HAVE bodies; the derived values are true (and in test3 stronger) facts of those bodies,
and the call sites now see the enriched summaries (e.g. `k∈1:x.ts,k←1:x.ts[0:i]`; test3's `x` loses
its shallow-fallback `*`). Pure contract consumption remains the `testShallow*` variants' territory.
Production contracts are UNAFFECTED: aapi preloads sit on external-library methods, which the slot
loop never visits. All suites green with the new defaults (modification-analyzer, -common, -link,
-prepwork, run-*).

**Dogfood re-validation on the fresh devel lineage** (closes README-attribution.txt's caveat — the
campaign binary predated the cb2376ae devel merge; this one is built from it, with the ungating):
`~/git/ws/eventual/retention-default-20260803-0613/`, eight CLI runs plus the ratchet's in-process
run, no env gates: **eight at 53, one at 39** (V7). 24↔10 stays dead. The 53-run survivor SETS are
all identical, and result digests even repeat across runs (V3=V4=V6, V2=V5, V1=V8 — the first
full-digest reproductions ever observed on the composed dogfood). V7's delta is exactly the
14-type statement family, en bloc, 39 ⊂ 53 — the J-phase signature: the timing layer (write-once,
iteration-timed eventual minting) is untouched by this commit and still fires, now at ~1-in-9
observed instead of the campaign's coin flip. The deferral design fix (terminal-phase minting)
remains the principled closure and remains Bart's call.

**The ratchet baseline** (`dogfood/expected-eventual-survivors.txt`) is re-derived at **53** — a
strict superset of the old 24 (+29, the types the retention fixes stabilised), verified identical
across all 53-runs, wobble type absent throughout. `TestEventualRatchet` passes against it live.
Deliberate choice: the statement family is NOT moved to the wobble file — a ~1-in-9 en-bloc failure
with a documented signature (see the baseline header) keeps the pressure on the deferral decision,
whereas wobbling 14 types would blind the ratchet to real statement-family regressions.

### The deferral round (2026-08-03): terminal-phase re-derivation — the eventual layer is deterministic

Bart green-lit the deferral design. Implementation (IteratingAnalyzerImpl): at the terminal
certification point — after MODREACH reaches its joint fixpoint, before the contraction — the whole
eventual family (EVENTUAL_METHOD, EVENTUALLY_NON_MODIFYING_METHOD, EVENTUALLY_UNMODIFIED_PARAMETER,
EVENTUALLY_IMMUTABLE_TYPE) is cleared, the cluster ledger reset, and the loop continues: the eventual
walks re-derive over the SETTLED state (links final, modification frozen, immutability certified) and
re-certify; the contraction then runs on a ledger whose shape is a function of the fixpoint alone.
One round (`eventualDeferralRounds`), re-armed if a MODREACH re-derivation clears the full family
again; opt-out `EVENTUALDEFER=0`; skipped under incremental early-cutoff. The mechanics are the §14
MODREACH clear-and-re-derive pattern verbatim — `clearEventualFamily` is `clearDerivedFamily`
restricted to the four eventual keys. Cost: one extra re-derivation leg (~10 iterations, +60-80s on
the dogfood). All suites green.

**Validation** (`~/git/ws/eventual/deferral-20260803-0704/`, six runs): **68, 68, 68, 68, 68, 55.**
The five 68-runs have IDENTICAL survivor sets. The eventual layer's own nondeterminism is dead — and
the D6 outlier proves it from the other side: its divergence is visible at MODREACH round 1
(**18784 shadow edges vs 18783**), long before the deferral fired — the *input* state differed, and
given different inputs the deferral faithfully computed a different (smaller: 55 ⊂ 68, the api
statement family lost en bloc) answer. Runs with identical settled state produce identical eventual
verdicts, every time.

**The 68-world vs the transient 53-world**: +23 / −8, NOT a superset. Gained: the
`Value`/`ValueImpl` analysis family, `FieldInspectionImpl`, the `CompilationUnitPrinter` pair,
`ForEachStatement`, `EvalSum.Factor`, `GreaterThanZeroImpl.XBImpl`, … — walks that in transient
worlds read still-settling inputs and failed now succeed over the fixpoint. Lost: `ModuleInfo`, the
five arithmetic expression interfaces (`Divide`/`Equals`/`Product`/`Remainder`/`StringConcat`),
`Break`/`ContinueStatementImpl` — all eight were members of the original sound 24-set, but their
verdicts rested on reads that happened AFTER a dependency's transient verdict existed (no edge
recorded = no witnessing); under full witnessing their leans do not discharge. Whether they are
honestly recoverable (a contract, an excusal) is a climb quest, not a regression. The ratchet
baseline is re-derived at 68.

**What remains — exactly one upstream wobble.** The D6 seed is a LINK-layer edge: one extra shadow
edge at round 1, cascading to the statement family. It is NOT the EvalInequality provenance
tristability (positions 97/119/125 persist in all runs; D3/D5 share D6's 119 yet landed 68 —
uncorrelated). Next instrument: an edge-set dump gate in ShadowModificationPass, run until both
variants are captured, diff → name the edge → canonicalize its source. The eventual layer is no
longer the carrier; the hunt moves down a layer, with a 1-in-6 reproduction rate and a two-count
signature (18783/18784) to grep for.

### The edge hunt (2026-08-03, same morning): not one edge — an in-memory variant family

The hunt (`~/git/ws/eventual/hunt-shadow-edge.sh`, gate `EDGEDUMP=1`) caught the pair on run 4 of
`~/git/ws/eventual/edgehunt-20260803-0745/` — and the diff REFUTES the one-edge theory of the
previous section. Three corrections, each load-bearing:

1. **The diff is a family, not an edge**: ~15 edges differ (net +1), clustered on two shapes —
   (a) constructors with MULTIPLE same-typed parameters, whose param→field link sets smear
   differently per run: `InlineConditionalImpl.<init>` (condition/ifTrue/ifFalse, all
   `Expression`), `SwitchStatementOldStyleImpl.SwitchLabelImpl.<init>` (literal/whenExpression),
   `AssertStatementImpl.<init>` (expression/message), `FieldReferenceImpl.<init>`; and (b)
   value-mediated `Element.translateAnnotations:translationMap → <lambda>.apply:param` edges whose
   lambda targets vary (`ExplicitConstructorInvocationImpl.$5`, `ForStatementImpl.$12`,
   `LocalVariableCreationImpl.$5`).
2. **The edge count is NOT the discriminator**: R4 has 18784 edges and lands the canonical
   68-set — identical survivors to the 18783-runs. D6's 55 was a particular variant COMBINATION
   breaking a statement-family lean, not the count.
3. **The variance never persists**: R1 vs R4 persisted results are identical except the
   EvalInequality provenance metadata — the varying links are in-memory statement-level /
   lambda-mediated state, invisible to the codec. The ratchet-relevant persisted world is stable.

Net state: the composed dogfood's survivor set is 68 with an occasional (~1-in-6 combined) drop to
55 whose trigger is an in-memory link-derivation variant in the same-typed-multi-param family. The
next surgical target is naming WHY those constructors' param→field links derive differently per
run (candidate mechanisms: the on-demand recursion context of the statement lambdas, or an
instance-selection residue the rendering tie-break cannot see) — a link-layer quest, downstream of
nothing: the eventual layer consumes whatever it is fed, deterministically, since the deferral
round.

### The argument-links round (2026-08-03): last-write-wins lands; the variant survives one level deeper

Chasing §"The edge hunt": the E1 edges are built from `LINKED_VARIABLES_ARGUMENTS` — the call-site
argument links, an element-internal, never-persisted value whose `ListOfLinksImpl` record equality
bottoms out in PRIMARY-ONLY `Links` equality. First-arrival retention (TolerantWrite) froze
whichever derivation arrived first. Two fixes were tried:

1. **Canonical-max retention (`strictlyRicherThan` on `ListOfLinksImpl`) — WRONG, reverted before
   committing.** The polarity is inverted for this value: a smeared conservative fallback (derived
   while the callee summary was incomplete) has MORE content than the precise full-context mapping,
   and the set of derivations seen varies per run — a max over a varying set is neither precise nor
   deterministic. First validation run: 55 (D6's digest exactly).
2. **Last-write-wins (`ExpressionVisitor.writeArgumentLinks`) — LANDED.** The value is per-visit
   scratch state, not an accumulating verdict; the final write of any run is the settled-state
   derivation. Suites green.

Validation (`lolret-20260803-0917`, batch externally stopped after 5 runs): L1–L4 all 68 with
**byte-identical round-1 edge dumps** — a first. L5: 55, 18784 edges — and its diff vs L1 is
SHAPE-IDENTICAL to the R1/R4 pair: the same 23 edges, the same two stable variants
(InlineConditionalImpl/SwitchLabelImpl/AssertStatementImpl `<init>` param→field cross-links,
translateAnnotations→lambda targets). Conclusion: the bistability is NOT in the stored value's
retention — the DERIVATION itself is bistable. The E1 targets go through `project(mi, vd, …)`,
the caller's statement-level VariableData (the `translate()` bodies' links of
`translatedCondition/translatedIfTrue/…` to the §-faces of `this`), and THAT in-memory state has
two stable variants, one rare (~1-in-5). Next hypothesis to test: the link engine's intra-pass
exploration/witness-selection order (the iteration-1 LINKWORK variance measured long ago: witness
counts 16146 vs 16610 on identical closures) leaves the final statement-level projection in one of
two shapes — instrument the caller-side `vd` links for `InlineConditionalImpl.translate` across
runs (LINKTRACE on that method) rather than the stored summaries, which are proven identical.

### The seed-order round (2026-08-03): the salted boundary of the fixpoint engine

The LINKTRACE pair (T1 bwd/55 vs T2 fwd/68, `lthunt-20260803-1006`) settled the derivation-vs-
selection question emphatically: the traces diverge FROM THE FIRST SEED EVENTS — 10 369 differing
lines out of ~8 000, same seed content ($__rv numbering identical), different ORDER. Seed order is
derivation order (FIFO queue), and derived-fact survival is order-sensitive (the engine's own
completeSymmetrically comment records the m∩copy precedent). The order was leaking from per-JVM
SALTED collections (java.util.ImmutableCollections) at the engine's boundaries. Three fixes, all
suites green with ZERO pin fallout (every pinned behavior already assumed the natural order — the
salt just sometimes disagreed):

1. `IncrementalFixpointEngine.addSymmetricEdge`: `Set.of(fact, mirror)` → `List.of(...)` — the
   two seeds of every symmetric edge entered the queue in salted order, a literal per-JVM coin flip.
   (The salt was the FIRST bistability suspect ever, exonerated for the 24↔10 retention fork by
   SALTPROBE — it was alive one layer down all along. Alone this was measured INSUFFICIENT: the
   next validation batch still flipped, with a new edge count 18781.)
2. `Graph.transformToSharedVariable`: the shared-variable collapse re-added member edges iterating
   an unmodifiableSet (salted) — now FQN-sorted.
3. `IncrementalFixpointEngine.incrementalUpdate`: canonical seed sort (source, target, label
   renderings) at THE single entry point — every caller's collection order is normalized in one
   place; each update is a function of its seed SET.

Audited and left alone (order-insensitive uses): ExpandSlice/MakeGraph set equality and disjoint
checks, LinkGraph removal sets, mediatedPairs membership. Next-round candidates if variance
persists: VirtualModificationIdenticals' salted result streams (variablesPartOf, equivalentStream).

Validation on the canonical binary (`seedord-20260803-1154`): S1, S2 both 68 survivors, round-1
edge count 18781, **byte-identical EDGEDUMPs**. The batch was externally killed at S3 and repeated
relaunches are being killed within minutes (cause unknown — memory was 52% free with no competing
workers), so the streak stands at 2/2; the old flip rate (1-in-5) makes that suggestive, not
conclusive. Completing the 10-run batch is the next action once runs can survive.

### The lambda-slot round and the vanishing coin (2026-08-03, afternoon)

Two more layers, one confirmed and one that dissolved under observation:

**The λ-slot fix (landed).** The retention round's slot-recompute covers analysis-order methods
only; enclosed (lambda/anonymous-class) methods reached METHOD_LINKS through the on-demand
`getOrCreate` (ExpressionVisitor, LOCK case) — the FIRST computation froze, in whatever context the
first toucher had. Now: enclosed methods always recompute and canonical retention decides (the slot
rule extended to non-order methods). With this plus the seed-order round, the ENGINE is proven
reproducible at a depth never reached before: across runs, byte-identical round-1/2/3 shadow edge
dumps (8/10, second mode 2/10) and — decisively — byte-identical 2571-edge assumption LEDGERS
between same-outcome runs.

**The residual coin (open, environmental).** With all engine layers proven identical, the 68↔55
flip STILL occurred — including between runs with byte-identical round-1..3 edge dumps — placing it
in the deferral leg's eventual re-derivation reading state no dump captures. Then it vanished: from
13:52 the machine produced six consecutive 68s (instrumented, uninstrumented, and under an 8G
memory hog), where the same binary produced 6×55/4×68 between 12:53 and 13:45. Instrumentation
exonerated (E5-plain: 68), memory pressure exonerated (E6load: 68). The correlate is an
UNIDENTIFIED environmental bit that changed around 13:50. Next instruments when it reappears:
(a) diff the full JVM/env fingerprint (System properties, env, /usr/bin/time -l) of a 55-run vs a
68-run; (b) EC_ASSUME_DEBUG is cheap and the ledger diff of a caught 55 names the first divergent
eventual decision directly — the E1-E4 streams are the 68-reference, archived in
~/git/ws/eventual/echunt-20260803-1346/.

## The climb census (2026-08-03): the 68-world's honest blockers, measured

Bart: "proceed with the normal planning towards 254." Baseline measurement before any design
(EC_RETRACT_DEBUG on the 68-world + EC_TYPE_DEBUG/EC_SITE_DEBUG diagnostics, runs C1/C2 in
~/git/ws/eventual/climb-20260803-1437/, ledgers in echunt-20260803-1346/):

**139 retractions; every big root NEVER MINTS** (not retracted — the walks fail): ParameterizedType
(55 leans on it), TypeInfo (40), MethodInfo (29), Element (26), ExpressionImpl (25), Runtime (20).
Of the lost-8 vs the transient 53-world, only Sum ever minted (retracted via Runtime); the rest
never form.

**The blocker families, by census:**
1. **The `typesReferenced(nature, detailedSources, visited)` chain** (blocks ParameterizedType /
   TypeParameter → the 55-lean root): eup walks bail at (a) the recursive call to the ABSTRACT
   `ParameterizedType.typesReferenced` (the union has no labels while being derived — the
   internalCompareTo shape), (b) `Stream.flatMap/map/concat` external calls, (c) the `visited`
   accumulator param (`java.util.Set`, doomed external), (d) `DetailedSources` (api type, no
   eventual intent, not a candidate).
2. **The `translateAnnotations(translationMap)` twin** (blocks the Element family): bails at
   abstract `Expression.translate` + `Stream.map`; `TranslationMap` not a candidate.
3. **Runtime, the keystone** (breaks Sum and the arithmetic family): api-side leans on sibling
   interfaces `Predefined`/`Types` (not candidates — the upward closure never fires because Runtime
   itself lacks direct intent now); impl-side `fieldHolds` refusals for the service fields
   `e2ImmuAnnotations` (E2ImmuAnnotationsImpl) and `computeMethodOverrides`
   (ComputeMethodOverrides); eup bails at abstract `Eval.and` + doomed `java.util.List`; the
   `MethodCallImpl.parameterExpressions` List field (the copy-backed-accessor shape — lever (b)).
4. **typeLevel doomed leans on raw `java.util.List`/`Function`** at Element and its lambdas.

**The quest order this implies** (each honest, witnessed, measured on the dogfood + Fernflower):
- **Quest R (Runtime keystone, lever (c) + (b))**: honest unconditional immutability-hc for the
  service leaves (E2ImmuAnnotationsImpl is a constants holder; ComputeMethodOverrides likely
  stateless — verify and, where uncomputable, contract), Predefined/Types candidacy via Runtime's
  restored direct intent, `parameterExpressions()`-style trusted-leaf contracts. Expected to revive
  Sum + the arithmetic api family (the old quest-7 constellation).
- **Quest T (the typesReferenced chain, lever (b) + aapi)**: aapi contracts for the
  Stream.map/flatMap/concat shapes consumed here; DetailedSources' honest classification; the
  visited-accumulator eup semantics (an accumulator param is honestly modified — check whether the
  walk needs eup on it at all or the label should come from elsewhere). Biggest single payoff: the
  ParameterizedType root discharges 55 leans.
- **Quest E (translateAnnotations)**: the Expression.translate union labels must be available to
  the eup walk (Part A''-style, terminal-phase only); TranslationMap classification.
Measure after each quest; re-derive the ratchet baseline per gain; Fernflower A/B per the golden
rule before any default flips.

**Census addendum (same afternoon): the quest order inverts — T is the fulcrum.** Quest R's
impl-side leg bottoms out in the core: `E2ImmuAnnotationsImpl.annotationTypes` already IS
`Map.copyOf`-backed (the wrapper arm's precondition holds) — it fails because the value type
`AnnotationExpression` is `@Mutable eventual=null`, and AnnotationExpression MINTED and was
RETRACTED leaning on TypeInfo + ExpressionImpl, which never mint, which lean on ParameterizedType,
which the `typesReferenced` chain blocks. Every family's arrows converge there. Order: **Quest T
first** (the typesReferenced eup chain: abstract-union label availability during the walk,
Stream aapi shapes, the visited-accumulator semantics, DetailedSources classification), then E
(translateAnnotations, same shapes), then R's residue (Predefined/Types candidacy,
ComputeMethodOverrides, parameterExpressions trusted leaves) — much of R may fall out of T+E via
the cluster closure.

## Quest T, day one (2026-08-03 afternoon): the census's headline was a red herring; the real wall was Either

**The eup bails were never the blocker.** The census ranked the `typesReferenced` arg-2 bails
(126+67 on `visited`, 82 on `detailedSources`) as quest T's first target. Tracing whole walk
instances (EC_SITE_DEBUG, runs C1/T2) showed all six bailing walk roots are the eup walks on the
`typesReferenced` family's OWN parameters — structurally unwinnable (the label type `java.util.Set`
is doomed external, and `TypeParameterImpl` genuinely mutates the accumulator via `visited.add(this)`,
so no unmodified-once-L promise can exist) and consumed by nothing: all three enm walks (both impls
and the api default) converge and WRITE their labels regardless. The bails are per-iteration re-walk
noise, worth a cheap skip-guard someday (a parameter whose type has no committable label space can
never land eup), but no mint depends on them.

**Cut 1 — the trusted-leaf pair (the fieldModifiers precedent, applied).** The honest blocker was
one level up: `ECTYPE ... DEPENDENT: method parameters returns java.util.List` /
`extractTypeParameters returns java.util.Set` → after-mark independence `@Dependent` → the
dependence cap holds afterMark at `@FinalFields` → "buys nothing" → never mints. The cut:
`@Independent(hc = true)` TRUSTED LEAF contracts on `ParameterizedType.parameters()`,
`extractTypeParameters()` (fresh `Set.of`/`toUnmodifiableSet` returns) and — found by the next
trace — `replaceByTypeBounds()` (fresh `List.of`/staticToList; its unchanged-input pass-through
hands out `typeBounds()`, which is `List.copyOf`-backed at inspection commit, so still honest);
plus the honesty backing `this.parameters = List.copyOf(parameters)` in the PTImpl main
constructor (no-op for the `List.of()`/`.toList()` callers). Effect, measured (T2/T3,
EC_TYPE_DEBUG): independence reads `@Independent(hc=true)`, and **ParameterizedType computed
`immutableAfterMark=@Immutable(hc=true)` with the full mark set
`[parameters, typeInfo, typeParameter, wildcard]` and WROTE the verdict for the first time ever** —
then the terminal contraction took it back: `ECRETRACT ParameterizedType <- broken:
[Element, TypeInfo, TypeParameter]` (T3). The fulcrum now mints-and-retracts with the info family
instead of never forming. Survivors 68→68 (T1 hit the 55 coin; T2/T3 the canonical 68).

**The wall behind it: nothing materializes external-library bytecode annotations.** TypeInfo caps
on ONE method (`compilationUnitOrEnclosingType` returning `Either<CompilationUnit,TypeInfo>`),
TypeParameter likewise (`getOwner` returning `Either<TypeInfo,MethodInfo>`). `io.codelaser.maddi.support.Either`
carries `@ImmutableContainer(hc=true)` in its class file — the support library is fully annotated at
source, marks and all — but the support jar has no AAPI package, and absent-counts-as-doomed
(`candidateDoomed`) makes every exposure of an Either or an inspection holder DEPENDENT.

**Cut 2 — the `libs/support` AAPI package.** New curated hints
`maddi-aapi-archive/.../libs/support/OrgE2immuSupport.java` mirroring the library sources verbatim:
`Either` (`@ImmutableContainer(hc=true)`), `SetOnce` (`after="t"`), `EventuallyFinal` and
`EventuallyFinalOnDemand` (`after="isFinal"`) with their `@Mark/@Only/@TestMark` methods — the
compiled json carries real `eventuallyImmutableType` verdicts, so the support types enter
`treatAsEventuallyImmutable` through the PROVEN path, exactly as the `candidateDoomed` javadoc
anticipated. Generation: `GenerateSupportAnalysisResults` (maddi-run-openjdk test, @Disabled,
manual re-run after editing the hints). Preload wiring: the dogfood wrapper and
`TestEventualRatchet.PRELOAD` gained `libs/support`. NOTE: the inspection SetOnce/EventuallyFinal
fields now have real eventual semantics — the info family's own marks ("inspection") stop reading
as absent; expect movement well beyond ParameterizedType, and re-derive the ratchet baseline.

**T4, the first support-aapi world (survivors 41): a different equilibrium, not a monotone step.**
The preload cuts both ways. Gains: the arithmetic api family (Divide, Equals, Product, Remainder,
StringConcat) mints — quest R's constellation arriving early, presumably through Eval/Runtime paths
that stopped bailing on support types. And the broken-candidate census SWAPS whole families versus
the 68-world: the api EXPRESSION family + Runtime + Factory + ParameterizedType + ExpressionImpl are
no longer broken, while the api STATEMENT family + StatementImpl + the impl EXPRESSION family break
instead. Losses (net −27 vs the 68-reference): the 13-statement family (the coin's shape, possibly
caused here), the Value api+impl family (~11, all `broken: []` = cascade victims through the
closure), FieldInspection + FieldInspectionImpl, AnnotationExpression.KV/KVI, EvalSum.Factor,
GreaterThanZeroImpl.XBImpl, ExpressionComparator.Unwrapped. ParameterizedType STILL does not mint
(eventual=null) — out of the broken lists but its verdict did not survive either. api.info.TypeInfo
is WORSE: afterMark=@Mutable now (was @FinalFields), and its Either cap
(`DEPENDENT: compilationUnitOrEnclosingType`) did NOT lift — `excused()` consults only
`isEventual()` and `treatAsEventuallyImmutable`; there is NO a-fortiori acceptance of an
unconditionally immutable-hc exposure (the clause `isEventuallyImmutableFieldType` has). The Either
aapi verdict therefore never reaches the dependence check on the method side.

Open, in order: (1) the excused() a-fortiori clause — small, principled, mirrors the field-side
discharge rule; (2) root the statement/Value swap in the T4 world (its own retract census; the swap
smells like the coin's bistability amplified, so replicate first); (3) the info-family mark
propagation under the new honest transition semantics (methods calling setFinal/set are now
transitions — the owners' @Mark derivation must carry what previously read as plain modification).

**The chain, walked to its root (T5-T8).** T5 replicated T4 byte-identically (41): the support-aapi
world is a stable equilibrium, not a coin face. Cut 3, the `excused()` a-fortiori clause (an
unconditionally immutable-hc exposure shares hidden content only — the discharge rule of
`isEventuallyImmutableFieldType`, mirrored; no lean witnessed): TypeInfo's Either cap 35→0 (T6),
independence @Dependent→@Independent — and the cap moved to the immutable side: TypeInfo is
MUTABLE-after-mark because super Info reads @FinalFields and `isMutable(@FinalFields)` sinks every
sub-interface; Info in turn caps on Element. Cut 4 (T7 diagnosis → T8): Element's ONLY dependence
cap was `comments()` returning raw `List<Comment>` (45×) — the trusted-leaf sweep: contract on
`Element.comments()` + `List.copyOf` in the ten committed-face constructors (CompilationUnitImpl,
ModuleInfoImpl ×3, RecordPatternImpl, CatchClauseImpl, SwitchEntryImpl, StatementImpl,
InspectionImpl, ExpressionImpl; Builders stay raw — the before-state face). The sweep surfaced a
latent api violation: `ExpressionImpl`'s convenience constructor stored NULL comments while the api
declares `@NotNull comments()` — the copyOf sites are null-tolerant (`null → List.of()`), fixing
both. Suites green after the fix. T8 = 42 (+BitwiseNegation), Element DEPENDENT 45→0,
`independent=@Independent(hc=true)`, its unconditional verdict touching @Immutable(hc=true) in
optimistic rounds — the honest residue is `afterMark=@FinalFields excusedM=6`: the modifying-with-
enm-labels method family (translate, rewire, the visitor surface) fails to excuse. **Quest T's
chain now bottoms out exactly in Quest E's territory.** The day ends 42 vs the old 68-world — the
two worlds are not comparable by count alone: the support aapi re-rooted the equilibrium (the
statement/expression family swap), and the climb from 42 goes through Element's method excusals
(E), not through more trusted leaves.

## Quest E, step 1 (2026-08-03, night): the E6 teleport — one collector predicate marked the whole visitor surface modifying

The residue behind Element's FinalFields was NOT translate/rewire (their labels derive fine) but
the visitor surface: visit/reject/typesReferenced(Predicate), 27 impls nonModifying=false including
pure leaves, while the same-shaped visit(Visitor) resolved true everywhere. The reduction
(TestVisitPredicateDisclaimer, 35576929) PASSED — disclaimer and machinery sound in the minimal
shape — so the corpus held an extra ingredient. MODREACH_EXPLAIN (single-substring filter; reached
receivers only) named the whole chain:

    visit(Predicate) <- Predicate.test:0:arg0 <- ExtractComponentsOfTooComplex.test:0:e
      <- NamedType/TypeInfoImpl.asParameterizedType <- typeParameters
      <- seed: EventuallyFinalOnDemand.get() (non-analyzed modifying callee)

Every link honest but the union: get() is honestly modifying PRE-mark (the on-demand loader; its
excuse is eventual, invisible to plain reachability); the evidence legitimately reaches the
collector predicate's own parameter — and then the E6 union edge teleported it INTO the jdk
abstract's parameter, past the aapi contract unmodifiedParameter=1, and back down into every
predicate.test(this) call site. (En route the detour also verified the aapi parameter chain is
fully live: the compiled json stores single-parameter subs under the SINGULAAR "sub" key —
CodecImpl.E.write — and LoadAnalysisResults reads both; an earlier "compiler drops parameters"
alarm was a reading artifact.)

The cut (810287bf): an OUT-OF-ORDER (jar/aapi) abstract with a decided TRUE gets no E6 edge — the
contract is authority (writeVerdicts never visits out-of-order infos, no union is computed over
them); in-order abstracts keep their edges, the pass remains their authority. Measured: all 68
visit(Predicate) impls true; dogfood 42 -> 55 (E3/E4 byte-identical; baseline b88c225b): the
Value/ValueImpl family + FieldInspection pair + KV/KVI return; the arithmetic api family drops out
(-6) — its T4 mint rode Runtime/Eval leans the repaired modification world reshuffled; the NEXT
census target. Element/Info/TypeInfo/PT still do not mint — the remaining Quest E caps sit above
the modification layer. Fernflower A/B: eventual identical, one tightening (ConstantPool.pool
-> @Independent).

**The arithmetic census (E5/E6): the candidacy-ignition paradox, measured both ways.** Why the
arithmetic api family fell out of the 55-world: their marks arrive via Part A'' from the impls'
enm labels, candidacy flows upward from the impls, and candidacy is DERIVED state the
MODREACH/deferral clears wipe. E5's timeline: DivideImpl.rewire's enm WRITES at it=19/31/42 (once
per phase), then "no enm (null)" at it=49-51 — in the final re-derivation the cascade fails to
ignite, because the E6-guard-improved plain layer left whole method families plain-true with no
enm needed: FEWER ignition points, and the bootstrap (enm needs candidacy for its leans, candidacy
needs enm) deadlocks. The api's terminal visit finds candidate=false (both BinaryOperator and
DivideImpl) and mints nothing. THE PARADOX: improving the plain modification layer STARVES the
eventual layer's bootstrap.

The blunt fix was tried and MEASURED OUT: candidacy surviving resetForRederivation (E6 run) brings
the arithmetic five back (+5) but costs twelve others (55 -> 48): stale candidates admit doomed
leans whose contractions drag real winners down — exactly candidateDoomed's shadowing effect, now
observed end to end. REVERTED. The open design fork, for the next round:
  (a) an IGNITION PASS: after each clear, drive the enm/candidacy cascade to its own fixpoint
      (repeat enm sweeps over the analysis order until stable) BEFORE any typeLevel consults —
      no staleness, decouples ordering;
  (b) intent-preserving candidacy: carry over only DIRECT candidacy whose eventual intent is
      re-derivable in principle (e.g. re-note from the pre-clear enm/eventual method KEYS, not
      the values), letting the hierarchy closure rebuild fresh;
  (c) accept the 55-equilibrium and recover the arithmetic family from the other side (their
      unconditional @Mutable comes from the hierarchy; a Runtime/Eval quest-R cut may reach them
      without touching the bootstrap).
Artifacts: E5 (diagnostics), E6 (the measured -7), questT-20260803/.

## Quest R, the arithmetic census revisited (2026-08-03 evening): fork (c), the privacy rule, and the three roots

Fork (c) was chosen: the bootstrap stays untouched; the arithmetic family is approached from its
own caps. Two log-only instruments joined the toolbox first, because the previous round's story
("the api's terminal visit finds candidate=false") deserved distrust before surgery:
`EC_CAND_DEBUG=1` prints candidacy provenance -- one `ECCAND direct/super/itf` line per first-time
cache entry per epoch, plus the `reset` markers -- and `EC_TREATAS_DEBUG=<fqn substrings>` prints
EVERY treatAs call and result for a matching member or candidate, regardless of debugContext. The
second instrument exists because the site filter has a structural blind spot: a treatAs call made
under a stale or foreign context prints nothing, and silence had been read as "not called".

**The candidacy story was wrong -- rightly distrusted.** R1: the terminal epoch re-ignites
candidacy FULLY, within its first iteration (it=42: Element, Expression, BinaryOperatorImpl,
DivideImpl all direct; Divide via the interface closure through BinaryOperator). The ignition
paradox, as previously stated, does not survive contact with the instrument. What actually
happened, walked to its root over R2/R3:

1. BinaryOperatorImpl's FIRST terminal visit runs pre-ignition and computes a WRONG mark set
   (`[precedence]` alone -- the other four carrier fields' types were not yet candidates); it buys
   nothing and writes nothing. Harmless.
2. Its SECOND visit computes the full set `[lhs, operator, parameterizedType, precedence, rhs]`,
   `independent=@Independent(hc=true)` -- and `afterMark=@FinalFields`. That buys over the
   unconditional @Mutable, so it WRITES -- write-once, frozen.
3. The @FinalFields cap: the NON-PRIVATE-FIELD rule at the top of
   `TypeImmutableAnalyzerImpl.loopOverFieldsAndMethods`. The five carrier fields are `protected
   final`, and the rule read their types' PLAIN immutability (Expression = @Mutable) with no
   after-mark participation -- the one rule in the loop that never joined the excusal regime.
4. The frozen half-verdict then poisons every subtype SILENTLY: a super whose `ev.isEventual()`
   short-circuits both `immutableSuper` and `independentSuper` BEFORE their treatAs branches, and
   `@FinalFields.toCorrespondingIndependent()` is DEPENDENT. DivideImpl reads a PROVEN (not
   seeded) @FinalFields/@Dependent super, caps at afterMark=@Mutable, buys nothing, never mints;
   Part A'' has no implementor labels to give api Divide. R3 proved the short-circuit by absence:
   zero ECTREATAS calls for the pair in the terminal epoch.

**The cut (landed): the privacy rule joins the after-mark excusals.** A non-private field passes
in after-mark mode when (i) it is in `afterMark.fields()`, (ii) it is FINAL -- finality is not
implied by membership: a @Mark method assigns its field, and a non-private assignable field would
let a package-mate bypass the mark discipline entirely -- and (iii) its type COMMITS at the mark:
eventual, unconditionally immutable-hc, or the witnessed treatAs seed (`fieldTypeCommits`, the
immutability twin of the independence side's premise re-check). Soundness: a committed referent
bars mutation for ANY holder, so who can read the reference no longer matters; assignment is
excluded by (ii). Gated under EVENTUALCLUSTER, off-gate byte-parity preserved.

**Measured (R4/R5): formation unlocked, survival still owed to the roots.** With the cut,
BinaryOperatorImpl computes `afterMark=@Immutable(hc=true)` and the WHOLE arithmetic family forms
-- all five api types and their impls mint with the full mark set. The contraction then takes
every one of them back, and the R5 ledger names the debt precisely:
`BinaryOperatorImpl <- broken: [BinaryOperator, MethodInfo, ExpressionImpl]`, everything else pure
cascade (`broken: []`) or the Part B lean `<- broken: [BinaryOperator]`. Survivors 55, fp
byte-identical -- the fix is observably neutral today and structurally necessary: the family moved
from CANNOT FORM to FORMS, AWAITING THE ROOTS. The three roots are the census's own: MethodInfo
(the Info-family wall, quest E's residue), ExpressionImpl (abstract-class Part A: ALL analyzed
subclasses must share a label), and api BinaryOperator (which inherits the first two). The quests
converge where the census said they would; there is no separate arithmetic quest anymore.
Artifacts: questR-20260803/ (R1 candidacy provenance, R2 impl-chain trace, R3 the silence proof,
R4 the fix measured, R5 the ledger).

## The contract leg (2026-08-04): seven annotations, one inspector bug, and the race that remains

Bart picked "source contracts first" from the keystone fork. The leg, measured run by run
(questR-20260804, R6-R18):

**R6-R10, the veto census.** New `ECTYPE MUTABLE: method/field` prints in the immutability loop
named the Element surface's blockers: `typesReferenced`, `print`, `rewire` -- NON_MODIFYING=false,
not excused. The abstract enm batch (`methodEventuallyNonModifying`) was vetoed by the Info-family
impls; a debug-only ALL-blockers enumeration (AbstractMethodAnalyzerImpl) then showed the full
lists at once instead of one veto per run. MethodInfoImpl's falseness is pure MODREACH dispatch
taint (its own walk demands nothing -- `no enm ([])`, the unwritable-∅ shape live in the wild);
TypeInfoImpl's is honest (printing/typesReferenced force the on-demand inspection commit through
EventuallyFinalOnDemand.get()).

**The cut, part 1: seven `@NotModified(after = "inspection")` contracts** on the veto methods
(MethodInfoImpl.typesReferenced, TypeInfoImpl.print, TypeParameterImpl.print+rewire,
ParameterInfoImpl.rewire, FieldInfoImpl.typesReferenced+print -- all five Info types share the
field name `inspection`), plus the CONTRACTS-WIN clause in the enm step (TypeEventualAnalyzerImpl):
step 2 now mirrors step 1's contract precedence, so an inline source contract reaches
EVENTUALLY_NON_MODIFYING exactly as an AAPI one does. Note the AnnotationToProperty semantics: the
after= form also floors plain NON_MODIFYING at FALSE -- honest for the on-demand committers,
conservative for the taint victims. R11: 56 (+api.element.ImportStatement); the print batch
collapsed to its one unannotated sibling, and the typesReferenced/rewire batches WROTE their
unions. R12: **Element computed immutableAfterMark=@Immutable(hc=true) for the first time in the
campaign** -- all seven surface methods excused, a 60-label union mark set.

**The cut, part 2: the translate parameter.** R13 named Expression's sole dependence reason, 48
times over: `translate(TranslationMap):0:translationMap` @Dependent, TranslationMap not excusable.
The honest classification: the map is a read-only lookup that receives the receiver as a KEY --
hidden content -- so `@Independent(hc = true)` went onto the parameter, with the parameter twin of
`contractedIndependentHc` wired into the independence loop. It did not bite (R14/R15: the parsed
parameter carried NO annotations) -- which unmasked **an inspector bug**: ClassSymbolScanner
pre-creates methods from javac class symbols (everything references Expression, so its methods are
created early with commits deferred), and ScanCompilationUnit's FILL branch set parameter sources
but never copied the declaration's parameter annotations; `loadAnnotations` on a source VarSymbol
is empty before attribution. Every parameter annotation on a symbol-pre-created method was
silently dropped in full runs -- including `@IgnoreModifications` on Element.visit/typesReferenced,
which had been working only through the jdk-AAPI channel. Fixed in the fill branch, guarded on
emptiness. R17: the contract reads, Expression independence lifts to @Independent(hc=true), and
Expression computes hc-after-mark on its good visits.

**What remains -- the write-once/excusal-timing race.** Expression alternates: visits where
translate is excused compute @Immutable(hc=true); visits where the enm batch has not yet re-run
this epoch compute @FinalFields -- and whichever WRITES first freezes, an FF write sinking the
whole subtree through the `isMutable(@FinalFields)` hierarchy exit (ValueImpl: isMutable is true
for value 0 AND 1). The same shape as the privacy-cap incident, one mechanism up: not a missing
excusal but the ORDER excusals land in versus the write-once verdict. All four roots
(Element/Expression/ElementImpl/ExpressionImpl) now FORM within every terminal epoch and end
eventual=null. Baseline: 56, R17/R18 byte-identical; Fernflower A/B type-line identical both
sides; full suite green. The race is the deferral design's territory -- the next decision point,
recorded here for the fork.

## The FF-cap measurement (2026-08-04, same morning): 56 -> 285

Bart said "do the measurement" on the third race shape: in after-mark mode, a @FinalFields
supertype caps the subtype at @FinalFields through the hierarchy min instead of falling through
the isMutable(@FinalFields) exit to MUTABLE. Soundness: the sub inherits the super's
post-mark-mutable content, no more -- FINAL_FIELDS is the honest ceiling and the min already
expresses it; a truly MUTABLE super still sinks, and the unconditional domain is untouched
(after-mark + cluster gated).

R19: **285 survivors** (from 56), R20 byte-identical. The sink had been holding back the entire
api layer: one transiently capped FF write on Expression turned every subtype @Mutable, and "buys
nothing" erased the subtree -- with the cap the subtree floors at FF instead. Composition: 194
@FinalFields + 91 @Immutable(hc); zero Builders (the setter guards held). Element STANDS in the
final fp at @Immutable(hc=true) with the 60-label union; Expression, ExpressionImpl, the
arithmetic five (via Part B, @FinalFields(after=parameterizedType,testType)), the statement
family, ModuleInfo and the Comment families are all in. Fernflower A/B: exactly one pure addition
(SwitchOnReferenceCandidate, FF after=myTempVarAssignments), zero losses; full suite green;
ratchet baseline re-derived at 285.

The write-once/excusal-timing race is NOT gone -- its failure mode changed: a capped first write
now lands FF where a later visit would have computed hc (the arithmetic five sit at FF for
exactly this reason). Degradation, not erasure. Closing that gap -- batch-before-typeLevel
ordering, or defer-while-batches-pending -- remains the deferral design's open decision, now
worth revisiting with the stakes visible: it is the difference between FF and hc for a large
slice of the 194.

A unit pin for the cap was attempted and dropped: the terminal phase (where weak after-mark
levels write) activates only when types remain immutability-undecided at the certification point,
which a small self-contained fixture cannot produce -- every type decides. The 285-line ratchet
baseline is the pin: any regression of the cap collapses the count.

## The independence sampling round (2026-08-04, afternoon): the seed's last measured consumer, closed

The warm-up sweep (the parked epoch-extension patch, `questR-20260804/warmup-sweep-uncommitted.patch`)
exposed a bistability the standard epoch masks: 179hc/106FF vs 165hc/120FF at identical 285
membership, ~14 `api.statement` interfaces flipping en bloc. The forensics, run pair by run pair:

- **Not retraction**: EC_RETRACT_DEBUG ledgers are five lines, identical across attractors.
- **Not run shape**: both worlds run exactly 67 iterations with byte-identical per-iteration
  top-10 change summaries.
- **Not identity hashing per se**: `-XX:hashCode=3` pairs still flipped — reconfirming the trace
  round's caveat (JIT/VM threads perturb the counter; the clean `-Xint` discriminator remains
  unrun). The full-corpus dump diff between attractors is TWO entries: the 14 symptom verdicts,
  and the `EvalInequality.twoTerms` methodLinks provenance anchor (97 vs 125) — the seed's
  familiar, semantically-neutral fingerprint.
- **The consumer, caught by probe** (EC_SITE_DEBUG prints in the abstract batch and the mod-indy
  writer, `questR-20260804/probes-amabatch-modindy.patch`): the fork is the stored
  `INDEPENDENT_PARAMETER` of `ForStatementImpl.translate(TranslationMap):0`. After the MODREACH
  re-derivation reset (it≈23) it re-derives from the links state OF THAT MOMENT and the first
  answer freezes: `@Independent` in one world, `@Dependent` in the other, unchanged to iteration
  67 while the links themselves heal. The abstract batch's `getOrDefault(DEPENDENT)` union carries
  it onto `Statement.translate:0`; the terminal epoch's write-once after-mark evaluation reads it;
  DEPENDENT uncured = the FF cap; the family follows the root. Unpatched, the SAME parameter
  already flips `@Independent(hc=true)` vs `@Independent` from iteration 1 (probed, 4 runs) — the
  committed world's byte-identical dogfoods are consumers-not-looking, not absence of the coin.

Two optimism-by-sampling holes in `TypeModIndyAnalyzerImpl.worstLinkToFields` made the sample a
verdict: EMPTY links concluded full INDEPENDENT (the loop never ran), and a link to a field whose
type immutability was still undecided was silently skipped — worse, the plain `typeImmutable`
defaults an undecided SOURCE type to MUTABLE, so a cleared-not-yet-rederived field read as
DEPENDENT. Fixed monotonicity-preserving ((b)+(c) of the design discussion; (a), same-writer
replacement, was considered and parked because it trades the lattice termination argument for an
empirical one):

- `LinkToField.immutableOfLinkedField` now uses `typeImmutableNullIfUndecided` and reports
  undecided as null; the new `reachesJudgeableField` lets `worstLinkToFields` distinguish
  "nothing to judge" (skip) from "field, still undecided" (return null = wait). A decided MUTABLE
  still concludes DEPENDENT immediately — bottom dominates.
- The abstract batch unions (`independent`, `methodIndependent`) read implementations with
  `getOrNull` and WAIT on any undecided implementation instead of defaulting it DEPENDENT.
- The cycle-breaking null branches in `doIndependent` — dead code until now, since the derivation
  could never be null — write their optimistic INDEPENDENT only for never-decided values and no
  longer carry the `assert write`: an undecided re-derivation must not erase a stored honest
  verdict.

The first real write now happens from decided inputs only: ⊥ → decided is the only transition,
TolerantWrite stays monotone, and whatever never decides falls to cycle breaking's deliberate,
single-point optimism instead of an accidental freeze at a random iteration.

Gates: full suite green; unpatched dogfood 4/4 byte-identical with every verdict unchanged
(91hc/194FF at 285 — the committed world never consumed the flip); ratchet green; Fernflower A/B
byte-identical (2 survivors both sides).

**The residual, measured and named**: under the warm-up epoch extension the coin survives at
reduced frequency (~1 in 3, was ~1 in 2). The probe pair V1/V2 shows the same parameter re-derive
at it=23 from mlv whose CONTENT differs: `0:translationMap.§$s⊆this.initializers.§$s` in one
world, `~this.initializers.§$s` plus `≤this.initializers` in the other — different link NATURES
and faces for the same variable pair, honest verdicts on both sides of a forked input. The
carrier is nature-variant link emission in the MODREACH re-derivation window (the m∩copy /
redundancy-suppression family, at emission rather than storage), one layer below the independence
verdicts. Both worlds' final persisted state remains identical outside the 14 eventual= levels.
The warm-up sweep's determinism gate therefore still fails; candidate directions: canonicalize
nature-variant emission under re-derivation, or redesign the sweep so the type-level evaluation
never samples the re-derivation window at all (the batch-before-typeLevel ordering of the FF-cap
section's open decision).

## The re-land (2026-08-04, evening): tier-uniformity, the Predefined catch, and a deterministic terminal epoch

The residual after "undecided is not a verdict" (~1-in-3 flips) traced one layer further down: the
probe pair V1/V2 showed the SAME parameter re-deriving from method links whose CONTENT differed —
`0:translationMap.§$s⊆this.initializers.§$s` in one world, `~this.initializers.§$s` plus
`≤this.initializers` in the other. Adjacent natures of one content-sharing family, freely
interchanged by the combination lattice's order — and `LinkToField.linkedType` judged `~`/`⊇` while
silently skipping `⊆`/`≤`. The verdict fork was manufactured by the consumer's incomplete nature
table, not by the emission variance itself. Scheduling was ruled out on principle: the forked links
persist to the fixpoint (read unchanged at iteration 67), so no re-ordering of WHEN the type level
samples could ever help.

**The tier-uniformity fix**: `⊆` joins the `~`/`⊇` branch (same judged type). `≤`/`≥`/`∩` stay
deliberately unjudged — `≤`'s target is the whole container, so judging it reads a DIFFERENT type
than the face variants and would re-open the fork from the other side; extend only with a variance
witness in hand. Result: warm-up runs 6/6 byte-identical — the coin is DEAD — at a price: 285→279,
the whole runtime family retracted (`broken: [Predefined]` on every new ledger entry).

**The Predefined catch.** The single blocking member: `Predefined.primitives()` returns
`primitiveByName.values()` — a LIVE view; `values().remove()` mutates the map, i.e. Predefined's
own state, before or after any mark. The 285-world's `@Immutable(hc=true)(after="isAssignableFrom2")`
for Predefined had been resting on the analyzer's blindness to the `⊆`-link. The determinism
campaign's closing act was the dogfood catching a real soundness hole in its own corpus. Fix in the
corpus, not the analyzer: `primitives()` hands out `Set.copyOf(...)`, and the interface declaration
carries the (now true) `@Independent(hc = true)` contract — the contract leg's mechanism, one more
time. Full recovery: 285 types, membership identical to the baseline, deterministic 4/4.

**The seeded-test root cause**, finally: the certification-gate hold unset `done`, but the
verify-certify loop's own exit ("worklist dry + full verification pass clean") returns WITHOUT the
terminal machinery and never consulted the hold — on the tiny fixture it escaped mid-window,
skipping TERMINAL_CERTIFIED, the deferral and the contraction. The hold now also breaks `verifying`,
re-arming the full-verification dance; when the window closes, certification exits through the stop
block as designed. Both sentinel tests green; the dogfood output is byte-identical across the
repair (Y1=Y2=Y3=Y4).

**The deterministic terminal epoch, landed.** Final composition: 285 = 163 @Immutable(hc) + 122
@FinalFields — between the old coin's two worlds (179/106 vs 165/120), and honestly so: the
179-world's surplus rode the optimistic side of the fork. ModuleInfo.Provides sits at FF, the one
honest level residual. Gates: full suite; unpatched determinism 4/4 (pre-re-land measurement);
warm-up determinism 6/6 (279 world) and 4/4 (recovered 285 world); ratchet (membership unchanged);
Fernflower A/B byte-identical twice (after the analyzer fix, and after the corpus+contract+hold
package). Still open, now cosmetic: nature-variant emission itself (witness: the twoTerms anchor),
the consumer-table audit beyond LinkToField, and the never-completed `-Xint` seed experiment.
