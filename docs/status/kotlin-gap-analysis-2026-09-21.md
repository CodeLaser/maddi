# Kotlin gap analysis — what is still between maddi and a "Java+Kotlin" claim (2026-09-21)

**Question answered here:** the Kotlin work of the last two months is extensive; what has to close before
maddi can be described, in the README and on the website, as a Java **and Kotlin** analyzer?

**Verdict.** The front end is not the gap. maddi has a capable Kotlin front end and, around it, a *second
pipeline*: its own runner, its own CLI, its own distribution, no persistence, no IDE, no build-plugin reach.
Meanwhile the *Java* pipeline accepted Kotlin input and discarded it without a word. Every blocker below was
a variant of one problem — **the failures were silent**. Two of them are closed as of this document
(§7); the rest are ranked in §3–§6.

**What is honest to claim today** (after §7's two commits):

> maddi parses and analyzes Kotlin and mixed Java/Kotlin projects through its Kotlin CLI, proven on detekt
> (1,271 types, 31 source sets) and javalin, and every run reports how much of the code it could not read.
> IDE, Gradle/Maven plugin, incremental analysis and result persistence remain Java-only, and a Java entry
> point handed Kotlin now refuses instead of skipping it.

Not yet honest: "maddi is a Java+Kotlin analyzer", unqualified. §8 is the ordered path to it.

---

## 1. How this was assembled, and what it is worth

Four parallel sweeps over the `ws/dsl` worktree at `8d90d319a` (the most advanced Kotlin line; `devel`
carries all 210 Kotlin commits since July), each reporting `file:line` evidence: the documented state, the
front end's construct coverage, the downstream stages, and everything around the analysis (build systems,
IDE, corpora, packaging). The implementation in §7 was then done on `ws/python`, fast-forwarded to `devel`.

⚠ **Read-in-a-comment is marked as such throughout.** Claims that a *document* makes about the code were not
taken as facts about the code — §6 lists five documents that are stale in ways that would flatter the claim.
Figures quoted from commit bodies are labelled; they were not re-measured for this document.

## 2. What is solid

- **Corpora.** detekt: 1,271 types over 31 source sets, prep isolation 8 → 0 (`e9f716922`), modification
  analysis certifying with `@Immutable=676` once the JDK archive is loaded. coil's JVM slice parses (a
  hand-assembled configuration; multiplatform defeats both the Gradle plugin and `--compile-log`).
- **Mixed Java+Kotlin in one source set works, and was measured into the ground.** javalin went 0 Java types
  → 11 of 156 units failing → 3 → 1 → **0 javac errors, 0 dropped units, 0 prep errors** over five fixes
  (`1a0c08017`, `a0a301979`, `c8b86b972`, `baf650e53`, `7def28e26`). `parseInterleaved` + `SourceSetInterleave`
  are the mechanism.
- **Prep is genuinely language-neutral and tested that way.** 21 Java prepwork tests were re-expressed in
  Kotlin asserting the *identical* `VariableData` oracle (`CommonKotlinPrep.kt:26-36`). There is no
  `isKotlin` flag anywhere downstream, which is the right design and most of why any of this works.
- **Source-set reconstruction** from kotlinc logs, Gradle and Maven, including mixed logs folding
  kotlinc+javac into one output directory (`db0b6e6bd`).

## 3. Tier 1 — silent-wrong

| # | Gap | State |
|---|---|---|
| 1.1 | **The Gradle plugin dropped Kotlin without a word.** It collects `src/main/kotlin` (`AnalyzerPlugin.java:140-142`) and `dependsOn(compileKotlin)`, then forks an analyzer whose walk is `filter(p -> p.endsWith(".java"))` (`JavaInspectorImpl.java:1637`). Green build, partial analysis, no diagnostic. The Maven plugin has zero Kotlin code; `maddi-run-main` has zero hits for "kotlin". | ✅ **closed** — §7 |
| 1.2 | **Placeholders were analysis holes with no signal.** 36 emission sites produce `EmptyExpression("k2-…")`; `ExpressionVisitor.java:86` maps it to `EMPTY` — no links, no reads, no modifications. No run reported how many. | ✅ **closed** — §7 |
| 1.3 | **Two constructs vanished with no placeholder at all.** A `when` arm `!is T` lost both condition and pattern; a *local* `val x by lazy {}` disappears entirely, pinned known-wrong in `DelegatedPropertyTest.kt:161-180`. | ⬤ **half closed** — the `!is` arm is now a negated `InstanceOf` (§7.3); the local delegated property is still dropped |
| 1.4 | **Unmarked `newEmptyExpression()`.** Besides the 36 marked sites, the converter emitted bare empties where a REQUIRED child was missing — invisible to the census by construction. | ✅ **closed** — §7.3, and the distinction is now written down where it can be read |
| 1.5 | **The IDE daemon was the fourth Java-only entry point**, skipping `.kt` exactly as the plugins did (`WarmAnalysisService.java:57`). | ✅ **closed** — §7.4, as a reported problem rather than a refusal |

## 4. Tier 2 — CST model holes a real project hits on day one

Verified in code (zero occurrences / absent from the dispatch), not read in a doc:

- **Annotations on Kotlin declarations are not converted at all** — `addAnnotation` is never called in
  `maddi-kotlin-k2` or `maddi-inspection-kotlin`. Only `@Jvm*` are *read*, for structure. For an analyzer
  whose contracts and hints ride on annotations this is the largest single hole.
- **`suspend` does not exist** — zero occurrences of the token in either module's main sources. A suspend
  function converts as an ordinary method with the wrong JVM signature. That is most modern Kotlin.
- **`::foo`, `Foo::class` and parenthesized expressions are placeholders** — `KtCallableReferenceExpression`
  appears only in `ReferenceRecall.kt`; `KtClassLiteralExpression` and `KtParenthesizedExpression` appear
  nowhere in the converter's dispatch. Local `fun` declarations likewise.
- Not carried: `open`/`lateinit`/`const`/`operator`/`infix`/`tailrec`; `internal` maps to public; no
  `PropertyInfo`; `inline`/`value class`/`reified` unmodelled; class-delegation forwarders get empty bodies
  and delegated *properties* of an interface are not forwarded.
- **The front end's own recall figure is the honest yardstick**: on detekt's hand-written code, two recorded
  measurements, 26.0% EXACT / 70.8% DROPPED (`5748b45f1`) and 29.4% EXACT. *(commit bodies; not re-measured.)*
  It grades reference *locatability*, so it bounds renaming more than analysis — but it is this front end's
  own number and it is low.

## 5. Tier 3 — the stack past the parse

1. **No encode path.** No Kotlin module references `Codec`; `RunMixedPrepAnalyzer` writes no results. So no
   incremental analysis, no IDE daemon, no result reuse for Kotlin. The one decode defect that surfaced
   (`mapOf`, `CodecImpl.java:423-424`) suggests others wait.
2. *(Answered for lambdas, §7.22: same verdicts.)* **The link engine has zero Kotlin tests**, and `VirtualFieldComputer.java:110` excludes
   `java.util.function` from virtual fields — every Kotlin lambda is a `Function1` and therefore takes a
   *different* path than its Java equivalent, untested.
3. `GetSetHelper.java:249` hard-codes the `"set"` prefix and wants a backing field with a body; every
   immutability/independence consumer of `getSetField()` inherits that for Kotlin properties.
4. **The Kotlin stdlib archive is 10 JVM types / 24 annotated methods** (`Kotlin.json`,
   `KotlinCollections.json`, `KotlinText.json`), assembled by walking detekt's own `@FinalFields` bucket —
   against this repository's own rule that "a partial archive is indistinguishable from none". Contracts can
   only be written as Java shadow sources and have **no notion of a property** (`AnalysisHintsParser`).
5. `IsolateClass.java:658` writes `.java`: an isolated Kotlin defect reproducer is unusable as-is, and the
   default `TypeInfo.print()` emits Java syntax for a Kotlin-parsed CST.
6. **`maddi-cst-print-kotlin` has nine printer classes and no `src/test` directory at all.**
7. Read-only collections collapsing to `java.util.List` is real but **measured small** — 5 types / 7 fields
   = 0.4% on detekt (`docs/status/kotlin-corpora.md` §5.5). Do not spend Tier-1 effort on it.

## 6. Tier 4 — evidence and record

- **Three corpus tests skip silently.** `TestDetektCorpus`, `TestDetektReferenceRecall`, `TestCoilJvmSlice`
  all `assumeTrue(Files.exists(config))` on a locally generated `inputConfiguration.json`. On a machine
  without it they pass green measuring nothing — and they are the only corpus-scale exercise of the front end.
- **No Kotlin ratchet.** `corpus/catalogue/baselines/*.tsv` is Java-only. The javalin mixed-language
  regression lives in **jfocus**, not here.
- **Issues #32–#48 are all open on GitHub** although the fixes are committed — the branch carrying them was
  unmerged when they landed, so nothing auto-closed. The public record understates the work by ~15 issues.
- **Stale documents that would flatter the claim**, each to fix or retract:
  `maddi-cst-api/kotlin-stdlib-extension-facades.md` still says "blocked, parked 2026-07-01" (superseded by
  `44999b60e`); `maddi-cst-api/kotlin-parser-plan.md` closes with "Next action: M2" while its own §5 marks
  M2–M5b done; `docs/status/kotlin-corpora.md` §2/§5.6 carry 2026-08-03 numbers (and `TestDetektCorpus.java:132`
  copies them); `RunMixedPrepAnalyzer`'s javadoc said it "stops after prep" while its body runs the
  modification analysis; `mixed-language-integration.md` §7 still reads "in progress" against its own §5.
- **README tension**: the headline says "Java (and Kotlin, via a shared syntax tree)" while the status table
  says Kotlin ships only as a CLI zip. One of the two has to move.

## 7. What was implemented on 2026-09-21

### 7.1 The Java entry points refuse Kotlin sources — `1a1cd6a7b`

`DetectKotlinSources` (maddi-inspection-resource) walks the source directories of every
`parsedFromSource()` source set, resolving relative paths against the working directory **exactly as the
front end does** — a detector reading other paths than the parser measures another tree. The test is the
file extension, never the source set's name: the motivating case is a `.kt` under `src/main/java`.

⭐ `directoriesScanned` is part of the record. "No Kotlin found" is also what a detector whose path
resolution silently produced nothing returns; the negative-control test asserts the denominator, so this
check cannot prove its own converse.

Both `RunAnalyzer`s (run-main, run-openjdk) call one shared `refuse(...)` before `initialize` — two copies
in two runners is the drift `PluginOptions`' header was written about. Opt-out as agreed:
`--skip-kotlin-sources` on the CLI, `skipKotlinSources` on both plugins, carried in `GeneralConfiguration`
so it reaches the forked Gradle worker; with it the run continues and says the analysis is INCOMPLETE,
without it exit `6` (`EXIT_KOTLIN_SOURCES`, pinned in both `Main` copies with its message).

(The IDE daemon, the fourth entry point, is §7.4.)

### 7.2 Every Kotlin run says how much it could not read — `a19bf6fd2`

`PlaceholderCensus` (maddi-kotlin-k2) counts `k2-…` placeholders per kind over nested types, constructors,
methods and field initialisers, walking the tree the way `ReferenceRecall` does. `RunMixedPrepAnalyzer` runs
it immediately after the parse and logs at WARN when non-zero; the count joins `Summary` and the Kotlin
CLI's completion line, on the same line as the type counts — a run that reports what it parsed without
reporting what it could not read invites the reader to take the first for the whole.

⚠ The denominators travel with the count (`typesVisited`, `membersVisited`): `0 placeholders` is also what a
census that walked nothing reports. `K2_PLACEHOLDER_PREFIX` replaces the three hand-written `"k2-"` literals.

**Measured:** 11 new tests; the six touched modules' suites 354 tests, 0 failures. **And then the corpus
number, which is §7.5** — the one figure that sizes Tier 2, and it did not exist until this ran.

### 7.3 A `!is` arm is a condition, and a missing required child is marked — `e3dbd7b71`

Both were found by *writing* §3, not by a failing test. A `when` arm `!is T` is the switch-entry spelling of
`o !is T`, which the expression path already converted, so it now becomes the same negated `InstanceOf` over
the already-converted subject. Red first, quoted from the run with the drop restored:
`the arm must carry a condition, not nothing ==> expected: <1> but was: <0>`. The positive arm is pinned
unchanged in the same test.

Nine positions where a **required** child was absent (an assignment's value, if/while/do conditions, a loop
range, a throw, a destructuring initializer, a `when` subject's initializer, and a value `if`'s condition and
both branches) now emit a marked `k2-absent-…` with the enclosing node's range.

⚠ The bare empties that remain are deliberate, and the converter now says so: *"there is no expression here"*
is a fact about well-formed Kotlin — `val x: Int` with no initializer, a `for` variable, a `when` with no
guard, an `else` arm, `return` from a Unit function — spelled exactly as the Java front end spells it. A
missing required child is a different claim: it happens only on source that does not parse.

**Measured:** maddi-kotlin-k2 212 tests / 0 failures. No existing fixture produced a `k2-absent-…`, which is
the expected result — those paths fire on broken source, not on unsupported constructs.

### 7.4 The IDE daemon reports the Kotlin it does not read — `1aec31843`

It assembles an `InputConfiguration` from the editor's source directories and hands it to `JavaInspectorImpl`.
It does **not** refuse the way the CLI does, deliberately: an editor asking about a mixed project is better
served by the Java half plus a visible problem than by nothing. `initProblems` already carries "your analysis
is not what you think it is" to the editor, so the count and the source sets go there, and to the log at ERROR.

### 7.5 ⭐ The number that sizes Tier 2 — first corpus census

| corpus | placeholders | types holding one | members holding one | distinct kinds |
|---|---|---|---|---|
| detekt | **5,913** | **846 of 1,384 (61%)** | 2,322 of 7,747 (30%) | ~1,060 |
| coil (JVM slice) | **429** | 72 of 186 (39%) | 211 of 1,451 (15%) | ~190 |

**Three in five detekt types hold at least one position where the analysis reads nothing.** That is the
honest size of the front end's coverage gap, and it is the first time it has been a number rather than an
impression. (Both figures are AFTER §7.6; before it, detekt was 6,057 and coil 437.)

The kinds it reports, detekt, largest first: `k2-unresolved-call:add` 546, `KtParenthesizedExpression` 349
(now fixed, §7.6), `k2-unresolved-call:resolveToCall` 289, `KtBlockExpression` 270, `KtReturnExpression` 266,
`k2-unresolved-access:symbol` 176, `k2-unresolved-call:configure` 107, `:getByName` 107 — and ~1,050 more
kinds behind them.

⛔ **My first reading of that kind table was WRONG, and the correction is the more useful half.** The biggest
kind, `k2-unresolved-call:add` at 546, was read here as collection mutation, and §5's "read-only collections
are measured small" was retracted on that basis. It is not collection mutation. **759 of detekt's 5,913
placeholders (13%) are in GENERATED Gradle Kotlin-DSL accessors** — `gradle.kotlin.dsl.accessors._<hash>.…`,
which the compile-log route picks up as source — and their `add` is `DependencyHandler.add`, a library
member. §5's original 0.4% figure stands; this document's retraction of it does not. The warning sign was
there before the sites were: a four-line fixture of `mutableListOf().add(...)` converts perfectly, so the
name could not have meant what it seemed to.

### 7.5b What the sites actually say

detekt's own code, generated accessors excluded — **5,154 placeholders**, by family:

| family | count | share | what it is |
|---|---|---|---|
| `k2-unresolved-call` | 2,034 | | a call K2 resolved that the CST could not |
| `k2-unresolved-access` | 1,315 | | same, for a property/field access |
| `k2-unresolved-ref` | 818 | | same, for a bare name |
| **— those three together** | **4,167** | **81%** | **a symbol K2 knows and the CST does not** |
| `k2-unsupported-expr` | 701 | 14% | a construct the converter does not handle |
| everything else | 286 | 5% | operators, constructors, destructuring, delegates |

⭐ **Four in five holes are symbol resolution, not language coverage** — and the names say where:
`resolveToCall` (289), `symbol` (207), `classId` (79), `getArgumentExpression` (77), `docComment` (62),
`expressionType` (49), `asString` (44), `listOf` (52). Those are the Kotlin Analysis API, PSI and the
stdlib: **members of LIBRARY types**, which §4 — assembled by reading the converter — did not rank at all.

⛔ **And the cause is NOT the M5c "hierarchy-only library type" remainder this section first blamed.** That
was a guess from the shape of the names; §7.7 is the measurement, and it says something narrower and much
more fixable.

The genuine construct gaps are the 701, and they are led by `KtBlockExpression` (270) and
`KtReturnExpression` (266) — a block or a `return` in *expression* position — with
`KtCallableReferenceExpression` (`::foo`) third at 88. `suspend`, `Foo::class` and the missing annotations
do not register on this corpus at all.

⚠ detekt is a static analyzer built on the Kotlin compiler, so its library profile is not every project's.
**So coil was split the same way, and it agrees**: 429 placeholders, no generated code at all (every owner is
`coil3.*`), and unresolved call 199 + access 68 + ref 52 = **319, 74%** — against 36 (8%) genuine construct
gaps. Its unresolved names are okio's `FileSystem` (`exists`, `delete`, `atomicMove`, `close`) and the
stdlib's scope functions (`apply`, `let`), not the Kotlin compiler API. **Two corpora with nothing in common
but the language, 81% and 74%, and the same cause: members of library types.**

⚠ The generated-accessor share is a property of the compile-log route, not of the project: coil, configured
by hand, has none.

### 7.6 The census pays for itself on day one — `176d67d48`, `edde9dc63`

`(a + b).f()` was a placeholder: parentheses were not in the expression dispatch, and a placeholder
swallows everything inside it. Fixed by returning the inner expression, as javac's parser does.

⚠ **The total does not fall by what the fix removes, and that is this number's honest shape**: detekt
6,057 → 5,913 (−144, not −349), coil 437 → 429 (−8, not −20). Fixing a swallowing placeholder REVEALS the
holes it hid — ~205 expressions inside those parentheses are themselves unconverted. A census that fell by
exactly 349 would have meant the inner code was never counted at all.

⛔ **And it exposed a defect one layer down.** Three detekt tests went to `StackOverflowError` the moment
those expressions carried a real type: `CommonType`'s join guard fires only when a type argument is exactly
the type being joined, so a recursive generic one level deeper walks forever. It is a **Java-side defect**
the Kotlin corpus merely walked into. The join now carries the pairs it is still computing (removed on the
way out, so it bounds the stack and not the traversal).

### 7.7 ⭐ Why a library call does not resolve — measured, and it is not what §7.5b assumed

