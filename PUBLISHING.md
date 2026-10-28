> **Three repositories.** Since 2026-09-30 maddi is split by tier
> ([plan](docs/roadmap/split-maddi-into-three-repositories.md)): this repository holds the **base** (CST, front
> ends, inspection, call graph, the run drivers, the `AnalysisEngine` interface, and `corpus/`, the corpus tooling);
> [**maddi-mod**](https://github.com/CodeLaser/maddi-mod) holds the modification analysis (`maddi-modification-*`,
> `maddi-aapi-parser`, `maddi-run-analysis`, the analysis tests); and
> [**maddi-dist**](https://github.com/CodeLaser/maddi-dist) holds what users install (daemon and IDE plugins, the
> Gradle and Maven plugins, the `maddi` / `maddi-kotlin` CLI distributions, `dogfood/`). The plugins, the IDE artefacts and the CLI zips (`release-cli.sh`) are published from maddi-dist; modules named below that
> are not in this checkout live in those siblings, checked out next to it (`../maddi-mod`, `../maddi-dist`).

Publishing strategy
===================

> **Status: fully active since 0.9.1 (2026-08-19).** Every leg of Package 1 is published: the
> annotations (`io.codelaser:maddi-annotation` + `io.codelaser:maddi-support`) and the Maven plugin
> (`io.codelaser:maddi-mvnplugin`) on Maven Central, the Gradle plugin on the Plugin Portal
> (`io.codelaser.maddi.analyzer`), and the two CLI zips on GitHub Releases. This document records
> the agreed strategy and the state of each leg.
>
> Two things learned publishing 0.9.0, worth knowing before the next release:
>
> * **A published artifact must declare no dependency that is not itself on Central.**
>   `maddi-support` deliberately does *not* apply `java-library-conventions`: that adds
>   `api(platform(project(":platform")))` plus jetbrains-annotations and slf4j, all of which leak into
>   the POM and Gradle module metadata. The internal `io.codelaser:platform` BOM is not on Central, so
>   such a POM is unresolvable for a consumer.
>
>   Until 0.9.0 this was checked as the stricter, simpler "the POM must have no
>   `<dependencies>`/`<dependencyManagement>` and all four module-metadata variants must be empty",
>   which was true of `maddi-support` and easy to verify. **The `maddi-annotation` split retires that
>   phrasing**: `maddi-support` now legitimately declares one dependency, on `maddi-annotation`, which
>   ships to Central in the same release train. Checked literally, the old rule fails on a correct
>   artifact — so check the resolvability, which is what the rule was always for: every entry in
>   `<dependencies>` must be an artifact a consumer can actually resolve from Central.
> * **`Attempt N of 101` in the JReleaser log is a polling loop, not a retry after failure.** The
>   deployment is already uploaded and publishing; do not re-run the deploy. Portal→`repo1` sync adds
>   another 10-30 minutes, and `maven-metadata.xml` catches up later still. Verify with
>   `repo1.maven.org`, never `search.maven.org` — the latter reported `numFound: 0` for an artifact
>   that had been published for a year.

This document describes *what* maddi publishes, *where*, and *why*. The concrete Maven Central
credentials and commands are in the appendix at the bottom.

The guiding principle (agreed): the individual analyzer modules are of no use to an outside consumer,
so **we do not publish them as a fine-grained library**. We publish only the things people actually
consume — the annotations, the build plugins, the command-line tools (Package 1) and the IDE plugins
(Package 2) — and we keep the Kotlin front-end on a separate delivery channel.


Why Kotlin is separate
----------------------

`maddi-kotlin-k2` (and everything above it: `maddi-inspection-kotlin`, `maddi-inspection-mixed`,
`maddi-run-kotlin`, `maddi-cst-print-kotlin`) depends on JetBrains **K2 "for-ide" analysis-API
artifacts that are not on Maven Central** (they come from the JetBrains repositories declared in
`settings.gradle.kts`), and the Kotlin modules target a newer JVM bytecode level than the Java stack.

