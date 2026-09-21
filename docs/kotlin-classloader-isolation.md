# Isolating the Kotlin front end behind a classloader

**Status: in progress** (started 2026-09-21). Tracked downstream as `jfocus-refactor-server/modularization/cassandra/GAPS.md` §G46.

## 1. The defect

`maddi-kotlin-k2` declares `implementation("org.jetbrains.kotlin:kotlin-compiler:2.4.0")`. An
`implementation` dependency is invisible on a consumer's **compile** classpath and fully present on their
**runtime** classpath — which is why the module's own comment ("deliberately self-contained … to keep the
IntelliJ-dependencies graph isolated from the rest of the build") read as true while being false exactly
where it mattered.

That jar is 61.9 MB and carries **8,235 non-Kotlin classes under their original package names** across 17
roots: `org.antlr.v4.runtime`, `com.google.common`, `com.sun.jna`, `org.jline`, `com.fasterxml.aalto`,
`it.unimi`, `io.vavr`, `org.jdom`, `org.apache.log4j`, `javax.inject`, `org.picocontainer`, …

On a flat classpath the first jar that has a class wins, and jar order is an accident of dependency
resolution. The copies are ProGuard-minimised — only the members Kotlin itself calls survive. So:

```
java.lang.NoSuchMethodError: 'org.antlr.v4.runtime.CodePointCharStream
                              org.antlr.v4.runtime.CharStreams.fromString(java.lang.String)'
  at com.puppycrawl.tools.checkstyle.JavaParser.parse(JavaParser.java:85)
```

`javap` on the bundled copy: `CharStreams` has **one** method left, `fromString(String, String)`.

Reported by the Cassandra modularization thread, which lost its whole write path to it. On their
245-entry classpath the fat jar shadows 174 of ANTLR's 215 classes, 787 of guava's 1,962, and 115 of JNA's
124 — 1,098 classes. Guava's copies still match member-for-member, which is luck, not design.

⚠ **maddi ships the same hazard.** In `maddi-kotlin-0.9.1.zip`, `kotlin-compiler` is classpath entry **27**
and `antlr4-runtime-4.12.0` is entry **65**. It has never bitten us only because the dependency that drags
ANTLR in (`jgrapht-io`) turns out never to call it.

## 2. Why not relocation (shading)

The obvious fix — rename the third-party roots inside a shaded jar — was measured and rejected:

- ⭐ it works and it is fast: all 8 jars shade in **13 s**, `UP-TO-DATE` afterwards, and since our own
  bytecode touches exactly one relocatable root (`KDocResolutionKt` → `org.picocontainer`) the bundle is a
  pure function of the Kotlin version, so the edit-test loop pays nothing;
- ⛔ but `shadow` 9.2.2 — the version this repo already uses for both build plugins — **cannot process
  Kotlin 2.4.0 jars at all**: `Provided Metadata instance has version 2.4.0, while maximum supported version
  is 2.3.0`. That couples every Kotlin upgrade to a third party tracking Kotlin's metadata format, on the one
  module whose whole job is tracking Kotlin;
- ⛔ and merging 8 jars that share 6 packages forces a first-one-wins decision at build time — the very rule
  that caused the bug, moved earlier.

`kotlin-compiler-embeddable` is not a swap-in: everything in it is under `org/jetbrains/`, while the
`*-for-ide` artifacts reference `com.intellij` in **1,560** class files.

## 3. Why a classloader is affordable

The usual objection is that isolation breaks code that finds classes by *name*. Counted:

| | classes | `ServiceLoader` | context classloader | `Class.forName` | `getResourceAsStream` |
|---|---|---|---|---|---|
| bundled IntelliJ core | 3,959 | 1 | **0** | 4 | 6 |
| the 7 `*-for-ide` jars | 5,052 | 0 | **0** | 0 | — |

Standalone mode is the mode JetBrains built for embedding, and the dynamic plumbing is not in it.

The library is `org.codehaus.plexus:plexus-classworlds` — Maven's own realm system, small, and with the
import filtering this needs. ⚠ Checked before depending on it: the fat jar does **not** bundle plexus
(its `org/codehaus` is `stax2`). Isolating something with a library that thing also ships would have been a
bad discovery on day three.

## 4. The boundary

Everything the host may see lives in **`maddi-kotlin-api`**; everything else is loaded inside the realm.
Measured surface, across the three consuming modules in this repo:

| | used as |
|---|---|
| `KotlinScan(runtime, sourceSet, infoByFqn).parse(filesByName)` | one call site |
| `KotlinProjectScan(runtime, infoByFqn, ctm).parse(...)` / `.open(...)` | four construction sites |
| `Session`: `convert`, `declare`, `complete`, `isDeclared`, `isCompleted`, `declaredTypes`, `delegationOf`, `hasOrAwaitsBody`, `observe`, `result`, `close` | the interleaved mixed parse |
| `KotlinParseObserver` | passed through opaquely; its `observe` is `internal` and consumers never call it |
| `PlaceholderCensus` | pure CST walk — **no K2 dependency at all** |

Every parameter and return type is a CST type (`Runtime`, `SourceSet`, `TypeInfo`, `MethodInfo`), an
`InfoByFqn`, a `CompiledTypesManager`, or a JDK type. The only K2 type anywhere in the surface is `KtFile`,
on the `internal` observer method.

⛔ **The invariant to protect.** `maddi-cst-api` and its implementation must be loaded **once, by the host**,
and imported into the realm. The mixed inspector's whole point is that a cross-language reference resolves to
a *single* `TypeInfo`; two copies of the CST classes and that silently stops being true, with no error.

## 5. Order of work

1. ✅ `maddi-kotlin-api` exists; `PlaceholderCensus` (which never needed K2) moved into it, with
   `NoCompilerOnTheApiClasspathTest` asserting the compiler, the IntelliJ platform, ANTLR, guava and JNA are
   all *unloadable* from it — the check a consumer actually experiences.
2. The front-end contracts as interfaces in `maddi-kotlin-api`; `maddi-kotlin-k2` implements them; consumers
   go through them. Still one classpath, so a regression here is a compile error, not a runtime one.
3. The realm: plexus-classworlds, a bootstrap that takes the K2 jar list, and the import filter.
4. The heavy jars leave `runtimeElements`; a resolvable configuration replaces the transitive leak.
5. Verification: the Kotlin suites, detekt and coil, and a first-one-wins classpath census that must reach
   **0** (today: 1,098 on the consumer's classpath, 174 in our own zip).

## 6. Consumers

Exactly two configurations downstream declare the front end, both `testRuntimeOnly`, both reaching it by
`ServiceLoader` (`KotlinSupport`); no production configuration declares it and no distribution bundles it.
So the bootstrap line lands in two places. The real exercise of the new contract is
`TestEveryWritingVerbOnKotlin` in `codelaser-refactor-graalpy`'s test source set, which runs every registered
writing verb against a `source.kotlin=true` project — point the first snapshot there.

⚠ Related, and closed by the same change: `maddi-kotlin-k2` forces IntelliJ's *patched*
`kotlinx-coroutines-core` through a `dependencySubstitution` rule. Substitution rules are local to the
project that declares them, so a consumer does not inherit it — downstream had to hand-copy the rule into two
build files to stay correct. Both copies are deleted when the realm lands, because the patched coroutines
goes inside it.
