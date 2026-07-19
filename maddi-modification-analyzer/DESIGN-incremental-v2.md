# Incremental reuse v2 — the single-module giant-SCC monorepo (task #35)

Status: DESIGN, 2026-07-19 (post checkpoint/resume v1); RECONCILED same day with the kotlin-side
early-cutoff machinery after the merge (see §0). Owner: sv-integration thread.

## 0. Reconciliation with the delivered early-cutoff worklist (kotlin branch, 2026-07-18)

The metrics thread delivered, independently: `EarlyCutoffWorklist` driving REAL per-primary-type
recompute through the reload path — `recompute(t) = clear optimistic carry (removeIf
CROSS_TYPE_DERIVED) + prep + analyze + AnalysisFingerprint.of(t)`, queueing t's dependents ONLY
when the fresh output fingerprint differs (TestEarlyCutoffWorklistDriver; property analysis-tier
flags; seeded `analyze(order, graph, initialDirty)`). That solves the DAG/multi-source-set case
at type granularity, with output-fingerprint EARLY CUTOFF as the frontier device.

This design remains the complement for the case that model cannot crack: inside the giant SCC,
dependents-of(t) over the type-dependency relation ≈ the whole cycle, and the first recompute
floods. The composition is: KEEP their worklist and output-fingerprint cutoff; REPLACE the
dependents relation inside SCCs with the learned summary-consumption edges of §3.3 (much sparser
than potential dataflow). §3.1/§3.2 below largely coincide with what their machinery + checkpoint
v1 already provide and shrink to integration work; §3.3 is the remaining new piece. Their known
gap (Codec fieldIndex throw when fingerprinting a single-type-analysed type whose links reference
a modifiable field) is the same family as checkpoint v1's decode tail — one shared fix list.
Prereqs in place: checkpoint v1 (CheckpointWriter, restoreCodec, goDirTolerant, CHECKPOINT/
CHECKPOINT_RESTORE gates — commits 84a6003c, 49415b2e, b197b101); per-sourceset fingerprints
(maddi-kotlin branch, other thread).

## 1. Problem

The 3M-line target is a SINGLE module whose largest call-graph cycle touches ~2/3 of the code.
Standard incremental units are useless there:

- per-SOURCESET invalidation: one source set = the whole module = full re-run;
- SCC-transitive invalidation: the giant cycle makes almost any change invalidate almost
  everything.

A full cold run at elasticsearch scale (240k elements) is 5h+; the monorepo will be several
times that. Incremental reuse must work at ELEMENT granularity or not at all.

## 2. What v1 already gives us

- **Value persistence at element granularity**: the checkpoint files carry per-info analysis
  values (file per primary type), restore preloads them tolerantly, present-values-win.
- **The verify-certify loop as a soundness net**: a resumed run does not trust preloaded values —
  the first pass re-derives from summaries, TolerantWrite makes agreeing writes cheap, and
  certification refuses a fixpoint with standing refusals (STRICTCERT; refused-downgrade counter).
- **Observed behavior on resume (fernflower)**: preload + verify certifies quickly; one
  non-confluent corner (TargetInfo.LocalvarTarget) converged to a *different but certified* (more
  precise) fixpoint — the EdgeType family. Incremental reuse inherits exactly this property:
  the result is A certified fixpoint, not necessarily THE cold-run fixpoint.

## 3. Design

### 3.1 Element-level fingerprints

Fingerprint each Info's ANALYSIS-RELEVANT syntax: the method body's CST (post-parse, pre-analysis),
the field initializer, the type's member signature list. Store alongside the checkpoint
(per primary type, same file or a sibling `.fp` file). The openjdk scanner already computes
source fingerprints (computeFingerPrints flag) — reuse that machinery; the fingerprint of a
method must NOT include its callees' bodies (that is what the consumption edges are for, §3.3).

### 3.2 Optimistic preload

On resume after a source change:
1. Parse everything (unavoidable; javac is fast relative to analysis).
2. For each element whose fingerprint matches the stored one: preload its checkpointed values
   (restoreCodec, present-wins).
3. For changed elements: no preload; they are analyzed cold.

This is OPTIMISTIC because a preloaded value may depend on a changed element's summary. That is
not detected up front — it is caught by the net (§3.4) or cut off by the edges (§3.3).

### 3.3 Learned summary-consumption edges

During any full run, record for each element E the set of summaries (other elements' analysis
values) that E's analysis actually READ — a "consumption edge" E -> D. This is much sparser than
the call graph inside a giant SCC: a method deep in the cycle typically consumes a handful of
summaries, not 2/3 of the codebase. Persist the edges with the checkpoint.

On resume: invalidate the preload of any element with a consumption path to a changed element
(transitive closure over consumption edges, NOT over the call graph). The giant SCC stops
mattering: invalidation follows actual dataflow, not potential dataflow.

Instrumentation point: the analyzer's summary reads all go through a small number of accessors
(`analysis().getOrDefault/getOrNull` on OTHER infos during link computation and modification
propagation). A thread-local "currently analyzing E" + a recording wrapper yields the edges with
minimal intrusion. Cost: one set-add per cross-element read; measure with ASPROF before/after.

### 3.4 Verify-certify as the final net

Even with §3.3, preloaded values are never trusted blindly: the resumed run executes the normal
iterating loop over (changed elements + invalidated elements + a verification pass over the rest),
and the certification rules (incl. STRICTCERT refusals) decide. Soundness therefore does NOT
depend on the completeness of consumption-edge recording — an under-recorded edge shows up as a
verification-pass change, which re-dirties the element and its consumers. Consumption edges are a
PERFORMANCE device; certification is the correctness device.

### 3.5 Non-goals of v2

- Cross-version codec migration (the legacy-map fallback pattern covers format evolution
  case-by-case).
- Sub-method granularity (statement-level reuse) — measure v2 first.
- Distributed/parallel-machine analysis.

## 4. Phasing

- **Phase A (measure)**: instrument consumption-edge recording behind an env gate (CONSEDGES=1),
  run fernflower + elasticsearch, report edge-count distribution (per element: how many summaries
  consumed; per element: how many consumers). GO/NO-GO: if the median consumer closure of a
  random element is <5% of the codebase, the design wins big; if it approaches SCC size, stop.
- **Phase B (fingerprints)**: element fingerprint computation + storage next to the checkpoint;
  A/B: fingerprint stability across two identical parses (must be 100%).
- **Phase C (resume path)**: wire §3.2+§3.3 into CHECKPOINT_RESTORE; acceptance: touch one method
  body in fernflower, resume, assert (a) verdicts equal the cold run's mod known non-confluence,
  (b) analyzed-element count ~ consumption closure size, not corpus size.
- **Phase D (scale)**: elasticsearch — touch one file, measure wall-clock vs the 5h21m cold run.

## 5. Open questions (for the user)

1. Are the maddi-kotlin per-sourceset fingerprints meant to be REUSED here (shared engine), or is
   element-level a separate mechanism? (Design assumes separate but reusing the hash machinery.)
2. Consumption-edge persistence format: piggyback on the checkpoint JSON (per-type files) or a
   separate compact binary? Edge volume at elasticsearch scale is the deciding number (Phase A).
3. Is the resume-fixpoint non-confluence acceptable for the monorepo use case (certified ≠
   bit-identical to cold)? If bit-identical matters for downstream diffing, we need a
   "canonicalization pass" item first.
