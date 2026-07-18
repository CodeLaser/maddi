# Analysis rewiring: the analysisFingerprint

How to avoid recomputing the *expensive* part of an incremental run. Companion to `rewiring.md` (the
parser-level reload/rewire) and `maddi-modification-analyzer/definitions.md`. This document is the plan;
nothing here is implemented yet.

- **Status: first deliverable landed (2026-07-18).** `AnalysisFingerprint` + a test on real analyzer output are
  in (`maddi-modification-prepwork/io/AnalysisFingerprint`, `analyzer/rewire/TestAnalysisFingerprint`). See
  "First results" below. Carry / early-cutoff wiring not started.
- Builds on the parked `carryOnRewire` mechanism (`rewiring.md` "Parked mid-flight") as its substrate.

## First results (2026-07-18)

`AnalysisFingerprint.of(runtime, primaryType)` = MD5 of the canonical `WriteAnalysisResults` dump, restricted to
analyzer output (`WriteAnalysisResults` gained an optional `Predicate<Property>`; default `true` keeps the
production writer byte-identical — its round-trip tests still pass). Tested on real analyzer output: deterministic
across two independent analyses, flips on an immutable→mutable change. The dump of a tiny immutable class reads
exactly as intended — type verdicts (`containerType/immutableType/independentType`), field
(`finalField/unmodifiedField/links`), method (`methodLinks/nonModifyingMethod`), parameter
(`unmodifiedParameter`). Two findings from the exercise:

1. **`VARIABLE_DATA` is already absent from the codec dump.** `WriteAnalysisResults` only walks Info-level analysis,
   and the codec does not serialise `VARIABLE_DATA` even there — so excluding it is a no-op today. `ANALYZER_OUTPUT_ONLY`
   keeps the guard anyway (future-proofing + `VARIABLES_OF_ENCLOSING_METHOD`), but the "exclude VARIABLE_DATA" concern
   is moot until/unless it starts being serialised.
2. **Link encodings embed source positions — a precision leak, now normalised out.** A field/return link serialises
   its scope expression with its line:col, e.g. `["variableExpression","4-23:4-26",["T",["a.b.X"]]]`. So an edit that
   merely *shifts lines* changed the embedded positions and flipped the fingerprint of an otherwise-unchanged type.
   **Fixed** by a normalizer pipeline (below); `testLineShiftInvariant` confirms a leading-comment edit leaves the
   default fingerprint unchanged while the RAW fingerprint differs.

### The normalizer pipeline (extensible by design)

The fingerprint is `MD5(normalize(dump))`, where `normalize` is an ordered pipeline of `FingerprintNormalizer`s —
each erases from the serialised dump some detail a *dependent* cannot read, widening the class of edits the
fingerprint ignores. Many are expected (the reason for the abstraction): positions now, variable-name blanking for
rename-invariance later, ordering canonicalisation, etc. Substrate is the serialised dump (`String`) — the one
representation every normalizer shares; a structural one may parse/re-serialise internally.

- `FingerprintNormalizer{ name(); normalize(String); }` with a stated **soundness contract**: a normalizer may
  only erase detail a dependent cannot read (erasing positions is sound; erasing a verdict would not be — it would
  hash two different results equal and skip a needed recomputation).
- `SourcePositionNormalizer` — the first: strips the quoted `compact2` coordinate (`"\d+-\d+:\d+-\d+"`), anchored on
  its distinctive shape (no collision with statement indices / fqns / link markers).
- `AnalysisFingerprint`: profiles `RAW` (none) and `DEFAULT` (positions); `of(runtime, type)` uses `DEFAULT`,
  `of(runtime, type, profile)` takes a custom list. Position-invariance is on by default because it is sound and
  only improves precision.

### Wired into the flow — the storage half (2026-07-18)

