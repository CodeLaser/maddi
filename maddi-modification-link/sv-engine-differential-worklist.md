# sv engine — differential worklist (194 assertion failures)

> **STATUS (superseded in large part):** the reconstruct half is now implemented; suite
> is **144/383 failing** (was 196). The dominant `←`-drop cluster and the active-collapse
> coarsening described below are **fixed** (commits `8fd6567f`, `a30fd159`, `e4bdd7e6`,
> `bd10f936`). The "prototype outcome: reconstruct missing across the whole extraction
> path" section below is now **done** — see `sv-engine-handoff.md` → STATUS UPDATE for the
> mechanism (intra-group `assignmentEdgeStream`, `expandRepToMembers`, `rehome`+`FromPair`
> faces, `isInvalidFieldContainment`). Remaining: §m on reconstructed edges (1 test) and
> engine order-stability. The histograms below are the *original* baseline analysis, kept
> for reference.

Baseline: `sv-integration` big-bang, `maddi-modification-link` at 196/383 failing
(15 skipped). This groups the **194 assertion diffs** by *what the new engine gets
wrong* versus the old fixpoint, so the engine phase has a prioritized to-do.

Method: for each failing `expected: <…> but was: <…>`, tokenize both link sets and
diff them. **Dropped** = in expected, missing from engine output (engine
under-produces). **Spurious** = engine emitted, not expected (engine over-produces).

> Caveat: a `?` bucket (no nature symbol) is mostly non-link tokens — method
> summaries like `C<init>(0)` and modification-set entries — i.e. tokenizer noise,
> not real link differences. Ignore it; focus on the nature-bearing rows.

## Headline: the engine drops assignment & identity links

**Dropped (engine MISSING), by nature:**

| count | nature | meaning |
|------:|:--|:--|
| **177** | `←` | **is-assigned-from — by far the dominant failure** |
| 31 | `→` | is-assigned-to |
| 27 | `≡` | identical-to (incl. §m mutation links) |
| 26 | `∈` | is-element-of |
| 16 | `⊆` | is-subset-of |
| 11 | `∩` | object-graph-overlaps |
| 8 | `~` | shares-elements |
| 8 | `∋` | contains-as-member |
| 7 | `≺` | is-field-of |
| 6 | `≥` | object-graph-contains |
| 1 | `⊇` | is-superset-of |

**Spurious (engine EXTRA), by nature:** `∋` 13, `≻` 13, `≺` 10, `∩` 6, `∈` 6, `⊆` 5,
`~` 4, `≡` 3, `←` 1.

**Read:** the engine **loses `←`/`→`/`≡`** (the assignment-flow and identity edges)
and, where it does emit something, tends to substitute **field-of / contains
(`≺`/`≻`/`∋`)**. That is consistent with the label algebra's `combine`/`best`
demoting or discarding the high-value assignment/identity edges during closure and
reduction — exactly the "prefer high-value direct edges (`←`+`∈`)" invariant the
engine's own doc says it must preserve but evidently doesn't yet.

**Structural:** ~113 of the 194 are near-total drops — the engine emits **nothing**
(or only `-`) where the old fixpoint had a full link set. So this is not fine
mis-labeling at the margins; whole link sets are collapsing.

## ROOT CAUSE FOUND (dominant cluster): shared-variable collapse drops fresh assignments

`Graph.mergeEdgeBi`, the `IS_ASSIGNED_FROM` branch. When **both endpoints are fresh**
(neither is yet a graph vertex — `fromInGraph.isEmpty() && toInGraph.isEmpty()`), it
creates the shared group (`sharedVariables.isAssignedFrom → create`) but **neither
`transformToSharedVariable` branch fires, so it `return true` adding nothing to the
graph** — the assignment is silently dropped. Only the "one endpoint already
materialised" case is implemented (see the nearby `TODO what with fromInGraph?`).

This is the *common* case: a constructor's `field ← param` links and method-entry
assignments are the **first** edges into an empty graph. Traced on
`TestSharedVariable :: direct assignment`:
- the `record S(String s1,String s2)` constructor feeds `this.s1 ← 0:s1`,
  `this.s2 ← 1:s2` into an empty graph → both dropped → **`S.<init>`'s summary is
  empty** → `new S($__rv1,$__rv2)` contributes no field links → `choose`/`method`
  emit `-`.

**The closure engine underneath is sound.** Bypassing the collapse (route `←`
through the normal edge path) makes the closure correctly derive
`choose.s1 ∈ r.list1.§$s` from `s1 ← param` combined with `param ∈ r.list1.§$s`.

**Quantified impact:** with the shared-variable collapse OFF, link failures drop
**196 → 114** — the incomplete collapse accounts for **~82 failures (42%)**,
including the ~113 "produces nothing" cluster. The remaining 114 are the intended
label-algebra re-baselines + other gaps.

