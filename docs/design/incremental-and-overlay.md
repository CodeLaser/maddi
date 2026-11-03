# Reparsing on the fly, and the property delta of a textual delta

**Status:** assessment and direction, 2026-11-03. Refined as the work proceeds; the sections marked *open* are
decisions still to take. Java front end only; Kotlin is out of scope for now.

Two demands drive this:

1. **Reparsing on the fly.** The IDE plugins, the daemon and the MCP server re-analyse the whole project on every
   trigger. A one-line edit costs a full run (`maddi-dist/docs/roadmap/ide-todo.md` §1).
2. **"If I made this change, what would it do?"** The property delta of a textual delta: which verdicts move,
   which contracts break, before the edit is applied (`maddi-dist/docs/roadmap/mcp-server.md`, `what_if`). Call
   this an *overlay*: a hypothetical version of some files laid over the analysed project, answered without
   touching the base.

The first is a performance problem on a path that exists. The second needs something the model does not have today:
two versions of a type at once.

Companions: `rewiring.md` (the parser-level reload and rewire), `partial-reparse-rewire.md` (class files from the
inspection pass), `../../../maddi-mod/docs/design/analysis-rewiring.md` (carrying analysis results, the output
fingerprint) and `../../../maddi-mod/maddi-modification-analyzer/DESIGN-incremental-v2.md` (checkpoint and resume).
Those record what was built in July 2026. This document records what that adds up to, and what is missing.

## 1. What exists, by layer

Verified against the code on 2026-11-03. "Production" means reachable from a shipped entry point; "test" means
demonstrated in a test and nowhere else.

### Parser (openjdk inspector)

| Capability | State | Where |
|---|---|---|
| `reloadSources`: which files changed, by MD5 of the raw text | production code, one production caller (below) | `JavaInspectorImpl.reloadSources` |
| `parse(setInvalidated)`: `UNCHANGED` kept, `INVALID` re-parsed, `REWIRE` copied with references re-pointed | production code; stress-tested by `RunRewireTests` | `JavaInspectorImpl.reparse`, `TypeInfoImpl.rewirePhase0..3`, `InfoMapImpl` |
| Unit of re-parse | **the whole source set**: a set with one `INVALID` type is rescanned in full | `JavaInspectorImpl.actionFor` |
| `lastRewireInfoMap()`: old → new, read-only, after a reparse | production code | `JavaInspector` |
| `parse(transformedString, CompilationUnit, ParseResult, options)`, one class in the context of a parse | **not implemented**, throws | `JavaInspectorImpl` |
| `parseSingleFileInSourceSet` | test only; writes into the shared registries, does not see same-set siblings from source | `JavaInspectorImpl` |
| Source fingerprint finer than the file, or insensitive to comments | none | |
| `setGeneratedClassesDirectory`: class files from the parse, so dependents resolve against what maddi read | production code, **no production caller** | `JavaInspector` |

A single-source-set project never rewires anything: everything is rescanned. See §3 for whether javac leaves an
alternative.

### Prepwork

| Capability | State | Where |
|---|---|---|
| Prep of one type: `doPrimaryType(t)` | production code | `PrepAnalyzer` |
| Re-prep of a prepped type is skipped (`PREPPED` marker) | production code | `PrepAnalyzer` |
| Method-level `VARIABLE_DATA` guarded against a second write; statement-level **not** (throws) | | `MethodAnalyzer` |
| Cross-type reads: the callee's `GET_SET_FIELD`; the enclosing type's `VARIABLES_OF_ENCLOSING_METHOD` | | `ApplyGetSetTranslation`, `MethodAnalyzer` |
| Cross-type **write**: `IMPLEMENTATIONS` onto the overridden method, upstream; nothing prunes a deleted implementation | `rewiring.md` trap 7 | `MethodAnalyzer.addImplementation` |

### Carrying analysis across a rewire

| Capability | State | Where |
|---|---|---|
| `Property.analysisTier()`: `PARSE_TIME` / `INTRINSIC` / `CROSS_TYPE_DERIVED` | production code | `Property`, `PropertyImpl` |
| The rewire phases carry `carryOnRewire` properties; only `GET_SET_FIELD` opts in | production code | `TypeInfoImpl.rewirePhase3` and siblings |
| `Value.rewire` for the derived tier (`IMPLEMENTATIONS`, `LINKS`, `METHOD_LINKS`, `PART_OF_CONSTRUCTION`, ...) | implemented; `VariableData`, `ListOfLinks`, `ParameterParSeq` still throw | `ValueImpl`, `LinksImpl`, `MethodLinkedVariablesImpl` |
| Carry of the derived tier from outside the reload, through `lastRewireInfoMap()` | test (`TestOutsideReloadCarry`) | |
| `PropertyValueMap.removeIf`: clear before recompute, so the monotonic guard accepts a lowered value | production code | `PropertyValueMapImpl` |

