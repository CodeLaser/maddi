# Handoff — measuring the build plugins against a corpus

**Status: both plugins have a corpus instrument and run the checks. Nine defects found; the two
REPAIRS are refuted for a plugin (§6a).** Dated 2026-08-19.
Gradle: `e96b5f122 b702a49e9 c8fb38c03 b809d4927 df0593929 456d46d73 90514490d`.
Maven: `fb1f5e193 c2db4ce98 80e986647 092034ed7`.
Shared: `0475ee092 8fc2f626d`. Instruments: `bedc73433 7c06a75ca`.

---

## 1. Why this work exists

maddi has **three** producers of one `InputConfiguration`:

| producer | sees | exercised by, before this |
|---|---|---|
| `--compile-log` (`CompileListToInputConfiguration`) | a whole reactor at once | fernflower, timefold, elasticsearch, pulsar … |
| `maddi-gradleplugin` | one project, siblings as jars | **`dogfood` only — maddi's own code** |
| `maddi-mvnplugin` | one module, siblings as jars | **nothing**, until 2026-08-19 (§4) |

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

⚠ Two of these five have now recurred in the Maven plugin (`c8fb38c03` as `c2db4ce98`, `b809d4927` as
`80e986647`). **A defect found in one plugin is a hypothesis about the other**, and it was true both times.

---

## 4. What the Maven plugin's first corpus run found

Three defects, in one afternoon, on a plugin that had never been pointed at anything.

| commit | defect | measured |
|---|---|---|
| `fb1f5e193` | **the scope filter was computed and never applied** — all four scope passes walked the same unfiltered graph, so the plugin produced ONE class path and used it for both compilations | timefold `core/main`: **60** dependencies against javac's **12**; langchain4j-core/main **52 → 5**, exactly its five compile-scope deps |
| `c2db4ce98` | **a reactor sibling resolves to `target/classes`, so `file.getName()` is `classes` for every one** — the same defect as `c8fb38c03`, in the other plugin | `-pl tools/benchmark -am`: 3 siblings, **1** class-path part before / **3** after (control run: 41 non-JDK parts vs 43) |
| `80e986647` | **`sourceRelease` read the properties only**, and the properties are the minority spelling | poms with the property vs with `<release>` in the compiler plugin's own config: activemq 0/8, guava 0/10, jenkins 0/1, camel 12/19. **Jenkins core: 100 parse errors → 0** |

Verdicts, `--analysis-steps=none`, before/after, on every corpus that takes the
`maven-plugin` route:

| | before | after |
|---|---|---|
| langchain4j | 0 errors, 8 warnings | 0 errors, 8 warnings |
| activemq | 0 errors, 7 warnings | 0 errors, **4** warnings |
| **jenkins** | **100 errors**, 99 warnings | **0 errors**, 36 warnings |
| camel | 0 errors, 5 warnings | 0 errors, **2** warnings |

### ⛔ Jenkins had been parsing with 100 errors, at exit 0, and nothing said so

`--analysis-steps=none` returns 0 whether or not the sources parse; the error
count is a separate question, which is why §2 says to count with `grep -c`. The
one instrument that would have caught this is `config.baseline` — and of the four
`maven-plugin` corpora, **only langchain4j has a catalogue entry at all**.
activemq, camel and jenkins exist as `Taskfile` tasks and nothing else, so no
baseline, no `generates` preserve-list, and no `catalogue.py` phase can see them.

That instrument now exists: all three have catalogue entries with
`route: maven-plugin` and a recorded baseline (activemq 500 primary types, camel
388, jenkins 1,464), and the route learned an `mvn_flags` field, because jenkins
needs `-Denforcer.skip=true` on the config run as well as the build run. Every
later change to either plugin is measured through `catalogue.py baseline`.

### The self-checks: done, and they are not what stopped the defects

`checkNamesAreIdentities`, `checkEveryDependencyResolves` and
`checkDependencyReleases` were **private methods of
`CompileListToInputConfiguration`**, so the only producer verifying anything was
the one that already had a reference to diff against. They are now
`run-config/util/ConfigurationChecks`, called by all three (`0475ee092`).

