# Modification link + analyzer hardening roadmap (real-world robustness)

How we harden `maddi-modification-link` and `maddi-modification-analyzer` against **real-world code**, from a
two-part code audit on 2026-07-14. Companion to `prep-analyzer hardening.md` (same house style) — read that first;
the prep stage is already hardened and is the **template** for what follows.

## Status / framing

- **Scope is robustness, not outcome.** "Never crash / degrade gracefully on real code" is the target here.
  The *correctness* of the link relations and the modification/immutability verdicts is owned by other threads;
  this roadmap deliberately does **not** touch analysis logic.
- **Ownership split (decisive — see [[maddi-branches]]):**
  - `maddi-modification-link` is owned by the **`sv-integration`** thread (separate checkout `~/git/maddi`),
    which stays a distinct branch for the coming days. **Do not fix link internals on the `kotlin` trunk** —
    route link findings there as notes + distilled reproducers (the precedent is the get/set bug report already
    filed into its tree).
  - `maddi-modification-analyzer` was merged into `kotlin` from `openjdk` (the `nolink` suite + `immutableSuper`
    fix). It lives here, so analyzer-boundary and run-boundary work can land on the trunk — **but only as
    fault-isolation wrappers, never as analysis-logic changes** (exactly the line the prepwork error-model work
    respected).
- **This mirrors three prior wins**, and reuses their machinery:
  1. the openjdk **JDK-module survey** that shook out front-end crashes (methodology);
  2. the prepwork **`Options.faultTolerant` + `exceptions()` collect-and-continue** (the fault-isolation pattern);
  3. the analyzer **`nolink` property tests** — assert final properties, not the link representation
     (the independent-test pattern).
- **The surfacing half of §2 is now on the trunk.** The **guard** stack (contract verification) was merged into
  `kotlin` on 2026-07-14 (`3214206d`, from `~/git/maddi-aapi @ guard-mode`; this thread now owns it). It added a
  **structured-finding collector threaded through the analyzer** and rendered by `ErrorReport`:
  - `Message` (`maddi-cst-api/.../analysis/Message.java`) is now a structured finding — `Severity` (WARN/ERROR),
    `source()`, `info()`, `category()` (kebab-case, e.g. `"contract-violation"`), and `causes()` (a nested
    evidence/"why" chain).
  - a `List<Message> messages = Collections.synchronizedList(...)` is injected into every sub-analyzer alongside
    `propertiesChanged` (`impl/SingleIterationAnalyzerImpl.java`), exposed via `IteratingAnalyzer.messages()` /
    `SingleIterationAnalyzer.messages()`.
  - `ErrorReport` gained a third parameter enumerating analysis findings (category, `uri:line-col`, indented
    `because:` cause chains, errors-first, warnings capped at 50); both runners feed it and map ERROR findings to
    `EXIT_ANALYSER_ERROR`.
  This is exactly the collector + surfacing §2 needs — so §2 becomes a *small*, trunk-ownable addition on top of
  the merged collector, not a new parallel channel. See §2.

Severity: **H** high, **M** medium, **L** low. Tags: **(K)** ownable on the kotlin trunk · **(L→sv)** route to
`sv-integration`.

---

## 0. The core structural gap — no fault isolation below prep  (H)

Prep is the *only* fault-tolerant stage. Everything downstream aborts the whole run on the first bad `Info`:

- **Link:** `LinkComputerImpl.doType` (`impl/LinkComputerImpl.java:146-149`) iterates methods with **no**
  try/catch; `doMethod` (`:157-166`) and `doBlock` (`:423-445`) each `LOGGER.error(...)` then **rethrow**.
  There is **no** graceful flag — `LinkComputer.Options` (`LinkComputer.java:22-61`) is
  `record Options(recurse, forceShallow, checkDuplicateNames, trackObjectCreations)`, no fault-tolerant member.
- **Analyzer:** `SingleIterationAnalyzerImpl.go` (`impl/SingleIterationAnalyzerImpl.java:84-108`) wraps the
  per-`Info` loop in `catch (RuntimeException | AssertionError e) { LOGGER.error(...); throw e; }` — **rethrows**.
  `IteratingAnalyzerImpl.analyze` (`impl/IteratingAnalyzerImpl.java:77-101`) has no catch at all.
- **Runner asymmetry:** the openjdk `RunAnalyzer` collects and *reports* prep failures per-item and continues
  (`maddi-run-openjdk/.../RunAnalyzer.java:178-204`, prep built with `setFaultTolerant(true)`), but the analyzer
  call is terminal: one throwable from `analyzer.analyze(order)` sets `EXIT_ANALYSER_ERROR` and ends the run
  (`RunAnalyzer.java:227-238`). The in-house `maddi-run-main/.../RunAnalyzer.java` builds prep **without** fault
  tolerance (`:164`).

