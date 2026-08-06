# Kotlin corpora: what runs today, and what is in the way

*Status document. Started 2026-08-03, when the first two Kotlin OSS corpora were added and the
modification analysis ran on Kotlin for the first time. Numbers are from that day's runs.*

Until now every OSS corpus was Java. This document records what the Kotlin front end does on two real
projects, how their input configurations are produced (which is not uniform), and the defect tails that
remain — so that the next round starts from evidence rather than from a re-derivation.

## 1. The two corpora

| | coil | detekt |
|---|---|---|
| Shape | Kotlin **Multiplatform** | Kotlin/JVM, multi-module |
| Size | 445 `.kt`; the analysed slice is 101 | ~1,100 `.kt`, 623 of them main |
| Source sets analysed | 1 (flattened) | **31** |
| Java | none | none (its 9 `.java` are test *resources*) |
| Config route | hand-assembled script | **`--compile-log`** |
| Primary types parsed | 171 | **1,271** |

Tests: `TestCoilJvmSlice`, `TestDetektCorpus` (both `@Tag("slow")`, in `maddi-run-kotlin`, resolving the
checkout via `TestOssCorpus` — which lives in that module's **test fixtures** so the Kotlin and Java corpus
tests can share one locator).

### 1.1 Why coil needs a hand-assembled configuration

Neither documented route reaches a multiplatform project:

- the **Gradle plugin** keys on the `org.jetbrains.kotlin.jvm` plugin, the java plugin's `SourceSet`
  container and the `compileKotlin` task. A multiplatform build has none of those — its sets live under
  `kotlin.sourceSets` and the task is `compileKotlinJvm`;
- **`--compile-log`** needs the build to run, and coil applies the Android Gradle plugin, so its build
  cannot even configure without an Android SDK.

`corpus/scripts/coil-input-configuration.py` (`task corpus:config:coil`) therefore assembles the
configuration directly, resolving the slice's compile classpath from Maven Central so nothing has to build
coil at all.

The slice is coil-core's six JVM-target *main* source sets flattened into **one** maddi source set — which
is what a `compileKotlinJvm` invocation would itself yield, the hierarchy sets having no compile of their
own. All six are required: `commonMain` holds the `expect` declarations whose `actual`s live in the others.
Android, JS, wasmJs and native are excluded on purpose: `KotlinProjectScan` builds its session on
`JvmPlatforms.defaultJvmPlatform`, and keeping several targets would give one FQN several `actual`s.

### 1.2 detekt exercises the compile-log route

detekt is the first corpus whose configuration comes from `--compile-log`, so `ParseKotlincList` and
`CompileListToSourceSets` met a real project for the first time. 32 `kotlinc` invocations become **31 source
sets** linked by output identity, with 82 library jars and generated-source directories (buildConfig,
kotlin-dsl accessors) picked up, and a real dependency graph — `detekt-core` depends on 18 others. It is
therefore also the first genuine **multi-source-set** Kotlin parse; coil is a single flattened set.

