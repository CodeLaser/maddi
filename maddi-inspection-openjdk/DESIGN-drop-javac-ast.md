# Dropping the javac AST after parse — freeing heap for heavy analysis on large projects

Status: IMPLEMENTED (Option B, gated) + validated at server/main scale, 2026-07-22. Motivated by running
the extract-interface engine on very large single-SCC projects (miles-core: 23,394 types, 661,633
elements, ~32G). See §8 for the implementation and measurements.

## 1. The opportunity

maddi parses Java by driving the OpenJDK **javac** front-end and then translating javac's output into
its own CST (`org.e2immu.language.cst.impl.*`). A heap histogram at analysis peak shows that, long after
parsing, the javac representation is **still resident**:

- server/main (4,875 types): `com.sun.tools.javac.*` = **407 MB** (`JCTree$JCIdent` 41M,
  `JCTree$JCFieldAccess` 27M, `com.sun.tools.javac.util.List` 72M, `Symbol$VarSymbol` 20M, …), ~8% of a
  ~5 GB live set. On a project several times larger (miles-core), the absolute figure scales up.

The analysis engine (prepwork, linking, modification) works **entirely on maddi's CST** and never reads
a `JCTree` node or a javac `Symbol` — the CST is fully self-contained (a broad grep for `com.sun.*` /
`javax.lang.model` across `maddi-cst-impl`/`-api`/`-analysis` returns nothing). So the javac AST is dead
weight for analysis, and reclaiming it is a memory lever for fitting large projects under a heap ceiling.

## 2. What retains the javac AST, and why it is NOT a stray leak

The retention is deliberate. The javac graph hangs off two `JavaInspectorImpl` fields:

- **`lastScanUnits`** (`JavaInspectorImpl.java:79`, assigned `:490`) — a `ScanCompilationUnits` holding
  the live javac front-end as final fields (`ScanCompilationUnits.java:51-58`): `JavacTask task`,
  `Trees`, `SourcePositions`, `Types`, and a `ClassSymbolScanner`. The `task` transitively pins the
  whole javac `Context` → parsed `JCTree`s + symbol tables.
  - `ClassSymbolScanner` keeps `IdentityHashMap`s of `MethodSymbol`/`VarSymbol` → maddi Info
    (`ClassSymbolScanner.java:84-85`) and `topLevelClassSymbolsOfSources`
    (`ClassSymbolScanner.java:92`); each source `ClassSymbol` retains its `JCClassDecl.tree` — this is
    what pins the source `JCIdent`/`JCFieldAccess`/`util.List` in the histogram.
- **`openFileManagers`** (`JavaInspectorImpl.java:85`) — `StandardJavaFileManager`s kept open on purpose
  (`:80-84`, `:672-674`).

## 3. THE CONSEQUENCE: dropping the AST disables `getOrLoad()`

The reason the javac state is kept alive is **lazy, on-demand loading of compiled types during
analysis**:

- `initialize()` wires the lazy loader: `ctm.setLazyLoader(this::loadCompiledTypeOrNull)`
  (`JavaInspectorImpl.java:211`).
- `loadCompiledTypeOrNull` (`JavaInspectorImpl.java:203-205`) delegates to
  `lastScanUnits.loadCompiledTypeOrNull(...)`, which drives the **live** `task.getElements()` /
  `cs.complete()` (`ScanCompilationUnits.java:498-529`).
- `CompiledTypesManagerImpl.getOrLoad(fqn)` (`CompiledTypesManagerImpl.java:99-113`): cache **hit** →
  return; cache **miss** → `lazyLoader.apply(fqn)` → the javac task loads the compiled (library/JDK)
  type by FQN. Its own comments note this runs "from PARALLEL analyzer worker threads"
  (`:16-18`, `:103-105`).
- Real analysis-phase callers of `getOrLoad`: `maddi-modification-common/.../IsolateMethod.java`,
  `maddi-modification-link/.../vf/VirtualFieldComputer.java`, `.../io/LinkCodec.java`,
  `maddi-modification-prepwork/.../io/PrepWorkCodec.java`, `maddi-aapi-parser/.../AnalysisHintsParser.java`.

**Therefore: if the javac AST is dropped (i.e. `lastScanUnits = null`), every `getOrLoad` cache MISS
returns `null`** (`CompiledTypesManagerImpl.java:102`). Any type the analysis references but has not yet
loaded becomes silently unresolvable — the analysis drops it or fails. This is why the AST is
**load-bearing, not accidental**, and why it cannot be nulled unconditionally.

