# Handoff: a type parsed from source whose own jar is on the class path has its hierarchy built twice

**Written 2026-08-21.** Found by parsing the whole CodeLaser tree as ONE project — 160 source sets
over eight repositories, for the jfocus JPMS/`module-info.java` campaign. Status: **open**; nothing
is fixed, the campaign works around it by excluding one source set from the parse.

The two guards that exist against double-building a type's hierarchy both ask about the **symbol**.
This defect is the case where the symbol and the `TypeInfo` disagree: a class-file symbol handed to
a `TypeInfo` the source scan already built. `ClassSymbolScanner` already calls its own arrangement
here a **STOPGAP** (line 502); this is the case the stopgap does not cover.

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

Seen through the refactoring server's `ScriptRunner`, but nothing about that path is implicated —
the configuration is what matters (below), and the same configuration fails the same way under
`maddi-run-openjdk`'s CLI.

## The assertion, and what it really means

`TypeInspectionImpl.java:73`, in the constructor that runs at `commit()`:

```java
assert interfacesImplemented.size() == interfacesImplemented.stream().distinct().count()
        : "Extending multiple identical interfaces";
```

No Java type can declare the same interface twice, so this never fires because a file said so. It
fires because the list was **built twice**: `TypeInspectionImpl.Builder.addInterfaceImplemented`
(line 306) appends and does not de-duplicate, so any path that re-runs a type's setup block doubles
it. `commit()` is where it is noticed, which is why the reported location is the *declaration* and
tells you nothing about the second writer.

## The configuration that triggers it

The parse holds `maddi/maddi-annotation/main` **as a source set**, and
`maddi-annotation-0.9.1.jar` is on the compile class path of **155 of the 165** compile lines,
because every other module in the tree is compiled against the published artifact. So one
fully-qualified name is reachable two ways inside a single parse:

- as source, from the source set;
- as bytecode, whenever another source set resolves `@NotModified` through its own class path.

`InfoByFqn` is keyed by fully-qualified name, so both routes land on the **same `TypeInfo`**.