⚠ **They would not have caught any of the eight defects, and the honest version
of that sentence is worth keeping.** The sibling drop is the instructive case:
three projects all named `classes` means the configuration holds one part, every
edge points at it, and both checks are satisfied by a graph missing two thirds of
its class path. What catches that is the **name-clash warning at the point of
construction**, in each plugin's `ComputeSourceSets`. The checks cover the family
next door — a name that means two things, an edge that means nothing — which the
log route has been measured to hit twice.

Two things did fall out of doing the work:

- **`emit` dropped edges in silence.** It built dependency lists with
  `.map(allByName::get).filter(Objects::nonNull)`, while the `sourceSet == null`
  branch three lines below had always logged. It also made the new check blind:
  the drop leaves behind exactly the consistent graph the check looks for.
- **Dependency lists are now sorted.** `graph.edges()` iteration order is an
  artefact of insertion and capacity, so an unrelated edit reorders a list and a
  byte-comparison across a refactor — §2's strongest evidence — reports a
  difference that means nothing. Measured: `java.se`'s 11 dependencies, same set,
  different order, across a change that touched neither.

### The Gradle plugin got its instrument too — and A/B pairs are now expressible

`gradle-plugin` had been a working route with **no entry using it**, so nothing
measured that plugin but `dogfood`. Two entries now do (`bedc73433`, `7c06a75ca`):

| entry | mode | what it can find |
|---|---|---|
| `fernflower-plugin` | single project | the plugin's own construction; nothing between projects |
| `pulsar-plugin` | `:managed-ledger`, 79-project reactor | siblings as ARTIFACTS — the `c8fb38c03` family |

The blocker §7 recorded is gone: **`config.output` may be relative**, resolved
against the project dir, so two entries share one checkout and each owns its file.
Absolute `$TEST_OSS_ROOT/...` does not work — `_path` expands it only when the
variable is set, so the entry would resolve differently per caller.

Both A/Bs agree where the routes overlap: fernflower 199 + 28 primary types,
pulsar `managed-ledger/main` 121 and `/test` 57, plugin and log identical.

