# Mixed-language integration: Java (openjdk) + Kotlin (K2) over a shared core

This document describes how the **openjdk (javac) Java front-end** and the **Kotlin (K2) front-end** are
being integrated so that a project can mix Java and Kotlin source that cross-reference each other, over **one
shared JDK/library core**. It covers the design, the two phases that are implemented (Phase 1 and Phase 2),
and the two that are not yet (Phase 3 and Phase 4).

> Scope note. maddi has *two* Java front-ends: the homegrown CongoCC parser (`maddi-inspection-integration`)
> and the javac-based one (`maddi-java-openjdk` / `maddi-inspection-openjdk`). This integration targets the
> **openjdk** one only.

## 1. Goal

Given a project with both `.java` and `.kt` sources:

- A Kotlin declaration may reference a Java type, and a Java declaration may reference a Kotlin type.
- Every `java.*` / classpath type, and every cross-language *source* type, must be a **single `TypeInfo`
  instance** shared by both front-ends. If a Java method and a Kotlin method both mention `java.util.List`
  (or a shared project type), they must link against the *same* CST node — otherwise the modification /
  immutability / linking analysis cannot reason across the boundary.

The output of both front-ends is the same **CST** (`TypeInfo` / `MethodInfo` / `FieldInfo` / …), built with
the same `Runtime`. Integration is therefore about **identity**: which front-end builds a given type, and how
the other reuses it.

## 2. The substrate (already in place before this work)

Three pieces make the shared model possible; all are injectable:

| Piece | Where | Role |
|---|---|---|
| `Runtime` | `maddi-cst-api` (impl `RuntimeImpl`, openjdk uses `RuntimeWithCompiledTypesManager`) | The CST factory. Both front-ends must use the **same** instance so CST nodes are comparable. |
| `InfoByFqn` | `maddi-inspection-resource` | The shared type registry, keyed by `(FQN, SourceSet)`, single-instance per key, with **distance-based resolution** over a `SourceSet` dependency graph. |
| `CompiledTypesManager` | `maddi-inspection-api`; openjdk impl `CompiledTypesManagerImpl` | The library/compiled-type manager. Exposed on `JavaInspector` via `runtime()` and `compiledTypesManager()`. |

`SourceSet` (`maddi-cst-api`) has a **dependency graph** (`dependencies()`) and an `externalLibrary()` flag.
`InfoByFqn` uses the graph to pick the nearest type when an FQN exists in several source sets.

**Source-set modelling for a mixed module.** A mixed module is modelled as **one source set** populated by
both front-ends. FQNs are unique across a module (a Java class and a Kotlin class cannot share an FQN), so
`(FQN, "main")` is unambiguous — and this avoids a dependency cycle that mutual Java↔Kotlin source sets would
create. The graph only needs to distinguish `source → library → jdk` (a clean DAG).

## 3. The two hard problems

1. **The JDK/library core was loaded twice by two authorities.** The Java front-end loads `java.*` from
   **bytecode** (`ClassSymbolScanner`); the Kotlin front-end loaded them from **K2 symbols**. Two loaders →
   risk of divergent shapes for the same `java.util.List`.
2. **Compiler asymmetry.** K2 *can* read Java sources; javac *cannot* read Kotlin. So the two directions of
   the boundary need different bridges.

## 4. The four phases

| Phase | Delivers | Status |
|---|---|---|
| 1 | Common JDK/library core (no source interop yet) | **done** |
| 2 | Kotlin → Java source references | **done** |
| 3 | Java → Kotlin source references (via generated Java stubs for javac) | **in progress** (stub generator built + de-risked) |
| 4 | A unified `MixedInspector` orchestrating both front-ends | not started |

---

## 5. Phase 1 — a shared JDK/library core

**Goal.** `java.*` and classpath types resolve to one **bytecode-authoritative** `TypeInfo` instance shared
by both front-ends. The Java (openjdk) front-end owns loading; the Kotlin front-end reuses.

### 5.1 The Kotlin delegation

`KotlinScan` and `KotlinTypeMapper` (in `maddi-kotlin-k2`) take an optional `CompiledTypesManager? = null`.
When a driver injects one, `KotlinTypeMapper.mapClassType` — *before* its K2-based library load — delegates:

```kotlin
val typeInfo = infoByFqn.getType(kotlinFqn, sourceSet) ?: run {
    val jvmFqn = mapToJvmFqn(type.classId)                 // kotlin.collections.List -> java.util.List
    compiledTypesManager?.getOrLoad(jvmFqn, librarySourceSet)?.also { infoByFqn.put(jvmFqn, it, librarySourceSet) }
        ?: <existing K2-based library load>                // fallback: standalone, or a Kotlin-only stdlib type
}
```

So the shape of `java.*` comes from bytecode (matching the Java front-end and the AAPI), not from K2's
read-only view. When no manager is injected (standalone parsing), the original K2 path runs unchanged, so
existing behaviour and tests are unaffected. K2 is still used to obtain the FQN and type arguments; only the
`TypeInfo` is taken from the shared manager.

### 5.2 Lazy on-demand loading in the openjdk manager

