# Building source sets from Kotlin compiler debug output

This module (`maddi-run-kotlin`) is the Kotlin sibling of `maddi-run-openjdk/.../javac`. It reconstructs a
maddi `InputConfiguration` (source sets + their dependency graph + library classpath) from the **kotlinc**
command lines a build tool emits when run with debug logging — exactly as the javac side does for `javac`.

The output type is the shared, language-agnostic `InputConfiguration` (`maddi-inspection-resource`), so the CST
front-ends consume it uniformly.

## 1. What the javac side does (recap)

`maddi-run-openjdk/.../javac`, four stages:

1. `MakeJavacList` — runs a clean + a debug build, greps stdout for compiler-argument lines.
2. `Javac` (record + `parse`) — tokenizes one `javac` command line into a typed record.
3. `ParseJavacList` — reads a log (plain / `.gz` / json-list), matches the javac/Gradle/Maven marker regexes,
   maps each match to a `Javac`, and wraps the result in an `InputConfiguration` (adding the `java.se` jmod
   closure as JDK libraries).
4. `JavacListToSourceSets.compute` — the intelligence: `List<Javac>` → source sets + jars. The key idea is
   **output-identity linking**: each module's `-d` destination is its identity; another module whose
   `-classpath` contains that destination gets a **dependency edge**. Naming comes from destination path-suffix
   frequency stats + `/main`|`/test`; source directories come from `-sourcepath` or are inferred from the
   source files' `package` declarations.

## 2. Generalization: one linking engine, two front-ends

The linking algorithm is entirely language-independent, so it now lives in `maddi-run-config` and is shared:

- **`org.e2immu.analyzer.run.config.compile.CompileInvocation`** — the accessor surface the engine needs:
  `destination`, `classpath`, `modulePath`, `sourcePath`, `sourceFiles`, `encoding`, plus two optional signals
  that Kotlin provides and Java does not: `moduleName()` and `friendPaths()` (Java returns `null` / empty).
- **`org.e2immu.analyzer.run.config.compile.CompileListToSourceSets`** — the former `JavacListToSourceSets`,
  generalized to `List<? extends CompileInvocation>`. Two Kotlin-aware additions, both no-ops for Java:
  - **friend-paths as dependency edges** — `-Xfriend-paths=<main-out>` on a test compile is resolved (via the
    same output-identity map) to the main source set and added as a dependency; its presence also marks the set
    as `test`.
  - **package regex accepts Kotlin** — the source-dir inference regex now treats the trailing `;` as optional,
    so a semicolon-less Kotlin `package a.b.c` yields a source-root prefix just like Java's `package a.b.c;`.
- `JavacListToSourceSets` is now an empty subclass of `CompileListToSourceSets` (keeps `Javac`'s callers and
  tests — which reference `JavacListToSourceSets.Result` / `.JSourceSet` / `.compute` — compiling unchanged).

`JSourceSet`'s payload field is renamed `javac` → `invocation` (type `CompileInvocation`); no caller used it.

## 3. What is different about kotlinc (validated against a live build)

Captured from a real Gradle 9.5.1 / Kotlin build (`--debug`), the marker and layout are:

```
… [DEBUG] [org.gradle.api.Task] …: Kotlin compiler args: -jvm-target 17 -module-name maddi-x_main \
   -jdk-home /…/jdk -no-stdlib -no-reflect -classpath /…/a.jar:/…/b.jar -api-version 2.0 \
   -language-version 2.0 -d /…/maddi-x/build/classes/kotlin/main \
   /…/maddi-x/src/main/kotlin/a/b/Foo.kt /…/maddi-x/src/main/kotlin/a/b/Bar.kt
```

