# vavr 1.0.1: analysis hints and analysis state

Status: 2026-09-25. Covers the four hint files next to this report (`IoVavr`, `IoVavrCollection`,
`IoVavrControl`, `IoVavrConcurrent`) and their compiled results in
`src/main/resources/.../analyzedPackageFiles/libs/vavr`.

These hints are **what maddi computes** for vavr, not what vavr means. Every public type whose computed
verdict differs from the semantically expected one carries an `// EXPECTED ...` comment in its shadow, taken
from the table at the end of this report. The families of differences (G1–G6) are the engine's worklist
for vavr.

## Using the hints

vavr is **side-loaded**: its results are not in `libs.jar` and not in `LoadAnalysisResults.ANALYZED_RESULTS`,
so a maddi run does not load them by default. Preload them explicitly, together with the JDK hints:

```
--preload-analysis-results-dirs <archive>/analyzedPackageFiles/jdk,<archive>/analyzedPackageFiles/libs/vavr
```

where `<archive>` is `maddi-aapi-archive/src/main/resources/io/codelaser/maddi/aapi/archive`. A consumer then
sees vavr's public API with maddi's computed verdicts, including their weak spots below (read G6 first).

## Where vavr stands

236 public types (nested ones included). Computed type immutability: 5 `@Immutable`, 74 `@Immutable(hc = true)`,
156 `@FinalFields`, 1 mutable.

**As expected (177 types):** `Option`, `Some`, `None`, `Either` with `Left`, `Right` and both projections,
`Validation` with its builders, `Try.Success`, `Tuple`/`Tuple0..8`, the hash-trie and red-black internals,
`Tree.Empty`, `Ordered`, the multimap builders, the utilities (`API`, `Predicates`, `HashCodes`), and all
functional interfaces (`Function0..8`, `CheckedFunction0..8`, `PartialFunction`, ...), which by the JDK-hint
convention make no immutability claim.

**Not as expected:** every persistent collection. `List`, `Vector`, `HashMap`, `HashSet`, `Queue`, `Array`,
`TreeMap`, `Stream` and the rest compute `@FinalFields`, most of them `@Dependent`, where vavr's own contract is
"persistent, immutable". The 59 rows of the table below are the types that need an explanation: 35 are gaps,
the other 24 are cases where the computed verdict is right, or follows another type's, for a reason
worth stating.

## The gap families

### G1: a stateful type shares the collection interfaces (32 types)

vavr's `Iterator` is itself a `Value` and a `Traversable`, and so are `Future` and `Lazy`. maddi takes an abstract
method's modification as the union over its implementations. Where `Iterator`, `Future` or `Lazy` implement it by
consuming, completing or computing, the abstract method is modifying. Currently `Value.isEmpty()` (through
`Iterator.isEmpty() = !hasNext()` and `Future.isEmpty()`) and `Value.get()` (through `Iterator`, `Future`, `Lazy`,
`Function0`). That union is right for the abstract method. It is too coarse for a persistent receiver:

- `Value.toQueue()` (and `toList()`, `toArray()`, ...) calls `ValueModule.toTraversable(this, ...)`, which calls
  `value.isEmpty()` and `value.get()` on its argument. So every conversion "modifies" its receiver, whatever it is.
- A `Cons` method reaching such a default on its `tail` makes the field `Cons.tail` modified. The constructor
  `Cons(head, tail)` stores its `tail` parameter there, so that parameter is modified too. And
  `List.prepend(element) = new Cons<>(element, this)` then "modifies" `this`.
- Result: 47 of `List`'s 149 methods compute as modifying (`append`, `prepend`, `remove`, `sorted`, `take*`,
  ...), 58 of `Vector`'s 148, 49 of `HashMap`'s 106. The persistent collections are capped at `@FinalFields`,
  and the collection interfaces (`Seq`, `Set`, `Map`, ...) with them.

`Value`, `Traversable` and `Foldable` themselves are **right** at `@FinalFields`: they are implemented by
stateful types. The gap is that their persistent implementations inherit the union. The engine lever is to judge
a call on a parameter or on `this` by the concrete receiver type (a per-receiver summary of which methods a
parameter is modified through), not by the abstract method.

### G2: persistent collections are @Dependent (22 types)

