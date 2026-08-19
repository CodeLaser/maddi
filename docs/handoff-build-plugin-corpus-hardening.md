# Handoff — measuring the build plugins against a corpus

**Status: the Gradle plugin is done for now; the Maven plugin has not started.**
Dated 2026-08-19. Commits `e96b5f122 b702a49e9 c8fb38c03 b809d4927 df0593929 456d46d73 90514490d`.

---

## 1. Why this work exists

maddi has **three** producers of one `InputConfiguration`:

| producer | sees | exercised by, before this |
|---|---|---|
| `--compile-log` (`CompileListToInputConfiguration`) | a whole reactor at once | fernflower, timefold, elasticsearch, pulsar … |
| `maddi-gradleplugin` | one project, siblings as jars | **`dogfood` only — maddi's own code** |
| `maddi-mvnplugin` | one module, siblings as jars | **nothing** |

`TestCompileLogCli` states the structural difference outright: *"This is the only importer that sees a
whole reactor at once; the Maven and Gradle plugins are invoked per module and see siblings as jars."*

Pointing a real corpus at the Gradle plugin found **five defects in one day**, four of which
`dogfood` structurally could not show. The Maven plugin has had even less exposure: **`maddi-mvnplugin`
has no tests at all.**

---

## 2. The method — this is the transferable part

1. **A/B against `--compile-log` on the same checkout.** Regenerate *both* configs; do not trust a
   checked-in one (fernflower's was stale by two fields, langchain4j's by three).
2. **`--analysis-steps=none` is the verdict.** Exit code *and* error count — exit 0 with warnings is a
   different answer from exit 0 clean. Count with
   `grep -c 'Error found in'`; the `ErrorReport` list is capped ("… and N more") and will understate.
3. **Run the control before concluding.** Twice this session the obvious diagnosis was wrong and a
   control refuted it: the sibling-source path was *not* the cause of the multi-module failure
   (siblings-as-jars failed too), and the engine `AssertionError` was *downstream* of a missing
   `sourceRelease`, not a defect of its own.
4. **Mutation-check every new test.** Revert the fix, confirm the test goes red, restore. Two tests
   this session passed against the defect until this was done.
5. **Byte-compare the configs before and after a refactor.** fernflower and pulsar came out identical
   across the whole de-duplication commit; that is far stronger evidence than a green suite.
6. **Classify a missing package before fixing it**: is it *absent* from the config, or *present and
   unwired*? 27 of 38 were present — which turned "add the inputs" into "fix the wiring".

---

## 3. What was fixed (Gradle side)

| commit | defect |
|---|---|
| `781c28a97` `b809d4927` | jmod construction written in **three** places; `jmods` defaulted to `java.base` in Gradle and `java.se` in Maven. Then: **`sourceRelease` never recorded**, so a release-17 corpus parsed at JDK 26. |
| `513ace8c7` | **a source set's `uri` was its first source directory, not its class output** — javac silently recompiled the dependency instead of reading its class files, and lost any type whose directory ≠ package. |
| `c8fb38c03` | **a project dependency resolves to `build/classes/java/main`, so `file.getName()` is `main` for every sibling** — the de-dup guard dropped 6 of pulsar's 7, with no log line. |
| `df0593929` | a co-analysed sibling was given the **consumer's** classpath under the **consumer's** test/runtime scoping. |
| `456d46d73` | a refuted design, pinned so nobody rebuilds it (see §6). |
| `90514490d` | duplication sites B, E, F, H shared; C and G deliberately not (see §7). |

Result: fernflower ≡ `--compile-log`; pulsar `:managed-ledger` exit 0; dogfood unmoved throughout.

---

## 4. ▶ THE NEXT JOB: the same work for the Maven plugin, starting with langchain4j

**First deliverable: regenerate langchain4j's `inputConfiguration.json` and commit it.** The
checked-in one predates `buildUnit`, `sourceRelease` and the `module` flag — verified 2026-08-19:
regenerating it now adds `buildUnit=dev.langchain4j:langchain4j-core`, `sourceRelease=17`, and
`module` on **31 of its 73 class path parts**. It is a corpus baseline, so it was left for a
deliberate decision rather than updated in passing.

```console
$ eval "$(ws env server)"
$ ./gradlew :maddi-mvnplugin:publishToMavenLocal          # or: task corpus:config:plugin (publishes BOTH)
$ cd ../../test-oss/langchain4j
$ MAVEN_OPTS="$MADDI_EXPORTS -Xmx6G" mvn -q -pl langchain4j-core generate-test-sources \
      io.codelaser:maddi-mvnplugin:0.9.1:write-input-configuration
$ cp langchain4j-core/target/inputConfiguration.json inputConfiguration.json
```

Then the verdict, from the maddi checkout:

```console
$ ./gradlew -q :maddi-run-openjdk:run --args="\
    --input-configuration=<abs>/langchain4j/inputConfiguration.json --analysis-steps=none"
```

### ⛔ The A/B needs a reference, and langchain4j has none

`corpus/catalogue/langchain4j.yml` is `route: maven-plugin` **only**. There is no compile-log config
for it, so there is nothing to diff against — unlike fernflower, which carried `gradle-log` and made
the Gradle A/B possible in one run. Two ways to get one:

- **Capture `maven-log` for langchain4j** (`./mvnw -X clean test-compile`, grep `^\[DEBUG] -d `, feed
  to `--compile-log`). Its build only partially completes (`expect: partial`), which may be enough:
  only `langchain4j-core` is parsed.
