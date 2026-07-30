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

### 4. A source set is resolved through the **compiled** form of the sets it depends on

javac has no view of maddi's CST. When it type-checks `test`, every reference into `main` is resolved from what
`main` looks like on javac's class path — `JavaInspectorImpl.createTask` puts each non-external dependency's
`SourceSet.uri()` there. So a source set's dependencies must be *findable*, and what "findable" means is either:

- a **class file** in that entry (`build/classes/java/main/a/b/Base.class`), or
- a **source file** in it: javac's class path doubles as its source path, so `a/b/Base.java` found there is
  compiled implicitly. Both build plugins rely on this without saying so — `ComputeSourceSets` sets a source
  set's `uri()` to its first *source* directory, never to a class output. It works, at the cost of re-parsing
  the dependency's sources once per dependent set, and it is why a package restriction is fatal on a modular
  project (`SourceSet.restrictToPackages`).

When neither is there — the class output was cleaned, never built, or is one edit behind — the references do not
resolve, the compilation units holding them are dropped as tolerable warnings, and the analysis silently covers
less than it appears to. A stale class file is worse than a missing one: it can also fail hard, with
`Inspection of a.b.Base has already been committed`, javac having loaded from the class file a type maddi
already committed from source.

**This is now reported.** Before scanning a set, the inspector checks every dependency it has parsed types for,
and warns (in the `Summary`, and on the log) when a type has neither a class file nor a source file in the
entry, or has a class file older than its source:

```
Source set 'x/test' resolves its references into 'x/main' through class files, and of the 340 type(s) parsed
from it, 12 have neither a class file nor a source file in /…/build/classes/java/main (e.g. a.b.C, a.b.D).
Those references will not resolve, and the compilation units holding them are dropped. Rebuild 'x/main'
before analysing, or have maddi compile it itself (JavaInspector.setGeneratedClassesDirectory).
```

Only dependencies with parsed types read from disk are checked: an unparsed set makes no claim to verify, and an
in-memory (test-protocol) set has no build output to be wrong about.

### 5. Removing the dependency on the build: `setGeneratedClassesDirectory`

```java
javaInspector.setGeneratedClassesDirectory(Path.of(buildDir, "maddi-classes")); // before initialize/parse
```

The inspector then runs javac's code-generation phase after scanning each source set and points its dependents
at *those* class files. What a dependent resolves against is then by construction the code maddi just read —
no build state involved, and no re-parsing of the dependency's sources either. The directory is the switch;
`null` (the default) leaves it off, because the cost is javac's `generate()` on top of parse and analyze.

- A set is generated into `<directory>/<sanitised source set name>-<hash>`, **wiped before each scan** of that
  set, so a renamed or deleted type cannot linger. Callers own the directory: put it under the build directory
  to have `clean` clear it, or in a user-level cache to have it survive.
- A set that is not re-scanned keeps the class files of its last scan — which is what its unchanged sources
  compiled to. The linearization guarantees a re-scanned set regenerates before any dependent re-scans, so this
  composes with the re-parse machinery (§`JavaInspectorImpl.reparse`) without further bookkeeping.
- The generated directory **replaces** the build's for that dependency rather than shadowing it. Mixing the two
  would let a type come from our current output while its sibling still came from the build's stale one — the
  failure this removes, made harder to see. When generation yields nothing for a set (it does not compile, or
  javac aborts), we fall back to the build's output wholesale and say so.
- `CLASS_OUTPUT` is set *only* when generation is on. Left unset, javac writes class files next to the source
  file it compiles, so an unconditional `generate()` would scatter `.class` files through the user's tree.
- **`generate()` destroys the javac task**, so the scan it ran on can no longer answer
  `compiledTypesManager().getOrLoad(...)` — the on-demand library load the analysis depends on (see
  `DESIGN-drop-javac-ast.md` §3). Compiled-type loading therefore moves to a **source-free loader task**: same
  source set and class path, zero compilation units, never generated, built on the first load that needs it.
  A zero-unit task resolves class-path symbols fine; it just must never be `parse()`d. With generation off
  nothing changes — the retained scan serves those loads exactly as before. Full account:
  `docs/partial-reparse-rewire.md` §10.

Reference: `TestGeneratedClassOutput` covers missing output, stale output, the implicit-source-path case, and
the wipe.

### 6. `BigInteger` / `BigDecimal` fail to load through the module-based platform

`getOrLoad(BigInteger.class)` returns `null` when `java.base` is registered as a **module** source set, because
their `jdk.internal.*` deps are encapsulated; loading `java.base` as a **classpath** jmod avoids it. Full
analysis in `maddi-inspection-openjdk/module-vs-classpath jdk.internal loading.md`.