`AnalysisFingerprint.storePerSourceSet(runtime, primaryTypes)` groups the analyzed primary types by source set,
computes each set's rollup (`ofSourceSet` = hash of its fqn-sorted, normalised per-type dumps), and stores it on the
set — **SetOnce-guarded**, so a set already carrying a loaded fingerprint is left alone. Both production runners
(`run-openjdk`, `run-main`) call it right after the modification analysis converges, and log the count. This
activates the dormant `SourceSet.analysisFingerPrintOrNull()` hook; the value already persists through the
analysis-results JSON (`JsonStreaming`), so a later run can read the previous fingerprint back.

This is deliberately only the **storage** (and persistence) half. The **compare** half — on reload, skip
re-analysing a set whose stored fingerprint is unchanged, carrying its results — needs two things that are not in
yet: the reload flow must actually *run analysis* (today `RunRewireTests` reloads + reparses + recomputes the call
graph but does **not** analyse, so post-reload there is nothing to compare), and the parked `carryOnRewire` must land
so a spared type comes back with its analysis rather than empty. Test `TestAnalysisFingerprint.testSourceSetRollupAndStore`.

### Compare half — prototype (2026-07-18)

`run-openjdk/TestAnalysisEarlyCutoffPrototype` prototypes the compare *decision* (not yet the saving): analyse a
tiny project, snapshot per-type fingerprints, edit a file, analyse again, diff. The diff is exactly the set of types
whose analyzer output moved — the blast radius a real incremental run must recompute; everything else is what the
cutoff would spare. It lives in `run-openjdk` because that is the only module with the analyzer, the openjdk
front-end, and the fingerprint on one path (`run-rewire` cannot see `modification.analyzer`). Both runs are full,
clean analyses on a fresh inspector — so it shows the decision, not the reload/skip wiring.

Two findings, both instructive:

1. **A comment-only edit moves nothing** — the whole run would be cut off. This is the headline win, and it is what
   the position normalizer buys (without it, the shifted line:col in link encodings would have moved everything).
2. **The blast radius is output-based, far tighter than structural reachability.** Making `Data` mutable moved only
   `x.Data`'s fingerprint; the dependents `User` (exposes the field either way) and `Holder` (public `Data` field)
   both *use* Data but neither's output moved on this analyzer, so both are spared. A structural "reaches Data" rule —
   which is what the parser rewiring's REWIRE state uses — would have recomputed both. This is precisely why the
   compare half is worth building: it prunes the REWIRE cone down to the types whose output actually changed.
   (Open question flagged: whether `Holder`'s immutability *should* have moved is an analyzer-propagation matter,
   separate from the compare mechanism; not presumed by the prototype.)

**Validated on the real reload path** (`testReloadPathCommentEditCutsOff`). The same comment edit driven through the
actual incremental machinery — `reloadSources` → `Invalidated` (INVALID/REWIRE/UNCHANGED via `PrimaryTypeUseGraph`) →
reparse → re-prep → re-analyze — again yields a blast radius of `[]`. Two things this de-risks for the real skip:

1. **Re-analysing after a reload does not crash.** Re-running prep + analyzer over a mix of freshly-parsed (INVALID)
   and kept (UNCHANGED, already-analysed) types is tolerated: prep skips re-deriving `VARIABLE_DATA` where present,
   and the analyzer's monotonic-overwrite guard accepts a recomputed-identical value. So the flow can re-analyse
   without a clean inspector.
2. The fingerprint decision holds on the real path, not only on two clean analyses.

**Next, to turn the decision into the saving:** the pieces are now proven separately — run analysis inside the reload
flow (works), hash each recomputed type's output (works), diff against the stored/prior fingerprint (works). What
remains is the actual *skip*: when a recomputed type's fingerprint is unchanged, stop propagating to its dependents
and carry their prior analysis instead of recomputing (the `carryOnRewire` substrate, now resumed — `rewiring.md`).
That is a worklist over the use graph seeded by the fingerprint-changed types, and it is the next build.

## The point: skip link + analyzer, not prepwork

Prepwork is ~1/10 of the modification-analysis cost; **link + analyzer are the other 9/10**. So the prize is
never "carry the prepwork `VariableData`" — that is the cheap thing, just re-run it. The prize is to **not
recompute link + analyzer** for a type whose analysis result will not change. The `analysisFingerprint` is the
gate that decides when that skip is sound.

