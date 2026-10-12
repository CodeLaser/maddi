# Running maddi on real projects (openjdk runner)

Worked examples for the openjdk CLI (`io.codelaser.maddi.run.openjdkmain.Main`) on parts of the
JDK, on [Timefold Solver](https://github.com/TimefoldAI/timefold-solver), and on
[LangChain4j](https://github.com/langchain4j/langchain4j). For the full option reference see the
manual's command-line section (`maddi-manual/.../050-command-line.adoc`).

The CLI lives in the `maddi-dist` repository since the split: run the examples there with
`./gradlew :maddi-cli:run --args="…"`, or with an installed `maddi`. The launcher applies the javac
`--add-exports` the openjdk front-end needs; it requires **JDK 27**. Pass **absolute** paths to logs and
sources (the `$MADDI/…` below is this repository's root).

## Two bundled Maven build logs

`src/test/resources/javac/` ships compressed Maven `-X` logs for two large open-source projects:
`mvnTimefold-solver.txt.gz` and `mvnLangchain4j.txt.gz`. `--compile-log` derives maddi's input
configuration (source sets + classpath) straight from such a log — no build-tool integration needed.
Deriving the configuration works anywhere; a *full analysis* additionally needs the project's sources
checked out at the paths recorded in the log.

## Timefold Solver — derive the input configuration from the bundled log

```console
$ ./gradlew :maddi-cli:run --args="\
    --compile-log $MADDI/maddi-run-openjdk/src/test/resources/javac/mvnTimefold-solver.txt.gz \
    --extra-jmod java.sql \
    --write-input-configuration /tmp/timefold-ic.json"
```

`--write-input-configuration` is terminal (write and exit, no analysis). Drop it and add
`--analysis-steps prep --analysis-results-dir /tmp/timefold-out` to run the prep analysis instead —
that needs the Timefold checkout present at the paths in the log.

## LangChain4j — same, from its bundled log

```console
$ maddi --compile-log $MADDI/maddi-run-openjdk/src/test/resources/javac/mvnLangchain4j.txt.gz \
    --write-input-configuration /tmp/lc4j-ic.json
```

## Parts of the JDK — java.base on the module path

To analyze sources against a JDK module directly (no build log), put the module on the path with
`--jmod` and point `--source` at the code. This is the shape exercised by `TestRunAnalyzer`:

```console
$ maddi --jmod=java.base \
    --classpath=$MADDI/maddi-support/build/libs/maddi-support-<version>.jar \
    --source=$MADDI/maddi-cst-api/src/main/java \
    --analysis-steps=prep
```

`--classpath=jmod:java.base` (java.base on the *classpath* rather than as a module) is the
encapsulation-ignoring variant used when composing annotated APIs for JDK packages; see
`module-vs-classpath jdk.internal loading.md` in `maddi-inspection-openjdk` for why the two views
differ.

## This project itself — input configuration from the Bazel build

Bazel is a fourth way (alongside Gradle, Maven, and debug logs) to obtain an input
configuration. This repository's Bazel build is a proof of concept over the base modules only (Gradle is the
build); it is enough to show the route. Rather than a build log, we read Bazel's *action graph* and emit an
`InputConfiguration` JSON directly, with `bazel_inputconfiguration.py` (in this module):

```console
$ bazel build //...                                                    # jars + actions must exist
$ bazel aquery 'mnemonic("Javac", kind("java_library", //...))' \
      --output=jsonproto > /tmp/aquery.json
$ python3 maddi-run-openjdk/bazel_inputconfiguration.py /tmp/aquery.json /tmp/maddi-ic.json
$ maddi --input-configuration /tmp/maddi-ic.json --analysis-steps prep \
      --analysis-results-dir /tmp/maddi-out
```

Each `java_library` becomes a source set (its `src/main/java`); inter-module and external
Maven dependencies are supplied as the **compiled jars Bazel built** — that is how maddi
resolves types across modules (a Gradle/Maven-derived config is shaped the same way, with
jar dependencies rather than co-parsed source). The `java.se` jmod closure is added so the
JDK is on the classpath.

Notes:

* The generated JSON embeds absolute exec-root / workspace / JDK paths, so it is
  machine-specific — **regenerate it, don't commit it**.
* `maddi-java-openjdk` and `maddi-inspection-openjdk` reach into `com.sun.tools.javac.*`
  internals and cannot be parsed *from source* (the encapsulated-package limitation in
  `module-vs-classpath jdk.internal loading.md`); they are carried as compiled jars, like any
  library. With that, prep analysis of the remaining 25 modules runs clean.
