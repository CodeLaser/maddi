# sv engine — catalogue of the remaining link failures

## UPDATE — RedundantLinks ported; 74 failing

The pre-sv engine ran a **cross-variable transitive-redundancy suppressor**
(`graph/RedundantLinks`, per-statement accumulating guards per nature-group) on every
extracted builder — it did not survive the sv big-bang port, which explains the whole
SPUR[⊆] band ('stream1.§xs⊆0:in.§xs' emitted where the old model kept only the nearest
hop 'stream1.§xs⊆stream.§xs'). Ported to `linkgraph/RedundantLinks` and wired into
WriteLinksAndModification (skip returns; skip last-statement parameters — the method
summary reads them there; `lastStatement` re-threaded through doBlock/doStatement/go).
[NORL gates]

Suite 79 → 74: fixed MR-swap/find-first/identity/test4 + filter and 4 more turned
order-only (re-baselined). One documented trade-off: TestLinkMethodCall 'generic
factory method' gains a spurious `0:from*≈1:to*` — a second-order effect through the
modification flow (RL-pruned builder → different unmodified verdict → different graph
state), NOT a direct suppression; joins test7/Stream.generate as known gaps. The old
engine also coupled its §m-modification check to RedundantLinks.modificationLinks
(completion-based, with the ☷ pass logic) — NOT ported; the VMI groups-based check
covers it. Porting modificationLinks for full fidelity is a candidate follow-up if the
≈ or the DROP[≡] band (5, missing return-side §m companions) point there.

## UPDATE `9ac37d4b` — SPINE MERGED; suite 79 failing, deterministic

`sv-spine-wip` is merged into `sv-integration` (86 → 79, 0 regressions vs 140, verified
run-to-run deterministic). The owner≻own-virtual-field spine is restored; the varargs ∩
family is GREEN. Four engine bugs fixed en route: half-translated collapse re-keying,
witness-orphan loss on vertex removal (materialization, with symmetric-coherence labels),
the ☷ pass set ignored in §m modification propagation, and NONDETERMINISTIC witness
choice (putIfBetter first-arrival ties + unsorted support print) rooted in
LocalVariableImpl's missing hashCode (identity-hash map iteration). Env gates
NOSPINE/NOMAT/NOBOTH/NOMIRROR/NOPASSFIX/NODESC remain for bisecting.

Remaining known gaps vs the pre-spine baseline (2): TestSupplier.test7 (one link,
`entry.§xy≺0:optional` — at baseline it arrives via an unidentified post-extraction
path; note test7 fails in ISOLATION even at baseline, class runs only) and
TestSupplierSpec Stream.generate. Follow-ups worth noting: LocalVariableImpl.hashCode
(core CST fix would de-risk everything), and the closure-dump tests now assert
deterministic witness text (re-baselined).

## (historical) UPDATE branch `sv-spine-wip` (331b1f21) — varargs-∩ ROOT CAUSE SOLVED; integration WIP parked

**Ground truth obtained by running the OLD engine** (worktree on the `openjdk` branch,
graph dump of TestVarargs.test3a): the old graph links every variable to its own virtual
field — `collection ≻ collection.§is`, `0:target ≻ target.§is` — via `AddEdge.addField`,
unconditionally. The varargs ∩ derives through
`target.§is ~ collection.§is ≺ collection ∈ collections.§iss` (`~∘≺=∩`, `∩∘∈=∩`).
**The sv engine's `invalidEdge` (commit `29d597d9`, a graph-size reduction) severed this
owner≻own-virtual-field SPINE** — that one reduction is the root of the entire fan-out
family, NOT a missing for-each edge (all 3 earlier edge-injection attempts were doomed).

Branch `sv-spine-wip` holds the five-component restoration (each env-gated: NOBOTH/NOMAT/
NOPASSFIX/NOMIRROR), with four REAL engine bugs found and fixed along the way:
1. `invalidEdge`: allow ≻/≺ for owner↔own-virtual-field only (`from == fieldScopeRoot(to)`).
2. `transformToSharedVariable` re-keyed only the FROM side of member edges — the
   untranslated to-side resurrected removed vertices; half-translated spine edges were
   dropped at every collapse, order-dependently.