Three things the capture needs (encoded in `corpus/Taskfile.yml`'s `_config:gradle-log-kotlin`):
`--no-configuration-cache` (a configuration-cached run does not re-log the compiler arguments),
`--no-build-cache --rerun-tasks` (a cached compile logs nothing), and `-Dorg.gradle.warning.mode=summary`
(detekt sets `warning.mode=fail`, which `--debug` trips). detekt's own build needs a **JDK 17 toolchain**.

Producing the configuration also required `--write-input-configuration` on `maddi-run-kotlin`'s `Main`; the
Java CLI had it, the Kotlin one did not.

## 2. What the pipeline does on detekt

| Stage | Result |
|---|---|
| Parse | 1,271 types over 31 source sets, ~8 s |
| Prep | analysis order **9,223**, 8 elements isolated |
| Modification | ~7 iterations to certification, ~15 s |

Verdicts:

```
type.immutable      @Immutable=676  @Immutable(hc=true)=137  @FinalFields=517  @Mutable=418
method.nonModifying true=5224  false=659  ?=274
field.unmodified    true=1243  false=75
```

Two runner changes were needed, both bringing `RunMixedPrepAnalyzer` in line with what
`run-openjdk`'s `RunAnalyzer` has always done:

- **fault-tolerant prep.** Without it prep aborted detekt outright at 652 of 1,202 types. One failing method
  must not deny analysis to a corpus. Isolated elements are listed in full and the exit code reports them,
  so a run cannot look clean while skipping work.
- **`--preload-analysis-results-dirs`.** See §3 — this is the one that is easy to get wrong.

## 3. No immutable types means the annotated APIs are missing

The first modification run on detekt reported `@FinalFields=1320, @Mutable=428` and **not one immutable
type**. That is not a plausible verdict for a codebase of that shape, and it was not the verdict: the
annotated APIs were never loaded, so every library type was an unknown and nothing built on one could be
concluded immutable.

What makes this worth a section of its own is that **nothing else looked wrong**. The run converged,
certified, and reported thousands of verdicts. Loading the JDK archive moves it to:

```
before   @FinalFields=1320  @Mutable=428
after    @Immutable=676  @Immutable(hc=true)=137  @FinalFields=517  @Mutable=418
```

and `field.unmodified=false` drops 115 → 75. So most of what was `@FinalFields` was in fact immutable, held
back only by library types the analysis could not see into.

Loading happens **after** the parse, as on the Java side: only by then is the compiled-types manager
populated, and loading earlier resolves none of the hint types. `Summary.immutableTypes` exists so
`TestDetektCorpus` can assert a floor on it — a run that silently loses the archive would otherwise pass
every other check.

Minor and unfixed: the source set of request for the hint load is the first Kotlin set by iteration order
(`build-logic/main` on detekt). It works, but a set chosen for its library dependencies would be principled.

## 4. Kotlin-only projects: the shared core was never actually shared

Both corpora have **no Java source sets**, which turned out to be a case the mixed driver had never really
been run in. Three separate things were wrong, each only visible once the one before it was fixed. The
detail is in `maddi-inspection-kotlin/mixed-language-integration.md` §11; the summary:

1. `onlyPreload()` scans the *configured* source sets. With none, no scan ran, `lastScanUnits` was never
   set, and the shared `CompiledTypesManager`'s lazy bytecode loader had no live javac task behind it — so
   `getOrLoad` returned null for **every** library type, `java.util.List` included. K2 quietly fell back to
   its own view and the "bytecode is the authority for library shape" invariant did not hold.
2. The Java half needs the project's class path, not just `jmod:java.base`.
3. `KotlinTypeMapper.loadLibraryClass` had to delegate to the manager the way `mapClassType` already did.

Also: **Java stubs are now skipped when there are no Java source sets.** A stub exists for exactly one
reason — javac cannot read Kotlin, so Java *source* referencing a Kotlin type needs something to resolve
against. With no Java source there is no consumer, and every gap in `JavaStubGenerator`'s fidelity was
becoming a hard failure on a parse that was otherwise complete. detekt is where that bit: all 31 source sets
parsed, then the run aborted compiling stubs nothing would read.

## 5. Open tails

Roughly in the order they are likely to matter.

### 5.1 Prep: `variableData` overwrite

The 8 elements detekt isolates are all one cause,
`IllegalArgumentException: Trying to overwrite a value for property variableData`. coil isolates 1, for a
different reason (§5.2). This is the largest single remaining prep tail on Kotlin.

### 5.2 `try` as an expression

`kotlin-cst-assessment.md` lists this as open ("rare; desugar to a helper or accept a small new node if it
actually shows up"). It has shown up: coil's `coil3.util.getCompletedOrNull` is
`return try { getCompleted() } catch (_: Throwable) { null }`. No CST node yields a value from a `try`, so
the statement is built without a `Source` and `MethodAnalyzer` NPEs on `statement.source().index()`. Prep
isolates it and continues. Choosing between desugaring and a new node is a design decision.

### 5.3 Kotlin primitive array classes — blocked on the library loader

`ByteArray` should be `byte[]`, not a shell type named `kotlin.ByteArray`. The mapping is correct in
isolation and was implemented, but it **changes the order in which library types are first reached**, and
`maxMemberDepth`'s first-visit-wins rule makes that order decide whether a type keeps its members: it
stranded `java.util.Iterator` as a members-less shell (reached at depth 2 while loading `java.lang.String`)
and broke `TypeResolutionTest.chainedLibraryCallResolves`. Raising the depth to 3 traded one failure for
four — that constant is tuned.

The prerequisite is a loader that **deepens a shell on a later, shallower visit** instead of letting the
first visit decide. Until then `JavaStubGenerator` translates the names so the generated Java is at least
valid, and `ExpectActualTypealiasTest.primitiveArrayClassesAreJvmPrimitiveArrays` is `@Disabled` recording
the intent.

### 5.4 Kotlin read-only collections collapse to the mutable JVM type

`kotlin.collections.List` and `MutableList` **both** map to `java.util.List`, which the AAPI marks
`@Container`, not immutable. Kotlin's strongest immutability signal is therefore discarded at the mapping
boundary, and the 676 immutable types on detekt are found *despite* it.

The choice is deliberate and documented in `KotlinTypeMapper`: loading the Java symbol keeps `java.*`
matching the Java front end and the AAPI, whereas honouring the Kotlin read-only view would be
order-dependent precisely because both Kotlin types map to one JVM type. Improving on it needs somewhere to
put the distinction other than the shared `TypeInfo` — a Kotlin-side property, or a hidden-content
treatment. Design question, not a fix.

### 5.5 `JavaStubGenerator` fidelity

Still real, but no longer blocking a Kotlin-only corpus (§4). Found on detekt, unfixed:

- **implicit `super()` with no matching parent constructor** — a stub constructor body throws, but javac
  still inserts `super()`, and the parent (`Markdown`, `java.io.PrintStream`) has no no-arg one. Note the
  parent may be a *library* type, so "give every stub a no-arg constructor" only solves half of it;
- **duplicate methods** — a property's generated getter colliding with a declared `getIndent()`;
- **method-level erasure** — `DetektPomModel.getModelAspect` erases its own type parameter, so it neither
  overrides nor differs from `PomModel`'s generic method ("same erasure, yet neither overrides the other").
  The fix applied for coil kept type arguments on `extends`/`implements`; methods still erase.

These matter again as soon as a corpus mixes Java *and* Kotlin source, which neither of these two does.

## 6. Defects fixed via these corpora

For the record, since each was invisible to the unit suite:

| Where | What |
|---|---|
| `KotlinNaming` | JVM file-facade naming ignored kotlinc's sanitisation: `utils.nonAndroid.kt` → `Utils_nonAndroidKt`, not `Utils.nonAndroidKt`. Pervasive in multiplatform, and the dotted name matches no class kotlinc emits |
| `KotlinTypeMapper` | `actual typealias` was never expanded (`expect class Bitmap` + `actual typealias Bitmap = org.jetbrains.skia.Bitmap`); the symbol provider cannot supply it, since to a plain JVM module the two declarations are one redeclaration and the class wins |
| `KotlinScan` | Kotlin **interface delegation** (`: Sink by delegate`) produced a type with *no members at all* |
| `JavaStubGenerator` | Java keywords as identifiers; interface fields without initializers; annotation classes emitted as classes; erased generic supertypes; empty-body interface defaults treated as abstract; `void`-typed fields (Kotlin `Unit`) |
| `MixedProjectInspector` | nested types stubbed twice; no classpath for the stub compiler |
