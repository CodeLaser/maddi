# maddi 0.9.6

**maddi comes to the IDE and to coding agents.** The IntelliJ and Eclipse plugins are published for the first time:
the computed annotations appear in the editor, where you would write them, and the gutter says *why* each verdict
holds. A new MCP server gives a coding agent the same analysis: it can configure a project, analyse it and ask
about the verdicts. Many fixes and improvements to the analyses and the translators come with them.

And **maddi is now Apache-2.0, all of it.** Until 0.9.5 only the annotations were; the analyzer was
LGPL-3.0-or-later. Contributions are now made under a Contributor License Agreement, which replaces the DCO.

Code that compiles against the 0.9.5 annotations compiles against 0.9.6 unchanged. Some verdicts change; see
[Changes that can affect you](#changes-that-can-affect-you).

## IDE plugins

- **Published for the first time**: the IntelliJ plugin on JetBrains Marketplace (*maddi*), and the Eclipse plugin as
  an update site, `https://codelaser.github.io/maddi-dist/eclipse/`, listed on the Eclipse Marketplace. Both bundle
  the analysis daemon and run it on a JDK 25 or later, which they pick from the IDE's own JDKs. Nothing leaves your
  machine: the plugin talks to its daemon over a loopback socket.
- **Annotations where you would write them**: on the line above a declaration, and before a parameter's type. An
  annotation you wrote is not shown again, and one implied by another or by its context (`@Independent` beside
  `@Fluent`, `@FinalFields` on an interface) is hidden unless you ask for all of them.
- **The gutter says why.** The tooltip of a type, method or field has its full set of annotations, its parameters'
  included, and under each annotation that has one the chain of statements behind it, from the element to the
  statement that decides it: `⇐ line 9: calls store(..)`, `⇐ Store.java:12: calls items.add(..)`.
  - A method, field or parameter that is `@Modified`, `@Dependent` or `@Nullable`, and a method with
    `@StaticSideEffects`.
  - A type below `@Immutable`: the rule it fails, at the member that fails it, for `@Mutable`, `@FinalFields` and
    `@Immutable(hc=true)` alike.
  - A type that is not `@Container`, when it is close: at most 2 parameters modified, each with the statement that
    modifies it, under `not @Container:`.
- **Contracts are checked as you work.** A contract you wrote that the code breaks is marked where it is broken, with
  the reasons. A modifying abstract method is now reported at the implementation that modifies, not at the abstract
  method.
- **Library sources show the analysis's annotations**, and say whether they come from maddi's annotated API or are
  the default it assumes.
- **Results while the run goes on**: values appear as they are decided, the progress line says which pass is
  running, and a run can be stopped. Changes made during a run start one more run after it, instead of being
  dropped. The daemon exits with the IDE.
- **IntelliJ: Translate to Kotlin with maddi.** maddi's own Java-to-Kotlin translator, from the IDE: a preview of
  each translation beside its Java, or a whole module translated and, after a confirmation, replaced, as one undoable
  command. IntelliJ also analyses Kotlin sources, with the Kotlin front end downloaded on demand.
- **The Eclipse plugin caught up with IntelliJ** for Java: per-project source sets with their own class paths, the
  JDK from *Installed JREs*, a build before each analysis, a Findings view with progress and a run log, ruler icons
  with their own icon for eventual verdicts, and underlined contract violations.
- **The daemon runs the same passes as the command line** (reachability and nullability), so the IDE and the CLI
  agree.
- **A [user guide](https://github.com/CodeLaser/maddi-dist/blob/v0.9.6/docs/ide-user-guide.md)** covers setup,
  reading the results, library sources, translating and the settings. It is also the front page of the Eclipse
  update site.

## MCP server for coding agents

`maddi-mcp` is an [MCP](https://modelcontextprotocol.io) server: a coding agent such as Claude Code uses it to run
maddi and ask about the results. It drives the same daemon as the IDE plugins, so the agent sees what the IDE shows.

- **Configure a project.** A Gradle build by its directory alone, with no maddi plugin; an `inputConfiguration.json`
  written by the Gradle or Maven plugin or the CLI; or directories and a class path. The configuration is stored with
  the hashes of the build files that shaped it, and computed again only when one of them changes.
- **Analyse all of it or part of it**, by source set or package. A run goes on in the background: the agent follows
  its progress, asks for the values decided so far, or stops it.
- **Ask about the results**:
  - `overview`: a project, package or type at a glance;
  - `verdicts` and `explain`: the annotations, the properties behind them, why they hold (the same chains of
    statements as the IDE's tooltips, and why a type is not `@Container` for every type, not only close ones), and
    the findings about an element;
  - `element_at`: what is at a file and line;
  - `diff_runs`: what changed between two runs, the question after an edit;
  - `library_annotations`: what the run knows of JDK and library types.
- **Every answer says how far it can be trusted**: only `FINAL` values are certified.
- **Runs and configurations survive a restart**, the last 8 runs per project.
- **A project can name the JDK its daemon runs on**, for an annotation processor such as Lombok that fails on a newer
  JDK.

To install it on macOS or Linux, `brew install codelaser/tap/maddi-mcp`, which brings its JDK along, and register it
with the agent: `claude mcp add maddi -- $(brew --prefix)/bin/maddi-mcp` for Claude Code. Elsewhere, download
`maddi-mcp-0.9.6.zip` from this release, unzip it and register its `bin/maddi-mcp`. For Claude Desktop,
`maddi-mcp-0.9.6.mcpb` is the same server as an MCP bundle: open it, and name a JDK when it asks. It needs a JDK 25 or later; the
[README](https://github.com/CodeLaser/maddi-dist/blob/v0.9.6/README.md#coding-agents-mcp) has the options. What comes next,
asking what an edit would do to the verdicts and whether it would break a contract before making it, is planned in
[`mcp-server.md`](https://github.com/CodeLaser/maddi-dist/blob/v0.9.6/docs/roadmap/mcp-server.md).

## Licence: Apache-2.0 throughout

- **What changes.** Every module of [maddi](https://github.com/CodeLaser/maddi),
  [maddi-mod](https://github.com/CodeLaser/maddi-mod) and [maddi-dist](https://github.com/CodeLaser/maddi-dist) is
  licensed under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0). Each repository has
  a `LICENSE` file in place of `COPYING` and `COPYING.LESSER`. The file headers, the published POMs, the VS Code
  extension's `package.json` and the Eclipse feature all say so.
- **The copyright holder is CodeLaser BV**, Belgium. The notices read `Copyright 2020-2026, CodeLaser BV`; until 0.9.5
  they named Bart Naudts, who develops maddi at CodeLaser.
- **Why.** Most people run maddi; they don't ship it. The LGPL's obligations only start when someone distributes the
  analyzer inside their own product, so in practice it protected nobody, while it did stop companies whose policies
  rule out anything GPL-family from building on maddi at all. maddi is also a library: its syntax tree, front ends
  and analysis engine are meant to be built upon. Apache-2.0 is the licence a Java shop already has on its approved
  list, and it carries an explicit patent grant.
- **What it means for you.**
  - *Using the annotations* (`maddi-annotation`, `maddi-support`): nothing changes. They have been Apache-2.0 since
    0.9.0.
  - *Running the analyzer* (CLI, Gradle or Maven plugin, IDE plugin): nothing changes in practice; the LGPL did not
    restrict that either.
  - *Building on maddi as a library, or embedding it in something you distribute*: you may now do so under the usual
    Apache-2.0 terms (keep the `NOTICE` and the licence text) and without the LGPL's obligations to provide maddi's
    source and to allow relinking.
- **Earlier versions keep their licence.** 0.9.5 and earlier were released under LGPL-3.0-or-later, and the copies
  already out there stay under those terms (the annotations from 0.9.0 onward excepted, which were Apache-2.0
  already). Apache-2.0 applies from 0.9.6. If you need the permissive terms for the analyzer, upgrade.
- **The Maven coordinates, package names and plugin ids do not change.**

## For contributors: a CLA replaces the DCO

- Pull requests no longer need `Signed-off-by` lines. Instead you sign the
  [CodeLaser Contributor License Agreement](../../CLA.md) once, by replying to the bot on your first pull request.
  One signature covers maddi, maddi-mod and maddi-dist.
- **You keep the copyright** in your contribution; CodeLaser BV receives a licence to it, including the right to
  license it under other terms, because maddi is also offered under commercial licences. In return CodeLaser
  promises to keep distributing your contribution under the free licence it was made under.
- Contributing for an employer or as a company: someone who can sign for it sends the agreement to
  cla@codelaser.io, as its [§9](../../CLA.md#9-how-to-accept) explains. See
  [CONTRIBUTING.md](../../CONTRIBUTING.md).

## Modification and immutability analysis

- **Why a verdict holds is recorded**, when a run asks for it (the IDE daemon and the MCP server do): the
  statements behind a `@Modified`, `@Dependent`, `@Nullable` or `@StaticSideEffects`, why a type stops below
  `@Immutable`, and why it is not `@Container`. These are what the IDE tooltips and the MCP server's `explain` show.
- **Static side effects are computed by default.** A method that modifies static state gets `@StaticSideEffects`;
  instance field initializers and method references count, and `@IgnoreModifications` excuses a field or method.
- **A method that modifies its own type's static state is `@Modified`**, as the book says: a call counter in
  `Circle.area()` makes it modifying. Another type's static state stays a static side effect.
- **Contract violations are reported at their culprits**: rule 1 and rule 3 violations, and `@Independent` ones, at
  the statement that causes them; a field exposed through a view (`items.subList(..)`) blames the getter.
- **Link and reachability fixes**, among them:
  - a reassigned local, field or array slot keeps none of its old links;
  - a self-referential reassignment keeps the old value's links;
  - an element handed to a function passed in leaves its container unmodified;
  - a receiver chain that leads back to itself ends;
  - a subtype of `Iterator` yields many elements, as `Iterator` does.
- **`@UnmodifiableView`**: at a call it answers for every override the call may reach; `null` is neutral.
- **Faster re-linking**: a method's links are not computed again when nothing they read changed.

## Nullability analysis

- Many fixes to when a parameter tolerates `null` (overloads, overrides, overridable methods, a parameter annotated
  non-null), to element nulls in arrays and collections, and to Kotlin's nullable instantiations, measured
  against an oracle on Guava.

## Annotated APIs

- **The JDK's mutators are `@Modified` and its queries `@NotModified`**, stated rather than left to the default the
  analyzer assumes. `java.lang`'s queries and their arguments are `@NotModified`.
- **`Iterator.hasNext()` and `Enumeration.hasMoreElements()` are `@Modified`**: a contract for lazy implementations,
  which advance in `hasNext()`.
- Views and copies as the JDK makes them: `Arrays.asList` is dependent, `TreeMap.firstEntry()` and its kind return
  snapshots, and `listIterator()` and `descendingIterator()` write through only where
  the JDK does. The value-based classes are immutable.
- The analysis results now say which verdicts of a library method or parameter are only the default assumption.
  The IDE uses it to tell you so.

## Translations

- **Java → Rust (experimental).** `maddi --translate rust` prints the modification and independence verdicts as
  Rust's ownership modes (`&self` or `&mut self`, borrows with lifetimes, `Rc`, `RefCell`), and rustc's borrow checker
  judges them: when it rejects the output, a verdict and the code disagree, at that line. The mapping is in
  [#167](https://github.com/CodeLaser/maddi/issues/167), and the printer's design in
  [`rust-printing.md`](../../maddi-cst-print-rust/rust-printing.md).
- **Java → Kotlin.** All 610 of Guava's files now compile (603 in 0.9.5), after fixes for raw types, wildcards and
  nullable type arguments. `apply { … }` for construct-configure-return, and a record deconstruction becomes an `is`
  test with a guard on its components, not an `else` arm. A case with several patterns and a guard has no Kotlin
  form; the printer reports it and keeps the Java.
- **Java printer**: qualifiers kept where they are needed (an `Outer.this` on a call, a superinterface's static
  method, a nested type whose simple name another type took), text block escapes written again, a constructor call's
  type arguments imported, a type's annotations qualified as its imports are
  ([#168](https://github.com/CodeLaser/maddi/issues/168), [#169](https://github.com/CodeLaser/maddi/issues/169),
  [#175](https://github.com/CodeLaser/maddi/issues/175)–[#178](https://github.com/CodeLaser/maddi/issues/178)).

## Java front end

- **A case label with several patterns keeps all of them.** Java 22 allows `case Circle _, Square _ ->` (JEP 456).
  Both front ends kept only the first pattern, so the analysis and the translators saw nothing of the others
  ([#171](https://github.com/CodeLaser/maddi/issues/171)).
- **A type of a source set's own sources never resolves to another set's copy** of the same file
  ([#179](https://github.com/CodeLaser/maddi/issues/179)).
- K2: a secondary constructor's own body is converted ([#174](https://github.com/CodeLaser/maddi/issues/174)).

## Changes that can affect you

- **Some verdicts change**, all towards what the code does:
  - a method that modifies its own type's static state is `@Modified`, and a type with such a method can drop below
    `@Immutable`;
  - `@StaticSideEffects` appears, since static side effects are computed by default;
  - an `Iterator` implementation's `hasNext()` is held to `@Modified`, and JDK mutators and queries are stated rather
    than assumed.
- **The licence is Apache-2.0** from this release; see above.
- **Contributions need the CLA**, not a `Signed-off-by` line.
- **The IDE plugins and their daemon must have the same version.** The daemon protocol is now checked at the
  handshake, and a mismatch is refused with a message.

## Installing

Nothing about the coordinates changes, apart from the version. New in 0.9.6: the IDE plugins, the MCP server, and a
[Homebrew tap](https://github.com/CodeLaser/homebrew-tap).

| What | Where |
|---|---|
| IntelliJ plugin | *maddi*, on [JetBrains Marketplace](https://plugins.jetbrains.com/) |
| Eclipse plugin | the update site `https://codelaser.github.io/maddi-dist/eclipse/`, listed on the Eclipse Marketplace |
| Gradle plugin | `id("io.codelaser.maddi.analyzer") version "0.9.6"`, on the Gradle Plugin Portal |
| Maven plugin | `io.codelaser:maddi-mvnplugin:0.9.6`, on Maven Central |
| Annotations | `io.codelaser:maddi-annotation:0.9.6` and `io.codelaser:maddi-support:0.9.6`, on Maven Central |
| Command line | `maddi-0.9.6.zip` and `maddi-kotlin-0.9.6.zip`, on the [GitHub release](https://github.com/CodeLaser/maddi/releases) |
| MCP server | `maddi-mcp-0.9.6.zip`, on the [GitHub release](https://github.com/CodeLaser/maddi/releases); `maddi-mcp-0.9.6.mcpb`, the same as an MCP bundle for Claude Desktop; on the [MCP Registry](https://registry.modelcontextprotocol.io) as `io.github.codelaser/maddi-mcp` |
| Homebrew (macOS, Linux) | `brew install codelaser/tap/maddi`, `codelaser/tap/maddi-kotlin` or `codelaser/tap/maddi-mcp`: the command line or the MCP server, with the JDK it runs on |

The analyzer needs JDK 25 or later to run, and the Kotlin distribution needs JDK 26 or later. The IntelliJ plugin
downloads the Kotlin front end from this release's `maddi-kotlin-0.9.6.zip` when a project has Kotlin sources.

## Known limitations

- The VS Code extension is not published yet, and shows no explanations.
- The Eclipse plugin analyses Java only.
- The explanations are recorded only for the IDE and the MCP server; the command line does not show them yet.
- Java → C# and Java → Rust are experimental: judged on compilation only.

## Documentation

- The [IDE user guide](https://github.com/CodeLaser/maddi-dist/blob/v0.9.6/docs/ide-user-guide.md) and the
  [IDE change notes](https://github.com/CodeLaser/maddi-dist/blob/v0.9.6/docs/ide-changelog.md).
- The MCP server: the [README section](https://github.com/CodeLaser/maddi-dist/blob/v0.9.6/README.md#coding-agents-mcp)
  and the [roadmap](https://github.com/CodeLaser/maddi-dist/blob/v0.9.6/docs/roadmap/mcp-server.md).
- [`rust-printing.md`](../../maddi-cst-print-rust/rust-printing.md), for the Rust translation.
- The [user manual](https://www.codelaser.io/maddi/manual/) and the book,
  [*The Road to Immutability*](https://www.codelaser.io/maddi/road/).
