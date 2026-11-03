# maddi — architecture and module map

> **Three repositories.** Since 2026-09-30 maddi is split by tier
> ([plan](../roadmap/split-maddi-into-three-repositories.md); `tools/tiers/check_tiers.py` enforces it):
>
> - **maddi** (this repository) holds the **base**: CST, front ends, inspection, the call graph, the
>   run drivers, the `AnalysisEngine` interface, the annotated-API archive, the books, and `corpus/`,
>   the corpus tooling.
> - [**maddi-mod**](https://github.com/CodeLaser/maddi-mod) holds the modification analysis:
>   `maddi-modification-*`, `maddi-aapi-parser`, `maddi-run-analysis` and the analysis tests.
> - [**maddi-dist**](https://github.com/CodeLaser/maddi-dist) holds what users install: the daemon,
>   the IDE plugins, the MCP server, the Gradle and Maven plugins, the `maddi` / `maddi-kotlin` CLI
>   distributions, and `dogfood/`. Everything except the annotations is published from there.
>
> Modules named below that are not in this checkout live in those siblings, checked out next to it
> (`../maddi-mod`, `../maddi-dist`). The tables give each module's repository.

This is the orientation document for developers (and LLM assistants) meeting the codebase for the
first time. It answers three questions: *what is the pipeline*, *what does each module do*, and
*where do I start reading for a given task*. For the concepts behind the analyzer (immutability
levels, modification, linking, independence), read
[`road-to-immutability/llm-summary.md`](../../road-to-immutability/llm-summary.md) first. This document
deliberately does not repeat it.

> Historical note: the root Java package was `org.e2immu.*`, after the project's predecessor. The
> coordinated rename landed on 2026-08-17 and shipped in 0.9.1: it is now `io.codelaser.maddi.*`, in
> line with the project, artifacts and distributions, which were already named **maddi**. What an
> existing user had to change is in [`docs/project/release-notes-0.9.1.md`](../project/release-notes-0.9.1.md).

## The pipeline

maddi is a whole-program static analyzer. It parses a project into a language-agnostic syntax
tree, then iterates an analysis to a fixpoint. The result is modification, independence,
immutability and nullability verdicts, stored as *properties* on types, methods and fields.

```
 Java source ────┐
 Kotlin source ──┼──► front end ("inspection") ──► CST: one Common Syntax Tree
 .class files ───┘                                  │   for all input languages
                                                    ▼
                                   call graph, analysis order  (maddi-callgraph, base)
                                                    ▼
                        ┄┄┄┄┄┄┄┄┄┄ AnalysisEngine  (maddi-analysis-api, base) ┄┄┄┄┄┄┄┄┄┄
                                   found as a service; implemented in maddi-mod
                                                    ▼
                                   PrepAnalyzer  (maddi-modification-prepwork)
                                   part-of-construction, final fields,
                                   per-method variable data
                                                    ▼
                                   IteratingAnalyzer  (maddi-modification-analyzer)
                                   multi-phase fixpoint; invokes the link engine
                                   (maddi-modification-link) per method
                                                    ▼
                                   properties on types/methods/fields
                                   (element.analysis(), write-once)
                                                    ▼
       annotations · JSON analysis results · CLI, build plugins, IDE daemon, MCP server
       · Java → Kotlin / C# / Rust translation (the printers read the verdicts)
```

The base never depends on maddi-mod at compile time. A run driver (`maddi-run-openjdk`,
`maddi-run-kotlin`) finds the modification analysis through `AnalysisEngines` when
`maddi-run-analysis` is on the runtime class path. Without it, only a parse-only run
(`--analysis-steps=none`) works.

Two side channels feed the pipeline:

- **Annotated APIs (AAPI)**: curated annotations for library types without source. They live in
  `maddi-aapi-archive` (base), are compiled to JSON by `maddi-aapi-parser` (maddi-mod), and are
  preloaded into the property store. Without them the analysis barely concludes that anything is
  immutable.
- **CST I/O**: `maddi-cst-io` (de)serializes analysis results and CST fragments. That is how
  results are cached, preloaded and shipped.

## Layers and modules

Dependencies point strictly downward. Layer by layer, bottom up:

### Foundation (maddi)

| Module | Purpose | Start reading |
|---|---|---|
| `platform` | Gradle `java-platform` BOM; single source of external dependency versions. | `platform/build.gradle.kts` |
| `maddi-annotation` | The published annotations (`@Immutable`, `@Container`, `@Independent`, `@Modified`, …), split out of `maddi-support` at 0.9.1. Zero dependencies, Java 17, on Maven Central as `io.codelaser:maddi-annotation`. | `io.codelaser.maddi.annotation.Immutable` |
| `maddi-support` | The "eventually final" support classes (`SetOnce`, `Freezable`, `EventuallyFinal`, `Lazy`, `FirstThen`). Depends on and re-exports `maddi-annotation`. Java 17, on Maven Central. | `io.codelaser.maddi.support.SetOnce` |
| `maddi-util` | Small utilities (Trie, string/list helpers, get/set name derivation). | `io.codelaser.maddi.util.Trie` |
| `maddi-graph` | Graph layer over JGraphT: dependency graphs, SCCs, cycle breaking. | `io.codelaser.maddi.graph.G`, packages `graph/op`, `graph/analyzer` |

`maddi-annotation` and `maddi-support` are the only libraries a user's own code compiles against.
That is why they do not apply `java-library-conventions`: see [`PUBLISHING.md`](../PUBLISHING.md).

### CST: the Common Syntax Tree (maddi)

One language-agnostic tree ("Common", not "Concrete") shared by the Java and Kotlin front ends.
Everything downstream of parsing works on it exclusively.

| Module | Purpose | Start reading |
|---|---|---|
| `maddi-cst-api` | The interfaces: elements, expressions, statements, types, `TypeInfo`/`MethodInfo`/`FieldInfo`, and the central `Runtime`/`Factory` facade that builds trees. | `…cst.api.runtime.Runtime`, `…cst.api.element.Element`, `…cst.api.info.TypeInfo` |
| `maddi-cst-impl` | The implementation: node classes, `RuntimeImpl`/`FactoryImpl`, constant evaluation, Java printing. Largest module. | `…cst.impl.runtime.RuntimeImpl` |
| `maddi-cst-analysis` | The property/value model in which all analyzer verdicts are stored (`PropertyImpl`, `ValueImpl`, property maps, messages). | `…cst.impl.analysis.PropertyImpl` |
| `maddi-cst-io` | Codec: (de)serialization of CST and analysis results to/from JSON. | `…cst.io.CodecImpl` |
| `maddi-cst-print` | Language-neutral formatter: renders the `OutputElement` IR to formatted text. | `…cst.print.formatter2.Formatter2Impl` |
| `maddi-cst-print-translate` | What the Java-to-X printers share: `PrintMessage` and its severities, the per-thread `TranslationState` of a file's printing, language-neutral CST queries. | `…cst.print.translate.TranslationState` |
| `maddi-cst-print-kotlin` | Prints the CST as *Kotlin* source (same `OutputElement` IR; the target language is a printer choice, not a CST property). | `…cst.print.kotlin.KotlinTypePrinter`, `kotlin-printing.md` |
| `maddi-cst-print-csharp` | Prints the CST as *C#* source (Java → C# translation, #115, experimental), on the same `OutputElement` IR and printer seam. `tools/csharp-check` compiles the output. | `…cst.print.csharp.CSharpTypePrinter`, `csharp-printing.md` |
| `maddi-cst-print-rust` | Prints the CST as *Rust* source (Java → Rust translation, #167, experimental). The modification and independence verdicts become the ownership modes, so rustc's borrow checker judges them. Emits text directly (a Java type is several Rust items). | `…cst.print.rust.RustTranslator`, `rust-printing.md` |

### Inspection: the front ends (maddi)

Every front end follows the same two-tier pattern: a lower **CST-producing** module (grammar /
compiler binding → CST) and a thin **driver** module implementing the shared `JavaInspector`
contract from `maddi-inspection-api`.

| Front end | CST producer | Driver |
|---|---|---|
| Hand-written Java parser (CongoCC) | `maddi-java-parser` (+ `maddi-java-bytecode` for `.class` deps, via ASM) | `maddi-inspection-integration` |
| javac / OpenJDK (the **primary** front end) | `maddi-java-openjdk` (drives javac internals; needs `--add-exports`) | `maddi-inspection-openjdk` |
| Kotlin K2 Analysis API | `maddi-kotlin-k2` (PSI/FIR → CST), loaded inside the realm (below) | `maddi-inspection-kotlin` |
| Mixed Java + Kotlin | (composes the two above over one shared core) | `maddi-inspection-mixed` |

Shared machinery:

| Module | Purpose | Start reading |
|---|---|---|
| `maddi-inspection-api` | Contracts for the whole layer: `JavaInspector`, `Resolver`/`Context`, `CompiledTypesManager`, `InputConfiguration`. | `…inspection.api.integration.JavaInspector` |
| `maddi-inspection-parser` | Language-neutral resolution/binding: overload resolution, type contexts, generics, Lombok. *Not a source parser despite the name.* | `…inspection.impl.parser.ResolverImpl` |
| `maddi-inspection-resource` | Input configuration, source sets, parse results, the shared `InfoByFqn` type registry, the build-file capture. | `…inspection.resource.InfoByFqn` |
| `maddi-kotlin-api` | The Kotlin front end's contract *without* the Kotlin compiler: the types that cross the classloader boundary. Nothing here may depend on `kotlin-compiler`, the `*-for-ide` artifacts or `com.intellij` (`TestNoCompilerOnTheApiClasspath`). | `build.gradle.kts` header |
| `maddi-kotlin-realm` | The realm: an isolated classloader in which the K2 compiler and its 62 MB of bundled libraries are loaded, so none of it lands on a consumer's flat class path (G46). | [`kotlin-classloader-isolation.md`](kotlin-classloader-isolation.md) |

The K2 jars are resolved into a `k2Runtime` configuration that is deliberately not on any runtime
class path. The tests hand them to the realm, and the `maddi-kotlin` distribution carries them in
`lib-k2/`, next to `lib/`.

Essential reading before running or debugging anything on the javac front end:
[`maddi-inspection-openjdk/parsing-stability.md`](../../maddi-inspection-openjdk/parsing-stability.md)
(javac is not thread-safe; the flake it causes and the built-in fix) and
[`maddi-inspection-openjdk/calling-the-javainspector.md`](../../maddi-inspection-openjdk/calling-the-javainspector.md).

### Call graph and the analysis boundary (maddi)

| Module | Purpose | Start reading |
|---|---|---|
| `maddi-callgraph` | Call graph, primary-type use graph, analysis order. Base since the split: the call graph is a property of the parse, and `maddi-run-rewire` and the refactoring metrics need it without the modification analysis. **Consuming the call graph? Read the two conventions below first.** | `…callgraph.ComputeCallGraph`, `…callgraph.ComputeAnalysisOrder` |
| `maddi-analysis-api` | The `AnalysisEngine` service interface between the base and maddi-mod: requests, outcomes, problems, hints, nullability facts. `AnalysisEngines` finds the implementation. | `…analysis.api.AnalysisEngine` |

#### Two call-graph conventions that will silently mislead a reader

Both are deliberate and load-bearing, and both make a real relationship invisible to the obvious
way of looking for it. Nothing is missing from the graph and nothing errors: the relation is simply not
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

### Analysis: the heart of the project (maddi-mod, except the archive)

| Module | Repo | Purpose | Start reading |
|---|---|---|---|
| `maddi-modification-common` | maddi-mod | Shared base: shallow/default analyzers that turn annotations into properties for library types; get/set handling. Its test fixtures hold `CloneBenchCorpus`. | `…modification.common.defaults.ShallowAnalyzer` |
| `maddi-modification-prepwork` | maddi-mod | Phase 0 per primary type, on top of the call graph: part-of-construction, final-field status, hidden content, escapes, per-method variable data. | `…modification.prepwork.PrepAnalyzer` |
| `maddi-modification-link` | maddi-mod | The **link engine**: computes `(from, nature, to)` links between variables, method-call linking, virtual fields (`§`), functional-interface lifting, shared-variable collapse. | `linking-manual.md` §5, then `…link.impl.LinkMethodCall`; `TestLinkMethodCall` is the spec-by-example |
| `maddi-modification-analyzer` | maddi-mod | The **orchestrator**: multi-phase fixpoint iteration with worklist, certification and cycle breaking; also guard (contract-check) mode. | `…analyzer.impl.IteratingAnalyzerImpl`, then `SingleIterationAnalyzerImpl`; `README.md` for the phases, `definitions.md` for the concept chain |
| `maddi-aapi-archive` | maddi | The curated annotated-API content: hand-written annotated stubs for JDK/library packages + their compiled JSON under `src/main/resources`. Pure data. | `…aapi.archive.jdk.JavaUtil` |
| `maddi-aapi-parser` | maddi-mod | Compiles the archive's stubs into machine-readable JSON analysis results. | `…aapi.parser.AnalysisHintsCompiler` |
| `maddi-run-analysis` | maddi-mod | The `AnalysisEngine` implementation (the modification tier behind the service). Also home of the large-corpus tests (`@Tag("slow")`, `./gradlew slowTest`): the Java→Kotlin and Java→C# ratchets and the corpus smoke tests, against the `test-oss` corpus that `corpus/` builds. | `AGENTS.md` §Commands before quoting a slow run |
| `maddi-run-kotlin-analysis` | maddi-mod | Tests only: the analysis on Kotlin and mixed inputs, through the realm as the shipped CLI does, including the Kotlin census corpora (detekt, Exposed, Retrofit, Coil). | its `src/test` |
| `maddi-inspection-kotlin-analysis` | maddi-mod | Tests only: Kotlin front-end tests that need the prep analysis. | its `src/test` |

### Runners, plugins, IDE integration

| Module | Repo | Purpose |
|---|---|---|
| `maddi-run-config` | maddi | Shared run configuration, error reporting, exit codes; used by every runner and both build plugins. |
| `maddi-project-config` | maddi | A project's configuration without an IDE: an `inputConfiguration.json` from a plugin's file, directories named by hand, or the javac invocations captured from a Gradle, Maven or any other build's log (`ParseJavacList`, maddi-run-config); whether its build files changed since (`BuildFileCheck`); making it again. Storage is an interface (`ProjectRecords`). Used by maddi-mcp and the daemon's compile-log route, and open to any tool that stores projects its own way; published with maddi. New in 0.9.7. |
| `maddi-run-openjdk` | maddi | **The main driver**: the javac front end, plus the modification analysis when `maddi-run-analysis` is on the class path. |
| `maddi-run-kotlin` | maddi | The driver for mixed Java+Kotlin projects; runs K2 through the realm. |
| `maddi-run-main` | maddi | Driver on the hand-written-parser front end. |
| `maddi-run-rewire` | maddi | Small driver for incremental/partial re-analysis (rewiring), independent of the inspector implementation. |
| `maddi-cli` | maddi-dist | **The main CLI** (`bin/maddi`, distZip): `maddi-run-openjdk` + `maddi-run-analysis`, no code of its own. |
| `maddi-cli-kotlin` | maddi-dist | CLI for mixed Java+Kotlin projects (`bin/maddi-kotlin`, distZip). This is how Kotlin support ships: the K2 jars are not on Maven Central, so they ride along in `lib-k2/`. The IDE plugins download this zip for their Kotlin front end. |
| `maddi-gradleplugin` | maddi-dist | Gradle plugin (`io.codelaser.maddi.analyzer`); shades the whole Java analyzer stack so it is self-contained. |
| `maddi-mvnplugin` | maddi-dist | Maven plugin, same shading approach; hand-maintained `plugin.xml`. |
| `maddi-ide-daemon` | maddi-dist | Out-of-process analysis daemon (NDJSON over a loopback socket) that the IDE plugins and the MCP server launch and query. The protocol version is checked at the handshake. |
| `maddi-ide-client` | maddi-dist | IDE-agnostic daemon client + DTOs; plain JDK + Jackson, Java 21, published to Maven local for the Eclipse build. |
| `maddi-mcp` | maddi-dist | MCP server (Model Context Protocol, over stdio) for coding agents: configure a project, analyse it, query verdicts, diff runs. Drives the daemon through `maddi-ide-client`; its distribution carries the daemon in `daemon/`. New in 0.9.6. |
| `maddi-intellij` | maddi-dist | IntelliJ plugin (talks to the daemon; no direct analyzer dependency). |
| `maddi-intellij-j2k` | maddi-dist | Benchmark tool, never published: headless IntelliJ J2K, to compare maddi's Java → Kotlin translation with IntelliJ's (CodeLaser/maddi-mod#36). |
| `maddi-eclipse` | maddi-dist | Eclipse plugin: a separate Maven/Tycho reactor, not a Gradle module. |
| `maddi-vscode` | maddi-dist | VS Code extension (TypeScript, npm); reimplements the wire contract, bundles the daemon. Build from source only. |
| `maddi-manual` | maddi | The user-facing manual (AsciiDoc → HTML/PDF): getting started, plugins, CLI, configuration, exit codes. |
| `road-to-immutability` | maddi | The concepts book (AsciiDoc) + its maintained digest `llm-summary.md`. |

Not in a Gradle build: `maddi-eclipse` (Tycho) and `maddi-vscode` (npm) in maddi-dist; in maddi,
`corpus/` (the corpus catalogue and its Taskfile, see [`corpus/README.md`](../../corpus/README.md))
and `tools/` (`tiers` for the split's tier check, `csharp-check` for the C# output, `rename`
and `relicense` for the one-off migrations). The Bazel files beside the Gradle ones are a proof
of concept for the base modules (`CONTRIBUTING.md`); Gradle is the build.

## What is published, and for whom

Publishing has been active since 0.9.1; [`PUBLISHING.md`](../PUBLISHING.md) records the strategy and
the state of each leg. The analyzer modules are **not** published as a fine-grained library. Consumers
get:

1. **Annotations**: `io.codelaser:maddi-annotation` and `io.codelaser:maddi-support` on Maven
   Central, the only artifacts user code compiles against;
2. **Build plugins**: the Gradle plugin on the Plugin Portal and the Maven plugin on Maven Central,
   each self-contained by shading the Java analyzer stack;
3. **CLI distributions and the MCP server**: the self-contained zips `maddi`, `maddi-kotlin` and
   `maddi-mcp` on the GitHub release (`release-cli.sh` in maddi-dist), also as Homebrew formulas in
   `codelaser/tap`;
4. **IDE plugins**: IntelliJ on JetBrains Marketplace and Eclipse as an update site, since 0.9.6.
   Both bundle the daemon.

## Reading paths by intent

- **"I want to understand what maddi computes"**:
  [`road-to-immutability/llm-summary.md`](../../road-to-immutability/llm-summary.md), then the book
  chapters it points to; `maddi-modification-analyzer/definitions.md` (maddi-mod) for the compact
  concept chain.
- **"I want to run maddi on a project"**: the manual
  (`maddi-manual/src/docs/asciidoc/`), then `maddi-run-openjdk/running-examples.md`.
- **"I want to work on the link engine"** (maddi-mod):
  `maddi-modification-link/linking-manual.md` starting at §5 (LinkMethodCall) + §6 (worked
  examples); `TestLinkMethodCall` as executable spec; the module `README.md` for the
  link-nature combination table; `src/main/java/…/link/vf/virtual-fields.md` for `§` virtual fields.
- **"I want to work on the iterating analyzer / convergence"** (maddi-mod):
  `maddi-modification-analyzer/README.md` (phases), llm-summary §Convergence, then
  `IteratingAnalyzerImpl`.
- **"I want to work on a front end"**: `maddi-inspection-api`'s `JavaInspector`, then the
  producer/driver pair for your language (table above); for javac, read
  `parsing-stability.md` *before* anything else; for Kotlin,
  [`kotlin-classloader-isolation.md`](kotlin-classloader-isolation.md).
- **"I want to touch the printers/formatter or the translations"**:
  [`formatter-analysis.md`](formatter-analysis.md), `maddi-cst-print`,
  `maddi-cst-print-kotlin/kotlin-printing.md`, `maddi-cst-print-csharp/csharp-printing.md` and
  `maddi-cst-print-rust/rust-printing.md`.
- **"I want to run or extend the corpus tests"**: [`corpus/README.md`](../../corpus/README.md), then
  `AGENTS.md` §Commands (a green slow run can be cached, skipped, vacuous or heap-starved).
- **"I want to work on the plugins, IDE integration or MCP server"** (maddi-dist): `maddi-run-config`,
  then the plugin module; for IDEs and the MCP server, `maddi-ide-daemon`'s `DaemonProtocol` and
  maddi-dist's `Taskfile.yml` (the IDE build tasks); state in `maddi-eclipse/README.md`, open items in
  maddi-dist's `docs/roadmap/ide-todo.md`.

## Documentation map

- Maintained references: this file; `road-to-immutability/llm-summary.md` (concepts);
  the linking manual and analyzer `definitions.md`/`README.md` (engine, maddi-mod);
  `parsing-stability.md` (javac); the manual (users); [`PUBLISHING.md`](../PUBLISHING.md) (release
  strategy); [`CONTRIBUTING.md`](../../CONTRIBUTING.md) (workflow).
- Working notes and plans: [`docs/README.md`](../README.md) is the index; module-local notes
  live in each module (`notes/`, `sv-*.md` journals in `maddi-modification-link`). Open work items
  are GitHub issues.
- For LLM assistants: [`AGENTS.md`](../../AGENTS.md) (tool-agnostic) and [`CLAUDE.md`](../../CLAUDE.md);
  maddi-mod and maddi-dist each have their own `CLAUDE.md`.
