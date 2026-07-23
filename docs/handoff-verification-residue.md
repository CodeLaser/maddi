# Handoff — the verification-pass residue (the gate on the eventual-immutability endgame)

**Audience:** a fresh model implementing this without the 2026-07-22 session context.
**Status:** diagnosed at the symptom level only; the iterating-analyzer internals were NOT dug into.
**Base commit:** `54bd9859` on branch `ws/eventual`. Tree clean.
**Gate: NONE.** Unlike the Part B work (see `docs/handoff-eventual-interface-nonmodification.md`), this is
ungated engine-core work in `IteratingAnalyzerImpl` — every change affects default behavior, so the corpus
byte-identity rule (§5) applies to the change itself, not to a gated no-op.

---

## 0. TL;DR / the one task

On the dogfood run (maddi analyzing its own CST), the iterating analyzer certifies with a
**verification-pass residue of 587 elements** — elements that never complete analysis and keep carried or
absent values. Among them: the whole `Codec` family, `Element`'s anonymous types, and — critically — enough
of the `Expression`/printing machinery that **71 methods end with `nonModifying=null` (undecided)**.

The eventual-immutability arc (Part B, complete at method level — see `docs/eventual-info-hierarchy.md`
§"Part B" through §"Ring 3") is now blocked exclusively by this: a method with undecided `NON_MODIFYING`
can never be excused, the types holding such methods never obtain eventual verdicts, and the greatest-
fixpoint contraction (correctly) retracts everything that leaned on them. **Fix the residue → the
`Expression` hierarchy and `ParameterizedType` become provable → the `Info` flagship family can finally
survive.**

---

## 1. The evidence (dogfood run, base commit)

Recipe in §4. The log shows, at the terminal certification point:

```
ERROR IteratingAnalyzerImpl -- Certification reached with 78 refused downgrade attempt(s) — frozen
  optimistic values survive the fixpoint (see PLAN-modification-reachability):
  {immutableType=2, independentMethod=1, independentType=22, methodLinks=15, unmodifiedParameter=7,
   unmodifiedVariable=31}
INFO  IteratingAnalyzerImpl -- Verification-pass residue (587 elements): org.e2immu.language.cst.api
  .analysis.Codec, Codec.Context, Codec.DI, Codec.DecoderException, ... CompilationUnit.Builder,
  Element.$0, Element.$1, Element.$2, ...
INFO  IteratingAnalyzerImpl -- Verification-pass residue (2 elements): ParameterizedTypeImpl.$9, ...
```

plus the historical note in `dogfood/README.md`: exit code 5 = `ANALYSER_ERROR` ("cycle protection trips on
a few of the printer methods; the analysis results are still written").

Quantified consequence in the FPDUMP (`FPDUMP=<file>` on the run): `grep -c "^method nonModifying=null"`
→ **71** undecided methods. Also `grep -cE "^type"` and the per-type eventual scoreboard queries in the
Part B handoff §8.2.

## 2. Code anchors (base commit `54bd9859`)

- `maddi-modification-analyzer/.../impl/IteratingAnalyzerImpl.java`:
  - ~607: `LOGGER.info("Verification-pass residue ({} elements): {}"...)` — where the residue is reported;
    the surrounding block (~555–620) is the worklist/verification interplay: "elements silently dropped by
    the subset filter — THE residue the verification passes kept", "route through the verification branch
    below instead of stopping here", "never reached to keep their carried values — a full verification pass
    would re-analyze".
  - The "78 refused downgrade attempt(s)" ERROR — `TolerantWrite` refusals surviving certification; the
    breakdown by property is printed. `PLAN-modification-reachability.md` (module root,
    `maddi-modification-analyzer/PLAN-modification-reachability.md`) §14–§17 carries the modreach record and
    is explicitly referenced by the error message.
- `maddi-modification-analyzer/.../impl/SingleIterationAnalyzerImpl.java` — fault tolerance (`failed` set,
  `crashFinding`), the two-pass type loop, the abstract-method batch between passes.
- The crash class historically named: printer methods (`print(...)` across the CST impls) via "cycle
  protection". Whether today's residue is crash-driven, subset-filter-driven, or both is **not yet
  established** — that distinction is the first thing to find out.

## 3. Suggested attack (in order)

1. **Characterize, don't fix, first.** Run the dogfood with whatever debug the residue block offers (or add
   an env-gated dump like `EC_RETRACT_DEBUG`, precedent in `EventualClusterContraction.retract`): for each
   residue element, WHY is it residue — crashed (in `failed`)? never reached by the worklist subset? cycle
   protection? Bucket the 587. The 71 `nonModifying=null` methods are the payload — map each to its bucket.
