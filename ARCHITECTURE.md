# maddi — architecture and module map

This is the orientation document for developers (and LLM assistants) meeting the codebase for the
first time. It answers three questions: *what is the pipeline*, *what does each module do*, and
*where do I start reading for a given task*. For the concepts behind the analyzer (immutability
levels, modification, linking, independence), read
[`road-to-immutability/llm-summary.md`](road-to-immutability/llm-summary.md) first — this document
deliberately does not repeat it.

> Historical note: the root Java package is `org.e2immu.*`, after the project's predecessor.
> The project, artifacts and distributions are all named **maddi**. Do not "fix" package names
> ad hoc; a coordinated rename is planned.

## The pipeline

maddi is a whole-program static analyzer: it parses a project into a language-agnostic syntax
tree, then iterates an analysis to a fixpoint, producing modification / independence /
immutability verdicts as *properties* attached to types, methods and fields.

```
 Java source ────┐
 Kotlin source ──┼──► front end ("inspection") ──► CST — one Common Syntax Tree
 .class files ───┘                                  │    for all input languages
                                                    ▼
                                   PrepAnalyzer  (maddi-modification-prepwork)
                                   call graph, analysis order, part-of-construction,
                                   per-method variable data
                                                    ▼
                                   IteratingAnalyzer  (maddi-modification-analyzer)
                                   multi-phase fixpoint; invokes the link engine
                                   (maddi-modification-link) per method
                                                    ▼
                                   properties on types/methods/fields
                                   (element.analysis(), write-once)
                                                    ▼
                  annotations · JSON analysis results · plugin/IDE reporting
```

Two side channels feed the pipeline:

- **Annotated APIs (AAPI)**: curated annotations for library types without source
  (`maddi-aapi-archive`, compiled to JSON by `maddi-aapi-parser`), preloaded into the property
  store — without them the positive immutability side barely concludes.
- **CST I/O**: analysis results and CST fragments are (de)serialized by `maddi-cst-io`, which is
  how results are cached, preloaded and shipped.

## Layers and modules

Dependencies point strictly downward. Layer by layer, bottom up:

### Foundation

| Module | Purpose | Start reading |
|---|---|---|
| `platform` | Gradle `java-platform` BOM; single source of external dependency versions. | `platform/build.gradle.kts` |
| `maddi-support` | The published annotations (`@Immutable`, `@Container`, `@Modified`, …) plus "eventually final" support classes (`SetOnce`, `Freezable`). The only library a user's own code compiles against (Java 17; on Maven Central as `io.codelaser:maddi-support`). | `org.e2immu.annotation.Immutable`, `org.e2immu.support.SetOnce` |
| `maddi-util` | Small utilities (Trie, string/list helpers, get/set name derivation). | `org.e2immu.util.internal.util.Trie` |
| `maddi-graph` | Graph layer over JGraphT: dependency graphs, SCCs, cycle breaking. | `org.e2immu.util.internal.graph.G`, packages `graph/op`, `graph/analyser` |

### CST — the Common Syntax Tree

One language-agnostic tree ("Common", not "Concrete") shared by the Java and Kotlin front ends;
everything downstream of parsing operates on it exclusively.

| Module | Purpose | Start reading |
|---|---|---|
| `maddi-cst-api` | The interfaces: elements, expressions, statements, types, `TypeInfo`/`MethodInfo`/`FieldInfo`, and the central `Runtime`/`Factory` facade that builds trees. | `…cst.api.runtime.Runtime`, `…cst.api.element.Element`, `…cst.api.info.TypeInfo` |
| `maddi-cst-impl` | The implementation: node classes, `RuntimeImpl`/`FactoryImpl`, constant evaluation, Java printing. Largest module. | `…cst.impl.runtime.RuntimeImpl` |
| `maddi-cst-analysis` | The property/value model in which all analyzer verdicts are stored (`PropertyImpl`, `ValueImpl`, property maps, messages). | `…cst.impl.analysis.PropertyImpl` |
| `maddi-cst-io` | Codec: (de)serialization of CST and analysis results to/from JSON. | `…cst.io.CodecImpl` |
| `maddi-cst-print` | Language-neutral formatter: renders the `OutputElement` IR to formatted text. | `…cst.print.formatter2.Formatter2Impl` |
| `maddi-cst-print-kotlin` | Prints the CST as *Kotlin* source (same `OutputElement` IR; the target language is a printer choice, not a CST property). | `…cst.print.kotlin.KotlinTypePrinter`, `kotlin-printing.md` |

### Inspection — the front ends

Every front end follows the same two-tier pattern: a lower **CST-producing** module (grammar /
compiler binding → CST) and a thin **driver** module implementing the shared `JavaInspector`
contract from `maddi-inspection-api`.

