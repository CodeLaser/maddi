# Bug report: `visitCompoundAssignment` uses the **int** `+` operator for a **String** `+=`

**Reported:** 2026-07-16, from the `openjdk` branch of `jfocus-stdbase`, while hardening the standardizer.
**For:** the thread that owns `maddi-java-openjdk`.
**Related:** `compound-assignment-missing-binary-operator.md` (same method, RESOLVED 2026-07-14). This is a
**different** bug in the same `switch`, surfaced *by* that fix: now that `binaryOperator()` is populated, its
value is observably wrong for `String +=`.

## Summary

`ScanCompilationUnit.visitCompoundAssignment` (`:1770`) picks the operator from the **syntactic kind alone**,
without consulting the target's type:

```java
Tree.Kind kind = node.getKind();
MethodInfo operator = switch (kind) {
    case PLUS_ASSIGNMENT -> runtime.assignPlusOperatorInt();   // <-- always the *int* operator
    ...
};
currentExpression = runtime.newAssignmentBuilder()
        .setAssignmentOperator(operator)
        .setBinaryOperator(runtime.assignOperatorToBinary(operator))   // -> plusOperatorInt
        ...
```

So for `String s; s += "x";` the CST carries `assignmentOperator() == int.+=(int)` and
`binaryOperator() == int.+(int,int)` — the numeric operators — on a `String` target.

`assignPlusOperatorString()` / `plusOperatorString()` exist and are public API
(`PredefinedWithoutParameterizedType:47`, `PredefinedImpl:283/550/670`), and `assignOperatorToBinary` **already**
maps `assignPlusOperatorString -> plusOperatorString` (`PredefinedImpl:473`). That mapping is currently
unreachable: nothing ever produces `assignPlusOperatorString`.

Note the **plain** binary path is correct: `String t = s + "y";` yields a `BinaryOperator` with
`operator() == java.lang.String.+(String,String)`. Only the compound form disagrees.

## Why it matters (observed crash)

Any consumer that reconstructs `s += x` as `s = s + x` and then evaluates it routes a String into the numeric
sum. `EvalBinaryOperator.determineValue` dispatches on operator identity:

```java
if (operator == runtime.plusOperatorInt()) return runtime.sum(l, r);      // taken
...
if (operator == runtime.plusOperatorString()) return runtime.newStringConcat(l, r);   // wanted
```

and `SumImpl`'s constructor asserts its operands are numeric:

```
java.lang.AssertionError: Have stringConstant@11-20:11-22, Type String
  at org.e2immu.language.cst.impl.expression.SumImpl.<init>(SumImpl.java:39)
  at org.e2immu.language.cst.impl.expression.eval.EvalSum.eval(EvalSum.java:70)
  at org.e2immu.language.cst.impl.runtime.EvalImpl.sum(EvalImpl.java:100)
  at org.e2immu.language.cst.impl.expression.eval.EvalBinaryOperator.determineValue(EvalBinaryOperator.java:62)
  at org.e2immu.language.cst.api.runtime.Eval.sortAndSimplify(Eval.java:168)
  at io.codelaser.jfocus.standardize.analyzer.parse.ExpressionVisitor.handleBinaryOperator(...)
```

This took down 3 stdbase test classes (`parse.TestLocalVariable.test4/test4b`,
`parse.reassign.TestReassignment.test15`) — all real-world code doing `str += "<"`.

## Repro

```java
String concat(String s) {
    s += "x";      // assignmentOperator() == int.+=(int),  binaryOperator() == int.+(int,int)
    return s;
}
String plain(String s) {
    return s + "y";  // operator() == java.lang.String.+(String,String)   <-- correct
}
```

## Suggested fix

Select the String variant when the target is a `String`; `assignOperatorToBinary` then yields
`plusOperatorString` for free, so only the `switch` needs to change:

```java
case PLUS_ASSIGNMENT -> target.parameterizedType().isJavaLangString()
        ? runtime.assignPlusOperatorString()
        : runtime.assignPlusOperatorInt();
```

`target` is already in scope (`:1775`), and `ParameterizedType.isJavaLangString()` is public API (`:176`). No
other `case` needs changing: `String` supports no other compound operator.

Worth a look while you are there: `char`/`long`/`double` compound assignments also get the `int` operators.
That is likely harmless (the numeric ops are shared/widened), unlike the String case which is a different
operation entirely — but it is the same "operator chosen from syntax, not type" root.

## Note

jfocus-stdbase has a **robustness fallback** (`ExpressionVisitor.expandAdditionAssignment`: if the binary
operator is `plusOperatorInt` but the target is a `String`, substitute `plusOperatorString`), so it is correct
regardless. But the CST should be right at the source — other consumers will hit the same assert. Covered there
by `harden/TestCompoundAssignment.stringCompoundAddIsConcat` + `stringCompoundConverges`.

## RESOLUTION (2026-07-16, kotlin trunk — maddi-java-openjdk owner)

Fixed exactly as suggested. `ScanCompilationUnit.visitCompoundAssignment` (`:1776`) now selects the String
operator for `PLUS_ASSIGNMENT` when `target.parameterizedType().isJavaLangString()`, and the numeric one
otherwise. Confirmed the three claims it rests on: `isJavaLangString()` is public API
(`ParameterizedType:176`), `assignPlusOperatorString()` exists (`PredefinedWithoutParameterizedType:47`), and
`assignOperatorToBinary` already maps it to `plusOperatorString` (`PredefinedImpl:473`) — so
`setBinaryOperator(assignOperatorToBinary(operator))` yields the String binary operator for free, no other line
changed. `target` is non-null (`asAssignmentTarget` casts, never returns null).

Regression tests in `expression/TestCompoundAssignment`: `stringCompoundAddIsConcat` (`s += "x"` →
`assignPlusOperatorString` / `plusOperatorString`) and `stringPlainAddIsConcat` (asserts the already-correct
`s + "y"` form agrees, so the two paths are pinned together). The existing int `test` is untouched.

Left as-is, deliberately: the `char`/`long`/`double` observation. Those are numeric operations that widen, so the
shared int operator is not a *different* operation the way String concatenation is — no crash, no evidence of a
consumer misreading them — and expanding the fix there would change working behaviour on suspicion alone. If a
concrete case surfaces, it is the same one-line pattern per `case`.

NB: validated by compilation only; the test *run* is deferred to a pristine Gradle setting (the shared build host
was busy). The fix and tests compile clean against `:maddi-java-openjdk`.