This reprioritises everything relative to the parser rewiring, whose carry started at prepwork. Here the order is
the *inverse of cost*:

1. **analyzer output** (the verdicts) — carry first;
2. **link system** (`LINKS`, `METHOD_LINKS`) — carry second;
3. **prepwork `VariableData`** — recompute (cheap); carry only if measurement later says it pays.

Two facts make this order also an order of *increasing complexity*, so the highest-value tier is the easiest:

- **The analyzer's headline output is variable-free and lives on structural-identity Info.** `IMMUTABLE_TYPE`,
  `CONTAINER_TYPE`, `INDEPENDENT_*`, `NON_MODIFYING_METHOD`, `UNMODIFIED_*`, `FINAL_FIELD` are scalar values on
  `TypeInfo`/`MethodInfo`/`FieldInfo`/`ParameterInfo`, all of which have structural identity (fqn, or method+index).
  Carrying them is a trivial `carryOnRewire` opt-in with a `return this` / Info-mapping `rewire`. No variable names,
  no renames.
- **The rename problem does not arise at the coarse stage.** A coarse fingerprint that matches means the analysis
  output is byte-for-byte identical *including variable names*, so even the variable-bearing link data
  (`METHOD_LINKS`, field `LINKS`) carries with **no rename map** — `Variable.rewire` maps it by unchanged names.
  Alpha-renaming (a local/parameter rename absorbed as "no change") is a *later refinement* (v3 below), not a
  prerequisite. The rename example that motivated this work is deliberately the last case, not the first.

## The mechanism: an output fingerprint + early cutoff

Today the reload flow classifies each primary type `UNCHANGED` / `REWIRE` / `INVALID` (see `rewiring.md`) and:

- `UNCHANGED` keeps its objects and its analysis — sound, nothing to do;
- `INVALID` is re-parsed and **fully recomputed**;
- `REWIRE` (reaches an INVALID type) **drops all derived analysis and recomputes** — conservatively, because
  "REWIRE = reaches an INVALID type" is a *structural* reachability, not an *analysis* one.

That last line is where the 9/10 is wasted. A REWIRE type reaches a changed type *structurally*, but if that
changed type's **analysis output** did not actually change, the REWIRE type's analysis is still valid. The
`analysisFingerprint` replaces structural propagation with **output propagation** — classic incremental
"early cutoff" / "firewall" (à la Salsa red-green):

> Recompute a dirty type. Hash its analysis **output**. If the hash equals last run's, the change **did not
> propagate** — its dependents are spared (they carry their old analysis). Only a *changed* output dirties
> dependents.

So an edit deep in a method that does not change any verdict costs exactly one type's recomputation, not its whole
downstream cone. An edit that is *only* comments or formatting (v2) costs nothing. An edit that is *only* a rename
(v3) costs nothing.

## The fingerprint is the cst-io codec dump, hashed

Do not write a new traversal. `maddi-modification-prepwork/io/WriteAnalysisResults` already serialises a type's
analysis, and it is exactly the right shape:

- `write(codec, ctx, info, i)` encodes an Info's `analysis().propertyValueStream()` (filtered, sorted) to an
  `EncodedValue`; `writeMethod` folds in each parameter's analysis as subs; `writeType` recurses subtypes / fields
  (sorted by name) / methods (sorted by fqn) — **canonical order**.
- It runs on `PrepWorkCodec`, which composes the prepwork + link codecs, so one dump already covers the verdicts,
  `METHOD_LINKS`, field `LINKS`, and prepwork values. `Codec.TypeAndSorted` gives the sorted, index-referenced
  ordering; references encode by fqn / structural index, so the bytes are **deterministic and reference-stable
  across runs**. Source positions are not in `analysis()`, so they are excluded for free; analyzer messages
  (contract-violation / near-miss) are collected separately, not in `analysis()`, so they are excluded too.