| Front end | CST producer | Driver |
|---|---|---|
| Hand-written Java parser (CongoCC) | `maddi-java-parser` (+ `maddi-java-bytecode` for `.class` deps, via ASM) | `maddi-inspection-integration` |
| javac / OpenJDK (the **primary** front end) | `maddi-java-openjdk` (drives javac internals; needs `--add-exports`) | `maddi-inspection-openjdk` |
| Kotlin K2 Analysis API | `maddi-kotlin-k2` (PSI/FIR → CST) | `maddi-inspection-kotlin` |
| Mixed Java + Kotlin | — (composes the two above over one shared core) | `maddi-inspection-mixed` |

Shared machinery:

| Module | Purpose | Start reading |
|---|---|---|
| `maddi-inspection-api` | Contracts for the whole layer: `JavaInspector`, `Resolver`/`Context`, `CompiledTypesManager`, `InputConfiguration`. | `…inspection.api.integration.JavaInspector` |
| `maddi-inspection-parser` | Language-neutral resolution/binding: overload resolution, type contexts, generics, Lombok. *Not a source parser despite the name.* | `…inspection.impl.parser.ResolverImpl` |
| `maddi-inspection-resource` | Input configuration, source sets, parse results, the shared `InfoByFqn` type registry. | `…inspection.resource.InfoByFqn` |

Essential reading before running or debugging anything on the javac front end:
[`maddi-inspection-openjdk/parsing-stability.md`](maddi-inspection-openjdk/parsing-stability.md)
(javac is not thread-safe; the flake it causes and the built-in fix) and
[`maddi-inspection-openjdk/calling-the-javainspector.md`](maddi-inspection-openjdk/calling-the-javainspector.md).

### Analysis — the heart of the project

| Module | Purpose | Start reading |
|---|---|---|
| `maddi-modification-common` | Shared base: shallow/default analyzers that turn annotations into properties for library types; get/set handling. | `…modification.common.defaults.ShallowAnalyzer` |
| `maddi-modification-prepwork` | Phase 0 per primary type: call graph, analysis order, part-of-construction, final-field status, hidden content, per-method variable data. **Consuming the call graph? read the two conventions below first.** | `…modification.prepwork.PrepAnalyzer`, `…prepwork.callgraph.ComputeCallGraph` |
| `maddi-modification-link` | The **link engine**: computes `(from, nature, to)` links between variables, method-call linking, virtual fields (`§`), functional-interface lifting, shared-variable collapse. | `linking-manual.md` §5, then `…link.impl.LinkMethodCall`; `TestLinkMethodCall` is the spec-by-example |
| `maddi-modification-analyzer` | The **orchestrator**: multi-phase fixpoint iteration with worklist, certification and cycle breaking; also guard (contract-check) mode. | `…analyzer.impl.IteratingAnalyzerImpl`, then `SingleIterationAnalyzerImpl`; `README.md` for the phases, `definitions.md` for the concept chain |
| `maddi-aapi-archive` | The curated annotated-API content: hand-written annotated stubs for JDK/library packages + their compiled JSON under `src/main/resources`. Pure data. | `…aapi.archive.jdk.JavaUtil` |
| `maddi-aapi-parser` | Compiles the archive's stubs into machine-readable JSON analysis results. | `…aapi.parser.AnalysisHintsCompiler` |

#### Two call-graph conventions that will silently mislead a reader

Both are deliberate, both are load-bearing, and both make a real relationship invisible to the obvious
way of looking for it. Nothing is missing from the graph and nothing errors — the relation is simply not
where you looked. Full detail in `ComputeCallGraph`'s edge-type comment (types A–E).

1. **A method's access to its own type's fields is recorded backwards**: `field -> method`, not
   `method -> field`. The arrow encodes "this value flows into that method", which is what the analysis
   order needs; reversing it would put every accessor ahead of the state it reads. Access to *another*
   type's field keeps the ordinary direction, so a single outgoing walk collects foreign access and
   misses self-access.
2. **A lambda or anonymous class is its own vertex.** Its outgoing edges belong to that synthetic
   `TypeInfo`, not to the method it is written inside, so every dependency a lambda introduces looks
   like a dependency of the *type declaration*. Fold them back with `TypeInfo.enclosingMethod` when
   working at member granularity.

If you are asking "what does this member touch", you need both corrections. Consumers have been caught
by each of them; the second one silently under-reported every member-move lever in the refactoring
metrics until it was found.

### Runners, plugins, IDE integration