Corollary — the source `JCTree`s (the biggest single chunk) are genuinely dead after parse (compiled
loading reads `.class` files, not source trees), but they ride on the **same** `lastScanUnits` /
`ClassSymbolScanner` reference as the lazy loader, so they cannot be freed independently without
javac-internal surgery (clearing each source `ClassSymbol`'s `JCClassDecl.tree` while keeping the task).

## 4. The one existing release path

`JavaInspectorImpl.invalidateAllSources()` (`JavaInspectorImpl.java:112-125`) is the only release: it
closes every file manager and nulls `lastScanUnits`. `JavaInspectorImpl` implements neither `Closeable`
nor `AutoCloseable`; `CompiledTypesManagerImpl` is explicitly designed to hold no javac refs
(`:13`, `:25-27`), so it survives the release — but its lazy loader is now dead.

## 5. Options, ordered by robustness

**A. Drop after `parse()`** — call `invalidateAllSources()` immediately after parsing. Frees the full
javac AST. **Breaks any `getOrLoad` miss during analysis.** Safe only when everything the analysis needs
is already loaded by parse+preload — which is NOT true for large projects, whose linking discovers many
library types on demand. Rejected for heavy analysis.

**B. Drop after the analysis-hints load (recommended sweet spot).** `LoadAnalysisResults.go(…,
ANALYZED_RESULTS)` decodes the AAPI archive, and decoding each entry **resolves+loads** (via
`getOrLoad`) the library/JDK types it carries annotations for — populating the `CompiledTypesManager`
cache with the common dependency surface. **After a successful hints load, most types the analysis will
reference are already cached** → subsequent `getOrLoad` calls **hit** → the javac task is no longer
needed for them. Dropping the AST here breaks only the **residual** misses (types referenced by the
source but absent from both the hints archive and the loaded set).
  - Trade-off: safety depends on hints coverage of the project's actual dependencies.
  - **Hard prerequisite: the hints load must SUCCEED.** If it is skipped or partial (e.g. miles-core's
    slf4j codec skew, where the 0.8.2 AAPI archive's slf4j 1.x `Logger` mismatches the corpus's slf4j
    2.x `Logger.atDebug` — see `TestExtractInterfaceStress`'s tolerant catch), the cache is NOT
    pre-populated and dropping the AST is unsafe. So Option B presupposes a version-matched hints
    archive (regenerate the AAPI archive / the corpus `inputConfiguration` so the codec does not skew).

**C. Eager pre-load, then drop (most robust).** After parse+hints, force `getOrLoad` over the transitive
closure of compiled types the source references, fully populating the cache; then
`invalidateAllSources()`. `getOrLoad` never misses afterward. Cost: the library `TypeInfo`s are retained
instead of the javac AST — but a shallow maddi `TypeInfo` is far smaller than its javac `Symbol`+tree,
so still a net win. Highest up-front cost, no residual-miss risk.

**D. Keep the task, drop only the source trees (deepest).** The source `JCTree`s are the biggest chunk
and are unneeded by compiled loading; ideally clear each source `ClassSymbol`'s `JCClassDecl.tree` while
keeping `task` alive for `getOrLoad`. Targets the largest saving without losing lazy loading, but is
javac-internal surgery. Long-term ideal.

## 6. Recommendation and wiring

For heavy analysis on large single-SCC projects (the extract-interface / cycle-breaking use case):

1. **Get a successful hints load** — regenerate the AAPI archive (and/or the corpus `inputConfiguration`
   via the maven plugin) so its library versions match the corpus and the codec does not skew. This
   improves analysis quality *and* enables Option B.
2. **Drop the AST after the hints load (Option B), behind a flag** (e.g. `DROP_AST=1`). Add a gated call
   to `invalidateAllSources()` after `LoadAnalysisResults.go(ANALYZED_RESULTS)` in the run path
   (`RunAnalyzer`, the extract-interface harness, the IDE daemon).
3. **Fail loud, not silent.** When the AST has been dropped, a `getOrLoad` miss must **log the missing
   FQN** (ideally throw under a strict flag) rather than return `null`, so residual coverage gaps
   surface during testing instead of silently corrupting the analysis. This is the single most important
   safety measure — the failure mode of Option A/B is *silent* missing types.
4. If residual misses appear, escalate to Option C (eager pre-load). Option D is the eventual best.

## 7. Expected impact

At server/main scale this frees ~407 MB; at miles-core scale (and the 3M-line commercial target) it is
the difference-maker between a run that sits at 99% of a 32G heap and one with comfortable headroom —
because the parse floor (dominated by the javac AST + maddi CST held simultaneously) is exactly what
leaves too little room for the analysis accumulator. It is a prerequisite capability for pushing heavy
analysis onto projects larger than ~250k elements at a fixed 32G ceiling.

## 8. Implementation and validation (2026-07-22)

Built as **Option B, gated**, plus the fail-loud safety net (§6.3):

- **Engine** (`CompiledTypesManagerImpl`): after the AST is dropped, a `getOrLoad` **miss is surfaced**,
  not silently nulled — it is counted and the FQN logged once (measurement mode), or thrown
  (`DROP_AST_STRICT`). A `lazyLoaderDisabled` flag mirrors "no live javac task"; `JavaInspectorImpl`
  sets it in `invalidateAllSources()` and clears it whenever a scan reassigns `lastScanUnits`, so a
  normal partial-classpath miss stays benign while a task is live. Accessors expose distinct/total miss
  counts + a sample (`TestDropAstFailLoud`).
- **Harness** (`AnalysisRun`, extract-interface): `DROP_AST=1` calls `invalidateAllSources()` after prep
  and before the analysis accumulator grows; a post-run line reports residual misses.

**Key finding — the drop does NOT depend on a successful hints load.** §5B feared Option B was unsafe
without hints coverage. In practice **parse itself** eagerly resolves and caches the compiled types the
source references (via `getOrLoad` during scanning), so by analysis time the cache already holds the
surface the linker walks. Both validation runs recorded **0 residual misses** with no reliance on the
AAPI hints archive:

| corpus        | types | elements | reclaimed at drop | peak used (ceiling) | gcFraction | misses |
|---------------|-------|----------|-------------------|---------------------|------------|--------|
| guava         |   610 |   21,837 | 1111 → 260 MB     | (small)             | ~0         | **0**  |
| server/main   | 4,875 |  138,385 | 4148 → 1853 MB    | 11.3 G (12 G)       | ≤0.017     | **0**  |

server/main context: same corpus was ~20.6 G peak (multi-pass, no flatten) → ~16 G (single-pass+flatten)
→ **11.3 G with the AST drop added**, GC pressure near zero. The drop removes the parse-floor javac
retention and hands the analysis real headroom — exactly the miles-core lever.

Residual risk for miles-core: its dependency surface differs, so a few residual misses are possible;
the run stays in **log mode** (not strict) to measure them rather than abort. If the count is non-trivial,
escalate to Option C (eager pre-load of the referenced closure before the drop).

### miles-core outcome (the target, 2026-07-22)

miles-core is 23,394 types / 661,633 elements (2.65× full ES) and is one giant SCC. With `DROP_AST=1`
+ single-pass + flatten:

- **@ 32G: does not fit.** The drop clearly helped — the run advanced smoothly to **48% at gcFrac ~1.5%**
  (the prior no-drop run teetered at 30-31G from the start) — then entered the back-half SCC wave and
  thrashed to death (used pinned 31G, gcFrac 0.85, no progress). The wall is the **persistent write-once
  output accumulator + the retained per-method VD**, which grow monotonically through the single pass.
- **@ 44G: ran the SCC wave healthily** (gcFrac 1-2%, no thrash) but was killed by external memory
  pressure before completing (concurrent gradle in other worktrees; the true full peak is unconfirmed).
- **Live histogram at the 48% wall: 29.4G** — dominated by prepwork VariableData ~7.2G (miles-core's
  10K-statement methods make each flattened last-statement VD large), CST ~4.5G (irreducible),
  analysis() maps 1.4G, map/array backing ~9.6G.

**Settled: ~44G is the miles-core requirement with today's levers (drop-AST + single-pass + flatten);
32G needs a further lever.** The drop-AST work is complete and validated — it removed the parse-floor
javac retention and was the difference between teetering-from-the-start and a clean 48%-at-1.5%-GC climb.
The next lever, if 32G is pursued later, is to **drop a method's VD entirely after linking in single-pass
mode** (the ~7.2G dominant) rather than merely flatten it — a real engine task, because the flatten
retains the last-statement VD for the concurrent FieldAnalyzer/CommonAnalyze readers active during the
pass. Secondary: lazy-allocate the ~1.4G of mostly-empty `analysis()` PropertyValueMapImpl maps.
