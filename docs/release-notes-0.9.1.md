# maddi 0.9.1

**This release renames every package.** If you use maddi 0.9.0 or earlier, your imports will not
compile against 0.9.1 until you change them. That is deliberate and it is a one-time cost: 0.9.1 is
the release in which the project finishes taking its own name, and no version after it will move
these packages again.

It is also the first release to ship the build plugins and the command-line tools, so for most
people it is not an upgrade but the first installable version.

## What moved

`org.e2immu.*` becomes `io.codelaser.maddi.*` throughout. Two of those prefixes appear in code that
*you* write; the rest are internal to the analyzer.

| Before | After |
|---|---|
| `org.e2immu.annotation` | `io.codelaser.maddi.annotation` |
| `org.e2immu.util.external.support` | `io.codelaser.maddi.support` |

So `import org.e2immu.annotation.Immutable;` becomes
`import io.codelaser.maddi.annotation.Immutable;`, and likewise for `@Container`, `@Independent`,
`@NotModified` and the rest. A project-wide find-and-replace of `org.e2immu.annotation` →
`io.codelaser.maddi.annotation` is the whole migration for most codebases.

JPMS module names follow their packages: `requires org.e2immu.util.external.support` becomes
`requires io.codelaser.maddi.support`.

**The Maven coordinates do not change.** `io.codelaser:maddi-support` is still
`io.codelaser:maddi-support`; only the packages inside it moved. The one exception is the Gradle
plugin id, below.

## The annotations are their own artifact now

`maddi-support` has been split in two:

| Artifact | Module | Contents |
|---|---|---|
| `io.codelaser:maddi-annotation` | `io.codelaser.maddi.annotation` | the 27 annotations, no dependencies at all |
| `io.codelaser:maddi-support` | `io.codelaser.maddi.support` | `SetOnce`, `Either`, the annotated-API support |

If you only want the annotations, depend on `maddi-annotation` alone. **If you already depend on
`maddi-support`, you need change nothing**: it declares `requires transitive` on the annotation
module and an `api` dependency in its POM, so the annotations still arrive with it.

## Licensing

Unchanged in substance, but worth restating because the split touches it:

- `maddi-annotation` and `maddi-support` — **Apache-2.0**. These are the artifacts your own code
  compiles against, and a copyleft licence on an annotations dependency is the kind of thing that
  stalls in legal review for months.
- The analyzer itself — **LGPL-3.0-or-later**, unchanged.

**0.8.2 was published under LGPL-3.0-or-later and stays that way.** A released version cannot be
relicensed. Only 0.9.0 onward is Apache-2.0, so if you need the permissive terms you must be on
0.9.0 or later.

## Installing

**Annotations** (Maven Central):

```xml
<dependency>
  <groupId>io.codelaser</groupId>
  <artifactId>maddi-annotation</artifactId>
  <version>0.9.1</version>
</dependency>
```

**Gradle plugin** — the id has changed, along with the packages:

```kotlin
plugins {
    id("io.codelaser.maddi.analyzer") version "0.9.1"
}
```

It was `org.e2immu.analyzer-plugin`. The old id is not maintained and will not receive this or any
later version; the Gradle Plugin Portal requires a plugin's id and its Maven group to share a
top-level namespace, and `org.e2immu.*` under group `io.codelaser` never satisfied that.

**Maven plugin** — `io.codelaser:maddi-mvnplugin`, goal prefix `maddi`, five goals (`run`,
`write-input-configuration`, `statistics`, `write-analysis-hints`, `compile-analysis-hints`). The
JVM running Maven needs the javac `--add-exports` flags for the openjdk-based goals; set them in
`.mvn/jvm.config` or `MAVEN_OPTS`.

**Command line** — two self-contained zips are attached to this release. Unpack and run; each
carries every jar it needs in `lib/` and the required flags baked into the launcher. No JVM
configuration and no Maven resolution is involved.

- `maddi-0.9.1.zip` — the Java analyzer, launcher `bin/maddi`
- `maddi-kotlin-0.9.1.zip` — Java **and** Kotlin, launcher `bin/maddi-kotlin`

Kotlin support ships only this way. It depends on JetBrains K2 "for-ide" artifacts that are not on
Maven Central, so it cannot be published as a resolvable library — inside the zip they are simply
files in `lib/`.

## Known limitations

- **The Gradle plugin may not be installable the day this release is published.** A first
  publication under a new namespace goes through manual review by a Gradle engineer before it
  becomes visible on the Plugin Portal. Until it clears, use the command-line distribution.
- **The Maven plugin is newly published and lightly exercised.** Its descriptor is
  hand-maintained rather than generated, because the descriptor-generation tooling is not
  compatible with Gradle 9. Please report anything that does not behave as documented.
- The analyzer modules themselves are still not published as libraries, by design. The plugins
  bundle what they need. See `PUBLISHING.md` if you were hoping to embed the analyzer.

## Documentation

*The Road to Immutability* — the book explaining what maddi computes and why — is at
<https://www.codelaser.io/maddi/road/>.
