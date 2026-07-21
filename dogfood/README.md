maddi on maddi
==============

A **standalone** Gradle build — deliberately not listed in the root `settings.gradle.kts`, so nothing
here can affect the normal maddi build. It exists to run the analyzer on maddi's own code, which is
where eventual immutability (`docs/eventual-immutability.md`) has to prove itself: maddi is written in
that style throughout.

One subproject per maddi module, each pointing at that module's real source directory:

| subproject | analyzed as | why |
|---|---|---|
| `:cst-api` | source | holds `TypeInfo`, the **interface** |
| `:cst-impl` | source | holds `TypeInfoImpl`, the **implementation** |
| maddi-support, maddi-util, maddi-cst-analysis | jars (flatDir) | below the pair under test; maddi-support stays a jar so that reading `@Mark`/`@Only` out of **byte code** is exercised |

Both interface and implementation must be analyzed *as source*: a jar type never enters the
abstract-method batch, so nothing an implementation computes can travel up to its interface — and by
the hierarchy rule an undecided or mutable supertype then drags every implementation down again.
Carrying `:cst-api`'s sources into `:cst-impl`'s input configuration is what the plugin's
`e2immuSourceElements` variant is for.

How to run
----------

```console
$ ./gradlew :maddi-gradleplugin:publishAllPublicationsToLocalPluginRepoRepository
$ ./gradlew build                                    # the dependency jars must exist
$ cd dogfood && ../gradlew --refresh-dependencies :cst-impl:e2immu-write-input-configuration
$ cd .. && ./gradlew :maddi-run-openjdk:run --args="\
    --input-configuration $PWD/dogfood/cst-impl/build/inputConfiguration.json \
    --analysis-steps prep,modification \
    --analysis-results-dir /tmp/dogfood-out"
```

After changing the plugin, re-publish it and pass `--refresh-dependencies`: the version does not
change, so Gradle otherwise serves the cached jar. Exit code 5 is `ANALYSER_ERROR` (cycle protection
trips on a few of the printer methods); the analysis results are still written.

Why the modules are not merged
------------------------------

Every maddi module is a JPMS module. Merging several into one Gradle project puts several
`module-info.java` in one compilation ("too many module declarations"); dropping them instead makes
javac compile the merge as *one* of the modules, and every `requires`d package then comes back as
"package org.slf4j is not visible".

The jars are consumed through a `flatDir` repository rather than `files(...)`: the plugin records a
dependency only when the resolved artifact has a module or project component identifier, so a plain
file dependency never reaches the input configuration at all.

`sourcePackages` is deliberately NOT set
----------------------------------------

Setting it triggers an analyzer bug that is fatal for any **modular** project. `ParseOptions.ignoreModule`
(which the openjdk runner always sets) filters `module-info.java` out of the compilation units — but a
package restriction makes `JavaInspectorImpl` put the source roots on javac's `SOURCE_PATH`, where javac
finds that same `module-info.java` and compiles it *implicitly*. The compilation is then a named module
after all, everything on the class path belongs to the unnamed module, and every cross-module reference
fails with "package org.e2immu.language.cst.api.info does not exist".

Symptom to recognise: `restrictToPackages` set + `module-info.java` present ⇒ a flood of "package X does
not exist" that disappears the moment the restriction is dropped.
