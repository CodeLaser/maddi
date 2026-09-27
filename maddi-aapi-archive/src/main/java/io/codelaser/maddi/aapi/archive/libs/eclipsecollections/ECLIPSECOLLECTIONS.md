# Eclipse Collections 13.0.0: analysis state (DRAFT, not committable yet)

Status: 2026-09-25. **Work in progress.** The hint files next to this report were generated
by `composeAnalysisHints` and they compile. They are **not fit to commit** as they stand, for four reasons:

1. **Size.** 173 hint files (12 MB of Java) and 19 MB of compiled JSON. The JSON ends up inside
   `maddi-aapi-archive`'s jar, as vavr's 1.6 MB already does. That is not acceptable. See "The size problem".
2. **30 frozen-optimistic type verdicts** (F3 below) would ship as `@Immutable(hc = true)` for types that are not.
3. **18 unsound eventual verdicts** (F4) compile to an UNCONDITIONAL `@Immutable(hc = true)`.
4. **A nondeterministic analyzer crash** was seen on a rerun (F5), and it is not explained.

The analysis results themselves (the source run) are complete and are what this report describes.

**Parked 2026-09-25.** The generated hint sources and JSON, and the five registry edits (as a patch), were moved
out of the tree so the vavr regeneration (`fa010d66b`) could be tested and committed alone. A fifth reason
surfaced doing that: with the EC hint sources in `maddi-aapi-archive`, `TestAnalysisHintsCompiler` and
`TestAnalysisHintsComposer` fail to initialise: their inspector parses the whole module and 13 of the EC hint
files overflow its stack (`StackOverflowError`, 25 parse errors). `compileAnalysisHints` itself had compiled them.

**Next generation will be expected-annotated,** like vavr since `fa010d66b`: the shadows state what EC means
(`Immutable*` as `@ImmutableContainer(hc = true)` / `@Immutable`), from a type table in this report, with the
computed verdict in a comment. Family rows (`org.eclipse.collections.api.list.Immutable*` style prefixes) keep
that table short.

## Setting the corpus up

`corpus/catalogue/eclipse-collections.yml` records how to load, build and configure the corpus. In short:
tag 13.0.0, `mvn -B install -DskipTests -pl eclipse-collections-api,eclipse-collections -am`, then
`corpus/scripts/eclipse-collections-config.py`, which captures both modules' configurations and derives ONE, with the
API and the implementation both as SOURCE. The default per-module route would have read the API from its
compiled classes, so its 169 hand-written interfaces would have had no computed verdicts at all.

Source run: 3,592 primary types (API 1,537, implementation 2,055), about 10 minutes (9m48s), preloading the `jdk`,
`libs/test` and `libs/log` hints. About three quarters of the sources are generated primitive specialisations
(`Int*`, `Long*`, `Char*`, ...).

## Where Eclipse Collections stands

3,194 public types (nested ones included), grouped by what their names promise:

| family | types | IMM | HC | FF | mutable | expected |
|---|---:|---:|---:|---:|---:|---|
| functional (`block.*`) | 814 | 3 | 260 | 508 | 43 | no immutability claim (JDK-hint convention) |
| factories (`*Factory*`, `Lists`, `Sets`, ...) | 750 | 8 | 723 | 19 | 0 | `@Immutable` / `@Immutable(hc)`: stateless |
| mutable primitive collections | 229 | 0 | 0 | 126 | 103 | mutable, `@Container` |
| lazy views (`Lazy*`, `impl.lazy`) | 218 | 0 | 0 | 218 | 0 | `@FinalFields @Dependent`: a view over a source |
| other implementation | 215 | 37 | 8 | 126 | 44 | case by case |
| tuples (`Pair`, `Twin`, ...) | 167 | 2 | 164 | 1 | 0 | `@Immutable(hc)`, primitive pairs `@Immutable` |
| read-only API (`RichIterable`, `ListIterable`, ...) | 166 | 1 | 0 | 165 | 0 | no claim: shared by mutable and immutable types |
| `Synchronized*` | 147 | 0 | 0 | 139 | 8 | `@FinalFields`, modifies through its delegate |
| `Unmodifiable*` | 136 | 0 | 0 | 135 | 1 | `@FinalFields @Dependent`: a view over a mutable delegate |
| `Immutable*`, primitive | 119 | 0 | 0 | 111 | 8 | **`@Immutable`** |
| mutable object collections (`FastList`, `UnifiedMap`, `Mutable*`) | 82 | 1 | 0 | 53 | 28 | mutable, `@Container` |
| iterators | 49 | 9 | 0 | 23 | 17 | mutable (a cursor); the empty iterators `@Immutable` |
| `Immutable*`, object | 47 | 8 | 0 | 36 | 3 | **`@ImmutableContainer(hc = true) @Independent(hc = true)`** |
| `Partition*` | 43 | 0 | 10 | 33 | 0 | `Partition<Mutable>*`: `@FinalFields`; `PartitionImmutable*`: hc |
| `MultiReader*` | 12 | 0 | 0 | 6 | 6 | mutable |

The families are assigned by name (a script in the working notes, not yet committed), so a few types may be in
the wrong row. The expected column is what the name promises; this draft has no per-type table yet.

