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
  where the clean-path verdict is DEPENDENT/FF — suspicion: FieldAnalyzer breaking branch LINKS=EMPTY
  erasure feeding independence.
- Lambda promotion gap: stateless / read-only-capture lambdas never promoted past @FinalFields.
- Constants-only interface verdicted @Mutable (interface default).
- camel-api null cluster (246/787): abstract methods without hints stay NON_MODIFYING=null under the
  current breaking strategy.
- Aliased-static-singleton unsoundness (fernflower DUMMY; tier-3 same-instance canonicalization).
- Constructor non-confluence under PARALLEL (EdgeType.<init> oscillates; conservative side correct).
- WHY do stacked §m.§m faces exist in VMI groups (producers now skip at Link creation; origin
  unexplained).

Engine, robustness/performance:
- Per-method closure cost ceiling: one elasticsearch method ground a worker for ~30 minutes in
  IncrementalFixpointEngine while 7 threads idled; add a fact-count/effort ceiling with the
  throw-and-degrade-to-shallow design (same as cycle protection).
- Dual-identity family (source-scanned type also lazily loaded from bytecode): task #33 — member
  types of anonymous classes (prep repro @Disabled in TestAnonymousMemberRecord); plus the
  'Create multi' setInternal UOE (scoped around by dropping build-tooling source sets).
- CompileListToSourceSets: two -d destinations for one module (generated-classes step) corrupt both
  the source-set name and its URI (elasticsearch libs/native); derive from the classes/java/<name>
  destination only. MRJAR overlay source sets are an open design question.

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