- **Or A/B on `timefold-solver` instead**, which already has `route: maven-log` over its whole
  65-source-set reactor — run the Maven plugin on one of its modules and compare against the reactor
  config restricted to that module. This is the closer analogue of what pulsar did for Gradle.

Do langchain4j first (it is the stated deliverable and it is cheap), but expect the *defects* to come
from the second.

### What to look for, in priority order

1. **Class-path part naming.** `ComputeSourceSets.processDependencyNodes` still names parts by
   `artifact.getFile().getName()` — the exact shape of `c8fb38c03`. In Maven a reactor sibling
   normally resolves to a jar from `~/.m2` (distinct names), but an in-reactor build resolving to
   `target/classes` collides identically. **Untested; no corpus exercises it.**
2. **`sourceRelease` reads properties only** (`maven.compiler.{,test}{release,source}`). A pom setting
   `<release>` inside `maven-compiler-plugin`'s own `<configuration>` is not seen and falls back to 0.
   Deliberate — a wrong release is worse than none — but it is the likeliest gap on a real corpus.
3. **`ComputeDependencies` (the one in `maddi-run-config`) has no notion of `runtimeOnly`**, so a
   runtime-only library reaches every Maven source set. The Gradle twin keeps it off a compile class
   path. See §7.
4. **No `AnnotationProcessorOutput` / `TypeUseAnnotationClosure` / config self-checks.** The log route
   runs all of these; neither plugin does. `checkEveryDependencyResolves` is the one whose absence
   cost a day on Elasticsearch, per its own javadoc.
5. **`maddi-mvnplugin` has no tests.** Anything found should arrive with one; the Gradle side's
   `TestMultiProjectClassPath` is the model, and §5 is why it must be functional.

---

## 5. Traps that cost time — all three were weak instruments

- **`ProjectBuilder` resolves no artifacts at all.** A fixture using it asserted over an empty class
  path and reported "0 parts" as a failure of the expectation, not of the code.
- **Plain `java` vs `java-library`.** Only the library plugin publishes a `classes` variant, which is
  what makes Gradle hand over the *directory* rather than the jar. With plain `java` the siblings
  arrive as `alpha.jar`/`beta.jar`, whose names already differ — **the test passes against the
  defect.**
- **A 3-project GradleTestKit fixture does not reproduce a 79-project build's resolution locking.**
  The refuted variant in §6 passed its functional test and did nothing on a corpus.

⛔ **For anything touching cross-project resolution, the gate is a real multi-module corpus, never a
fixture.**

---

## 6. Refuted, do not rebuild

A companion variant publishing the producer's compile class path (so a co-analysed sibling gets the
exact answer, and its `compileOnly` dependencies stop being invisible). Gradle 9 refuses it:

```
Resolution of the configuration ':pulsar-metadata:compileClasspath' was attempted
without an exclusive lock. This is unsafe and not allowed.
```

All 7 pulsar siblings failed; the variant contributed 0 files. The full note is pinned in
`AnalyzerExtension` next to `SOURCE_ELEMENTS_CONFIGURATION_NAME`, where the next person to have the
idea will read it. It also records the one route that could work: the **producer** writing its class
path during its own build, as a task output, so there is no cross-project resolution — at the cost of
a task run per sibling.

---

## 7. Decisions left open

- **Site G — `parallel` default.** `false` in the Gradle extension, `true` in the Maven `@Parameter`.
  The CLI flag and `GeneralConfiguration` both default `false`, so **Maven is the outlier** — but
  flipping it makes every Maven run slower, and flipping Gradle's instead moves dogfood's parallel
  output ordering. A product decision, not a refactor.
- **Site C — the two `ComputeDependencies`.** 90 lines against 161; the Maven one has no notion of
  sibling projects, runtime-only scoping or source-project edges. Merging means putting the Gradle
  model on the Maven path with nothing to measure it. Each now names the other and records the
  divergence; revisit **once the Maven plugin has a corpus and tests.**
- **The `gradle-plugin` corpus route has no catalogue entry.** Adding one means two entries writing
  one `inputConfiguration.json` into a shared checkout, which breaks the `generates` preserve-list.
  Needs a `config.output` convention for A/B entries.
- **The residual 108 diagnostics on pulsar with siblings-as-source** are a sibling's `compileOnly`
  dependencies, which Gradle propagates to no consumer. Only §6 would close it. Note that this path
  engages only when the plugin is applied to *every* project (the dogfood pattern); the corpus route
  applies it to one, where pulsar is exit 0.

---

## 8. Operational notes

- `eval "$(ws env server)"` before any maddi gradle command; `env -u GRADLE_USER_HOME` for a corpus's
  own build.
- The **whole-box lock** is an flock: heavy commands refuse with exit 3 while another job holds it.
  Probe with `boxlock status`; never read `.box.lock`, which always exists. Queue work with
  `until boxlock status >/dev/null 2>&1; do sleep 15; done; <cmd>` in the background.
- `MADDI_EXPORTS` (the five `--add-exports`) must be set for any `:maddi-run-openjdk:run`.
- `--no-build-cache` throughout: a cache hit restores outputs without running javac or the tests.
- The Gradle-plugin corpus route drives `corpus/scripts/maddi-plugin.init.gradle.kts` and touches no
  file in the corpus checkout; `config.gradle_args` carries a build's own flags (pulsar needs
  `-PskipJavaVersionCheck` on JDK 26).
