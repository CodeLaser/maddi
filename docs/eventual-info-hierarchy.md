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
method → type → …) whose eventual immutability is a joint fixpoint: each needs the others. That is a
distinct, larger problem than the inherited-mark of step 6 — closer to cycle-breaking-for-eventual —
and is where the next push has to aim. Steps 1–5 are the necessary substrate; they are correct and
land the whole self-field pattern, but the cross-reference cluster is what the real `Info` hierarchy
is actually made of.
