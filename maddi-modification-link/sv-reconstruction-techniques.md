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
  variables that are filtered from output. `reachable(m, fwd/bwd, deep)` walks them. `deep` is
  keyed on `Util.primary(emitM)`: TRUE for **return variables and their faces** (`justJ.j`), and
  for **parameters** (gate `NOPDEEP`) — both are summary endpoints. Chain-through nodes:
  - pass-through intermediates (`$__rv…`) — always;
  - bare scalar **locals** — under `deep` (`return ← ttt ← tt ← 0:t` → `method ← 0:t`;
    `0:in → v → this.f` → the parameter summary's `0:in → this.f`);
  - the **field face of a dying local** (`b.j` of a builder local) — under `deep`; it does not
    survive into the summary either (`justJ.j ← b.j ← 0:jp` → `justJ.j ← 0:jp`). Plain
    `FieldReference` on a real local only;
  - a **foreign method's return face** — under `deep`; the SAM's `get` rv in an anonymous-class
    capture is a value pass-through (`m ← get ← 0:x` → `m ← 0:x`). The primary's own rv is never
    a chain node in its own extraction (it is the emitM).
  Guards: never chain through array elements (`DependentVariable`) — dimension mismatch (crashed
  on `grid[0][0]`/varargs); never through parameter/`this` faces — they are summary endpoints
  themselves (the `makeFromGet ≈ 0:box` shortcut came from bridging those). For a plain local
  `m` (per-statement view) stay shallow, preserving the collapse's dedup (`ttt ← tt`, not the
  transitive `ttt ← 0:t`).
- **Element-face source aliasing** (fwd emit loop): a to-side ELEMENT face aliases onto its
  base's assignment **sources** — `m ← r[0]` with the for-each row `r ← g[0]` equally holds as
  `m ← 0:g[0][0]` (the local face dies at summary time; the parameter face survives). Sources
  only: a recipient sibling of the base may be reassigned later.
- **Sibling recipients** (gate `NOSIBR`, `deep` only): the summary endpoint and another face
  that both RECEIVED the same source's value are related — `method ← y` + `this.ys[1] ← y` ⟹
  `method → this.ys[1]` (the returned value was also stored in the slot). One hop; dying
  siblings filter downstream.
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
- **Derived faces across construction chains** (`derivedFaceKeyed`, gate `NODF`): the
  jfocus-transform shape. `ldIn = new Builder().set(0,col).set(1,matrix)….build()` decomposes
  FIELD-wise (`ldIn.variables ← $__rv137.variables`; there is never a whole-object
  `ldIn ← $__rv137` edge), so the primary `ldIn` is in **no** whole-object group and `faceKeyed`
  finds nothing. But its field `ldIn.variables` is grouped with the build-result face
  (`$__rv137.variables`), whose scope root `$__rv137` is whole-object-grouped with the entire
  fluent chain (`$__c122 … $__rv135`). A slot member recorded on a chain sibling
  (`$__rv124.variables[1]`, grouped with `matrix` by `set(1, matrix)`) denotes the same slot as
  the primary's element: rehome it (`ldIn.variables[1] ← matrix`). Only the **source** direction
  transfers (`pf ← s`, via `assignmentSources`), mirroring the memberFieldsOf rule. Two later
  refinements: (a) a WHOLE-OBJECT source's own faces rehome directly onto pf
  (`withException.exit ← $__c_a` + `$__c_a.exception ← 0:e` → `withException.exit.exception ←
  0:e` — the original skip only held when the primary itself was in the whole-object group);
  (b) the group member may BE the sibling face itself (fluent chain `setJ(jp).setK(kp)`:
  m = `$__rv9.j`); the emit loop's self-link guard suffices. Derived slot faces also get their
  ∈/∋ containment companions at the derivation site (`td.variables[0] ∈ td.variables`,
  `td.variables ∋ someSet`) — scoped to DERIVED faces only; emitting them for every
  reconstructed DV assignment regressed 14 tests.
