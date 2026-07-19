# Regression: JDK preload fails on a jmod-less `alternativeJREDirectory`

**Reported by:** the IDE thread (`maddi-ide`, branch `ide`), 2026-07-19, after merging `kotlin` at `95ed83ea`.
**Status:** open. Reproduces in *this* tree — `maddi-ide-daemon`'s `WarmAnalysisServiceTest` is red here too.
**Impact:** all three IDE front-ends (IntelliJ, Eclipse, VS Code) cannot analyse anything on this machine.

## Symptom

Every test that runs a real analysis fails during JDK preload:

```
java.lang.AssertionError: Type nature of jdk.internal.vm.ThreadContainer has not been set
	at org.e2immu.language.cst.impl.info.TypeInfoImpl.isInterface(TypeInfoImpl.java:329)
	at org.e2immu.language.java.openjdk.ClassSymbolScanner.addMethodToType(ClassSymbolScanner.java:743)
	at org.e2immu.language.java.openjdk.ClassSymbolScanner.ensureMethod(ClassSymbolScanner.java:874)
	at org.e2immu.language.java.openjdk.ClassSymbolScanner.getOrLoadMethod(ClassSymbolScanner.java:1419)
	at org.e2immu.language.java.openjdk.ClassSymbolScanner.addMethodToType(ClassSymbolScanner.java:819)
	at org.e2immu.language.java.openjdk.ClassSymbolScanner.addMemberToType(ClassSymbolScanner.java:606)
	at org.e2immu.language.java.openjdk.ClassSymbolScanner.loadType(ClassSymbolScanner.java:356)
	at org.e2immu.language.java.openjdk.ScanCompilationUnits.preload(ScanCompilationUnits.java:454)
	at org.e2immu.language.java.openjdk.ScanCompilationUnits.preloadJdk(ScanCompilationUnits.java:428)
	at org.e2immu.language.java.openjdk.ScanCompilationUnits.scan(ScanCompilationUnits.java:151)
	at org.e2immu.language.inspection.openjdk.JavaInspectorImpl.singleSourceSet(JavaInspectorImpl.java:474)
	...
	at org.e2immu.analyzer.ide.daemon.WarmAnalysisService.analyze(WarmAnalysisService.java:64)
```

It is `preloadJdk`, i.e. loading platform types — not anything about the analysed sources. `ThreadContainer` is
reached transitively (nothing in the test mentions it) and `isInterface` is asked before its nature is set.

## Reproducing

```bash
./gradlew :maddi-ide-daemon:test --tests '*WarmAnalysisServiceTest*'
```

Red in this tree at `95ed83ea`, on a machine whose only JDK 25+ is jmod-less (see below). The test is tiny —
it writes one class to a temp dir and analyses it — so the source under analysis is not a factor.

## What changed

Two commits from this batch combine. Neither is wrong alone; together they open a path nothing had taken.

1. **`43076fc8` openjdk: honour alternativeJREDirectory.** The IDE daemon has always set
   `builder.setAlternativeJREDirectory(config.sdkHome())`
   (`maddi-ide-daemon/.../InputConfigurationAssembler.java:37`). Until this commit that was effectively
   ignored and platform types came from the running JVM. Now it is honoured, so they come from the
   configured JDK. This is the behaviour the IDEs want — they deliberately analyse against a chosen JDK 25+
   rather than the JVM the editor runs on — so the commit is doing the right thing.

2. **`4a77305a` inspection: run on jmod-less JDKs by reading modules from the runtime image.** The JDK now
   being pointed at has no `jmods` directory, so the new runtime-image reader is what serves the preload.
   That is the path that fails.

Machine JDKs, for context — note that the *only* JDK new enough for maddi is also the only one without jmods:

| JDK | jmods | usable as `sdkHome` (needs 25+) |
|---|---|---|
| temurin-26.0.1 | **no** | yes — and it is what the IDEs are configured with |
| jdk-24.0.2, openjdk@21, @17, @11, semeru-17 | yes | no, too old |