`List`, `Vector`, `HashMap`, `HashSet`, `Seq`, `Map`, ... compute `@Dependent` at type level, where a persistent
collection shares structure only with other immutable nodes: `@Independent(hc = true)` is expected. This
verdict is computed since the type-independence fix of 2026-09-24 (before it, it was frozen optimistic and
unreliable). Not traced for this report; it may largely be G1 again (a modifying method can link its receiver to
what it returns), but that is unverified.

### G3: memoizing and completing types (Lazy, Future, the Stream family; 8 downstream)

`Lazy` evaluates its supplier once, `Future` completes once, and `Stream` memoizes its tail. All three are
observationally immutable after the fact, which maddi expresses as an *eventual* verdict (`@Immutable(after =
...)`). But eventual verdicts need a mark (`@Mark`, `@Only`), and vavr has none: the eventual cluster now refuses
to certify a type without a real transition behind it (2026-09-25, after vavr showed 119 ungrounded verdicts).
Closing G3 needs marks (hand-written hints on vavr's `Lazy`/`Future` internals) or a notion of benign
memoization. The 8 `API.For*Future` comprehension carriers follow `Future`.

### G4: types holding a Throwable (5 types, 8 downstream; computed is right)

`Try.Failure`, `Either.Failure`, `MatchError`, `NotImplementedError` hold a `Throwable`, whose stack trace is
mutable, so `@FinalFields` is honest, and `Try` is `@FinalFields` because of `Failure`. The 8 `API.For*Try`
carriers follow. Listed so that nobody "fixes" them.

### G5: `Patterns` is not recognised as a utility class (1 type)

`io.vavr.Patterns` holds only static extractors (`$Cons`, `$Tuple2`, ...) and static final pattern constants.
It computes `@FinalFields`, not `@UtilityClass`. Untraced.

### G6: computed too optimistic (read this first as a consumer)

`Iterator.length()` computes `@NotModified`, but it consumes the iterator: it inherits `Traversable`'s
`foldLeft`, which iterates `iterator()`, and for an `Iterator` that returns `this`. The hints therefore claim that
`length()` leaves an iterator untouched. This is the unsafe direction. It is likely a family (every
`Traversable` default that iterates `iterator()`, as seen on an `Iterator`), not yet traced. Treat any
`@NotModified` on a method of `io.vavr.collection.Iterator` with suspicion until this is resolved.

## How faithful the hint files are

The hint files are generated from a maddi source analysis of vavr and compiled back to results. On the public
API, the compiled hints reproduce the computed verdicts except for 144 elements:

- about 90 are encoding-equivalent: methods returning `String`, a primitive or `void`, parameters of primitive,
  `String` or `Class` type, and constructors, where one side stores a value the other treats as implied;
- about 50 are the extractor methods of `io.vavr.Patterns` (`$Cons`, `$Tuple2`, ...), which lose their
  `@NotModified` in compilation (untraced, see "Tooling defects").

Before this was made explicit, the round trip lost 1,254 verdicts, most of them in the optimistic direction; see
"Tooling defects".

## Regenerating

1. Source analysis of vavr (`~/git/test-oss/vavr`, main sources only; `corpus/scripts/vavr-main-config.py`
   derives the input configuration):
   ```
   ./gradlew :maddi-run-openjdk:run -PjvmArgs="-Dmaddi.workCeiling=30000000" --args="--input-configuration \
     <test-oss>/vavr/inputConfiguration.main.json --preload-analysis-results-dirs <archive>/analyzedPackageFiles/jdk,\
     <archive>/analyzedPackageFiles/libs/test,<archive>/analyzedPackageFiles/libs/log --analysis-steps prep,modification \
     --analysis-results-dir <vavr-results>"
   ```
2. Compose the hint sources from the vavr jar, with the computed verdicts loaded and this report's table as
   `// EXPECTED` comments:
   ```
   ./gradlew :maddi-aapi-parser:composeAnalysisHints \
     -Pmaddi.compose.anchor=io.vavr.Value,io.vavr.match.annotation.Patterns -Pmaddi.compose.packages=io.vavr \
     -Pmaddi.compose.target=io.codelaser.maddi.aapi.archive.libs.vavr -Pmaddi.compose.out=../maddi-aapi-archive/src/main/java \
     -Pmaddi.compose.preload=<vavr-results> \
     -Pmaddi.compose.notes=../maddi-aapi-archive/src/main/java/io/codelaser/maddi/aapi/archive/libs/vavr/VAVR.md
   ```
   Then hand-qualify the one site the printer gets wrong: `Stream.Empty<T> instance()` in `IoVavrCollection`
   must read `io.vavr.collection.Stream.Empty<T>` (see "Tooling defects").
3. `./gradlew :maddi-aapi-archive:compileJava` (the shadows must compile), then
   `./gradlew :maddi-aapi-parser:compileAnalysisHints`. `TestAnalysisHintsCompiler` checks the committed results.

## Tooling defects found on the way

Fixed alongside these hints:

- `AnalysisHintsComposer` printed an interface's `<clinit>` (vavr's `Future` has static fields) as a method,
  making the file uncompilable. `isStaticInitializer()` does not recognise it from byte code.
