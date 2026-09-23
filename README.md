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
> those elements are of a type parameter the `Config` class cannot see into. Not hc-free `@Immutable`, 
> but immutable from `Config`'s point of view.

That distinction is the point of the project. A tool that answers "not immutable" for both versions
has told you nothing about the difference between them.

The four levels are `@Mutable` → `@FinalFields` → `@Immutable(hc=true)` → `@Immutable`, plus a
separate independence axis and **eventual immutability** for the builder/freeze pattern
(`@Mark`, `@Only`, `@Immutable(after=…)`) — types that are mutable while being constructed and
immutable ever after. The concepts are developed in full in the book,
[*The Road to Immutability*](https://www.codelaser.io/maddi/road/); the
[condensed digest](road-to-immutability/llm-summary.md) is the fastest way in.

## Install

Since `0.9.1` there is one analysis engine and three ways to run it: a Gradle plugin, a Maven
plugin, and a self-contained command line. All three need a recent JDK (the analyzer runs on
current Java; development happens on JDK 26).

**Gradle** — from the
[Gradle Plugin Portal](https://plugins.gradle.org/plugin/io.codelaser.maddi.analyzer):

```kotlin
plugins {
    java
    id("io.codelaser.maddi.analyzer") version "0.9.1"
}

maddi {
    analysisSteps = "modification"   // parse -> prep -> modification -> write results
}
```

```bash
./gradlew maddi-analyzer     # one result .json per package, under build/maddi
```

The plugin derives the source sets and classpath from the Gradle model and forks a worker JVM with
the flags the analyzer needs — nothing else to configure.

**Maven** — `io.codelaser:maddi-mvnplugin` on Maven Central, goal prefix `maddi`, five goals
(`run`, `write-input-configuration`, `statistics`, `write-analysis-hints`,
`compile-analysis-hints`). The JVM running Maven needs the javac `--add-exports` flags for the
analysis goals; set them once in `.mvn/jvm.config` or `MAVEN_OPTS` — the
[user manual](https://www.codelaser.io/maddi/manual/) lists them.

**Command line, for any build system** — two self-contained zips on the
[latest release](https://github.com/CodeLaser/maddi/releases/latest):

- `maddi-<version>.zip` — the Java analyzer, launcher `bin/maddi`
- `maddi-kotlin-<version>.zip` — Java **and** Kotlin, launcher `bin/maddi-kotlin`

`bin/maddi-kotlin` takes the **same command line** as `bin/maddi` and is a strict superset of it:
given a project with no `.kt` file it runs the Java analyzer and behaves identically, and given
one with Kotlin it runs both front ends over a shared core. Take the small zip for a Java-only
build (85 MB of K2 compiler jars is the price of the Kotlin one); take the Kotlin zip for
anything mixed, and use one tool. An option the mixed pipeline cannot honour yet — writing
analysis results, incremental analysis, the hints compiler, all of which need a Kotlin codec
round trip — is **refused by name**, never run as a no-op.

Unpack and run: every jar rides along in `lib/`, and the required JVM flags are baked into the
launcher. Kotlin support ships only this way — it depends on JetBrains K2 artifacts that are not
on Maven Central, so it cannot be a resolvable library.

To point the CLI at your project, capture what the build actually compiled and hand it to maddi —
no build-tool integration needed:

```bash
./gradlew :your-module:compileJava --debug 2>&1 | grep 'Compiler arguments:' > build.log
maddi --compile-log build.log --analysis-steps modification --analysis-results-dir out
```

More worked examples, including two bundled Maven build logs you can run without checking the
projects out: [`maddi-run-openjdk/running-examples.md`](maddi-run-openjdk/running-examples.md).
Configuration, analysis hints, error reporting and exit codes are covered in the
[user manual](https://www.codelaser.io/maddi/manual/).

## The annotations

The one thing your own code compiles against is the annotations library. It is on Maven Central,
targets Java 17, and is **Apache-2.0** licensed — you can depend on it without taking on the
analyzer's LGPL:

```kotlin
implementation("io.codelaser:maddi-annotation:0.9.1")   // Gradle
```

```xml
<dependency>                                            <!-- Maven -->
  <groupId>io.codelaser</groupId>
  <artifactId>maddi-annotation</artifactId>
  <version>0.9.1</version>
</dependency>
```

That gives you `@Immutable`, `@Container`, `@Independent`, `@Modified` and friends, with **no
dependencies at all** — useful as documentation and as contracts on your interfaces even before you
run the analyzer, since maddi verifies them against what it computes.

If you also want the "eventually final" support classes (`SetOnce`, `Freezable`, `EventuallyFinal`,
`Lazy`, `FirstThen`), depend on `io.codelaser:maddi-annotation`'s companion instead:

```kotlin
implementation("io.codelaser:maddi-support:0.9.1")      // annotations arrive with it
```

`maddi-support` declares exactly one dependency, on `maddi-annotation`, and re-exports it — so if you
were already using `maddi-support` before the two were split, nothing changes for you.

> Versions up to `0.8.2` were LGPL-3.0; `0.9.0` onward is Apache-2.0.

## Status — August 2026

`0.9.1` is the first release where everything above is installable: the Gradle plugin on the
Plugin Portal, the Maven plugin and the annotations on Maven Central, the CLI distributions on
GitHub Releases. Where each part stands:

| Part | State |
|---|---|
| Concepts, and the book | Stable |
| Parser / resolver (javac front end) | Robust; exercised on many open-source projects and one closed-source 3M-line codebase |
| Modification & immutability analysis | Runs to a certified fixpoint on a proving-ground corpus (Timefold, LangChain4j, Fernflower, Guava, ActiveMQ, Jenkins, Camel) |
| Gradle / Maven plugins | Published in 0.9.1; both validated against a corpus of real multi-module builds |
| Kotlin front end | Works; ships only via the `maddi-kotlin` CLI distribution, which takes the same command line as `maddi` |

The engine is stable on everything we run it on — and your codebase will contain Java the corpus
does not. If maddi mis-parses, crashes, or computes something you can argue is wrong, that is
exactly the report we want: please open an issue.

## Documentation

| You want to… | Read |
|---|---|
| Understand the concepts (levels, modification, linking, independence) | [*The Road to Immutability*](https://www.codelaser.io/maddi/road/); in-repo: the [condensed digest](road-to-immutability/llm-summary.md) and the [book sources](road-to-immutability/src/docs/asciidoc/) |
| Run maddi on your own project | the [user manual](https://www.codelaser.io/maddi/manual/) ([sources](maddi-manual/src/docs/asciidoc/)) |
| Understand the codebase (pipeline, ~40 modules, where to start) | [`ARCHITECTURE.md`](ARCHITECTURE.md) |
| Build, test, contribute | [`CONTRIBUTING.md`](CONTRIBUTING.md) |
| Work on it with an AI assistant | [`AGENTS.md`](AGENTS.md) / [`CLAUDE.md`](CLAUDE.md) |

Cross-module design notes and plans are indexed in [`docs/README.md`](docs/README.md).

## Building from source

```bash
git clone https://github.com/CodeLaser/maddi.git && cd maddi
./gradlew build                                  # compile + fast tests
```

Requires a recent JDK on `JAVA_HOME` (no Gradle toolchain provisioning). A self-contained first
run — maddi analyzing its own CST API against `java.base`:

```bash
./gradlew :maddi-run-openjdk:run --args="\
    --jmod=java.base \
    --source=$PWD/maddi-cst-api/src/main/java \
    --analysis-steps=prep"
```

> Embedding the openjdk front end in your own code? Read
> [`maddi-inspection-openjdk/parsing-stability.md`](maddi-inspection-openjdk/parsing-stability.md)
> first — javac is not thread-safe, and that document is the authoritative guide to deterministic
> runs.

## Background

maddi re-implements [e2immu](https://www.e2immu.org), which ran from 2020 until it was archived.
The root Java package was `org.e2immu.*`, after the predecessor; it became
`io.codelaser.maddi.*` in 0.9.1 — see [`docs/release-notes-0.9.1.md`](docs/release-notes-0.9.1.md).

maddi is developed by [Bart Naudts](mailto:bart.naudts@codelaser.io) at
[CodeLaser](https://codelaser.io), and is and will remain open source. The **analyzer** is
LGPL-3.0. The **annotations** (`maddi-annotation`, with `maddi-support`) — the only artifacts your
own code compiles against — are **Apache-2.0** from 0.9.0 onward, so depending on them carries no
obligation. CodeLaser's commercial Refactor product is built on this engine; the engine stays here,
under this licence. Questions, use cases and criticism are all welcome — mail, or open an issue.

___

(C) Copyright Bart Naudts, 2020-2026.
