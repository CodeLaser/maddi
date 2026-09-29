# Eclipse Collections 13.0.0: analysis hints and analysis state

Status: 2026-09-29. Covers the hint files next to this report (`OrgEclipseCollectionsApi*`) and their compiled
results in `src/main/resources/.../analyzedPackageFiles/libs/eclipsecollections`.

These hints state **what Eclipse Collections means**, as the JDK hints do for the JDK and `VAVR.md` for vavr:
an `ImmutableList` is an immutable container with hidden content, a `MutableList` a mutable container, a factory
stateless. They start from maddi's source analysis of the library; where the computed verdict differs from the
library's contract, the shadow carries the EXPECTED annotations from the table at the end of this report, with the
computed verdict kept in a comment. The differences are the engine's work list (`ENGINE-WORKLIST.md`); the day the
analysis computes a row's expected verdict, that row can go.

## Scope: the object-typed public API

The hints cover `org.eclipse.collections.api` **without** its generated primitive specialisations (the
`..primitive` packages; the primitive iterables at the API root, `IntIterable` and its lazy twin, stay, a consumer
of `IntList` sees them) and without the functional interfaces (`api.block.*`, no immutability claim, as for the JDK's). That is
199 primary types; the implementation module (`org.eclipse.collections.impl`) is not covered, its
public classes (`FastList`, `UnifiedMap`, ...) are reached through the API interfaces and factories anyway.

The reason is size. The first cut (2026-09-25) composed everything under `org.eclipse.collections`: 173 hint
files, 12 MB of Java, 19 MB of compiled JSON, three quarters of it the primitive pattern repeated eight or nine
times, and 13 of the files overflowed the inspector's stack when `TestAnalysisHintsCompiler` parsed the module.
The object API is 43 files, 1.0 MB of Java and 1.2 MB of JSON: vavr's size.
`composeAnalysisHints` takes `-Pmaddi.compose.exclude=org.eclipse.collections.api.block,.primitive` for it.

## Using the hints

Eclipse Collections is **side-loaded**: its results are not in `libs.jar` and not in
`LoadAnalysisResults.ANALYZED_RESULTS`, so a maddi run does not load them by default. Preload them explicitly,
together with the JDK hints:

```
--preload-analysis-results-dirs <archive>/analyzedPackageFiles/jdk,<archive>/analyzedPackageFiles/libs/eclipsecollections
```

where `<archive>` is `maddi-aapi-archive/src/main/resources/io/codelaser/maddi/aapi/archive`.

## Reading a shadow

```java
    //public interface ImmutableList<T> extends ImmutableCollection<T>, ListIterable<T>
    //annotated as EXPECTED; computed @FinalFields @Dependent -- G1: ... (ECLIPSECOLLECTIONS.md)
    @ImmutableContainer(hc = true) @Independent(hc = true)
    interface ImmutableList$<T> {
```

As in `VAVR.md`: the declaration comment is the library type's own header; the annotations are the only part
compiled and, for a type with an expected row, they are the EXPECTED verdict; the second comment keeps what maddi
computes and names the gap. The members follow their type (`ComposeAnalysisHints.applyExpected`): in an
immutable type every instance method is `@NotModified`; in a container no method modifies its arguments; in an
hc-independent type a computed `@Dependent` member becomes `@Independent(hc = true)`. A bare `@Container` in the
table is a MUTABLE container: `MutableList` may change, its methods store their arguments and never modify them.

Member rows (`type#method`) state receiver modification where the computed verdict contradicts the method's
name: a mutator (`add`, `put`, `clear`, ...) computed `@NotModified`, a pure query (`size`, `contains`,
`toList`, ...) computed `@Modified`. They are generated, not judged: the same M1/M2 name families as
`corpus/scripts/ec-wrong-verdicts.py`.

## Where Eclipse Collections stands

Source run of 2026-09-29 (engine `b6fcd70b4 plus the results-writer fix of F5`, API + implementation as source, 3,726 primary types, 4
minutes, 30M work ceiling, 0 ceiling trips). The public types by what their names promise, over the whole library:

| family | types | IMM | HC | FF | mutable |
|---|---:|---:|---:|---:|---:|
| functional (`block.*`) | 814 | 3 | 173 | 493 | 145 |
| factories | 750 | 8 | 719 | 21 | 2 |
| `Immutable*`, primitive | 433 | 87 | 4 | 306 | 36 |
| other implementation | 238 | 37 | 9 | 70 | 122 |
| mutable collections, primitive | 229 | 0 | 0 | 119 | 110 |
| lazy views | 224 | 0 | 0 | 180 | 44 |
| tuples | 171 | 2 | 168 | 0 | 1 |
| read-only API | 166 | 1 | 0 | 138 | 27 |
| `Synchronized*` | 147 | 0 | 0 | 121 | 26 |
| `Unmodifiable*` | 136 | 0 | 0 | 112 | 24 |
| `Immutable*`, object | 113 | 8 | 0 | 31 | 74 |
| mutable collections, object | 82 | 1 | 0 | 22 | 59 |
| iterators | 49 | 9 | 0 | 16 | 24 |
| `Partition*` | 28 | 0 | 1 | 27 | 0 |
| `MultiReader*` | 12 | 0 | 0 | 3 | 9 |

**As expected:** the factories and tuples, the mutable, synchronized and unmodifiable collections, the lazy views
and the read-only API interfaces (they make no claim, and `@FinalFields` makes none).

**Not as expected: every `Immutable*` type**, capped at `@FinalFields` or computed mutable outright, through the
implementor union (F1 below). Those are exactly the rows the expected table overrides.

## Findings

### F1: the vavr G1 shape recurs: interfaces shared by mutable and immutable implementations

An interface method's verdict is combined over ALL its implementations. `RichIterable` is implemented by
`FastList`, by the lazy views, by the `Synchronized*`/`Unmodifiable*` wrappers and by the immutable collections
alike. Its `select`, `reject`, `groupBy`, `partition`, `chunk`, ... come out `@Dependent`: they return a new
collection, so the expected verdict is `@Independent(hc = true)`, but a lazy view's `select` really is a view onto
its source. `ImmutableList` inherits those methods, and a dependent type is capped at `@FinalFields`. On the
primitive side, `IntIterable.toArray()` is `@Dependent` because 32 delegating implementations (`Synchronized*`,
`Unmodifiable*`, `Immutable*HashMap`) call `delegate.toArray()`, so the interface's verdict depends on itself
through the union and the cycle is broken at `@Dependent`.

This is vavr's G1 (`Iterator` shares `Value`/`Traversable`) in a second library, at a larger scale: the evidence
for the per-receiver engine lever (evaluate `select` on an `ImmutableList` receiver over `ImmutableList`'s
implementations only). Until then the expected rows state the contract.

### F2: a fresh result's modification is charged to the receiver

```java
default MutableIntIntMap with(int key, int value) { return this.with().withKeyValue(key, value); }
```

`with()` returns a new, empty map; adding to it is nevertheless charged to `this`, so `with(k, v)` and
`of(k, v)` come out `@Modified` on all 504 `with`/`of` overloads of the primitive `Mutable<K><V>MapFactory`
interfaces. Out of the hints' scope (primitive), still an analyzer defect worth a fixture.

### F3: frozen optimistic type verdicts

The first certification refuses `immutableType` downgrades on the `Partition*` types (`PartitionSet`,
`PartitionBag`, ... and the implementation's `PartitionFastList`, ...), the `impl.parallel.*ProcedureFactory`
types and two `FixedSize*FactoryImpl`: the analysis wanted `@FinalFields`, the write-once guard kept
`@Immutable(hc = true)`. A mutable partition holds two mutable collections, so `@FinalFields` is right. In scope,
the `Partition*` rows of the table state it; the rest is out of scope.

### F4: unsound eventual verdicts

EC has no eventual method at all (0 `@Mark`/`@Only`), yet the primitive `Synchronized*Collection` /
`Unmodifiable*Collection` wrappers, `UnmodifiableMutableCollection` and `SynchronizedRichIterable` get
`@Immutable(hc = true, after = "collection")`-style verdicts whose labels are field names: the vavr `MatchError`
shape that `EventualCluster.isGroundedInMark` was meant to close, so some path still writes an eventual verdict
without that gate. None of these types is in the hints' scope (`applyExpected` also drops eventual labels on an
annotated type).

### F5: the results writer dropped 42 API types (fixed 2026-09-29)

A source run on the current engine wrote 175 files and "3726 types", and `RichIterable`, `MutableList`,
`ImmutableList`, `MutableMap`, ... were not in them: `CodecImpl.encode` returned null for an element whose own
property stream was empty, discarding its members' encodings. Those interfaces now compute MUTABLE (the default,
so no type-level value) where the 09-25 engine gave `@FinalFields`; one value gone, the whole type gone, no
message. Fixed (null only when the members carry nothing either; `TestCodecMemberDataOnly`). Check a results
directory by per-package type counts, never by file count.

