# Integrating the `sv` engine into `openjdk` — incremental plan

Goal: bring the `sv` branch's work (a new incremental fixpoint engine + shared-variable
reassignment/modification tracking) onto `openjdk` in small, independently-green steps.

## Strategic framing: port, don't merge

`sv` is a **reference implementation to port from**, not a branch to merge:

1. **Off the old base** (`7c331cc9`). A direct merge re-opens the whole pre-openjdk conflict
   surface on top of an openjdk that already contains `optimize`.
2. **Not green.** `sv` is a research spike — it carries an unresolved *"graph is not stable"*
   observation and fluctuating red counts.
3. **50 WIP commits**, many `tmp`/broken intermediate states.

Instead: cherry-pick the *ideas* forward as focused, independently-green slices onto the current
`openjdk`, using a **parallel-engine-behind-a-toggle** approach (`LinkComputer.Options.engine`)
so `openjdk` stays green at every commit. The big enabler: `sv`'s engine has standalone unit
tests, so it can be hardened in isolation before it touches live linking.

Work happens on branch **`sv-integration`** (off `openjdk`); each phase merges to `openjdk` once
green.

## Delta map (`optimize..sv`, since `openjdk` now contains `optimize`)

New generic engine (`impl/graph/`, no maddi dependency):
- `IncrementalFixpointEngine`, `Closure`, `Fact`, `Witness`, `WitnessIndex`, `LabeledGraph`

New maddi graph layer (`impl/linkgraph/`):
- `Graph`, `LinkGraph` (new); `SharedVariables`, `VirtualModificationIdenticals` (new)
- `Edge`, `ExpandSlice`, `FollowGraph`, `MakeGraph` (renamed from `impl/graph/`)

New shared-variable marker:
- `impl/localvar/SharedVariable` (prefix `$__sv_`)

Deleted (optimize's old engine, removed once the new one is default):
- `impl/graph/{AddEdge, FixpointPropagationAlgorithm, LinkGraph, RedundantLinks, Timer}`

Modified — interfaces (prepwork): `variable/{Link, LinkNature, Links}`, `variable/impl/LinksImpl`
Modified — wiring/impl: `LinkNatureImpl` (label algebra), `LinkComputerImpl`,
`WriteLinksAndModification`, `ExpressionVisitor`, `LinkAppliedFunctionalInterface`,
`translate/{TranslateConstants, VariableTranslationMap}`, `analyzer/SingleIterationAnalyzerImpl`,
`module-info`.

Acceptance tests carried from `sv`:
- Engine (isolated): `impl/graph/{TestEngine, TestClosureWitnessIndex, TestLabeledGraph}`
- Shared variables: `impl/basics/TestSimpleSharedVariable`, `staticvalues/TestSharedVariable`
- Stability stress: `impl/large/Test3`

## Phases

### Phase 0 — Groundwork  ✅ (this commit)
- Delta map (above) + this document.
- Add `LinkComputer.Options.engine = {LEGACY, INCREMENTAL}`, default `LEGACY`. Inert: nothing
  reads it yet.
- **Green gate:** no behavior change; full suite unchanged.

### Phase 1 — Label algebra + prepwork interfaces  ⚠️ REVISED: not additively separable
**Finding (during Phase 1 start):** the label algebra is *coupled* to the engine and cannot be
landed additively:
- `LinkNatureImpl` is a **shared value class**. sv **renumbers all ranks**, adds two natures
  (`∈?` `COULD_BE_ELEMENT_OF` / `∋?` `COULD_CONTAIN_AS_MEMBER`), removes `known()` and
  `replaceSubsetSuperset()`, and broadens `valid()` (`rank>=SHARES_FIELDS` → `rank>EMPTY`).
- The renumber changes `known()`/`valid()` results, which the **legacy** fixpoint reads.
- Exactly two legacy call sites depend on the removed/changed methods, both in code the engine
  replaces: `WriteLinksAndModification:185` (`replaceSubsetSuperset`) and
  `graph/FollowGraph:76` (`known()`, `valid()`).

So Phase 1 folds into the engine slice. **Recommended approach (II — coexistence):** port sv's
`LinkNatureImpl` (new ranks, natures, `score`, `compareTo`) but **re-implement `known()` /
`replaceSubsetSuperset()` / `valid()` by identity** so legacy `FollowGraph`/`WriteLinksAndModification`
keep byte-identical behavior, while the engine uses `score()` + the new natures. Keeps the toggle
alive. Alternatives: (I) big-bang replace legacy in one slice (simpler, no toggle fallback, riskier);
(III) clone the label type for the engine (most isolation, most code).
- **Immediate next step:** empirically test approach (II) — apply the port + identity-preserving
  methods, run the legacy suite; green ⇒ (II) viable.

### Phase 2 — The engine as an isolated, hardened library  (the crux; now includes the label algebra)
- Port the generic `impl/graph/` engine + `TestEngine`, `TestClosureWitnessIndex`,
  `TestLabeledGraph`, and the `Test3`/large stress test.
- **Harden the known instability here, in isolation.** Attack with: repeat-N determinism runs
  (sorted output identical), the ParSeqElement stress case, and confluence property tests (closure
  independent of insertion order). Fix witness/completion logic until stable.
- **Green gate:** engine unit tests pass *deterministically* across repeated runs.

### Phase 3 — Wire in behind the toggle + differential testing
- Port `impl/linkgraph/` (`Graph`, `LinkGraph`, renamed `MakeGraph/FollowGraph/Edge/ExpandSlice`);
  wire into `LinkComputerImpl` under `engine=INCREMENTAL` (legacy path still default).
- Differential harness: run every existing link test through *both* engines, diff output.
  Classify each divergence as engine bug (fix) or intended improvement (re-baseline, with written
  justification — same discipline as the feedParam call).
- **Green gate:** with the toggle ON, `modification-link` + `modification-analyzer` pass; every
  re-baseline documented.

### Phase 4 — Shared variables / reassignment
- Port `SharedVariable`, `SharedVariables`, `VirtualModificationIdenticals` + the
  `WriteLinksAndModification` reassignment handling.
- Pins: `TestSimpleSharedVariable`, `TestSharedVariable` (`x = y; x.mutate() ⇒ y modified`).
- **Green gate:** shared-variable tests pass; existing suite still green under the new engine.

### Phase 5 — Flip default, delete legacy, measure
- Make `INCREMENTAL` default; delete optimize's `FixpointPropagationAlgorithm` + `RedundantLinks`
  + old `impl/graph` graph classes; remove the toggle.
- Perf check: confirm the engine is faster on `TestCloneBench` (the point of the reduction work).
- **Green gate:** full-project green + no perf regression.

## Cross-cutting hardening (every phase)
- **Differential testing** (new vs legacy) is the primary safety net.
- **Determinism runs** — repeat each linking computation N times; sorted output byte-identical.
  Directly targets the instability `sv` flagged.
- **Always-green** — the toggle keeps `openjdk` shippable at every commit; each phase is its own
  reviewable series.
- **Reference, not truth** — where `sv`'s tests encode over-generation we already rejected (bare
  `∋`, etc.), re-baseline to openjdk's canonical behavior.

## Biggest risks
1. **Engine stability** (Phase 2) — hardest; `sv` never fully solved it. Isolated stress +
   determinism testing is the mitigation; if it can't be made stable, stop-and-reassess.
2. **Divergence volume** (Phase 3) — the differential harness makes it tractable/auditable.
3. **Shared-variable ↔ engine interaction** (Phase 4) — keep as a separate slice so failures are
   attributable.
