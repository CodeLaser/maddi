# Guard mode: analysis and design proposal

Goal: a "guard" mode where the user annotates e.g. an interface with `@Container`, and the
analyzer verifies all implementations against that contract, warning on violation **with an
explanation of why** the contract is broken.

- **Status of this document: initially based on `openjdk` @ 348b7cf9; revised on the
  `kotlin` branch @ bbfebcd7 (2026-07-13).** The "Current state" section below describes the
  openjdk branch; the "Kotlin-branch revision" section at the end records what the kotlin
  branch changes (run-level error system) and how it alters the proposal.
- **Implementation status (branch `guard-mode`, 2026-07-14): Phases 0, 0b, 1 and a first
  iteration of 2/2b are implemented** — see the "Implementation log" section at the end.

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

## Implementation log (branch `guard-mode`)

### Done (2026-07-14)

- **Phase 0 — message model.** `Message` (cst-api) gained `category()` (machine-readable,
  kebab-case, e.g. "contract-violation") and `causes()` (nested messages = the why-chain).
  `MessageImpl` derives `Source` from the blamed `Info` (fixes the null-source defect),
  offers `warn/error/cause` factories, uses the shared `Message.Severity` (LevelEnum retired).
- **Phase 0b — collector + surfacing.** The iterating analyzer carries a synchronized message
  list, injected into every sub-analyzer alongside `propertyChanges` (same pattern), exposed
  as `IteratingAnalyzer.messages()` / `SingleIterationAnalyzer.messages()`.
  `ShallowTypeAnalyzer.check`'s five consistency checks now emit structured findings
  (categories `hierarchy-inconsistency`, `modified-in-immutable`, `property-inconsistency`;
  offending supertypes as causes) instead of write-only LOGGER.warns, and flow into
  `ShallowAnalyzer.Result` + the iteration collector. `ErrorReport.report` gained a third
  parameter enumerating analysis findings (errors first, warnings capped at 50, indented
  "because:" cause chains, uri:line-col locations). Both runners feed it; ERROR findings →
  `EXIT_ANALYSER_ERROR`; analysis-hints compiler mode collects instead of counting.
- **Phase 1 — contract provenance, simplified.** Insight: for source-code contracts no
  persistence is needed — the contract side is re-derivable on demand from the CST.
  `ContractReader` (modification-common, `defaults` package, subclass of the package-private
  `AnnotationToProperty`) exposes `contracts(Info): Map<Property, Value>`. The guard diffs
  that against `analysis()` (computed side) — for abstract types, where `analysis()` holds the
  trusted contract, the relevant computed values live on the *implementations*, which is
  where the guard looks. The InfoData/AnnotationOrigin plumbing stays untouched; it would
  only matter for verifying loaded analysis-hints values, which stay trusted by design.