### F6: `super.<T>m()` defeats the detailed-source parser (8 files, no verdict impact)

`maddi-java-parser` rejects explicit type arguments on a `super.` call in eight `Synchronized*` files. javac
parses them, so the verdicts are unaffected; those files lose detailed sources (refactoring positions) only.

## How faithful the hint files are

Compiled hint JSON against the 2026-09-29 source run, over the values of the hinted types: 2,444 differ.

- **Intended (2,374):** 2,056 on the 119 types annotated with their expected verdict (the type's own
  annotations and the members that follow it), and 318 `nonModifyingMethod` values on the read-only interfaces
  (`RichIterable`, `ParallelIterable`, `Multimap`, ...) from the member rows: pure queries the analysis computed
  `@Modified`, stated `@NotModified`.
- **Encoding (about 70):** `@Independent` parameters and methods the compiler states as `@Independent(hc = true)`
  or leaves unstated, and conditional independence (`[level, {}, [Iterator.remove]]` in the source run) stated as
  its level. Same families as vavr's round trip.

The hints compiler logs 308 messages for `libs/eclipsecollections`, hierarchy inconsistencies from the side-load
compile context (`java.lang.Object` is `@Mutable`), as it does 311 for vavr; none is about these hints.

## Regenerating

1. Onboard and build per `corpus/catalogue/eclipse-collections.yml`.
2. Source run, `--analysis-results-dir <results>`, preloading `jdk,libs/test,libs/log`
   (`-PjvmArgs=-Dmaddi.workCeiling=30000000`; about 4 minutes).
