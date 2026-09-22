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
2. **The link engine has zero Kotlin tests**, and `VirtualFieldComputer.java:110` excludes
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
   = 0.4% on detekt (`docs/kotlin-corpora.md` §5.5). Do not spend Tier-1 effort on it.

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
  M2–M5b done; `docs/kotlin-corpora.md` §2/§5.6 carry 2026-08-03 numbers (and `TestDetektCorpus.java:132`
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
prep** on both corpora — which retires `docs/kotlin-corpora.md` §5.1 (the `variableData` overwrite, 8
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

## 8. The ordered path to the claim

1. ✅ Refuse loudly (§7.1) — converts a silently wrong answer into a stated scope.
2. ✅ Count the holes (§7.2), and close the two silent drops the count could not see (§7.3, §7.4) — "maddi analyzes Kotlin *and tells you what it could not read*" is defensible
   at today's coverage; the unqualified claim is not.
3. ✅ **One entry point** (§7.13). `bin/maddi-kotlin` takes the Java CLI's own option surface and routes on
   whether the project holds a `.kt` file, so it is a strict superset of `bin/maddi` rather than a second
   tool. Two bundles stay (85 MB vs 11 MB), one command line. What the mixed pipeline cannot honour is
   refused by name, which points at step 6.
4. **Close the model.** The whole statements-in-expression-position family is ✅ done (§7.9–§7.12): the
   arity rule and operator extensions, `bootstrapString`'s statics, extension properties, blocks in
   expression position, `x ?: return`, and `try` as a value. detekt **6,057 → 5,525** sites, types holding
   one **846 → 791**, coil **437 → 379**, prep isolation **0** on both.
   What remains, in order: **callable references** (92 on detekt), then **annotations** and **`suspend`**,
   which neither corpus reaches, then the **local delegated property** (§3, 1.3). The largest remaining
   families are now unresolved *calls* and *accesses* rather than unmodelled syntax — a different kind of
   work, and one the site dump can drive.
   ⭐ Both corpora agree (81% and 74%) with no overlap in what they call, which is as close to a sample as
   two projects get.
5. **Make the evidence fail.** Turn the three `assumeTrue` skips into hard failures in CI, commit a Kotlin
   baseline ratchet beside the Java ones, and move one mixed-language regression into this repository.
6. **Persistence**: codec encode plus a real Kotlin round trip — which is what unlocks incremental and the
   IDE daemon.

Steps 3–5 buy "maddi is Java+Kotlin, with a published coverage boundary". Step 6 and Tier 3 buy the
unqualified claim.

### 8b. Not on the ladder, but done since — the front end stopped poisoning its hosts

`docs/kotlin-classloader-isolation.md` (G46). The K2 front end's 62 MB fat compiler jar carries 8,235
non-Kotlin classes under their original package names and was reaching every consumer's *runtime* classpath
through an `implementation` dependency; downstream it shadowed 174 of ANTLR's classes, 787 of guava's and
115 of JNA's, and killed a conformance oracle. It now loads in a plexus-classworlds realm behind the
five-type contract in `maddi-kotlin-api`. ⭐ The verification that matters for this document: the detekt
placeholder dump through the realm is **identical, site for site**, and prep isolation stays 0 — the
isolation changed the classpath and nothing about what the front end reads. It does not advance the ladder,
but it removes the reason a host would refuse to put maddi's Kotlin support on their classpath at all.
