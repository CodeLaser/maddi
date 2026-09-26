# Kotlin parity study — parser, prepwork, link, analyzer (2026-09-26)

**Question:** how far is maddi, structurally, from treating Kotlin as Java is treated — layer by layer, not
"does detekt parse".

**Verdict.** The stack below the front end is shared, and the shared part is where parity already holds:
prepwork, link and the analyzer carry almost no Java names, and every Kotlin-vs-Java verdict fixture written so far
agrees. The distance is in three places, in this order:

1. **The front end still has silent-wrong shapes.** The placeholder census counts what it refuses; it cannot count
   what it converts to the wrong thing. A 20-line probe written for this study found **six** shapes that convert
   with zero placeholders and mean something else (§2.3). The census's "detekt 8 / coil 4 / javalin 13" is a
   floor on the unknown, not a measure of it.
2. **The analyzer's knowledge of the Kotlin stdlib is ~1/16th of its knowledge of the JDK** (31 annotated members
   vs 4,877), and an uncontracted library method is a *modifying* one by default (§5.2). That is the single
   largest verdict-level gap, and it is not a front-end problem.
3. **Kotlin reaches the downstream layers only from outside them.** prepwork/link/analyzer hold ~1,000 Java-input
   tests and zero Kotlin-input tests; the Kotlin evidence is 42 ported prep tests and ~14 verdict fixtures in
   other modules (§6). What is proven is that the shared code is neutral on the shapes those fixtures cover.

The entry points around the analysis (Gradle/Maven plugins, IDE clients, incremental analysis) remain Java-only by
construction; that is scope, not structure, and §7 lists it without re-arguing it.

Evidence is `path:line` throughout; **[V]** = read in code or run here, **[I]** = inferred. Four parallel code sweeps
plus one probe run; the 2026-09-21 gap analysis (`docs/kotlin-gap-analysis-2026-09-21.md`) was used for leads only
— its §3–§6 are known stale and are re-verified in §7.

---

## 1. The shape of the stack

Main-source lines and tests per layer, so the "shared" claim has a size:

| Layer | Java-only | Kotlin-only | Shared | Tests (Java in / Kotlin in) |
|---|---|---|---|---|
| CST model + printers | `cst-print` 1.2k | `cst-print-kotlin` 0.9k (no `src/test`) | `cst-api` 12.6k, `cst-impl` 34.0k | 47 / 12 (printer tests live in `inspection-kotlin`) |
| Front end | `java-parser` 55.5k, `inspection-openjdk` 2.4k, `java-openjdk` 9.1k | `kotlin-k2` 9.9k, `inspection-kotlin` 0.5k, `inspection-mixed` 0.6k, `kotlin-api/-realm` 0.7k | `inspection-api` 2.9k, `inspection-resource` 2.0k | 281 / **501** (k2 390, inspection-kotlin 80, mixed 26, api 5) |
| Prepwork | — | — | 7.9k | 249 / 0 in-module; **42** in `inspection-kotlin/prepwork` |
| Link | — | — | 12.0k | 426 / 0 |
| Analyzer + common | — | — | 11.9k + 5.9k | 328 + 105 / 0 |
| Archive (contracts) | JDK 249 types | kotlin 14 types | `aapi-parser` 1.4k | — |
| Runners | `run-openjdk` 1.6k | `run-kotlin` 1.1k | `run-config` 4.3k | 63 / 61 |

Two readings. The Kotlin front end is ~1/6th the size of the Java one and has *more* unit tests — because it
lowers to the Java CST and every lowering is a test. And 37.7k lines of shared downstream code have never been run
on Kotlin input from inside their own modules; the 103 Kotlin-input tests that reach them sit in `inspection-kotlin`
and `run-kotlin`.

## 2. Parser / front end (`maddi-kotlin-k2`)

### 2.1 What is modelled — the JVM shape, deliberately

The front end emits what kotlinc emits, not what Kotlin says: `Unit`→`void`, `Int`→`int`, `Int?`→`Integer`+NULLABLE
(`KotlinTypeMapper.kt:250-253, 295-322`), `List`/`MutableList`→`java.util.List` (`:1046`), function types →
`kotlin.jvm.functions.FunctionN` (`:239`), suspend → trailing `Continuation` + `Object` return (`:736-748`),
properties → private field + `getX`/`setX` with `GET_SET_FIELD` pre-set (`KotlinScan.kt:1562-1618, 2049, 2069`),
companions → nested type + static field, `const`/`@JvmField` → outer statics, top-level → `FooKt` facades,
defaults → `$default` with masks (`KotlinScan.kt:1399-1470`), data classes → RecordSynthetics + real `componentN`/
`copy` bodies (`:2129-2165`), annotations placed where K2 places them, with arguments (`KotlinAnnotations.kt:47-86`).

