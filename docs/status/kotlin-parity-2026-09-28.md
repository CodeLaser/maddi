# Kotlin parity with Java: state and remaining work (2026-09-28)

**Question:** how far is maddi from treating Kotlin as it treats Java, and what work is left?

**Verdict.** The analysis stack below the front end is shared and has no Kotlin-specific paths: prepwork, link and
the analyzer treat a lowered Kotlin body like a Java one, and every Kotlin-vs-Java verdict fixture written so far
agrees. The distance to parity is in *trust*, not structure:

- the front end still converts some shapes silently wrong, and only differential fixtures find them;
- the Kotlin stdlib contracts are about 38% of the JDK's by member count, and an uncontracted library call is a
  modifying one by default;
- one known unsound hole in the link engine (#65);
- the shared modules hold no Kotlin-input tests of their own;
- the entry points around the analysis (build plugins, IDEs, incremental analysis) are Java-only.

This document updates `docs/kotlin-parity-study-2026-09-26.md`. That study remains the reference for the
structure of the stack (its §1, §3–§5); where the two disagree, this one is newer. Numbers are the latest recorded
ones (commit messages, issues, the archive files); nothing was re-measured for this document.

---

## 1. What changed since the 2026-09-26 study

- **Silent-wrong shapes.** The study's probe found six shapes that convert with zero placeholders and mean
  something else, and a seventh, milder one. Six are fixed and closed: local `val x by lazy {}` (#52), enum
  entries with constructor arguments and bodies (#53), class delegation `: I by d` (#54), `++` on a type with
  `operator fun inc()` (#55), evaluation order of named arguments (#56), a `when` arm with several `is` tests
  (#57). The non-local return (#58) is modelled in the CST (`ReturnStatement.exitLevels()`, e2fcc08ac) and
  guarded in the analyzer, but the link engine does not use it yet (#65).
- **`when` as a value** yields its arms' values, as a Java switch expression does (3791920d5), including a
  `when` at the tail of a try used as a value (04e0f0ddc).
- **Kotlin stdlib archive:** 14 types / 31 annotated members became **38 types / 2,161 members**
  (`libs/kotlin/*.json`, 9 files). The JDK archive has 249 types / 5,617. `@InlineOnly` stdlib members, which have
  no JVM method a contract could name, are lowered to the body kotlinc inlines (f8533c58d, 1272c2146,
  58ccd7f78). `ReadOnlyProperty.getValue` is read-only (226e06d48).
- **Closed front-end and prep issues:** #32–#48 (prep isolation on detekt, run-to-run instability, the K2
  session leak, abstract functions and properties, override families, data classes, imports, enum-entry and
  Java-declaration references, companion references).
- **Instruments:** `-Dmaddi.libraryCallDump` (every `kotlin.*` member called, and whether a contract reached
  it), `-Dmaddi.memberVerdictDump`.

## 2. State by layer

| Layer | Shared code handles Kotlin? | Evidence | Gap |
|---|---|---|---|
| Front end (K2) | ~90% of constructs converted | 400+ unit tests; placeholder ratchets on detekt and coil | an unknown residue of silent-wrong conversions; 64 placeholder kinds |
| Prepwork | yes, no Kotlin-specific paths | 21 test files in `inspection-kotlin/prepwork` assert the Java tests' `VariableData` strings; since this document, a Kotlin test tier inside the module (§5) | synthesized accessors, `$default`, `<init>` bodies |
| Link | yes in code | since this document, a Kotlin tier inside the module (§6); ~14 fixtures in `run-kotlin` | #65 non-local returns; #80 `FunctionN` takes the custom-functional-interface path; #78 library collection links (Java too); #83 cross-parse state |
| Analyzer | yes (3 Java-specific names) | since this document, a Kotlin tier inside the module (§7); 7 fixture pairs in `run-kotlin`, one detekt corpus floor | #87 `toList`/`listOf` not recognised as copies; #84 fields assigned in an `init` block undecided (Java too); #88 `apply { }` hides modification of `this` |
| Stdlib contracts | ~38% of the JDK's member count | per-family differential tests (`TestKotlinCollectionReadsVsJava`) | §3.2 |
| Persistence | write and read back | `TestKotlinAnalysisRoundTrip` | incremental analysis, rewire, `--analysis-results-target-dir`, `--updated-hints-dir` refused by name |
| Entry points | the `maddi-kotlin` CLI | — | Gradle/Maven plugins refuse (or skip with `skipKotlinSources`); the IDE daemon reports Kotlin as a problem; IntelliJ, VS Code, Eclipse are Java-only |

## 3. Remaining work, in order

> **Update (end of 2026-09-28).** Of the issues below, #68, #69, #72, #73, #74, #75, #77, #79, #81–#87, #90 and #92 are
> closed; see the "Fixed since" note in §6. New: #93 (link, SAM path of a forwarder), #94 (engine: a lambda's write
> through a captured holder is not seen, Java too; the remaining half of #72).

### 3.1 Soundness: Kotlin input that gets a wrong verdict

- **#65, link engine.** The value of a non-local return never reaches the enclosing method's return variable, so
  the analyzer can call a result independent of an input it depends on. About 120 sites on detekt, 34 on
  javalin, 9 on coil (a rough scan). The only known unsound hole; design first.
- **#69.** A statement lowered into two (`val v = try …`, `val v = if (…) { …; a } else …`, the control-flow
  elvis) is indexed `<i>.0`/`<i>.1`, and prepwork forgets `v` at `<i+1>`. Found by a downstream transform; the
  effect on the modification analysis is not measured. Size M.
- **#68.** In a mixed project, a call from a Kotlin body to a member of a Java-source class is a
  `k2-unresolved-call` placeholder, and its arguments go with it. Size S–M.
- **A standing silent-wrong hunt.** Every new comparison against compiled Kotlin found more: six in the
  09-26 probe, five in a downstream kotlinc-vs-transform differential check on 09-27. The placeholder census
  cannot see these by construction. What is missing is a standing family of differential fixtures per
  construct family (Kotlin vs hand-written Java), not per corpus site.
- **Placeholder floor.** detekt 9 against a pin of 8 (the ratchet in `maddi-run-kotlin:slowTest` is red; the
  09-26 commits still report 8, and the commit that added the ninth is not identified), coil 4, javalin 13
  and unpinned.
- **#72, #74, #75** (found by the prepwork tier, §5): a `var` assigned in a lambda is not assigned in the
  enclosing method; a local assigned or read in a switch expression's arm does not reach the enclosing statement
  (Java too); the arms of a `when` used as a value are indexed from 0 instead of under their statement.
- **Found by the link tier (§6):** #77 a synthesized property setter cannot be linked (NPE); #81 a data class's
  `copy()` with an omitted argument passes `null`; #82 a property with a custom setter, written from outside, is a
  field write (the setter never runs); #78 `toList`/`toSet`/`filter`/`asSequence().first()` link to nothing and
  library vararg calls link only their last argument (Java too); #79 a Java pattern variable in a conditional
  expression links to nothing (Java); #83 link results of one parse carry a static-call scope from an earlier
  parse in the same JVM (Java too).
- **Found by the analyzer tier (§7):** #85 a property initializer reading a constructor parameter is outside the
  constructor, so the parameter reads `@Independent` (unsound); #84 a field assigned in a nested constructor block
  (every Kotlin `init` block) never gets `INDEPENDENT_FIELD` (Java too); #86 a `vararg val` constructor property's
  parameter is typed as the element; #88 a modification inside `apply { }` is not a modification of the receiver,
  so a builder method reads as non-modifying (unsound); #87 `toList()` is not a recognised copy; #89 tracks the
  stdlib contract gap (`xs.sum()` modifies `xs`); #73 now measured for a companion `var` (mutability moves to the
  companion); #90 class delegation never assigns `$$delegate_0` in the constructor, so the type reads `@Independent`
  of its delegate (unsound); #92 a callable reference (`sb::append`) is typed `KFunction<A,R>`, not a functional
  interface, so its link loses the lambda marker.
- **#67.** Kotlin printed as Java does not compile: item 1 is a real placeholder (a comparison on a smart-cast
  value), item 2 a wrong type (`is Int` as `instanceof int`); items 3 and 4 may be intended.

### 3.2 Stdlib knowledge: the largest verdict-level gap

`ShallowMethodAnalyzer` reads a method with no body and no contract as modifying its receiver and every
non-trivial parameter. Not contracted: `Result`, coroutines and `suspend` (no contract mentions `Continuation`),
`Unit`, `Char` extensions, the remainder of the text and map extensions, and every library outside
kotlin-stdlib. The contract language (`AnalysisHintsParser`) has no notion of a property, so a getter contract
on a library property does not reach a Kotlin caller. Last measurement (09-26, before the later archive
additions): 566 of detekt's 3,151 stdlib calls read as modifying, 426 of them `getValue`/`invoke`; `getValue`
has since been contracted. Neither CLI loads an archive by default, for Java or Kotlin.

### 3.3 Evidence inside the shared modules

prepwork, link and the analyzer hold ~1,000 Java-input tests and, before §5, zero Kotlin-input ones.

- prepwork: a Kotlin tier covering the main topics (§5); synthesized members at the `doPrimaryTypes` level.
- link: the `FunctionN` path is covered by §6 (`TestKotlinLinkLambdas`: own higher-order functions, lambdas as
  values, SAM conversion to the JDK all agree; a bound callable reference is mistyped, #92); the decision whether
  `kotlin.jvm.functions` is standard is still #80.
- analyzer: `$default`, synthesized accessors and delegation forwarders are covered by §7 (delegation: #90); an empty synthesized body reads as
  "modifies nothing" (`explicitlyEmptyMethod()`).

### 3.4 Analyzer rules that name Java

`immutableCopyExpression` accepts only `List/Set/Map.copyOf|of`: Kotlin's `listOf`/`toList`/`toSet` are not
recognised defensive copies. `isPrimitiveStream` is `java.util.stream.*` only. Small, but both move verdicts.

### 3.5 Persistence, incremental analysis, IDE

Writing and reading results back works. Incremental analysis needs fingerprinting and rewiring for Kotlin; the
IDE daemon depends on it.

### 3.6 Entry points

Route the Gradle plugin to the mixed pipeline, then Maven. The open question is the size of the bundle (~85 MB
of K2). The IDE clients follow §3.5.

### 3.7 Tooling

- `IsolateClass` writes reproducers as `.java`; `maddi-cst-print-kotlin` is wired into no main code.
- Reference recall on detekt: 70.8% EXACT on 2026-09-25, with import, annotation and named-argument sites
  dropped. Six recall issues (#41, #42, #45–#48) closed since; not re-measured, and no test pins a percentage.

### 3.8 Decisions pending

- **#58:** non-local returns in the front end: the current marker, a refusal, or inlining the scope functions
  (125 detekt sites under `analyze{}`).

### 3.9 Stale documents

The 09-26 study (§7) lists about ten stale claims in `kotlin-parser-plan.md`, `kotlin-corpora.md`,
`mixed-language-integration.md`, the README, `PUBLISHING.md` and `release-cli.sh` (`lib/` vs `lib-k2/`).

## 4. The shortest path

1. #65 and #69 (soundness), and a standing silent-wrong fixture family.
2. Stdlib contracts to the point where an uncontracted Kotlin call is rare, loaded by default.
3. The Gradle plugin on the mixed pipeline.

Everything else is structurally in place and needs evidence rather than code.

## 5. The Kotlin tier in `maddi-modification-prepwork` (added 2026-09-28)

Package `io.codelaser.maddi.modification.prepwork.kotlin`, 43 tests in 6 classes, part of the module's ordinary
`test` task (about 5 s on top of the Java tests). Every fixture is a pair: a Kotlin class `k.X` and the Java
class `j.X` that says what kotlinc makes of it. Both go through **one** `MixedProjectInspector` parse, the
production path (one runtime, the JDK read from bytecode, kotlin-stdlib on the class path), with a
zero-placeholder guard, and prep runs over every primary type. The assertion is differential: for a method that
exists on both sides, every local and parameter must have the same definition, assignments and reads. Where the
two sides differ for a reason that is filed, the test pins both sides and names the issue, so it fails the day
the issue is fixed.

`KotlinScan` alone was not used: without the Java front end's `CompiledTypesManager`, K2 builds library types
from its own symbols, a model no production run uses; `s.length` does not even resolve there.

| Class | Topic | Agrees with Java | Pinned divergence |
|---|---|---|---|
| `TestKotlinAssignments` | if/else (statement, value), compound assignment, `when` (statement, value, subject-less), elvis, `!!`, string templates, arrays | 11 of 13 | #74 (arm assignments/reads, Java too), #75 (arm indices); subject-less `when` is a switch with conditional entries, not an if-chain (a shape difference, not a defect) |
| `TestKotlinLoops` | for-in over collections and arrays, while, do-while, `while (true)`, break/continue, labeled jumps | 7 of 7 | — |
| `TestKotlinTry` | try/catch/finally, several catches, rethrow, try as a value, `val v = if (…) { …; a } else …`, both inside a loop | 4 of 7 | #69 (the local of a two-statement lowering is lost at the next statement) |
| `TestKotlinLambdas` | captured reads, a `var` assigned in a lambda, non-local return, local `fun`, `?.let { } ?:` | 2 of 5 | #72, #65 (prep side), #69 (null-safe hoisting) |
| `TestKotlinEscapes` | `error()`, `TODO()`, `throw` in a `when` arm, `?: return`, `?: throw` | 2 of 5 | #75, #69 (control-flow elvis) |
| `TestKotlinTypeLevel` | call graph and analysis order; part of construction (init block, secondary constructor); final fields; getter/setter classification of property accessors, `componentN`, `lateinit`, `@JvmField`, objects; bodies of delegation forwarders, `$default` bridges, `copy`; the members with empty bodies | pinned values | #73 (a companion's `const val` modelled twice), #90 (the delegation constructor is empty) |

What the tier found on its first run:

- **#69 has four shapes, not one.** `val v = try …`, `val v = if (…) { …; a } else …`, `val t = s ?: return`
  (and `?: throw`), and `val n = s?.let { … } ?: 0`. In each, the method's variable data has no `v`/`t`/`n`
  at all, and the statement that uses it records no read.
- **#72:** a `var` assigned in a lambda (kotlinc: an `IntRef`) is assigned only in the lambda's own
  variable data.
- **#74, a Java defect:** an assignment inside a switch expression's arm is not an assignment of the enclosing
  statement; reads of locals in the arms are not recorded (Java), or are recorded through #75's colliding
  indices (Kotlin).
- **#65 at the prep level:** a non-local `return it` assigns the lambda's return variable; the enclosing method's
  return variable is assigned by its own `return` only.

Not covered yet: synchronized/`use {}`, destructuring declarations, `for ((k, v) in map)`, coroutines, and a
`doPrimaryTypes`-level run over a corpus slice. The Java tests' `CommonTest` classes that were ported in
`maddi-inspection-kotlin/prepwork` (21 files, same `VariableData` strings) remain where they are.

## 6. The Kotlin tier in `maddi-modification-link` (added 2026-09-28)

Package `io.codelaser.maddi.modification.link.kotlin`, 41 tests in 8 classes, in the module's ordinary `test`
task. Same design as §5: one mixed parse per fixture pair, the annotated JDK and `libs/kotlin` results loaded as in
the Java tests' `CommonTest`, prep, then `LinkComputerImpl` (`Options.TEST`). A method's `MethodLinkedVariables`
prints without fully qualified names, so the Kotlin and Java strings compare directly, in a normal form (each
parameter's links sorted: K2 and javac insert the two links of a constructor parameter in opposite order).

**Harness fix, both tiers:** kotlin-stdlib must be a class-path *part* and a *dependency* of the Java source set as
well as the Kotlin one. As a plain class-path string it reached K2 only; javac then stubbed every `kotlin.*` type
memberless and silently dropped a Java file that called one (#76 describes what the stub does to K2).

| Class | Topic | Agrees with Java | Pinned divergence |
|---|---|---|---|
| `TestKotlinLinkFields` | primary constructor, synthesized getters, setter, collection field read/modified/viewed, another instance's field | 5 of 6 | #77 (synthesized setter throws) |
| `TestKotlinLinkConditionals` | identity, if/when/elvis values, reassignment, cast, smart cast, arrays, varargs | 6 of 8 | #67 item 3 (smart cast loses `§m≡`), #79 (Java ternary pattern) |
| `TestKotlinLinkCollections` | index, copy constructor, maps, for-in, `listOf`, `first`, `toList`/`toSet`/`filter`, `mutableListOf` | 4 of 6 | #78 (library copies link nothing; vararg last-argument) |
| `TestKotlinLinkFunctions` | function types vs Java `Function1` and `java.util.function`, non-local return, `var` assigned in a lambda | 1 of 4 | #80 (decision), #65, #72 |
| `TestKotlinLinkTypes` | data class (constructor, `componentN`, destructuring, `copy`), another class's property, custom setter, `object`, loop, try, `also`/`apply`/`let`, sequences | 4 of 9 | #81, #82, #80 (`let`), #78 (sequence); `also`/`apply` do not see a modification through the lambda, by the engine's shared design (c0289fa84) |
| `TestKotlinLinkIsolation` | state across parses | — | #83 |
| `TestKotlinLinkDelegation` | class delegation: forwarders, constructor | 1 of 2 | #90 (the constructor never assigns `$$delegate_0`) |
| `TestKotlinLinkLambdas` | own higher-order function, lambda and bound callable reference as values, SAM conversion to `removeIf`/`computeIfAbsent`/`stream().filter`, a call into a Java-source class | 3 of 5 | #92 (callable reference type), #68 (call into Java source, both shapes) |

**Fixed since (ws/dsl, merged into ws/object 2026-09-28):** #74, #75, #77, #81, #82, #83; later on ws/dsl #84–#87,
#90. **Fixed on ws/object (2026-09-28, unpushed):** #92 (callable reference typed as `FunctionN`), #73 (companion
state and `const` on the enclosing class, as kotlinc), #69 (lowered statements renumbered as siblings), #79 (pattern
variable in a conditional expression), #72 (a lambda-assigned `var` is a `Ref` holder; the engine half is #94, Java
too); #68 closed as not reproducible. Their pins have become
parity assertions; the tables in §5 and §6 give the counts as first measured.

Language-level parity holds wherever the types agree: a Kotlin function type links exactly as Java code taking a
`Function1`, a Kotlin `object` as a Java singleton, a `$default` bridge as ordinary code. Of the 12 issues the two
tiers filed, 4 are defects in the shared engine that Java code hits too (#74, #78, #79, #83); #80 is a decision,
and #76 was first misreported (a harness artifact) and corrected the same day.

## 7. The Kotlin tier in `maddi-modification-analyzer` (added 2026-09-28)

Package `io.codelaser.maddi.modification.analyzer.kotlin`, 44 tests in 8 classes, in the module's ordinary `test`
task. Same design as §5 and §6, run to the end as the mixed CLI does: fault-tolerant prep that must isolate
nothing, then `IteratingAnalyzerImpl` (30 iterations, stop on a cycle without improvement). A type's verdicts are
printed one line per member (type: immutable/independent; field: final/unmodified/independent; method:
non-modifying/independent plus each parameter's unmodified/independent), sorted, and compared with the Java twin,
optionally restricted to the members both sides declare.

| Class | Topic | Agrees with Java | Pinned divergence |
|---|---|---|---|
| `TestKotlinAnalyzerTypes` | value class, mutable class, `lateinit`, collection holder, generic box, sealed hierarchy, open/override, interface default, inner class, `object`, enum, data class, companion state | 10 of 11 | #73 (companion `var`) |
| `TestKotlinAnalyzerConstruction` | constructor property, property initializer, `init` block, `vararg val` | 1 of 4 (the `init` block agrees with Java's nested-block twin, on #84's undecided field) | #85, #84, #86 |
| `TestKotlinAnalyzerCollections` | defensive copy with `List.copyOf` and with `toList()` | 1 of 2 | #87 |
| `TestKotlinAnalyzerFunctions` | function-typed parameter and field, extension functions, and #65/#72/#82's shapes | 4 of 4 | — (those defects do not move these fixtures' verdicts) |
| `TestKotlinAnalyzerMethods` | fluent builder, `apply` builder, identity, parameter and parameter-field modification, `Nothing`, recursion, `sum()` | 4 of 6 | #88, #89 |
| `TestKotlinAnalyzerSynthesized` | class delegation, `$default`, `private set`, computed and `by lazy` properties, top-level functions and extension properties, operator, anonymous object, template, `with`/`run`/`also`/`use` on a field | 5 of 7 | #90, #88 |
| `TestKotlinAnalyzerSuspend` | `suspend` functions (the Continuation parameter), the `sequence { }` builder | 2 of 2 (plus one shared doubt) | #89 (uncontracted `sequence`/`yieldAll`: the result reads `@Independent` of the elements it exposes, Java too) |
| `TestKotlinAnalyzerControl` | `?.`/`?:`/`!!`, `when` with `is`, map destructuring, `buildList`, local function, interface and abstract properties, nested class, companion factory | 6 of 7 | #87 (a `toList()` at the only call site) |

What agrees is most of the type system: value classes, sealed hierarchies, inheritance, objects, enums, generics,
function types, extension functions, fluent and identity methods. The divergences cluster in two places:
**construction** (Kotlin puts code into the primary constructor that the lowering does not: #84, #85, #86) and
**the stdlib** (#87, #88, #89). Three of them are unsound rather than conservative: #85 (a stored parameter reads
`@Independent`), #88 (a mutating method reads non-modifying) and #90 (a type with a `by` delegate reads
`@Independent` of it: the synthesized constructor does not assign `$$delegate_0`, which
`TestKotlinLinkDelegation` in the link tier pins). #88 is measured wider than `apply`: `with`, `run`, `also` and `use`
lose the modification whether the object is the receiver or `it`.

