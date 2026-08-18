# The Road to Immutability, chapter 12, against the code it quotes

Chapter 12, *Support classes*, prints seven listings from `maddi-support` and annotates them with
callouts. This is a line-by-line comparison of those listings against
`maddi-support/src/main/java/io/codelaser/maddi/support/` as of 0.9.1.

**The two are not a drifted copy of each other.** `git log` shows the book section and the support
sources arriving in this repo in the same commit, `896aed989 moved code from other repositories`,
already out of step; nothing since has touched either except the package rename. So these are not
edits that were made to one and forgotten in the other — they are two independently maintained
lineages that were both imported. That matters for how to fix it: there is no "correct" side to
copy from, and each finding needs a judgement.

Three of the findings change what the book *claims*, not just what it shows. They are marked
**substantive** below.

## Summary

| # | Where | What |
|---|-------|------|
| 1 | chapter opening | The artifact description is wrong after the annotation split |
| 2 | `Lazy` | **substantive** — the listing's central mechanism is not in the code, and `Memo` answers the same question a different way |
| 3 | `FirstThen` | **substantive** — mark label is `mark` in the book, `first` in the code, and the book contradicts itself |
| 4 | `FirstThen` | **substantive** — two callouts explain a null-check dance the code no longer does |
| 5 | all seven | **answered** — the analyzer computes `@Independent(hc=true)` at 13 of the book's 16 positions; the sources stopped asserting it |
| 6 | `Freezable` | A callout explains an inference the source no longer relies on |
| 7 | `FlipSwitch` | Mark label `t` vs `isSet`; `@Modified`; `final`; `synchronized` |
| 8 | `SetOnce` | `getOrDefault` signature and contract |
| 9 | `EventuallyFinal` | Exception message |
| 10 | `SetOnceMap` | `@Modified`, `@NotModified`, `@NotNull` now explicit |
| 11 | §14 opening | `@ExtensionClass` and `@Singleton` are said to live in `maddi-support` |

---

## 1. The chapter opening describes the wrong artifact

> The `maddi-support-1.0.0.jar` library (in whichever version it comes) essentially contains the
> annotations of the analyzer, and a small selection of support types.

Two things are now wrong, and the second one is the release you are about to publish:

- the version is 0.9.1, not 1.0.0 — though "in whichever version it comes" covers that;
- **the annotations are no longer in `maddi-support`.** They were split into `maddi-annotation`,
  which is the artifact `maddi-support` now declares its one dependency on. That split is also the
  licence boundary the site footer advertises: annotations Apache-2.0, analyzer LGPL-3.0.

A reader who follows this sentence adds the wrong dependency. It is the first paragraph of the
chapter, and it describes the shape of the very thing being released.

Same error in `120-other-annotations.adoc`:

> `@ExtensionClass` and `@Singleton` exist in `maddi-support`, but nothing in the analyzer reads
> them.

Both are in `maddi-annotation` (`io/codelaser/maddi/annotation/type/`), along with every other
annotation the book names.

## 2. `Lazy` — the listing's central mechanism is not in the code

**substantive.**

The book:

```java
@NotNull(content=true)
@Independent(hc=true, after="t")
private Supplier<T> supplier;          // not final

public T get() {
    if (t != null) return t;
    t = Objects.requireNonNull(supplier.get());
    supplier = null;                   // <4>
    return t;
}
```

The source:

```java
private final Supplier<T> supplier;    // final, never cleared

public T get() {
    if (t != null) return t;
    t = Objects.requireNonNull(supplier.get());
    return t;
}
```

`supplier = null` is not a detail of the listing. The fifteen lines after it are an argument built
on it — *"why is `supplier` as a field not linked to the constructor parameter?"* — answered by the
field being emptied at the transition, and concluding with an extension to the definition of
immutability:

> **Rule 2**: All fields are either private, of immutable type, or equal to null.

That rule is stated in the book as a consequence of how `Lazy` works. In the code, `supplier` is
`final` and outlives the transition, so it is not an example of the rule, and the book's own
conclusion — that the constructor parameter is `@Independent(hc=true)` — is not what the source
claims either: the source annotates nothing.

