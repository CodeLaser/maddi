# `overrides()` is empty for types sharing a compilation unit, 2026-07-28

## The defect

A compilation unit may legally declare several top-level types, as long as at most one is public. When it does,
**every method of those types reports an empty `overrides()`**.

Reproducer, already in the repo and `@Disabled`:
`maddi-java-openjdk/src/test/java/org/e2immu/language/java/openjdk/method/TestOverridesTwoInterfaces.java`,
method `sameCompilationUnit`. Remove the `@Disabled` and it fails; the file's other two tests are the same
scenario split across three compilation units, and they pass (verified over 6 runs).

```java
package a.b;
interface InterfaceC  { void cm1(); void cm3common(); }
interface InterfaceIC { void cm3common(); void method3(); }
class C implements InterfaceC, InterfaceIC {
    @Override public void cm1() { }          // overrides() == []  <-- expected [a.b.InterfaceC]
    @Override public void cm3common() { }    // overrides() == []  <-- expected both interfaces
    @Override public void method3() { }      // overrides() == []
}
```

Note it is not limited to the two-interface case: `cm1` implements a single interface declared three lines
above it and still reports nothing. So this is about the compilation-unit boundary, not about multiple
inheritance of a signature.

## Why it matters

`overrides()` is how a consumer answers "which interface does this member belong to". jfocus's splitclass reads
it in `SplitAlongInterfaces.computeInterfaceTypes` to group members by implemented interface; with an empty
result every such method is attributed to *no* interface and silently lands in the common part. Any analysis
keyed on interface membership degrades the same way, without an error.

## Where to start

`ScanCompilationUnit` in `maddi-java-openjdk`. The two passing tests differ from the failing one **only** in
whether the types arrive as one unit or three, so the divergence is in how the scanner resolves a type
reference to a sibling declared in the same unit — most likely the interfaces are not yet inspected (or not yet
registered under their fully-qualified names) at the point where the implementing class's methods are matched
against them. Compare with the type-resolution path that already works for the multi-unit case.

Worth checking whether the same boundary affects other hierarchy-derived queries
(`typeHierarchyExcludingJLO`, `interfacesImplemented`) or only `overrides()`.

## A trap, recorded because it cost real time

This shape reproduces "`overrides()` lost one of two interfaces" **perfectly**, and it is not that. While
hunting a non-determinism in splitclass I wrote exactly this test, saw it fail, and briefly believed I had
found the cause. The two-interface behaviour is correct and deterministic; only the single-unit case is broken.
Any test written to probe `overrides()` must use separate compilation units unless it is probing this defect.