| Module | Purpose |
|---|---|
| `maddi-run-config` | Shared run configuration, error reporting, exit codes; used by every runner and both build plugins. |
| `maddi-run-openjdk` | **The main CLI** (`bin/maddi`, distZip): analyzer on the javac front end. Also home of the large-corpus smoke tests (`@Tag("slow")`), which run against the external `test-oss` corpus (see `CONTRIBUTING.md` §Building and testing; managed by the `CodeLaser/maddi-oss` repo). |
| `maddi-run-kotlin` | CLI for mixed Java+Kotlin projects (`bin/maddi-kotlin`); how Kotlin support ships (the K2 jars are not on Maven Central, they ride along in `lib/`). |
| `maddi-run-main` | CLI on the hand-written-parser front end. |
| `maddi-run-rewire` | Small driver for incremental/partial re-analysis (rewiring), inspector-implementation-agnostic. |
| `maddi-gradleplugin` | Gradle plugin (`org.e2immu.analyzer-plugin`); shades the whole Java analyzer stack so it is self-contained. |
| `maddi-mvnplugin` | Maven plugin, same shading approach; hand-maintained `plugin.xml`. |
| `maddi-ide-daemon` | Out-of-process analysis daemon (NDJSON over a loopback socket) that all IDE front ends launch and query. |
| `maddi-ide-client` | IDE-agnostic daemon client + DTOs; plain JDK + Jackson, Java 21, published to Maven local for the Eclipse build. |
| `maddi-intellij` | IntelliJ plugin (talks to the daemon; no direct analyzer dependency). |
| `maddi-eclipse` | Eclipse plugin — separate Maven/Tycho reactor, not a Gradle module. |
| `maddi-vscode` | VS Code extension (TypeScript, npm); reimplements the wire contract, bundles the daemon. |
| `maddi-manual` | The user-facing manual (AsciiDoc → HTML/PDF): getting started, plugins, CLI, configuration, exit codes. |
| `road-to-immutability` | The concepts book (AsciiDoc) + its maintained digest `llm-summary.md`. |

Not in the Gradle build: `testgradleplugin-*` and `testmvnplugin-export` (plugin test fixtures,
currently excluded/Maven-based), `maddi-eclipse` (Tycho), `maddi-vscode` (npm), `target/`
(JReleaser staging output).

## What is published, and for whom

Publishing is not yet active; [`PUBLISHING.md`](PUBLISHING.md) records the agreed strategy.
In short: the analyzer modules are **not** published as a fine-grained library. Consumers get
exactly three things:

1. **Annotations** — `io.codelaser:maddi-support` on Maven Central (the one artifact user code
   compiles against);
2. **Build plugins** — Gradle and Maven plugins, each self-contained by shading the Java
   analyzer stack;
3. **CLI distributions** — self-contained zips from `maddi-run-openjdk` (Java) and
   `maddi-run-kotlin` (mixed), released via `release-cli.sh`.

The IDE plugins are delivered separately and talk to the bundled daemon.

## Reading paths by intent

- **"I want to understand what maddi computes"** —
  [`road-to-immutability/llm-summary.md`](road-to-immutability/llm-summary.md), then the book
  chapters it points to; `maddi-modification-analyzer/definitions.md` for the compact
  concept chain.
- **"I want to run maddi on a project"** — the manual
  (`maddi-manual/src/docs/asciidoc/`), then `maddi-run-openjdk/running-examples.md`.
- **"I want to work on the link engine"** —
  `maddi-modification-link/linking-manual.md` starting at §5 (LinkMethodCall) + §6 (worked
  examples); `TestLinkMethodCall` as executable spec; the module `README.md` for the
  link-nature combination table; `vf/virtual-fields.md` for `§` virtual fields.
- **"I want to work on the iterating analyzer / convergence"** —
  `maddi-modification-analyzer/README.md` (phases), llm-summary §Convergence, then
  `IteratingAnalyzerImpl`.
- **"I want to work on a front end"** — `maddi-inspection-api`'s `JavaInspector`, then the
  producer/driver pair for your language (table above); for javac, read
  `parsing-stability.md` *before* anything else.
- **"I want to touch the printers/formatter"** — `docs/formatter-analysis.md`,
  `maddi-cst-print`, and `maddi-cst-print-kotlin/kotlin-printing.md`.
- **"I want to work on the plugins or IDE integration"** — `maddi-run-config`, then the plugin
  module; for IDEs, `maddi-ide-daemon`'s `DaemonProtocol` and `Taskfile.yml` (the IDE build
  tasks); status in `docs/eclipse-plugin-state.md` and `docs/ide-todo.md`.

## Documentation map

- Maintained references: this file; `road-to-immutability/llm-summary.md` (concepts);
  the linking manual and analyzer `definitions.md`/`README.md` (engine);
  `parsing-stability.md` (javac); the manual (users); `PUBLISHING.md` (release strategy, not
  yet active);
  [`CONTRIBUTING.md`](CONTRIBUTING.md) (workflow).
- Working notes and plans: [`docs/README.md`](docs/README.md) is the index; module-local notes
  live in each module (`notes/`, `sv-*.md` journals in `maddi-modification-link`).
- For LLM assistants: [`AGENTS.md`](AGENTS.md) (tool-agnostic) and [`CLAUDE.md`](CLAUDE.md).
