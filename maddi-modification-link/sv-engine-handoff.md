# sv engine integration — handoff / status

Entry point for the `sv` (shared-variable) engine integration. Read this first, then
the companion docs:
- `sv-integration-plan.md` — the overall port strategy + delta map + phases.
- `sv-semantic-differences.md` — the new link model vs the legacy one (the "why").
- `sv-engine-differential-worklist.md` — the 194-failure breakdown + root-cause notes.

Branch: **`sv-integration`** (off `openjdk`). `openjdk` itself is untouched and green.

## TL;DR

The `sv` engine (witness-based incremental fixpoint + shared-variable / modification
equivalence groups) is **fully ported and compiling** on `sv-integration`, at
**196/383 link tests failing** (15 `@Disabled` large stress mocks). The failures are
**not** a broken port — they are the visible signature of a redesigned link
semantics that is **unfinished in one specific way**:

> **The equivalence *collapse* is implemented; the field-precise *reconstruct* is not.
> So collapse either drops assignment links (both-fresh case) or flattens field
> structure into a scalar shared variable (active case), losing precision.**

The closure + label algebra underneath are **sound and precise** — proven by the
`NOSV` experiment below. The remaining work is a **structure-preserving
collapse/reconstruct**, which is a genuine engine-design decision.

## Purpose of sv (north star)

`sv` = **shared variable**. Goal: **bound the link count**. When an object with a
deep, explicit structure is passed around and its members are referenced often, the
"I-am-part-of-you" links (`≺` field-of, `≻` contains-as-field, `∈` element-of, `∋`
contains-as-member) explode — they combine transitively, and when the same object
flows through several variables the whole part-of web is duplicated per variable.

Fix = represent **equivalence classes as one node** instead of materialising the
pairwise links inside them. Two tiers, built in this order:
1. **Modification equivalences** (`VirtualModificationIdenticals`) — variables that
   mutate together (`§m`-identical, `≡`). Has BOTH halves: collapse + reconstruct
   (`edges()`/`Group.expand()`), O(N) not O(N²).
2. **Assignment groups** (`SharedVariables`, `$__sv_`) — variables joined by `←`
   collapse to a representative. Has the collapse half (`translateForward`) but **not
   the reconstruct half** — this is the core gap.

`←`/`≡` are the *duplication multipliers*; collapsing them lets the expensive `≺`/`≻`/
`∈`/`∋` web be stored once per class.

## Failure taxonomy (quantified)

Full link suite = 196 failing / 383. Three experiments pin the causes (all on
`Graph.mergeEdgeBi`, the `IS_ASSIGNED_FROM` branch):

| variant | failing | interpretation |
|---|---:|---|
| as-ported baseline | 196 | — |
| both-fresh → normal edge (reverted candidate `973a3f2c`) | 141 | the **both-fresh drop ≈ 55** |
| all shared-var collapse off (`NOSV`, diagnostic only) | 114 | collapse off recovers **82** total |

So, of the 196:
- **~55** = the **both-fresh drop** (see below).
- **~27** = the **active-collapse coarsening** (both-fresh recovered them from "empty"
  but into coarse output; `NOSV` avoids them entirely).
- **~114** = **label-algebra re-baselines** — the intended new `≻`/`∩`/candidate-nature
  semantics (broadened `valid()`, reordered ranks, `∈?`/`∋?`). These are genuine
  re-baselines to be done per-class *after* the shared-variable output is precise.

## Root cause #1 — both-fresh drop (~55)

`Graph.mergeEdgeBi`, `IS_ASSIGNED_FROM` branch: when **both endpoints are fresh**
(`fromInGraph.isEmpty() && toInGraph.isEmpty()`), it creates the shared group but
**neither `transformToSharedVariable` branch fires → `return true` adds nothing** →
the assignment is dropped. (Nearby `// TODO what with fromInGraph?` flags the same
unfinished area.)

This is the common case: a constructor's `field ← param` and method-entry
assignments are the *first* edges into an empty graph. Traced on
`TestSharedVariable :: direct assignment` (INPUT: `record S(String s1,String s2)`;
`record C(R r,int i){ S choose(){ return new S(r.list1.get(i), r.list2.get(i)); } }`):
`S.<init>` feeds `this.s1 ← 0:s1`, `this.s2 ← 1:s2` into an empty graph → both
dropped → `S.<init>`'s summary is **empty** → `new S(...)` links nothing → `choose`
emits `-`.

The reverted candidate `973a3f2c` (add a normal edge when both fresh) recovered these
55. It is arguably correct-by-design (nothing to dedup when both are fresh) — but see
root cause #2: it exposed coarse output, so it is not a complete fix on its own.

## Root cause #2 — active-collapse coarsening (~27) — the precision problem

Once the constructor summary survives, applying it in `choose` produces
`choose.s1 ← $__rv1`, and `$__rv1` already carries the precise `$__rv1 ∈ r.list1.§$s`
(from `List.get`). Because `$__rv1` is materialised, `transformToSharedVariable`
fires and **folds the field-precise `$__rv1` into a scalar `$__sv_s1`** (created with
`referenceVariable.parameterizedType()`, a bare `String`). The fact that this rep
stands for **the `s1` field of `choose`** is lost, so extraction can only scope up:

```
expected: choose.s1 ∈ this.r.list1.§$s , choose.s2 ∈ this.r.list2.§$s
got:      choose ≻ choose.s1 , choose ∋… , choose ∩ this.r.list1.§$s , choose ∩ this.r.list2.§$s
```

