# A type parsed from source has its hierarchy built twice, by the preload

**Written 2026-08-21, FIXED the same day.** Found by parsing the whole CodeLaser tree as ONE project —
160 source sets over eight repositories, for the jfocus JPMS/`module-info.java` campaign. Status:
**closed**; fixed in `ScanCompilationUnits`, `ClassSymbolScanner` and `ScanCompilationUnit`, with a
hermetic regression test (`TestPreloadBeforeSourceSymbols`, `maddi-inspection-openjdk`).

⚠ **This document was first filed with a different diagnosis, and the title above is the corrected one.**
The original read the shape as *"one FQN reachable both as source and as bytecode, because the module's own
jar is on 155 of the 165 compile lines"*. That is a true description of the configuration and it is **not
what causes the duplication** — see [What the first diagnosis got wrong](#what-the-first-diagnosis-got-wrong).
The reproduction holds **no artifact at all** for the type that doubles.

## The symptom

```
Cannot switch to ParseResult: 5 parse error(s) in 5 compilation unit(s).
  - Exception: java.lang.AssertionError
In: .../maddi/maddi-annotation/src/main/java/io/codelaser/maddi/annotation/Fluent.java
In: compilation unit
Message: Extending multiple identical interfaces
```

Five of `maddi-annotation`'s twenty-seven types: `Fluent`, `Independent`, `Modified`, `NotModified`,
`method/GetSet`. It aborts the parse, so no analysis runs.

## The assertion, and what it really means

`TypeInspectionImpl.java:73`, in the constructor that runs at `commit()`:

```java
assert interfacesImplemented.size() == interfacesImplemented.stream().distinct().count()
        : "Extending multiple identical interfaces";
```

No Java type can declare the same interface twice, so this never fires because a file said so. It
fires because the list was **built twice**: `TypeInspectionImpl.Builder.addInterfaceImplemented`
appends and does not de-duplicate, so any path that re-runs a type's setup block doubles it.
`commit()` is where it is noticed, which is why the reported location is the *declaration* and tells
you nothing about the second writer.

## The mechanism

Two writers meet on one `TypeInfo`, and the guard that separates them was blind at exactly the moment
they met.

1. **`ScanCompilationUnits.scan()` preloaded a class-path package before it published this task's
   source symbols.** The parse preloads `io.codelaser.jfocus.transform.support` from the class path
   (a `JavaInspector.preload(...)` the refactoring server asks for). The preload ran *above* the line
   that computes `topLevelClassSymbolsOfSources` and hands it to the scanner.

2. **A preloaded class-file type carries `@NotModified`, and resolving that annotation reaches a
   source type.** `loadAnnotations` → `annotationExpression` → `convert(compound.type)` →
   `classTypeInfo` → `lazilyLoadPrimaryTypeFromClassFile(NotModified)`. In *this* javac task
   `io.codelaser.maddi.annotation.NotModified` is a **source** symbol (`maddi-annotation/main` is the
   source set being scanned), so `ensureSourceSet` — whose first line is `if (!fromClassFile(cs))
   return sourceSetOfCurrentTask` — attributes the new `TypeInfo` to **the current source set**.

3. **`isSourceSymbol` failed open, so the class scanner built the hierarchy anyway.**

   ```java
   private boolean isSourceSymbol(Symbol symbol) {
       if (topLevelClassSymbolsOfSources == null) return false;   // ⛔ the wrong answer, not "unknown"
       ...
   ```

   The map was still `null` — step 1 — so the guard said *"not a source symbol"* about a symbol javac
   had entered from source. `loadType` therefore ran the whole setup block: it added
   `java.lang.annotation.Annotation` from `cs.getInterfaces()`, and the type's own `@Target`/`@Retention`
   from `loadAnnotations`.

4. **The source scan then adopts that very `TypeInfo` and builds it again.**
   `ScanCompilationUnit.visitClass` looks the FQN up and reuses what it finds when the source sets
   match — and they match, because of step 2:

   ```java
   if (known != null && known.compilationUnit().sourceSet().equals(compilationUnit.sourceSet())) {
       typeInfo = known; // was already created because of the order
   }
   ```

   `continueType` then appends the interfaces and annotations from source, on top of what step 3 left.
   `commit()` throws.

**Why annotation types are the tripwire.** An `@interface` implicitly implements
`java.lang.annotation.Annotation`, and the source scan adds it explicitly (`ScanCompilationUnit:468`),
so its interface list is exactly one entry and never empty. A class with no `implements` clause has an
empty list; doubling it is still empty and the assert cannot fire. Annotations are the *smallest*
tripwire, not the only type at risk — the regression test carries a plain
`class Impl implements Iface` beside the `@interface`, and pre-fix **both** fail.

**Why five of twenty-seven, and it is not arbitrary.** The five that die are exactly the five maddi
annotations that occur in the preloaded package. `io.codelaser.jfocus.transform.support` uses
`@NotModified`, `@Independent`, `@GetSet`, `@Fluent` and `@Modified` — and `@Immutable`, which appears
there **only inside comments**. The other twenty-two annotation types are never resolved during the
preload, so nothing writes them twice.

**Why the first source set.** The preload runs under `if (!runtime.objectTypeInfo().hasBeenInspected())`,
i.e. once per parse, in whichever source set is scanned first. `maddi-annotation/main` was it.

## The fix

Three changes, two of them independent repairs of the same failure and the third structural.

1. **`ScanCompilationUnits.scan()` publishes `topLevelClassSymbolsOfSources` *before* the preload.**
   Pure reordering: the map is derived from `units`, which `task.parse()`/`task.analyze()` have already
   produced at the top of the method. It also makes `classTypeInfo`'s "one of the source files we are
   parsing" branch correct during the preload.

2. **`ClassSymbolScanner.isSourceSymbol` no longer depends on that timing.** javac itself knows: a
   `ClassSymbol` entered from source has its `classfile` pointing at the `.java` it came from, which is
   precisely what `fromClassFile` tests. So

   ```java
   Symbol.ClassSymbol top = primary(enclosing);
   if (!fromClassFile(top) && top.sourcefile != null) return true;
   return topLevelClassSymbolsOfSources != null && topLevelClassSymbolsOfSources.containsKey(top);
   ```

   This is the change the regression test pins: with (1) reverted and (2) in place, the fixture passes.

3. **`ScanCompilationUnit.continueType` claims the type on the shared registry, and undoes a class-file
   load that got there first.**

   ```java
   if (!typeData.markClassScannerSetupDone(typeInfo)) {
       builder.clearInterfacesImplemented();
       builder.clearAnnotations();
   }
   ```

   `InfoByFqn.markClassScannerSetupDone` is identity-keyed on the `TypeInfo` and outlives any one
   scanner. Claiming it here says *the source scan owns this type's hierarchy*, in a place both scanners
   can see, which is the linear `DEFINED_BY_CLASS_SCANNER` / `DEFINED_IN_SOURCE` state that
   `ClassSymbolScanner:502`'s STOPGAP note asks for. It works in both directions: a class-file load
   **later** (another source set's on-demand load, a re-parse's `commitType`) finds the claim taken and
   skips its setup block; a class-file load **earlier** is undone, because `visitClass` adopts the very
   `TypeInfo` it registered. `TypeInfo.Builder.clearAnnotations()` is new, and is the counterpart of the
   existing `clearInterfacesImplemented()`.

The per-scanner `typeInterfacesLoaded` / `typeAnnotationsLoaded` sets and the `isSourceSymbol` test at
the interface guard are left as they are: they still guard the LAZILY-then-LOAD_MEMBERS re-entry, and
nothing above makes them wrong.

## The annotations half — it was real, and it is silent

`InspectionImpl.Builder.addAnnotation`/`addAnnotations` append exactly like `addInterfaceImplemented`,
and there is **no distinctness assert on annotations** (line 73 is the only one). So the same hole put
a type's own `@Target` in the list twice: a wrong answer, not a crash, and invisible because the parse
aborted at the interfaces assert first. The regression test asserts it directly —
`assertEquals(List.of("java.lang.annotation.Target"), markerAnnotations)` — and it fails pre-fix for the
same reason the interfaces do.

## Reproduction

`maddi-inspection-openjdk/src/test/.../TestPreloadBeforeSourceSymbols.java`, hermetic, ~2 s. The shape:

- a class-path jar holding **only** package `p`: `p.Used`, annotated `@a.b.Marker`, and `p.User`, whose
  field type is `a.b.Impl`;
- one source set declaring `a.b.Marker` (an `@interface`), `a.b.Iface` and `a.b.Impl implements Iface`
  — and **nothing of `a.b` on the class path**, so javac must resolve those names from source;
- `javaInspector.preload("p")`.

Pre-fix both `a.b.Marker` and `a.b.Impl` die with *"Extending multiple identical interfaces"*.

⚠ Two things that are **not** needed, both of which the first write-up believed were: a jar shadowing
the source type, and a second source set. One source set and a preload are enough.

## What the first diagnosis got wrong

Recorded because the wrong reading survived a careful code walk, and the difference is instructive.

| the first reading | what is actually true |
|---|---|
| `maddi-annotation-0.9.1.jar` on 155 compile lines makes one FQN reachable two ways, and `InfoByFqn` hands both routes the same `TypeInfo`. | The jar is irrelevant. Both routes are the **same source symbol**; the guard simply could not see that yet. The fixture holds no `a.b.*` artifact at all. |
| `isSourceSymbol(cs)` asks about the ClassSymbol rather than the TypeInfo, and javac supplies a class-file symbol. | It asks the right thing about the right symbol. Its defect was the **fail-open on a null map**, and the map was null because of *when* the preload ran. |
| Which types fail is traversal order, so a fix must not be validated against "these five". | The rule is sharper and fully determined: the five are exactly the maddi annotations **used in the preloaded package**. (The advice not to validate against those five still stands, and the regression test does not.) |
| A narrower repair would be to mark `markClassScannerSetupDone` from the ordinary source `put(TypeInfo)`. | That would be wrong: `lazilyLoadPrimaryTypeFromClassFile` calls the **same** `put`, so a legitimate pre-source lazy load would be left with no parent class and no type parameters. The claim belongs in `continueType`, which is where the source scan actually takes over. |

The generalisation the first write-up drew from reading — *"any type with a non-empty interface list
that arrives both ways should double the same way"* — is right, and is now measured rather than read:
`a.b.Impl implements Iface` fails pre-fix exactly like the `@interface`.

## Why no corpus has ever surfaced this

It needs a **preloaded class-path package that references a type the first source set declares**. Every
OSS corpus is analysed as source with third-party jars on the class path — disjoint sets, so a preloaded
library never names a type the corpus itself declares. It takes **self-analysis**: running the toolkit
over a tree that publishes the very artifacts it also compiles against, and preloading one of them.

`dogfood` is maddi's self-analysis instrument and avoids the shape by construction, keeping
`maddi-annotation`, `maddi-support` and `maddi-util` as jars while only `cst-api`/`cst-impl`/
`cst-analysis` are source. That split is load bearing, and not only for the reason its comments give.

## The workaround that is no longer needed

The jfocus JPMS campaign excluded `maddi/maddi-annotation/main` from the parse (dropped from the compile
log before the input configuration is generated). That exclusion can be lifted; the cost of keeping it
was small but real — the source set's own types were absent from the CST, so the campaign's
`module-info` reconciliation could not see them.
