Publishing strategy
===================

> **Status: not yet active.** Nothing is being published on a regular basis yet; this document
> records the agreed strategy and the wiring completed so far, for when releases start.

This document describes *what* maddi publishes, *where*, and *why*. The concrete Maven Central
credentials and commands are in the appendix at the bottom.

The guiding principle (agreed): the individual analyzer modules are of no use to an outside consumer,
so **we do not publish them as a fine-grained library**. We publish only the three things people
actually consume — the annotations, the build plugins, and the command-line tools — and we keep the
Kotlin front-end on a separate delivery channel.


Why Kotlin is separate
----------------------

`maddi-kotlin-k2` (and everything above it: `maddi-inspection-kotlin`, `maddi-inspection-mixed`,
`maddi-run-kotlin`, `maddi-cst-print-kotlin`) depends on JetBrains **K2 "for-ide" analysis-API
artifacts that are not on Maven Central** (they come from the JetBrains repositories declared in
`settings.gradle.kts`), and the Kotlin modules target a newer JVM bytecode level than the Java stack.

Therefore the Kotlin side **cannot be published to Maven Central** — a consumer could not resolve its
transitive dependencies. Kotlin support is delivered only as a self-contained CLI distribution (below),
where those jars are simply files in `lib/` and no Maven resolution is involved.


What we publish (Package 1)
---------------------------

=== 1. Annotations — Maven Central

`maddi-support` (`io.codelaser:maddi-support`) contains the user-facing annotations
(`org.e2immu.annotation.*` — `@Immutable`, `@Container`, `@Independent`, …). This is the one artifact
a user's *own code* compiles against, so it is a small, stable library on Maven Central. It already has
the `maven-publish` + jreleaser configuration; publish it per the appendix below.

=== 2. Build plugins — self-contained (shaded)

Because we do **not** publish the analyzer modules, the plugins cannot declare Maven dependencies on
them; instead each plugin **shades the (Kotlin-free) Java analyzer into its own jar**, so it is
self-contained. This is viable precisely because both plugins use only the openjdk (Java) analyzer —
no K2 — and they run it on a classpath (a forked worker for Gradle), where the stripped module
descriptors do not matter.

* *Gradle plugin* (`maddi-gradleplugin`) → **Gradle Plugin Portal**, id `org.e2immu.analyzer-plugin`.
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
  there *with no analyzer module on any classpath*. *Remaining:* apply `com.gradle.plugin-publish`
  (website/vcsUrl/tags) and run `publishPlugins` with a Portal key — the actual push, needs credentials.
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
  `packaging=maven-plugin`, dependency-free POM. *Remaining:* test against a real `mvn` invocation (not done
  — no Maven on this laptop); the host Maven JVM needs the javac `--add-exports` (via `.mvn/jvm.config` or
  `MAVEN_OPTS`) for the openjdk-based goals.

=== 3. Command-line tools — GitHub Releases (not Maven)

The runners apply the Gradle `application` plugin, so `distZip` / `installDist` produce self-contained
bundles (launcher + every runtime jar in `lib/`, with the required javac `--add-exports` baked into the
launcher). Publish these as **GitHub Release** assets:

* `maddi` — the openjdk (Java) runner, from `maddi-run-openjdk:distZip` → `maddi-<version>.zip`, launcher
  `bin/maddi`.
* `maddi-kotlin` — the mixed Java+Kotlin runner, from `maddi-run-kotlin:distZip` →
  `maddi-kotlin-<version>.zip`, launcher `bin/maddi-kotlin`. **This is how Kotlin support ships**: the K2
  "for-ide" jars ride along in `lib/` (verified: the 7 `*-for-ide` jars + `kotlin-compiler` are bundled).

**DONE**: both runners set `applicationName` so the launcher/archive names are `maddi` / `maddi-kotlin`
(not the module names). The build+upload is wired in `release-cli.sh <tag>` (builds both `distZip`s and
`gh release create`/`upload`s the two zips). Both launchers are smoke-tested self-contained (`maddi
--help` exits 0; `maddi-kotlin` loads and reports usage). *Remaining:* run `release-cli.sh` for a tag —
needs an authenticated `gh` and is an outward-facing publish, so run it deliberately.

Not published: none of the fine-grained analyzer modules (`maddi-cst-*`, `maddi-inspection-*`,
`maddi-java-*`, `maddi-modification-*`, `maddi-aapi-*`, `maddi-run-config`, …). No BOM.


Versioning
----------

One release train, one version for the whole project, centralized in `gradle.properties`
(`group=io.codelaser`, `version=0.8.2`) — **DONE**: the root `gradle.properties` is inherited by every
subproject, and `maddi-support` / `maddi-mvnplugin` no longer hard-code the version. Bump it there
before a release; Maven Central rejects re-publishing an existing version.


Release checklist
-----------------

. Bump the version in `gradle.properties` (once centralized).
. Annotations: `./gradlew :maddi-support:clean :maddi-support:publishMavenJavaPublicationToStagingRepository :maddi-support:jreleaserDeploy` (see the appendix for the credentials/GPG setup).
. Gradle plugin: `./gradlew :maddi-gradleplugin:publishPlugins` (Gradle Plugin Portal key required).
. Maven plugin: publish to Central once the descriptor is generated.
. CLI: `./release-cli.sh <tag>` (builds both `distZip`s and attaches `maddi-<version>.zip` +
  `maddi-kotlin-<version>.zip` to the GitHub Release for `<tag>`; needs an authenticated `gh`).


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
