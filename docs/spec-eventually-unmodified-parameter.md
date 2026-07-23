# Spec — `EVENTUALLY_UNMODIFIED_PARAMETER` (`@NotModified(after=…)` on parameters)

**Audience:** a fresh model implementing this without the 2026-07-23 session context. Companion to
`docs/handoff-eventual-interface-nonmodification.md` (Part B, the receiver twin) and
`docs/handoff-verification-residue.md` §7–§9 (why the honest modification state makes this the next
mechanism). **Status: IMPLEMENTED (2026-07-23, same session) — §8 below records what landed, the
measured outcome against §6, and the follow-on quests the measurements exposed.**
**Gate:** everything is behind `EVENTUALCLUSTER` (computation, consumption, propagation). The golden
rule for gated work applies: gate-off corpus FPDUMPs byte-identical by construction; prove it anyway.

---

## 0. TL;DR / the one mechanism

`EVENTUALLY_NON_MODIFYING_METHOD` (enm) says: *this method's modification of its RECEIVER is confined
to reads-through-eventual-fields; after the named marks have fired, calling it no longer modifies the
receiver.* The exact same statement is needed for a **parameter**:

> `EVENTUALLY_UNMODIFIED_PARAMETER` (eup), a `SetOfStrings` on a `ParameterInfo`: this method does
> not modify the argument's object graph EXCEPT through chains that an eventual verdict excuses —
> once every label in the set is committed **on the argument**, a call leaves the argument
> unmodified.

With eup, `commitLabels` can excuse handing a this-derived value (including bare `this`) to a helper
whose parameter is plainly `@Modified` — the shape that currently sinks the whole print/rewire family.

## 1. Motivation: the evidence from the honest (MODREACH) state

After the P3 shadow-pass redesign and the `@IgnoreModifications` sweep (commits `23a01d71`…`6ebb7225`),
`Element` is unconditionally `@Immutable(hc=true)` gate-off, but under `MODREACH=1` six holdouts
remain, and they share one shape:

- `ParameterizedTypeImpl.print/fullyQualifiedName/descriptor/…` hand `this` to the static
  `ParameterizedTypePrinter.print(...)`.
- The printer's `parameterizedType` parameter is **honestly** `unmodified=false`: the printer calls
  `parameterizedType.typeInfo().isStatic()` → `inspection.get()` — the pre-mark modification of the
  *referenced* `TypeInfoImpl`, reached through the parameter's object graph.
- `TypeInfoImpl.isStatic()` itself already carries `eventuallyNonMod=[inspection]` — the receiver
  machinery has done its job. The information dies at the parameter boundary: nothing states "the
  printer stops modifying its argument once the argument's content is committed", so the caller
  (`print`) cannot be excused, `ParameterizedType` never gets its eventual verdict, and through
  `Element.print(Qualification)` the retraction cascades over the entire CST.

Same shape: `rewire(InfoMapView)` (the 17 remaining `infoMap` guard violations),
`Element.variableStream*` (stream helpers over this-derived content), and every future utility that
takes a CST object and reads it.

## 2. Semantics, precisely

For method `m` and its parameter `p` of type `P`:

- `eup(p) = L` (a non-empty set of labels) means: every modification `m` performs on `p`'s object
  graph is a chain that roots in `p` and is excused, under the eventual machinery, by the labels in
  `L`. **Labels are field names in `P`'s label space** — the same space `enm` uses for receivers:
  own eventually-immutable fields of `P` (or its cluster candidates), e.g. `inspection`, `typeInfo`.
- `eup(p) = ∅` is meaningful and is the property's vacuous-true: `m` touches `p`'s graph only
  harmlessly (this will coincide with plain `unmodified=true`; do not write ∅ — absence + plain TRUE
  already says it; write only non-empty sets, mirroring enm).
- No value = no statement (the parameter is modified in ways the machinery cannot excuse).
- The property is **additive and eventual-layer only**: it never overrides `UNMODIFIED_PARAMETER`,
  is invisible to the plain fixpoint and to MODREACH (which freezes only the three plain
  modification properties), and is written once per analysis like enm (TolerantWrite `set`).

