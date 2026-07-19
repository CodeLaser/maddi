# sv engine — live state and open items

> This file is the LIVE summary. The full engineering journal (all dated UPDATE sections since the
> original link-failure catalogue closed) is in **`sv-journal.md`**. Design references:
> `sv-reconstruction-techniques.md` (reconstruction techniques + guards), `sv-engine-handoff.md`
> (sv-doc entry point), `linking-manual.md` (link module manual), `org-review-2026-07-18.md`
> (module organization review + 13-point plan).

## HANDOFF (2026-07-19 evening) — mechanical steps anyone (or any session) can run

### UPDATE (early evening): SHADOW-PASS PRECISION — the divergence economy re-partitioned (8821a434)

Fable-session deep dive (cause-chain forensics on the fernflower cascades): the dominant
"union conservatism" reading was WRONG for the biggest family. The engine was RIGHT to keep the
frozen TRUEs in the gatherGenerics family — the SHADOW over-approximated, twice:
(1) E3 built field->param edges for ALL link natures; the content-tier chain upper ∋ left ∈
this.args (element read OUT of the parameter, stored in the field, field cleared) widened the
destination container's modification back to the source — precisely what the engine's own
relevantLinkForModification rule refuses. Fixed: E3 mirrors that rule verbatim.
(2) The closure propagated THROUGH immutable-typed nodes (String params/fields) which cannot
transmit modification. Fixed: closure cuts at immutable-typed nodes.
TestElementFlowWidening pins the precise behavior in three escalating shapes (all green — the
engine's linking was precise on all of them; the defect was shadow-only).
NUMBERS: fernflower 971 -> 452 divergences (53% artifacts), immutable-guarded 178 -> 17,
0 reverse throughout; clonebench seed class UNTOUCHED at 212, propagated 71 -> 12, re-pinned.
CONSEQUENCE FOR THE ROLLOUT TABLE: the MODREACH downgrades measured earlier (ff 793, guava 5935,
amq 823, jenkins 2893, camel 88/650) include artifact downgrades — RE-MEASURE all corpora with
the precise pass before the default-ON decision (same one-command legs).
FERNFLOWER RE-MEASURED (precise pass): **432 downgrades** (was 793; 45% artifacts), 0 reverse
kept, joint fixpoint in 2 rounds — and the 18 type-immutability corrections are the IDENTICAL
18 types as before (verified set-equal): the artifact downgrades were scattered and never
immutability-load-bearing; the genuine core (FastFixedSetFactory & co) is robust to the
precision fixes. Strong signal that the corrected corpus table will keep the type-level story
while shrinking the member-level churn.

### UPDATE (late afternoon): #35 phases B/C/D delivered

- IncrementalState (prepwork io): per-primary-type SOURCE + OUTPUT fingerprints + consumption
  edges (type-lifted), one tolerant JSON next to the checkpoint; CHECKPOINT arms the recorder.
- RunAnalyzer gate INCREMENTAL=<dir of prior CHECKPOINT run>: restore values, changed types by
  source fingerprint, seed the early-cutoff worklist, persisted consumption edges unioned into
  the wake relation (closes the value-mediated-flow gap on resume). INCREMENTAL_FILL opt-in
  re-analyzes unrestored elements (measured: floods, slower than cold — default reports holes).
- Fernflower smoke: cold+checkpoint 147s -> untouched resume **7s**; TestIncrementalConsumptionWake
  green (comment edit cuts off, semantic edit wakes the consumer through the RECORDED edge).
- #35 REMAINING: restore-coverage tail (the shared codec fix list — 72 fully-empty elements +
  partial decodes = ~2.3k null verdicts on fernflower resumes); ES-scale touch-one-file
  experiment; element-granularity fingerprints if type granularity proves too coarse on the
  monorepo. The ES overnight run now ALSO writes incremental state: tonight's checkpoint dir is
  directly usable for the first ES-scale INCREMENTAL experiment.

The phase-2 cutover is COMPLETE and validated; what remains is evidence gathering and two
decisions. Exact commands (all from ~/git/maddi, all via bin/gradle-locked.sh):

0. **GOTCHA for every big-corpus run: TESTXMX=24G** (maddi-run-openjdk test JVM heap, default
   8G — an 8G elasticsearch sweep dies OOM ~32 min in, and the sweep REPORTS exit=-1 without
   failing the build; grep 'SWEEP SUMMARY'). All the ES commands below need the env.
1. **Remaining corpus legs** (baseline first if no build/imm-<corpus>-*.txt.gz pin exists, then
   MODREACH; diff the two FPDUMPs; expect: downgrades + null-decisions, type transitions in both
   directions, 0-or-few reverse-kept — all four corpora so far fit this):
   `FPDUMP=/tmp/imm-X-base.txt bin/gradle-locked.sh :maddi-run-openjdk:test --tests TestCamelCore --rerun -PskipCloneBench`
   `MODREACH=1 FPDUMP=/tmp/imm-X-mr.txt bin/gradle-locked.sh :maddi-run-openjdk:test --tests TestCamelCore --rerun -PskipCloneBench`
   Corpus test classes: TestCamelCore (8 modules), TestElasticsearch2 (5h+, overnight; baseline
   already pinned build/imm-elasticsearch-2-2026-07-19.txt.gz). timefold/langchain4j SKIP until
   their inputConfiguration.json is regenerated in ~/git/test-oss.
2. **ES overnight verification** (pre-cutover measurements, unchanged command from the killed
   run): plain run first (type-null verdict + CONSEDGES + CHECKPOINT=<dir> dogfood — wave-boundary
   protection is now live), MODREACH leg after/separately.
