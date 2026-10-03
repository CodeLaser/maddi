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
  36 modules; stage 5), `corpus/` (configs for the slow-test battery; mod -- ⚠ moved back to base on
  2026-10-03, see §7 "corpus/ back to base"), `dogfood/` (runs the analyzer plugin over maddi's own modules;
  ext, it depends on the plugin).
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
| repository names | `maddi`, `maddi-mod`, `maddi-dist` (the user chose the short name for mod and `maddi-dist` over `maddi-ext`, 2026-09-30; the tier formerly called ext is `dist`) |
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

### Stage 2 — done (2026-09-30)

- New base module **`maddi-callgraph`** (`io.codelaser.maddi.callgraph`): `ComputeCallGraph`,
  `ComputeAnalysisOrder`, `PrimaryTypeUseGraph`, and the 8 tests that exercise them without running prep
  (29 tests, with a trimmed `CommonTest` copy). The read found no by-name reach into prepwork. The 5 tests
  in the old package that run prep first stay in prepwork; `TestComputePartOfConstruction` keeps a copy
  of the one fixture it borrowed from `TestCallGraph`.
- `RECURSIVE_METHOD` is declared in cst-analysis `PropertyImpl` and registered in `PropertyProviderImpl`
  (required by `TestEveryWritablePropertyDecodes`, which enumerates `PropertyImpl` reflectively); the
  link codec's private map entry for it is gone, its fallback finds it. Same key and default.
- prepwork depends on the new module as `api` / `requires transitive`: `PrepAnalyzer` returns a
  `ComputeCallGraph`. `run-rewire` depends on it **instead of** prepwork and is now clean.
