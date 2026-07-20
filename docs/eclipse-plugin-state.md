# maddi Eclipse plugin — state of the union

Survey taken 2026-07-18 on branch `ide` (HEAD `520ca3f7`). The plugin itself has been dormant since
`1ec8bc3e`; the branch has advanced 83 commits of analyzer work on top of it.

## 1. History

Seven commits, all on `ide`, merged into `kotlin` via `a1f37782`:

| Commit | What |
|---|---|
| `c40a7b26` | Extract `maddi-ide-client` — IDE-agnostic daemon client shared by IntelliJ + Eclipse |
| `707b10e7` | Eclipse plugin scaffold (Tycho) |
| `57ef3016` | Preferences page + navigable findings view |
| `3a053e4b` | Bundle the daemon distribution inside the plugin |
| `f8d8d94f` | Headless test proving the JDT config mapping runs live |
| `ec10a878` | Computed analysis hints → gutter markers |
| `1ec8bc3e` | Gutter-hint filter (B) + auto-analyze on build (D) |

Since then only `db259ca0` (repo-wide JPMS `module-info` sweep) touched these modules.

## 2. Layout

A self-contained **Maven/Tycho** reactor at `maddi-eclipse/`, deliberately outside the Gradle
multi-project (it is not in `settings.gradle.kts`) — Eclipse plugins are OSGi bundles, built PDE-style.

```
maddi-eclipse/
  pom.xml                                       # Tycho 5.0.3 parent, 0.1.0-SNAPSHOT
  io.codelaser.maddi.eclipse/                   # packaging: eclipse-plugin  (13 files, ~1085 LOC)
    plugin.xml  build.properties  META-INF/MANIFEST.MF
    src/org/e2immu/analyzer/ide/eclipse/*.java
    lib/                                        # generated: maddi-ide-client.jar + 3 jackson jars
    daemon/                                     # generated: bin/ + lib/ of the daemon installDist
  io.codelaser.maddi.eclipse.tests/             # packaging: eclipse-test-plugin
    src/.../tests/MaddiEclipseConfigBuilderTest.java
  io.codelaser.maddi.eclipse.feature/           # packaging: eclipse-feature (what a user installs)
    feature.xml                                 # plugin unpack="true" — the daemon must be on disk
  io.codelaser.maddi.eclipse.repository/        # packaging: eclipse-repository (the p2 update site)
    category.xml
```

- **Target platform**: no `.target` file. The parent pom points a p2 `<repository>` straight at
  `https://download.eclipse.org/releases/2025-09/`; `target-platform-configuration` pins
  `executionEnvironment` to `JavaSE-21`. Bumping that one URL moves the baseline.
- **Installable**: the reactor builds a p2 update site at
  `io.codelaser.maddi.eclipse.repository/target/repository` (and the same zipped). See
  `../maddi-eclipse/README.md`.
- `MANIFEST.MF`: `singleton:=true`, `Eclipse-BundleShape: dir` (so the bundled daemon can be launched
  from disk), lazy activation, `Bundle-ClassPath: ., lib/maddi-ide-client.jar, lib/jackson-*.jar`.

## 3. What it does today

Extension points contributed (`io.codelaser.maddi.eclipse/plugin.xml`):

| Extension point | Contribution |
|---|---|
| `org.eclipse.core.resources.markers` | `contractViolation` (problemmarker + textmarker), `analysisHint` (textmarker) |
| `org.eclipse.ui.editors.annotationTypes` / `markerAnnotationSpecification` | `analysisHint` → gutter + overview-ruler annotation, colour `115,135,225`, layer 4 |
| `org.eclipse.ui.commands` | `io.codelaser.maddi.eclipse.analyze` with a `defaultHandler` |
| `org.eclipse.core.runtime.preferences` / `org.eclipse.ui.preferencePages` | initializer + "maddi" page |
| `org.eclipse.ui.views` | category `maddi`, view `io.codelaser.maddi.eclipse.findingsView` |
| `org.eclipse.ui.menus` | popup *Analyze with maddi*, `visibleWhen` a single `IProject` is selected |

**No builder and no project nature.** Auto-analysis is a programmatically registered `POST_BUILD`
resource-change listener instead.

Key classes, all under `io.codelaser.maddi.eclipse/src/org/e2immu/analyzer/ide/eclipse/`:

- **`MaddiEclipsePlugin`** — `AbstractUIPlugin` activator. Owns the single long-lived
  `MaddiDaemonProcess` for the workspace; registers `MaddiBuildListener` in `start()`, unregisters and
  closes the daemon in `stop()`.
