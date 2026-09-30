# Split maddi into three repositories — work plan

**Status: plan (2026-09-30).** Written for the thread that carries it out. Every claim about the code
below was measured on `ws/server` at `3329d329d` (maddi) on 2026-09-30; re-measure before relying on a
number, the commands are given.

## 1. Purpose, and the one rule

The whole maddi+jfocus codebase is being reorganised into repositories with taller walls, so that the
~8 Claude threads supporting two developers stay bound to their topic. maddi goes first. It becomes three
repositories:

| repository | tier | holds |
|---|---|---|
| `maddi` (this one, name and history kept) | **base** | CST, parsers and front-ends (Java, bytecode, Kotlin), inspection, graph, annotations, support, util, the annotated-API archive, the run configuration, and — after stage 2 — the code-structure (call) graph |
| `maddi-modification` | **mod** | prepwork, link, analyzer, modification-common, the AAPI compiler, the `run-*` drivers |
| `maddi-ext` | **ext** | IDE daemon and client, IntelliJ, Eclipse, VS Code, the Gradle and Maven plugins |

The rule that the split enforces, and that a check script (stage 0) keeps enforced:

> **base depends on nothing above it; mod depends on base only; ext depends on both.** A consumer of the
> base tier sees the modification analysis only as values on `Info`, through the property map of
> `cst-api` and the property constants of `cst-analysis`. Anything that *runs* the analysis, *persists*
> it, or names its *internal* types is mod-side.

The consumer this rule is for: the refactor engine's base tier (`refactor-api`, `refactor-impl`,
`commonservice`, `conformance`, the shared metrics) **will build without `maddi-modification` on its
class path or module path.** What it needs from maddi must therefore live in base. Today that is
everything it imports except two things: the call graph (stage 2) and the run configuration's dependency
on the AAPI compiler (stage 1). The refactor side's own moves (the prepared-project factory behind a
provider, the results cache and the dataflow metric to the mod side) are a later thread's work and are
listed in §5 as the contract this split must honour.

## 2. The tiers as measured

Assignment by declared main-scope dependency closure (`build.gradle.kts` `project(":…")` lines; the
`module-info.java` `requires` directives agree — no base descriptor requires a `modification` module):

- **base (26):** aapi-archive, annotation, cst-analysis, cst-api, cst-impl, cst-io, cst-print,
  cst-print-kotlin, graph, inspection-api, inspection-integration, inspection-kotlin, inspection-mixed,
  inspection-openjdk, inspection-parser, inspection-resource, java-bytecode, java-openjdk, java-parser,
  kotlin-api, kotlin-k2, kotlin-realm, manual, support, util; plus `run-config` after stage 1.
- **mod (10):** modification-common, modification-prepwork, modification-link, modification-analyzer,
  aapi-parser, run-config *(until stage 1)*, run-rewire, run-main, run-openjdk, run-kotlin.
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
- **inspection-kotlin's edge into mod is test scope only.** Main scope imports nothing from
  modification. Its build declares `testImplementation(project(":maddi-modification-prepwork"))`, used by
  24 test files: the `prepwork/` test package (ports of the Java prep-analyzer tests, Kotlin source in,
  same `VariableData` assertion strings out), the analyzer smoke test and two printer tests. Stage 3.
- **The eight properties declared in the modification modules** (`links`, `methodLinks`, `typePrepped`,
  `partOfConstructionType`, `recursiveMethod`, `unmodifiedVariable`, `downcastVariable`,
  `localVariablesOfEnclosingMethod`) are the analysis's working state, not verdicts. They stay where
  they are, except `recursiveMethod`, which travels with `ComputeCallGraph` (stage 2). The 57 verdict
  properties are already in cst-analysis, which depends on cst-api only — nothing to do there.