- Refactor side, 94 files rewritten. Five metrics modules (cluster, cycle, footprint, methodcallgraph,
  movetypegraph) used prepwork only for the call graph and now depend on `maddi-callgraph` instead; the
  others add it and keep prepwork (§5 is the refactor-side thread's). Every repository's `maddiProjects`
  list in `gradle.properties` must name a new maddi module, or the composite does not substitute it and
  Gradle looks for it in a Maven repository — stage 5 has to extend those lists the same way.
- Gate: maddi suite 4,003 run, the same single failure, prepwork 289 → 260 and maddi-callgraph 29;
  refactor composite compiles; metrics tests: cluster 42, cycle 32, extractinterface 177, footprint 12,
  methodcallgraph 7, movetypegraph 1, all pass; dataflow 37 run with 1 failure,
  `TestMethodFlow` "constructor linked to return variable", which fails identically at the stage-1
  commits of all repositories (pre-existing). Tier check: 33 problems.

### Stage 3 — done (2026-09-30)

Three commits: 3a (the service, the drivers into base, the tests into mod), 3b (ext compiles against base
only), and the CLI launchers into mod, which 3a had left broken.

- **Deviation: a typed engine, not a generic step.** `maddi-analysis-api` holds one service type,
  `AnalysisEngine`, with one typed method per `--analysis-steps` step and per service around them (prep,
  modification, results loading and writing, hints compile and compose, decorator, shallow defaults), plus
  `bookkeepingProperties()` for the clients to hide. Every caller consumes a typed result (the call graph,
  the isolated problems, the messages), which a generic `run(AnalysisContext)` would have hidden behind
  casts. `AnalysisEngines.require(why)` throws naming `io.codelaser:maddi-run-analysis (repository
  maddi-mod)`; `--analysis-steps=none` needs no engine. Request records carry the options; a null field
  keeps the analyzer's default, so each driver states exactly what it stated before.
- `maddi-run-analysis` (mod) implements it with the bodies moved, not rewritten, from run-openjdk's
  `RunAnalyzer`, including the CLI's environment gates (SHADOWDIFF, MODREACH, CHECKPOINT, INCREMENTAL).
  The drivers run-openjdk, run-main and run-kotlin compile against analysis-api alone.
- **Deviation: three test hosts, not one.** The analysis-running tests moved to `maddi-run-analysis`
  (from run-openjdk and run-main, with `slowTest`, the dogfood ratchet, `corpus/` and the JSON
  resources), `maddi-run-kotlin-analysis` (from run-kotlin: the K2 realm setup) and
  `maddi-inspection-kotlin-analysis` (from inspection-kotlin: the flat front end, Kotlin plugin). The
  realm and the flat front end cannot share a test JVM. Found by running the base tests WITHOUT the engine
  and reading their captured output for its error: several tests call `Main` without checking the exit
  code and passed while doing nothing.
- 3b: the daemon runs prep and modification through the engine, its collector hides the engine's
  bookkeeping properties, its hints loader and tagger use the engine's loader and decorator; build and
  descriptor name the API only, `run-analysis` is `runtimeOnly`. The plugins split `shade`: base modules
  in `shade` (compiled against), mod modules in `shadeRuntime` (extended by `runtimeOnly`), and the shadow
  jar bundles `shadeAll`, which extends both so a shared transitive is bundled once. The Maven hints
  mojo's comment decorator delegates to the engine's decorator instead of subclassing `DecoratorImpl`.
- **Added: `maddi-cli` and `maddi-cli-kotlin` (mod)**, the shipped `maddi` and `maddi-kotlin`
  distributions: a driver plus `maddi-run-analysis`, no code of their own; `maddi-kotlin` keeps its
  `lib-k2/` layout for the IDE plugins' installer. The base drivers lost `application`; release-cli.sh,
  the corpus Taskfile and catalogue (launcher `run` vs test-host `slowTest`, now two maps), the Bazel
  binary and the docs follow. run-main keeps its launcher (nothing ships it). **Stage 5 consequence:** the
  corpus scripts call `{maddi}/gradlew … :maddi-cli:run`; after the split that module is in maddi-mod.
- The refactor side: `jfocus-refactor-server` projectconfig takes `runtimeOnly` maddi-run-analysis
  (its `RunAnalyzerCommand` runs the openjdk driver); `maddiProjects` names analysis-api everywhere and
  run-analysis in server and service; two Kotlin prepare scripts call `:maddi-cli-kotlin:run`.
- Gate, as measured:
  - suite 4,003 run, 45 skipped, the same single pre-existing failure; run-openjdk 51 → 30 + 21,
    run-main 9 → 6 + 3, run-kotlin 56 → 33 + 23, inspection-kotlin 80 → 17 + 63;
  - slow battery: 20 classes, 30 tests before and after, 11 classes now in the two mod hosts (counted in
    the sources; the battery was **not executed**, it needs the corpora and hours);
  - analysis results, the pre-stage-3 CLI (`9d8198ed7`) against `maddi-cli`, `--analysis-steps=
    modification` on maddi-util, maddi-graph and maddi-cst-api: `diff -r` byte-identical, same verdict
    fingerprints, same exit codes (0, 0, 5);
  - the CLI without `maddi-run-analysis` in `lib/`: parse-only exits 0, modification exits 1 with the
    message naming the jar;
  - the daemon: `analyzeProject` over its socket, pre-stage-3 `installDist` against the new one, on
    maddi-util and maddi-graph: identical results once elapsed time and object identity hashes are
    masked. (The daemon sends prep's `variableData` as `VariableDataImpl@<hash>` in an element's
    properties, before and after: a value that is not a result, and not hidden as bookkeeping.)
  - daemon `installDist/lib` holds the mod jars; both shadow jars hold the analyzer once and register the
    engine; plugin tests 14 + 14 pass;
  - tier check: 0 problems (50 modules).
  - Not measured: the stage-0 parse-only baseline was never taken; the parse-only runs above exit 0.
- Found on the way, not caused by the split: the installed `maddi-kotlin` does not exit after a
  successful run on Kotlin sources. A K2 "ApplicationImpl pooled thread" is non-daemon, and `Main` calls
  `System.exit` only on failure. Judged from the code (Main has no split commit), not from a pre-split run.

### Before stage 5 — decisions and preparation (2026-09-30)

- **The third repository is `maddi-dist`** (the user's choice over `maddi-ext`); the tier is called `dist`
  everywhere (`tiers.txt`, `check_tiers.py`). **The CLI distributions go there**: `maddi-cli` and
  `maddi-cli-kotlin` are tiered dist. Consequence for stage 5: the corpus catalogue and Taskfile (mod) call
  `:maddi-cli:run` and `:maddi-cli-kotlin:run`, which after the split live in `../maddi-dist` — a reference
  in tooling only, not in any build.
- **The dist compile-time wall is enforced by the build**: `maddi-tier-guard` in build-logic fails any
  dist module whose compile class path or annotation-processor path RESOLVES a mod module (by name, so
  from source and pinned alike). Applied by java-library-conventions (inert outside dist) and by
  maddi-intellij directly. Four deliberate leaks fail with its message. Base and mod need no such guard:
  after the split neither build includes a tier above it. `tiers.txt` moved into build-logic's resources
  as the single list for the guard and `check_tiers.py`. A CI job compiling dist without maddi-mod was
  considered and not built.
- **No reflection across the wall**: the analyzer's static switches (static side effects, eventual
  cluster) are `ModificationOptions` fields; the daemon test uses them.
- **The eventual ratchet moved to `maddi-gradleplugin`** with the dogfood input task, running through
  `AnalysisEngine`; it passes (317 survivors against the pinned list). A mod test no longer needs a dist
  build.
- Fixed on the way: `build-logic/settings.gradle.kts` of stage 4 was one unclosed comment.

### Stage 5 — done (2026-09-30)

- **Carve.** maddi-mod and maddi-dist were carved with `git filter-repo` from a clone of this branch
  (`ws/split` at `6de106609`), with their paths PLUS the directories those paths were renamed from
  (`aapi-parser`, `modification-io`, `modification-linkedvariables`, `modification-common`,
  `modification-prepwork`, `maddi-modification-io`, `maddi-modification-linkedvariables`; `run-gradleplugin`,
  `run-mvnplugin`) and the old root-level names of moved docs: 1,417 and 178 commits back to the first
  import. Files that moved in from a module that stays in base (the analysis tests out of run-openjdk) keep
  their earlier history here only. Each carved tree equals this branch's files at those paths. The user's
  initial commit on GitHub (`.gitignore`, `.githooks`) is replayed on top, on branch `ws/split` in each
  repository; their `main` still equals `origin/main`.
- **maddi** keeps its SHAs: one `git rm` commit, settings with the base modules only, root documents open
  with where the moved modules live, `docs/README.md` still indexes all three repositories.
- **Cross-repository references**: every `project(":…")` into another repository became an `io.codelaser`
  coordinate at `maddiVersion` / `maddiModVersion`; source paths into base go through `../../maddi`;
  maddi-dist's dogfood input task reaches maddi's jar tasks through `gradle.includedBuild("maddi")`; the
  conventions take the platform BOM by coordinate outside maddi; the server composite substitutes it
  explicitly. The corpus tooling (maddi-mod) finds the CLI launchers and the build plugins in
  `MADDI_DIST_REPO`, the sibling `../maddi-dist` by default. `TestVersionSkew` checks each jar in the daemon
  distribution against the version of the repository that builds it.
- **Refactor side**: the seven repositories that include maddi also include `../maddi-mod`; `maddiProjects`
  keeps the base modules and `maddiModProjects` lists the mod ones (clean/test links follow).
- Gate, as measured:
  - maddi `./gradlew build` green; maddi-mod `test`: 1,550; maddi-dist `test`: 118; together **4,003, module
    for module as the monorepo**, with the single pre-existing failure (now in maddi-mod);
  - tier check over the three roots: 50 modules, 0 problems; maddi-tier-guard active in maddi-dist;
  - the `maddi` CLI installed from maddi-dist writes modification results on maddi-util byte-identical to
    the pre-stage-3 CLI's; the daemon distribution bundles the six mod jars;
  - refactor-server and refactor-service composites compile; metrics tests as at stage 2 (one pre-existing
    dataflow failure), `codelaser-metrics-immutable` 78 pass.
  - **Not run**: maddi-mod's `slowTest` against the corpora (hours; the battery's 30 tests are counted in the
    sources, stage 3), and the stage-0 parse-only baseline, which was never taken. "dist compiles with
    ../maddi-mod absent" is replaced by maddi-tier-guard, which the user chose over a CI job.