That choice is why the downstream is neutral: prep sees a getter, not a property.

Construct coverage, verified against the dispatch (`KotlinBodyConverter.kt:1268-1350, 1935-2019`):

| Done | Partial | Refused (named placeholder) |
|---|---|---|
| callable refs (all but 10 sub-kinds), `X::class`, `X::class.java`, local `fun`, `object :`, receiver lambdas, destructuring (3 positions), string templates, elvis/safe-call/`!!`/`as?`/smart casts, `lateinit`, `const`, `by lazy`/`by Delegates`/custom delegates (member & top-level), companions, objects, sealed, data, nested/inner, type aliases, generics + variance, defaults, named args, varargs/spread, extensions, facades, `@Jvm*`, `open`/`final`, interface defaults, `expect`/`actual`, KMP `dependsOn` | labeled expressions (statement level only), `inline`/`reified` (ordinary methods), source value classes (no unboxing/mangling), operators (`++`/`--` always int), `when` (see §2.3), ranges (`..`/`..<` build the type; `downTo`/`step` are calls), `try` as value (4 positions), `internal` (no `$module` mangling), `DefaultImpls` not modelled | reified `T::class`, `@InlineOnly` with no bytecode (`Result.getOrNull`), labeled `return@l` as a lambda value, elvis-jump after an unstable argument, multi-statement block in expression position, gradle-DSL sources (K2 compiler plugins) |

Corpus census (two-sided ratchet, tolerance 5): detekt **8** in 1,384 types, coil **4** in 186, both prep-isolated
0 (`TestDetektCorpus.java:198-199`, `TestCoilJvmSlice.java:181-182`). javalin 13 is doc-only; no test pins it [V].

### 2.2 The two-model problem is still open, patched per shape

A library type reaches the CST either from bytecode (mixed/CLI: `KotlinTypeMapper.kt:138-142, 563-589`) or built
from K2 symbols (standalone: `:591+`). They differ (`Lazy.value` vs `getValue()`, suspend arity, vararg shape,
`String.get` vs `charAt`), and each difference has been fixed where a corpus hit it (`:737-742, 772-777, 144-150,
197-203`). A unit fixture in the K2 model can agree with itself and be wrong against the class file; the rule now is
"JDK-typed behaviour is tested in run-kotlin", but nothing enforces it.

### 2.3 ⛔ Silent-wrong shapes — six found by one probe

The census (`PlaceholderCensus.kt:58-157`) counts `k2-` EmptyExpressions. It cannot see a conversion that is well
formed and wrong. A probe fixture written for this study (one class, 14 members) produced **one** placeholder and
these six wrong conversions [V, run 2026-09-26]:

| # | Kotlin | Converted to | Wrong how |
|---|---|---|---|
| S1 | `l.forEach { if (it > 2) return it }` (non-local return from an inline lambda) | `forEach(l, it -> { if (it>2) { return it; } })` | returns from the lambda; the source returns from the enclosing function. Prep/link then see a value flowing out of a `Function1`, not a method return. No guard in `rawStatement` (`KotlinBodyConverter.kt:1286`) |
| S2 | `val x by lazy { 5 }` (local delegated property) | `int x;` | initializer dropped: `statement.initializer` is null for a delegate (`:373-375`). Known since the 09-21 doc (§3 1.3), still open |
| S3 | `enum class E(val code: Int) { A(1), B(2) { override fun g() = 9 } }` | `A=<empty>`, `B=<empty>`; no subtype for `B` | constructor arguments dropped (`KotlinScan.kt:958`), entry bodies skipped (`:723-726`). Every enum with state loses it |
| S4 | `class Del(d: I) : I by d` | `f()` with an **empty body**; `getP()` absent | no forwarding call (`KotlinScan.kt:2174-2211`, `emptyBlock()` at `:2206`); the delegated property is not materialised at all. An empty source body is "modifies nothing" downstream (§5.3) |
| S5 | `var v = c; v++` where `C` declares `operator fun inc()` | `v++` (int increment) | user `inc()` never called (`:4096-4106`); the object is read as a number |
| S6 | `f(b = n++, a = n++)` | `f(this.n++, this.n++)` | named arguments are reordered into parameter order with no temporaries (`:3058-3100`); evaluation order inverted |
| — | `when (o) { is String, is Int -> 1 }` | `case int it -> 1` | multi-`is` arm keeps only the last pattern (`:1715-1717`) — a seventh, milder one |