3. **Witness-choice nondeterminism**: a closure fact between two survivors dies on vertex
   removal iff the (arbitrarily chosen) recorded witness routes through the removed vertex.
   Fixed by `materializeWitnessOrphans` in `removeVertices`: promote such facts to raw
   edges — with the closure's OWN label per direction (the closure is direction-asymmetric,
   rev(combined)≠combine(reversed)), else the graph⊆closure consistency invariant breaks.
4. `notLinkedToModifiedVirtualModification` ignored the ☷ pass set (flat equivalentStream):
   `next()` modifying the ITERATOR marked the iterated COLLECTION modified (spurious `*`).
   Fixed group-aware with the pass check (mirrors the ≡-branch of notLinkedToModified).
5. Field-level mirrors of reconstructed intra-group assignments (`combine.§is←target.§is`),
   projecting the rep's field vertices onto both endpoints in sharedAssignmentEdgeStream.

WIP status: TestVarargs 4/4 content-exact (order-only), deterministic; suite 92 vs 86 —
net -6, so NOT merged. Remaining on the branch: 4× TestSimpleSharedVariable closure-dump
re-baselines (spine facts are correct, additive), TestConstructor coarse `≤` (broadened
algebra), TestDeepStructureBench consistency assert (`Worse! mapRight.§tts[-1] ~ $__rv26.§ts
have ∩` — removal semantics of slices/intermediates vs materialized edges; note clear()
leaves `v.§f` vertices orphaned when `v` is removed), TestSupplier test7 + Stream.generate,
and a 2-test run-to-run flip (TestSupplier test1/test5Method2). The remaining work is
concentrated in the engine's REMOVAL SEMANTICS — a focused design pass, with the gates
making each component independently testable.

## UPDATE `cdf7cf3f` — 86 failing; remaining roots mapped

Since 100: `aa5a8593`+`73551f8c` directionality attribution of a rep's incoming
edges (100→92); `cdf7cf3f` multi-valued-assignment vs reassignment
(`m = cond ? a : b` keeps both `m←a` and `m←b`; 92→86). All 0-regression.

Remaining 86 are scattered across many small roots (biggest class TestStaticValuesRecord
×11, TestLanguageConstructs ×9). Clean roots identified but DEFERRED as deeper/riskier:
- **Fluent setter `setX.x←this*.x` (~3: TestGetSet×2, TestStaticValues1).** `return this`
  collapses {return, this*}; the whole-object link `setI←this*` is present but its
  field-level mirror `setI.i←this*.i` is not derived. Needs field-expansion of a
  whole-object `←` link, or the FollowGraph sibling-face pass (reverted earlier for
  regressing) applied to non-assignment field edges.
