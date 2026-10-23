# maddi 0.9.5

**maddi now translates Java.** `maddi --translate kotlin` prints a whole Java program as idiomatic Kotlin.
`--translate csharp` prints it as C#, which is experimental. A new nullability analysis decides what may be null in
the output, and the modification analysis decides which collections are read-only. Both run on the same linking
engine as the immutability analysis, which is now one application of that engine among three.

0.9.5 is the first public release since 0.9.1; 0.9.2 and 0.9.3 were internal. Code that compiles against the 0.9.1
annotations compiles against 0.9.5 unchanged. Two changes to the command line can affect existing scripts; see
[Changes that can affect you](#changes-that-can-affect-you).

## Java → Kotlin translation

- **`--translate kotlin --translate-output <dir>`** on the `maddi` command line. By default the modification analysis
  and the nullability pass run first and their facts drive the printer; `--analysis-steps none` translates without
  them. The output directory gets a `translation-report.txt` that lists printer crashes and messages. A crash leaves
  that one file out and exits non-zero.
- The printer covers all of Java's syntax. What makes the output idiomatic comes from the analyses:
  - `T` or `T?` from the nullability verdicts;
  - `?.` and `?:` from Java's null checks, and `!!` only where the analysis knows more than the types say;
  - `List` or `MutableList`, and `val` or `var`, from the modification facts;
  - data classes from records, and expression bodies;
  - JSpecify and type-use `@Nullable` annotations in the input are kept.
- **Measured on whole corpora.** Six open-source corpora are printed as Kotlin and compiled with `kotlinc`:

  | corpus | files | compiling |
  |---|---|---|
  | Fernflower | 199 | 198 |
  | LangChain4j core | 346 | 346 |
  | Guava | 610 | 603 |
  | Nacos client API | 274 | 271 |
  | Jenkins core | 1,329 | 1,271 |
  | Timefold Solver core | 1,583 | 1,361 |

## Java → C# translation (experimental)

- **`--translate csharp --translate-output <dir>`** writes C# under `#nullable enable` and a `Translated.csproj`.
  Roslyn judges the output:

  | corpus | files | compiling |
  |---|---|---|
  | Fernflower | 199 | 185 |
  | LangChain4j core | 346 | 301 |
  | Timefold Solver core | 1,583 | 1,324 |

- The JDK is mapped onto the .NET base library: collections, LINQ for streams, `java.time` and concurrency. Where
  there is no counterpart, a small compatibility layer is written next to the output.
- The analyses decide `?` and `!`, `IReadOnlyList<T>` for parameters that are never modified, properties for getters
  and setters, delegates for functional interfaces, and `sealed` for what nothing extends.

## Nullability analysis

- A new pass runs after the modification analysis, on its link facts. It decides, for every field, parameter, method
  return and type argument, whether it can hold `null`.
- The JDK's annotated API now carries nullness generated from JSpecify's annotated JDK, type arguments and array
  elements included. Libraries marked `@NullMarked` are honoured.
- In 0.9.5 the verdicts reach users through the translations. Writing them back into Java as `@Nullable`/`@NonNull`
  is implemented in the engine but not yet exposed on the command line or in the plugins.

## Kotlin

- **Close to parity with Java.** The analysis stack below the front end is shared, and has no Kotlin-specific paths.
  Test tiers of Kotlin/Java fixture pairs check that both get the same verdicts. A long series of K2 front-end fixes
  closed the shapes that converted silently wrong: local delegated properties, enum entries with bodies, class
  delegation, operator overloads, named-argument evaluation order, multi-type `when` arms, and the inlined scope
  functions `apply`, `also`, `let`, `run`, `with` and `use`.
- **The Kotlin stdlib's annotated API** grew from one file to 48 types and 2,252 members.
- **`maddi-kotlin` takes `maddi`'s command line.** It routes on content: a project without `.kt` files runs exactly
  as `maddi` does. An option the mixed pipeline cannot honour yet is refused by name, with exit code 7.
  `--skip-kotlin-sources` analyses only the Java of a mixed project, accepting an incomplete result.

## Java front end

- **JDK 27.** maddi parses and runs on JDK 27, including javac 27's new trees, and its JDK annotated API is generated
  from JDK 27.
- **Stability:**
  - A failing annotation processor no longer drops a whole source set. A Lombok processor that cannot run is reported
    as a parse error.
  - Unresolved calls and imports become unresolved symbols, not errors.
  - A compilation unit that ends up with neither types nor a failure is reported.
  - Receiver parameters parse. Deeply recursive generics no longer overflow the stack.
- `--compile-log` also reads Ant build logs and shaded jars.
- The parser logs one INFO line per parse.

## Modification and immutability analysis

- A reachability pass after the fixpoint propagates modification along call chains. Several fixes stop it from
  over-modifying delegating implementations and chained receivers, or from losing a lambda's modification of its
  enclosing instance.
- Eventual immutability: after the mark, what a value shares decides its verdict, not its type.
- A captured parameter counts as a field for independence. A view shares the modification of the collection it views.

## Annotated APIs

- **The JDK** grew from 249 types / 5,664 members to 527 / 9,493. The additions include read-only arguments found by
  a census of real call sites, `java.time`, `java.util.jar`, formatting, `Collections.unmodifiable*` and the loggers.
- **`libs.jar`**, shipped with maddi, now covers these libraries:
  - AssertJ, hamcrest, JUnit Jupiter params and Mockito;
  - Guava collect, commons-io and Jackson 2.

  slf4j's `Logger` is now a `@Container` whose log methods ignore their own modification. In 0.9.1 it was immutable.
- Analysis results now state every verdict explicitly.

## Changes that can affect you

- **The shipped results load by default.** The CLI and the Gradle and Maven plugins now load the JDK and library
  results that come with maddi when you name no `--preload-analysis-results-dirs`. `none` loads nothing, and
  `default` names the shipped results in a list of directories.
- **`-s` means `--source` only.** In 0.9.1 the short option `-s` was declared for both `--source` and
  `--preload-analysis-results-dirs`. Use the long option for the second.
- **Java entry points refuse Kotlin sources.** Before, they skipped them silently. Use `maddi-kotlin`, or
  `--skip-kotlin-sources`.

## Installing

Nothing about the coordinates changes, apart from the version.

| What | Where |
|---|---|
| Gradle plugin | `id("io.codelaser.maddi.analyzer") version "0.9.5"`, on the Gradle Plugin Portal |
| Maven plugin | `io.codelaser:maddi-mvnplugin:0.9.5`, on Maven Central |
| Annotations | `io.codelaser:maddi-annotation:0.9.5` and `io.codelaser:maddi-support:0.9.5`, on Maven Central (Apache-2.0) |
| Command line | `maddi-0.9.5.zip` and `maddi-kotlin-0.9.5.zip`, on the [GitHub release](https://github.com/CodeLaser/maddi/releases) |

The analyzer needs JDK 25 or later to run, and the Kotlin distribution needs JDK 26 or later. The IDE plugins
(IntelliJ, Eclipse, VS Code) are not published; build them from source in
[maddi-dist](https://github.com/CodeLaser/maddi-dist).

## For contributors: three repositories

On 2026-09-30 maddi was split by tier into three repositories:

- **maddi:** the base, with the CST, the front ends, the printers, the annotated APIs and the corpus tooling.
- **maddi-mod:** the analyses.
- **maddi-dist:** the CLI, the plugins and the IDEs.

maddi-dist compiles against the base only, and a build check enforces it. Build from source with the three checked out
side by side (see the [README](../../README.md#building-from-source)). Development is on JDK 27.

## Known limitations

- `--translate` refuses a project with Kotlin sources: there is no single Java program to translate.
- C# output is judged on compilation only; no corpus's tests run against it yet.
- `--translate` does not expose the printers' options yet.
- The Kotlin stdlib's annotated API is still smaller than the JDK's, and an uncontracted library call counts as
  modifying.

## Documentation

- The [README](../../README.md), rewritten for this release.
- [`kotlin-printing.md`](../../maddi-cst-print-kotlin/kotlin-printing.md) and
  [`csharp-printing.md`](../../maddi-cst-print-csharp/csharp-printing.md), for how the translations work.
- The [user manual](https://www.codelaser.io/maddi/manual/) and the book,
  [*The Road to Immutability*](https://www.codelaser.io/maddi/road/), which gains a chapter on nullability
  ([sources](../../road-to-immutability/src/docs/asciidoc/sections/085-nullability.adoc)): the definitions, what the
  analyzer computes and how, and the closed and open world.
