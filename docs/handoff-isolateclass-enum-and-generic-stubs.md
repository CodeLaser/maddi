# Handoff: two `IsolateClass` stub defects — an enum stubbed as a class, and an inherited generic method erased

Written 2026-07-30. **Both resolved the same day**, at the two lines §2 and §3 name. All three drivers in
`TestIsolateClass4Compiles` pass, and the class-isolate corpus went from **57 to 66 of 100 trees compiling** and
from 93 to **100 of 100 parsing back**. [§7](#7-what-the-fixes-turned-out-to-be) records what each fix actually
took — including a third defect the enum one uncovered in the parser, the two precautions §3 asks for that turn out
to be unnecessary, and the one claim elsewhere in the docs that this work shows to be wrong.

```bash
GRADLE_USER_HOME=/Users/bnaudts/git/ws/object/.gradle-home \
  gradle :maddi-modification-common:test --tests '*TestIsolateClass4Compiles'
```

The original job, kept for the diagnosis: **make the two remaining tests in
`TestIsolateClass4Compiles` (`maddi-modification-common`) pass.** Both were failing on purpose, both were
deterministic, and both were one decision in `IsolationCore` rather than anything structural.

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
| `maddi` `0cee08a5` | **Defect D fixed** (§7c): a field materialized on `ClassSymbolScanner`'s lazy path now gets its flags, so a class-file enum constant is synthetic. Includes the regenerated `JavaLangAnnotation.json`. |

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

*(What it actually produced is in §7; the list below is what was asked for, and all of it was run.)*

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

## 7. What the fixes turned out to be

Both landed where §2 and §3 said, in `IsolationCore`, and the enum one pulled a third defect out of the parser
with it. Measured on the class-isolate corpus, produced twice from the same closed-core parse, once with the
changes stashed:

| | baseline | after |
|---|---|---|
| trees written | 100 of 100 | 100 of 100 |
| trees fully parsed back | 93 of 100 | **100 of 100** |
| compilation units parsed back | 21445 of 21452 | **21452 of 21452** |
| trees that COMPILE | 57 of 100 | **66 of 100** |

By error kind, `<kind>: baseline → after`, over the trees each affects (a tree with several kinds appears in
several rows, so these do not sum to the tree count):

```
pattern or enum constant required                 3 → 0     defect A
annotation value not of an allowable type         6 → 0     defect A, and it needed defect D as well
invalid type for annotation interface element     6 → 0     the second message on those same trees
incompatible types: <X> cannot be converted <X>   1 → 0     defect B
cannot find symbol                              16 → 11    defect B
name clash ... same erasure (3 distinct)          3 → 1     defect B
invalid override                                  2 → 1
abstract method not implemented                  13 → 13    untouched, the largest cause left
qualified new of static class                     5 → 5      untouched
constant expression required                      5 → 5      untouched
```

Per tree, as a set difference: **9 fixed, 0 broken.** Six to defect A (five on an annotation value that is an enum
constant, one on a `switch`), three to defect B — an erased inherited return type, a dropped unit, and, less
obviously, an *erasure name clash*: `GenerateBaumusterBasedVehicleStrategy` and its abstract parent each got a
copy of the same declared method, one per receiver, and the two erased to the same signature without overriding.
Placing it on the declaring type is what removed the clash.

`MAX_TREES_NOT_COMPILING` therefore goes **43 → 34**, with this histogram summarised above the constant.

### 7a. The seven trees that were being dropped

Defect B was costing more than a wrong signature. The baseline drops seven compilation units at re-parse, on five
distinct unresolved calls: `setLayout` (×2), `withLayout`, `isBetween`, `isPositive`, `isTrue` — log4j's builders
and AssertJ's assertions, both of them the self-type idiom (`B extends Builder<B>`, `SELF extends
AbstractAssert<SELF, ACTUAL>`). The erased owner put the inherited fluent method on the wrong type, so the call
did not resolve at all and the unit was dropped.

That is `isolate-class.md` §5, *"still open: fluent chaining through a self-type generic … the one remaining cause
and the only one I would call hard"* — **it is this defect, and it is fixed.** The corpus now parses back whole,
21452 of 21452 units. Worth noting how it looked from the other side: as a *placement* problem it is one line; as
"the receiver's type is a type parameter bounded by the type itself" it looked like recursive generics, which is
why it was written up as hard.

### 7b. Defect A needed the fourth thing this document only suspected

§2 item 4 said to *check* `reproducedParentClass` for an enum, because "the commit may assert it". It does:
`TypeInspectionImpl.Builder.commit` asserts that an enum-natured type's parent is `java.lang.Enum`, and with the
nature changed and nothing else, the driver failed with a bare `AssertionError` whose stack points at the commit
rather than at the decision. Hence `enumParentOf`, giving each enum stub `java.lang.Enum<itself>` — which
`TypePrinterImpl` then declines to print, exactly as the language does. `reproducedParentClass` still maps
`java.lang.Enum` to `Object` and now says why: that is right for every stub that is *not* enum-natured.

`enumStubs` was needed, and for a reason beyond the four sites §2 lists: `values()` and `valueOf(String)`. They
are compiler-generated for every enum, so stubbing them is "values() is already defined" — but only now that the
stub is an enum; as a class it was legal. They reach `ensureMethodInfo` only from the class-file path (javac's
parse tree does not carry them), so no driver can exercise that guard; it is written from the corpus's side.

### 7c. Defect D, which defect A uncovered: an enum constant that was not synthetic

With the nature fixed, six corpus trees moved from two flavours of annotation error to a third:

```
an enum annotation value must be an enum constant
```

for verbatim `@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)`, against the stub

```java
public enum TransactionAttributeType { ; public static TransactionAttributeType REQUIRES_NEW; }
```

— an enum with an *empty* constant list and an ordinary static field beside it. The isolator asks
`fieldInfo.isSynthetic()`, which §2 is right to say is the marker; the constant simply did not have it.

**`ClassSymbolScanner.ensureField` never applied the flags.** It is the lazy path — a field referenced before its
owner's members are loaded — and unlike its eager twin `addFieldToType` twenty lines above, it did not call
`flagHelper.field`. So the field arrived with no modifiers at all: not `final`, and decisively not `synthetic`,
the one thing separating an enum constant from an ordinary static field of the same type. `addFieldToType` dedups
by name, so whichever path materialized the constant first decided the answer for good. A probe on two JDK enums,
before and after:

```
RetentionPolicy.RUNTIME    synthetic=false static=true   ->  synthetic=true
TimeUnit.SECONDS           synthetic=false static=true   ->  synthetic=true
```

This is **the same defect shape as §4's** — one property, two inspection paths, different answers — and it went
to the same kind of place: the parse site, one line, mirroring the twin that had it right. §4's warning still
holds and was the reason to look here rather than at the accessor.

It has one visible consequence outside the isolators, and it is worth knowing about: `TestAnalysisHintsCompiler`
regenerates the committed analysis archive, and `JavaLangAnnotation.json` changed — ten enum constants of
`ElementType` and `RetentionPolicy` gained `notNullField`, which the four that had already been loaded eagerly
carried all along. The archive was recording the inconsistency; it is now uniform. Nothing else in the archive
moved, and the whole maddi suite is green.

### 7d. Two things §3 asked for that were not needed

- *"keep stubbing the scope type"* needs no care at all: the `ensureTypes(mc.object().parameterizedType())` call
  that used to compute the erased owner already does it, and the fix only changes what is done with the result.
- `superTypeStubOf` is **not** reusable here, as §3 wondered. It answers a different question — which stub a
  `super.m()` belongs on, falling back to the isolated type's own parent — and there is no
  "declared on the scope type itself" case to write either, because `ensureType` of that type *is* the scope's
  stub.

### 7e. Verification, as run

Everything §5 asks for, all green: `TestIsolateClass4Compiles` on its own and as part of
`:maddi-modification-common:test` (68); `:maddi-java-openjdk:test` (549), `:maddi-inspection-openjdk:test` (50),
`:maddi-modification-prepwork:test` (218, `TestGetSet` included), `:maddi-modification-link:test` (405),
`:maddi-modification-analyzer:test` (269), `:maddi-aapi-parser:test` (213); downstream
`:codelaser-refactor-extractmodule:test` (504) and `:codelaser-refactor-structuremodule:test` (63). `cst-impl`,
`cst-analysis` and `inspection-resource` are upstream of the change and were reported UP-TO-DATE / NO-SOURCE
rather than re-run.

### 7f. Left deliberately

- The leftover `import static a.b.UsesEnum.Kind.CLASS;` in the enum driver's output. A `case` label is a
  default-scope `FieldReference`, indistinguishable in the CST from an unqualified static read, so it is recorded
  as a static import. Checked against javac: two single-static-imports of same-named fields are legal, and only an
  *ambiguous use* is an error — a case label is not a use of an import. Noise, not a hazard.
- **The isolated type's own nature.** `IsolateClass.isolate` always emits `typeNatureClass()`, so isolating an
  enum loses its nature and its constants. The stubs now reproduce every nature; the isolated type does not. No
  type in the hundred-class corpus is an enum, so this is unmeasured rather than harmless.
- The `int`-per-numeric-constant hack in `ensureField` stays, per §2: still needed for genuine `static final int`
  constants, and enums no longer reach it.
