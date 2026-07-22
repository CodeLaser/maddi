# Handoff — the verification-pass residue (the gate on the eventual-immutability endgame)

> **2026-07-22, second session: CHARACTERIZATION COMPLETE — read §7 first.** The bucket structure
> assumed below (§0–§3) turned out to be wrong in instructive ways; §7 carries the true diagnosis
> (recursion pessimism at the `isNonModifying` undecided-callee default), the minimal repro
> (`TestRecursionThroughAbstract`), the measurements, and the two candidate fix designs awaiting a
> decision. §0–§6 are kept as the record of what we believed going in.

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

---

## 7. Characterization (2026-07-22, second session) — the true diagnosis

Every finding below is from dogfood runs at `38ec1152` plus env-gated, log-only diagnostics
(committed with this section: `FPDUMP_PARAMS`, `MODREACH_DEBUG`, `MODREACH_EXPLAIN`).

### 7.1 The assumed buckets do not exist

- **No crash bucket.** The baseline run has zero crashes, zero cycle-protection trips, zero
  StackOverflows; `SingleIterationAnalyzerImpl.failed` stays empty. The `dogfood/README.md` note
  ("cycle protection trips on printer methods") is stale: **exit code 5 comes from the guard
  analyzer** — 138 (base) / 243 (MODREACH) `contract-violation` findings, dominated by
  "`parameter 'qualification' of *.print(Qualification) is modified`" against the `@Container`
  contract on `Element`, plus `Element` methods computing as modifying against its `@Immutable`
  contract. Those violations are *symptoms of the same pessimism diagnosed in §7.3*.
- **The 587-element "residue" is mostly an artifact.** The genuine pre-cycle-breaking residue is
  **2 elements** (`ParameterizedTypeImpl.$9`, a lambda, plus its `apply`), with a
  `set:variablesLinkedToObject@LCI=55` churn on every full pass. The 587 appear at iteration 14
  because cycle breaking activates *mid-verification* and decides 481 immutability-undecided types;
  the residue log then reports the (expected) summary fallout as if it were adjacency-gap churn.
  The reporting is misleading, not the convergence.
- **The 71 `nonModifying=null` methods are all abstract cst-api methods whose implementations are
  outside the dogfood source set**: the `Codec` family (46), `FormattingOptions` (10), the
  printing/factory interfaces (~10), `FingerPrint.isNoFingerPrint`, `TypeInfo.Builder.source()`,
  `Factory.parameterizedTypeWildcard()`, `TranslationMap.ModificationTimesHandler`. Nothing ever
  writes `NON_MODIFYING_METHOD` on a no-impl abstract method; cycle breaking's
  `NO_INFORMATION_IS_NON_MODIFYING` is consulted only inside type-immutability/independence.
  **`MODREACH=1` decides all 71 (`71 null->TRUE`, frontier-complete unreached), so this bucket is
  already solved by the gated shadow pass.**

### 7.2 What actually blocks `ParameterizedType` / the `Expression` hierarchy

`ParameterizedType`'s five holdouts (`print`×2 via the interface, `rewire`×2, `concreteSuperType`,
`mostSpecific`, `replaceByTypeBounds`) are **`nonModifying=false` — decided modifying, not
undecided**. The chain (established with `FPDUMP_PARAMS`):

`ParameterizedTypeImpl.print(q,…)` hands `this` to `ParameterizedTypePrinter.print(...)` (static,
in scope, itself `nonModifying=true`) whose `parameterizedType` **parameter** is
`unmodified=false` ← it hands `parameterizedType.typeInfo()` to `TypeNameImpl.typeName(...)` whose
`typeInfo` parameter is `unmodified=false` ← `typeName` only calls six `TypeInfo` accessors, of
which `TypeInfoImpl.packageName()` / `descriptor()` / `fromPrimaryTypeDownwards()` are
`nonModifying=false` ← each is a pure read that **recurses through the abstract declaration**
(`compilationUnitOrEnclosingType.getRight().packageName()`).

### 7.3 Root cause: the undecided-callee default makes recursion permanently modifying

