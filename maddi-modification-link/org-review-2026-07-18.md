# Organization review, maddi-modification-link — 2026-07-18

Four parallel read-only reviews (core pipeline; graph layer; support packages + io + natures; tests +
docs), synthesized. Verdict up front: **the architecture's spine is intact and recognizable**
(Computer → Visitor → LinkMethodCall → Graph → Writer; generic engine vs domain graph split held
through the sv retrofit). The accretion is **concentrated, not smeared**: two source files and one
method carry most of the structural debt, and the code is consistently comment-compensated. The
*derived artifacts* — README, operator.adoc, the manual's baseline, the codec — have drifted more
than the code itself.

## Sound (no action needed)

LinkMethodCall (exemplary LEAF/test-comment discipline) · ExpressionVisitor (dispatch clean, edges
strained) · Result/MethodLinkedVariablesImpl · the whole impl/graph engine package (still fully
generic) · VirtualModificationIdenticals, MakeGraph, RedundantLinks, FollowGraph (core loop) ·
localvar & translate packages · vf package (virtual-fields.md is the best doc in the module) · the
FI trio's seam (explainable from class javadocs alone) · test suite themes (one dominant base class,
real spec layer in TestLinkMethodCall + the *Spec files).

## The four concentrated problems

1. **`WriteLinksAndModification` (needs-split).** >half re-implements graph-layer semantics its own
   comments admit "mirror" FollowGraph/Graph/LinkImpl rules (5 shadow sites); its core
   `doVariableReturnRecompute` is a 215-line multi-phase god-method whose phase ORDER is load-bearing
   and documented only inline; `iterateOverShared` is a near-verbatim copy of
   `Graph.expandRepToMembers` diverging on `==` vs `.equals`.
2. **`LinkComputerImpl` (needs-split).** Two classes in one file: ~300-line orchestrator +
   ~700-line `SourceMethodComputer` that has absorbed property-WRITING jobs (VL2O, casts, downcasts,
   NON_MODIFYING) that belong in the writer layer. Its 4-arg `doStatement` (~140 lines) is the whole
   per-statement lifecycle in one body.
3. **`Graph.java` (needs-split along its own seam).** Six responsibilities: engine facade; edge
   admission policy; sv-collapse WRITE path (mergeEdgeBi ~100 lines); VMI routing; reconstruction
   READ path; two side-band fact stores. Write and read paths never call each other — separable as
   e.g. SvCollapse / SvReconstruction with Graph keeping facade+policy+side-bands.
4. **`ShallowMethodLinkComputer.transfer`** (160 lines, 4 interleaved cases + §m emission);
   `correspondingTypeParameters` re-implements `VirtualFieldComputer.hiddenContentHierarchy` and
   allocates a GenericsHelper per recursion level.

## Cross-cutting findings

- **Env gates: 44 call sites, 36 distinct names** (26 NO* behavior-reversion + 7 trace/dump), 32 of
  44 clustered exactly at the sv seam (Graph/WLAM/SharedVariables). Documented at site, inventoried
  nowhere; raw string literals (typo = silent no-op); several self-declared settled (NOVMIDIR,
  NOPASSFIX, NOFLIPSAME) are retirement candidates.
- **Vocabulary:** group/face/derived-face/source/recipient/rehome naming is consistent and matches
  the sv doc 1:1. Frictions: "rep" exists only in comments (the SharedVariable class plays both
  roles); "expand" means five different things across five classes.