Net: **one unsupported expression anywhere in a project = zero analysis results for the whole project.** Closing
this gap is the precondition for everything else (a survey can't even enumerate crashes until a run survives the
first one).

---

## 1. Real-code survey harness — the missing measurement  (H) (K)

Today **every** link and analyzer test parses curated hand-written snippets, with real JDK types only as *shallow*
library dependencies (link `CommonTest`; analyzer `nolink` `CommonTest.java:100-105`). Nothing runs the **full
pipeline over a real corpus**, so the crash sites in §3 are only ever exercised against shapes we already thought
of. The two closest things fall short:

- `TestCloneBench` (`maddi-modification-analyzer/.../clonebench/TestCloneBench.java`) — runs the *full* analyzer
  over a multi-directory corpus + JDK `jmod:`s, parse-once then analyze-in-parallel. Closest we have, but it is a
  green/red **test** (aborts on first crash), not a crash **census**.
- `RunMixedPrepAnalyzer` (`maddi-run-kotlin/.../RunMixedPrepAnalyzer.java`) — surveys real projects but **stops
  after prep** ("modification analysis has open issues on real code, handled elsewhere", `:40-43`).

- [ ] **H** Build a **full-pipeline survey** (parse → prep → link → analyzer) that runs fault-tolerantly over big
  real corpora (JDK modules, a handful of real third-party jars) and, instead of failing, **buckets every crash**
  by `(exception type, throwing class:line, offending Info, minimal source span)`. Model it on the `TestCloneBench`
  loader/parallel harness + the `RunMixedPrepAnalyzer` survey `Main`. Output = a ranked crash inventory that feeds
  §3/§4. This is a **runner concern**, so it lands on the trunk.
- [ ] **M** Run the survey **single-fork / one javac task per thread** (or bucket the javac-concurrency failures
  separately): the inspector is not concurrency-safe and throws the `tree.starImportScope is null` NPE under
  parallel execution — a known, documented flake (`maddi-inspection-openjdk/calling-the-javainspector.md` §1),
  not a link/analyzer bug. Do not let it pollute the census.
- [ ] **L** Persist the inventory (one row per distinct crash signature) so re-runs show progress, and so a crash
  fixed on `sv-integration` can be ticked off here after merge-back.
- [ ] **M** **Split the `modification` step into `link` and `analyzer` sub-steps** (front-end for the survey, and
  a handy CLI lever for the exact "run over the JVM modules / kotlin-stdlib" case). `ANALYSIS_STEPS`
  (`maddi-run-openjdk/.../Main.java:60-63`, values `none`/`prep`/`modification`/`rewire-tests`) is today a coarse
  *phase* selector, and `modification` **fuses link and the analyzer** — the runner never calls `LinkComputer`
  directly; link runs inside `SingleIterationAnalyzerImpl`'s per-`Info` loop. A `link` step would drive
  `LinkComputerImpl.doMethod` directly (the shape the `linking-manual.md` §8 probe already uses), letting the
  survey (a) **attribute** a crash to *link* vs. the *verdict* analyzer, and (b) exercise link over a whole corpus
  without the iterating analyzer on top. Notes:
  - **Gated behind §2 — do not add this first.** Step granularity only turns a phase *off*; it does **not** let a
    phase survive a crash. Over the JVM modules / kotlin-stdlib, a `link`-only step still dies on the first bad
    method and yields one crash's worth of coverage. Only with §2's collect-and-continue does
    `--analysis-steps=link` + fault-tolerant produce a clean per-method link crash census (i.e. *this* inventory).
    The coarse `none`/`prep` steps already give the important lever — surveying front-end + prep without ever
    entering link/analyzer, which is why `RunMixedPrepAnalyzer` is prep-only.
  - **Small wiring change, at the analyzer/link seam `sv-integration` owns** (exposing link outside the analyzer
    loop) — coordinate; keep it additive.

---

## 2. Fault isolation at the analyzer / run boundary  (H) (K — trunk, on the merged collector)

