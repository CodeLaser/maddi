# Handoff — the Builder leans (the last structural gap before the flagship family can survive)

> **STATUS UPDATE (2026-07-23, round 2): RESOLVED — all ten §2 edges gone, §4c below.** The write-once
> ordering theory of §4b is falsified alongside the conflation theory; the real mechanism and the two
> fixes are recorded in §4c. The Builder interfaces no longer appear among the retraction roots.

**Audience:** a fresh model (or Bart) deciding and implementing the Builder mechanism without the
2026-07-23 session context. Companion to `docs/spec-eventually-unmodified-parameter.md` §8–§9 and the
running record in `docs/eventual-info-hierarchy.md` (read "The interface clique round" first).
**Status: CHARACTERIZED, design decision open.** Ring 2 already named this a modeling decision
("exclude builder-natured types from candidacy, or give builders their own eventual story"); this
handoff adds the measured evidence and the constraint that rules out the easy answers.

## 1. Where the arc stands (commit `b417dad7`, branch `ws/eventual`)

Composed dogfood (`MODREACH=1 EVENTUALCLUSTER=1`), stable across two runs: survivors 11,
retracted 92, and the ENTIRE flagship family (TypeInfoImpl, MethodInfoImpl, FieldInfoImpl,
ParameterInfoImpl, the TypeInfo/MethodInfo interfaces, ParameterizedTypeImpl) **forms eventual
verdicts and is retracted by the cluster contraction** — no longer "never forms". The measured
retraction roots (`EC_RETRACT_DEBUG`): the five `*.Builder` interfaces (~26 leans), `Element`(9) /
`Statement`(9) (the print/rewire abstract-union breadth), `VariableImpl`(7) (assignable lazy caches),
`FieldInspection`(4), long tail.

## 2. The evidence: exact leaning sites

`EC_ASSUME_DEBUG=.Builder` (new: prints DIRECT assumption edges with the computation that recorded
them — `EventualCluster.setDebugContext`, thread-local, log-only):

```
FieldInfoImpl  -> FieldInfo.Builder      at enm FieldInfoImpl.rewirePhase3(InfoMap)
MethodInfoImpl -> MethodInfo.Builder     at enm MethodInfoImpl.rewirePhase3(InfoMap)
MethodInfoImpl -> ParameterInfo.Builder  at enm MethodInfoImpl.translate(TranslationMap)
MethodInfoImpl -> TypeParameter.Builder  at enm MethodInfoImpl.copyAllButBody…(TranslationMap)
MethodInfoImpl.$6 -> TypeParameter.Builder at eup $6.apply(TypeParameter):0:tp0
TypeInfoImpl   -> MethodInfo.Builder     at eup TypeInfoImpl.handleMethodOrConstructor(…):1:rewiredMethod
TypeInfoImpl   -> ParameterInfo.Builder  at eup TypeInfoImpl.rewireParameters(…):1:rewiredMethod
TypeInfoImpl   -> TypeInfo.Builder       at enm TypeInfoImpl.rewirePhase1(InfoMap)
InfoMapImpl    -> ParameterInfo.Builder  at eup InfoMapImpl.createSyntheticArrayConstructor(…):0:methodInfo
FactoryImpl    -> MethodInfo.Builder     at eup FactoryImpl.newArrayCreationConstructor(…):0:type
```