3. **Default-ON flip** (Bart's decision): set `modificationViaReachability` default true in
   IteratingAnalyzer.Configuration + ConfigurationBuilder (or invert the RunAnalyzer gate);
   expect suite golden churn in maddi-modification-analyzer (modification + immutability pins);
   metrics thread then executes their §16 checklist (delete deepFieldChains saturation pin —
   deepFieldChainsModReach is the replacement; rewrite TestShadowCloneBench to
   IteratingAnalyzerImpl + assert divergences==immutableGuarded && reverse==0).
4. **P2.5** is IMPLEMENTED AND VALIDATED (jenkins: round 1 = 2893 downgrades + 1016 decided,
   round 2 = exactly the 2 drift downgrades, round 3 = 0 => joint fixpoint; final invariant
   holds — 1001 divergences all immutable-guarded; 79 REV all frozen-FALSE conservative keeps).
   NOTE the receiverChain memoization (33c9b8fc) is LOAD-BEARING: without it the chained-receiver
   projection is exponential in expression nesting (jenkins hung >50 min, jstack-confirmed) —
   any big-corpus MODREACH run needs that commit.
   ROLLOUT TABLE (downgrades / nulls decided / type transitions weakened+strengthened /
   reverse-kept): fernflower 793/4/14+4/0 · guava 5935/74/98+57/2 · activemq 823/58/9+13/0 ·
   jenkins 2893/1016/47+82/9 (setter-family cluster named in the log + 2 test-mock primitives;
   triage batch for the successor) · camel-util 88/13/-/0 · camel-support 650/31/-/2 (both joint
   fixpoint in 2 rounds; dumps pinned in build/). Jenkins also shows 251 analyzed-callee
   missing-arg-link sites (top: IconSet.addIcon=82) — a real E1 gap, honestly tainted (no
   optimistic TRUE there). Consistent picture across all six corpora: corrections dominate on
   well-decided corpora, null-deciding dominates on under-decided ones; both directions are the
   pass being MORE decided. ES verification (plain leg: type-null vs 23206 baseline + CONSEDGES +
   CHECKPOINT dogfood incl. wave-boundary deltas) LAUNCHED 2026-07-19 afternoon as a background
   run — results in build/es-checkpoint-2026-07-19 + the FPDUMP; the MODREACH ES leg is the one
   remaining big A/B (run it only at 33c9b8fc or later — the memoization commit).
5. Open engineering (designed, measured, not built): #35 frontier integration
   (DESIGN-incremental-v2.md + Phase A numbers: closure NO-GO, direct-edge GO); #39 step 2
   (awaits jfocus owner's EIDEDUP_SHADOW data); #42 refinement (compute 'wholly cast-mediated'
   from Link.mediated() — the basic @Modified(downcast/downcastTo) annotation + decorator
   emission are DONE).

## CURRENT STATE (2026-07-19, end of the post-merge session — read this first after compaction)

### UPDATE 2026-07-19 (phase-2 session, later same day) — at 5c880b3c

- Reverse-divergence triage DONE (2beee0f9): 8 -> 0, one mechanism (statement-level field
  modification via non-this scopes), channel mirrored in the shadow pass; clonebench re-pinned
  {1,8,274}/{71,212}, 0 reverse.
- Wave-boundary checkpoint fix DONE (31a6e60c): waveCompleted feed + 60s-throttled deltas at the
  strata barriers; the cold first pass now checkpoints continuously. ES verification rerun is
  READY TO LAUNCH in the next quiet window (unchanged command; benefits from the wave fix).
- Phase-2 sequencing DECIDED and written as PLAN §14: incremental cutover (fixpoint unchanged ->
  post-convergence reachability pass with overwrite authority as single writer -> consumers
  re-derived -> certification). P2.1 trackObjectCreations GO (cost nil, 0.12% churn); P2.2a
  chained-receiver E2 DONE (clonebench 3068->5 unprojected/0 new divergences, fernflower 2597->1,
  +136 all union-conservatism downstream of a few hub seeds — the cutover-churn map); P2.2b
  measured tiny (27 analyzed-callee sites of 179).
- **P2.3 CUTOVER DONE 2026-07-19** behind Configuration.modificationViaReachability / env MODREACH
  (implies trackObjectCreations; OFF by default, all suites green both ways). (a) writer:
  reached=>FALSE overwrite, unreached-null=>TRUE only with constructible frontier (§10.1),
  tainted=>honest null, existing FALSE kept, immutable-typed params/fields never downgraded;
  single-writer via analyze()-scoped TolerantWrite freeze (trims NO_INFORMATION_IS_NON_MODIFYING
  and FieldAnalyzer cycle-break writes for free). (b) re-derivation: clear derived immutability
  family (9394 values on ff), continue loop to fresh terminal with modification frozen.
  **TestDeepCaptureChain GREEN** (phase-0 acceptance, red since 2026-07-18). Fernflower A/B vs
  track-on baseline: 793 TRUE->FALSE + 3 null->FALSE + 1 null->TRUE, 0 frontier-skipped, 0
  reverse-kept; FPDUMP delta 156 fields + 118 methods + 18 types (14 weakened
  @ImmutableHC->@FinalFields incl. FastFixedSetFactory = the §9.4-named suspect; 4 STRENGTHENED
  from newly-decided nulls). P2.4 promoted-baseline invariant: under MODREACH the re-run diff
  shows 0 reverse + divergences == immutable-guarded count (union over-reach on int/String
  nodes the writer refuses; Report.immutableGuardedDivergences counts them).
- Corpus rollout leg 2 — GUAVA (baseline freshly pinned build/imm-guava-2026-07-19.txt.gz +
  modreach dump alongside; both runs green): 5935 TRUE->FALSE + 13 null->FALSE + 61 null->TRUE,
  0 frontier-skipped, 2 REVERSE KEPT (first nonzero — engine-FALSE/pass-unreached, a shadow-gap
  channel guava exercises that fernflower didn't; writer keeps them conservatively but they need
  NAMES — add logging — and a triage like the original 8). 38301 derived values cleared. FPDUMP
  delta: 1971 methods + 410 fields + 157 types; type transitions: WEAKENED 98 (44 ImmHC->FF,
  24 ImmHC->Mut, 13 FF->Mut, 17 Imm->lower), STRENGTHENED 57 (39 FF->ImmHC, 16 Mut->ImmHC) —
  the pass's null-deciding makes re-derivation MORE decided in both directions. Caveat: A/B
  conflates track-on churn with cutover churn (no separate track-only guava leg; ff evidence
  says track-on churn is tiny). Note: timefold + langchain4j corpus tests SKIP — no locally
  generated inputConfiguration.json in ~/git/test-oss for them.
  Reverse-kept NAMED (writer logs them now): RegularImmutableMultiset.hashCode + .size — lazy
  primitive cache fields the engine holds FALSE on (an int field has no object graph; engine-side
  quirk); kept conservatively, LOW priority.
  POST-REDERIVATION DRIFT (guava MODREACH+SHADOWDIFF): the diagnostic pass AFTER re-derivation
  sees 818 divergences (816 immutable-guarded, 2 NOT) + 42 diagnostic-REV (e.g.
  SimpleStatsCounter.recordHits: LongAdder-field modification, engine-FALSE correct and KEPT —
  the re-derived immutability suppressed the link that seeded it). Cause: re-derivation moves
  METHOD_LINKS (immutability feeds linking), so the pass's input artifacts shift after the write.
  No unsoundness (all frozen FALSE kept); magnitude of the residual: 2 would-be-downgrades /
  13633 methods. Candidate P2.5: iterate pass <-> re-derivation to joint fixpoint; decide by
  magnitude at corpus scale. Fernflower shows zero drift.
  REMAINING: jenkins/activemq/camel legs (+ES MODREACH leg overnight-sized); §9.4 audit
  cross-read of the guava strengthened/weakened sets; P2.5 decision; metrics-thread notification
  delivered as PLAN §15; decision on default-ON (Bart's, once rollout evidence is in).
- New task #42 queued by the user: DOWNCAST presentation (@Modified(downcast=...) design opinion
  delivered; DecoratorImpl already half-implements it; @Modified lacks the element declaration).

- Branch `sv-integration` at e2797cdf, tree clean, merged BOTH ways with origin/kotlin (incl. the
  metrics thread's E7 suite + ShadowModificationPass and the kotlin thread's early-cutoff
  worklist). All suites green on the merged base.
- DONE this session: #33 both fronts (elasticsearch SWEEP GREEN, first ever); #34 checkpoint v1
  incl. production gates + decode fixes (85% restore coverage); #35 Phase A measured (closure
  NO-GO, direct-edge frontier GO, median wake-set 5); #39 engine side complete + jfocus shadow
  slice verified end-to-end (the original falsifier correctly classified); type-null breaking
  fix (guava 100% coverage; ES verification PENDING RERUN — the run was killed externally);
  §9.4 cross-read executed (FastFixedSetFactory named by both evidence lines; 836 fernflower
  divergences via the new SHADOWDIFF gate).