The failing set reduces to four lines. Resolving fine: `x.map{}`, `x.filter{}`, `x.sorted()`, `mapOf(…)`,
`s.trim()`, `s.uppercase()`, `s.let{}`, `sb.apply{}`, `File.exists()`, `mutableListOf().add(…)`. Failing:
`x.joinToString(",")`, `listOf("a","b")`, `"a" to 1`, `s.substringAfter(".")`, `it.length`.

**The rule: a library callee is looked up by name + the number of arguments the source WRITES**
(`collectMethods`, `KotlinBodyConverter.kt:1150`: `it.parameters().size == arity`). A call resolves only
when the written count equals the callee's JVM parameter count — so the two Kotlin features that break that
identity are exactly the failing set:

| cause | example | why |
|---|---|---|
| **omitted defaulted parameters** | `joinToString(",")` is `joinToString/7` on the JVM | writing every defaulted argument makes the identical call resolve — proven A/B |
| **`vararg`** | `listOf("a","b")`, `split(",",";")` | N written arguments against 1 array parameter. `listOf("a")` "works" only because it binds the stdlib's single-element overload |
| **infix/operator library extensions** | `"a" to 1` fails, `"a".to(1)` resolves | `operatorFunctionCall` (`:1623`) looks only at members of the left operand's type and never takes the extension-facade route |
| **the predefined `String`** | `s.length` | ⚠ **a FIXTURE artefact, not a corpus gap — §7.10.** `bootstrapString` runs only when nothing has inspected `java.lang.String`, i.e. a Kotlin-only parse with no JDK; both corpora show ZERO `k2-unresolved-access:length` |
| **extension properties** | `s.lastIndex`, `c.java`, `d.symbol` | ✅ fixed, §7.11 — and it was the biggest access family, not the footnote this row implies |