One shape: the **rewire/translate/synthetic-construction machinery** — obtain an under-construction
copy (`infoMap.methodInfo(this)`, a rewired local, a parameter), fill it through
`copy.builder().setX(…).commit()`. The `builder()` accessors themselves are pre-mark reads guarded
by preconditions (`assert inspection.isVariable(); return (Builder) inspection.get();` — or the
if-throw variant in `MethodInfoImpl.builder()`); `commit()` is the mark. Neither carries an eventual
classification today: `computeEventual` recognizes transitions only through CALLS to
already-eventual callees with this/this-field receivers, and deliberately ignores `@TestMark`
observations — the assert-precondition shape is the "precondition reasoning we are not reviving"
(see `immutableAfterMark`'s field-finality note), except here it is method-side and small.

The lean itself fires in the commit walk's handed-on/committability judgments
(`returnTypeHoldsCommittableContent`/`typeParametersHoldCommittableContent` meeting a Builder-typed
value): Builder interfaces are cluster candidates via the upward closure, so
`treatAsEventuallyImmutable` answers optimistically and witnesses the edge. The Builders can never
prove (plain setters; `haveSetters` in `computeImmutableType` is an unconditional MUTABLE exit,
BEFORE any after-mark relaxation), so every one of these leans is guaranteed to be retracted.

## 3. THE CONSTRAINT that rules out the easy answers

**The flagship formation currently depends on these doomed leans.** The leaning computations are enm
labels on the flagships' own rewire/translate methods, and `computeTypeLevel` needs every modifying
method excused. Therefore:

- *Excluding never-provable types from candidacy* (Ring 2 option 1): the leaning chains BAIL instead
  of leaning → the rewire methods lose their enm labels → the flagship types stop forming.
  Regression, not progress.
- *Classifying `builder()` as `@Only(before)` alone*: the transition-callee bail in
  `commitExcusedLabels`/`commitLabels` fires on ANY non-fresh receiver → same bail, same regression.

Any fix must REPLACE the doomed lean with a sound excuse for the shape
`nonFreshCopy.builder().setX(thisDerivedArg).commit()`. Semantically the excuse exists: the
builder-chain modifies the COPY's object graph, not `this`/`p` — only the this-derived ARGUMENTS
need committing (and `commitArguments` already handles them).

## 4. Candidate designs (in preference order, none implemented)

**A. Root-derivation-scoped transition bail + precondition-based `@Only(before)` (recommended
sketch).** Two coupled changes, both gated:
  1. Relax the transition-callee bail: a `@Mark`/`@Only(before)` callee on a receiver the walk
     proves NOT root-derived (`commitLabels(receiver) = ∅`) is another object's transition — it
     cannot modify the root; continue with argument labels only. (Today the bail fires regardless
     of the receiver, `rootedInFresh` being the only exemption.) This is what makes the rewire
     chains excusable arg-only, WITHOUT consulting Builder candidacy — the lean disappears because
     the handed-on judgment is never reached (`receiver.isEmpty()` early return).
  2. Recognize the precondition shape: a method whose body is guarded by `assert <@TestMark(before)
     call on this.f>` (or `if (<test>) … else throw`) and otherwise only reads the before-side is
     `@Only(before=f)` — this classifies `builder()` honestly, keeps it excused at the type level
     (`computeTypeLevel` excuses `@Only(before)` methods), and stops enm from mislabeling
     "throws-after-mark" methods as "non-modifying after the mark".
  Caveat to verify: with (1) in place, check each §2 site actually resolves to a ∅ receiver (the
  locals are tracked with the labels of `infoMap.methodInfo(this)` — currently ∅ via the
  candidate/argument escapes); any site where the receiver is genuinely root-derived (bare
  parameter roots in the eup sites!) still bails — the eup sites in §2 (`…:1:rewiredMethod`,
  `…:0:methodInfo`) have the ROOT as the builder receiver, so for THOSE the eup value is honestly
  unattainable (the method genuinely fills the argument's builder = modifies the argument
  unconditionally, pre-commit). Losing those eup values is correct; measure what it costs.
**B. InfoMap factory freshness**: a contract on `InfoMap.typeInfo/methodInfo/fieldInfo(…)` that the
  returned object is under construction ("its lifecycle belongs to the rewire") — would extend
  `rootedInFresh` through the map lookups. Honest concern: the map RETAINS the copies (dependent),
  and other phases observe them mid-construction; the "freshness" is a whole-rewire-transaction
  notion, not a per-method one. Needs a real soundness story.
**C. Builders' own eventual story** (`@Immutable(after="commit")` on the Inspection.Builder impls):
  blocked by the `haveSetters` unconditional-MUTABLE exit, which is not relaxed by afterMark; the
  builders' setters genuinely work only pre-commit, so this is a coherent but larger reform (the
  field-finality/precondition reasoning the current design explicitly retired).