- **Doc drift (the real debt):**
  - linking-manual §8/§9 baseline is WRONG: the two "long-standing failures" both pass (suite fully
    green modulo 7 documented @Disabled items); anyone applying "third failure = regression" today
    has the wrong contract. §2 still credits the fixpoint to LinkGraph (it's
    IncrementalFixpointEngine); §10's file map omits the entire sv/graph layer;
    TestFixedpointPropagationAlgorithm doesn't exist.
  - README (2026-01-07): lists 12 of 18 natures; documents a dead `$__l` kind; misses `$__sv_`,
    `$_afi` distinctness, and that `Λ` is print-only decoration; no routing to the manual/handoff.
  - operator.adoc generator omits `∈?`, `∋?`, `↖` — 3 of 18 operators missing from the
    "comprehensive" table and from the symmetry test.
  - virtual-fields.md §8.2 describes a load-bearing BUG that has since been FIXED — the md now warns
    against a fix that already happened (doc and code tell opposite stories).
  - sv-remaining-catalogue.md (1730 lines): the catalogue proper CLOSED at line 747; since then it is
    a reverse-chronological engineering journal with the live state buried mid-file. Rename/split:
    frozen journal + small live CURRENT-STATE doc.
- **Codec holes (latent bugs found by review):** `☷` (pass-variant ≡) is EMITTED by
  ShallowMethodLinkComputer for Iterator.remove-style annotations but cannot be decoded (throws
  "Unknown symbol ☷"; pass set has no encoding at all). `$_v` someValue markers share one literal
  name AND the decode side never caches them (encode does) → second `$_v` hits an assert on decode.
  SharedVariable/IntermediateVariable have no codec branch, kept out of summaries only by implicit
  filtering.
- **Dead code:** LinkComputerImpl.variableCounter (never read); ListOfLinksImpl (half-implemented
  Value: encode()=null, rewire()=NYI); LinkNatureImpl's pass==null hashCode fast path (pass never
  null); commented-out blocks at ExpressionVisitor:465, WLAM:121, LinkAppliedFunctionalInterface:168-178.
- **Tests:** impl/ vs typelink/ boundary eroded (typelink mixes deep and FORCE_SHALLOW; hosts the
  Spec files); one true duplicate pin (impl/TestList INPUT1 vs typelink/TestList INPUT1, same source,
  drift risk); Stream coverage spread over 5 files; ≥4 incompatible pinned-string harness variants
  (CommonTest contributes none); two `staticvalues` packages; vague numbered names (Test1/2/3,
  TestWriteAnalysis2); no package-info.java anywhere.

## Ranked action plan

Small/high-value (do first, mostly docs — an afternoon total):
1. Fix linking-manual §8/§9 baseline + §2 fixpoint attribution + §10 file map (add the sv layer).
2. Regenerate operator.adoc with the 3 missing natures; sync README's nature list; fix README's
   marker-variable taxonomy; add a 10-line "start here" routing block.
3. Fix virtual-fields.md §8.2 (the bug it warns about is fixed).
4. Split sv-remaining-catalogue: frozen journal + small live-state doc.
5. Close the ☷/`$_v` codec holes (encode pass set or assert-at-encode; cache someValue on decode).
6. Delete dead code (variableCounter, commented blocks, dead hashCode path); delete
   WLAM.iterateOverShared in favor of Graph.expandRepToMembers.

Medium (structural, each behavior-preserving and A/B-verifiable):
7. Decompose doVariableReturnRecompute into named phase methods with a phase-order contract comment.
8. Extract SourceMethodComputer to its own file; move its property-writing (VL2O/casts/
   copyModificationsIntoMethod) toward the writer layer.
9. Gate registry in Gate.java (constants + one-line provenance each; standing retirement rule);
   retire the self-declared-settled gates with their dead arms.
10. Split Graph.java into facade+policy / SvCollapse / SvReconstruction.
11. Decompose ShallowMethodLinkComputer.transfer; reuse hiddenContentHierarchy.
12. Name the five inline sub-techniques of SharedVariables.assignmentEdgeStream after the doc's terms.

Opportunistic:
13. Consolidate the pinned-string test harness (promote the *Spec files' mlvOf pattern into
    CommonTest); document the test taxonomy (package-info or manual §8); merge the duplicate
    TestList pin; resolve the duplicate staticvalues packages.