Therefore the Kotlin side **cannot be published to Maven Central** — a consumer could not resolve its
transitive dependencies. Kotlin support is delivered only as a self-contained CLI distribution (below),
where those jars are simply files in `lib-k2/` and no Maven resolution is involved.


What we publish (Package 1)
---------------------------

=== 1. Annotations — Maven Central

`maddi-support` (`io.codelaser:maddi-support`) contains the user-facing annotations
(`io.codelaser.maddi.annotation.*` — `@Immutable`, `@Container`, `@Independent`, …). This is the one artifact
a user's *own code* compiles against, so it is a small, stable library on Maven Central. It already has
the `maven-publish` + jreleaser configuration; publish it per the appendix below.

=== 2. Build plugins — self-contained (shaded)

Because we do **not** publish the analyzer modules, the plugins cannot declare Maven dependencies on
them; instead each plugin **shades the (Kotlin-free) Java analyzer into its own jar**, so it is
self-contained. This is viable precisely because both plugins use only the openjdk (Java) analyzer —
no K2 — and they run it on a classpath (a forked worker for Gradle), where the stripped module
descriptors do not matter.

* *Gradle plugin* (`maddi-gradleplugin`) → **Gradle Plugin Portal**, id `io.codelaser.maddi.analyzer`.
  **Shading + publication wiring DONE** (`com.gradleup.shadow` + `maven-publish`): a dedicated `shade`
  configuration lists the analyzer modules; `implementation` extends it; `shadowJar` bundles only
  `shade`, so `gradleApi()` and `kotlin-stdlib` (Gradle-provided) are excluded and the analyzer +
  jackson/logback/asm/congocc are bundled (~11 MB, verified). Class names are **not relocated** — the
  forked worker references `RunAnalyzer` by its real name, and it runs in an isolated process so
  third-party clashes with the consuming build don't arise. The thin jar is pushed to the `-plain`
  classifier; the shadow jar takes the main name. The `pluginMaven` publication ships the shadow jar
  and a **dependency-free POM** (Gradle Module Metadata disabled, `<dependencies>`/`<dependencyManagement>`
  stripped — nothing to resolve, everything is bundled). Proven self-contained by
  `TestAnalyzerPluginShadedJarIsolation`: it publishes to a local repo and resolves+runs the plugin from
  there *with no analyzer module on any classpath*. **`com.gradle.plugin-publish` 2.1.1 applied
  2026-08-17** with `website`/`vcsUrl`/`tags`, a sources and a javadoc jar (the Portal requires both,
  and `setArtifacts` replaces the artifact list, so they are named there explicitly), and a
  `description` on the plugin declaration — it had been set on the enclosing scope, i.e. on the
  *project*, so the declaration carried none. *Remaining:* run `publishPlugins` — the rename it was waiting for landed 2026-08-17.

  ⚠️ **The Portal is not instant, and the id cannot be corrected cheaply.** Publishing docs, verified
  2026-08-17: a new plugin "will go through a manual review process" by a Gradle engineer before it
  becomes visible, and "any change of Maven group or plugin ID will cause the manual approval process
  to be triggered again". Nothing exists under `io.codelaser` or `org.e2immu` on the Portal today, so
  this is a first publish and `publishPlugins` succeeding does **not** mean the plugin is installable
  that evening — say so in the release notes rather than promising an install line that 404s.

  The same page requires that "the plugin ID and group ID share the same top-level namespace". The
  group is `io.codelaser`, so the pre-rename id `org.e2immu.analyzer-plugin` **would have been refused
  at review**: publishing the plugin before the rename was never actually available. Since the cutover
  (2026-08-17) the id is `io.codelaser.maddi.analyzer`, which satisfies it.