This is the one finding that cannot be fixed by editing a listing. Either the code is meant to
blank the field and does not, or the book's Rule 2 extension needs a different example (or needs to
be presented as a definition in its own right rather than as something `Lazy` demonstrates).

### Settled by measurement: blanking the field changes nothing

`TestBookIndependenceOfSupportTypes` runs `Lazy` twice — once as shipped (`final supplier`, never
cleared) and once **in the book's own shape**, non-final and with `supplier = null` inside `get()`.
The two blocks come out identical:

```
field supplier                             DEPENDENT      book: INDEPENDENT_HC DIFFERS
constructor, parameter 0                   DEPENDENT      book: INDEPENDENT_HC DIFFERS
get(), return                              INDEPENDENT_HC book: INDEPENDENT_HC agrees
```

So the divergence is not that the code drifted away from the book. **The book's own code, run
through today's analyzer, does not produce the annotations the book prints on it.** `supplier = null`
buys nothing the analyzer can see, and the fifteen lines of argument that rest on it — ending in the
Rule 2 extension, "all fields are either private, of immutable type, or equal to null" — have no
worked example behind them any more.

That narrows the decision. Restoring `supplier = null` to the source would not make the listing
correct, because the listing is not correct on the book's own code either. Either the analyzer no
longer implements the rule the section describes, or the section describes a rule it never
implemented; §12.6 needs a decision on which before it can be repaired.

Smaller, in the same type: the constructor now null-checks its argument; `hasBeenEvaluated` gained
`@TestMark("t")`; `get()` gained `@Modified`.

### 2a. `Memo` is not a variant of `Lazy` — it is a different answer, from a different chapter

`Memo` and `IntMemo` solve the same problem as `Lazy`, and they do it without ever raising the
question the book's `Lazy` section exists to answer.

```java
@IgnoreModifications(comment = "idempotent lazy cache: writes are observationally invisible (road §050)")
public final class Memo<T> {
    private volatile T value;

    public T get(Supplier<? extends T> compute) {
        T v = value;
        if (v == null) {
            v = Objects.requireNonNull(compute.get(), "A Memo cannot cache null");
            value = v;
        }
        return v;
    }
}
```

**`Memo` has no supplier field.** The supplier is a parameter of `get()`, supplied fresh at each
call site. So there is nothing to blank out, no field that is "of relevance before the transition
and not after", and no need to extend Rule 2 with "or equal to null". The entire argument in the
book's `Lazy` section is about a field `Memo` does not have.

The two types are justified under different chapters:

| | `Lazy` | `Memo` |
|---|---|---|
| holds the supplier | yes, in a field | no, it is a parameter of `get` |
| annotations | `@ImmutableContainer(after="t")`, `@Mark`, `@TestMark`, `@Final(after=)` | `@IgnoreModifications` at class level, nothing else |
| the claim | the type becomes immutable at the mark | the writes are observationally invisible, so a *holder* of this field stays non-modifying |
| book chapter | §12.6, eventual immutability | §050, `@IgnoreModifications` as manual hidden content |
| in `COMMIT_ONCE_TYPES` | yes | **no** — the engine does not treat it as a commit-once slot |
| makes an immutability claim about itself | yes | none; its field is plainly mutable |

`Memo` is not eventually immutable and does not pretend to be. It is a mutable slot with a written
disclaimer, and `SourceContractMaterializer.materializeIgnoreModificationsFromFieldType` is what
makes a field *of that type* inherit the disclaimer — so the idiom is declared once on the class
"whose whole purpose it is" rather than repeated on every memo field.

### 2b. Which of the three is actually used

Neither, in the strict sense. Counting field declarations and constructions across the whole repo,
excluding each type's own module:

- **`Lazy`** — used nowhere. Its only appearances are `maddi-support/src/test/.../TestLazy.java`
  and its name in `TestEventualConformance.COMMIT_ONCE_TYPES`. Its own javadoc says *"This is an
  example class! Please extend and modify for your needs."*