## 4b. Option A implemented (same session) — six of the ten edges gone; the residue characterized

Both halves of §4A are code (gated, `TestCommitLabels` INPUT_GUARD pins, suites green):
`receiverProvablyNotRoot` relaxes the transition bail (fluent chains unwrapped to their base; a base
free of root content, or a tracked local the walk judged committed-after-∅, is another object's
lifecycle), and `scanPreconditions`/`recordGuardSide` classify the guard shapes — leading
`assert <state test>` and both if-throw variants — as `@Only` on the side the live path requires.

**Composed dogfood: 23 methods classified `@Only`; Builder edges 10 → 4; enm 673→654 (the newly
`@Only` methods leave the enm layer, excused at type level via the `@Only(before)` route instead);
survivors 10, retracted 93. The flagship family still forms** (all five in the retracted set).
The three survivor drops vs the previous round (`InfoImpl`, the TypeReference pair) are pure-cascade
retractions (`broken: []`) — formation is fine; the folded assumption graph pulls them under while
the clique is unconverged.

**The four remaining edges, precisely characterized** (temporary stack instrumentation on
`isEventuallyImmutableFieldType`; all four share ONE path):
`returnTypeHoldsCommittableContent(Builder)` ← `handedOnValueSafe` fallback ← the committed-receiver
gauntlet in `commitLabels`. The mechanism is a CONFLATION plus a write-once race:

1. `rewiredMethod = infoMap.methodInfo(this)` — the tracked local's labels come from
   `commitArguments`' accumulator, which folds the EUP-CONSULT labels of the `this` argument
   (labels excusing THE CALL) into what is then used as the RESULT VALUE's commit-labels. The local
   becomes "root-derived, committed after L" instead of ∅.
2. A non-∅-tracked receiver takes committed-receiver semantics: `rewiredMethod.builder()` runs the
   handed-on gauntlet, whose final fallback asks whether the Builder RETURN TYPE is committable —
   the doomed candidacy lean.
3. The `@Only(before)` classification would bail this chain instead (mislabeled but lean-free) —
   except the call resolves to the ABSTRACT `MethodInfo.builder()`, whose `@Only` arrives via the
   abstract batch an iteration LATER than the impl's; enm is write-once, and whichever iteration
   lands first wins. When eup labels have landed but the abstract `@Only` has not, the lean wins.

The label-kind separation in `commitArguments` (call-excuse eup labels vs value-commit labels) is
IMPLEMENTED (`valuePosition` flag; value positions no longer fold eup labels into a tracked local's
commit set) — it is correct hygiene, all suites green, gate-off Fernflower 0 lines — but it did NOT
cut the residue: the four edges and every composed number are unchanged. **The conflation theory is
falsified as the mechanism for these four**; what remains most plausible is the write-once ordering
(the enm of `rewirePhase1/3` lands in an iteration where the ABSTRACT `builder()`'s `@Only` has not
yet propagated via the batch, so the chain is judged plain-modifying with a committed-ish receiver).
The next investigator should instrument WHICH iteration writes those enm values and what the
receiver's tracked labels are at that moment (`EC_ASSUME_DEBUG` + a one-off print in
`trackAssignment` for the specific locals) before designing further. Note the greatest-fixpoint
also tolerates these four: they retract their members' verdicts, but the flagships still FORM —
the cost is confined to survival, not formation.

## 4c. Round 2 (2026-07-23) — RESOLVED: the residue was the modifying-fluent-setter fallback