* *Maven plugin* (`maddi-mvnplugin`) → **Maven Central**. **Descriptor + shading DONE.** A hand-maintained
  `src/main/resources/META-INF/maven/plugin.xml` (auto-generation stays blocked on the Gradle-9-incompatible
  `maven-plugin-development` tool) describes all 5 goals (`run`, `write-input-configuration`, `statistics`,
  `write-analysis-hints`, `compile-analysis-hints`), `goalPrefix=maddi`; `processResources` keeps its
  `<version>` in sync with `gradle.properties`. Shadow bundles the (unpublished) analyzer modules +
  jackson/logback/asm/congocc + the aapi-archive analysis-result resources (~12 MB, verified). Unlike the
  Gradle plugin, three things are **left to the Maven runtime, not bundled**: the Maven API, the Aether
  resolver (`maven-resolver-*` → `compileOnly`; its `org.eclipse.aether.*` objects come from Maven core's
  injected `ProjectDependenciesResolver`, so a second bundled copy would `LinkageError`), and `slf4j-api`
  (excluded from the jar; Maven core exports it + its binding). The publication ships the shadow jar with a
  `packaging=maven-plugin`, dependency-free POM. *Remaining:* test against a real `mvn` invocation — still
  not done, but **no longer blocked: Maven 3.9.16 is installed on the Mac as of 2026-08-17**, so the
  "no Maven on this laptop" excuse is spent. Do it before publishing a plugin nobody has ever invoked.
  The host Maven JVM needs the javac `--add-exports` (via `.mvn/jvm.config` or `MAVEN_OPTS`) for the
  openjdk-based goals.

=== 3. Command-line tools — GitHub Releases (not Maven)

The runners apply the Gradle `application` plugin, so `distZip` / `installDist` produce self-contained
bundles (launcher + every runtime jar in `lib/`, with the required javac `--add-exports` baked into the
launcher; the Kotlin zip also carries `lib-k2/`, below). Publish these as **GitHub Release** assets:

* `maddi` — the openjdk (Java) runner, from `maddi-cli:distZip` → `maddi-<version>.zip`, launcher
  `bin/maddi`.
* `maddi-kotlin` — the mixed Java+Kotlin runner, from `maddi-cli-kotlin:distZip` →
  `maddi-kotlin-<version>.zip`, launcher `bin/maddi-kotlin`. **This is how Kotlin support ships**: the K2
  "for-ide" jars ride along in `lib-k2/`, beside `lib/` and never on the launcher's CLASSPATH: the K2 realm
  loads them in a classloader of its own (G46). The 7 `*-for-ide` jars + `kotlin-compiler` were verified bundled
  when they still sat in `lib/`; they moved to `lib-k2/` on 2026-09-21 (e70270088).
  Since 2026-09-21 it takes the **same option surface** as `maddi` — literally
  `openjdkmain.Main.createOptions()`, asserted by `TestOneEntryPoint` — and routes on whether the project
  holds a `.kt` file, so it is a strict superset of `maddi` rather than a second tool. The two zips stay
  separate because the Kotlin one is 85 MB against 11 MB, which is a bad default for a Java-only user.

**DONE**: both runners set `applicationName` so the launcher/archive names are `maddi` / `maddi-kotlin`
(not the module names). The build+upload is wired in `release-cli.sh <tag>` (builds both `distZip`s and
`gh release create`/`upload`s the two zips). Both launchers are smoke-tested self-contained (`maddi
--help` and `maddi-kotlin --help` both exit 0 and print the full option surface). *Remaining:* run `release-cli.sh` for a tag —
needs an authenticated `gh` and is an outward-facing publish, so run it deliberately.

Not published: none of the fine-grained analyzer modules (`maddi-cst-*`, `maddi-inspection-*`,
`maddi-java-*`, `maddi-modification-*`, `maddi-aapi-*`, `maddi-run-config`, …). No BOM.


The IDE plugins (Package 2)
---------------------------