- **Varargs `∩` fan-out (4: TestVarargs, all miss `0:target.§is∩1:collections.§iss`).**
  DIAGNOSED (confirmed correct, incl. the ∩-vs-~ question): the fields carry multiplicity as
  array dimension — `target.§is : I[]` (depth 1), `collections.§iss : I[][]` (depth 2). So
  `collection.§is : I[]` is an ELEMENT (a row) of `collections.§iss : I[][]`, i.e. the bridge is
  `∈`, and `~ ∘ ∈ = ∩` (combine table line 369-373). So `∩` is correct and type-consistent (NOT
  `~`; `~` would need a flattened I-set model). NOSV == SV here (both miss it), so this is a
  base-engine gap, not sv. Root: `collection.§is` is structurally DISCONNECTED in the closure —
  its only edge is `~ target.§is`; nothing links it to `collections.§iss`. The whole `collection`
  has `∈ collections.§iss` but that ∈ never descends to `collection.§is`.
  DEAD ENDS (3 attempts, all reverted, all regress). Full-graph dump at that statement:
  ```
  [collection]        -> ∈ $__rv0.§iss, ∈ collections.§iss     ($__rv0 = iterator, deep content)
  [collections.§iss]  -> ∋ collection, ⊇ $__rv0.§iss
  [$__rv0.§iss]       -> ∋ collection, ⊆ collections.§iss
  [target.§is]        -> ~ collection.§is
  [collection.§is]    -> ~ target.§is                          ← ISOLATED pair
  ```
  The two facts `target.§is ~ collection.§is` and `collection ∈ collections.§iss` share NO vertex
  (`collection.§is` ≠ `collection`), so the closure cannot combine them. To bridge, `collection.§is`
  must connect to `collection` or `collections.§iss`. Attempts:
  1. `collection.§is ∈ collections.§iss` as a raw for-each extra edge — over-produces: the ∈
     combines broadly in the closure (spurious `ii.§$∈iis.§$s`, `0:from*≈1:to*`, dropped `←`/`≥`
     in TestForEach/TestLinkMethodCall/TestList/TestPrefix), and it surfaces as a first-class link
     on the loop var's own view (`collection.§is∈…`, breaks the `collection∈∈…` assertion).
     Re-visiting the iterable to get the container var also renumbers intermediates globally.
  2/3. Variants of (1) with tighter guards / no re-visit — same over-production.
  BLOCKER: the natural container↔own-hidden-content edge (`collection ∋/∈ collection.§is`, or
  `collection.§is ⊆ collection`) that would let the closure bridge is either rejected by
  `invalidEdge` (real↔virtual allows only ∈ ∋ ← →, NOT ⊆/~/≡/≺/≻) or doesn't combine
  (`∈ ∘ ∈` is undefined in the table). So the fix is NOT a raw edge; it needs either a
  multiplicity-aware CLOSURE rule (descend an element-of through hidden content) or a precise
  nested-content mapping in `LinkMethodCall.objectToReturnValue` for `iterator().next()` (map the
  returned element's `§is` to the object's one-deeper `§iss`), done so it does NOT materialise a
  standalone edge on the loop var. Both are deep label-algebra/closure surface; needs a design pass.
- Scattered: `DROP[] SPUR[]` (13, heterogeneous — `∈`/`∈?`, `*`-modification-marker,
  var-name), `DROP[→]` (4, TestCast), Stream/BoundTypeParameter HC (structural).

## UPDATE `9c971a99`/`ea7ca0b3` — Supplier cluster CORE FIXED (112 → 100)

The Supplier/Optional `result ← optional.§x` drop was root-caused (after four
misdirections — return summary, cross-statement carry, LinkMethodCall this.§t,
final-vd union — all ruled out by tracing) to **two adjacent drops in
`Graph.mergeEdgeBi`**, found by logging every edge touching `§x`:
1. `invalidEdge` rejected any real↔virtual edge whose label wasn't `∈`/`∋`, so
   `x ← optional.§x` was dropped outright. Fixed: also allow `←`/`→` (a value read
   out of hidden content is assigned from it).
2. the shared-variable collapse then folded `optional.§x` into x's group. Fixed:
   collapse only whole-object aliases (skip when either endpoint is virtual).
`9c971a99` — 11 tests recovered, 0 regressions. `ea7ca0b3` — re-baselined the
order-only `§x` assertions the fix exposed. `TestSupplier.test1` (canonical case)
passes.

**Remaining Supplier sub-issue (secondary, ~4-8 tests):** a spurious param
cross-link `0:optional.§x → 1:alternative`. Cause: `x` is multi-source
(`x ← optional.§x` OR `x ← alternative`); `{x, alternative}` still collapse, and
the rep's edge `rep ← optional.§x` (which belongs to x) is wrongly attributed to
the sibling `alternative` when the parameter is extracted — the collapse can't tell
which member owns a non-group edge. Specific to stored-lambda FI shapes (inline
lambda `test1` is clean). Fix needs per-member edge provenance or not collapsing a
variable with its own source when it has other sources. Deferred.

## UPDATE `33fa42a9` — 112 failing; the DROP[←] cluster mapped

Since 116: `e9779b5c` reconstructs field/element return endpoints via sibling faces
(116→114); `33fa42a9` suppresses coarse scope-up links (copy≈0:pair) redundant with
reconstructed field links (114→112). Both 0-regression.

