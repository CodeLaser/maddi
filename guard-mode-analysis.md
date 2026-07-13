# Guard mode: analysis and design proposal

Goal: a "guard" mode where the user annotates e.g. an interface with `@Container`, and the
analyzer verifies all implementations against that contract, warning on violation **with an
explanation of why** the contract is broken.

- **Status of this document: initially based on `openjdk` @ 348b7cf9; revised on the
  `kotlin` branch @ bbfebcd7 (2026-07-13).** The "Current state" section below describes the
  openjdk branch; the "Kotlin-branch revision" section at the end records what the kotlin
  branch changes (run-level error system) and how it alters the proposal. Work should happen
  on `kotlin`.

## Current state (openjdk branch)

### Diagnostics infrastructure: prototype-stage, two competing types, neither wired end-to-end

- `Message` (`maddi-cst-api/.../analysis/Message.java`): source, info, free-text message,
  WARN/ERROR level. Impl: `MessageImpl` (maddi-cst-analysis). No message code/category, no
  structured payload (property, contracted vs computed), `MessageImpl.warn(...)` sets
  `source = null` so no location.
- Emitted at exactly **3 sites**, all sanity checks in `ShallowMethodAnalyzer`
  (`maddi-modification-common/.../defaults/ShallowMethodAnalyzer.java:293,303,313`).
  The deep analyzer (`maddi-modification-analyzer`), `-link`, `-prepwork` emit **zero**
  structured messages.
- Aggregation: only `ShallowAnalyzer.Result.messages()`. **Dropped by every production entry
  point** (`maddi-run-main/.../RunAnalyzer.runShallowAnalyzer`, run-openjdk, mvnplugin);
  only `AnalysisHintsCompiler` propagates it, and its only caller is a test.
- Second channel: `ANALYZER_ERROR` property (`Value.Message`, `PropertyImpl.java:99`),
  read by `DecoratorImpl.comments()` (prepwork io) to render comments in AAPI output —
  but **never written** anywhere. Dead channel.
- The richest existing checks are `ShallowTypeAnalyzer.check(TypeInfo)`
  (`maddi-modification-common/.../defaults/ShallowTypeAnalyzer.java:245-302`):
  @Immutable/@Container/@Independent hierarchy inconsistencies, @Modified field/method
  inside @Immutable type, @Independent-vs-@Immutable mismatch. All `LOGGER.warn` only,
  private counter, not Messages; only runs in the shallow/AAPI pass.

### Contract awareness: provenance exists but is transient and invisible to the computed analyzer

- Annotation → property translation: `AnnotationToProperty.annotationsToMap`
  (`maddi-modification-common/.../defaults/AnnotationToProperty.java:78-284`).
- Provenance: `ShallowAnalyzer.AnnotationOrigin` `{ANNOTATED, FROM_OVERRIDE, FROM_TYPE,
  FROM_OWNER, FROM_PARAMETER, FROM_METHOD, FROM_FIELD, DEFAULT}` (`ShallowAnalyzer.java:49`),
  tracked per property in `InfoData.originMap`, paired in `AnnotationToProperty.ValueOrigin`.
  **Stored in a transient side map (`Result.dataMap`), not on the Value; not serialized into
  the analyzed-package JSON; consumed only by the AAPI writers** (`DecoratorWithComments`,
  `AnalysisHintsWriter`). At computed-analysis time there is no way to ask "is this value
  contracted, and where was it written?"
- The `contract()` attribute declared on every annotation (`maddi-support`) is **never read**
  in analysis. The verification mode described in road-to-immutability §070
  (`@Container(contract=true)` on a parameter, "should cause an ERROR") is documented intent,
  zero implementation.
- AAPI values load via `LoadAnalysisResults` (prepwork io) as plain Property→Value pairs,
  indistinguishable from computed values. (Note: currently commented out with FIXME in both
  RunAnalyzer variants, `:110` — pipeline mid-refactor.)

### The trust-don't-verify pattern (the crux)

Computed analyzers early-return when a "good enough" (e.g. contracted) value is present, so
contracts are silently trusted, never verified:

