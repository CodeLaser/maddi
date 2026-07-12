# Bug report: PrepAnalyzer AssertionError on a qualified `super()` call (get/set analysis runs on a constructor)

**Reported:** 2026-07-12, from the `kotlin` branch while running parse+prep over JDK 26 modules
(milestone "deduce AnalysisHints from JDK sources"). **For:** the prepwork/modification thread
(`sv-integration`). Line numbers below are against `sv-integration`.

## Summary

`PrepAnalyzer` crashes with an **uncaught** `AssertionError` when a method body contains a **qualified
explicit superclass constructor invocation** — `outer.super()` — such as an inner class that extends
another class's inner class. The get/set translation calls `GetSetHelper.doGetSetAnalysis` on the
*constructor* being invoked, and that method asserts it is never given a constructor.

Two defects, really:
1. **Correctness:** `ApplyGetSetTranslation.ensureGetSet` runs get/set analysis on a constructor `MethodInfo`.
2. **Robustness:** `PrepAnalyzer` has no per-type / per-method fault isolation, so a single failing method
   aborts the *entire* run (process exits on the uncaught throwable) instead of recording it and moving on.

## Stack

```
java.lang.AssertionError
    at ....common.getset.GetSetHelper.doGetSetAnalysis(GetSetHelper.java:56)
    at ....common.getset.ApplyGetSetTranslation.ensureGetSet(ApplyGetSetTranslation.java:177)
    at ....common.getset.ApplyGetSetTranslation.translateExpression(ApplyGetSetTranslation.java:65)
    at ....cst.impl.expression.MethodCallImpl.translate(MethodCallImpl.java:388)
    at ....prepwork.MethodAnalyzer$Visitor.beforeExpression(MethodAnalyzer.java:965)
    at ....cst.impl.expression.MethodCallImpl.visit(MethodCallImpl.java:266)
    at ....prepwork.MethodAnalyzer.analyzeEval(MethodAnalyzer.java:880)
    at ....prepwork.MethodAnalyzer.doStatement(MethodAnalyzer.java:476)
    at ....prepwork.MethodAnalyzer.doMethod(MethodAnalyzer.java:334)
    at ....prepwork.PrepAnalyzer.doMethod(PrepAnalyzer.java:112)
    at ....prepwork.PrepAnalyzer.doType(PrepAnalyzer.java:171/183/160)
    at ....prepwork.PrepAnalyzer.doPrimaryTypesReturnComputeCallGraph(PrepAnalyzer.java:132)
    at ....run.openjdkmain.RunAnalyzer.runAnalyzer(RunAnalyzer.java:173)
```

## Root cause

The failing assert is `GetSetHelper.doGetSetAnalysis` line 56:

```java
public static boolean doGetSetAnalysis(MethodInfo methodInfo, Block methodBody) {
    assert methodBody != null;
    assert !methodInfo.isConstructor();   // <-- fails
    ...
```

It is reached from `ApplyGetSetTranslation.translateExpression` (line 64-65):

```java
if (expression instanceof MethodCall mc) {
    ensureGetSet(mc.methodInfo());          // line 65
    ...
```

and `ensureGetSet` (line 175-179):

```java
private static void ensureGetSet(MethodInfo methodInfo) {
    if (!methodInfo.isAbstract()) {
        GetSetHelper.doGetSetAnalysis(methodInfo, methodInfo.methodBody());   // line 177
    }
}
```

A **qualified explicit constructor invocation** `outer.super(...)` (JLS 8.8.7.1) is represented in the CST as
a `MethodCall` whose `methodInfo()` is the superclass **constructor** (`<init>`). `translateExpression`
already keeps its hands off `Lambda` and anonymous `ConstructorCall` (line 56), but a `MethodCall` that
wraps a constructor invocation is not excluded, so `ensureGetSet` forwards the constructor straight into
`doGetSetAnalysis`, tripping the assert.

## Minimal reproducer (verified)

Independent of the JDK sources and of the `kotlin`-branch front-end changes — a 7-line file is enough:

```java
// p/A.java
package p;
public class A {
    public class Inner { }
    static class Sub extends A.Inner {
        Sub(A a) {
            a.super();          // qualified explicit super() constructor invocation
        }
    }
}
```

```bash
maddi --source <dir-containing-p/A.java> --analysis-steps prep --jmod java.base
# -> Caught exception in statement 0@6:13-6:22
#    Caught exception in method p.A.Sub.<init>(p.A)
#    Exception in thread "main" java.lang.AssertionError at GetSetHelper.doGetSetAnalysis(GetSetHelper.java:56)
```

## Where it bites in the real world

`com.sun.tools.javac.comp.TransPatterns` (jdk.compiler) has an inner class extending `Types.SignatureGenerator`:

```java
private class PrimitiveGenerator extends Types.SignatureGenerator {
    ...
    PrimitiveGenerator() {
        types.super();          // TransPatterns.java:810 — same construct
    }
```

Running parse+prep over `jdk.compiler` (JDK 26) dies here. The front-end scan of `jdk.compiler` is otherwise
clean (368/369 types committed) after two openjdk fixes on `kotlin`
(`b13541f3` empty-switch, `3c834f1f` anonymous-class-in-type-argument); this prep assert is the only thing
now blocking a green parse+prep of that module.

## Suggested fix

Smallest correct change — skip constructors in `ensureGetSet` (get/set analysis is meaningless for a
constructor, and `doGetSetAnalysis` explicitly forbids them):

```java
private static void ensureGetSet(MethodInfo methodInfo) {
    if (!methodInfo.isAbstract() && !methodInfo.isConstructor()) {
        GetSetHelper.doGetSetAnalysis(methodInfo, methodInfo.methodBody());
    }
}
```

(Equivalently, exclude constructor-valued `MethodCall`s in `translateExpression` alongside the existing
`Lambda` / anonymous-`ConstructorCall` guard on line 56.)

Separately, please consider wrapping the per-type/per-method loop in `PrepAnalyzer` with fault isolation so
one bad method is recorded and skipped rather than aborting the whole run — this dovetails with the
error-model unification plan (`ErrorReport` / located, collected analyzer errors). A single construct in one
of ~2400 types should not take the process down.

## Notes

- Not touched from this side: this is modification/prepwork territory. Filing only.
- The `kotlin`-branch front-end fixes referenced above are what made these JDK types reachable by prep, but
  the reproducer proves the get/set defect is independent of them.
