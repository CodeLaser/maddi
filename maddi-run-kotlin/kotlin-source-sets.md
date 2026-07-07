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
| Gradle marker | `…Compiler arguments: …` | `…Kotlin compiler args: …` | new regex |
| Maven marker | `[DEBUG] (-d …)` | kotlin-maven-plugin DEBUG line | new regex (see §5) |
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

```
GRADLE : .*Kotlin compiler args:\s*(.+)      # org.jetbrains.kotlin.gradle plugin, --debug
MAVEN  : .*\[DEBUG].*Kotlin compiler.*args.*?(-.+)   # kotlin-maven-plugin, -X   (validate on a real capture)
KOTLINC: kotlinc(?:-jvm)?\s+(.+)             # raw CLI
```

The Gradle marker and the argument layout in §3 are validated against a live capture. The Maven marker is
best-effort and flagged for validation against a real `mvn -X` kotlin-maven-plugin log (Phase 0 follow-up).

## 6. Phasing

- **Phase 0** — capture real logs, pin markers/layout. *Done for Gradle; Maven marker pending a real capture.*
- **Phase 1** — `Kotlinc.parse` (+ argfile), unit-tested against captured-shape lines.
- **Phase 2** — generalize the engine to `CompileInvocation`; `Javac` ported onto it; `TestJavac*` stay green.
- **Phase 3** — `maddi-run-kotlin` + `ParseKotlincList` → `InputConfiguration`; fixture tests.
- **Phase 4 (later)** — a K2 driver that consumes an `InputConfiguration` (build a `KtSourceModule` per source
  set, wire library jars + upstream source sets + friend paths). This is the analogue of how the openjdk
  `JavaInspector` consumes the config; it is out of scope for Phases 0–3.
- **Phase 5 (later)** — feed **both** javac and kotlinc invocations from one build log into a single
  `CompileListToSourceSets`, so Java and Kotlin modules link by output identity in one pass (feeds the
  mixed-language `MixedInspector`).

## 7. Known limitations / follow-ups

- Maven marker unvalidated (§5).
- Naming still uses the destination path-suffix heuristic (proven for both languages); `-module-name` is
  captured but not yet the naming source. Revisit for Maven, where Java and Kotlin share `target/classes` and
  the two languages' outputs collide on one identity.
- Bare source **directory** args (not files) are not treated as roots (build tools pass files); revisit if a
  raw-`kotlinc` use passes dirs.