**The dominant remaining cluster is DROP[←] (~35 of 112): a return summary drops a `←`
link that is present at statement level.** Sub-clusters:
- **Supplier/Optional `result ← optional.§x` (~13, biggest): TestSupplier×8, TestSupplierSpec×~7.**
  Root cause (traced): `X x = optional.orElseGet(...); return x;` collapses {x, alternative,
  return} into a rep. The whole-object group assignments (`method←x`, `method←1:alternative`)
  are reconstructed by `sharedAssignmentEdgeStream`, but `x`'s NON-group link
  `x←0:optional.§x` (to a param virtual field that never collapsed) is carried by neither the
  group reconstruct NOR the graph (it lives only in VariableData; `optional.§x` is never a
  graph vertex at extraction — a FollowGraph probe gated on it never fired). The return
  statement (`LinkComputerImpl` ~547-555) only adds `return ← x`.
  **DEEPER (second trace): the fix is NOT at the return summary.** At the return statement's
  forward pass, `previousVd.variableInfo("x")` is already just `x←1:alternative` — x has
  ALREADY lost `←optional.§x` — while the test's final `vd0` read (post-fixpoint) has both.
  So `←optional.§x` exists only in statement 0's *final/back-propagated* extraction, not in
  the forward-carried state that feeds the return summary, and `r.links()` for the bare
  `return x` is empty. x is assigned from TWO sources (`optional.§x` OR `alternative`); the
  collapse via `x←alternative` keeps only the group member and drops the non-group
  `x←optional.§x` from x's carried state (cf. the `merge()` NYI note for `x=y;x=z`). **Real
  fix is cross-statement / fixpoint-ordering, not the summary** — either preserve a collapsed
  variable's non-group links across statements, or recompute the return summary after
  back-propagation. Materially larger; needs a dedicated design pass.
- getter/setter `setX.x←this*.x` (~3: TestGetSet×2, TestStaticValues1) — fluent setter returns
  `this`, field flow to a `this*` face.
- LanguageConstructs `m←0:a` (~4: ternary, switch-guard, intersection bound, wildcard-super) —
  param→return through a control-flow construct.
- one-offs: `reverse3←$_v` (whole←value-marker), `prev←1:x.ts[0:i]`, record-builder array elts.

DROP[→]×4, DROP[≡]×3 (§m companions), SPUR[∋]×5 / SPUR[⊆]×4 / SPUR[∩]×3 (coarsening) are the
next bands. 23 of 112 are fully-empty-got.

## UPDATE `45d1046f` — 116 failing (was 144 → 140 → 120 → 116)

Since the snapshot below (144), three things landed:
- **Test infra / speed (not link fixes).** AAPI analysis hints are now gated on
  classpath module presence (`d2304078`): the loader skips a primary type whose
  module is absent instead of force-loading it. The shared test classpath is lean
  by default (java.base only; heavy = explicit `javaInspectorFactory("java.desktop"…)`)
  (`8fd9d59c`). Link suite 2m45s → ~1m20s.
- **Order-only re-baseline `b7e08432` (140 → 120).** 38 failing assertions differed
  from expectation only in link ORDER (multiset-identical), an artifact of §m being
  synthesized inline after its parent ←/→/≡ edge rather than as a rank-sorted graph
  edge. Re-baselined to the engine's deterministic output; 20 tests went green (the
  other 18 touched tests still fail on genuine diffs). The deep fix remains
  **§m-as-a-real-edge** (would remove the artifact at source + the truly-dropped-§m
  cluster + the enum regression).
- **Return-summary reconstruct `45d1046f` (120 → 116).** `SharedVariables.reachable`
  now chains through bare scalar locals (not just `$__rv` intermediates), gated to the
  whole `ReturnVariable` start with a dimension guard (no array elements / field faces).
  Fixes the `return ← ttt ← tt ← 0:t` → `method←0:t` drop
  (`TestAssignmentIdentityMethod.test1`, the regression test). 0 regressions.

Remaining 116 are genuine diffs. Dominant clusters (re-measured): **empty-got return
summaries (~29)** and **dropped-`←` (~30 more)** — the same return-summary-lost family,
now partly addressed; next candidates are the field/element return endpoints
(`TestSupplier`, `TestSupplierSpec`, `TestLinkTypeParameters`, `TestStaticValuesRecord`
builders) that still lose their `←`. Then structural (varargs `∩`, Map `∋`, Stream/
bound-type-param HC) and the §m-as-edge work.