## 3. Computation (in `TypeEventualAnalyzerImpl`, alongside `computeEventuallyNonModifying`)

For each analyzed method `m` (instance or static — static is the whole point) and each parameter `p`
with plain `UNMODIFIED_PARAMETER == FALSE`:

1. Re-run the body walk that `commitExcusedLabels`/`commitLabels` performs for the receiver, with the
   **root variable `p` instead of `this`**: collect every modification site (assignment through a
   `p`-rooted face; call whose receiver chain or modified argument roots in `p`).
2. Excuse each site with `commitLabels(ownerType = P's bestTypeInfo, expr, ctx)` where the
   "this-field" cases match **`p`-scoped FieldReferences** (`p.f` ↔ `this.f`), and `p` itself plays
   the bare-`this` role (candidate-owner rule applies when `P` is a cluster candidate). The existing
   `LocalContext` (freshness, local commit-map) machinery carries over unchanged — locals derived
   from `p` are `p`-derived.
3. Union the labels; any inexcusable site → no value. Write only when non-empty.

Implementation note: the cleanest cut is to parameterize the existing walk by a `root` (This vs a
specific ParameterInfo) rather than duplicating it; `rootedInFresh`, `handedOnValueSafe`,
`valueIsHarmless`, look-through and the ternary/cast unwraps are root-agnostic already.

## 4. Consumption (the `commitLabels` MethodCall/argument case)

Where `commitExcusedLabels`/`commitLabels` currently bails because a callee's parameter is plainly
modified and the argument is this-derived:

- If the callee's parameter carries `eup = L`, the call is excusable **iff every label in `L` is
  committable on the argument**, judged exactly like receiver labels: for argument = bare `this` of
  a candidate owner, `L ⊆` committable labels of the owner (they join the caller's enm set); for a
  this-field argument `this.f`, the labels must be committable on `f`'s type
  (`isEventuallyImmutableFieldType` route) and the caller's label is `f` (the receiver-labels rule:
  the field's own commitment covers its content's transitions — the §060 ride-along argument).
- The labels contributed to the CALLER are the translation of `L` into the caller's label space,
  identical to how `labelsOfReceiver` translates a field-rooted receiver chain today.
- Cluster provenance: if `L`'s committability leans on candidate assumptions, record witnessed
  assumption edges through the existing success-only buffers (`treatAsEventuallyImmutable` /
  `noteLabelInheritance`); the contraction then cascades correctly for free.

## 5. Propagation and the batch

- `AbstractMethodAnalyzerImpl`: union `eup` over implementations per parameter position, mirroring
  `methodEventuallyNonModifying` (weakest-common-guarantee union under the gate; a TRUE-unmodified
  impl parameter contributes nothing; a modified impl parameter without eup sinks the abstract's
  value). Call `noteLabelInheritance` for provenance, as enm does.
- FPDUMP: extend the `FPDUMP_PARAMS` param lines with ` eventuallyUnmod=[...]` (the separate gate
  keeps the corpus A/B format untouched).

## 6. Definition of done

- Unit: a `TestCommitLabels`-style fixture — a static helper `int size(T t) { return
  t.inspection().get().length(); }` over an eventual `T`; assert `eup(size:0) = [inspection]`, and a
  caller `int mySize() { return Helper.size(this); }` gains `enm = [inspection]`.
- Dogfood (composed `MODREACH=1 EVENTUALCLUSTER=1`):
  `ParameterizedTypePrinter.print(...):1:parameterizedType` gets a non-empty eup;
  `ParameterizedTypeImpl.print/fullyQualifiedName/descriptor` gain enm labels;
  `Element.print(Qualification)` leaves the unlabeled-holdout list; survivors strictly above the
  current **5**, retraction strictly below the current **63** (values at commit `6ebb7225`).
- Gate-off: corpus FPDUMPs byte-identical (all writes gated); full module suites green.

## 7. Edges and non-goals

- **Varargs**: apply per declared parameter; the varargs array itself is never committable (arrays
  are excluded from committability today) — expect no value there.
- **Generics**: labels name fields of the *declared* parameter type's best TypeInfo; if the concrete
  argument type differs (subtype candidate), the owner-candidacy rule already covers the receiver
  analog — reuse, do not invent new translation.
