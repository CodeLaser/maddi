# IsolateMethod: the three issues, resolved — and the one they were hiding, 2026-07-28

The three issues handed over earlier on 2026-07-28 are fixed (`c94b5d2f`, `78524232`). Isolating the 40 biggest
methods of closed-core now writes **40 of 40** frames and **39 of them parse back**, against a baseline of 39
written and 37 parsing back.

Each was reproduced as a unit test first — 2 to 4 seconds each, against an 85 s / 16 G corpus run — so all three
are now permanent regression tests rather than corpus observations.

| | was | now |
|---|---|---|
| isolates written / requested | 39 / 40 | **40 / 40** |
| parse back cleanly | 37 | **39** |

One frame still fails, on a defect the `ParamDouble` error was masking. It is described in
[§4](#4-still-open-placement-is-decided-by-the-first-reference-not-by-all-of-them), and it is the interesting
one: the fix for issue 2 has changed what the right design for it is.

---

## How to reproduce any of this

The harness lives in `jfocus-refactor-service`, module `codelaser-refactor-extractmodule`, package
`io.codelaser.jfocus.refactor.plan.splitmethod.run` (`ClosedCoreIsolates`, `TestIsolateclosed-coreMethods`,
`TestSplitIsolatedMethods`). It self-skips without the commercial closed-core corpus. That repo includes maddi
with `includeBuild("../maddi")`, so a maddi working-tree change reaches the driver directly — no publish step.

```bash
# produce the corpus: parses 3M lines, ~100 s, needs 16G. Explicitly gated -- it will OOM the
# module's ordinary 8G test task otherwise.
gradle :codelaser-refactor-extractmodule:test --tests '*TestIsolateclosed-coreMethods' \
    -Dtest.split.isolate=1 -PtestMaxHeap=16G

# the isolates land in codelaser-refactor-extractmodule/build/closed-core-isolates/isolate/
```

The `VERIFICATION` section of that run lists every frame that did not parse back. It is on the test's stdout,
which gradle does **not** put on the console — read it from the JUnit XML. The drop *reasons* are in the
scanner's warnings, in the same place:

```bash
python3 -c "
import re,html,glob
f=glob.glob('codelaser-refactor-extractmodule/build/test-results/test/*TestIsolateclosed-coreMethods.xml')[0]
out=html.unescape(re.search(r'<system-out>(.*)</system-out>', open(f).read(), re.S).group(1))
for l in out.split('\n'):
    if 'Dropping compilation unit' in l:
        m=re.search(r'/([A-Za-z0-9_]+)\.java \(unresolved symbol\): (.*)', l)
        if m: print(f'{m.group(1):58s} {m.group(2)[:55]}')
"
```

Two things the corpus taught us the hard way, both already encoded in the harness:

- A compilation unit whose symbols do not resolve is **dropped with a warning**, not a parse exception.
  Counting `summary.parseExceptions()` reports a clean corpus that has silently lost files; set-difference
  between what was written and what parsed back is the only check that catches it.
- The frame names carry an ordinal (`..._11`, `..._31`) from the size ranking. It has been stable across
  every run so far, but it is derived, not intrinsic — confirm by the class name, not the number.

And one learned this round: **a fixed error can reveal a worse one.** `RecordDTO_set_31` moved from
"Type ParamDouble not found" to "Unresolved method call 'getObjectID'". The frame count did not change, and a
run that only counts would have read as "issue 2 not fixed". Read the reasons, not the totals.

---

## 1. Two JDK types with the same simple name — fixed

**Was.** `ExportJob_insertRecords_11` dropped on `Unresolved method call 'setDate'`. The frame
imported both `java.sql.*` (the import computer collapses four or more types of a package) and `java.util.Date`
single-type. A single-type import beats an on-demand one, so the verbatim `Date dateValue` meant
`java.util.Date`, and `PreparedStatement.setDate(int, java.sql.Date)` rejected it.

**The open question, answered.** Yes — `java.sql.Date` *is* a competitor. It is collected, and arbitration
picks it correctly. That was never the problem.

**Why the earlier attempt failed.** It filtered what `IsolateMethod` hands to the import computer. But the
computer does not only import what you hand it: it also collects every type it finds while walking the
compilation unit, and imports that too. `java.util.Date`, reached through a reconstructed signature, arrived
that way no matter what the caller did. Two further traps behind it, both in `ImportComputerImpl`:

- it resolves a simple-name clash **first-come-first-served over a `HashSet`**, so which type wins is
  effectively arbitrary and can change between runs;
- its `conflict()` check — the guard that should suppress `import java.sql.*` when a `java.util.Date` is in
  play — asks `typesPerPackage` for the contents of `java.sql`, and that function only knows the *main
  sources*. For a JDK package it returns nothing, so no conflict is ever detected.

**The fix.** An explicit veto, `ImportComputer.doNotImport(TypeInfo)`: never import this type, print it fully
qualified. The vetoed type stays in `typesReferenced` so `conflict()` can still see it. `IsolateMethod` records
which JDK types the verbatim text names by simple name (`Data.jdkNamedSimplyInSource`, set in `ensureType`),
gives those the name, and vetoes the rest — `arbitrateJdkImports`, sorted by FQN first so the outcome does not
depend on hash order.

Regression test: `TestIsolateMethod17ImportCollision`. It needs `java.sql` on the class path, which the lean
test inspector does not have — see its `beforeEach`.

**Left in place, deliberately.** The order-dependence and the blind `conflict()` are still there for any caller
that does not know to veto. Fixing them properly means giving the import computer a package-contents source
that covers the JDK, which is a change to how it is constructed everywhere.

## 2. A stub in a namespace chain, referenced both fully-qualified and simply — fixed

**Was.** `RecordDTO_set_31` dropped on `Type ParamDouble not found`. Declared in the namespace chain
`frame → be → closed-core → legacy → parameter` — which is what keeps the 18 verbatim
`(com.example.legacy.parameter.ParamDouble)value` casts resolving — but a *reconstructed* field printed
`ParamDouble acceleration;`, and a simple name does not reach into a sibling branch of the nesting tree.

**The diagnosis was right; the blocker was one level down.** The fix is in printing, and the per-element
printer hook is the right seam — but handing a custom field/method printer to the compilation-unit printer
only ever reaches the **top-level** types. `TypePrinter`'s two-argument `print(importData, doTypeDeclaration)`
delegates to the five-argument one with `MethodPrinterImpl::new, FieldPrinterImpl::new, TypePrinterImpl::new`
— it resets to the defaults. Every stub is nested, so every stub got the default printer. Any amount of work
on the qualification alone would have changed nothing.

**The fix.** `PropagatingTypePrinterFactory` hands the custom printers back down at each level of nesting, and
`OutOfScopeQualified` prints a referenced stub qualified from the primary type
(`RecordDTO_set_31.com.example.legacy.parameter.ParamDouble`) — but only where the simple name genuinely
does not resolve from the referencing type. `outOfScope` walks the referencing type outwards looking for the
target or its enclosing type; supertype members are not considered, because over-qualifying is harmless
whereas under-qualifying is what breaks the parse. Ordinary isolates are unchanged.

Regression test: `TestIsolateMethod16NamespaceReferences`.

**Found on the way.** `runtime.newMethodPrinter(mi)` constructs `MethodPrinterImpl(methodInfo)`, which passes
`formatter2 = false`. That is why the `@Override` supertype has always printed with different spacing from
every other stub. Added `Factory.newMethodPrinter(TypeInfo, MethodInfo, boolean)`, the overload
`newFieldPrinter` and `newTypePrinter` already had; the two expected outputs that had baked in the old spacing
are updated (`TestIsolateMethod12Override`, `13OverrideQualified`).

## 3. `MethodPrinterImpl` NPE — fixed, and it was not about `return;`

**Was.** `legacyDataSourceGenerator.generateDataSourceXMLForPanel` could not be isolated at all, so the corpus
was 39 files for 40 requested. Diagnosed earlier as a value-less `return;` yielding a null expression.

**It is not.** Line 124 read the default value as
`methodBody().asInstanceOf(ReturnStatement.class).expression()`. A method body is a `Block`, and a `Block` is
never a `ReturnStatement`, so `asInstanceOf` returned null **always**. The branch could only ever NPE, for any
input. What actually reaches it is a *non-abstract method on an annotation type* — and only a synthetic builder
can produce one, because the parser makes every annotation attribute abstract.

**The real defect, upstream.** `Data.ensureMethodInfo` gave a member of an `@interface` stub the ordinary stub
shape: a body returning null. An annotation member is implicitly abstract and may have neither parameters nor a
body, so the frame was never valid Java — the printer merely failed first. The other route into an annotation
stub (an annotation *use* in the pasted text, `@Named("x")`) already built it correctly; both now go through
one `ensureAnnotationAttribute`, keyed in `methodMap`, so a body that both applies an annotation and reads an
attribute off an instance gets one declaration rather than two.

The printer is fixed alongside: it reads a single return statement out of the block, and prints `;` when there
is none, which is what an attribute stub has.

Regression test: `TestIsolateMethod11Annotations.a3` — `named.value()` on an annotation-typed parameter.

**Separately noted, not fixed.** The parser drops an attribute's `default` value entirely:
`ParseMethodDeclaration` never visits the `DefaultValue` node, so `String extra() default "!"` round-trips as
`String extra();`. `TestAnnotations.test6`/`test9` parse such types but never print them, which is why nothing
caught it. This is why no *parsed* annotation reaches the branch above either.

---

## 4. Still open: placement is decided by the first reference, not by all of them

**Symptom.** `RecordDTO_set_31`, the one frame of 40 that still does not parse back:
`Unresolved method call 'getObjectID'`.

**What the frame contains.**

```java
    class ObjectId extends IParamDataType implements Serializable {Long getObjectID() { return null; } }   // 175
    class be { class closed-core { class legacy { class parameter {                                            // 176
        class ParamBoolean ... class ParamDate ... class ParamDistance ... class ParamDouble ...                   // 13 siblings
    } } } }
        this.id = ((com.example.legacy.parameter.ObjectId)value).getObjectID();                          // 202
```

`ObjectId` and its thirteen package-siblings all come from `com.example.legacy.parameter`. Thirteen are in
the namespace chain; `ObjectId` alone is in the frame. So the verbatim cast at line 202 names a type that
does not exist, the parser invents a stub for it, and `getObjectID()` is unresolved.

**Root cause.** `Data.ensureType` decides placement on the **first** encounter and caches it in `typeMap`. The
branch that builds a namespace chain fires on `ds != null && ds.detail(typeInfo.packageName()) != null` — this
one reference was written package-qualified. `ObjectId` happens to be reached first through a path with no
detailed sources (a reconstructed field or signature), lands in the frame, and the later fully-qualified cast
silently reuses that placement. Nothing reconciles the two.

This is a general defect, not specific to this type: any type whose first reference is reconstructed and whose
later reference is written package-qualified lands wrong. It is only rarely fatal, because the fully-qualified
spelling has to actually occur in the pasted text.

**The constraint, before any design.** An isolate has two kinds of reference to a stubbed type, and they pull
in opposite directions:

- a **written** reference is verbatim text and its spelling cannot be changed. `IOrderService orderService =
  ...` requires the type nested *directly in the frame*, because a class buried in a namespace chain is not in
  scope by simple name — that is what `TestIsolateMethod9Imports` pins, and its header comment says so.
  `(com.example.legacy.parameter.ObjectId) value` requires exactly the opposite.
- a **reconstructed** reference is ours to print, and since issue 2 `OutOfScopeQualified` qualifies it wherever
  the simple name would not resolve.

So issue 2 freed the reconstructed side only. **Written references still dictate placement**, and there is one
declaration site per type. Placing everything in the namespace chain uniformly is therefore *not* an option:
it would break every isolate whose body names an imported type by its simple name, which is most of them.
(An earlier revision of this document recommended exactly that. It is wrong.)

**So the fix is fixed in shape — gather all written evidence, then place — and the choice is how.**

| | Approach | Cost |
|---|---|---|
| A | A scan-only pass through the existing `ensureType` / `MyVisitor`, recording evidence without building stubs | No duplicated knowledge, but it runs mutating code in a non-mutating mode; the two passes can drift apart silently |
| B | A separate lightweight visitor that only collects "was this type's package written out" | Clean separation, but duplicates `MyVisitor`'s knowledge of which constructs carry a written type name — miss one and a type is misplaced with no error |
| C | Defer stub creation: collect every reference first, then create and place in one pass | Conceptually right, largest change — stub identity is needed *during* traversal, since fields and methods are attached to stubs as they are discovered |

I would take **B**. The set of constructs that can carry a written type name is short and stable, and it leaves
the mutating pass untouched; A's failure mode is invisible drift between two passes that must agree. C is the
right end state if `IsolateClass` needs it anyway.

**The residual case, currently unreachable.** A type written *both* simply and package-qualified in one body
cannot be satisfied at all: no single declaration is in scope both ways. Either accept and log it, or declare
it at frame level with a subclass alias in the chain (`class ObjectId extends X.ObjectId {}`) — casts
and calls would resolve through inheritance, but it is a distinct type, so `instanceof` and assignment
compatibility go subtly wrong. Not needed for the corpus: `ObjectId` is written once, so A, B and C all fix
it today. This only matters if the guarantee is wanted.

Validate on the corpus, not on the unit tests: no unit test has two references to one type disagreeing.

**Where.** `Data.ensureType` (the placement branches), `Data.namespaceStub`, `Data.nestingWouldHide`,
`Data.frameSimpleNameClaims`.

---

## State, so a later run can tell drift from regression

Baseline of 2026-07-28, after the fixes, all measured on this machine:

| | |
|---|---|
| isolates written / requested | **40 / 40** |
| parse back cleanly | **39** (§4 blocks one) |
| parse errors in the corpus | 0 |
| split frames exercised | 39 (the frame of §4 produces no type, and the driver says so) |
| split attempts / applied / failed | 1,476 / 1,374 / **0** — of which logged-not-thrown: 0 |
| splits written and re-parsed | 37, **0 parse errors** |
| `IsolateMethod` unit tests | 49, 0 failures, 1 skipped |
| whole maddi `test` | green |

Do not compare the split counts to the earlier 9,573 / 8,863: the corpus changed and the candidate bounds
(`-Dtest.split.minLines/targetLines/maxLines`) are configurable, so those two runs are not the same
measurement. What does compare across runs is `failed` and the re-parse error count, and both are 0.

One splitter bug is knowingly tolerated, in `TestSplitIsolatedMethods.KNOWN_FAILURES` with its reproducer:
`AddMethod.returnStatementAtEnd:335` asserts `recordType != null` on exactly the path taken when a record IS
needed to carry several values out. Reproduce with `-Dtest.split.only=Factory_parse_29
-Dtest.split.perMethod=0`, candidate 13. That entry comes out when the record-type decision is fixed.

## A note for IsolateClass

Ten of the eleven earlier fixes, and all three of these, landed in the reusable core — `Data` (`ensureType`,
`ensureTypes`, `ensureMethodInfo`, `ensureField`, `ensureAnnotationAttribute`, `addDummyInterfaceMethods`) and
`MyVisitor` — so they carry over. The `OwnedMethod` dedup key matters more for a class isolate, where many kept
methods reach the same declared method through different receivers. The only method-shaped code is
`isolate(MethodInfo, String)`: frame naming, mapping the declaring type's type parameters onto the frame, the
single `_method_to_be_replaced_` marker, and the `@Override` supertype. That is the seam to generalise — seed
`visit` per kept member, and grow the printer to a marker per method.

Issue 2's fix matters more for `IsolateClass` than for `IsolateMethod`: a class isolate reconstructs many more
signatures, so many more references print with simple names, and `OutOfScopeQualified` is what keeps them
resolving. §4 matters more too, for the same reason — more reconstructed first-encounters racing ahead of the
written ones. Settling §4 before that work starts is worth it.
