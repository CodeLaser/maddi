# maddi for VS Code

**Status: working.** Analysis runs end to end, findings appear as diagnostics and computed annotations as
inline hints — all verified in a running VS Code against a real project. Packaging is not done. See
*What is missing*.

Third front-end after IntelliJ and Eclipse. Same architecture — the analyser runs out of process and is
spoken to over a socket in plain JSON — with one thing that is genuinely different here.

## The problem this front-end had to solve

maddi's openjdk parser needs the project's **`.class` files** on the classpath. IntelliJ and Eclipse both
have a compiler and a project model to ask, so the other two plugins just read the compiler output
directories. VS Code has neither: it is an editor, and Java support is an extension.

The answer is that the extension it delegates to — `redhat.java` — runs **jdt.ls**, which is Eclipse JDT.
That gives us both halves at once: a real project model *and* a real compiler writing real `.class` files.
So this extension asks jdt.ls rather than reimplementing anything:

| What | How |
|---|---|
| projects | `java.project.getAll` (minus jdt.ls's synthetic catch-all project) |
| classpath + output dirs | `java.project.getClasspaths(uri, {scope})` — the runtime classpath, which includes the compiler output folders |
| source roots | `java.project.listSourcePaths` |
| fresh `.class` files | `java.workspace.compile(false)` — resolves only when the build has finished |

All of it goes through `java.execute.workspaceCommand`, the bridge `redhat.java` exposes onto jdt.ls's
delegate commands.

## Two things that fail silently, and are handled

**LightWeight mode.** `java.server.launchMode` defaults to *Hybrid*, so jdt.ls starts in a mode where
`java.execute.workspaceCommand` does not fail — it logs a warning and returns `undefined`. Query it then and
you get an empty project model and an analysis of nothing, with no error to explain it. The extension waits
for `Standard` (`onDidServerModeChange`, then `serverReady()`).

**Stale `.class` files.** jdt.ls autobuilds by default, but "autobuild is on" and "the build has finished"
are different claims. `java.workspace.compile` is awaited as a genuine barrier before the model is read.

## Requirements

- `redhat.java` (declared as an extension dependency)
- a **JDK 25+** in `maddi.jdkHome` — maddi runs on this and reads `java.base` from it. It is not the JDK the
  project targets, and not the one VS Code runs on.

## Building

```bash
npm install
npm run compile   # or: npm test
```

Debug with **Run Extension** in VS Code (F5), which opens an Extension Development Host.

The daemon distribution has to be reachable. Either build it and point `maddi.daemonInstall` at it:

```bash
./gradlew :maddi-ide-daemon:installDist   # -> maddi-ide-daemon/build/install/maddi-ide-daemon
```

or copy that tree to `maddi-vscode/daemon/`, which is where a packaged extension looks. Note the launcher
loses its executable bit inside a `.vsix` (a zip, exactly as a p2 install and a Maven copy do); the launcher
restores it before spawning.

## Tests

`npm test` — Node's built-in runner over the compiled output. The modules holding the logic worth testing
(`analysisModel`, `configBuilder`, `daemonClient`'s framing) deliberately import no `vscode`, since that
module only exists inside the extension host and anything touching it cannot be unit-tested.

The config-builder tests drive the mapping with canned jdt.ls responses in the exact shapes jdt.ls 1.59
returns. That is the part worth pinning: a wrong mapping produces an analysis of nothing rather than an
error.

## What is missing

- **packaging** — no `.vsix` is produced by this module's own scripts (the repo-root `Taskfile.yml` has a
  `vscode:package` task that does it).
- **hints are inline only, unlike the other two front-ends** — and this is settled, not a gap to close.
  IntelliJ and Eclipse put a declaration's annotations on a line of their own above it. VS Code has no
  equivalent: an `InlayHint` is positioned at a `Position` and renders within the line, and nothing in the
  API adds one. The only route is injecting `display: block` CSS through a decoration's `textDecoration`,
  which is unsupported and version-fragile; inline was reviewed in situ and judged fine, so do not add the
  hack.
- **no gutter or findings view** — the Problems panel covers the second, and VS Code has no real analogue of
  the first.
