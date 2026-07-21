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
| [independent-type-optimism.md](independent-type-optimism.md) | note | Reproduced defect: `INDEPENDENT_TYPE` can be frozen at an optimistic value; why the obvious fix unmasks a second inconsistency. |
| [sam-linking-reconciliation.md](sam-linking-reconciliation.md) | note | What actually diverges between the two SAM conventions: contract vs inference, not virtual fields; retires two theories. |
| [eventual-immutability.md](eventual-immutability.md) | plan | Bringing road-to-immutability §060 back into the engine: contract the support classes, propagate; no preconditions. |
| [formatter-analysis.md](formatter-analysis.md) | note | Analysis of the `maddi-cst-print` formatter. |
| [formatter-doc-ir-plan.md](formatter-doc-ir-plan.md) | plan | Rewrite plan: a Doc IR for `maddi-cst-print`. |
| [prep-analyzer hardening.md](prep-analyzer%20hardening.md) | plan | Robustness hardening roadmap for `maddi-modification-prepwork`. |
| [modification-link-analyzer hardening.md](modification-link-analyzer%20hardening.md) | plan | Real-world robustness hardening roadmap for the link analyzer. |
| [TEST_MIGRATION.md](TEST_MIGRATION.md) | plan | Migrating tests from `maddi-inspection-integration` to `maddi-java-openjdk`. |
| [discrepancies openjdk-maddi parsers.md](discrepancies%20openjdk-maddi%20parsers.md) | note | Observed differences between the openjdk (javac) and hand-written (CongoCC) Java front ends. |
| [regression-jdk-preload-jmodless-alternative-jre.md](regression-jdk-preload-jmodless-alternative-jre.md) | note | Regression report: JDK preload failing on a jmod-less `alternativeJREDirectory`. |
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
