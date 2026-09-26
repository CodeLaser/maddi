# Handoff — collapsing saturated closures in the link engine

**Written 2026-07-28. Self-contained for a maddi-only thread: no jfocus stack, no external corpus and no
Elasticsearch checkout is needed to reproduce anything below.** The measurements that motivated it were taken
downstream, but every claim is either reproducible from `maddi-modification-link`'s own tests or stated here
with its numbers so you do not have to re-derive it.

Read first, in this order:

1. `maddi-modification-link/linking-manual.md` §5 (LinkMethodCall) and §6 (worked examples) — the vocabulary.
2. `IncrementalFixpointEngine`'s own class comment, features 1–9. Feature **6** is the one that matters here.
3. `maddi-modification-link/src/test/java/.../impl/large/TestSaturatedClosureBench` — the reproduction, runs
   in ~7 s with no corpus.

---

# 1. The problem in one paragraph

`IncrementalFixpointEngine` maintains, per method, the transitive closure of a labelled link graph over the
method's variables. In some real methods that closure **saturates**: every variable ends up linked to every
other. The engine then stores and walks ~N² individual facts and spends cubic time propagating them, to
express something that has no information content — *everything here is linked to everything*. When the cost
exceeds the per-method work ceiling the method is abandoned and degraded to a shallow summary
(`DegradedAnalysisException`, `LinkComputerImpl.doMethod`).

**The proposal: detect saturation and represent it as one fact instead of N².**

Nothing in this handoff asks you to change what the engine *concludes*. The links being derived are correct
(§3). The question is purely one of representation.

---

# 2. What is already known — do not re-litigate these

These were measured, not argued. Two of them killed hypotheses that looked obvious.

**(a) It is not a work-ceiling tuning problem.** The ceiling is
`IncrementalFixpointEngine.WORK_CEILING` (`-Dmaddi.workCeiling`, default 10M edge visits per method,
opt out with `NOWORKCEILING=1`). Moving it on a 5047-type corpus:

| ceiling | methods degraded | whole-run wall clock |
|---|---|---|
| 10 M | 111 | 516 s |
| 30 M | **77** | **937 s** |

3x the budget buys a 31% reduction in degradations for 81% more time. **And four methods that completed at
10M tripped at 30M**: a method that now finishes writes a richer `METHOD_LINKS` summary, and its callers
inherit the extra links. The knob feeds the thing it bounds. *Leave the default at 10M.*

**(b) There is no cliff, so "the degraded methods" are not a distinct class.** On the same corpus, 549 methods
spend over 100k work; p50 of that tail is 423k, p90 is 4.46M, and the largest survivor is **9.81M** against a
10M line. The distribution runs continuously into the ceiling.

**(c) It is not method length.** Two of the degraded methods are one and four lines long respectively; longer
methods beside them are fine.

**(d) What the expensive methods share is closure density:**

| facts / ordered variable pairs | p10 | p50 | p90 | max |
|---|---|---|---|---|
| degraded methods (111) | 0.28 | **0.55** | 0.74 | 1.00 |
| survivors above 100k (438) | 0.17 | 0.50 | 0.86 | 1.00 |

**24 methods link over 95% of all variable pairs.** The largest is 173 variables and 29 756 facts — a
complete graph.

---

# 3. The shape that causes it, and why it is *correct*

`TestSaturatedClosureBench` reduces the smallest fully saturated method found (38 variables, 1338 facts, 100%
saturated — a plain validation method, no builder, no loop worth the name). It contains three variants, and
**two of them are negative results kept deliberately, because they are the expensive part of the answer**:

```
PROBE n=  5  accumulator=    64 ms   messages=     9 ms   containerViews=   108 ms
PROBE n= 10  accumulator=     7 ms   messages=     8 ms   containerViews=   207 ms
PROBE n= 20  accumulator=     7 ms   messages=    11 ms   containerViews=   835 ms
PROBE n= 40  accumulator=     7 ms   messages=    15 ms   containerViews=  5487 ms
```

- **`accumulator`** — a fluent `field(name, value)` chain fed n distinct values. **Flat.** This was the
  obvious guess: the saturated method names downstream are dominated by `toXContent` / `writeTo` /
  `buildTable`, and a fluent chain is what an earlier performance defect turned out to be
  (`TestBuilderChainBench`, a cubic in `SharedVariables.assignmentSources`, memoized away). Not this.
- **`messages`** — n exception messages each concatenating a shared parameter with a distinct value, which is
  literally what the reduced method's bulk is. **Flat**, with `Object` operands or `String` ones.
- **`containerViews`** — one container decomposed into `keySet()` / `values()` / `entrySet()` /
  `stream().sorted().collect()`. **51x for 8x the views.**

**And that saturation is semantically right.** `keySet()`, `values()` and `entrySet()` of one map alias each
other: a modification through any one is observable through the others. The engine is not over-approximating.
A method that threads one container through many views genuinely has a dense link closure.

So there is no defect to fix in the link natures or in `MakeGraph`. **The cost is the representation.**

---

# 4. The design question you have to answer first

A collapsed group must keep working for everything the closure is used for. In rough order of difficulty:

**(1) Witnesses.** `WitnessIndex` maps *each* `Fact` to a `Witness` — either a `DirectWitness` (a real graph
edge, with its statement index) or a `CompositeWitness` (a DAG node naming the left and right sub-facts).
`putIfBetter` prefers direct over composite and higher-scoring labels; `doesNotCreateCycle` uses
`Witness.support()` to refuse circular reasoning. If a saturated group is one fact, **what is the witness of
a pair inside it?** Options, none free:

- keep per-pair witnesses and collapse only the closure — saves the walk but not the memory, and
  `putIfBetter`'s preference order still has to be evaluated somewhere;
- give the group one witness and synthesise a per-pair witness on demand from the two members' edges into the
  group — cheap to store, but `support()` is what removal and cycle-avoidance depend on, so the synthesis has
  to be *stable* (the same pair must yield the same witness regardless of insertion order — see the "diamond
  pin" note in `completeSymmetrically`);
- refuse to collapse when a pair's witness is queried, i.e. collapse lazily and materialise on demand.

**Decide this before writing anything.** Every other question is downstream of it.

**(2) Labels are not uniform.** The engine is generic in `L`, and a group is only truly collapsible if all
pairs carry the *same* label. Feature 6 says forward propagation goes over the closure precisely so it can
find the *best* combination — a group that flattens distinct labels into one would silently lose that. So the
collapse condition is not "dense" but "dense **and label-uniform**", and you need to measure how much of the
observed saturation is label-uniform. **That number does not exist yet; get it before committing.**
`-Dmaddi.workDump=<method-name-substring>` prints one method's full closure via
`IncrementalFixpointEngine.printClosure()` and is the intended way to look.

**(3) Removal and repair.** Feature 8: modifications remove edges, and the closure is recomputed
(`recompute`, `Closure.removeFacts`, `WitnessIndex.removeFacts`, `materializeWitnessOrphans`). Removing one
edge from a collapsed group may split it. The cheap first implementation is *dissolve the group and rebuild*;
whether that is affordable depends on how often removal hits a saturated group.

**(4) Symmetry.** Features 2/3: the graph and the closure are symmetric by construction, and
`completeSymmetrically` maintains the mirror of every derived fact so that derivation is order-independent. A
group representation must preserve that property, ideally by construction rather than by maintenance.

---

# 5. Suggested order of work

1. **Measure label uniformity** (§4.2). If saturated groups are not label-uniform, the whole idea narrows to
   a special case and you want to know that on day one. `TestSaturatedClosureBench` gives you a saturated
   closure locally; dump it and look.
2. **Write the red spec before the mechanism.** Assert the *property*, not the timing: for the
   `containerViews` shape, `sizeOfClosure()` must stay O(N) rather than O(N²) while every
   `closure.label(a, b)` query returns what it returns today. A pure-timing assertion will be flaky and will
   not tell you when the semantics drift.
3. **Build the collapse behind a gate** (`Gate` is already used in this package for `NOWORKCEILING`), so a
   corpus run can compare on/off without a rebuild.
4. **Prove equivalence, then measure.** Equivalence first: over the existing suite, every `label(a, b)` and
   every `MethodLinkedVariables` outcome must be unchanged. `:maddi-modification-link:test` is 405 tests and
   runs in ~2 min; `TestLinkMethodCall` is the spec-by-example. Only then look at the ceiling trips.

**Acceptance:** the 405 tests stay green, `sizeOfClosure()` on the `containerViews` shape grows linearly, and
`TestSaturatedClosureBench`'s `containerViews` column flattens. Any change in a *linking outcome* is a
failure, not a trade-off — this is a representation change.

---

# 6. Instrumentation already in place

Committed with the measurement work (maddi `536ade20`), all of it usable without any downstream stack:

| knob | what it does |
|---|---|
| `-Dmaddi.workReport=N` | `LinkComputerImpl` logs one `LINKWORK` line per method above N work (default 100k): work, ceiling %, graph size, closure size, witness count, method FQN. Everything is at DEBUG regardless. |
| `-Dmaddi.workDump=<substring>` | full closure dump (`printClosure()`) for every method whose FQN contains the substring. |
| `-Dmaddi.workCeiling=N` / `NOWORKCEILING=1` | move or disable the ceiling. |
| `IncrementalFixpointEngine.work()` / `.workCeiling()` | the same numbers programmatically. |

`DegradedAnalysisException.Reason` distinguishes the two guards that abandon a method — `WORK_CEILING` (this
document) and `EXPANSION_ROUNDS` (`LinkGraph`'s 30-round expansion limit, which did **not** fire once in the
corpus measured). They used to share one message; do not let them merge again.

---

# 7. Why this matters downstream (context only — you do not need to act on it)

A degraded method carries no per-call data, so consumers that need it must treat the whole file
pessimistically. In the campaign that motivated this, that blocks interface extraction on exactly the types
worth extracting from: 52 types held for one target, 11 and 8 for two others. Those are the densest types in
the codebase — which is the same property that makes their closures saturate. Nothing about that changes the
maddi-side task, which stands on its own: the engine spends cubic time storing an answer with no information
content.