- `TypeContainerAnalyzerImpl.go:39-41` — `if (container.isTrue()) return; // no point`
- `TypeImmutableAnalyzerImpl.go:47-50` — same for immutable
- `AbstractMethodAnalyzerImpl.methodNonModifying:132-135` — a contracted `@NotModified`
  interface method skips the aggregation over implementations entirely; a modifying
  implementation is NOT flagged. Same shape for `independent`, `methodIndependent`,
  `unmodified` (parameters).

These early-returns also protect the **monotonic overwrite guard**
(`PropertyValueMapImpl.setAllowControlledOverwrite:90-106` + `Value.overwriteAllowed`,
strictly-increasing only): a lower computed value hitting a higher contracted one throws
`UnsupportedOperationException` — a crash, not a diagnostic. (The immutableSuper fix
bf959257 was one instance of this crash class. Guard mode turns this class of conflict
into a proper diagnostic.)

### Reusable assets

- `AbstractMethodAnalyzerImpl` already does the exact traversal a guard needs: interface
  member → `IMPLEMENTATIONS` (`Value.SetOfMethodInfo`) → fold with min/AND. The guard is the
  same loop with "compare and explain" instead of "write".
- `ShallowTypeAnalyzer.check` contains the hierarchy-consistency logic, needs re-homing +
  Message-ification.
- `DECIDE.debug(...)` call sites throughout the analyzer impls mark exactly where each
  property value is decided — natural instrumentation points for a decision journal.
- Tests: `nolink/TestInterfaceImmutable.testConflictingImplementations` asserts no-crash +
  correct value; its guard-mode sibling would assert warning + explanation shape.
  `TestParseAnalyzeWrite:143-176` tests AnnotationOrigin.

## Proposed design (4 phases, each independently shippable)

**Phase 0 — diagnostic backbone.** Extend `Message` into a structured finding: message
code/category enum, `Property`, contracted value + source location, computed value, blamed
`Info`, and a list of causes (nested findings/evidence — carries the "why"). One central
collector passed to/owned by the analyzer; wire `Result.messages()` through
run-main/run-openjdk/gradle/maven; convert `ShallowTypeAnalyzer.check` warns into findings.

**Phase 1 — persist contract provenance.** Make (Info, Property) → {contracted?, source
location} queryable during computed analysis. Recommended: promote the AnnotationOrigin side
map into a first-class structure surviving into the computed phase (and optionally the AAPI
JSON codec), rather than changing Value/PropertyValueMap in cst-api.

**Phase 2 — the guard pass.** For each contracted property: compute anyway, into a local
(not `analysis()` — avoids the overwrite crash, keeps contracts authoritative downstream),
then diff, then emit. For `@Container` on an interface: every implementation type × every
non-private method × every parameter, check `isUnmodified`; on failure emit e.g.
"`MyImpl` violates the `@Container` contract on `MyInterface` (contracted at
MyInterface.java:12): parameter `e` of `MyImpl.add(E)` is modified — `e.setMessage(...)`
called at MyImpl.java:34." Host: a step after the iteration fixed point converges (values
final, no warnings on undecided intermediates). Early-returns stay for unannotated values.

**Phase 3 — the "why" chain.** (a) on-demand blame walk: given "parameter e is modified",
walk to the statement/call that made it so, recursing through propagation (link module data
is the evidence trail); (b) optional decision journal at DECIDE sites:
(property, value, because-of) tuples → causal graph, at memory cost. Start with (a) for
container/modification/independence.

**Phase 4 — tests + hardening.** `guard` test package parallel to `nolink`, seeded from the
conflicting-implementation shapes already in the sweep suites.

## Open decisions (recommendations, awaiting user input)

1. Contract = **any** user annotation on analyzed (non-AAPI) code (recommended), vs only
   `contract=true`. Keep `contract=true` for §070 parameter-level dynamic checking later.
   AAPI values stay trusted-not-verified.
2. Severity: contracted-vs-computed conflict = ERROR; hierarchy inconsistency = WARN.
3. Provenance storage: side-structure (recommended) vs adding origin to PropertyValueMap in
   cst-api (cleaner long-term; touches codec + wide API surface).

