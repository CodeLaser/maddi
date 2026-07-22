# VariableData flatten-snapshot — bounding pass-1 peak heap on a single giant SCC

Status: DESIGN, 2026-07-22. Owner: extract-interface engine-stress thread. Gated, OFF by default.

## 1. Problem

At elasticsearch scale the modification analyzer OOMs only when all 27 source sets are analysed
together; any single module (even `server/main`, 138k elements) fits at 32G. But the **3M-line
commercial target is a single module whose largest call-graph cycle touches ~2/3 of the code** — it
cannot be split by module, and the standing accumulator during pass 1 is the per-statement
`VariableData` of every method, which grows with total elements analysed. That is the case this design
targets. (For ES itself, per-module / module-group spreading is the answer and needs none of this.)

## 2. Why the cheap levers are dead (measured)

Two studies (2026-07-22, in `EXTRACT-INTERFACE-CASES.md`): whole-method VD eviction and "evict all but
the last statement" are both UNSOUND. The killer is the data structure: a statement's `VariableData` is
a map `FQN → VariableInfoContainer`, and each VIC holds `previousOrInitial = Either.left(prevVic)` — a
**strong back-reference to the previous statement's VIC for the same variable**
(`VariableInfoContainerImpl.java:28`, populated at `MethodAnalyzer.java:542`). So the last statement's
VICs transitively pin the entire O(statements × variables) chain alive. Dropping intermediate
`VARIABLE_DATA` map entries (a) trips `assert vd != null` in re-linking (`LinkComputerImpl.java:664`,
read every pass) and (b) frees almost nothing, because the chain keeps the payload reachable.

## 3. The lever: break the chain, then drop + regenerate

Three moves, per method, gated:

1. **Flatten** the method's *consumed* VD (the last statement of the main block — the one anchored at
   `methodInfo.analysis()` VARIABLE_DATA, `MethodAnalyzer.java:347`, and read by cross-method consumers)
   into a **back-reference-free snapshot**: replace each VIC with one whose
   `previousOrInitial = Either.right(oldVic.best())` and no eval/merge, so `best()` returns the same
   final `VariableInfo` (same links) but the VIC no longer references the chain.
2. **Drop** the intermediate statements' `VARIABLE_DATA`
   (`statement.analysis().removeIf(p -> p == VARIABLE_DATA)` — the write-once guard is cleared by
   `removeIf`, `PropertyValueMapImpl.java:143`). With both anchors gone (the chain from move 1, the map
   entries from move 2), the intermediate VICs/`VariableInfo`/`Assignments`/`Reads`/`Links` become
   GC-eligible.
3. **Regenerate** before a re-link: worklist passes re-link dirty methods, and re-linking re-reads every
   statement's VD (`LinkComputerImpl.java:663`). Before re-linking a method whose VD was flattened,
   re-run prepwork for that one method to rebuild the full per-statement chain, then re-link, then
   flatten+drop again.

### Memory model

Pass-1 standing accumulator drops from `Σ_methods O(N·M)` to `Σ_methods O(M)` (N = statements/method,
M = live vars): one flattened last-statement VD per method, no chain, plus the *currently-linking*
method's full chain + transient graph. On method-length-heavy code (the 10K-line methods) N is large,
so the reduction is ~N-fold. That is what makes the single-SCC 3M target fit.

### Cost model

Regeneration re-runs prepwork's per-method VD build for each method **each pass it is dirty**. Pass 1
(the memory-critical one) links every method once → no regeneration, full win. Passes 2+ operate on the
worklist, which shrinks fast (framework: 15502 → 5279 → 1006 → 438 → …), so regeneration is bounded and
mostly cheap. Net: memory bought with a bounded recompute in the tail passes. Measure both with the
`AnalysisProgressFeed` heap curve + wall-clock A/B.

## 4. Hazards and their containment (validated)

- **`Either.right` snapshot flips `isInitial()`/`isRecursivelyInitial()` to true.** Blast radius
  (grep, non-test, outside VIC itself): `isInitial` 0 callers, `isRecursivelyInitial` 0,
  `getRecursiveInitial` 1 (`MethodAnalyzer.java:820` — prepwork, runs only on freshly-built chains,
  never on flattened VD), `getPreviousOrInitial` 1, `indexOfDefinition` 6. `best()` (the hot path)
  returns the snapshot value unchanged. So the semantic change is safe as long as flatten runs only on
  the *consumed last statement* and regeneration restores a real chain before any prepwork re-touch.
- **Cross-method reads during the loop** (`FieldAnalyzerImpl` ~:144/:162, `ShadowModificationPass`
  ~:276/:334, extract-interface `CommonAnalyze.java:580`) read the **last** statement's VD/links —
  exactly what flatten preserves. None call `isInitial`. Confirm during Phase 1 they read `best()`/
  links only.
- **Worklist re-link of the same method** needs its intermediates → move 3 regenerates them. A method
  re-linked before its first flatten (still has full chain) skips regeneration (idempotent guard).
- **Consumers that need statement-level VD after `analyze()` returns** (extract-interface `CommonAnalyze`
  and the other refactorings). Flatten keeps only the *last* statement; intermediate statements are
  gone. Any consumer that walks intermediate statements would see them absent → this is why the whole
  feature is **gated OFF by default** and only turned on for a run whose only goal is the analysis
  verdicts (or where a post-`analyze()` full prepwork re-run is acceptable to restore VD).

## 5. Phasing (each phase independently testable)

- **Phase 0 (spike — the load-bearing unknown): DONE (GO), commit `9a0fab0d`.** `PrepAnalyzer.doMethod`
  is per-method re-invocable; `TestVariableDataRegeneration` proves drop (`removeIf` VARIABLE_DATA on
  method + statements) then re-run rebuilds identical per-statement VD.
- **Phase 1 (flatten mechanism, no wiring): DONE, commit `322e2033`.** `VariableInfoContainer.flattened()`
  collapses `Either.left(prevVic)` to `Either.right(getPreviousOrInitial())` keeping own eval/merge;
  `VariableDataImpl.flattened()` maps it. `TestVariableDataFlatten` asserts best()/getPreviousOrInitial/
  indexOfDefinition/hasEvaluation/hasMerge preserved, `isPrevious()==false`, idempotent, over both
  eval-having and previous-only containers.
- **Phase 2 (drop + gate):** `Configuration.flattenVariableData()` default false; after
  `linkComputer.doMethod` in `processElement` (`SingleIterationAnalyzerImpl.java:319`), when enabled and
  this is the method's settled link for the pass, flatten the consumed VD and `removeIf`-drop the
  intermediates. Cross-method-read regression: run the extract-interface stress on framework with the
  gate ON and assert candidate outcomes are unchanged from OFF (CommonAnalyze reads the last statement,
  which survives).
- **Phase 3 (regenerate before re-link):** mark flattened methods; before re-linking a marked dirty
  method, regenerate (Phase 0 mechanism), re-link, re-flatten. Correctness A/B: full analysis with gate
  ON vs OFF must certify the same verdicts on fernflower + guava (mod known non-confluence).
- **Phase 4 (scale):** the `AnalysisProgressFeed` heap curve on `server/main` and a large ES
  module-group with gate ON vs OFF — quantify the peak-heap reduction and the wall-clock cost. Then the
  real target: whether a synthetic large single-SCC fits where it did not.

## 6. Non-goals

Statement-level reuse across runs; changing the link algorithm; the ES-spreading path (separate, works
today). This is purely a heap-footprint change to the analysis of one unsplittable SCC, behind a gate.
