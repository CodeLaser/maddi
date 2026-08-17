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
and re-run, so the change stays a function.

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