- The composer resolved imports from the library jar's own source set, which sees no type in any package. So it
  printed clashing star imports (`java.util.*` and `io.vavr.collection.*` both hold a `BitSet`, an `Iterator`, a
  `TreeSet`, a `SortedSet`): 200 compile errors. `write` now takes the source set to resolve imports from.
- `DecoratorImpl` omitted verdicts it considered defaults, but the hints compiler's defaults differ: an
  unannotated parameter of type `T` reads back `@NotModified`, and an unannotated `@FinalFields` type reads back
  mutable. For vavr that turned 731 computed-modified parameters and 143 dependent ones into optimistic claims,
  and 156 `@FinalFields` types into mutable ones. An explicit mode now prints every computed verdict
  (`@Modified`, `@FinalFields`, `@Independent(absent = true)`), and `ComposeAnalysisHints` uses it.
- `LoadAnalysisResults` could not load a source analysis' results (internal properties such as `links` have no
  decoder, and `downcastParameter` needs types still being loaded). It now takes a property-key filter.

Open:

- The printer does not qualify a nested type whose outer simple name clashes with an import
  (`Stream.Empty` while `java.util.stream.Stream` is imported). One site, hand-fixed.
- The `@NotModified` on `io.vavr.Patterns`' extractor methods and their parameters does not survive compilation.
- Bazel globs these hint sources but has no vavr dependency (as for the existing Kotlin hints).

## Types that need an explanation

`computed` is maddi's verdict in these hints; `expected` is vavr's semantic intent; `gap` names the family above.
The first column is read by `ComposeAnalysisHints` (`-Pmaddi.compose.notes`): keep one type per row, fully
qualified, in backticks.