---

# (historical) catalogue of the 144 remaining link failures

Snapshot at `eaf67350` (`sv-integration`), full `:maddi-modification-link:test` run.
Baseline was 196; the reconstruct work (see `sv-engine-handoff.md` → STATUS UPDATE)
brought it to **144/383**. This is the current, categorised to-do.

> **Determinism — the suite IS deterministic (the "instability" was a measurement
> artifact).** An earlier report of ~40 tests flipping between runs was wrong: it came from
> diffing two captures made with **inconsistent HTML-entity decoding** (`T->R` vs
> `T-&gt;R` counted as different tests). With a consistent XML parser, **three independent
> parallel runs (`forks=4, forkEvery=0`) are byte-identical, and equal serial, monolith
> (all classes in one JVM), and isolated (JVM per class)** — 0 flips across all. So the
> counts below are canonical and reproducible; `forkEvery=1` was reverted (bought no
> determinism, cost ~20% wall time). The known intermittent javac `SharedNameTable` issue
> is mitigated by `-XDuseUnsharedTable=true`.

## Top-line breakdown

| category | count | meaning |
|---|---:|---|
| **"Assignment diffs"** | **61** | only `←`/`→`/`≡` differ — but a read-to-confirm pass showed these are **NOT re-baselines, they are reconstruct bugs** (see below). The `←` links are *dropped to empty* or lose their `§m≡` companion. The semantic-differences "`←`→group" premise predates the reconstruct; now that reconstruct exists the `←` links should reappear and match the old expectations. **Do not re-baseline these.** |
| **Structural diffs** | **72** | `∈`/`⊆`/`∩`/`~`/`≺`/`≻` differ, or same natures with different vars/order. Mix of intended broadenings (§3–5) and real gaps. |
| **Crashes** | **5** | one cause: `SharedVariable.acceptForLinkedVariables()` throws `UnsupportedOperationException`. |
| **Engine unit tests** | **3** | `TestEngine`, `TestLabeledGraph`, `TestClosureWitnessIndex` — isolated engine assertions. |
| **Empty summary** | **4** | return summary fully lost (`--> -`). |

Dropped-nature histogram (expected − got): `←`×100, `≡`×28, `∈`×19, `≺`×9, `→`×6,
`∩`×6, `∋`×4, `⊆`×4, `≥`×3, `≻`×2, `~`×1. Spurious: `∩`×6, `⊆`×5, `≡`×5, `≈`×4,
`←`×3, rest ≤2. The `←`/`≡` dominance is the re-baseline signature.

## Per-class map (reb = assignment re-baseline, str = structural)

| class | reb | str | empty | crash | note |
|---|--:|--:|--:|--:|---|
| TestLanguageConstructs | 9 | 6 | | 2 | broad mix + 2 crashes (labeled break/continue) |
| TestStaticValuesRecord | 11 | 1 | | | almost pure re-baseline |
| TestSupplierSpec | 8 | 2 | | | mostly re-baseline (FI supplier) |
| TestList | 2 | 6 | | | structural (collection HC) |
| TestLinkMethodCall | 4 | 4 | | | the spec — split |
| TestSupplier | 6 | 1 | 1 | | mostly re-baseline |
| TestStream | | 6 | | | **all structural** (stream HC flow) |
| TestBoundTypeParameter | 1 | 5 | | | **structural** (type-param HC) |
| TestStaticBiFunction | 3 | 3 | | | split |
| TestVarargs | | 4 | | | structural — missing varargs `∩` fan-out |
| TestForEachLambda | | 4 | | | structural (FI consumer) |
| TestStreamBasics | | 3 | 1 | | structural |
| TestDependent | | 3 | | | structural (Iterable.iterator `∈` lost) |
| TestArrayAccess | | 3 | | | structural |
| TestLinkModificationArea | | 3 | | | structural |
| TestMap | | 1 | | 2 | 2 crashes + `∋`→`∩` coarsening |
| TestSwitchExpression | 1 | 2 | | | |
| TestStaticValues1 | 3 | | | | re-baseline |
| TestFunction | | 2 | | | Optional `∈` lost |
| TestStaticValuesMerge / TestStaticValuesIndexing | | 2 each | | | structural |
| TestGetSet / TestCast / TestLinkTypeParameters | 2 each | | | | re-baseline |
| ~18 more classes | | | | | 1 each (see run) |
| **TOTALS** | **60** | **72** | **4** | **5** | + 3 engine |

