# The IDE daemon, 2026-08-25: five commits, and what they left open

A session that began with "the NPE I thought was fixed is back" and ended three defects deeper. This note is
the state at `be3340473`; the reasoning for each fix is in its commit message and in
[`ide-todo.md`](ide-todo.md) §4b and §5.D.

## What landed

| commit | what |
|---|---|
| `1d583c80d` | the daemon reports the source state it was built from |
| `528ce0bd5` | the tool window shows what a run is doing; the daemon loop becomes fast |
| `fef0ba81e` | finishing an abandoned method means every field the full path sets |
| `9a114f514` | `-source` is not `--release`; the shared JDK is not the first source set's |
| `be3340473` | an eventual verdict is legible, and no longer looks like a proven one |

Whole reactor green at each: 3248 tests, 28 modules. **`slowTest` was NOT run** — that half of the merge gate
is outstanding, and `TestEventualRatchet` (`@Tag("slow")`) is the one most exposed to `9a114f514`.

## ⛔ The thing that cost the most: three of the four "defects" were one stale daemon

The IDE runs the daemon its **plugin bundles**, and nothing observable distinguished two builds: plugin `0.1.0`,
maddi `0.9.1`, daemon `0.1.0-dev`, whatever the source state. Three worktrees produced three different bundles
over two days, and already-fixed defects presented as regressions **twice**. `1d583c80d` is the answer — the
daemon prints its source state and returns it on handshake, and every front-end logs it — and the tool window
now shows it permanently. ⭐ **Before diagnosing any analyzer behaviour reported from the IDE, read the build
stamp**: `grep 'maddi daemon' ~/Library/Logs/JetBrains/*/maddi-daemon.log | tail -1`, and compare with
`git rev-parse --short=9 HEAD`.

The fast loop that makes this stop recurring: Settings → maddi → *Daemon install override* →
`maddi-ide-daemon/build/install/maddi-ide-daemon`, then `installDist` → *Restart daemon* → *Analyze*.

## Open, with measurements

**1. A modular source set cannot read a class-path jar.** `maddi-cst-api` has a `module-info.java`;
`maddi-annotation` arriving as `maddi-annotation-0.9.1.jar` on the class path is in the unnamed module, so
`@Fluent` does not resolve and `MethodInfo.java` is dropped **whole** — which reads as "the analyser said
nothing about this type", not as a parse failure. Giving it a source set instead, same corpus:

| | annotation as a jar | as a source set |
|---|---|---|
| dropped units | 314 | **153** |
| parse errors | 52 | **3** |
| element annotations | 24,049 | **30,568** |

Same family as #30/#32: none of the four config producers puts a modular set's dependencies on the module path.
Half the remaining drops on that corpus are this.

**2. Test source sets sink production eventual verdicts.** At whole-project scope the contraction retracts 272
optimistic verdicts, `MethodInfo` and the whole `Info` family among them. `EC_RETRACT_DEBUG=1` names the
breakers (`EventualClusterContraction` prints `ECRETRACT <type> <- broken: [...]` on stdout, so it reaches the
daemon log; `daemon-drive.py` passes the environment through): `ParameterizedType` 72, `Runtime` 42,
`Qualification` 32, **`io.codelaser.maddi.java.openjdk.CommonTest` 30 — a test class**, `Statement` 28,
`Info` 17; 80 of the 272 are pure cascade. ⭐ `TestEventualRatchet` disagrees with the IDE because
`dogfood/cst-impl/build/inputConfiguration.json` has **three source sets, all `/main`, no test set at all** —
the gap is `main`-only versus `main`+`test`, not just 5 modules versus 64. The `Info` family is a casualty of
the expression/statement families, not the origin.

**3. The hint archive addresses methods by position and records no JDK.** `dogfood`'s `jdk/*.json` say
`"transferTo(10)"`; a method list that differs by one release makes the index point elsewhere, and
`LoadAnalysisResults` silently skips. `CodecImpl:385`'s own message asks for the fix — "name+descriptor needed
to disambiguate" — and the archive should carry the JDK it was generated from, so a mismatch is reported
rather than absorbed. Floor on a uniform-release configuration is 66 skips; a mixed one now costs 83.

**4. `postprocess.py` transform 2 is redundant.** The pipeline harness has rewritten every `-source N` /
`--release N` to 25 since 2026-08-21 to dodge the ct.sym clash `9a114f514` fixes. With that commit the mixed
config measures what the normalised one does (314 drops / 300 second-definitions), so the transform can go —
and until it does, that corpus cannot see a mixed-release defect. ⚠ It is also why an earlier "control" in this
session was not one.

**5. Unbuilt here.** The Eclipse one-liner in `MaddiAnalysis` (Tycho, resolves `maddi-ide-client` from
mavenLocal per `maddi-eclipse/README.md`) and the VS Code line in `extension.ts`.

## How to measure any of it

`daemon-drive.py <installDir> <per-module json>` in
`ws/python/jfocus-refactor-server/work/codelaser/pipeline/` — ~3 minutes per run, and a one-line change in the
product gives a clean A/B. Configs used here (scratchpad, not in the repo): `maddi-permodule.json` as shipped
(uniform release 25), a mixed variant with maddi's real levels restored (54 at 25, two at 17, two at 21), and
an "annotated" variant adding `maddi-annotation` as a source set. ⚠ `EVENTUALCLUSTER=0`, `MODREACH=0`,
`EVENTUALDEFER=0` and `EC_RETRACT_DEBUG` are environment opt-outs on this path; a developer with one exported
measures a different engine.