- **Derived modification expansion** (`SharedVariables.derivedShared`, consumed in
  `WriteLinksAndModification.go`): the inverse map, for the modification cascade. A
  functional-interface call (`Loop.run(ldIn)` resolving `X::UpperTriangleLoopBody`) marks
  `ldIn.variables[1]` modified — a variable that never existed as a graph vertex, so
  `allShared` can't expand it. Rehome the key through the same composition onto the chain
  sibling faces and mark THEIR groups' members ({`matrix`, `0:ld.variables[1]`}) modified: same
  runtime slot, same modification.

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
- **Derived from-pairs** (gate `NODFP`): an expanded rep member (`rv6.bodyThrowingFunction`, a
  face of the collapsed builder chain) may not be part of the primary while its RECIPIENT
  group-sibling (`td.throwingFunction`) is. The chain rep's field face
  (`$__sv_$__rv6.bodyThrowingFunction`) then carries knowledge the collapse stranded there (the
  `←Λ$_fi` method-reference edge): read THAT vertex's closure, emit keyed on the recipient face.
  Only source→recipient transfers (`assignmentSources` check).
- **Reverse return-targeted facts** (gate `NORVREV`): engine feature #9 never composes facts
  TARGETING a return variable, so `return run ↖ 1:r.function` exists keyed on the return vertex
  only. For a non-return primary, read the return vertices' closures and emit the reverse
  (`1:r.function ↗ run`) keyed on the primary's face.
