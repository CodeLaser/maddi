# maddi

[![build](https://github.com/CodeLaser/maddi/actions/workflows/build.yml/badge.svg)](https://github.com/CodeLaser/maddi/actions/workflows/build.yml)

**maddi works out what is actually immutable in your Java codebase — and tells you why it isn't.**

It is a whole-program static analyzer for Java (and Kotlin, via a shared syntax tree). You do not
write annotations; maddi *computes* them. It reads your sources and classpath, follows how objects
flow between fields, parameters and return values, and derives `@Immutable`, `@Container`,
`@Modified` and `@Independent` for every type, method and field.

## Why not just "immutable or not"

Real code is rarely deeply immutable, and a yes/no verdict throws away everything useful. maddi
computes a **level**, so it can tell you how far you got and what stopped you.

```java
public final class Config {
    private final Map<String, String> settings;

    public Config(Map<String, String> settings) {
        this.settings = settings;          // (1)
    }

    public String get(String key) {
        return settings.get(key);
    }
}
```

> `@FinalFields @Container` — the field is final and nothing here modifies it, but the caller still
> holds the map it passed in. maddi sees the field and the parameter sharing one *modification
> component*, so anyone outside can still change this object's state after it is built.

Change line (1) to `Map.copyOf(settings)` and the verdict moves up a level:

> `@Immutable(hc=true) @Container` — after the copy the only link left runs to the map's
> *elements*, not to the map itself. `hc=true` ("hidden content") records the one honest caveat:
> those elements are of a type parameter maddi cannot see into. Not hc-free `@Immutable`, and it
> should not pretend otherwise.

That distinction is the point of the project. A tool that answers "not immutable" for both versions
has told you nothing about the difference between them.

The four levels are `@Mutable` → `@FinalFields` → `@Immutable(hc=true)` → `@Immutable`, plus a
separate independence axis and **eventual immutability** for the builder/freeze pattern
(`@Mark`, `@Only`, `@Immutable(after=…)`) — types that are mutable while being constructed and
immutable ever after. The concepts are developed in full in
[*The Road to Immutability*](road-to-immutability/src/docs/asciidoc/); the
[condensed digest](road-to-immutability/llm-summary.md) is the fastest way in.

## Status — July 2026

maddi is **not yet production ready**, and this section is kept honest deliberately.

| Part | State |
|---|---|
| Concepts, and the book | Stable |
| Parser / resolver (javac front end) | Robust; exercised on many open-source projects and one closed-source 3M-line codebase |
| Modification & immutability analysis | Runs to a certified fixpoint on a proving-ground corpus (Timefold, LangChain4j, Fernflower, Guava, ActiveMQ, Jenkins, Camel). Not yet ready for general use |
| Kotlin front end | Works; ships only via the mixed CLI |
| Gradle / Maven plugins | Functional, but have had little attention recently |
| Releases | None yet — build from source (below). See [`PUBLISHING.md`](PUBLISHING.md) |

If you run it on your own code today, expect rough edges. Issues and questions are very welcome.

## Try it

Requires a recent JDK on `JAVA_HOME` (development happens on JDK 26; no Gradle toolchain
provisioning). No release artifacts yet, so build first:

```bash
git clone https://github.com/CodeLaser/maddi.git && cd maddi
./gradlew build                                  # compile + fast tests
```

Then analyze something self-contained — maddi's own CST API, against `java.base`:

```bash
./gradlew :maddi-run-openjdk:run --args="\
    --jmod=java.base \
    --source=$PWD/maddi-cst-api/src/main/java \
    --analysis-steps=prep"
```

To point it at *your* project, capture what the build actually compiled and hand that to maddi —
no build-tool integration needed:

```bash
./gradlew :your-module:compileJava --debug 2>&1 | grep 'Compiler arguments:' > build.log
maddi --compile-log build.log --analysis-steps modification --analysis-results-dir out
```

More worked examples, including two bundled Maven build logs you can run without checking the
projects out: [`maddi-run-openjdk/running-examples.md`](maddi-run-openjdk/running-examples.md).
Gradle and Maven plugins, configuration and exit codes are covered in the user manual
(`./gradlew :maddi-manual:buildDocs`).

> Running anything on the openjdk front end? Read
> [`maddi-inspection-openjdk/parsing-stability.md`](maddi-inspection-openjdk/parsing-stability.md)
> first — javac is not thread-safe, and that document is the authoritative guide to deterministic
> runs.

## Documentation

| You want to… | Read |
|---|---|
| Understand the concepts (levels, modification, linking, independence) | [`road-to-immutability/llm-summary.md`](road-to-immutability/llm-summary.md), then the [book](road-to-immutability/src/docs/asciidoc/) |
| Run maddi on your own project | the user manual, [`maddi-manual/src/docs/asciidoc/`](maddi-manual/src/docs/asciidoc/) |
| Understand the codebase (pipeline, ~40 modules, where to start) | [`ARCHITECTURE.md`](ARCHITECTURE.md) |
| Build, test, contribute | [`CONTRIBUTING.md`](CONTRIBUTING.md) |
| Work on it with an AI assistant | [`AGENTS.md`](AGENTS.md) / [`CLAUDE.md`](CLAUDE.md) |

Cross-module design notes and plans are indexed in [`docs/README.md`](docs/README.md).

## Background

maddi re-implements [e2immu](https://www.e2immu.org), which ran from 2020 until it was archived.
The root Java package is still `org.e2immu.*`, after the predecessor.

maddi is developed by [Bart Naudts](mailto:bart.naudts@codelaser.io) at
[CodeLaser](https://codelaser.io), and is and will remain open source. The **analyzer** is
LGPL-3.0. The **annotations** (`maddi-support`) — the only artifact your own code compiles
against — will ship under a permissive licence, so that depending on them carries no obligation.
CodeLaser's commercial Refactor product is built on this engine; the engine stays here, under
this licence. Questions, use cases and criticism are all welcome — mail, or open an issue.

___

(C) Copyright Bart Naudts, 2020-2026.