- **`AnalyzeMaddiHandler`** — command handler; adapts the selection (`IProject` / `IJavaProject` /
  `IAdaptable`) and calls `MaddiAnalysis.schedule(javaProject, true)`.
- **`MaddiAnalysis`** — the orchestrator. Runs in an Eclipse `Job` off the UI thread, coalesced by a
  static `AtomicBoolean RUNNING`. Resolves JDK home + daemon install, `ensureStarted`, builds the
  config, sends `analyze`, applies markers inside `ResourcesPlugin.getWorkspace().run(...)`, updates
  `MaddiResults`, optionally reveals the view.
- **`MaddiEclipseConfigBuilder`** — the JDT→daemon mapping, the Eclipse analog of IntelliJ's
  `MaddiConfigBuilder`. Walks `getRawClasspath()`: `CPE_SOURCE` → source root + its output dir;
  `CPE_LIBRARY` → classpath; `CPE_PROJECT` → the required project's output dir; `CPE_CONTAINER` →
  resolved entries, **skipping `JRE_CONTAINER`** (maddi loads jmods from its own JDK 25+);
  `CPE_VARIABLE` → resolved. `entry.isTest()` maps to `test`/`compile` scope. The load-bearing idea:
  *compiler output dirs become classpath entries*, kept hot by JDT's incremental builder.
- **`MaddiMarkers`** — deletes and recreates both marker types over the whole workspace root each run.
  Violations get severity plus a why-chain (`appendCauses`, indented `⇐` lines, depth-capped at 8).
- **`MaddiResults`** — process-wide singleton holding the latest `Result`, with listeners.
- **`MaddiFindingsView`** — `ViewPart` with a 4-column `TableViewer`; double-click opens the editor at
  the matching marker.
- **`MaddiPreferences` / `MaddiPreferenceInitializer` / `MaddiPreferencePage`** — keys `maddi.jdkHome`,
  `maddi.daemonInstall`, `maddi.daemonXmxMb` (4096), `maddi.hintFilter`, `maddi.autoAnalyzeOnBuild`
  (default false). Path accessors resolve preference → system property → env var.
- **`HintFilter`** — `HIDE_CONTEXT_DEFAULTS` (default) / `ALL` / `POSITIVE_ONLY` / `NEGATIVE_ONLY` /
  `NONE`, filtering on `Annotation.contextDefault()` and `polarity()`.
- **`MaddiBuildListener`** — on `POST_BUILD`, if `autoAnalyzeOnBuild()`, schedules a quiet analysis for
  each affected open Java project.

## 4. Daemon integration

The plugin owns **no protocol code** — it reuses `maddi-ide-client` verbatim, as IntelliJ does.

- **Protocol**: loopback TCP carrying NDJSON (one UTF-8 JSON object per line), discriminated on
  `"type"`. `maddi-ide-daemon/.../DaemonProtocol.java`: `handshake`/`handshakeAck`, `analyzeProject`,
  `status`, `result`, `error`, `ping`/`pong`, `cancel`, `shutdown`/`bye`; `PROTOCOL_VERSION = 1`. Only
  records of primitives cross the boundary, so no maddi types leak into the IDE JVM.
- **Client**: `DaemonClient` (socket + Jackson, 600 s SO timeout, streams `status` frames until a
  terminal `result`/`error`), `MaddiDaemonProcess` (warm, `synchronized`, one request at a time,
  relaunches if dead, `-Xmx<n>m -XX:+UseG1GC`).
- **Launch**: `DaemonLauncher` runs the Gradle `installDist` start script `bin/maddi-ide-daemon[.bat]`
  with `--port 0`, `JAVA_HOME` set to the configured JDK, parses `DAEMON_PORT=` from stdout (60 s
  timeout), drains the rest in the background. It calls `setExecutable(true, false)` — the comment
  names the Eclipse plugin's copy step as the reason the executable bit is lost.
- **Bundled distribution**: `maven-resources-plugin` copies
  `maddi-ide-daemon/build/install/maddi-ide-daemon` into the bundle's `daemon/` at `process-resources`.
  `MaddiDaemonInstall.resolve()` prefers the configured path, else `FileLocator.find(bundle, "daemon")`.
- **Config**: `AnalysisModel.AnalyzeConfig(workingDirectory, sdkHome, sourceEncoding, jmods, sources,
  classpath, restrictToPackages, parallel)`. The plugin always passes `UTF-8`, empty
  `restrictToPackages`, `parallel = true`, and a hardcoded `DEFAULT_JMODS` list of 9 modules.

