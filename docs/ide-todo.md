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

**(b) "Still running" — DONE.** The daemon emits a `status` frame per phase transition plus a heartbeat every
2 s during the long analysis phase, and all three front-ends now show it: Eclipse via `SubMonitor.subTask`
(it previously discarded every frame and its `IProgressMonitor` was unused), IntelliJ via
`indicator.setText`/`setText2`, VS Code via `withProgress`. Streamed `partialResult` frames additionally
report the pass number and how many elements have been decided so far.

Deliberately no percentage anywhere: the run is a fixpoint iteration whose length is not known in advance,
so a fraction would be invented rather than measured.

**(c) Determinate progress (n of m types) — still open, and probably not worth it.** `typesDone` is always
null during analysis: the heartbeat is a liveness ping, not a counter, and `IteratingAnalyzer.analyze` has no
per-element callback to count. Since (b) landed and item 3 streams the decisions themselves, a counter would
add little — "23 elements decided, pass 2" already says more than a bar would.

**Cancellation — NOT supported, and not a small addition.** `DaemonProtocol` defines a `cancel` frame but
`DaemonMain` handles only handshake / ping / analyzeProject / shutdown, and while an analysis runs the
connection thread is blocked inside `handleAnalyze` and is not reading the socket at all. So a front-end
sending `cancel` today would have it sit unread until the run it wanted to stop had finished. Doing it
properly needs a reader thread on the daemon side plus an interruptible analyzer — an engine change, not
plumbing. Until then, no front-end should offer a cancel button that cannot work.

---

## 3. Stream early results, instead of waiting for the run to finish — DONE

Better than a progress bar, and it made most of item 2(c) unnecessary.

**The observation.** The analyzer is incremental, and the *shape* of the run is lopsided: the majority of the
output is hard-decided in the first iteration, and the tail is long in duration but short in number of
decisions. `IteratingAnalyzer.analyze`'s own javadoc says as much — worklist narrowing (default on) makes
"iterations 2+ only re-analyze elements that changed in the previous iteration plus their dependents". So a
user waiting for the terminal `result` is waiting mostly for decisions that were already made, about elements
they are probably not even looking at.

**The idea.** Emit decisions to the IDE as they harden, so hints and findings appear early and the tail merely
refines them. On a large project this changes the feel completely: the file on screen is annotated in seconds
rather than after the whole run.

**The design question — answered, and the engine half has landed.** `AnalysisValueFeed` (`d3269c2a`, merged
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

**Delivered** (`1e830074` daemon, `1c38719f` front-ends).

- *Daemon*: `StreamingValueFeed` turns pass boundaries into a **new non-terminal `partialResult` frame**,
  chosen over making `result` repeatable — `DaemonClient.analyze` loops until `result`/`error` and hands
  every other frame on, so a new type reaches existing clients untouched while a second `result` would be
  mistaken for the whole run.
- *All three front-ends*: merged by element via the shared `AnalysisModel.merge` (identity is kind + fqn, not
  position), never replaced — one frame is one pass. Eclipse deliberately does not rebuild markers per frame
  (`MaddiMarkers.apply` rewrites the whole workspace); hints update because they read `MaddiResults`.

The certainty ladder is in: the terminal `result` now carries an `outcome` (CERTIFIED / MAX_ITERATIONS /
PLATEAU / UNKNOWN), and `AnalysisModel.certaintyOf` turns it into PROVISIONAL / FINAL / BEST_AVAILABLE /
UNKNOWN for the front-ends. A missing outcome reads as UNKNOWN, never as final, so an older daemon cannot
have its values presented as certified.

Deliberately two rungs, not three: the feed's docs also describe a *quiet* state (an element that stops being
re-analysed). Tracking it means per-element last-seen bookkeeping in every front-end, and it buys little now
that the progress line reports the pass and the count — the distinction that actually misleads is a finished
run that never certified, because those annotations look exactly like certified ones.