`CompiledTypesManagerImpl` (openjdk) was a flat pre-loaded map — `getOrLoad` returned `null` on a miss, so
the Kotlin front-end would only share types the Java scan had already touched. It now lazily loads a single
type from bytecode on demand. The change is **entirely additive** (the scan flow is unchanged), across three
files:

1. **`ScanCompilationUnits.loadCompiledTypeOrNull(fqn)`** (`maddi-java-openjdk`) — loads one compiled type by
   canonical FQN using the scan's still-live javac task, mirroring the per-type body of `preload()`:
   ```
   task.getElements().getTypeElement(fqn) -> cs.complete()
     -> classSymbolScanner.lazilyLoadPrimaryTypeFromClassFile(cs)
     -> loadType(cs, pt, LOAD_MEMBERS) -> pt.builder().commit()
   ```
   This works because a `JavacTask`'s `Elements` resolves and *completes* classpath symbols on demand, and the
   task is **not closed** after `scan()` — the existing preload already calls `getTypeElement` after
   `analyze()`.
2. **`JavaInspectorImpl`** (openjdk) retains the most recent `ScanCompilationUnits` (`lastScanUnits`, set in
   `singleSourceSet`) so its live task stays usable; it exposes a `loadCompiledTypeOrNull` and injects it into
   the manager in `initialize`: `ctm.setLazyLoader(this::loadCompiledTypeOrNull)`.
3. **`CompiledTypesManagerImpl`** gets a **javac-free** `Function<String, TypeInfo> lazyLoader` — satisfying
   its documented "should not retain any references to OpenJDK structures" constraint — and a `getOrLoad`
   override: a miss calls the loader, then caches the result via `addTypeInfo`.

All javac use is **single-threaded** (javac's name table is per-`Context`; `createTask` passes
`-XDuseUnsharedTable=true`). The lazy loader must be called synchronously, which is how the Kotlin front-end
uses it (during its own single-threaded scan).

### 5.3 Tests (`maddi-inspection-kotlin`, `TestSharedJdkCore`)

- `javaAndKotlinShareJavaUtilList` — the openjdk inspector preloads `java.util`; a Kotlin `List<String>` and
  the Java `java.util.List` resolve to the **same** `TypeInfo` (`assertSame`).
- `kotlinTriggersLazyLoadOfANonPreloadedJavaType` — a Kotlin reference to `java.time.LocalDate` (deliberately
  *not* preloaded) triggers an on-demand bytecode load through the shared manager, then is cached as the same
  instance.

The openjdk test JVM needs `--add-exports jdk.compiler/com.sun.tools.javac.*=ALL-UNNAMED` (added to
`maddi-inspection-kotlin/build.gradle.kts`, mirroring the openjdk module).

---

## 6. Phase 2 — Kotlin resolving Java source types

**Goal.** A Kotlin declaration references a Java **source** class (defined in a `.java` file, not compiled),
and resolves it to the same shared `TypeInfo` the openjdk front-end builds.

### 6.1 Why it was small

`JavaInspectorImpl.singleSourceSet` **commits parsed source primary types to the `CompiledTypesManager`**
(`compiledTypesManager.addTypeInfo(...)`), not only bytecode types. So after the Java front-end parses
`a.b.Foo`, the type is already in the shared manager — and the Kotlin front-end reuses it through the **exact
same Phase-1 delegation** (`mapClassType` → `getOrLoad("a.b.Foo")` → the committed `Foo`). No new sharing
plumbing, and no need to inject `InfoByFqn` into the openjdk parser (its registry is private; the manager is
the seam).

### 6.2 The only new work: K2 must resolve the Java type

For the Kotlin front-end to *get a symbol* for `Foo`, K2 must resolve it — which, for a source-only type,
means K2 has to read `Foo.java`. `KotlinScan` gained an overload:

```kotlin
fun parse(filesByName: Map<String, String>, javaFilesByName: Map<String, String>): List<TypeInfo>
```

It lays the `.java` files into the **same** K2 source root as the `.kt` files. K2's `addSourceRoot` already
includes `.java` files, and the K2 resolver handles a mixed Java+Kotlin module. Only the `KtFile`s are
converted to CST; the `.java` files exist purely for K2's resolution — their `TypeInfo` comes from the shared
registry / manager, never a K2 rebuild.

Note the Java source is parsed twice by design: once by javac (authoritative `TypeInfo`) and once, superficially,
by K2 (only to obtain the FQN of the reference). This is analogous to how `kotlinc` reads Java sources for
resolution while `javac` produces the classes.

### 6.3 Tests

- `MixedSourceTest.k2ResolvesAJavaSourceType` (`maddi-kotlin-k2`, standalone) — de-risks that K2 resolves a
  Java source type at all: a Kotlin `fun f(foo: Foo)` with `a/b/Foo.java` in the source root resolves the
  parameter to `a.b.Foo` (not `Object`/a placeholder).