**`analysisFingerprint(type) = MD5(canonical codec dump of type.analysis(), recursively)`.** Reuse
`WriteAnalysisResults` (factor its per-Info encode into a shared helper that returns the `EncodedValue` instead of
writing a file; MD5 the serialised bytes). This is the existing dormant `SourceSet.analysisFingerPrintOrNull()`
hook made real, one level finer (per type, rolled up to the set).

**v1 fingerprints real analyzer output only — `VARIABLE_DATA` is left out.** The whole-type dump *minus* the
purely-internal prepwork detail: the verdicts (`IMMUTABLE_*`, `CONTAINER_TYPE`, `INDEPENDENT_*`,
`NON_MODIFYING_METHOD`, `UNMODIFIED_*`, `FINAL_FIELD`) and the link summaries (`METHOD_LINKS`, field `LINKS`).
`VARIABLE_DATA` is excluded from the start, for two reasons: it is recomputed anyway (prepwork is cheap), and it is
stuffed with variable names, so including it would flip a type's own fingerprint on a bare rename — self-defeating
for the v3 rename case. Excluding it does **not** break early-cutoff soundness: soundness requires only that the hash
cover what a *dependent* can read, and a dependent reads the callee's verdicts and link summaries, never its internal
per-statement `VariableData`.

A normalised `VARIABLE_DATA` (variable names standardised or blanked to positional placeholders, so it is
rename-invariant) can be folded back in **later** if measurement shows the coarser hash cuts off too eagerly —
that is a v4 refinement, deferred. So "coarse → fine" here means both **granularity** (whole type first, per method
later) and **coverage** (analyzer output first, normalised prepwork detail later), never omitting an output that a
dependent actually reads.

## The propagation graph