### Analyzer

| Capability | State | Where |
|---|---|---|
| `analyze(order, graph, initialDirty, beforeFirstRecompute)`: a seeded worklist that stops when dry | production code, reached only through `INCREMENTAL=<dir>` on the CLI | `IteratingAnalyzerImpl`, `AnalysisEngineImpl.incrementalSeed` |
| The wake relation: symmetric graph edges, plus `overrides()`, plus persisted consumption edges | production code | `IteratingAnalyzerImpl` (`dependersOf`) |
| In incremental mode, **skipped**: MODREACH, eventual deferral and contraction, cycle breaking, the strata-parallel first pass, certification | | `IteratingAnalyzerImpl` |
| Nullability: whole-program after `iterate`, also in incremental mode | | `IteratingAnalyzerImpl`, `NullabilityPass` |
| `AnalysisFingerprint` (output hash, positions normalised), `IncrementalState`, `EarlyCutoffWorklist`, `ConsumptionEdgeRecorder`, `CheckpointWriter` | fingerprints persisted, **never compared** in production; the worklist class has no production caller | `maddi-modification-prepwork/io`, `callgraph` |
| `AnalysisEngine` API (`PrepRequest`, `ModificationRequest`, `ModificationOptions`) | **no incremental input at all**: no seed, no dirty set, no carry, no state directory. `environmentGates` hands control to environment variables | `maddi-analysis-api` |
| Tests of the `INCREMENTAL` path | **none** | |

The production early cutoff is element-summary change inside the analyzer's worklist, not the output fingerprint.

### Consumers

| Consumer | On a change |
|---|---|
| IDE daemon (`WarmAnalysisService`) | a fresh `JavaInspectorImpl` per request, full parse, full prep, full modification; `storeFingerprints=false`, `callGraph=null` (no worklist narrowing); `lastRun` dropped at the start of the next run |
| IntelliJ, Eclipse | whole-project run on build; the file deltas they have are discarded |
| MCP `diff_runs` | compares two stored results by rendered annotation strings per element, and findings by text; certainty is not compared |
| Gradle, Maven | cold; the `incrementalAnalysis` flag is plumbed through `PluginOptions` and **read by nothing** |
| Refactor service (outside this workspace) | the only production user of `reloadSources` + `setInvalidated`; opt-in, off by default; rebuilds the whole source set; the rewire cone is dependents only |

### Two findings that are not gaps but bugs

- **Stale values on the seed.** `AnalysisEngineImpl.incrementalSeed` restores the whole checkpoint directory,
  including the old values of the *changed* types, onto the freshly parsed CST. The clear hook never fires for the
  seed (`everAnalyzed` is pre-seeded with it, on the assumption that the seed is fresh source without values). In the
  engine path that assumption is false: a changed type whose verdict lowers meets the monotonic guard, or keeps the
  stale value.
- **The hierarchy-upward flow is covered inside the analyzer, not outside it.** `Circle.area()` becoming modifying
  makes `Shape.area()` `@Modified` and `Shape` not `@Immutable`. The analyzer's wake relation is symmetric and
  includes `overrides()`, so one seeded `analyze` call handles it. The parser's `REWIRE` cone
  (`PrimaryTypeUseGraph.dependentsOf`) and prep's `IMPLEMENTATIONS` set do not: a type-level cone computed from
  dependents alone misses `Shape`, which is what `mcp-server.md` warns about.

## 2. Why nothing joins up

Two incremental stories were built in July 2026 by two threads: the parser rewire (`rewiring.md`) with its reload
driver, and the analysis checkpoint/resume (`DESIGN-incremental-v2.md`) with a cold parse. Each is demonstrated in
its own tests. The join, a warm inspector reloaded and a seeded analyze over the result, exists only as
`TestEarlyCutoffSkip` and `TestEarlyCutoffWorklistDriver`, both test-side orchestration. The daemon, which is where
the demand lands, cannot reach either: it builds a cold inspector, and the engine API has no seed.

## 3. Reparsing on the fly: what is missing

Mechanisms are not the gap. Plumbing and three decisions are.

