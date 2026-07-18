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
  plain-HashMap registry); watch for recurrence.
- Elasticsearch first contact IN PROGRESS: config via compile-log pipeline (server closure,
  27 source sets after scoping); 5 scanner bugs fixed en route; prep isolated 4 known types out of
  43k; modification analysis proceeds past isolated prep errors since today. Long-tail grind
  observed (one method ~30 min in the closure) → per-method ceiling needed (below).

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
- #37 VL2O nature tier: addVL2O accepts every nature except ≡, so content-tier links (∈ ~ ∩ ...)
  inherit modification semantics into a type-aliasing question → over-rejection. Measure first,
  then tier-filter.
- #38 targeted test for the ≡ exclusion corner (real-variable aliases expressed only as ≡ must still
  reach VL2O, else missed rejection).
- #39 ⚠ REIMPLEMENTATION — PREMISE REVISED 2026-07-18 after an attempted first slice: engine ←
  links ERASE syntactic mediation (pattern bindings and casts both yield plain ←, and closure
  composition spreads the blindness), while CommonAnalyze's walk computes DECLARED-TYPE coupling,
  which mediation deliberately decouples. Slice reverted (jfocus baseline restored, 176/1-known).
  Correct sequence: (1) engine adds mediation provenance to assignment-tier links (nature variant or
  flag; sticky through composition), (2) then de-duplicate CommonAnalyze consuming unmediated ←
  only. Maddi-internal same-disease siblings unaffected and still valid: WLAM's five mirror blocks,
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