- **Non-module entries in `settings.gradle.kts`:** `platform` (the BOM; base, the other two import it),
  `road-to-immutability` and `maddi-manual` (docs; base), `buildSrc` (one conventions plugin applied by
  36 modules; §3 stage 5), `corpus/` (configs for the `run-openjdk` slow tests; mod), `dogfood/` (runs
  the analyzer plugin over maddi's own modules; ext, it depends on the plugin).
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
     (`build/test-results/test`).
   - refactor composite: `cd jfocus-refactor-service && ./gradlew compileJava compileTestJava
     --no-build-cache` and the same in `jfocus-refactor-server`.
   - ext: `./gradlew :maddi-ide-daemon:installDist`, `:maddi-gradleplugin:test` (includes
     `TestAnalyzerPluginShadedJarIsolation`), `:maddi-mvnplugin:test`.
2. Write `tools/check_tiers.py`: reads `tiers.txt` (one line per module: `<module> <tier>`, the source
   of truth for §2), every `build.gradle.kts` (`project(":…")` per configuration) and every
   `module-info.java` (`requires`), and fails on any main-scope edge that goes up a tier or from base to
   ext. Test-scope edges are reported, and fail only when not listed in an allowlist in `tiers.txt`.
   Run it now: it must fail on exactly the edges §2 names (aapi-parser→mod, run-config→aapi-parser,
   inspection-kotlin test→prepwork) and nothing else. That is the check's own test.

Gate: baseline recorded; `check_tiers.py` reports the three known edges and no other.

### Stage 1 — invert run-config ↔ aapi-parser

Move `AnalysisHintsConfiguration` and `AnalysisHintsConfigurationImpl` from aapi-parser into run-config.
A JPMS package lives in one module, so they change package: `io.codelaser.maddi.run.config.hints` (or the
root `run.config` package; pick one and say why). aapi-parser then depends on run-config; run-config
drops aapi-parser. Update the 17 import sites (run-config 4, run-main, run-openjdk, run-kotlin, the
daemon, both plugins, and any on the refactor side — `grep -r 'aapi.parser.AnalysisHintsConfiguration'`
over the workspace). Update both `module-info.java`.

Gate: maddi suite counts unchanged; refactor composite compiles; `check_tiers.py` now lists run-config
as base with no violation.

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
compiles and `codelaser-metrics-*` tests pass; `check_tiers.py` places `maddi-callgraph` in base.

### Stage 3 — inspection-kotlin's analyzer tests

Move the 24 test files that drive the prep analyzer (the `prepwork/` test package,
`KotlinAnalyzerSmokeTest`, `TestKotlinPrinter`, `TestKotlinPrinterRoundTrip`) into `maddi-run-kotlin`'s
test source set, which already depends on inspection-kotlin, kotlin-k2 and prepwork. They test that the
Kotlin CST feeds the analyzer faithfully, which is a claim about both tiers, so they belong on the mod
side. Drop the `testImplementation` on prepwork (and cst-print, cst-print-kotlin, inspection-openjdk if
nothing left uses them) from inspection-kotlin.

Gate: the number of tests executed in inspection-kotlin + run-kotlin equals the stage-0 baseline for
those two modules; `check_tiers.py` reports no test-scope edge from base into mod.

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
2. **`maddi-modification` and `maddi-ext` are carved from a fresh clone with `git filter-repo`** and a
   `--path` list per tier (modules, `corpus/` for mod, `dogfood/` for ext, and the docs from §2 that
   belong to that tier). Each new repository holds the full history of its own paths.
3. Each new repository gets: `settings.gradle.kts` with `pluginManagement { includeBuild("../maddi/build-logic") }`
   and `includeBuild("../maddi")` (ext also `includeBuild("../maddi-modification")`), the Gradle wrapper,
   `gradle.properties` with its own `version` (start both at `0.9.1`, the current line), and a README
   that states its tier and the rule from §1. Cross-repository dependencies are written as coordinates,
   `implementation("io.codelaser:maddi-cst-api:$maddiVersion")`, exactly as the refactor side already does
   for maddi; `includeBuild` substitutes them from source.
4. `docs/`: design and roadmap documents about prepwork, link and the analyzer move with the mod
   repository; `docs/README.md` in each repository indexes only what it holds and links the others.
   `PUBLISHING.md` splits: annotations and support (Central) stay here; the plugins and CLI zips go with
   ext, and the note that the plugins shade the analyzer now reads as "ext bundles mod and base".
   `CONTRIBUTING.md`'s customer-name hook is installed in all three.
5. Workspace tooling: the `.ws` `REPOS` lists in `ws/*` and `ALL_REPOS` in jfocus-devops `scripts/ws.conf`
   gain the two repositories; `jfocus-refactor-service/settings.gradle.kts` and the server's replace
   `includeBuild("../maddi")` with maddi + maddi-modification (ext is not on the refactor side's graph).
   Creating the remotes on `laser1` is the user's step; the thread leaves two local repositories under
   `~/git/` with a clean `main`/`devel` and says so.

Gate, run in the aside workspace with all three checkouts side by side:
- `maddi`: `./gradlew build --no-build-cache`; `check_tiers.py` (now trivially base-only).
- `maddi-modification`: `./gradlew test --no-build-cache` and `slowTest` with the corpus present
  (a green `slowTest` with `build/test-results/slowTest` empty is not a result — see `AGENTS.md`).
- `maddi-ext`: `:maddi-ide-daemon:installDist`, the Gradle plugin isolation test, the Maven plugin
  tests, `dogfood`.
- refactor composite: both `compileJava compileTestJava` gates from stage 0, plus the
  `codelaser-metrics-*` tests.
- Test counts per module equal the stage-0 baseline, module for module.

### Stage 6 — the wall (mechanism only, default unchanged)

Add to `maddi-modification` and `maddi-ext` a property switch: `-PmaddiFromSource=false` drops the
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
- Report numbers: tests executed and passed per module before and after; the tier check's output.
- Commit per root cause, with the stage in the subject; the user pushes.
- The customer behind the private corpus is never named (CONTRIBUTING.md; the hook enforces it).
- If a stage's gate cannot be made green without touching something outside its scope, stop that stage,
  write down what was found, and continue with the stages that do not depend on it.

## 5. The contract with the refactor side (not this thread's work)

After this split, the refactor engine's base tier imports from `maddi` only:
cst-api, cst-impl, cst-analysis, cst-io, cst-print, graph, inspection-api, inspection-resource,
inspection-openjdk, inspection-integration, inspection-mixed, java-openjdk, java-parser, java-bytecode,
aapi-archive, run-config, **callgraph**, support, util, annotation, kotlin-realm. It must not import
modification-common, prepwork, link, analyzer, aapi-parser or any `run-*` driver. The refactor-side thread
that follows will: split the `Prepwork` record (base) from the factory that runs `PrepAnalyzer` (behind
an injected provider on the mod side); move `AnalysisResultsCache`, `codelaser-metrics-dataflow`,
`codelaser-metrics-immutable` and the one variable-data use in `codelaser-metrics-duplicate` to the mod
side; and only then take `maddi-modification` off refactor-base's graph.

## 6. Decisions taken by default — say so if you want another

| decision | default in this plan |
|---|---|
| repository names | `maddi`, `maddi-modification`, `maddi-ext` |
| base keeps its SHAs | yes: `git rm` in base, `filter-repo` only for the two new repositories |
| new module for the call graph | `maddi-callgraph`, JPMS `io.codelaser.maddi.callgraph` |
| `recursiveMethod` property | moves to cst-analysis `PropertyImpl` |
| inspection-kotlin analyzer tests | move to `maddi-run-kotlin` |
| shared build logic | `buildSrc` → included build `build-logic/` in `maddi` |
| versions | three `gradle.properties`, all starting at `0.9.1`, bumped independently |
| from-source vs pinned | mechanism built in stage 6, default stays from-source |
| Bazel files | travel with their modules; not a gate |
