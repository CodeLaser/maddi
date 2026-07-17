# Running maddi on real projects (openjdk runner)

Worked examples for the openjdk CLI (`org.e2immu.analyzer.run.openjdkmain.Main`) on parts of the
JDK, on [Timefold Solver](https://github.com/TimefoldAI/timefold-solver), and on
[LangChain4j](https://github.com/langchain4j/langchain4j). For the full option reference see the
manual's command-line section (`maddi-manual/.../050-command-line.adoc`).

Each example is given twice: once via the Gradle `application` plugin, once as a **bazel-initiated
run**. Both launchers already apply the javac `--add-exports` the openjdk front-end needs, and both
require **JDK 26** (see `../.bazelrc` and the module `build.gradle.kts`).

> `bazel run` executes from a runfiles directory, not the repo root, so pass **absolute** paths to
> logs and sources (the `$PWD/…` below assumes you launch from the repo root).

## Two bundled Maven build logs

`src/test/resources/javac/` ships compressed Maven `-X` logs for two large open-source projects:
`mvnTimefold-solver.txt.gz` and `mvnLangchain4j.txt.gz`. `--compile-log` derives maddi's input
configuration (source sets + classpath) straight from such a log — no build-tool integration needed.
Deriving the configuration works anywhere; a *full analysis* additionally needs the project's sources
checked out at the paths recorded in the log.

## Timefold Solver — derive the input configuration from the bundled log

Gradle:

```console
$ ./gradlew :maddi-run-openjdk:run --args="\
    --compile-log $PWD/maddi-run-openjdk/src/test/resources/javac/mvnTimefold-solver.txt.gz \
    --extra-jmod java.sql \
    --write-input-configuration /tmp/timefold-ic.json"
```

Bazel:

```console
$ bazel run //maddi-run-openjdk:maddi -- \
    --compile-log $PWD/maddi-run-openjdk/src/test/resources/javac/mvnTimefold-solver.txt.gz \
    --extra-jmod java.sql \
    --write-input-configuration /tmp/timefold-ic.json
```

`--write-input-configuration` is terminal (write and exit, no analysis). Drop it and add
`--analysis-steps prep --analysis-results-dir /tmp/timefold-out` to run the prep analysis instead —
that needs the Timefold checkout present at the paths in the log.

## LangChain4j — same, from its bundled log

Bazel:

```console
$ bazel run //maddi-run-openjdk:maddi -- \
    --compile-log $PWD/maddi-run-openjdk/src/test/resources/javac/mvnLangchain4j.txt.gz \
    --write-input-configuration /tmp/lc4j-ic.json
```

Gradle is the same call via `./gradlew :maddi-run-openjdk:run --args="…"`.

## Parts of the JDK — java.base on the module path

To analyze sources against a JDK module directly (no build log), put the module on the path with
`--jmod` and point `--source` at the code. This is the shape exercised by `TestRunAnalyzer`:

```console
$ bazel run //maddi-run-openjdk:maddi -- \
    --jmod=java.base \
    --classpath=$PWD/maddi-support/build/libs/maddi-support-<version>.jar \
    --source=$PWD/maddi-cst-api/src/main/java \
    --analysis-steps=prep
```

`--classpath=jmod:java.base` (java.base on the *classpath* rather than as a module) is the
encapsulation-ignoring variant used when composing annotated APIs for JDK packages; see
`module-vs-classpath jdk.internal loading.md` in `maddi-inspection-openjdk` for why the two views
differ.
