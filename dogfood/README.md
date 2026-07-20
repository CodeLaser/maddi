maddi on maddi
==============

A **standalone** Gradle build — deliberately not listed in the root `settings.gradle.kts`, so nothing
here can affect the normal maddi build. It exists to run the analyzer on maddi's own code, which is
where eventual immutability (`docs/eventual-immutability.md`) has to prove itself: maddi is written in
that style throughout.

It analyzes one module from source, `maddi-cst-impl`, because that is where `TypeInfoImpl` lives.

How to run
----------

```console
$ ./gradlew :maddi-gradleplugin:publishAllPublicationsToLocalPluginRepoRepository
$ ./gradlew build                                    # the dependency jars must exist
$ cd dogfood && ../gradlew e2immu-write-input-configuration
$ cd .. && ./gradlew :maddi-run-openjdk:run --args="\
    --input-configuration $PWD/dogfood/build/inputConfiguration.json \
    --analysis-steps prep,modification \
    --analysis-results-dir /tmp/dogfood-out"
```

After changing the plugin, re-publish it and pass `--refresh-dependencies` to the dogfood build:
the version does not change, so Gradle otherwise serves the cached jar.

Why one module, and not a merged slice
--------------------------------------

Every maddi module is a JPMS module. Merging several into one Gradle project puts several
`module-info.java` in one compilation ("too many module declarations"); dropping them instead makes
javac compile the merge as *one* of the modules, and every `requires`d package then comes back as
"package org.slf4j is not visible". So the module is kept whole and its maddi dependencies come in as
the jars the real build produces — the shape `TestJavaInspector5RealClasspathModule` uses.

The jars are consumed through a `flatDir` repository rather than `files(...)`: the plugin records a
dependency only when the resolved artifact has a module or project component identifier, so a plain
file dependency never reaches the input configuration at all.

Two things this exercise fixed
------------------------------

- `ComputeSourceSets` never marked anything as a Java module. Without `module(true)` the openjdk front
  end puts a modular dependency on the classpath instead of javac's module path, and the whole
  analysis dies on "package X is not visible".
- The input-configuration JSON dropped the `module` flag on the way out and re-derived it from
  `partOfJdk` on the way back in, so even a correctly computed configuration lost it in the round trip
  (`JsonStreaming`).