⛔ **AND USING THE ENTRY REFUTED A CLAIM IN §7.** It said the corpus route applies
the plugin to one project. The init script says `allprojects`, so the route had
always been running siblings-as-**source**, which on `:managed-ledger` is exit 1
with **107** diagnostics. The mode is now explicit (`maddi.applyTo` / `apply_to`),
because the two are **different tests and neither covers the other**:

    siblings as SOURCE     -> df0593929  (sibling handed the consumer's class path)
    siblings as ARTIFACTS  -> c8fb38c03  (every sibling's part named `main`)

`pulsar-plugin` picks artifacts and is green. **The siblings-as-source path has no
entry on purpose** — its 107 are a sibling's `compileOnly` dependencies, which
Gradle propagates to no consumer at all, and the only design that would supply
them is refuted (§6b). An entry that is permanently red is a gate people learn to
ignore. Reproduce with `-Dmaddi.applyTo=all`; expect 107.

### trino: a fourth Maven defect, on the first run

⚠ `RequireJavaVendor` first — trino's enforcer wants Temurin or Oracle, and
`-Denforcer.skip=true` does **not** skip it.

The A/B on `core/trino-main` (151 dependencies, the heaviest module, against a
209-source-set `maven-log` reference): **nothing missing on either scope**, extras
all sibling jars where the reactor log had them as parsed source — the designed
difference. One error, and it was not about class paths at all.

**Neither plugin recorded `--add-modules`** (`8fc2f626d`). trino declares
`<compilerArgs><arg>${extraJavaVectorArgs}</arg>` = `--add-modules=jdk.incubator.vector`.
`SourceSet` has carried an `addModules` field all along, `--compile-log` records it
on 6 of trino's 209 source sets, and `CompileInvocation`'s javadoc **names trino** —
while both plugins set it on nothing. ⛔ It is not reachable by widening `jmods`:
an incubator module is not in the `java.se` closure and cannot be added to it; it
must be in the compilation's ROOT SET. Gate: 1 error → 0.

Fixed in both plugins on the twin rule. ⚠ **No corpus exercises the Gradle half**,
so the unit test and the symmetry are what stand behind it, not a measurement.

### ▶ THE NEXT JOB: trino has no catalogue entry

It is the largest Maven reference available — 106 modules, 209 source sets, 1,015
class-path parts, all release 25 — and it found a defect the moment it was pointed
at the plugin. It is currently a `Taskfile` task plus an ad-hoc config, **which is
exactly the shape that let jenkins parse with 100 errors at exit 0 unnoticed**.
A `trino` + `trino-plugin` pair, on the model of `pulsar`/`pulsar-plugin`, keeps
what it found. It needs `mvn_flags` or a `JAVA_HOME` for the vendor enforcer;
`core/trino-main` is the module the A/B used.

### What the A/B needed, and how it was got

`corpus/catalogue/langchain4j.yml` is `route: maven-plugin` only, so there was
nothing to diff it against. **timefold-solver** supplied the reference instead:
it carries `route: maven-log` over its whole 65-source-set reactor, so running
the Maven plugin on one of its modules and diffing that module's dependency list
against the reactor's is a true A/B. That is where the 60-vs-12 came from, and it
is the shape to reach for again — langchain4j is too small and too clean to find
anything.

## 5. Traps that cost time — five weak instruments

- **`ProjectBuilder` resolves no artifacts at all.** A fixture using it asserted over an empty class
  path and reported "0 parts" as a failure of the expectation, not of the code.
- **Plain `java` vs `java-library`.** Only the library plugin publishes a `classes` variant, which is
  what makes Gradle hand over the *directory* rather than the jar. With plain `java` the siblings
  arrive as `alpha.jar`/`beta.jar`, whose names already differ — **the test passes against the
  defect.**
- **A 3-project GradleTestKit fixture does not reproduce a 79-project build's resolution locking.**
  The refuted variant in §6 passed its functional test and did nothing on a corpus.

- **A vendor enforcer that `-Denforcer.skip=true` does not skip.** trino's `RequireJavaVendor` wants
  Temurin or Oracle, fails after every other rule has passed, and prints nothing useful without
  `-e` — the rule number simply goes missing from the list.

⛔ **For anything touching cross-project resolution, the gate is a real multi-module corpus, never a
fixture.**

- **A corpus that is BUILT before the plugin runs will hide a stale read.** `AnnotationProcessorOutput`
  produced real-looking generated-class libraries on langchain4j and activemq — off the *previous*
  build's `target/classes` (§6a). Every maven-plugin corpus runs `mvn install` first, so no corpus in
  the catalogue could have shown it. The instrument that did was a Gradle *fixture*, which is the one
  place where a fresh build is guaranteed.

---

## 6. Refuted, do not rebuild

### 6a. The two REPAIRS `--compile-log` makes do not belong in a plugin

`AnnotationProcessorOutput` and `TypeUseAnnotationClosure` were wired into the shared plugin path on
2026-08-19 and taken straight back out. `TestAnalyzerPluginFunctional#configurationCacheCompatible`
is the instrument, three runs:

| wired in | verdict |
|---|---|
| checks only | **PASSES** |
| + `TypeUseAnnotationClosure` | FAILS |
| + `AnnotationProcessorOutput` | FAILS |

```
configuration cache cannot be reused because the file system entry
'build/classes/java/main' has been created.
```

⛔ **The cache is the symptom; the cause is *when* this runs.** `AnalyzerPlugin` computes the whole
configuration inside a `project.provider(...)` resolved at **configuration/store time** — it says so
in as many words — and both repairs read the file system, so Gradle discards the entry the moment
compilation creates the directory.

⛔⛔ **And with the cache off they would answer nothing, which is the real point.** Both ask about a
source set's **compiled destination**, and a plugin builds this configuration *before* the compile
tasks it depends on have run — the same sentence `PluginSourceSets.classPathUri` already carries.
There are no classes there to read.

⚠ **On Maven they LOOKED like they worked**, and this is the trap worth remembering: langchain4j-core
and activemq-broker really did yield generated-class libraries. Those corpora are built with
`mvn install` first, so the mojo was reading the **previous** build's `target/classes`. *A repair
whose input is last time's output is not a repair; it is a stale read that happens to be right.*

⭐ What is different about `--compile-log`: it parses a log written **after** the compilation, so the
destination it reads is the output of the very compile it describes. No build plugin has that
guarantee at configuration time. ▶ If it is ever worth doing, it belongs at **execution time** —
inside the task action or the forked worker — which is a design change, not a call site. The full
note is pinned in `PluginInputConfiguration.emit`.

⭐ It was not wasted: feeding a plugin's source-set name to `AnnotationProcessorOutput` for the first
time found a live defect in the **log route's own** code — it built its library URI by concatenation,
`URI.create("file:" + target)`, which throws `Illegal character in path at index 90` on
`LangChain4j :: Core/test`. The identical sentence — *"toURI(), not `file:` + path"* — was already
written in `PluginSourceSets.classPathPart`, one package away, and had landed in only one of the two.
Fixed and kept, wiring reverted.

### 6b. A variant publishing the producer's class path

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
  sibling projects or source-project edges. ⚠ The condition this bullet used to carry — "revisit once
  the Maven plugin has a corpus and tests" — **is now met**, and the answer is still no: merging puts
  the Gradle model on the Maven path with nothing to gain. `runtimeOnly` is no longer a divergence
  either; it is a FLAG on the Gradle side and a SCOPE on the Maven side, resolved in
  `mvnplugin/ComputeSourceSets` before the shared class sees anything. Revisit when a corpus asks for
  something only the Gradle model can express.
- ~~**The `gradle-plugin` corpus route has no catalogue entry.**~~ Done (§4): `config.output` may now
  be relative, which is what makes an A/B pair expressible.
- **The residual 107 diagnostics on pulsar with siblings-as-source** are a sibling's `compileOnly`
  dependencies, which Gradle propagates to no consumer. Only §6b would close it, and §6b is refuted.
  ⛔ The sentence that used to end this bullet — "the corpus route applies it to one, where pulsar is
  exit 0" — **was wrong**; the init script says `allprojects`. The mode is now a deliberate setting
  (§4), and `pulsar-plugin` chooses the one that can be green.

---

## 8. The corpus map, as of 2026-08-19

| entry | route | what it measures | baseline |
|---|---|---|---|
| `langchain4j` | maven-plugin | one clean module; found nothing, and that is its value as a control | 509 |
| `activemq` | maven-plugin | `<release>` in the compiler plugin, incl. an MRJAR execution to ignore | 500 |
| `camel` | maven-plugin | a deep reactor's one module | 388 |
| `jenkins` | maven-plugin | inherited `<release>`; **was 100 errors at exit 0** | 1,464 |
| `timefold-solver` | maven-log | the Maven A/B REFERENCE, 65 source sets | — |
| `fernflower` / `fernflower-plugin` | gradle-log / gradle-plugin | the single-project A/B pair | 227 |
| `pulsar` / `pulsar-plugin` | gradle-log / gradle-plugin | the multi-module A/B pair, siblings as artifacts | 4,112 / 178 |
| trino | maven-log, **no entry** | 209 source sets; found `--add-modules`. §4's next job | — |

---

## 9. Operational notes

- `eval "$(ws env server)"` before any maddi gradle command; `env -u GRADLE_USER_HOME` for a corpus's
  own build.
- The **whole-box lock** is an flock: heavy commands refuse with exit 3 while another job holds it.
  Never read `.box.lock`, which always exists. **To wait, use `boxlock --wait[=30m] <cmd>`**, added
  2026-08-19 (`jfocus-devops` `16247f2`).
  ⛔ **The loop this bullet used to recommend is wrong, and it was wrong here for a day**:
  `until boxlock status; do sleep 15; done; boxlock <cmd>` takes the lock **twice**, so between the
  probe and the real acquire the box is free for anyone — observed, a run took exit 3 immediately
  after a successful probe. It is also a lottery rather than a queue: whoever's `sleep` expires
  nearest the release wins, however long anyone has waited. `--wait` asks once and blocks in the
  kernel; on its deadline it exits 3, the same code as a refusal, so nothing downstream changes.
- `MADDI_EXPORTS` (the five `--add-exports`) must be set for any `:maddi-run-openjdk:run`.
- `--no-build-cache` throughout: a cache hit restores outputs without running javac or the tests.
- The Gradle-plugin corpus route drives `corpus/scripts/maddi-plugin.init.gradle.kts` and touches no
  file in the corpus checkout; `config.gradle_args` carries a build's own flags (pulsar needs
  `-PskipJavaVersionCheck` on JDK 26).
