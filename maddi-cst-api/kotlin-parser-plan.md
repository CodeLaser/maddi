# Building a Kotlin front-end for the CST — implementation plan

*Plan date: 2026-06-27. Goal: a third source-language front-end (after the congocc and javac Java
parsers) that turns Kotlin source into the shared CST, using the **Kotlin K2 Analysis API** as the
resolved source of truth, modelled on `maddi-java-openjdk` + `maddi-inspection-openjdk`.*

Companion document: `kotlin-cst-assessment.md` (what the CST API itself must grow to host Kotlin).

> **Workspace note:** this Kotlin effort lives in the separate clone `~/git/maddi-kotlin` on branch
> `kotlin`, to avoid colliding with concurrent work in `~/git/maddi`.

---

## 1. Decisions taken

| Question | Decision |
|---|---|
| Compiler surface | **K2 Analysis API** (`analyze {}` / `KaSession`, `KaSymbol`, `KaType`). Resolved symbols + types + nullability — the analogue of javac's `Trees`/`Types`/`Elements`. |
| First milestone | **Walking skeleton**: new module compiles, takes the compiler dependency, converts `class Foo { fun bar(): Int = 1 }` into a CST `TypeInfo`/`MethodInfo` end-to-end. |

## 2. Decisions settled (2026-06-27)

- **D1 — Bridge language is Kotlin, not Java. ✅ ACCEPTED.** The Analysis API entry point
  `analyze(element) { … }` is an inline function with a `KaSession` receiver; not realistically callable
  from Java. The scanner module `maddi-kotlin-k2` is written in **Kotlin**; the build gains the Kotlin
  Gradle plugin.
- **D2 — Module split. ✅ START COLLAPSED.** One module `maddi-kotlin-k2` for now; extract
  `maddi-inspection-kotlin` once the driver grows (M5).
- **D3 — Kotlin version. ✅ LATEST STABLE = 2.4.0.** Pinned, with its matching Standalone Analysis API
  artifacts (verified in M0).
- **D4 — Bazel. ✅ GRADLE-ONLY for the skeleton.** Add `rules_kotlin` once the module stabilises.

## 3. Architecture (target)

Both Java front-ends produce the same CST behind the same `JavaInspector` contract. The Kotlin
front-end becomes a third producer:

```
                         maddi-cst-api / -impl   (shared tree; grows per kotlin-cst-assessment.md)
                                  ▲
        ┌─────────────────────────┼─────────────────────────┐
   congocc (java-parser)     javac (java-openjdk)       K2 (kotlin-k2)        ← NEW
   inspection-integration    inspection-openjdk         inspection-kotlin     ← NEW (later; D2)
```

Mirror of the javac front-end:

| Java (javac) | Kotlin (K2) | Responsibility |
|---|---|---|
| `maddi-java-openjdk` (`ScanCompilationUnit`, `ClassSymbolScanner`, `ConvertType`, `FlagHelper`) | `maddi-kotlin-k2` | Walk the resolved compiler model → call `runtime.new…` CST factories |
| `maddi-inspection-openjdk` (`JavaInspectorImpl`, `CompiledTypesManagerImpl`) | `maddi-inspection-kotlin` | Build the standalone session, drive the scan, implement `JavaInspector` |

**Key lesson borrowed from java-openjdk:** it is a *hybrid* — javac for semantics, plus the congocc
parser re-run purely to recover surface syntax (comments, keyword offsets, `detailedSources`) that the
compiler discards. The Analysis API keeps **PSI (`KtFile`/`KtElement`) available alongside resolved
symbols**, so for Kotlin we get both from one session — no second parser needed. Use `KaSymbol` for
semantics and the symbol's `psi`/`KtElement` for source positions & detailed sources.

## 4. The factory contract the bridge calls (grounded in ScanCompilationUnit)

The bridge never constructs CST nodes directly; it calls `Runtime` factories and commits builders:

