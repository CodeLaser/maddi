# Split maddi into three repositories — work plan

**Status: plan (2026-09-30, revised twice the same day: the run drivers stay in base behind an
analysis-step service; ext compiles against base only and carries mod at run time).** Written for the thread that carries it out. Every claim about the code below was measured on
`ws/server` at `3329d329d` (maddi) on 2026-09-30; re-measure before relying on a number, the commands are
given.

## 1. Purpose, and the one rule

The whole maddi+jfocus codebase is being reorganised into repositories with taller walls, so that the
~8 Claude threads supporting two developers stay bound to their topic. maddi goes first. It becomes three
repositories:

| repository | tier | holds |
|---|---|---|
| `maddi` (this one, name and history kept) | **base** | CST, parsers and front-ends (Java, bytecode, Kotlin), inspection, graph, annotations, support, util, the annotated-API archive, the run configuration, the run drivers (CLI + compile-log route + the step pipeline), the analysis-step **service interface**, and — after stage 2 — the code-structure (call) graph |
| `maddi-mod` | **mod** | prepwork, link, analyzer, modification-common, the AAPI compiler, and the module that **implements** the analysis steps and hosts every test that runs the analysis |
| `maddi-ext` | **ext** | IDE daemon and client, IntelliJ, Eclipse, VS Code, the Gradle and Maven plugins — compiled against base, shipping mod as a runtime-only dependency |

The rule that the split enforces, and that a check script (stage 0, `tools/tiers/check_tiers.py`) keeps enforced:

> **base depends on nothing above it; mod depends on base only; ext compiles against base only and
> carries mod at run time.** A consumer of the base tier sees the modification analysis only as values
> on `Info`, through the property map of `cst-api` and the property constants of `cst-analysis`, and
> *runs* it only through the analysis-step service interface, whose implementations arrive on the class
> path from mod. Anything that implements a step, persists analysis results, or names the analysis's
> internal types is mod-side. **No module outside mod names a `modification.*` or `aapi.parser.*` type
> in a compile-scope import or a `requires`.**

Three consumers of that interface, one mechanism:

- the **run drivers** in base: `--analysis-steps=none` (parse only) works from base alone; `prep`,
  `modification`, `rewire-tests` and the hints compile are steps that mod provides;
- the **IDE daemon and the build plugins** in ext: they bundle `maddi-mod` (the daemon's
  `installDist` lib directory, the plugins' shaded jar) as a **runtime-only** dependency and drive the
  analysis through the same steps, so their compile class path is base alone;
- the **refactor engine's base tier** (`refactor-api`, `refactor-impl`, `commonservice`, `conformance`,
  the shared metrics), which will build without `maddi-mod` and obtain its prepared project
  through the same `prep` step (§5).

## 2. The tiers as measured

Assignment by declared main-scope dependency closure (`build.gradle.kts` `project(":…")` lines; the
`module-info.java` `requires` directives agree — no base descriptor requires a `modification` module):

- **base (31 after stages 1–3):** aapi-archive, annotation, cst-analysis, cst-api, cst-impl, cst-io,
  cst-print, cst-print-kotlin, graph, inspection-api, inspection-integration, inspection-kotlin,
  inspection-mixed, inspection-openjdk, inspection-parser, inspection-resource, java-bytecode,
  java-openjdk, java-parser, kotlin-api, kotlin-k2, kotlin-realm, manual, support, util; **run-config**
  (after stage 1); **callgraph** (new, stage 2); **analysis-api** (new, stage 3); **run-main,
  run-openjdk, run-kotlin, run-rewire** (after stage 3).
- **mod (6):** modification-common, modification-prepwork, modification-link, modification-analyzer,
  aapi-parser, **run-analysis** (new, stage 3: the step implementations and the analysis-running tests).
- **ext (7):** ide-daemon, ide-client, intellij, eclipse, vscode, gradleplugin, mvnplugin.

Facts that decide the edge cases:

- **aapi-parser is mod, not base.** It compiles the archive's annotated stubs into analysis results by
  running the shallow analyzer and annotation provider from modification-common and the result IO from
  prepwork (five files). Its consumers are the `run-*` drivers, the plugins, the daemon and one test in
  link. Nothing in base uses it. The **archive** is pure data (depends on `support` only) and stays base.
- **run-config is base by intent and mod by accident.** The refactor side imports it from 32 files
  (30 of them the JSON streaming utility). Its only mod-tier import is the AAPI *hints configuration*
  interface and implementation, 157 lines of plain Java with no maddi imports, declared in aapi-parser.
  Stage 1 inverts that edge.
- **The call graph is a property of the parse, not of the analysis.** `ComputeCallGraph`,
  `ComputeAnalysisOrder` and `PrimaryTypeUseGraph` in prepwork's `callgraph` package import only
  cst-api, cst-impl, graph and inspection-api. They sit in prepwork because the analyzer needs its
  worklist order. The refactor side's biggest edge into mod is exactly these classes: the `Prepwork`
  record in `codelaser-metrics-common` (referenced from 20 refactor modules) is built from them, and
  cluster, cycle, footprint, extractinterface and methodcallgraph import `ComputeCallGraph` for its
  static edge-value decoders (`isAtLeastReference`, `weightedSumInteractions`, `handleFieldAccess`, the
  `TYPE`/`CODE`/`REFERENCES` constants). Stage 2 moves them down. `ComputePartOfConstructionFinalField`
  (uses prepwork variable data) and `EarlyCutoffWorklist` (used by prepwork's `IncrementalState`) stay.
  `run-rewire` (152 lines) uses only `ComputeCallGraph` and `PrimaryTypeUseGraph`, so it is base as
  soon as they are.
- **The run drivers are three layers, two of them clean.** `run-openjdk` and `run-kotlin` hold the CLI
  (`Main`), the compile-log route (`javac/` and `kotlinc/` packages: parse the compiler invocations into
  source sets — 0 modification imports) and one pipeline class each (`RunAnalyzer`,
  `RunMixedPrepAnalyzer`); `run-main` is the same shape for the plugin. The pipeline already reads
  `--analysis-steps` and branches on the names `none`, `prep`, `modification`, `rewire-tests`. What it
  reaches into mod for, per step: `prep` → `PrepAnalyzer`; `modification` → `IteratingAnalyzer` and its
  `ConfigurationBuilder`; result IO → `LoadAnalysisResults`, `WriteAnalysisResults`,
  `AnalysisFingerprint` (the codecs for the values, e.g. `LinkCodec`, are mod); the hints compile →
  `AnalysisHintsCompiler`/`Parser`. Everything else it does (parse, configuration, the call graph and
  order after stage 2) is base. Stage 3 puts an interface between the two.
- **Tests that run the analysis sit in base modules today** and must move, because a base repository
  cannot have even a test-scope dependency on mod (Gradle composite builds do not allow a cycle between
  builds). Counted: `run-openjdk` 10 of 36 test files, `run-kotlin` 15 of 38, `run-config` 1 of 20,
  `inspection-kotlin` 24 (the `prepwork/` test package — ports of the Java prep-analyzer tests, Kotlin
  source in, same `VariableData` assertion strings out — plus the analyzer smoke test and two printer
  tests). The `slowTest` corpus battery is configured in `run-openjdk`'s build and runs the analysis, so
  it moves too, with `corpus/`. All of these go to `maddi-run-analysis` (stage 3).
- **The eight properties declared in the modification modules** (`links`, `methodLinks`, `typePrepped`,
  `partOfConstructionType`, `recursiveMethod`, `unmodifiedVariable`, `downcastVariable`,
  `localVariablesOfEnclosingMethod`) are the analysis's working state, not verdicts. They stay where
  they are, except `recursiveMethod`, which travels with `ComputeCallGraph` (stage 2). The 57 verdict
  properties are already in cst-analysis, which depends on cst-api only — nothing to do there.
- **Non-module entries in `settings.gradle.kts`:** `platform` (the BOM; base, the other two import it),
  `road-to-immutability` and `maddi-manual` (docs; base), `buildSrc` (one conventions plugin applied by
  36 modules; stage 5), `corpus/` (configs for the slow-test battery; mod), `dogfood/` (runs the
  analyzer plugin over maddi's own modules; ext, it depends on the plugin).
- **ext's compile-time footprint on mod is nine files, and each use has a base-side home.** The
  client, IntelliJ, Eclipse and VS Code modules import nothing from mod. The rest:
  - daemon `WarmAnalysisService`: the whole pipeline by hand (`PrepAnalyzer`, call graph and order,
    `IteratingAnalyzer` and its `ConfigurationBuilder`, `AnalyzerException`) — a second copy of
    `RunAnalyzer`; becomes a client of the steps.
  - daemon `StreamingValueFeed` implements `AnalysisValueFeed` (analyzer module) to stream partial
    results to the IDE. That interface imports only `Info` from cst-api: it moves to `analysis-api`.
  - daemon `HintsLoader`: `LoadAnalysisResults` + `PrepWorkCodec` (result IO and the codec registry);
    daemon `AnnotationTagger` and Maven `WriteAnalysisHintsMojo`: `DecoratorImpl`, which renders
    analysis values as annotations and comments and has **no** modification import of its own — it sits
    in prepwork's `io` package by history. Result IO and the decorator become base-side interfaces in
    `analysis-api`, obtained from the same registry as the steps; the decorator's implementation can move
    to base outright.
  - daemon `ResultCollector` filters out `PrepAnalyzer.PREPPED`, the analyzer's per-run bookkeeping
    property, before showing results. A client must not have to know a mod-internal property to hide
    it: the step reports which properties are bookkeeping (or does not leak them at all).
  - Gradle `AnalyzerPropertyComputer`, Maven `CompileAnalysisHintsMojo` and `CommonMojo`: the hints
    configuration, base after stage 1. Maven `WriteAnalysisHintsMojo`: `AnalysisHintsComposer`
    (aapi-parser) — the hints write becomes a step with the comments-decorator subclass on the mod side.
  - Build wiring: the plugins' `shade` configuration is what `implementation` extends from, so
    everything shaded is also on the compile class path. That split has to become explicit: base
    modules on the compile class path, mod modules `runtimeOnly` **and** shaded. The daemon's
    `module-info.java` drops its three `requires io.codelaser.maddi.modification.*` lines and declares
    `uses` for the service types; the mod jars sit on its module path at run time.
- **Bazel:** 28 `BUILD.bazel` files exist. CI (`.github/workflows/build.yml`) runs `./gradlew build`
  only; nothing in jfocus-devops runs Bazel. Default decision: the files travel with their modules and
  are not a gate. Say so in the commit message; do not spend time making Bazel build per repository.

## 3. Stages

Each stage ends with its gate green and **one commit per root cause** (the user pushes). Work in an
aside workspace so a running census in `ws/server` never picks up a half-moved module:
`ws new maddi-split --from ws/server` (all repos; the refactor side has import edits in stages 1–2),
then `eval "$(ws env maddi-split)"` before any Gradle command. Always `--no-build-cache`.

### Stage 0 — baseline and the tier check

1. Baseline, recorded in the handoff (counts, not colours):
   - maddi: `./gradlew test --no-build-cache` — tests executed and passed, per module
     (`build/test-results/test`); `./gradlew slowTest` with the corpus present, same reading
     (`build/test-results/slowTest`; an empty directory is not a result — see `AGENTS.md`).
   - refactor composite: `cd jfocus-refactor-service && ./gradlew compileJava compileTestJava
     --no-build-cache` and the same in `jfocus-refactor-server`.
   - ext: `./gradlew :maddi-ide-daemon:installDist`, `:maddi-gradleplugin:test` (includes
     `TestAnalyzerPluginShadedJarIsolation`), `:maddi-mvnplugin:test`.
   - the CLI's parse-only route, which the refactor side's module-graph recipe uses:
     `maddi-run-openjdk Main --compile-log … --analysis-steps=none` on one small corpus, exit code and
     the count of source sets it derives.
2. Write `tools/check_tiers.py`: reads `tiers.txt` (one line per module: `<module> <tier>`, the source
   of truth for §2), every `build.gradle.kts` (`project(":…")` per configuration) and every
   `module-info.java` (`requires`), and fails on any edge — **main or test scope** — that goes up a tier
   or from base to ext. For ext the rule is compile scope: `api`, `implementation`, `compileOnly` and a
   `requires` on a mod module fail; `runtimeOnly` and `shade` on a mod module are allowed and, for the
   daemon and the two plugins, **required** (the check reports their absence too). Run it now: it must
   fail on exactly the edges §2 names (aapi-parser→mod, run-config→aapi-parser, the four `run-*`
   modules→mod, the test-scope edges of run-openjdk, run-kotlin, run-config and inspection-kotlin, and
   the compile-scope edges of ide-daemon, gradleplugin and mvnplugin into mod) and nothing else. That
   is the check's own test.

Gate: baseline recorded; `check_tiers.py` reports the known edges and no other.

### Stage 1 — invert run-config ↔ aapi-parser

Move `AnalysisHintsConfiguration` and `AnalysisHintsConfigurationImpl` from aapi-parser into run-config.
A JPMS package lives in one module, so they change package: `io.codelaser.maddi.run.config.hints` (or the
root `run.config` package; pick one and say why). aapi-parser then depends on run-config; run-config
drops aapi-parser. Update the 17 import sites (run-config 4, run-main, run-openjdk, run-kotlin, the
daemon, both plugins, and any on the refactor side — `grep -r 'aapi.parser.AnalysisHintsConfiguration'`
over the workspace). Update both `module-info.java`.

Gate: maddi suite counts unchanged; refactor composite compiles; `check_tiers.py` now lists run-config
as base with one remaining edge (its single analysis-running test, handled in stage 3).

### Stage 2 — the code-structure graph down into base

New module **`maddi-callgraph`** (JPMS `io.codelaser.maddi.callgraph`, package the same), depending on
cst-api, cst-impl, cst-analysis, graph, inspection-api, util. Move `ComputeCallGraph`,
`ComputeAnalysisOrder`, `PrimaryTypeUseGraph` and their tests. Move the `RECURSIVE_METHOD` property
constant to `PropertyImpl` in cst-analysis next to the other method verdicts (users: link's
`ExpressionVisitor` and `LinkCodec`, the refactor side's dataflow). Leave
`ComputePartOfConstructionFinalField` in prepwork (move it up one package if the `callgraph` package
would otherwise hold one file). Update imports in prepwork (`PrepAnalyzer`), analyzer
(`IteratingAnalyzerImpl`), link (`ExpressionVisitor`, `LinkCodec`), the `run-*` drivers, the daemon
(`WarmAnalysisService`), and on the refactor side: `codelaser-metrics-common` (`Prepwork`,
`AnalysisResultsCache`), cluster, cycle, footprint, dataflow, extractinterface, methodcallgraph. Add the
new coordinate to the refactor build files that need it; do **not** add it to the copy-pasted blocks that
do not use it.

Before moving, read `ComputeCallGraph` once for anything that reaches into prepwork by name rather than
by import (string property names, reflection, a `Codec` registration); the imports say it is clean, a
read confirms it.

Gate: maddi suite counts unchanged (moved tests execute at their new location); refactor composite
compiles and `codelaser-metrics-*` tests pass; `check_tiers.py` places `maddi-callgraph` in base and
`run-rewire` no longer has a main-scope edge into mod.

### Stage 3 — the analysis-step service, and the run drivers into base

This is the stage that decides the shape of both walls, so read `RunAnalyzer` (openjdk, ~500 lines) and
`RunMixedPrepAnalyzer` end to end before designing, and write the interface down in the handoff before
implementing it.

1. **The interface**, in a new base module **`maddi-analysis-api`** (JPMS `io.codelaser.maddi.analysis.api`;
   depends on cst-api, inspection-api, graph, callgraph, run-config). One service type, discovered with
   `ServiceLoader` — say `AnalysisStep`: a literal `name()` (the `--analysis-steps` vocabulary: `prep`,
   `modification`, `rewire-tests`, plus the result IO and the hints compile and write, which today are
   not steps but flags and mojos), the names of the steps it must follow, and one `run(AnalysisContext)`
   that mutates the analysis maps on the CST. `AnalysisContext` carries what the pipeline has by then:
   runtime, inspector, parse result, the call graph and analysis order (base since stage 2), the
   configuration, the result-IO hooks, and an optional value feed. Besides the step, the module holds
   what ext needs to compile (§2): `AnalysisValueFeed` (moved down as is), a results-IO interface
   (load and write analysis values for a source set, the codec obtained from the registry), a
   `Decorator` interface (values → annotations and comments) with the implementation moved down from
   prepwork's `io` package, a problem report type that replaces `AnalyzerException` on the client side,
   and the step's list of bookkeeping properties a client must hide. Two rules from the refactor side's
   `DslModuleProvider`, which is the same pattern and has the scars: **names are literals**, and the
   registry that loads them **must throw, naming the missing jar, when a step requested on the command
   line has no provider** — the CLI already exits 0 for `--analysis-steps=none` whether or not the
   sources parsed, and a silent no-op analysis would be the same defect one tier up. JPMS: the pipeline
   module declares `uses`, the implementation module `provides … with`; without `uses` the loader
   returns an empty list.
2. **The implementations**, in a new mod module **`maddi-run-analysis`** (depends on the four
   modification modules, aapi-parser, analysis-api): the step bodies lifted out of the two pipeline
   classes, one class per step, and the codec registrations the result IO needs. The pipeline classes in
   `run-openjdk`, `run-kotlin` and `run-main` keep the orchestration (parse → preload → steps in
   dependency order → write) and lose every modification import. `run-kotlin`'s mixed-source posture
   (fault-tolerant per type, `ShallowMethodAnalyzer` for what the Kotlin front-end cannot give) is part
   of the `prep` step's configuration, not of the driver.
3. **The tests move** to `maddi-run-analysis`: the 10 + 15 + 1 driver tests that import the
   modification tier, the `slowTest` battery with `corpus/` and its `-Dmaddi.corpus.required` wiring,
   and inspection-kotlin's 24 analyzer tests (they test that the Kotlin CST feeds the analyzer
   faithfully, a claim about both tiers). The tests that remain in `run-*` and `inspection-kotlin` are
   the ones about parsing, configuration and the compile-log route. Drop the now-unused
   `testImplementation` lines.
4. **ext becomes a client of the same steps.** `WarmAnalysisService` loses its hand-written copy of the
   pipeline and runs the steps the CLI runs (one pipeline, not two — the daemon's "as
   `RunMixedPrepAnalyzer` does it" comments are the tell). `HintsLoader`, `AnnotationTagger`,
   `ResultCollector` and `StreamingValueFeed` move to the `analysis-api` types. The Maven hints write
   becomes a step; its comments-decorator subclass goes to `maddi-run-analysis`. Then the build wiring:
   the daemon's and the plugins' compile class paths hold base only; `maddi-run-analysis` and the four
   modification modules are `runtimeOnly` in the daemon (so `installDist` ships them) and `shade` in the
   plugins with `shade` **no longer** extended by `implementation`; the daemon's `module-info.java`
   drops the `requires io.codelaser.maddi.modification.*` lines and gains `uses`.

Gate: the number of tests executed across `run-openjdk` + `run-kotlin` + `run-config` +
`inspection-kotlin` + `maddi-run-analysis` equals the stage-0 baseline for the first four, and the
slow-test battery reports the same count from its new module; the parse-only CLI run from stage 0 gives
the same exit code and source-set count; a CLI run with `--analysis-steps=modification` and
`maddi-run-analysis` **absent** from the class path fails with the message that names it; with it present,
the results written for one small corpus are byte-identical to the stage-0 run (`diff -r` on the JSON);
the daemon's analysis of the same corpus (its `analyze` request, run through `maddi-ide-client` against
the `installDist` tree) produces the same values, and its `installDist/lib` holds the mod jars; the
plugin isolation test passes and the Maven plugin tests pass; `check_tiers.py` places every `run-*`
module and `analysis-api` in base with **no** edge, main or test, into mod, and reports for ide-daemon,
gradleplugin and mvnplugin no compile-scope edge into mod and a runtime one present.

### Stage 4 — build logic that can be shared across repositories

`buildSrc` cannot be consumed from another build. Rename it to a standalone included build
`build-logic/` (same plugin id `java-library-conventions`), wired through
`pluginManagement { includeBuild("build-logic") }` in this repository's settings, and through
`pluginManagement { includeBuild("../maddi/build-logic") }` from the other two — the same pattern the
refactor side uses with `gradle-conventions`. Same for `platform`: it stays here, and the other two reach
it as `io.codelaser:platform` through `includeBuild("../maddi")`.

Gate: `./gradlew build --no-build-cache` green in the single repository, unchanged test counts.

### Stage 5 — the physical split

Order matters, because docs cite commit SHAs (see `docs/project/history-rewrite-2026-08-04.md` for what
a rewrite costs):

1. **`maddi` keeps its history and its SHAs.** The mod and ext modules are removed with `git rm`; their
   history stays reachable in this repository's log. Nothing is rewritten.
2. **`maddi-mod` and `maddi-ext` are carved from a fresh clone with `git filter-repo`** and a
   `--path` list per tier (modules, `corpus/` for mod, `dogfood/` for ext, and the docs from §2 that
   belong to that tier). Each new repository holds the full history of its own paths.
3. Each new repository gets: `settings.gradle.kts` with `pluginManagement { includeBuild("../maddi/build-logic") }`
   and `includeBuild("../maddi")` (ext also `includeBuild("../maddi-mod")`, whose modules it
   names only in `runtimeOnly` and `shade` configurations), the Gradle wrapper,
   `gradle.properties` with its own `version` (start both at `0.9.1`, the current line), and a README
   that states its tier and the rule from §1. Cross-repository dependencies are written as coordinates,
   `implementation("io.codelaser:maddi-cst-api:$maddiVersion")`, exactly as the refactor side already does
   for maddi; `includeBuild` substitutes them from source.
4. `docs/`: design and roadmap documents about prepwork, link and the analyzer move with the mod
   repository; `docs/README.md` in each repository indexes only what it holds and links the others.
   `PUBLISHING.md` splits: annotations and support (Central) and the parse-only CLI stay here; the
   plugins and the analysing CLI zips go with ext, and the note that the plugins shade the analyzer now
   reads as "ext bundles mod and base". `CONTRIBUTING.md`'s customer-name hook is installed in all three.
5. Workspace tooling: the `.ws` `REPOS` lists in `ws/*` and `ALL_REPOS` in jfocus-devops `scripts/ws.conf`
   gain the two repositories; `jfocus-refactor-service/settings.gradle.kts` and the server's replace
   `includeBuild("../maddi")` with maddi + maddi-mod (ext is not on the refactor side's graph).
   Creating the remotes on `laser1` is the user's step; the thread leaves two local repositories under
   `~/git/` with a clean `main`/`devel` and says so.

Gate, run in the aside workspace with all three checkouts side by side:
- `maddi`: `./gradlew build --no-build-cache`; `check_tiers.py` (now trivially base-only); the
  parse-only CLI run from stage 0.
- `maddi-mod`: `./gradlew test --no-build-cache` and `slowTest` with the corpus present, read
  from `build/test-results/slowTest`.
- `maddi-ext`: `:maddi-ide-daemon:installDist` and the daemon analysis from stage 3, the Gradle plugin
  isolation test, the Maven plugin tests, `dogfood`; and the proof that mod is runtime-only: ext's
  `compileJava` tasks succeed with `../maddi-mod` **absent** from the workspace (the composite
  then fails only at the runtime configurations, which the check names).
- refactor composite: both `compileJava compileTestJava` gates from stage 0, plus the
  `codelaser-metrics-*` tests.
- Test counts per module equal the stage-0 baseline, module for module (moved tests counted at their
  destination).

### Stage 6 — the wall (mechanism only, default unchanged)

Add to `maddi-mod` and `maddi-ext` a property switch: `-PmaddiFromSource=false` drops the
`includeBuild("../maddi")` and resolves `io.codelaser:maddi-*:$maddiVersion` from a Maven repository
(`publishToMavenLocal` from `maddi`, or the file repository jfocus-devops will designate). Prove it once:
publish base, build mod against the published jars with the switch off, run its tests. **Leave the
default at from-source** until the refactor side has been split and its census and ratchet workflows
re-pointed; flipping the default is a decision for the user, recorded in the handoff as the next step.

## 4. Rules for the thread

- maddi is edited in `ws/<workspace>/maddi` only; never in `~/git/maddi` directly.
- A JPMS package lives in exactly one module: every move is a package rename, and every
  `module-info.java` on both sides changes with it. `requires` is the ground truth the tier check reads.
- Do not "clean up" declared-but-unused dependencies outside the modules a stage names; that is a
  separate, measurable change (the workspace map at 2026-09-14 counted 579 of 1,203 main-scope
  declarations unused, and the refactor side's are copy-pasted blocks). Note them, do not touch them.
- The refactor side is touched only for imports and coordinates that a stage names. No verb logic.
- Report numbers: tests executed and passed per module before and after; the tier check's output; the
  byte-comparison of analysis results in stage 3.
- Commit per root cause, with the stage in the subject; the user pushes.
- The customer behind the private corpus is never named (CONTRIBUTING.md; the hook enforces it).
- If a stage's gate cannot be made green without touching something outside its scope, stop that stage,
  write down what was found, and continue with the stages that do not depend on it.

## 5. The contract with the refactor side (not this thread's work)

After this split, the refactor engine's base tier imports from `maddi` only:
cst-api, cst-impl, cst-analysis, cst-io, cst-print, graph, inspection-api, inspection-resource,
inspection-openjdk, inspection-integration, inspection-mixed, java-openjdk, java-parser, java-bytecode,
aapi-archive, run-config, **callgraph**, **analysis-api**, support, util, annotation, kotlin-realm. It
must not import modification-common, prepwork, link, analyzer, aapi-parser or run-analysis.

The service interface of stage 3 is the seam refactor-base will use: today `Prepwork.make` in
`codelaser-metrics-common` calls `PrepAnalyzer` directly; afterwards it computes the call graph and order
itself (base) and asks the `prep` step, obtained through the same `ServiceLoader`, to do the rest. The
`Prepwork` record's fields are all base types already. The refactor-side thread that follows will make
that change; move `AnalysisResultsCache` (result IO is mod), `codelaser-metrics-dataflow`,
`codelaser-metrics-immutable` and the one variable-data use in `codelaser-metrics-duplicate` to the mod
side; and only then take `maddi-mod` off refactor-base's graph. Design the `AnalysisContext` of
stage 3 with that caller in mind: it holds a parse it made itself, not one the CLI made.

## 6. Decisions taken by default — say so if you want another

| decision | default in this plan |
|---|---|
| repository names | `maddi`, `maddi-mod`, `maddi-ext` (the user chose the short name for mod, 2026-09-30) |
| base keeps its SHAs | yes: `git rm` in base, `filter-repo` only for the two new repositories |
| new module for the call graph | `maddi-callgraph`, JPMS `io.codelaser.maddi.callgraph` |
| `recursiveMethod` property | moves to cst-analysis `PropertyImpl` |
| the service interface | `maddi-analysis-api` (base), one `AnalysisStep` type, `ServiceLoader` discovery, literal names, a missing provider throws |
| what else moves down into `analysis-api` | `AnalysisValueFeed`, a results-IO interface, a `Decorator` interface plus the implementation from prepwork's `io` package, a problem report type, the bookkeeping-property list |
| ext's dependency on mod | `runtimeOnly` (daemon) and `shade` not extended by `implementation` (plugins); no `requires`; the daemon runs the CLI's steps, not a copy |
| the implementations and the analysing tests | `maddi-run-analysis` (mod), including the slow-test battery and `corpus/` |
| inspection-kotlin analyzer tests | move to `maddi-run-analysis` |
| shared build logic | `buildSrc` → included build `build-logic/` in `maddi` |
| versions | three `gradle.properties`, all starting at `0.9.1`, bumped independently |
| from-source vs pinned | mechanism built in stage 6, default stays from-source |
| Bazel files | travel with their modules; not a gate |

## 7. Progress

Carried out in the workspace `ws/split` (all nine repositories, branched from `ws/server`).

### Stage 0 — done (2026-09-30)

- `tools/tiers/check_tiers.py` + `tools/tiers/tiers.txt`. Edges come from build configurations,
  `requires`, and source imports mapped to the module that owns the package (the plugins and the Kotlin
  modules have no descriptor). `tiers.txt` holds the TARGET tiers, so the check fails until the stages
  land. On `ws/split` at the start it reports 33 violations and 3 missing runtime edges, exactly the
  edges of §2: the four `run-*` modules and run-config into mod, inspection-kotlin's tests into
  prepwork, and the daemon's and both plugins' compile-scope use of mod (the plugins' `shade` is
  extended by `implementation`, so it counts as compile). aapi-parser is not reported: it is tiered mod.
- Baseline, maddi `./gradlew test --no-build-cache`: **4,003 tests executed, 1 failed, 45 skipped**
  across 31 modules. The failure is pre-existing and unrelated to the split:
  `maddi-modification-link` `TestKotlinLinkCollections.indexAndCopy` (the Kotlin `copyOf` fixture's
  links come out empty where the Java twin has two). Per-module counts are in the stage-0 commit message.
- Baseline, refactor side: the composite roots have no `compileJava` of their own (§3 stage 0 named a
  task that does not exist). The gate is instead `compileJava` + `compileTestJava` of every
  single-project included build of `jfocus-refactor-server`'s settings (50 builds, a superset of the
  service composite's): **green**.
- Baseline, ext: `:maddi-ide-daemon:installDist` green, 48 jars in `lib/`; the Gradle plugin's 14 tests
  (with the shaded-jar isolation test) and the Maven plugin's 14 pass inside the suite above.
- Deferred to the start of stage 3, where they are first needed: the parse-only CLI run, the analysis
  results for the byte comparison, and `slowTest`. They are taken from the last commit before stage 3.

### Stage 1 — done (2026-09-30)

- `AnalysisHintsConfiguration` and `AnalysisHintsConfigurationImpl` now live in run-config's root package
  `io.codelaser.maddi.run.config`, beside the `Configuration` that holds them: no new package, no new
  export. aapi-parser never used them itself. 12 importers rewritten (run-config, run-main, run-openjdk
  main and tests, both plugins). run-config drops aapi-parser from Gradle, its descriptor and Bazel;
  run-kotlin drops the aapi-parser line whose comment said it was there for this type only (it still
  reaches aapi-parser at run time through run-openjdk). The refactor side never named either type.
- Gate: maddi suite identical to the baseline, module for module (4,003 run, the same 1 failure);
  refactor composite compiles; run-config has **no** violation left — its one analysis-importing test
  was the serialisation test of these types. 34 problems remain.