- **Phase 2 — GuardAnalyzer, first iteration.** `GuardAnalyzerImpl` runs inside
  `IteratingAnalyzerImpl.analyze` after the fixed point (config `guardContracts()`, default
  true, builder-settable). Verifies:
  - `@Container` on a type: parameters of the type's own non-private concrete methods, and of
    every implementation (via prepwork's `IMPLEMENTATIONS` property) of its non-private
    abstract methods, must not have a decided-FALSE `UNMODIFIED_PARAMETER`.
  - `@NotModified` on an abstract method: no implementation with decided-FALSE
    `NON_MODIFYING_METHOD`.
  Violations: ERROR, category `contract-violation`, blamed on the offending parameter/method,
  why-chain = implements-link → contract location. Only decided FALSE values are reported —
  undecided stays silent (no findings on incomplete information). Computes into locals, never
  writes `analysis()` (contracts stay authoritative; no monotonic-overwrite crashes).
- **Phase 2b — tests.** `analyzer/guard/TestGuardContainer`: violation with full why-chain
  assertion, conforming-implementation positive control, guard-disabled control, concrete
  @Container class, @NotModified abstract method. 5/5 green; full modification-analyzer +
  common suites unaffected.

### Done (2026-07-14, kotlin trunk — Phase 3, "why" chain)

- **Phase 3(a) — blame walk.** `GuardAnalyzerImpl.blameParameterModified` / `blameMethodModifying`
  scan the implementation's own body for the *first* site that modifies the offending
  parameter (or the instance) and add it as the **top** cause of the violation, located at
  that statement (line/col via `MessageImpl.cause(Source, Info, …)`, new factory). So a
  violation's `causes()` now reads `[ blame, contract-provenance ]`, e.g.
  "`message.setMsg(...)` modifies `message`" → "BadRegistry implements ErrorRegistry.add" →
  "@Container contracted on ErrorRegistry". Three modification shapes are recognised:
  (1) a modifying call with the parameter as **receiver** (`pi.m(...)`, `m` modifying);
  (2) the parameter **passed** into a callee parameter slot the callee modifies;
  (3) a **field/element assignment** rooted at the parameter (`pi.f = …`, `pi[i] = …`).
  For `@NotModified` methods: a write to an instance field (`count++`) or a modifying call on
  `this`. Key design choice: it re-derives the evidence from the CST reading only the
  analyser's own accessors (`MethodInfo.isModifying()`, `ParameterInfo.isModified()` — the
  same signal that decided the violation, so it agrees for **shallow/JDK** callees like
  `StringBuilder.append` too), rather than threading the per-call `Result.modified`
  (`Variable → Set<MethodInfo>`) map out of the link module (which discards it after linking,
  and which `sv-integration` owns). Returns `null` — violation still reported, no deepest
  "where" — when modification is indirect (through a field link or a cycle), so it never
  fabricates a blame. `TestGuardContainer` extended to assert the blame branch on all three
  violation scenarios; 5/5 green, nolink + cst-analysis suites unaffected.

### Done (2026-07-14, kotlin trunk — @Independent breadth)

- **@Independent on an abstract method.** `guardMethod` now also verifies `@Independent`: every implementation
  whose computed `INDEPENDENT_METHOD` is **decided DEPENDENT** (`isDependent()`, value 0 — not delayed, not HC) is
  reported, mirroring the `@NotModified` branch. Gate on the contract being at-least-HC-independent
  (`isAtLeastIndependentHc()`). Shared implementation-iteration + finding emission factored into
  `implementationsOf` / `reportViolation`. Independence violations carry the contract-provenance cause; a deep
  "why" blame (which field/return exposes state) is deferred. Test `TestGuardIndependent` (a `List`-field-exposing
  impl is flagged, a `List.copyOf` impl is not); 1/1 green, container + nolink unaffected.
  Nuance deferred: a contract of *full* `@Independent` (2) against an implementation computed `INDEPENDENT_HC` (1)
  is weaker but not "dependent"; v1 flags only decided-DEPENDENT implementations.
- **Hierarchy monotonicity (concrete parents).** `guardOverride` covers the case the abstract→`IMPLEMENTATIONS`
  path misses: a **concrete** method contracted `@NotModified`/`@Independent`, overridden by a subclass method
  that modifies or becomes dependent. Walks each concrete method's `overrides()` upward, skips abstract parents
  (handled by `guardMethod`) and external-library parents, and diffs the parent's contract against the override's
  **genuinely computed** value (confirmed: the override is not tainted by the parent's trusted contract — a
  modifying override of a `@NotModified` base method is detected, with the "assigns field 'n'" blame). Test
  `TestGuardHierarchy`; 1/1 green.

### Done (2026-07-15, kotlin trunk — @Immutable on a type)