```
TypeInfo ti = runtime.newTypeInfo(compilationUnit, simpleName);
ti.builder().setTypeNature(runtime.typeNatureClass())
            .setParentClass(runtime.objectParameterizedType())
            .setAccess(runtime.accessPublic())
            .addMethod(md)
            .commit();
MethodInfo md = runtime.newMethod(ti, "bar", methodType);
md.builder().setReturnType(intPt).setMethodBody(block).addMethodModifier(...).commit();
```

So each Kotlin concept becomes "find the right `runtime.new…` / `…Modifier…` call". Where the assessment
doc identifies a gap (e.g. `variance()`, extension receiver, `isSuspend()`, `PropertyInfo`), the CST API
must grow **first**, then the bridge calls the new factory.

## 5. Milestones

**M0 — De-risk the dependency (D3). ✅ DONE — see §8.** The standalone Analysis API resolves, the spike
compiles, and the session bootstraps. Approach confirmed viable.

**M1 — Walking skeleton. ✅ DONE.** `KotlinScan` (`maddi-kotlin-k2/src/main/kotlin`) builds a standalone
session, walks each top-level class symbol and its declared function symbols, and emits a committed CST
`TypeInfo`/`MethodInfo` via the `runtime.new…` factories. `KotlinScanTest` proves
`class Foo { fun bar(): Int = 1 }` → a `TypeInfo("Foo")` with `findUniqueMethod("bar",0)` whose
`returnType()` is the CST `int`. Method bodies are a stub empty block (deferred to M3); parameters,
visibility, generics deferred to M2/M4. Commit lifecycle mirrors java-openjdk: per-method
`builder().commitParameters().commit()`, then `type.builder()…commit()`, then `cu.setTypes(...)`.

**M2 — Signatures & the type layer.** *(M2 signatures + M2a nullability ✅ DONE; M2b variance + generics
+ class-type resolution remain.)* Real `ParameterizedType` conversion: parameters, return types,
generics, `java.lang`↔Kotlin builtin mapping (`kotlin.Int`→`int`, `kotlin.String`→`String`).
- **M2a nullability — DONE.** Added `NullableState {UNSPECIFIED, NONNULL, NULLABLE}` to `cst-api` and a
  `nullable()` / `withNullable()` dimension on `ParameterizedType` (default `UNSPECIFIED`, default
  interface methods so other impls stay source-compatible). `ParameterizedTypeImpl` gets a 6-arg
  canonical ctor (the 5-arg delegates → all existing call sites unchanged), folds `nullable` into
  `equals` **but not `hashCode`** (equal→same-hash contract only; keeps hash-ordered collections across
  the analyzer byte-stable). `fullyQualifiedName()` stays nullability-free (semantic key). The bridge
  tags `T?` as `NULLABLE` and boxes nullable primitives via `ensureBoxed`; non-null stays UNSPECIFIED so
  it remains equal to predefined/Java types. Verified no regression in prepwork/inspection-openjdk/
  java-openjdk/cst-impl. (Pre-existing failures in modification-analyzer/-link are unrelated.)
- **M2b variance** (`out T`/`in T` on `TypeParameter`) and full generics/class-type resolution (needs a
  CompiledTypesManager, M5) still to do.

**M3 — Member bodies.** Statements & expressions. Per the assessment these mostly *shoehorn* onto
existing nodes; build a `ConvertExpression`/`ConvertStatement` pass for the bodies, operators-as-
`MethodCall`, `when`→switch, etc.

**M4 — Kotlin-specific info.** `PropertyInfo`, primary constructors, extension receiver, `suspend`,
`object`/`data`/`companion`, `internal` access, default parameter values — each gated on its CST API
addition from the assessment doc (priority order ranked there).

**M5 — Driver & integration.** Extract `maddi-inspection-kotlin` implementing `JavaInspector` (D2):
multi-file, classpath/`CompiledTypesManager`, preloading, round-trip print via `print2`.