**Fix (engine design — owner's call):** implement the both-fresh case so it
*materialises* the shared group in the graph (add the `$__sv_` vertex and represent
the assignment) so (a) later edges attach to the group and (b) summary extraction
reconstructs `field ↔ param` / element links. It must **reduce and preserve** — for
simple cases (no reduction benefit) the output should match the non-collapsed form.
Bypassing the collapse is a diagnostic, not the fix (it defeats the reduction goal).

## Prototype outcome: the reconstruct half is missing across the WHOLE extraction path

Prototyped the suggested (a)–(c) — `SharedVariables.edges()`, fold into
`edgesWithEquivalence()`, `engine.addVertex(sv)` for the both-fresh case — plus a
`FollowGraph` translate-back. `direct assignment` still emitted `-`. A probe of the
extraction state (`followGraph primary=… rep=… allShared=…`) showed the gap is
deeper and spans four points:

1. **`SharedVariables` rep↔members is one-way.** `memberToGroup` maps *members → rep*;
   the rep is never a key, so `allShared($__sv_s1) = [$__sv_s1]` — the rep can't
   enumerate its own members. (`SharedVariable.variables()` holds them, but nothing
   routes a rep query to it.)
2. **The graph is keyed on the synthetic rep, not real vars.** Collapse replaces
   `this.s1`/`param_s1` with a fresh `$__sv_s1` vertex, so `followGraph(choose)` finds
   nothing "part of" `choose`, and `followGraph($__sv_s1)` produces *rep*-keyed links,
   not `choose.s1`/`this.s1`-keyed ones.
3. **`edgesWithEquivalence()` feeds `computeSubs`, not the output.** Adding the
   shared expansion there doesn't reach the summary; the output path is
   `FollowGraph.closureStream` (which reads `engine.successorStream`, rep-keyed).
4. **Summary application doesn't carry the group through the call.** In `choose`,
   `allShared(choose)=[choose]` — applying `S.<init>`'s (would-be) shared-group
   summary never re-establishes the group in the caller, so the field↔param relation
   is lost even before extraction.

So completing the reconstruct is a coordinated change across `SharedVariables`
(rep→members + `edges()`), `Graph`/`FollowGraph` (expand rep vertices to
member-keyed links at extraction), and `LinkMethodCall` (translate the group when a
summary is applied) — not the 3-line patch first hoped.

**Simpler alternative worth weighing:** make the group representative a *canonical
real member* (union-find style) instead of a synthetic `$__sv_` node. Then the graph
stays keyed on real variables, `followGraph` finds them with no translate-back, and
only re-canonicalisation on reassignment needs care. This sidesteps points 1–3
entirely; point 4 (carry the group through summary application) remains.

## Prioritized attack plan

1. **`←` assignment-flow drop (177) — do this first.** One root cause almost
   certainly explains the bulk. Likely in the closure/reduction: assignment edges
   (`IS_ASSIGNED_FROM`/`_TO`, score = top) are being combined-away or reduced-away
   instead of preserved. Sharpest probes (produce *nothing*):
   - `TestSharedVariable :: direct assignment`
   - `TestGetSet :: modification in @Fluent setter`, `:: getS←this.s`
   - `TestList :: Analyze direct link from constructor parameter`
   Fix and re-measure — expect a large cascade.
2. **`≡` identity / §m drop (27)** — the mutation-identity edges. Probes:
   `TestCast :: links of cast` (`§m≡`), `TestRedundantModificationLinks :: simple chain`.
3. **`∈`/`⊆`/`~` element & subset drops (50 total)** — the stream/collection HC flow.
   Probes: `TestList :: 'sub'` (`⊆`), `TestMap` (`~`,`∩`,`≥`), `TestStreamMapSpec`.
4. **Spurious `≺`/`≻`/`∋` (36)** — usually the *flip side* of 1–3: once the correct
   `←`/`∈`/`~` survive closure, these field-of/contains substitutions should vanish.
   Re-check after 1–3 rather than chasing separately.
5. **`SharedVariable.acceptForLinkedVariables` (2 crashes)** — implement the
   unfinished path (return the intended boolean rather than throwing).
6. **Re-enable the `@Disabled impl/large` stress tests** once the mocks are made
   javac-compliant and 1–4 land — they are the engine-stability regression net.

## Highest-value test classes (dropped-link count — use as the differential targets)

| drops | class |
|------:|:--|
| 83 | `io.TestWriteAnalysis2` |
| 60 | `impl.TestLinkMethodCall` |
| 38 | `staticvalues.TestStaticValuesRecord` |
| 32 | `typelink.TestLanguageConstructs` |
| 24 | `typelink.TestSupplier` |
| 19 | `typelink.TestSupplierSpec` |
| 11 | `typelink.TestList`, `typelink.TestStreamMapSpec` |
| 10 | `impl.TestLinkModificationArea`, `typelink.TestStaticBiFunction` |

Start with `TestLinkMethodCall` / `TestGetSet` / `TestList` for the `←` root cause
(small, direct, assignment-centric), then widen to the stream/map classes.

## How to work it
- Keep the differential harness: `./gradlew :maddi-modification-link:test`, diff
  expected-vs-got per class.
- After each engine fix, re-run this analysis to watch the nature histogram shrink —
  the `←` row collapsing is the success signal.
- Each resolved diff is a real engine fix (correct link now produced), **not** a
  re-baseline — the old fixpoint's output is the oracle.