1. **An API, not environment variables.** `ModificationRequest` needs the dirty seed and the carry inputs;
   `PrepRequest` needs "these types only". Small, and it unblocks every non-CLI caller.
2. **A stateful daemon.** Keep the inspector, the runtime and the call graph alive per project; the request says
   which files changed; fall back to a cold run on a configuration or class-path change, or on anything unexpected.
   The protocol and thread rules are in `ide-todo.md` §1.
3. **The terminal passes** (*open*). An incremental run returns before MODREACH, the eventual contraction and
   certification. Options: run them whole-program on every step (MODREACH is a one-shot reachability and may be
   cheap enough; the contraction is not); scope them to the dirty closure; or declare the incremental result
   `BEST_AVAILABLE` and let a background full run certify it. The last fits the certainty vocabulary the MCP
   already has, and is the proposed start.
4. **Per-type rescan within a source set** is cheaper than the July decision assumed (§5: javac does not force
   the whole set, and the plumbing is mostly there), but it still comes after 1–3: the analysis carry absorbs a
   coarse rescan, and for every run but the IDE's the parse is not where the time goes.

## 4. The overlay: the property delta of a textual delta

The model is single-world throughout. `TypeInfoImpl.equals` is fqn plus source set; `InfoByFqn` and
`CompiledTypesManager` are keyed by fqn; `analysis()` lives on the `Info` object. A hypothetical `Circle` cannot sit
beside the real one in any structure keyed by `Info`. `TranslationMap` and `IsolateClass` both produce copies, but
copies that are `equals()` to their originals and live in the same runtime: neither is an independent world.

Three ways to get the delta.

**A. Two cold worlds, then diff.** Step 1 of `mcp-server.md`. Needs a content overlay in the inspector (javac file
objects from strings for a disk-backed set; today the in-memory map is the test protocol only) and a `diff_runs`
that compares values and certainty rather than annotation strings. Costs a full run per question. Buildable now,
and the reference every faster path is tested against.

**B. Mutate the base, diff, revert.** Reparse in place, run the seeded worklist, diff, reload the original. The
base is wrong while the question is open, the monotonic guard fights the revert, and a daemon serving an IDE cannot
have its model swapped under it. Ruled out.

**C. Fork the world.** The direction proposed here, because it is the same mechanism on-the-fly reparsing needs.
The rewire protocol is already copy-on-write: `INVALID` re-parsed, `REWIRE` copied, `UNCHANGED` shared. Let it
produce a *second* registry layered over the base instead of replacing the base's entries, with its own analysis
order and a worklist seeded with the `INVALID` elements. An IDE edit is then fork-and-commit (the fork becomes the
base); a what-if is fork-diff-discard. Two hard points:

- *The fork must own every type whose verdict can move.* Shared `UNCHANGED` objects share their analysis maps, so
  the fork-owned copies are `INVALID` ∪ their dependents ∪ the upward cone (the types of the methods they override,
  transitively) ∪ the dependents of those. That is the analyzer's symmetric wake relation lifted to type level;
  the graph and `overrides()` are there to compute it.
- *`Info` identity.* Every `Info`-keyed structure the analyzer uses (call graph, order, `dependersOf`, links) is
  built per world, never shared. A fork that reuses the base's `G<Info>` routes silently to base objects.

Everything else, the carry of cross-type-derived values onto fork copies, clear-before-recompute, the seeded
worklist and the terminal-pass decision, is shared with §3. Option A is the oracle for C, including the caveat
from `DESIGN-incremental-v2.md`: an incremental fixpoint is *a* certified fixpoint, not necessarily the cold one, so
the two deltas may differ where the analysis is not confluent.

## 5. Does javac force the whole source set?

No. Checked on 2026-11-03: the whole-set rescan is the inspector's choice, not javac's. The unit of work is fixed
before javac sees anything, in `JavaInspectorImpl.actionFor` (RESCAN on the first `INVALID` type of a set) and
`computeCompilationUnits` (every accepted file of the set is handed over). `ScanCompilationUnits.scan` then runs
`task.parse()` and `task.analyze()` with no arguments, so everything handed over is attributed, and builds CST for
every handed unit. Three things make a smaller unit possible without leaving the one-task-per-set design.

**javac's API is finer than the inspector uses.** `JavacTaskImpl`, which the scan already casts to for
`getContext()`, has `parse()`, `enter()`, `enter(Iterable<CompilationUnitTree>)`, `analyze(Iterable<Element>)` and
`generate(Iterable<Element>)`. Attribution and flow analysis, the expensive phases, can be restricted to named
classes; parsing and entering the rest is what gives those classes their symbols.