| Concern | javac | kotlinc | Handling |
|---|---|---|---|
| Gradle marker | `…Compiler arguments: …` (one line) | `…Kotlin compiler args: …` (one line) | new regex |
| Maven marker | `[DEBUG] (-d …)` (one line) | **labelled multi-line block** (see §5) | block accumulator |
| Destination | dir | dir **or `.jar`** | `-d`; jar path is a valid identity key |
| Module name | — | `-module-name <proj>_main` | `moduleName()` (retained; not yet used for naming) |
| Classpath | `-classpath` | `-classpath` / `-cp` | same |
| Versions | `-source/-target/-release` | `-jvm-target`, `-api-version`, `-language-version` | parsed, not needed by the engine |
| JDK | implicit | `-jdk-home <path>`, `-no-jdk` | `jdkHome()` retained |
| main↔test | classpath + dir heuristic | **`-Xfriend-paths=<main-out>`** | dependency edge + `test` flag |
| Sources | `.java` + `-sourcepath` | bare `.kt` **and** `.java` args, no `-sourcepath`; sometimes `@argfile` | argfile expansion; dirs inferred from packages |
| stdlib | — | stdlib jar on `-classpath`, `-no-stdlib` | treated as a library jar |
| plugins | annotation procs | `-Xplugin=`, `-P plugin:…`, `-Xjsr305=` | ignored |

## 4. Kotlin components (this module)

- **`Kotlinc`** (record + `parse(line)`) — the kotlinc tokenizer, `CompileInvocation` implementation. Collects
  `.kt`/`.java` source files; recognizes `-d`, `-classpath`/`-cp`, `-module-name`, `-jvm-target`,
  `-api-version`, `-language-version`, `-jdk-home`, `-Xfriend-paths=`; ignores `-Xplugin=`, `-P`, `-Xjsr305=`,
  `-jvm-default`, and the param-less `-no-stdlib`/`-no-reflect`/`-no-jdk`/`-java-parameters`/`-verbose`/
  `-progressive`/`-Xallow-no-source-files`/…; expands `@argfile` tokens. `sourcePath()`/`modulePath()` are
  empty (kotlinc has neither).
- **`ParseKotlincList`** — mirrors `ParseJavacList`: reads a log (plain/`.gz`/json-list), matches the Kotlin
  markers, maps each to a `Kotlinc`, and builds an `InputConfiguration` via the shared
  `CompileListToSourceSets` + the `java.se` jmod closure. `@argfile` is resolved relative to the log's dir.

## 5. Markers

Both the Gradle and Maven formats are **validated against live captures** (Gradle 9.5.1 + Kotlin 2.1.0
`--debug`; `mvn -X` + kotlin-maven-plugin 2.1.0 on JDK 21).

**Gradle / raw CLI — one line per invocation:**
```
GRADLE : .*Kotlin compiler args:\s*(.+)      # org.jetbrains.kotlin.gradle plugin, --debug
KOTLINC: kotlinc(?:-jvm)?\s+(.+)             # raw CLI
```

**Maven — a labelled multi-line block per compile execution** (the plugin does *not* emit a command line; the
`… with arguments:` line is followed by a useless reflective object dump — ignore it). One block:
```
[DEBUG] Compiling Kotlin sources from [<srcDir1>, <srcDir2>]     # opens a block; source DIRECTORIES
[DEBUG] Classpath: <cp1>:<cp2>:…                                 # ':'-separated
[DEBUG] Classes directory is <dest>                             # the -d equivalent
[DEBUG] Module name is <name>                                   # closes the block
```
`ParseKotlincList.parseLines` accumulates a block from `Compiling Kotlin sources from […]` to `Module name is
…`. Two consequences, both handled by the existing engine:
- **Maven has no `-Xfriend-paths`** — the main↔test link is the test's `Classpath:` containing the main output
  (`target/classes`), i.e. plain output-identity linking (as for javac).
- **Maven gives source directories** (not files), mapped to `Kotlinc.sourcePath()` — no package inference is
  needed there (unlike Gradle, whose bare source-file args do need it).

## 6. Phasing