- **`Memo` / `IntMemo`** — declared, documented, and wired into the analyzer by FQN in
  `IgnoreModifications`'s javadoc and `SourceContractMaterializer`'s — but **not instantiated
  anywhere, and with no unit test of their own** (`maddi-support/src/test/` has `TestEither`,
  `TestEventuallyFinal`, `TestFirstThen`, `TestLazy`, and nothing for `Memo`).
- **The idiom that is actually in the code** is the hand-written memo field carrying
  `@IgnoreModifications` directly, which is what `Memo` exists to replace:

  ```java
  // VariableImpl, cst-impl
  // @IgnoreModifications (road §050): idempotent memo state, disclaimed -- both the writes and the
  // slot's assignability are invisible to the modification/immutability analysis
  @IgnoreModifications private String cachedFqn;
  @IgnoreModifications private int cachedHash;
  ```

  Same shape in `UnaryOperatorImpl.hash`. `Memo`'s own javadoc names `VariableImpl.cachedFqn` as the
  precedent and says *"new code should not"* rely on the benign race those fields accept.
  `TestCommitLabels.testMemoField` covers exactly this shape — a hand-written `cachedFqn`, not a
  `Memo`.

So the picture for finding 2 is not "the book quotes `Lazy` and `Lazy` changed". It is:

1. `Lazy` is an example class nothing uses, and its listing in the book no longer matches it;
2. the analyzer solves lazy initialisation the `@IgnoreModifications` way, which the book covers
   properly in §050 but never connects to chapter 12;
3. `Memo` is the intended canonical form of that, and it is not yet adopted or tested.

That reframes the decision. The question is less "should `Lazy.supplier` be cleared" and more
whether chapter 12 should still present `Lazy` as the lazy-initialisation story at all, when the
codebase's answer is a §050 disclaimer. If `Lazy` stays, its Rule 2 argument needs the listing to
match; if `Memo` is meant to be the recommendation, chapter 12 has a type to document and the
Rule 2 extension needs to stand on its own rather than on an example.

## 3. `FirstThen` — the mark label, and an inconsistency inside the book

**substantive.**

The source marks on `"first"` throughout: `@ImmutableContainer(after="first")`, `@Mark("first")`,
`@Only(before="first")`, `@Only(after="first")`.

The book uses `"mark"` for three of those four — and `"first"` for the fourth:

```java
@ImmutableContainer(hc=true, after="mark")
...
@TestMark(value="first", before=true)   // <-- "first"
public boolean isFirst() { ... }

@TestMark(value="first")                // <-- "first"
public boolean isSet() { ... }

@Mark("mark")                           // <-- "mark"
public void set(...) { ... }

@Only(before="mark")                    // <-- "mark"
public S getFirst() { ... }
```

So the book's own listing tests a mark that its own listing never sets. This is wrong independently
of the source — a reader who copies it gets a type whose `@TestMark` refers to nothing. Whatever is
decided about the rest, this one is a straight bug.

The source also carries `@Final(after="first")` on both fields, which the book's listing omits.

## 4. `FirstThen` — two callouts explain code that is no longer there

**substantive.**

The book:

```java
public S getFirst() {
    if (first == null)
        throw new IllegalStateException("Then has been set"); // <1>
    S s = first;
    if (s == null) throw new NullPointerException();
    return s;
}
```

with callout 1 running to four lines: *"This is a bit convoluted. The precondition is on the field
`first`, and the current implementation of the precondition analyzer requires an explicit check on
the field. Because this field is not final, we cannot assume that it is still null after the initial
check; therefore, we assign it to a local variable, and do another null check…"*

The source:

```java
public S getFirst() {
    if (first == null) throw new IllegalStateException();
    return first;
}
```

and, for `get()`, `assert then != null; return then;` where the book has the same local-variable
dance and callout 2 explaining it.

The callouts describe a limitation of the precondition analyzer. If that limitation was lifted, the
callouts are stale and should go. If it was not, and the source is simply relying on `assert`, then
the source and the book disagree about whether the result is provably `@NotNull` — worth knowing
either way.

Also in `FirstThen`: the source has a private `FirstThen(S, T)` constructor and a static
`then(T)` factory that the book does not show (fine — it says "`FirstThen`", not "part of"), and
`equals` takes `@NotNull(absent=true) Object o` where the book writes `@Nullable Object o`. Callout
3 says `equals` and `hashCode` *"inherit the `@NotModified` annotation from `java.lang.Object`"*;
the source writes `@NotModified` on both explicitly, which is not what a reader expects after
reading that sentence.

