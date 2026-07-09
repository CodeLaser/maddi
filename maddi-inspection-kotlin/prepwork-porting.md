# Porting the modification-prepwork tests to Kotlin

The `prepwork` test package under `maddi-inspection-kotlin` is a **cross-language validation tier**: each class
ports a Java prep-analyzer test from `maddi-modification-prepwork` — the *same behaviour* expressed in Kotlin
source — and asserts the *same* `VariableData` result (see [`CommonKotlinPrep`](src/test/kotlin/org/e2immu/language/inspection/kotlin/prepwork/CommonKotlinPrep.kt)).
The Java test's oracle strings (`"D:0, A:[1.0.0, 1=M]"`, `"a.b.X.method(char)"`, …) are the truth: if the Kotlin
front-end (k2) produces a faithful CST — same statement indices, same reads/assignments — the analyzer yields
the identical string. So a passing port proves the k2 CST is structurally faithful for that construct.

## Status

Of the 52 `CommonTest`-based Java prepwork tests, the ones whose constructs translate structurally 1:1 to Kotlin
are ported (**16 classes**). The remainder rely on Java-only constructs whose exact statement/assignment
structure — which the oracle strings encode — cannot be reproduced in Kotlin; those are documented below rather
than force-fitted (which would need *fresh* oracle strings, i.e. new tests, not equivalence checks).

### Ported (identical `VariableData` oracle)

| Kotlin test | Java original | construct |
|---|---|---|
| TestAlwaysEscapes | TestAlwaysEscapes | escape analysis |
| TestAssignmentsConstructor | TestAssignmentsConstructor | constructor field assignment |
| TestAssignmentsDoWhile | TestAssignmentsDoWhile | do-while |
| TestAssignmentsEmpty | TestAssignmentsEmpty | for-each + empty if-block merge¹ |
| TestAssignmentsExpressionForms | TestAssignmentsExpressionForms | expression statements |
| TestAssignmentsFieldAccess | TestAssignmentsFieldAccess | compound field assignment (`this.f += d`)² |
| TestAssignmentsForEachIf | TestAssignmentsForEachIf | for-each + if |
| TestAssignmentsReturn | TestAssignmentsReturn | return / if-else |
| TestAssignmentsTryFinally | TestAssignmentsTryFinally | return-in-try with reassigning finally³ |
| TestAssignmentsWhen | TestAssignmentsSwitchNewStyle | `when` (arrow switch), arm indexing + merge⁴ |
| TestComputePartOfConstruction | TestComputePartOfConstruction | part-of-construction |
| TestFinalFieldBranchAssignment | TestFinalFieldBranchAssignment | effectively-final: when-branch / lambda / anon⁵ |
| TestHasBeenDefinedLocals | TestHasBeenDefinedLocals | if/else, do-while, while, try/catch definite-assignment |
| TestLabeledContinue | TestLabeledContinue | labeled continue |
| TestReassignment | TestReassignment | reassignment |
| TestWhenNoElseMerge | TestSwitchNoDefaultMerge | `when` exhaustiveness (else / sealed `is`-when) |

¹ only deviation: a `Array<String>` param renders as `kotlin.Array` in the method FQN where Java has `String[]`.
² the array (`a[i] += v`) and chain (`a.b.c = …`) sub-tests are N/A (see below).
³ the multi-catch and labeled-break sub-tests are N/A (see below).
⁴ the pattern-switch sub-test is N/A (instanceof-pattern; see below).
⁵ the positive control (effectively-final non-final-*declared* field) is N/A (see below).

### Not portable — Kotlin lacks the construct (documented in each ported file where a sub-test is dropped)

- **instanceof-pattern variables / record deconstruction / pattern flow-scoping** — Kotlin's `is` smart-casts the
  *selector* rather than binding a fresh pattern variable, so the pattern var the oracle tracks does not exist,
  and the selector picks up extra reads. Affects `TestCast`, `TestCastArray`, `TestAssignmentsInstanceOf`,
  `TestAssignmentsInstanceOfFlow`, `TestInstanceOfPatternVariables`, and the pattern arms of the switch tests.
- **assignment as expression / chained assignment** (`a = b = c`, `while ((x = f()) != null)`) — Kotlin
  assignment is not an expression. Affects `TestAssignmentsExpression`, `TestAssignmentsReuse`, `TestReads`,
  and parts of `TestAssignments`, `TestVariableData`, `TestVariableDataArrays`.
- **old-style (fall-through) switch** (`case 2: case 3:`) — no Kotlin equivalent; `when` has no fall-through.
  Affects `TestAssignmentsSwitchOldStyle`, `TestAssignmentsSwitch`, `TestSwitchLabeledBreak`, `TestAssignmentsNoExit`.
- **C-style / infinite `for`** (`for(;;)`, `for(i=0; i<n; i++)`) — becomes `while` / `for (i in …)`, a different
  block structure, so the indices shift. Affects `TestAssignmentsNoExit`, `TestMergeLocalVariables`,
  the labeled-break case of `TestAssignmentsTryFinally`, and others.
- **`synchronized` / `assert` statements** — Kotlin's are stdlib *functions* (`synchronized(l){}`, `assert(c){}`),
  parsed as calls, not statements. Affects `TestAssignmentsSyncAssert`.
- **multi-catch** (`catch (A | B e)`) — no Kotlin union catch. Affects a `TestAssignmentsTryFinally` sub-test.
- **indexed / property-chain assignment** — `a[i] += v` and `a.b.c = …` route through Kotlin's `get`/`set`
  operator convention and property setters, so no `a[i]` dependent variable / no `a`/`b`/`c` field variable is
  produced. Affects `TestAssignmentsFieldAccess` sub-tests.
- **effectively-final non-final-*declared* field** — Kotlin declares mutability explicitly (`var` = not final,
  `val` = trivially final), so there is nothing to *compute*. Affects the `TestFinalFieldBranchAssignment` control.
- **source-position / Java-synthetic-order assertions** — Kotlin source has different offsets, and a Kotlin data
  class generates different synthetic members/order. Affects `TestSwitchExpression` (`compact2()` offsets),
  `TestAssignmentsDependentVariable` (char-range variable names + `.length`), `TestCast`/`TestAnalysisOrder`
  (record `equals`/`hashCode`/`toString` ordering).

### Not yet attempted (lower priority: call-graph / get-set infrastructure, largely language-agnostic)

`TestCallGraph`, `TestCallGraph2`, `TestComputeCallGraph`, `TestAnalysisOrder`, `TestCloneBenchMethodHistogram`,
`TestTrack1`, `TestApplyGetSetTranslation`, `TestGetSet`, `TestGetSet2`, `TestLambda`, `TestRecord`,
`TestLocalType`, `TestDoNotRecurseIntoAnonymous`, `TestAssignmentsInConditions`, `TestAssignmentsLabeledBreak`,
`TestAssignments`, `TestReads`, `TestVariableData`, `TestVariableDataArrays`. These exercise multi-type call-graph
/ analysis-order / get-set machinery that is language-neutral (already covered by the Java suite) and whose
oracle strings are dominated by Java-specific FQNs and synthetic-member ordering.