- **Constructors**: capture (`this.f = p`) is linking, not modification — out of scope; eup on
  constructor params only for genuine modification chains.
- **Non-goals**: no plain-layer or MODREACH changes; no contraction weakening; no attempt at
  `Qualification`-style parameters that are GENUINELY mutated by design (the printer's
  `qualification` accumulates imports — that one is honest modification and stays `@Modified`; only
  reads-before-mark qualify).

---

## 8. Implementation record (2026-07-23)

### 8.1 What landed

- **Property**: `PropertyImpl.EVENTUALLY_UNMODIFIED_PARAMETER` (`eventuallyUnmodifiedParameter`,
  `SetOfStrings`), registered in `PropertyProviderImpl` (codec support for free).
- **The root parameterization** (§3's "cleanest cut", taken literally): `TypeEventualAnalyzerImpl`
  gained a `WalkRoot(member, labelType, root)` record threading through the whole `commitLabels`
  family (`commitExcusedLabels`, `commitLabels`, `lambdaCommitLabels`,
  `methodReferenceCommitLabels`, `commitArguments`, `handedOnValueSafe`, `buildLocalContext`,
  `trackAssignment`, `referencesRootOrTracked`). `root == null` is the receiver walk
  (behavior-identical to the old `owner`-only code); `root == a ParameterInfo` is the eup walk,
  with `labelType` = the parameter's declared type and `member` staying the ANALYZED type (the
  witness assumer, so label provenance folds consumers correctly).
- **Two look-through extensions** the flagship chain required:
  - inlining an accessor body evaluates it **re-rooted at `this`** (inside the callee, `this` IS
    the root object) — `WalkRoot.inlineInto`;
  - **abstract-accessor bridge**: an abstract accessor on the bare root (an interface-typed eup
    parameter, or an inherited accessor in a receiver walk) inlines through its IMPLEMENTATIONS —
    every implementation must be a committable single-return, the union of the labels is the
    answer (implementation field names, the enm convention on interface methods).
- **Computation**: `computeEventuallyUnmodified` in the `go()` loop, per parameter, gated
  `EventualCluster.ENABLED`; honest-FALSE precondition per §3; conservative bails for parameter
  rebinding, stores through p-rooted faces, and p-derived capture into fields.
- **Consumption** (§4): in `commitArguments` — the bare root handed to a parameter carrying eup
  contributes the labels iff each is committable on the root's type (`labelsCommittableOnRoot`),
  **or** the callee parameter's declared type IS the walk's label type (same-label-space
  pass-through: the 5-arg → 6-arg printer overload forward; an interface label type has no fields
  to check, the check is deferred to the ultimate this-walk consumer). Provenance via
  `noteLabelInheritance(member, calleeOwner)`.
- **Propagation** (§5): `AbstractMethodAnalyzerImpl.parameterEventuallyUnmodified`, union over
  implementations, mirroring `methodEventuallyNonModifying`, gated.
- **FPDUMP_PARAMS**: ` eventuallyUnmod=[...]` appended when non-empty.
- **Annotation twins**: `DecoratorImpl` emits `@NotModified(after=…)` on parameters;
  `AnnotationToProperty` parses it back (honest floor `unmodified=FALSE` + the label set).
- **Unit test**: `TestEventuallyUnmodifiedParameter` — runs the COMPOSED analyzer
  (`setModificationViaReachability(true)` + gate), because the plain fixpoint freezes helper
  parameters optimistically `unmodified=TRUE` and eup only fires on an honest FALSE. Covers the
  §6 fixture (direct field read AND accessor route → `eup=[inspection]`; caller `mySize()` gains
  `enm=[inspection]`), the rebinding bail, and the gate-off no-op.

### 8.2 Measured outcome (composed dogfood, MODREACH=1 EVENTUALCLUSTER=1)

| metric | baseline `6ebb7225` | with eup |
|---|---|---|
| eup-labeled parameters | – | **265** |
| enm-labeled methods | 548 | **567** |
| modifying-unlabeled methods | 1382 | 1365 |
| survivors (`eventual=@`) | 5 | 5 (same set) |
| retracted | 63 | 63 |