## 5. `@Independent(hc=true)` is in every book listing and in none of the sources

The book puts `@Independent(hc=true)` on parameters, fields and return types across `SetOnce`,
`EventuallyFinal`, `SetOnceMap`, `Lazy` and `FirstThen` — **sixteen positions** inside the listings
(eighteen mentions in the chapter, counting two in prose). Across the whole `maddi-support` package
the sources carry it six times: four in `SetOnceMap` (`keyStream`, `valueStream`, `stream`,
`putAll`) and two in `AddOnceSet`. **Not one of those six is on a member the book shows**, and not
one of the sixteen the book shows is in the source.

`Lazy` keeps a trace of the older shape in a comment:

```java
t = Objects.requireNonNull(supplier.get()); // this statement causes @NotNull1 and @Independent on supplier
```

`@NotNull1` appears nowhere else in `maddi-annotation` or `maddi-support` — it is a name from before
`@NotNull(content=true)` — and neither annotation the comment names is written on the type. The
comment is the book's version of `Lazy`, describing itself from inside the current one.

Two callouts hang off annotations that are not in the source:

- `SetOnce.getOrDefault`, callout 1: *"Even if it is only linked to the hidden content
  conditionally"* — attached to an `@Independent(hc=true)` the source does not write;
- `Lazy`, callout 1: *"The annotation has traveled from the field to the parameter"* — the field
  annotation it travels from is not in the source either.

The book's introduction says annotations in the text are *"a means of verification: the analyzer
will check if it generates the same annotation at that location."* Under that reading, an annotation
the source omits is not necessarily wrong — the analyzer may still compute it, and the source may
simply have stopped asserting it. **That is the question to settle before editing anything here**:
does the analyzer still compute `@Independent(hc=true)` at these positions? If it does, the book is
right and the sources have quietly stopped verifying it, which is a loss worth reversing. If it does
not, the listings are wrong.

`maddi-modification-analyzer`'s `TestBookIndependenceOfSupportTypes` answers it. It runs the analyzer
over the shipped bodies — reduced to the members under test, annotations and javadoc stripped, so
nothing is contracted and every value is computed — and reports all sixteen of the book's positions
in one block per type, each row carrying the book's claim beside the computed value.

### The answer

**The book is right at thirteen of the sixteen, understates one, and is wrong at two.**

| Type | Positions | Computed |
|---|---|---|
| `EventuallyFinal` | 3 | `INDEPENDENT_HC` — as printed |
| `FirstThen` | 4 | `INDEPENDENT_HC` — as printed |
| `SetOnceMap` | 3 | `INDEPENDENT_HC` — as printed |
| `SetOnce` | 3 | 2 as printed; `getOrDefault` returns fully `INDEPENDENT` |
| `Lazy` | 3 | `get()` as printed; **field `supplier` and the constructor parameter are `DEPENDENT`** |

So for thirteen positions the answer is the one that favours the book: **the analyzer still derives
`@Independent(hc=true)` there, and the sources have simply stopped asserting what it computes.**
Those annotations are not stale documentation; they are a verification the sources gave up. Putting
them back would be a gain, not a tidy-up.

`SetOnce.getOrDefault` is stronger than printed — fully `INDEPENDENT` rather than hidden-content
independent. `@Independent(hc=true)` is a weaker but still true statement, so the book is not wrong
here, just imprecise. Note the shipped body is not the book's (finding 8).

The two that differ are both in `Lazy`, and they are exactly the two the book argues hardest for.

## 6. `Freezable` — a callout explains an inference the source no longer relies on

Book: `@ImmutableContainer(after="frozen")` with callout 1, *"Because the type is abstract,
`hc=true` is implied."*

Source: `@ImmutableContainer(after = "frozen", hc = true)` — written out.

The callout's claim may still be true of the analyzer; the source no longer depends on it. The
source also carries `@Only(before="frozen")` on `ensureNotFrozen` and `@Only(after="frozen")` on
`ensureFrozen`, which the book's listing omits — a real omission, since those two methods are the
whole reason `Freezable` is a useful base class.