3. `python3 corpus/scripts/ec-expected-table.py <results>` and paste its rows over the table below (the
   computed cells summarise the run; the expected cells are the rules in the script).
4. `./gradlew :maddi-aapi-parser:composeAnalysisHints -Dorg.gradle.offline=false`
   `-Pmaddi.compose.anchor=org.eclipse.collections.api.RichIterable`
   `-Pmaddi.compose.packages=org.eclipse.collections.api`
   `-Pmaddi.compose.exclude=org.eclipse.collections.api.block,.primitive`
   `-Pmaddi.compose.target=io.codelaser.maddi.aapi.archive.libs.eclipsecollections`
   `-Pmaddi.compose.out=<tmp> -Pmaddi.compose.preload=<results>`
   `-Pmaddi.compose.notes=<this file>`, then copy the `*.java` here.
5. `:maddi-aapi-archive:compileJava`, then `:maddi-aapi-parser:compileAnalysisHints`, then
   `TestAnalysisHintsCompiler` / `TestAnalysisHintsComposer`.

Registries: `compileOnly` of the EC 13.0.0 jars and `requires static org.eclipse.collections.api/.impl` in
`maddi-aapi-archive`; `testImplementation` in `maddi-aapi-parser`; `CompileAnalysisHints.SIDE_LOADED_LIBRARIES`
plus its inspector factory. The hint packages are not exported.

## The EXPECTED table

Type rows are name families as package-qualified prefixes; the first matching row wins. The computed cell
summarises the source run over the family's members (count in parentheses). Rows whose expected cell is words
keep their computed annotations.

