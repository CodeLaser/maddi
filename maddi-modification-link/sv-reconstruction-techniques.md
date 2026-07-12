# sv reconstruction: the techniques catalogue

How field-precise, direction-correct links are **reconstructed at extraction time** after the
shared-variable collapse has folded them into equivalence groups. Companion to
`sv-engine-handoff.md` (history) and `sv-remaining-catalogue.md` (open items); this document is
the *design reference*: one section per technique, each with the triggering example, the
mechanism, and — most importantly — the guards, because **every guard below traces to a specific
observed failure**. When extending the machinery, violate a guard only with a full-suite A/B.

## 0. The problem in one picture

The collapse (`Graph.mergeEdgeBi`, `IS_ASSIGNED_FROM` branch) replaces whole-object assignment
webs by a single representative:

```
return zs;   zs = in.subList(..)          collapse           extraction must reconstruct
──────────────────────────────────     ─────────────         ──────────────────────────────
return ← zs                             {return, zs}  →  $__sv_zs      method ← 0:in.§-view
zs.§m ≡ in.§m (view!)                   graph keyed on $__sv_zs        method.§m ≡ 0:in.§m
zs.§$s ⊆ in.§$s                                                        method.§$s ⊆ 0:in.§$s
```

Everything the group *hides* must come back out, keyed on **real, visible** variables (returns,
parameters, fields — locals are filtered from summaries), with the right **direction** and
**field precision**. The techniques below are the pieces of that inverse function.

Three data sources feed reconstruction:

| store | contents | key API |
|---|---|---|
| `SharedVariables` / `SharedVariable` | whole-object assignment groups; each recorded `Assignment(from, to, statementIndex)` | `allShared`, `translateForward`, `assignmentEdgeStream`, `assignmentSources`, `isPureAssignmentSource` |
| `VirtualModificationIdenticals` (VMI) | §m-equivalence groups (`≡` / pass-marked `☷`), `Group(linkNature, members)` | `variablesPartOf`, `groupsOf`, `equivalentStream` |
| the engine graph + closure | everything else, keyed on reps after collapse | `closureStream`, `expandRepToMembers`, `rehome` |

## 1. Direction is the master key

Almost every technique hinges on one question: **which member of a group owns a piece of
knowledge attached to the rep?** The group's recorded `Assignment(from, to, statementIndex)`
list answers it. Terminology used throughout:

- a member is a **recipient** if it appears as `from` of an assignment (it was assigned a value);
- a member is a **pure source** if it only ever appears as `to` (`SharedVariables.isPureAssignmentSource`);
- **assignment sources of m** = members transitively reachable from `m` along `from → to`
  (`SharedVariables.assignmentSources`).

The transfer rules:

1. **A rep's incoming edges belong to recipients.** `X x = optional.orElseGet(() -> alternative)`
   collapses `{x, alternative}`; the rep carries `rep ← optional.§x`. That edge is *x's* (and
   flows on to whatever x flows into) — attributing it to the pure source `alternative` produced
   the spurious `0:optional.§x → 1:alternative`. Enforced in two places:
   - rehoming a rep's links onto a member (`WriteLinksAndModification.doVariableReturnRecompute`,
     the `pureSource` check): a pure source drops the rep's incoming `←` links;
   - `iterateOverShared` expansion (`expandShared`): the recipient side of an assignment edge
     expands to recipient members only.
2. **A source's knowledge transfers to its recipients** — never the reverse. `return zs` with
   `zs.§m ≡ 0:in.§m` (subList = view): the return inherits the §m equivalence
   (`Graph.virtualModificationEdgeStream` + `SharedVariables.assignmentSources`), because the
   return *is* zs. But `alternative` must not inherit `x`'s `← optional.§x` (rule 1): only the
   downstream direction is identity-preserving.
3. **Multi-valued assignment is not reassignment.** `m = cond ? a : b` records `m ← a` and
   `m ← b` at the *same* statement index — both sources stay in one group. Only an assignment at
   a *different* index evicts the old group membership
   (`SharedVariables.isReassignment(from, statementIndex)`). Without the index, the second arm
   evicted the first and `m ← 0:a` vanished (ternary/switch-guard family).

## 2. Intra-group assignment reconstruction — `SharedVariables.assignmentEdgeStream`

The collapse stores `field ← param` (record constructors), `return ← local` etc. once, on the
group. At extraction, hand back directed links keyed on the member that is part of the primary.