⚠ **The facade/part-class lead was a red herring** — `filter` and `joinToString` live in the SAME part class
(`CollectionsKt___CollectionsKt`) and it is found correctly. So is `deepen`. The two documents that would
have sent someone there (`kotlin-stdlib-extension-facades.md`, and this section's own first draft) are wrong
about it.

**Fixed — §7.9.** The two risks named here when it was still a plan were both avoidable: binding the REAL
method rather than a synthesized `$default` keeps the AAPI's contracts reachable, and a varargs call needs no
synthesized array because maddi already represents one with the arguments written out.

### 7.8 ⛔ The failure the census cannot count — `2ebee1eb7`

Chasing the above turned up something worse than a hole. `resolveCallee` ended `?: candidates.first()`:
when no overload matches the argument types, it took whichever came first. Measured, before the fix:

    s.replace("a", "b")  ->  java.lang.String.replace(char, char)   // two String literals, two chars
    s.split(",")         ->  java.lang.String.split(String)         // returns String[]; expression type List<String>

A placeholder says "nothing was read here" and the census counts it; **a guessed overload is a resolved call
in the tree, and no walk over the tree can find it afterwards**. So the 74–81% figure understates the blind
spot rather than bounding it.

The fix rules out what can be proven impossible without a type hierarchy — a reference argument can never
reach a primitive parameter unless it is that primitive's box. ⚠ The obvious tier, assignability, does NOT
work here and that is measured, not assumed: `isAssignableFrom(CharSequence, String)` is false, because the
predefined `String` is bootstrapped without its hierarchy (the same defect as the `s.length` row above), so
an assignability tier leaves `replace(char,char)` in place. What is still guessed is now counted
(`KotlinScan.ambiguousBindings`) — the blind spot beside the census.

### 7.9 ⭐ The arity fix — `0489230af`

`collectMethods` keyed on `parameters().size == <arguments written>`, so the two Kotlin features that break
that identity were exactly the failing set. Four changes:

1. `callArguments` asks the **K2 symbol** whether a parameter is optional (`hasDefaultValue`) rather than
   asking its PSI for a default value — a library declaration has no PSI, which is why every library call
   with an omitted default bailed out.
2. A library callee binds the **real** method with omitted parameters filled by their zero value, not a
   synthesized `$default`: the AAPI's contracts are keyed to the real signature. A source callee is
   unchanged.
3. A varargs candidate is matched as the Java front end represents one — arguments written out — and the
   overload tiers index by ARGUMENT through `typeOfParameterHandleVarargs`.
4. An operator/infix **extension** now routes through its facade. `"a" to 1` failed while `"a".to(1)`
   resolved — the same call written two ways — and it was worth 347 sites once `mapOf(…)` stopped
   swallowing its own arguments.

⚠ **The total barely moves, and the accounting is the result.** detekt 5,913 → 5,945; coil 429 → 411.

| family (detekt) | before | after | |
|---|---|---|---|
| `k2-unresolved-call` | 2,793 | 2,620 | **−173** |
| `k2-unresolved-operator` | 106 | 10 | **−96** |
| `k2-unresolved-ref` | 818 | 961 | +143 *surfaced* |
| `k2-ctor-unresolved` | 74 | 142 | +68 *surfaced* |
| `k2-unresolved-access` | 1,315 | 1,373 | +58 *surfaced* |

⭐ **"Surfaced" is measured, not asserted**: of the 366 new placeholder sites, **366 are in a member that
already held one, and zero in a member that was clean before.** No member that converted fully before
converts worse now. The metric that moves is coverage: types holding a hole 846 → **809**, members 2,341 →
**2,222** (detekt); members 211 → **203** (coil).

⚠ Still open and now visible: `s.split(",")` binds `java.lang.String.split(String)` (returning `String[]`)
while the expression type is `List<String>` — Kotlin's extension puts its vararg in the MIDDLE with
defaults after it, which this change does not synthesize.

### 7.10 ⚠ `bootstrapString`: statics yes, properties no — `e1eae5862`

`String.format`/`valueOf`/`join` were genuinely missing (static members live in their own scope, which the
hand-written bootstrap never read). But turning String's PROPERTIES into fields — which `loadLibraryMembers`
does for every other library type — was **wrong, and two measurements said so within the minute**: the
corpora show zero `k2-unresolved-access:length`, because every real run loads the JDK where `length` is a
METHOD; and a ported prepwork test went red with `java.lang.String.length#s` in a `VariableData` its Java
original does not have. The change would have forced a ported test's oracle apart from Java's for no gain on
any real run.

⚠ Two placeholder tests had used `s.length` as their "construct the front end cannot read" and were
re-fixtured on `String::class`. A test that pins a mechanism must not depend on a hole that only a
fixture-only path has.

### 7.11 ⭐ Extension properties — `b1b58d9ea`, and the access family drops 23%

An extension property is not a member of the receiver's type, so neither the field lookup nor the accessor
lookup could find one — `o.doubled` failed for a property declared in the same file. It compiles to a static
getter on a facade, like an extension function, and now resolves the same way; the library facade loader,
which was built from a package's FUNCTIONS, now carries property getters too.

detekt's `k2-unresolved-access` family: **1,373 → 1,054**.

| kind | before | after |
|---|---|---|
| `symbol` | 210 | **38** |
| `java` | 58 | **0** |
| `mainReference` | 19 | **0** |
| `containingClassOrObject` | 14 | **0** |
| `javaClass` | 9 | **0** |

⛔ **`symbol` at 210 was the corpus's biggest access kind, and §7.7 filed it as a KaSession MEMBER extension
needing an implicit-receiver route.** It is a plain top-level extension property. Another conclusion drawn
from reading a name rather than running it — the third this campaign, and the pattern is now unmistakable:
**every time the evidence was a name, it was wrong; every time it was a run, it held.**

Totals: detekt 5,945 → **5,914**, types holding one 809 → **804**, members 2,222 → **2,203**; coil 411 →
**395**. All 69 new sites are in a member that already held one.

### 7.12 Statements in expression position — `1d13fa423`, `b447bdddf`, this commit

The family §7.5b named as one item (`KtBlockExpression` 287 + `KtReturnExpression` 270 + throw/try/continue)
splits on measurement into two problems with very different prices.

**Blocks — done.** `val v = if (c) { a() } else { b() }` is one expression to Kotlin and two blocks to the
PSI, so every such `if` produced two placeholders. A block whose single statement is an expression IS that
expression. detekt `KtBlockExpression` **306 → 0**, with 17 left in a newly named
`k2-block-not-a-single-expression` — **94% of them were a single-expression branch.** Totals 5,914 → 5,769;
coil 395 → 383.

**`return`/`throw` — done, stage 1 (`b447bdddf`).** Before designing it I sampled the corpus source at
eight `KtReturnExpression` sites: **all eight were `x ?: return …`**, so the lowering targets that idiom
rather than the general problem.

    val t = s ?: return 0      ->      if (s == null) return 0;
                                       val t = s;

No temporary is needed in these positions. ⚠ Two statements from one are indexed `0.0`/`0.1` with nothing at
`0`: `pad` zero-pads sibling indexes so they SORT, so renumbering would move every statement after them,
while a nested pair sorts in place. A synthetic `Block` was the alternative and is wrong here — it
introduces a scope, and the dominant case declares a variable the following statements use.

⛔ Refused where the lowering would move an evaluation or leave the wrong method — a LABELLED return
(`?: return@mapNotNull null`, one of the eight samples) belongs to a lambda, and `g(1, s ?: return 0)`
cannot hoist its guard above an argument evaluated first. Both keep their placeholder and stay counted; the
64 that remain on detekt are exactly those two shapes.

detekt `KtReturnExpression` **269 → 64** (−76%), `KtThrowExpression` 10 → 7, total 5,769 → **5,565**; coil
383 → 381. Five new sites, all in a member that already held one. ⭐ **0 elements isolated by prep** over
1,271 types — the index scheme confirmed by a run, not by reasoning, plus a prepwork-level unit test that
asserts prep executes over a lowered method.

**`try` as a value — done, stage 2.** A `try` in a value position becomes a `try` STATEMENT whose branches
carry the value out: `return` it where the try is returned or is the expression body of a function, assign
it to the declared local where it is an initializer —

    val v = try { X } catch (e: E) { Y }    ->    T v;
                                                   try { v = X } catch (e: E) { v = Y }

the declaration split off ahead of the statement and indexed `n.0`/`n.1` as in stage 1. The same lowering
serves an `if` whose branch is a block of several statements, which is what the other half of §7.12 left
behind. A `finally` is never assigned: it does not yield the try's value.

⛔⛔ **The measurement that mattered, and the shape of the mistake.** Written this way — a lowering applied
where statements are converted — it removed **zero** of detekt's four remaining try sites. A statement list
lives in **four** places in the converter (a block body, a lambda body, and the branch blocks of a
value-yielding `try`/`if`), and the first cut reached only the first: three of the four sites are
expression-bodied functions (`fun f(): T = try { … } catch { … }`, by far the commonest shape) and the
fourth is a `val` inside a lambda. The unit tests were green, the total moved by +1, and only the per-kind
diff of the dump said so. The reach is now shared (`loweredStatements`), which is also what moved the STAGE
ONE numbers: `KtReturnExpression` **64 → 17**, because 47 of the elvis sites "refused" in stage 1 were not
refused at all — they were in a list the lowering never visited.

detekt `k2-unsupported-expr:KtTryExpression` **4 → 0**, `k2-block-not-a-single-expression` 17 → 11, total
5,565 → **5,525**, types holding one 791 (unchanged), members 2,163 → **2,160**; coil 381 → **379**. 17 new
sites, all in a member that already held one, and **no member newly dirty**. ⭐ **0 elements isolated by
prep** on both corpora — which retires `docs/status/kotlin-corpora.md` §5.1 (the `variableData` overwrite, 8
elements on detekt) as well as §5.2, its cause.

### 7.13 One entry point — `maddi-kotlin` is now a superset of `maddi`, not a second tool

§8.3 posed this as an either/or: the Java CLI gains the mixed inspector, or the Kotlin CLI gains the flags it
lacks. Two measurements decided it.

⚠ **First, the premise was wrong.** §8.3 named `maddi-run-main`. The shipped Java CLI is
`maddi-run-openjdk` (`applicationName = "maddi"`, `bin/maddi`, `maddi-<version>.zip`); `maddi-run-main`'s
distribution is not published at all. And that CLI already had `--compile-log`, `--help`, incremental, the
hints composer and the explicit `--source`/`--classpath` route — the whole list §8.3 said was missing was
missing from the *Kotlin* side only.

⭐ **Second, the distributions:** `maddi-kotlin-0.9.1.zip` is **85 MB** against `maddi-0.9.1.zip`'s **11 MB**,
because the K2 "for-ide" jars ride along. Folding Kotlin into `maddi` would make every Java-only user carry
an 8× bundle to buy a property they do not need. So: **one command line, two bundles.**

`kotlinmain.Main` now builds its command line from `openjdkmain.Main.createOptions()` — the same object, not
a second list that agrees today — and routes on `DetectKotlinSources`:

- **no `.kt` file** → the same `RunAnalyzer`, so the run *is* `maddi`. Asserted both ways in
  `TestOneEntryPoint`: same exit code on a clean project **and** on a parse error (agreeing only on success
  would be agreeing about very little).
- **Kotlin present** → the mixed pipeline, now also honouring `--parallel` and `--warn-near-misses`.

⛔ An option the mixed pipeline cannot honour is **refused by name** (new exit code 7), never run as a no-op:
`--analysis-results-dir`, `--incremental-analysis`, `--analysis-steps rewire-tests`, and the hints-compiler
modes. They all end at the same place — an analysis result that can be written and read back — which is
§8.6, and a run that wrote an empty result directory would look exactly like one that worked.

⛔⛔ **What `--help` revealed the moment there was one.** Two options shared the short form `-s`
(`--source` and `--preload-analysis-results-dirs`); commons-cli keys its short map by the letter, so the
later registration won. Measured: `maddi -s src --analysis-steps prep` → **"Running prep analyzer on 0
types", exit 0**. A run that analyzed nothing and reported success, on the shipping Java CLI, and `--source`
was not even listed in the help. Fixed (the hints option loses its short form), with
`noTwoOptionsShareAShortForm` over the whole assembled option set so the next one cannot repeat it. ⚠ The
deeper half is still open: **zero source sets is still exit 0 with a WARN.** Refusing it would be right, but
the build plugins run per module and an aggregator module legitimately has no sources, so that is a separate
change with its own evidence — filed, not smuggled in here.

⚠ A second family the same look revealed, **not** fixed here: the explicit `--source`/`--classpath`/`--jmod`/
`--source-packages`/`--source-encoding` options are read on one route only, so given alongside
`--input-configuration` or `--compile-log` they are accepted and dropped — the shape of bug `--jre` had and
that cost an Ignite corpus run. They are now **named in a warning** on those two routes; making them apply
is a separate change.

Also unified: `exitMessage` was a private `switch` in each runner's `Main` that **threw** on a code it did
not know, so a code added in one runner became an `UnsupportedOperationException` in another at the moment
it was reporting a failure. Both now delegate to the shared `ExitCode.message`, and each runner's `EXIT_*`
constants are aliases of it rather than a second copy of the numbers.

⭐ **Measured with the shipped launchers, not the test harness:**

| | `bin/maddi` | `bin/maddi-kotlin` |
|---|---|---|
| `--help` | full surface, exit 0 | **same** surface, exit 0 |
| fernflower (Java only) | `Running prep analyzer on 225 types`, exit 0 | "No Kotlin source file in 2 source set(s); running the Java analyzer" → **byte-identical**, exit 0 |
| detekt (Kotlin) | refuses (exit 6) | 1,271 Kotlin types, analysis order 15,118, 5,525 unreadable constructs, exit 0 |

### 7.14 ⛔ The lowering that was well formed and wrong — `01815b03e`, this commit

§7.12 reported the statement-as-value family closed on the strength of the placeholder census. The census
cannot see this: `controlFlowElvisLowering` needs its left operand twice — to test for null, and as the
value — and converting it twice **evaluates** it twice.

    val t = f() ?: return 0    ->    if (f() == null) return 0;
                                     val t = f();

Two calls where the source has one. `isControlFlowElvis` only ever constrained the RIGHT operand (an
unlabelled `return` or a `throw`); nothing constrained the left. ⭐ Measured: **190 sites on detekt, 3 on
coil** — not rare, and invisible, because nothing is missing.

**Fixed by a temporary**, exactly as a hand-written Java version would: `T $elvis0 = f(); if ($elvis0 ==
null) return 0; val t = $elvis0;`, three statements indexed `n.0/n.1/n.2`. A stable left operand (a name,
`this`, a constant, a dotted chain of those) keeps the two-statement form, because re-reading it evaluates
nothing. `elvisReEvaluations` stays as an invariant check — it must read 0 — and is logged on every project
parse *including when it is zero*, because an absence of output is not a measurement.

After: detekt and coil both **0** re-evaluations, prep still isolates **0**, immutable types unchanged at
669, analysis order **15,118 → 15,056** (62 duplicated elements gone), and the placeholder dump
**5,525 → 5,271**. ⭐ The entire 254-site drop is de-duplication: 254 fewer duplicated rows, and **zero** new
distinct sites. The census had been over-counting wherever a left operand did not convert.

### 7.15 ⛔⛔ …and the same defect, bigger, in the expression path — fixed, mostly

Chasing §7.14's remaining duplicates found the real shape of it. Both a safe call `a?.b()` and a *value*
elvis `a ?: b` lower to a ternary in which the tested operand stands twice, and the rule §7.12 inherited and
recorded as deliberate — "the left operand is converted TWICE … the CST is a tree and sharing a node makes
every walker visit its statements twice (#32)" — is right about sharing and wrong about evaluation. Down a
chain each level doubles the one below it.

⭐ Measured on detekt before: 331 sites twice over, 116 four times, 28 eight times, 6 sixteen times and **4
thirty-two times** — 20% of the census was duplicates. The worst,
`SuspendFunSwallowedCancellation.hasSuspendCalls`, is a chained value elvis over safe calls: **32 copies of
one call** in the tree.

**Fixed by hoisting the spine into temporaries** — the same remedy as §7.14, and the scoping is the whole
design:

⛔ **Only the unconditionally-evaluated spine.** Hoisting an evaluation out of a conditional position changes
it: in `x?.foo(g())` the source runs `g()` only when `x != null`, so lifting it above the statement would
run it always. So the walk descends *only* through safe-call receivers and elvis left operands, from the
statement's own expression — a path that is by construction evaluated every time, before anything is
tested. Everything off it (arguments, the right of `?:`, lambda bodies, branch arms) is untouched, still
converted twice, still counted. Innermost first, so `a?.b()?.c()` becomes `t0 = a.b(); t1 = (t0 == null) ?
null : t0.c()` — linear in the chain instead of exponential.

⚠ It took three passes to reach the sites that mattered, and each pass says where statements actually live:
the method-body loop alone (−186), then the lambda/returning/assigning loops (−52), then the **tail**
positions — a lambda's result expression, a block's last statement, an expression body — which is where the
16× and 32× cases were, inside `analyze(this) { … }`. The same "a statement list lives in four places"
lesson as §7.12, in its fourth costume.

| detekt | before | after |
|---|---|---|
| placeholder rows | 5,271 | **4,781** |
| distinct sites | 4,166 | **4,166 — unchanged** |
| inflation from duplication | 20% | **12%** |
| worst multiplicity | 32× | **17×** (one site); the 16× and 32× tiers are gone |
| analysis order | 15,056 | 14,946 |
| prep isolated / immutable | 0 / 669 | 0 / 669 |

`NullSafeSpineTest` asserts the property directly — **calls in the tree against calls in the source** —
which is what neither the census nor a passing parse could see.

⚠ 12% inflation remains, at multiplicities that are no longer powers of two (5×, 6×, 7×, 9×, 11×, 17×), so
the remaining duplication is mixed-path, not chain blow-up. A duplicated operand *inside* a conditional arm
is not hoistable to the statement, but could be hoisted to the head of that arm's own block. That is the
next refinement, and it is worth doing only if a verdict-level test shows it changes an answer.

### 7.16 ⭐ The first verdict-level test, and what it found in one run

Everything measured about the lowerings until now was about the PARSE — placeholders, statement indexes,
prep isolating nothing. None of it says the analyzer concludes the right thing, and §7.14 had just shown
that a lowering can be well formed and wrong for months without a red test.

`TestLoweredShapesVsJava` asks the other question: **for each lowered shape, does the modification analysis
reach the same verdict as for the Java a human would write?** Both sides call the same Java helper, so a
disagreement is the lowering and not the standard library; the sensors are `NON_MODIFYING_METHOD` and
`UNMODIFIED_PARAMETER`, which is what a mis-shaped tree moves.

Five shapes: `try` as a value, a multi-statement `if` as a value, `x ?: return`, a null-safe chain that
modifies, and one that does not. **Four agreed. One did not**, on the first run:

    safeChain   kotlin: box.unmodified=true    java: box.unmodified=false

`b?.next()?.add(t)` — the analysis did not see that the parameter was modified. The parse was clean, prep
isolated nothing, the census showed no hole, and the answer was wrong.

⛔ **The cause: a safe call in STATEMENT position lowered to a ternary with a void arm.** `(b == null) ? null
: b.add(t)` is not a shape Java can write — `add` returns `Unit` — and the analysis does not follow it.
Fixed by lowering a statement-position safe call to what it is, an `if`:

    b?.next()?.add(t)   ->   Box $nullSafe0 = (b == null) ? null : b.next();
                             if (!($nullSafe0 == null)) { $nullSafe0.add(t); }

⚠ And it took two attempts, the second found by probing the CST rather than by reading the code: hoisting
the spine of the safe call's RECEIVER leaves the outer node's own tested operand un-hoisted, so the receiver
ternary was still inlined twice — once in the condition, once in the call. The walk has to start at the
whole safe call.

detekt after: placeholder rows **4,781 → 4,744**, analysis order 14,946 → 14,908, prep isolated **0**,
immutable types 669, coil unchanged. ⚠ 19 sites are newly *visible* — the same reveal pattern as every other
swallow fixed in this campaign, and the invariant holds: all 19 sit in members that already held a
placeholder, and **no member is newly dirty**.

### 7.17 ⭐ The verdict test widened — and the 12% duplication question, answered

§7.16 shipped with five paired shapes and two sensors. Widened to **thirteen paired shapes** (`when` as a
value, elvis-with-throw, an expression-bodied `try`, a modifying call in an argument, in the right of `?:`,
in a branch arm, in a ternary arm, and a shape that genuinely duplicates) plus a **type-level row** — a
`KHolder`/`JHolder` pair whose field is reached through a lowered chain, sensed on `IMMUTABLE_TYPE`.

Two guards were added, because a comparison that agrees for the wrong reason is worse than one that fails:

- ⛔ **a placeholder census assertion**. A shape the front end cannot read becomes a placeholder, and two
  sides can then agree on a verdict neither derived from the code. The Kotlin half must convert to **zero**
  placeholders or the run is declared vacuous.
- ⛔ **a multiplicity assertion**. The duplication row asserts the modifying call still stands **twice** in
  the Kotlin tree. Without it the row silently stops asking its question the day the hoist widens.

⚠ The first multiplicity counter was wrong in the way this document keeps recording: it counted
`addAndSize` and `addAndEcho` together and reported "2×" for shapes that duplicate nothing. The rows it
"confirmed" proved nothing until it was made to count one named method.

⭐ **The §7.15 question is answered.** `b.addAndSize(c?.addAndEcho(t) ?: t)` — the elvis sits in an argument,
so the spine hoist deliberately does not reach it and the modifying call stands **2× in the Kotlin tree
against 1× in the Java**. The verdicts are identical, on every sensor, including the type-level one:

    dupOffSpine  kotlin: b.unmodified=false c.unmodified=false   java: b.unmodified=false c.unmodified=false
    dupOffSpine  addAndEcho in the Kotlin tree: 2×   in the Java tree: 1×

So the further refinement §7.15 proposed — hoisting a duplicated operand to the head of its own arm's block
— is **not warranted on verdict grounds**. The residual 12% is census inflation and analysis-order size, a
cost, not a wrong answer. ⚠ Demonstrated for the modification/immutability sensors on this shape, not
proven for every sensor; the row is there to fail if that changes.

### 7.18 ⛔⛔ The verdict test's real catch: a Java-engine defect, in Java

Widening the test produced exactly one disagreement, and it was not the Kotlin front end's:

    armModifies   kotlin: c.unmodified=true     java: c.unmodified=false

The Kotlin lowered `val v = if (b == null) c.addAndSize(t) else b.size()` into a ternary; the hand-written
Java used an `if` statement. Adding a **paired ternary row**, where both sides are ternaries, made both sides
say `true` — so the cause was the shape, not the language.

Reproduced minimally in the engine's own suite (`TestModificationInConditionalExpression`, pure Java, no
Kotlin anywhere): the same modifying call, in eight positions. Only two were right.

| shape | before | after |
|---|---|---|
| `c.addAndSize(t) + b.size()` | ✅ modified | ✅ |
| inside an `if` statement's arm | ✅ modified | ✅ |
| inside a ternary arm | ⛔ **not modified** | ✅ |
| inside the ternary's **condition** | ⛔ **not modified** | ✅ |
| in both arms | ⛔ **not modified** | ✅ |
| a ternary nested in an argument | ⛔ **not modified** | ✅ |
| a ternary assigned to a local | ⛔ **not modified** | ✅ |
| a switch-expression arrow arm | ⛔ **not modified** | ✅ |

⭐ It is not "conditional arms are skipped": the **condition** is not conditional at all and was lost too.
`ExpressionVisitor.inlineConditional` visits all three sub-expressions and merges them, then rebuilds the
result with `new Result(links, extra)` — the two-argument constructor, which resets the other five fields to
empty, `modified` among them. Every modification recorded anywhere inside a conditional expression was
discarded on the way out. The sibling `switchExpression`, written from the same template and carrying a
comment saying so, already used the preserving idiom `merge.with(newLinks)`; the asymmetry was the whole
defect. A switch **entry's arrow arm** had the identical drop — its block-bodied sibling never did, because
that path re-adds `d.modified` explicitly.

⚠ Two traps on the way to the fix, both caught by controls rather than by reading:

- `with()` also carries `evaluated`, and `merge` inherits it from its leftmost operand — so the naive fix
  made a ternary evaluate to its **condition**. `setEvaluated(ic)` restores what the old constructor left to
  `visit`'s fallback.
- The negative control (`l == null ? emptyList() : Collections.unmodifiableList(l)`, taken verbatim from the
  corpus) stayed red, and it was **not** the fix: the archive gives `unmodifiableList` `@Independent[M]` and
  no `@NotModified`, so its argument is modified by hint. The ternary had been hiding that. Pinned in the
  test as-is; whether that hint is right is a separate question, for the Java side.

**Measured, A/B on clone-bench, 9,319 types, nothing else changed:**

| `TestShadowCloneBench` | before | after |
|---|---|---|
| divergences (shadow modified, main optimistic) | 855 | **848** |
| — nonModifyingMethod / unmodifiedField / unmodifiedParameter | 16 / 27 / 812 | **11 / 26 / 811** |
| reverse (main modified, shadow unreached) | 263 | **273** |
| types holding a divergence | 969 | **972** |

Both directions are one story: the main analysis found **17 modifications it used to drop**. Seven the
shadow pass had already found, closing a divergence; ten it does not reach, opening a reverse — and those
ten are five near-clones × two methods, every one of them the `unmodifiableList` shape above. The ratchet is
re-baselined with that reasoning recorded beside it.

⭐ This is what the Kotlin exercise bought the Java side: **a verdict-level comparison between two languages
is a differential oracle**, and it found a core defect that 3,505 same-language tests and years of corpora
did not. Kotlin's lowerings emit ternaries where a human writes `if`, so the Kotlin front end walked into a
blind spot Java code mostly steps around.

### 7.19 ⭐ Callable references — five shapes of six, and the field that decides the answer

`::f` was the largest remaining unmodelled family (≈92 sites on detekt). It is Java's method reference and
the CST already has a `MethodReference`, so the work was resolution, not modelling. ⚠ The inventory was
established by probe rather than by reading the dump, which had only ever matched method *signatures*
containing the type name:

| shape | K2 resolves it to | before | after |
|---|---|---|---|
| `::two` (top-level) | `KaNamedFunctionSymbol` `/two` | placeholder | ✅ facade `PKt`, scope = type |
| `::twice` (member, implicit) | `KaNamedFunctionSymbol` `/P.twice` | placeholder | ✅ scope = `this` |
| `this::twice` | same | placeholder | ✅ scope = `this` |
| `Q::len` (unbound) | `KaNamedFunctionSymbol` `/Q.len` | placeholder | ✅ scope = type `Q` |
| `::Q` (constructor) | `KaConstructorSymbol` | placeholder | ✅ the constructor |
| `Q::i`, `String::length` | `KaFir…PropertySymbol` | placeholder | ⛔ `k2-callable-ref-property` |

⭐ **The field that decides what the analyzer concludes is `scope()`, not `methodInfo()`.**
`ExpressionVisitor.methodReference` treats a scope with no links-primary — a `TypeExpression` — as an
INTERNAL receiver and drops its self-modifications, and a scope that is a value as the caller's own object.
So `Q::len` and `this::len` must not produce the same tree, and a test asserting only "no placeholder"
would not have noticed if they did. `CallableReferenceTest` asserts the scope KIND on every shape.

The three no-receiver cases are the subtle ones: `::two` and `::twice` are written identically and mean
opposite things. The discriminator is whether the symbol's PSI has a containing class — a member is
implicitly `this` (bound), a top-level function lives on the file facade (unbound).

⛔ **Property references are deliberately left.** A Kotlin property is not a `MethodInfo` in this front end,
so `Q::i` has nothing to reference; it keeps a placeholder that NAMES the shape (`k2-callable-ref-property`)
rather than hiding among the unsupported expressions. The census is the disclosure instrument, so the name
is the deliverable.

⚠ One knock-on stays open and is pinned: the reference in `val g = ::twice` converts, but CALLING it
(`g(2)`) does not — invoke-operator sugar over a `KFunction1`, a type this front end knows only shallowly.

**Verdict-level, via §7.17's instrument** — both sides calling the same Java method through the same
functional interface, so a disagreement is the reference and not the library:

    refBound     kotlin: b.unmodified=true c.unmodified=false   java: b.unmodified=true c.unmodified=false
    refUnbound   kotlin: b.unmodified=true c.unmodified=true    java: b.unmodified=true c.unmodified=true

`c::add` marks `c` modified; `Box::touch` does not, on EITHER side — that is the documented conservative
drop for an unbound receiver in `ExpressionVisitor.methodReference`. ⭐ The Kotlin front end matches Java
including its conservatism, which is the right result for a differential test: fidelity to the engine, not
to an ideal.

⚠ **Not yet measured on a corpus.** The ≈92 detekt sites are a recorded figure, not a re-measurement; the
box was full of other threads' JVMs when this landed. Unit evidence only: 254 tests in `maddi-kotlin-k2`,
3,513 in the repo, 0 failures. The corpus delta is owed.

### 7.20 ⭐ Make the evidence fail — the rung that protects every other one

Three corpus tests opened with `Assumptions.assumeTrue(Files.exists(config))`. An absent corpus made them
SKIP, and a skipped test reports the same build outcome as one that analysed 9,319 types. `AGENTS.md`
§Commands has warned about exactly this for months. ⛔ **And it caught me an hour after I wrote it**: the
commit message for the conditional-expression fix claimed "slowTest green on clone-bench, detekt, coil and
fernflower". Fernflower was never run — `maddi-run-openjdk:slowTest` had not executed in the session at all,
and its only result file was 32 days old. Believing a green build outcome is the failure; it is not enough
to know about it.

**1. A corpus that is required cannot be skipped.** `TestOssCorpus.requireConfig` / `requireDir` skip by
default — a contributor without the checkouts still gets a green build — but under
`-Dmaddi.corpus.required=true` the same absence is a hard failure naming the corpus and the command that
generates it. `slowTest` sets the property, because measuring corpora is the whole reason that task exists;
`-Pcorpus.optional` turns it back off. ⚠ `TestCorpusRequirement` is the identity check for the mechanism —
deliberately NOT tagged slow and needing no checkout, because a guard nobody tests is the failure mode it
exists to prevent, one level up.

**2. A two-sided ratchet on the Kotlin census.** The Java corpus tests assert floors ("at least 1,000
types"), deliberately, so a version bump is not brittle. A floor is the wrong instrument for the quantity
this campaign moves: placeholders went 6,057 → 4,704, and every floor loose enough to survive that is loose
enough to miss a regression of several hundred. So `CensusRatchet` fails on a regression **and on an
unrecorded improvement**: a bound nobody tightens rots into a floor, and the only reliable moment to tighten
it is the run that beat it.

| pinned 2026-09-22 at `29e951ea1` | detekt | coil |
|---|---|---|
| placeholders | **4,704** (in 790 of 1,384 types) | **367** (in 69 of 186) |
| isolated by prep | **0** | **0** |
| immutable types | **668** | — |

⚠ The corpus runs can only ever exercise the ratchet's passing side, so both failing sides are proved
separately in `TestCensusRatchet`, free of any checkout.

**3. A mixed-language regression, owned here.** §7.17's instrument compares Kotlin → Java. The direction the
downstream planners actually hit is the other one — **Java calling Kotlin** — and it was covered only at the
PARSE level (`TestMixedHardening`: void/Unit, companions, facades, extensions, varargs, generics). A parse is
not a verdict, and the stubs javac resolves Kotlin through are maddi's own, so the boundary belongs here.
`TestMixedBoundaryVerdicts` asserts a matched pair: Java calling a modifying Kotlin method sees its
parameter modified, **and** Java calling a read-only one does not — without the second, "everything is
modified" would pass the first. `KBox`'s own verdicts are asserted first, so a failure says which side broke.

⭐ The census guard earned itself immediately: the fixture's first draft used `ArrayList`, which that source
set cannot resolve (the dependency runs Java → Kotlin here, so Kotlin has no JDK), and the run was refused
as vacuous rather than passing on three placeholders.

### 7.21 ⭐ A real Kotlin round trip — and two properties that made a results file unreadable

§5.1 recorded "no encode path: no Kotlin module references `Codec`". Rung 6 asks whether a Kotlin analysis
can be written and read back — the mechanism under incremental analysis and the IDE daemon.
`TestKotlinAnalysisRoundTrip` does it end to end: one session parses, analyses and writes; a second,
completely fresh session parses the same sources, runs NO analysis, and loads what the first wrote.

    fresh  : add nonModifying=false params=n:false; setCount …value:false; size nonModifying=false
    before : add nonModifying=false params=n:true ; setCount …value:true ; size nonModifying=true
    after  : add nonModifying=false params=n:true ; setCount …value:true ; size nonModifying=true

Four things the probe found, none of them guessable from the gap list:

1. ⭐ **The encoder already handles Kotlin.** The writer is language-agnostic and produced a complete
   `A.json` with the real verdicts on the first try. "No encode path" meant nobody *calls* it.
2. A Kotlin type is only resolvable by FQN when the configuration **also has a Java source set**. With a
   Kotlin-only configuration `runtime.getFullyQualified` returns null and every hint is skipped as "type not
   on the classpath" — which is how the first attempt read back 0 of 1 types while looking like a decode bug.
3. ⛔⛔ **Two properties the analysers write were unknown to the decoder**, which does not degrade: it
   asserts, and the ENTIRE file is lost, not just that value. `DEGRADED_ANALYSIS_METHOD` (written by
   `LinkComputerImpl` and `SingleIterationAnalyzerImpl`) and `INDEPENDENT_TYPE_PARAMETER` (written by
   `ShallowTypeAnalyzer`) were declared in `PropertyImpl` but never registered in `PropertyProviderImpl`.
   Java-side defects both, found once more through the Kotlin work.
   `TestEveryWritablePropertyDecodes` now removes the class of defect: every declared property must resolve.
   ⚠ Its first draft invented an exemption — "INTRINSIC means never persisted" — and `FINAL_FIELD`, which is
   INTRINSIC *and* has always been registered, refuted it on the first run. `AnalysisTier` grades reload
   cost, not persistence; there is no exemption.
4. The reader must be paired with the writer. `LoadAnalysisResults` lives in maddi-modification-prepwork,
   which structurally cannot know `methodLinks` (declared in maddi-modification-link, which prepwork must
   not depend on). `LinkCodec.restoreCodec()` is the matching read side.

⚠ Two traps in the test itself, both caught by its own controls rather than by review. The fingerprint's
first version filtered out lines matching `nonModifying=false params=` "to drop empty ones" — it dropped
`add`, the only modifying method, leaving a round trip that compared one read-only method with itself. And
the negative control first asserted the fresh type carries NO verdicts; it carries **defaults**, which is
exactly what a decode that did nothing would leave, so the control had to become "the fresh fingerprint
must DIFFER from the analysed one".

⚠ Also visible in the written JSON, and not chased here: Kotlin's synthesized property setter
`MsetCount(1,int)` carries `degradedAnalysisMethod` — the analysis of that accessor was abandoned. It
round-trips faithfully, but it is a verdict worth a look on its own.

**And then wired.** `RunMixedPrepAnalyzer.Options` gained `analysisResultsTargetDir`, and the mixed CLI now
honours `--analysis-results-dir` — an option it used to REFUSE by name with exit
{@code UNSUPPORTED_OPTION}. `TestOneEntryPoint` carried that refusal as an assertion, so the change shows up
there as a test that had to move: the refusal case now uses `--incremental-analysis`, which genuinely
remains unsupported, and a new case asserts the results directory is written. `TestMixedMain` adds the
end-to-end check with the half that matters — a run WITHOUT the option must write nothing, or the assertion
would pass on a directory something else filled.

⛔ The codec choice is load-bearing and easy to get wrong silently: `WriteAnalysisResults`' two-argument
overload builds a prep-work codec that cannot know `methodLinks`, so results written with it are unreadable
— and the reader does not degrade, it asserts and loses the whole file. The runner passes `LinkCodec`
explicitly, with the reason in a comment beside it.

**Scope.** ⚠ `--incremental-analysis` is still refused, deliberately. Being able to write and read results
is not the same as being able to consume them to SKIP work: that needs the rewire and fingerprint
machinery, which is a separate question. What rung 6 can now claim is that the persistence layer underneath
incremental analysis and the IDE daemon works for Kotlin, is reachable from the CLI, and has a test
standing on each half.

### 7.22 Kotlin lambdas vs Java lambdas — the same verdict, and a "mixed-pipeline gap" that was the fixture

§5 item 2 predicted a divergence: `VirtualFieldComputer` excludes `java.util.function`, so a Kotlin lambda
(`Function1`) takes a different path than a Java one (`Consumer`). `TestKotlinLambdaVsJavaLambda` asks whether
it is a different ANSWER, with three paired rows: a higher-order call through each language's own function
type, a SAM-converted lambda handed to a Java interface, and a read-only control. **All three agree on every
sensor**; `higherOrder` sees `b` modified through `(String) -> Unit` exactly as through `Consumer<String>`.

⛔ The test sat `@Disabled` behind a pinned "gap": in the mixed pipeline `ArrayList<String>().add(t)` was
three placeholders, and adding kotlin-stdlib to the classpath "changed nothing". The jar it added was
`kotlin-stdlib-jdk8-2.4.0.jar`, the first file name containing `kotlin-stdlib`, which carries none of
`kotlin.collections`. K2 answered `Unresolved reference 'ArrayList'` (it is a stdlib typealias). With the real
jar: zero placeholders. The refutation had measured the jar next to the question. The fixture now selects the
jar by exact name and checks it holds `kotlin/collections/CollectionsKt.class`.

### 7.23 Property references — the getter, bound or unbound

`Q::i` used as a function is `(Q) -> Int`, which a Java author writes `Q::getI`, so a property reference now
becomes a `MethodReference` to the getter the front end already builds, found exactly as a property ACCESS finds
it (`resolveAccessor`). The bound/unbound rule is the function reference's (§7.19): `Q::i` is scoped to the type,
`q::i`/`this::i`/`::i` to the value. Extension properties reference the facade's static getter, top-level ones
the facade getter. The receiver helper now also recognises a qualified or generic type (`java.util.ArrayList<String>::size`),
which function references gain too.

⭐ **Verdict level**, three new rows in `TestLoweredShapesVsJava` against a Java getter that modifies
(`Box.getCount()`, which Kotlin sees as the property `count`): `propBound` (`c::count` vs `c::getCount`) marks `c`
modified on both sides; `propUnbound` (`Box::count` vs `Box::getCount`) does not, on both sides (the engine's
documented conservatism for an unbound receiver); `propLibrary` (`ArrayList<String>::size`) reaches the class
file's `size()`. An identity check asserts each side holds one method reference to the SAME method.

⛔ Still named placeholders: a property with no getter method (`private`, `const`: read as the field, and a
method reference cannot name a field), a bound extension reference (`s::lastIndex`, no Java spelling), a
top-level non-extension library property. ⚠ In a standalone `KotlinScan` (no `CompiledTypesManager`), library
properties are loaded as FIELDS, so `StringBuilder::length` has no getter there; the shipping pipeline loads
class files, where it is a method. Pinned in `CallableReferenceTest`.

**Measured**, against a control run of the parent commit that reproduced the pins exactly: detekt
**4,704 → 4,701**, all three `k2-callable-ref-property` sites converted, **zero new sites**, nothing else moved;
coil 367 unchanged (it has none). Small, because the corpora hardly use the shape: the callable-reference
family's remainder on detekt is **37** `k2-callable-ref-unresolved` sites (`toRegex` 18, `pathGlobToRegex` 8),
which are the next target in this family, not property references.

### 7.24 Annotations — converted, placed where Java sees them, and the engine defect they exposed

§4's "largest single hole" is closed: annotations on Kotlin declarations are converted (`KotlinAnnotations.kt`) in
the shapes the Java class-file reader builds (`ClassSymbolScanner.annotationExpression`) — constants, `ArrayInitializer`
of `IntConstant`/`StringConstant`, enum entries as field references, class literals, nested annotations — which are
the shapes the contract reader casts to.

⭐ **Placement is K2's, measured before it was written.** K2 already applies Kotlin's use-site rules per symbol:
`@get:`/`@set:`/`@field:`/`@setparam:` sit on the getter/setter/backing-field/setter-parameter symbol, a Java
annotation on a class-body property on its backing field, a no-target one on a constructor `val` on the parameter
and the field, and a `PROPERTY`-only one on the property alone, which has no JVM element and so lands on no CST
element — as with kotlinc. So each CST element simply copies the annotations of the symbol it is built from
(`AnnotationPlacementTest`, each case asserting the neighbours it must NOT land on too).

⚠ Library enums had no constants in a scan without a class-file loader (`KotlinInspector`, the pure-Kotlin path):
K2 models a Java enum's constants as enum entries too, which `loadLibraryMembers` skipped. They are now fields.

**Verdict level:** `TestKotlinContractsVsJava` — the same contract annotations on a Kotlin interface and on its
Java twin, with callers, and an unannotated twin per row so a contract that changes nothing fails the test. All
rows agree across the two languages; `@Modified` on a parameter and `@NotModified` on a method move the verdicts.
Corpora unchanged: detekt 4,701 / coil 367 placeholders, ratchets green.

⛔⛔ **The Java-engine defect it found.** On both sides, a `@NotModified` parameter of an abstract method made the
CALLER's argument *modified*, where leaving the annotation out did not. Cause, traced with `RETAINTRACE`: the
abstract method's shallow link summary is computed while the parameter is undecided (read as dependent,
`b.§m ≡ this*.§m`); once it is decided `@Independent` the recomputed summary is `[-]`, and `methodLinks`
retention keeps the RICHER of two equal-keyed values — equal because the contract kept `b` out of the modified set
from the start. Without the annotation the modified set shrinks, the values compare unequal, and the fresh one wins.

A latest-wins fix for abstract-method summaries works (`TestAbstractSummaryFollowsDecisions`; fernflower 0 verdict
changes, clone-bench pins unchanged) but on guava moves 59 elements, all optimistic: ~half real corrections
(`Hasher.putBytes`' bytes, `BaseEncoding.encode`'s input), ~20 unsound (`ForwardingList.add`, `Maps.EntrySet.clear`,
…). Those reach their state through an abstract accessor (`delegate()`), and
`AbstractMethodAnalyzerImpl.doMethodWithoutImplementation` decides an UNIMPLEMENTED abstract method `@Independent`
— which the stale summary had been masking. A second defect sits beside it: a `@Dependent` abstract accessor whose
summary has no return link (`Fwd.add` is non-modifying today, fix or not). **Parked** on branch
`park/abstract-summary-latest-wins`, waiting on the ws/dsl work on abstract methods without implementations, which
answers the same question — what "no implementation" means — the other way for non-modification. The defect is
pinned in `TestKotlinContractsVsJava` as it is.

### 7.25 ⭐ The unresolved references were a class-file-shell defect — detekt 4,701 → 3,434

The 34 `k2-callable-ref-unresolved` sites split, by reading their source, into four shapes: an extension function
through its type (`String::toRegex` 18, `String::pathGlobToRegex` 8), local functions (5), an extension bound to an
implicit receiver (3), and `Path::toUri`/`Path::toFile`/`::FqName`, which should simply have resolved.

⛔⛔ **The last group was not about references.** A probe through the mixed pipeline showed `p.toUri()` and
`URI("x")` failing too: `java.nio.file.Path` and `java.net.URI` reached the Kotlin converter with ZERO members. The
Java front end registers a type it meets in another type's signature (`File.toPath()` names `Path`) as a SHELL,
hierarchy only, and completes its shells in a batch when its parse commits. In a mixed project the Kotlin parse runs
after that commit, and `CompiledTypesManager.type()` hands a registered shell over as it is. `java.util` types are
preloaded whole, which is why every fixture built on `ArrayList` looked fine.

Fixed without touching the Java front end's behaviour: `CompiledTypesManager.typeWithMembers` completes a shell
through the existing lazy loader, and the Kotlin converter calls it where members are LOOKED UP (method, constructor
and field lookups), so a type that is only named stays a shell. Extension references become the facade's static
method, receiver first (`StringsKt::toRegex` in Java); bound extensions and local functions keep NAMED placeholders
(`k2-callable-ref-bound-extension`, `-local-function`).

| detekt family | before | after |
|---|---|---|
| `k2-unresolved-call` | 2,446 | 1,796 |
| `k2-unresolved-access` | 864 | 279 |
| `k2-ctor-unresolved` | 145 | 7 |
| `k2-callable-ref-unresolved` | 34 | 0 |
| **total** | **4,701** | **3,434** (types holding one 790 → 579) |

Immutable types 668 and prep isolation 0, both unchanged; coil 367 → 362. ⚠ 92 new distinct sites are REVEALED
(an unresolved call swallows its arguments); all but one sit in a member that already held a placeholder, and that
one is itself a reveal of a SILENT drop: `class FindingAssert(…) : AbstractAssert<…>(actual, FindingAssert::class.java)`
— the super call to a shell (assertj) had no constructor to bind and vanished without a placeholder. ✅ Closed in the
next commit: the super call's target is completed like any other member lookup, and one that still cannot bind is a
NAMED placeholder statement (`k2-super-call-unresolved:<type>`, `k2-super-call-no-parent`) rather than nothing.
Whether it had bound was ORDER-dependent — a body call resolving up the hierarchy completed the parent first — so
detekt and coil show no change (0 such placeholders, counts identical), and `TestLibraryShellMembers` forces the
order that dropped it (`URL` loaded before a subclass of `URLStreamHandler`, `Format` before one of `ParsePosition`).

⭐ This retires part of §7.5b's reading: the "members of library types" family was in large part not K2 knowing
something the CST could not express, but the CST's library types being EMPTY at the moment of conversion.

### 7.26 Members of an implicit receiver — detekt 3,434 → 2,256

The largest remaining families were calls and reads with NO written receiver whose receiver is not the class's own
`this`: `append("# " + t)` inside `fun Md.h1()`, `configPaths` inside `with(configSpec) { … }`, the assignments inside
a builder lambda. The name-based lookup searched the enclosing class (and a field of the extension receiver), so a
method, or a property that is only an accessor on the JVM, fell through to a placeholder. K2 names the receiver
(`dispatchReceiver` of the resolved call or variable access); the converter now maps it through the same routine
the implicit extension receiver already used (class `this`, an enclosing class's `this`, or the `$receiver` of the
lambda / extension function whose type K2 names) and looks the member up there, field before accessor, exactly as a
qualified `obj.x` is converted. The class's own `this` stays on the old path, unchanged.

| detekt family | before | after |
|---|---|---|
| `k2-unresolved-call` | 1,796 | 1,159 |
| `k2-unresolved-ref` | 1,040 | 540 |
| `k2-assign-target` | 40 | 3 |
| **total** | **3,434** | **2,256** (types holding one 579 → 434, members 1,609 → 864) |

coil 362 → 335. 15 new distinct sites are reveals, none in a previously clean member (e.g. a top-level property
of ANOTHER file, `LIST_ITEM_SPACING`, inside a `debug { }` lambda that used to be swallowed whole — the same gap as
`NL`, next on the list). Immutable types 668 → 667, three types changed, none of them holding a changed site
itself: `dev.detekt.core.Analyzer` @FinalFields → @Immutable(hc=true), and `AnalysisFacade` plus its interface
`Detekt` @Immutable(hc=true) → @FinalFields. Each is transitive, from code the analysis did not read before
(`EnvironmentFacade`'s init lost nine assign-target holes; `withSettings`, `loadConfiguration`, `extractUris` now
read `loggingSpec`, `configSpec`, `resources` on the implicit receiver) — the verdict the Java spelling of the same
code would get, which is this document's criterion, not a judgement of the engine's precision there.
`ImplicitReceiverTest` fails four of its six without the change; the lambda-receiver CALL was already converted
(the lambda's `$receiver` is in scope), and is kept as a guard.

Still open in this family, from a probe of shapes: a primitive receiver (`i.toString()`, 72 on detekt),
`b.not()`, a member extension through an implicit dispatch receiver (`"x".ext()` inside `analyze(s) { }`, ~70 on
detekt's `resolveToCall`/`resolveToSymbol`/`isSubtypeOf`), `x?.own()` on a class-level member extension,
`arrayOf`, invoking a function type with receiver (`s.block()`), and a top-level property of another file.

### 7.27 Member extensions — detekt 2,256 → 1,351

A function or property declared as an extension INSIDE a type has two receivers: the extension receiver, written, and
the dispatch receiver, always implicit. detekt's whole Analysis-API surface is this shape --
`expression.resolveToCall()` inside `analyze(expression) { }` dispatches on the lambda's `KaSession` -- and the
converter knew only the facade route of a top-level extension, so every such call or access was a placeholder that
swallowed its receiver and arguments. On the JVM it is an instance method of the declaring type (or a supertype:
`resolveToCall` lives on `KaResolver`) with the extension receiver as argument 0; `expression.expressionType` is
`$receiver.getExpressionType(expression)`. The dispatch receiver is mapped by the same routine as §7.26.

| detekt | before | after |
|---|---|---|
| `k2-unresolved-call` | 1,159 | 402 (`resolveToCall` 246 → 59, `resolveToSymbol` 25 → 0, `isSubtypeOf` 16 → 0) |
| `k2-unresolved-access` | 281 | 85 (`expressionType` 58 → 11) |
| `k2-unresolved-ref` | 540 | 594 (reveals: bare names inside calls that used to be swallowed whole) |
| **total** | **2,256** | **1,351** (types 434 → 371, members 864 → 621) |

coil 335 → 327. 22 new distinct sites, none in a previously clean member.

⭐ Parity is measured, not argued: `TestLoweredShapesVsJava` has a `KReport` / `JReport` pair (a member extension
property, a member extension on `Any`, a modifying member extension on `Box`) and agrees on every method row and the
type. Its first run disagreed on the type alone -- Kotlin IMMUTABLE, Java IMMUTABLE_HC -- because the Java fixture
was not `final` and a Kotlin class is; an extensible type has hidden content.

⚠ Immutable types 667 → 665: `dev.detekt.api.OutputReport` and `CheckstyleOutputReport`, @Immutable → @FinalFields.
The one changed site in the report module is `filePath.invariantSeparatorsPathString.toXmlString()`, now
`this.toXmlString(…)` with a placeholder argument (`filePath` is a destructured lambda parameter, §7.26's list). Two
probes rule the class out: a placeholder argument costs a type nothing, and `CheckstyleOutputReport` reproduced
verbatim with stub interfaces stays @Immutable with and without this change. What did change is
`HtmlOutputReport` -- its private `FlowContent.renderGroup/renderRule/renderIssue` member extensions are now read --
and `OutputReport`'s verdict is the engine's aggregate over its implementations, which `CheckstyleOutputReport`
inherits. That aggregation is the area parked on ws/dsl (abstract-method summaries, defects A and B), not a lowering.

Next in the family: the 59 `resolveToCall` left are MIXED, and only partly sorted -- functions with a context
parameter (`context(session: KaSession)`, 13 in `SuspendFunSwallowedCancellation` alone; `session` is also 38
unresolved refs), calls whose extension receiver is implicit too (`analyze(this) { resolveToCall() }`), and calls in
nested lambdas not yet read. Then destructured lambda parameters (`(filePath, issues) ->`), bare member-extension
properties (`type`, `returnType`), and §7.26's list.

### 7.28 Which implicit receiver: nesting and smart casts — detekt 1,351 → 1,083

Two ways §7.26's receiver mapping named no receiver, so the member stayed a placeholder:

- **Nesting.** A receiver lambda held its receiver as `$receiver`, the innermost one only, so inside
  `with(b) { with(session) { size } }` the outer `Box` was out of reach. K2 names the function literal that owns the
  receiver (`owningCallableSymbol`); each receiver lambda now also keeps its receiver under that literal's key, and
  the lookup is exact. The innermost-by-type fallback remains, and now also tries the extension function's own
  `$receiver` (`bodyExpression` inside `with(session) { }` in `fun KtNamedFunction.f()`).
- **Smart casts.** `when (this) { is KaClassSymbol -> classId }`: the receiver is the declared `KaSymbol`, the member
  is `KaClassSymbol`'s. The member is looked up on the narrowed type K2 gives the receiver value -- the convention a
  WRITTEN smart-cast receiver already follows in `convertQualified` (its `expressionType`), no cast node.

| detekt | before | after |
|---|---|---|
| `k2-unresolved-ref` | 594 | 425 (`selectorExpression` 28, `classId` 22, `bodyExpression` 18 → 0) |
| `k2-unresolved-call` | 402 | 304 (`resolveToCall` 59 → 0) |
| **total** | **1,351** | **1,083** (types 371 → 360, members 621 → 586) |

coil 327 → 323. 4 new distinct sites, none in a previously clean member; no verdict moved (665 immutable types).
The `resolveToCall`s inside context-parameter functions resolved as well: they sit in `with(session) { … }`, whose
lambda receiver is typed even though `session` itself is still a placeholder -- the context parameter is the next
gap, and it is a SIGNATURE gap before it is a call one (`context(session: KaSession) fun f(x)` is `f(KaSession, x)`
on the JVM, and the scan declares `f(x)`).

### 7.29 Context parameters — a signature gap first, detekt 1,083 → 1,045

`context(session: KaSession) fun f(x: X)` (47 declarations in detekt, none in coil) was scanned as `f(X)`: the context
parameter was not modelled at all, so every reference to `session` in the body was a placeholder, and the method's
signature contradicted the class file. kotlinc compiles context parameters as the LEADING parameters, ahead of an
extension receiver -- measured with javap on kotlinc 2.4.0, not recalled: `top(Session, String)`,
`ext2(Session, Box, int)`, a context property's getter `getProp(Session, Box)`, `Host.member(Session, String)`.
Kotlin makes them no implicit receiver (`x.memberExt()` does not compile against one; detekt writes `with(session)`).

Declarations now carry them, in that order, on every signature a receiver is added to: the function, its `$default`,
its overloads, and computed and custom accessors. A call passes K2's `contextArguments` first on every route -- a
plain, facade, extension, member-extension call, and an extension property's getter -- each mapped by the implicit-
receiver routine, which now also names a context parameter passed on (`relay(x) = top(x)` passes relay's own `s`).
One that cannot be expressed is a named placeholder, `k2-context-argument-unresolved:<name>`.

detekt: `session` 38 → 0; total 1,083 → 1,045 (types 360 → 359, members 566); no new site (compared on member
names: the members' own signatures changed, which is the point), no verdict moved, coil unchanged at 323.
`TestLoweredShapesVsJava` gains `ctxModifies` / `ctxCaller`: a modification of the context parameter, and one
passed on through it, agree with the Java that spells the parameter first.

### 7.30 `super` with two supertypes, a Java parent's default constructor, implicit extension properties — detekt 1,045 → 849

Three defects, each found by probing a corpus site before building anything:

- **`super.visitX(…)` in a class that also implements an interface.** 112 of detekt's 333 `super.visitX(…)` calls
  were placeholders -- exactly the rules declared `: Rule(…), RequiresAnalysisApi`. With one supertype, `super`'s
  expression type is the superclass; with two it is not, and the callee was looked up where it is not declared. K2's
  resolved call names the supertype on its dispatch receiver, and `convertQualified` now uses it for `super`.
  Reproduced with a two-line fixture before the fix; a library parent (`KtTreeVisitorVoid`) and a Java source parent
  both bind to the PARENT's method, not to the override (`TestSuperCallTargets`).
- **`class D : V()` where the Java class `V` declares no constructor** was `k2-super-call-unresolved:V`. The Java
  front end models the generated default constructor as synthetic (`SYNTHETIC_CONSTRUCTOR`), and the target search
  skipped every synthetic constructor to avoid kotlinc's overloads -- which are synthetic but of the ordinary
  constructor type. It now skips only those. Found by the probe, not by a corpus count: neither corpus has the shape.
- **An extension property on an implicit receiver**, `containingClassOrObject` inside `fun KtProperty.f()`: the bare
  name looked only at the dispatch receiver. It now converts to the getter a written `recv.prop` does, top-level or
  member extension (`containingClassOrObject` 18, `mainReference` 14, `expressionType` 11 → 0).

detekt: `k2-unresolved-call` 304 → 192, `k2-unresolved-ref` 387 → 303; total **1,045 → 849**, types holding one
**359 → 297** -- for 62 rules the `super` call was the only hole. coil 323 → 316. No new site, no verdict moved
(665 immutable types).

### 7.31 Members of a primitive — detekt 849 → 755

`i.toString()` (72 on detekt), `b.not()` (17), `i.toLong()`: a member of `kotlin.Int` or `kotlin.Boolean` on a
receiver the CST types as `int`/`boolean`, where no Java type declares it. Each now becomes the Java a human writes,
which is also what kotlinc compiles -- read with javap on 2.4.0, not recalled: `String.valueOf(i)`, `!b`, a primitive
conversion (`i2l`, `i2d`, `i2c`), `Integer.hashCode(i)`, and `i + j` for `i.plus(j)`. Two differ from kotlinc on
purpose, because the question is what the equivalent Java would be analysed as: `i.compareTo(j)` is
`Integer.compare(i, j)` (kotlinc: `Intrinsics.compare`), `i.equals(j)` is `i == j` (kotlinc boxes both sides).
Overloads are matched on the EXACT parameter type, so that no widening can bind `valueOf(char[])`; a mixed-type call
(`i.compareTo(l)`) keeps its placeholder. A nullable receiver is boxed and keeps binding to the box's own member.

detekt: `toString` 72 → 0, `not` 17 → 2, `toLong` 4 → 0; `k2-unresolved-call` 192 → 98; total **849 → 755**
(types 297 → 288). coil 316 → 310. No new site, no verdict moved.

### 7.32 Top-level properties of another file or a library — detekt 755 → 657

A bare `NL` (detekt's own, another file) or ktlint's `INDENT_SIZE_PROPERTY` (36×, a library) resolved only when the
property lived on the method's OWN facade. It is now read through its facade as Java reads it: a `const val` or
`@JvmField` as the static field, anything else through the static getter (`CoreKt.getNL()`,
`…Kt.getINDENT_SIZE_PROPERTY()`). The library facade builder made getters only for EXTENSION properties; it now makes
them for plain ones too, and fields for public consts.

Two defects of my own, both caught before commit and both pinned in `TestTopLevelPropertyReads`:
- the first corpus run **crashed detekt's parse** (NPE in `Access.level()`): a const field computed its access
  against the library facade's, which is set only at the facade's commit. The facade now computes its access first.
  The unit fixture had missed it -- its facades held no const next to a called function -- so the regression uses the
  shape the crash trace named (`DurationKt`: `toDuration` beside the internal const `NANOS_IN_MILLIS`), and fails with
  the NPE when the fix is reverted.
- the stdlib's `SequenceBuilderKt` holds PRIVATE consts (`State_Ready`, …): they were being built as public fields. A
  private top-level property has no getter and no reader outside its file; the facade skips them.

Pinned, pre-existing, and not changed here: a `const` is folded to its VALUE before this route is reached (`MAX` → `3`,
`PI` → `3.14…`); and a library facade is the multi-file PART class (`IntrinsicsKt__IntrinsicsKt`,
`MathKt__MathJVMKt`) where Java names the facade (`IntrinsicsKt`) -- the locator every library top-level function
shares. Also seen: `kotlin.math.sqrt` is `@InlineOnly` (private in bytecode) and stays unresolved.

detekt: `INDENT_SIZE_PROPERTY` 36, `MAX_LINE_LENGTH_PROPERTY` 16, `NL` 3, `LIST_ITEM_SPACING` 2 → 0;
`k2-unresolved-ref` 303 → 205; total **755 → 657** (types 288 → 233). coil 310 → 305. No new site.
⚠ Immutable types 665 → **666**: `IgnoreAnnotatedKt` (a facade holding `val ignoreAnnotatedDefaults:
Array<IgnoreAnnotated> = arrayOf(…)`) @FinalFields → @Immutable, once its one reader (`printRule`'s
`ignoreAnnotatedDefaults.firstNotNullOfOrNull { … }`) resolved to the getter. NOT explained: a minimal reproduction
(a top-level array read from another file, with and without the reader, against the Java facade) stays @FinalFields
on both sides, so the lowering is at parity there; what differs in detekt (an abstract element type whose only value
is a private object) was not pursued. Recorded, not accepted as understood.

### 7.33 A library class name as a value: its companion — detekt 657 → 591

`ClassId.fromString(…)` (18× on detekt), `CompilerConfigurationKey.create(…)` (12×): the companion's MEMBER resolved,
the receiver did not -- a class name used as a value, which Kotlin means as the class's companion object (or, for an
`object`, the object itself). Found by printing the probe's holder, after two guesses at the cause were wrong: K2
resolves the name to the COMPANION symbol, and the K2-built model of a Kotlin LIBRARY class carried no static
`Companion` field (nor an `object`'s `INSTANCE`) to read it through -- the class file declares both. The model now
does, and the name converts to `ClassId.Companion`, `Charsets.INSTANCE`; the holder is reached through K2
(`outerClassId`), since a library companion is loaded as a type of its own with no enclosing type.
Pinned in `TestLibraryCompanions`, which fails with all six placeholders when the change is reverted.

detekt: `ClassId` 18, `CompilerConfigurationKey` 12, `KtlintWrapperProvider` 6 → 0; `k2-unresolved-ref` 205 → 139;
total **657 → 591** (types 233 → 208). coil 305 → 299. No new site, no verdict moved.

### 7.34 Destructuring in a lambda's parameters, and the stdlib's inlined components — detekt 591 → 467

`xs.partition { (_, rule) -> rule.autoCorrect }`: a destructured lambda parameter was not modelled, so every entry
(`rule`, `ruleInstance`, `key`, `value`, …) was an unresolved reference -- the bulk of detekt's remaining 139. It is
ONE parameter on the JVM (`$dstr0`), and the body now starts by reading each entry from it, through the same routine
`val (a, b) = x` uses; `_` declares nothing. That routine also learned the stdlib's `@InlineOnly` components, which
are absent from bytecode and compiled to what they inline: `Map.Entry`'s `getKey()`/`getValue()`, a `List`'s `get(N-1)`.
Chosen only when K2 resolves the entry to that extension. `TestDestructuring` fails with five placeholders when the
change is reverted.

⚠ A correction to my own expectation: I took the `k2-component1/2` placeholders (7 + 7) to be `Map.Entry`; they were
not -- 14 → 11. The rest are an initializer the lowering types differently (`val (a, b) = f() ?: return`,
`= when (…) { … }`) and `Regex`'s `MatchResult.Destructured`, which are not this route.

detekt: `k2-unresolved-ref` 139 → 23; total **591 → 467** (types 208 → 197, members 302 → 280). coil 299 → 288.
No new site, no verdict moved. (The printer shows a multi-entry declaration with the first entry's type; each
variable keeps its own -- the shape `val (a, b) =` always had.)

### 7.35 A class name called, and `arrayOf` — detekt 467 → 444

- **`RuleSet(id, rules)`** (12× on detekt) is not a constructor: `RuleSet`'s companion declares `operator fun
  invoke(id, rules: List<…>)`, and Kotlin calls it through the class name. It is `RuleSet.Companion.invoke(…)`
  (`Twice.INSTANCE.invoke(…)` for an `object`), built on §7.33's class-name-as-value.
- **`arrayOf(a, b)`** (8×) is an intrinsic with no bytecode of its own; it is `new T[]{a, b}`, in the exact shape the
  Java front end gives that expression (an array-creation constructor, one empty dimension, an initializer). The
  primitive `intArrayOf`-style builders too; not with a spread (`arrayOf(*xs)` copies).

Both pinned (`TestLibraryCompanions`, `TestPrimitiveMembers`), each failing with its placeholders when reverted.
detekt **467 → 444** (types 197 → 176); coil unchanged at 288. No new site, no verdict moved.

What is left of the unresolved families on detekt is small and heterogeneous: `k2-unresolved-call` 75 (`yield` /
`yieldAll` in `sequence { }` 18 -- the `suspend` work, not this family), `k2-unresolved-access` 82,
`k2-unresolved-ref` 23. The largest family is now `k2-unsupported-expr` (164: class literals, local functions, …) --
unmodelled syntax, the next ladder rung, not resolution.

### 7.36 Class literals, and a Java library class's static methods — detekt 444 → 358

`X::class` was unmodelled syntax, the largest `k2-unsupported-expr` kind (89 of 164). `X::class.java` (45) is now the
Java class literal `X.class` -- kotlinc compiles it to an `LDC`, so the `KClass` is never built; a bare `X::class`
(a `KClass`) is `Reflection.getOrCreateKotlinClass(X.class)`, the stdlib call kotlinc emits. A reified type parameter
(`T::class` in an inline function) has no Java spelling -- kotlinc substitutes the argument at each call site -- and
keeps its placeholder (3 on detekt).

Building the `KClass` form exposed a wider gap: the K2-built model of a JAVA library class (here
`kotlin.jvm.internal.Reflection`) had no static methods at all -- the loader read static FIELDS from the static member
scope, but functions only from the instance scope. It now reads both. No new site and no verdict moved on either
corpus, so nothing that resolved before resolved differently.

⚠ `K::class.simpleName` reads `simpleName` as a FIELD of the `KClass`: the K2-built model of a library Kotlin type
keeps properties as fields (pre-existing), where Java would call `getSimpleName()`.

detekt: class literals 89 → 3; `k2-unsupported-expr` 164 → 78; total **444 → 358** (types 176 → 160). coil 288 → 283.

### 7.37 Jumps in expression position, and annotated expressions — detekt 358 → 323

- **The control-flow elvis** (§7.10) accepted `?: return` and `?: throw` only, and only in a declaration or a
  `return`. It now also takes `?: continue`, `?: break` and a labelled `?: return@label v` (from the lambda, as the
  lambda's own `return@label` statement already converts), and an ASSIGNMENT, `x = f() ?: return false`. Same guard,
  same single evaluation of the left operand.
- **A `throw` as a lambda's last expression** (`e?.let { throw it }`) was wrapped as `return <throw>`; it is a
  statement.
- **`@Suppress("…") expr`** (18 on detekt, all `@Suppress`): an annotation on an expression has no run-time meaning
  and no Java spelling. The base expression is converted, as a statement and as a value.

`ControlFlowElvisTest` gains three cases, failing with five placeholders when reverted. detekt: `k2-unsupported-expr`
78 → 40 (left: local functions 27, a `return`/`throw` in an argument or other hoist-less position 9, reified class
literals 3); total **358 → 323** (types 160 → 143). 2 reveals (`Show`/`Hidden` under a formerly swallowed annotated
`try` in `AnalysisFacade`), no new member, no verdict moved; coil unchanged at 283.

### 7.38 Local functions — detekt 323 → 277

A local `fun` is lowered to a local variable of type `FunctionN<boxed parameters, boxed result>` whose value is an
anonymous implementation with an `invoke` method (an extension local takes `$receiver` first). A call to it is
`g.invoke(..)` on that variable, and `::g` is the variable itself. That is what kotlinc emits, minus the class name,
and what a Java programmer writes with a lambda. `LocalFunctionTest` (k2) fails with six placeholders when the change
is reverted; `TestKotlinLambdaVsJavaLambda` gains `localCaptures` and `localReads`, both agreeing with the Java
lambda. `FunctionN` is a stdlib type, so without the jar a local function stays a placeholder, and the rows cannot
live in the stdlib-free `TestLoweredShapesVsJava`.

detekt: all 27 `k2-unsupported-expr:KtNamedFunction` sites, the 5 local callable references and 25 calls to local
functions go (57); 11 appear, all inside bodies read for the first time (`yield`/`yieldAll` 5, `resolveToCall` 2,
`getArgumentExpression` 2, a `try` in a hoist-less position, and one `psi` access whose position is `0:0`, owed a
look). Total **323 → 277** (types 143 → 134, members 190 → 176); coil has no local functions and stays at 159.

**Verdicts: 666 → 667, every move explained**, and it took `FPDUMP`/`FPDUMP_PARAMS` on both sides to explain them.
Chains that ended in an unread local function had been **undetermined** (`nonModifying=null`); an undetermined
link never reports a modification, so the verdicts above it were resting on nothing. Now they resolve:

- `FunCoroutineLaunchesTraverseHelper` ↓ `@FinalFields`: its local `checkFunctionAndSaveToCache` writes the field
  `exploredFunctionsCache`. A reveal.
- `Analyzer` ↓ `@FinalFields`: `shouldAnalyzeFile` resolves, and its `Config` receiver is modified through
  `createPathFilters()`, whose receiver was already modified in the base (`Config.valueOrDefault`). A reveal: the
  type had been held up by a verdict nobody derived.
- `AnalysisFacade` and the `Detekt` interface ↑ `@Immutable(hc=true)`: `run`/`runAnalysis` go from `null` to
  non-modifying.
- `PathFilters` ↑ `@Immutable(hc=true)`, and this one needed a contract first. Its local
  `fun isIncluded() = includes?.any { it.matches(path) } ?: true` marked `includes` modified. The local function
  was innocent: the same body written inline gives the same verdict, and Java's `stream().anyMatch(..)` does not.
  `Iterable.any` had no contract, and an uncontracted receiver is a modified one. `any`, `all` and `none` are now
  contracted (b1f2a29d6, `TestKotlinPredicatesVsJava`, negative control included).

⚠ **Open, found on the way: invoking a function VALUE with an argument.** `fun p(b: Box, f: (Box) -> Unit) { f(b) }`
leaves `b` unmodified; Java's `Consumer<Box>.accept(b)` marks it modified. `java.util.function.Consumer.accept` is
in the JDK annotated API (its `arg0` is not `@NotModified`); `kotlin.jvm.functions.Function1.invoke` has no entry,
and an unannotated library parameter reads unmodified. It predates local functions (a function-typed PARAMETER
shows it) and is the next contract to write: `Function0`…`FunctionN.invoke` as the JDK's functional interfaces are.
✅ Closed by §7.40.

### 7.40 `kotlin.jvm.functions`: a function value's argument may be modified

`KotlinJvmFunctions` contracts `Function0`–`Function3` as `java.util.function.Function` is: `@Independent(hc=true)` on
the type, `@Modified` on every `invoke` argument. A Kotlin `(T) -> R` is one interface where Java has `Function`,
`Consumer`, `Predicate` and the rest, so it takes the general contract. `Predicate.test` is the one JDK interface
whose argument is `@NotModified`, and a Kotlin predicate has no type of its own to say so. Arities 4–22 are left
uncontracted until a corpus calls one.

`TestKotlinLambdaVsJavaLambda.invokesValue` (`f(b)` against `Consumer.accept(b)`) agrees now and disagrees with the
shadow removed. detekt: no method, parameter, field or immutability verdict moves. The only change in `FPDUMP` is
the independence of the 1,767 lambda types, `@Independent` → `@Independent(hc=true)`, which they inherit from the
new supertype contract as a Java lambda does from `Function`. coil runs prep only. `TestParseAnalyzeWrite`'s shadow
count moves by one: the new file imports nothing from `kotlin.*`, so the shared, stdlib-less factory keeps it.

### 7.39 A corpus pin measured the Gradle cache — coil 283 → 159 with no code change

coil's `inputConfiguration.json` names six jars by absolute path in the shared Gradle cache, and four (okio-jvm,
kotlinx-coroutines-core-jvm, atomicfu-jvm, skiko-awt) had been evicted. Nothing reports an absent class-path jar:
its calls just stop resolving. Unchanged code counted 283, then 208 when an unrelated build re-downloaded okio, then
159 once the rest were re-resolved (the cache paths are content hashes, so they land where the configuration looks).
The give-away was the kind of site that vanished: okio calls, and `let`/`also`/`use` on okio receivers.

`TestOssCorpus.requireCompleteConfig` now treats a missing jar like a missing corpus (a skip, or a failure under
`slowTest`), and both Kotlin corpus tests use it (80cfd34fc). Every coil number before 159 in this document was taken
cache-starved and is **not comparable** with it. detekt's 62 jars were all present, so its history stands. ⚠ It is
opt-in because the audit of all 20 corpus configurations found elasticsearch* (97 of 151 jars missing) and fernflower
(24 of 36), which belong to other lanes and whose counts carry the same exposure.

### 7.41 Every placeholder names its line

About 90 sites across detekt and coil printed `0:0`: `k2-unresolved-access`, `k2-indexed-set-unresolved`,
`k2-delegate-read` and some calls. A placeholder built inside a larger node (a selector, an indexed set, a
synthesised delegate accessor) never passed through `convertExpression`'s range step, so the site dump could not
point at it. They are now built through `placeholder(msg, psi)`; a delegate accessor's placeholder takes the `by`
expression's range. The one exception is `convertUnary`, whose operand PSI can be null. `PlaceholderCensusTest.
everyPlaceholderCarriesAPosition` fails on the delegate read and the indexed set when the change is reverted.
Corpora: 0 sites at `0:0` (from ~90); counts (277 / 159), the detekt site list and every verdict are identical.

### 7.42 Three unresolved-access causes — detekt 277 → 213, coil 159 → 154

A probe on the unresolved-access branch recorded the receiver's CST type, its K2 type, and the symbol K2 resolved.
Three causes covered 64 detekt sites:

- **Captured types** (`callableId` 24, `returnType` 6, `receiverParameter` 4, `psi` 3, …). `call.symbol` on a
  `KaCallableMemberCall<*, *>` has K2 type `CapturedType(*)`, which the mapper sent to `Object`, where nothing
  resolves. Java types such an expression by the wildcard's bound. `mapType` now approximates a captured type to its
  nearest denotable supertype (falling back to an `out` projection's type). `CapturedTypeTest`; a
  declared-`out` fixture was vacuous (K2 approximates it already), so the fixture uses an invariant parameter behind
  `*`, and fails 3/3 when the change is reverted.
- **Kotlin's mapped collection properties** (`keys` 12, `entries` 8 on a Map). kotlinc compiles `map.keys` and
  `map.entries` to `keySet()` and `entrySet()`, which no getter-naming rule finds. `resolveAccessor` tries them
  last. `MappedPropertyTest`, explicit and implicit receiver.
- **Enum `entries`** (8). kotlinc gives every Kotlin enum a static `getEntries(): EnumEntries<E>`. Neither the source
  nor the K2-built library enum had one, and `E.entries` was read on the companion, or on the class name typed
  `Unit`. Both enum models now carry the synthetic getter (not Java enums), and a resolved static property routes to
  its static getter. `EnumEntriesTest`, source, bare-in-companion and stdlib enum; 3/3 fail when reverted.

detekt **277 → 213** (types 134 → 95, members 176 → 124), no new site, **no verdict moved**. coil **159 → 154**.

### 7.43 Arrays are arrays — detekt 213 → 193, coil 154 → 143

A Kotlin array's `get`/`set` was converted as a CALL looked up by name. That produced a placeholder for every array
store and every primitive-array load, and a bogus `String.get(int)` for an `Array<String>` load (found on the
element type). On the JVM `IntArray` is `int[]` and `Array<T>` is `T[]`, and indexing is a load/store instruction.
When K2 resolves `a[i]` to the built-in operator of a Kotlin array class, it is now the element as a
`DependentVariable`, exactly as the Java parser builds `a[i]`. `a[i] = v` and `a[i] += v` fall into the ordinary
assignment path (Java's compound assignment). A user's EXTENSION index operator (detekt's
`operator fun ByteArray.set(c: Char, v: Byte)`) stays a call, to its facade with the receiver first.

`String.get(i)` is `charAt(i)` on the JVM, and resolving it exposed a model quirk. A `java.lang.String` built from K2's
`kotlin.String` (the unit-test fixture) has `get` and no `charAt`; the JDK's has `charAt` only. `resolveCallee` tries
`charAt` first and falls back to `get`.

Tests: `ArrayAccessTest` (k2), with 3 of 4 cases failing when reverted (the fourth, `List.set`, is the control);
`TestPrimitiveMembers` gains `s.get(0)`, `s[0]` and an `IntArray` load/store/`+=`, printing `s.charAt(0)` and
`a[0]=1; a[1]+=2;` against the JDK's String; `TestLoweredShapesVsJava` gains `arrayStore`, `arrayElementModified`
and `arrayRead`, all three agreeing with `Box[]` in Java. detekt **213 → 193**, coil **154 → 143**, no new site on
either, **no verdict moved**.

### 7.44 Four operator shapes, one of them not an operator — detekt 193 → 166, coil 143 → 141

The operator placeholders had four causes, one of which was not an operator at all:

- **For-loop destructuring was not implemented.** `for ((clazz, lines) in cache)` declared a loop variable `_` and
  never the entries, so `lines > allowedLines` compared an unresolved name (and 10 `k2-unresolved-ref` sites were the
  entries themselves). The loop variable is now `$dstr`, and the body opens by declaring the entries from it, through
  the same `destructure` a lambda parameter uses (`getKey()`/`getValue()` for a `Map.Entry`, `componentN()`
  otherwise). `convertBlock`/`statementsToBlock` take a prologue that shifts the body's indices.
- **`a.size` on an array** is Java's `a.length`: an `ArrayLength` node.
- **`==` between booleans** (`it.isPublic == publicModifier`, `a == b == c`) took the numeric path or an `equals`
  call, and a `boolean` has neither. It is the primitive `==`, built as the Java parser builds it.
- **`a..<b`** is `IntRange(a, b - 1)` for Int/Long, beside `a..b` → `IntRange(a, b)`.

`OperatorLoweringTest` (k2) fails with 5 placeholders when reverted; `TestDestructuring` (run-kotlin, JDK types)
gains a `Map.Entry` loop and a data-class loop, and fails when reverted. detekt **193 → 166**, coil **143 → 141**,
no new site. **One verdict moved, 667 → 666**: `UtilityClassConstructor` `@Immutable(hc=true)` → `@FinalFields`.
FPDUMP shows why: its `secondaryConstructors.any { it.isPublic == publicModifier && … }` was a placeholder. Read now,
it passes `it` to unannotated library members (the `psiUtil.isPublic` extension, `valueParameters`), which marks
`it` modified, and with it the field `klass` the constructors come from. A reveal. The same code in Java, against
the same unannotated library, gets the same verdict.

### 7.45 Blocks as values: a getter's whole body, `return if`, a lambda's `if` — detekt 166 → 154, coil 141 → 139

`k2-block-not-a-single-expression` had two causes:

- **A computed property's block-bodied getter**, `val x: T get() { … }`, lost its entire body. `bodyExpression`
  returns the block for `get() { … }` too, and `buildComputedGetter` took it as one expression, so each such getter
  was a single placeholder (coil's `Uri.pathSegments`, `filePath`, `BitmapImage.size`; detekt's
  `leftMostElementOfLeftSubtree`). The written-accessor path had the guard. Both accessor paths now convert through
  `convertAccessorBody`, the same routine as a function body, instead of a statement-by-statement loop that skipped
  the block-level lowerings: `val l = left ?: return this` in a getter was the other placeholder. Reading those
  bodies exposes two coil sites inside `BitmapImage.getSize()`.
- **An `if` with a multi-statement branch used as a value** was lowered for `val x = if …` and `fun f() = if …`
  only. `return if …` (and `return try …`) in a function or accessor now becomes an `if` whose branches return
  their tails; inside a lambda a bare `return` is non-local and is left alone. The same applies to a lambda's result
  (`joinToString { if (…) { …; a } else b }`).

`BlockGetterTest` and `ValueIfTest` (k2) fail when reverted. detekt **166 → 154**, coil **141 → 139**, no new site
on detekt, **no verdict moved**. Left of the kind: a field initializer holding a local function (1), and
multi-statement `when` branches.

### 7.46 A destructured value is evaluated once — detekt 154 → 140, coil 139 → 136

- ⛔ **A correctness defect, not only a hole.** `val (l, r) = <value>` converted the value once and let every entry
  read that same node: the CST held one node under two parents (#32 forbids it), and printed `val (l, r) = when {…}`
  with the whole `when` twice. It said the value was evaluated twice. kotlinc evaluates it once, into a temporary.
  A value that is not a stable reference is now bound to `$destructuredN` first; a stable one (a name, `this`, a
  dotted chain) is re-read per entry, a fresh node each time, as the elvis lowering already did.
- `val (a, b) = f() ?: return 0` had no control-flow elvis lowering: the components were placeholders. It is
  lowered like `val x = f() ?: return 0`, the entries reading the guarded temporary.
- `IntArray(n)`, `ByteArray(n)`, `LongArray(n)`, `BooleanArray(n)` and `arrayOfNulls<T>(n)` are `new T[n]`, built
  as the Java parser builds it. With an init lambda (`IntArray(n) { … }`, 3 on detekt) kotlinc inlines a filling
  loop no Java expression spells; that keeps a placeholder, now named `k2-array-constructor-with-init`.

`DestructuringValueTest` (k2) fails 2/2 when reverted. detekt **154 → 140**, coil **139 → 136**, **no verdict
moved**. New on detekt: the 3 renamed init-lambda sites, and one reveal in `MissingUseCall`: there
`if (A) {…} else if (B) {…} else { null } ?: return false` binds the elvis to the inner `if`, inside the outer else
branch, so the `return` is in a value branch. It was hidden behind the component placeholders before.

### 7.47 `suspend`, step one: the JVM signature — detekt 140 → 117

§4's "`suspend` does not exist" turned out to be two models of one function. A **class-file** type (the Java side
loads the stdlib from bytecode on a corpus) has kotlinc's shape, `Object yield(Object, Continuation)`. A type
built by **this** front end had the Kotlin one, `R f(A)`. A call resolved against whichever model the callee's type
came from, so detekt's `yield(x)` inside `sequence { }` found no one-parameter `yield` on the class-file
`SequenceScope`: all 23 of its `yield`/`yieldAll` placeholders. The unit fixtures could not show it, because there
`SequenceScope` is built from K2 and both sides agreed on the wrong shape.

The front end now builds kotlinc's shape everywhere:

- a source, forwarder or K2-built library `suspend fun f(a: A): R` is `Object f(A a, Continuation<R> $completion)`
  (`KotlinTypeMapper.continuationParameter`); its `f$default` keeps the continuation before the masks, as kotlinc's
  does. `@JvmOverloads` overloads are not generated for a suspend function (rare; they would need the continuation
  threaded through).
- every call to a suspend function passes the caller's continuation last: a suspend lambda's `$completion` in scope,
  else the enclosing function's `$completion` parameter (a non-suspend lambda inlined into it, `forEach { g() }`,
  reads that one too); `null` where neither exists.
- the body is converted against the KOTLIN return type, so `suspend fun f() = g()` returning `Unit` stays a
  statement.

`SuspendSignatureTest` (k2): the signature, a call passing `$completion`, the `$default` shape, and a library
suspend member resolving. detekt **140 → 117**, no new site, **no verdict moved**. coil is unchanged at 136: its
site list is identical by kind and position, and only the members' signatures moved (they carry the
continuation now).

**Step two, done:** a suspend FUNCTION TYPE mapped to `kotlin.coroutines.SuspendFunction1`, a K2-only class with
no JVM existence. It is now `Function{N+1}<[receiver,] P…, Continuation<R>, Object>`, as in bytecode. A suspend
lambda's `invoke` takes a trailing `$completion` and returns `Object`, so a suspend call inside `sequence { }` or
`launch { }` passes the lambda's own continuation, and invoking a suspend function value (`f(b)`) passes the
caller's. The lambda body is converted against the Kotlin return type (a `Unit` suspend lambda returns nothing).
`SuspendSignatureTest` gains both; they fail on step one's code. `TestKotlinLambdaVsJavaLambda` gains three rows,
a suspend function modifying its argument, a suspend caller of it, and a reader, against Java written in kotlinc's
shape (an explicit `Continuation` parameter): all agree. This is a parity guard, not a detector of the fix, since
the Box modification was visible before too. Corpora: identical sites and verdicts to step one, and coil's prep
still isolates nothing.

What `suspend` still lacks: no state machine is modelled, and none is needed, because the modification analysis
reads the body as straight-line code, which is what the source says. `@JvmOverloads` on a suspend function
generates no overloads.

### 7.48 Values named through a type — detekt 117 → 95, coil 136 → 119

**The baseline moved under this work, and not because of it.** The merge at e78616190 brought engine commits
(type independence walks interfaces; `Iterable` is `@ImmutableContainer(hc = true)` in the JDK hints). On that
merge, with no front-end change, detekt's immutable types went **666 → 641**: about twenty holders of a
`Collection`/`List`/`Map` (`ConfigSpec`, `RuleSet`, `Issue`, the `*Spec` interfaces) moved IMMUTABLE_HC →
FINAL_FIELDS. It is a parity question before it is a number, so `TestCollectionHoldersVsJava` pairs the three
shapes with the Java a human writes for them: **Java gets FINAL_FIELDS too**, on all three. The pin is lowered with
that reason; placeholders were unchanged at 117.

A value named through a TYPE, not a variable, was a placeholder in three spellings:

- **a qualifier chain**: `Notification.Level.Warning`, `RulesSpec.RunPolicy.NoRestrictions`, `sv.Note.Level.Info`.
  `staticMemberAccess` accepted a receiver that is ONE name, so a two-type chain was read as a value. The last name
  of a qualified receiver is the type now; a nested object still reads `INSTANCE`, and one the loaded model does not
  list (a library type's) is asked of K2 (`classAsValue` on the selector).
- **an import**: `IGNORE_CASE`, `NONE`, `Show`. A bare name K2 resolves to an enum entry is that enum's static
  field (`enumEntryValue`), library or source.
- **a companion's `@JvmField`/`const`**: `JvmTarget.DEFAULT`, `LanguageVersion.LATEST_STABLE`,
  `LanguageVersionSettingsImpl.DEFAULT`. K2 resolves the receiver to the COMPANION, but kotlinc puts the field on the
  OUTER class, and the companion has neither a field nor a getter for it. A probe in the detekt run showed the
  class-file companion empty. So a companion receiver looks on the outer class first. The K2-built library model
  had the Kotlin view instead: the value was an instance field of the companion. It now builds the JVM view, a
  static field of the outer class, and an `object`'s `const`/`@JvmField` becomes a static field of the object
  (§7.47's lesson again: two models of one library type, and the fixture agreed with the wrong one).

And `String.format(…)`: an `@InlineOnly` extension on `String.Companion`, with no method in any class file. kotlinc
inlines it to `java.lang.String.format(…)`, and so does the front end now, choosing the overload by the arguments
(the detekt site passes a `Locale`). A spread `*args` is left alone.

`StaticValueTest` (k2, six cases; five fail on the previous code, and the sixth, a qualified library enum constant,
is a guard that already passed). `TestLibraryCompanions` gains four rows in the class-file world:
`LanguageVersion.LATEST_STABLE`, `JvmTarget.DEFAULT`, and `String.format` with and without a `Locale`.
detekt **117 → 95** in 56 types / 71 members, **no verdict moved** (the type-verdict dump is identical to the
merge's). coil **136 → 119** in 43 types / 82 members.

### 7.49 A vararg callee, bound as kotlinc binds it — detekt 95 → 85

The instrument first: a probe at the unresolved-call placeholder, run once on detekt, printing what K2 resolved each
callee to, the facade and its methods of that name, and the argument types. 222 lines fired, most from conversions
retried and discarded, so it was joined to the surviving placeholders by (callee, member): 29 of 31 matched. They fell into
five causes, and the largest (≈11) was this one: the rebuild of a call that names or omits arguments
(`callArguments`) excluded EVERY vararg callee. `path.writeText(s)` omits a charset before the vararg options;
`splitToSequence(".")` has two defaulted parameters after its vararg; `getParentOfTypesAndPredicate(strict, A::class.java,
B::class.java) { … }` passes one after it. The facade had each method, and no call of the written arity matched it.

The vararg is now bound as in bytecode. Its items are every positional argument from its index on, or one named
argument, which is the array itself when it is spread or of the array type. They stay loose, Java-style, where the
vararg is the JVM signature's last parameter, as every vararg call is written elsewhere. They are packed into `new T[]{…}`
where the JVM parameter is a plain array: a parameter follows it, or the call binds `$default`, whose masks follow
it. `$default` is generated for a vararg function or constructor now, with the vararg typed as its array. K2's
`returnType` of a vararg parameter is the ELEMENT type.

`VarargCallTest` (k2, six shapes: a parameter after the vararg, `$default` with and without items, a spread, a named
array, a plain call). `TestLibraryVarargCalls` (class-file world): `writeText`, `splitToSequence` and
`getParentOfTypesAndPredicate` bind `PathsKt__PathReadWriteKt.writeText(p,"x",null)`,
`splitToSequence(s,new String[]{"."},false,0)` and `getParentOfTypesAndPredicate(e,true,new Class[]{…},it->true)`.
detekt **95 → 85** (10 sites gone, none new) in 50 types / 64 members; members 7,756 → 7,759 are the new
`$default`s. **No verdict moved.** coil unchanged at 119.

The other causes, for what follows: intrinsics written as calls (`arr.get(i)`, `bytes.set(i, v)`, `s.plus(x)`, a boxed
`?.not()`/`?.plus(1)`); invoking a function-typed property or receiver-typed parameter (`d.ruleProvider(config)`,
`init()`); `AutoCloseable.use`, whose facade lookup returns nothing; and receivers typed `Object` where a smart
cast should have narrowed them.

### 7.50 Intrinsics spelled as calls — detekt 85 → 79

The operator spellings converted, and the call spellings did not: `a.get(i)` / `a.set(i, v)` on a JVM array
(`Array`, `IntArray`, … have no such methods in a class file), `s.plus(x)` on a String, and a primitive member on a
BOXED receiver, `oldValue?.plus(1)` or `x?.contains("*")?.not()`, where the safe call types the receiver `Integer` or
`Boolean`. They are now the array load or store (`a[i]`, `a[i] = v`, the receiver implicit inside an extension on
`ByteArray`), the concatenation `s + x`, and the primitive operation.

⛔ The boxed case unboxes only when the callee is the primitive class's own MEMBER (`kotlin/Int.plus`,
`kotlin/Boolean.not`), which Kotlin calls only on a non-null value. The first cut unboxed every boxed receiver, and
`TestPrimitiveMembers`' row `i.toString()` on an `Int?` caught it: that is the `Any?.toString()` extension,
`String.valueOf(Object)`, which prints "null". Unboxed, it became `String.valueOf(int)`, which throws.

`IntrinsicCallTest` (k2, six shapes). detekt **85 → 79** (6 sites gone, none new) in 47 types / 59 members; **no
verdict moved**. coil unchanged at 119.

### 7.51 Function values invoked, and a facade in another JVM package — detekt 79 → 74, coil 119 → 113

Three shapes from the §7.49 probe:

- `AutoCloseable.use { }` (3 on detekt). The stdlib declares it as `kotlin.use` but compiles it into
  `kotlin.jdk7.AutoCloseableKt` (`@file:JvmPackageName`). The library facade was built from the callables of its JVM
  package, and `kotlin.jdk7` has none. It is built from the callable's own Kotlin package now, filtered by facade
  class id.
- a PROPERTY of function type called like a method: `d.ruleProvider(config)` is `d.getRuleProvider().invoke(config)`.
  K2 names `invoke` as the callee, and the written name is the property's.
- a parameter of a function type WITH a receiver, invoked with that receiver implicit: `init()` for
  `init: XMLStreamWriter.() -> Unit` is `init.invoke($receiver)`.

`FunctionValueCallTest` (k2). detekt **79 → 74** (6 gone, 1 new) in 44 types / 54 members; **no verdict moved**.
The new site is a reveal: `visitFile(…)` inside the `KotlinAnalysisApiEngine().use { }` whose placeholder used to
swallow the whole lambda. It is an extension on `this` typed by a two-bound type parameter (`T : Rule,
T : RequiresAnalysisApi`). coil **119 → 113** in 41 / 76.

### 7.52 A member on a narrowed receiver — detekt 74 → 67

A receiver whose K2 type is not a class had no TypeInfo, and its member was looked up on Object:

- a SMART CAST, which K2 types as the intersection of the declared and the tested type: `config.validate(…)` in
  `is ValidatableConfiguration ->`, `it.textContains('\n')` after `it is PsiWhiteSpace &&`, `kaCall.compoundOperation`,
  `(a ?: b ?: c).parent`;
- a TYPE PARAMETER, whose members are its bounds': `it.id`, `it.priority`, `it.init(settings)` on a reified
  `T : Extension`. That one arrives through a Java signature (`ServiceLoader<T>`) as the flexible `T!`, so flexible and
  definitely-not-null wrappers are unwrapped first.

`narrowedReceiverType` takes the class types the receiver stands for (conjuncts and bounds, recursively). It picks
the one that declares, or inherits, the class K2 resolved the member to, else the first. The CST writes no cast,
as §7.28's smart-cast implicit receivers do not. `NarrowedReceiverTest` (k2, eight shapes: smart-cast call, access and
inherited member, one and two bounds, `T!`, a smart cast in a lambda). ⚠ Its fixture showed `s.length` on a plain
`String` failing in the k2 unit world, where `String` is built from `kotlin.String`: an artifact of that world, since
neither corpus has such a site.

detekt **74 → 67** (7 gone, none new) in 40 types / 50 members; **no verdict moved**. coil unchanged at 113.
Still open from this family: `visitFile` on an IMPLICIT `this` typed by a two-bound type parameter (the implicit
receiver takes another route), and Gradle's Kotlin DSL (`withPathSensitivity`, `extendsFrom`).

### 7.53 `by lazy`, read against the class-file `Lazy` — detekt 67 → 57, coil 113 → 106

The third time today that one library type has two models (§7.47, §7.48). A delegated property's getter reads its
delegate. A hand-written delegate declares the `getValue(thisRef, property)` operator, and `kotlin.Lazy` declares
`val value`. The K2-built `Lazy` carries that as a field, and the read was `this.x$delegate.value`. The CLASS-FILE
`Lazy` is an interface whose `val value` is the abstract getter `getValue()`, which is what kotlinc calls. It has
neither the operator nor the field, so every read against it fell through to `k2-delegate-read`. Which model a
run holds depends on which side loaded `Lazy` first: on detekt it was the class file for ten properties, `by lazy`
and `by lazy(NONE)` alike. The read now calls `getValue()` when that is what the type has.

All ten detekt sites gone, none new, **no verdict moved**. detekt **67 → 57** in 35 types / 40 members, coil
**113 → 106** in 40 / 69. `TestKotlinLazyVsJavaLazy` still agrees.

### 7.54 A member extension index operator — detekt 57 → 49

`escapeLevels[c] = 4` in detekt's `Xml10EscapeSymbolsInitializer` (eight sites) goes through
`private operator fun ByteArray.set(c: Char, value: Byte)`, declared inside the `object` itself. The index route
knew a top-level extension operator (a facade static, §7.43) and not this one. On the JVM it is an instance method
of the declaring type with the array first, called on the implicit dispatch receiver, and that is what K2's
`dispatchReceiver` now builds, for `get` and `set` alike. `MemberIndexOperatorTest` (k2). All eight gone, none new,
**no verdict moved**. coil unchanged at 106.

### 7.55 A jump as a function's expression body — detekt 49 → 45

§7.37 lowered `return x ?: throw E()` in a block. The EXPRESSION body `fun f(): R = x ?: throw E()` was converted as
one value, and `throw` is none, so it stayed a placeholder. So did `fun f(): Nothing = throw E()`. The first is now
lowered as its block-bodied spelling is (`controlFlowElvisLowering(returnValue = true)`), and the second is a
`throw` statement. `ThrowBodyTest` (k2) shows the two spellings give the same tree, and `= s?.f() ?: return 0` binds
a temporary, so the left side is evaluated once. detekt **49 → 45** (four gone, none new), **no verdict moved**;
coil unchanged at 106.

Still open in this family, each needing an evaluation order the lowering cannot keep without hoisting the
arguments before it: `?: return` as a call ARGUMENT (2), `?: return` inside an inlined lambda (a non-local return,
1), an `if` expression's `?: return false` (1), and `try` as a lambda's result (1).

### 7.56 A delegated extension property — detekt 45 → 39

detekt's `var KtFile.modifiedText: String? by UserDataProperty(Key("modifiedText"))` is an EXTENSION property with a
delegate. kotlinc gives it a static `modifiedText$delegate` on the file facade and accessors
`getModifiedText(KtFile)` / `setModifiedText(KtFile, String)`, which pass the receiver to the delegate as `thisRef`.
The delegate accessors were built as if for a plain property: no receiver parameter, and `null` as `thisRef`. So
`it.modifiedText` found no one-argument getter, `ktFile.modifiedText = null` no setter, and a bare `modifiedText` inside
another extension on `KtFile` neither. The accessors take `$receiver` first now and pass it on.
`DelegatedExtensionPropertyTest` (k2: read, write, a bare read in an extension, and both accessor bodies). All six
detekt sites gone, none new, **no verdict moved**; coil unchanged at 106.

### 7.57 A primitive's infix members — detekt 39 → 37, coil 106 → 103

`a xor b`, `i shl 2`, `(i and 3) or (i ushr 1)`: infix MEMBERS of `kotlin.Boolean`/`Int`/…, which kotlinc compiles to
the JVM operators. They were looked up as methods and found none. They are now the Java operators `^ & | << >> >>>`,
mapped as the Java front end maps them (`…OperatorInt` whatever the operand type, `^` on booleans included), with
Java's precedences. `IntrinsicCallTest` gains the row. **No verdict moved.**

### 7.58 A delegate initializer's scope, and implicit narrowed receivers — detekt 37 → 33

Found by one probe run printing, at each surviving unresolved call and reference, the receivers K2 names (dispatch and
extension, with their kinds and types). Two fixtures written from reading the source had both passed without
reproducing anything.

- A delegate's `by` expression was converted in the delegated property's GETTER, where a primary-constructor
  parameter is not in scope: `private val resolvedNames by lazy(NONE) { imports… }` with `imports` a constructor
  parameter (two sites). It is converted where kotlinc initializes `x$delegate` now, in the same member as every
  other property initializer (`initializerContext`). Members 7,759 → 7,761 are the synthetic instance initializers
  this creates for two types. `DelegatedExtensionPropertyTest` gains the row, which fails on the previous code.
- The IMPLICIT-receiver twin of §7.52. `text` inside `containsNewline()`, after a `when (this)` whose other branches
  return, has a smart-cast implicit receiver, `KtExpression & KtResolvableCall`. `visitFile(…)` has an implicit
  `this` typed `T : Rule, T : RequiresAnalysisApi`. `receiverLookupType` takes the component declaring the member now,
  as the written-receiver path does. And matching an extension function's `$receiver` compares ERASED types: `this`
  is `T`, with no TypeInfo, and the parameter carries T's first bound. `NarrowedReceiverTest` gains five rows.

detekt **37 → 33** (four gone, none new), **no verdict moved**; coil unchanged at 103.

### 7.59 A jump in argument position — detekt 33 → 31

§7.37 excluded `f(a(), x ?: return)`: hoisting the guard would run the check before `a()`. That exclusion is kept,
and narrowed to what it is about. When the call is the whole statement, and the receiver and every argument before
the elvis, in SOURCE order, named or not, are stable references or constants, nothing the source evaluates first can
observe the move. So:

    check(p, x?.y() ?: return)   ->   T $elvis0 = x?.y(); if ($elvis0 == null) return; check(p, $elvis0);

The argument reads the temporary through `hoistedReads`, so the call itself is converted as any other.
`ArgumentJumpTest` (k2): a positional and a named argument, plus the two refusals (an unstable receiver, an unstable
earlier argument), which keep their placeholder. detekt **33 → 31** (both sites, none new), **no verdict moved**;
coil unchanged at 103.

### 7.60 A bound extension reference, as a lambda — detekt 31 → 28, coil 103 → 102

`rules.forEach(::printRule)` inside `fun YamlNode.printRuleSet(…)`, where `printRule` is an extension on `YamlNode`, was
a deliberate refusal. A BOUND extension reference binds the facade method's first argument, and no Java method
reference can spell that. A lambda can: `r -> YamlNodeKt.printRule($receiver, r)`, over an anonymous `FunctionN`,
built as a local function's value is (§7.38). That is what the bound reference is in effect.

⚠ Only where the lambda may read the receiver again at each call, since kotlinc evaluates it once, at the reference:
an explicit stable reference (`n::printRule`), or an implicit receiver. K2 names no receivers for a callable reference
as it does for a call, so the innermost `$receiver` in scope whose type the extension accepts is taken: a receiver
lambda's, then the function's. A member extension, which also needs its dispatch receiver, keeps the placeholder.

`BoundExtensionReferenceTest` (k2: implicit, explicit, and through `with(n) { }`); `CallableReferenceTest`'s pinned
refusal becomes the conversion. All three detekt sites gone, none new, **no verdict moved**. coil **103 → 102**.

### 7.61 An array constructor with an init lambda — detekt 28 → 25

§7.43 left `IntArray(n) { … }` a named placeholder: kotlinc inlines a filling loop that no single Java expression
spells. As a local's initializer, a statement context is available, so the loop is built:

    val dp = IntArray(n) { return@IntArray 1 }   ->   int[] dp = new int[n]; int $i0 = 0;
                                                      while ($i0 < dp.length) { dp[$i0] = 1; $i0++; }

The lambda's parameter (`it`, or its name) reads the counter. Only for a local `val`/`var` initialized by a Kotlin
array class's constructor whose lambda is ONE statement, a value or `return@Label v`. Every other shape keeps the
placeholder. `ArrayInitTest` (k2): the three detekt shapes, `Array(n) { IntArray(m) }` as `new int[n][]`
included, plus a named parameter. All three detekt sites gone, none new, **no verdict moved**. coil unchanged at 102.

### 7.62 The last implicit receivers and Java statics — detekt 25 → 18

The shapes from §7.58's probe that were still open, three causes and seven sites:

- A LOCAL extension function's receiver, read inside a receiver lambda nested in its body: detekt's
  `fun KtValueArgument.isNearestParentForSuspension()` calls `getArgumentExpression()` inside `with(session) { }`, where
  the innermost `$receiver` is the session's. A receiver lambda's `$receiver` was already reachable by key from lambdas
  nested in it; a local function's now is too, and `implicitReceiverValue` looks it up by the function K2 names as its
  owner. The same fix covers `resolveToCall()`, a member extension of the session applied to the local function's
  receiver. `LocalExtensionReceiverTest` (k2) asserts by IDENTITY, since both receivers print as `$receiver`.
- A Java static reached through a NESTED class, `ExtensionContext.Namespace.create(…)`: `staticCall` took a one-name
  receiver only, the limit §7.48 lifted for static fields.
- A Java static imported by name and called unqualified, `getLineAndColumnInPsiFile(…)`: a static call on the
  declaring class, as Java's static import is.

`TestLibraryCompanions` gains `Character.UnicodeBlock.of('a')` and `toHexString(5)`. All seven gone, none new, **no
verdict moved**. coil unchanged at 102.

### 7.63 Inline-only stdlib members, against the class file — detekt 18 → 15

Two models of one library type again (§7.47, §7.48, §7.53). A mixed-pipeline probe converted `runCatching { }.isSuccess`,
`.getOrNull()` and `val (x, y) = m.destructured` without a placeholder: there the stdlib types were K2-built,
with every member the Kotlin view declares. On detekt they are CLASS-FILE types, and the class file has what kotlinc
emits:

- `kotlin.Result` is a VALUE class. Its members are statics taking the unboxed value, `isSuccess-impl(Object)`, and
  there is no property or getter. A member of a value class, called or read, is now that static when the class file
  has it (`valueClassMember`).
- `MatchResult.Destructured.componentN()` is `@InlineOnly`, absent from the class file, which has `getMatch()` and
  `toList()`. kotlinc inlines its body, `match.groupValues[N]`, and so does this:
  `d.getMatch().getGroupValues().get(N)`, as `Map.Entry`'s inline components already were.

`Result.getOrNull()` stays a placeholder: its inline body reads the receiver twice, and the one site's receiver is a
call. No unit fixture: the k2 and mixed worlds build these types from K2, so only the corpus can show this. detekt
**18 → 15** (three gone, none new), **no verdict moved**; coil unchanged at 102.

### 7.64 The innermost receiver, the safe chain, operator calls — detekt 15 → 10

Five sites, five fixes, each reproduced in a unit fixture first and measured on the corpus once, together:

- **The innermost implicit receiver wins.** A bare name inside a receiver lambda was looked up on the enclosing
  class first. detekt's `EnvironmentFacade` builder lambda assigns a property its receiver shares a name with, and
  the conversion READ the class's own property: a silent wrong read, not a placeholder, except where the class's
  property had no setter (`k2-assign-target`). A name that K2 resolves against an implicit receiver PARAMETER now goes
  to that receiver first (`readsAReceiverMember`). `ReceiverShadowingTest`.
- **`a?.m[k]` is guarded whole.** The PSI is `(a?.m)[k]`, but the index belongs to the safe-call chain: K2 dispatches
  `get` on the non-null `m`, and types `a?.m` as the chain's result (on detekt, the element). It converted to
  `(a==null?null:a.m).get(k)`, which calls `get` on null. Now `a==null?null:a.m.get(k)`, as kotlinc compiles it.
  `MemberIndexOperatorTest`.
- **Unary operators that are calls.** `+"text"` in a kotlinx.html builder is `Tag.unaryPlus(String)`, a member
  extension called on the implicit tag. `-v` on a user type is its `unaryMinus()`, member or top-level extension;
  that one used to become Java's `-` silently. `+i` on a primitive is `i`. `UnaryOperatorCallTest`.
- **A `try` as a lambda's value** (`runCatching { try { … } catch … }`) returns from each arm, as the method-body
  form already did.
- **An annotated jump elvis** (`val (x, y) = @Suppress(…) if (b) { … } else { null } ?: return 0`) is lowered like
  the unannotated one: the annotation is looked through. `ThrowBodyTest`.
- **An annotation class gets no `$default` constructor.** kotlinc emits none, and the one built here carried the
  element default `[]` as a collection literal. ⚠ The annotation's primary constructor is still modelled. The JVM
  has none; its elements are abstract methods. That divergence is open.

The receiver fix is the one that could move verdicts, since it corrects reads rather than filling placeholders, and
none moved. detekt **15 → 10** (five gone, none new), members 7,761 → 7,759 (the two annotation constructors);
coil unchanged at 102.

Refused, and staying counted: UseDataClass's `map { … ?: return }` (a non-local return out of an inlined lambda,
which no Java lambda can express), `Result.getOrNull()` (§7.63), reified `T::class` ×3. The two open shapes are §7.65;
the gradle-DSL calls, §7.66.

### 7.65 Statements where Kotlin writes a value — detekt 10 → 8

- **A jump elvis as a branch of a value `if`.** Kotlin binds `if (a) x else if (b) y else { null } ?: return false`
  as `else (if (b) y else { null }) ?: return false`: the elvis is the outer ELSE branch, not the whole initializer
  (detekt MissingUseCall, in a destructuring under `@Suppress`). Such an `if` now takes the statement form: a
  temporary assigned in each branch, and the elvis branch `$elvis = inner; if ($elvis == null) return false;
  target = $elvis`. A nested value `if` in branch position is assigned in each of ITS branches. `ThrowBodyTest` g, h.
- **A property initializer only a statement can hold** (`val m = if (regex) { val r = …; fun f(…) = …; ::f } else
  { … }`, detekt AbsentOrWrongFileLicense; also a `try`). The field keeps no initializer. An instance property is
  assigned in the constructor that runs the init blocks, in source order with them, reading the constructor's
  parameters: that is where kotlinc puts it. A Java instance initializer would have been simpler and wrong, since it
  runs before the constructor assigns the properties it reads. A top-level or static one goes into the static
  initializer. `PropertyStatementInitializerTest`.

detekt **10 → 8** (two gone, none new), **no verdict moved**; coil unchanged at 102. Each fix was made at unit level
and the two were measured on the corpus together.

### 7.66 The gradle-DSL calls are K2's, not the conversion's

detekt's three remaining build-logic placeholders (`extendsFrom`, `withPropertyName`/`withPathSensitivity`, and
`generatedConfig.get()(fromProject) { … }`) do not resolve in K2 itself: the symbol and the dispatch receiver are
null. The class path is complete (gradle-api, gradle-kotlin-dsl 9.6.1 all present). Gradle compiles `kotlin-dsl`
sources with the **SAM-with-receiver** compiler plugin (`@HasImplicitReceiver`: an `Action<T>` lambda is `T.() -> Unit`)
and the **assignment** plugin (`prop = v` on a `Property<T>`). maddi's standalone K2 session registers no compiler
plugin, so inside `configurations.resolvable(…) { extendsFrom(…) }` there is no receiver. The Analysis API has the hook
(`KotlinCompilerPluginsProvider`); the standalone one is session-wide, so a per-module provider would be needed to
enable it only for source sets built with `kotlin-dsl`. Not done: it is a new dependency and a new capability.

### 7.67 A multiplatform target as a dependsOn chain — coil 102 → 29

Half of coil's placeholders (51 of 102) were K2 resolving nothing: symbol and receiver both null. The JVM slice lists
coil-core's six KMP fragments (`commonMain`, `nonAndroidMain`, `nonJsCommonMain`, `nonAppleMain`, `jvmCommonMain`,
`jvmMain`) as one source set, and KotlinProjectScan made that ONE K2 module. There an `expect` and its `actual` are two
declarations of one name. K2 resolved neither, and everything typed through them failed with them (`request.data`,
`.listener`, … in `RealImageLoader.execute`).

- **The fragments are a dependsOn chain.** A source set whose directories are all `src/<fragment>/kotlin`, with
  `commonMain` (or `commonTest`) first, becomes one K2 module per directory, each depending on the one before, with
  `MultiPlatformProjects` enabled. K2 then matches an `expect` to its `actual` as kotlinc does. A linear chain in the
  listed order is a valid refinement for a single target. Anything else stays one module: splitting an ordinary
  multi-directory set would hide its later directories from the earlier ones (`multiplatformFragments`).
- **A call to an `expect` fun goes to its actual's facade.** In `commonMain`, K2 resolves the call to the `expect`,
  for which kotlinc emits nothing. The top-level `actual` functions are indexed by package, name, receiver and arity
  (`registerActuals`), so `ioCoroutineDispatcher()` is `Coroutines_nonJsCommonKt.ioCoroutineDispatcher()`. An
  `expect class` was already dropped in favour of its `actual` (KotlinScan, `TestExpectActual`).
- **A typealias expansion is resolved in the alias's own module.** `actual typealias Bitmap = org.jetbrains.skia.Bitmap`
  sits in `nonAndroidMain`; analysing it from a `commonMain` use site throws `KaBaseIllegalPsiException`. The
  expansion's `ClassId` is now taken when the aliases are registered and rebuilt in the use-site session.

`MultiplatformFragmentsTest`. coil **102 → 29** (18 of 186 types, 26 of 1,458 members); coil runs prep only, so
there is no verdict to compare. detekt, which has no fragments, is unchanged at 8 with **no verdict moved**. The
stdlib parse (`commonMain` + `jvmMain` with `generated`/`jdkN` roots) is not a fragment set and is unaffected.

### 7.68 Called by the JVM name — coil 29 → 8

- **`@JvmName`.** A callee renamed for the JVM is called by that name: okio's `operator fun Path.div(child: String)` is
  `resolve` in the class file, `fun String.toPath()` is `get`, and its inline `FileSystem.read`/`write` are renamed too.
  A source function already carried its `@JvmName` as its CST name, but its callers looked it up by the Kotlin name.
  The call's conversion names the JVM name (from the declaration's PSI, else the symbol's annotations) for
  `resolveCallee` to try FIRST, then the written name. Both are needed: the unit world's stdlib is built from K2
  and keeps Kotlin names (`sum`), while the class file has `sumOfInt`. The rename is scoped to one call's resolution,
  so an argument's call never inherits an enclosing call's rename. `JvmNameCallTest`.
- **`x in 0.0..1.0`** is `0.0 <= x && x <= 1.0`, as kotlinc compiles it: a floating-point range has no class to
  construct. Only for a stable `x`, which is then read twice at no cost.
- **Arithmetic on a boxed primitive** (`pair.second + 1` with `Pair<*, Int>`): K2 resolved `Int.plus`, so it is Java's
  `+`, which unboxes.
- **A `try` alone in a value-`if` branch** takes the statement form (§7.65), each arm assigning.
- **`MutableList.removeAt(i)`** is `java.util.List.remove(int)`; the argument's type picks that overload.
  `CoilTailShapesTest`.

coil **29 → 8** (none new); detekt unchanged at 8, **no verdict moved**. coil's 8: reified `T::class` ×4 (refused, as
on detekt), `encodeUtf8` (a `@JvmStatic` member extension of `ByteString.Companion`), the `component1`/`component2` of a
value class in a destructuring, and `Canvas(bitmap).apply(::draw)`.

### 7.69 coil's last three — coil 8 → 4

- **An extension `componentN`.** coil's value class `IntPair` declares no components; `inline operator fun
  IntPair.component1() = first` is top-level. Destructuring looked for members only; an extension `componentN` is
  now called on its facade with the value as argument 0.
- **A member extension of an `object` or companion, called through its import** (okio's
  `import okio.ByteString.Companion.encodeUtf8` then `encodeUtf8()`). The dispatch receiver is the singleton. It was
  converted as `this.enc(s)`, with the companion's method and the CALLING class's `this`: well formed and wrong,
  invisible to the census, and the reason the class-file case (`encodeUtf8`) failed outright. Now
  `Bs.Companion.enc(s)`; written inside the object, its own `this` is still the receiver.
- **A reference to a member of the enclosing extension's receiver.** `fun Image.toBitmap(…) = Canvas(b).apply(::draw)`
  binds `draw` to the extension's `Image`, not to a `this` the facade does not have: `$receiver::draw`.

`CoilLastShapesTest`. coil **8 → 4**, and the 4 are reified `T::class` (refused, as on detekt). detekt unchanged at 8,
**no verdict moved**, although the companion fix corrects a receiver.

### 7.70 javalin: why fieldCouldBeFinal missed 8 vals — javalin 119 → 69

The ws/object thread's fieldCouldBeFinal port holds back a `var` whose name appears in unconverted code (verdict
`UNREAD_CODE`). On javalin that cost 8 vars that compile as `val`. Its copy's census: 119 placeholders in 40 of 542
types. ws/object's `f6e78a39c` and `b56721512` are cherry-picked here, since without the first the javalin parse
fails on jetty's `Request.Content`.

- **A test source set is its main set's friend.** kotlinc compiles tests as a friend of main, which sees `internal`.
  KotlinProjectScan gave a source-set dependency only as a regular one, so `internal object CorsUtils`,
  `internal data class OriginParts`, `LoomUtil` and `ReentrantLazy` resolved nowhere from the tests (~45 sites). Every
  upstream set is now a friend too: the code compiled, so any `internal` it names was visible to it.
  `FriendSourceSetTest`, with a negative control.
- **A getter or field typed by a type parameter carries its use-site type.** `cfg.servlet.value` on a
  `Lazy<ServletEntry>` was the erased `T`, so the destructuring `val (initializer, servlet) = …` found no `componentN`
  (JettyServer).
- **`Byte.toString()`** is `String.valueOf((int) b)`: Java has no `valueOf(byte)`. The primitive-member helper matched
  overloads against the receiver's type for every argument; it now matches each argument by its own type, except the
  receiver itself, which stays the primitive it is taken for (an `Int?` smart-cast is a boxed Integer to the CST).
- From the ws/object SARIF thread: an **assignment has its own source position** (it had `noSource()`, and a write
  sorted to line 0), and **`s += p` on a collection is the operator call** kotlinc compiles:
  `CollectionsKt.plusAssign(s, p)`, or `s = CollectionsKt.plus(s, p)` on a `var` of a read-only type. A primitive's and
  `String`'s `+=` stay Java's compound assignment. `AugmentedAssignmentTest`.

javalin **119 → 69** (none new); detekt unchanged at 8, **no verdict moved**; coil unchanged at 4.

Of the 8 misses: `Cookie.name` (TestMultipartForms) no longer has a placeholder, and `HttpClient.origin`
(TestCorsUtils) is freed by the friend fix. The others are still named in unconverted code: `Cookie.value`
(JettyServer: `this::addConnector`, `(x as? Handler.Wrapper)?.handler = …`), `Cookie.path` / `TestPlugins.context` /
`HelloWorldPlugin.context` (TestPlugins' `pluginConfig`, which a K2-built unit fixture converts, so a class-file
difference), and `KotlinApp.app` (TestSse's `runConcurrently` and `SerializableObject`). ⚠ 25 of the 69 are the COPY's,
not the front end's: every Java type in `javalin/test-classes` and `javalin-testtools` has a class file older than its
source, so the Java front end drops those units, and the Kotlin tests' `SerializableObject`, `TypedException`, … have
no type. A rebuilt copy (or one with preserved mtimes) removes them.

### 7.71 javalin's witness files cleared — javalin 119 → 63

Continuing §7.70 on the files whose unconverted code held back the 8 vars:

- **A written `this` is the one K2 names.** Inside `Server().apply { … }` it is the lambda's receiver; it had been
  the enclosing extension's receiver or the class's own `this`, whatever lambdas surrounded it. That was a read of
  the wrong object, and `this::addConnector` failed outright (JettyServer).
- **A local `vararg` function**'s value takes the array, and its call packs the arguments (TestSse's
  `runConcurrently({ … }, { … })`).
- **An assignment through a safe call** is `if (r != null) r.p = v`, and for `(x as? W)?.p = v` it is
  `if (x instanceof W) ((W) x).p = v`. A receiver not free to re-read is bound to a temporary first (JettyServer's
  `(this.unwrap() as? Handler.Wrapper)?.handler = …`). ⚠ The first cut built the guard without statement indices, and
  prep isolated the method with an NPE: the unit suite cannot see that, the corpus's prep did.
- **An inherited `@JvmField`** read through an implicit receiver: `@JvmField protected var pluginConfig` is declared on
  javalin's `Plugin`, and an inner class of a subclass reads it. implicitMemberAccess looked at the lookup type's own
  fields only. The unit fixture does not discriminate (it resolves by another route, with and without the fix);
  javalin does.

`JavalinWitnessShapesTest`, `FriendSourceSetTest`. javalin **119 → 63**; detekt unchanged at 8, **no verdict moved**;
coil unchanged at 4.

**The 8 misses, now.** Six of the eight vars are no longer named in unconverted code. The last two, `onPing`
(TestWebSocket) and `KotlinApp.app` (TestSse), are held back only by `SerializableObject`, a Java test class the Java
front end dropped because the copy's class files are older than its sources (§7.70). A copy with fresh class files
would release them. None of the 8 is still blocked by a front-end gap.

### 7.72 javalin's tail — javalin 63 → 14, everything left is refused

`52b0efbd0` (63 → 50):
- **A Java varargs constructor** binds as a Java one does (jetty's `ServerConnector(server, f1, f2)`), through K2's own
  parameter list.
- **`::p.isInitialized`** on a `lateinit` property is `this.p != null`.
- **An imported `@JvmField` of a library object** (`import kotlin.text.Charsets.UTF_8`) is a static field read.

Then 50 → 24 without a line of front-end code: ws/object rebuilt the copy's test classes, so the 24 sites that the
stale class files cost (§7.70) are gone. Then 24 → 14:
- **javac's default constructor.** A Java class that declares none (`WsConfig`) has only the SYNTHETIC_CONSTRUCTOR the
  Java front end builds for it, and every constructor lookup filtered synthetics out.
- **An overloaded Java setter.** `keyStorePath = …` on jetty's `SslContextFactory` has `setKeyStorePath(String)` and
  `setKeyStorePath(Path)`: the one taking the getter's type is Kotlin's. The same fixture found a **silent wrong read**:
  a Java class with a private field of the property's name had `f.path = v` written to the FIELD, where Kotlin calls
  `setPath` (and reads `getPath()`). Now a `KaSyntheticJavaPropertySymbol` never resolves to a field.
- **A Java static imported under an alias** (`import java.lang.Enum.valueOf as enumValueOf`) is looked up by its
  declared name.
- **A receiver-typed function value called on a written receiver**, `url?.openConnection()?.getter()`, is
  `getter.invoke(connection)`.
- **A jump inside a larger expression** (`spineElvisLowering`): `(l.find { … } ?: throw E()) as T`, `a ?: if (c) f
  else null ?: throw E()` (Kotlin binds the inner elvis to the else branch, so the right operand is a value `if`), and
  `v = a ?: try { f() } catch (e: E) { return@l w }`. The value goes to a temporary assigned in each branch, and the
  statement reads it. Only on the statement's spine, the part evaluated first, so nothing moves across an evaluation.
- **A data class's `componentN()` and `copy()`** have kotlinc's bodies, `return this.pN` and `return new C(p1, …)`.
  Converted from the PSI they were generated from, both were empty (ws/object's SARIF thread). The synthesized
  `equals`/`hashCode`/`toString` are still empty. They share `RecordSynthetics` with Java records, so filling them is
  an engine decision, not a Kotlin one.

The Java-interop rows are in `TestJavaSetterOverloads` (run-kotlin, class-file world). In the k2 fixture a JDK type's
property is a field, so it cannot tell the fix from its absence; the alias row has a negative control. The rest are in
`JumpInExpressionTest`, `JavalinTailTest` and `DataClassTest.componentAndCopyBodies`. javalin **24 → 14**, none new;
detekt unchanged at 8, **no verdict moved**; coil unchanged at 4.

**What is left on javalin (14):** reified `T::class` ×9 and mockk's reified `any()`, `Result.getOrNull()` ×3
(`@InlineOnly`, so kotlinc leaves no method to call). These are refused, as on detekt and coil. The one open site, `Array(n) { … }.joinToString()`
(TestResponse), is closed in §7.73.

### 7.73 An init-lambda array in expression position — javalin 14 → 13

§7.61 lowered `IntArray(n) { … }` only as a local's whole initializer. `spineArrayInitLowering` takes it on the
statement's spine (a receiver, a cast, parentheses, a function's expression body). The same filling loop fills a
temporary `$arrayN`, and the statement reads the temporary through `hoistedReads`:
`Array(n) { "0" }.joinToString()` → `String[] $array4 = new String[n]; int $i5 = 0; while (…) { … }` followed by
`joinToString($array4, …)`. The indices the hoisted statements need are stamped explicitly, as for the elvis
temporaries of §7.72, so no rework was needed. ⚠ The first cut took a temporary's number before knowing the call was
an array constructor. Every call on a spine passes through there, so it shifted the `$elvisN` names of unrelated
lowerings (ArgumentJumpTest, JumpInExpressionTest caught it).

`ArrayInitTest` rows e–g; `DestructuringValueTest`'s pinned placeholder (`fun withInit(n) = IntArray(n) { it * 2 }`)
is gone. javalin **14 → 13**, none new, and **every remaining site is a refused one**: reified `T::class` ×9, mockk's
reified `any()`, and `@InlineOnly` `Result.getOrNull()` ×3. detekt unchanged at 8, **no verdict moved**; coil
unchanged at 4.

## 8. The ordered path to the claim

1. ✅ Refuse loudly (§7.1) — converts a silently wrong answer into a stated scope.
2. ✅ Count the holes (§7.2), and close the two silent drops the count could not see (§7.3, §7.4) — "maddi analyzes Kotlin *and tells you what it could not read*" is defensible
   at today's coverage; the unqualified claim is not.
2b. ✅ **Ask the analyzer, not the parser** (§7.16–§7.18). `TestLoweredShapesVsJava` compares thirteen
   lowered Kotlin shapes and a type-level row against the Java a human would write for each, guarded by a
   zero-placeholder assertion so agreement cannot be vacuous. It found a wrong safe-call lowering on its
   first run, answered §7.15's duplication question (2× in the tree, same verdict — not worth refining),
   and then found a **Java-engine defect**: every modification inside a conditional expression was
   discarded. This rung is the instrument the remaining rungs should be measured on.
3. ✅ **One entry point** (§7.13). `bin/maddi-kotlin` takes the Java CLI's own option surface and routes on
   whether the project holds a `.kt` file, so it is a strict superset of `bin/maddi` rather than a second
   tool. Two bundles stay (85 MB vs 11 MB), one command line. What the mixed pipeline cannot honour is
   refused by name, which points at step 6.
4. **Close the model.** The whole statements-in-expression-position family is ✅ done (§7.9–§7.12): the
   arity rule and operator extensions, `bootstrapString`'s statics, extension properties, blocks in
   expression position, `x ?: return`, and `try` as a value. detekt **6,057 → 5,525** sites, types holding
   one **846 → 791**, coil **437 → 379**, prep isolation **0** on both.
   ⭐ **Callable references** are now converted for every shape but the property reference (§7.19), which
   keeps a named placeholder; the corpus delta is owed, the box being full when it landed.
   ✅ **Property references** (§7.23), ✅ **annotations** (§7.24). What remains, in order: ✅ **`suspend`** (§7.47),
   which neither corpus reaches, then the **local delegated property** (§3, 1.3). The largest remaining
   families are now unresolved *calls* and *accesses* rather than unmodelled syntax — a different kind of
   work, and one the site dump can drive. ✅ Class-file shells and extension references (§7.25) and implicit-receiver members (§7.26) and
   member extensions (§7.27) and receiver nesting and smart casts (§7.28) context parameters (§7.29), `super` dispatch (§7.30), primitive members (§7.31), top-level
   properties (§7.32), library companions (§7.33), lambda destructuring (§7.34), companion `invoke` /
   `arrayOf` (§7.35), class literals (§7.36), jumps in expression position (§7.37), local functions (§7.38) and the three
   unresolved-access causes of §7.42, arrays (§7.43) the operator shapes of §7.44 blocks as values (§7.45) single-evaluation destructuring (§7.46), suspend signatures (§7.47), values named through a type (§7.48) vararg binding (§7.49) intrinsics spelled as calls (§7.50) function values invoked (§7.51) narrowed receivers (§7.52) `by lazy` against the class-file `Lazy` (§7.53) member index operators (§7.54) jumps as expression bodies (§7.55) delegated extension properties (§7.56) infix primitive members (§7.57), delegate initializers and implicit narrowed receivers (§7.58) argument-position jumps (§7.59) bound extension references (§7.60) array constructors with an init lambda (§7.61) the last implicit-receiver and static-call shapes (§7.62) inline-only stdlib members (§7.63), the innermost receiver, the safe index chain and unary operator calls (§7.64) and statements where Kotlin writes a value (§7.65) have taken detekt 4,701 → 8; a multiplatform target as a dependsOn chain (§7.67) calls by the JVM name (§7.68) and coil's last three (§7.69) took coil 102 → 4 (all reified `T::class`) and coil 367 → 283 on that dump (coil is 102 once its class path is complete, §7.39, §7.42–§7.63; its
   earlier numbers were cache-starved).
   ⭐ Both corpora agree (81% and 74%) with no overlap in what they call, which is as close to a sample as
   two projects get.
5. ✅ **Make the evidence fail** (§7.16, §7.20). The three `assumeTrue` skips now fail under
   `-Dmaddi.corpus.required`, which `slowTest` sets; a two-sided ratchet pins the Kotlin census on both
   corpora; and the Java → Kotlin verdict boundary is a regression owned here rather than downstream.
   ⚠ Remaining: the ~10 Java corpus tests still use a bare `assumeTrue`, so `slowTest` will not yet fail
   for a missing guava/fernflower/elasticsearch checkout. Converting them is one line each, and should be
   done with a check of which corpora each machine has — it changes what a green `slowTest` means.
6. ✅ **Persistence** (§7.21). The round trip is measured — write, fresh re-parse, load, identical verdicts
   — it cost two Java-side codec fixes to get there, and the mixed CLI now honours `--analysis-results-dir`
   where it used to refuse it. ⚠ `--incremental-analysis` stays refused: consuming results to SKIP work
   needs the rewire/fingerprint machinery, which is the next question, not this one. The IDE daemon is
   likewise still Java-only; what changed is that the layer underneath both now exists for Kotlin.

Steps 3–5 buy "maddi is Java+Kotlin, with a published coverage boundary". Step 6 and Tier 3 buy the
unqualified claim.

### 8b. Not on the ladder, but done since — the front end stopped poisoning its hosts

`docs/design/kotlin-classloader-isolation.md` (G46). The K2 front end's 62 MB fat compiler jar carries 8,235
non-Kotlin classes under their original package names and was reaching every consumer's *runtime* classpath
through an `implementation` dependency; downstream it shadowed 174 of ANTLR's classes, 787 of guava's and
115 of JNA's, and killed a conformance oracle. It now loads in a plexus-classworlds realm behind the
five-type contract in `maddi-kotlin-api`. ⭐ The verification that matters for this document: the detekt
placeholder dump through the realm is **identical, site for site**, and prep isolation stays 0 — the
isolation changed the classpath and nothing about what the front end reads. It does not advance the ladder,
but it removes the reason a host would refuse to put maddi's Kotlin support on their classpath at all.