- **Phase 0** — capture real logs, pin markers/layout. *Done for both Gradle and Maven (live captures).*
- **Phase 1** — `Kotlinc.parse` (+ argfile), unit-tested against captured-shape lines.
- **Phase 2** — generalize the engine to `CompileInvocation`; `Javac` ported onto it; `TestJavac*` stay green.
- **Phase 3** — `maddi-run-kotlin` + `ParseKotlincList` → `InputConfiguration`; fixture tests.
- **Phase 4 (done)** — a K2 driver that consumes an `InputConfiguration`: `KotlinInspector.parseFromConfiguration`
  (maddi-inspection-kotlin) extracts source directories + library jars + dependency order and delegates to
  `KotlinProjectScan` (maddi-kotlin-k2), which builds ONE standalone K2 session with a `KaSourceModule` per
  source set (wired to the JDK, the library classpath, and its upstream source-set modules) and drives
  `KotlinScan.convert` per set in dependency order over one shared `InfoByFqn`. The analogue of how the openjdk
  `JavaInspector` consumes the config. Test: `TestKotlinInspectorFromConfiguration` (two on-disk modules, B→A,
  `assertSame` across the boundary). The session builder lives in maddi-kotlin-k2 because the K2 Analysis API
  (`*-for-ide`) is visible only there; the driver one module up passes plain `SourceSet`/`Path` inputs.
- **Phase 5 (done, parse side)** — `ParseMixedList` (maddi-run-kotlin, +dep on maddi-run-openjdk) reads one
  build log containing **both** `javac` and `kotlinc` invocations and feeds the combined
  `List<CompileInvocation>` to the single `CompileListToSourceSets`, so Java and Kotlin source sets link by
  output identity in one pass (e.g. a Java module whose classpath contains a Kotlin module's
  `build/classes/kotlin/main`).
  - **Consuming side (first increment done):** `MixedInspector.parseFromConfiguration(config)`
    (maddi-inspection-mixed) reads every source set's `.kt`/`.java` files off its source directories and runs
    the shared-core flow (Kotlin-first → generated stubs → Java), so a Java source set resolves a Kotlin
    source-set type to one shared `TypeInfo` from disk. `MixedInspector.parseFromConfiguration` **flattens** all
    source sets into one Kotlin bag + one Java bag (simplest first step).
  - **Multi-source-set (done):** `MixedProjectInspector.parse(config)` places each type in its OWN CST source
    set (Kotlin `Foo` in `kotlin/main`, Java `UseFoo` in `java/main`), not flattened. The openjdk
    `JavaInspectorImpl` owns the shared core (initialised with the Java sets + the stub dir on their classpath);
    Kotlin runs via `KotlinProjectScan` (now taking the shared `CompiledTypesManager`) one `KaSourceModule` per
    set in dependency order; stubs are generated for every Kotlin type; then the Java sets parse from disk
    (`parse(Map.of())` reads the configured source directories). The parse order follows the cross-language
    dependency direction: **Java→Kotlin** ⇒ Kotlin-first + stubs (above); **Kotlin→Java** ⇒ Java-first (its
    source types commit to the shared CTM), then Kotlin resolves them to the same instances (`KotlinProjectScan`
    gets the Java dirs as source roots). Java sets are rebuilt in dependency order with their Java-set deps
    remapped to the rebuilt instances, so Java→Java links across source sets survive the `withDependencies`
    rebuild. Test `TestMixedProjectInspector` (both cross-language directions + a two-Java-module project).
    Remaining follow-ups: a project mixing *both* cross-language directions (intra-module Kotlin↔Java cycle —
    the skeleton-pre-pass case), and mixed-Maven block interleaving on the parse side.

## 7. Known limitations / follow-ups

- **Mixed Maven modules**: kotlin-maven-plugin and maven-compiler-plugin both output to `target/classes`, so a
  Java and a Kotlin compile in one module collide on one output identity. Fine for pure-Kotlin modules; for
  Phase 5 (one-pass javac+kotlinc) this needs `-module-name` / language-tagging to keep them distinct.
- Naming still uses the destination path-suffix heuristic (proven for both languages); `-module-name` is
  captured but not yet the naming source.
- Bare source **directory** args on a raw `kotlinc` line (not files, not Maven's `sourcePath`) are not treated
  as roots; revisit if that form appears in practice.
- The Maven block parser keys off the English DEBUG labels; a localized Maven would need locale-aware markers.
