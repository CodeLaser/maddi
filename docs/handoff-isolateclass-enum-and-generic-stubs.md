# Handoff: two `IsolateClass` stub defects — an enum stubbed as a class, and an inherited generic method erased

Written 2026-07-30. **The job: make the two remaining tests in
`TestIsolateClass4Compiles` (`maddi-modification-common`) pass.** Both are failing on purpose, both are
deterministic, and both are one decision in `IsolationCore` rather than anything structural.

```bash
GRADLE_USER_HOME=/Users/bnaudts/git/ws/object/.gradle-home \
  gradle :maddi-modification-common:test --tests '*TestIsolateClass4Compiles'
```

Expected today: `switchOverEnum` and `inheritedGenericMethod` FAIL, `staticallyImportedInterfaceField` PASSES
(fixed, see §4). Nothing else in maddi fails.

---

## 1. Where these came from, and why they were invisible

They were found by isolating three real fernflower types through the new `debug.isolateClass` DSL method and
compiling the results. Both had been in the code for as long as `IsolateClass` has existed. What hid them:

- the isolate tests re-parse their output with **default** `JavaInspector.ParseOptions`, and
  `ParseOptions.Builder` leaves `failFast` false. That becomes `MaddiDiagnosticCollector.ignoreErrors`, so every
  javac error is logged at INFO and carried. `setFailFast(true)` is the switch, and it is what the new test uses;
- neither `parseExceptions()` nor the returned type set sees a javac ERROR, so the older assertions could not have
  caught them.

**The front end is not the weak link.** `ScanCompilationUnits.scan()` calls `task.parse()` then `task.analyze()`,
so javac's ENTER/ATTRIBUTE/FLOW all run and these are javac's own messages. An earlier attempt to add a separate
`javax.tools` harness was deleted for exactly that reason: it recomputed a verdict maddi already has.
`CompilationProblems` now carries the diagnostics with file, line and column, so the failure message is readable
without going to the log.

## 2. Defect A — an `enum` is stubbed as a `class`

Reduced fixture: `UsesEnum` with `private enum Kind {CLASS, FIELD, METHOD}` and a `switch (kind)` over it.
javac says, twice:

```
pattern or enum constant required
```

Because the stub is

```java
public static class Kind { public static Kind CLASS; public static Kind FIELD; public static Kind METHOD; }
```

— enough for `Kind.CLASS`, not enough for `case CLASS:`.

**The site is `IsolationCore`, in the stub-creation block around line 184:**

```java
.setTypeNature(typeInfo.isAnnotation() ? runtime.typeNatureAnnotation()
        : isInterface ? runtime.typeNatureInterface()
        : runtime.typeNatureClass())
```

Annotation and interface natures are reproduced deliberately (the comment above it explains why); **enum falls
through to `class`**, and "everything else is a class" is stated as if it were harmless.

**What an enum stub needs**, all of it verified in the code:

1. `runtime.typeNatureEnum()` when `typeInfo.isEnum()`.
2. Its constants emitted as **enum constants, not fields**. An enum constant is a field with
   `isSynthetic() == true`: `TypePrinterImpl.enumConstantStream` (line 241) prints exactly those, as bare names
   before the other members, and `FieldInspectionImpl.Builder` (line 79) has the note *"synthetic in particular
   distinguishes an enum constant … dropping it makes a translated enum print as `static final E X = new E()`
   instead of `X`"*. So in `ensureField`, when the owner stub is an enum and the original field is one of the
   enum's constants, build it with `setSynthetic(true)` and no explicit `static`/`final`/initializer.
3. Probably an `enumStubs` set beside `interfaceStubs` / `annotationStubs`: several places branch on those
   (`ensureField` line 291, line 389, line 564, line 600) and an enum stub is neither.
4. Check `reproducedParentClass` for an enum — a real enum extends `java.lang.Enum<E>`, and the commit may assert
   it. `applyStubTypeAccess` may also need care: a nested enum can be private, a top-level one cannot.

**A simplification this unlocks.** `ensureField` currently hands each numeric constant a unique `int` value:

```java
boolean numericConstant = newPt.isNumeric() && (isInterfaceField || fieldInfo.isStatic() && fieldInfo.isFinal());
```

with the comment *"a numeric constant … may appear as a switch `case` label; those must be distinct compile-time
constants"*. That exists because enums were being stubbed as classes and `int` constants were the only way to
make a `switch` work. Real enum stubs make it unnecessary **for enums**; it is still needed for genuine
`static final int` constants, so do not remove it — just do not extend it.

## 3. Defect B — a method inherited from a generic supertype loses its type argument

Reduced fixture: `ListStack<T>` with `T pop()`, `ItemStack extends ListStack<Item>`, and a body doing
`Item first = stack.pop();`. javac says, twice:

```
incompatible types: java.lang.Object cannot be converted to p.q.Item
```

The real case was fernflower's `ExpressionStack extends ListStack<Exprent>`, where the stub came out as

```java
public class ExpressionStack extends ListStack<Exprent> {
    public Object pop() { return null; }        // <-- erased, and on the wrong type
    ...
}
```

Note `ListStack<T>` *is* emitted as a generic stub — the type parameter survives. What does not survive is the
method's placement: `pop()` is declared on `ListStack<T>` and returns `T`, and the stub put it on the **scope
type** with `T` erased to `Object`.

**The site is `IsolationCore`, the `MethodCall` branch, lines 903–906:**

```java
} else {
    ParameterizedType firstOwner = ensureTypes(mc.object().parameterizedType(), ds(mc.object()));
    owner = erasedOwner(firstOwner);
}
ensureMethodInfo(owner, mc.methodInfo());
```