## 5. Tests

One headless test: `io.codelaser.maddi.eclipse.tests/.../MaddiEclipseConfigBuilderTest.java`.

- `eclipse-test-plugin`, run by `tycho-surefire-plugin` with `<useUIHarness>false</useUIHarness>` — a
  real Equinox runtime with `core.resources` + `jdt.core`, no workbench.
- `sourceRootAndOutputDirBecomeConfig` creates a real `MaddiSample` project with the Java nature,
  `src/` + `bin/`, and asserts sdkHome, encoding, `java.base` in jmods, `parallel`, the source root
  being non-test, and `bin` landing as a `compile`-scope classpath entry.
- `JDK_HOME` is the literal `"/opt/jdk-25"`, never dereferenced — the test passes without that JDK.
- No CI config for this reactor; run manually via `mvn verify` in `maddi-eclipse/`.

## 6. Build

**Maven + Tycho 5.0.3**, `mvn verify` from `maddi-eclipse/`. Two manual prerequisites, both documented
in the pom comments and currently satisfied:

1. `./gradlew :maddi-ide-client:publishToMavenLocal` — the pom pulls `io.codelaser:maddi-ide-client`
   from the local repo. The version is hardcoded as `<maddi.client.version>` and must be hand-synced
   with the root `gradle.properties` `version=`.
2. `./gradlew :maddi-ide-daemon:installDist`.

The build passes, tests included, and ends with a p2 site under
`io.codelaser.maddi.eclipse.repository/target/`. The one fragility is that resolution needs network access
to the 2025-09 p2 release train on a cold `~/.m2/repository/.cache/tycho`.

Two install-time facts worth not rediscovering: the bundle **must** be installed unpacked (it carries the
daemon distribution, whose launch script has to be a real file), which is why the manifest says
`Eclipse-BundleShape: dir` *and* the feature says `unpack="true"`; and **p2 does not preserve the executable
bit** on that script, so `DaemonLauncher.setExecutable` is what makes an installed plugin able to start its
analyser at all.

## 7. Gaps

No `TODO`/`FIXME` markers anywhere — the gaps are structural, not annotated. These are the Eclipse-side
ones; queued work across both front-ends and the daemon — partial re-analysis, progress — is in
`ide-todo.md`.

1. **No builder, no nature** — analysis is a listener, so there is no delta scoping. Every trigger is a
   whole-project run that deletes and rebuilds *all* markers at `DEPTH_INFINITE` from the workspace root.
2. **Coalesced triggers are dropped silently.** `MaddiAnalysis.RUNNING` is a bare `AtomicBoolean`; a
   build finishing mid-run is never computed. Needs a pending/re-run flag.
3. **Progress and cancel are dead.** `daemon.analyze(..., status -> { })` discards the
   phase/typesDone/typesTotal frames; the `IProgressMonitor` is unused, and `cancel` is defined in the
   protocol but never sent.
4. **Parity vs IntelliJ.** Inline hints now exist on both sides (code minings in Eclipse). Still missing
   in Eclipse: quick fixes, and any hover richer than the marker message.

Minor: `MaddiPreferenceInitializer`'s javadoc says "only the daemon heap has one" but it seeds three
defaults (stale comment).

*All display surfaces have since been verified live, in a running workbench and a running IDEA (2026-07-18):
inline/above hints, gutter markers, the findings view, and the guard-violation underline.*

*Resolved since this survey:*
- *the test bundle ran on JUnit 4 while the rest of the repo is on Jupiter — migrated to JUnit 5;*
- *`--warn-near-misses` was unreachable from either IDE (the daemon's port of `RunAnalyzer` had dropped
  the `setWarnNearMisses` line) — `warnNearMisses` now runs from a setting in both front-ends through
  `AnalyzeConfig` to the analyzer;*
- *the plugin was not installable — there is now a feature and a p2 update site
  (`io.codelaser.maddi.eclipse.repository/target/repository`), verified by installing it with the p2
  director into a clean Eclipse and starting the daemon from the result; see `../maddi-eclipse/README.md`;*
- *inline hints were thought to need an internal-API spike — they don't. `AbstractTextEditor`
  `.installCodeMiningProviders()` reads the `org.eclipse.ui.workbench.texteditor.codeMiningProviders`
  registry, and JDT registers its own minings through that same public point, so `MaddiCodeMiningProvider`
  is an ordinary EP client. Caveat: JDT gates every provider in its editor, ours included, on
  Java > Editor > Code Minings > Enable code minings.*