- **Transitive chains through fillers.** The recorded assignments form chains broken by
  variables that are filtered from output. `reachable(m, fwd/bwd, deep)` walks them, recursing
  through:
  - pass-through intermediates (`$__rv…`) — always;
  - bare scalar **locals** — only when reconstructing the **whole return** (`deep`), so
    `return ← ttt ← tt ← 0:t` collapses to `method ← 0:t`.
  Guards: never chain through array elements (`DependentVariable`) or field faces — that linked
  mismatched dimensions and crashed on `grid[0][0]`/varargs. Never deep-chain for field or
  parameter endpoints — that manufactured the spurious primary-level shortcut
  `makeFromGet ≈ 0:box`. For a plain local `m` (per-statement view) stay shallow, preserving the
  collapse's dedup (`ttt ← tt`, not the transitive `ttt ← 0:t`).
- **Sibling faces** (`faceKeyed`): the primary's faces are itself plus its whole-object group
  siblings. `Pair p = new Pair<>(x, m); return p;` — the field assignments live on the sibling
  (`p.f ← 0:x`) and are rehomed onto the primary's face (`create2.f ← 0:x`). The whole sibling
  itself is skipped (its whole-object edge is already emitted from the primary's own member;
  rehoming it would self-link).
- **Field-level mirrors** (`Graph.sharedAssignmentEdgeStream`): a reconstructed whole-object
  intra-group link (`combine ← target`) also projects the rep's field vertices onto both
  endpoints (`combine.§is ← target.§is`) — the collapse hides the `←` edge from the engine, so
  the field projections that the old engine's sub-propagation derived never arise on their own.
- **§m companions** (fold in `WriteLinksAndModification`): a real assignment edge gets its §m
  modification-equivalence generated in `FollowGraph`; a reconstructed intra-group edge bypasses
  that, so the fold adds it (`this.list ← 0:l` yields `0:l.§m ≡ this.list.§m`), with the same
  return/virtual guards as `FollowGraph`.

## 3. Rep expansion at extraction — `FollowGraph` + `Graph.expandRepToMembers` / `rehome`

The graph is keyed on reps; the output is keyed on members.

- A vertex contributes to a primary's extraction when it, or one of its **rep expansions**
  (a rep as the whole vertex `$__sv_list1`, or nested in a scope, `$__sv_list1.§$s` →
  `a.list1.§$s`), is part of the primary. `FromPair(queryFrom, emitFrom)`: read the closure of
  the graph vertex, emit keyed on the member form.
- When the primary is itself a rep (extraction of a collapsed return), its group members are
  **faces** of the same value; a vertex part of a face is rehomed face→primary
  (`Graph.rehome` builds the substituted scope chain).
- Guard: `!e.equals(v)` keeps ordinary vertices on the fast path — `expandRepToMembers` rebuilds
  equal-but-distinct `FieldReference`s which must not replace the original.

## 4. Expansion-artifact filters — `WriteLinksAndModification`

Expanding a rep to all members can surface links the rep masked. Two filters, **both scoped to
what they were built for**:

- `isInternalSelfFieldLink`: `choose ≻ $__sv_s1` expands to the internal `choose ≻ choose.s1`,
  which FollowGraph's internal-reference filter would have dropped.
- `isInvalidFieldContainment`: a `field ← param` collapse groups the field with the value
  source, so `wrap ≻ $__sv_v` expands to both `wrap ≻ wrap.v` (valid) and `wrap ≻ 0:y`
  (invalid — 0:y is a value source, not a part of wrap). **Only a REAL field-side invalidates**:
  a virtual field-side is *content*, and the owner≻content spine legitimately derives
  cross-variable content containment (`entry.§xy.§x ≺ 0:optional`) which is expected output.
  Making this filter fire on virtual sides killed the whole Supplier `≺`-family.
