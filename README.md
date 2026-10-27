# maddi

[![build](https://github.com/CodeLaser/maddi/actions/workflows/build.yml/badge.svg)](https://github.com/CodeLaser/maddi/actions/workflows/build.yml)

**maddi is a whole-program analysis engine for Java and Kotlin.** At its core is a *linking engine*: for every
field, parameter, return value and local, it works out which objects flow where, and which of them share state. Three applications are built on the same facts:

- **Modification and immutability.** Which methods modify what. Which types are `@Immutable`, `@Container` or
  `@Independent`, at what level, and what stops them from going higher.
- **Nullability.** Which declarations can hold `null`, and, at every dereference, whether the value can be null
  there.
- **Translation.** Idiomatic Kotlin from a whole Java program, with nullability and read-only collections decided by
  the analyses. Java → C# is experimental. Java → Rust is an experiment: the modification and independence verdicts
  become Rust's ownership modes, and rustc's borrow checker judges them.

Java and Kotlin parse to the same, fully resolved, **eventually immutable** common syntax tree, on which the linking engine runs.

## Modification and immutability

Real code is rarely deeply immutable, and a yes/no verdict throws away everything useful. maddi computes a
**level**, so it can tell you how far you got and what stopped you.

```java
public final class Config {
    private final Map<String, String> settings;

    public Config(Map<String, String> settings) {
        this.settings = settings;          // (1)
    }

    public String get(String key) {
        return settings.get(key);
    }
}
```

> `@FinalFields @Container`: the field is final and nothing here modifies it, but the caller still holds the map it
> passed in. maddi sees the field and the parameter sharing one *modification component*, so anyone outside can
> still change this object's state after it is built.

Change line (1) to `Map.copyOf(settings)` and the verdict moves up a level:

> `@Immutable(hc=true) @Container`: after the copy, the only link left runs to the map's *elements*, not to the map
> itself. `hc=true` ("hidden content") records the one honest caveat: those elements are of a type parameter the
> `Config` class cannot see into. That is not hc-free `@Immutable`, but it is immutable from `Config`'s point of view.

A tool that answers "not immutable" for both versions has told you nothing about the difference between them.

The four levels are `@Mutable` → `@FinalFields` → `@Immutable(hc=true)` → `@Immutable`. Independence is a
separate axis. **Eventual immutability** covers the builder/freeze pattern (`@Mark`, `@Only`,
`@Immutable(after=…)`): types that are mutable while being constructed and immutable ever after. The booklet
[*The Road to Immutability*](https://www.codelaser.io/maddi/road/) develops the concepts in full.

## Nullability

The nullability pass can run after the modification analysis, on the same link facts. It decides, for every field,
parameter, method return and type argument, whether it can hold `null`. The annotated APIs that come with maddi
seed it, and they are generated from JSpecify's annotated JDK. A library marked `@NullMarked` is honoured.

Its verdicts are what the translations print: `T` or `T?` in Kotlin, `T` or `T?` under `#nullable enable` in C#,
and `!!` or `!` where the analysis knows a value is there but the types cannot say so.

## Translation

The same command line that analyses a project can translate its main sources to Kotlin or C#. Take this input:

```java
public class Inventory {
    public record Item(String name, int count) { }
    private final Map<String, Item> items = new HashMap<>();

    public Item find(String name) { return items.get(name); }

    public static int total(List<Item> list) {
        int sum = 0;
        for (Item item : list) sum += item.count();
        return sum;
    }

    public List<String> names() { return new ArrayList<>(items.keySet()); }
    // ...
}
```

`maddi --translate kotlin` prints, leaving out the same members:

```kotlin
open class Inventory {
    private val items: MutableMap<String, Item> = HashMap<String, Item>()

    open fun find(name: String): Item? = items.get(name)

    open fun names(): MutableList<String> = ArrayList<String>(items.keys)

    @JvmRecord data class Item(val name: String, val count: Int)

    companion object {
        @JvmStatic fun total(list: List<Item>): Int {
            var sum = 0
            for (item in list) { sum += item.count }
            return sum
        }
    }
    // ...
}
```

Kotlin has no static methods: the static `total` moves into the class's `companion object`. `maddi --translate csharp`
prints:

```csharp
public sealed class Inventory {
    private readonly IDictionary<string, Item> items = new Dictionary<string, Item>();

    public Item? Find(string name) => items.Get(name);

    public static int Total(IReadOnlyList<Item> list) {
        int sum = 0;
        foreach (Item item in list) { sum += item.Count; }
        return sum;
    }

    public List<string> Names() => new List<string>(items.Keys);

    public sealed record Item(string Name, int Count);
    // ...
}
```

`find` returns `Item?` because `Map.get` can return null and nothing in between excludes it. `total` gets a
read-only `List<Item>` and `IReadOnlyList<Item>` because the modification analysis shows it never modifies the
list. `names` returns a fresh list the caller owns, so it stays mutable.

The translation works on the whole program, not one file at a time. It is measured on six open-source corpora: each
is printed as Kotlin and compiled with `kotlinc`. The C# output is compiled with Roslyn.

| Java → Kotlin | files | compiling |
|---|---|---|
| Fernflower | 199 | 198 |
| LangChain4j core | 346 | 346 |
| Guava | 610 | 603 |
| Nacos client API | 274 | 271 |
| Jenkins core | 1,329 | 1,271 |
| Timefold Solver core | 1,583 | 1,361 |

| Java → C# (experimental) | files | compiling |
|---|---|---|
| Fernflower | 199 | 185 |
| LangChain4j core | 346 | 301 |
| Timefold Solver core | 1,583 | 1,324 |

### Java → Rust: the verdicts as ownership

Rust's rule is *aliasing XOR mutability*. The Road to Immutability splits the same idea in two: modification is the
mutability half, independence the aliasing half. So `maddi --translate rust` prints the verdicts as Rust's ownership
modes, and the same `Inventory` becomes:

```rust
impl Inventory {
    pub fn find(&self, name: &str) -> Option<InventoryItem> {
        return self.items.get(name).cloned();
    }
    pub fn total(list: &Vec<InventoryItem>) -> i32 { /* ... */ }
    pub fn names(&self) -> Vec<String> { /* ... */ }
    pub fn add(&mut self, name: &str, count: i32) { /* ... */ }
}
```

`find` and `names` are `&self` because they modify nothing; `add` is `&mut self`. `total` borrows its list, `&Vec`,
because it neither modifies nor keeps it. `Item` is `@Immutable`, so a copy of it cannot be told from the shared
object, and `find` hands out a clone.

The mapping goes further: a dependent field is a borrow with a lifetime, `@Finalizer` takes `self` by value, an
eventually immutable type is a typestate, `@IgnoreModifications` is a `RefCell`, `@Immutable(hc=true)` is shared as
an `Rc`. [CodeLaser/maddi#167](https://github.com/CodeLaser/maddi/issues/167) has the table, with a test for each
row.

The point is less the translation than the check. The printer does not soften anything with `clone()` or
`Rc<RefCell<…>>`, so when rustc rejects the output, a verdict and the code disagree, at the exact line: `&self` on a
method that modifies is E0596. The rejections found six analyzer bugs so far. The design note is
[`maddi-cst-print-rust/rust-printing.md`](maddi-cst-print-rust/rust-printing.md).

## Languages

- **Java** through javac's own front end, up to **JDK 27**. It is exercised on many open-source projects and one
  closed-source codebase of about 3M lines.
- **Kotlin** through the K2 compiler front end. Below the front end the stack is shared: prepwork, the linking
  engine and the analyzer treat a lowered Kotlin body like a Java one, with no Kotlin-specific paths. Test tiers of
  Kotlin/Java fixture pairs check that both get the same verdicts. Mixed Java/Kotlin projects are analysed as one
  program. The gap that remains is in the library contracts: the Kotlin stdlib's annotated API is smaller than the
  JDK's.

## Where it stands (0.9.5)

| Part | State |
|---|---|
| Concepts, and the book | Stable |
| Java front end | Robust, on JDK 27 |
| Kotlin front end | Close to parity with Java; ships in the `maddi-kotlin` CLI |
| Linking engine; modification and immutability analysis | Runs to a certified fixpoint on the proving-ground corpus (Timefold, LangChain4j, Fernflower, Guava, ActiveMQ, Jenkins, Camel) |
| Nullability | New in 0.9.5; reaches users through the translations |
| Java → Kotlin translation | New in 0.9.5; whole-program, measured on six corpora |
| Java → C# translation | New in 0.9.5; experimental |
| Java → Rust translation | Experimental; an oracle for the modification and independence verdicts |
| Gradle and Maven plugins, CLI | Published |
| IDE plugins (IntelliJ, Eclipse, VS Code) | Build from source only |

What changed since 0.9.1: [release notes](docs/project/release-notes-0.9.5.md).

The engine is stable on everything we run it on, and your codebase will contain Java the corpus does not. If maddi
mis-parses, crashes, translates wrongly, or computes something you can argue is wrong, please open an issue. That is
exactly the report we want.

## Get it

There is one engine and three ways to run it. They need JDK 25 or later to run; the Kotlin distribution needs JDK 26
or later. The details, the IDE plugins included, are in
[maddi-dist's README](https://github.com/CodeLaser/maddi-dist#readme).

**Gradle**: [`io.codelaser.maddi.analyzer`](https://plugins.gradle.org/plugin/io.codelaser.maddi.analyzer) on
the Gradle Plugin Portal.

```kotlin
plugins {
    java
    id("io.codelaser.maddi.analyzer") version "0.9.5"
}
```

**Maven**: `io.codelaser:maddi-mvnplugin` on Maven Central, goal prefix `maddi`.

**Command line, for any build system**: two self-contained zips on the
[latest release](https://github.com/CodeLaser/maddi/releases/latest). `maddi-<version>.zip` analyses Java;
`maddi-kotlin-<version>.zip` takes the same command line and adds Kotlin. To point the CLI at a project, capture
what the build compiled and hand it over:

```bash
./gradlew :your-module:compileJava --debug 2>&1 | grep 'Compiler arguments:' > build.log
maddi --compile-log build.log --analysis-steps modification --analysis-results-dir out
maddi --compile-log build.log --translate kotlin --translate-output kotlin-src
maddi --compile-log build.log --translate csharp --translate-output csharp-src   # with a Translated.csproj
```

The [user manual](https://www.codelaser.io/maddi/manual/) covers configuration, analysis hints, error reporting and
exit codes. [`maddi-run-openjdk/running-examples.md`](maddi-run-openjdk/running-examples.md) has more worked
examples.

## The annotations

The one thing your own code could compile against is the annotations library. It is on Maven Central, targets Java 17,
and is **Apache-2.0** licensed, so you can depend on it:

```kotlin
implementation("io.codelaser:maddi-annotation:0.9.5")   // Gradle
```

```xml
<dependency>                                            <!-- Maven -->
  <groupId>io.codelaser</groupId>
  <artifactId>maddi-annotation</artifactId>
  <version>0.9.5</version>
</dependency>
```

That gives you `@Immutable`, `@Container`, `@Independent`, `@Modified` and the rest, with **no dependencies at
all**. They are useful as documentation, and as contracts on your interfaces even before you run the analyzer, since
maddi verifies them against what it computes.

If you also want the "eventually final" support classes (`SetOnce`, `Freezable`, `EventuallyFinal`, `Lazy`,
`FirstThen`), depend on `maddi-support` instead. It declares exactly one dependency, on `maddi-annotation`, and
re-exports it:

```kotlin
implementation("io.codelaser:maddi-support:0.9.5")      // annotations arrive with it
```

> Versions up to `0.8.2` were LGPL-3.0; `0.9.0` onward is Apache-2.0.

## Documentation

| You want to… | Read |
|---|---|
| Understand the concepts (levels, modification, linking, independence) | [*The Road to Immutability*](https://www.codelaser.io/maddi/road/); in-repo: the [condensed digest](road-to-immutability/llm-summary.md) and the [book sources](road-to-immutability/src/docs/asciidoc/) |
| Run maddi on your own project | the [user manual](https://www.codelaser.io/maddi/manual/) ([sources](maddi-manual/src/docs/asciidoc/)) |
| Understand the translations | [`kotlin-printing.md`](maddi-cst-print-kotlin/kotlin-printing.md), [`csharp-printing.md`](maddi-cst-print-csharp/csharp-printing.md) |
| Understand the codebase (pipeline, module map, where to start) | [`ARCHITECTURE.md`](ARCHITECTURE.md) |
| Build, test, contribute | [`CONTRIBUTING.md`](CONTRIBUTING.md) |
| Work on it with an AI assistant | [`AGENTS.md`](AGENTS.md) / [`CLAUDE.md`](CLAUDE.md) |

Cross-module design notes and plans for all three repositories are indexed in [`docs/README.md`](docs/README.md).

## Three repositories

Since 2026-09-30 maddi is split by tier ([plan](docs/roadmap/split-maddi-into-three-repositories.md)):

| Repository | Holds | Depends on |
|---|---|---|
| **maddi** (this one) | the base: CST, the Java and Kotlin front ends, inspection, call graph, the run drivers, the printers (Java, Kotlin, C#), the `AnalysisEngine` interface, the annotated APIs, and `corpus/`, the corpus tooling | nothing above it |
| [maddi-mod](https://github.com/CodeLaser/maddi-mod) | the analyses: the linking engine, the modification analyzer, the nullability pass, the analysis-hints parser, and the corpus ratchets | the base only |
| [maddi-dist](https://github.com/CodeLaser/maddi-dist) | what users install: the CLI distributions, the Gradle and Maven plugins, the IDE daemon and plugins, `dogfood/` | compiles against the base only; carries maddi-mod at run time |

Modules named in this repository's documents that are not in this checkout live in those siblings, checked out next
to it (`../maddi-mod`, `../maddi-dist`).

## Building from source

Check the three repositories out side by side, then build in the one you need:

```bash
git clone https://github.com/CodeLaser/maddi.git
git clone https://github.com/CodeLaser/maddi-mod.git
git clone https://github.com/CodeLaser/maddi-dist.git

cd maddi && ./gradlew build                          # the base: compile + fast tests
cd ../maddi-dist && ./gradlew :maddi-cli:installDist # the `maddi` CLI, in maddi-cli/build/install/maddi
```

Development happens on JDK 27, with no Gradle toolchain provisioning: the JDK on `JAVA_HOME` builds. A first run of
the CLI you just built, on a directory of Java sources:

```bash
maddi-cli/build/install/maddi/bin/maddi --jmod=java.base --source=<dir> \
    --analysis-steps modification --translate kotlin --translate-output out
```

> Embedding the openjdk front end in your own code? Read
> [`maddi-inspection-openjdk/parsing-stability.md`](maddi-inspection-openjdk/parsing-stability.md) first: javac is
> not thread-safe, and that document is the authoritative guide to deterministic runs.

## Background

maddi re-implements [e2immu](https://www.e2immu.org), which ran from 2020 until it was archived. The root Java
package was `org.e2immu.*`, after the predecessor; it became `io.codelaser.maddi.*` in 0.9.1 (see
[`docs/project/release-notes-0.9.1.md`](docs/project/release-notes-0.9.1.md)).

maddi is developed by [Bart Naudts](mailto:bart.naudts@codelaser.io) at [CodeLaser](https://codelaser.io), accepts contributions, and is
and will remain open source. From 0.9.6 onward, the whole project is **Apache-2.0** licensed.
CodeLaser's commercial Refactor product is built on maddi. Questions, use cases and criticism are all welcome: mail, or open an issue.

___

(C) Copyright CodeLaser BV, 2020-2026.