| pattern | computed | expected | gap |
|---|---|---|---|
| `org.eclipse.collections.api.factory.ServiceLoaderUtils` | @FinalFields @Independent @Container (1) | no claim: a utility class |  |
| `org.eclipse.collections.api.factory.ThrowingInvocationHandler` | mutable @Dependent @Container (1) | no claim |  |
| `org.eclipse.collections.api.factory.*` | @Immutable(hc = true) (38) | @Immutable(hc = true) | F-factory: a factory is stateless; a `Mutable*Factory` creates mutable collections, it is not one |
| `org.eclipse.collections.api.tuple.*` | @Immutable(hc = true) (4) | @Immutable(hc = true) | a tuple is a value; `Pair.put(Map)` writes its argument, so no container |
| `org.eclipse.collections.api.bag.Immutable*` | mutable @Independent(hc = true) (conditional) (2) | @ImmutableContainer(hc = true) @Independent(hc = true) | G1: the interface is shared with lazy views and implemented through mutable delegates |
| `org.eclipse.collections.api.bag.Mutable*` | mutable @Dependent (2) | @Container | a mutable collection stores its arguments, never modifies them |
| `org.eclipse.collections.api.bag.MultiReader*` | mutable @Dependent (1) | @Container | a mutable collection behind a lock |
| `org.eclipse.collections.api.bag.sorted.Immutable*` | mutable @Independent(hc = true) (conditional) (1) | @ImmutableContainer(hc = true) @Independent(hc = true) | G1: the interface is shared with lazy views and implemented through mutable delegates |
| `org.eclipse.collections.api.bag.sorted.Mutable*` | mutable @Independent(hc = true) (conditional) (1) | @Container | a mutable collection stores its arguments, never modifies them |
| `org.eclipse.collections.api.bimap.Immutable*` | mutable @Dependent (1) | @ImmutableContainer(hc = true) @Independent(hc = true) | G1: the interface is shared with lazy views and implemented through mutable delegates |
| `org.eclipse.collections.api.bimap.Mutable*` | mutable @Dependent (1) | @Container | a mutable collection stores its arguments, never modifies them |
| `org.eclipse.collections.api.collection.Immutable*` | mutable @Independent(hc = true) (conditional) (1) | @ImmutableContainer(hc = true) @Independent(hc = true) | G1: the interface is shared with lazy views and implemented through mutable delegates |
| `org.eclipse.collections.api.collection.Mutable*` | mutable @Dependent (1) | @Container | a mutable collection stores its arguments, never modifies them |
| `org.eclipse.collections.api.list.Immutable*` | mutable @Dependent (1) | @ImmutableContainer(hc = true) @Independent(hc = true) | G1: the interface is shared with lazy views and implemented through mutable delegates |
| `org.eclipse.collections.api.list.Mutable*` | mutable @Dependent (1) | @Container | a mutable collection stores its arguments, never modifies them |
| `org.eclipse.collections.api.list.MultiReader*` | mutable @Dependent (1) | @Container | a mutable collection behind a lock |
| `org.eclipse.collections.api.map.Immutable*` | mutable @Dependent (3) | @ImmutableContainer(hc = true) @Independent(hc = true) | G1: the interface is shared with lazy views and implemented through mutable delegates |
| `org.eclipse.collections.api.map.Mutable*` | mutable @Dependent (3) | @Container | a mutable collection stores its arguments, never modifies them |
| `org.eclipse.collections.api.map.sorted.Immutable*` | mutable @Dependent (1) | @ImmutableContainer(hc = true) @Independent(hc = true) | G1: the interface is shared with lazy views and implemented through mutable delegates |
| `org.eclipse.collections.api.map.sorted.Mutable*` | mutable @Dependent (1) | @Container | a mutable collection stores its arguments, never modifies them |
| `org.eclipse.collections.api.multimap.Immutable*` | @FinalFields @Dependent (1) | @ImmutableContainer(hc = true) @Independent(hc = true) | G1: the interface is shared with lazy views and implemented through mutable delegates |
| `org.eclipse.collections.api.multimap.Mutable*` | @FinalFields @Dependent (1) | @Container | a mutable collection stores its arguments, never modifies them |
| `org.eclipse.collections.api.multimap.bag.Immutable*` | @FinalFields @Dependent (2) | @ImmutableContainer(hc = true) @Independent(hc = true) | G1: the interface is shared with lazy views and implemented through mutable delegates |
| `org.eclipse.collections.api.multimap.bag.Mutable*` | @FinalFields @Dependent (2) | @Container | a mutable collection stores its arguments, never modifies them |
| `org.eclipse.collections.api.multimap.list.Immutable*` | @FinalFields @Dependent (1) | @ImmutableContainer(hc = true) @Independent(hc = true) | G1: the interface is shared with lazy views and implemented through mutable delegates |
| `org.eclipse.collections.api.multimap.list.Mutable*` | @FinalFields @Dependent (1) | @Container | a mutable collection stores its arguments, never modifies them |
| `org.eclipse.collections.api.multimap.set.Immutable*` | @FinalFields @Dependent (2) | @ImmutableContainer(hc = true) @Independent(hc = true) | G1: the interface is shared with lazy views and implemented through mutable delegates |
| `org.eclipse.collections.api.multimap.set.Mutable*` | @FinalFields @Dependent (2) | @Container | a mutable collection stores its arguments, never modifies them |
| `org.eclipse.collections.api.multimap.sortedbag.Immutable*` | @FinalFields @Dependent (1) | @ImmutableContainer(hc = true) @Independent(hc = true) | G1: the interface is shared with lazy views and implemented through mutable delegates |
| `org.eclipse.collections.api.multimap.sortedbag.Mutable*` | @FinalFields @Dependent (1) | @Container | a mutable collection stores its arguments, never modifies them |
| `org.eclipse.collections.api.multimap.sortedset.Immutable*` | @FinalFields @Dependent (1) | @ImmutableContainer(hc = true) @Independent(hc = true) | G1: the interface is shared with lazy views and implemented through mutable delegates |
| `org.eclipse.collections.api.multimap.sortedset.Mutable*` | @FinalFields @Dependent (1) | @Container | a mutable collection stores its arguments, never modifies them |
| `org.eclipse.collections.api.partition.PartitionImmutable*` | @FinalFields @Dependent @Container (1) | @ImmutableContainer(hc = true) @Independent(hc = true) | G1: two immutable collections; computed through the implementor union |
| `org.eclipse.collections.api.partition.Partition*` | @FinalFields @Dependent @Container (2) | @FinalFields | F3: a partition holds two collections in final fields; the mutable and shared ones are not immutable |
| `org.eclipse.collections.api.partition.bag.PartitionImmutable*` | @FinalFields @Dependent @Container (2) | @ImmutableContainer(hc = true) @Independent(hc = true) | G1: two immutable collections; computed through the implementor union |
| `org.eclipse.collections.api.partition.bag.Partition*` | @FinalFields @Dependent @Container (4) | @FinalFields | F3: a partition holds two collections in final fields; the mutable and shared ones are not immutable |
| `org.eclipse.collections.api.partition.bag.sorted.PartitionImmutable*` | @FinalFields @Dependent @Container (1) | @ImmutableContainer(hc = true) @Independent(hc = true) | G1: two immutable collections; computed through the implementor union |
| `org.eclipse.collections.api.partition.bag.sorted.Partition*` | @FinalFields @Dependent @Container (2) | @FinalFields | F3: a partition holds two collections in final fields; the mutable and shared ones are not immutable |
| `org.eclipse.collections.api.partition.list.PartitionImmutable*` | @FinalFields @Dependent @Container (1) | @ImmutableContainer(hc = true) @Independent(hc = true) | G1: two immutable collections; computed through the implementor union |
| `org.eclipse.collections.api.partition.list.Partition*` | @FinalFields @Dependent @Container (2) | @FinalFields | F3: a partition holds two collections in final fields; the mutable and shared ones are not immutable |
| `org.eclipse.collections.api.partition.ordered.Partition*` | @FinalFields @Dependent @Container (3) | @FinalFields | F3: a partition holds two collections in final fields; the mutable and shared ones are not immutable |
| `org.eclipse.collections.api.partition.set.PartitionImmutable*` | @FinalFields @Dependent @Container (2) | @ImmutableContainer(hc = true) @Independent(hc = true) | G1: two immutable collections; computed through the implementor union |
| `org.eclipse.collections.api.partition.set.Partition*` | @FinalFields @Dependent @Container (4) | @FinalFields | F3: a partition holds two collections in final fields; the mutable and shared ones are not immutable |
| `org.eclipse.collections.api.partition.set.sorted.PartitionImmutable*` | @FinalFields @Dependent @Container (1) | @ImmutableContainer(hc = true) @Independent(hc = true) | G1: two immutable collections; computed through the implementor union |
| `org.eclipse.collections.api.partition.set.sorted.Partition*` | @FinalFields @Dependent @Container (2) | @FinalFields | F3: a partition holds two collections in final fields; the mutable and shared ones are not immutable |
| `org.eclipse.collections.api.partition.stack.PartitionImmutable*` | @FinalFields @Dependent @Container (1) | @ImmutableContainer(hc = true) @Independent(hc = true) | G1: two immutable collections; computed through the implementor union |
| `org.eclipse.collections.api.partition.stack.Partition*` | @Immutable(hc = true) (1), @FinalFields @Dependent @Container (1) | @FinalFields | F3: a partition holds two collections in final fields; the mutable and shared ones are not immutable |
| `org.eclipse.collections.api.set.Immutable*` | mutable @Dependent (2) | @ImmutableContainer(hc = true) @Independent(hc = true) | G1: the interface is shared with lazy views and implemented through mutable delegates |
| `org.eclipse.collections.api.set.Mutable*` | mutable @Dependent (2) | @Container | a mutable collection stores its arguments, never modifies them |
| `org.eclipse.collections.api.set.MultiReader*` | mutable @Dependent (1) | @Container | a mutable collection behind a lock |
| `org.eclipse.collections.api.set.sorted.Immutable*` | mutable @Independent(hc = true) (conditional) (1) | @ImmutableContainer(hc = true) @Independent(hc = true) | G1: the interface is shared with lazy views and implemented through mutable delegates |
| `org.eclipse.collections.api.set.sorted.Mutable*` | mutable @Dependent (1) | @Container | a mutable collection stores its arguments, never modifies them |
| `org.eclipse.collections.api.stack.Immutable*` | mutable @Dependent (1) | @ImmutableContainer(hc = true) @Independent(hc = true) | G1: the interface is shared with lazy views and implemented through mutable delegates |
| `org.eclipse.collections.api.stack.Mutable*` | mutable @Independent(hc = true) (conditional) (1) | @Container | a mutable collection stores its arguments, never modifies them |
| `org.eclipse.collections.api.BooleanIterable#allSatisfy` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.BooleanIterable#anySatisfy` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.BooleanIterable#contains` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.BooleanIterable#containsAll` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.BooleanIterable#count` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.BooleanIterable#detectIfNone` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.BooleanIterable#injectInto` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.BooleanIterable#noneSatisfy` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.BooleanIterable#toArray` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.BooleanIterable#toBag` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.BooleanIterable#toList` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.BooleanIterable#toSet` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ByteIterable#allSatisfy` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ByteIterable#anySatisfy` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ByteIterable#average` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ByteIterable#contains` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ByteIterable#containsAll` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ByteIterable#count` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ByteIterable#detectIfNone` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ByteIterable#injectInto` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ByteIterable#max` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ByteIterable#median` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ByteIterable#min` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ByteIterable#noneSatisfy` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ByteIterable#sum` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ByteIterable#toArray` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ByteIterable#toBag` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ByteIterable#toList` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ByteIterable#toSet` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ByteIterable#toSortedList` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.CharIterable#allSatisfy` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.CharIterable#anySatisfy` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.CharIterable#average` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.CharIterable#contains` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.CharIterable#containsAll` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.CharIterable#count` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.CharIterable#detectIfNone` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.CharIterable#injectInto` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.CharIterable#max` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.CharIterable#median` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.CharIterable#min` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.CharIterable#noneSatisfy` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.CharIterable#sum` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.CharIterable#toArray` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.CharIterable#toBag` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.CharIterable#toList` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.CharIterable#toSet` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.CharIterable#toSortedList` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.DoubleIterable#allSatisfy` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.DoubleIterable#anySatisfy` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.DoubleIterable#average` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.DoubleIterable#contains` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.DoubleIterable#containsAll` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.DoubleIterable#count` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.DoubleIterable#detectIfNone` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.DoubleIterable#injectInto` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.DoubleIterable#max` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.DoubleIterable#median` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.DoubleIterable#min` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.DoubleIterable#noneSatisfy` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.DoubleIterable#sum` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.DoubleIterable#toArray` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.DoubleIterable#toBag` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.DoubleIterable#toList` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.DoubleIterable#toSet` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.DoubleIterable#toSortedList` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.FloatIterable#allSatisfy` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.FloatIterable#anySatisfy` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.FloatIterable#average` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.FloatIterable#contains` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.FloatIterable#containsAll` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.FloatIterable#count` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.FloatIterable#detectIfNone` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.FloatIterable#injectInto` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.FloatIterable#max` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.FloatIterable#median` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.FloatIterable#min` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.FloatIterable#noneSatisfy` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.FloatIterable#sum` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.FloatIterable#toArray` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.FloatIterable#toBag` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.FloatIterable#toList` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.FloatIterable#toSet` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.FloatIterable#toSortedList` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.IntIterable#allSatisfy` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.IntIterable#anySatisfy` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.IntIterable#average` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.IntIterable#contains` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.IntIterable#containsAll` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.IntIterable#count` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.IntIterable#detectIfNone` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.IntIterable#injectInto` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.IntIterable#max` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.IntIterable#median` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.IntIterable#min` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.IntIterable#noneSatisfy` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.IntIterable#sum` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.IntIterable#toArray` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.IntIterable#toBag` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.IntIterable#toList` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.IntIterable#toSet` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.IntIterable#toSortedList` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.LazyIterable#getFirst` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.LazyIterable#toArray` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.LongIterable#allSatisfy` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.LongIterable#anySatisfy` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.LongIterable#average` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.LongIterable#contains` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.LongIterable#containsAll` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.LongIterable#count` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.LongIterable#detectIfNone` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.LongIterable#injectInto` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.LongIterable#max` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.LongIterable#median` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.LongIterable#min` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.LongIterable#noneSatisfy` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.LongIterable#sum` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.LongIterable#toArray` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.LongIterable#toBag` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.LongIterable#toList` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.LongIterable#toSet` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.LongIterable#toSortedList` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ParallelIterable#allSatisfy` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ParallelIterable#allSatisfyWith` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ParallelIterable#anySatisfy` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ParallelIterable#anySatisfyWith` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ParallelIterable#appendString` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ParallelIterable#count` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ParallelIterable#countWith` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ParallelIterable#detect` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ParallelIterable#detectIfNone` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ParallelIterable#detectWith` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ParallelIterable#makeString` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ParallelIterable#max` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ParallelIterable#maxBy` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ParallelIterable#min` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ParallelIterable#minBy` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ParallelIterable#noneSatisfy` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ParallelIterable#noneSatisfyWith` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ParallelIterable#sumOfDouble` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ParallelIterable#sumOfFloat` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ParallelIterable#sumOfInt` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ParallelIterable#sumOfLong` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ParallelIterable#toArray` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ParallelIterable#toBag` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ParallelIterable#toList` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ParallelIterable#toMap` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ParallelIterable#toSet` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ParallelIterable#toSortedList` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ParallelIterable#toSortedMap` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ParallelIterable#toSortedSet` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.PrimitiveIterable#appendString` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.PrimitiveIterable#isEmpty` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.PrimitiveIterable#makeString` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.PrimitiveIterable#notEmpty` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.PrimitiveIterable#size` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.PrimitiveIterable#toString` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.RichIterable#allSatisfy` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.RichIterable#allSatisfyWith` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.RichIterable#anySatisfy` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.RichIterable#anySatisfyWith` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.RichIterable#appendString` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.RichIterable#contains` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.RichIterable#containsAll` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.RichIterable#containsAllArguments` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.RichIterable#containsAllIterable` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.RichIterable#count` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.RichIterable#countWith` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.RichIterable#detect` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.RichIterable#detectIfNone` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.RichIterable#detectWith` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.RichIterable#getAny` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.RichIterable#getFirst` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.RichIterable#getLast` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.RichIterable#getOnly` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.RichIterable#injectInto` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.RichIterable#isEmpty` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.RichIterable#makeString` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.RichIterable#max` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.RichIterable#maxBy` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.RichIterable#min` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.RichIterable#minBy` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.RichIterable#noneSatisfy` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.RichIterable#noneSatisfyWith` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.RichIterable#notEmpty` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.RichIterable#size` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.RichIterable#sumOfDouble` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.RichIterable#sumOfFloat` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.RichIterable#sumOfInt` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.RichIterable#sumOfLong` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.RichIterable#toArray` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.RichIterable#toBag` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.RichIterable#toList` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.RichIterable#toMap` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.RichIterable#toSet` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.RichIterable#toSortedList` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.RichIterable#toSortedMap` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.RichIterable#toSortedSet` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.RichIterable#toString` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ShortIterable#allSatisfy` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ShortIterable#anySatisfy` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ShortIterable#average` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ShortIterable#contains` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ShortIterable#containsAll` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ShortIterable#count` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ShortIterable#detectIfNone` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ShortIterable#injectInto` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ShortIterable#max` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ShortIterable#median` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ShortIterable#min` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ShortIterable#noneSatisfy` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ShortIterable#sum` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ShortIterable#toArray` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ShortIterable#toBag` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ShortIterable#toList` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ShortIterable#toSet` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ShortIterable#toSortedList` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.bag.Bag#equals` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.bag.Bag#hashCode` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.bag.Bag#occurrencesOf` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.bag.Bag#sizeDistinct` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.list.ListIterable#equals` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.list.ListIterable#get` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.list.ListIterable#getFirst` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.list.ListIterable#getLast` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.list.ListIterable#hashCode` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.list.ListIterable#lastIndexOf` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.list.MutableList#getFirst` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.list.MutableList#getLast` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.map.MapIterable#containsKey` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.map.MapIterable#containsValue` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.map.MapIterable#detect` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.map.MapIterable#equals` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.map.MapIterable#get` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.map.MapIterable#getIfAbsent` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.map.MapIterable#getIfAbsentValue` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.map.MapIterable#hashCode` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.map.MapIterable#toString` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.map.sorted.MutableSortedMap#getIfAbsentPutWith` | @NotModified | @Modified | M1: a mutator |
| `org.eclipse.collections.api.multimap.Multimap#containsKey` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.multimap.Multimap#containsValue` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.multimap.Multimap#equals` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.multimap.Multimap#get` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.multimap.Multimap#hashCode` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.multimap.Multimap#isEmpty` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.multimap.Multimap#notEmpty` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.multimap.Multimap#size` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.multimap.Multimap#sizeDistinct` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.multimap.Multimap#toMap` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.multimap.Multimap#valuesView` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.multimap.MutableMultimap#get` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.multimap.MutableMultimap#putAll` | @NotModified | @Modified | M1: a mutator |
| `org.eclipse.collections.api.multimap.bag.MutableBagIterableMultimap#removeAll` | @NotModified | @Modified | M1: a mutator |
| `org.eclipse.collections.api.multimap.bag.MutableBagMultimap#removeAll` | @NotModified | @Modified | M1: a mutator |
| `org.eclipse.collections.api.multimap.list.MutableListMultimap#removeAll` | @NotModified | @Modified | M1: a mutator |
| `org.eclipse.collections.api.multimap.set.MutableSetIterableMultimap#removeAll` | @NotModified | @Modified | M1: a mutator |
| `org.eclipse.collections.api.multimap.set.MutableSetMultimap#removeAll` | @NotModified | @Modified | M1: a mutator |
| `org.eclipse.collections.api.multimap.sortedbag.MutableSortedBagMultimap#removeAll` | @NotModified | @Modified | M1: a mutator |
| `org.eclipse.collections.api.multimap.sortedset.MutableSortedSetMultimap#removeAll` | @NotModified | @Modified | M1: a mutator |
| `org.eclipse.collections.api.ordered.OrderedIterable#getFirst` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ordered.OrderedIterable#getLast` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ordered.OrderedIterable#indexOf` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ordered.OrderedIterable#max` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.ordered.OrderedIterable#min` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.set.Pool#get` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.set.SetIterable#equals` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.set.SetIterable#hashCode` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.stack.StackIterable#equals` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.stack.StackIterable#getFirst` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.stack.StackIterable#getLast` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.stack.StackIterable#hashCode` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.stack.StackIterable#peek` | @Modified | @NotModified | M2: a pure query |
| `org.eclipse.collections.api.stack.StackIterable#toString` | @Modified | @NotModified | M2: a pure query |
