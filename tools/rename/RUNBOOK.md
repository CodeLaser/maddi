# Cutover runbook — `org.e2immu` → `io.codelaser.maddi`

Seven repos move together: **maddi** plus `jfocus-{metrics,refactor-server,refactor-service,standardize,stdbase,transform}`
and `refactor-resource`. They share a source-level composite build (`includeBuild("../maddi")`),
so there is no version pin to hide behind — either they all move or none do.

The Gradle coordinates (`io.codelaser:codelaser-*`) do **not** change. Only Java
packages, JPMS module names, directories, and the plugin id.

## The one idea that saves the day

**A mechanical rename is a function, not a diff. Never ask git to merge it — replay it.**

Everything below follows from that.

## Measured, rehearsal 1 (2026-08-05, maddi + 8 siblings cloned to sandbox)

| step | result |
|---|---|
| `move` | 1,750 files; commit shows **0 insertions, 0 deletions, 1,750 renames** |
| `substitute` | 1,823 files |
| `verify` | 7 unmapped, all prose (see below) |
| maddi `compileJava compileTestJava` | green, 26 s |
| maddi `test` | **green, 436 tests, 4 m 37 s** |
| downstream subprojects vs renamed maddi | green |

The mechanical part is **minutes**. The freeze window is dominated by test suites
and by the manual follow-ups, not by the rename.

### What rehearsal 1 caught — do not re-learn these

1. **The tool rewrote itself.** `rename.py`/`RUNBOOK.md` are `.py`/`.md`, i.e. in
   `TEXT_EXT`, and quote the tokens being renamed. Section 2 turned `SURVIVOR_OK`'s
   `analyser` into `analyzer`, after which `verify` flagged every correctly-renamed
   `ANALYZER` constant — 358 phantom findings. Fixed by `SELF_EXCLUDE_GLOBS`.
2. **93 files were silently skipped**: `maddi-eclipse` uses Eclipse PDE's flat
   `src/org/e2immu/…` layout, which an earlier `src/*/{java,resources}/` gate missed.
3. **The rename turned a passing test into a failing one.** `TestStringUtil`
   asserts on `$` as a package separator, so section 1 renamed its *expected*
   value while leaving its *input* untouched. A green compile would not have
   caught it. Now handled by scoped section-8 rules.
4. **Source-position assertions shift.** `TestModuleInfo` hardcodes
   `5-14:5-45`; eliding `util.internal` shortened the line by 6 characters, so
   it became `5-14:5-39`. Two lines, manual, expected — the test encodes text
   geometry, not names.
5. **Recorded logs were half-rewritten.** Four captured gradle/maven debug logs
   in `jfocus-refactor-server` (1,362 lines) had package tokens renamed but bare
   `org.e2immu:` groupIds left, leaving each internally inconsistent. Now frozen.
6. **Root `./gradlew compileJava` fails in `jfocus-refactor-server` — at baseline
   too.** `Task 'compileTestJava' not found in project ':maddi'`. Pre-existing;
   do not chase it on the day. Compile named subprojects instead.

## Before the day

1. **Review `name-map.tsv` with whoever else works on the tree.** It is 17 rules
   plus five hand-managed exceptions; it is the entire change, and it is far
   easier to review than 14,000 changed lines.
2. **Rehearse in a throwaway clone** until all seven repos build green
   (`ws/python/sandbox`, never the shared corpus checkout). Record the wall clock —
   the freeze window should be a number you measured, not one you guessed.
3. **Agree the freeze.** Everyone pushes and merges everything. The tree must be
   quiescent: `rename.py` refuses a dirty working tree for exactly this reason.

## The cutover

Per repo, in this order. The two commits are separate **and must stay separate**.

```bash
cd tools/rename
./rename.py move       --repos ../../..            # git mv only, no content change
git -C <repo> commit -m "rename: move sources to io/codelaser/maddi (paths only)"

./rename.py substitute --repos ../../..            # content only, no path change
git -C <repo> commit -m "rename: org.e2immu -> io.codelaser.maddi (content only)"

./rename.py verify     --repos ../../..            # must print "clean"
```

