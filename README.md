# maddi: modification analysis for Java and Kotlin

[![build](https://github.com/CodeLaser/maddi/actions/workflows/build.yml/badge.svg)](https://github.com/CodeLaser/maddi/actions/workflows/build.yml)

**modification analysis** tells you where objects are modified. In well-designed code, modifications are _local_ and _limited to a construction phase_. Thus, the analyzer helps catching unexpected side-effects such as mutable state leaking to another module, or allowing data corruption in parallel threads. 

**maddi** is a static whole-program code analyzer for Java and Kotlin focusing on (im)mutability.
Its _linking engine_ traces relations between objects from statement to statement, in particular, as received from or passed on as arguments to other methods.
From these relations, it traces modifications and nullability. From the modification verdicts, it derives immutability levels. The booklet [*The Road to Immutability*](https://www.codelaser.io/maddi/road/) develops maddi's modification and immutability concepts in full.

The engine has other applications, notably

- _idiomatic_ translation from Java to Kotlin and C#
- enabling complex refactoring steps such as _code duplication removal_ and _interface extraction_ in the [*CodeLaser*](https://www.codelaser.io) product

In the age of code generation by LLMs, maddi's MCP server can play the role of guardian of the mutability bounds of your codebase. It can analyze all the code at once, and can trace contract violations or explain unexpected modifications far beyond the classes or packages in the current context.

 maddi's IntelliJ and Eclipse plugins allow you to view the modification, immutability and nullability verdicts as virtual annotations. 

<img
  src="docs/images/intellij-contract-violation.png#gh-light-mode-only"
  alt="Code snippet"
  width="600"
/>
<img
  src="docs/images/intellij-contract-violation-dark.png#gh-dark-mode-only"
  alt="Code snippet"
  width="600"
/>

In this example, `WrongContainer` claims `@Container`, but `fill` adds to the list it is given. maddi computes `@Modified` for the parameter, underlines the broken contract, and shows above the class what it does compute.


## How to get it

Install maddi as an **IDE plugin** from the 
- JetBrains Marketplace, or from the 
- Eclipse update site `https://codelaser.github.io/maddi-dist/eclipse/`.

The fastest way to install the **MCP server** is with 

```sh
brew install codelaser/tap/maddi-mcp
claude mcp add maddi -- $(brew --prefix)/bin/maddi-mcp
```

or the equivalent command of your favourite coding assistant.

maddi also runs as a **Gradle plugin**: [`io.codelaser.maddi.analyzer`](https://plugins.gradle.org/plugin/io.codelaser.maddi.analyzer) on the Gradle Plugin Portal; add

```kotlin
plugins {
    java
    id("io.codelaser.maddi.analyzer") version "0.9.6"
}
```

or as a **Maven plugin**: `io.codelaser:maddi-mvnplugin` on Maven Central, goal prefix `maddi`.

Finally, you can download the zip of the latest stable release from this page, and run maddi from the **command line**. There is a separate distribution that contains support for parsing Kotlin.

## Examples

Open the sibling projects `maddi-java-demo` or `maddi-kotlin-demo` to see most modification verdicts in action.
The following examples are taken from those projects.

### Kotlin in IntelliJ

...

### Java in Eclipse

...

### Java → Kotlin in IntelliJ

...

### Java → C#

### Java → Rust: the verdicts as ownership

Rust's rule is *aliasing XOR mutability*. [*The Road to Immutability*](https://www.codelaser.io/maddi/road/) splits the same idea in two: modification is the mutability half, independence the aliasing half. So `maddi --translate rust` prints the verdicts as Rust's ownership
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
method that modifies is E0596.


## Where it stands (0.9.6)

| Part | State |
|---|---|
| Concepts, and the book | Stable |
| Java front end | Robust, on JDK 25-27. Works with OpenJDK and Temurin. |
| Kotlin front end | Close to parity with Java; ships in the `maddi-kotlin` CLI; based on the K2 compiler. |
| Linking engine; modification and immutability analysis | Runs to a certified fixpoint on the proving-ground corpus (Timefold, LangChain4j, Fernflower, Guava, ActiveMQ, Jenkins, Camel) |
| Nullability | Reaches users through the translations and @Nullable/@NotNull annotations |
| Java → Kotlin translation | Whole-program, measured on six corpora |
| Java → C# translation | Experimental |
| Java → Rust translation | Experimental, mostly aimed at internal testing |
| Gradle and Maven plugins, CLI | Published |
| MCP server | New in 0.9.6 |
| IntelliJ plugin | New in 0.9.6 |
| Eclipse plugin | New in 0.9.6 |
| VS Code plugin | Build from source only, experimental |

What changed since 0.9.5: [release notes](docs/project/release-notes-0.9.6.md).

## The annotations

To add contract annotations to your code, link the annotations library. It is on Maven Central, targets Java 17, and is **Apache-2.0** licensed, so you can depend on it:

```kotlin
implementation("io.codelaser:maddi-annotation:0.9.6")   // Gradle
```

```xml
<dependency>  <!-- Maven -->
  <groupId>io.codelaser</groupId>
  <artifactId>maddi-annotation</artifactId>
  <version>0.9.6</version>
</dependency>
```

That gives you `@Immutable`, `@Container`, `@Independent`, `@Modified` and the rest, with **no dependencies at
all**.

If you also want the "eventually final" support classes (`SetOnce`, `Freezable`, `EventuallyFinal`, `Lazy`,
`FirstThen`), depend on `maddi-support` instead. It declares exactly one dependency, on `maddi-annotation`, and
re-exports it:

```kotlin
implementation("io.codelaser:maddi-support:0.9.6")      // annotations arrive with it
```

> Versions up to `0.8.2` were LGPL-3.0; `0.9.0` onward is Apache-2.0.

## Documentation

| You want to… | Read |
|---|---|
| Understand the concepts (levels, modification, linking, independence) | [*The Road to Immutability*](https://www.codelaser.io/maddi/road/); in-repo: the [condensed digest](road-to-immutability/llm-summary.md) and the [book sources](road-to-immutability/src/docs/asciidoc/) |
| Run maddi on your own project | the [user manual](https://www.codelaser.io/maddi/manual/) ([sources](maddi-manual/src/docs/asciidoc/)) |
| Understand the translations | [`kotlin-printing.md`](maddi-cst-print-kotlin/kotlin-printing.md), [`csharp-printing.md`](maddi-cst-print-csharp/csharp-printing.md) |
| Understand the codebase (pipeline, module map, where to start) | [`docs/design/ARCHITECTURE.md`](docs/design/ARCHITECTURE.md) |
| Build, test, contribute | [`CONTRIBUTING.md`](CONTRIBUTING.md) |
| Work on it with an AI assistant | [`AGENTS.md`](AGENTS.md) / [`CLAUDE.md`](CLAUDE.md) |

Cross-module design notes and plans for all three repositories are indexed in [`docs/README.md`](docs/README.md).

## Building from source

Check the three repositories out side by side, then build in the one you need:

```bash
git clone https://github.com/CodeLaser/maddi.git
git clone https://github.com/CodeLaser/maddi-mod.git
git clone https://github.com/CodeLaser/maddi-dist.git

cd maddi && ./gradlew build                          # the base: compile + fast tests
cd ../maddi-dist && ./gradlew :maddi-cli:installDist # the `maddi` CLI, in maddi-cli/build/install/maddi
```

Development happens on JDK 26 and 27.

___

(C) Copyright CodeLaser BV, 2020-2026.
