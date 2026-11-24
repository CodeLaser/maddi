# maddi 0.9.7

**Faster, more precise and more incremental.** The modification engine runs fernflower, timefold and langchain4j in
roughly half the time it took in 0.9.6. After an edit, the IDE daemon and the MCP server can re-analyse only what the
edit changed (experimental). A new, opt-in ownership pass computes who holds an object: `@Fresh` results, `@Borrowed`
parameters and `@Owned` fields. The VS Code extension catches up with the IntelliJ and Eclipse plugins, and is
published for the first time. Kotlin
sources are handled far better: the Kotlin front end now converts all but 3 of the Exposed declarations it used to
leave as placeholders (243), and the Gradle plugin analyses Kotlin sources.

Code that compiles against the 0.9.6 annotations compiles against 0.9.7 unchanged; three annotations are new. Some
verdicts change, most towards what the code does; see [Changes that can affect you](#changes-that-can-affect-you).

## Incremental analysis (experimental)

- **Re-analyse only what an edit changed.** The IDE daemon and the MCP server can keep the previous run's model and,
  after an edit, re-parse only the changed files, re-analyse the changed types and their dependents, and keep every
  other type's results. An early cutoff stops at a changed type whose dependents see nothing different.
- **A warm result is the cold one, or it is replaced by the cold one.** When a change could reach further than the
  cutoff can prove, the run falls back to a full analysis. A slow test checks, edit after edit on fernflower, that
  the warm result equals a cold run's.
- **Measured on fernflower**: over three edits and their restores, five of six warm runs took 0.13 to 1.4 s, against
  about 239 s cold. An edit with a wide reach saves little: 193 of 227 primary types re-analysed took 236 s.
- **Turning it on**, off by default everywhere:
  - IntelliJ, Eclipse and VS Code: the setting *Incremental re-analysis (experimental)* (`maddi.incrementalReanalysis`
    in VS Code). Changing it restarts the daemon.
  - MCP server: `--incremental` or `MADDI_INCREMENTAL` (the bundle's `incremental` setting). `analyze` with `cold`
    gives a certified run again, and each run says what it re-parsed and seeded.
- **Stored results** (MCP server: `--persist` or `MADDI_PERSIST`, off by default): each result is written to disk, and
  a request whose inputs have not changed gets it back, across daemon restarts and project switches. The model is
  then rebuilt in the background, so the first edit after a restart is already incremental.
- **An incremental result is never certified.** The clients show it as best available. Turn the setting off, or ask
  for a `cold` run, for certified values.
- Adding or removing a file, moving a type, and changing a declaration (an overload, a supertype) are handled without
  falling back; the dependents that rely on the moved declaration are re-parsed. The heap stays flat across edits.

## Ownership: `@Fresh`, `@Borrowed`, `@Owned`

A new pass, run after the modification analysis converges, says who may hold an object:

| Annotation | On | Means |
|---|---|---|
| `@Fresh` | method | every object it returns was created during the call and is held nowhere else (or it returns `null`) |
| `@Borrowed` | parameter | nothing that outlives the call holds the argument; unlike independence, keeping an immutable object counts |
| `@Owned` | field | the object is reachable only through its holder; `@Owned(elements = true)` says the same of a collection's elements |

- **Positive evidence only.** The pass works from the links the analysis proved; a missing or weaker summary reads as
  shared, so a verdict is never stronger than the evidence. A run-time oracle on fernflower's test suite finds no
  contradictions.
- **Closed world.** A project that is an application, rather than a library, can say so: no code outside it calls its
  public methods, so many more parameters are borrowed. On fernflower, closed world gives 100 owned fields and 1,358
  borrowed parameters; on gson, 18 and 469.
- **Why not.** The IDEs and the MCP server explain why an element is not `@Fresh`, `@Borrowed` or `@Owned`, as they
  already did for `@Container`.
- **`@Finalizer` is computed**, no longer only declared. A receiver used after its finalizer has been called, such as
  a reused stream, is reported as a `finalizer-violation`.
- **Contracts.** A `@Fresh`, `@Borrowed` or `@Owned` you write that the analysis refutes is reported as a contract
  violation.
- **JDK contracts** so far: `toArray()`, `ArrayList.clone()`, all `Arrays.copyOf`/`copyOfRange`, `Map.entry` and
  `String.toCharArray` return fresh objects.
- **Turning it on**, off by default:
  - command line: `--ownership`, and `--closed-world` for an application (Java and Kotlin CLIs, `maddi-run-main`);
  - Gradle plugin: `ownership = true`, `closedWorld = true`; Maven plugin: `-Downership`, `-DclosedWorld`;
  - IDEs: the daemon always runs the pass; closed world is a per-project toggle in IntelliJ's *Tools* menu, the
    `maddi.closedWorld` setting in VS Code, and a workspace preference in Eclipse;
  - MCP server: `configure_project` takes `closedWorld`; `overview` counts the three annotations and `verdicts` names
    them.
- **The Rust translation uses them**: a `@Fresh` method returns by value, an `@Owned` field is a plain field.
- The design is in [`ownership.md`](https://github.com/CodeLaser/maddi-mod/blob/v0.9.7/docs/roadmap/ownership.md),
  and the book's chapter on ownership explains the annotations.

## A faster modification engine

- **The link engine computes the overlap between two objects once per statement**, instead of deriving and storing
  it for every pair (maddi-mod#92). Measured on laser1, two runs each:

  | Corpus | 0.9.6 | 0.9.7 |
  |---|---|---|
  | fernflower | 652 / 685 s | 436 / 393 s |
  | timefold-solver | 944 / 876 s | 478 / 493 s |
  | langchain4j | 156 / 154 s | 87 / 88 s |
  | elasticsearch | 163 / 127 s | 248 / 218 s |

  Elasticsearch is slower, because of one method, `DesiredBalanceComputer.compute`. The more precise analysis of
  recursive methods (below) costs back some of the gain: +11% on fernflower, +26% on Guava.
- **Large call cycles run in parallel.** A cycle of more than 512 elements is split into chunks that are analysed
  concurrently. On a closed-source corpus of 25,000 types, a cycle of about 10,000 had run on one thread for hours.
- **Eventual verdicts wake only what depends on them**, instead of repeating whole passes: on that same corpus the
  eventual round had taken 10 full passes, about two hours.
- **Less memory**, all with identical output:
  - fernflower's live heap after the first iteration: 784 → 367 MB, by interning link ends and virtual fields;
  - consecutive statements share identical links; on langchain4j 39% fewer distinct link objects;
  - property maps are created lazily and packed when small; javac's trees are released after the parse; one archive
    file system per jar instead of one per source set. On the 22,000-type corpus these save several gigabytes, and a
    1,680-source-set project no longer runs out of the default heap.
- **A memo that leaked** 150 MB per thread is held weakly now, without the 30% slowdown dropping it would cost
  (fernflower 358 s instead of 516 s).
- **Two failures on large projects are fixed**: a dropped model of 43,500 types (8.6 GB) that stayed reachable, and an
  `OutOfMemoryError` after a 22,000-type analysis.
- **Quieter logs**: per-iteration diagnostics are at DEBUG; an element that takes more than 5 s is still reported at
  INFO, with the reason it was linked again.

## VS Code extension

- **First release on the VS Code Marketplace and Open VSX**, as *maddi* (`CodeLaser.maddi-vscode`), marked as a
  preview. Its version is that of the daemon it bundles, 0.9.7. The `.vsix` is also on the GitHub release.
- **The hover says why**, as IntelliJ's gutter and Eclipse's ruler do: the chain of statements behind each verdict,
  with links to them, why a type is not `@Container` when it is close, and a method's parameters.
- **Stop maddi Analysis**, as a command and as the cancel button of the progress notification. What the run has
  already sent stays, marked provisional. A daemon that dies ends the run at once, instead of after a ten-minute
  timeout.
- **Per-module source sets.** The workspace is sent as each module's main and test source sets, read from the Java
  language server, as the Eclipse plugin does; a single class path is the fallback.
- **Hints follow their declarations through edits**, and disappear with them. *Analyze* saves the Java editors first,
  and asking again during a run queues one more run.
- **The new settings**: incremental re-analysis and closed world.
- Fixed: a workspace opened through a symbolic link (on macOS, `/var`) showed no hints.
- It needs the *Language Support for Java* extension (`redhat.java`) and a JDK 25 or later in `maddi.jdkHome`.
- It analyses Java only.

## Kotlin

- **The Kotlin front end converts nearly everything.** Constructs it cannot convert become placeholders, which the
  analysis treats as unknown. On Exposed, placeholders went from 243 to 3; detekt is at 1, retrofit at 0
  ([#220](https://github.com/CodeLaser/maddi/issues/220), [#221](https://github.com/CodeLaser/maddi/issues/221)).
  Among the constructs now handled: companion supertypes and their constructor calls, import aliases, reified
  `T::class`, unsigned types, `try`, `if` and jumps inside expressions, scope functions, `@JvmName` operators,
  inline-class mangled names, typealias constructor references, `Array(n) { }`, `entries`, local suspend functions,
  and the kotlin-dsl compiler plugins (sam-with-receiver, assignment), read per module.
- **Kotlin and Java agree.** fernflower analysed from Kotlin and from Java now gives the same type verdicts (4 types
  differed), and the remaining differences in fields and methods dropped from 33 to 5. Fixes include a modification
  through `map.values`, a labelled `run { }`, an `object :` helper or a captured local, and a private `var` without
  a setter, which is effectively final.
- **`forEach` is a loop.** `xs.forEach { it.mutate() }` now marks `xs` modified, as a `for` loop does (maddi-mod#145).
- **Library contracts reach Kotlin calls**: a method inherited from a supertype carries the supertype's contracts,
  and Kotlin and kotlinx hints cover 199 more library members.
- **The Gradle plugin analyses Kotlin.** When a source directory holds a `.kt` file, the plugin runs the Kotlin
  analyzer in a separate process on a JDK 26 toolchain. It downloads `maddi-kotlin-0.9.7.zip` from this release,
  checks its SHA-256 and caches it; `kotlinDistribution` points at a local copy for offline builds, and
  `kotlinJavaHome`, `kotlinJavaVersion` and `kotlinJvmArgs` adjust the JVM. `skipKotlinSources = true` keeps the old,
  Java-only behaviour.
- **Mixed Maven builds**, found on Quarkus: a whole-reactor `mvn -X` log (3 GB) is streamed, kotlin-maven-plugin 2.x
  modules are no longer split in two, kapt output joins its source set, and a Lombok too old for the JDK is probed per
  source set instead of stopping the parse.
- **Translation to Kotlin**: `--translate kotlin-stubs` prints the declarations with `TODO("maddi:<id>")` bodies and a
  `maddi-bodies.json`, for a language model to fill in; a one-value record used only in its own file becomes a
  `@JvmInline value class`; an inner class of a generic class keeps its outer type's arguments. All 610 of Guava's
  files still compile.

## Modification and immutability analysis

- **Recursive methods** get a bounded summary of their own result, instead of the most pessimistic one. On the
  clone-bench corpus this removes 188 spurious "modified" verdicts on recursions over files, superclass chains and
  sorts (maddi-mod#127).
- **An overridable method takes its overrides' modifications** (maddi-mod#116).
- **Fewer false modifications**: `hashCode`, `equals`, `compareTo` and `toString` are queries, and a lazily cached
  field or lazily created view is a memo (maddi#208, maddi-mod#124, maddi-mod#136); annotation members do not
  modify; a deeper walk of what a call reaches settles 61 fernflower verdicts as unmodified.
- **Fewer missed modifications**: an element modified through a getter, a `values()` view or a stream's mapped
  elements modifies its owner (maddi-mod#62, #100, #138, #147).
- **`@Identity` and `@Fluent` need every return** to yield the parameter or `this`. A false `@Identity` had let a
  refactoring drop a null guard (maddi-mod#31).
- **`@NotModified(contract = true)`** can now be written on a parameter of a method with a body (maddi-mod#125).
- Independence: abstract methods that fill a parameter, static state, and a field of an immutable final class
  (maddi-mod#81, #107, #135).
- **The same result from run to run.** Link summaries no longer depend on the JVM's set ordering, and the stored
  results write an explicit `this.x` as `x`, so two analyses of the same sources give the same output (maddi-mod#103).

## Nullability analysis

- A parameter in `@NullMarked` code is not tolerant of `null` (maddi-mod#68); fixes for override cycles and anonymous
  objects (maddi-mod#85).
- Measured against the Guava oracle: disagreements 1,977 → 1,786.

## Annotated APIs

- `System.arraycopy` modifies its destination, `Field.set` its target, and `toArray(T[])` writes into its argument
  and may return it ([#205](https://github.com/CodeLaser/maddi/issues/205),
  [#206](https://github.com/CodeLaser/maddi/issues/206), [#211](https://github.com/CodeLaser/maddi/issues/211)).
- Reflection getters are queries ([#207](https://github.com/CodeLaser/maddi/issues/207)); `Throwable`'s stack-trace
  methods are independent; contracts for `BitSet`, `PathMatcher.matches`, `Scanner`, `ZipFile` and
  `DirectoryStream` ([#215](https://github.com/CodeLaser/maddi/issues/215)) and vavr's `Lazy`
  ([#212](https://github.com/CodeLaser/maddi/issues/212)).
- Analysing Guava, Jackson or Mockito from source with the shipped results preloaded no longer fails with an
  `AssertionError`.

## Translations

- **Java → C#** (experimental): on timefold-solver, errors 641 → 405 and compiling files 1,321 → 1,390;
  langchain4j 171 → 155 errors. Declaration-site variance, read-only collections, readonly and record structs, and
  Java's string conversion and ordering. A new check runs sample programs on the JVM and on .NET and compares their
  output. `csharp-check` runs on .NET 10 or 11.
- **Java → Rust** (experimental): shared objects as `Shared<T>`, a closed hierarchy as one struct with a kind enum,
  more of `java.io`, `java.nio` and `BitSet`, and JUnit tests as `#[test]`. Test sources are translated too. 302 of
  the test set's crates compile (297).

## Java front end and configuration

- **Paths with spaces**: a Maven `-X` log prints javac's arguments unquoted, so a module such as Quarkus's
  `integration-tests/simple with space` was cut at the space. Such paths are rejoined, checked against the file
  system, and percent-encoded in the configuration's URIs.
- **The same type name in unrelated source sets** resolves as javac resolves it, and `ParseResult` lists such names
  in a fixed order, with `duplicatePrimaryTypes()` to count them ([#224](https://github.com/CodeLaser/maddi/issues/224)).
- **Lombok** runs only for source sets whose sources use it, so a project with an old, unused Lombok on its class path
  no longer fails.
- `var` with an intersection type on JDK 27 ([#189](https://github.com/CodeLaser/maddi/issues/189)); an inner class of
  a generic class keeps its outer type's arguments ([#142](https://github.com/CodeLaser/maddi/issues/142)); an
  anonymous class's creation names the superclass constructor
  ([#194](https://github.com/CodeLaser/maddi/issues/194)); `T[]::new`
  ([#195](https://github.com/CodeLaser/maddi/issues/195)); the source position of an annotated varargs type.
- **A module's required JDK modules** are added with `--add-modules`. A source set with a `module-info.java` that
  requires an incubator module, such as trino's `jdk.incubator.vector`, lost every file that uses it
  ([#225](https://github.com/CodeLaser/maddi/pull/225)).
- A type-use annotation's arguments count as references, in `typesReferenced` and in the call graph; a labelled
  `@see` resolves to its reference.

## MCP server

- **Configures Maven builds by their directory alone**, and any other build by a compile command or log; `buildJdk`
  names the JDK the build runs on.
- **Incremental runs and stored results**, see above; **ownership counts** in `overview` and `verdicts`.
- `stop_run` also stops a build that was just starting.

## Changes that can affect you

- **Some verdicts change.** Most move towards what the code does:
  - recursive methods, overridable methods and the `hashCode`/`equals` family, as above;
  - `xs.forEach { it.mutate() }` modifies `xs`. On Exposed, `Expression` and 24 of its subclasses go from
    `@Immutable` to `@FinalFields`, because the scope functions inlined now show a call of a function value;
  - the JDK contracts above (`System.arraycopy`, `Field.set`, `toArray(T[])`).
- **New behaviour is on by default**; each has a gate to restore the old one (`NODISPATCHUNION`, `NONARROWRECURSION`,
  `NOOVERLAPBYTYPE`, ...). Several experimental gates of 0.9.6 are removed.
- **Stored analysis results are format version 2**, which also records anonymous, local and lambda types. 0.9.7 still
  reads version 1.
- **Launchers need one more export**: `--add-exports jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED`. The shipped
  launchers and plugins pass it; without it you get a warning and the old, memory-hungry behaviour.
- **For code that builds on maddi as a library**:
  - the javac-log parser moved to `io.codelaser.maddi.run.config.javac` in `maddi-run-config`;
  - an anonymous creation's `constructor()` is the superclass constructor, no longer `null`; use
    `isAnonymousCreation()` and `callsConstructorOfCreatedType()`;
  - `ParseResult` has a new method, `duplicatePrimaryTypes()`;
  - `maddi-project-config` is published from maddi.
- **The IDE plugins and their daemon must have the same version**, as in 0.9.6.

## Installing

| What | Where |
|---|---|
| IntelliJ plugin | *maddi*, on [JetBrains Marketplace](https://plugins.jetbrains.com/) |
| Eclipse plugin | the update site `https://codelaser.github.io/maddi-dist/eclipse/`, listed on the Eclipse Marketplace |
| VS Code extension | *maddi* (`CodeLaser.maddi-vscode`), on the [VS Code Marketplace](https://marketplace.visualstudio.com/items?itemName=CodeLaser.maddi-vscode) and [Open VSX](https://open-vsx.org/extension/CodeLaser/maddi-vscode); `maddi-vscode-0.9.7.vsix` on the [GitHub release](https://github.com/CodeLaser/maddi/releases) |
| Gradle plugin | `id("io.codelaser.maddi.analyzer") version "0.9.7"`, on the Gradle Plugin Portal |
| Maven plugin | `io.codelaser:maddi-mvnplugin:0.9.7`, on Maven Central |
| Annotations | `io.codelaser:maddi-annotation:0.9.7` and `io.codelaser:maddi-support:0.9.7`, on Maven Central |
| Command line | `maddi-0.9.7.zip` and `maddi-kotlin-0.9.7.zip`, on the [GitHub release](https://github.com/CodeLaser/maddi/releases) |
| MCP server | `maddi-mcp-0.9.7.zip` and `maddi-mcp-0.9.7.mcpb` on the [GitHub release](https://github.com/CodeLaser/maddi/releases); `io.github.codelaser/maddi-mcp` on the [MCP Registry](https://registry.modelcontextprotocol.io) |
| Homebrew (macOS, Linux) | `brew install codelaser/tap/maddi`, `codelaser/tap/maddi-kotlin` or `codelaser/tap/maddi-mcp` |

The analyzer needs JDK 25 or later to run, and the Kotlin distribution, and so the Gradle plugin on a project with
Kotlin sources, JDK 26 or later.

## Known limitations

- Incremental analysis is Java only, and an incremental run skips some passes over the whole program: the
  reachability re-derivation, the eventual round and the ownership pass. That is one reason its result is never
  certified.
- Ownership is off by default on the command line and in the build plugins.
- elasticsearch is slower than in 0.9.6.
- The VS Code extension and the Eclipse plugin analyse Java only; the VS Code extension is a preview.
- The explanations are recorded only for the IDE and the MCP server; the command line does not show them yet.
- Java → C# and Java → Rust are experimental: judged on compilation, and for C# on a few sample programs.

## Documentation

- The [IDE user guide](https://github.com/CodeLaser/maddi-dist/blob/v0.9.7/docs/ide-user-guide.md) and the
  [IDE change notes](https://github.com/CodeLaser/maddi-dist/blob/v0.9.7/docs/ide-changelog.md).
- The Gradle plugin's [README](https://github.com/CodeLaser/maddi-dist/blob/v0.9.7/maddi-gradleplugin/README.md), for
  Kotlin sources.
- [`ownership.md`](https://github.com/CodeLaser/maddi-mod/blob/v0.9.7/docs/roadmap/ownership.md) and
  [`incremental-and-overlay.md`](../design/incremental-and-overlay.md).
- The [user manual](https://www.codelaser.io/maddi/manual/) and the book,
  [*The Road to Immutability*](https://www.codelaser.io/maddi/road/).