- **Left to the user**: pushing (`ws/split` in each repository; for the two new ones, deciding whether it
  becomes `main`, which then does not fast-forward from the initial commit on GitHub); adding maddi-mod and
  maddi-dist to jfocus-devops `ws.conf` `ALL_REPOS` and to `~/git/` as main checkouts, which the `ws` tool
  expects (in `ws/split` they are plain clones, so `.ws` was left as it is); CI workflows for the two new
  repositories (each needs the sibling checkouts).

### Stage 6 — done (2026-09-30), default unchanged

- `maddi-publishing` (build-logic) gives every base and mod library a Maven publication; the Kotlin modules
  outside the conventions apply it directly. maddi-mod and maddi-dist take `-PmaddiFromSource=false`: no
  included sibling, coordinates from Maven local (`-Dmaven.repo.local=<dir>` for a designated one) or
  `-PmaddiRepo=<url>`, listed before Maven Central.
- Proven: maddi published to a scratch directory (32 artefacts), maddi-mod `clean test` pinned = the same
  1,550 tests; maddi-mod published too, maddi-dist `clean test :maddi-cli:installDist` pinned = the same 118.
  Not covered pinned: maddi-dist's dogfood `slowTest` (it needs the included maddi build).
- The default stays from source. Flipping it is the user's decision, after the refactor side is split and
  its census and ratchet workflows are re-pointed.

