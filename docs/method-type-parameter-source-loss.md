# Type parameters are lost to the symbol scanner: two routes, one root cause

**Written 2026-08-01. RESOLVED 2026-08-01.** Front end: `maddi-java-openjdk` (the javac one).

Two bugs with the same root cause — **the symbol scanner writing type parameters onto a type the source scan
owns** — reached by opposite routes, and §8 is the second one. In both the symbol view silently won over the
declaration: no source position, and every bound widened with `? extends`.

| | route | why the symbol won | pinned by |
|---|---|---|---|
| **method** type parameters (§1–§7) | the symbol view arrives **first**, from a call site | `MethodInfo.Builder` has no `addOrSet`, so the declaration could not replace it | `TestMethodTypeParameterSource` |
| **class** type parameters (§8) | the symbol view arrives **second**, via a nested type | `addOrSetTypeParameter` replaces by index, so it overwrote the declaration | `TestClassTypeParameterSource` |

## 1. What happened

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
| **caller first, before the fix** | **`null`** | **`? extends Score<Score_>`** |
| caller first, after | `5-13:5-40` | `Score<Score_>` |

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

**Confirmed.** `RenameTypeParameter` in the jfocus rename module: the *uses* of `Score_` all had sources, the
*declaration* did not, so a rename rewrote the uses and left the declaration — `cannot find symbol`, emitted
silently. Found by the timefold rename fuzz. It was given a refusal for the case
(`jfocus-refactor-service` `d27cdf9f`), which turned silent corruption into a visible conflict; that refusal
is now unreachable through this route and is kept as a backstop.

Anything else that must rewrite or locate a type parameter declaration is exposed the same way: move type,
extract interface, isolate class.

**Never measured, and the reason this was worth more than a refactoring bug.** Symptom 2 put a *different bound*
on the type parameter — `? extends Score<Score_>` instead of `Score<Score_>`. That is a semantic difference in
the CST, reaching the analyzer, decided by scan order. Whether it changes any verdict is untested; the
question is whether assignability, independence or immutability reasoning ever consults a method type
parameter's bounds. If it does, this was a source of order-dependent analysis results, which would be a much
more serious thing than a missing source position. Moot now, but it is why the bound was fixed along with the
source rather than only the source.

## 5. The plan (kept for the reasoning; §7 is what landed)

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
type parameter in the same file. The caller-first assertions are now identical to the declaration-first ones —
a source file's CST may not depend on the order its compilation units were scanned in, and that identity is
the cheapest way to say so.

**A note on how it was found, because it cost time.** The downstream test was flaky — about four runs in six
— and looked like a thread race. It is not: parsing was sequential. The test passed its sources as `Map.of`,
whose iteration order is randomised per JVM run, and iteration order **is** scan order. Any maddi test that
hands a multi-entry `Map.of` to a parse is sampling a random scan order, and any order-dependent defect it
touches will present as flakiness.

## 7. What landed

Both steps of §5, and both risks held.

**`ClassSymbolScanner.addMethodToType`** — the type parameters are created and registered as before, and then
nothing else happens when `deferCommitToDeclaration`: no annotations, no bounds, no commit. Deferring
*everything* rather than only the commit is what keeps the declaration the single writer, so nothing is added
twice and no reader sees a half-built bound. The flag is the old `deferParameterCommit`, renamed because it
now governs both.

**`ScanCompilationUnit.visitMethod`, the `isKnown` branch** — fills in from the declaration through the
existing `parseTypeBoundsAndCommit`, which already sets source, annotations and the written bounds. It runs
**first** in that branch, before the return type and the parameters, since both may mention `T`.

The guard is `!typeParameter.hasBeenInspected()`, not `source() == null`. That is exactly the precondition for
calling `builder()`, which asserts the inspection is still open: a type parameter the symbol path *did* commit
(a synthetic method, another source set) also has no source, and reaching for its builder would trip that
assertion rather than fill anything in.

Neither risk materialised. The element-stack one is handled by construction — the instances are filled in, not
replaced, so the `parameterMap` entries and every body reference still point at them. The never-committed one
is bounded by `declaredInCurrentTaskSource`: if the type is in this task's source set its declaration is
visited, which is the same assumption the parameters have relied on.