## 6. First test to write (M1 acceptance)

```kotlin
// input
class Foo { fun bar(): Int = 1 }
```
```java
// assertion (sketch, mirrors java-openjdk TestClass1)
TypeInfo foo = kotlinScan.parse("Foo", source);
assertEquals("Foo", foo.simpleName());
assertEquals(1, foo.methods().size());
MethodInfo bar = foo.methods().getFirst();
assertEquals("bar", bar.name());
assertEquals("int", bar.returnType().fullyQualifiedName()); // or boxed Integer — TBD in M2
```

## 7. Risks

- **R1 (high) → RETIRED in M0.** Standalone Analysis API artifact availability resolved; jars download.
- **R2 (med):** Kotlin plugin / JDK target alignment — daemon runs JDK 26; Kotlin 2.4 caps target at
  JVM 25 (a warning, builds fine). No toolchain forced.
- **R3 (med):** Kotlin↔Java builtin type mapping (`Int`↔`Integer`, `Unit`↔`void`, `Nothing`,
  flexible/platform types from Java interop).
- **R4 (low, deferred):** Bazel parity (D4).

## 8. M0 results (verified 2026-06-27)

- **Latest stable Kotlin = 2.4.0.**
- **Hosting:** the standalone Analysis API ships as `*-for-ide` artifacts on
  `https://packages.jetbrains.team/maven/p/ij/intellij-dependencies`, **not** Maven Central. Added to
  `settings.gradle.kts` centrally (the project uses `FAIL_ON_PROJECT_REPOS`).
- **Verified artifact set @ 2.4.0** (transitivity disabled; the FIR impl was renamed):
  `analysis-api-for-ide`, `analysis-api-k2-for-ide` *(was `high-level-api-fir-for-ide`)*,
  `analysis-api-impl-base-for-ide`, `low-level-api-fir-for-ide`,
  `analysis-api-platform-interface-for-ide`, `symbol-light-classes-for-ide`,
  `analysis-api-standalone-for-ide`, plus `kotlin-compiler` (Maven Central).
- **Standalone-runtime extras** (the `*-for-ide` jars are stripped; discovered by walking
  `NoClassDefFoundError`): `org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0`,
  `com.github.ben-manes.caffeine:caffeine:3.1.8`, and the **IntelliJ-patched coroutines**
  `org.jetbrains.intellij.deps.kotlinx:kotlinx-coroutines-core:1.10.2-intellij-1` (provides
  `kotlinx.coroutines.internal.intellij.IntellijCoroutines`, absent upstream; cf. detekt#9176). The
  upstream coroutines that `kotlin-compiler` drags in is substituted out via
  `resolutionStrategy.dependencySubstitution` so only one set of `kotlinx.coroutines.*` is present.
  That was the **complete** chain — no fastutil/trove needed.
- **API surface:** the spike compiled against `buildStandaloneAnalysisAPISession`,
  `buildKtModuleProvider`/`buildKtSourceModule`, `analyze {}`, `KaClassSymbol`,
  `KaNamedFunctionSymbol` — all present. `buildKtModuleProvider` is a *member* of the session builder
  (not a top-level import).
- **Session bootstraps, runs, and resolves — green.** `StandaloneApiSpike.resolvesSimpleClass()` passes:
  it builds a standalone session over an in-memory `class Foo { fun bar(): Int = 1 }`, calls
  `analyze {}`, and asserts the resolved class symbol (`Foo`), its function (`bar`), and return type
  (`kotlin/Int`). End-to-end proof that the K2 Analysis API is usable as the CST source of truth.

---

*Next action: M2 — real signatures & the type layer. Parameters (`ParameterInfo`), proper return/param
type conversion with the Kotlin↔Java builtin mapping (`kotlin/Int`→int, `kotlin/Unit`→void,
`kotlin/String`→String), generics, then the assessment's type-layer gaps (nullability, variance).*
