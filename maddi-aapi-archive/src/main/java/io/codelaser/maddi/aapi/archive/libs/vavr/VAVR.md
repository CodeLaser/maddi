# vavr 1.0.1: analysis hints and analysis state

Status: 2026-09-25. Covers the four hint files next to this report (`IoVavr`, `IoVavrCollection`,
`IoVavrControl`, `IoVavrConcurrent`) and their compiled results in
`src/main/resources/.../analyzedPackageFiles/libs/vavr`.

These hints state **what vavr means**, as the JDK hints do for the JDK: that is what a client's analysis needs to
compute immutability in real code. They start from maddi's source analysis of vavr; where the computed verdict
differs from vavr's semantic contract, the shadow carries the EXPECTED annotations from the table at the end of
this report, with the computed verdict kept in a comment. The differences (G1–G6) are the engine's worklist: the
day the analysis computes a row's expected verdict, that row can go.

## Using the hints

vavr is **side-loaded**: its results are not in `libs.jar` and not in `LoadAnalysisResults.ANALYZED_RESULTS`,
so a maddi run does not load them by default. Preload them explicitly, together with the JDK hints:

```
--preload-analysis-results-dirs <archive>/analyzedPackageFiles/jdk,<archive>/analyzedPackageFiles/libs/vavr
```

where `<archive>` is `maddi-aapi-archive/src/main/resources/io/codelaser/maddi/aapi/archive`. A consumer then
sees vavr's public API with its semantic verdicts: the persistent collections as `@ImmutableContainer(hc = true)`,
`Iterator`'s consuming methods as `@Modified`. Where the table says something a hint cannot state (an eventual
verdict, or one conditional on another type), the computed verdict stays; see "What the hints do not state".

## Reading a shadow

```java
    //public final class Array implements IndexedSeq<T>, Serializable
    //annotated as EXPECTED; computed @FinalFields @Dependent -- G1, G2: persistent collection (VAVR.md)
    @ImmutableContainer(hc = true)
    class Array$<T> {
        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent(hc = true) @NotModified
        Array<T> append(@Independent @NotModified T element) { return null; }
```

- **The declaration comment** is the library type's own header, so a reader sees what the shadow stands for.
- **The annotations are the only part compiled**, and for this type they are the EXPECTED verdict from the table.
  As in the JDK hints, `@ImmutableContainer(hc = true)` implies `@Independent(hc = true)`.
- **The second comment** says so, and keeps what maddi computes (`@FinalFields @Dependent`) and why it differs
  (gap families G1, G2). It is a comment: the hints compiler ignores it.

The members follow their type, as the JDK hints' defaults do. In a type annotated immutable, every instance method
is `@NotModified` and its eventual labels are dropped; in a container, no method modifies its arguments; in an
(hc-)independent type, a computed `@Dependent` method or parameter becomes `@Independent(hc = true)`. Everything
else on a member is the computed verdict. The rules are in `ComposeAnalysisHints.applyExpected`.

A type whose row has an expected cell in words keeps its computed annotations, and its comment reads
`//EXPECTED <words> -- computed <verdict>, annotated -- <gap>`. A type without a row is annotated as computed and
has no comment: its computed verdict is the expected one.

Computed verdicts are stated explicitly, because the hints compiler's default for an unannotated element is not
the analysis' default: `@FinalFields` is printed (an unannotated type reads back as mutable), and a dependent
member reads `@Independent(absent = true)`, there being no `@Dependent` annotation.

## What the hints do not state

- **Eventual verdicts (G3):** `Lazy`, `Future` and the `API.For*Future` carriers keep their computed annotations;
  "eventually immutable after evaluation/completion" needs marks that vavr does not have.
- **"No immutability claim":** `Value`, `Traversable`, `Foldable` and the functional interfaces keep their
  computed verdicts, which make no claim.
- **Inherited members:** a shadow lists the members its type declares. A method a persistent collection inherits
  without overriding it (a `Traversable` default) keeps the verdict of the interface it is declared in.
- **44 eventual member labels** (39 in `API`, 3 in `Either`, 1 each in `Value` and `BitSet.Builder`):
  `@NotModified(after = "...")` named after fields. vavr has no marks, so these are the member-level twin of the
  ungrounded type verdicts fixed in 2026-09 (`TestEventualNeedsAMark`): not grounded, still computed. Before the
  expected annotations there were 515, almost all on the persistent collections, where they are now dropped. A
  consumer reads a remaining one as "modifying until a mark that never comes", the pessimistic reading. Open
  engine item.

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

### G6: computed too optimistic on `Iterator`