- `TestMixedSourceReference` (`maddi-inspection-kotlin`) — the full path: the openjdk inspector
  `parse("a.b.Foo", src)` (committed to the shared manager); a Kotlin `fun f(foo: Foo): Int = foo.x` sharing
  the runtime + manager; `assertSame(javaFoo, kotlinFoo)`. The Kotlin body even reads the Java field `foo.x`
  through the shared type.

---

## 7. Phase 3 — Java resolving Kotlin source types (in progress)

This is the harder direction because **javac cannot read Kotlin**. The plan:

- A cross-language **skeleton pass A** registers `TypeInfo` skeletons for all source types of both languages
  in the shared `InfoByFqn`.
- Generate **signature-only Java stubs** from those Kotlin types and hand them to javac, so javac can resolve
  its own trees. The stubs are throwaway scaffolding — the *CST* still comes from the shared registry,
  because `ClassSymbolScanner.classTypeInfo()` checks the registry first and finds the real Kotlin `TypeInfo`.

### 7.1 Stub generator (built + de-risked)

`JavaStubGenerator.stub(typeInfo)` (`maddi-inspection-kotlin`) emits a signature-only Java source for any CST
`TypeInfo`: package, type declaration (class/interface, type parameters + bounds, `extends`/`implements`),
fields, constructors, methods. Bodies `throw` (so nothing runs); type references are **erased** (raw, no
generic arguments) so a reference resolves without pulling in transitive stubs; members are `public`
(over-exposing does not break resolution — the real access lives in the CST).

**De-risk (`TestJavaStub`):** the make-or-break question — *does a stub generated from a Kotlin type actually
satisfy javac?* — is answered yes. A Kotlin `Widget` is parsed, its stub generated, and javac **attributes**
(parse + `JavacTask.analyze()`, no code-gen) a Java `UseWidget` that constructs `Widget`, reads its
field/getter and calls its methods — against the stub only, with **zero errors**.

### 7.2 Remaining wiring (part of Phase 4)

Two pieces remain for the end-to-end Java→Kotlin flow, both belonging to the unified driver:

1. **Deliver the stubs to javac** — either as compiled `.class` on the classpath, or as source that the
   openjdk scan is told *not* to convert (so it does not build a duplicate `TypeInfo` from the stub).
2. **The openjdk front-end reuses the shared Kotlin type** — its resolution must find the registered Kotlin
   `TypeInfo` (via the shared `InfoByFqn`/`CompiledTypesManager`) rather than the stub. `classTypeInfo()`
   already checks the registry first; the open item is sharing the registry (the openjdk `InfoByFqn` is
   currently private — the `CompiledTypesManager` seam, as used in Phase 2, is the likely path since the
   Kotlin front-end can publish its types there).

Open questions: stub fidelity (signatures + generic bounds suffice; Java cannot call Kotlin
default-arg/`$default` shapes, so those need not be visible); and nullability / declaration-site variance /
platform types across the boundary — more a modification/linking concern than a parsing one.

## 8. Phase 4 — a unified driver (not started)

A `MixedInspector` that orchestrates: pass A (register skeletons for both languages) → generate Java stubs →
pass B (each front-end fills members for its own language, reusing `InfoByFqn` for cross-language references).
Order is Kotlin-aware-of-Java and Java-aware-of-Kotlin-via-stubs, mirroring real `kotlinc` + `javac` interop.

## 9. Design notes / invariants

- **The `CompiledTypesManager` is the sharing seam.** It is exposed on `JavaInspector` and already holds both
  compiled and parsed-source types. It stays javac-free (lazy loading is a callback).
- **One `Runtime`, one `CompiledTypesManager`** across both front-ends. `JavaInspector.runtime()` (an
  openjdk `RuntimeWithCompiledTypesManager`) works fine as `KotlinScan`'s runtime.
- **Identity, not shape, is the contract.** Both front-ends produce the same CST; the integration only has to
  ensure a given conceptual type is one instance. Bytecode is the authority for library shape (deterministic,
  matches the AAPI); source parsing is the authority for each language's own source types.
- **Single-threaded javac.** Any on-demand load must run synchronously on the calling thread.

## 10. File index

| Concern | File |
|---|---|
| Kotlin delegation to the manager | `maddi-kotlin-k2/.../KotlinTypeMapper.kt` (`mapClassType`), `KotlinScan.kt` (ctor param, `parse` overload) |
| Lazy single-type load | `maddi-java-openjdk/.../ScanCompilationUnits.java` (`loadCompiledTypeOrNull`) |
| Loader wiring + retained task | `maddi-inspection-openjdk/.../JavaInspectorImpl.java` (`lastScanUnits`, `loadCompiledTypeOrNull`, `initialize`) |
| `getOrLoad` override + callback | `maddi-inspection-openjdk/.../CompiledTypesManagerImpl.java` |
| Phase 1 tests | `maddi-inspection-kotlin/.../TestSharedJdkCore.kt` |
| Phase 2 tests | `maddi-inspection-kotlin/.../TestMixedSourceReference.kt`, `maddi-kotlin-k2/.../MixedSourceTest.kt` |
| Phase 3 stub generator + de-risk | `maddi-inspection-kotlin/.../JavaStubGenerator.kt`, `.../TestJavaStub.kt` |
