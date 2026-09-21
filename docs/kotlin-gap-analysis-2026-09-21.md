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
| **the predefined `String`** | `s.length` | `bootstrapString` (`KotlinTypeMapper.kt:597`) loads named functions only — 83 methods, **0 fields** — so Kotlin's `length` PROPERTY is dropped. `java.util.List` goes through `loadLibraryMembers`, which does convert properties, which is why `x.size` works |
| **extension properties** | `s.lastIndex` | `convertQualified`'s name-reference branch has no facade route at all |

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

## 8. The ordered path to the claim

1. ✅ Refuse loudly (§7.1) — converts a silently wrong answer into a stated scope.
2. ✅ Count the holes (§7.2), and close the two silent drops the count could not see (§7.3, §7.4) — "maddi analyzes Kotlin *and tells you what it could not read*" is defensible
   at today's coverage; the unqualified claim is not.
3. **One entry point.** Either `maddi-run-main` gains `--compile-log` + the mixed inspector, or the Kotlin
   CLI gains the flags it lacks (no `--source`/`--classpath`, no `--analysis-results-dir`, no incremental,
   no hints composer, no `--help`). Until then the claim is about a second tool.
4. **Close the model.** ✅ The arity rule and the operator-extension route are done (§7.9). What remains,
   in order: `bootstrapString`'s missing properties (`s.length`, and it is also why assignability cannot be
   used in overload resolution, §7.8), extension **properties** (no facade route at all), a vararg in a
   middle position with defaults after it (`s.split(",")`), then **statements in expression position**
   — `KtBlockExpression` 287 + `KtReturnExpression` 270 + `throw`/`try`/`continue` are ONE family, the
   `x ?: return` and `val v = if (c) {…} else {…}` idioms, ~575 sites — then callable references (92), then
   annotations and `suspend`, which neither corpus reaches, then the local delegated property (§3, 1.3).
   ⭐ Both corpora agree (81% and 74%) with no overlap in what they call, which is as close to a sample as
   two projects get.
5. **Make the evidence fail.** Turn the three `assumeTrue` skips into hard failures in CI, commit a Kotlin
   baseline ratchet beside the Java ones, and move one mixed-language regression into this repository.
6. **Persistence**: codec encode plus a real Kotlin round trip — which is what unlocks incremental and the
   IDE daemon.

Steps 3–5 buy "maddi is Java+Kotlin, with a published coverage boundary". Step 6 and Tier 3 buy the
unqualified claim.
