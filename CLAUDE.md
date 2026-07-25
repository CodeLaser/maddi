# maddi — notes for AI assistants

Before quoting a `slowTest` run as evidence for an engine change, read **`AGENTS.md` §Commands**:
a green corpus run can be cached, skipped, vacuous, or heap-starved, and each looks like success.
Force the re-run and read the per-test roll-call.

Orientation: **`ARCHITECTURE.md`** (pipeline, module map, reading paths by intent),
**`AGENTS.md`** (tool-agnostic assistant guidance: commands, engine facts, working style),
**`CONTRIBUTING.md`** (build/test workflow), **`docs/README.md`** (index of cross-module
working notes, with status labels).

Before reasoning about immutability, modification, independence, linking, or the analyzer's
convergence machinery, read **`road-to-immutability/llm-summary.md`** — a condensed, maintained
reference of the project's authoritative vocabulary, the four immutability levels and their rules,
the link system, and the iterating analyzer. It exists so you do not have to re-read the full book;
open individual chapters of `road-to-immutability/src/docs/asciidoc/sections/` only when the summary
lacks detail.

Deeper technical references, in reading order per topic:

- Link engine: `maddi-modification-link/linking-manual.md` (start at §5 LinkMethodCall + §6 worked
  examples; `TestLinkMethodCall` is the spec-by-example), `maddi-modification-link/README.md`
  (link-nature combination table), `maddi-modification-link/src/main/java/.../vf/virtual-fields.md`.
- Shared-variable reconstruction: `maddi-modification-link/sv-reconstruction-techniques.md`.
- Parsing stability (javac thread-hostility): `maddi-inspection-openjdk/parsing-stability.md`.

## ⚠️ Test corpora: use the locator, never hardcode a path

When a test reads the clone-bench corpus, **do not** hardcode a path such as
`"../../testarchive/…"`. Resolve it via `CloneBenchCorpus`
(`maddi-modification-analyzer` test scope): `TESTARCHIVE_ROOT` / `-Dtestarchive.root`,
else the default sibling checkout. Remember the corpus lives on the **`analyzed`
branch** of `testarchive`.

maddi is a standalone OSS project **upstream** of the jfocus repos, so it does **not**
use their `CodeLaserCorpus` / `TEST_CODELASER_ROOT` test fixture — keep maddi's corpus
handling self-contained here.
