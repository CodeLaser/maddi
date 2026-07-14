# Bug report: `visitCompoundAssignment` does not set `Assignment.binaryOperator()`

**Reported:** 2026-07-14, from the `openjdk` branch of `jfocus-stdbase`, while hardening the standardizer.
**For:** the thread that owns `maddi-java-openjdk`. This is a **CST-completeness** bug: a compound assignment
(`i += x`) is built without its `binaryOperator`, so consumers that reconstruct `i = i <op> x` lose the target.

## Summary

`ScanCompilationUnit.visitCompoundAssignment` (≈ line 1726) builds the `Assignment` for `i += x` with only the
**assignment** operator set, not the **binary** operator:

```java
// visitCompoundAssignment(CompoundAssignmentTree node, ...)
MethodInfo operator = switch (kind) {
    case PLUS_ASSIGNMENT -> runtime.assignPlusOperatorInt();
    ...
};
currentExpression = runtime.newAssignmentBuilder()
        .setAssignmentOperator(operator)   // the '+=' operator
        .setValue(value)                   // the RHS, x
        .setTarget(target)                 // i
        .build();                          // <-- binaryOperator() left null
```

But the **unary** path (`i++`, `i--`, ≈ line 2792) correctly sets **both**:

```java
currentExpression = runtime.newAssignmentBuilder().setAssignmentOperator(operator)
        .setAssignmentOperatorIsPlus(isPlus)
        .setBinaryOperator(isPlus ? runtime.plusOperatorInt() : runtime.minusOperatorInt())  // <-- set
        .setTarget(...).setValue(runtime.intOne(...)).build();
```

`Assignment.binaryOperator()`'s javadoc says it is *"the underlying binary operator used to compute the new
value of a compound assignment"* — so it should be populated for every compound assignment, exactly as the
unary path does.

## Impact

A consumer that reconstructs the semantics `i += x  ≡  i = i <op> x` reads `binaryOperator()`; when it is null
it cannot form `i <op> x` and typically falls back to just `x`, i.e. treats `i += x` as `i = x` and **drops the
accumulated value**. Concretely, the jfocus-stdbase standardizer produced `rv = x` for
`{ int i = a; i += 3; return i; }` instead of `a + 3`. (`i++` was unaffected precisely because the unary path
sets `binaryOperator`.)

## Fix

In `visitCompoundAssignment`, also set the binary operator — the runtime already provides the mapping used
elsewhere (`Predefined.assignOperatorToBinary`, `PredefinedImpl` line ≈460):

```java
    .setAssignmentOperator(operator)
    .setBinaryOperator(runtime.assignOperatorToBinary(operator))   // + this
    .setValue(value)
    .setTarget(target)
```

(Optionally also `setAssignmentOperatorIsPlus(kind == PLUS_ASSIGNMENT)` for parity with the unary path.)

## Reproduction

Parse `class X { int m(int a){ int i=a; i+=3; return i; } }` with the openjdk inspector and inspect the
compound-assignment `Assignment`: `assignment.binaryOperator()` is `null` (expected: the `+` operator,
`runtime.plusOperatorInt()`); `assignment.assignmentOperator()` is the `+=` operator.

## Note

jfocus-stdbase now has a **robustness fallback** (derive the binary operator from `assignmentOperator()` via
`runtime.assignOperatorToBinary(...)` when `binaryOperator()` is null), so it is correct regardless. But the CST
should be complete at the source — other consumers will hit the same gap. Low risk: the one-line addition only
populates a field that is currently null.

## RESOLUTION (2026-07-14, kotlin trunk — maddi-java-openjdk owner)

Fixed exactly as suggested. `ScanCompilationUnit.visitCompoundAssignment` (`:1748`) now sets, alongside the
assignment operator:

```java
.setBinaryOperator(runtime.assignOperatorToBinary(operator))
.setAssignmentOperatorIsPlus(kind == Tree.Kind.PLUS_ASSIGNMENT)
```

giving parity with the unary (`i++`/`i--`) path. Verified `runtime.assignOperatorToBinary` covers **every**
operator the switch produces — including the three shift operators, whose `assign…OperatorInt()` accessors return
the same `MethodInfo` fields the mapping compares against (`PredefinedImpl:585-591` ↔ `:475-477`) — so there is no
`NYI`/crash risk for `<<=` / `>>=` / `>>>=`. Regression test `expression/TestCompoundAssignment`: for
`{ int i=a; i+=3; return i; }`, `binaryOperator()` is now `runtime.plusOperatorInt()` (was `null`),
`assignmentOperator()` is `+=`, `assignmentOperatorIsPlus()` is true. Green; java-openjdk suite unaffected.

The stdbase fallback can stay (harmless), or be removed now that the CST is complete at the source.