- **`guardImmutable`.** `@Immutable`/`@ImmutableContainer` on a type is now verified **rule by rule**, so a finding
  names both the rule and the member that breaks it, blamed on that member with the contract location as cause:
  rule 0 (field assignable after construction), rule 1 (field modified; plus the §050 variant for *abstract* methods
  that are modifying), rule 2 (non-private field of non-immutable type), rule 3 (field dependent — exposed or shared).
  At most one finding per field: the rules are ordered and a field failing rule 0 usually fails others too. Concrete
  modifying methods need no check of their own — to modify, they must assign or modify a field, which rule 0/1 catches
  there. Mirrors `TypeImmutableAnalyzerImpl`, but reads **per-member** computed values, never the type's own
  `IMMUTABLE_TYPE`: a contracted type carries the trusted contract there (the analyzer early-returns), exactly as
  `guardContainer` reads parameters rather than `CONTAINER_TYPE`. Test `TestGuardImmutable`, 7/7 green (one per rule,
  a conforming positive control, an eventual-immutability control, a guard-disabled control); full
  modification-analyzer + common suites green (183 tests).
- **Two false-positive traps found and avoided**, both worth knowing about:
  1. **`FieldInfo.isPropertyFinal()` reads `FINAL_FIELD` with `getOrDefault(…, FALSE)`**, so an *undecided* field
     reads as "not final". `TypeImmutableAnalyzerImpl` may use it (a wrong guess only delays a fixed point), but a
     guard may not — it would blame on incomplete information. `guardImmutable` uses `isFinal()` (the modifier) plus
     a `getOrNull(FINAL_FIELD)` decided-FALSE instead.
  2. **`ContractReader.contracts(TypeInfo)` always contains `INDEPENDENT_TYPE`**, annotated or not:
     `AnnotationToProperty` calls `simpleComputeIndependent(typeInfo, immutable)` whenever no `@Independent`
     annotation is present, which returns DEPENDENT — or even INDEPENDENT for an unannotated type whose non-private
     methods only speak primitives. So its presence is *no evidence the user wrote anything*, and a type-level
     `@Independent` guard keyed off it would fire on unannotated code. Hence there is deliberately no such guard;
     rule 3 is covered per field/parameter inside `guardImmutable`. (`IMMUTABLE_TYPE` and `CONTAINER_TYPE` are safe:
     they are only ever set from a real annotation.)
- **Eventual immutability is skipped.** A type with `after="…"` on any annotation is not guarded: the fields carrying
  the state transition are assignable and the marker methods modifying *by design*, so the rules only hold after the
  mark — which the analyzer cannot see (§060: contracted eventual immutability is unimplemented). Without this gate,
  `SimpleImmutableSet1` reports its own design as a rule-0 violation.

### Done (2026-07-15, kotlin trunk — @Independent completed)

- **`@Independent` on a type.** `guardIndependentType` reports every field whose computed `INDEPENDENT_FIELD` is
  decided DEPENDENT — the accessible content escaping, which is exactly what §080's definition forbids. Runs **only
  in the absence of an `@Immutable` contract** (that one already covers independence as its rule 3; reporting the
  same field twice is noise), and **only on an explicitly written `@Independent` annotation**, checked against
  `typeInfo.annotations()` rather than the contract map — see the note on trap 2 below, which is now confirmed.
  Fields only, deliberately: abstract methods and their parameters also carry independence values, but for an
  abstract method those may come from `ShallowMethodAnalyzer`'s `DEPENDENT_DEFAULT` rather than a computation — the
  same "a default is not a decision" trap as `isPropertyFinal()`.
- **Blame for `@Independent` violations.** `blameMethodDependent` finds the first `return <field-of-this>` — the
  classic exposure (`return data;`) that links the return value to the accessible content — and adds it as the top
  cause, located at that return statement. Both `guardMethod` and `guardOverride` now pass it instead of `null`, so
  an independence why-chain reads `[ "returns the field 'list' itself, exposing it", "@Independent contracted here" ]`.
  Returns `null` when the exposure is indirect (through a call, a wrapper, a parameter): naming the exposed field in
  general needs link data, so the violation is still reported, just without the deepest "where".
- Tests: `TestGuardIndependentType` (dependent field reported; `GetterSetter` and a copying constructor stay silent;
  unannotated types not considered), `TestGuardIndependent` extended to assert the blame branch. 17 guard tests over
  5 classes; modification-analyzer + common green (186 tests).