> **Status: nothing published; ready to upload from 0.9.6 (prepared 2026-10-28).** Two plugins ship in 0.9.6,
> IntelliJ (JetBrains Marketplace) and Eclipse (an update site on GitHub Pages, listed on the Eclipse
> Marketplace). VS Code waits for a later release. Everything that does not need a CodeLaser account is done;
> what remains is listed under *What only an account holder can do* below, in order.

Both plugins bundle the same daemon distribution, so they share the release version, the licence files and
`THIRD-PARTY.txt` (maddi-dist root; `:maddi-ide-daemon:checkThirdPartyNotices` fails the build when a bundled
library is missing from it). The user-facing changes per release are in maddi-dist's `docs/ide-changelog.md`; the
IntelliJ change notes are built from it.

⚠ **A plugin release needs the CLI release of the same version.** The plugin does not bundle the Kotlin front
end; on a project with `.kt` sources it offers to download `maddi-kotlin-<version>.zip` from the `v<version>`
GitHub release (`KotlinFrontEndInstaller.releaseZip`), checks it against the `<zip>.sha256` beside it, and keeps
only its `lib-k2/`, matched to the bundled daemon's version. `release-cli.sh` publishes the zip and its checksum,
and refuses a tag other than `v<version>`; v0.9.1's zip predates `lib-k2/` and the checksums, and is refused.

What the IntelliJ plugin ships is unusual enough to say plainly, because both the listing text and the
reviewer need it: the zip **bundles the whole maddi daemon distribution** at `<plugin>/daemon` (42 jars)
and **launches it as a separate JVM process**, because the IDE runs on JBR 21 and maddi needs a JDK 25+.
Nothing leaves the machine, which is what the form's data-collection question turns on: the only socket
in `maddi-ide-client` is `new Socket(InetAddress.getLoopbackAddress(), port)`, and the only
`openConnection()` in the daemon is a `jar:` URL reading a bundled resource (checked 2026-08-26). So no
telemetry disclosure and no privacy policy are needed — but re-check before answering, because the
answer is a statement to JetBrains, not a guess.

=== Identity — the parts that cannot be changed later

The Marketplace's own words about the plugin id: *"Make sure to pick a stable ID, as the value cannot be
changed later after public release."* The same is effectively true of the version, since it rejects a
version it has already seen. So these were settled before any upload:

* **id `io.codelaser.maddi`** — renamed 2026-08-26 from `io.codelaser.maddi.intellij`, which
  `verifyPluginStructure` refuses outright: *"The plugin ID 'io.codelaser.maddi.intellij' should not
  include the word 'intellij'."* That check comes from the Marketplace's own `plugin-structure` library,
  so it is what the upload would have said — after the id was already public.
  ⛔ The id also appears in `MaddiAnalysisService.PLUGIN_ID`, which is how the plugin finds
  `getPluginPath()/daemon`. A drifted constant yields a null descriptor, i.e. "no bundled daemon", which
  presents as a packaging fault rather than a typo. `PluginIdentityTest` now fails if the two disagree.
* **name `maddi`** — a Marketplace search for it returned nothing on 2026-08-26, and it satisfies the
  naming rules (unique, ≤ 30 characters, no "Plugin"/"IntelliJ"/JetBrains product name).
* **version = the project version** — `maddi-intellij` said `0.1.0` and `maddi-ide-client` said `0.8.2`
  while the project was at `0.9.1`, so the shipped zip carried a stale client jar and a version number
  that meant nothing. Both now inherit `gradle.properties`; `maddi-eclipse`'s pom pins the client
  coordinate and moves with it.

=== Before the first upload

* **Logo — DONE.** `META-INF/pluginIcon.svg` + `pluginIcon_dark.svg`, the CodeLaser icon from the 2022
  brand set, squared to the required 40x40 SVG. Required at upload, and it must not be the IntelliJ
  template's default.
