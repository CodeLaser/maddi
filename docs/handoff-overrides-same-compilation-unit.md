# `overrides()` is empty for types sharing a compilation unit, 2026-07-28

> **Update 2026-07-28, later the same day.** A *second*, unrelated override defect has since been found and
> **fixed**: an uncommitted method's fully qualified name was `?.?.<name>`, and since that name is a method's
> identity, two same-signature interface methods were equal while scanning, so the `Set` of overrides kept one.
> That one — not this one — was the cause of the splitclass non-determinism. See
> `MethodInspectionImpl.Builder.fullyQualifiedName` and `TestOverridesUnitOrder`, and read the corrected trap
> section at the bottom of this file before using it. **The defect described below is still open.**

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

## A trap, recorded because it cost real time — and then corrected

Originally recorded as: *"this shape reproduces 'overrides() lost one of two interfaces' perfectly, and it is
not that; the two-interface behaviour is correct and deterministic."*

**The second half of that was wrong.** The two-interface behaviour was *not* deterministic: `overrides()` did
lose an interface, whenever the implementing class was scanned before an interface it implements. It was
invisible here because the two passing tests in `TestOverridesTwoInterfaces` build their source map with a
`HashMap`, whose order is fixed for fixed keys, and that fixed order happens to be a good one. Callers that
build it with `Map.of(...)` get an order the JDK randomises per JVM run, which is how splitclass saw it and
this file did not. `TestOverridesUnitOrder` pins all six orders; the fix is in
`MethodInspectionImpl.Builder.fullyQualifiedName`.

What survives of the original warning: the single-compilation-unit shape still reproduces "lost an interface"
and is still a *different* defect from anything about multiple inheritance of a signature. And the lesson under
it is now sharper — **a test that fixes one arbitrary order of the inputs proves nothing about the others.**
Probe `overrides()` across orders, not in one.