The §4b instrumentation mandate was executed with a new iteration-stamped site trace (`EC_SITE_DEBUG`,
house pattern, log-only: per-computation MC/receiver/gauntlet/track lines plus WRITE stamps at every
eventual-property landing, `EventualCluster.ITERATION` set by the iterating loop). The trace killed the
write-once ordering theory immediately: **the abstract `builder()`'s `@Only(before="inspection")` is
already written in iteration 1, before the leaning enm computations run** — every `builder()` call in
the `rewirePhase1/3` walks shows `calleeEventual=@Only(before=…)`, and the transition bail is correctly
rescued by `receiverProvablyNotRoot` (the copies' locals are ∅-tracked).

**The real mechanism** (witnessed by a `lean on <type>` print inside `isEventuallyImmutableFieldType`):
the fluent chain `builder.addComments(comments()).addAnnotations(annotations())…` folds ARGUMENT labels
into the chain value, so the next link runs the handed-on gauntlet with `receiver=[inspection]`,
`receiverCommitted=true` — and the callee is a MODIFYING Builder setter, which never reaches
`handedOnValueSafe`'s independence branch (guarded by `isNonModifyingRead`) and falls through to
`returnTypeHoldsCommittableContent(…Builder)`: the doomed candidacy lean, witnessed 6× per rewire body.

**Fix 1 — the not-root gauntlet short-circuit** (gated): `receiverProvablyNotRoot` is consulted BEFORE
`handedOnValueSafe`. A chain whose base is provably not root-derived (the copy's ∅-tracked builder
local, the `infoMap` parameter) hands on a value of another object's graph; the root-derived content
that flowed in via the arguments is already committed by the labels in acc — the §060 ride-along
stance — so no candidacy lean on the return type is needed. This removed the three enm edges and the
`handleMethodOrConstructor` eup edge, and let previously-bailing methods land labels
(`Value.FieldValue.createVariable` enm=[field], `EvalEquals/EvalImpl` eup, `IsAssignableFrom` enm).

**Fix 2 — transitive freshness** (gated): the surviving fifth edge
(`TypeInfoImpl.copyAllButConstructorsMethodsFieldsSubTypesAnnotations`) was the same shape one
indirection removed: `typeInfo` is FRESH (both branches construct) but tracked non-∅ (ctor args carry
root content), and `TypeInfo.Builder b = typeInfo.builder()` is the local-variable spelling of a chain
`rootedInFresh` excuses inline — the one-pass constructor-call-only freshness never chased through it.
Freshness is now a least fixpoint of `rootedInFresh` over the method's assignment graph: a local is
fresh when every assignment is rooted in a fresh creation, directly or through reads off already-fresh
locals. This also returned the `InfoMapImpl.createSyntheticArrayConstructor:0:methodInfo` eup honestly.

**Result (composed dogfood, stable):** `EC_ASSUME_DEBUG=.Builder` prints NOTHING — 0 edges (from 10 at
§2, 4 after round 1). enm 654→657, eup 303→307, @Only 23, survivors 10, retracted 94; the flagship
family still forms (all in the retracted set). The retraction-root list no longer contains any Builder:
it is now led by `Statement`(7), `Element`(4), `FieldInspection`(3), `VariableImpl` — i.e. §6's quests.
Survivors did not rise above 11 because the flagships also lean on those remaining roots; the Builder
quest's cost was formation-side and is fully paid off. Pins: `TestCommitLabels.INPUT_FLUENT`
(`rewire(Wr)` = the not-root chain, `fillFresh()` = the transitive-freshness local; both ∅ off the
gate). Gate-off Fernflower byte-identity and green suites per the golden rule.

## 5. Definition of done (when someone picks this up)

- The ten §2 edges disappear from `EC_ASSUME_DEBUG=.Builder` output.
- The flagship family still FORMS (retracted-set membership at worst), i.e. the rewire/translate
  enm labels survive via the new excuse; ideally survivors > 11 as the Builder roots vanish.
- The `@Only(before)` classification lands on `builder()` accessors in the FPDUMP
  (`eventual=@Only(before=…)`) and computeTypeLevel excuses them.
- Gate-off three-corpus byte-identity (everything gated); suites green.

## 6. The other two open roots (independent quests)

- **Element/Statement breadth**: ~51 print implementations plus the rewire family behind the
  abstract unions of `Element.print/rewire/variableStream*` — mostly shapes the machinery now has
  (container ride-along, eup), needs per-body chasing.
- **VariableImpl caches** (`cachedFqn`, `cachedHash`, assignable): field finality is deliberately
  not relaxed; a lazy-cache exemption (@IgnoreModifications on the cache fields does NOT currently
  exempt `fieldsAssignable`) is its own small design decision.
