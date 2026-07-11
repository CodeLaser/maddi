# Some java.base value types (BigInteger/BigDecimal) fail to load: module-vs-classpath / `jdk.internal.*`

Status: **open**, needs the platform-loading rework. Written from a session that added the
`java.math` annotated-API package; `RoundingMode`/`MathContext` work, `BigInteger`/`BigDecimal`
do not.

## Symptom

When the archive (or any consumer) references `java.math.BigInteger` / `java.math.BigDecimal`,
the openjdk inspector cannot load them:

```
WARN  AnalysisHintsParser -- Ignoring type 'java.math.BigDecimal', cannot load it.
WARN  AnalysisHintsParser -- Ignoring type 'java.math.BigInteger', cannot load it.
```

`compiledTypesManager().getOrLoad(BigInteger.class)` returns `null`. Their `java.math` siblings
`RoundingMode` (enum) and `MathContext` (final class) load and analyse correctly.

- warning site: `maddi-aapi-parser/.../AnalysisHintsParser.java:112`
- the hard failure underneath is `ClassSymbolScanner.java:167`
  `throw new UnsupportedOperationException("Type " + cs.fullname + " not found")` (owner kind NIL),
  the same shape as the well-known "Type Applet not found" / TestCloneBench failure.

## Root cause

`BigInteger`/`BigDecimal` class files reference **encapsulated `jdk.internal.*` packages**; the
inspector reads `java.base` as an encapsulated **module**, so those references cannot be resolved
while completing the class symbol, and the whole type fails to load. Evidence (`javap -p -v`):

| type | referenced `jdk.internal.*` (from constant pool / annotations) |
|---|---|
| `BigInteger` | `jdk.internal.vm.annotation.{Stable, IntrinsicCandidate, ForceInline}`, `jdk.internal.math.{FloatConsts, DoubleConsts}`, `jdk.internal.util.ArraysSupport` |
| `BigDecimal` | `jdk.internal.vm.annotation.Stable`, `jdk.internal.math.FormattedFPDecimal`, `jdk.internal.util.DecimalDigits`, `jdk.internal.access.{JavaLangAccess, SharedSecrets}` |
| `RoundingMode`, `MathContext` | **none** — which is exactly why these two load |

`jdk.internal.*` packages are strongly encapsulated (not `exports`ed by `java.base`).

## The decisive comparison (module vs classpath)

The **same `JavaInspectorImpl`** (same javac task, same `--release=26`) loads `BigInteger` in one
setup and not in the other, so `--release` is **not** the discriminator:

- **Works** — `AnalysisHintsComposer` tests (`TestAnalysisHintsComposer`) build the inspector with a
  single raw classpath entry:
  ```java
  new InputConfigurationImpl.Builder().addSources("none").addClassPath("jmod:java.base")
  ```
  `java.base` is on the **classpath** (the unnamed module); classpath reading **ignores module
  encapsulation**, so `jdk.internal.*` resolves and `BigInteger` loads. This is how the composer was
  able to generate `JavaMath.java` in the first place.

- **Fails** — the test harness (`maddi-modification-common` testFixtures `CommonTest.javaInspectorWithExtras`,
  used by `maddi-aapi-parser`) registers `SourceSetImpl.javaBase()` — a **module-based** source set
  (`SourceSetImpl.java:73`, `module=true`, `externalLibrary=true`) — via `addClassPathParts(...)`.
  That source set becomes `inputConfiguration.javaBase()`, which `CompiledTypesManagerImpl` uses as
  *the* `javaBase` to resolve every `java.*` type (`JavaInspectorImpl.java:153`). `BigInteger` is
  resolved through that encapsulated module context and its `jdk.internal.*` deps are looked up in
  the *same* context → unresolvable.

Relevant file-manager wiring in `JavaInspectorImpl`:
- `externalLibrary` source sets are excluded from the actual class/module path
  (`JavaInspectorImpl.java:367` `if (!dependency.externalLibrary())`), so `javaBase()` is *not* added
  to `CLASS_PATH`/`MODULE_PATH` — the platform comes from javac's default/`--release` view.
- `CLASS_PATH` / `MODULE_PATH` locations: `JavaInspectorImpl.java:377` / `:380`.
- javac task options: `JavaInspectorImpl.java:404`
  (`-proc:none --enable-preview --release=26 -parameters -XDuseUnsharedTable=true`).

## Fixes that were tried and did NOT work

1. **`--add-exports java.base/jdk.internal.{vm.annotation,math,util,access}=ALL-UNNAMED`** added to the
   javac task options (`JavaInspectorImpl.java:404`). Still `null`. (`--add-exports` changes
   accessibility, not which platform view is consulted; and it addresses the wrong layer since the
   composer already works without it.)

2. **`.addClassPath("jmod:java.base")`** added to the harness builder (mirroring the composer). Still
   `null`: the module-based `javaBase` source set is the authoritative resolution context for
   `java.math`; a *supplementary* classpath jmod does not change which context resolves the type.

## Suggested direction

Make the platform (`java.base`) that `CompiledTypesManager` resolves against readable as
**classpath full-classes** (encapsulation-ignoring), the way the composer's `addClassPath("jmod:java.base")`
does — rather than as an encapsulated module. The complication: `java.desktop` and `java.net.http`
are registered as **modules** (`SourceSetImpl.jdkModule(...)`) and depend on `java.base` being the
module `javaBase`, so the two cannot simply be swapped. Options to weigh:

- give the module-based platform read access to the specific `jdk.internal.*` packages (a real
  `--add-exports`/`--add-reads` that actually reaches the class-file completion path, not just the
  source-compilation options), or
- resolve `java.base` from a classpath jmod for symbol completion while keeping `java.desktop` /
  `java.net.http` on the module path, or
- special-case completion of platform types so an unresolvable `jdk.internal.*` reference is a soft
  failure (like `reportMissingClassFile`) instead of the hard `ClassSymbolScanner:167` throw.

## Minimal reproduction

Any test extending the `maddi-aapi-parser` `CommonTest`:

```java
assertNull(compiledTypesManager().get(java.math.BigInteger.class));   // fails to load
assertNotNull(compiledTypesManager().get(java.math.RoundingMode.class)); // loads fine
```

## Consequence for the archive

`java.math` is annotated in `maddi-aapi-archive/.../jdk/JavaMath.java` (all four types
`@ImmutableContainer`; see `TestJavaMath`). The annotations are correct and apply wherever
`java.base` is on the classpath (composition, real analyzer runs). Until this loading issue is
fixed, `BigInteger`/`BigDecimal` cannot be asserted in the aapi-parser harness, and each archive
load logs the two "cannot load it" warnings above.