`Iterator.length()` computes `@NotModified`, but it consumes the iterator: it inherits `Traversable`'s
`foldLeft`, which iterates `iterator()`, and for an `Iterator` that returns `this`. That is the unsafe direction,
and likely a family (every `Traversable` default that iterates `iterator()`, as seen on an `Iterator`), not yet
traced in the engine. The hints no longer depend on it: the member table at the end annotates every instance
method of `Iterator` `@Modified`, except eleven pure inspections (`hasNext`, `isEmpty`, the `is*` constants,
`stringPrefix`, `iterator`). The lazy operations (`map`, `filter`, ...) are stated `@Modified` too: they consume
the iterator through their result, and the conservative statement is the safe one for a client.

## How faithful the hint files are

The compiled hints compared with the source analysis, on the public API:

- **On the 46 types annotated as expected (and `Iterator`'s member rows): 2,511 differences, by design.** They
  are the upgrades the table asks for: 33 types raised to `@Immutable(hc = true)` or `@Immutable`, 1,129 methods to
  `@NotModified`, 1,028 parameters to `@NotModified` (containers), about 200 independence values to
  `@Independent(hc = true)`, and `Iterator.length()`/`groupBy()` down to `@Modified` (G6). Two things there are not
  by design: the 29 second parameters of `Patterns`' extractors (`$Cons`, `$Tuple2`, ...), printed `@NotModified`
  but compiled without it (untraced, the pessimistic direction), and `toString`/`stringPrefix`, whose `String`
  result compiles as `@Independent(hc = true)` rather than `@Independent` (equivalent for an immutable result).
- **On every other type: 37 differences, all encoding-equivalent:** methods returning `String`, a primitive or
  `void`, and parameters of primitive, `String` or `Class` type, where one side stores a value the other treats
  as implied.

Before the decorator was made explicit, the round trip lost 1,254 computed verdicts, most of them in the
optimistic direction; see "Tooling defects".

## Regenerating

1. Source analysis of vavr (`~/git/test-oss/vavr`, main sources only; `corpus/scripts/vavr-main-config.py`
   derives the input configuration):
   ```
   ./gradlew :maddi-run-openjdk:run -PjvmArgs="-Dmaddi.workCeiling=30000000" --args="--input-configuration \
     <test-oss>/vavr/inputConfiguration.main.json --preload-analysis-results-dirs <archive>/analyzedPackageFiles/jdk,\
     <archive>/analyzedPackageFiles/libs/test,<archive>/analyzedPackageFiles/libs/log --analysis-steps prep,modification \
     --analysis-results-dir <vavr-results>"
   ```
2. Compose the hint sources from the vavr jar: the computed verdicts are loaded, then this report's tables
   overwrite them where they state an expected verdict (the type table and the member table):
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

`computed` is maddi's verdict; `expected` is vavr's semantic intent, and it is what the hints ANNOTATE whenever the
cell is nothing but annotations (see "Reading a shadow"); `gap` names the family above. The table is read by
`ComposeAnalysisHints` (`-Pmaddi.compose.notes`): keep one type per row, fully qualified, in backticks. An expected
cell in words ("no immutability claim", "eventually ...", "once Future is") leaves the computed annotations in place.

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
| `io.vavr.Patterns` | @FinalFields @Independent | @UtilityClass | G5: static extractors only (a utility class is immutable) |

### Member expectations (G6)

Read the same way; `type#method` names every instance method of that name, `type#*` every other instance method
of the type's shadow (the first matching row wins, so the exceptions come first). Expected is `@Modified` or
`@NotModified` on the receiver.

| member | computed | expected | gap |
|---|---|---|---|
| `io.vavr.collection.Iterator#hasNext` | @NotModified | @NotModified | inspects, does not consume |
| `io.vavr.collection.Iterator#isEmpty` | @NotModified | @NotModified | inspects, does not consume |
| `io.vavr.collection.Iterator#hasDefiniteSize` | @NotModified | @NotModified | a constant of the type |
| `io.vavr.collection.Iterator#isAsync` | @NotModified | @NotModified | a constant of the type |
| `io.vavr.collection.Iterator#isDistinct` | @NotModified | @NotModified | a constant of the type |
| `io.vavr.collection.Iterator#isLazy` | @NotModified | @NotModified | a constant of the type |
| `io.vavr.collection.Iterator#isOrdered` | @NotModified | @NotModified | a constant of the type |
| `io.vavr.collection.Iterator#isSequential` | @NotModified | @NotModified | a constant of the type |
| `io.vavr.collection.Iterator#isTraversableAgain` | @NotModified | @NotModified | a constant of the type |
| `io.vavr.collection.Iterator#stringPrefix` | @NotModified | @NotModified | a constant of the type |
| `io.vavr.collection.Iterator#iterator` | @NotModified | @NotModified | returns `this` |
| `io.vavr.collection.Iterator#*` | mostly @NotModified | @Modified | G6: consumes the iterator (the lazy operations, `map`, `filter`, ..., consume it through their result: stated conservatively) |
