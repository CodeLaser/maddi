# Contributing / developer workflow

How to build, test and change maddi without tripping over the things that bite. New here?
Read [`ARCHITECTURE.md`](ARCHITECTURE.md) first for the module map and reading paths.

## Prerequisites

- A recent JDK. The project compiles on the Gradle daemon's JDK (development currently happens
  on JDK 26); the IDE-integration flows in `Taskfile.yml` require JDK 25+. There are no Gradle
  toolchain downloads — what's on your `JAVA_HOME` is what compiles.
- Nothing else for the core build. The IDE front ends additionally use Maven (Eclipse/Tycho)
  and Node/npm (VS Code); `task doctor` reports what is installed.

## Building and testing

```bash
./gradlew build          # compile + fast tests (everything NOT tagged "slow")
./gradlew slowTest       # the large-corpus smoke tests, one fork at a time
./gradlew :maddi-modification-link:test --tests 'TestLinkMethodCall'   # one spec
```

- The fast/slow split lives in `buildSrc/src/main/kotlin/java-library-conventions.gradle.kts`:
  `test` excludes JUnit tag `slow`; `slowTest` runs only that tag and inherits each module's
  own `test` configuration (jvmArgs, heap via `TESTXMX`, system properties). Tag any test that
  parses a real-world corpus with `@Tag("slow")`.
- Slow corpus tests live mainly in `maddi-run-openjdk` (guava, fernflower, langchain4j,
  timefold, jenkins-core, …) and `maddi-modification-analyzer` (`TestCloneBench`).
  `-Dclonebench.parallelism=<n>` tunes the CloneBench fork.
- **The corpus tests need the external `test-oss` corpus** — real open-source projects, cloned
  and built next to this repo. Without it they *skip silently* (JUnit assumptions): a green
  `slowTest` does not mean the proving ground ran. Setup lives in the **maddi-oss** repository
  (`CodeLaser/maddi-oss`): check it out *inside* the corpus root (e.g.
  `~/git/test-oss/maddi-testoss`) and run `task install:wired` to clone + build the projects
  the tests consume, plus `config:*` tasks to generate each project's
  `inputConfiguration.json`. Discovery: `TestOssCorpus` (in `maddi-run-openjdk`'s tests)
  defaults to `../../test-oss`, i.e. a sibling of the maddi checkout; override with
  `-Dtest.oss.root=/path/to/test-oss`. `TestCorpusSweep` sweeps whatever projects are present.
- **Bazel** builds in parallel to Gradle: `bazel build //...`,
  `bazel test //maddi-graph:maddi-graph_test`. Some tests only run under Gradle (relative
  class-file paths); Gradle remains the authoritative build.

## Rules that are easy to violate and expensive to learn

1. **javac is not thread-safe.** All `JavacTask` access must stay single-threaded, and runs use
   `-XDuseUnsharedTable=true`. Before running or debugging anything on the openjdk front end,
   read [`maddi-inspection-openjdk/parsing-stability.md`](maddi-inspection-openjdk/parsing-stability.md).
2. **`TypeInfo`/`MethodInfo`/`FieldInfo` are single-instance** per (FQN, source set): compare
   with `==`, never `equals`.
3. **Properties are write-once.** Absence means *undecided*, which is first-class state — the
   iterating analyzer revisits. Only the guarded `TolerantWrite` may refine monotonically.
4. **The golden rule for engine changes**: performance or engine-structure changes are accepted
   only with a byte-identical `FPDUMP` A/B comparison on the proving-ground corpora (modulo the
   documented constructor non-confluence). Speed never buys verdict changes.
5. **Concept changes must land in the docs.** If you change what the analyzer *means* (levels,
   link natures, convergence rules), update `road-to-immutability/llm-summary.md` (and the book
   chapter if affected) in the same change.

## Diagnostics and gates

The engine reads opt-out/diagnostic gates from the environment (via `Gate`), including:
`NOWORKLIST` (disable the dirty-element worklist), `PARALLEL` (parallel iteration),
`NOCYCLEBREAKING`, `FPDUMP=<file>` (per-element verdict dump — the basis of A/B comparisons),
`MLTRACE` (link-engine tracing), plus link-module-specific gates. See llm-summary §Convergence
and `maddi-modification-link/linking-manual.md` for context.

## Test conventions

- **Spec-by-example**: the link engine's behavior is specified by `TestLinkMethodCall` and
  friends — extend those rather than writing ad-hoc assertions elsewhere.
- Keep test files focused and grouped by nature (one theme per file); don't grow grab-bag test
  classes.
- The proving-ground corpora (timefold, langchain4j, fernflower, guava, activemq, jenkins-core,
  camel-core) are kept certified and crash-free; a change that breaks certification is not done.
  They come from the `test-oss` corpus above — make sure it is installed before trusting a
  corpus run.

## IDE front-end development

The **Eclipse plugin** (Maven/Tycho) and **VS Code extension** (TypeScript/npm) live outside
the Gradle build; [`Taskfile.yml`](Taskfile.yml) drives them (`brew install go-task`, then
`task` to list everything):

```bash
task doctor         # report the toolchain: JDK, Maven, Node/npm, VS Code (+redhat.java), Eclipse
task install:tools  # install what's missing (Homebrew)
task gradle-ide     # shared artifacts: platform + ide-client to ~/.m2, daemon installDist
task eclipse:build  # Tycho build → p2 update site
task vscode:run     # launch an Extension Development Host with the extension loaded
task vscode:package # produce the .vsix (compiled extension + bundled daemon)
```

The **IntelliJ plugin** is not in the Taskfile — it is a regular Gradle module using the
IntelliJ Platform plugin: `./gradlew :maddi-intellij:runIde` starts a sandboxed IDE with the
plugin and the freshly built daemon.

Status and open items: `docs/eclipse-plugin-state.md`, `docs/ide-todo.md`.

## Releasing

Publishing is **not yet active**. The strategy, the wiring completed so far, and the Maven
Central mechanics (GPG/JReleaser, as an appendix) are recorded in
[`PUBLISHING.md`](PUBLISHING.md); `release-cli.sh` builds and uploads the CLI distribution
zips when the time comes.

## Documentation conventions

- Maintained references live next to what they document (module `README.md`s, the linking
  manual, `parsing-stability.md`) or at the root (`README.md`, `ARCHITECTURE.md`, this file).
- Cross-module working notes, plans and investigation reports go in `docs/` — add new ones to
  the index in [`docs/README.md`](docs/README.md) with a status label.
- Module-local bug notes go in the module's `notes/` directory.
- `AGENTS.md` and `CLAUDE.md` carry the LLM-assistant entry points; keep them short and
  pointer-based (they should link to references, not duplicate them).