**Verification.** 2895 fast tests across all 27 maddi modules, plus the corpus `slowTest` suites. Downstream,
the jfocus rename module's `testMethodTypeParamSelfReferentialBoundCallerFirst` — which pinned the *refusal* —
now asserts the same successful rename as its declaration-first twin, which was the point.

One unrelated casualty found on the way: `TestFormatterStress` passed `scan("X", ...)` where the FQN
`a.b.X` is required, so it had been failing on its own for a while. Fixed, since it was masking the rest of
the module's `slowTest`.

**Then still open**: the 8% of *class* type parameters that came back without a source on timefold-solver
(§3). That turned out to be the second route; see §8.

## 8. The second route: class type parameters, overwritten by a nested type

§3 recorded that 285 of timefold's 3469 class type parameters also lacked a source, "by some route the
reproducer does not reach". It is the mirror of §1, and it needs nothing exotic at all.

**The trigger is a forward reference from a type to its own nested type.** Guava's `AbstractIterator`:

```java
public class AbstractIterator<T> {
    private State state = State.NOT_READY;    // State is not registered yet ...
    private enum State { ... }                // ... it is declared here
}
```

Resolving `State` at the field finds no registered subtype, so it is loaded from its symbol
(`lazilyLoadTypeFromClassFile`, whose name is misleading — no class file need exist). Loading a **nested**
type lazily loads its **enclosing** type, "so that we can compute access as soon as possible" — and that
enclosing type is the one the scan is in the middle of building. `loadType` returns early only on
`hasBeenInspected()`, which is false until the whole compilation unit is committed, so its setup block ran on
a half-built source type. `addOrSetTypeParameter` **replaces by index**, so the symbol-built type parameters
won over the ones `continueType` had produced seconds earlier.

The stack, which is what settled it:

```
ClassSymbolScanner.loadType:384          <- the setup block, overwriting
ClassSymbolScanner.loadType:373          <- "ensure that the enclosing types have at least been lazily loaded"
ClassSymbolScanner.lazilyLoadTypeFromClassFile:247
ClassSymbolScanner.classTypeInfo / classType / convertTree
ScanCompilationUnit.convertTypeWithAnnotations / visitVariable   <- a field declaration, mid-scan
```

### What it was, and is

| corpus | source sets | generic types affected | class type params without source |
|---|---:|---:|---:|
| guava | 1 | 37 of 698 | 68 of 1120 → **0** |
| timefold-solver | 64 | 74 of 1469 | 285 of 3469 → **0** |
| jenkins | 2 | 1 of 145 | 2 of 183 → **0** |
| langchain4j | 2 | 1 of 63 | 1 of 97 → **0** |
| fernflower | 2 | 0 of 9 | 0 → 0 |
| activemq | — | 0 of 8 | 0 → 0 |

Guava is the one that mattered: **one** source set, and the same ~5% rate as timefold's sixty-four. That
killed the theory that this was about project structure, and every affected type turned out to have a nested
type.

### Two negative results, recorded so nobody re-runs them

- **Source-to-source references do not do it.** `B extends A<String>`, a field of type `A`, a call to a static
  method of `A`, a use of `A.Nested` — all fine, in either scan order. The reference has to be one the source
  scan has not registered *yet*.
- **The same FQN as both source and class file does not do it.** timefold's corpus puts 67 of its own
  `target/classes` directories on the parse classpath, which looked like the obvious culprit; javac prefers
  the source and the class file is never read. Compiling `A` to a temp directory and putting it on the
  classpath alongside `A`'s source reproduces nothing.

### The guard

In `loadType`'s setup block: when the type already holds type parameters and **every one of them has a
source**, they were built by the declaration — keep them, and register those instances in the type-parameter
map so the supertype and interface conversions below resolve against what the type actually holds. Anything
else falls through to the symbol path exactly as before.

Deliberately narrow. The alternative — having `ScanCompilationUnit.continueType` mark the type as
class-scanner-setup-done, so the whole block is skipped — is tidier but also skips the parent class, the
interfaces and the annotations, which is a much larger behavioural change for no measured benefit.
