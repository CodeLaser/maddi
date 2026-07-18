# maddi — notes for AI assistants

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