Note this composes with partial re-analysis (item 1) rather than competing: that one shrinks *what* is
analysed, this one shortens *when you see it*. Either helps alone; together a small edit should show revised
hints almost immediately.

---

## 4. Smaller, independent of the above

Each of these is a GitHub issue (#24–#28); this section stays the reasoning.

- **(#24) Coalesced triggers are dropped silently.** Both front-ends guard with a bare `AtomicBoolean`
  (`MaddiAnalysis.RUNNING`), so a build finishing while a run is in flight is simply never computed. Needs a
  pending flag and a re-run, not a second concurrent analysis.
- **(#25) Whole-workspace marker churn (Eclipse).** `MaddiMarkers.apply` deletes every maddi marker at
  `DEPTH_INFINITE` from the workspace root and recreates them all, on every run. Wasteful now; wrong once
  analysis is partial, since a partial result must not erase markers for files it did not look at.
- **(#26) No builder / project nature (Eclipse).** Analysis is a resource-change listener, so there is no delta
  scoping. Related to 1: a real builder is the idiomatic place to hook incremental analysis.
- **(#27) Quick fixes and richer hover (Eclipse).** IntelliJ has an external annotator with a why-chain tooltip;
  Eclipse has only the marker message. No quick fixes on either side.
- **(#28) GUI install path unverified.** The p2 site is verified by installing with the director; nobody has driven
  `Help > Install New Software` by hand, so the license page and category rendering are unconfirmed.
- **(#29) The tool window keeps the previous run's results when a run fails, and during every run.**
  `MaddiFindingsPanel.render` is the only thing that ever clears the tree (`root.removeAllChildren()` +
  `model.reload()`), and it runs only on `MaddiResultListener.TOPIC`, which only
  `MaddiAnalysisService.applyResult` publishes. Two paths reach neither: the daemon-error branch
  (`MaddiAnalysisService:124`, `notifyUser("Daemon error: …")` then a bare `return`) and the
  `catch (Exception e)` in `analyzeInBackground` (:91). So a run that OOMs, errors, or throws leaves the
  **previous** run's findings on screen with nothing marking them stale — and because `index()` is also
  skipped, `latest` keeps feeding the inlays, gutter icons and annotator too. Observed 2026-08-21 on the
  CodeLaser tree: after a failed run the panel was indistinguishable from a good one.
  Nothing resets the panel when a run *starts* either, so even a successful re-parse shows the old tree for
  the whole run — minutes, on a large project.
  Wanted: clear (or visibly mark stale) on analysis start, and publish a terminal state on every exit path so
  failure is distinguishable from "not run yet" and from a stale success. Note the parse-error case is
  *not* covered by this: `WarmAnalysisService` returns a real `Result` with empty `elementAnnotations` and
  `OUTCOME_UNKNOWN`, so `applyResult` does run and the tree does clear — it just goes quiet, which is its
  own reason to show the outcome in the panel rather than only in a balloon.
  ✅ **DONE (2026-08-25), together with the rest of the tool window.** A second topic, `MaddiRunListener`,
  carries what a run is DOING (started / status / pass / failed / finished) alongside `MaddiResultListener`,
  which carries what it produced; every terminal path publishes, including the daemon-error branch and the
  `catch` in `analyzeInBackground`. The findings tree is marked stale rather than cleared while a run is in
  flight — what it holds until the terminal frame IS the previous run's findings, and destroying them buys
  nothing. What the run was doing was the other half of the complaint that prompted this: mid-run the panel
  rendered a merged `partialResult` as `0 finding(s), 18771 annotated element(s), 0 hint type(s), 0 ms`, since
  three of those four numbers do not exist until the run ends. ⛔ **RENDERING "NOT KNOWN YET" AS A ZERO READS
  AS A MEASUREMENT.** The panel now has a header (state + a client-side elapsed clock, phase and last message,
  and the daemon's install directory and build stamp), the tree, and a timestamped run log fed by every status
  frame, heartbeat and pass. `MaddiToolWindowTest` covers six of those behaviours; the control (unsubscribing
  the panel from the run topic) turns all six red.
- **(#30) Self-analysis: the IDE config makes every module both source and bytecode, and commits break.** Symptom on the
  CodeLaser tree (2026-08-21): `[ERROR/parse] UnsupportedOperationException … Cannot commit. Type
  io.codelaser.maddi.cst.impl.statement.StatementImpl.Builder has a null parent class, and it is not JLO`
  — 12 occurrences over four types (`StatementImpl.Builder`, `Trie`, `JavacListToSourceSets`,
  `TestIsolateMethodCodec`), plus 3,677 `resolves its references into … through class files` warnings
  (the CLI run on the same tree: 439 warnings, zero errors).
  ⭐ The stack names the **bytecode** scanner, not the source one:
  `ClassSymbolScanner.loadType: …StatementImpl.Builder COMPLETE_SUB` → `addMemberToType:913` →
  `TypeInspectionImpl$Builder.commit:350`. So a type that is *also* a source type in the same parse is
  being loaded from a class file and comes out with no parent class — the shape
  [`handoff-source-and-jar-duplicate-interfaces.md`](handoff-source-and-jar-duplicate-interfaces.md)
  fixed for the *preload* path in these same files, and whose table predicts verbatim that a pre-source
  lazy load "would be left with no parent class and no type parameters".
  **Why the IDE hits it and the CLI never does.** `MaddiConfigBuilder` puts every module's compiler
  output dir on one flat classpath ("the crucial mapping is compiler output dirs → classpath … hot class
  files") while every module's source root is also in the source list. Measured on the same tree, the
  CLI's input configuration has **zero** overlap between the 160 source-set output URIs and the 491
  classpath parts: a source set's output is its `uri` (its identity), never a flat classpath entry, and
  inter-set dependencies are by NAME (`maddi/maddi-cst-api/main`). The IDE model has no such names, so it
  substitutes class files — and every FQN in the project becomes reachable both ways.
  That is also why the warnings name a *source* directory as the place class files were expected.
  Wanted: give each module its own classpath and let source sets reference each other by name, rather than
  a project-wide union of output dirs. Same root cause as the OOM in #31.
  ⭐ **MEASURED self-analysis-only.** A first reading of this entry generalised it to any multi-module
  project, on the argument that the IDE puts *every* module's output dir on the classpath regardless of
  whose code it is. That was wrong. Driven against **Pulsar** with the identical plugin-style flattening
  (98 source roots, 681 classpath entries, all 90 build-output dirs present on disk, so the
  source-and-bytecode precondition held): **0 null-parent commits**, and 165 through-class-files warnings
  against CodeLaser's 3,677. So the `ClassSymbolScanner` route needs maddi's own bytecode meeting maddi's
  own source — the same self-analysis requirement
  [`handoff-source-and-jar-duplicate-interfaces.md`](handoff-source-and-jar-duplicate-interfaces.md)
  states for the preload route. Third-party projects do not hit this one.
- **(#31) Whole-project classpath union is quadratic, and OOMs before parsing.** Same `MaddiConfigBuilder`
  flattening: `OrderEnumerator.orderEntries(project).librariesOnly()` unions every library in the project,
  and `InputConfigurationAssembler` relies on `Builder.build()` wiring "each source set's dependencies to
  all classpath parts and all earlier source sets". On the CodeLaser composite that is 160 sets × 491
  parts = 75,520 (set, entry) pairs against the 5,524 the CLI config declares — **14×**. javac opens a
  `ZipFileSystem` per container per source set, so both OOMs (12 GB and 24 GB) died in
  `onlyPreload()` → `ClassFinder.scanUserPaths` → `ZipFileSystem.initCEN`, before any project source was
  parsed. `maddi-intellij` alone contributes 201 of maddi's 282 jar dependencies (71%) — the IDEA
  platform, 383 jars / 1.0 GB. Unloading modules is the only workaround today.
  ⭐ **Scale-only, and third-party projects are on the same curve.** Pulsar, same flattening, 16 GB heap:
  90 sets x 611 parts = 54,990 pairs against the 8,365 its own CLI config declares — **6.6x**, versus
  CodeLaser's 14x. It cleared `onlyPreload()` in **25 s at 11.9 GB RSS**, the exact phase where CodeLaser
  died at both 12 GB and 24 GB. So this is a threshold, not a maddi-specific defect: 90 modules survives,
  and a tree with several hundred modules, or one dragging a fat SDK, will not.
- **(#32) `compileOnly` dependencies are missing from the IDE classpath.** `maddi-mvnplugin` is the only
  maddi module using `compileOnly` (maven-plugin-api/-core/-artifact/-model, plugin-annotations, Aether).
  Under the plugin, `CommonMojo extends AbstractMojo` fails to resolve, and the *implicit* `super()` in
  its constructor throws `Unexpected null symbol for unqualified call to 'super'`
  (`ScanCompilationUnit:3022`) hundreds of times. The CLI config carries all 25 maven jars for
  `maddi-mvnplugin/main`, because it is derived from the javac compile log, which sees `compileOnly` like
  any other compile input. ⚠ One unresolvable module degrades the **whole** request:
  `WarmAnalysisService:88` returns findings-only with empty `elementAnnotations` and `OUTCOME_UNKNOWN` on
  `summary.haveErrors()`, so no annotations appear anywhere in the project.
  ⭐ **This is the one that generalises, and it is the most damaging.** Pulsar, plugin-style, produced
  **344 parse errors / 2,865 findings / `elementAnnotations: 0` / `outcome: UNKNOWN`** in 118 s — no OOM,
  no null-parent commits, and still not one annotation. The leaf messages are the same family as
  `CommonMojo`: 866 x `Unexpected null symbol for unqualified call to 'X'`, 460 x `Cannot convert a null
  javac type; the caller's symbol or target type was never attributed`, 73 x `Unknown identifier type
  null` — concentrated in `pulsar-functions`, `pulsar-client-tools`, `pulsar-proxy`, `pulsar-websocket`,
  the modules heaviest in `provided`-scope and shaded dependencies.
  ⛔ The short-circuit is what turns a partial classpath gap into total silence, so it is arguably worth
  fixing ahead of the classpath itself: a project with ONE unresolvable module currently gets the same
  empty tool window as a project that was never analysed.

---

## 4b. Which build is the IDE actually running? — DONE (2026-08-24)

Not a feature. A day was spent diagnosing a "regression" that was a **stale bundled daemon**, and nothing
observable distinguished it from a current one.

**What happened.** The `access()`-null defect closed on 2026-08-23 (`d6f85131d`) came back after an IDE restart:
50 x `MethodInfo.access()` + 1 x `FieldInfo.access()` NPEs, plus 3 x `CompilationUnit.sourceSet()` NPEs whose
fix (`d34d6c525`) was older still. The plugin at `~/Library/.../plugins/maddi/daemon/` was carrying a
`maddi-java-openjdk` jar **byte-identical to a build from another worktree** (`ws/python`, at `ba36eb4e1` — the
commit that *documented* the defect, one before the fix) with the then-uncommitted per-module protocol on top:
a combination no commit ever had. Everything on offer said the same thing a correct build says — plugin
`0.1.0`, maddi `0.9.1`, daemon `0.1.0-dev`, and even `per-module configuration: 64 source set(s)`, which looked
like proof of freshness and was not. Settling it took `javap -p` on the installed jar, grepping for a method
name from the fix.

⛔ **A VERSION IS A CONSTANT OF THE SOURCE; ONLY A STAMP IS A FUNCTION OF THE BUILD.** Three versions were on
the wire already and not one of them could move between two builds of the same release.

**What was added.** `generateBuildStamp` (in `maddi-ide-daemon/build.gradle.kts`) writes
`build-stamp.properties` next to `DaemonMain`, holding a stamp derived from the **source state**:
`7dbfd36e1` when committed and clean, `7dbfd36e1+a3f01c9e` when the working tree carries changes (the suffix
hashes `git diff HEAD` plus `git status --porcelain`, so a new untracked file counts too), `nogit` otherwise.
It is a function of the sources and never of the clock, so the tree's jars stay byte-reproducible. `DaemonMain`
logs it on the first line — `maddi daemon 0.1.0-dev (build 7dbfd36e1+a3f01c9e, maddi 0.9.1) listening on …` —
and returns it in `handshakeAck`; `MaddiDaemonProcess.buildStamp()` exposes it and `MaddiAnalysisService` logs
it with the install directory, so `idea.log` records which daemon answered. `TestBuildStamp` covers the three
ways the check can quietly stop working (resource not packaged, packaged in the wrong package, not on the
wire); the middle one is the control that was run.

**The fast loop, and it now works — DONE (2026-08-25).** Settings → maddi → *Daemon install override* pointed
at `…/maddi-ide-daemon/build/install/maddi-ide-daemon` skips the plugin rebuild-and-reinstall entirely: only a
front-end change needs a new plugin. Two things had to change before that was usable, both the same shape of
trap as the stale bundle itself — the setting said one thing and the running process was another:

- `MaddiDaemonProcess.ensureStarted` returned early on nothing but "the process is alive", so a changed install
  directory, JDK or heap had no effect until that daemon happened to die. It now remembers what it launched
  WITH and relaunches when the request differs.
- there was no way to pick up a fresh `installDist` at all, since the daemon is deliberately kept warm across
  requests — including across the very rebuild being tested. The tool window has a **Restart daemon** button
  (`MaddiAnalysisService.restartDaemon`).

So the loop is: `./gradlew :maddi-ide-daemon:installDist` → *Restart daemon* → *Analyze*. No plugin build, no
reinstall, no IDE restart.

---

## 5. The input-configuration gap — solution sketch

#30/#31/#32 are one defect wearing three hats: **`MaddiConfigBuilder` reconstructs a classpath from
IntelliJ's project model, and the model cannot express what maddi needs.** The javac-log route gives
per-source-set classpaths and lets source sets name each other; IntelliJ's gives neither, so the builder
substitutes a project-wide union plus class files. Broad-brush options, in the order they are worth doing.

### C. Stop discarding the parse — DONE (2026-08-21)

`Summary.parseResultIgnoringErrors()` + `SummaryImpl`; `WarmAnalysisService` analyses what parsed, builds
`PrepAnalyzer` fault-tolerant (a partial parse trips prep far more often), and forces `OUTCOME_UNKNOWN`;
`AnalysisModel.certaintyOf` caps at BEST_AVAILABLE when `parseErrorCount > 0`, and the plugin reports the file
count. No protocol change: `parseErrorCount` was already on `Result`. `parseResult()` keeps refusing on errors,
so only the IDE opts in. Regression: `TestPartialParse`, verified to FAIL with the old short-circuit restored.
Field check: Pulsar went from findings-only to `analysing the 1792 type(s) that did parse`.

### C (original write-up)

`WarmAnalysisService:88` returns findings-only whenever `summary.haveErrors()`, so **one** unresolvable
module yields zero annotations for the whole project. Measured: Pulsar, 344 parse errors out of 98 source
roots → `elementAnnotations: 0`. That is also self-contradictory — the daemon sets `failFast=false`
explicitly "so a project with in-progress errors still yields partial results", and then throws those
results away.

The gate is `SummaryImpl:64`: `parseResult()` throws on `haveErrors()`. But `types`, `sourceSetsByName`
and `sourceSetToModuleInfo` are all populated — it is a **policy** refusal, not absent data. So this wants
a partial accessor (`parseResultAllowingErrors()`, or a flag) and a daemon that analyses what did parse,
labelling the result partial. ⚠ Note the CLI would refuse identically; it simply never has parse errors,
because its configuration is right. So C is resilience, not the fix — but in an IDE, where a tree is
routinely mid-edit, it is the difference between "some hints" and "nothing at all". Pairs with #29:
the outcome belongs in the tool window, not only in a balloon.

### A. Per-module source sets from IntelliJ's model — DONE (2026-08-22)

`DaemonProtocol.ModuleSourceSet` (mirrored in `AnalysisModel`), added to `AnalyzeConfig` with the 9-arg
constructor kept so the flat form still works for Eclipse and the fixtures. `InputConfigurationAssembler`
branches to a per-module path built ONLY with the object style, so `build()`'s auto-wiring loop is never
entered; libraries become a shared pool, a set's output is its `uri` and never a class-path entry, and sets are
created in topological order (a cycle drops the closing edge with a warning rather than throwing).
`MaddiConfigBuilder` emits one spec per module and root kind, with `orderEntries(module).librariesOnly()
.withoutSdk()` and NO scope filter, so PROVIDED/`compileOnly` survives.
Tests: `TestPerModuleConfiguration` (4) asserts own-class-path-only, zero output/class-path overlap, and that
the flat form still auto-wires as a contrast; `MaddiConfigBuilderTest` rewritten (6) — note
`testCompilerOutputBecomesClasspath` became `testCompilerOutputIsIdentityNotClasspath`, an INVERTED
expectation, and `testFlatPairIsEmpty` guards the fallback from being silently re-entered.
⚠ Not yet exercised against a real IDE run; that is the outstanding validation.

### A (original write-up)

Swap `InputConfigurationImpl.Builder`'s **string style** (`addSource`/`addClassPath`, whose `build()`
auto-wires every set to all parts and all earlier sets) for the **object style**
(`addSourceSets(SourceSet...)`), which `CompileListToSourceSets` already uses for the javac-log route.
One IntelliJ module becomes two `SourceSetImpl`s:

| `SourceSetImpl` field | IntelliJ source |
|---|---|
| `name` | module name + `/main` \| `/test` |
| `sourceDirectories` | `ModuleRootManager.getSourceRoots(SOURCE \| TEST_SOURCE)` |
| `uri` | `CompilerModuleExtension.getCompilerOutputPath()` / `…ForTests()` |
| `sourceRelease` | the module's effective language level |
| `dependencies` | direct module dependencies (as source sets) + that module's own libraries |
| `test` | which of the two |

⭐ **Only DIRECT dependencies are needed.** `SourceSetImpl.recursiveDependencies` computes the closure
itself, which is exactly the shape `ModuleRootManager.getDependencies()` returns — no transitive
resolution to reimplement.

This fixes all three at once: no project-wide union (#31); each module's classpath carries its own
PROVIDED/`compileOnly` entries (#32); and a module's output dir becomes its `uri` — its identity — rather
than a flat classpath entry, restoring the **zero overlap** the CLI config has between the 160 source-set
outputs and the 491 classpath parts (#30).

Costs: `AnalyzeConfig` is flat, so the protocol needs a per-source-set shape and `PROTOCOL_VERSION` 1→2;
Eclipse shares the daemon and needs the same treatment. Residual risk: IntelliJ's model is itself a
projection of the build, so shaded/relocated artifacts may still not match javac reality.

### D. `-source` is not `--release`, and the shared JDK is not the first source set's — DONE (2026-08-25)

Not an IDE defect, and the one that made `MethodInfo.java` carry no annotations on the CodeLaser tree.

**Two conflations, both in shared code.** Every config producer collapsed javac's two level settings into one
integer — `CompileInvocation.effectiveRelease()` (`--release` ?: `-source`), the Gradle plugin's
`sourceReleaseOf` (`options.release` ?: `sourceCompatibility`), the Maven plugin's `sourceRelease`
(`<release>` ?: `<source>`), IntelliJ's language level — and `JavaInspectorImpl` turns any non-zero value into
`--release=N`, which reads `java.base` through `ct.sym`. But `-source N` sets the **language** level and leaves
the API at the running JDK's; only `--release` pins the API. ⛔ **REPORTING THE WEAKER SETTING AS THE STRONGER
INVENTS A PLATFORM THE BUILD NEVER COMPILED AGAINST.** maddi's own build states `-source 17 -target 17` for
`maddi-support`, whose test calls `List.getFirst()` — legal for that build, impossible under `--release 17`.

**And the shared `java.*` model was built by whichever source set happened to be scanned first.**
`ScanCompilationUnits` preloads under `if (!runtime.objectTypeInfo().hasBeenInspected())`, through that task's
file manager. So the first set's band became every set's `java.base`, and since a committed type cannot gain a
member, a later set at a higher band met a `java.util.List` without the method it needed and its unit was
dropped. Measured in the daemon on maddi itself: `maddi-annotation` (first, level 17) committed `List` from the
11–20 band; **391 of 572 dropped compilation units**, and **543 analysis hints skipped** because the hint
archive addresses methods by position in a method list that is shorter at 17 than at 21.

⭐ **The CLI was never immune — its harness had been patched.** `postprocess.py` (the pipeline's compile-log
step) has rewritten every `-source N`/`--release N` to 25 since 2026-08-21, with a comment naming this exact
failure: *"java.util.List committed from the 17 band (maddi-support) cannot then gain getFirst() for a 21 source
set (maddi-ide-client)"*. maddi's own compile log carries 31 x `-source 17`, 14 x `--release 21`, 496 x
`-source 25`, 2 x `-source 26`.

**What changed.** (1) Only a real `--release` answers "what API was this compiled against"; a build that states
only `-source`/`sourceCompatibility`/`maven.compiler.source` now reports `0`, which is the truth. IntelliJ
reports `0` always — its model has a language level and no `--release`, and inventing one is the defect; the
cost is accepted and written down (a project that genuinely cross-compiles, as pulsar does, is parsed against
the running JDK for that set). (2) The preload gets its own pass before any source set, on a source-free task
given the RUNNING JDK, so the shared model is the superset and a set at `--release N` can only ever find what
it needs already committed. Each set's own sources keep being attributed at its own release — the OpenSearch
and pulsar reasoning in `createTask` is untouched, and `TestSharedJdkRelease` asserts both halves.

⚠ Three things the preload pass must NOT do, each found by a test rather than by reasoning: it must not parse a
warm-up compilation unit (a unit lands in the `Summary`, the source set's file list and the incremental
bookkeeping — `TestAnalysisEarlyCutoffPrototype`, `TestReloadSourcesFromDisk`, `TestInvalidate`); it cannot call
`scan()` on a source-free task (`task.parse()` answers `IllegalStateException: error: no source files`, hence
`ScanCompilationUnits.preloadOnly()`); and it must not commit what it loads, because committing pulls
transitive types in and copying those into the CTM makes `java.lang.invoke.VarHandle` resolvable where
`TestJavaInspector1OnlyJmod` asserts "no pre-load". Only the release changes; everything downstream stays.

---

### B. Ask the build system instead (highest fidelity, narrowest reach)

Both build plugins **already emit exactly this file**: `maddi-write-input-configuration`
(`AnalyzerExtension:22`) and the Maven `write-input-configuration` goal. The IDE could run that task and
hand the daemon a path; the daemon already has `JsonStreaming` on its classpath via `maddi-run-config`, so
loading is the same one-liner the CLI uses — `objectMapper.readValue(file, InputConfigurationImpl.class)`
(`Main:358`).

Fidelity is perfect by construction: it is the route that measured green on this tree. But it needs a
build invocation (slow, and stale whenever dependencies change) and the maddi plugin applied to the
analysed project — fine for dogfooding CodeLaser and maddi, not general.

**Suggested order: C, then A, with B as an opt-in "use my build's configuration" for projects that apply
the plugin.** A and B are not exclusive — A is the fallback whenever B is unavailable.