| type | computed | expected | gap |
|---|---|---|---|
| `io.vavr.collection.Array` | @FinalFields @Dependent | @ImmutableContainer(hc = true) @Independent(hc = true) | G1, G2: persistent collection |
| `io.vavr.collection.BitSet` | @FinalFields @Independent(hc = true) | @ImmutableContainer(hc = true) @Independent(hc = true) | G1: persistent collection |
| `io.vavr.collection.CharSeq` | @FinalFields @Dependent | @ImmutableContainer | G1, G2: a persistent sequence of chars: no hidden content |
| `io.vavr.collection.HashMap` | @FinalFields @Dependent | @ImmutableContainer(hc = true) @Independent(hc = true) | G1, G2: persistent collection |
| `io.vavr.collection.HashMultimap` | @FinalFields @Container @Independent | @ImmutableContainer(hc = true) @Independent(hc = true) | G1: persistent collection |
| `io.vavr.collection.HashSet` | @FinalFields @Dependent | @ImmutableContainer(hc = true) @Independent(hc = true) | G1, G2: persistent collection |
| `io.vavr.collection.IndexedSeq` | @FinalFields @Independent(hc = true) | @ImmutableContainer(hc = true) @Independent(hc = true) | G1: implemented by persistent collections only |
| `io.vavr.collection.LinearSeq` | @FinalFields @Dependent | @ImmutableContainer(hc = true) @Independent(hc = true) | G1, G2: implemented by persistent collections only |
| `io.vavr.collection.LinkedHashMap` | @FinalFields @Dependent | @ImmutableContainer(hc = true) @Independent(hc = true) | G1, G2: persistent collection |
| `io.vavr.collection.LinkedHashMultimap` | @FinalFields @Container @Independent | @ImmutableContainer(hc = true) @Independent(hc = true) | G1: persistent collection |
| `io.vavr.collection.LinkedHashSet` | @FinalFields @Dependent | @ImmutableContainer(hc = true) @Independent(hc = true) | G1, G2: persistent collection |
| `io.vavr.collection.List` | @FinalFields @Dependent | @ImmutableContainer(hc = true) @Independent(hc = true) | G1, G2: persistent collection |
| `io.vavr.collection.List.Cons` | @FinalFields @Container @Independent(hc = true) | @ImmutableContainer(hc = true) @Independent(hc = true) | G1: persistent collection |
| `io.vavr.collection.List.Nil` | @FinalFields @Container @Dependent | @ImmutableContainer(hc = true) @Independent(hc = true) | G1, G2: persistent collection |
| `io.vavr.collection.Map` | @FinalFields @Dependent | @ImmutableContainer(hc = true) @Independent(hc = true) | G1, G2: implemented by persistent collections only |
| `io.vavr.collection.Multimap` | @FinalFields @Dependent | @ImmutableContainer(hc = true) @Independent(hc = true) | G1, G2: implemented by persistent collections only |
| `io.vavr.collection.PriorityQueue` | @FinalFields @Dependent | @ImmutableContainer(hc = true) @Independent(hc = true) | G1, G2: persistent collection |
| `io.vavr.collection.Queue` | @FinalFields @Dependent | @ImmutableContainer(hc = true) @Independent(hc = true) | G1, G2: persistent collection |
| `io.vavr.collection.Seq` | @FinalFields @Dependent | @ImmutableContainer(hc = true) @Independent(hc = true) | G1, G2: implemented by persistent collections only |
| `io.vavr.collection.Set` | @FinalFields @Dependent | @ImmutableContainer(hc = true) @Independent(hc = true) | G1, G2: implemented by persistent collections only |
| `io.vavr.collection.SortedMap` | @FinalFields @Dependent | @ImmutableContainer(hc = true) @Independent(hc = true) | G1, G2: implemented by persistent collections only |
| `io.vavr.collection.SortedMultimap` | @FinalFields @Container @Dependent | @ImmutableContainer(hc = true) @Independent(hc = true) | G1, G2: implemented by persistent collections only |
| `io.vavr.collection.SortedSet` | @FinalFields @Dependent | @ImmutableContainer(hc = true) @Independent(hc = true) | G1, G2: implemented by persistent collections only |
| `io.vavr.collection.Stream` | @FinalFields @Dependent | @ImmutableContainer(hc = true) @Independent(hc = true) | G1, G3, G2: persistent, lazily evaluated (memoizing tail) |
| `io.vavr.collection.Stream.Cons` | @FinalFields @Independent | @ImmutableContainer(hc = true) @Independent(hc = true) | G1, G3: persistent, lazily evaluated (memoizing tail) |
| `io.vavr.collection.Stream.Empty` | @FinalFields @Container @Dependent | @ImmutableContainer(hc = true) @Independent(hc = true) | G1, G3, G2: persistent, lazily evaluated (memoizing tail) |
| `io.vavr.collection.Tree` | @FinalFields @Dependent | @ImmutableContainer(hc = true) @Independent(hc = true) | G1, G2: persistent collection |
| `io.vavr.collection.Tree.Node` | @FinalFields @Independent | @ImmutableContainer(hc = true) @Independent(hc = true) | G1: persistent collection |
| `io.vavr.collection.TreeMap` | @FinalFields @Independent | @ImmutableContainer(hc = true) @Independent(hc = true) | G1: persistent collection |
| `io.vavr.collection.TreeMultimap` | @FinalFields @Container @Independent | @ImmutableContainer(hc = true) @Independent(hc = true) | G1: persistent collection |
| `io.vavr.collection.TreeSet` | @FinalFields @Independent | @ImmutableContainer(hc = true) @Independent(hc = true) | G1: persistent collection |
| `io.vavr.collection.Vector` | @FinalFields @Dependent | @ImmutableContainer(hc = true) @Independent(hc = true) | G1, G2: persistent collection |
| `io.vavr.Value` | @FinalFields @Dependent | no immutability claim | root of G1 (computed is right): also implemented by Iterator, Future, Lazy: stateful |
| `io.vavr.collection.Foldable` | @FinalFields @Independent(hc = true) | no immutability claim | root of G1 (computed is right): also implemented by Iterator, Future, Lazy: stateful |
| `io.vavr.collection.Traversable` | @FinalFields @Dependent | no immutability claim | root of G1 (computed is right): also implemented by Iterator, Future, Lazy: stateful |
| `io.vavr.Lazy` | mutable @Independent(hc = true) | eventually @Immutable(hc = true), after evaluation | G3: memoizes its supplier once |
| `io.vavr.concurrent.Future` | @FinalFields @Dependent | eventually @Immutable(hc = true), after completion | G3: completes once |
| `io.vavr.API.For1Future` | @FinalFields @Independent | @Immutable(hc = true), once Future is (G3) | downstream of G3: a comprehension over Futures |
| `io.vavr.API.For2Future` | @FinalFields @Independent | @Immutable(hc = true), once Future is (G3) | downstream of G3: a comprehension over Futures |
| `io.vavr.API.For3Future` | @FinalFields @Independent | @Immutable(hc = true), once Future is (G3) | downstream of G3: a comprehension over Futures |
| `io.vavr.API.For4Future` | @FinalFields @Independent | @Immutable(hc = true), once Future is (G3) | downstream of G3: a comprehension over Futures |
| `io.vavr.API.For5Future` | @FinalFields @Independent | @Immutable(hc = true), once Future is (G3) | downstream of G3: a comprehension over Futures |
| `io.vavr.API.For6Future` | @FinalFields @Independent | @Immutable(hc = true), once Future is (G3) | downstream of G3: a comprehension over Futures |
| `io.vavr.API.For7Future` | @FinalFields @Independent | @Immutable(hc = true), once Future is (G3) | downstream of G3: a comprehension over Futures |
| `io.vavr.API.For8Future` | @FinalFields @Independent | @Immutable(hc = true), once Future is (G3) | downstream of G3: a comprehension over Futures |
| `io.vavr.MatchError` | @FinalFields @Container @Dependent | @FinalFields | G4 (computed is right): holds a Throwable, which is mutable |
| `io.vavr.NotImplementedError` | @FinalFields @Container @Dependent | @FinalFields | G4 (computed is right): holds a Throwable, which is mutable |
| `io.vavr.control.Either.Failure` | @FinalFields @Container @Dependent | @FinalFields | G4 (computed is right): holds a Throwable, which is mutable |
| `io.vavr.control.Try` | @FinalFields @Dependent | @FinalFields | G4 (computed is right): holds a Throwable, which is mutable |
| `io.vavr.control.Try.Failure` | @FinalFields @Container @Dependent | @FinalFields | G4 (computed is right): holds a Throwable, which is mutable |
| `io.vavr.API.For1Try` | @FinalFields @Dependent | @FinalFields | downstream of G4 (computed is right): a comprehension over Try (Failure holds a Throwable) |
| `io.vavr.API.For2Try` | @FinalFields @Dependent | @FinalFields | downstream of G4 (computed is right): a comprehension over Try (Failure holds a Throwable) |
| `io.vavr.API.For3Try` | @FinalFields @Dependent | @FinalFields | downstream of G4 (computed is right): a comprehension over Try (Failure holds a Throwable) |
| `io.vavr.API.For4Try` | @FinalFields @Dependent | @FinalFields | downstream of G4 (computed is right): a comprehension over Try (Failure holds a Throwable) |
| `io.vavr.API.For5Try` | @FinalFields @Dependent | @FinalFields | downstream of G4 (computed is right): a comprehension over Try (Failure holds a Throwable) |
| `io.vavr.API.For6Try` | @FinalFields @Dependent | @FinalFields | downstream of G4 (computed is right): a comprehension over Try (Failure holds a Throwable) |
| `io.vavr.API.For7Try` | @FinalFields @Dependent | @FinalFields | downstream of G4 (computed is right): a comprehension over Try (Failure holds a Throwable) |
| `io.vavr.API.For8Try` | @FinalFields @Dependent | @FinalFields | downstream of G4 (computed is right): a comprehension over Try (Failure holds a Throwable) |
| `io.vavr.Patterns` | @FinalFields @Independent | @UtilityClass / @Immutable | G5: static extractors only |
