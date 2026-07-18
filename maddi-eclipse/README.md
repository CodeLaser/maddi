# maddi for Eclipse

Shows maddi's analysis in the Java editor: computed annotations as hints, and violations of contracts you
have written (`@Container`, `@NotModified`, `@Immutable`, `@Independent`) as problems carrying the reasoning
that led to them.

Requires **Eclipse 2025-09 or later** and a **JDK 25+** for the analysis to run on. That JDK is not the one
Eclipse itself runs on: the analyser is bundled with the plugin and runs out of process, so Eclipse can stay
on whatever JVM it likes.

## Installing

Build the update site (see below), then in Eclipse:

**Help → Install New Software… → Add… → Local…**, select
`io.codelaser.maddi.eclipse.repository/target/repository`, tick **maddi**, and follow the wizard.

`target/io.codelaser.maddi.eclipse.repository-*.zip` is the same site archived — use *Add… → Archive…* for
an offline install, or publish it as a release asset and point *Add…* at its URL.

After installing, set the analysis JDK in **Preferences → maddi**. Nothing will run until you do.

## Using it

Right-click a Java project → **Analyze with maddi**. Results appear as:

- **hints** — computed annotations, on their own line above each declaration (parameters inline). Needs
  *Preferences → Java → Editor → Code Minings → Enable code minings*, which gates every provider in JDT's
  editor, this one included.
- **problems** — contract violations as errors, near misses as info, both with the why-chain.
- **the maddi view** — Window → Show View → Other… → maddi, a navigable list of findings.

Preferences → maddi also has: which annotations to show, where declaration hints go, whether to re-analyse
after each build, and whether to warn about near misses (off by default; noisy on uncurated code).

## Building

A Maven/Tycho reactor, deliberately separate from the Gradle multi-project: Eclipse plugins are OSGi bundles,
built PDE-style. Two prerequisites come from the Gradle side, so build them first:

```bash
./gradlew :platform:publishToMavenLocal :maddi-ide-client:publishToMavenLocal :maddi-ide-daemon:installDist
cd maddi-eclipse && mvn clean verify
```

- `maddi-ide-client` is resolved from the local Maven repo and embedded on the bundle classpath. Its version
  is pinned in the parent pom as `maddi.client.version` and must be kept in step with the root
  `gradle.properties` by hand.
- the daemon `installDist` tree is copied into the bundle at `process-resources`.

Modules: the **bundle**, a headless **test** bundle (a real Equinox runtime with JDT, no workbench), the
**feature** (what a user installs), and the **repository** (the p2 site).

Resolution needs network access to the 2025-09 p2 release train the first time; after that Tycho caches it
under `~/.m2/repository/.cache/tycho`.

## Two things worth knowing

**The bundle must be installed unpacked.** It carries the daemon as a distribution with a launch script that
has to be a real file on disk. That is why the manifest says `Eclipse-BundleShape: dir` and the feature says
`unpack="true"`; drop either and the plugin installs as a jar and cannot start its analyser.

**p2 does not preserve the executable bit** on that launch script, so a freshly installed plugin has a
non-executable `daemon/bin/maddi-ide-daemon`. `DaemonLauncher` restores it before launching, which is the
only reason installs work — do not remove that call.
