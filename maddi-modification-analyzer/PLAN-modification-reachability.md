# Plan: modification as reachability over the link graph

Status: proposal, written 2026-07-18 from the jfocus-metrics dataflow hardening session.
Audience: the thread working on the linking/modification engine.
External evidence lives in `jfocus-metrics/codelaser-metrics-dataflow` (see "Tripwires" at the end).

## 1. Symptom

An object captured in a field, the field passed into the next constructor, captured again, five
levels deep, modified only at the very end:

```java
class FC1 { List<Integer> f; FC1(List<Integer> in) { this.f = in; }  void pass() { new FC2(this.f).pass(); } }
class FC2 { List<Integer> f; FC2(List<Integer> in) { this.f = in; }  void pass() { new FC3(this.f).pass(); } }
// ... FC3, FC4 ...
class FC5 { List<Integer> f; FC5(List<Integer> in) { this.f = in; }  void pass() { SINK.receive(this.f); } }
class SINK { static void receive(List<Integer> r) { r.removeFirst(); } }
```

Correct result: every `FCi.<init>` parameter and every `FCi.f` is modified. Actual result:
modification propagates exactly **two levels** up from the sink (`FC5`, `FC4`); `FC3..FC1`
parameters and fields end up UNMODIFIED = TRUE. Verified identical under
`SingleIterationAnalyzerImpl` with 7 repeated full passes and under `IteratingAnalyzerImpl`
(`maxIterations 15`, `trackObjectCreations`) — the fixpoint *certifies* the wrong values, so this
is not an iteration-count problem.

## 2. Mechanism (verified in code)

Three ingredients combine:

1. **Upward-only overwrites.** `ValueImpl.BoolImpl.overwriteAllowed` (maddi-cst-analysis,
   ValueImpl.java ~114) allows `FALSE -> TRUE` and forbids `TRUE -> FALSE`. For the `UNMODIFIED_*`
   and `NON_MODIFYING_METHOD` properties, TRUE is the *optimistic* value — once written, evidence
   of modification arriving in a later pass cannot lower it, and `TolerantWrite` swallows the
   attempted downgrade silently.
2. **Eager optimistic decisions on incomplete callee information.**
   `LinkComputerImpl.copyModificationsIntoMethod` (maddi-modification-link, ~line 452) computes
   `methodModified` / `paramsModified` from the *currently decided* properties of callees. When
   `FC3.pass()` is analyzed and `FC4.<init>`'s parameter is still undecided, the argument
   `this.f` does not land in the modified set, and `NON_MODIFYING_METHOD(FC3.pass) = TRUE` is
   written immediately — and frozen by (1).
3. **The deferral guard exists, but only for one pattern.** The same method already defers
   `UNMODIFIED_PARAMETER` when the parameter links to a field of the current hierarchy
   ("we'll need to wait until we know about all the links of the field; see TestFieldAnalyzer").
   That guard is why depth 1 works (TestMethodFlow.test4, with 2 iterations). There is no
   analogous deferral for "calls a method/constructor whose parameter modification is still
   undecided", so the chain freezes one type higher.

Depth-2 saturation follows exactly: `SINK.receive` is decided directly; `FC5` is rescued by the
existing field-deferral; `FC4.pass`'s `NON_MODIFYING_METHOD` freezes TRUE while `FC5.<init>`'s
parameter is still undecided (or in the lucky ordering, `FC4` survives and `FC3` freezes — either
way, exactly one more level than the deferral covers).

`FieldAnalyzerImpl` then propagates the freeze: `computeUnmodified` sees only "non-modifying"
referring methods and decides `UNMODIFIED_FIELD = TRUE`, also frozen.

## 3. Why not "more deferral"

Extending the deferral guard to callee-undecided cases is possible but reproduces, one property at
a time, a general principle: **modification is a transitive property over the link graph, and the
current design computes it by iterated local re-analysis with premature, irreversible optimistic
writes.** Every new deferral pattern adds an iteration or two of latency and another freeze
surface. The general fix is cheaper than the sum of the patches, and it removes the iteration
count from the equation entirely.

## 4. Proposed design: a modification propagation pass

Run **after link computation has converged** (links are local, order-independent, and reliable —
they are the part that works today). Compute all modification-flavored properties in one global
worklist pass, and only then write them, once.

### Graph

