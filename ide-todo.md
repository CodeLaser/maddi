# IDE front-ends — TODO

Work queued for the IntelliJ and Eclipse plugins and the daemon they share. Current state of what exists is
in `eclipse-plugin-state.md` (Eclipse) and the plugin sources; this file is only what is *not* done.

Ordered by what a user on a large project would feel first.

---

## 1. Partial re-analysis, once rewiring is finished

**Why:** on larger projects the analysis is slow, and today *every* trigger re-analyses the whole project
from scratch. A one-line edit costs a full run. This is the single biggest win available.

**Blocked on:** the rewiring work (`rewiring.md`). That is what makes it possible to rebuild only the changed
type and what is downstream of it, keeping everything upstream as the *same objects*. Status there:
openjdk inspector complete and running end to end; Kotlin inspector not started. Also relevant:
`partial-reparse-rewire.md`.

**What has to change in the daemon.** `WarmAnalysisService.analyze` constructs a **fresh**
`JavaInspectorImpl(true, false)` per request and re-parses everything. Partial re-analysis means the opposite:
keep the inspector alive across requests, and hand it the set of changed files so it can classify each primary
type `UNCHANGED` / `INVALID` / `REWIRE` and rebuild only what must be rebuilt. Consequences to design for:

- the protocol needs a way to say *what changed* — today `analyzeProject` carries a whole project config and
  nothing else. Either a new frame, or changed-file paths on the existing one.
- the daemon becomes stateful per project. It currently holds nothing between requests, which is why it is
  robust; that property is being traded away, so decide deliberately when to fall back to a cold run
  (config changed, classpath changed, first request, anything unexpected).
- keep **all** maddi work on one thread: the runtime cache is not thread-safe. The existing heartbeat thread
  only emits status frames and touches no maddi state; anything new must respect the same rule.
- the IDEs already know what changed — IntelliJ from `MaddiCompilationListener`, Eclipse from
  `MaddiBuildListener`'s `IResourceDelta` — but both currently discard that and ask for a whole-project run.

---

## 2. Progress, and "still running"

Three separate things, with very different costs. Worth not conflating them.

**(a) Background computation — already done, both sides.** Eclipse runs the analysis in a `Job` off the UI
thread (`MaddiAnalysis`), IntelliJ in a `Task.Backgroundable` (`MaddiAnalysisService`). Neither blocks the
editor. Nothing to do here unless something is observed to block.

**(b) "Still running" — on the wire already; Eclipse throws it away.** The daemon emits a `status` frame every
2 s during the long analysis phase (`WarmAnalysisService.runWithHeartbeat`), plus one per phase transition
(initialize / hints / parse / prep / order / analyze / collect).

- IntelliJ consumes them: `daemon.analyze(..., status -> indicator.setText("maddi: " + phase))`.
- **Eclipse discards them**: `daemon.analyze("req-" + …, config, status -> { })`, and its `IProgressMonitor`
  is unused. So an Eclipse user gets no indication at all beyond the Job spinner.

Cheap and worth doing now: feed the frames into the `IProgressMonitor` (`subTask` per phase), and use the
monitor for cancellation — the protocol has a `cancel` frame that no front-end has ever sent.

**(c) Determinate progress (n of m types) — needs analyzer support.** The `status` frame already carries
`typesDone` / `typesTotal`, and `typesTotal` is populated, but **`typesDone` is always null during analysis**:
the heartbeat is a liveness ping, not a counter. `IteratingAnalyzer.analyze(order)` is one blocking call with
no per-element callback, so the daemon has nothing to count. Real progress means giving the analyzer a
progress listener — analyzer work, not IDE plumbing. Do (b) first; it may be enough.

---

## 3. Smaller, independent of the above

- **Coalesced triggers are dropped silently.** Both front-ends guard with a bare `AtomicBoolean`
  (`MaddiAnalysis.RUNNING`), so a build finishing while a run is in flight is simply never computed. Needs a
  pending flag and a re-run, not a second concurrent analysis.
- **Whole-workspace marker churn (Eclipse).** `MaddiMarkers.apply` deletes every maddi marker at
  `DEPTH_INFINITE` from the workspace root and recreates them all, on every run. Wasteful now; wrong once
  analysis is partial, since a partial result must not erase markers for files it did not look at.
- **No builder / project nature (Eclipse).** Analysis is a resource-change listener, so there is no delta
  scoping. Related to 1: a real builder is the idiomatic place to hook incremental analysis.
- **Quick fixes and richer hover (Eclipse).** IntelliJ has an external annotator with a why-chain tooltip;
  Eclipse has only the marker message. No quick fixes on either side.
- **GUI install path unverified.** The p2 site is verified by installing with the director; nobody has driven
  `Help > Install New Software` by hand, so the license page and category rendering are unconfirmed.
