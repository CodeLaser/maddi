# Calling the openjdk `JavaInspector`

How to drive the javac-based `JavaInspector` (`maddi-inspection-openjdk`, `JavaInspectorImpl`) from code — the
setup sequence, the parse/load entry points, and the stability gotchas you must know before relying on it.

## Minimal call sequence

```java
// a compiled library to have on the classpath (its jar is located from the class)
SourceSet slf4j = SourceSetImpl.sourceSetOf(Logger.class);

JavaInspector javaInspector = new JavaInspectorImpl();
javaInspector.preload("java.base::java.util.");                 // optional: eagerly load whole packages

InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
        .addClassPathParts(SourceSetImpl.javaBase(), slf4j)     // platform + libraries (compiled types)
        .addSourceSets(SourceSetImpl.testProtocolSourceSet())   // where parse(fqn, source) snippets live
        .build();
javaInspector.initialize(inputConfiguration);
javaInspector.onlyPreload();                                    // run the configured preloads
```

(This is the shape used by `TestPreload` and the shared test `CommonTest`s.)

## Parsing source

- **A single snippet — always give the fully-qualified name:**
  ```java
  TypeInfo x = javaInspector.parse("a.b.X", "package a.b; class X { }");
  TypeInfo y = javaInspector.parse("a.b.Y", src, parseOptions);          // with ParseOptions
  ```
  ⚠️ **`javaInspector.parse(String input)` (no fqn) is a stub** in the openjdk impl — it throws
  `UnsupportedOperationException("Add fqn!")`. Use the `parse(fqn, input[, options])` overloads. The fqn is the
  primary type's FQN, derivable from the `package` + first top-level type in the source.

- **Many sources / a whole run:** `Summary parse(Map<String,String> sourcesByFqn, ParseOptions parseOptions)`
  (returns a `Summary`; `parseResult()` for the `ParseResult`). `ParseOptions.Builder()` knobs of note:
  `setFailFast(false)` tolerates javac *semantic* errors (missing return, undeclared throws) and still yields a
  parse tree; `setDetailedSources(true)` records line/col `Source` on elements.

## Loading a compiled (classpath / JDK) type on demand

```java
TypeInfo mdc = javaInspector.compiledTypesManager().getOrLoad("org.slf4j.MDC", sourceSetOfRequest /*or null*/);
```
`getOrLoad` completes the type's members from the classfile (methods, fields, **constructors**) and caches it.

---

## Stability & gotchas

### 1. Not concurrency-safe — parallel test execution is flaky

The inspector is backed by a **javac task**, which is **not safe for concurrent use**. Running many
inspector-backed tests in parallel — JUnit parallel execution, a suite with several `maxParallelForks`, or a test
that itself fans out (e.g. `TestCloneBench`'s parallel workers) — intermittently throws:

```
java.lang.IllegalStateException: java.lang.NullPointerException:
  Cannot invoke "com.sun.tools.javac.code.Scope$StarImportScope.isFilled()" because "tree.starImportScope" is null
```
and, less often, `org.e2immu.language.java.openjdk.CompilationProblems`.

Observed in **`maddi-modification-analyzer`** (2026-07-13): the *full* suite fails with a **small, different set
of tests each run** (e.g. one run `TestNeedMethodReturnTypeInHCT`, `TestHCSConstructor`,
`TestTypeParameterChoices`, `TestCloneBench`; another `TestVarious`), while those same tests **pass in
isolation**. It reproduces **with and without** unrelated code changes, so it is a pre-existing
parallel-execution stability issue, not a deterministic regression from any single change. Do not treat an
occasional red in that suite as a real failure without re-running the specific test alone.

Guidance: keep a single `JavaInspector` (and its javac task) to one thread; run inspector-heavy suites
serially / single-fork if you need determinism. A proper fix is a parallel-mode audit of the shared javac
usage (related: the shared-`MethodAnalyzer` / non-atomic-counter notes in `docs/prep-analyzer hardening.md` §8).

### 2. Tests need the javac `--add-exports` JVM flags

The front end reaches into `com.sun.tools.javac.*`. Test JVMs must pass:
```
--add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED
--add-exports jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED
--add-exports jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED
--add-exports jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED
--add-exports jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED
```
Without them you get `IllegalAccessError: ... com.sun.tools.javac.api.BasicJavacTask ... does not export ... to unnamed module`.

### 3. JDK platform types: no private members (`ct.sym`)

javac resolves JDK (`java.base`, …) types from the stripped **`ct.sym`** symbol file, which contains only the
public/protected API — **no private members at all**. So a JDK type's private constructor/method is *not*
recoverable (`java.lang.Math`'s private constructor is absent from `getAllMembers`, `getEnclosedElements` and
the internal member scope). **Regular classpath JAR** classfiles *do* carry private members, so those load in
full — including private constructors (see `TestPrivateConstructor`), which analyzed-package decode relies on.

### 4. `BigInteger` / `BigDecimal` fail to load through the module-based platform

`getOrLoad(BigInteger.class)` returns `null` when `java.base` is registered as a **module** source set, because
their `jdk.internal.*` deps are encapsulated; loading `java.base` as a **classpath** jmod avoids it. Full
analysis in `maddi-inspection-openjdk/module-vs-classpath jdk.internal loading.md`.
