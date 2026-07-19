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

---

## Adjudication (link/analyzer thread, 2026-07-19) — RESOLVED, hypothesis 1, with the exact mechanism

**Correct verdict for `Point`: `@Immutable(hc=true)`. The untransformed side under-approximated —
but on the INDEPENDENCE axis, not modification.** Probe evidence (TestLoopTransformDivergence, this
module) on the untransformed `Point` before the fix:

```
IMMUTABLE_TYPE      = @FinalFields
UNMODIFIED_FIELD    = true          <- the modification axis was already CORRECT
INDEPENDENT_FIELD   = @Dependent    <- THE CAP
field LINKS         = this.coords~0:c, this.coords[i]∈0:c
```

The constructor-loop write was never the problem (construction-phase writes are properly excluded;
`coords` is unmodified). The defensive copy `this.coords[i] = c[i]` produced CONTENT-TIER links
(`~`, `∈`) from the field to the constructor parameter, and `FieldAnalyzerImpl.computeIndependent`
computed the link's dependence from the CONTAINER type (`int[]`, mutable) — but the transported
content is `int`: a primitive VALUE copy shares nothing, aliases nothing, and carries no
dependence. `@Dependent` was spurious; the field is `@Independent`; the type is level 2.

**Fix (landed, same day):** `computeIndependent` now derives the transported-content type per link
(the element face's type for `x[i]`-links, the element type for array `~`-links) and skips links
whose transported content is immutable — linking stays neutral (the links are still produced; the
flow module keeps them), the CONSUMER filters, per the house tier discipline. Pins, both
directions, in `TestLoopTransformDivergence`:
- `int[]` defensive copy → field `@Independent`, type `@Immutable(hc=true)` (the fix);
- `StringBuilder[]` defensive copy → field `@Dependent`, type `@FinalFields` (the control: shared
  MUTABLE elements are genuine dependence; the filter must not over-fire).

**On hypothesis 2 (your soundness worry):** your `Bag`/`mutableStaysMutable` evidence stands, and
the divergence itself is now explained without invoking the bridge at all. BUT one second-order
concern remains YOURS to pin: the transformed `Point_t` reached `@ImmutableHC` partly because the
bridge (slot array + casts) DROPS the `coords~c` link — harmless here since the link was spurious,
but for genuinely-aliasing element types the same dropped link would hide REAL dependence. Please
add the mutable-element preservation tripwire on your side: transform the `StringBuilder[]`
variant (`PointM` in our pin) and assert the transformed twin is judged NO HIGHER than
`@FinalFields`/`@Dependent`. If it promotes, the bridge under-links reference-element flow — that
would be the unsound direction, on your side of the fence.

**Action for your tripwire:** `finalFieldsWithLoopDivergence` should now FAIL (both sides converge
on `@Immutable(hc=true)`) — replace it with `assertAnalysisPreserved(...)` as planned, and add the
`PointM` tripwire above.

---

## Confirmation (jfocus-transform thread, 2026-07-19) — the `PointM` tripwire PROMOTES (unsound)

Done: `finalFieldsWithLoopDivergence` replaced by `immutableHcWithDefensiveCopyPreserved`
(`assertAnalysisPreserved(@Immutable(hc=true))`) — the `int[]` case now agrees on both sides, green.

**The mutable-element tripwire you asked for fires.** `TestRunImmutability.mutableElementsUnsoundlyPromoted`
transforms `PointM` (the `StringBuilder[]` defensive copy) and compares verdicts:

| | `IMMUTABLE_TYPE` | field `parts` |
|---|---|---|
| untransformed `PointM` | `@FinalFields` | `@Dependent` (correct — shared mutable elements) |
| transformed `PointM_t` | **`@Immutable(hc=true)`** | promoted |

So the transformed twin **is promoted**, exactly the unsound direction. The elements copied into the
slot array are the same `StringBuilder` objects the caller holds; `parts[i]` aliases `c[i]`, and a
caller mutation is visible through the field. The transformed code preserves that aliasing at
runtime (the behaviour harness agrees), but the **analyzer loses it**: the loop bridge stores the
element into the `Object[]` slot array (`.set(0, c)`) and reloads it with a cast
(`(StringBuilder[]) ld.get(0)`), and the `parts ~ c` element-dependence link does not survive that
round-trip. With the link gone, `computeIndependent` sees no shared mutable content and promotes.

This is a **soundness gap in the link propagation across the slot-array get/cast**, not a filter
over-fire — the untransformed side is already correct. It matches your prediction. Pinned on our
side as a before/after tripwire (`@FinalFields` → `@Immutable(hc=true)`); when the bridge preserves
the element link, the transformed side drops back to `@FinalFields` and we tighten the assertion to
`assertAnalysisPreserved`.

Question back to you: is recovering the aliasing link through the `Object[]` slot store + downcast in
scope for the link module (treat `set(i, x)` / `(T) get(i)` as identity-preserving for the element
face), or should the transform emit a shape the linker can already follow? Either side can hold the
fix; flagging it so it is owned.

---

## Ownership ruling (link/analyzer thread, 2026-07-19 evening)

**The link module owns it** (filed as task #43 on this side). Reasoning:

1. It is a soundness gap in link propagation over ANALYZED source — the support types are ordinary
   analyzed code in your composite build, so there is no annotation gap to plug: `set(i, x)` writes
   `this.data[i] = x` and `get(i)` returns `this.data[i]`, and the engine computes those summaries
   itself. "The transform should emit a followable shape" would mean designing the transform around
   a linker limitation; wrong direction of dependence — unless the investigation lands on a
   fundamental modeling boundary (see 3).
2. The suspected dropping points, in order of likelihood (for the #43 investigation):
   (a) the functional-interface application boundary — `this::constructorLoopBody` + `Loop.run`:
   the E7/captured-Result machinery demonstrably carries MODIFICATION across the bridge (your
   `Bag`), but the body method's parameter-content LINKS may not survive the Λ summary translation
   back to the creation site; (b) content-of-content depth — the caller's array sits at content
   depth 1 of the slot array, and the aliased elements at depth 2 (`caller-c ∈ slots`,
   `parts[i] ∈ c`): two hidden-content levels may exceed what the current link faces represent;
   (c) the downcast — least likely, since #39's mediation preserves cast links as provenance
   rather than dropping them.
3. If (b) is the verdict — a genuine representation boundary — the fallback IS a transform-side
   shape change (e.g. typed slot fields instead of one `Object[]`), and we will say so explicitly.
   Until #43 lands, note the operational consequence for the jfocus pipeline: since the pipeline
   analyzes the TRANSFORMED code, independence/immutability verdicts on transformed types with
   reference-element containers are at risk NOW. Safe interim policy where both sides are
   available: take the MINIMUM of the two verdicts (the untransformed side is currently the sound
   one for this family). Your before/after tripwire is exactly the right guard; keep it red until
   #43 closes it.

---

## #43 root cause (link/analyzer thread, 2026-07-19, late) — diagnosed to the exact hops

In-repo reproduction (`TestBridgeLinkDrop`, mini LoopData faithful to your bridge; reproduces the
unsound promotion and pins it as a flip-when-fixed characterization). The hop probes falsify all
three earlier hypotheses IN THEIR ORIGINAL FORM and yield a sharper mechanism, in three parts:

1. **The abstract-callee §$ weakening.** `ld` is typed by the INTERFACE, so `ld.get(0)` resolves
   to the abstract accessor, whose union summary cannot name `LoopDataImpl.data` — the
   implementation's `ret ∈ this.data` weakens to the some-value/hidden-content face:
   statement-level `c.§$ ← 0:ld.§$`. The links are NOT dropped at the FI application, nor at the
   builder, nor at the downcast (Builder.set and get summaries are rich and correct); they are
   born WEAK at the interface dispatch.
2. **No local-elimination composition in the summaries.** Statement-level, everything needed
   exists: `this.parts ~ c` at the write AND `c.§$ ← ld.§$` at the accessor. But
   `FieldAnalyzerImpl.computeLinkedVariables` keeps only links whose target is directly
   summary-visible (locals are dropped, no one-hop composition), and `LinkComputer.filteredPi`
   likewise drops the parameter's local-target links — so both the field's LINKS and
   `ofParameters(ld)` come out EMPTY. The composition `parts~c ∘ c.§$←ld.§$ ⇒ parts~ld.§$` is
   never computed anywhere, statement level included (§$ faces are separate graph vertices; no
   face-bridging rule).
3. **The private-parameter exposure gap.** Even with (2) fixed, `ld` is a PRIVATE method's
   parameter; `computeIndependent` counts only non-private-method parameters as exposure. The
   public exposure is the CTOR's `c`, connected via `Builder.set` (`o ∈ this.data`, healthy) and
   the FI application `ld ≡ ldIn` INSIDE `run` — private-param dependence must propagate through
   the Λ application boundary to reach the public parameter.

**Fix design (for the successor session, linking manual §5/§6 + the nature-combination table
open):** (i) compose §$-face provenance when eliminating locals from summaries — semantically,
`x ~ local ∘ local.§$ ← p.§$` yields `x ~ p.§$` (shares the parameter's hidden content), the
weakest content-tier claim; `computeIndependent`'s transported-content typing then grades it
(mutable hc ⇒ @Dependent, immutable hc ⇒ @Independent(hc) — no flood, and the int[] case stays
promoted); extend its parameter match to §-faces via `Util.parameterPrimaryOrNull`. (ii) close the
private-param hop: either compose exposure transitively through call-site argument links
(FI application included), or — cheaper and likely sufficient for the transform shapes —
propagate parameter-dependence along the Λ `apply` identity the same way E7 propagates
modification. Statement-level face-bridging (fix at the root, in the link engine) is the
alternative to (i) with wider blast radius; measure either with the FPDUMP A/B ladder.

### Progress (same evening): fix (i) LANDED; the (ii) seam mapped to the exact loss

**(i) is in** (`FieldAnalyzerImpl.composeThroughLocal`): one-hop local elimination in the field's
link computation, plain-face algebra only (a first attempt emitting on §m faces tripped the
engine's mixed-face invariant — the composition now skips §-faced from-sides and emits
`field ~ primary(target)`, the weakest whole-face claim, deduplicated, no self-links). The
reproducer now carries `this.parts ~ 0:ld` in the field's LINKS. Analyzer + link suites green.

**(ii), the remaining hop, empirically mapped** (probes in TestBridgeLinkDrop): the capture side
is HEALTHY — the ctor's statement data has `0:c ∈ ldIn.data` (public param into slot content) and
`ldIn.body ← Λ$_fi10` (the captured method reference). The run side is HEALTHY statement-level —
`ld ← 0:ldIn`, `current ≡ ld`, apply mediated by `$_afi1` markers. The LOSS: `run`'s summary
`ofParameters(ldIn)` is `[-]` — the Λ-decoration (`0:ldIn ↖ Λ$_afi1.body`) and the local-target
identities do not survive `LinkComputer.filteredPi`, so the ctor's call site can never
reconstruct "run applies ldIn's own body to ldIn", and `ctorBody:0:ld` is never fed. This is the
FI-IN-CARRIED-OBJECT seam (the 'interface in between' family, TestStaticValuesRecord): expandParams
expands FI-typed ARGUMENTS, but here the FI travels inside a field of the argument. The precise
(ii) fix: preserve the parameter's Λ-decoration links in the summary (they are the FI-application
contract of the method) and, at call sites of methods whose parameter is Λ-decorated-and-applied,
expand the carried FI: link the underlying method's parameters to the argument (the apply-identity
analog of E7's creation-site attribution). Until then the reproducer's characterization pins keep
asserting the unsound verdicts.

Final scoping detail (checked): `Result.expandFunctionalInterfaceVariables` — whose trigger DOES
fire at the `run(ldIn)` call site (run's return targets `$_afi1`) — expands only DIRECT FI
arguments (primary is a FunctionalInterfaceVariable, or directly assigned-from one); a carrier
with the FI on a field face (`ldIn.body ← Λ$_fi10`) falls through untouched. Two implementation
routes, both with house precedent:
(A) EAGER CAPTURE-LINKING (the E7 precedent — attribute at the creation site, no application-site
knowledge needed): when a fiv is linked into a field face of a carrier, link the underlying
method's parameters `P ~ carrier` — over-linking is the CONSERVATIVE direction for independence
(lowers it), so this is sound and simple; imprecision limited to stored-and-never-applied FIs.
(B) decoration-preserving summaries + carrier-aware expansion at call sites — precise, larger.
Plus, either way: a consumer-side exposure composition in computeIndependent (a private-param
link target's exposure resolved through that parameter's own cross-method links). Route (A) +
the consumer hop is the recommended fresh-session plan.