Identical content makes a move a 100 %-similarity rename, which git follows and
merges cleanly. Fold the move and the substitution into one commit and rename
detection collapses across 1,750 files — which is precisely what turns a
colleague's branch from a replay into a week of conflicts.

`verify` must end with `clean:` and the two known-survivor counts:

```
known survivors: 1124 in frozen .gml, 303 org.e2immu.analyser in text blocks
clean: no unmapped org.e2immu / analyser tokens remain
```

Any other number is a gap in the map, not something to fix by hand — fix the map
and re-run, so the change stays a function. The counts themselves move whenever the
frozen or acknowledged set changes; what must not move is the word `clean`.

⛔⛔ **`clean` IS NOT EVIDENCE THAT THE PROSE SURVIVED — IT IS EVIDENCE THAT NOTHING IS
UNACCOUNTED FOR, AND A REWRITTEN SENTENCE IS ACCOUNTED FOR.** `verify` looks for *surviving*
`org.e2immu` tokens. A document whose old-name references were all substituted has none left,
so it passes with nothing to report — which is exactly what a falsified document looks like.
The cutover ran clean and had rewritten four documents; it took a reader, five days later, to
notice that the 0.9.1 migration table said `io.codelaser.maddi.annotation` →
`io.codelaser.maddi.annotation`. Read the substitution diff over `*.md`, every time
(`git show <substitute-commit> -- '*.md' | grep -E '^[-+][^-+]'`); it is a few dozen lines and
it is the only thing that looks at the direction of a change rather than at its residue.

## Replaying an in-flight branch

If a branch `B` off base `X` could not be merged before the freeze, do **not**
merge it across the rename. Rewrite the patch instead — this works precisely
because the rename is a pure function of the text:

```bash
git diff X..B > /tmp/b.patch
./rename.py substitute --map name-map.tsv --patch /tmp/b.patch   # paths AND content
git checkout -b B2 <the substitution commit>
git apply /tmp/b.patch
```

`--patch` is not implemented yet — it is a ~20-line addition to
`phase_substitute` (rewrite `a/`+`b/` path headers with `apply_path`, hunk bodies
with `apply`). Add it only if a branch actually needs it; quiescing is cheaper.

## Rehearsal 2 (same day): manual follow-ups applied, all nine repos verify clean

| item | result |
|---|---|
| `gradle.properties` 0.9.0 → **0.9.1** | done |
| deleted `org.e2immu.analyser.properties` descriptor | Gradle generates `io.codelaser.maddi.properties`; jar verified |
| `dogfood` plugin pin 0.8.2 → 0.9.1 + new id | done (the version half is now moot: dogfood pins none) |
| `testmvnplugin-export/pom.xml` coords + `maddi.version` | done |
| 4 prose statements the rename made false | rewritten |
| 5 dead `\|\| startsWith("org.e2immu")` stack filters | removed, compiles |
| maddi `test` | green |
| **`verify` across all nine repos** | **clean** |

Residue is now fully accounted: 1,124 frozen, ~300 kept text blocks, 5 acknowledged prose.

## Rehearsal 3: the maddi-support split — ~90% done, 8 tests outstanding

Decided to go ahead (the published-artifact objection was overruled: no known
consumer). Committed in the sandbox at `3a55a86b7`, 42 files.

**The module wiring was the easy part and it is clean.** `maddi-annotation`
carries the 27 annotation files with no `requires` at all; `maddi-support` keeps
`support` + `annotatedapi` and declares `requires transitive
io.codelaser.maddi.annotation`. Because of that one word, **no consumer
`module-info` needed changing** — all 21 existing `requires
io.codelaser.maddi.support` still resolve the annotations.

**The real cost is one idiom, spread across ~12 sites.** The codebase locates the
annotations artifact by naming a single class:

```java
SourceSet maddiSupport = SourceSetImpl.sourceSetOf(Immutable.class);
```

`sourceSetOf` resolves *the artifact containing that class*. Before the split that
jar held the annotations **and** `SetOnce`/`Either`; now it holds only annotations,
so every such site silently loses the support types. The failure mode is not an
error but an **empty result** — the daemon test failed with `no annotation
starting with '@Immutable' in []`, because the analysed source could not resolve
`SetOnce`, so nothing was analysed at all.

Ten sites fixed, including one in **production** (`InputConfigurationAssembler`,
the daemon's classpath assembly) and one hardcoded path
(`.addClassPath("../maddi-support/build/classes/java/main")`) whose absence broke
**386 of 436** tests in `maddi-inspection-integration`. One assertion legitimately
changed meaning: `TestJavaInspector3RealClasspath` asserts which source set
`@ImmutableContainer` comes from — now `maddi-annotation`.

⛔ **Run the suite with `--continue`.** Gradle stops at the first failing task, so
"only ide-daemon fails" hid those 386 failures entirely until `--continue` ran.

**Outstanding: 8 tests in `maddi-inspection-openjdk`** (`TestJavaInspector5RealClasspathModule`,
`TestJavaInspector6MultiProject`), failing with `CompilationProblems`. They parse
maddi's *own sources*, and `maddi-support/src/main/java` now carries a
`module-info` requiring `io.codelaser.maddi.annotation`; putting the annotation
source set on the classpath is not enough, it has to be wired as a module in the
parser's graph. That is work on the inspector's test harness, not another
one-liner.

## After the rename, same session

These are not scriptable and each breaks the build if forgotten:

- `gradle.properties`: `version` 0.9.0 → **0.9.1**
- **Delete** `maddi-gradleplugin/src/main/resources/META-INF/gradle-plugins/org.e2immu.analyser.properties`
  (a dead second plugin id; the real descriptor is generated)
- `dogfood/build.gradle.kts` requests the **old** plugin id — rename it, or dogfood stops
  resolving. Since 2026-08-17 there is no version to move with it: nothing under `dogfood/`
  names a version any more (`settings.gradle.kts` reads it from `gradle.properties`), so the
  plugin and the maddi-support/maddi-util jars follow the bump for free
- `testmvnplugin-export/pom.xml`: artifactIds `e2immu-external-support` /
  `-internal-util` / `-internal-graph` → `maddi-annotation` / `maddi-support` /
  `maddi-util` / `maddi-graph`
- The **maddi-support split** (section 7) — run `./rename.py split-triage` and
  work from its output, not from the snapshot in the map
- Regenerate the `test-oss` corpus annotations (`update-docstrings`), whose
  `package-info.java` files import `org.e2immu.annotation.Docstrings`
- **Hand-review every `.md` the substitution touched** — the diff above, not the file list.
  A document that *mentions* the old prefix on purpose cannot be protected by a per-file
  marker unless the whole file is about the old name (then: section 4, FROZEN). The mixed
  ones — `PUBLISHING.md`, `docs/eventual-info-hierarchy.md` — carry live maddi packages that
  must be renamed **and** claims about the old name that must not, in the same document, and
  only a reader can tell them apart. Of `PUBLISHING.md`'s three substitutions, two were right
  and one turned a true sentence into a self-contradiction

## The SECOND rename: the names users type (2026-08-19)

The `org.e2immu` cutover moved packages, JPMS modules, directories and the plugin id. **It moved
nothing a user types.** Two days later 0.9.1 was about to ship a plugin called
`io.codelaser.maddi.analyzer` whose task was `e2immu-analyzer`, configured by an `e2immu { }` block,
writing to `build/e2immu` -- and both CLI mains printed `e2immu-analyzer` in their own `--help`
while the launcher is `maddi`. That was not a decision anyone had made; it was the residue of a map
that only ever matched dotted package prefixes.

Applied across all eight repos: 143 replacements in 52 files, plus one file rename
(`E2ImmuAnnotationsImpl` -> `MaddiAnnotationsImpl`).

| kind | before | after |
|---|---|---|
| Gradle task | `e2immu-analyzer` | `maddi-analyzer` |
| Gradle task | `e2immu-write-input-configuration` | `maddi-write-input-configuration` |
| Gradle extension, task group | `e2immu` | `maddi` |
| default results dir | `build/e2immu` | `build/maddi` |
| consumable variant | `e2immuSourceElements` | `maddiSourceElements` |
| variant category | `e2immu-sources` | `maddi-sources` |
| CST API | `Types.e2immuAnnotation(s)` | `Types.maddiAnnotation(s)` |
| class | `E2ImmuAnnotationsImpl` | `MaddiAnnotationsImpl` |
| system properties | `e2immu.localPluginRepo`, `.pluginVersion`, `.modanalyzer.*`, `e2immu.preload` | `maddi.*` |
| identifiers | `withE2ImmuSupportFromClasspath`, `acceptAsE2ImmuModification`, `isNotE2ImmuInstance`, `runE2immu`, `e2immuDependencyGraph`, `e2immuPrep`, `e2immuConfig`, `e2immuSourceSetName` | `...Maddi...` / `maddi...` |

⛔⛔ **NO RULE MAY MATCH A BARE `e2immu`.** Of the 1,998 case-insensitive occurrences across the
eight repos, **1,429 are supposed to be there** -- 1,129 frozen `.gml` node labels and ~300 parser
test-input packages in Java text blocks -- and a further tranche is prose about the genuinely
separate predecessor project, its website and its GitHub issues. Every rule above is an exact
compound token; the four bare-word cases (`"e2immu"` as extension name and as task group, `e2immu {`,
and two AsciiDoc spellings) were listed individually. A blanket rule would have rewritten the very
fixtures section 4 exists to freeze.

Excluded by hand, and why: `docs/doc-audit-2026-07-30.md` (a dated audit that QUOTES the old task
name as its finding), `docs/eventual-info-hierarchy.md` and `eventual-design-improvements.md`
(design prose), `TestWriteAnalysis` and `TestTypeDependencies` (fixtures whose data IS the old
package), `TestExtractBuildProjectNames` (assertions over a recorded build log),
`OrgE2immuSupport.java` (the AAPI class name mirrors a package that moved -- cosmetic, and its
`PACKAGE_NAME` constant is already correct), and the recorded `inputConfiguration.json` /
`refactor.log` / `refactor.graphml`.

THE GATE: whole-build `compileJava compileTestJava` green; `:maddi-gradleplugin:test` 11 tests, 0
skipped, 0 failed; `:maddi-gradleplugin:slowTest` `shadedPluginResolvesAndRunsFromLocalRepo` green --
that one publishes the plugin to a local repo, resolves it fresh with no analyzer module on any
classpath, writes a `maddi { }` block and runs `maddi-analyzer`, so it exercises the whole renamed
surface end to end.

⚠ The Gradle Plugin Portal re-triggers manual review on an **id** change, never on a task rename.
That asymmetry is why this was worth doing before the first publish, and why it would still have
been cheap afterwards.

## NOT COVERED: the `jfocus` package rename

This map does **`org.e2immu` only**. `io.codelaser.jfocus` is a live Java package
prefix in the six private repos and nothing here touches it:

| repo | `package io.codelaser.jfocus…` | dirs |
|---|---|---|
| jfocus-refactor-service | 959 | 25 |
| jfocus-metrics | 385 | 23 |
| jfocus-stdbase | 282 | 8 |
| jfocus-standardize | 231 | 14 |
| refactor-resource | 206 | 11 |
| jfocus-transform | 166 | 10 |
| **total** | **2,229** | **91** |

Plus the repo directory names themselves, which appear in every
`includeBuild("../jfocus-…")` line across all seven settings files.

It is a second map of the same shape and the same machinery drives it — but the
target prefix has not been chosen, and unlike `org.e2immu` it is **not** required
for maddi to be published. Decide it separately; running both in one cutover
doubles the blast radius on the one day you least want that.

### And a third thing, which is NOT the package rename: the `E2Immu*` identifiers

#### The identifier families: DONE, verified 2026-08-19

Surveyed 2026-08-17, after the `org.e2immu` cutover verified clean. The six
families below totalled **1,020 occurrences**. They are now at **12** — and all
twelve are the table itself, below, describing them. Nothing in code remains.

⚠️ Grep for this **case-insensitively**, then and now. A case-sensitive sweep
found 128 and read as harmless leftovers; it missed every one of these.

| identifier | was | what it was |
|---|---|---|
| `e2immuAnalysis` | 382 | field and variable name |
| `E2IMMU_PREP` | 261 | `DSLBase.Resource` enum constant |
| `E2ImmuPrep` | 190 | record, `jfocus-metrics/…/metrics/common/E2ImmuPrep.java` |
| `e2ImmuPrep` | 147 | local variables |
| `E2ImmuPrepResource` | 22 | class, `refactor-service` |
| `E2IMMU_SUPPORT` | 18 | constant |

Kept here because the hard part is worth remembering rather than because it is
outstanding. Most of it was identifier renaming a compiler checks. One part was
not:

```java
ResourceService.java:17   String E2IMMU_PREP_NAME = "E2IMMU_PREP";
…                         resourceService.resource(DSLBase.Resource.E2IMMU_PREP.name())
```

`.name()` made the enum constant's **string** a runtime lookup key, and the name
had left the code entirely: **8 `.py` recipe files** under `modularization/` and
**25 `.md` documents**, `docs/ADDING-A-DSL-METHOD.md` among them. Recipes are
data; renaming the constant without them would have broken them at run time with
nothing in between to catch it. The string was in no `.json`/`.csv`/`.txt`, which
is what made it a rename and not a data migration.

`PREPWORK` was chosen because every sibling constant — `FOOTPRINT_BASE`,
`TEXT_INDEX`, `METHOD_CALL_GRAPH`, `ANALYSIS_ORDER`, `CYCLE_SCORES`,
`MODIFICATION_ANALYSIS` — is named for **what it is**, and `E2IMMU_PREP` alone
was named after a project that no longer exists.

#### What is left, surveyed 2026-08-19

Measured with `git grep -i e2immu` over tracked files in the nine `ws/object`
worktrees, corpora symlinks excluded: **1,842 matching lines in 193 files**, of
which `maddi` is 1,718 in 136. That total is dominated by two populations
deliberately left alone (see *Out of scope* below); what follows is everything
that is not.

The four groups are in the order they should be done, which is the order of how
expensive it becomes to do them later.

##### 1. The Gradle plugin's user-facing vocabulary — the only one with a deadline

⛔ **Blocked as of 2026-08-19: there is ongoing work in `maddi-gradleplugin`.
Do not start here until Bart says it is clear.**

The plugin id is `io.codelaser.maddi.analyzer`; everything a user then types is
still `e2immu`:

| constant | value | what the user meets |
|---|---|---|
| `ANALYZER_EXTENSION_NAME` | `e2immu` | the `e2immu { … }` block in their build file |
| `ANALYZER_TASK_NAME` | `e2immu-analyzer` | `gradle e2immu-analyzer` |
| `WRITE_INPUT_CONFIGURATION_TASK_NAME` | `e2immu-write-input-configuration` | ditto |
| `SOURCE_ELEMENTS_CONFIGURATION_NAME` | `e2immuSourceElements` | the variant a depending project reselects |
| `SOURCES_CATEGORY` | `e2immu-sources` | the `Category` attribute value |
| `AnalyzerPropertyComputer.PREFIX` | `e2immu-analyzer.` | property prefix |
| `AnalyzerPlugin` `setGroup` | `e2immu` | the heading in `gradle tasks` |
| `AnalyzerPlugin:185` | `build/e2immu` | the output directory on disk |

`AnalyzerExtension.java:20,21,22,30,31`, `AnalyzerPropertyComputer.java:55`,
`AnalyzerPlugin.java:77,90,185`. Two more in the plugin's own build:
`maddi-gradleplugin/build.gradle.kts:169,170` set `e2immu.localPluginRepo` and
`e2immu.pluginVersion`.

**Why this one first.** A Gradle plugin's extension and task names *are* its API.
After the Portal publish (`PUBLISHING.md`, step 4) changing them breaks every
consumer's build, and a first publish in a new namespace is reviewed by hand, so
there is no quick correction. The Maven plugin — same campaign, same release —
already got this right: `artifactId maddi-mvnplugin`, `goalPrefix maddi`. The
asymmetry is the argument.

They are load-bearing, not decorative. maddi's own projects use them:
`dogfood/{cst-api,cst-impl,cst-analysis}/build.gradle.kts` each open `e2immu {`,
`maddi-run-openjdk/build.gradle.kts:101` names the task
`:cst-impl:e2immu-write-input-configuration`, `testgradleplugin-analyzer` and
`testgradleplugin-writeaapi` both `dependsOn(tasks.getByName("e2immu-analyzer"))`,
and `maddi-aapi-parser/build.gradle.kts:80` sets `group = "e2immu"`.

##### 2. Public API whose name is the only thing not yet renamed

Each of these has a body that already says `maddi`. The name is the whole of what
is left, which is why they read as oversights rather than decisions.

| declaration | the body says |
|---|---|
| `InputConfiguration.withE2ImmuSupportFromClasspath()` — `maddi-inspection-api/…/InputConfiguration.java:197` | `Map.of("maddiSupport", "io/codelaser/maddi/annotation")` (`InputConfigurationImpl.java:71`) |
| `ToolChain.CLASSPATH_E2IMMU` — `maddi-inspection-integration/…/ToolChain.java:22` | every entry `io/codelaser/maddi/…` |
| `OrgE2immuSupport` — `maddi-aapi-archive/…/libs/support/OrgE2immuSupport.java:36` | `PACKAGE_NAME = "io.codelaser.maddi.support"` |
| `Types.e2immuAnnotation(String)`, `Types.e2immuAnnotations()` — `maddi-cst-api/…/runtime/Types.java:42,44` | implemented over `E2ImmuAnnotationsImpl` |
| `E2ImmuAnnotationsImpl` — `maddi-cst-impl/…/element/E2ImmuAnnotationsImpl.java:40` | the maddi annotation set |
| loggers `e2immu.modanalyzer.decide`, `e2immu.modanalyzer.delay` — `CommonAnalyzerImpl.java:26,27` | a logger name is config and log output, so it is user-visible too |

This group leaks outward, which the first does not:
`withE2ImmuSupportFromClasspath()` is called from **`jfocus-refactor-service`**
(`TestWriteFile.java:59`) and **`refactor-resource`** (`TestAddFile.java:54`), and
`CLASSPATH_E2IMMU` from **`jfocus-stdbase`** (`TestParseViewerProject.java:53`).
Rename these and those call sites move in the same commit or nothing compiles —
which is the good kind of coupling, and the reason to do it before the group
grows.

##### 3. Names of modules that no longer exist

maddi's modules are `maddi-cst-api`, `maddi-inspection-parser`, … These still say
`e2immu-`, and unlike everything above they are not merely old — they name
artifacts that cannot be produced:

- `jfocus-refactor-service/src/test/resources/io/codelaser/jfocus/refactor/service/inputConfiguration.json:96–209`
  — fifteen distinct module names over 31 lines, each `"name": "e2immu-cst-api"` paired with
  `"uri": "file:libs/e2immu-cst-api.jar"` and so on.
- `jfocus-standardize/README.md:5–94` — the entire module list, `e2immu-cst-api`
  through `e2immu-shallow-main`.
- `maddi/testmvnplugin-export/pom.xml:122` — `<artifactId>e2immu-shallow-analyzer</artifactId>`;
  there is no shallow module in `settings.gradle.kts` at all.
- `maddi/testgradleplugin-analyzer/build.gradle.kts:77` — a path into
  `analyzer-shallow/e2immu-shallow-aapi/…`.

Nothing here is compiler-checked, so it will not be found by doing the work
above; it has to be listed, which is what this is.

##### 4. Consumer-side identifiers in the sibling repos

Ordinary renaming a compiler checks, listed so the sweep is complete:

| identifier | where |
|---|---|
| `e2immuDependencyGraph` | `jfocus-metrics` `GraphComputer.java:9` — an **interface parameter** — plus `GraphComputerImpl.java:40,47`, `TestInternalMethodCycle.java:76,99`, `TestTypeHierarchyCycle.java:55,64`; and `jfocus-refactor-service` `CycleScoresResource.java:32` |
| `RunAnalyzerCommand.runE2immu(…)` | `jfocus-refactor-server` `RunAnalyzerCommand.java:49,55`, called from `TestInputConfigurationAction.java:15` — public static |
| `acceptAsE2ImmuModification` | `jfocus-standardize` `AnalyzedMethodImpl.java:635,645` |
| `isNotE2ImmuInstance` | `jfocus-stdbase` `Preconditions.java:39,49` |
| `e2ImmuResource` | `jfocus-refactor-server` `TestSplitClass2.java:74`, a local |

#### Out of scope — decided by Bart, 2026-08-19

Recorded so the next survey does not re-raise them as findings. Both are large
enough to dominate a naive count, which is exactly why they are written down.

- **`package org.e2immu.analyser.resolver.testexample;`** — **522 occurrences across
  46 maddi test files**, i.e. more than a quarter of the 1,842, inside inline Java
  strings that are test *input*. It is
  arbitrary sample source that maddi parses; nothing resolves against it. Renaming
  is free and buys only a quieter grep.
- **Prose and links** — issue links to `github.com/e2immu/e2immu/issues/…`,
  `e2immu.org` URLs, and comments referring to the tool as it was ("e2immu's
  `GET_SET_FIELD` analysis", "the historical e2immu distance vocabulary"). These
  are history and read correctly as history. The `e2immu.org` links on the repo
  homepage and README are a separate publication task, not a rename.

Sequencing: independent of the `jfocus` package rename above. Group 1 is gated on
the plugin work finishing; 2, 3 and 4 are not gated on anything and 2 is the one
that gets more expensive as the API spreads.

## Do not

- **Do not add `e2immu` to `.githooks/internal-names.txt`.** 303 `org.e2immu.analyser`
  tokens survive on purpose inside parser test text blocks; the scrub hook would
  refuse every commit that touches those fixtures.
- **Do not touch the two `*.gml` files.** They record a dependency graph of the
  old e2immu-analyser project and `TestTypeDependencies` asserts topology only
  (maxCycleSize 31, actionLog 101, list 100). Their directory moves; their bytes
  do not.
- **Do not run this with the refactor server.** A prefix rename with no target
  collisions has no semantic content, and the server reaches neither module
  names, directories, `.bazel`/`.kt`/`MANIFEST.MF`, nor the plugin id. Its
  compile probe is also narrower than a seven-repo blast radius.

## Proving it worked

`verify` is the fast check. The stronger one is the round-trip: apply the inverse
map and diff against the pre-rename commit (`./rename.py inverse` prints the
recipe). It proves the map lost no information — but only **modulo two
exemptions it lists**, because section 1 is many-to-one (`language` and
`analyzer` both collapse to `io.codelaser.maddi`) and `analyser→analyzer` is
lossy. A green build across all seven repos plus a green `slowTest` on maddi
remains the acceptance test; the round-trip is what tells you *why* it is green.
