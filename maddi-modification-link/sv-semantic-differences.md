# sv engine — semantic differences from the legacy link model

Assuming the sv label-algebra + engine changes are **intentional**, the 194 test
diffs are not bugs but the visible signature of a redesigned link semantics. This
maps the old model → the new model and explains each observed diff pattern.

## 1. Assignment: edge → equivalence group  (explains the 177 dropped `←`)

**Old:** `a = b` produces a first-class link `a ← b` (`IS_ASSIGNED_FROM`), which
survives into the output and is chased transitively in the fixpoint.

**New:** `Graph` intercepts an `IS_ASSIGNED_FROM` edge (`Graph.isAssignedFrom` →
`transformToSharedVariable` → `translateForward`) and folds the two endpoints into a
**`SharedVariable`** equivalence group (`$__sv_…`). Thereafter every reference is
`translateForward`-ed to the group representative, and modifications propagate across
the whole group (`WriteLinksAndModification` iterates `graph().allShared(v)`).

**Consequence:** `←` (and its mirror `→`) largely **disappear from the output by
design** — assignment is now *identity within a group*, not an edge. This single
change accounts for the dominant diff (177 `←` + 31 `→` "dropped"). The old tests
assert the edges; the new model asserts group membership. → **re-baseline, not bug.**

## 2. Modification-weakening: nature-swap → closure repair  (explains `⊆`/`⊇` drops, some `~`)

**Old:** an ad-hoc `replaceSubsetSuperset()` rewrote `⊆`/`⊇` into `~` *after* a
modification on the object (e.g. `List.add`), plus a second fixpoint iteration.

**New:** modifications instead **remove a selection of edges and recompute the
closure** (engine feature 8, the "repair" function). The `⊆`/`⊇` → `~` rewrite and
the removed subset edges are now emergent from re-closure, not a per-link swap. So
`⊆`/`⊇` links that the old model kept-then-rewrote may simply be absent post-repair.
→ mostly **re-baseline**; verify the surviving `~`/element edges match intent.

## 3. Two-axis labels: `rank` + `score`  (governs which links win, hence what survives)

**Old:** a single `rank` ("least→most interesting"); `best()` = higher rank; `valid`
and `known` were rank thresholds.

**New:** two axes.
- **`rank`** — specificity/interestingness, *renumbered and reordered*: object-graph
  natures (`∩ ≤ ≥`) are now the **least** interesting (ranks 2–4), then field/shares
  (`≈ ≺ ≻ ~`), then subset (`⊆ ⊇`), then the candidates (`∈? ∋?`), element (`∈ ∋`),
  decoration, and finally **identity/assignment (`≡ ← →`) as the most interesting**.
  `best()` still keeps the higher rank.
- **`score`** (new, *inverse* of rank: `∩`=20 … `← ≡`=1) — a **path-combination
  value**. During closure the engine "prefers direct high-value edges" (feature 6:
  `←+∈` beats `∈+~`), and forward propagation runs **over the closure, not the raw
  graph**, so it can find the *best* combination rather than *a* combination.

**Consequence:** composition is now score-directed. Where the old model would emit a
weaker combined edge, the new one prefers the stronger direct one — changing which
links appear. → **re-baseline**, but the target output should be *more* canonical.

## 4. Candidate natures `∈?` / `∋?`: hypothesis-then-confirm  (new vocabulary)

**Old:** `combine` was a direct table; a composition either produced a concrete
nature or `X` (dropped).

**New:** combining an element relation with a shares/subset relation yields a
**tentative** `∈?`/`∋?` ("could be element of / could contain"). It is later
**resolved to `∈`/`∋` iff the closure independently supports it** (feature 6, doc
cites `TestDependent,2`: "`∈?` finally gets replaced by `∈` because of an already
existing combination in the closure"), otherwise it stays weak or is pruned. This is
a **non-monotone, closure-aware** composition — a genuinely different inference step,
not present in the old algebra. Expect `∈?`/`∋?` to appear in intermediate state and
occasionally in output where the old model had `X` or a firm `∈`.

## 5. Broadened `valid()`: field-of / contains-as-field are now first-class  (explains spurious `≺`/`≻`)

**Old:** `valid()` = `rank ≥ SHARES_FIELDS`, which **filtered out** `≺` (`IS_FIELD_OF`)
and `≻` (`CONTAINS_AS_FIELD`) — they were never stored.

**New:** `valid()` = `rank > EMPTY` — **every real nature is stored**, including
`≺`/`≻`. That is exactly why the diff shows "spurious" `≻` (13), `≺` (10): they are
now legitimate, retained relations that the old model discarded. → **re-baseline**;
these are additions the new model intends.

## 6. Witness-tracked, directional closure  (soundness machinery, not visible output)

- **Witnesses** record *how* each fact was derived (a DAG), giving cycle-free
  reasoning and a sound basis for **reduction** (removing genuinely-redundant links).
  The old fixpoint had no provenance, so it over-kept.
- **Forward + backward phases**: because `reverse` and `combine` don't commute
  (`rev(∋+∈) ≠ rev(∋)+rev(∈)`), the closure is computed one direction at a time.
- **`return values cannot form composite facts`** (feature 9, `acceptForComposite`):
  return variables are excluded from transitive composition — a deliberate scoping
  rule the old model lacked.

Net: the reduction is the *point*. Many "dropped" links are redundant edges the new
model correctly removes.

## What this means for the 194 diffs

Under the intentional-semantics assumption, the diffs partition roughly as:

| pattern | count (approx) | verdict |
|---|---|---|
| `←`/`→` dropped | 208 | **re-baseline** — assignments are now shared-variable groups (§1) |
| `≡` dropped | 27 | **re-baseline / verify** — §m identity via groups + repair (§1,§2) |
| `⊆`/`⊇`/`~` dropped | 25 | **re-baseline / verify** — modification-repair (§2) |
| spurious `≺`/`≻`/`∋` | 36 | **re-baseline** — now-valid field relations (§5) |
| `∈`/`∩`/`≥` shifts | ~30 | **verify** — score-directed composition (§3,§4) |
| ~113 near-total drops | — | **verify carefully** — some are §1 groups; a whole set collapsing to `-` may be a genuine gap |
| 2 crashes (`acceptForLinkedVariables`) | 2 | **genuine unfinished path** — implement |

So the honest recommendation flips: **most of the 194 are re-baselines to the new
model, not bugs.** The real work is:
1. Regenerate/re-baseline the expectations from the new engine, per class, *reading*
   each to confirm it matches the intended semantics above (not blind-accepting).
2. Isolate the true bugs: the near-total-collapse cases where even the new model
   should produce links (start with the `got-empty` list in the worklist), and the
   2 unfinished `SharedVariable` crashes.
3. Re-enable the `@Disabled` stability tests once the mocks compile.

The `SharedVariable`-group representation of assignment is the linchpin: confirm it
first (e.g. `TestSharedVariable :: direct assignment`, `TestSimpleSharedVariable`) —
if the group semantics are right there, most of the `←`/`→`/`≡` diffs are explained
and safe to re-baseline.