This is not the stale-jar case the runtime message suggests ("*if a type is defined BOTH in your
sources and in a jar on the classpath, the jar may be stale — rebuild or delete it*"). The jar here
was rebuilt in the same `--rerun-tasks` sweep that produced the compile log. Rebuilding changes
nothing: the duplication is structural, not a version skew.

## Why annotation types are what trip it

An `@interface` implicitly implements `java.lang.annotation.Annotation`, and the source scan adds it
explicitly — `ScanCompilationUnit.java:468`:

```java
if (typeInfo.typeNature().isAnnotation()) {
    ParameterizedType javaLangAnnotationAnnotation = convertType.convert(jcClassDecl.sym.getInterfaces().getFirst());
    builder.addInterfaceImplemented(javaLangAnnotationAnnotation);
}
```

So an annotation's interface list is exactly one entry, and never empty. A class with no `implements`
clause has an empty list; doubling it is still empty and the assert cannot fire. Annotations are the
smallest possible tripwire — **not the only type at risk**. Any type with a non-empty interface list
that arrives both ways should double the same way; that generalisation is read from the code, not
measured, because the parse aborts at the first annotation.

## Why the existing guards miss it

`ClassSymbolScanner.java:495–509` is the guard, and its comment states exactly this hazard for the
sibling case:

```java
// Do not load the interfaces of a *source* type here: ... ScanCompilationUnit also adds the
// interfaces from source -- since addInterfaceImplemented appends, the result is a duplicated
// interface list. A source type's hierarchy is owned by ScanCompilationUnit.
if (!isSourceSymbol(cs) && typeInterfacesLoaded.add(newTypeInfo)) {
```

Both halves fail open here:

1. **`isSourceSymbol(cs)` asks about the ClassSymbol, not about the TypeInfo.** javac resolved
   `@Fluent` out of the jar, so `cs` is a class-file symbol and the test is `false` — even though the
   `TypeInfo` it is about to write is one `ScanCompilationUnit` owns.
2. **`typeInterfacesLoaded` is per-scanner** (line 57), and this is that scanner's first load of the
   type.

The shared guard that would have caught it is never armed on this path.
`InfoByFqn.markClassScannerSetupDone` (line 56) is identity-keyed on `TypeInfo` and outlives any one
scanner, but only two sites set it: `loadType` itself (`ClassSymbolScanner.java:426`) and the
**anonymous-type** `put` (line 1884, whose comment describes the equivalent defect for anonymous
types). The ordinary source `put(TypeInfo)` at line 1871 does not mark, so a source-built primary
type reaching `loadType` later is unguarded.

## The same hole duplicates ANNOTATIONS, and that half has no assert

Worth handling in the same change. `InspectionImpl.Builder.addAnnotation`/`addAnnotations` (lines
199, 205) append exactly like `addInterfaceImplemented`, and the type-level add is guarded at
`ClassSymbolScanner.java:445` by `typeAnnotationsLoaded` — **per-scanner, with no `isSourceSymbol`
half at all**. What protects it instead is `loadAnnotations` (line 648):

```java
private List<AnnotationExpression> loadAnnotations(Symbol symbol) {
    if (isSourceSymbol(symbol)) return List.of();
```

which asks about the **symbol** again. So on this path — class-file symbol, source-built `TypeInfo`,
fresh scanner — it returns the bytecode annotations and appends them a second time. There is no
distinctness assert on annotations in `TypeInspectionImpl` (line 73 is the only one), so this
half is **silent**: a duplicated `@NotModified` on a type, not a crash.

**Read, not measured** — the parse aborts at the interfaces assert before anything could observe it.
Confirming it is the cheapest first step for whoever picks this up, because it decides whether this
is a crash to fix or a wrong-answer to fix.

## Why five of twenty-seven — it is order, not a property

Checked, and none of these discriminates:

| candidate rule | refuted by |
|---|---|
| usage count | `NotNull` (943 uses) survived; `GetSet` (119) failed |
| `@Target` | the five span METHOD, FIELD, PARAMETER, TYPE and one with no `@Target` at all |
| `@Retention` | 25 of the 27 are `CLASS`, including all the survivors |

What decides it is **traversal order**: which annotations happen to be resolved from the jar by some
source set *after* their own source has been scanned. That makes the failure set unstable — a
different scan order fails on a different subset — and it means a fix must not be validated against
"these five".

## Reproduction

No corpus is needed in principle. The shape is: one source set declaring type `T`, plus a jar
containing `T` on the class path of a second source set that references `T`, in one
`InputConfiguration`. `T` an `@interface` gives the crash; `T` a class with any `implements` clause
should give it too.

**That fixture has not been written.** The existing
`maddi-java-openjdk/src/test/.../other/TestCrossScannerDuplicateInterfaces.java` reproduces the
*neighbouring* case hermetically — two scanners, shared `InfoByFqn`, both class-file — and is the
right file to extend; its `scan(...)` helper does not currently offer a class-path artifact
shadowing a source type, which is the piece to add.

To reproduce as found: build the whole-tree input configuration described in
`jfocus-refactor-server/work/codelaser/pipeline/`, leaving `maddi-annotation/main` in.

## What the code already proposes

`ClassSymbolScanner.java:502`, four lines above the guard, and it is the design note for this
handoff rather than a fresh idea:

> **STOPGAP:** the proper fix is a linear inspection-state on the builder
> (`DEFINED_BY_CLASS_SCANNER` vs `DEFINED_IN_SOURCE`), which would make the double-load impossible
> (and assertable) rather than guarded.

That state is on the **builder**, i.e. on the thing that actually accumulates the list, and it is
what removes the whole class of "which symbol asked?" questions. Both this defect and the
annotations half above are instances of asking the symbol a question only the `TypeInfo` can answer.

A narrower repair — marking `markClassScannerSetupDone` from the ordinary source `put(TypeInfo)`, so
the shared guard is armed for source types the way it already is for anonymous ones — would close
the reported crash. It leaves the underlying asymmetry in place, so it is a candidate for
*unblocking*, not for closing.

## The workaround in use, and what it costs

`maddi/maddi-annotation/main` is excluded from the parse (dropped from the compile log before the
input configuration is generated). It costs nothing measurable:

- the jar stays on every class path, so nothing loses the annotations;
- `graph.moduleDependencies` still resolves `io.codelaser.maddi.annotation` as a **LIBRARY** module,
  named from the jar's own `module-info.class`;
- it is what `dogfood/settings.gradle.kts` already does deliberately, so that reading `@Mark`/`@Only`
  out of **bytecode** is exercised.

## Why no corpus has ever surfaced this

Every OSS corpus is analysed as source with third-party jars on the class path — disjoint sets, so a
name is never reachable both ways. It takes **self-analysis**: running the toolkit over a tree that
publishes the very artifacts it also compiles against. `dogfood` is maddi's self-analysis instrument
and it avoids the shape by construction, keeping `maddi-annotation`, `maddi-support` and
`maddi-util` as jars while only `cst-api`/`cst-impl`/`cst-analysis` are source.

That is worth stating as a property rather than an accident: **`dogfood`'s jar/source split is load
bearing, and not only for the reason its comments give.** A future widening of `dogfood` to cover
more modules as source walks into this defect.