`MethodInfoImpl.isNonModifying()` (cst-impl:402) is
`getOrDefault(NON_MODIFYING_METHOD, FALSE).isTrue()` — **undecided reads as modifying** at every
call site (`MethodModification.go`). For any recursive method — even direct self-recursion of a
pure function, no interface involved — the first evaluation sees its own callee undecided, marks
the receiver (and receiver-rooted field) modified, the summary freezes that, the abstract batch
aggregates the impl's FALSE, and the result is a self-consistent pessimistic fixpoint that no
later iteration can leave (the write discipline is monotone; no downgrade is ever *attempted*, so
this never even shows among refused downgrades). Minimal repro pinned in
`maddi-modification-analyzer/.../modification/TestRecursionThroughAbstract.java`:
`direct=false C.name=false I.name=false`, all three of which should be `true`.

The shadow pass cannot repair this class because it **seeds from the converged summaries**
("seeded by `TypeInfoImpl.packageName()` modified `this.compilationUnitOrEnclosingType`" — the
`MODREACH_EXPLAIN` output verbatim), and abstract in-order methods additionally seed from their
frozen FALSE. The poison re-seeds itself; the node is "reached" by its own seed, so it is neither
a divergence nor a reverse divergence. Separately, the dogfood has **281 unique REVERSE
divergences** (fixpoint modified, shadow unreached: 433/356/4 field/parameter/method dump lines
across rounds — `MODREACH_DEBUG`), a class the pass doctrine calls "a bug in this pass" and keeps
FALSE conservatively; on this interface-heavy code they are largely downstream of the same
pessimism (the `qualification` parameters of the whole print family are in there).

### 7.4 Measurements (dogfood, cst-api+analysis+impl, preloaded aapi)

| run | nonModifying=null | survivors (`eventual=@`) | retracted | enm labels | exit |
|---|---|---|---|---|---|
| base (no gates) | 71 | – | – | – | 5 |
| `EVENTUALCLUSTER=1` | 71 | 8 | 36 | 414 | 5 |
| `MODREACH=1` | **0** | – | – | – | 5 |
| `MODREACH=1 EVENTUALCLUSTER=1` | **0** | **5** | **59** | 522 | 5 |

MODREACH alone: `1404 TRUE->FALSE` honest downgrades, `71 null->TRUE`, `231 reverse kept` (round 1).
Composed, the eventual scoreboard gets **worse** (5 survivors, 59 retractions): the honest
downgrades remove non-modifying verdicts the optimistic eventual seed was leaning on. The label
count rising 414→522 while survivors fall shows the method-level machinery is fine — the
type-level verdicts die on the recursion pessimism. **Fixing §7.3 is the entire game.**

### 7.5 Candidate fixes (decision needed — this is where the next session starts)

**A. Shadow-pass trust-model refinement (recommended; stays behind the MODREACH gate).**
Two coupled changes: (1) *primitive seeding* — stop blind-seeding every summary-modified entry of
analyzed methods; seed only evidence the reachability graph cannot re-derive (degraded bodies,
statement-level assignment evidence, calls to NON-analyzed callees with modifying/`@Modified`
contracts, the E7/marker channels), and let E1/E2/E6 edges re-derive analyzed-callee effects;
(2) *reverse upgrade* — in `writeVerdicts`, treat frontier-complete unreached frozen-FALSE nodes
exactly like the `null->TRUE` writes the pass already performs (the "reverse = pass bug" doctrine
is falsified on interface-heavy code). Corpus-inert by construction (pass only runs under
`MODREACH`); the honest cost is re-pinning `TestShadowCloneBench` / `TestShadowModificationPass`
baselines and re-running the §17 rollout table. Interacts with the default-ON decision, which is
Bart's call — hence this handoff stops here.

**B. Fixpoint-side optimistic recursion (not recommended).** Treating an undecided in-order callee
as non-modifying at the call site needs TRUE→FALSE downgrades when the callee later decides
modifying — exactly the write direction the monotone discipline forbids and the reason MODREACH
exists as post-convergence single writer. Reopening that would fight the §14 architecture.

Cosmetic side-quests, independent of the decision: silence the residue INFO for the first pass
after cycle-breaking activation (it reports expected fallout as churn); refresh the stale exit-5
note in `dogfood/README.md`.

