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
| [eventual-design-improvements.md](eventual-design-improvements.md) | proposal | cst-api/cst-impl design follow-ups from the certification arc: the dogfood ratchet, conformance tests, `Memo` support types, and the asserted-contract compromise. Ready for implementation. |
| [formatter-analysis.md](formatter-analysis.md) | note | Analysis of the `maddi-cst-print` formatter. |
| [formatter-doc-ir-plan.md](formatter-doc-ir-plan.md) | plan | Rewrite plan: a Doc IR for `maddi-cst-print`. |
| [prep-analyzer hardening.md](prep-analyzer%20hardening.md) | plan | Robustness hardening roadmap for `maddi-modification-prepwork`. |
| [handoff-isolateclass-enum-and-generic-stubs.md](handoff-isolateclass-enum-and-generic-stubs.md) | note | Two `IsolateClass` stub defects, each a single decision in `IsolationCore`, each with a driver in `TestIsolateClass4Compiles` — **both resolved 2026-07-30**, taking the class-isolate corpus from **57 to 66 of 100 trees compiling** and from 93 to **100 of 100 parsing back**: an **enum stubbed as a class** (line ~184 reproduced the annotation and interface natures and let enum fall through, so `case CLASS:` got "pattern or enum constant required" — the fix is `typeNatureEnum` plus constants as *synthetic* fields, which is how `TypePrinterImpl.enumConstantStream` recognises them, plus a `java.lang.Enum<E>` parent the commit asserts), and a **method inherited from a generic supertype stubbed on the erased scope type** (lines 903–906, so `ListStack<T>.pop()` became `Object pop()` on the subtype — the fix places it on the declaring type, as the field branch thirty lines below already did, and it is what stopped 7 trees being dropped at re-parse on an unresolved builder-pattern call). Found by compiling fernflower isolates through the new `debug.isolateClass`; invisible before because the isolate tests re-parse with `failFast` false, where javac's errors are logged and carried. §7 records what the fixes turned out to be, including the **third defect the enum fix uncovered** — `ClassSymbolScanner.ensureField`, the lazy path, never applied the flags, so an enum constant read through it was not synthetic and not final; the same "which path loaded it decided the answer" shape as §4's interface-field defect, and it visibly changed the committed analysis archive. §4 records that earlier one (`9bacea3b`), and why the JLS rule had to go at the parse site rather than into `FieldInfo.isStatic()`. |
| [handoff-detailedsources-written-qualifier.md](handoff-detailedsources-written-qualifier.md) | note | **Open.** `DetailedSources.qualifier` returns the type that DECLARES a nested type, not the one the source wrote it through. `ClassSymbolScanner.iterateUpToPackageLevel` (~1261) climbs the resolved type's enclosing chain and files each step at the written token's position, so `HashMap.Entry` — legal Java for `Map.Entry` — records `java.util.Map` over the seven columns that spell `HashMap`. Nothing can then tell the two spellings apart, and an import computer emits `java.util.Map` for text that says `HashMap`. Found downstream: splitclass copies member text into a new type whose imports come from the CST, and javac reads the qualifier as a package (`package HashMap does not exist`, 4 errors over 3 class-isolate trees). `expression.type` carries the written type where the declaring one is being used. jfocus works around it by span arithmetic, which misses a same-length qualifier. |
| [isolate-class.md](isolate-class.md) | note | `IsolateClass`: lifting a whole type into a standalone JDK-only source tree — one compilation unit per stub, in the package the original came from. Records **why a project rather than one file**: the single-compilation-unit constraint is what produced all of `IsolateMethod`'s placement machinery, and a project has none of it. Twelve corpus rounds took closed-core's hundred largest types from 0 to **100/100 isolated and 94/100 trees parsing back (21,299 of 21,305 units)**; the eleven defects found are listed, nine of them in shared code, along with the two that were silent no-ops whose unit test passed anyway. One cause remains (§5): fluent chaining through a self-type generic. |
| [modification-link-analyzer hardening.md](modification-link-analyzer%20hardening.md) | plan | Real-world robustness hardening roadmap for the link analyzer. |
| [handoff-saturated-closure-collapse.md](handoff-saturated-closure-collapse.md) | plan | Why the link engine's per-method work ceiling fires: not length, not a cliff, not a tunable budget — a **saturated closure** (median 55% of all variable pairs linked, some 100%), which is semantically correct. Proposes collapsing such a group to one fact instead of N². Self-contained; reproduction needs no corpus. Includes two negative results and the witness-selection question to settle first. |
| [TEST_MIGRATION.md](TEST_MIGRATION.md) | plan | Migrating tests from `maddi-inspection-integration` to `maddi-java-openjdk`. |
| [discrepancies openjdk-maddi parsers.md](discrepancies%20openjdk-maddi%20parsers.md) | note | Observed differences between the openjdk (javac) and hand-written (CongoCC) Java front ends. |
| [regression-jdk-preload-jmodless-alternative-jre.md](regression-jdk-preload-jmodless-alternative-jre.md) | note | Regression report: JDK preload failing on a jmod-less `alternativeJREDirectory`. |
| [landing-surface-checklist.md](landing-surface-checklist.md) | plan | Everything an outsider hits before reading any code: e2immu.org redirect, repo metadata, CI, publishing the book, the 0.9.0 release, the LGPL/permissive licence split. |
| [eclipse-plugin-state.md](eclipse-plugin-state.md) | status | Eclipse plugin: state of the union. |
| [ide-todo.md](ide-todo.md) | status | IDE front ends (IntelliJ/Eclipse/VS Code): what is *not* done yet. |
| [doc-audit-2026-07-30.md](doc-audit-2026-07-30.md) | status | A pass over every `.md` in the repo for documents whose work has landed: what was deleted, what is kept because code cites it, and the per-document decisions still to make. |

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