`NOSV` (collapse off) keeps `$__rv1` precise and the closure yields the exact
`choose.s1 ∈ r.list1.§$s`. **So the collapse trades precision for reduction** — the
scalar shared variable flattens field structure. This coarse output is NOT an
acceptable re-baseline.

## The reconstruct gap (why (a)–(c) weren't enough)

Prototyped `SharedVariables.edges()` + folding into `edgesWithEquivalence()` +
`engine.addVertex(sv)` for both-fresh + a `FollowGraph` translate-back. Still `-`.
An extraction probe (`followGraph primary=… rep=… allShared=…`) showed the reconstruct
is missing across four points:
1. **`SharedVariables` rep↔members is one-way** — `memberToGroup` maps *members→rep*;
   `allShared($__sv_s1) = [$__sv_s1]`, the rep can't enumerate its own members.
2. **The graph is keyed on the synthetic rep, not real vars** — `followGraph(choose)`
   finds nothing "part of" `choose`; `followGraph($__sv_s1)` yields rep-keyed links.
3. **`edgesWithEquivalence()` feeds `computeSubs` (MakeGraph:43), not the output** —
   the output path is `FollowGraph.closureStream` → `engine.successorStream`, rep-keyed.
4. **Summary application doesn't carry the group through the call** — in `choose`,
   `allShared(choose)=[choose]`; applying `S.<init>`'s group summary never
   re-establishes the group in the caller.

So the reconstruct half must be completed across `SharedVariables` (rep→members +
field-precise `edges()`), `Graph`/`FollowGraph` (expand rep vertices to
member-keyed, *field-precise* links at extraction), and `LinkMethodCall` (translate
the group when a summary is applied).

## The core design decision (open — yours to make)

For `sv` to hit *fewer links **without** losing precision*, the shared representation
must **preserve the field path through collapse and reconstruct it**. Options:

1. **Field-structured reps + field-precise reconstruct.** The group must remember it
   stands for `choose.s1` (a field), and extraction emits `choose.s1 ∈ …`, not
   `choose ∩ …`. Most faithful to the design; most work (completes the reconstruct
   half symmetric to `VirtualModificationIdenticals`, but field-aware).
2. **Collapse only whole primaries, never field-bearing intermediates.** Don't fold
   `$__rv1`/`choose.s1` into a scalar; keep field-precise vars precise and only group
   at the primary level where the explosion actually lives. Least invasive; worth
   measuring whether it keeps precision *and* still reduces on deep-structure cases.
3. **Canonical-first-member rep** (real var, earliest-defined, instead of synthetic
   `$__sv_`). Fixes the *keying* (points 1–3 of the reconstruct gap) but still
   flattens the *other* member's field path — must be paired with option (1)'s
   field-precise reconstruct.

Recommendation: prove option (2) first (cheap, tells you if "precise + reduced" is
even in tension for the real cases), then commit to (1)/(3) for the general solution.
`NOSV` being fully precise means the closure already has the right answer at field
granularity — all the work is in structure-preserving collapse/reconstruct.

## How to reproduce / key levers (all in `linkgraph/Graph.java` unless noted)

- **Baseline count:** `./gradlew :maddi-modification-link:test` then count `<failure`
  across `build/test-results/test/*.xml`.
- **The crucial test:** `--tests '*staticvalues.TestSharedVariable.test1'` ("direct
  assignment"); expected in `TestSharedVariable.java`.
- **`NOSV` experiment (precision proof):** gate the whole `isAssignedFrom` branch of
  `mergeEdgeBi` behind `System.getenv("NOSV")==null`; run with `NOSV=1`. → 114/383,
  and `direct assignment` passes precisely.
- **Both-fresh probe:** the drop is the `else`-less fall-through after the two
  `transformToSharedVariable` branches (lines ~118–126). The reverted candidate is
  commit `973a3f2c` (see `git show`).
- **Extraction probe:** `FollowGraph.followGraph` — `fromList` is graph vertices
  `isPartOf(primary, v)`; collapsed members aren't there. `Graph.printEquivalence` /
  `SharedVariables.print` show the `$__sv_` groups.
- Engine internals: `impl/graph/IncrementalFixpointEngine` (class doc lists the 9
  design features), `Closure`, `Fact`, `Witness`, `WitnessIndex`, `LabeledGraph`.
- Label algebra: `impl/LinkNatureImpl` (ranks + `score` + `combine`/`reverse`/`best`;
  autogenerated combine table in `operator.adoc`).
- `merge()` in `SharedVariables` is `NYI` (throws) — a separate gap for `x=y;x=z`.
- `SharedVariable.acceptForLinkedVariables()` throws — 2 remaining crashes; an
  unfinished path.

## Commit trail on `sv-integration`

```
fd0c52ae  revert the both-fresh candidate (coarse output rejected)
169d509e  worklist: prototype outcome + reconstruct-gap map
973a3f2c  (reverted) both-fresh -> normal edge candidate  [196->141]
da4ea329  root-cause the dominant cluster (shared-var collapse)
081aca3e  semantic analysis framed on the link-explosion purpose
a83b0514  semantic-differences analysis
06795cc1  clear mechanical failures (@Disabled large mocks; formatting) [->196]
abb98276  fqn-adapt sv tests to openjdk parser [219->201]
d3eb9e5c  test reorg; main+tests compile [219/383 baseline]
73af66ec  big-bang: swap in the sv engine; main compiles
167b60e7  Phase-1 coupling finding, revised phasing
736ee738  build: -PskipCloneBench
87056b6f  Phase 0: plan + inert engine toggle
```
`openjdk` is the last green base (optimize already merged in there).