- NEXT, in rough order: (1) DONE 2026-07-19 — the 8 reverse shadow divergences triaged to zero
  (see below); (2) rerun the ES verification overnight — the wave-boundary checkpoint fix is IN
  (AnalysisValueFeed.waveCompleted default method; SingleIterationAnalyzerImpl fires it at each
  strata-wave barrier via a callback wired by IteratingAnalyzerImpl; CheckpointWriter batches
  wave primaries and flushes at most once per 60s — a cold first pass now checkpoints
  continuously instead of nothing for 3-4h; interval is a ctor param, tests use 0/MAX);
  (3) phase-2 sequencing decision, then the reachability implementation
  (acceptance: TestDeepCaptureChain green + promoted shadow baseline; expect TestShadowCloneBench
  pins + deepFieldChains tripwire to fire as re-baseline signals); (4) #39 step 2 activation data
  from the jfocus owner (EIDEDUP_SHADOW=1, their suite); (5) #35 frontier integration (their
  worklist + consumption edges as wake relation).
- REVERSE-DIVERGENCE TRIAGE (2026-07-19, phase 2's opening move): all 8 were ONE mechanism —
  modification of a field through a NON-this scope (local.field on a locally created object:
  fernflower's AnnotationContainer via collectAllAnnotations' `result`, LabelSets via
  processStatementLabel's `sets`, Root.firstExprents via an accessor alias). That evidence never
  enters any method-level mlv.modified() summary (the observing method's receiver/params are not
  implicated), but FieldAnalyzerImpl.computeUnmodified reads it from the last statement's
  VariableData (UNMODIFIED_VARIABLE == FALSE) → UNMODIFIED_FIELD = FALSE, and the record/ctor
  parameters inherit FALSE via the field link (handleParameter). The 3 parameter REVs were pure
  E3-downstream of the 5 field REVs. Fix: ShadowModificationPass gained a
  seedStatementLevelFieldModifications channel mirroring the field analyzer exactly (same
  exclusions: constructors, part-of-construction, the field's own getter/setter, same primary
  type only). Fernflower: 0 reverse. Clonebench re-baselined {1,6,272}/{71,208} →
  {nonModifyingMethod=1, unmodifiedField=8, unmodifiedParameter=274}/{propagated=71, seed=212},
  still 0 reverse — the 4 new forward divergences are genuine refused-downgrades
  (TestData.expected/.other via a local in an anonymous execute(), + 2 downstream ctor params).
  Channels inventoried but NOT yet mirrored (would appear as new REVs if a corpus exercises
  them; add mirrors evidence-driven): (a) modifiableThroughInheritedDefaultMethod
  (FieldAnalyzerImpl — inherited non-overridden modifying default method + overridden accessor,
  see notes/default-method-modification-not-propagated-to-impl-field.md); (b) closure-captured
  ENCLOSING-method parameters: copyModificationsIntoMethod counts inClosure modification as
  methodModified, but the shadow's seedVariable routes a captured ParameterInfo to the parameter
  case and never seeds the method node.

## PREVIOUS STATE (2026-07-18)

- Branch `sv-integration`, suites green: java-openjdk, prepwork 199, link 393, analyzer 145.
- Proving ground certified & crash-free on engine defaults (worklist ON, PARALLEL ON, hints
  preloaded, cycle breaking at certification): timefold (50k elements), langchain4j (53k), guava,
  fernflower, activemq, jenkins-core, 8 camel-core modules.
- Corpus-wide immutability is live (hints post-parse + cycle breaking); fernflower distribution
  after the precision-audit fixes: 54 @Immutable / 62 @ImmutableHC / 229 @FinalFields / 161 @Mutable
  / 1 null.
- Profiling rounds 1-3 done: -26% CPU (dedup keys ran the CST printer), -37% allocations (eager
  debug args, getenv-per-lookup, scopeVariables), lock-clean. ASPROF env gate in run-openjdk.
- javac parse flake root-caused and fixed (concurrent lazy getOrLoad into the live JavacTask +
  plain-HashMap registry). The 2026-07-18 18:41 recurrence was REDIAGNOSED same day: overlapping
  suite timestamps prove 3-4 fork JVMs failed SIMULTANEOUSLY — not in-JVM corruption but a
  CROSS-PROCESS race: a second gradle invocation in the same repo recompiling (locally modified)
  sources and rewriting class files/jars while live test forks read them. Explains the
  starImportScope NPEs (javac on half-written classpath entries), the garbled XML filename (JUnit
  discovery on a half-written class file), and the morning maddi-graph unreadable-jar race.
  PROTOCOL: do not run gradle in ~/git/maddi from two threads concurrently (task #40). CAUGHT
  RED-HANDED 21:39:08 same day: soak run 3 (of 3; runs 1-2 quiet + 0 flakes) had 4 classpath jars
  (cst-impl, graph, util, inspection-openjdk) rewritten by a sibling process mid-run — the failing
  suite's XML timestamp matches the jar mtimes to the second, and the garbled discovery filename
  reappeared. Mechanical fix: bin/gradle-locked.sh (stale-proof lock in build/), MANDATORY per
  CLAUDE.md. TRUE ROOT CAUSE FOUND 2026-07-18 22:3x after ~10 certified collision events (18:41 →
  22:08): jfocus-standardize/settings.gradle.kts line 1 = includeBuild("../maddi") — the sibling
  thread's compiles (run in ~/git/jfocus-standardize, believing they used maddi-kotlin) executed
  maddi's OWN jar tasks via the composite build, rewriting ~/git/maddi/*/build outputs (incl.
  maddi-support.jar, on the test inspector's javac classpath) while live test forks read them. No
  protocol violation by any thread; the lock cannot see composite-build side doors. User repointed
  the includeBuild. Note for the future: a "foreign" build-output write with no foreign gradle
  process in the repo = look for includeBuild references from sibling repos. The chain-taint round
  was committed on pins + certified 0-diff A/B; the owed link-suite green LANDED 2026-07-18 22:3x
  after the repoint (prepwork/analyzer had passed repeatedly throughout) — the round is fully
  gated, saga CLOSED. Gate runs now SELF-CERTIFY: touch a marker
  before gradle, find-newer on build outputs (excluding own modules) after — red + foreign writes
  = collision (rerun in a quiet window); red + certified-quiet = real bug, and would revive the
  in-JVM residual hypothesis (the 21:48 event's causal chain via the aapi-parser CLASS DIR is the
  least direct of the five — noted for honesty). In-JVM
  leads (owner-thread assertion, one-lock-per-JavacTask) stay relevant for analyzer-PARALLEL
  corpus runs only. SECOND ROOT CAUSE FOUND 2026-07-18 (lead from the flakiness thread, confirmed
  + fixed here): createTask returned the JavacTask from INSIDE try-with-resources on its
  StandardJavaFileManager — every parse/analyze/lazy-load ran against a CLOSED file manager.
  Use-after-close: mostly self-healing (closed containers lazily re-created) but intermittently
  corrupting mid-read — the historical low-victim-count flakes that no locking could cure. Fix:
  fm outlives the task (openFileManagers, closed in invalidateAllSources). Plus an assert that
  -XDuseUnsharedTable is HONORED (Names.table is not SharedNameTable), ScanCompilationUnits.
- CHECKPOINT GRANULARITY GAP (from the externally killed 2026-07-19 ES verification run, ~3.5h
  in, 0 checkpoint files): pass-boundary writes give NO protection during a cold run's FIRST
  pass — which at monorepo scale is 3-4h+, exactly the stretch that needs protecting. Fix
  direction: iteration 1's strata waves are natural intra-pass boundaries — emit a
  passCompleted-style delta per completed WAVE (the barrier already guarantees the wave's
  elements are final for that pass). The ES type-null verification (null count, distribution,
  CONSEDGES at scale) is PENDING RERUN, schedule overnight. Process note: 'clean tree ⇒
  concurrent gradle safe' holds only while the tree STAYS clean — post-merge/post-edit rebuilds
  rewrote jars under the live ES JVM (accepted silently; wrong). The lock exists so this never
  needs per-case reasoning; bypass only for genuinely read-only overlaps.
- Elasticsearch SWEEP GREEN 2026-07-19 04:12 (5h21m, 24G, post-#33): **BUILD SUCCESSFUL, zero
  isolated types** — the sweep-green bar is MET. 239,741 elements (+9 = exactly the
  BinaryFieldMapperTests.$13.BytesCompareUnsigned members, the #33 pin type, now fully analyzed).
  A/B vs first contact: 466 lines (142 added / 133 removed) — the new members + formerly-degraded
  neighborhoods re-analyzed. Distributions: methods 101,829 nonMod / 48,549 mod / 1,841 null
  (10 fewer undecided); fields 28,586/12,569/562; types 798 @Imm (+8) / 1,975 @ImmHC (+8) /
  1,367 @FF / 18,459 @Mutable / 23,206 null (the 51% cluster UNCHANGED — breaking-strategy item
  stands). Dump: build/imm-elasticsearch-2-2026-07-19.txt.gz.
- Elasticsearch first contact COMPLETED 2026-07-18 (attempt 11, 5h29m, 24G heap, work ceiling
  active): **239,732 elements** — 152,210 methods (101,822 nonModifying / 48,537 modifying /
  1,851 null = 99% decided), 41,717 fields (28,581/12,571/565), 45,805 types. Types:
  **23,206 null (51%)** / 18,474 @Mutable / 1,368 @FF / 1,967 @ImmHC / 790 @Imm — the camel-api
  null pattern AT SCALE (abstract/external supers without hints under the current breaking
  strategy). Journey: 7 attempts — config pipeline (extra-jmod, native dual -d bug, MRJAR
  overlays, build-tooling scoping), 5 scanner bugs fixed, prep isolation (4 types), 8G OOM,
  96-min uncapped straggler (per-pop ceiling missed it), edge-visit ceiling → completion.
  Dump preserved: build/imm-elasticsearch-2026-07-18.txt.gz. Sweep exit non-zero (isolated prep
  errors reported) — bar for GREEN needs #33 fixed. The 5h29m runtime is the checkpoint
  argument (#34) in numbers; the 51% type-null cluster is the breaking-strategy work item.

## OPEN ITEMS

Modification-as-reachability (PLAN-modification-reachability; §11 ownership split settled
2026-07-19 — METRICS SIDE COMPLETE, engine work = THIS thread):
- Incoming (metrics thread, ~/git/maddi-metrics kotlin branch, unpushed; Bart coordinates
  delivery; purely additive): cb0eec9c E7 characterization (TestModificationFunctionalE7,
  8 green shapes = PRESERVATION suite + PLAN §12) and 1aa8209e phase-1 ShadowModificationPass
  (+ PLAN §13; no writes, diffs vs frozen properties with BFS cause chains).
- Shadow corpus verdict (testarchive, 9,306 types, ~34s): 279 divergences, ALL sound-direction,
  0 reverse. 208 "seed"-class = the refused-downgrade family NOW ENUMERABLE WITH NAMES (input for
  the §9.4 cross-read vs the immutability audit); 71 "propagated" = deep-capture disease in the
  wild. Lower bound (650 call sites without LINKED_VARIABLES_ARGUMENTS, 3,068 chained receivers
  unprojected). Baseline pinned in TestShadowCloneBench vs kotlin fba60b23.
- E7 edge-class constraints my implementation must honor (from the 8 shapes): attribution is
  EAGER and CREATION-SITE (a capturing lambda charges captured targets where created/passed, even
  when the callee provably ignores the FI); enclosing-method parameters propagate, locals filter
  (acceptForExtra); field-stored callbacks charge at REGISTER, not trigger (application-site-only
  would drop them); opaque whole-object fallback + $_afi marker survive forwarding hops.
- §9.4 CROSS-READ EXECUTED 2026-07-19 (SHADOWDIFF gate in RunAnalyzer, forces
  trackObjectCreations, diagnostic runs only): fernflower under the FULL iterating analyzer =
  **836 divergences** (539 unmodifiedParameter / 193 unmodifiedField / 104 nonModifyingMethod;
  list in scratchpad shadow-ff-divs.txt) vs 279 on testarchive single-iteration — cycle breaking
  freezes optimistic values by design, and the shadow now names them. DIRECT HIT: FastFixedSetFactory
  (the audit's flagship independence anomaly) appears with 4 entries incl. iterator() and
  colValuesInternal — the two independent evidence lines indict the same structures. BUT: **8
  REVERSE divergences** in the full-iterating context (their single-iteration corpus had 0) =
  shadow-pass gaps by its own contract; triage-to-zero is the FIRST phase-2 sub-task, since the
  836 is untrustworthy as a bound while the pass under-reaches. Caveats scale up too: 180 call
  sites without argument links, 2,597 unprojected receivers.
- PHASE 2 IS MINE (metrics thread has stopped engine work). Acceptance contract on landing:
  TestDeepCaptureChain (my @Disabled pin) goes green; promoted shadow baseline = second gate;
  EXPECT TestShadowCloneBench pinned counts + metrics deepFieldChains tripwire to fire — both are
  re-baseline signals, not regressions; frozen-TRUE downgrades will shift corpus FPDUMPs (plan
  the A/B story before landing). Sequencing decision (phase 2 scheduling) now schedulable per §11.

Engine, soundness/precision (from the semantic + precision audits):
- StackEntry rule-2 miss (public final fields of mutable types accepted at level 2) — investigate,
  suspicion: cycle-breaking-era undecided field type.
- Independence under cycle breaking: fernflower records storing ctor args verdicted INDEPENDENT
  where the clean-path verdict is DEPENDENT/FF. MECHANISM FOUND 2026-07-18 via the new
  refused-downgrade counter: certification passes with 11 refused downgrades standing, 6 of them
  independentType (@Independent(hc=true) frozen against @Dependent evidence, e.g.
  FastFixedSetFactory.FastFixedSet) — the freeze class of PLAN-modification-reachability, not (only)
  the LINKS=EMPTY erasure. STRICTCERT=1 now refuses such certifications; default surfaces an ERROR
  with per-property counts.
- Lambda promotion gap: stateless / read-only-capture lambdas never promoted past @FinalFields.
- Constants-only interface verdicted @Mutable (interface default).
- camel-api null cluster (246/787): abstract methods without hints stay NON_MODIFYING=null under the
  current breaking strategy.
- Aliased-static-singleton unsoundness (fernflower DUMMY; tier-3 same-instance canonicalization).
- Constructor non-confluence under PARALLEL (EdgeType.<init> oscillates; conservative side correct).
- WHY do stacked §m.§m faces exist in VMI groups (producers now skip at Link creation; origin
  unexplained).

Engine, robustness/performance:
- Per-method closure cost ceiling: DONE 2026-07-18 (edge-visit granularity, 10M default,
  -Dmaddi.workCeiling, opt-out NOWORKCEILING; the per-pop first cut never tripped — the elasticsearch
  monster burned 96 min inside single propagations). Gate: link 394/0, fernflower 0 trips + 0-diff.
- Dual-identity family (source-scanned type also lazily loaded from bytecode): task #33 has TWO
  fronts. IN-HOUSE PARSER FRONT FIXED 2026-07-18: ParseTypeDeclaration could not declare a member
  type inside an anonymous/local body AT ALL — the bySimpleName lambda resolves members by
  scan-phase FQN lookup, but an anonymous enclosing ($0) is never in the type context
  ('Cannot find type a.b.X.$0'). Fix: create-or-reuse the member TypeInfo directly (+
  handleTypeModifiers, as parseLocal does); forward references then resolve free of charge via
  parseBody's subtypes-first ordering. TestAnonymousMemberRecord ENABLED (asserts full source
  build). TestDegradedAnalysisMarker's trigger became obsolete by this fix and was replaced with a
  SYNTHETIC one (committed non-abstract method without body + source-set-carrying CU — the
  half-built shape the lazy class-scanner path presents). OPENJDK FRONT FIXED same day, three
  layers: (1) anonymous-body member types get the local-type recursivelyCommit treatment (field
  initializers defaulted + committed — they are invisible to the end-of-scan commit walk);
  (2) ClassSymbolScanner.primary() now hops METHOD/initializer owners, so isSourceSymbol correctly
  claims members of anonymous/local classes and the class-file path no longer double-loads their
  interfaces/annotations ('Extending multiple identical interfaces'); (3) with both, prep runs
  clean. Pinned in TestAnonymousMemberRecordOpenJdk (link module = openjdk inspector). Gates:
  5 suites green certified-quiet, fernflower A/B 0-diff EXACT. #33 CLOSED except the 'Create
  multi' setInternal UOE (build-tooling source sets, scoped around — separate small item).
  Elasticsearch sweep-green re-run queued overnight 2026-07-18→19.
- CompileListToSourceSets: two -d destinations for one module (generated-classes step) corrupt both
  the source-set name and its URI (elasticsearch libs/native); derive from the classes/java/<name>
  destination only. MRJAR overlay source sets are an open design question.

Checkpoint/resume + incremental (session tasks #34/#35):
- #34 checkpoint/resume v1 CORE DONE 2026-07-19 (~04:45): CheckpointWriter (analyzer module)
  implements AnalysisValueFeed — delta writes at every pass boundary (only primary types touched
  that pass; file-per-type = idempotent; IO failures logged, never fatal; terminal marker file).
  Restore = LoadAnalysisResults.goDir with LinkCodec.restoreCodec() — a decode mode where
  ALREADY-PRESENT values win (generalizes the GET_SET_FIELD special case in CodecImpl; needed
  because prep recomputes partOfConstructionType etc. before the preload) — then re-running the
  analyzer, whose verify-certify sweep is the soundness net. Round-trip pinned in
  TestCheckpointResume (analyze → checkpoint → invalidate+reparse → restore → identical verdicts,
  TERMINAL_CERTIFIED). Deliberately out of v1: source-change detection (per-sourceset fingerprints
  live on the maddi-kotlin branch); resume assumes unchanged sources. PRODUCTION WIRING DONE same
  night: CHECKPOINT=<dir> / CHECKPOINT_RESTORE gates in RunAnalyzer (FPDUMP value-convention);
  crash-consistency hardened after live testing on fernflower — per-type atomic writes (temp dir +
  ATOMIC_MOVE; an encode assert mid-stream had left truncated JSON), goDirTolerant restore
  (skip-and-count unreadable files), feed guard now also catches AssertionError/SOE (an encode
  assert had KILLED the analysis through the RuntimeException-only guard). Verified two-step on
  fernflower: cold writes 150/227 primaries (codec asserts skip mid-iteration values), warm
  preloads 82, both certify; cold-vs-baseline 0-diff mod oscillator. DECODE ASYMMETRY largely
  FIXED same night (55%→85% restore coverage): (1) VariableToTypeInfoSetImpl encoded a map with
  LIST-valued keys — syntactically invalid JSON (21 files); now a sorted list-of-pairs with
  legacy-map decode fallback, cold-vs-baseline 0-diff EXACT; (2) recursiveMethod added to
  LinkCodec's property provider (9 files); (3) marker-variable definitions were per-CODEC-INSTANCE
  while files are per-type — a shared instance put the definition in whichever file used the
  marker first ('Cannot find $_ce0M', 29 files); CheckpointWriter now takes a codec SUPPLIER,
  fresh codec per file = self-contained. REMAINING TAIL: 19 NPE + 2 UOE + 1 stale-overload decode
  skips (re-analyzed on resume; coverage, not soundness). POST-MERGE 2026-07-19: the kotlin-side
  corpus configs enable the results-WRITE path, which exposed two more codec gaps on fernflower:
  typeExpression (FIXED — TypeExpressionCodec added to ExpressionCodec) and variables owned by
  ANONYMOUS types (encodeInfoOutOfContextStream asserts !isAnonymous — 76 skipped values on
  fernflower). WriteAnalysisResults now SKIPS unencodable values with a warning + counter instead
  of aborting the run (rides the existing null-skip pathway). SHARED CODEC FIX LIST (with the
  early-cutoff thread's fieldIndex gap): anonymous-type out-of-context encoding, decode-tail NPEs,
  stale-overload disambiguation. (b) resume-fixpoint non-confluence observed once:
  TargetInfo.LocalvarTarget @FF(cold)→@ImmHC(warm) — the preload starts iteration from converged
  values and certifies a (more precise) different fixpoint; same family as EdgeType.<init>.
- #35 incremental v2 for the single-module 3M-line monorepo: giant cycle spans ~2/3 of the code, so
  per-sourceset granularity (fingerprints on maddi-kotlin branch) and SCC-transitive invalidation are
  useless — design: element-level fingerprints + optimistic preload of unchanged elements' values +
  the verify-certify loop as the soundness net, invalidating via learned summary-consumption edges.

Extract-interface consumer (../jfocus-refactor-service extractmodule; adequacy review 2026-07-18,
session tasks #36-#39):
- #36 DONE 2026-07-18 (commit 494cb2bb): DEGRADED_ANALYSIS_METHOD stamped at all three rungs
  (shallow fallback, analyzer fault isolation, prep type isolation); ComputeCallGraph null-body
  guard added en route. Consumer half (pessimistic per-type gate in ComputeReplaceTypesActions)
  implemented in jfocus-refactor-service, UNCOMMITTED there for the owner thread; jfocus suite at
  known baseline 176/1.
- #37 VL2O nature tier: MEASURED 2026-07-18 (gate VL2OTIER=1, fernflower): assignment-tier adds
  118,958 vs content-tier adds 2,285,123 — a 19:1 ratio; ~95% of the blast radius carries
  shares-content semantics, confirming material over-rejection for extract-interface. NEXT: an
  ADDITIVE second map (assignment-tier-only VL2O) so the consumer can switch without changing the
  existing property; coordinate the switch with the jfocus thread (their suite is the gate).
  Related: #38 CLOSED — the ≡ exclusion is deliberate and correct (separately-cast aliases are
  type-DEcoupled; mediation semantics), pinned in TestVl2oAliases.
- #38 targeted test for the ≡ exclusion corner (real-variable aliases expressed only as ≡ must still
  reach VL2O, else missed rejection).
- #39 ⚠ REIMPLEMENTATION — PREMISE REVISED 2026-07-18 after an attempted first slice: engine ←
  links ERASE syntactic mediation (pattern bindings and casts both yield plain ←, and closure
  composition spreads the blindness), while CommonAnalyze's walk computes DECLARED-TYPE coupling,
  which mediation deliberately decouples. Slice reverted (jfocus baseline restored, 176/1-known).
  Correct sequence: (1) engine adds mediation provenance to assignment-tier links (nature variant or
  flag; sticky through composition), (2) then de-duplicate CommonAnalyze consuming unmediated ←
  only. STEP 1 DONE 2026-07-18: Link.mediated() flag (LinkImpl 4th component, EXCLUDED from
  equals/hashCode/toString — provenance, not identity) + a direct-pair registry on Graph
  (mediatedPairs, populated at simpleAddToGraph from the produced link, pruned on clear/remove)
  consulted at WLAM's two final emission points; the flag ALSO flows through LinkImpl
  translate/translateFrom and the Builder 4-arg add. Design finding: the pattern-binding '→' never
  enters the SharedVariables collapse (collapse fires on ← only) — the stored ii←0:o comes from the
  ENGINE's symmetric edge via FollowGraph, whose facts carry no flag; hence the out-of-band pair
  registry covering BOTH paths, not an Assignment-record thread. Pinned in TestMediatedLinks
  (enabled, green). (b) cast-mediated assignments DONE same day: both assignment producers
  (ExpressionVisitor.assignment, handleSingleLvc initializer) flag mediated when the RHS primary is
  in Result.casts — the cast itself is link-transparent, so provenance comes from the side-record;
  covers the COLLAPSE path too (the ← does enter SharedVariables, and the registry recovers it);
  pinned in TestMediatedLinks.testCast + testPlain negative control. Residual: an EXPRESSION cast
  ('x = (II) o.bar()') records no cast side-band (addCast fires for variable operands only) — same
  variable-based scope as jfocus's own casts join. (c) codec persistence DONE same day: optional
  4th element on the [from, nature, to] wire format, appended only when mediated — unmediated
  output byte-identical, old 3-element files decode unmediated; single decoder funnel
  (MethodLinkedVariablesImpl.decodeLink); pinned in TestWriteAnalysisMediated. (a) chain-transitive
  taint DONE same day (commit 582fb564): two-pass reachability in assignmentEdgeStream — a chain
  target is mediated only when NO unmediated path reaches it (coupling wins); summary-level pin
  return<-jj<-ii<-(cast)o. ENGINE SIDE COMPLETE. Residuals: closure-composed engine facts (not
  collapse chains) carry no taint; expression casts unflagged (variable-operand scope). NEXT =
  step 2: de-duplicate CommonAnalyze in jfocus consuming unmediated <- only (coordinate with the
  jfocus owner thread; their suite is the gate), after the owed 3-suite green confirmation.
  STEP 2 SHADOW MODE prepared 2026-07-19 (uncommitted in jfocus-refactor-service, owner-thread
  convention like #36's consumer half): env gate EIDEDUP_SHADOW logs, per assignment-carrying
  statement, (a) the ENGINE's assignment-tier view of the target (unmediated vs mediated split —
  mediated must never couple) and (b) 'EIDEDUP syntactic' lines at every coupling site
  (bidirectional + the three directional adds in linkAssignedValue). Divergence between the two
  log streams is the evidence base for replacing linkAssignedValue's value-shape recursion.
  DIRECTIONALITY is the known crux: stored ← does not distinguish method-return/identity flows
  (directional in the rc-graph) from plain aliases (bidirectional) — the shadow data must answer
  whether that distinction is recoverable or the replacement stays partial. VERIFIED same day
  (clean-tree concurrent gradle is safe — up-to-date tasks write nothing): compiles; smoke on
  TestReplaceTypeInstanceOf shows THE original falsifier correctly classified ('target ii
  unmediated [] mediated [ii←0:o]' — the cast link is mediated-only, empty unmediated set =
  must-not-couple; provenance works end-to-end in the consumer). NOTE for the owner thread:
  their suite currently fails ~10 tests against maddi HEAD (splitclass output diffs, one AIOOBE
  in TestMetricsAndExtractInterfaceComputer) — NOT from the sv-side changes (persist under
  NOCYCLEBREAKING=1; mediated flag inert; shadow gated off) — most plausibly the kotlin-merge
  printer changes (enum-constant listing) vs their pinned expected outputs, plus their own
  in-flight edits (ExportedExtract, TestAddTypeSub modified in tree).
  Maddi-internal same-disease siblings unaffected and still valid: WLAM's five mirror blocks,
  iterateOverShared vs expandRepToMembers (==/.equals divergence),
  ShallowMethodLinkComputer.correspondingTypeParameters vs hiddenContentHierarchy.

Module organization (see org-review-2026-07-18.md for the full ranked plan):
- Small/high-value items 1-6 (docs, dead code, codec ☷/$_v holes) — in progress 2026-07-18.
- Structural items 7-12 (doVariableReturnRecompute phases, SourceMethodComputer extraction, gate
  registry, Graph split, transfer decomposition, assignmentEdgeStream naming) — not started.

Docs/process:
- unmodifiedField doc note in road-to-immutability 030-modification (content-only semantics).
- Corpus FPDUMP baselines: RE-PINNED 2026-07-19 04:26 post all of the day's fixes (mediation
  provenance, #33 both fronts, fm lifetime): build/imm-{fernflower,timefold,langchain4j,
  elasticsearch-2}-2026-07-19.txt.gz — fernflower 623,899 lines / timefold 50,464 /
  langchain4j 53,644 / elasticsearch 239,741. Use these for all future A/Bs.
- Suite gates must grep BUILD SUCCESSFUL (stale-XML trap, journal 2026-07-18d).

## JOURNAL

New dated entries land here and are periodically moved to `sv-journal.md`.

### 2026-07-18g — why the link computation has single-thread phases (analysis, keep for reference)

Observed live on elasticsearch attempt 8: one worker grinding ~30 min while 7 idled. Three distinct
mechanisms produce single-thread stretches:

1. **Wave barriers on iteration 1.** The first pass is scheduled as call-graph strata
   (`ComputeAnalysisOrder.waves`); each wave fans out over the pool and `joinAll` barriers before the
   next wave ("the next wave's callees are complete", SingleIterationAnalyzerImpl ~178). The barrier
   buys PRECISION (callee summaries exist when callers run → most verdicts decide in pass 1) and
   BOUNDS NONDETERMINISM (the non-confluence window; EdgeType.<init> is what leaks through even so).
   Cost: tail latency — a wave finishes at its slowest element, and method cost is heavily
   long-tailed, so idle-7-drain-1 is the barrier draining to a straggler, not a bug.
2. **Per-method grain.** No intra-method parallelism by design: one IncrementalFixpointEngine per
   method, densely mutable (closure, witness index, queue); fine locking would cost more than it buys
   on the 99% of methods finishing in ms. A pathological method therefore rides one thread — now
   capped by the work ceiling (2M facts → "cycle protection" → shallow summary).
3. **Genuinely serial sections.** javac parse (thread-hostile); coordinator work between passes
   (cheap); iterations 2+ run a plain parallel loop over the dirty subset (no waves) — utilization
   drops there because the worklist got SMALL, not because anything blocks.

Improvement candidates, in order:
- Cost-descending launch order within a wave (biggest first): shrinks the drain window, zero risk.
- True dataflow scheduling (element starts when ITS deps are done, no wave barrier): maximizes
  utilization but widens the non-confluence window — do not trade before the constructor-confluence
  item is fixed.
- Giant-SCC corpora (the 3M-line monorepo): waves inside the big cycle are mostly flat (cycles cannot
  be stratified); iterations-to-converge dominates there → task #35 territory, not scheduling.