**As expected:** the factories and tuples, the mutable, synchronized and unmodifiable collections, the lazy views
and the read-only API interfaces (they make no claim, and `@FinalFields` makes none).

**Not as expected: every Immutable\* type.** Of the 47 object Immutable\* types, 36 come out `@FinalFields` and
nearly all of those `@Dependent`: `ImmutableList`, `ImmutableSet`, `ImmutableMap`, `ImmutableBag`,
`ImmutableSortedMap`, `ImmutableStack`, and the public implementations `ImmutableUnifiedMap`, `ImmutableTreeMap`,
`ImmutableHashBag`. 111 of the 119 primitive ones are `@FinalFields` (`ImmutableIntList`, ...). The
implementation classes most programs actually get (`ImmutableArrayList`, `ImmutableIntArrayList`, ...) are
package-private, so a consumer only ever sees the API interfaces, and those carry the cap.

## Findings

### F1: the vavr G1 shape recurs: interfaces shared by mutable and immutable implementations

An interface method's verdict is combined over ALL its implementations. `RichIterable` is implemented by
`FastList`, by the lazy views, by the `Synchronized*`/`Unmodifiable*` wrappers and by the immutable collections
alike. Its `select`, `reject`, `selectWith`, `rejectWith`, `selectInstancesOf`, `groupBy`, `groupByEach`,
`partition`, `partitionWith` and `chunk` come out `@Dependent`. These return a new collection, so the expected
verdict is `@Independent(hc = true)`, but a lazy view's `select` really is a view onto its source. `ImmutableList`
inherits those methods, and a dependent type is capped at `@FinalFields`.

The primitive side shows a second mechanism with the same effect. `IntIterable.toArray()` is `@Dependent`,
although every array-backed implementation copies into a fresh `int[]` (and is computed `@Independent`, correctly).
Of its implementations, 32 are `@Dependent`, and they are **delegators**: the `Synchronized*` and `Unmodifiable*`
wrappers and the `Immutable*HashMap`/`ImmutableIntHashBag` classes that wrap a mutable delegate, each calling
`delegate.toArray()`. That makes the interface's verdict depend on itself through the implementation union, and
the cycle is broken at `@Dependent`.

This is what vavr showed (`VAVR.md`, G1: `Iterator` shares `Value`/`Traversable`) in a second library, at a
larger scale. It is the evidence for the per-receiver engine lever: evaluate `select` on an `ImmutableList`
receiver over `ImmutableList`'s implementations only.

`ImmutableIntArrayList` needs its own note. All its methods are computed non-modifying and independent, and the
type is `@Independent`, yet it is `@FinalFields`, while `ImmutableIntSingletonList` (same interfaces, a single
`int` field) reaches `@Immutable`. The difference is the `int[] items` field, which the constructor takes from its
caller. The type-debug run that would have shown the exact reason crashed (F5).

### F2: a fresh result's modification is charged to the receiver (504 methods)

```java
default MutableIntIntMap with(int key, int value) { return this.with().withKeyValue(key, value); }
```

`with()` is computed `@Independent @NotModified`: it returns a new, empty map. Adding to that new map is
nevertheless charged to `this`, so `with(k, v)` and `of(k, v)` come out `@Modified`. That happens on all 504
`with`/`of` overloads of the primitive `Mutable<K><V>MapFactory` interfaces, which are themselves (correctly)
`@Immutable(hc = true)`, so the hints compiler warns "@Modified method in @Immutable type" 504 times. This is a
separate analyzer defect and a small fixture should reproduce it.

### F3: frozen optimistic type verdicts (30 types). Read this first as a consumer.

The first certification (before the MODREACH rounds) had 244 refused downgrades, 56 of them on `immutableType`. For 30 types the analysis wanted
to lower `@Immutable(hc = true)` to `@FinalFields` and the write-once guard refused, so the optimistic value
survived:

- `Partition*`: `PartitionFastList`, `PartitionUnifiedSet`, `PartitionTreeSortedSet`, `PartitionHashBag`, ... and
  the API interfaces `PartitionMutableList`, `PartitionSet`, `PartitionBag`, .... A mutable partition holds
  two mutable collections, so `@FinalFields` is the right answer.
- `impl.parallel.*ProcedureFactory` (8 types).
- `FixedSizeMapFactoryImpl`, `FixedSizeSetFactoryImpl`.

vavr's run had no refused `immutableType` downgrades. These 30 must not ship as `@Immutable(hc = true)`.

### F4: 18 unsound eventual verdicts, and the hints compile them as unconditional

EC has no eventual method at all (0 `@Mark`/`@Only`), yet 18 types get an eventual verdict:
`@Immutable(hc = true, after = "collection")` (9), `after = "collection,lock"` (8), `after = "delegate,lock"` (1).
They are the primitive `Synchronized*Collection`/`Unmodifiable*Collection` wrappers,
`UnmodifiableMutableCollection` and `SynchronizedRichIterable`. The labels are field names. This is the vavr
`MatchError` shape that the grounding fix `5e7f2cd58` (`EventualCluster.isGroundedInMark`,
`TestEventualNeedsAMark`) was meant to close, so **some path still writes an eventual verdict without that
gate**.

