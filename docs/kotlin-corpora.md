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

### 5.5 The Kotlin stdlib has no annotated APIs

§3 is about the JDK archive: without it, no detekt type could be concluded immutable. The same hole is one
level out, and became visible only once library **extension** calls resolved (maddi#43). `x.map { … }`,
`a to b`, `s.trimIndent()` are now real calls to `CollectionsKt`, `TuplesKt`, `StringsKt` — and the analyzer
knows nothing about any of them, so a field passed to one is a field handed to a method that may modify it.

Measured on detekt, the day extension calls started resolving: analysis order 12,543 → 14,697 (17% more
elements analysed), and 19 verdicts moved — 16 down (`@Immutable`/`@Immutable(hc=true)` → `@FinalFields`,
e.g. every `DefaultValue` subclass in `detekt-generator`, whose `printAsYaml` writes `name to quoted`),
3 up. The drop is honest: the analyzer used to see a placeholder where it now sees a call it cannot judge.

The fix is an annotated-API archive for `kotlin-stdlib`, the way the JDK has one. Until then, expect a
Kotlin corpus to under-report immutability wherever the stdlib's extension functions touch a field.

**Started, and the first measurement is about the SHAPE of the work.** `libs/kotlin` existed (for
`kotlin.Lazy`) but no Kotlin corpus ever loaded it — `TestDetektCorpus` preloaded `jdk` alone; it now
loads both, and `kotlin.Pair` + `kotlin.TuplesKt.to` are in the archive. **Neither moves a single one of
the 1271 verdicts.** A type's verdict is held down by *every* unannotated call in it, not by the first one
contracted: `DefaultValue` is a sealed interface whose subclasses also call `map`, `mapOf` and `error`, so
contracting `to` alone changes nothing. The archive therefore pays off in steps of a whole type's call
set, and **a partial archive is indistinguishable from none** — which is why it should be built by taking
one affected type at a time to green, not by working down a frequency list of stdlib methods.

Two silent failures to know about when adding to it. An unresolved symbol in a hints file drops the
**whole compilation unit**, and `compileAnalysisHints` still exits 0 — "annotated 0 types" in a log nobody
reads, the previous `.json` left in place; check the file changed. And naming a real Kotlin type needs
`kotlin-stdlib` on `maddi-aapi-archive`'s own compile path (`compileOnly` + `requires static`), which
nothing needed before: the Lazy contract names only its own shadow.

#### ⚠ A contract must name the PART class, and a method token must carry its parameter types

`TuplesKt` could be written straight off because it is a **single-file** facade that declares `to` itself.
Every facade that matters — `CollectionsKt`, `MapsKt`, `StringsKt`, `SetsKt`, `SequencesKt` — is a
**multifile class facade**: `kotlin.collections.CollectionsKt` declares *nothing at all* but a private
constructor, and `extends CollectionsKt___CollectionsKt`, from which it inherits every method. A contract
written against the facade is ignored method by method —

```
Ignoring method 'CollectionsKt$.map(Iterable,Function1)', not found in target type 'kotlin.collections.CollectionsKt'
```

— leaving a `.json` that holds the type with **no method contracts**, and a green build. Name the **part
class** instead: that is also what a CALL names, since `jvmFacadeClassId` reads the symbol's FIR
containerSource, whose ClassId for a multifile part is the part
(`FacadeAndExtensionTest.aMultifileFacadeCallNamesTheClassThatDeclaresIt` pins it).

That was enough for `map`. `mapOf` still fell out at load, and for an unrelated reason worth knowing:

```
Skipping analysis hint for unresolvable element 'mapOf(15)': ambiguous method 'mapOf' (2 overloads) with a stale index
```

A method was addressed by a positional **index** into the type's sorted method list, with a fallback to a
unique simple name. An overload has neither: the index is computed from the bytecode the archive is built
from and resolved against the Kotlin front end's model of the same class, which is a different list. The
token carries the erased parameter types now — always, because whether a name is overloaded is a property
of the *decoder's* view and the encoder cannot know it (`mapOf` is one method in bytecode and two in the
Kotlin model). They are optional in the grammar, so every archive written before them still decodes.

Two defects surfaced on the way: a library `vararg` was modelled as its ELEMENT type, so `mapOf(vararg
Pair)` collided with the single-pair overload and one of them was dropped; and
`TestParseAnalyzeWrite` parses the whole archive with the shared inspector factory, which deliberately
carries no kotlin-stdlib — so both Kotlin hints files are dropped whole there, and the count says so.

detekt then skipped **no hint at all** — and `map` and `mapOf` still carried nothing but
`{"annotatedApi":1}`. A third silent failure, below the two above:

#### ⚠ A resolved contract for a package-private type says nothing

`ShallowAnalyzer.go` filtered *every* type through `acceptAccess(info) = !onlyPublic ||
info.access().isPublic()`, and `AnalysisHintsCompiler` passes `onlyPublic = true`. A package-private type
handed to it was dropped from `allTypes` and never reached `DEFAULTS_ANALYZER` — so the contract parsed,
resolved, wrote its `.json`, and exited 0 carrying only the `ANNOTATED_API` marker and no computed
property. Nothing in the pipeline says so.

That is not an edge case for the Kotlin stdlib, it is the whole of it: a multifile facade's part class is
package-private **by construction**, and the public facade declares nothing. `onlyPublic` is meant to prune
the closure the analyzer walks into — sub- and supertypes it reaches on its own — not the list it was
handed, so `inScope(t, requested)` now exempts the types the caller NAMED (`AnalysisHintsParser.types()`,
which is exactly what a hand-written shadow writes down, nested types included).

Across the 27-file JDK archive plus `libs/`, exactly two `.json` files move: `KotlinCollections.json`, and
`JavaAwt.json` — whose hand-written contracts for `java.awt.Component`'s two **protected** nested classes,
`BltBufferStrategy` and `FlipBufferStrategy`, had been writing marker-only shells all along.

One level up, `CompileAnalysisHints.compile` was discarding the `List<Message>` that `AnalysisHintsCompiler.go`
returns — the analyzer's complaints about the shadows, thrown away exactly where the archive is regenerated.

`map` and `mapOf` are applied now, and `DefaultValue` is still held down by the rest of its call set
(`error`, and detekt's own `YamlNode` builders). Which is the §5.5 rule again: a whole type's calls, or
nothing.

### 5.6 `JavaStubGenerator` fidelity

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
| `KotlinTypeMapper`, `KotlinBodyConverter` | a call to a library **extension** function resolved to nothing, and the placeholder swallowed its arguments — a lambda, and every declaration written in it (maddi#43). The facade now holds extensions as statics whose first parameter is the receiver, the receiver may be implicit (`run { … }` in a member), and a receiver lambda carries its receiver as `$receiver` so `sb.apply { append(…) }` resolves |