> **DONE (2026-07-14).** `IteratingAnalyzer.Configuration.faultTolerant()` (default false); when on,
> `SingleIterationAnalyzerImpl.go`'s per-`Info` loop (plus the abstract-method batch and the 2nd type pass)
> catches `RuntimeException | AssertionError | StackOverflowError`, emits an ERROR finding
> (`analyzer-crash` / `link-crash`, blamed on the `Info`, located) into the guard collector, records the `Info`
> as failed so it is not retried, and continues. Both production runners flip it on; the findings surface through
> the existing `messages()` → `ErrorReport` → `EXIT_ANALYSER_ERROR` path. Test `TestFaultIsolation` injects a
> deterministic crash (an un-prepped method → link `assert vd != null`) and proves the good method is still
> analyzed; default stays fail-fast. Because link runs inside the per-`Info` loop, this isolates link crashes too.

**Do not build a parallel prep-style `exceptions()` channel.** The merged guard collector already threads the
right vehicle: a `List<Message>` through every sub-analyzer, exposed via `IteratingAnalyzer.messages()` and
rendered by the extended `ErrorReport` (see framing; `3214206d`). §2 is the *crash* producer for that existing
*findings* channel — a small catch-and-emit in the per-`Info` loop. **Pure wrapper — no analysis logic**, which is
the one modification-subsystem change the ownership rule permits.

- [ ] **H** `SingleIterationAnalyzerImpl.go` (the per-`Info` loop; today `catch (RuntimeException | AssertionError)
  { LOGGER.error(...); throw e; }`, `:84-108` on the kotlin trunk / `guard-mode`): on a `faultTolerant` flag, turn
  the catch into **emit a `Message`** — `Severity.ERROR`, `category("analyzer-crash")` (or `"link-crash"` when the
  throwable comes from `LinkComputerImpl`), `info()` = the offending `Info`, `source()` derived from it,
  `message()` = the exception summary, `causes()` optionally carrying the trimmed stack — into the **same `messages`
  collector** guard injected, then **continue** to the next `Info`. No new type, no `AnalyzerException` revival;
  reuse the guard `Message` path end to end.
- [ ] **H** Because **link runs *inside* the analyzer's per-method loop** (`SingleIterationAnalyzerImpl.java:55,89`
  → `LinkComputerImpl.doMethod`; the runner never calls link directly), this single catch *also* isolates link
  crashes: a method whose linking throws is recorded and skipped, the type's other methods still analyze. Highest-
  leverage robustness change here — it converts "1 crash = 0 results" into "1 crash = 1 skipped method," and the
  crash lands in the survey (§1) as one more categorized finding.