Second defect, in the hints pipeline: the shadow prints both `@FinalFields` and
`@Immutable(hc = true, after = ...)`, and the compiled hint reads that as an unconditional
`immutableType = @Immutable(hc = true)`. So these 18 wrappers would be published as immutable.

### F5: a nondeterministic crash in the link computer (unexplained)

A rerun with `EC_TYPE_DEBUG` set (which should only add logging) exited 5 after:

```
IllegalArgumentException: Trying to overwrite a value for property variablesLinkedToObject
  at PropertyValueMapImpl.set(PropertyValueMapImpl.java:105)
  at LinkComputerImpl$SourceMethodComputer.writeOutMethodCallAnalysis(LinkComputerImpl.java:951)
  on AbstractMutableByteKeySet.anySatisfy(BytePredicate)
```

The clean run, with the same code, input and JDK 27, had no such exception. The exception fired on a pool thread
next to a recursion-prevention report ("done by? null"), which suggests a race between two threads computing the
same method. It has not been reproduced or chased.

### F6: `super.<T>m()` defeats the detailed-source parser (8 files, no verdict impact)

`maddi-java-parser` rejects explicit type arguments on a `super.` call (`super.<V>collect(function)`), in eight
`Synchronized*` files. javac parses them, so the verdicts are unaffected; those files lose detailed sources
(refactoring positions) only. Recorded in the catalogue entry.

## How faithful the hint files are

Compiled hint JSON against the source run, over the 184,104 values the hints cover: 2,312 differ (1.3 %).

- **Checked, encoding only (1,121):** 786 parameters that are primitive (and 6 of type `String`/`Class`),
  stated `@Independent` by the compiler where the source run leaves them unstated; 329 methods, mostly
  functional-interface methods returning a primitive, plus enum `values()`.
- **Real (18):** the F4 eventual verdicts, compiled as unconditional.
- **Not yet classified (about 1,170):** 799 methods computed `@NotModified` whose compiled hint has no
  `nonModifyingMethod` key, and 344 `independentMethod`/`independentParameter` values present in the source run
  and absent in the hint (plus a few dozen smaller rows). Until these are explained they count as fidelity
  losses, not as encoding.
- The hints compiler also logs about 4,600 "hierarchy inconsistency" warnings ("`java.lang.Object` is
  `@Mutable`"). vavr's compile logs the same kind (311): they come from the side-load compile context, not from
  these hints.

## The size problem

| | hint sources | compiled JSON |
|---|---:|---:|
| vavr (committed) | 1.0 MB | 1.6 MB |
| JDK hints (committed) | | 1.8 MB |
| Eclipse Collections (this draft) | 12 MB | 19 MB, of which 14 MB primitive specialisations |

Two things make it large. One: the library is large, and three quarters of it are primitive specialisations that
repeat the object pattern eight or nine times. Two: the compiled JSON writes every default-filled key on every
method (`annotatedApi`, `defaultsAnalyzer`, `containerMethod`, `immutableMethod`, `notNullMethod`, ...), so each
method costs several hundred bytes. And side-loaded libraries' JSON ships inside `maddi-aapi-archive`'s jar even
though no maddi run loads it by default. The size decision is open (the user's).

## Regenerating (current procedure, draft)

1. Onboard and build per `corpus/catalogue/eclipse-collections.yml`.
2. Source run, `--analysis-results-dir <dir>`, preloading `jdk,libs/test,libs/log`.
3. `./gradlew :maddi-aapi-parser:composeAnalysisHints -Dorg.gradle.offline=false`
   `-Pmaddi.compose.anchor=org.eclipse.collections.api.RichIterable,org.eclipse.collections.impl.factory.Lists`
   `-Pmaddi.compose.packages=org.eclipse.collections`
   `-Pmaddi.compose.target=io.codelaser.maddi.aapi.archive.libs.eclipsecollections`
   `-Pmaddi.compose.out=<tmp> -Pmaddi.compose.preload=<dir>`, then copy the `*.java` here. The offline flag is
   needed only for the first resolution of the EC jars.
4. `:maddi-aapi-archive:compileJava`, then `:maddi-aapi-parser:compileAnalysisHints`.
5. Round-trip diff against the source run.

Registries (all five done, uncommitted): `compileOnly` of both EC 13.0.0 jars and `requires static
org.eclipse.collections.api/.impl` in `maddi-aapi-archive`; `testImplementation` in `maddi-aapi-parser`;
`CompileAnalysisHints.SIDE_LOADED_LIBRARIES` plus its inspector factory. Hint packages are not exported.

## Next

- Decide the size question. Scope and format both matter: which types, and whether defaults are written.
- F4: find the eventual writer that bypasses `isGroundedInMark`. It is the same shape as vavr's `MatchError`,
  with a fixture already in place to extend.
- F5: try to reproduce with a plain rerun first, to separate "debug flag" from "race".
- F3: correct or drop the 30 frozen types before any hint is published.
- Per-type table with EXPECTED rows, as for vavr, once the scope is fixed.