- **Canonical ordering** (determinism): the parts-first fromList comparator is a PARTIAL,
  intransitive order — TimSort's result depends on input order, which comes from unordered
  `graph.variables()`. A total canonical pre-sort (emitFrom FQN, then queryFrom FQN) before the
  semantic sort makes the emission order — and hence the block-set redundancy suppression —
  input-order-independent. `reverseReturnFacts` are sorted before appending for the same reason.

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
  Second exemption: a RETURN-VARIABLE from-side is value-level by nature (a return is never
  structurally a field of anything) — `m ≺ 0:r` from a record-pattern binding
  (`if (r instanceof R(X xx)) return xx;`) is expected output. A group-aware relaxation for
  OTHER from-sides was tried and reverted (admits the accessor-copy `o ≺ 0:r`, misses the
  pattern `set ≺ 0:i` — the pattern/copy distinction is not visible at this filter).
  §m-companion scope (the fold): whole-return endpoints get no §m companion (FollowGraph's
  convention), but a return's FIELD FACE does — `withException.exit.exception.§m ≡ 0:e.§m` is
  what call-site modification propagation consumes.
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
- `returnSideModificationCompanions` (returns only, gate `NORVM`): whole-return §m companions
  from content-flow links, run AFTER `handleReturnVariable` on the assembled builder
  (consumption-aware: returns never enter this method's own modification branch, so these are
  output/summary-only). Two shapes: `method.§X ⊆ S.§Y` ⟹ `method.§m ← S.§m` (content from S's
  content: S-modifications reach the returned value); double-∩ `method ∩ Y.§X` AND
  `method.§X' ∩ Y` ⟹ `method.§m ≡ Y.§m` (each sits in the other's content web — they may be
  the same object cluster). Any existing §m link between the pair subsumes the companion (an
  assignment's ≡ beats the derived ←). Emits semantically-true extras on stream/collector
  summaries the old engine never derived — re-pinned, analyzer-suite-verified.

### 2b. One slot, one group — sibling-spelling group resolution (gate `NOSIBFACE`)

`SharedVariables.groupOfWithSiblingSpelling`: a field face with no direct group, whose scope
belongs to a whole-object group, is looked up under each sibling spelling ('b.j' →
'$__c0.j'/'$__rv2.j' for the chain group {$__c0, $__rv2, $__rv6, b}); on a hit the face JOINS
the found group. Prevents the builder-chain two-groups-per-slot disease (the setter statement
groups the slot under construction-time spellings; build()'s summary respells it through the
chain-tail local — a disconnected parallel group strands the slot's ce-constants/Λ$_fi
knowledge). Two invariants of the join: the triggering assignment IS recorded even though both
sides now resolve to one group (reachable() walks the records; without it the chain to the
group's true sources breaks at the joined spelling), and the group is RETURNED so mergeEdgeBi
re-keys both sides' graph vertices (the 'same group' null path asserts no vertices remain).
Overlap note: derivedFaceKeyed/derivedFromPairs (§2/§3, NODF/NODFP) special-case the same
disconnection at extraction time; candidates for simplification now that the group layer fixes
it at formation (A/B with the gates before touching).

### 4b. The ⊇→~ rewrite fires ONCE, at the modification statement (gate `NOFLIPSAME`)

The old engine re-flipped ⊆/⊇→~ at every statement for `previouslyModified` variables; harmless
there because its per-statement graph rebuild re-derived the containment through unflipped
sibling builders (self-healing). In the sv engine the rewrite mutates the ONE persistent graph
(`replaceReturnAffected` descends the witness DAG to raw edges) — re-flipping permanently
destroys containment the modification never invalidated (test4b: `1:rr.§$s ⊇ 0:in.§$s`,
established BY `rr.embed(in)` itself, died at the next statement). Two rules:
1. collect flips only for `modifiedInEval` (modification in THIS statement's evaluation, direct
   or linked), never for `previouslyModified` carry-over — the persistent graph already
   remembers the earlier rewrite;
2. `replaceReturnAffected` skips raw edges whose `DirectWitness.statementIndex` equals the
   current statement — same-statement edges are the modifying call's POST-STATE.

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
- **Symmetric completion** (`completeSymmetrically`): the closure's two directions DERIVE
  independently, so whether a derived fact's mirror existed depended on whether the insertion
  order enabled its own derivation path (`copy ∩ list` derivable in one edge order only — the
  m∩copy per-JVM flake, reproduced in-JVM by the 24-permutation `twoLevelComposition` pin).
  Every added composite fact now immediately derives its mirror, with the naturally-oriented
  witness (`rev(f∘g) = rev(g)∘rev(f)`; the reversed sub-facts exist because edges and mirrors
  are themselves symmetric) so witness choice stays canonical (the diamond pin holds).
  `acceptForComposite` guards feature #9: no composites TARGETING a return variable — or a
  someValue marker.
- **someValue markers ($_v) are opaque**: no §-content/sub mirrors (`MakeGraph`), no §m
  equivalences (`FollowGraph`), no composite facts targeting them (`acceptForComposite`, gate
  `NOACM`). And they must NEVER be graph vertices: a shared `$_v` vertex is a closure HUB
  (bench 0.9s → 3-19s, nondeterministic). The fresh-object-return fact
  (`return new URL(...)`, reduced away by `LinkGraph.reduceLinks`) travels SIDE-BAND instead:
  `Graph.markFreshObjectReturn` → `handleReturnVariable.isFreshObjectReturn` inserts the
  `← $_v` marker at write-out only. Related: `ExpressionVisitor`'s no-data-constructor fallback
  returns the fresh-object intermediate for EXTERNAL types only — fabricating intermediates on
  recursion-prevention nulls floods deeply recursive code.
- **Both-sides re-key on collapse** (`mergeEdgeBi`): when BOTH endpoints of an `a ← b` collapse
  have graph vertices, re-key both (to-side recomputed after the from-pass). Removing just the
  bare to-vertex left its §-descendants orphaned (assert crash on the next edge of the pair) and
  silently deleted the to-side knowledge.
- **Reassignment gates** (gate `NORSRC`): (a) a group member that participated only as a SOURCE
  (`method ← 1:num`) and is later assigned (`num = amb`) must not alias the new value into the
  group — keep the edge as a plain rep edge; (b) a ReturnVariable is exempt from the
  reassignment removal: a second `return` is a path merge, not a reassignment (removing it
  abandons the group rep carrying the first path's knowledge). Open: clear-on-reassign still
  kills other-keyed old-value edges (loop-carried provenance, `method ← 0:amb`).
- **Insertion-ordered maps everywhere first-match decides** (`SharedVariables` uses
  LinkedHashMap): JDK unordered sets/maps iterate in per-JVM salted order.

## 6b. The cost boundary — `Options.objectGraphLinks`

The spine makes the coarse object-graph web (`∩ ≤ ≥`) derivable across deep recursive
structures — quadratically (TestParSeqLinkBench: 48.7s vs ~0.7s). Linking's applications
(modification propagation: `≻ ≈ ∋ →` + §m; same-type/VL2O: reachability over
direct links; new-object tracking: assignments) consume none of those natures — they are
full-fidelity OUTPUT for the tests. `Options.objectGraphLinks` (TEST=true, PRODUCTION=false)
excludes the three labels from the closure via the engine's valid-predicate; the direct spine
edges (which modification's `≻` check does use) stay. **Run TestParSeqLinkBench after every
engine-level change** — it found two production crashes and this cost cliff in one session.

The **fourth application** confirms the boundary: jfocus-transform (loop/try-catch →
simple-statement rewriting; `~/git/jfocus-transform`, `Loop.java`/`Try.java` in
`codelaser-transform-support`) packs locals into `Object[] variables` slot arrays behind
`@GetSet("variables")` accessors, with the body as a functional interface. Its correctness
criterion — modification identical before/after transformation — consumes only
`← → ∈ ∋ ≡ ~ ≺ ↗ Λ` + §m; the `∩` cross-slot web appears solely in commented-out assertions.
Transformed code is wall-to-wall nested slot arrays, the worst case for the `∩` web, so the
production cut is *required* by this consumer, not merely compatible.
`maddi-modification-analyzer TestModificationLoopTransform` ports the UpperTriangle cascade
(literal `Loop`/`Try` inlined) as the maddi-side guard; the jfocus-transform original
(`codelaser-transform-loops TestModification`) is stale on lambda numbering and pinned to
`../maddi-kotlin`.

## 6c. Element types of DependentVariables under downcast slots

A slot access is statically `Object`-typed (`ld.variables[1]`) while its group carries the real
type (`float[][] matrix`). Two places recomputed a DV's element type via
`copyWithOneFewerArrays` on the (possibly non-array) base and asserted: `expandRepToMembers`
(now passes `dv.parameterizedType()` through) and `VariableTranslationMap` (falls back to the
original DV's element type when the translated array has no array dimension).

## 7. Open shapes, in this vocabulary

> The authoritative, current list lives at the TOP of `sv-remaining-catalogue.md` (15 link
> tests at the time of writing). The shapes below are the ones with a sketched mechanism:

- **Directional §m from content flow** (test4b/4c): the old engine generated DIRECTIONAL §m
  links from content-flow links, not just the ≡ companions of assignments — `0:in → r.§t`
  (param stored into a virtual field) ⟹ `r.§m → 0:in.§m`; `method.§$s ⊆ 1:rr.§$s` ⟹
  `method.§m ← 1:rr.§m`. Needs a §m-companion rule keyed on content natures with direction
  following the content flow.
- **ce-constants on derived record faces** (`method.i ← $_ce1`, `method.list.§$s ∋ $_ce3`):
  constant-marker links do not survive onto derivedFaceKeyed-style faces (markers are excluded
  from groups, so their knowledge is graph-edge-only and rep-keyed).
- **Varargs pair**, record-pattern ≡ residue, and singles: see the catalogue.
- **Loop-carried old-value provenance**: clear-on-reassign removes the vertex and with it
  other-keyed edges about the old value (`method ← 0:amb`); the old engine kept those.
- **DV index-spelling drift**: `tmp[1][1]` where the old engine kept symbolic `tmp[u][v]`
  (loop variables) — suspicious constant-folding in DV spelling; and per-row `~` coarsened
  to whole-object with `∈?/∋?` variants.
- Solved since first written (kept for the record): the fluent-setter band (§2 mirrors +
  derived faces), statement-scoped §m faces (derivedShared side), `generic factory ≈`.

## 8. Working rules (hard-earned)

- **Bisect with class runs**, never single-test runs: several tests are context-sensitive in
  isolation (`test7`, `test3`, `generic factory`).
- **Trust only full-suite A/B** for attribution; env gates exist for every major mechanism
  (`NOSPINE NOMAT NOBOTH NOMIRROR NOPASSFIX NODESC NORL NOSV NODF NODFP NORVREV NORSRC NOACM
  NOPDEEP NOSIBR NOVMIDIR NOFLIPSAME NORVM NOSIBFACE NOSIBEQ NOFLIPOWN NORVEQ NORVSP NOVARTO NORVSUB NOSPINEI`). Debug aids: `SVDUMP` (per-statement group dump), `SBDUMP=<simpleName>`
  (closure with witnesses for a primary), `TRACEVAR=<substr>` (mergeEdgeBi + re-key trace),
  `RVTRACE` (return-variable builders b1/b2/b at write-out).
- **Beware stale XMLs**: a silently failing gradle run leaves the previous run's test results in
  place; check timestamps when a result contradicts expectations.
- **Always `--rerun` when toggling env gates**: gradle does not treat env vars as test-task
  inputs — without `--rerun`, a `VMIDIR=1` run happily re-reports the previous run's results.
  This manufactured the phantom 'class-vs-full-suite context divergence' (a gate-on run looked
  green because it never ran).
- **Order-only diffs are re-baselineable** (multiset-identical link sets); everything else needs
  a root cause first. The ordering artifact stems from §m/reconstructed links being appended
  after their rank-sorted position.
- The old engine on the `openjdk` branch is runnable ground truth: worktree + a dump probe
  answers "what did the old graph actually contain" faster than archaeology.