2. **The crash bucket**: reproduce one printer-method crash in isolation (a unit test on the offending
   method's type), fix the link-computer/cycle-protection cause. `parsing-stability.md` and the linking
   manual are the references; `LinkComputerImpl` cycle handling is the likely region.
3. **The never-reached bucket**: the ~555–620 block already knows about "silently dropped by the subset
   filter"; decide whether the verification pass should force-analyze residue elements to a decided state
   (a `nonModifying` default is NOT acceptable — it must be computed; see `SourceContractMaterializer`'s
   philosophy: modification is computed, never trusted).
4. **The refused-downgrade bucket** (78): read `PLAN-modification-reachability` §14–§17 first; these are
   frozen optimistic values — whether they matter for the 71 methods is part of step 1's mapping.

## 4. Definition of done + the eventual-side measurement

- Dogfood run: residue count → 0 (or a justified, named remainder), exit code no longer 5,
  `nonModifying=null` count → 0 (or named remainder).
- Then re-run the **gate-ON** dogfood (recipe below) with `EC_RETRACT_DEBUG=1` and read the broken-roots
  list: `ParameterizedType` and `Expression` entries should shrink or vanish; every type they unblock is
  measured by the survivors count (`grep -cE '^type .*eventual=@' <fpdump>`, base value **8**) and the
  contraction line (base value **36 retracted**).

```
AAPI=$PWD/maddi-aapi-archive/src/main/resources/org/e2immu/analyzer/aapi/archive/analyzedPackageFiles
EVENTUALCLUSTER=1 EC_RETRACT_DEBUG=1 FPDUMP=/tmp/fp.txt ./gradlew :maddi-run-openjdk:run --args="\
  --input-configuration $PWD/dogfood/cst-impl/build/inputConfiguration.json \
  --preload-analysis-results-dirs $AAPI/jdk,$AAPI/libs/test,$AAPI/libs/log \
  --analysis-steps prep,modification --analysis-results-dir /tmp/fp_out"
```
(Regenerate the input configuration per `dogfood/README.md` if missing. Compare aggregates only, never
per-file JSON.)

## 5. The golden rule, for UNGATED work

Any engine change here is accepted only with a **byte-identical Fernflower FPDUMP A/B** against base — or a
diff that is line-by-line justified as strictly-better verdicts (residue elements gaining decided values is
exactly such a diff; expect it, review it, and run Timefold + Langchain4j as well before merging):

```
FPDUMP=/tmp/B.txt ./gradlew :maddi-run-openjdk:slowTest --tests "...TestFernflower" --rerun-tasks
# A side: git checkout 54bd9859 (clean tree), same command → /tmp/A.txt; back to ws/eventual
sort /tmp/A.txt > /tmp/A.s; sort /tmp/B.txt > /tmp/B.s; diff /tmp/A.s /tmp/B.s
```
Known base flake: `StatEdge.EdgeType.<init>(int)` nonModifying flips between two base runs — when the diff
shows exactly that line, re-run the A side and compare again (technique proven 2026-07-22: A-vs-A2 showed
the same flip, B was byte-identical to A).

A green `slowTest` can be cached/vacuous — `--rerun-tasks`, then check the roll-call
(`tests="1" failures="0"` in the XML) and that the FPDUMP is non-empty. (`AGENTS.md` §Commands.)

## 6. What NOT to do

- Do not default/contract `NON_MODIFYING` on residue methods to unblock the eventual arc — modification is
  computed, never assumed (repo-wide philosophy).
- Do not weaken `TolerantWrite` or the contraction to make numbers move; the eventual side is behind
  `EVENTUALCLUSTER` and is not the patient here.
- Do not diagnose from a single green run — re-run; the engine has known run-to-run nondeterminism.