**File-granular selection already exists.** The package restriction (`SourceSet.restrictToPackages`) hands javac
only the accepted files, puts the set's source roots on `SOURCE_PATH` so that javac parses the others on demand, and
records which files are the task's own (`ClassSymbolScanner.setCompilationUnitUris`, read by `isSourceSymbol`). The
filter, `accept()`, is evaluated per file; only its predicate is per package.

**An unchanged sibling resolves to the kept `TypeInfo`.** When the scan meets a symbol of a type that is not among
the task's own units, `ClassSymbolScanner` asks the registry first (`known = getType(fqn)`) and returns it unless the
known object is a stub or an external-library copy from another class path. A kept source type of the same set is
neither, so the reference lands on the kept object, whether javac entered the sibling from the source path or read
it from a class file. This is the "resolution against kept objects" that `rewiring.md`'s plan listed as the crux; it
is already how a package-restricted scan works.

Three ways to use this, in order of how much javac work they save:

| | javac sees | cost per edit | what it needs |
|---|---|---|---|
| a. parse and enter the whole set, attribute the changed classes (`analyze(changed)`) | every file | parse + enter of the set, attribution of the changed units | the smallest change: `scan` takes the set of units to attribute and build |
| b. hand only the changed files, source roots on `SOURCE_PATH` | the changed files, siblings on demand | the above, minus parsing of files nothing references; javac attributes on-demand units unless `analyze(changed)` is used as well (*to measure*) | `restrictToPackages` generalised to a file predicate |
| c. hand only the changed files, the set's own class files on `CLASS_PATH` | the changed files, siblings as bytecode | parse + attribute of the changed units only | class files of the previous scan: `setGeneratedClassesDirectory`, today without a production caller; or the build's output, which may be stale |

For the 3.5M-line monolith in `partial-reparse-rewire.md` (two source sets, a full javac pass of about two
minutes), (a) alone turns the per-edit parse cost from a compile into a parse-and-enter plus the edited units.
For the corpus runs it changes nothing: parse is under 2% of CPU there (`../status/performance-2026-10-22.md`),
and the analysis is the cost. So this is an IDE and what-if lever, not a batch one, and the July ordering
(analysis first) stands; what changes is that the parser side is cheaper than that ordering assumed.

What the reparse has to do differently, whichever of the three is chosen:

1. Invalidate per type, not per set (`ctm.invalidate` is already per primary type; `reparse` calls it for every
   type of the set).
2. Seed the `InfoMap`'s `rebuilt` set with the `INVALID` types only, so that `REWIRE` types of the *same* set are
   copied by the rewire phases instead of rebuilt by javac.
3. **A signature change must re-attribute the same-set dependents** (*open*). Today a caller in the same set whose
   callee lost a method fails attribution and its unit is dropped, which is at least visible. With a partial scan the
   caller is `REWIRE`, copied with references the `InfoMap` cannot map, and nothing says so. The cheapest sound rule
   is to hand javac `INVALID` ∪ (`REWIRE` ∩ same set) for attribution and rebuild what fails; whether the rest of
   that cone is rebuilt or rewired is the question. A file-level fingerprint of the *declarations* (signatures only,
   bodies and comments out) would tell a body edit, which cannot break a caller, from a signature edit, which can.
4. `lastScanUnits` and the loader task: a partial scan's task serves `getOrLoad` for the set as the full scan's did;
   nothing changes, but the compiled-type loader must not be built from a task that only parsed a subset of the set
   with (b) or (c), since its `SOURCE_PATH`/`CLASS_PATH` differ from a full scan's.