* **Description — DONE (2026-10-28).** Its first sentence, the preview card, is plain: *"Immutability,
  modification and nullability analysis for Java and Kotlin, shown in the editor."* It states the JDK 25+
  requirement (picked from the IDE's JDKs), the separate daemon process, that nothing leaves the machine, and the
  Kotlin front-end download.
* **`<change-notes>` — DONE (2026-10-28).** `patchPluginXml` renders the `## <version>` section of
  maddi-dist's `docs/ide-changelog.md` (plain bullets), and fails the build when the section is missing. No
  `org.jetbrains.changelog`: one small function in `maddi-intellij/build.gradle.kts` does it.
* **Vendor — OPEN.** `<vendor>` carries no `email`; the guidelines require the vendor website and email to be
  "provided, valid, and functional". Which address is a CodeLaser decision. A CodeLaser vendor profile must exist
  and the Developer Agreement be accepted before the form will take anything.
* **License — DONE (2026-10-28).** Apache-2.0 since 0.9.6, plugins included; the form needs the source link,
  `https://github.com/CodeLaser/maddi-dist`. The zip now carries `LICENSE`, `NOTICE` and `THIRD-PARTY.txt` at
  the plugin root, beside `lib/` and `daemon/`; until 0.9.6 it shipped no licence text at all.
* **Tags** on the upload form — they are the search filters, so they decide discoverability. Proposed: *Code
  analysis*, *Java*, *Kotlin*, *Inspection*, *Code tools*.
* **Screenshots** (recommended): the Java demo (`maddi-java-demo`) with hints above declarations, a guard
  finding's tooltip, and the tool window mid-run.

=== The Plugin Verifier — wired and run (2026-08-26, rerun 2026-10-28)

**Rerun 2026-10-28 on 0.9.6: `Compatible` against IU-253.33813.55, IU-261.27258.48, IU-262.10968.63 and
IU-263.7307.44 (2026.3).** The same one internal and two experimental usages as below, and one deprecated
(`ReadAction.compute`, from 2026.1). 2026.3 had deprecated a second call, `FileEditorManager.openFile(file, focus)`
in the "Open daemon log" action; it now uses `OpenFileDescriptor.navigate`. The August run follows.

The Marketplace runs the IntelliJ Plugin Verifier on upload, so `pluginVerification { ides { recommended() } }`
runs it here first. `recommended()` resolves the same set `printProductsReleases` prints — with an
open-ended `untilBuild` that is every release from `sinceBuild` up: **IU-253.33813.55, IU-261.27258.48,
IU-262.10315.19**, ~2.8 GB of downloads on the first run, ~1 minute per IDE afterwards.

**Result: `Compatible` against all three — no binary-compatibility problems at all.** What is left is
API-status usage, identical on every IDE:

[cols="1,1,3"]
|===
| Category | N | What

| internal | 1 | `DeclarativeInlayHintsPassFactory.Companion.resetModificationStamp()`
| experimental | 2 | `AboveLineIndentedPosition`, the class and its constructor
| deprecated | 0-1 | `ReadAction.compute(ThrowableComputable)`, deprecated from 2026.1
|===

Two findings were fixed rather than accepted, and both are worth knowing:

* ⛔ **Every plugin-descriptor lookup became internal in 2026.2.** `PluginManagerCore.getPlugin(PluginId)`
  was the original call; `PluginManager.getPluginByClass(Class)` is *equally* `@ApiStatus.Internal` there,
  as is the whole of `PluginManager`'s lookup surface — so swapping one for the other buys nothing. The
  plugin now derives `<plugin>/daemon` from its own `CodeSource` (plain JDK, no platform API), with
  `PathManager.getPluginsPath()` as the fallback. A side benefit: no plugin-id constant in code to drift
  from `plugin.xml`.
* `DaemonCodeAnalyzer.restart()` (no argument) is deprecated; the reason-carrying overload
  `restart(Object)` exists in every IDE from 253 on.

`resetModificationStamp()` is **kept deliberately**. Declarative inlays cache on a modification stamp that
`DaemonCodeAnalyzer.restart()` does not invalidate, so without it an analysis result only appears on some
later pass, and the public `com.intellij.codeInsight.hints.declarative` package contains no reset of any
kind. `ReadAction.compute` is likewise still open: the replacement is a `NonBlockingReadAction` with
different cancellation semantics, a real change rather than an import swap.

⚠ Because `failureLevel` is set explicitly (it excludes the three categories above and fails on
everything else — compatibility, invalid plugin, missing dependencies, structure warnings, scheduled-for-removal,
override-only, non-extendable), **a NEW internal usage would not fail the build either**. The verifier
always prints its counts; read them, not the exit code. Reports land in
`maddi-intellij/build/reports/pluginVerifier/<IDE>`.

=== Build wiring

`publishing { token = providers.gradleProperty("intellijPlatformPublishingToken") }` is wired (2026-10-28), for the
updates after the first upload. **Signing is not**: `signPlugin` needs a certificate we do not have. Author
signing is optional as far as we know (the Marketplace adds its own signature); confirm on the upload form. Two existing settings are deliberate but worth re-reading before a
release: `buildSearchableOptions = false` (costs the Settings-search entry for our configurable) and
`untilBuild = null`, which matches JetBrains' advice — *"highly recommended not to set this attribute"*.

=== What a first impression should not include

* **The JDK wall — GONE (2026-10-27).** The plugin picks a JDK 25+ from the IDE's own JDKs (`MaddiJdks`), and the
  settings offer them in a list with *Add / Download JDK*.
* The input-configuration gaps — closed by per-module source sets (maddi-dist `docs/roadmap/ide-todo.md` §5).
* Whole-project re-analysis on every trigger (`docs/roadmap/ide-todo.md` §1) on a large project. Still so.

=== Order of operations

. **DONE (2026-08-26)** — id renamed, versions centralized, `verifyPluginStructure` clean, logo added,
  `PluginIdentityTest` guarding all of it.
. **DONE (2026-10-28)** — description, `<change-notes>` from `docs/ide-changelog.md`, licence files inside the zip.
. **DONE (2026-08-26, rerun 2026-10-28)** — `verifyPlugin`: see above.
. The vendor email (`<vendor email="…">` in `plugin.xml`).
. Create the CodeLaser vendor profile; accept the JetBrains Marketplace Developer Agreement.
. Release the CLI for the same version first (`release-cli.sh v<version>`): the plugin downloads its Kotlin front
  end from that release.
. **Upload the zip by hand** at `plugins.jetbrains.com/plugin/add`. The first version cannot be
  published any other way — `publishPlugin` and the API only *update* an existing plugin. Manual review
  follows, with no guaranteed turnaround. (Maximum plugin size is 400 MB; we are at ~18 MB.)
. Afterwards, updates are
  `./gradlew :maddi-intellij:publishPlugin -PintellijPlatformPublishingToken=<token>` (a Marketplace
  personal access token; `publishing { token }` is wired). The alpha/beta/eap channels are
  separate repositories that users must add by URL, so they are not a soft-launch shortcut.

=== Eclipse: an update site, and an Eclipse Marketplace listing

Eclipse installs from a **p2 update site**: a URL serving `content.jar`/`artifacts.jar` and the bundles. The
Eclipse Marketplace only *lists* a plugin and points at such a URL; it hosts nothing. So the Eclipse leg is a
site of our own plus a listing.

* **Built by** the Tycho reactor in maddi-dist's `maddi-eclipse/` (its README): bundle, headless tests, feature,
  repository. Needs **Maven 3.9** (3.10 breaks Tycho 5.0.3). Versions are the release version (`0.9.6.qualifier`
  in the manifests and the feature, `0.9.6-SNAPSHOT` in the poms); the embedded client is copied from the Gradle
  build, not taken from `~/.m2`.
* **Published by** `release-eclipse.sh v<version>` (maddi-dist): it checks the versions, builds, then
  - pushes the repository to the **`gh-pages` branch** of maddi-dist as `eclipse/<version>/`, and rewrites the
    composite at `eclipse/` that lists every version. The composite URL never changes:
    **`https://codelaser.github.io/maddi-dist/eclipse/`** is what users add and what the listing names;
  - attaches the same repository as `maddi-eclipse-<version>.zip` (+ `.sha256`) to maddi's `v<version>` release,
    for offline installs.

  `DRY_RUN=1` builds and prepares the `gh-pages` commit without pushing or uploading. A version is never
  republished: Eclipse caches a repository by URL.
* **Unsigned.** p2 shows an "unsigned content" warning that the user accepts once. Signing needs a code-signing
  certificate (bought, identity-checked: days to weeks); later.
* **Eclipse Marketplace** (`marketplace.eclipse.org`, *Add content*, needs an Eclipse account): name *maddi*,
  the update-site URL above, categories *Code Analysis* / *Languages: Java*, licence Apache 2.0, the feature
  description as the text, screenshots as for IntelliJ. Moderated; usually quick, not guaranteed.

Account-holder steps, in order: enable GitHub Pages on maddi-dist (Settings → Pages: branch `gh-pages`, root);
run `release-eclipse.sh v<version>` after `release-cli.sh`; check the site installs in a clean Eclipse
(*Help → Install New Software → Add…* the URL); then the Marketplace listing.

Versioning
----------

One release train, one version for the whole project, centralized in `gradle.properties`
(`group=io.codelaser`, currently `version=0.9.1`) — **DONE**: the root `gradle.properties` is inherited
by every subproject. Bump it there before a release; Maven Central rejects re-publishing an existing
version, and so does JetBrains Marketplace.

"Every subproject" was aspirational until 2026-08-26: `maddi-support` and `maddi-mvnplugin` had stopped
hard-coding a version, but `maddi-intellij` (`0.1.0`) and `maddi-ide-client` (`0.8.2`) still did, which
is how a plugin zip built from a 0.9.1 tree came to contain `maddi-ide-client-0.8.2.jar`. Both now
inherit. `maddi-eclipse` consumes the client from mavenLocal and pins the coordinate in its pom
(`<maddi.client.version>`), so that property has to move with the release.


Release checklist
-----------------

. Bump the version in `gradle.properties` (once centralized).
. Annotations — **two artifacts since 0.9.1, and the ORDER matters.** `maddi-support`'s POM carries
  a dependency on `maddi-annotation`, so publishing support first puts an artifact on Central whose
  dependency does not exist yet. Central will not stop you; consumers find out instead.
+
--
./gradlew :maddi-annotation:clean :maddi-annotation:publishMavenJavaPublicationToStagingRepository :maddi-annotation:jreleaserDeploy
./gradlew :maddi-support:clean    :maddi-support:publishMavenJavaPublicationToStagingRepository    :maddi-support:jreleaserDeploy
--
+
Let the first reach `repo1.maven.org` before starting the second — see the note at the top of this
document about the polling loop and the Portal→repo1 delay. Credentials/GPG setup: see the appendix.
+
Check both against the resolvability rule above before deploying. Verified for 0.9.1 on 2026-08-17:
`maddi-annotation`'s POM has no `<dependencies>` and all four module-metadata variants are empty;
`maddi-support` has exactly one, `io.codelaser:maddi-annotation:<version>` at `compile`, with
`apiElements`/`runtimeElements` matching it and javadoc/sources empty.
+
⛔ `withJavadocJar()` makes **javadoc a release blocker, not a warning** — a failed javadoc task
means no artifact at all. This bit at the split: `IgnoreModifications` moved into `maddi-annotation`
still carrying `{@link io.codelaser.maddi.support.Memo}`, a type that stayed in `maddi-support`,
which `maddi-annotation` must never depend on. Nothing but running javadoc finds it — the reference
is correctly *named*, merely unreachable from where the class now lives. In this module, name
cross-module types as `{@code}` text, never `{@link}`.
. Gradle plugin: `./gradlew :maddi-gradleplugin:publishPlugins` (Gradle Plugin Portal key required).
  Expect a wait — a first publication under a new namespace is reviewed by hand before it appears.
. Maven plugin: publish to Central once the descriptor is generated.
. CLI: `./release-cli.sh <tag>` (builds both `distZip`s and attaches `maddi-<version>.zip` +
  `maddi-kotlin-<version>.zip` to the GitHub Release for `<tag>`; needs an authenticated `gh`).
. IntelliJ plugin: `./gradlew :maddi-intellij:buildPlugin`, then — for the **first** version only —
  upload `maddi-intellij/build/distributions/maddi-<version>.zip` by hand at
  `plugins.jetbrains.com/plugin/add`. Later versions go through `publishPlugin`. Read "The IDE plugins
  (Package 2)" first; several of its prerequisites are not met yet.


Deferred: the analyzer as a library
------------------------------------

If embedding the analyzer as a library ever becomes a real requirement, do **not** publish the 21
fine-grained JPMS modules. Instead merge them into a handful of coarse JPMS modules (unioning their
`module-info`s) and publish those + a BOM:

[cols="1,3"]
|===
| Target module | Absorbs

| `maddi-support` (kept separate) | the annotations
| `maddi-cst` | util, graph, cst-api/impl/io/print/analysis, inspection-api, inspection-resource
| `maddi-parser-openjdk` | inspection-openjdk, java-openjdk
| `maddi-parser-java` | java-parser, inspection-parser, inspection-integration, java-bytecode
| `maddi-modification` | modification-common, prepwork, link, analyzer
| `maddi-aapi` | aapi-parser, aapi-archive
| `maddi-run` | run-config, run-main, run-openjdk
|===

At that point the plugins would depend on these published modules instead of shading. The Kotlin
modules would remain off Maven Central (see "Why Kotlin is separate").


Appendix — Maven Central mechanics (GPG / JReleaser)
----------------------------------------------------

(Previously the root-level `HOWTO.md`. Key ids and credentials below are placeholders.)

Create gpg key

```
# make an RSA-RSA 4096 bit key, with correct email address
gpg --full-generate-key
# initial check
gpg --list-keys
# export private key
gpg --armor --export-secret-keys  3483290483902483024832  > private.key
# test private key
gpg --list-packets ~/.jreleaser/private.key
# generate public key
gpg --armor --export 3483290483902483024832 > ~/.jreleaser/public.key
# send public key so that Maven Central can see it
gpg --keyserver hkps://keys.openpgp.org --send-keys 3483290483902483024832
# has it been received?
gpg --keyserver hkps://keys.openpgp.org --recv-keys 3483290483902483024832
```

Ensure

```
cat ~/.jreleaser/config.properties
JRELEASER_GITHUB_TOKEN=github_pat_11....

JRELEASER_DEPLOY_MAVEN_MAVENCENTRAL_SONATYPE_USERNAME=abc123
JRELEASER_DEPLOY_MAVEN_MAVENCENTRAL_SONATYPE_PASSWORD=ueohureoi890g024ntoehnieo

JRELEASER_GPG_PUBLIC_KEY=/Users/bnaudts/.jreleaser/public.key
JRELEASER_GPG_SECRET_KEY=/Users/bnaudts/.jreleaser/private.key
JRELEASER_GPG_PASSPHRASE=the secret passphrase
```

Then run
```
gradle :maddi-support:clean :maddi-support:publishMavenJavaPublicationToStagingRepository :maddi-support:jreleaserDeploy
```
