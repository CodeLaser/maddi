# Stable, deterministic parsing with the maddi openjdk inspector

**Authoritative reference.** If your (test) runs using `maddi-inspection-openjdk`
intermittently fail with `tree.starImportScope is null`, flip results between runs, or
you are about to add `forkEvery = 1` / disable parallelism to "fix flakiness" — read this
first. The instability has one real root cause (already fixed inside the library) and one
caller-side rule. Everything else that *looked* like flakiness was a measurement artifact.

TL;DR:

1. **The library already applies the fix.** `-XDuseUnsharedTable=true` is passed on the
   single javac invocation path. You get it for free by using `JavaInspectorImpl`. Do not
   remove or override it.
2. **Never run javac concurrently.** Parse *serially* in one `JavaInspector`; parallelize
   only the javac-free analysis/linking afterwards.
3. **The output is deterministic** (CST keyed by FQN strings, stable hashcodes). So
   `maxParallelForks > 1` and `forkEvery = 0` are safe. You do **not** need a fresh JVM
   per test class.

---

## 1. Root cause: javac's process-wide `SharedNameTable`

javac interns identifiers into a `com.sun.tools.javac.util.Name` table. By default that
table is drawn from a **process-wide `SharedNameTable` freelist** — static state shared
across *every* `JavacTask`/`Context` in the JVM. Under repeated parsing (hundreds or
thousands of compilations in one JVM, e.g. `parseSingleFileInSourceSet` in a loop, or many
test classes in one fork) that freelist intermittently corrupts. It surfaces later, in an
unrelated compilation, as:

```
java.lang.IllegalStateException: tree.starImportScope is null   (during task.analyze())
```

Because the corruption and the crash happen in *different* compilations, it reads as
"flaky": the same suite passes one run and fails the next, and shrinking the run to a
single class often makes it disappear.

### The fix (already in the library — do not undo)

Each compilation is given its **own** name table via the javac option
`-XDuseUnsharedTable=true`. It is set unconditionally on the one and only
`JavaCompiler.getTask(...)` call:

- `maddi-inspection-openjdk/src/main/java/org/e2immu/language/inspection/openjdk/JavaInspectorImpl.java`
  (the `getTask` call, ~line 393-405), alongside `-parameters`, `-proc:none`,
  `--enable-preview`, `--release=26`.

This is safe **because maddi keys its CST by fully-qualified-name strings, not by javac
`Name` identity.** Nothing in maddi relies on names being shared across compilations, so
not sharing them changes no result — it only removes the shared mutable state.

There is exactly one javac entry point, so every consumer of the inspector (production
`parse(...)`, `onlyPreload()`, single-file test parses) inherits the fix. If you write your
own `getTask` against javac elsewhere, you must add `-XDuseUnsharedTable=true` yourself.

## 2. Caller-side rule: javac is not thread-safe — parse serially

`-XDuseUnsharedTable=true` removes the *cross-compilation* corruption, but a single javac
`Context` is still **not safe to drive from multiple threads**, and parsing the same logical
files through many parallel `JavaInspector` instances re-introduces the `starImportScope`
crash. The rule:

> Parse **once, serially**, in a single `JavaInspector`/runtime. Parallelize only the
> analysis/linking phase, which is javac-free.

The reference implementation of this pattern is the clone-bench harness:

- `maddi-modification-analyzer/src/test/java/org/e2immu/analyzer/modification/analyzer/clonebench/TestCloneBench.java`
  — all directories are parsed up front in one inspector (one `SourceSet` per directory so
  that identically-named default-package types don't collide); only the analysis is fanned
  out across threads.

If you parse many independent inputs, give each its own `SourceSet` inside **one**
inspector rather than spinning up an inspector per input — that also avoids thousands of
redundant JDK loads.

## 3. The output is deterministic — parallelism across forks is fine

maddi's linking/analysis output is deterministic: the graph and label algebra use
FQN-stable hashcodes, not identity or iteration-order-dependent hashing. This was verified
directly: three independent parallel runs (`maxParallelForks = 4`, `forkEvery = 0`) were
byte-identical, and equal to serial, to monolith (all classes in one JVM), and to isolated
(one JVM per class) — **zero** result flips across every configuration.

Consequences for your test setup:

- `maxParallelForks > 1` is safe (each fork is a separate JVM, so javac's static state is
  per-fork, not shared).
- `forkEvery = 0` (the Gradle default: one JVM runs all of a fork's classes) is safe. You
  do **not** need `forkEvery = 1`. It buys no determinism and costs ~20% wall time.
- Reference config:
  `maddi-modification-link/build.gradle.kts` (`tasks.withType<Test>`) —
  `maxParallelForks = 4`, `forkEvery = 0`, both left overridable via `-PtestForks` /
  `-PforkEvery` for emergencies only.

### The "flakiness" that wasn't

An earlier investigation reported ~40 tests flipping between runs and added `forkEvery = 1`
on that basis. That report was a **measurement artifact**: two run captures were diffed with
*inconsistent HTML-entity decoding* (`T->R` in one, `T-&gt;R` in the other), so identical
results were counted as different. With a consistent XML parser the flips are zero.
`forkEvery = 1` was reverted.

> If you need to compare two suite runs, parse `build/test-results/test/*.xml` with a real
> XML parser (unescape entities once, consistently) before diffing. Do not string-diff the
> raw XML or terminal output.

## 4. Checklist for a downstream project

Required to run at all (javac internals are not exported by default):

```kotlin
tasks.withType<Test> {
    // javac is not thread-safe: separate JVMs are fine, threads within one are not
    maxParallelForks = 4      // safe; scale to cores
    forkEvery = 0             // safe; do NOT set to 1 for "stability"
    maxHeapSize = "2G"

    jvmArgs(
        "--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
    )
}
```

Application/`main` runs need the same `--add-exports` (see
`maddi-run-openjdk/build.gradle.kts`, `applicationDefaultJvmArgs`).

In code:

- Use `JavaInspectorImpl` — the `-XDuseUnsharedTable=true` fix is built in; do not fork a
  private javac path without it.
- Call `setParameterNames(true)` **before** any loading if you want real (class-file)
  parameter names; it must precede the first parse/preload.
- Parse serially in one inspector; parallelize only the analysis/linking that follows.
- One `SourceSet` per input directory when parsing many inputs, to keep same-named types
  apart and load the JDK only once.

## Symptom → cause quick table

| Symptom | Cause | Action |
|---|---|---|
| `IllegalStateException: tree.starImportScope is null` | javac `SharedNameTable` corruption, or concurrent javac | Ensure `-XDuseUnsharedTable=true` is present (it is, in `JavaInspectorImpl`); ensure you parse serially |
| Results differ between two runs | almost always a diff/decoding artifact | re-diff with a consistent XML parser; results are deterministic |
| Tempted to add `forkEvery = 1` | belief that per-class JVM resets fix flakiness | don't — it doesn't help and is slower; the two rules above are the real fix |
| Synthetic `arg0, arg1` parameter names | `setParameterNames(true)` not called, or called after loading | call it before the first parse/preload |

## Sources

- `maddi-inspection-openjdk/.../JavaInspectorImpl.java` — the `getTask` options (the fix).
- `maddi-modification-link/build.gradle.kts` — reference test config + the forkEvery note.
- `maddi-modification-analyzer/.../clonebench/TestCloneBench.java` — parse-serial /
  analyze-parallel pattern.
- `maddi-modification-link/linking-manual.md` — notes the `starImportScope` flake.
