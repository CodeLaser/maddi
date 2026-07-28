# `overrides()` for types sharing a compilation unit — RESOLVED: there was no defect, 2026-07-28

**Status: closed the same day it was written. The reported defect does not exist.** `overrides()` resolves
correctly when several top-level types share one compilation unit. The reproducer was reading the wrong type.

Kept because the way it was wrong is worth more than the claim was.

## What was reported

> A compilation unit may legally declare several top-level types, as long as at most one is public. When it
> does, **every method of those types reports an empty `overrides()`** — not just the two-interface one, but
> `cm1`, which implements a single interface declared three lines above it.

## What was actually happening

`CommonTest.scan(String fqn, String content)` answered `primaryTypes().getFirst()` — **ignoring the `fqn` it
was handed**. With three top-level types in one unit, that is whichever was declared first: `a.b.InterfaceC`.
So `C.findUniqueMethod("cm1", 0)` was `InterfaceC`'s own `cm1()`, an abstract interface method that correctly
overrides nothing. Every assertion in the test was pointed at the wrong type.

The parser was right throughout. Instrumenting `ScanCompilationUnit` at the point of `addOverrides` shows it,
for the single-unit input:

```
OVR a.b.C.cm1()       raw=[a.b.InterfaceC.cm1]                                  mapped=[a.b.InterfaceC.cm1()]
OVR a.b.C.cm3common() raw=[a.b.InterfaceC.cm3common, a.b.InterfaceIC.cm3common] mapped=[both]
OVR a.b.C.method3()   raw=[a.b.InterfaceIC.method3]                             mapped=[a.b.InterfaceIC.method3()]
```

javac's `elements.overrides(...)` returns `true` for exactly the right pairs, `findOverriddenMethods` returns
both symbols, and both map to the right `MethodInfo`. Nothing is lost anywhere in the chain.

## What changed

- `CommonTest.scan(fqn, content)` now returns the type **named by `fqn`**, and throws listing the primary types
  if there is none. The trap is gone rather than documented.
- Eight call sites passed an `fqn` that did not match what their source declares (`"X"` for `a.b.X`, `"A.b.Y"`
  for `a.b.Y`, `"a.b.C"` for a `C` in the default package, `"a.b.C"` for a type actually called `a.b.X`). All
  corrected; they were harmless only because the argument was being ignored.
- `TestOverridesTwoInterfaces.sameCompilationUnit` is un-`@Disabled` and **passes**, kept as a regression test
  on both facts: several top-level types per unit resolve their overrides, and the harness returns the type you
  asked for.

## Why this is worth keeping

Two false conclusions were drawn from this test in one day, in opposite directions.

1. It was first written up as *"a harness artefact, not a defect"* — correct, but for no articulated reason and
   without evidence.
2. That was then retracted and it was recorded as a genuine parser defect, on the reasoning that several
   top-level types per unit is legal Java and nothing about them sharing a file should change what a method
   overrides. Sound reasoning; the premise — that `overrides()` was empty for `a.b.C` — had never been checked.

The lesson is not "the harness was at fault". It is that **a failing test is a claim about the test, not only
about the code**, and until you have looked at the value the assertion actually received, you do not know which
of the two you are holding. The instrumentation that settled it took ten minutes and could have been run on day
one, before either write-up.

A companion defect found the same day *was* real, and is fixed: an uncommitted method's fully qualified name
was `?.?.<name>`, and since the FQN is a method's identity, two same-signature interface methods compared equal
while scanning, so a `Set` of overrides kept one — see `MethodInspectionImpl.Builder.fullyQualifiedName` and
`TestOverridesUnitOrder`. **That** one is why splitclass was non-deterministic. The two shared a symptom and
nothing else, which is exactly why the first was mistaken for more of the second.