- **§6 flagship: MET.** `ParameterizedTypePrinter.print(…):1:parameterizedType` (both overloads)
  = `eup=[typeInfo, typeParameter]`; `TypeNameImpl.typeName(…):0:typeInfo` = six labels — the §7.2
  evidence chain of the residue handoff, closed end-to-end. `ParameterizedTypeImpl.print` (both) /
  `fullyQualifiedName` / `descriptor` / `printFqn` / `printForMethodFQN` / `simpleString` /
  `toString` / `detailedString` / `mostSpecific` / `formalToConcrete`, and the matching
  `ParameterizedType` interface methods, all gain `enm=[typeInfo, typeParameter]`.
  `Variable.rewire` / `ParameterInfoImpl.rewire` join too. Net +21 enm.
- **§6 scoreboard: NOT met** — survivors stay 5, retraction stays 63,
  `Element.print(Qualification)` stays a holdout. But two survivors STRENGTHEN their level:
  `ModuleInfoImpl.ProvidesImpl`/`UsesImpl` go `@FinalFields(after=…)` → `@Immutable(hc=true)(after=…)`.
- **Two enm "losses" are honest corrections, not regressions**: the baseline's
  `Element.complexity()=[fieldInfo, inspection]` and `Element.translateComments()=[inspection]`
  were STALE OPTIMISTIC unions — their IMPLEMENTATIONS include `SwitchEntryImpl.complexity` /
  `TryStatementImpl.CatchClauseImpl.complexity`, honestly modifying-unlabeled in BOTH runs; the
  baseline's write-once union fired while those were still transiently `nonModifying=TRUE`
  (pre-modreach-downgrade). With eup shifting convergence timing, the batch now sees the honest
  state and correctly declines. (Write-once + downgrade-later is a known composed-mode wrinkle;
  the same mechanism, not new.)

### 8.2b Gate-off certification (the §0 golden rule)

Every write is gated `EVENTUALCLUSTER` (the ungated surfaces — property registration, the
`AnnotationToProperty` parameter arm, the `DecoratorImpl` emission — are inert without the
annotation resp. the property). Proven anyway, three-corpus A/B vs the `6ebb7225`-state dumps:
Fernflower = exactly the known `StatEdge.EdgeType.<init>(int)` flake line (both directions, nothing
else), Langchain4j = **0 lines**, Timefold = 6 lines, all inside the documented base-vs-base flake
envelope (`Testdata*Solution` independence + a `BiNeighborhoodsPredicate.$0` flip). Roll-calls
`tests="1" failures="0"`, dumps non-empty, `--rerun-tasks` forced. All four module suites green.

### 8.3 Why the cascade stops, precisely — the next quests

`ParameterizedTypeImpl`'s type verdict (and through it the big retraction cluster: the interface
`ParameterizedType` appears in almost every `ECRETRACT broken:` list) now dies on its REMAINING
modifying-unlabeled methods, which are new shapes, each a designed-mechanism decision:

1. **List-of-candidate-content fields** — `typesReferenced(nature, ds, visited)` bails on
   `this.parameters.stream()`: `parameters` is `List<ParameterizedType>`, and
   `fieldHoldsCommittableContent` rightly refuses a mutable `List` type. Excusing read-only
   stream/iteration chains over a final List field whose ELEMENT type is committable needs its own
   ride-along rule (and its own soundness argument: the list itself must never be mutated).
2. **The type-parameter-map family** — `initialTypeParameterMap`/`forwardTypeParameterMap`/
   `concreteSuperType`/`replaceByTypeBounds`: recursive map-building over `parameters` content.
3. **`rewire(InfoMapView, Map)`** and `withNullable` (fresh-copy constructors reading own state).

`Element.print(Qualification)`'s abstract union needs ~51 print implementations excused — those
bodies run through `OutputBuilder` chains and the printer interfaces (`TypePrinter`,
`MethodPrinter`), and mostly hit shape 1/2 again. eup is a necessary ingredient (it closed the
static-helper hop) but not sufficient for the print union.
