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

## 3. Stream early results, instead of waiting for the run to finish

Better than a progress bar, and the same analyzer hook pays for both.

**The observation.** The analyzer is incremental, and the *shape* of the run is lopsided: the majority of the
output is hard-decided in the first iteration, and the tail is long in duration but short in number of
decisions. `IteratingAnalyzer.analyze`'s own javadoc says as much — worklist narrowing (default on) makes
"iterations 2+ only re-analyze elements that changed in the previous iteration plus their dependents". So a
user waiting for the terminal `result` is waiting mostly for decisions that were already made, about elements
they are probably not even looking at.

**The idea.** Emit decisions to the IDE as they harden, so hints and findings appear early and the tail merely
refines them. On a large project this changes the feel completely: the file on screen is annotated in seconds
rather than after the whole run.

**The design question — answered, and the engine half has landed.** `AnalysisValueFeed` (`ec77f8bb`, merged
from `kotlin`) settles it: published values are **write-once and refine monotonically**, so a consumer never
needs retraction — a displayed value can only get stronger. Streaming is sound; no "stable subset" compromise
is needed.

The feed emits `passCompleted(iteration, fullPass, analyzed)` at pass boundaries **from the coordinator
thread with the workers quiescent**, so reading `info.analysis()` there is safe, plus `phase(...)` for
cycle-breaking activation and the three terminal outcomes. Registered through
`IteratingAnalyzer.setValueFeed`, a default no-op, so nothing pays for it unregistered.

Three things it imposes on a consumer:

- `analyzed` is **valid only during the call** — copy anything retained.
- it **over-approximates**: the whole order on the first pass, the shrinking dirty set after. Diff against
  your own previous state rather than trusting it as a changed-set.
- **positive type-immutability typically arrives last**, in the cycle-breaking pass. So an early stream looks
  complete while systematically missing `@Immutable` on types; display wants the status ladder the interface
  documents (provisional → quiet → final at `TERMINAL_CERTIFIED`) rather than presenting early values as
  settled.

**What remains — the daemon adapter and the front-ends.**
- *Protocol*: `status` is progress-only and there is exactly one terminal `result`. Prefer a **new
  `partialResult` frame** over making `result` repeatable, and not only on taste: `DaemonClient.analyze`
  loops until it sees `result` or `error` and hands every *other* frame to the `onStatus` consumer. So a new
  non-terminal type flows through the existing client untouched, and old clients ignore it; a repeatable
  `result` would make them return on the first one and treat a partial answer as the whole run.
- *Both front-ends*: results are currently replaced wholesale (`MaddiResults.update`,
  `MaddiAnalysisService.applyResult`), and Eclipse rewrites every marker in the workspace per run. Incremental
  arrival means merging by element and updating markers/hints additively — the same work item 4 lists under
  marker churn, and another reason to do it.

Note this composes with partial re-analysis (item 1) rather than competing: that one shrinks *what* is
analysed, this one shortens *when you see it*. Either helps alone; together a small edit should show revised
hints almost immediately.

---

## 4. Smaller, independent of the above

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
