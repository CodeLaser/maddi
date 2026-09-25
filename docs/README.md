# docs/ — index

This directory holds **cross-module working documents**: design notes, hardening roadmaps and
investigation reports that do not belong to a single module. The *maintained reference
documentation* lives elsewhere — see [the documentation map in the root README](../README.md)
and the pointers at the bottom of this page.

The documents are grouped by what they are for:

| Directory | Holds | When it leaves |
|---|---|---|
| [`design/`](#design) | How a mechanism works and why it was built that way. Cited from production code and tests as the rationale. | When the mechanism is removed. |
| [`roadmap/`](#roadmap) | Proposals, plans and application surveys for work not (fully) done. | When the work lands (the record moves to `design/`) or is abandoned. |
| [`defects/`](#defects) | Analyses of defects that are reproduced but not fixed. | When fixed — a regression test then carries the record. |
| [`status/`](#status) | State-of-the-union documents, updated as work progresses. | When superseded. |
| [`project/`](#project) | Repository, release and process notes. | Rarely. |

Each entry below is labeled:

- **plan** — an execution plan or roadmap; describes intended work, possibly partially done.
- **note** — a dated design/investigation note; accurate for the code as it was on that date,
  kept for the reasoning, not guaranteed to track later changes.
- **status** — a living state-of-the-union / TODO document, updated as work progresses.
- **tracked** — the document explains; the open items inside it are GitHub issues. New work items
  go to issues, not to new checkbox files.

Dated handoff notes for defects that have since been fixed are **not kept** here: the fix, its
regression test and the commit message carry the record. Thirteen such documents were removed on
2026-09-25 (listed [at the bottom](#removed)); `git log --all --diff-filter=D -- 'docs/*.md'`
finds any of them.

**`closed-core`** throughout these documents is the closed-source ~3M-line commercial codebase used
as a private proving corpus, and `com.example.*` stands for its packages. It is not distributable,
so a reproduction that needs it is a driver, not a test. The customer must not be named here — see
[CONTRIBUTING.md §Names that must not appear](../CONTRIBUTING.md#names-that-must-not-appear), which
is enforced by a commit hook.

## design

Mechanisms that exist, and the reasoning behind them. Read the one that matches the code you are
about to touch.

### Rewiring and incremental analysis

| Document | Status | What it covers |
|---|---|---|
| [rewiring.md](design/rewiring.md) | note | How CST rewiring works in general, and its pitfalls. |
| [analysis-rewiring.md](design/analysis-rewiring.md) | note | The `analysisFingerprint` mechanism and how analysis results are rewired onto a re-parsed CST. |
| [partial-reparse-rewire.md](design/partial-reparse-rewire.md) | note | Emitting `.class` files from the openjdk inspection pass; partial re-parse/re-wire for IDE use. |

### Eventual immutability

| Document | Status | What it covers |
|---|---|---|
| [eventual-immutability.md](design/eventual-immutability.md) | spec | Bringing road-to-immutability §060 back into the engine: contract the support classes, propagate; no preconditions. Implemented; with the parameter spec below, the specification of the `EVENTUALCLUSTER` behaviour. |
| [eventual-info-hierarchy.md](design/eventual-info-hierarchy.md) | note | Why the `Info`/`*InfoImpl` family is `@Mutable`, and the method-level `@NotModified(after=)` primitive built to unblock it (steps 1–5); the cross-reference cluster that remains. The running record of the certification arc. |
| [eventual-design-improvements.md](design/eventual-design-improvements.md) | proposal | cst-api/cst-impl design follow-ups from the certification arc: the dogfood ratchet, conformance tests, `Memo` support types, and the asserted-contract compromise. §§1–3 implemented (see the enforcement round in eventual-info-hierarchy.md, and the corrections at the top); §4 and §6 still open. |
| [spec-eventually-unmodified-parameter.md](design/spec-eventually-unmodified-parameter.md) | spec | `EVENTUALLY_UNMODIFIED_PARAMETER` (`@NotModified(after=…)` on parameters): the mechanism, what landed, the measured outcome. |
| [handoff-eventual-interface-nonmodification.md](design/handoff-eventual-interface-nonmodification.md) | note | Surfacing the `*Info` interfaces' eventual verdict (greatest-fixpoint Part B). §5, the `commitLabels` reframe, is what `TypeEventualAnalyzerImpl` implements. |
| [handoff-verification-residue.md](design/handoff-verification-residue.md) | note | The verification-pass residue: recursion pessimism at the `isNonModifying` undecided-callee default, the minimal repro (`TestRecursionThroughAbstract`), and the fix designs. §7.5 design A is what `ShadowModificationPass` implements as primitive seeding. |
| [handoff-builder-leans.md](design/handoff-builder-leans.md) | note | The Builder leans — resolved: the real mechanism (§4c) and the two fixes; §4A/§4b are the rationale `TypeEventualAnalyzerImpl` cites for the precondition shape and the handed-on fallback. |
| [dynamic-immutability-feasibility.md](design/dynamic-immutability-feasibility.md) | note | A field's dynamic immutability: materialized (part 1) and consumed (part 3) from a hand-written contract, with a local guard check; inference (part 2) deliberately not built. |
| [builder-interface-split-impact.md](design/builder-interface-split-impact.md) | note | Costing the two ways to stop the mutable Builder capping the read-only Inspection interfaces; measured, no refactor landed. |
| [book-vs-support-divergence.md](design/book-vs-support-divergence.md) | note | *The Road to Immutability* chapter 12 against the `maddi-support` code it quotes: two independently maintained lineages, and which side each finding changes. |
| [independent-type-optimism.md](design/independent-type-optimism.md) | note | **Fixed 2026-09-24.** `INDEPENDENT_TYPE` was frozen at an optimistic value while a type's members were undecided. Kept as the rationale for "no optimistic default for an undecided input", which `DynamicImmutabilityInference` cites; the fix unmasked the SAM false positive in `defects/`. |

### Other mechanisms

| Document | Status | What it covers |
|---|---|---|
| [guard-mode-analysis.md](design/guard-mode-analysis.md) | note | Guard mode (contract verification): analysis and design proposal; phases 0–2 implemented in `GuardAnalyzerImpl`. |
| [isolate-class.md](design/isolate-class.md) | note | `IsolateClass`: lifting a whole type into a standalone JDK-only source tree — one compilation unit per stub, in the package the original came from. Records **why a project rather than one file**: the single-compilation-unit constraint is what produced all of `IsolateMethod`'s placement machinery, and a project has none of it. Twelve corpus rounds took closed-core's hundred largest types to **100/100 isolated and parsing back**; the eleven defects found are listed, nine of them in shared code, along with the two that were silent no-ops whose unit test passed anyway. §5's "one remaining cause, and the only one I would call hard" was **resolved 2026-07-30** and the write-up says why that reading was wrong: the recursive generic was what the symptom was made of, not what the defect was. §6 is the round that took the **compile** gate from 34 failing trees to 3 — twelve causes, a driver each, four of them one of the three method-stub paths lacking something another had — and it is the place to read before touching what a stub declares: one fix that reproduced its failure and went green took the corpus from 97 trees compiling to **9**, unit suite still passing. §7 names the three that are left, one of them a source/binary incompatibility in a third-party jar that cannot be fixed here. §10 extends the isolator from one type to a **set** of them, kept verbatim together so that a reference from one to another reaches the real type rather than a stub: the design point is that `originalType` was two different questions in one field (*is this kept somewhere* vs *which type is `this` right now*), and the section records which of the three construction barriers are load-bearing, that the characteristic failure — a body-less override of a declaration kept one inheritance level up — is **not** a compile error, and why per-unit bookkeeping may never be keyed by `CompilationUnit` (its `equals` is `(uri, sourceSet)`, so two isolates out of one file are the same key). |
| [kotlin-classloader-isolation.md](design/kotlin-classloader-isolation.md) | **reference** | Why the K2 front end moves behind a plexus-classworlds realm: the fat compiler jar shadows ANTLR/guava/JNA on every consumer's runtime classpath (G46), why shading was measured and rejected, and the five-type boundary. |
| [formatter-analysis.md](design/formatter-analysis.md) | note | Analysis of the `maddi-cst-print` formatter (`Formatter2Impl`), with the bug classes F1–F8 that motivate the Doc IR plan. |

## roadmap

What could or should be built next. **The application roadmap for the modification engine is
[modification-link-applications.md](roadmap/modification-link-applications.md).**

| Document | Status | What it covers |
|---|---|---|
| [modification-link-applications.md](roadmap/modification-link-applications.md) | plan | Six candidate applications for the link substrate beyond the three prioritized ones (modification analysis, same-type linking for extract-interface, object tracking). The organizing observation: the link natures are a **relational object-graph algebra**, and the element-level (`∈ ∋ ⊆ ⊇ ~`) and decoration families appear to be computed and not consumed by anything downstream. The criterion that ranks the six is whether a candidate needs a link to be **present** or **absent** — presence is a derived fact, absence is spoiled both by saturation and, more seriously, by degraded summaries, where "they do not interact" can mean "the analysis gave up", which is unsound in the unsafe direction. Five are presence-based and buildable now; disjointness-for-parallelisation is the one to hold back. Nothing built, nothing costed, and no change proposed to what the engine concludes. |
| [semantic-preconditions-for-relocation.md](roadmap/semantic-preconditions-for-relocation.md) | plan | Design for §3.1 of the applications roadmap: the four ways relocating state changes behaviour (`this` identity, a split monitor, a duplicated reassigned field, re-timed static initialisation), which maddi data decides each, and why every check must return *found / clean / not checked* — presence-based checks are unsafe over degraded summaries too. Corrects §3.1: aliasing survives a split. |
| [handoff-saturated-closure-collapse.md](roadmap/handoff-saturated-closure-collapse.md) | plan | Why the link engine's per-method work ceiling fires: not length, not a cliff, not a tunable budget — a **saturated closure** (median 55% of all variable pairs linked, some 100%), which is semantically correct. Proposes collapsing such a group to one fact instead of N². Self-contained; reproduction needs no corpus. Includes two negative results and the witness-selection question to settle first. |
| [formatter-doc-ir-plan.md](roadmap/formatter-doc-ir-plan.md) | plan | Rewrite plan: a Doc IR for `maddi-cst-print`. `Formatter2Impl` is still the renderer. |
| [prep-analyzer-hardening.md](roadmap/prep-analyzer-hardening.md) | tracked | Robustness hardening roadmap for `maddi-modification-prepwork`. H items are issues (#13). |
| [modification-link-analyzer-hardening.md](roadmap/modification-link-analyzer-hardening.md) | tracked | Real-world robustness hardening roadmap for the link analyzer. H items are issues (#14). |
| [ide-todo.md](roadmap/ide-todo.md) | tracked | IDE front ends (IntelliJ/Eclipse/VS Code): what is *not* done yet. The input-configuration gaps found by the IDE daemon session of 2026-08-25 are in §4b and §5.D. |

## defects

Reproduced, characterized, not fixed. When one is fixed, its regression test takes over the record
and the document goes.

| Document | Status | What it covers |
|---|---|---|
| [sam-linking-reconciliation.md](defects/sam-linking-reconciliation.md) | note | A custom SAM's parameter (`ThrowingFunction.apply:0:o`) lands in a caller's modified set although nothing modifies it. Live since the independence fix and pinned by two tests; contract vs inference, not virtual fields. The mechanism is disputed between the note and the tests' comments (see its header). |
| [handoff-importcomputer-star-collapse.md](defects/handoff-importcomputer-star-collapse.md) | note | `ImportComputerImpl` star-collapsing changes JLS 6.5.5 name resolution. Reproduced at maddi level 2026-09-25: the printed unit fails javac when the homonym is in the unit's own source set, or collides with `java.lang`. Downstream consumers pass never-collapse. |
| [discrepancies-openjdk-maddi-parsers.md](defects/discrepancies-openjdk-maddi-parsers.md) | note | Observed differences between the openjdk (javac) and hand-written (CongoCC) Java front ends. One open item (`source().index()` of methods and formal parameters). |

## status

| Document | Status | What it covers |
|---|---|---|
| [kotlin-gap-analysis-2026-09-21.md](status/kotlin-gap-analysis-2026-09-21.md) | status | What is still between maddi and a "Java+Kotlin" claim, ranked by how silently each gap fails. The front end is not the gap: Kotlin is a second pipeline (own runner, own CLI, own distribution, no persistence, no IDE, no plugin reach) while the Java pipeline used to accept Kotlin input and discard it without a word. Records what is solid (detekt 1,271 types, javalin mixed at 0 errors, prep tested against the identical Java oracles), the verified model holes (annotations not converted at all, `suspend` absent, `::`/`::class`/parens/local funs as placeholders), the stack past the parse (no Kotlin encode path, link engine untested on Kotlin, a 10-type stdlib archive), and the evidence gaps (three corpus tests that pass green measuring nothing, no Kotlin ratchet). §7 records the two gaps closed on 2026-09-21; §8 is the ordered path to the claim. |
| [kotlin-corpora.md](status/kotlin-corpora.md) | status | The first two Kotlin OSS corpora (coil, detekt) and what the pipeline does on them: **1,271 types over 31 source sets** parsed, prep to a 9,223-element order, and the **first modification run on Kotlin** — ~7 iterations to certification. Records why coil needs a hand-assembled configuration (multiplatform defeats both the Gradle plugin and `--compile-log`) while detekt is the first corpus to exercise the compile-log route for real. Two things worth reading before touching this area: **"no immutable types" means the annotated APIs were not loaded** — detekt reported `@FinalFields=1320, @Mutable=428` and not one immutable type while converging, certifying and otherwise looking healthy; with the JDK archive it is `@Immutable=676, @Immutable(hc=true)=137` — and **a Kotlin-only project had no shared core at all**, the compiled-types manager resolving null for every library type including `java.util.List`. §5 is the open tail: a `variableData` prep overwrite, `try` as an expression (now confirmed in the wild), Kotlin primitive arrays blocked behind the library loader's first-visit-wins rule, read-only collections collapsing to the mutable JVM type, and the `JavaStubGenerator` gaps that will matter again on a genuinely mixed corpus. |
| [handoff-build-plugin-corpus-hardening.md](status/handoff-build-plugin-corpus-hardening.md) | status | **Both plugins now have a corpus instrument and run the configuration checks; nine defects found, and the two REPAIRS are refuted for a plugin (§6a).** maddi has three producers of one `InputConfiguration` (`--compile-log`, `maddi-gradleplugin`, `maddi-mvnplugin`) and until 2026-08-19 only the log route met a corpus. Five defects on the Gradle side (a source set's `uri` was its first SOURCE directory; every sibling's class-path part was named `main`, so 6 of pulsar's 7 vanished silently; no `sourceRelease`; divergent `jmods` defaults; a sibling given the consumer's class path), four on the Maven side (a scope filter computed and never applied — timefold `core/main` 60 dependencies against javac's 12, at zero errors; a reactor sibling resolving to `target/classes` naming every sibling `classes`; `sourceRelease` reading only the properties, which are the minority spelling — **jenkins core had been parsing with 100 errors at exit 0 and nothing reported it**; and `--add-modules` recorded by neither plugin, so trino's incubator module was invisible). §2 is the transferable method; §4 covers both plugins' corpus runs, the shared checks (and why they would not have caught any of the nine), the A/B pairs, and the next job; §5 records five weak instruments; §6a refutes wiring `AnnotationProcessorOutput`/`TypeUseAnnotationClosure` into a plugin — they read a compiled destination that does not exist yet at configuration time, and on Maven they *looked* like they worked by reading the previous build — §6b refutes a Gradle variant, §7 lists what is left open, and §8 maps every corpus entry to what it measures. |

The state of the Eclipse plugin is in [`maddi-eclipse/README.md`](../maddi-eclipse/README.md), not here.

## project

| Document | Status | What it covers |
|---|---|---|
| [release-notes-0.9.1.md](project/release-notes-0.9.1.md) | release | 0.9.1: every package renamed `org.e2immu.*` → `io.codelaser.maddi.*`; first release with the build plugins and CLI. |
| [history-rewrite-2026-08-04.md](project/history-rewrite-2026-08-04.md) | note | Every commit SHA in the repository changed on 2026-08-04: `git filter-repo` removed the customer names from all 2,187 commits and 25 branches, blob contents and messages alike, verified by pickaxe, by decompressing all 13,512 blobs, and over every commit message. Records what it cost — **126 cited SHAs**, all 177 citation sites rewritten in the same change — what to do with a stale clone, and why the commit hook exists so this is never needed twice. The old→new map is held by the maintainer, not committed: it is an index of pre-rewrite hashes, which a forge may still serve. |
| [opening-up-to-contributors.md](project/opening-up-to-contributors.md) | plan | Preparing a project with 0 stars and 0 forks to receive its first outsider: pull requests yes but asymmetrically (contributors PR, maintainer pushes), and why the `.md` working documents must **not** be converted wholesale into issues — 38 of them are cited from production code, and only three contain checkbox TODOs at all. Four phases from "safe to land on" to writing the rule down, plus the three decisions that are the maintainer's: PR target, DCO vs CLA (settle before the first PR, not after), and issue granularity. §5 records the customer-name scrub and the commit hook that now enforces it. |
| [landing-surface-checklist.md](project/landing-surface-checklist.md) | plan (superseded) | Everything an outsider hits before reading any code: e2immu.org redirect, repo metadata, CI, publishing the book, the 0.9.0 release, the LGPL/permissive licence split. All shipped with 0.9.1; kept for the e2immu.org deploy procedure and the CI findings. |

## Removed

Removed 2026-09-25, as development leftovers. Each was a dated handoff or report whose work is
done; the citing code comments now name the test that pins the behaviour.

| Document | Why it went | The record now |
|---|---|---|
| `handoff-anonymous-types-rewire.md` | fixed 2026-07-23 | `TestRewireAnonymousType` |
| `handoff-detailedsources-written-qualifier.md` | fixed 2026-07-30 | `ClassSymbolScanner.iterateUpToPackageLevel`, `ImportComputerImpl` |
| `HANDOFF-maddi-detailedsources-typeparameter-identity.md` | resolved 2026-09-14 with a different diagnosis | `TestClassTypeParameterIdentity` |
| `handoff-isolateclass-enum-and-generic-stubs.md` | resolved 2026-07-30 | `TestIsolateClass4Compiles`; `isolate-class.md` §5–§6 |
| `handoff-linkcomputer-recursion-vd-null.md` | fixed 2026-08-02 | `TestLinkUnpreppedCallee` |
| `handoff-source-and-jar-duplicate-interfaces.md` | fixed 2026-08-21 | `TestPreloadBeforeSourceSymbols` |
| `handoff-uninspected-methods-null-access.md` | fixed 2026-08-23 | `TestDroppedUnitMethodAccess` |
| `method-type-parameter-source-loss.md` | resolved 2026-08-01 and 2026-09-14 | `TestMethodTypeParameterSource`, `TestClassTypeParameterSource`, `TestClassTypeParameterIdentity`, `TestTypeParameterIdentityCorpus` |
| `regression-jdk-preload-jmodless-alternative-jre.md` | fixed (`JavaInspectorImpl.createTask`, `isRunningJdk`) | `WarmAnalysisServiceTest`, `TestAlternativeJRE` |
| `handoff-ide-daemon-2026-08-25.md` | session note; its open items are in `roadmap/ide-todo.md` §4b and §5.D | commit messages `1d583c80d`..`be3340473` |
| `eclipse-plugin-state.md` | a 2026-07-18 survey that called the plugin dormant; contradicted by the plugin itself (issue #6) | `maddi-eclipse/README.md` |
| `TEST_MIGRATION.md` | the migration to `maddi-java-openjdk` is done | the tests |
| `doc-audit-2026-07-30.md` | a dated audit, superseded by this reorganisation | this index |

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
