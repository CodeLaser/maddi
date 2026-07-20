# maddi

_maddi_ stands for Modification Analyzer for Duplication Detection and Immutability.
It is a re-implementation of [e2immu](https://www.e2immu.org), an open source project which started in 2020 but has now been archived.

_maddi_ is a whole-program static analyzer for Java (and Kotlin, via a shared syntax tree): it
computes **modification**, **independence** and **immutability** properties of your types,
methods and fields, and reports them as annotations such as `@Immutable`, `@Container`,
`@Modified` and `@Independent`.

_maddi_ is and will remain open source. Please contact [Bart Naudts](mailto:bart.naudts@codelaser.io) at [CodeLaser](https://codelaser.io) for any information.

## Start here

| You want to… | Read |
|---|---|
| Understand the concepts (immutability levels, modification, linking) | [`road-to-immutability/llm-summary.md`](road-to-immutability/llm-summary.md), the maintained digest of the *Road to Immutability* book (AsciiDoc sources in [`road-to-immutability/`](road-to-immutability/src/docs/asciidoc/), published rendering [here](https://www.e2immu.org/docs/road-to-immutability.html)) |
| Run maddi on your own project (plugins, CLI, configuration) | the user manual: [`maddi-manual/src/docs/asciidoc/`](maddi-manual/src/docs/asciidoc/) (build HTML/PDF with `./gradlew :maddi-manual:buildDocs`) |
| Understand the codebase (pipeline, ~40 modules, where to start reading) | [`ARCHITECTURE.md`](ARCHITECTURE.md) |
| Build, test, contribute | [`CONTRIBUTING.md`](CONTRIBUTING.md) |
| Work on it with an AI assistant | [`AGENTS.md`](AGENTS.md) / [`CLAUDE.md`](CLAUDE.md) |

Cross-module design notes and plans are indexed in [`docs/README.md`](docs/README.md).
The future publishing strategy (not yet active) is recorded in [`PUBLISHING.md`](PUBLISHING.md).

## Current status

_maddi_ is still under development, and, as of October 2025, will receive plenty of attention with the goal of being production ready in 2026.

- concepts: stable
- parser, resolver: has been tested on a number of open source projects, and one closed source 3M lines of code project. Not without errors, but pretty robust.
- modification analysis: has only been tested on one larger test set. Not ready for general use.
- plugins: Maven and Gradle plugins should work but have not received any attention in the last half year, since _maddi_ is mostly being run from [CodeLaser](https://codelaser.io)'s Refactor engine.

## Building

_maddi_ builds with a recent JDK on your `JAVA_HOME` (development happens on JDK 26; there is no
Gradle toolchain provisioning). Both Gradle and Bazel are supported:

```bash
./gradlew build       # compile + fast tests
./gradlew slowTest    # large-corpus smoke tests (tagged @Tag("slow"))
bazel build //...     # Bazel build; test one module: bazel test //maddi-graph:maddi-graph_test
```

Notes:

- the Maven plugin is, naturally, built with Maven; the Gradle plugin only with Gradle
- quite a few tests do not run in Bazel, because they expect to find class files in some relative location. This needs to be fixed at some point.
- The Bazel build system has been added to test CodeLaser's Refactor input configuration construction system.

> Running tests (or any tool) on top of the openjdk parser? Read
> [`maddi-inspection-openjdk/parsing-stability.md`](maddi-inspection-openjdk/parsing-stability.md)
> — the authoritative guide to stable, deterministic runs (the `tree.starImportScope is null`
> flake, the built-in `-XDuseUnsharedTable=true` fix, and why parallel test forks are safe).

More detail in [`CONTRIBUTING.md`](CONTRIBUTING.md).

___

(C) Copyright Bart Naudts, 2020-2025.