## 7. `FlipSwitch`

| | book | source |
|---|---|---|
| mark label | `"t"` | `"isSet"` |
| field | `private volatile boolean t` | `private volatile boolean isSet` |
| class | `public class` | `public final class` |
| `set()` | `@Mark @Modified` | `@Mark` only |
| `set()` body | bare | wrapped in `synchronized (this)` |
| also | — | `toString()` |

The mark label is the one that matters: a reader comparing the book's `FlipSwitch` to the jar's sees
a different type. The listing is titled "most of", so the missing `toString` is fine.

## 8. `SetOnce`

The book's `getOrDefault`:

```java
@Independent(hc=true) @NotModified
public T getOrDefault(T defaultValue) {
    if (isSet()) return get();
    return defaultValue;
}
```

The source's:

```java
@NotModified @NotNull
public T getOrDefault(@NotNull T alternative) {
    if (isSet()) return get();
    return Objects.requireNonNull(alternative);
}
```

Different contract, not just different annotations: the source rejects a null alternative and
guarantees a non-null result. The source's field also carries `@Nullable // eventually not-null, not
implemented yet`, and `set` synchronises on `this`.

The source additionally has `get(String message)`, `getOrDefaultNull`, `copy`, `toString`, `equals`
and `hashCode`. The listing says "parts of", so that is not an error.

## 9. `EventuallyFinal`

Beyond the missing `@Independent(hc=true)` (finding 5), one difference: the book throws
`"Trying to overwrite a final value"`, the source `"Trying to overwrite final value"`. Trivial, but
the listing is titled `io.codelaser.maddi.support.EventuallyFinal` with no qualifier, so it reads as
verbatim.

## 10. `SetOnceMap`

The source writes explicitly what the book leaves implicit: `@Modified` on `put`, `@NotModified` on
`isSet`, `@NotNull` on `get`'s parameter. Callout 2 — *"Implicitly, the parameter `K k` is
`@Independent`, because the method is `@NotModified`"* — still reads correctly.

## 11. Coverage

The book discusses seven support types. The package now has fourteen: `AddOnceSet`, `Either`,
`EventuallyFinalOnDemand`, `IntMemo`, `Memo` and `VariableFirstThen` are not mentioned. The chapter
says "We discuss a selection of the building blocks here", so this is not an error in itself — but
the selection is now a poor one:

- `TestEventualConformance` treats ten types as one "commit-once family" whose `@Mark`/`@Only`
  contracts the engine knows. Three of those ten — `AddOnceSet`, `EventuallyFinalOnDemand`,
  `VariableFirstThen` — are undocumented, and `EventuallyFinalOnDemand` is one of the most-used
  types in the analyzer (six files) while `Lazy`, which the book gives a full section to, is used in
  none.
- `Memo` and `IntMemo` are not in that family at all (see 2a), and are the one part of the package
  the analyzer's own machinery names by fully-qualified string.

---

## What to decide

1. ~~**Finding 5** first, because it governs six of the others.~~ **Answered** by
   `TestBookIndependenceOfSupportTypes`: yes at thirteen of sixteen positions. The remaining work is
   a decision, not an investigation — put the thirteen annotations back into `maddi-support` (they
   are verification the sources gave up), and fix the two `Lazy` rows in the book.
2. **Finding 2**, `Lazy`, is now the only open question, and it got harder rather than easier:
   clearing `supplier` does not change what the analyzer computes, so the book's Rule 2 extension has
   no worked example. Is that a gap in the analyzer or a rule the book overstated? And separately:
   should chapter 12 present `Lazy` as the lazy-initialisation story at all, when the analyzer's own
   lazy caches are `@IgnoreModifications` memo fields (§050) and `Memo` is the canonical form of
   that — undocumented in the book, and not yet adopted in the code.
3. **Findings 1 and 3** are unambiguous and should be fixed before the book is announced with the
   0.9.1 release — one misdescribes the artifact being published, the other is internally
   inconsistent.

The rest are mechanical once 1–3 are settled.