## Kotlin-branch revision (@ bbfebcd7)

Merge-base with openjdk is 348b7cf9 (i.e. kotlin contains all the modification-analyzer
hardening work). ~500 files differ, mostly kotlin parsing; the guard-relevant deltas:

### What changed

1. **Run-level error reporting now exists** — `maddi-run-config/.../report/`:
   - `ErrorReport.report(Summary, Throwable terminal)` (`ErrorReport.java`): harmonized,
     enumerated rendering of parse/inspection errors + the terminal throwable, for all three
     runners. Wired: `run-main/RunAnalyzer.printSummaries():317`,
     `run-openjdk/RunAnalyzer:355`, `run-kotlin/Main.java:92,95`. Caps warnings at 50.
     Explicit javadoc: collected errors were "previously write-only".
   - `ExitCode.java`: shared exit codes `OK/INTERNAL_EXCEPTION/PARSER_ERROR/
     INSPECTION_ERROR/IO_EXCEPTION/ANALYSER_ERROR`; RunAnalyzer catch-blocks now map
     exception classes to codes and stash `terminalError` for the report.
2. **`Message.Severity` enum added to cst-api `Message`** — a concrete WARN/ERROR `Level`
   implementation "shared by the parse path (Summary.ParseException) and the analysis path
   (Message)". This is the severity model to reuse.
3. **`Summary.ParseException` upgraded** (`maddi-inspection-api/.../Summary.java`):
   carries a `Message.Level level` (WARN = tolerable, ERROR) and a `Source` (line/col)
   derived from the `where` element; message rendering includes `uri:line-col`
   (`source.compact()`). `Summary` now has `parseWarnings()` distinct from
   `parseExceptions()`.
4. **AAPI renamed to "Analysis Hints"** (`AnalysisHints`, `AnalysisHintsCompiler`,
   `AnalysisHintsConfiguration`, "analyzed analysis hints" = analysis results). The
   compiler mode is live again in run-main (`runAnalysisHintsCompiler`), and
   `LoadAnalysisResults` preloading is re-enabled (use case 1,
   `preloadAnalysisResultsDirs`), replacing the openjdk-branch FIXME.
5. **`AnalysisHintsCompiler.go(hints)` → `List<Message>` is now consumed in production**
   (run-main `RunAnalyzer.runAnalysisHintsCompiler`) — but only **counted**
   ("produced {} message(s)"), still not enumerated/rendered.

### What did NOT change (verified)

- `maddi-modification-common`, `maddi-modification-analyzer`, `maddi-cst-analysis`: no
  non-IDE-file changes. So everything in "Current state" about the shallow/deep analyzers
  holds on kotlin too: 3 Message emission sites, `ShallowTypeAnalyzer.check` still
  LOGGER-only, trust-don't-verify early-returns, transient AnnotationOrigin, dead
  `ANALYZER_ERROR` channel, unused `contract()` attribute.

### Impact on the proposal

- **Phase 0 shrinks substantially.** The surfacing pipeline (severity model, per-runner
  report hook, exit codes, located errors) now exists. Instead of inventing one:
  - extend `ErrorReport.report(...)` to take and enumerate analysis `Message`s alongside
    parse errors (single rendering point for all three runners);
  - decide exit-code policy for contract violations (natural: `ANALYSER_ERROR` when any
    ERROR-severity finding, mirroring PARSER_ERROR ← Summary.haveErrors());
  - reuse `Message.Severity`; follow `ParseException`'s pattern of deriving a `Source`
    from the blamed `Element` for guaranteed line/col (fixes the `MessageImpl.warn`
    null-source defect);
  - what remains of Phase 0: the structured finding type (code/category, property,
    contracted vs computed, causes) extending `Message`, a collector reachable from the
    analyzers (`IteratingAnalyzer` down to the per-property analyzers), and rendering the
    already-collected `AnalysisHintsCompiler`/`ShallowAnalyzer` messages instead of
    counting them.
- Phases 1-4 unchanged.
- Vocabulary: use "analysis hints" not "AAPI" in new code on this branch.