---

## 8. Design A implemented (2026-07-22, same session — approved by Bart)

The §7.5-A redesign is in `ShadowModificationPass`, MODREACH-gated as before. What landed:

1. **Primitive seeding.** Methods whose body the walk fully sees (no bound method references,
   anonymous classes, or local type declarations; lambda bodies ARE walked, unbound `Type::m`
   references are transparent) no longer seed their receiver-rooted summary entries (This,
   this-scoped FieldReferences, own parameters). Replacement evidence: direct assignments (field
   rebinding → owner graph; array-element stores → array object, construction excluded), boundary
   callees (outside the analysis order: modifying/`@Modified` verdicts incl. the conservative
   no-information default; the seeded callee nodes travel over the existing E1/E2 edges), the
   conservative mirror for **undecided abstract in-order callees** (the SAM shape), and an E1
   fallback connecting a boundary callee's `@Modified` parameters to syntactically projected
   arguments when argument links are absent. Opaque bodies keep full summary seeding (E7 eager
   creation-site attributions live there).
2. **Abstract seeding by evidence class.** Frozen FALSE on an abstract seeds only when E6 cannot
   re-derive it: an implementation outside the order **or not connected by overrides()** (a
   method-reference implementation is in IMPLEMENTATIONS but has no override relation), or an
   explicit source `@Modified`. All-impls-in-order aggregations are left to the E6 edges — this
   alone un-poisons the `TypeInfo.packageName` class. No-impl abstracts stay unseeded when
   undecided (preserves the validated null→TRUE cutover for the Codec class).
3. **Reverse upgrade.** `writeVerdicts` now upgrades frontier-complete unreached FALSE nodes to
   TRUE (`REVERSE_UPGRADED`, in the joint-fixpoint trigger), retiring the "reverse divergence =
   pass bug" doctrine; tainted nodes keep FALSE. `TestShadowModificationPass`'s forwarding-hop
   pin re-pinned with justification (the engine's own frozen state was internally inconsistent:
   `apply:0` unmodified TRUE vs stale `run:td` FALSE).
4. **`@IgnoreModifications` mirror** in all projections (was missing entirely — MODREACH had been
   downgrading `InfoImpl.propertyValueMap` to modified, sinking the eventual flagship store), and
   an **immutable-variable cut** in `project()` (an immutable-typed face contributes no nodes —
   the projection-layer mirror of the closure cut / writer guard).
5. **`Either.isLeft()/isRight()` got `@NotModified`** in maddi-support (the only ungated change;
   style-consistent with getLeft/getRight — the jar's byte-code annotations are its aapi, and the
   missing contract made every `Either`-discriminated accessor conservatively modifying).
6. Diagnostics: `SHADOW_SEEDS` (env-gated seed dump with origins).

**Dogfood results (MODREACH=1):** seeds 2638→~1085, reverse-kept 231→0, joint fixpoint clean at
round 3 with 0 reverses; ~317+25 FALSE→TRUE upgrades; `nonModifying=null` stays 0;
`TestRecursionThroughAbstract.testModReach` all-true; `TypeInfoImpl.packageName()` /
`fromPrimaryTypeDownwards()` / abstract `TypeInfo.packageName()` now TRUE. `descriptor()` and the
`print` family stay FALSE — **correctly**: their chains run through `inspection.get()` (the
pre-mark modification) and genuinely-modifying jar boundaries; they are the EVENTUAL layer's job.

**The reframed endgame:** composing `MODREACH=1 EVENTUALCLUSTER=1` still nets fewer survivors
(4, retracted 61; `InfoImpl` survives for the first time) because modreach's ~1586 honest
TRUE→FALSE downgrades hand the eventual layer more methods than `commitLabels` currently excuses.
The next front is therefore **Part B coverage against the honest (modreach) modification state**
— plus one open engine question: `Stream.map` (and friends) seeding as "non-analyzed modifying
callee" despite the preloaded jdk aapi (suspected per-sourceSet Info identity mismatch at the
preload boundary; see `MODREACH_EXPLAIN` chains through `SetOfMethodInfoImpl.nice()`).