- `dedupReversePairs` (every variable's builder, after assembly): the multiple assembly paths can
  contribute exact duplicates and both directions of one pair
  (`b.variables[0] ∈ b.variables` alongside `b.variables ∋ b.variables[0]`) — FollowGraph's
  reverse-block only sees its own emissions. Keep-rule for a reverse-pair: the link keyed on the
  structurally **deeper** from-side wins (parts-first, FollowGraph's fromList convention): `∈`
  keyed on the element beats `∋` keyed on the container; `s.r.j → s.k` beats `s.k ← s.r.j`.
  Implementation note: `Link` is a record with value equality — track removals by INDEX, or one
  removal sweeps all equal duplicates. Survivors keep insertion order (output order is asserted).
- `suppressRedundantScopeUps` (returns only): a coarse scope-up (`copy ≈ 0:pair`) is redundant
  once the finer link (`copy.f ← 0:pair.f`) exists — but the finer links arrive from a
  *different* builder (the reconstruct), which FollowGraph's own redundancy block never sees.
  Re-applies the `redundantFromUp/ToUp/Up` rule across the fully assembled return builder.

## 5. Cross-variable transitive redundancy — `linkgraph/RedundantLinks`

Ported from the pre-sv engine (it had not survived the big-bang port; its absence was the whole
SPUR[⊆] band). Per statement, per nature-group, accumulate `from → {to}` guards over the
per-variable extraction loop; a link whose target is transitively reachable through
already-emitted same-group links is dropped: **keep the nearest hop**
(`stream1.§xs ⊆ stream.§xs` stays; the transitive `stream1.§xs ⊆ 0:in.§xs` goes, because
`stream.§xs ⊆ 0:in.§xs` was already emitted). Skip rules: returns stay complete; in the *last*
statement parameters stay complete (the method summary reads them there). Env gate `NORL`.

Known second-order effect: pruning a builder can change the modification verdict downstream
(one documented case: `generic factory method` gains a spurious `≈`).

## 6. Structural prerequisites in the engine (not reconstruction, but load-bearing for it)

- **The owner≻own-virtual-field spine**: every variable is linked `≻` to its own virtual fields
  (`collection ≻ collection.§is`) — `invalidEdge` allows real↔virtual `≺/≻` only for this
  owner/own-field pair (`fieldScopeRoot` match). The varargs `∩` fan-out and the whole `≺`-family
  derive through it. It was severed by a graph-size reduction; nothing at extraction level can
  compensate for its absence (three failed attempts documented in the catalogue).
- **Collapse re-keying translates BOTH endpoints** (`transformToSharedVariable`): a member↔member
  edge like the spine must become `$__sv ≻ $__sv.§is`; the untranslated to-side resurrected
  removed vertices and the half-translated edge was dropped, order-dependently.
- **Witness-orphan materialization** (`removeVertices`): a closure fact between two survivors
  whose witness support touches a removed vertex is promoted to a raw edge (with the strongest
  symmetric-coherent label per direction) — otherwise fact survival depended on *which* witness
  the derivation happened to record. Scope-end (`clear`) also removes a variable's
  scope-descendant vertices so `v.§f` orphans do not linger.
- **Deterministic witness choice** (`WitnessIndex.putIfBetter` canonical tie-break; sorted
  support print; direct witnesses registered even for already-derived facts). Pinned by
  `TestEngineDeterminism`: closure facts+labels are insertion-order-independent; witness choice
  is canonical among offered candidates.

## 6b. The cost boundary — `Options.objectGraphLinks`

The spine makes the coarse object-graph web (`∩ ≤ ≥`) derivable across deep recursive
structures — quadratically (TestParSeqLinkBench: 48.7s vs ~0.7s). Linking's three
applications (modification propagation: `≻ ≈ ∋ →` + §m; same-type/VL2O: reachability over
direct links; new-object tracking: assignments) consume none of those natures — they are
full-fidelity OUTPUT for the tests. `Options.objectGraphLinks` (TEST=true, PRODUCTION=false)
excludes the three labels from the closure via the engine's valid-predicate; the direct spine
edges (which modification's `≻` check does use) stay. **Run TestParSeqLinkBench after every
engine-level change** — it found two production crashes and this cost cliff in one session.

## 7. Open shapes, in this vocabulary

- **Fluent setter** (`setI.i ← this*.i`, ~9 tests): `return this` groups `{return, this}`; the
  whole-object link reconstructs, but the field mirror onto the `this*` face does not — `this`
  is excluded from several face paths (`FollowGraph` fromList treats `This` specially). Likely a
  §2-style mirror with a `this`-face allowance.
- **Statement-scoped faces** (`l1.§m ≡ method.§m` on a *local's* summary, ~4 tests): the TO-side
  of a link whose target collapsed into the return should display the return face — but naive
  global rehoming leaks *later*-statement knowledge into *earlier* per-statement views (stmt-1
  `l2` gained `≡ l3.§m` from stmt-3's collapse). A face substitution is only valid in views
  at/after the collapse statement; the reconstruction needs the statement index (which
  `Assignment` already carries).
- **Documented one-offs**: `TestSupplier.test7` (one link, `entry.§xy ≺ 0:optional`, arrives via
  an unidentified post-extraction path at baseline; fails in isolation even at baseline — use
  class runs), `TestSupplierSpec Stream.generate`, `generic factory ≈` (see §5).

## 8. Working rules (hard-earned)

- **Bisect with class runs**, never single-test runs: several tests are context-sensitive in
  isolation (`test7`, `test3`, `generic factory`).
- **Trust only full-suite A/B** for attribution; env gates exist for every major mechanism
  (`NOSPINE NOMAT NOBOTH NOMIRROR NOPASSFIX NODESC NORL NOSV`).
- **Beware stale XMLs**: a silently failing gradle run leaves the previous run's test results in
  place; check timestamps when a result contradicts expectations.
- **Order-only diffs are re-baselineable** (multiset-identical link sets); everything else needs
  a root cause first. The ordering artifact stems from §m/reconstructed links being appended
  after their rank-sorted position.
- The old engine on the `openjdk` branch is runnable ground truth: worktree + a dump probe
  answers "what did the old graph actually contain" faster than archaeology.