- **Trap 2 confirmed, with a caveat.** Instrumenting the branch with the gate removed shows `a.b.X` and
  `a.b.X.OnlyPrimitives` — neither carrying any annotation — entering `guardIndependentType` with a synthesized
  `contract=@Independent`. So the premise is real. What could **not** be constructed is an unannotated type
  producing a false *finding*: the same condition that synthesizes INDEPENDENT (a non-private API speaking only
  primitives) tends to leave every field independent too. The gate is therefore principled + defence in depth rather
  than a fix for a demonstrated bug; the test says so explicitly.

### Coverage today

| Contract | On a type | On an abstract method | On a concrete method (override) |
|---|---|---|---|
| `@Container` | yes (own + inherited method parameters) | — | — |
| `@Immutable` | yes (rules 0-3, per member) | — | — |
| `@NotModified` | — | yes (implementations) | yes (`guardOverride`) |
| `@Independent` | yes (dependent fields; explicit annotation only) | yes (decided-DEPENDENT only) | yes (decided-DEPENDENT only) |

Blame coverage: `@Container`/`@NotModified` violations get a direct-site walk (receiver, argument, field assignment);
`@Independent` violations get the `return <field>` walk; `@Immutable` violations name the rule and the member.
Everything else reports without a "where".

Not covered: `@NotModified`/`@Independent` contracted directly **on a parameter or field**; `@Container(contract=true)`
on a parameter (§070 dynamic checking); the `@Immutable`-strength nuance (contracted `@Immutable` (3) vs computed
`IMMUTABLE_HC` (2)), mirroring the same deferred nuance for `@Independent` (2 vs 1) — v1 flags only the unambiguous
"not immutable at all" / "decided dependent" cases. Independence of *abstract* members is not guarded (shallow
defaults, above).

### Next steps

1. **Deepen Phase 3 further**: indirect modification (parameter linked to a modified field;
   modification one call removed), and reporting *all* modification sites rather than the
   first. Today's walk is direct-site, first-match — enough for the common "modifies the
   argument" story, best-effort beyond it.
   **This needs the link module.** The canonical shape is §040's `ErrorRegistry`: `add(@Modified ErrorMessage
   message)` violates a `@Container` contract, yet *nothing in `add`'s body modifies anything* — the modification is
   in `changeFirst`, reached through the field. Detection is local (the parameter's `UNMODIFIED_PARAMETER` already
   carries it, which is why `TypeContainerAnalyzerImpl` needs nothing more); the *explanation* is not. Blame has to
   follow parameter → field → the method modifying the field's object graph, which is link data the CST cannot
   supply, and which `blameParameterModified` today answers with `null` (violation reported, no "where").
2. Deepen the `@Independent` blame beyond `return <field>`: exposure through a call (`return wrap(data)`), through a
   parameter, or from a constructor storing an argument without copying. For the last one,
   `PARAMETER_ASSIGNED_TO_FIELD` (`Value.AssignedToField`, computed) names the fields a parameter is assigned to, and
   would give "parameter 'ts' is assigned to field 'data'" without any link data — the cheapest next win.
3. More guarded contracts: `@NotModified`/`@Independent` contracted directly on a parameter or field;
   `@Container(contract=true)` on a parameter (dynamic checking, §070). See the coverage table above.
4. The strength nuances: contracted `@Immutable` (3) vs computed `IMMUTABLE_HC` (2), and contracted `@Independent`
   (2) vs computed `INDEPENDENT_HC` (1). Both are genuine weakenings that v1 accepts silently. Note the trap for the
   first: `@Immutable` without `hc` on an *abstract or extensible* type must be read as `IMMUTABLE_HC` — the
   appendix says `hc=true` is implicitly present there — so a naive 3-vs-2 diff would fire on every
   `@ImmutableContainer interface`.
5. Surface findings in the gradle/maven plugins (currently only run-main/run-openjdk).
6. Consider deduplication policy when both an interface contract and a subtype contract are
   violated by the same parameter.
