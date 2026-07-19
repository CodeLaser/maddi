# Immutability divergence across the jfocus-transform loop desugaring

**For:** the link/analyzer thread.
**From:** the jfocus-transform thread (preservation stress-testing).
**Date:** 2026-07-19.
**Status:** open question — needs adjudication. Pinned as a test tripwire, not yet resolved.

## One-line summary

The same type is judged **`@FinalFields`** before jfocus-transform desugars a constructor's
defensive-copy loop, and **`@Immutable(hc=true)`** after. A transform that only rewrites method
bodies should not change a type's immutability verdict, so one side is wrong (or imprecise). Please
determine which, and why.

## Background: what the transform does

jfocus-transform rewrites loops/try into calls on the `Loop`/`Try` support types: the loop body
becomes a (method-reference) method, locals are packed into a slot array, and `Loop.run` drives the
iteration. Correctness requires the analyzer to give **identical** modification / independence /
immutability verdicts before and after — a mutation inside the body has to cascade back across the
functional-interface bridge to the original variable. The modification axis is already covered by
`TestModificationLoopTransform` (in this module) and, from the transform side, by a new
behaviour/modification/immutability preservation harness in
`codelaser-transform-loops` (package `io.codelaser.jfocus.transform.run`).

## The case

Original program (`a.b.Point`):

```java
package a.b;
public class Point {
    private final int[] coords;
    public Point(int[] c) {
        this.coords = new int[c.length];
        for (int i = 0; i < c.length; i++) this.coords[i] = c[i];   // defensive copy, in a loop
    }
    public int total() {
        int s = 0;
        for (int x : coords) s += x;
        return s;
    }
}
```

`coords` is a private, final `int[]`, filled by a defensive-copy loop in the constructor, never
reassigned, never exposed, and never modified by any method after construction. `total()` only reads
it. By the immutability rules (road-to-immutability §050/§080), a private, never-modified,
never-exposed field of a mutable type is **hidden content**, so the type should be **level 2,
`@Immutable(hc=true)`**. That is what the *transformed* code is judged; the *untransformed* code is
judged only `@FinalFields` (level 1).

### Verdicts observed (iterating analyzer, `maxIterations=10`, analysis hints loaded)

| | `IMMUTABLE_TYPE` | `CONTAINER_TYPE` | `total()` |
|---|---|---|---|
| untransformed `Point` | `@FinalFields` | true | `@Independent`, non-modifying |
| transformed `Point_t` | `@Immutable(hc=true)` | true | `@Independent`, non-modifying |

Only the type immutability level differs. Everything else matches.

### The transformed code (post-desugaring, printed then re-parsed for analysis)

```java
package a.b;
import io.codelaser.jfocus.transform.support.ArrayUtil;
import io.codelaser.jfocus.transform.support.Loop;
import java.util.stream.IntStream;
public class Point_t {
    private final int[] coords;
    public Point_t(int[] c) {
        this.coords = new int[c.length];
        Loop.LoopData ldIn = new Loop.LoopDataImpl.Builder()
            .set(0, c)
            .iterator(IntStream.iterate(0, i -> i < c.length, i -> i + 1).iterator())
            .body(this::constructorLoopBody)
            .build();
        Loop.run(ldIn);
    }
    private Loop.LoopData constructorLoopBody(Loop.LoopData ld) {
        int[] c = (int[]) ld.get(0);
        int i = (int) ld.loopValue();
        this.coords[i] = c[i];              // the field-content write, now in a separate instance method
        return ld;
    }
    public int total() {
        int s = 0;
        Loop.LoopData ldIn = new Loop.LoopDataImpl.Builder()
            .set(0, s)
            .iterator(ArrayUtil.iterable(coords).iterator())
            .body(this::totalLoopBody)
            .build();
        Loop.LoopData ld = Loop.run(ldIn);
        s = (int) ld.get(0);
        return s;
    }
    private Loop.LoopData totalLoopBody(Loop.LoopData ld) {
        int s = (int) ld.get(0);
        int x = (int) ld.loopValue();
        s += x;
        return ld.with(0, s);
    }
}
```