S1 and S6 are wrong *modification* answers; S3 and S4 are wrong *structure*; S2 and S5 lose a read. None is in a
corpus pin because detekt/coil/javalin's authors happen not to write them where the verdict would move — that is
luck, not coverage. The probe, verbatim (parse with `KotlinScan`, print each member's `methodBody().statements()`
and each field's `initializer()`; census = 1, the `labeled` row):

```kotlin
interface I { fun f(): Int; val p: Int }
class Del(d: I) : I by d                                                        // S4
enum class E(val code: Int) { A(1), B(2) { override fun g() = 9 }; open fun g() = code }   // S3
class C(var n: Int) {
    operator fun inc(): C = C(n + 1)
    fun lazyLocal(): Int { val x by lazy { 5 }; return x }                      // S2
    fun nonLocal(l: List<Int>): Int { l.forEach { if (it > 2) return it }; return 0 }   // S1
    fun incUser(c: C): C { var v = c; v++; return v }                           // S5
    fun whenIs(o: Any): Int = when (o) { is String, is Int -> 1; else -> 0 }    // multi-is
    fun namedOrder(a: Int, b: Int): Int = a - b
    fun callNamed(): Int = namedOrder(b = n++, a = n++)                         // S6
}
```

### 2.4 What the census does not grade

- Reference recall (`ReferenceRecall.kt`, detekt report 2026-09-25): 70.8% EXACT, 21.7% DROPPED over 24,940
  references; IMPORT, ANNOTATION and NAMED_ARGUMENT sites 100% dropped, CALLABLE_REFERENCE 88.7%. This bounds
  *rename/locate* work, not analysis — but it is the only number the front end has for "did the CST keep the
  reference", and no test pins a percentage (`TestDetektReferenceRecall.java:46,64` asserts row count only).
- Placeholders built without a range (`k2-unary`, `k2-incr-target`, `k2-delegate-*`, `k2-property-initializer`)
  report line 0 (`KotlinBodyConverter.kt:4093-4096`, `KotlinScan.kt:1852-1934, 1248`).

## 3. Prepwork (`maddi-modification-prepwork`)

**Neutral, with three requirements the Kotlin front end must meet and one it meets by accident.**

- No `kotlin`, `Intrinsics`, `$default`, `Companion`, `INSTANCE` literal anywhere in prepwork/link/analyzer/common
  main [V, grep]. The Java names that exist: `java.lang` ECI filter (`ComputeCallGraph.java:686-687`, matched because
  `kotlin.Any`/`kotlin.Enum` map to `java.lang.*`), the `§m` AtomicBoolean marker (`Util.java:176-178`), synthetic
  `super()` skip (`MethodAnalyzer.java:465-467` — Kotlin never emits one).
- **Requirement 1 — every statement carries a source index**: read at `MethodAnalyzer.java:209, 223, 304-308, 407,
  503, 607, 625, 1295`. A missing one is an NPE that isolates the method; it happened on javalin's safe-assign
  (§7.71 of the gap doc). The front end pads and nests (`KotlinBodyConverter.kt:334-354, 437, 472-516`).
- **Requirement 2 — one CST node per position**: sharing throws "overwrite variableData"
  (`TestKotlinPrepFailures.java:30-33`); the elvis/safe-call lowerings convert twice on purpose.
- **Requirement 3 — `DetailedSources.putReference`** for references the CST drops (`ComputeCallGraph.java:395-410`):
  an extra contract Java does not need.
- **By accident:** synthesized getter/setter bodies use `noSource()` with a **null index** (`KotlinScan.kt:2039,
  2078`); `NO_SOURCE.index()` is null (`SourceImpl.java:27`). It survives today; nothing documents why [I].
- Evidence: 42 tests in `inspection-kotlin/prepwork` assert the **identical `VariableData` strings** the Java tests
  assert (`CommonKotlinPrep.kt:26-35`; 20 of 52 Java CommonTest classes ported, `prepwork-porting.md`). Only
  `doMethod` — synthesized accessors, `<init_0>`, `$default` bodies are not covered at this tier.

## 4. Link (`maddi-modification-link`)

**Neutral in code; one measured divergence in the path taken; zero in-module Kotlin tests.**

- `VirtualFieldComputer.java:106-110` gives `java.util.function` types no virtual fields; `kotlin.jvm.functions`
  is not excluded, so a `Function1` gets `§m` + hidden content like any custom SAM. `Util.needsVirtual()`
  (`prepwork/Util.java:388-397`) excludes all functional interfaces. Kotlin lambdas therefore take the *custom-FI*
  branch (`LinkMethodCall.java:142`, `LinkAppliedFunctionalInterface.java:63`), not the `java.util.function` one.
  `TestKotlinLambdaVsJavaLambda` measured the verdicts equal on higher-order, SAM-converted and local-function
  shapes — for those shapes.
- `isStandardFunctionalInterface()` (`ParameterizedType.java:200-208`): `java.util.function`, `Runnable`, or
  synthetic. `FunctionN` is none of these. Consequence is in §5.2.
- `LinkMethodCall.java:296-298` asserts a multi-dim vararg parameter name ends in `s` — convention, and a Kotlin
  `vararg rows: Array<Int>` named otherwise would trip it [I].
- `$default` methods are **instance** methods here where kotlinc makes them static (`KotlinScan.kt:2290-2295`);
  link has no special case, so they are analysed as ordinary code — which is the point.
- In-module tests: 426, all Java. Kotlin reaches link only through the 14 run-kotlin fixtures (§6).

## 5. Analyzer + archive (`maddi-modification-analyzer`, `maddi-aapi-*`)

### 5.1 The analyzer itself: three Java names, none load-bearing yet

No `kotlin` literal in 39 files [V]. Java-specific: `immutableCopyExpression` accepts only `List/Set/Map.copyOf|of`
(`TypeIndependentAnalyzerImpl.java:637-642`) — Kotlin's `listOf`/`toList` is not a recognised defensive copy;
`isPrimitiveStream` is `java.util.stream.*` only (`TypeEventualAnalyzerImpl.java:2490-2494`); `isJavaLangObject()`
hierarchy stops (neutral via the mapper). Kotlin-vs-Java verdict fixtures: 7 pairs, all agree (§6).

### 5.2 ⛔ The default for an uncontracted method is "modifies" — and the Kotlin archive is 1/16th of the JDK's

`ShallowMethodAnalyzer` (`LinkComputerImpl.java:279-281`): with no body and no annotation, a method **modifies its
receiver** (`:500-511`) and **modifies every non-trivial parameter** (`:440-465`); a parameter gets
IGNORE_MODIFICATIONS only if it is a *standard* functional interface (`:423-425`) — so a library `Function1`
parameter is *modified* where a `Consumer` is ignored [V]. The archive that overrides these defaults:

| Archive | Types | Method entries | **Annotated** |
|---|---|---|---|
| JDK (27 files) | 249 | 5,299 + 318 ctors | **4,877** |
| kotlin (4 files: Kotlin, KotlinCollections, KotlinJvmFunctions, KotlinText) | 14 | ~327 | **31** |

`Regex`, `Pair`, `Lazy`, `TuplesKt`, three `CollectionsKt` parts, `MapsKt`, `SetsKt`, `Function0-3` (type-level
only). Not contracted: `StringsKt` (every string extension), `Result`, `Sequence`/`SequencesKt`, `Unit`, `Ranges`,
`ArraysKt`, `Text`/`Char` extensions, coroutines. The rule `a partial archive is indistinguishable from none`
(`docs/kotlin-corpora.md:206`) is the repository's own, and this is a partial archive. Measured cost: the day
extension calls began resolving on detekt, 16 immutable verdicts went *down* (`kotlin-corpora.md:185-203`) —
resolution turned "unknown call" into "modifying call". detekt's `@Immutable ≥ 641` floor is set *with* the kotlin
archive loaded; the run-kotlin CLI loads **no archive unless `--preload-analysis-results-dirs` is passed**
(`RunMixedPrepAnalyzer.java:151-157`, `Main.java:173-175`).

Contract authoring (`AnalysisHintsParser`): Java shadow classes resolved against bytecode (`:128-146`). A Kotlin
type can be shadowed in its JVM shape; extension functions as statics on the multifile part class
(`KotlinCollections.java:33-53`). **No notion of a property** — a getter contract on a library Kotlin property does
not reach a Kotlin caller because the front end models it as a field (`Kotlin.java:62-75`); no `suspend` contract
exists and `Continuation` appears in none; method-level type parameters are a TODO (`:214`); an unresolved shadow
drops the whole unit and exits 0 (`kotlin-corpora.md:209-212`).

### 5.3 Empty bodies are optimistic

`explicitlyEmptyMethod()` → NON_MODIFYING, INDEPENDENT, parameters unmodified (`TypeModIndyAnalyzerImpl.java:
200-219`); an empty non-abstract source body links as zero statements (`LinkComputerImpl.java:233`). So every empty
body the Kotlin front end synthesizes is "modifies nothing": class-delegation forwarders (§2.3 S4), abstract-`var`
setters (`KotlinScan.kt:2031`), data-class `equals`/`hashCode`/`toString` (shared with Java records — an engine
decision, not a Kotlin one). Static synthetic `<static_0>` is *not* explicitly empty (`MethodInfoImpl.java:536-541`)
and is skipped instead — an asymmetry nothing tests.

### 5.4 Annotations written in Kotlin source reach the analyzer

`@NotModified`/`@Modified`/`@Independent`/`@Immutable(hc)` on Kotlin declarations are converted with use-site
targets (`AnnotationPlacementTest.kt:36-62`) and the verdicts equal Java's (`TestKotlinContractsVsJava.java:75-200`).
`@Container`/`@Final` untested; type-level `@Immutable` on source is guard-checked, not trusted, as in Java.

## 6. What the evidence is, exactly

Kotlin input reaches prep/link/analyzer through 103 tests, none inside those modules:

| Tier | Where | Count | What it proves |
|---|---|---|---|
| Prep oracle | `inspection-kotlin/prepwork` | 42 | identical `VariableData` strings to the Java tests, `doMethod` only |
| Prep smoke | `KotlinAnalyzerSmokeTest` | 9 | prep runs over lowered shapes and `doPrimaryTypes` |
| Verdict vs Java twin | `run-kotlin`: LoweredShapesVsJava (13 shapes + type row), LambdaVsJavaLambda (2), LazyVsJavaLazy (3), PredicatesVsJava, ContractsVsJava, CollectionHoldersVsJava | 11 | NON_MODIFYING / UNMODIFIED_PARAMETER / IMMUTABLE_TYPE equal to hand-written Java, each guarded by a zero-placeholder assertion and (mostly) a negative control |
| Cross-boundary | `TestMixedBoundaryVerdicts` | 1 | Java calling Kotlin sees Kotlin's modification |
| Persistence | `TestKotlinAnalysisRoundTrip` | 1 | write → fresh parse → load → same verdicts (needs a Java source set for FQN resolution) |
| Corpus | `TestDetektCorpus` (slow) | 1 | placeholders ≤8, isolated 0, `@Immutable` ≥641 with the kotlin archive; `TestCoilJvmSlice` is prep-only |
| Instruments | `-Dmaddi.verdictDump`, `-Dmaddi.placeholderDump`, `CensusRatchet` | — | diff two runs; never asserted on javalin |

The differential-oracle tier is the right instrument (it found a Java-engine defect — gap doc §7.18 — and a wrong
safe-call lowering). It has **13 lowered shapes**. §2.3 shows what one more fixture finds.

## 7. Around the analysis — re-verified against the 09-21 doc

| 09-21 item | Now | Evidence |
|---|---|---|
| §5.1 no encode path | **partial** — `--analysis-results-dir` writes via `LinkCodec`; incremental, rewire, `--analysis-results-target-dir`, `--updated-hints-dir` refused; daemon Java-only; no messages/SARIF for either language | `RunMixedPrepAnalyzer.java:185-217`, `Main.java:200-217` |
| §5.2 link has zero Kotlin tests | closed (from outside the module) | `TestKotlinLambdaVsJavaLambda` |
| §5.3 GetSetHelper `set` prefix | open but harmless: recognition is by body shape; Kotlin pre-sets `GET_SET_FIELD` | `GetSetHelper.java:92-166, 247-250`; `KotlinScan.kt:2049, 2069` |
| §5.4 stdlib archive 10/24 | now 14 types / 31 annotated — still partial (§5.2) | `libs/kotlin/*.json` |
| §5.5 IsolateClass writes `.java`; `print()` is Java | open; `cst-print-kotlin` wired nowhere in main (`inspection-kotlin` testImplementation only) | `IsolateClass.java:658`, `TypeInfoImpl.java:635-637` |
| §5.6 print-kotlin has no tests | 12 tests exist, in `inspection-kotlin`; still no `src/test` | `TestKotlinPrinter*.kt` |
| §6 corpus tests skip silently | closed for Kotlin (`maddi.corpus.required`, `slowTest` sets it); ~10 Java corpus tests still bare `assumeTrue` | `TestOssCorpus.java:50, 113-124`; `AGENTS.md:32` |
| §6 no Kotlin ratchet | `CensusRatchet` ±5 on detekt/coil; no TSV baselines; javalin unpinned | `CensusRatchet.java:38-60` |
| §6 stale docs | **still stale**: `kotlin-stdlib-extension-facades.md:3` ("parked"), `kotlin-parser-plan.md:478` ("Next: M2"), `kotlin-corpora.md:61-73` (676/8 isolated), `mixed-language-integration.md:171` ("in progress"), `RunMixedPrepAnalyzer.java:58` ("stops after prep"), `Main.java:54-59` (lists results-dir as refused), `README:95-96` (writing results "refused"), `README:99`/`PUBLISHING.md:124`/`release-cli.sh:7` (`lib/` vs actual `lib-k2/`) | quoted lines |
| §6 issues #32–#48 | no record in repo | — |
| Entry points | CLI: one command line (`maddi-kotlin` ⊇ `maddi`), two pipelines inside; Gradle/Maven plugins **refuse** (or `skipKotlinSources` with WARN); daemon reports and analyzes Java only; IntelliJ/VS Code/Eclipse Java-only | `AnalyzerWorkAction.java:21,51`, `DetectKotlinSources.java:125-138`, `WarmAnalysisService.java:63-74`, `plugin.xml:18-24` |
| CLI non-exit | `main` returns without `System.exit` on success; a non-daemon K2/IntelliJ thread keeps the JVM alive; no `close()` of the K2 session found on the mixed path [I]; undocumented | `Main.java:76-82`, `KotlinProjectScan.kt:92-97` |

## 8. How far, in one table

"Parity" = the same input shape gets the same verdict through the same code, with the same evidence standard.

| Layer | Structural parity | Evidence parity | What closes the gap |
|---|---|---|---|
| Front end | ~90% of syntax by construct count; **unknown** silent-wrong residue (6 found in one probe) | census pinned on 2 corpora; no pin on wrong-shape | a *silent-wrong* hunt: differential fixtures per construct family, not per corpus site (§2.3 is the first batch) |
| Prepwork | full — neutral code, 3 stated requirements | 42 oracle tests, `doMethod` only | port the remaining 32 CommonTest classes; a `doPrimaryTypes` tier for synthesized members |
| Link | full in code; FunctionN takes the custom-FI path | 0 in-module; 14 fixtures outside | Kotlin-input link tests for the custom-FI path (VirtualField, applied-FI) — or decide `kotlin.jvm.functions` is standard |
| Analyzer | full in code (3 Java names) | 7 twin fixtures, 1 corpus floor | `listOf`/`toList` as immutable copies; `Sequence` as a stream |
| **Knowledge** | **1/16th** | detekt floor only | contract `StringsKt`, `SequencesKt`, `Result`, `ArraysKt`, coroutines; a property notion in `AnalysisHintsParser`; load `libs/kotlin` by default in the mixed CLI |
| Persistence | write/read yes; incremental/daemon no | 1 round trip | fingerprint/rewire for Kotlin |
| Reproducers / print | none for Kotlin | — | `IsolateClass` via `KotlinTypePrinter` |
| Entry points | CLI yes; plugins refuse; IDEs no | 7 one-entry-point tests | plugin → mixed pipeline (85 MB bundle question) |

**The shortest path to a defensible "Java and Kotlin":** (1) fix §2.3's six and make the silent-wrong hunt a
standing fixture family; (2) contract the stdlib to the point where an uncontracted Kotlin call is rare in a real
project, and load that archive by default; (3) route the Gradle plugin to the mixed pipeline. Everything else is
already structurally there and needs evidence, not code.

## 9. Not done here

- The six §2.3 shapes are found, not fixed. They belong in GitHub issues (public repo; one per shape).
- javalin's numbers remain doc-only; a third corpus pin needs a copy this repo owns.
- Nothing above was re-measured on `devel`; the worktree is `ws/python` at `32ff9cb92`.