`erasedOwner` (line 795) resolves a type parameter to its bound, which is right for *its* purpose and wrong as
the owner of an inherited method.

**The fix is already written, thirty lines below, for fields.** The `VariableExpression` / `FieldReference`
branch places an inherited field on its **declaring** type, with a comment that states the principle:

> *"still stub the scope's own type (it may be referenced nowhere else), but place the field on its DECLARING
> type, not on the scope type. An inherited field accessed via a subtype (`paymentPeriod.residualValue`, declared
> on `PeriodData`) belongs on the supertype stub: the subtype inherits it via `extends`, and an access via the
> supertype resolves too."*

Methods want the same treatment: stub the scope type as now, but pass `mc.methodInfo().typeInfo()` as the owner
when the method is not declared on the scope type. The generic parameter then stays `T` on `ListStack<T>`, and
`ItemStack extends ListStack<Item>` makes the call site see `Item`. Apply it to the `MethodReference` branch too
(lines 909–921), which has the identical shape.

Two things to watch:

- keep stubbing the scope type. The field branch's parenthesis — *"it may be referenced nowhere else"* — applies
  equally here, and dropping it will lose types.
- `superTypeStubOf` (used by the `super.` case) may already do most of this; check before writing a new helper.

## 4. What is already done — do not redo it

| | |
|---|---|
| `maddi` `9bacea3b` | **Defect C fixed:** an interface field is static whichever path read it. Applied in `ScanCompilationUnit.field`, **not** in `FieldInfo.isStatic()`. |
| `maddi` `8bd48182` | `CompilationProblems` carries its diagnostics; `TestIsolateClass4Compiles` added with all three drivers. |
| `jfocus-refactor-service` `1fde5cb2` | Both isolate drivers ask maddi instead of running a second javac: `ClosedCoreIsolates.compileErrors`. |
| `jfocus-refactor-service` `91a16a7f`, `jfocus-refactor-server` `afaea7e`, `ebfeda0` | `debug.isolateClass` and its coverage; `ADDING-A-DSL-METHOD.md`. |

**The interface-field fix is the cautionary tale of this piece of work, and it generalises to both defects
above.** The instruction was to hard-code the JLS 9.3 rule in `FieldInfo.isStatic()`, and the justification was
sound: the nature of a type is recorded before its fields are scanned (`ScanCompilationUnit.field` branches on
`owner.typeNature().isRecord()` the line after computing the flag). Doing it in the accessor broke four
`TestGetSet` cases:

```
expected: java.util.List._synthetic_list#a.b.X.intList
but was:  java.util.List._synthetic_list
```

A `FieldReference` to a **static** field drops its scope. maddi deliberately models synthetic *instance* fields on
interface types — `CreateSyntheticFieldsForGetSet:94` gives `java.util.List` a non-static `_synthetic_list` — so
making the accessor authoritative collapsed variable identity between two lists. An accessor cannot tell a
synthetic instance field from a constant; the parse site has the declaration in hand and can.

So: **a rule that is true of the language is not automatically true of maddi's model**, which carries synthetic
members with deliberate flags. Both fixes above set properties on stub types; check what else branches on those
properties before changing them. (Note also the honest counter-example: `isPropertyFinal()`, ten lines below
`isStatic()` in the same class, *does* apply JLS 9.3 through `owner.isInterface()` — safe there, because a
synthetic field reading as `final` collapses nothing.)

## 5. Verification expected of the fixes

- `:maddi-modification-common:test` — all of `TestIsolateClass4Compiles` green, and run it **both** on its own and
  as part of the module. The interface case was JVM-state dependent before it was fixed properly, and that was
  only visible because the two runs disagreed.
- `:maddi-cst-impl:test :maddi-java-openjdk:test :maddi-inspection-openjdk:test :maddi-modification-prepwork:test`
  — green now; `TestGetSet` in prepwork is the canary for field-modelling changes.
- `:maddi-modification-link:test :maddi-modification-analyzer:test :maddi-aapi-parser:test :maddi-cst-analysis:test
  :maddi-inspection-resource:test` — green now (~2.5 min).
- Downstream, in `~/git/ws/object/jfocus-refactor-service` (composite build, no publishing needed):
  `:codelaser-refactor-extractmodule:test` and `:codelaser-refactor-structuremodule:test`.
- The real measure of an `IsolateClass` change is the class-isolate corpus, whose javac gate is a **ratchet**:
  `TestIsolateClosedCoreClasses.MAX_TREES_NOT_COMPILING`, currently **43** of 100, was 100 before five `IsolationCore`
  fixes. It needs the closed-core corpus, so it is a driver, not a test — but if you have that corpus, both defects
  here should move the number down, and the comment above the constant asks you to lower it when they do.

## 6. Where the evidence is, if a reduction turns out not to match

The three fernflower isolates that produced these errors can be regenerated in about a minute:

```bash
cd ~/git/ws/object/jfocus-refactor-server
gradle :codelaser-refactor-graalpy:slowTest --tests '*TestIsolateClassFernflower'   # leaves a configured project
gradle :codelaser-refactor-graalpy:scriptRunner \
    -PclProject=fernflower-TestIsolateClassFernflower -PclRunnerDir=/tmp/clr -PclMode=ro
# then drop codelaser-refactor-server/src/test/resources/isolateclasses.py into /tmp/clr/in/
# trees land under work/fernflower-TestIsolateClassFernflower/isolate/<Type>/
```

`SwitchPatternHelper` gives 14 files, `ClassWriter` 70, `ExprProcessor` 61. Compile one with an empty classpath to
see the errors as originally found. Note this is the *diagnostic* route — the gate itself should stay
`setFailFast(true)`, per §1.