Note: `constructorLoopBody` is an **instance** method reached only from the constructor via
`this::constructorLoopBody` → `Loop.run`. (Before a transform fix on 2026-07-19 it was emitted
`static` while still writing `this.coords`, which did not compile; that is fixed and unrelated to
this divergence — the divergence reproduces with the corrected, compilable output above.)

## Two hypotheses

1. **The untransformed side under-approximates (most likely).** A direct array-element write
   `this.coords[i] = c[i]` inside a constructor loop is conservatively treated as a
   possibly-escaping / not-provably-construction-confined content modification, capping `Point` at
   `@FinalFields`. Once the write lives in a separate method reachable from the constructor
   (`constructorLoopBody`, part-of-construction), the analyzer proves the content is only written
   during construction and reaches the correct `@Immutable(hc=true)`.

2. **The transformed side over-approximates (must be ruled out — this is the unsound direction).**
   If the analyzer fails to attribute `constructorLoopBody`'s `this.coords[i] = ...` write to the
   field's content (e.g. it does not trace the write through the `this::constructorLoopBody` method
   reference + `Loop.run` bridge into "modifies `coords` content"), it could land on
   `@Immutable(hc=true)` for the *wrong* reason — by not seeing the write at all. That would be a
   latent unsoundness that only happens to be harmless here because the write is in fact
   construction-confined.

### Evidence that points at hypothesis 1

A sibling soundness test (`TestRunImmutability.mutableStaysMutable`) transforms:

```java
public class Bag {
    private int total;
    public void addAll(int[] xs) { for (int x : xs) total += x; }   // writes a field OUTSIDE construction
    public int total() { return total; }
}
```

`Bag` is `@Mutable` (the field is written outside the construction phase). It stays **`@Mutable`
after transformation** — i.e. the analyzer *does* see the `total += x` field write through the
desugared `this::addAllBody` + `Loop.run` bridge and correctly refuses to promote the type. So the
bridge does not generally hide field writes; that argues the transformed `Point` reaches
`@Immutable(hc=true)` for the *right* reason, and the untransformed `Point` is the imprecise one.

But this is not proof for the `coords` (array-content, construction-confined) case, which is a
different code path from `Bag`'s scalar out-of-construction write. Please confirm.

## What to check

- Why does untransformed `Point` stop at `@FinalFields`? Is the constructor-loop array-element write
  treated as breaking hc-immutability, and is that intended or an imprecision?
- Confirm the transformed `Point_t` reaches `@Immutable(hc=true)` because `constructorLoopBody` is
  correctly classified *part-of-construction* and its `this.coords[i]` write is attributed to
  `coords`' content (not because the write is invisible across the bridge). Inspect the link
  summary / `METHOD_LINKS` of `constructorLoopBody` and the modified-set feeding `coords`' field
  verdict.
- Decide the correct verdict for `Point` and align the two sides.

## How to reproduce

Transform side (jfocus-transform-main, needs the local maddi-kotlin composite build):

```
./gradlew :codelaser-transform-loops:test --tests \
  "io.codelaser.jfocus.transform.run.TestRunImmutability"
```

`finalFieldsWithLoopDivergence` pins the current (divergent) verdicts as a tripwire; when this is
resolved so the two sides converge, that assertion will fail and should be updated to
`assertAnalysisPreserved(...)`. `mutableStaysMutable` is the soundness guard described above.

The harness: analyze an independent parse of the original (ground truth), transform with the real
pipeline, print + re-parse the output, analyze that, and compare `IMMUTABLE_TYPE` / `CONTAINER_TYPE`
/ per-method `INDEPENDENT_METHOD` + `isModifying()`. Driven by the full `IteratingAnalyzerImpl`
(`analyze(order)`), analysis hints loaded via the module's testFixtures setup.