- [ ] **M** Config + surfacing: thread a `faultTolerant` flag through `IteratingAnalyzer.Configuration`
  (`ConfigurationBuilder`); the runners already feed `IteratingAnalyzer.messages()` into `ErrorReport` and map
  ERROR findings to `EXIT_ANALYSER_ERROR` (guard's `RunAnalyzer` deltas), so crash findings surface **after** the
  run completes rather than aborting it — no extra runner wiring beyond the flag. Also flip the in-house runner's
  prep to fault-tolerant (`maddi-run-main/.../RunAnalyzer.java:164`).
- [ ] **M** Keep the default **`faultTolerant=false`** so unit tests and direct callers keep fail-fast (prep's
  choice); the survey (§1) and the production runners flip it on.
- **Now unblocked and trunk-ownable.** The collector, `IteratingAnalyzer.messages()`, the `ErrorReport` findings
  surface and the ERROR→`EXIT_ANALYSER_ERROR` mapping are all merged (`3214206d`), so §2 is just the per-`Info`
  catch-and-emit + the `faultTolerant` flag on top — no cross-branch coordination left. The one remaining external
  seam is `LinkComputerImpl` (owned by `sv-integration`): distinguishing a `link-crash` from an `analyzer-crash`
  reads the throwable's origin but changes nothing in link.

---

## 3. Link-internal graceful degradation  (→ sv)

**Do not fix on the trunk.** File as notes + minimal reproducers (distilled from the §1 survey) into the
`sv-integration` tree, ranked by the recon's real-world crash likelihood. Sites are in
`maddi-modification-link/src/main/java/org/e2immu/analyzer/modification/link/`:

- [ ] **H** `impl/ExpressionVisitor.java:87` — the `visit(...)` switch `default -> throw new
  UnsupportedOperationException("Implement: " + …)`. Any CST expression kind not enumerated aborts the method.
  Proposal: degrade to an empty `Result` (+ a recorded diagnostic) rather than throw. This is the single biggest
  real-world crash risk.
- [ ] **H** `impl/graph/LinkGraph.java:41` — `throw new UnsupportedOperationException("cycle protection")` after a
  hard 20-iteration cap (comment admits maddi's own code approaches it). Proposal: fall back to shallow linking for
  that method rather than abort. Regression already exists (`impl2/Test2` "cycle protection").
- [ ] **M** Name/arity/structural assumptions that real code violates:
  - `impl/LinkMethodCall.java:275` — `assert simpleName.endsWith("s")` then singularizes a varargs param name;
    `T... data` breaks it (mirror at `impl/LinkFunctionalInterface.java:255`).
  - `impl/LinkMethodCall.java:132` — **hard-coded** `packageName().startsWith("io.codelaser")` library check
    (self-flagged FIXME); wrong for every non-codelaser project.
  - `impl/ShallowMethodLinkComputer.java:544` — `assert parameters().size() == 2`.
  - `impl/ExpressionVisitor.java:451,465` — unguarded `(VariableExpression) rTarget.getEvaluated()` casts in
    `assignment`; a non-plain LHS → `ClassCastException`.
  - `impl/ExpressionVisitor.java:469` — `assert cc.object()==null … : "NYI"` — `outer.new Inner()` is legal Java.
  - `impl/translate/VirtualFieldTranslationMapForMethodParameters.java:84` and
    `impl/LinkFunctionalInterface.java:313` — generic-resolution / field-match fall-throughs `throw new
    UnsupportedOperationException()`; real generic code can reach them.
- [ ] **M** Assert-guards-then-dereference (crash under test `-ea`, silent-then-NPE without it in prod):
  `impl/ShallowMethodLinkComputer.java:594` (`assert sam != null` then `sam.typeInfo()`),
  `impl/LinkMethodCall.java:68,151`, `impl/graph/LinkGraph.java:91`, and the `getFirst()/getLast()` on decoded
  lists in `impl/MethodLinkedVariablesImpl.java:63-91` / `io/LinkCodec.java`. Convert load-bearing ones to real
  validation or graceful degrade.
- Note the many `// FIXME`/`// TODO`/`// shaky`/`// hard-coded` markers the recon catalogued
  (`LinkGraph.java:112`, `MakeGraph.java:60`, `FollowGraph.java:34`, `MethodLinkedVariablesImpl.java:150`,
  `ShallowMethodLinkComputer.java:68,170,351`, …) — these are the author's own robustness debt list.

---

## 4. Independent, property-level regression tests  (both)

Extend the `nolink` pattern — **assert computed properties, not the link representation** (its stated design:
`nolink/TestContainer.java:31-35`, `nolink/TestIndependent.java:34-40`) — with the real-world shapes the survey
surfaces.

- [ ] **M (K)** Analyzer: for each survey crash whose fix is an analyzer/boundary concern, add a distilled snippet
  to `nolink/` asserting the run *completes* and the surrounding `Info`s still get their properties (i.e. the
  fault-isolation actually isolated). Lands on the trunk.
- [ ] **M (L→sv)** Link: file each link-internal reproducer as a failing `impl/`/`typelink/` snippet **in the
  `sv-integration` tree** (they own the fix + the green-baseline discipline noted in `linking-manual.md` §8).
- [ ] **L** Guard the "no new hard-throw" invariant: a lint/test that fails if `ExpressionVisitor`'s switch grows a
  new `throw` default, once §3.1 degrades it.

---

## Suggested sequencing (who does what)

1. **§2 fault isolation — DONE (2026-07-14).** The enabler: production runs no longer die on the first crash
   *and* the §1 survey can complete instead of halting at crash #1. See §2 for the shape; `TestFaultIsolation`
   pins it. Next up is §1 (the survey harness), which now actually completes a run over a real corpus.
2. **§1 survey harness (K).** Produces the ranked crash inventory — the measurement everything else is triaged
   against.
3. **Triage the inventory:** analyzer/boundary crashes → §2/§4 on the trunk; link-internal crashes → §3/§4 notes +
   reproducers into `sv-integration`.
4. **Merge-back:** as `sv-integration` lands link fixes, merge into `kotlin` (per [[maddi-branches]]) and tick the
   §1 inventory rows.

## Risks / boundaries

- **Ownership is the hard rule:** no link analysis-logic edits on the trunk; §2 must be a pure isolation wrapper.
- **Parallel-mode contamination:** the javac front-end is not thread-safe (`calling-the-javainspector.md` §1);
  a parallel survey will manufacture phantom `starImportScope` NPEs — run single-fork or bucket them out.
- `LinkGraph`'s duplicate-name assert (`:45`) is already disabled in the `PRODUCTION` preset — don't "re-enable
  for safety."
- The `IteratingAnalyzer.Configuration` `faultTolerant` addition is now a trunk change: the guard collector +
  `ErrorReport` findings surface are merged (`3214206d`), so §2 rides on them rather than re-inventing a channel.