## Real bugs (the read-to-confirm correction)

A read-to-confirm pass over the 61 "assignment diffs" found they are **not** re-baselines
but two reconstruct-bug clusters:
- **~13 §m-only dropped** — a reconstructed `field ← param` edge is missing its `§m≡`
  companion (`X←Y` present, `X.§m≡Y.§m` gone). Same root as the enum-§m regression;
  hits `TestStaticValuesRecord`×8, `TestCast`, `TestList`, `TestBoundTypeParameter`,
  `TestRedundantModificationLinks`. **One root cause: §m is generated in `FollowGraph`
  for a real graph edge, but the reconstructed intra-group edge bypasses it.**
- **~45 return-summary-lost** — the `←` field/element links on the *return* side drop to
  empty (`TestSupplierSpec`×8, `TestSupplier`×6, `TestLinkMethodCall`×4,
  `TestStaticBiFunction`×3, `TestGetSet`×2 return side, …). Reconstruct gap in
  constructor/supplier/bifunction returns — extends this session's field-of-rep /
  return-alias work into more shapes.

Plus the structural cluster below.

The genuine remaining defects cluster:

1. **~~`acceptForLinkedVariables()` crash~~ DONE (crashes removed, not counts).**
   Implemented to `return false` (the rep is synthetic; its members are expanded
   separately) — the 5 `UnsupportedOperationException`s are gone. But the underlying tests
   still fail: they are **2-D array / `∈∈` / large-JSON** cases (`TestMap`×2,
   `TestLanguageConstructs` labeled break/continue over `g[0][0]`, `TestWriteAnalysis2`),
   now clean assertion-fails in the structural bucket. `expandRepToMembers` was also
   extended to array (`DependentVariable`) scopes for symmetry with the field branch — no
   test delta yet; groundwork for the array cluster.
2. **Return-summary-lost (4 empty + several structural-empty).** `method∈0:list.§ts → -`
   in `TestDependent` (`Iterable.iterator`), `TestFunction`/`TestSupplier` (`Optional`) —
   residual field-of-rep / FI-lift reconstruct gaps in the functional-interface path.
3. **Varargs `∩` fan-out (`TestVarargs`×4).** `0:target.§is∩1:collections.§iss`
   consistently dropped.
4. **Map coarsening (`TestMap` test1b).** `§vs∋value` expected, `§vs∩…` got — residual
   `∋`→`∩` coarsening in the Map hidden content.
5. **Stream / bound-type-parameter HC (`TestStream`×6, `TestBoundTypeParameter`×5).**
   All-structural clusters; focused pass.
6. **3 engine unit tests** — check against the engine directly, separate from linking.

## Suggested attack order

0. ~~Order-stability~~ **done** (`forkEvery = 1`; see the instability note above).
1. `acceptForLinkedVariables` (5 crashes, one fix).
2. Regenerate the 60 assignment/identity re-baselines per class (mechanical, read-to-
   confirm) — drops the visible count ~40%.
3. Return-summary-lost cluster (FI/Optional/iterator).
4. Varargs `∩` and Map `∋` coarsening.
5. Stream / bound-type-parameter structural pass.

## How this snapshot was produced

`./gradlew :maddi-modification-link:test -PskipCloneBench`, then parse
`build/test-results/test/*.xml`: bucket each `<testcase>` with a `<failure>`/`<error>`
by (a) thrown vs assertEquals, (b) for assertEquals, the multiset diff of nature symbols
between `expected:` and `but was:` (only `←/→/≡` differ ⇒ re-baseline; else structural;
empty RHS ⇒ empty).