So on this machine the two changes are not independent: honouring `alternativeJREDirectory` *forces* the
jmod-less path.

## Why the suite did not catch it

`maddi-inspection-openjdk`'s own tests are green here, including the new `TestRuntimeImageFallback`. The gap
is the **combination**:

- `TestAlternativeJRE` requires `-Dtest.jdk21.home` (or `$JDK21_HOME`) and **skips** when unset, which it is
  here. It also targets a JDK ≤ 25 by design (it asserts `java.applet.Applet` resolves, removed in 26), so it
  exercises alternative-JRE against a JDK that *has* jmods.
- `TestRuntimeImageFallback` covers the jmod-less reader, but presumably for the running JVM rather than a
  separately configured one.

Nothing covers *jmod-less **and** alternative-JRE*, which is exactly what every IDE front-end does: they all
set `sdkHome` to a configured JDK 25+, on purpose (a JBR 21 or an older project SDK produces a
`CompiledTypesManagerImpl … typeData is null` NPE, which is why the setting exists at all).

A regression test pinning that pair would be worth having regardless of the fix.

## Where we would look

`ScanCompilationUnits.preloadJdk` → `preload` → `ClassSymbolScanner.loadType`: a type is being asked for
`isInterface` (via `addMethodToType`) before its nature has been set. Reads like an ordering/completeness
difference between the jmods reader and the runtime-image reader — the latter perhaps yielding class symbols
whose nature is populated later, or not at all for `jdk.internal.*`. We have not investigated further; it is
your area and we would be guessing.

## What we did on our side

Nothing — no workaround, no pin. The merge is committed on `ide` as-is (`05e637f6`) with the tests red, so
the regression stays visible rather than being papered over. The alternative was to install a JDK 25 *with*
jmods locally, which would have hidden a bug real users will hit: jmod-less JDK images are common, and the
IDE plugins point `sdkHome` at whatever JDK 25+ the user configures.

Happy to test a fix quickly — the daemon tests are a ~10 second run.

## Resolution (kotlin thread, 2026-07-19)

**Fixed.** `:maddi-ide-daemon:test` is green again (4/4), and `:maddi-inspection-openjdk:test` stays green
including `TestAlternativeJRE`.

**Root cause — one correction to the diagnosis above.** The trigger is `43076fc8` (`--system`) alone; the
jmod-less reader (`4a77305a`) is *not* on this path. The daemon uses the **openjdk** front-end (see the stack:
`ClassSymbolScanner`/`ScanCompilationUnits`), which reads platform types through javac, never through the
integration `ResourcesImpl.addModuleFromRuntimeImage`. It reproduces on a **jmod-ful** JDK too (confirmed on
homebrew-openjdk-26). The jmod-less-ness was incidental: it is only *why* the machine's chosen `sdkHome` is
temurin-26, not what breaks.

What actually happens: `DaemonAnalysisFixture` (and the real IDEs) set `alternativeJREDirectory = sdkHome =
the JVM the daemon runs on`. Honouring it now routes that to `--system <that same JDK>`. `--system` serves the
full runtime image, which — unlike the default `--release`/`ct.sym` — exposes `jdk.internal.*`; the JDK
*preload* then loads `jdk.internal.vm.ThreadContainer` and trips the "type nature not set" assertion. `--release`
never surfaced those types, which is why it worked before.

**The fix.** Skip `--system` when the alternative JRE resolves to the JDK we are running on, and use `--release`
then (identical platform, documented API, no internals). `--system` is kept only for a *genuinely different*
JDK — the cross-JDK case the option exists for (e.g. resolving `java.applet.Applet`, removed in 26). So the
IDE daemon, which runs on its own `sdkHome`, is back to the pre-regression `--release` path; the `--jre`
feature is unchanged for a different target JDK. Fix in `JavaInspectorImpl.createTask` (`isRunningJdk`).

**Regression guard.** `WarmAnalysisServiceTest` is the guard — it reproduces on any machine (the trigger is
`--system`, not jmods), so no jmod-less JDK is needed to catch a recurrence.
