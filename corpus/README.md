# The maddi test corpus

maddi is measured against real projects: Apache, Google, JetBrains and other open-source codebases,
checked out at fixed commits, built, and turned into an `inputConfiguration.json` that maddi parses.
This directory is the tooling that does that, reproducibly, on any machine. The slow tests
(`./gradlew slowTest` in maddi-mod) are its consumers.

This README is the manual. The two other places worth reading are the module docstring of
[`scripts/catalogue.py`](scripts/catalogue.py) (the reference for the catalogue's rules) and the
comments in each [`catalogue/<name>.yml`](catalogue/) (why each corpus is configured the way it is).
Where this file and those disagree, those are newer.

- [How it fits together](#how-it-fits-together)
- [Quick start: a new machine](#quick-start-a-new-machine)
- [Concepts](#concepts)
- [The catalogue entry, field by field](#the-catalogue-entry-field-by-field)
- [Config routes](#config-routes)
- [Commands](#commands)
- [Workflows](#workflows)
- [Environment variables](#environment-variables)
- [What lives where](#what-lives-where)
- [Troubleshooting](#troubleshooting)

---

## How it fits together

```
  catalogue/<name>.yml          one file per corpus: where it comes from, how to build it,
          │                     how to configure it, what to measure
          ▼
  scripts/catalogue.py          reads the entries; prints the command for each phase,
          │                     checks pins / JDKs / outputs, records baselines
          ▼
  Taskfile.yml                  runs those commands (task corpus:...), streams their output
          │
          ▼
  $TEST_OSS_ROOT/<name>/        the checkout (default ~/git/test-oss/<name>), built, with
                                inputConfiguration.json beside its sources
          │
          ├──▶ maddi-mod  ./gradlew slowTest           the corpus tests (TestTimefoldSolver, ...)
          ├──▶ ~/refactorhome/{projects,work}/<name>   registration, so the engine can load it
          └──▶ catalogue/baselines/<name>.tsv          primary types per source set, to diff against
```

Three repositories take part. All three are expected side by side (`~/git/maddi`, `~/git/maddi-mod`,
`~/git/maddi-dist`), and each location can be overridden (see [Environment variables](#environment-variables)):

| repository | what the corpus tooling uses from it |
|---|---|
| **maddi** (this one) | `corpus/`: the catalogue, the scripts, the Taskfile. The parser, which a baseline counts with. |
| **maddi-mod** | the corpus tests (`maddi-run-analysis`, `maddi-run-kotlin-analysis`), run by `test:slow`, `sweep` and the `analyse` phase |
| **maddi-dist** | the CLI launchers (`maddi-cli`, `maddi-cli-kotlin`) the config and parse phases run, and the Maven/Gradle plugins the plugin routes use |

`corpus/` lived in maddi-mod from the three-repository split until 2026-10-03, when it moved back
here. The reasons are in
[docs/roadmap/split-maddi-into-three-repositories.md §7](../docs/roadmap/split-maddi-into-three-repositories.md).

---

## Quick start: a new machine

### Prerequisites

- **[Task](https://taskfile.dev)** (`brew install go-task`) and **[uv](https://docs.astral.sh/uv/)**
  (`brew install uv`). `catalogue.py` declares PyYAML in its `#!` line and `uv` provides it, so no pip
  install is needed. Run it as `scripts/catalogue.py` or through `task`, never with a bare `python3`.
- **git**, and the three repositories checked out side by side.
- **A JDK for maddi itself: 26 or newer.** Some maddi modules compile at source level 26. maddi always
  runs on the ambient JDK (`JAVA_HOME` / `java` on PATH).
- **The JDKs the corpora need.** Most build on whatever JDK is ambient. Those that do not say so in
  `build.jdk` / `config.jdk`, and a [machine profile](#machine-profiles) tells the tooling where each
  version lives on this machine. The public entries need 21 (exposed's and retrofit's captures) and 25
  (pulsar). Older Lombok-based builds in an overlay typically need 17.
- `ant` only for tomcat, which is not in the catalogue.

### Steps

Run these from the maddi repository root. `task corpus:<x>` is `task <x>` run in `corpus/`, and the
root Taskfile also loads `corpus/.env`.

```bash
task corpus:init          # writes corpus/.env (TEST_OSS_ROOT, JAVA_HOME); review it
task corpus:doctor        # prints the resolved paths and checks the JDK
task corpus:catalogue:machine INIT=1     # drafts a machine profile from the JDKs installed here
#   -> review it, save it as <profiles dir>/<hostname -s>.yml, point CORPUS_MACHINES at that dir
task corpus:install:wired # obtain at the pin + build + configure + register every corpus this
                          # machine holds -- HOURS. FROM=<name> resumes after a failure
task corpus:verify        # per corpus: present? at its pin? built? configured? registered?
task corpus:test:slow     # the corpus tests, in maddi-mod (forces a re-run)
```

One corpus at a time is `task corpus:ready NAME=fernflower` (pull, build, config, check). fernflower
is the small one, about 500 source files, and the right first target.

⚠ **A green `slowTest` is not by itself evidence.** A test task served from cache, a corpus that
was skipped, or a run that analysed nothing all look green. Read AGENTS.md §Commands before quoting
one, and read the per-test roll-call.

---

## Concepts

### Entries and phases

Every corpus is one YAML file in a catalogue directory, an **entry**. It describes up to five
**phases**, each optional:

| phase | what it does | declared by | Task |
|---|---|---|---|
| **build** | builds the project with its own tool (Maven, Gradle) | `build.cmd` | `catalogue:build` |
| **config** | produces `inputConfiguration.json`: source sets, class path, compiler options | `config.route` | `catalogue:config` |
| **parse** | maddi parses the configuration, no analysis (`--analysis-steps=none`). Does it load, and does everything parse? | always available (Java runners) | `catalogue:parse` |
| **analyse** | the analyzer over the corpus: the entry's `parse.test` in maddi-mod's `slowTest`, or else a run at `parse.steps` | `parse.test` / `parse.steps` | `catalogue:analyse` |
| **tests** | the project's *own* tests | `tests.cmd` | `catalogue:tests` |

`catalogue.py plan <phase> <name>` prints the shell command a phase would run, and the Taskfile runs
it in the project directory. Exit code 2 from `plan` means "this entry has no such phase", which is
normal. guava has no `build` because its config route *is* the build, and the composites skip that
phase and go on.

### Success is what the next phase needs

A build is judged by **`build.provides`**: the paths that must exist afterwards, not the exit
code. langchain4j's reactor fails on provider modules nobody parses (`expect: partial`), and that is
fine as long as `langchain4j-core/target/classes` is there. `catalogue:build` therefore ignores the
build's exit code and then runs `check-provides`.

### Pins

`source.rev` is the commit a corpus is measured at. Baselines are shared by every machine, so a
checkout at another commit would make a diff about upstream instead of about maddi. So:

- `obtain` clones if needed and checks out the pin, detached. It refuses to check out over tracked
  edits.
- `build`, `baseline` and `register` refuse an off-pin checkout (`check-rev`).
- `doctor` shows the state of each checkout: `ok`, `OFF` (another commit), `unpinned`, `+dirty` (tracked edits),
  `+patched` / `+UNPATCHED` (see [Patches](#patches)).
- `pin` writes the checkout's HEAD (or `--rev`) into the file that declared `source`. It does **not**
  re-record the baseline. Do that separately, after reading the diff (see
  [Moving a pin](#moving-a-pin)).

An entry without `source` that shares another entry's checkout (`dir: pulsar`) is pinned by that
entry, its **owner**. One tree has one pin.

### Patches

Occasionally upstream code at the pin cannot go through a tool we cannot avoid. Example: Lombok
1.18.48, the only Lombok that runs on JDK 27, rejects pulsar's `@Builder(builderClassName = "Builder")`.
**`source.patches`** lists patch files that are part of the corpus:

- they are applied by `obtain` and `clean`, after checking out the pin;
- the pin checks accept exactly *HEAD + the declared patches*. A declared patch left unapplied
  fails, and so does any other tracked edit. The comparison is of trees, so the same edit made by
  hand counts as applied;
- the gate records such a tree as `<sha>+<patch>`;
- a patch applied *after* the project was built leaves sources newer than their class files, and
  maddi drops the compilation units that reference a stale class. `obtain`/`clean` warn about
  this: rebuild before measuring.

A patch file lives beside the catalogue file that declares it (`catalogue/patches/<name>/`). It
starts with a header saying what it changes, why, which tool and version force it, and when it can
be removed. Patches are for making upstream code *go through a tool*, never for making maddi's
numbers better.

### Baselines

`config.baseline` names a TSV of **primary types per source set**, recorded from a parse-only run.
`catalogue:config` diffs against it after every configuration, and `catalogue:baseline` does so on
demand:

```
# source set	primary types -- `catalogue.py baseline <name> --record`
# recorded 2026-10-03 on <host>, corpus 8576283da4ad, maddi 85bdb4efb2a1
managed-ledger/main	121
managed-ledger/test	57
...
```

The table is per source set, not a total, because a total hides coverage moving between modules.
It catches the failure that has actually happened: a capture that silently lost modules (timefold
captured without `clean`: 22 source sets instead of 65, and nothing else noticed). A drift is
printed as `+`/`-`/`~` lines. Read them, then accept with `RECORD=1`. Recording requires a pin and
writes where the baseline was declared. A private overlay's baseline never lands in this public
repository (`record_refusal`).

### Vendoring

A generated configuration names its class path by absolute path, mostly into `~/.gradle/caches` and
`~/.m2`. Gradle deletes cache entries it has not used for 30 days, and on 2026-09-25 that took 18
jars from three corpora at once. So `catalogue:config` ends with `catalogue.py vendor`, which copies
every such jar to **`$TEST_OSS_ROOT/lib/<project>/`** and rewrites the configuration to point there
(`scripts/vendor-libraries.py`; its docstring has the layout). It is idempotent, it hard-links jars
shared between projects, and it downloads a jar that is already gone from Maven Central, checked
against its SHA-1.

It also replaces a **Lombok that cannot run on the target JDK**: Lombok ≤ 1.18.46 dies on JDK 27
(`EndPosTable` was removed), so on 27 the configuration is pointed at 1.18.48 instead. A Lombok that
runs on the target JDK is left as the project declared it.

### Registration

`catalogue:config` ends by **registering** the corpus with the refactoring engine:
`$REFACTOR_HOME/projects/<name>` links to the checkout, and `$REFACTOR_HOME/work/<name>/` gets a
generated `project.yml` and a *link* (not a copy) to the configuration. `doctor` shows it in the
`engine` column. A stale link counts as unregistered. Registering checks the pin, because the engine
resets a project to the `baseRevision` written there.

### Machine profiles

A profile, `<hostname -s>.yml` in a directory on `$CORPUS_MACHINES`, says what *this* machine is
supposed to hold and where its JDKs are. This repository ships the reader and no profiles, because
profiles name hosts and home-directory paths.

```yaml
host: my-box                    # must match the file name
os: linux                       # linux | macos
role: gate                      # gate | devel
test_oss_root: ~/git/test-oss   # checked against the effective TEST_OSS_ROOT
jdks:                           # build.jdk / config.jdk pick from this list by version
  - {version: 17, home: /usr/lib/jvm/temurin-17-jdk-amd64}
  - {version: 25, home: ~/.gradle/jdks/eclipse_adoptium-25-amd64-linux.2}
holds: active                   # every `status: active` entry, or a list of names
skip:                           # exceptions to `holds`, each with its reason
  vavr: not checked out here yet
```

`catalogue:machine INIT=1` drafts one from the JDKs it finds (`/usr/lib/jvm`,
`/Library/Java/JavaVirtualMachines`, Homebrew's `/opt/homebrew/opt/openjdk*`, sdkman,
`~/.gradle/jdks`). On macOS prefer Homebrew's `opt/` paths, which survive a `brew upgrade`. Without a
profile, every entry is "held" and `doctor` reports gaps without failing on them. With a profile,
`doctor` exits 1 when a held entry is not present, built, configured and registered.

### Catalogue directories and overlays

`$CORPUS_CATALOGUE` is a colon-separated list of catalogue directories, in precedence order. The
default is this `catalogue/`. A later directory, typically a private one, can:

- add entries maddi never sees;
- **`extends: <name>`**: add or override fields of an earlier entry. Mappings merge recursively,
  lists and scalars are replaced whole;
- **`replaces: true`**: replace an earlier entry entirely.

Two files with the same name and neither key is an error. `catalogue:show NAME=<x>` prints the
resolved entry and which file each field came from. Everything written *from* a field (a pin, a
baseline, a patch, a `config.then` script) lives beside **the file that declared that field**, so
private data stays in the private catalogue.

CodeLaser's private overlay (machine profiles, private entries) lives in its own repository and sets
`CORPUS_CATALOGUE` and `CORPUS_MACHINES` in its Taskfile. maddi never names what is in it.

### What we write into a checkout

The checkouts are third-party trees, and some of what is in them is ours: `inputConfiguration.json`,
`compile.log`, `compile.javac.log`, and a few entry-specific files. `catalogue.py generates` lists
them, derived from each entry's route and `config.generates`/`generates`, so a preserve-list for
`git clean` cannot go stale. `clean` discards tracked edits, deletes only the directories
`source.generated` names (never `git clean -fdx`: questdb and caffeine keep corpus sources in build
output), and returns to the pin. With `--also-ours` it deletes our files too.

---

## The catalogue entry, field by field

The entries in [`catalogue/`](catalogue/) are the best examples: `fernflower.yml` (small, complete),
`timefold-solver.yml` (large Maven reactor), `pulsar.yml` (multi-module Gradle, a JDK, a patch),
`vavr.yml` (`config.then`, `parse.config`), `detekt.yml` (Kotlin), `fernflower-plugin.yml` (an A/B
twin on a shared checkout).

### Top level

| field | meaning |
|---|---|
| `name` | the entry's name; defaults to the file name |
| `status` | `active` is what profiles hold by default; anything else (`dormant`) is listed but not held |
| `summary` | one paragraph: what this corpus is for and which tests consume it |
| `dir` | the checkout directory, relative to `TEST_OSS_ROOT` (default: the name), or absolute (`~` and `$VARS` expanded) for a project outside the corpus root |
| `generates` | our own files in the checkout that no phase writes (fernflower's Eclipse `.project`/`.classpath`) |
| `extends` / `replaces` | overlay keys, see [above](#catalogue-directories-and-overlays) |

### `source`: where the tree comes from

| field | meaning |
|---|---|
| `kind` | `git`; anything else cannot be obtained by the tooling (copy-only trees) |
| `url` | the clone URL |
| `rev` | the pin, a full SHA; written by `pin` |
| `patches` | patch files that are part of the corpus, relative to the declaring file; see [Patches](#patches) |
| `generated` | directories (not patterns) of generated sources that `clean` deletes |

### `build`: making it compile

| field | meaning |
|---|---|
| `cmd` | the build command, run in the checkout |
| `expect` | `complete` or `partial`: whether the command is expected to succeed. Informational; `provides` decides |
| `known_failures` | for `partial`: what fails and why that does not matter |
| `provides` | paths that must exist after the build; `check-provides` asserts them |
| `jdk` | `{version: N, vendor: [...]}`: the JDK the build needs. Resolved through the machine profile (or `BUILD_JAVA_HOME`) and passed as `JAVA_HOME`, and for a Gradle build also as `-Dorg.gradle.java.home`, which overrides a pin in `~/.gradle/gradle.properties`. `check-jdk` verifies version and vendor first |

### `config`: producing `inputConfiguration.json`

| field | meaning |
|---|---|
| `route` | how the configuration is produced; see [Config routes](#config-routes). `none` or absent: no config phase |
| `module` | maven-plugin: the Maven module (`-pl`). gradle-plugin: the Gradle project path |
| `tasks` | maven-log / gradle-log: the goals or tasks the capture runs |
| `jdk` | gradle routes: the JDK Gradle runs on, as `-Dorg.gradle.java.home` |
| `build_java_home` | maven-log: an explicit JDK for the capture (default: `build.jdk`'s) |
| `output` | where the configuration goes. Relative to the checkout (an A/B twin's own file), or absolute (a private project's work dir). Default `inputConfiguration.json` |
| `baseline` | the TSV to diff against; see [Baselines](#baselines) |
| `generates` | what the route writes into the checkout, when it differs from the route's default |
| `then` | shell commands run after the route, each only if the one before succeeded. `{scripts}` is the `scripts/` beside the declaring catalogue |
| `extra_jmods` | JDK modules beyond `java.se` to put on the class path (e.g. `jdk.compiler`) |
| `mvn_flags` | maven-plugin: flags the plugin run must repeat from the build (jenkins: `-Denforcer.skip=true`) |
| `gradle_args` | gradle-plugin: extra Gradle arguments |
| `apply_to` | gradle-plugin: which project the init script applies the plugin to (default: all) |
| `init_script` | gradle-log routes: a Gradle init script that adjusts the build without editing the checkout (retrofit's toolchains) |
| `rewrite_reactor_jars` | maven-log under `install`: rewrite sibling `target/*.jar` entries back to `target/classes` |
| `exclude_modules` | maven-log: modules to leave out of the reactor |
| `maddi_args` | maven-log: extra options for `maddi --compile-log` (e.g. `--jre`) |
| `mem` | maven routes: `-Xmx` for Maven (default `6G`) |
| `script` | script route: the script that writes the configuration, relative to the declaring file |

### `parse`: what maddi does with it

| field | meaning |
|---|---|
| `runner` | `openjdk` (default), `kotlin`, or `main`: which driver and which corpus-test module |
| `test` | the corpus test in maddi-mod that consumes this corpus (`TestTimefoldSolver`); the `analyse` phase runs it |
| `steps` | analysis steps for the `analyse` phase when there is no `test` (`[prep]`, `[modification]`) |
| `config` | the configuration the parse reads, when it is not the one the config phase wrote (vavr's derived `inputConfiguration.main.json`) |
| `resource_claim` | read by the jfocus testrunner: tests that mutate this tree in place hold it exclusively |

Kotlin entries have no parse-only mode yet: `--analysis-steps=none` stops before parsing there, so
`parse` and `baseline` refuse them instead of passing vacuously. Use `analyse`.

### `tests` and `engine`

| field | meaning |
|---|---|
| `tests.cmd` | runs the project's own tests (`./mvnw test`) |
| `engine.project` | the name the engine knows this corpus by, when it differs from the entry name (A/B twins) |
| `engine.branch` | the branch written into `project.yml`; default: the remote's default branch |

---

## Config routes

| route | how | writes | use it for |
|---|---|---|---|
| `maven-plugin` | `mvn -pl <module> generate-test-sources maddi-mvnplugin:write-input-configuration` | `inputConfiguration.json` | one module of a Maven reactor (activemq, camel, jenkins, langchain4j, vavr) |
| `maven-log` | `./mvnw -X clean <tasks>`, filter the javac lines, `maddi --compile-log` | `compile.log`, `compile.javac.log`, config | a whole Maven reactor at once (timefold-solver, guava) |
| `gradle-log` | `./gradlew --rerun-tasks <tasks> --debug`, grep the compiler arguments, `maddi --compile-log` | `compile.log`, config | a whole Gradle build (elasticsearch, fernflower, pulsar) |
| `gradle-log-kotlin` | the same, with kotlinc's arguments, through `maddi-cli-kotlin` | `compile.log`, config | Kotlin and mixed builds (detekt, exposed, retrofit) |
| `gradle-plugin` | the maddi Gradle plugin applied by an init script, `:maddi-write-input-configuration` | config | the A/B twins that test the plugin itself (fernflower-plugin, pulsar-plugin, opensearch-plugin) |
| `script` | runs `config.script` | config | builds no route reaches (coil's KMP slice, eclipse-collections) |

The plugin routes need the plugins in `~/.m2`: `task corpus:config:plugin` publishes both from
maddi-dist, and `install:wired`/`config:all` do it first.

Two traps every new log-route entry should know, both measured:

- **maven-log needs `clean`.** maven-compiler-plugin skips an up-to-date module and prints no javac
  line for it, so a capture over a built reactor is *silently partial*. The route always cleans;
  gradle-log uses `--rerun-tasks` for the same reason.
- **`install` vs `test-compile`.** Under `install`, later modules see siblings as jars, and maddi
  finds those types twice. Use `test-compile` where the reactor allows it (timefold). Where it needs
  `install` (guava), use `rewrite_reactor_jars`. Never delete those entries: they are the only link
  between modules.

---

## Commands

### Task (from the maddi root: `task corpus:<task>`)

**One corpus**

| task | does |
|---|---|
| `pull NAME=x` | clone if absent, check out the pin (+ patches) (= `catalogue:obtain`) |
| `build NAME=x` | `check-rev`, `check-jdk`, the build, `check-provides` (= `catalogue:build`) |
| `config NAME=x` | the route, `vendor`, the baseline diff if declared, `register` (= `catalogue:config`) |
| `check [NAME=x]` | `doctor` for one corpus, or all |
| `ready NAME=x` | pull → build → config → check |
| `clean NAME=x [ALSO_OURS=1]` | discard edits, delete `source.generated`, back to the pin (+ patches) |
| `catalogue:parse NAME=x` | parse-only run |
| `catalogue:analyse NAME=x` | the corpus test, or a run at `parse.steps` |
| `catalogue:tests NAME=x` | the project's own tests |
| `catalogue:baseline NAME=x [RECORD=1]` | diff the parse against the baseline; `RECORD=1` accepts it |
| `catalogue:pin NAME=x [REV=sha]` | move the pin (does not re-record the baseline) |
| `catalogue:show NAME=x` | the resolved entry, with each field's file |

**Everything this machine holds**

| task | does |
|---|---|
| `install:wired [FROM=x]` | `config:plugin`, then obtain/build/config each held corpus. Hours |
| `config:all` | regenerate every held configuration and diff its baseline (projects must be built) |
| `verify` | `doctor` over everything (= `catalogue:doctor`) |
| `catalogue:list [STATUS=active]` | one line per entry, with flags **P**resent, at pin (**R**), **B**uilt, **C**onfigured, **E**ngine-registered |
| `catalogue:machine [INIT=1]` | check this host's profile; `INIT=1` drafts one |

**Running maddi**

| task | does |
|---|---|
| `test:slow` | `./gradlew slowTest --rerun-tasks` in maddi-mod |
| `sweep [SWEEP=a,b]` | `TestCorpusSweep`, a first-contact run over every corpus with a configuration |
| `run:prep PROJECT=x \| CONFIG=path [STEPS=..] [XMX=..]` | the CLI on one configuration |

**Environment**: `doctor` (paths and JDK), `init` (writes `.env`), `env` (export lines), `vendor
[CONFIG=..] [DRY=1]` (vendoring by hand), `config:plugin` (publish both build plugins).

The Taskfile also still has tasks for projects that are not in the catalogue yet (`gradle`,
`hadoop`, `hazelcast`, `hibernate-orm`, `hive`, `keycloak`, `quarkus`, `tomcat`, `trino`, `wildfly`,
`sonarqube`, and `config:ignite-core` / `config:trino`). Those are hand-written clone/build/capture
recipes, and they become entries as they are needed.

### `scripts/catalogue.py`

The Taskfile is a thin layer over it, and every subcommand also works directly. The full list is in
its docstring. These are the ones without a Task wrapper or with options worth knowing:

```bash
scripts/catalogue.py plan <phase> <name>     # print, don't run, a phase's command
scripts/catalogue.py doctor [<name>...]      # the state table; exit 1 on a gap
scripts/catalogue.py check-rev|check-jdk|check-provides <name>
scripts/catalogue.py dir <name>              # where the phases run
scripts/catalogue.py generates [<name>...]   # our files in the checkouts (the git-clean preserve-list)
scripts/catalogue.py register <name>
scripts/catalogue.py vendor <name> [--dry-run]
```

Tests: `scripts/test_catalogue.py` and `scripts/test_vendor_libraries.py`, both runnable directly
(uv). They need no network and no build.

---

## Workflows

### Adding a corpus

1. Write `catalogue/<name>.yml`, starting from the entry closest in shape (see
   [the field reference](#the-catalogue-entry-field-by-field)). Leave `rev` out for now.
2. `task corpus:catalogue:obtain NAME=<name>`. With no pin, it clones and leaves the default branch.
3. Iterate on `build` until `task corpus:build NAME=<name>` passes `check-provides`, then on `config`
   until `task corpus:config NAME=<name>` writes a configuration that `catalogue:parse` loads.
4. `task corpus:catalogue:pin NAME=<name>` writes the commit you built.
5. Add `config.baseline: baselines/<name>.tsv` and record it:
   `task corpus:catalogue:baseline NAME=<name> RECORD=1`.
6. If a test consumes it, set `parse.test`. Put the *why* of every non-obvious field in a comment
   beside it: the next person to touch the entry has only those comments.

### Moving a pin

```bash
git -C $TEST_OSS_ROOT/<name> fetch && git -C $TEST_OSS_ROOT/<name> checkout <new sha>
task corpus:catalogue:pin NAME=<name>
task corpus:build NAME=<name> && task corpus:config NAME=<name>   # the config step diffs the baseline
# read the diff: is it upstream's change, as expected?
task corpus:catalogue:baseline NAME=<name> RECORD=1
```

Then check `source.patches`: a patch that no longer applies fails `obtain`, and one upstream has made
redundant should be removed with its declaration. Golden numbers in the corpus tests may need the
same move. Their commit message names the cause.

### A baseline drifted

`catalogue:config` or `catalogue:baseline` printed `+`/`-`/`~` lines and exited 1. Work out which
of three things happened before accepting anything:

- **the corpus changed**: the pin moved, the checkout is off its pin, dirty, or unpatched. `doctor`
  shows which;
- **the configuration changed**: a route lost modules (the partial-capture trap), a JDK changed, a
  jar went missing;
- **maddi changed**: the parser now finds more or fewer primary types. This is the case the baseline
  exists to surface. The `# recorded` line names the maddi commit it was measured with.

Accept with `RECORD=1` only when the diff is explained.

### A corpus needs a particular JDK

Declare it in the entry, `build.jdk: {version: N}` (and `config.jdk` for a Gradle capture), and make
sure every machine's profile lists that version. `check-jdk` fails before the build when it is
missing. Don't put the JDK into `build.cmd`: the profile is what knows where the JDK lives on each
machine. A JDK vendor requirement (trino's enforcer accepts only Temurin or Oracle) goes in
`build.jdk.vendor`.

### Upstream code needs a patch

1. Confirm the patch is unavoidable and does not change what is measured. pulsar's patch is a
   different spelling of the same annotation, generating the same code.
2. On a clean checkout at the pin, make the edit and `git diff > catalogue/patches/<name>/<what>.patch`.
   Restore the file.
3. Put a header above the diff: what, why (tool, version, the exact error), the date, and when it
   can be removed. `git apply` ignores text before the first `diff --git`.
4. Declare it in `source.patches`, with a comment that says the same in one paragraph.
5. `task corpus:pull NAME=<name>` applies it. Rebuild before measuring. Do this on every machine.

### Setting up a machine profile

`task corpus:catalogue:machine INIT=1`, review the draft (role, `test_oss_root`, which JDKs to keep,
`holds`/`skip`), save it as `<hostname -s>.yml` in your profiles directory, and point
`CORPUS_MACHINES` at that directory. Then `task corpus:catalogue:machine` checks it, and
`task corpus:verify` shows what this machine still lacks.

### Checking a slowTest result

See AGENTS.md §Commands. In short: force the run (`test:slow` does), check that the result files
are fresh, read the roll-call per test, and look at the scale each test prints. `slowTest` sets
`-Dmaddi.corpus.required=true`, so a corpus that the `Corpora` locator cannot find fails the test
instead of skipping it, but only for tests that go through that locator.

---

## Environment variables

| variable | default | meaning |
|---|---|---|
| `TEST_OSS_ROOT` | through Task: `test-oss` beside the maddi checkout; `catalogue.py` on its own: `~/git/test-oss` | where the corpora are cloned; also read by the corpus tests (`-Dtest.oss.root`) |
| `CORPUS_CATALOGUE` | `corpus/catalogue` | colon-separated catalogue directories, in precedence order |
| `CORPUS_MACHINES` | none | colon-separated directories holding machine profiles |
| `BUILD_JAVA_HOME` | none | forces the build phase's JDK, overriding the profile |
| `REFACTOR_HOME` | `~/refactorhome` | the engine workspace `register` writes into |
| `MADDI_REPO` | this checkout | the maddi checkout |
| `MADDI_MOD_REPO` | `../maddi-mod` | where `test:slow`, `sweep` and `analyse` run |
| `MADDI_DIST_REPO` | `../maddi-dist` | the CLI launchers and build plugins |
| `MADDI_PLUGIN_VERSION` | maddi-dist's `version=` | the plugin version the plugin routes ask for |
| `JAVA_HOME` | ambient | the JDK maddi runs on (26 or newer) |
| `MADDI_EXPORTS` | set by the Taskfile | the five `--add-exports jdk.compiler/...` maddi's javac front end needs |

`task corpus:init` writes `TEST_OSS_ROOT` and `JAVA_HOME` into `corpus/.env`, which is not
committed. Invoking `corpus/Taskfile.yml` directly (`task -d corpus ...`) does not load `.env`, so
export the variables yourself in that case.

---

## What lives where

```
maddi/corpus/
  README.md                       this file
  Taskfile.yml                    the tasks
  catalogue/<name>.yml            the public entries
  catalogue/baselines/<name>.tsv  recorded primary types per source set
  catalogue/patches/<name>/       source.patches, each with its header
  scripts/catalogue.py            the driver (docstring = reference)
  scripts/vendor-libraries.py     vendoring and the Lombok rule
  scripts/*.py, *.gradle(.kts)    per-corpus helpers that entries name (config scripts, init scripts)
  scripts/test_*.py               the tooling's own tests
  java_kotlin_refactoring_projects.md   older per-project notes, from before the catalogue

$TEST_OSS_ROOT/
  <name>/                         the checkout, at its pin, built
  <name>/inputConfiguration.json  the configuration (plus compile.log etc., per route)
  lib/<project>/                  vendored jars; lib/<project>/_backup/ holds configs before rewriting

$REFACTOR_HOME/
  projects/<name>  -> checkout
  work/<name>/project.yml, build.inputConfiguration.json -> the configuration
```

---

## Troubleshooting

**`check-rev` fails: "checkout is at X, pinned at Y".** Someone pulled, or a test moved the tree.
`task corpus:pull NAME=x` puts it back, unless tracked files are modified. Then look before you
`task corpus:clean NAME=x`.

**`doctor` says `+dirty` or `+UNPATCHED`.** Tracked edits that are not the declared patches, or
declared patches not applied. The first needs a look (`git -C <dir> status`). For the second, run
`pull`.

**"class file older than source", and compilation units dropped.** A source changed after the
build, typically a patch applied to an already-built tree. Rebuild: `task corpus:build NAME=x`.

**A Gradle build runs on the wrong JDK.** `~/.gradle/gradle.properties` may set
`org.gradle.java.home`, which beats `JAVA_HOME`. The build phase passes `-Dorg.gradle.java.home`
for an entry with `build.jdk`. Check with `./gradlew -Dorg.gradle.java.home=<home> --version`
("Daemon JVM"). Running a build by hand without that flag uses the pinned JDK, and a failed compile
can empty a project's classes directory on the way out.

**A build dies in Lombok (`ExceptionInInitializerError`, `EndPosTable`).** That project's Lombok
cannot run on this JDK, and JDK 27 broke every Lombok up to 1.18.46. Give the entry a `build.jdk`
the project's Lombok supports. maddi's own parse is covered separately, by vendoring.

**`catalogue:build` "failed" but the composite went on.** Intended: the build's exit code is
ignored, and `check-provides` decides.

**"no <phase> phase in this entry -- skipped".** Normal: the entry does not declare that phase.

**A corpus test skipped.** Its corpus or configuration was not found. `catalogue:doctor` for the
corpus, and check `TEST_OSS_ROOT` / `-Dtest.oss.root`, especially from a worktree one directory
deeper than usual.

**"configured but the engine cannot load it".** Not registered, or registered for another tree.
`task corpus:config NAME=x` re-registers, or `scripts/catalogue.py register x` on its own.

**The public catalogue vanished from a listing.** A `CORPUS_CATALOGUE` entry with an unexpanded
`$VAR`: `catalogue.py` refuses those by name. Also make sure no stray `._*.yml` files (macOS
metadata, from a `tar` copy) sit in a catalogue directory.

**`catalogue.py needs PyYAML`.** It was run with a bare `python3`. Run it as
`scripts/catalogue.py` (uv) or through `task`.