The in-house CongoCC inspector is per file by construction and already rewires per type (`rewiring.md`), but it
is not the front end to build on (`JavaInspector`'s own comment).

## 6. The prep-only path: refactorings that never link

Several refactoring operations in the refactor engine (outside maddi) need the parse, prepwork and the call graph
but not the link and modification analysis. For them the 9/10 that `analysis-rewiring.md` set aside is not there
to save, and the whole-set reparse plus the whole-project prep *is* the cost. What they pay per edit today, after
the refactor service's incremental reparse (`PrepworkResource.incrementalReparse`, opt-in, off by default):

| step | scope today | why |
|---|---|---|
| javac + CST | the whole source set of every changed type | §5 |
| prep per type (`VARIABLE_DATA`, get/set, hidden content) | the rescanned set ∪ the `REWIRE` cone | kept types are skipped by the `PREPPED` marker; rebuilt and rewired types are new objects with no intrinsic data |
| `ComputeCallGraph` | the whole project | built from scratch by an `ImmutableGraph.Builder` over every primary type |
| `setRecursiveMethods`, `ComputePartOfConstructionFinalField` | the whole graph | global by nature (cycles, reachability) |
| `ComputeAnalysisOrder` | the whole graph | `Prepwork.make` always computes it; a prep-only consumer does not need it |

There is no single list for this. What exists, in four places:

- `rewiring.md` §"Parked mid-flight" and "Resumed": the **intrinsic carry**, items 1–4 (`VariableDataImpl` /
  `VariableInfoImpl.rewire` with its two traps, `Statement.rewireAnalysis`, the `VARIABLE_DATA` opt-in, tests).
  Parked on 2026-07-18 because the analysis tier was the bigger prize; the "still to do" line stands.
- `rewiring.md` §"Plan: call-graph-granular rewiring": per-type rescan, steps (a)–(c). Deferred the same day. §5
  above shows the javac side is nearer than that plan assumed.
- `refactor-base/docs/PLAN-modification-analysis-resource-tier2.md` (2026-07-22): stages 2a–2e. Stage 2a, the
  reparse with carry and a dirty frontier, is built as `incrementalReparse`; the header still says "design only".
- `refactor-base/docs/ANALYSIS-INCREMENTAL-REPARSE.md` (2026-10-22, "not ready"): eleven gaps against a cold
  reload, with the order to fix them (1, 2, 5, 9; then 3, 4; then a switch-on/switch-off harness). Gap 8 is this
  section's subject: "`Prepwork.make` runs over the whole parse result".

No GitHub issue in maddi or maddi-mod covers prepwork rewiring or a partial prep, and the prepwork code carries no
TODO beyond the `rewire` methods that throw.

The list for a prep-only incremental step, in order:

1. **Measure before choosing.** Split a cold reparse of a real project (the audit's 71 s, `ANALYSIS-INCREMENTAL-REPARSE.md`)
   into javac, CST, prep per type, call graph, recursive methods, part-of-construction and order. The `ASPROF`
   hook of `../status/performance-2026-10-22.md` does this. Nothing below is worth doing before its share is known.
2. **The service-side gaps** (`ANALYSIS-INCREMENTAL-REPARSE.md` 1, 2, 5, 9, then 3, 4), since nothing is switched
   on until they are closed, and the Kotlin inspector's lack of a reload keeps the audit on the cold path regardless.
3. **Per-type rescan** (§5), the parse half. Shape (a) first: parse and enter the set, attribute and build the
   `INVALID` units, resolve siblings to the kept objects. The open point is item 3 of §5, re-attribution after a
   signature change.
4. **The intrinsic carry** (`rewiring.md` items 1–4), the prep half: a `REWIRE` type keeps its `VARIABLE_DATA` and
   the other `INTRINSIC` properties instead of being re-prepped. The one cross-type input, the callee's
   `GET_SET_FIELD`, is parse-time and already carried, so the carry is sound unless a callee changed its getter or
   setter status, which the `INVALID` set names.
5. **A per-type call graph.** `ComputeCallGraph` already has a one-type constructor; keep each primary type's
   vertices and edges as its own contribution and union them, so an edit replaces the contributions of the
   `INVALID` ∪ `REWIRE` types and keeps the rest. `PrimaryTypeUseGraph` is a projection and follows.
   `setRecursiveMethods` and `ComputePartOfConstructionFinalField` stay global on the merged graph until item 1
   says they matter.
6. **`PrepRequest` takes a subset and an existing graph.** Today `AnalysisEngine.prep` preps `primaryTypes` and
   builds the graph from them; a prep-only incremental caller needs "prep these, merge into that". The precedent
   for prepping one type over a kept universe is `TestPrepOnePrimaryTypeAtATime`.
7. **Skip the analysis order** for a prep-only consumer (`doPrepWorkForModificationAnalysis=false` exists;
   `Prepwork.make` passes `true`).

## 7. Proposed order

1. Clear the seed's stale values in `incrementalSeed`, and add a test of the `INCREMENTAL` path.
2. Expose the seed and the carry through `AnalysisEngine`.
3. Content overlay in the inspector, value-level `diff_runs`: `what_if` as two cold worlds (A).
4. A stateful daemon: reload, seeded analyze, `BEST_AVAILABLE` until a background full run certifies.
5. Fork the world (C), tested against A on the hierarchy-upward cases.