The dirty closure runs on `prepwork/callgraph/PrimaryTypeUseGraph` — the transposed call graph ("who uses this
type?"), already the reload flow's reverse-reachability. Seed it with the types whose `analysisFingerprint`
**changed** (not merely those re-parsed); everything reachable is a recompute candidate; the rest carries.

One edge the CST graph lacks (`rewiring.md` Trap 7): `IMPLEMENTATIONS` is written *upstream*, onto the overridden
abstract method in a supertype. So the analysis-dependency graph is `PrimaryTypeUseGraph ∪ implementations-edges`;
the closure must include it or a changed implementation's effect on a supertype's `IMPLEMENTATIONS`-derived verdicts
is missed.

## What a fingerprint match lets each tier carry

Under a v1 match (identical output, identical names), all three tiers carry cleanly; the inventory sweep pinned the
exact remap needs for when v3 breaks the "identical names" assumption:

| tier | on a fingerprint match | when renames enter (v3) |
|---|---|---|
| analyzer verdicts (`IMMUTABLE_*`, `CONTAINER_TYPE`, `INDEPENDENT_*`, `NON_MODIFYING_METHOD`, `UNMODIFIED_*`, `FINAL_FIELD`) | carry via `carryOnRewire`; `Value.rewire` is `return this` / Info-map | unaffected — variable-free |
| link (`METHOD_LINKS`, field `LINKS`) | carry as-is; names unchanged so `Variable.rewire` maps by name | needs the local↔local rename map; parameters are positional / structural, so safe |
| prepwork (`VARIABLE_DATA`) | recompute (cheap) | recompute — the rename map matters only if we ever choose to carry it |

Rename scope, for v3 (from the sweep): a **local** rename perturbs both String-fqn keys (`VariableData.vicByFqn`)
and Variable-object-keyed structures (locals' `equals` is fqn=name); a **parameter** rename perturbs only String-fqn
structures (`ParameterInfo.equals` is method+index). `Reads`/`Assignments` are pure statement indices — invariant.
Existing remap hooks sit at the right places: `Links.translate`, `MethodLinkedVariables.translate`, and the
stubbed `rewire(InfoMap)` on `VariableDataImpl:106`, `LinksImpl:437`, `MethodLinkedVariablesImpl:167`.

## Refinement path (coarse → fine)

1. **v1 — output fingerprint, whole type, early cutoff.** Hash the full analysis dump; propagate change through the
   use graph; carry the analyzer verdicts (tier 1). Biggest structural win on any real edit.
2. **v1.1 — carry the link tier** (tier 2) on a match; measure the additional saving.
3. **v2 — coarse *input* fingerprint** (comment/whitespace-invariant token hash of the source), so format/comment-only
   edits never even mark a type changed (they skip re-parse-triggered recompute entirely, before the output hash).
4. **v3 — alpha-rename** locals/params in the input hash → a pure rename is "no change"; now carrying the link tier
   needs the local rename map (the sweep's machinery). This is the motivating example, arriving last on purpose.
5. **v4 — per-method granularity and per-property precision** — only where measurement shows the whole-type blob is
   too coarse (one trivial verdict flip re-running a large type's dependents).

## Relationship to the parked `carryOnRewire`

`carryOnRewire` (parked, `rewiring.md`) is the substrate: per-property opt-in carry + rename-aware `Value.rewire`,
with `PropertyValueMapImpl.rewire` filtering on `carryOnRewire()`. This plan **reprioritises which properties opt in
first**: the parked note starts at `VARIABLE_DATA` (prepwork); here the analyzer verdicts opt in first, then the link
properties, and `VARIABLE_DATA` is deprioritised. The two are compatible — same mechanism, different opt-in order —
but Phase 0 (finish + validate the parked carry) remains a prerequisite for any carry to work at all.

## First deliverable (proposed)

An **`AnalysisFingerprint` utility + a measurement harness**, no behaviour change, low risk, no Gradle burden:

1. `AnalysisFingerprint.of(TypeInfo)` — reuse `WriteAnalysisResults`' per-Info encode to produce the canonical
   `EncodedValue`, serialise, MD5. (Factor the encode out of `WriteAnalysisResults` so both share it.)
2. Extend `RunRewireTests`: after each reload + reanalyse, compute every type's `analysisFingerprint` and report,
   for the edit just made, **how many types' fingerprints are UNCHANGED vs the previous run**. This *measures* the
   available cutoff (how much recomputation v1 would save) on real edits, and doubles as the soundness check —
   an edit that should change nothing must leave (almost) every fingerprint stable; a semantic edit must flip
   exactly the affected cone. Only once this reads right do we wire the actual skip.

## Open questions / risks

1. **Canonicalisation audit.** Every value encoder on the fingerprint path must emit deterministic *and
   position-free* bytes. Two sub-parts: (a) sorted maps/sets — `MethodLinkedVariablesImpl.encode` sorts `modified`;
   confirm `LinksImpl`, `VariableBooleanMapImpl`, `VariableToTypeInfoSetImpl` do likewise (determinism across runs
   held in the test, so this is likely already fine on the analyzer-output path); (b) **strip source positions** —
   DONE via `SourcePositionNormalizer` (First results). Remaining audit item: confirm no *other* position encodings
   (detailed sources, statement sources) leak onto the analyzer-output path beyond `compact2`.
2. **What exactly is "the analysis output" — SETTLED for v1: analyzer output only, `VARIABLE_DATA` excluded** (see
   above). The hash covers the cross-type-observable outputs (verdicts + `METHOD_LINKS`/`LINKS`), which is what a
   dependent can read, and drops the internal prepwork detail (recomputed anyway, and rename-noisy). Implemented as a
   property-tier selection on the codec stream — a "fingerprint-relevant" predicate over `Property`, the same shape
   as `carryOnRewire`. Re-inserting a normalised (name-blanked) `VARIABLE_DATA` is a later, measurement-driven step.
3. **Cost of hashing.** Encoding-to-hash is O(output size), cheap vs computing it; confirm on a large type.
4. **Interaction with the fixpoint.** The analyzer iterates to a fixed point; the fingerprint must be taken on the
   *converged* output (post-`logVerdictFingerprint` point in `IteratingAnalyzerImpl`), and the early-cutoff decision
   must not itself perturb convergence order for the types that do recompute.
