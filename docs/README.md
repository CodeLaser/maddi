# docs/ — index

This directory holds **cross-module working documents**: design notes, hardening roadmaps and
investigation reports that do not belong to a single module. The *maintained reference
documentation* lives elsewhere — see [the documentation map in the root README](../README.md)
and the pointers at the bottom of this page.

Each entry below is labeled:

- **plan** — an execution plan or roadmap; describes intended work, possibly partially done.
- **note** — a dated design/investigation note; accurate for the code as it was on that date,
  kept for the reasoning, not guaranteed to track later changes.
- **status** — a living state-of-the-union / TODO document, updated as work progresses.

| Document | Status | What it covers |
|---|---|---|
| [analysis-rewiring.md](analysis-rewiring.md) | note | The `analysisFingerprint` mechanism and how analysis results are rewired onto a re-parsed CST. |
| [rewiring.md](rewiring.md) | note | How CST rewiring works in general, and its pitfalls. |
| [partial-reparse-rewire.md](partial-reparse-rewire.md) | note | Emitting `.class` files from the openjdk inspection pass; partial re-parse/re-wire for IDE use. |
| [guard-mode-analysis.md](guard-mode-analysis.md) | note | Guard mode (contract verification): analysis and design proposal. |
| [dynamic-immutability-feasibility.md](dynamic-immutability-feasibility.md) | note | A field's dynamic immutability: materialized (part 1) and consumed (part 3) from a hand-written contract, with a local guard check; inference (part 2) still open. |
| [independent-type-optimism.md](independent-type-optimism.md) | note | Reproduced defect: `INDEPENDENT_TYPE` can be frozen at an optimistic value; why the obvious fix unmasks a second inconsistency. |
| [sam-linking-reconciliation.md](sam-linking-reconciliation.md) | note | What actually diverges between the two SAM conventions: contract vs inference, not virtual fields; retires two theories. |
| [builder-interface-split-impact.md](builder-interface-split-impact.md) | note | Costing the two ways to stop the mutable Builder capping the read-only Inspection interfaces; measured, no refactor landed. |
| [eventual-immutability.md](eventual-immutability.md) | plan | Bringing road-to-immutability §060 back into the engine: contract the support classes, propagate; no preconditions. |
| [eventual-info-hierarchy.md](eventual-info-hierarchy.md) | note | Why the `Info`/`*InfoImpl` family is `@Mutable`, and the method-level `@NotModified(after=)` primitive built to unblock it (steps 1–5); the cross-reference cluster that remains. |
| [formatter-analysis.md](formatter-analysis.md) | note | Analysis of the `maddi-cst-print` formatter. |
| [formatter-doc-ir-plan.md](formatter-doc-ir-plan.md) | plan | Rewrite plan: a Doc IR for `maddi-cst-print`. |
| [prep-analyzer hardening.md](prep-analyzer%20hardening.md) | plan | Robustness hardening roadmap for `maddi-modification-prepwork`. |
| [handoff-from-jfocus-standardize.md](handoff-from-jfocus-standardize.md) | note | Two defects found from downstream: `PART_OF_CONSTRUCTION` marks call-graph-reached types as prepped (trips the link computer's `assert vd != null`), and `ClassSymbolScanner.convert` has no `BottomType` case. Plus a request to disambiguate the two meanings of `NYI`. **Replied 2026-07-26:** issue 1 fixed in both halves — the guard now reads `PrepAnalyzer.PREPPED`, and `go()` leaves `PART_OF_CONSTRUCTION`/`FINAL_FIELD` undecided for any type whose bodies were not analysed (binary types included, though measurably few); issues 2 and 3 untouched. **Issues 2 and 3 resolved 2026-07-26** — see the row below. |
| [handoff-nulltype-classsymbolscanner.md](handoff-nulltype-classsymbolscanner.md) | note | Handover to the standardize thread for issues 2 and 3 of the above: `ClassSymbolScanner:1064` has no `BottomType` case, `Type$2` is javac's *recovery* type (identified from the JDK sources), and the throw is unreachable in every maddi/jfocus path measured here — so the reported 128 are specific to that intake pipeline. Includes the probe, the ruled-out paths, and maddi's non-obvious working rules. **Resolved 2026-07-26** (`ws/std2`): the `<nulltype>` is javac's marker for an *unresolvable annotation enum constant* (`ClassReader` substitutes `syms.botType` and warns; maddi keeps only ERRORs) — not the type of `null`, so §6's `BOT` case was deliberately not added; §2a/§4/§5 corrected. See [`maddi-java-openjdk/notes/nulltype-from-unresolved-annotation-enum-constant.md`](../maddi-java-openjdk/notes/nulltype-from-unresolved-annotation-enum-constant.md). |
| [handoff-isolatemethod-remaining.md](handoff-isolatemethod-remaining.md) | note | The three `IsolateMethod` issues left after the closed-core hardening round — **all three resolved 2026-07-28** (`c94b5d2f`, `78524232`), corpus now **40 of 40 written and 39 parsing back**, up from 39/37. Records what each one actually was, twice differing from the original diagnosis: the `MethodPrinterImpl` NPE was not about a bare `return;` but a printer branch that could only ever NPE, over an annotation stub that should never have had a body; and the namespace-qualification fix was blocked one level down, by `TypePrinter`'s two-argument `print` resetting to the default printers. §4 is the defect the second of these was masking, **also fixed**: placement was decided by the *first* reference to a type, and a reconstructed reference carries no evidence, so a later verbatim fully-qualified one could name something that did not exist. Since a stub's enclosing type is final, the traversal now runs twice — a probe round for the evidence, then the round that keeps the stubs — which costs 3 s on a 1 m 43 s corpus run and, unlike a scan-only mode or a second visitor, cannot drift from the traversal it describes. Records the one case no placement can satisfy (a type written both simply and package-qualified), which closed-core turns out to contain. |
| [modification-link-analyzer hardening.md](modification-link-analyzer%20hardening.md) | plan | Real-world robustness hardening roadmap for the link analyzer. |
| [handoff-saturated-closure-collapse.md](handoff-saturated-closure-collapse.md) | plan | Why the link engine's per-method work ceiling fires: not length, not a cliff, not a tunable budget — a **saturated closure** (median 55% of all variable pairs linked, some 100%), which is semantically correct. Proposes collapsing such a group to one fact instead of N². Self-contained; reproduction needs no corpus. Includes two negative results and the witness-selection question to settle first. |
| [TEST_MIGRATION.md](TEST_MIGRATION.md) | plan | Migrating tests from `maddi-inspection-integration` to `maddi-java-openjdk`. |
| [discrepancies openjdk-maddi parsers.md](discrepancies%20openjdk-maddi%20parsers.md) | note | Observed differences between the openjdk (javac) and hand-written (CongoCC) Java front ends. |
| [regression-jdk-preload-jmodless-alternative-jre.md](regression-jdk-preload-jmodless-alternative-jre.md) | note | Regression report: JDK preload failing on a jmod-less `alternativeJREDirectory`. |
| [landing-surface-checklist.md](landing-surface-checklist.md) | plan | Everything an outsider hits before reading any code: e2immu.org redirect, repo metadata, CI, publishing the book, the 0.9.0 release, the LGPL/permissive licence split. |
| [eclipse-plugin-state.md](eclipse-plugin-state.md) | status | Eclipse plugin: state of the union. |
| [ide-todo.md](ide-todo.md) | status | IDE front ends (IntelliJ/Eclipse/VS Code): what is *not* done yet. |

## Where the maintained documentation lives

- **Concepts** (immutability, modification, linking, independence):
  [`road-to-immutability/llm-summary.md`](../road-to-immutability/llm-summary.md) — the condensed,
  maintained digest; the full AsciiDoc book is in
  [`road-to-immutability/src/docs/asciidoc/`](../road-to-immutability/src/docs/asciidoc/).
- **User manual** (running maddi, plugins, CLI, configuration):
  [`maddi-manual/src/docs/asciidoc/`](../maddi-manual/src/docs/asciidoc/).
- **Link engine**: [`maddi-modification-link/linking-manual.md`](../maddi-modification-link/linking-manual.md)
  and [`maddi-modification-link/README.md`](../maddi-modification-link/README.md).
- **Analyzer definitions and phases**:
  [`maddi-modification-analyzer/definitions.md`](../maddi-modification-analyzer/definitions.md),
  [`maddi-modification-analyzer/README.md`](../maddi-modification-analyzer/README.md).
- **Parsing stability (javac)**:
  [`maddi-inspection-openjdk/parsing-stability.md`](../maddi-inspection-openjdk/parsing-stability.md).
- **Calling the inspector from code**:
  [`maddi-inspection-openjdk/calling-the-javainspector.md`](../maddi-inspection-openjdk/calling-the-javainspector.md).

Module-specific working notes (bug reports, dated audits, sv-engine journals) stay inside their
module, typically in a `notes/` subdirectory or as `sv-*.md` files in `maddi-modification-link/`.