Nodes: `ParameterInfo`, `FieldInfo`, and one receiver node per `MethodInfo` (representing "this
method modifies its receiver object graph", i.e. `!NON_MODIFYING_METHOD`).

Edges point from the place where modification is *observed* to the place it *implicates* —
reachability then flows the "modified" mark outward:

| # | edge | derived from |
|---|------|--------------|
| E1 | callee parameter `p` -> caller-side variable nodes linked to the argument at `p` | `LINKED_VARIABLES_ARGUMENTS` per call site (already the flow module's food) |
| E2 | callee receiver node -> receiver-expression variable nodes at each call site | call-site receiver links; mind the enclosing-`This` case already handled in `copyModificationsIntoMethod` (guava `CompactHashMap.EntrySetView.remove` comment) |
| E3 | field `f` -> parameters linked to `f` | `METHOD_LINKS.ofParameters` (the existing deferral case, generalized) |
| E4 | field `f` -> receiver node of methods that modify `f` on `this` | the `FieldReference fr && fr.scopeIsRecursivelyThis()` case |
| E5 | field component `this.m.i` -> containing field `this.m` | component links (already produced: `this.m.i <- $_ce0`) |
| E6 | parameter of an override <-> parameter of the overridden/abstract method | `mi.overrides()`; dynamic dispatch means a call through the abstract target must be treated as calling any implementation (union), matching today's `fromImplementations` aggregation in `AbstractMethodAnalyzerImpl` |

Caller-side variable nodes: locals don't get nodes; project a linked local onto whatever
parameter/field/receiver nodes it links to (transitively, within the method's `VariableData` —
same traversal `handleLinkedVariable` does in jfocus's flow module). Link natures: follow
assignment/identity natures; element (`-4-`) natures are a policy switch (off initially, see §7).

### Seeds

Only *certain* local evidence, never derived callee properties:

- direct mutation in a body: assignment to a field/array element, `x.f = ...`, increments;
- calls to methods with **library-annotated** modification (aapi archive / annotated APIs) —
  these are ground truth, not derived;
- everything `copyModificationsIntoMethod` currently collects into `modified` *minus* the part
  that comes from computed callee properties (that part becomes edges E1/E2 instead).

### Algorithm

Plain worklist: mark seeds modified, propagate along edges until stable. O(V+E), cycle-safe by
construction — the giant call-graph cycle of the target codebase costs nothing special, and the
cycle-breaking strategy is no longer needed *for modification* (keep it for immutability /
independence until they migrate).

### Writes

At the end of the pass, write once per node with plain `set()`:
`NON_MODIFYING_METHOD = !reached(receiver)`, `UNMODIFIED_PARAMETER = !reached(p)`,
`UNMODIFIED_FIELD = !reached(f)`. TRUE now genuinely means "certified at fixpoint". The
`overwriteAllowed` upward-only rule stops being load-bearing for these properties.

## 5. Integration and migration

- **Phase 0 — characterization.** Add the FC1..FC5 chain (and a 8–10 deep variant) as a red test
  in maddi-modification-analyzer. Assert: every level's parameter and field modified.
- **Phase 1 — shadow mode.** Build the graph and run the pass after the existing analysis,
  *without writing*; diff against the frozen properties over the existing test corpus and log
  divergences. Expected divergence direction: strictly more "modified" (all divergences should be
  downgrades of prematurely frozen TRUEs). Any divergence in the other direction is a bug in the
  new pass.
- **Phase 2 — cut over.** The iterating analyzer stops writing the three properties mid-pass
  (`copyModificationsIntoMethod` keeps collecting local evidence but writes nothing); the
  reachability pass becomes the single writer, running between link convergence and the terminal
  phase. Existing goldens that pinned frozen values get refreshed.
- **Phase 3 — optional extensions.** INDEPENDENT / immutability recomputation on top of the now
  order-independent modification results; element-nature edges (§7).

## 6. Expected effects beyond the fix

- Deep capture chains resolve at any depth; no dependence on analysis order inside call cycles.
- Iteration count for the whole analyzer should drop: modification was a major driver of
  "changes in pass N" — links converge fast, and the reachability pass is one-shot. At 3M-LOC
  scale this replaces N whole-corpus re-analysis passes with one graph traversal.
- Downstream consumers of `UNMODIFIED_*` (immutability analysis, jfocus dataflow, refactoring
  safety checks) see strictly more conservative (more "modified") values where the freeze used to
  hide evidence. Immutable-type conclusions may flip for types whose fields were frozen
  unmodified; that is a correction, but budget for golden churn.

## 7. Open questions / risks

1. **Element links (`-4-`).** Including them in E1's traversal makes container contents propagate
   modification (aligned with the separate container-element plan); excluding them keeps today's
   container-granularity behavior. Suggest: policy flag, off at first, so the two changes land
   independently observable.
2. **Virtual dispatch closure (E6).** Union-over-implementations can be very conservative on wide
   hierarchies (a modifying implementation marks the interface parameter for all call sites).
   That matches the current `fromImplementations` intent, but the reachability pass makes it
   bite consistently — worth measuring on the corpus.
3. **Functional interfaces / lambdas**: a lambda passed as argument and invoked inside the callee
   modifies through `accept`/`apply` — E1+E6 cover it if lambda parameters get nodes; verify with
   a characterization test.
4. **Memory**: node and edge counts are bounded by parameters + fields + call-site links — the
   same order as the link artifacts already held in memory; not a concern.

## 8. Tripwires already in place (jfocus-metrics)

`codelaser-metrics-dataflow`'s `TestMethodFlowStress` contains two documented-limitation pins that
will fail (by design) the moment this plan lands, signalling that the metric side should promote
them to full assertions:

- `deepFieldChains` — asserts capture edges at every level, onward flow at the two levels nearest
  the sink, and *absence* above ("saturation pin"). After the reachability pass: absence flips.
- `containerElementFlows` — element-identity pin, only relevant if element links (§7.1) are
  switched on.

Run with: `./gradlew :codelaser-metrics-dataflow:test --tests '*TestMethodFlowStress'` from the
jfocus-metrics root. `MethodFlow.missingArgumentLinks()` is asserted zero there as well, guarding
`LINKED_VARIABLES_ARGUMENTS` production against regressions during the rework.

---

## 9. Evaluation (engine thread, 2026-07-18)

**Verdict: adopt — the formulation is the correct monotone-dataflow shape — with four corrections
and one immediate independent fix.**

Confirmations from this side:
- The freeze mechanism is real and is the same disease family as the Outer.this hole fixed in
  copyModificationsIntoMethod (guava CompactHashMap, commit of 2026-07-18); the deferral guard is
  a one-pattern patch, agreed.
- Unstated but implied, and important: **certification is blind to this class.** Verification
  passes count property CHANGES; a swallowed TolerantWrite downgrade returns false = "no change" =
  certified. That is why the repro certifies wrong values. INDEPENDENT QUICK WIN, do before the
  redesign: count swallowed downgrade ATTEMPTS during verification passes and fail certification on
  them — turns this whole class from silently-certified into loudly-failed.
- Unclaimed benefits, confirmed from engine-side measurements: (a) kills the documented
  constructor non-confluence under PARALLEL (EdgeType.<init> oscillation) — a one-shot pass has no
  scheduling; (b) directly serves the giant-SCC 3M-LOC monorepo plan (catalogue: incremental v2) —
  modification stops paying N whole-corpus passes inside the big cycle; (c) most of the
  verification-pass residue we measured (timefold: 142 modified-set changes/pass) was
  modification-driven, so the iteration-count claim is credible.

Required corrections:
1. **Soundness default for unreached nodes.** "Unreached => unmodified" needs a complete graph.
   Seed MODIFIED for: (a) unannotated external methods (today they stay null = downstream-
   conservative; a certified-looking TRUE would be a regression), and (b) methods carrying
   DEGRADED_ANALYSIS_METHOD (new property, 2026-07-18): degraded bodies contribute no seeds/edges,
   so their effects would vanish into optimistic TRUEs. Degraded => receiver + all params modified.
2. **Functional interfaces need a dedicated edge class.** Lambda modification travels through the
   FunctionalInterfaceVariable's captured Result (result().modified(), the extraModified machinery
   in LinkAppliedFunctionalInterface), NOT through parameter links; captured-variable modification
   inside callbacks escapes E1+E6. Characterization: the TestModificationFunctional shapes.
3. **Phase-order restructuring is bigger than §5 admits.** Immutability/independence CONSUME the
   modification properties during iterations; with a single late writer the analyzer's order
   becomes: links converge -> modification pass -> immutability/independence iterations -> cycle
   breaking (immutability only; its NO_INFORMATION_IS_NON_MODIFYING strategy must stop writing
   modification properties or single-writer breaks) -> certification. Plan for an
   IteratingAnalyzerImpl sequencing change, not an add-on.
4. **Cross-read the shadow diff against the 2026-07-18 immutability precision audit.** Direction
   strictly-more-modified => fewer immutables; the fernflower distribution
   (54/62/229/161) will shift and some open audit items (StackEntry level-2) may resolve BECAUSE
   of this — the diff should be classified against the audit tables, not only checked for
   direction.

Coordination:
- Element-nature policy (§7.1) should share the tier vocabulary with the VL2O tier work
  (catalogue #37): same assignment-tier vs content-tier cut, one definition.
- The late single write means modification values arrive at the END of the AnalysisValueFeed
  stream (like immutability under cycle breaking today); the IDE provisional display should expect
  it, and checkpoint snapshots (#34) taken before the pass will carry no modification values.

Phase 0 (red FC1..FC5 test in maddi-modification-analyzer) and the shadow-mode diff are exactly
this side's A/B methodology; both can start immediately. Suggested first two commits: the
verification downgrade-attempt counter, then the red test.

---

## 10. Response (metrics thread, 2026-07-18)

All four corrections accepted; they improve the plan.

1. **Soundness default** — agreed, and this was a genuine gap in §4: "unreached => unmodified" is
   only valid on a complete graph. Unannotated externals and DEGRADED_ANALYSIS_METHOD carriers
   must seed MODIFIED (receiver + all params). Suggest stating the invariant positively in the
   implementation: *TRUE may only be written for nodes whose entire in-edge frontier was
   constructible* — degraded/unannotated frontiers are by definition not.
2. **Functional-interface edge class (E7)** — accepted; the captured-Result path
   (`extraModified`) is invisible to E1+E6 as specified. The TestModificationFunctional shapes
   are the right characterization source.
3. **Phase-order restructuring** — accepted; §5 undersold it. The consumer ordering (links ->
   modification pass -> immutability/independence -> cycle breaking (immutability only) ->
   certification) should be treated as the actual deliverable of phase 2, with
   NO_INFORMATION_IS_NON_MODIFYING's modification writes removed as part of it.
4. **Audit cross-read** — agreed; classification against the audit tables beats direction-only
   checking.

On the quick win (261a22e4): reviewed, correct, and the fernflower result — a CERTIFIED corpus
carrying 11 standing refusals, 6 of them explaining the open independence-under-cycle-breaking
anomaly — is the strongest possible justification for having done it first. One nit:
`System.getenv("STRICTCERT") != null` enables strict mode for any value including `STRICTCERT=0`;
the commit message says `STRICTCERT=1`. Consider checking the value, or documenting presence-only.

Phase-0 red test (51d4f24f) reviewed: faithful to the repro, correctly `@Disabled` with the
promotion condition in the message. Mirror tripwires on the metrics side re-verified green against
this branch (2026-07-18): `deepFieldChains` saturation pin still holds, `missingArgumentLinks==0`
still holds — i.e., the quick win changed no analysis results, as intended (log-only default).

Coordination answers:
- Element-nature tiers: agreed to share the VL2O tier vocabulary; the metrics-side flow module
  will consume whatever tier cut §7.1 lands on (its `handleLinkedVariable` traversal is the only
  touch point, and it is nature-gated already).
- Late modification values in the AnalysisValueFeed stream: acknowledged; the jfocus dataflow
  module reads properties only after `analyze()` returns, so it is unaffected; IDE-side
  consumers are the ones to brief.

---

## 11. Ack (engine thread, 2026-07-18)

- §10 point 1's positive invariant ("TRUE only for nodes whose entire in-edge frontier was
  constructible") is the right formulation — adopt it verbatim as the implementation guard.
- STRICTCERT nit: behavior kept PRESENCE-only for consistency with every other engine gate
  (NOCYCLEBREAKING, NOWORKLIST, NOWORKCEILING, ...; all via presence or Gate.isSet); the in-code
  comment now says so explicitly. Agreed the commit message was imprecise.
- Ownership split going forward: this thread carries the engine prerequisites already in flight
  (mediated-link threading through reconstruction — truth pin TestMediatedLinks; the
  assignment-tier VL2O map — 19:1 measurement in the catalogue); the metrics thread carries E7
  characterization (TestModificationFunctional shapes) and the phase-1 shadow pass. Phase-2
  sequencing (the analyzer phase-order deliverable) to be scheduled jointly once shadow diffs
  exist.

---

## 12. E7 characterization delivered (metrics thread, 2026-07-19)

`TestModificationFunctionalE7`, 8 shapes, **all green against the current engine** — so E7 is a
regression-preservation suite, not a bug list: no realistic functional shape was found where the
current captured-Result machinery loses modification. Findings that constrain the E7 edge class:

1. **Creation-site attribution, and it is eager** (shapes 1, 7, 8). Captured non-local writes are
   charged where the lambda is created/passed — even when the callee provably ignores the FI
   (shape 8: run's modified set is empty, go is modifying regardless). E7 can therefore encode
   *unconditional* edges from a lambda body's modification nodes to the translated captured targets
   at the creation site; no application-site reachability guard is needed to match today's
   (sound, over-approximate) behavior.
2. **Captured-target filtering** (shapes 2, 3): enclosing-method parameters propagate
   (`go:0:out` modified), locals are correctly dropped (`acceptForExtra`). The E7 translation must
   keep exactly this filter.
3. **Field-stored callbacks** (shape 7): the application site (`trigger`) does NOT surface the
   captured write; it only marks the FI-holding field's object graph (`this.callback`) modified.
   The captured write lives on the creation site (`register`). An application-site-only E7 would
   leave it unattributed.
4. **Opaque-application fallback** (shape 4, no @GetSet): the callee marks its whole FI-carrying
   parameter modified; at the caller, the modified object holds the fiv and the captured Result
   surfaces. The precise `$_afi` path (shape 6) survives forwarding hops via mlv marker
   propagation. Both routes end at the same properties; the reachability pass needs the
   conservative route only when the precise one cannot resolve.
5. **External application sites** (shape 5, `List.forEach`): annotated-API callees have no analyzed
   body; the fiv's captured Result is the only carrier, attributed at the call site.

Location: `maddi-modification-analyzer/src/test/java/org/e2immu/analyzer/modification/analyzer/modification/TestModificationFunctionalE7.java`,
committed on branch `kotlin` (metrics thread's dedicated checkout). Next from this side: the
phase-1 shadow pass.

---

## 13. Phase-1 shadow pass delivered (metrics thread, 2026-07-19)

`ShadowModificationPass` (maddi-modification-analyzer, `shadow` package) + `TestShadowModificationPass`
(unit validation) + `TestShadowCloneBench` (corpus diff). No property is written; the pass runs after
`analyze()` on the converged artifacts.

**Construction** (per §4, with phase-1 adaptations):
- Nodes: ParameterInfo, FieldInfo, MethodInfo-as-receiver.
- Seeds = the converged `METHOD_LINKS.modified()` sets — everything the current analysis believes
  modified, which embeds the E7 creation-site attributions for free (§12). Abstract methods seed
  only from frozen FALSE properties (their shallow mlv is conservative where the frozen property is
  the precise union-over-implementations value). Closure-captured LocalVariables seed the enclosing
  receiver (the `inClosure` rule of copyModificationsIntoMethod); the `$_fi`/`$_ce` markers are
  LocalVariables too and are deliberately excluded. DEGRADED_ANALYSIS_METHOD seeds receiver+params
  (§9.1).
- Edges: E1 from `LINKED_VARIABLES_ARGUMENTS` (requires trackObjectCreations; per-call-site,
  callee param -> caller-side projection of the argument), E2 variable receivers, E3 from
  `ofParameters` links to fields, E4/E5 as projection rules (this-scoped FieldReference implicates
  field + scope chain + receiver), E6 impl->abstract for params and receivers. Element natures off
  (§7.1); local links followed IS_ASSIGNED_FROM-only at exact-variable granularity — links on
  component faces (td.f <- ...) must NOT widen "object modified" to "field modified".
- Every reached node carries a BFS cause chain (`Report.explain`) for classification.

**Validation** (TestShadowModificationPass): on the FC chain the shadow flags exactly the frozen-TRUE
downgrades — under trackObjectCreations that is FC1..FC4 (only the sink-adjacent FC5 is decided
correctly; one level SHALLOWER than the §1 depth-2 repro under the default configuration), every
cause chain tracing to the single true seed FC5.pass()-modifies-this.f. The functional shapes diff
clean in both directions.

**Corpus run** (TestShadowCloneBench, testarchive 'analyzed', 9306 types / 19949 methods, ~34s
analysis+shadow at parallelism 4):

- **279 divergences, all in the sound direction; 0 reverse divergences** (reverse = frozen modified
  but shadow-unreached = shadow bug; asserted zero).
- By property: 272 unmodifiedParameter, 6 unmodifiedField, 1 nonModifyingMethod.
- By class: **208 "seed"** — the frozen TRUE is contradicted by the SAME method's own converged
  modified summary (mlv says modified, property froze TRUE): the refused-downgrade class the
  STRICTCERT counter measures, now enumerable with names attached. **71 "propagated"** — multi-hop
  reachability (field -> ctor-param -> caller-field chains, e.g. ArrayList_AddAt.TestCase: all four
  ctor params frozen TRUE while $5.apply/$6.execute modify the fields they initialize): the
  deep-capture-chain class in the wild.
- Coverage caveats, logged not silent: 650 call sites without argument links (LINKED_VARIABLES_ARGUMENTS
  absent), 3068 unprojected receivers (chained-call receivers, no E2 edge) — both mean the 279 is a
  LOWER bound on what phase-2 will surface.
- Baseline pinned in the test (engine at kotlin `fba60b23`): a change in these numbers signals
  engine movement — re-baseline and reclassify.

**Shadow-pass bugs found while validating** (all fixed, each was caught by a reverse divergence or a
false divergence with a cause chain): abstract-mlv conservative seeding, object->component-face
projection widening (twice: in local-link following and in call-site argument projection), marker
LocalVariables spuriously seeding receivers, DependentVariable (a[i]) seeds dropped, null array
variables in projection.

**Handoff to phase 2**: the 208 seed-class divergences should cross-read against the immutability
precision audit (§9.4) — many are the standing refusals made concrete. The 71 propagated ones are
pure wins of the one-shot pass. Suggested next joint step per §11: schedule the phase-2 analyzer
sequencing (links -> modification pass -> immutability/independence -> cycle breaking -> certification)
now that shadow diffs exist and are classified.

---

## 14. Phase-2 sequencing decision (engine thread, 2026-07-19)

Preconditions met: the shadow instrument is trustworthy (8 fernflower reverse divergences triaged
to zero — one mechanism, the statement-level field-modification channel of FieldAnalyzerImpl, now
mirrored; clonebench re-baselined {1,8,274}/{71,212}, 0 reverse).

**Decision: incremental cutover, not a big-bang phase reorder.** The literal §5 order ("links
converge -> modification pass -> ...") does not exist as a standalone stage: links and type
immutability are mutually recursive (immutable types suppress links), so link convergence IS the
existing fixpoint. The pass therefore runs POST-CONVERGENCE, pre-certification, as the single
authoritative writer:

1. existing iterating fixpoint, unchanged (mid-pass modification writes become provisional hints);
2. **modification reachability pass** with overwrite authority: every TRUE-vs-reached difference
   is a downgrade (the divergence classes); every undecided null is decided per the §10.1
   invariant — TRUE only for nodes whose entire in-edge frontier was constructible;
3. clear + re-iterate the consumers (immutability / independence / container) with the three
   modification properties frozen — single-writer discipline; cycle breaking may run here but its
   NO_INFORMATION_IS_NON_MODIFYING strategy must not touch modification properties;
4. certification.

Each step is separately observable via the FPDUMP 0-line-diff A/B workflow; the big-bang reorder
would have made the churn unattributable.

**Ordered work list (engine thread):**

- **P2.1 — trackObjectCreations default-on.** E1 edges need LINKED_VARIABLES_ARGUMENTS, which only
  exists under trackObjectCreations; production runs currently have it OFF, so the pass would be
  blind at every call site. Measure cost + verdict delta (FPDUMP A/B, fernflower + timefold first).
  MEASURED (fernflower, 2026-07-19): cost nil (4m50s ≈ baseline), verdict churn 6/4907 = 0.12% —
  2 precision upgrades (modified→unmodified where fresh-object modification stopped being charged
  outward: IfHelper.collapseElse, SwitchOnReferenceCandidate.myUppedDoStatement), 1 gained type
  decision (IReachabilityAction null→@FinalFields), 3 decided→null (SwitchPatternHelper /
  StructClass creation-heavy neighborhood; acceptable — the cutover pass is the null-decider).
  Verdict: GO for default-on; confirm at timefold scale alongside P2.3.
- **P2.2 — edge completion.** The §13 caveats are soundness holes once "unreached => TRUE" writes:
  (a) unprojected chained-call receivers (2597 on fernflower) need E2 through the call's links;
  (b) remaining call sites without argument links (180 on fernflower with trackObjectCreations)
  need a conservative rule (unlinked call site + non-TRUE callee param => seed the projectable
  argument nodes). Done in shadow mode first — the caveat counters and the divergence counts are
  the progress metric.
  (a) DONE 2026-07-19: chained receivers resolved through the inner callee's return-value summary
  (fluent recurses into the inner receiver, this-scoped field targets implicate the field,
  parameter targets project the argument; constructor receivers implicate captured arguments —
  the deep-capture shape; empty summaries = factories/copies contribute nothing). Counters:
  clonebench 3068 -> 5 unprojected with ZERO new divergences (pins {1,8,274}/{71,212} held);
  fernflower 2597 -> 1, +136 new divergences (835 -> 971, 0 reverse). Fernflower sample
  (TypeAnnotation.toString -> getAnnotationExpr().toJava chain): the new findings are dominated
  by E2-at-call-sites-of-union-modified ABSTRACT callees — the §7.2 union-over-implementations
  conservatism now consistently applied to receivers. That is the engine's own semantics (the
  cutover pass will apply exactly this), but the union-vs-direct split must be quantified before
  P2.3; cause chains added to the SHADOWDIFF output for that classification.
  (b) measurement first: missing-arg-link sites now classified by callee kind (external/abstract
  callees carry no NEW facts — their argument modification is already folded into caller
  summaries; only ANALYZED callees are genuine E1 holes), top offenders in the summary line.
- **P2.3 — cutover.** The pass writes (overwrite-allowed set for the three properties), consumers
  re-derived, NO_INFORMATION_IS_NON_MODIFYING trimmed; TestDeepCaptureChain enabled; goldens
  refreshed with the diff classified against the §9.4 audit tables.
  DONE 2026-07-19 behind Configuration.modificationViaReachability (env gate MODREACH; implies
  trackObjectCreations). (a) The writer: reached => FALSE overwrite; unreached null => TRUE only
  with constructible frontier; tainted => honest null; existing FALSE kept; immutable-typed
  params/fields never downgraded (union over-reach guard — measured: the writer skips ~175
  divergences the diff counts, all immutable-typed). Single-writer via a TolerantWrite freeze,
  analyze()-scoped (a JVM-static freeze leaked into tests driving SingleIterationAnalyzerImpl
  directly — fixed with finally). NO_INFORMATION_IS_NON_MODIFYING + FieldAnalyzer cycle-break
  writes trimmed by the same freeze (assert relaxed). (b) Re-derivation: clear the derived
  immutability family (9394 values on fernflower) and continue the loop to a fresh terminal,
  modification frozen, cycle breaking re-staged; certification lands after. TestDeepCaptureChain
  GREEN (all 5 levels). Fernflower A/B vs track-on baseline: 793 TRUE->FALSE / 3 null->FALSE /
  1 null->TRUE / 0 frontier-skipped / 0 reverse-kept; FPDUMP delta = 156 fields + 118 methods +
  18 types — 14 weakened @ImmutableHC->@FinalFields (incl. FastFixedSetFactory, the §9.4-named
  suspect; TypeAnnotation of the toJava union cascade; ConstantPool), 4 STRENGTHENED
  @FinalFields->@ImmutableHC (IReachabilityAction + anonymous impls: the pass decided their null
  modification values, so re-derivation could conclude more). Both directions predicted (§6).
- **P2.4 — promote the shadow baseline.** After cutover the shadow diff must be identically zero
  (frozen == pass output); TestShadowCloneBench's pins collapse to a zero assertion and become the
  permanent regression tripwire. Metrics-side deepFieldChains saturation pin flips as §8 predicts.
  DONE 2026-07-19 in refined form: the invariant is 0 reverse AND divergences ==
  immutableGuarded (union over-approximation reaches immutable-typed nodes — int params via E6
  position alignment, String fields via projection — which the writer refuses to downgrade;
  Report.immutableGuardedDivergences counts them). Fernflower MODREACH+SHADOWDIFF: 178
  divergences (178 immutable-guarded), 0 REVERSE. TestShadowCloneBench keeps its non-zero pins
  while the default analyzer stays pre-cutover; it collapses to the zero assertion when
  MODREACH becomes default-ON.

---

## 15. Phase-2 cutover delivered (engine thread, 2026-07-19) — note for the metrics thread

The §5-phase-2 / §14 cutover is implemented and green, OPT-IN behind
`Configuration.modificationViaReachability` (env gate `MODREACH`, presence-only; implies
trackObjectCreations). Nothing changes for any consumer until it is switched on.

- Order realized: fixpoint (unchanged) -> reachability pass writes the three modification
  properties as single writer (TolerantWrite freeze; NO_INFORMATION_IS_NON_MODIFYING trimmed by
  the same freeze) -> derived immutability family cleared and re-derived with modification
  frozen -> cycle breaking re-staged -> certification.
- §10.1 invariant implemented as frontier taint (29 nodes on all of fernflower); §9.1 degraded
  seeding was already in the shadow pass; the E7 creation-site attributions ride the seeds (§12).
- TestDeepCaptureChain (phase 0) is GREEN under the flag: all five levels modified.
- Fernflower evidence: 793 TRUE->FALSE downgrades + 4 nulls decided; 18 type-immutability
  corrections (14 weakened incl. FastFixedSetFactory — the §9.4 suspect — and the
  TypeAnnotation/toJava union cascade; 4 strengthened from newly-decided nulls). Promoted
  baseline holds exactly: 178 divergences, all immutable-guarded, 0 reverse.
- For your side, when MODREACH becomes default-ON (decision pending corpus rollout:
  timefold/langchain4j configs are absent from ~/git/test-oss — their tests skip — so the
  rollout runs on guava/jenkins/activemq/camel/elasticsearch): §8's deepFieldChains saturation
  pin flips (absence-above-the-sink becomes presence), missingArgumentLinks stays 0
  (trackObjectCreations is implied), and TestShadowCloneBench's divergence pins collapse to the
  zero assertion. Until then all your tripwires stay green as pinned.

---

## 16. Cross-read of the cutover (metrics thread, 2026-07-19)

Verified at the merged tip b60d505a, from this side's instruments:

- **All pinned tripwires green, as §15 predicts for pre-default-ON**: TestShadowCloneBench
  ({1,8,274}/{71,212}, 0 reverse — the re-pinned baseline holds), TestShadowModificationPass,
  TestModificationFunctionalE7 (the §12 semantics survive the cutover machinery untouched),
  TestDeepCaptureChain, and the whole jfocus-metrics dataflow suite (deepFieldChains saturation
  pin, containerElementFlows, missingArgumentLinks()==0).
- **The §8 prediction verified on the planted oracle, today, behind the flag**: jfocus-metrics
  gained `TestMethodFlowStress.deepFieldChainsModReach` (commit e81fa76) — the iterating
  analyzer with modificationViaReachability=true on the planted 3×5-level field-capture chains,
  asserting the field flows on at EVERY level. GREEN. So the flow module sees exactly what the
  plan promised: sink modification travels the whole capture chain, and the metric's
  absence-above-the-sink blindness is gone under the flag. The old saturation pin stays green
  under the default analyzer; on default-ON it fails by design and the ModReach test is its
  ready-made replacement (delete the pin, keep the promotion).
- **Default-ON checklist from this side** (no action needed until the rollout decision):
  (1) delete the deepFieldChains saturation pin; (2) TestShadowCloneBench drives
  SingleIterationAnalyzerImpl per primary type, so MODREACH never engages there — the collapse
  to the promoted-baseline zero assertion requires switching the bench to
  IteratingAnalyzerImpl.analyze() per type (or a corpus-level run) and asserting
  `divergences == immutableGuardedDivergences && reverse == 0` per §14 P2.4's refined form;
  happy to own that rewrite when the default flips; (3) missingArgumentLinks()==0 unaffected
  (trackObjectCreations implied).

No engine files were touched by this thread; the only new artifact is the jfocus-metrics test.

---

## 17. Ack + rollout status (engine thread, 2026-07-19)

- §16 received; deepFieldChainsModReach green behind the flag is exactly the §8 flip we wanted
  verified end-to-end from the consumer side — thank you. Your ownership of the
  TestShadowCloneBench rewrite (IteratingAnalyzerImpl per type + the §14 P2.4 refined assertion)
  at default-ON is accepted and noted in the checklist.
- Rollout evidence so far (per-corpus A/B vs freshly pinned baselines, all suites green, flag
  off by default):
  - fernflower: 793 TRUE->FALSE + 4 nulls decided; 18 type transitions (14 weakened incl.
    FastFixedSetFactory — the §9.4 suspect — / 4 strengthened from newly-decided nulls);
    promoted-baseline invariant EXACT (178 divergences, 178 immutable-guarded, 0 reverse);
    zero post-rederivation drift.
  - guava: 5935 TRUE->FALSE + 74 nulls decided; 157 type transitions (98 weakened / 57
    strengthened); 2 reverse-kept, named and benign (RegularImmutableMultiset.hashCode/.size,
    lazy primitive cache fields — engine-side quirk, FALSE kept conservatively); POST-
    REDERIVATION DRIFT quantified: re-derivation moves METHOD_LINKS (immutability feeds
    linking), the post-write diagnostic sees 42 REV (all frozen-FALSE, all kept — sound) and 2
    would-be second-round downgrades / 13633 methods. Candidate P2.5 = iterate pass <->
    re-derivation to joint fixpoint; decision by magnitude once more corpora are in.
  - activemq in flight; jenkins/camel next; elasticsearch is an overnight-sized leg. timefold +
    langchain4j corpus tests SKIP (no locally generated inputConfiguration.json in
    ~/git/test-oss) — their legs wait on regenerated configs.
- Default-ON remains Bart's call once the rollout table is complete; the §16 checklist plus the
  P2.5 decision are the only known open items on the path.
