# A method's type parameters are lost to the symbol scanner when a caller is scanned first

**Written 2026-08-01. Investigation report — nothing fixed.** Front end: `maddi-java-openjdk` (the javac
one). Reproducer: `TestMethodTypeParameterSource` in `maddi-java-openjdk`, four drivers, currently green
because two of them **assert the bug**.

## 1. What happens

A method's type parameters can be built by either of two paths, and which one wins depends on the order the
compilation units are scanned in.

```java
// a/AbstractStepScope.java
public <Score_ extends Score<Score_>> InnerScore<Score_> getScore() { ... }

// a/User.java  -- resolving this call creates getScore from its SYMBOL
InnerScore<?> x = s.getScore();
```

| scan order | `tp.source()` | `tp.typeBounds()` |
|---|---|---|
| declaration first | `5-13:5-40` | `Score<Score_>` |
| **caller first** | **`null`** | **`? extends Score<Score_>`** |

Two independent symptoms, and the second is the one that is not merely a refactoring inconvenience:

1. **The declaration has no source.** Nothing can locate the `<Score_ ...>` token, nor any occurrence of
   `Score_` inside its own bounds — those hang off `tp.source()` too.
2. **The bound differs.** The symbol path widens every bound with `withWildcard(wildcardExtends())`
   (`ClassSymbolScanner` ~line 455–467). *The CST of one source file therefore depends on the order its
   compilation units were scanned in.*

## 2. Where

`ClassSymbolScanner.addMethodToType`, and the code already knows:

```java
for (Symbol.TypeVariableSymbol typeParameter : ms.getTypeParameters()) {
    TypeParameter newTp = newTypeParameters.get(i++);
    addTypeBoundsAndCommit(null, null, typeParameter, newTp);
    // FIXME when source type, do not commit yet, we must set detailed sources
}
```

`addTypeBoundsAndCommit` ends in `newTp.builder()...setTypeBounds(...).commit()` — **no `setSource`**, and
committed immediately. Once committed the inspection is closed, so the source path cannot fill it in later.

Thirty lines above, the *same hazard for parameters* is handled:

```java
// ... a method reference may cause the method to be created from its symbol before its declaration is
// reached; see TestParameterInfoSource.
boolean deferParameterCommit = !synthetic && declaredInCurrentTaskSource;
```

`declaredInCurrentTaskSource` is already computed. The type-parameter loop simply does not use it.

The other half is in `ScanCompilationUnit.visitMethod`. When the method is already known
(`typeData.getMethod(jcMethod.sym) != null`) that branch back-fills:

- the **return type**'s detailed source (`TestReturnTypeSource`),
- each **parameter**'s source, then `commitParameters()` (`TestParameterInfoSource`),

and **nothing for type parameters**. The block that builds them with sources sits in the `else` — the
fresh-method branch — so it never runs for a method the symbol scanner already created.

### Why classes are mostly spared

| | builder API | effect |
|---|---|---|
| `TypeInfo.Builder` | `addOrSetTypeParameter` | the source path **replaces** what the symbol scanner left |
| `MethodInfo.Builder` | `addTypeParameter` | add only — the symbol-built instance stays |

That asymmetry is the whole difference. `TestMethodTypeParameterSource` pins it on the same file: the class
type parameter `AbstractStepScope<Solution_>` keeps its source in both orders while the method's does not.

## 3. How often, on real corpora

Counted over a full parse, production and test sources:

| corpus | generic methods | method type params | **without source** | methods affected |
|---|---:|---:|---:|---:|
| langchain4j | 81 | 87 | **33 (38%)** | 31 of 81 |
| timefold-solver | 2103 | 4292 | **1843 (43%)** | 889 of 2103 |

**About two in five.** This is the ordinary case, not an edge case — any project where a caller happens to be
scanned before the declaration, which for a flat scan over a source set is most of them.

Class type parameters, same runs: 1 of 97 (langchain4j) and **285 of 3469 (8%)** (timefold) also come back
without a source. So `addOrSetTypeParameter` is not complete protection either, by some route the reproducer
does not reach. That is a **separate open question**, not part of the diagnosis above.

## 4. What it breaks, and what it might

**Confirmed.** `RenameTypeParameter` in the jfocus rename module: the *uses* of `Score_` all have sources, the
*declaration* does not, so a rename rewrote the uses and left the declaration — `cannot find symbol`, emitted
silently. Found by the timefold rename fuzz. It now refuses instead
(`jfocus-refactor-service` `d27cdf9f`), which converts silent corruption into a visible conflict but is
obviously not a fix for this.

Anything else that must rewrite or locate a type parameter declaration is exposed the same way: move type,
extract interface, isolate class.

**Not measured, and the reason this is worth more than a refactoring bug.** Symptom 2 puts a *different bound*
on the type parameter — `? extends Score<Score_>` instead of `Score<Score_>`. That is a semantic difference in
the CST, reaching the analyzer, decided by scan order. Whether it changes any verdict is untested; the
question is whether assignability, independence or immutability reasoning ever consults a method type
parameter's bounds. If it does, this is a source of order-dependent analysis results, which would be a much
more serious thing than a missing source position.

## 5. What a fix would look like

Symmetric with the parameter fix that is already there:

1. **`ClassSymbolScanner.addMethodToType`** — when `declaredInCurrentTaskSource`, create the type parameters
   but do **not** commit them (exactly what the FIXME says, and exactly what `deferParameterCommit` does one
   loop below).
2. **`ScanCompilationUnit.visitMethod`, the `isKnown` branch** — back-fill from the declaration and commit,
   next to where it already does this for parameters and the return type. Because the commit was deferred, the
   builder is still open, so both `setSource` and `setTypeBounds` are available; the bound should be the
   tree-built one, which fixes symptom 2 along with symptom 1.

Two risks worth checking before touching it:

- **a deferred commit that never happens.** `deferParameterCommit`'s conditions are careful for this reason
  (binary types, other source sets, synthetics all commit immediately). A method type parameter left
  uncommitted because the declaration is never visited would be worse than one with a null source.
- **the type parameter is on the element stack.** `ScanCompilationUnit` pushes the *symbol-built* instances
  into `parameterMap` (line ~715) before the body is resolved, so body references resolve to them. If the
  back-fill replaced the instances rather than filling them in, those references would dangle — filling in is
  therefore the shape to keep, not replacing.

## 6. Reproducing it

```
gradle :maddi-java-openjdk:test --tests '*TestMethodTypeParameterSource*'
```

Four drivers: declaration-first and caller-first for the method type parameter, and the same two for the class
type parameter in the same file. The two caller-first assertions record the bug; when it is fixed,
`testCallerFirst` should fail and become a copy of `testDeclarationFirst`.

**A note on how this was found, because it cost time.** The downstream test was flaky — about four runs in six
— and looked like a thread race. It is not: parsing was sequential. The test passed its sources as `Map.of`,
whose iteration order is randomised per JVM run, and iteration order **is** scan order. Any maddi test that
hands a multi-entry `Map.of` to a parse is sampling a random scan order, and any order-dependent defect it
touches will present as flakiness.