### Pinning — the jfocus side pinned by default (2026-09-30)

- maddi and maddi-mod are at 0.9.2; every jfocus build pins `maddiVersion=0.9.2` (maddi) and
  `maddiModVersion=0.9.2` (maddi-mod), two properties since the split. The pinned jars come from each
  machine's `~/.m2`: publish them with `./gradlew publishToMavenLocal` in maddi, then in maddi-mod, at
  the commit the pin names — on every machine that builds the jfocus side, the gate's included.
- The switch lives once, in gradle-conventions: the settings plugin `jfocus-maddi-source`, applied by
  every jfocus build (each module is its own build, and a repository does not reach included builds).
  Pinned, it adds `~/.m2` (or `-PmaddiRepo=<url>`) as the EXCLUSIVE source of maddi's coordinates.
  `-PmaddiFromSource=true` includes `../maddi` and `../maddi-mod` instead, through `includeMaddiBuild`.
- That function is not spelled `includeBuild` on purpose: `ws rdeps` reads the include graph from the
  settings text, and a pinned dependency is not an edge. Now a maddi change retests maddi, maddi-mod and
  maddi-dist; a maddi-mod change, maddi-mod and maddi-dist; the jfocus repos retest when the pin moves.
- maddi-mod and maddi-dist still default to from source: their own switch (stage 6) is unchanged.
- Proven on ws/split: the server and service composites compile pinned (resolving the 0.9.2 jars, not the
  projects) and from source; the five other repo composites configure both ways; 1,274 jfocus tests pass
  pinned (graalpy, metrics-dataflow, stdbase-parser, transform-common, standardize-encoder, refactor-impl).

- **Moved to 0.9.3 (2026-10-01)**: maddi devel's fixes of that day (an unresolved unqualified call is an
  unresolved symbol; abandoned types) cured jfocus's `TestGuavaStress`. maddi and maddi-mod 0.9.3 published to
  `~/.m2`; the jfocus pins moved with gradle-conventions' `pinMaddi` task, run at each repository's root:
  `./gradlew pinMaddi -PtoMaddi=0.9.3 -PtoMaddiMod=0.9.3` (only the two pin lines of every gradle.properties
  change). ⚠ Done by hand, before `release-maddi` exists (jfocus-devops `gate/RELEASE-MADDI.md`, design only):
  no gate record, no `-alpha.N`.

### corpus/ back to base (2026-10-03)

`corpus/` (the catalogue, its scripts and `corpus/Taskfile.yml`) went to maddi-mod at stage 5 as "configs for
the slow-test battery", because that battery runs the analysis. It moved back to maddi; the reason no longer
held, for three reasons:

1. **The base tier consumes the corpora.** maddi-run-openjdk, maddi-run-kotlin and maddi-util read them --
   maddi-util holds the `Corpora` locator every corpus test resolves through -- so the code that reads them
   was in base while the tooling that produces them sat one tier up. An upward dependency, and one
   `check_tiers.py` cannot see: it reads Gradle files and `module-info.java` only.
2. **The tooling is parser-level work**: obtain each checkout at its pin, generate `inputConfiguration.json`
   from a compile log (maddi's parse-only CLI route), vendor the jars it names (the Lombok substitution of
   2026-10-03 is a parser concern too).
3. **maddi's own documents never followed the move**: `CONTRIBUTING.md`, `docs/status/kotlin-corpora.md`,
   `docs/status/kotlin-gap-analysis-2026-09-21.md` and aapi-archive's GUAVA.md, ECLIPSECOLLECTIONS.md and
   ENGINE-WORKLIST.md all say `corpus/...` as if it were here. Now they are right again.

No Java code reads files under `corpus/` at run time (only comments name it). maddi gets a root Taskfile
(`task corpus:*`); maddi-mod's keeps a `corpus:` include of `../maddi/corpus`, because the analysis's corpus
tests -- `test:slow`, `sweep`, a catalogue entry's `parse.test` -- still run there: the corpus Taskfile and
`catalogue.py` find maddi-mod as `MADDI_MOD_REPO`, the sibling `../maddi-mod` by default, beside
`MADDI_DIST_REPO`. maddi's copy is verbatim from maddi-mod `0bb09066`; the history before it stays in
maddi-mod (`git log -- corpus`). jfocus-devops (`corpus/Taskfile.yml`'s `MADDI_CORPUS`, `gate/lib.sh`'s
corpus pre-flight, `gate/corpus-refs.py`, the private catalogue overlays' headers) and diagnostics (README,
`scoreboard/catalogue.py`) name the new place in the same change.
