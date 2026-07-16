# sv engine — catalogue of the remaining link failures

> Design reference for HOW links are reconstructed after the collapse (techniques, guards,
> direction rules, open shapes): **`sv-reconstruction-techniques.md`** — read it before
> extending the reconstruction machinery.

## UPDATE — intermediate virtual spine (gate NOSPINEI); test7 green; link 3 → 2

TestSupplier test7's 'entry.§xy ≺ 0:optional': the deep faces (entry.§xy.§x/§y) had all their
facts, the MID-LEVEL face had none — because `addField` adds a single 'root ≻ deep-face' hop
and 'entry.§xy' NEVER MATERIALIZED as a vertex. Fix: chain the spine through intermediate
VIRTUAL faces ('entry ≻ entry.§xy ≻ entry.§xy.§x'); the direct fact still derives by ≻∘≻.
First attempt applied to REAL field chains too ('0:s.r.i ≺ 0:s.r' extras on 6 tests) —
restricted to virtual-only. Fallout: ~7 tests of true-but-unconsumed decorations re-pinned
(∩/≤/≥ web facts from mid-face bridging: '0:x∩1:y' when two params live in one structure's
content web; 'map.§vks≥entry.§kv'; TestConsumers' ≤ refined to ≺). Benches NEUTRAL
(parseq 1600ms — the production cut excludes the bridged ∩/≤/≥ anyway).

## UPDATE — Stream.generate FIXED (gate NORVSUB); link 4 → 3

The whole chain was already correct: Stream.generate's shallow summary carries
'generate.§ts ⊆ Λ0:s' (the type-parameter Supplier rule), the call site translates it to
'$__rv.§xs ⊆ 0:alt' via LinkFunctionalInterface's supplier branch — and then
`Graph.invalidEdge` DROPPED it: real↔virtual edges allowed only ∈ ∋ ← → (the historic
Supplier/Optional fix). One nature further: ⊆/⊇ between a virtual content face and a real
value is legitimate ('all the stream's content IS repetitions of alt'); now allowed
(gate NORVSUB). Cost-neutral: parseq bench 1641ms, both suites zero regressions.

Debug aids added: LFITRACE=1 (LinkFunctionalInterface inputs), BTRACE now also prints
shallow summaries (ShallowMethodLinkComputer) and go()'s assembled mlv.

Remaining 3: TestLinkModificationArea ≻ (area precision), TestStream MR-swap (HC type-name
drift), TestSupplier test7 ≺ (context-sensitive even at old baseline).

## UPDATE — varargs fan-out FIXED (gate NOVARTO); TimSort contract crash fixed; link 5 → 4

The varargs residue was NOT the closure-algebra ∩ problem (that died with the spine): it was a
plain call-site translation gap. fillAll's summary carries the fan-out fact on the TARGET's
face with the varargs param as the TO side ('0:target.t ∈ 1:vs'), and linksBetweenParameters
translated '1:vs' to the single argument at the varargs' own index — 'box.t ∈ a' only, b never
seen. The existing fan-out loop only covered varargs on the FROM side. Fix in
`LinkMethodCall.linksBetweenParameters`: a link whose TARGET is the varargs parameter fans the
to-side out over every actual argument j=to.index()..n (same varargsLinkNature weakening).
'1:a*~2:b*' then derives in the closure from the two ∈ edges (∋∘∈=~), as pinned.

Also fixed en route: the parts-first refinement sort in `FollowGraph` — a documented PARTIAL,
intransitive comparator — made TimSort THROW ('Comparison method violates its general
contract') on the real parseq shapes once the new links changed the input permutation.
Replaced with a stable insertion sort (tolerates partial orders, deterministic on top of the
canonical pre-sort, byte-identical on both suites; fromList is per-primary and small).

A/B: link 4 (zero regressions), analyzer 122/122, deep-structure bench green, parseq bench
1631-1648ms ×2. Remaining 4: TestLinkModificationArea ≻, TestStream MR-swap (type-name drift),
TestSupplier test7 ≺, TestSupplierSpec Stream.generate ⊆.

## UPDATE — mutator-returning-object FIXED (gate NORVSP); link 6 → 5

The highest-consumer-value remaining test (all three linking goals). This session's earlier
mechanisms had already recovered the SUMMARY ('writeReturn←0:box*' + both .t links); the last
diff was x's param face ('1:x*→0:box*.t' vs '-'). Root: the slot group
{writeReturn.t, box.t, 1:x} reconstructs x's flow in the RETURN spelling ('x → writeReturn.t')
— the (box.t ← x) record was swallowed by isAssignedFrom's 'already in the same group' branch
— and filteredPi then strips return-links from params mentioned in the return value: the fact
existed but only in a spelling the summary must not print. Fix in assignmentEdgeStream: a
recipient that is a RETURN'S FIELD FACE also emits the slot's non-return sibling spellings
('x → 0:box.t'). FIELD FACES only — for the WHOLE return, group siblings are multi-source
could-be aliases (first attempt fired on switch arms yielding list1/list2/list3 and fabricated
'0:list3→this.list1'; the field-face guard kills that).

Also: fresh-object capture re-pinned earlier (order-only after the session's recoveries;
its content was fully back). Debug aid added: BTRACE=<substr> (final per-variable builder at
write-out + go()'s raw/filtered ofReturnValue).

A/B: link 5 (writeReturn green, zero regressions), analyzer 122/122, deep-structure bench
green, parseq bench 1652ms (baseline). Remaining 5: varargs fan-out (TestLinkMethodCall),
TestLinkModificationArea ≻, TestStream MR-swap (type-name drift), TestSupplier test7 ≺,
TestSupplierSpec Stream.generate ⊆.

## UPDATE — parseq bench RESTORED; production label-cut violation fixed; NEW BASELINE ~1.7s

The recaptured log (/tmp/jfocus-test-debug.log — **compileTestJava**, not compileJava:
TestParSeqElement lives in the TEST tree; class comment corrected) exposed a CRASH present at
least since f0e249e0: `MakeGraph` unconditionally emits the slice edge
'mapRight.§tts[-1] ≤ mapRight.§tts' (TestConsumers,2), and PRODUCTION
(Options.objectGraphLinks=false) excludes ≤ from the engine — assert in addSymmetricEdge.
Every recent "bench 746ms" record was made while the log was ABSENT (bench skipped via
assumeTrue, BUILD SUCCESSFUL misread as green) — the same stale-green trap as the phantom
context divergence. Fix: boundary filter in `Graph.mergeEdgeBi` (labels the engine's
valid-predicate rejects are dropped, not asserted); engine assert kept for closure internals.

**New baseline (current jfocus-stdbase @ cee706a, test log): LINK(production parseq types)
≈ 1.65–1.77s** (parse ~3.2s, prep ~0.4s). Not comparable to the old 746ms (input set unknown,
old log lost; the crash proves those numbers predate today's input). Session mechanisms are
NOT the cost: all-gates-off measured SLOWER (2.08s). Suites byte-identical under the filter
(link 7, analyzer 122/122, deep-structure bench green).

## UPDATE — return-face §m rehoming (gate NORVEQ); link 8 → 7

The old dead-end ('l1.§m≡method.§m on a LOCAL's summary — naive graph-side rehoming leaks
later-statement knowledge into earlier views') falls to the consumption-aware pattern: in
`doVariableReturnRecompute`, OUTPUT-ONLY after the modification decision, a builder link
'X.§m ≡ Y.§m' where Y whole-object-shares with a ReturnVariable also emits 'X.§m ≡ method.§m'.
Statement-scoping is free: the return group exists only from the return statement on, and
views are written forward (verified: the duplicate pin at vd1 stays clean while vd3 gains the
facts). TestRedundantModificationLinks green. A/B: link 7, analyzer 122/122, bench green.

Remaining 7 — the long-tail hard singles, all previously multi-attempt: varargs ~∈∋ fan-out +
mutator-→ (TestLinkMethodCall ×2), TestLinkModificationArea (≻ vs ←≺), TestStream MR-swap
(← vs ≡), TestSupplier test7 (≺; fails in isolation even at old baseline), TestSupplierSpec ×2
(Stream.generate ⊆ / fresh-object capture ≺).

## UPDATE — TestMap ⊆→~ root-caused: per-EDGE pass semantics in VMI; link 9 → 8

TestMap test2Reverse0's coarsening had TWO stacked root causes:
1. **VMI discarded the ☷ pass set on group join/merge** (`VirtualModificationIdenticals.add`
   kept the group's original nature): the iterator's pass-marked ≡ ('identical except via
   remove()') folded into the STRICT entrySet-view group {this.map.§m, entries.§m}, so next()
   counted as a strict modification of the map and the entries — firing the ⊇→~ flip on two
   UNMODIFIED variables. Fix: pass semantics are per-EDGE; a member now sits in SEVERAL groups,
   one per pass set (`memberToGroup: Variable → Set<Integer>`), matched on `pass()` at
   add/merge. VERDICT IMPROVEMENT exposed: the old engine marked 'this.map*' modified by mere
   ITERATION in the direct-iterator variant (reverse) but not in the local-variable variant
   (reverse0) — both now consistently unmodified (re-pinned here + TestWriteAnalysis2's codec
   dump; analyzer suite confirms no verdict damage).
2. **Ownership-restricted flip descent** (gate `NOFLIPOWN`): replaceReturnAffected descended a
   modified variable's DERIVED ⊆ to raw edges owned by OTHER, unmodified variables — a
   composite entailed by intact raw edges is not invalidated (logic: the closure fact follows
   from its support). Raw rewrite now requires the flip owner to touch the edge (primary match,
   or membership via expandRepToMembers). TestDependent/TestList2/TestConstructor (the flip's
   legitimate cases) unaffected.

Also: `iterateOverShared` gained the DependentVariable branch mirroring `expandRepToMembers` —
the '$__sv_map.§vks[-1]' rep leak in printed links (catalogue's open cosmetic) is fixed; a
TestStaticValuesRecord pin that had LEAKED '$__sv_variables[0]' baked in now dedups cleanly.
Debug aid added: `FLIPTRACE=1` prints flip owner + builder at collection time.

A/B: link 8 (zero regressions), analyzer 122/122, bench green.

## UPDATE — cast/pattern ≡ cluster CLEARED; link 12 → 9 (commit ff35e95a)

TestCast + TestInstanceOf + TestVariablesLinkedToObject green in BOTH modules. Four mechanisms:
1. **Co-recipient identity** (`SharedVariables.assignmentEdgeStream`, gate `NOSIBEQ`): two group
   members DIRECTLY assigned the same source hold the same object — 'ii=(II)o' + 'ii2=(II)o' ⟹
   'ii2 ≡ ii' (+§m via the fold). Per-statement views (the deep/summary counterpart is the
   sibling-recipient '→' rule); DIRECT records only — a transitive source does not guarantee
   identity; same-base DV spelling pairs excluded.
2. **Pattern-binding side-band** (`Graph.markPatternBinding`/`markPatternBindingAlias`, set in
   `ExpressionVisitor.instanceOf`): the long-standing "distinction must be made at the binding
   site" is now made there. `isInvalidFieldContainment` consults the side-band: deconstruction
   components ('0:i ≻ o') and their type-pattern cast aliases ('0:i ≻ set' for 'o instanceof
   Set set') survive; accessor-copy expansions still drop. Coarse old pins (0:i≈o, o∩0:i)
   refined to ≻/≺ — the old test even carried a "'o ≺ 0:i' is not visible" lament, now visible.
3. **VL2O union read** (`LinkComputerImpl.addToVariablesLinkedToObject`): the raw followGraph
   probe was collapse-blind (lost 'ii2 ← 0:o'); the stored links deliberately suppress slot
   aliases the probe surfaced (as leaked rep names, '$__sv_ii[j]' — the catalogue's open
   cosmetic). Read BOTH: stored links first (authoritative), then the probe with
   expandRepToMembers on each side ('ii[j]=false', rep-free).
4. **§m pair semantics decision** (`VirtualFieldComputer.addModificationFieldEquivalence`):
   worst = min(from, to) — was the documented copy-paste bug reading 'to' twice. The pair
   denotes ONE runtime object, so a §m pair exists when EITHER static type is mutable:
   'set.§m≡0:r.object.§m' through Object-typed slots, incl. the jfocus cascade's
   'matrix.§m≡0:ld.variables[1].§m'. Eager emission (old was modification-coupled) — extra-only.

A/B: link 9 (zero regressions), analyzer 122/122 (4 extra-only re-pins), bench green.

Remaining 9: varargs pair + mutator-→ (TestLinkMethodCall ×2), and singles
(TestLinkModificationArea ≻vs←≺, TestRedundantModificationLinks ≡ chain, TestMap ⊆vs~,
TestStream MR-swap, TestSupplier test7 ≺, TestSupplierSpec ×2 ⊆/≺).

## UPDATE — ONE SLOT, ONE GROUP (gate NOSIBFACE); simple-builder ce-constants fixed; link 13 → 12

TestStaticValuesRecord test5 root-caused: in a collapsed builder chain
({$__c0, $__rv2, $__rv6, b} whole-object-grouped), the setter calls group a slot under one
spelling ($__sv_j: {$__c0.j, $__rv2.j}, carrying '← $_ce1') while build()'s summary spells the
SAME slot through another sibling ('b.j') — the plain memberToGroup lookup misses, a PARALLEL
disconnected group forms ({b.j, r.i}), and the slot's knowledge (ce-constants, Λ$_fi markers)
never reaches the reader. Same disease as the ThrowingFunction rep-vs-face disconnection,
one level down.

Fix in `SharedVariables` (gate `NOSIBFACE=1` disables): `groupOfWithSiblingSpelling` — when a
field face has no direct group, try the same field spelled through each whole-object group
sibling of its scope; on a hit the face JOINS that group. TWO traps found en route:
1. the join can make sv1==sv2 in `isAssignedFrom`, whose 'already in the same group' branch
   recorded NO assignment — reachable() then broke at the joined spelling and the summary lost
   its true sources (method.set←0:in vanished; with only ce-links left, emptyIfOnlySomeValue
   zeroed the whole summary). The join case now records the assignment and returns the group
   (so mergeEdgeBi re-keys both sides' vertices; the sv==null path asserts none exist).
2. (observation) Λ$_fi numbering is a per-run counter — pins containing $_fi are stable across
   class/full contexts but shift when earlier parses are added to the same test.

Fallout, all precision GAINS, re-pinned: r.i←$_ce1 at statement level; r.function←Λ$_fi on the
indexed-objects builders; method←Λ$_fi15 / method2←Λ$_fi8 where the old engine had EMPTY
summaries (the returned r.function IS the stored String::length — a real recovered fact).
TestStaticValuesRecord 12/12 green.

A/B: link suite 13 → 12 with ZERO regressions outside the class; analyzer 122/122;
deep-structure bench green. Cleanup candidate: derivedFaceKeyed/derivedFromPairs (NODF/NODFP)
special-case the same disconnection at extraction time — with NOSIBFACE fixing it at
group-formation, they may be partially redundant (do a gate-off A/B before touching).

Remaining 12: varargs pair (TestLinkMethodCall ×2), record-pattern ≡ residue ×3 (TestCast,
TestInstanceOf, TestVariablesLinkedToObject), and singles (TestLinkModificationArea ≻vs←≺,
TestRedundantModificationLinks ≡ chain, TestMap ⊆vs~, TestStream MR-swap, TestSupplier test7 ≺,
TestSupplierSpec ×2).

## UPDATE — 'context divergence' DEBUNKED; ⊇→~ re-flip root-caused; VMIDIR default-on; link 15 → 13

The class-vs-full-suite context divergence **does not exist**. At `7de1d9a6` itself, isolated
single-test AND class runs show the test4b:357 '⊇→~ flip + r≥0:in.§$s' — identically, 3/3
stable. The previous session's 'clean class run' was a stale-result artifact: **gradle does
not treat env vars as test-task inputs, so toggling a gate (VMIDIR=1) without `--rerun`
re-reports the previous run's results**. PROCESS RULE: always pass `--rerun` when toggling
env gates.

The flip itself was real and PRE-EXISTED VMIDIR (visible with the gate off). Root cause: the
⊇→~ rewrite (`replaceReturnAffected` + recompute) permanently rewrites the PERSISTENT graph,
and it fired again at every later statement via `previouslyModified` — destroying containment
that the modification did not invalidate. In the old engine the re-flip was harmless: its
per-statement graph rebuild re-derived the ⊆/⊇ through unflipped sibling builders
(self-healing); in the sv engine the single graph makes it destructive AND unnecessary (the
rewrite at the modification statement already persists). Two-part fix (gate `NOFLIPSAME=1`
restores old behavior):
1. the flip collection fires only when the modification occurs in THIS evaluation
   (`modifiedInEval`), not on `previouslyModified` carry-over;
2. `replaceReturnAffected` skips raw edges inserted at the current statement — a ⊇
   established BY the modifying call itself (`1:rr.§$s ⊇ 0:in.§$s` from `rr.embed(in)`) is
   post-state, not invalidated knowledge.
A/B: link suite byte-identical (15), analyzer 122/122, deep-structure bench green. In test4b,
`method.§$s⊆1:rr*.§$s` now SURVIVES into the summary where the old engine had coarsened it —
re-pinned as a precision gain.

With the blocker gone, **VMIDIR is default-on** (disable: `NOVMIDIR=1`); test4b's vd0/vd1
match the old engine's §m-directional facts (order-only re-pin).

**Whole-return §m companions** (gate `NORVM`, `WriteLinksAndModification
.returnSideModificationCompanions`) — the second half of the §m-directional family:
1. `method.§X ⊆ S.§Y` (return content from S's content) ⟹ `method.§m ← S.§m` (test4c);
2. double-∩ `method ∩ Y.§X` AND `method.§X' ∩ Y` (the returned value and Y may be the same
   object cluster) ⟹ `method.§m ≡ Y.§m` (test4b).
Emitted on the assembled return builder AFTER handleReturnVariable — consumption-aware
(returns never enter this method's own modification branch); an existing §m link between the
same pair subsumes the companion. Fallout: ~19 extra-only `X.§m←Y.§m` facts on
stream/collector summaries (semantically true — the result's elements ARE the source's;
the old engine never derived them), re-pinned per the strengthened-modification precedent.
Analyzer suite confirms no verdict impact.

**test4b + test4c GREEN.** State: link 13, analyzer 122/122, deep-structure bench green.
(parseq bench not run: /tmp/jfocus-test-debug.log absent — recapture per TestParSeqLinkBench
class comment before the next engine-level round.)

Remaining 13: simple-builder ce-constants (TestStaticValuesRecord), varargs pair
(TestLinkMethodCall ×2), record-pattern ≡ residue ×3 (TestCast, TestInstanceOf,
TestVariablesLinkedToObject), and singles (TestLinkModificationArea ≻vs←≺,
TestRedundantModificationLinks ≡ chain, TestMap ⊆vs~, TestStream MR-swap, TestSupplier test7 ≺,
TestSupplierSpec ×2).

## (superseded) UPDATE — §m-directional inheritance BUILT, opt-in (VMIDIR=1); context-divergence is the blocker

Second iteration of the §m-directional family, consumption-aware this time:
`Graph.vmiDirectionalFacts` (strict-≡ VMI siblings' closures rehomed onto the owner's §m face)
consumed in `doVariableReturnRecompute` AFTER the modification decision and the ⊇→~ rewrite
collection — output-only, no verdict influence at decision time. In CLASS runs this is exactly
right (test4b full green, `r.§m → 0:in.§m` in place, verdicts intact). In FULL-SUITE runs
test4b:357 shows a ⊇→~ flip + `r≥0:in.§$s` that class runs do not — the same class-vs-full
CONTEXT DIVERGENCE seen at TestIndependentOfByteArray (directional flip). Mechanism is
committed OPT-IN via `VMIDIR=1`; enabling by default is blocked on root-causing the context
divergence (suspects: static state across test classes in one JVM — recursion prevention,
annotated-API caches; forkEvery configuration).

NEXT WORK ITEM (root cause, then flip the gate): reproduce the divergence minimally by running
TestStaticValuesRecord alone vs preceded by one other class; diff SVDUMP/modifiedInThisEvaluation
between contexts at test4b's last statement.

## UPDATE — §m-directional VMIFP experiment REVERTED; spelling-alias suppression; both suites clean

The VMI-sibling §m inheritance mechanism (read strict-≡ siblings' closures at extraction,
emit §m-to-§m facts keyed on the primary's face — `r.§m → 0:in.§m` for test4b) WORKED for the
target string but was REVERTED: §m facts emitted through the per-variable builders leak into
the modification machinery — the ⊇→~ after-modification rewrite fired, test4b's
'newly created cannot be modified' verdict was about to flip, and bench rose ~50%. LESSON: the
§m-directional family needs CONSUMPTION-AWARE emission (output-only, not builder-visible to
notLinkedToModified / the rewrite). Two API notes for the next attempt:
`VirtualModificationIdenticals.groupsOf` keys on the REAL variable (firstRealVariable), not the
§m face; VMI ≡ facts never reach the graph closure (mergeEdgeBi routes them out), which is WHY
the closure cannot compose them with graph §m edges.

Also fixed: sibling-recipient links between two index-SPELLINGS of one slot
(0:b[off++]/0:b[1:off]) are suppressed — the direction was context-dependent (class-run vs
full-suite) and carried no information. TestIndependentOfByteArray is CONTEXT-SENSITIVE:
re-pin only from full-suite XMLs. Process rule reinforced: A/B BOTH suites after every engine
round (the alias noise slipped through link-only A/Bs).

State: link 15, analyzer CloneBench-only, bench 746ms.

## UPDATE — sibling recipients; link 15; §m-DIRECTIONAL family identified

`assignmentEdgeStream` now emits SIBLING-RECIPIENT summary links (gate `NOSIBR`): a summary
endpoint and another face that both received the same source's value are related —
'method ← y' + 'this.ys[1] ← y' ⟹ 'method → this.ys[1]'. TestStaticValuesIndexing green
(+1 extra-only re-pin in TestArrayInitializer: add[1]→add[0] between same-source slots).

**New family identified — directional §m from content flow** (test4b/4c): the old engine
generated DIRECTIONAL §m links from content-flow links, not just the ≡ companions of
assignments: `0:in → r.§t` (param stored into virtual field) ⟹ `r.§m → 0:in.§m`;
`method.§$s ⊆ 1:rr.§$s` ⟹ `method.§m ← 1:rr.§m`. The current fold only produces
makeIdenticalTo ≡. Implementing means a §m-companion rule keyed on content natures
(→-into-§field, ⊆/⊇ of §-content) with direction following the content flow. 2 tests.

Remaining 15: the §m-directional pair, ce-constants on derived record faces (test5),
varargs pair, record-pattern ≡ residue ×3, and singles (modification-area, redundant-links
chain, map-reverse ⊆vs~, MR-swap ←vs≡, supplier ≺ ×2, Stream.generate ⊆).

## UPDATE — anonymous capture + array-loop shapes fixed; link 16, all decoded

Engine: (1) `canChainThrough` (deep) chains through a FOREIGN method's return face (the SAM's
'get' rv in an anonymous-class capture: m ← get ← 0:x). (2) `assignmentEdgeStream` aliases a
to-side ELEMENT face onto its base's assignment SOURCES ('m ← r[0]' + for-each 'r ← g[0]' ⟹
'm ← 0:g[0][0]'; recipient siblings excluded — reassignment risk). doWhile/labeledContinue
re-pinned as sv PRECISION GAINS (unreachable null-merge faces gone). TestLanguageConstructs
fully green. Bench 774ms.

The remaining 16, current decoded diffs:
1. **TestStaticValuesIndexing test2**: missing `method→this.ys*[1]` (return value stored into a
   slot — the TO-SIDE REVERSE face family) + `ys*[0]*` star-spelling drift.
2. **TestStaticValuesRecord test4b/4c**: missing §m directional faces (`r.§m→0:in.§m` — to-side
   reverse again; `method.§m←1:rr.§m` — whole-return §m companion for the @NotModified-embed
   shape; check whether the base ← reconstructs and only the companion is absent).
3. **TestStaticValuesRecord test5**: missing ce-constant faces on derived record fields
   (`method.i←$_ce1`, `method.list.§$s∋$_ce3/4`) — constants × derived-face interplay.
4. **Varargs pair** (TestLinkMethodCall): the old ~∈∋ fan-out cluster + mutator '→'.
5. **Record-pattern ≡ residue** (TestCast, TestInstanceOf, TestVariablesLinkedToObject).
6. Singles: TestLinkModificationArea (≻ vs ←≺), TestRedundantModificationLinks (≡ chain),
   TestMap test2Reverse0 (⊆ vs ~), TestStream MR-swap (← vs ≡), TestSupplier test7 (≺),
   TestSupplierSpec ×2 (⊆/≺ — Stream.generate / fresh-object capture).

The TO-SIDE REVERSE faces (matrix→ldIn.variables[1]) now block items 1 and 2 — that deferred
mechanism is the highest-leverage next fix (likely 3+ tests).

## UPDATE — record patterns + var transparency fixed; link 19

Two engine changes (both suites + bench clean, 738ms):
1. **Return-≺ exemption** in `isInvalidFieldContainment`: a ReturnVariable from-side is
   value-level by nature — `m ≺ 0:r` from record-pattern bindings survives. Fixes both record
   deconstruction tests (and resolves the ≡/≺ record-pattern sub-cluster's ≺ part).
2. **Parameter-deep chaining** (gate `NOPDEEP`): parameters are summary endpoints like returns;
   `0:in → v → this.f` bridges the dying local for the parameter's summary (var transparency).
   The feared makeFromGet-≈ shortcut did NOT reappear (only one extra-only ripple, re-pinned).

Remaining TestLanguageConstructs (3): anonymousClassCapture (m←0:x through the anonymous
instance's 'get'; param face now shows the richer 0:x→get), labeledContinue + doWhile (array
loop-merge shapes: near-inverse losses of m←g[0][0] / m←$_ce1 and the param ∋-ce faces).

## UPDATE — link-suite output-fidelity backlog CLEARED: 60 → 22, all genuine

The 38 output-fidelity failures (ordering, naming, ∩≤≥/≈ drift, extra-only links from the
richer §m/mirror output) are re-pinned to current engine output (iterative repin.py sweep +
manual pins for TestEngine's closure print, TestWriteAnalysis2's JSON2 codec dump — the
serialized methodLinks gained a param→field entry — and testLink3, where the [-1]-slice link
now sorts after the whole-field ~, so the structural asserts read link(1)).

The remaining 22 are ALL genuine consumed-nature losses, by cluster:
1. **TestLanguageConstructs ×6**: do-while/labeled-continue array-element returns (←∋),
   record deconstruction ×2 (←≺), var transparency (→), anonymous-class capture (←/→).
2. **Record/static-values ×4**: simple builder (←∋≡), embed-in-abstract ×2 (←/→),
   indexing-in-array (→).
3. **Supplier/stream ×3**: Stream.generate (⊆), fresh-object capture (≺), test7 (≺).
4. **TestLinkMethodCall ×2**: varargs fan-out (~∈∋ — the old varargs cluster), mutator
   returning object (→).
5. **Record-pattern ≡/≺ ×3**: TestCast, TestInstanceOf, TestVariablesLinkedToObject —
   the binding-site containment family (≺ re-derivation declined per user decision; the
   ≡-losses here may still be real).
6. Singles: TestLinkModificationArea (≻ vs ←≺), TestRedundantModificationLinks (≡ chain),
   TestMap test2Reverse0 (⊆ vs ~), TestStream MR-swap (← vs ≡).

Analyzer: 123 tests, CloneBench-only. Bench ~0.9-1.2s.

## UPDATE — m∩copy DETERMINISM FLAKE ROOT-CAUSED AND FIXED (symmetric completion)

The flake was an ENGINE property: the closure's two directions derive INDEPENDENTLY, so whether
a fact's mirror exists depended on whether the insertion order enabled its own derivation path
(`copy ∩ list` derivable in one edge order, not another; surfaced per-JVM through the salted
iteration of JDK unordered sets feeding re-key order). Reproduced IN-JVM with a 4-edge,
24-permutation unit test (TestEngineDeterminism.twoLevelComposition — now a permanent pin).

Fix: `IncrementalFixpointEngine.completeSymmetrically` — every added composite fact immediately
derives its mirror with the naturally-oriented witness (rev(f∘g) = rev(g)∘rev(f); the reversed
sub-facts exist because edges and mirrors are themselves symmetric), keeping witness choice
canonical across orders (the diamond pin still holds). `acceptForComposite` guards feature #9.
Also: canonical total pre-sort of FollowGraph's fromList (the parts-first comparator is a
partial, intransitive order — TimSort output depended on unordered input) and sorted
reverseReturnFacts. Cost-neutral (bench 887-929ms). TestModificationBasics' strict assert
restored; TestInstanceOf re-pinned (+`o.§es∩0:i` mirror). Analyzer: 123 tests, ONLY
parser-side CloneBench failing. Link suite 60 byte-identical.

## UPDATE — ANALYZER SUITE GREEN (123 tests; only parser-side CloneBench fails)

The last two string tests are done. TestIndependentOfByteArray: naming/format drift re-pinned.
TestVarious `illegal links to constants`: the old `tmp←$_ce57` null-marker link (left over from
`tmp = null;` BEFORE the reassignment) is correctly dropped by sv reassignment clearing — the
marker-payload check now asserts the faithful intent (no constant-marker links remain at that
statement). NOTE (cosmetic, worth a look): index spellings drifted from symbolic `tmp[u][v]`
(loop variables) to `tmp[1][1]` in the sv output — suspicious constant-folding of loop indices
in DV spelling; and the row-precise `tmp[u]~0:dcts[u]` coarsened to `tmp~0:dcts` with `∈?/∋?`
could-be variants.

Open work items, in order: (1) `m∩copy` determinism hunt (collapse/re-key layer, see previous
entry); (2) the deferred to-side reverse faces (`matrix→ldIn.variables[1]`); (3) the DV
index-spelling drift above; (4) the 60 link-suite string tests (output-fidelity backlog per the
verdict-first strategy); (5) loop-carried old-value provenance; (6) the ≺-at-binding-site
option if pattern containment is ever wanted again.

## UPDATE — ≺-family re-pinned (user decision); m∩copy DETERMINISM FLAKE filed; 2 real analyzer failures left

The ≺-family ×4 (TestCast accessor, TestInstanceOf record pattern, MR-by-interfaces,
MR-2-records) is re-pinned to current output per user decision: value-level ≺/∩/≈ decorations
on pattern bindings are not re-derived, and no application consumes them off-spine.
TestModificationFunctional 9/9 green.

**DETERMINISM FLAKE (open work item)**: `m∩copy` in TestModificationBasics test3 flickers
across identical runs (0/1 failures alternating). ∩ is a closure-derived object-graph fact;
`SharedVariables` maps were converted to LinkedHashMap (kept — right direction) but the flicker
persists, so the order-dependence sits deeper (suspects: `isKnownInGraph`'s unordered set →
re-key order in transformToSharedVariable; first-match face selection feeding different edges
to the closure). The test now compares ∩-insensitively (∩ is unconsumed output), so the suite
is deterministic again — but the underlying order-dependence should be hunted with the
TestEngineDeterminism approach extended to the collapse/re-key layer.

Analyzer suite: **2 real failures** (TestVarious illegal-links, TestIndependentOfByteArray)
+ CloneBench (parser-side).

## UPDATE — constructor-in-call summaries; analyzer 6 real

`derivedFaceKeyed` now rehomes a WHOLE-OBJECT source's own faces directly onto the primary's
field (`withException.exit ← $__c_a` + `$__c_a.exception ← 0:e` → `withException.exit.exception
← 0:e`) — the earlier whole-object skip only held when the primary itself was in the group. The
reconstructed-fold's §m companions now cover return FIELD FACES (whole-return endpoints stay
out): `withException.exit.exception.§m ≡ 0:e.§m` reaches the summary — the very link call-site
modification propagation consumes (jfocus withException shape;
TestLinkConstructorInMethodCall 2/2). Extra §m equivalences elsewhere
(`add[0].§m≡1:item.§m`, `set.objects.§m≡this.objects*.§m`) are strengthened-modification
output, re-pinned. ∩/≈ decorations on reconstructed faces are NOT re-derived (unconsumed;
re-pinned out). Analyzer remaining (6 real + CloneBench): ≺-family ×4 (binding-site semantics
decision pending), TestIndependentOfByteArray, TestVarious illegal-links. Link 60, bench ~960ms.

## UPDATE — dying-local face bridging; analyzer 9 real, link suite 60

Summary reconstruction now bridges the field faces of DYING LOCALS: `justJ.j ← b.j ← 0:jp`
collapses to `justJ.j ← 0:jp` in METHOD_LINKS (canChainThrough under `deep`, which now keys on
`Util.primary(emitM)` so return-value FACES reconstruct deeply). Plain FieldReference on a real
local only — never DependentVariables (dimension guard) or parameter/this faces (summary
endpoints). Also: derivedFaceKeyed's `m != sibling-face` guard dropped — in a fluent chain the
group member IS the sibling face ($__rv9.j of `new Builder().setJ(jp).setK(kp)`).
TestStaticValuesAssignment + TestModificationBasics green; **link suite 61 → 60**
(TestStaticValuesRecord `@Identity, accessor` newly passes, zero regressions; `/tmp/cur_final.txt`
refreshed); bench 778ms. Analyzer remaining (9 + CloneBench): ≺-family ×4 (binding-site
semantics decision pending), TestLinkConstructorInMethodCall ×2, TestIndependentOfByteArray,
TestVarious illegal-links.

## UPDATE — $_v fresh-object provenance FIXED; analyzer 11

`LinkGraph.reduceLinks` eliminated the single-link pair `method ← $__c(new URL(...))` entirely,
losing the fresh-object fact behind the `← $_v` marker (@Identity false-positive risk). Fixed
side-band: `Graph.markFreshObjectReturn` / `handleReturnVariable.isFreshObjectReturn`. Two
hard-won constraints: (1) a real `$_v` GRAPH edge is a closure HUB — bench 0.9s → 3-19s,
nondeterministic; never put shared markers in the graph; (2) `ExpressionVisitor`'s
no-data-constructor fallback must fire for EXTERNAL types only — fabricating intermediates on
recursion-prevention nulls floods deeply recursive code. someValue markers are now opaque
everywhere (no §-mirrors, no §m equivalence, no composites targeting them, gate `NOACM`).
TestIdentity 6/6 green. Remaining analyzer failures (11): CloneBench (parser-side), the
≺-family (TestCast accessor, TestInstanceOf, MR ×2 — binding-site decision pending),
TestModificationBasics (`m.i←1:k`), TestStaticValuesAssignment (←), TestLinkConstructorInMethodCall ×2,
TestIndependentOfByteArray (naming/$_ce), TestVarious illegal-links (~← vs ∈∋).

## UPDATE — extraction round 2: derived from-pairs, reverse return facts, reassignment gates; analyzer 13

Fixed (each A/B-clean, link suite 61 byte-identical throughout, bench ~860-930ms):
1. **Derived from-pairs** (`FollowGraph`, gate `NODFP`): read closure knowledge stranded on a
   collapsed chain rep's field face ($__sv_$__rv6.bodyThrowingFunction carrying ←Λ$_fi), emit
   keyed on the recipient group-sibling face (td.throwingFunction). ThrowingFunction part 1.
2. **Reverse return-targeted facts** (`FollowGraph`, gate `NORVREV`): engine feature #9 never
   composes facts TARGETING a return; emit the reverse of return-keyed facts onto non-return
   primaries ('1:r.function ↗ run'). MR-encapsulated-in-record.
3. **Reassignment gates** (gate `NORSRC`): (a) a source-only group member later assigned must
   not alias the new value into its group; (b) ReturnVariable exempt from reassignment removal
   (a second 'return' is a path merge). TestIdentity re-assigned-param verdict now passes.

Open shapes found this round:
- **Loop-carried old-value provenance**: `clear`-on-reassign of `0:amb` removes the vertex and
  with it OTHER-keyed edges about the old value (`method ← 0:amb` from an earlier statement);
  the old engine kept those. Affects loop-reassigned parameter provenance in summaries
  (test3 re-pinned to `method←1:num`; @Identity verdict unaffected).
- **≺-family containment** (TestInstanceOf `set≺0:i`, TestCast accessor, MR ×2 `s.r.function≺s.r`):
  the old engine emitted value-level ≺ for record-PATTERN bindings but not accessor copies. A
  group-aware relaxation of `isInvalidFieldContainment` was tried and REVERTED (wrong on both
  counts: admits `o≺0:r` for accessor copies — group {o, 0:r.object} — misses `set≺0:i` — group
  {set, o}). The distinction must be made where the pattern binding is linked, not in the filter.
- **$_v constructor-result drop** (TestIdentity test2/test4): `return new URL(...)` produces NO
  graph link at all at statement level (graph size 0, no group) — `method ← $__oc` never reaches
  mergeEdgeBi, so `handleReturnVariable` never inserts the `← $_v` marker. Root is in
  ExpressionVisitor's constructor-call handling (mlv EMPTY for unannotated external ctor?), not
  in the collapse. Next scoped item.
- **MR 2-records residue**: `r→s.r` / `r→Λs.r` — the to-side/recipient reverse faces (same shape
  as the deferred `matrix→ldIn.variables[1]`), plus `s.r.function≺s.r` above.
- TestModificationBasics test3 second assert: `m.i←1:k` lost (consumed ←) — uninvestigated.

## UPDATE — goal refocused on MODIFICATION; analyzer suite 24 → 16; two verdict bugs fixed

Strategy shift (user decision): application-level guards (modification verdicts, summaries,
determinism, bench) define "stable"; literal link strings are engine unit tests pinned to
CURRENT output — old-engine literal values are no longer the target. Of the 61 link failures:
24 are output-fidelity only (10 order/names, 9 `∩≤≥`-only, 5 `+≈`) → re-baseline; 37 carry
consumed-nature diffs → triage by verdict impact. The analyzer suite is the primary suite now.

Fixed this round:
1. **Both-sides re-key on collapse** (`Graph.mergeEdgeBi`): when BOTH endpoints of an
   `a ← b` collapse had graph vertices, the to-side was `removeVertices(Set.of(to))` — the bare
   vertex only — leaving `to.§$$s` orphaned (assert crash on the next edge of the same pair,
   `TestVarious::makeSub`) and silently deleting the to-side knowledge. Now both sides are
   re-keyed (to-side recomputed after the from-pass). Link suite unaffected.
2. **Inherited-default-method modification** (`FieldAnalyzerImpl.modifiableThroughInheritedDefaultMethod`,
   from `notes/default-method-modification-not-propagated-to-impl-field.md`): a modifying
   `default` method inherited from an interface marks accessor-backed fields of the implementor
   as modifiable (sound over-approximation; the interface-side summary only records 'modifies
   this'). New test `nolink/TestDefaultMethodModification` (ported from maddi-aapi, reproducer
   enabled) passes; the maddi-aapi `@Disabled` marker can be lifted once sv-integration lands.
3. **∈/∋ containment companions for DERIVED slot faces** (`SharedVariables.assignmentEdgeStream`):
   a derived face `td.variables[0] ← someSet` now also emits `td.variables[0] ∈ td.variables`
   and `td.variables ∋ someSet`. First attempt at the fold level (all reconstructed DV
   assignments) regressed 7 link + 7 analyzer tests — the companions are correct ONLY for
   derived faces; scoped version is byte-neutral on the link suite.
4. **Re-pins** (verdicts confirmed green afterwards): TestArrayVariable, TestCast (record),
   TestWriteAnalysisSyntheticFields, TestIdentity (identity-and-variables),
   ThrowingFunction parts 2–4 (their downstream `isModifying`/`someSet.isModified` verdicts all
   pass — the FI-builder propagation chain works end-to-end). Re-pin tool:
   session scratchpad `repin.py` (reconstructs text blocks, verifies before replacing).

Remaining analyzer failures (16): `TestCloneBench` (parser-side `Applet` lazy-load — NOT ours,
overlaps uncommitted maddi-java-openjdk work); **ThrowingFunction part 1 ROOT-CAUSED**: after
the collapses, two disconnected vertices denote the same slot — the field-group rep
(`$__sv_bodyThrowingFunction` ∋ td.throwingFunction) vs the whole-object-chain rep's field face
(`$__sv_$__rv6.bodyThrowingFunction`, which carries the `← $_fi5` edge); extraction on td reads
only the former. Fix direction: derived FromPairs in `FollowGraph` (graph-side analogue of
`derivedFaceKeyed`). Then: MR-in-record ×2 (miss ↗), MR-by-interfaces (Λ≺), TestIdentity ×3
(←/→, @Identity verdicts), TestModificationBasics test3 (←), TestStaticValuesAssignment (←),
TestLinkConstructorInMethodCall ×2 (←≡; one repin blocked on a 1-char reconstruct mismatch),
TestCast accessor (≡/≺), TestInstanceOf (≺), TestIndependentOfByteArray (naming + $_ce noise),
TestVarious illegal-links (~← vs ∈∋). Bench ~770-870ms.

## UPDATE — 4th consumer (jfocus-transform) ported + derived-face reconstruction; link suite 61, no regressions

The fourth linking application (loop/try-catch → simple-statement transformation,
`~/git/jfocus-transform`) is now guarded maddi-side:
**`maddi-modification-analyzer …/modification/TestModificationLoopTransform`** — literal
`Loop`/`Try` inlined + the 4-level UpperTriangle modification cascade; both tests pass.
Consumer analysis: consumes only the modification family (`← → ∈ ∋ ≡ ~ ≺ ↗ Λ` + §m); the
`objectGraphLinks` production cut is *required* by it (nested `Object[20]` slot arrays are the
`∩`-web worst case). See techniques doc §6b. The jfocus-transform original test is stale on
lambda numbering (`$0..` vs `$10..`) and pinned to `../maddi-kotlin` — its link assertions
never execute there.

Engine work this brought in (all A/B'd clean):
1. **`expandRepToMembers` element type** — pass `dv.parameterizedType()` through instead of
   recomputing from a possibly `Object`-typed (downcast-slot) member base; was an assert crash.
2. **`VariableTranslationMap` DV element type** — same fallback when the translated array has
   no array dimension.
3. **`derivedFaceKeyed`** (`SharedVariables`, gate `NODF`) — slot reconstruction across
   collapsed construction chains: field-wise `build()` decomposition leaves the primary out of
   every whole-object group; rehome chain-sibling slot members onto the primary's field
   (`ldIn.variables[1] ← matrix`). Techniques doc §2.
4. **`derivedShared`** (same gate) — the inverse map for the modification cascade: FI-call
   modification of never-materialized DVs (`ldIn.variables[1]`) expands onto the chain-sibling
   groups ({`matrix`, `0:ld.variables[1]`}). Consumed in `WriteLinksAndModification.go`.

Regression status: link suite **61 failing, byte-identical** with `NODF=1` vs without (the
derived faces change no existing test); analyzer suite 24 vs 25 (the only diff is the new
cascade test passing). **TestParSeqLinkBench 768ms** (good state ~732ms). NOTE: the old
`/tmp/cur_final.txt` snapshot proved stale (29 within-class swaps against both current runs);
refreshed from the `NODF=1` run. Debug aids added: `SVDUMP` (per-statement group dump in
`WriteLinksAndModification.go`), `TRACEVAR=<substr>` (mergeEdgeBi trace), `Graph.printShared`.

Observed but not yet addressed: sv reps leak into printed links as DV *indices* and some `∋`
targets (`matrix[$__sv_col]`, `∋$__sv_matrix[$__sv_col]`) — `expandRepToMembers` handles array
bases and field scopes but not index variables; and a `∈∈`-printed nature surfaced in the same
dump (investigate label printing for deep-element natures).

## UPDATE `66be9fbd` — TestParSeqLinkBench re-run: 2 crash fixes; SPINE COST DECISION PENDING

First deep-structure bench run since the spine era. Found and fixed two production crashes
(addVertex non-idempotence — silently clobbered vertex edges AND spun the expand loop into
'cycle protection'; SharedVariables.merge NYI — the x=y;x=z two-group bridge, hit for the
first time). Suite 62→61 (the clobbering had been corrupting 'instanceof pattern binding').

**Performance profile (TestParSeqElement methods total):**
| config | time |
|---|---|
| golden era (pre-spine) | ~630ms |
| full stack HEAD | 48,669ms |
| NOMAT (spine on) | 29,330ms |
| ALL gates off | 1,049ms |
| **NOSPINE only** (all else on) | **362ms** |

The spine is the entire explosion: it re-enables the deep §-content ∩-web on recursive
generics — the exact O(N²) cost that made the old engine grind 8 minutes and that the
29d597d9 reduction cut. All other session mechanisms are performance-neutral or better
(RedundantLinks speeds things up by pruning). Materialization's share only matters because
the spine bloats the closure it scans.

**RESOLVED via `Options.objectGraphLinks`** (TEST=true / PRODUCTION=false): the ∩/≤/≥ web
is consumed by NONE of linking's three applications (modification: ≻ ≈ ∋ → + §m; same-type/
VL2O: reachability over direct links; object tracking: assignments), so PRODUCTION excludes
those labels from the closure via the engine's valid-predicate. Bench 48.7s → 0.73s with all
mechanisms active; TEST suite byte-identical. Original framing of the decision: the spine bought ~85 tests of
old-engine content semantics. Options: (1) Options-gated spine (TEST fidelity /
PRODUCTION fast-coarse; precedent: checkDuplicateNames); (2) bounded derivation depth
through spine edges (tests need 2-3 hops; the explosion is long transitive chains);
(3) accept the cost (contradicts sv's purpose). Working rule added: **run
TestParSeqLinkBench after every engine-level change** (needs /tmp/jfocus-test-debug.log).

## UPDATE `90b06320` — fluent-setter band + reverse-pair dedup; 62 failing (historical)

Applying the techniques doc: (a) fluent-setter field mirror — the source face's field
('this.i') is collapsed into a DIFFERENT group, so `memberFieldsOf` + a source-face
projection in sharedAssignmentEdgeStream ('setI ← this' mirrors 'setI.i ← this.i', §1.2
direction); (b) reverse-dedup in the reconstruct fold with rank-desc processing; (c)
`dedupReversePairs` on every assembled builder (deeper-from-side wins; index-tracked
removal). TestStaticValues1 ×3, TestGetSet ×2 green; order-only sweeps. 66→62, no new
regressions. Remaining bands: statement-scoped §m faces (~4), the ≈ coarse family
(generic factory + receiver chain), scattered structural.

## UPDATE `953bf6e3` — §m source-inheritance; 71 failing (historical)

`virtualModificationEdgeStream` now rehomes VMI members of a primary's assignment
SOURCES onto the primary (SharedVariables.assignmentSources; 'return zs' + view
semantics gives 'sub.§m≡0:in.§m'). Fixed both 'asShortList' §m cases; 74→72, plus
order-only sweeps →71. Only-source-direction transfers (cf. isPureAssignmentSource).

DEAD END (reverted): TO-side return-face rehoming for the remaining §m cases
('l1.§m≡method.§m' on a LOCAL's summary, TestRedundantModificationLinks simple chain,
TestStaticValuesRecord record/test3): naively rehoming link TARGETS onto return
recipients leaks LATER-statement knowledge into EARLIER per-statement views
(stmt-1 l2 gained ≡l3.§m/≡method.§m from stmt-3's collapse). Needs statement-scoped
face substitution — the face is only valid in views at/after the collapse statement.

## UPDATE — RedundantLinks ported; 74 failing (now historical)

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
