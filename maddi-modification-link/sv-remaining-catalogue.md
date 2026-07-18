# sv engine — live state and open items

> This file is the LIVE summary. The full engineering journal (all dated UPDATE sections since the
> original link-failure catalogue closed) is in **`sv-journal.md`**. Design references:
> `sv-reconstruction-techniques.md` (reconstruction techniques + guards), `sv-engine-handoff.md`
> (sv-doc entry point), `linking-manual.md` (link module manual), `org-review-2026-07-18.md`
> (module organization review + 13-point plan).

## CURRENT STATE (2026-07-18)

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
- Dual-identity family (source-scanned type also lazily loaded from bytecode): task #33 — member
  types of anonymous classes (prep repro @Disabled in TestAnonymousMemberRecord); plus the
  'Create multi' setInternal UOE (scoped around by dropping build-tooling source sets).
- CompileListToSourceSets: two -d destinations for one module (generated-classes step) corrupt both
  the source-set name and its URI (elasticsearch libs/native); derive from the classes/java/<name>
  destination only. MRJAR overlay source sets are an open design question.

Checkpoint/resume + incremental (session tasks #34/#35):
- #34 checkpoint/resume v1: pass-boundary write via the AnalysisValueFeed seam (commit ec77f8bb),
  restore = fingerprint check + LoadAnalysisResults preload + re-certification. Codec prerequisite
  DONE (☷ pass-set round-trip, commit 0ae52f51).
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
  Maddi-internal same-disease siblings unaffected and still valid: WLAM's five mirror blocks,
  iterateOverShared vs expandRepToMembers (==/.equals divergence),
  ShallowMethodLinkComputer.correspondingTypeParameters vs hiddenContentHierarchy.

Module organization (see org-review-2026-07-18.md for the full ranked plan):
- Small/high-value items 1-6 (docs, dead code, codec ☷/$_v holes) — in progress 2026-07-18.
- Structural items 7-12 (doVariableReturnRecompute phases, SourceMethodComputer extraction, gate
  registry, Graph split, transfer decomposition, assignmentEdgeStream naming) — not started.

Docs/process:
- unmodifiedField doc note in road-to-immutability 030-modification (content-only semantics).
- Corpus FPDUMP baselines: superseded by the audit-round fixes; re-pin on next runs.
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
