# IsolateMethod: the three issues left, 2026-07-28

`IsolateMethod` was hardened by isolating the 40 biggest methods of closed-core and re-parsing every frame
on a JDK-only class path. Eleven defects were found and fixed (commits `269c5c84`, `4736d21e`, `645c8bfb`,
`2cbe2106`, `7046d751`). **37 of 39 written isolates now parse back**, up from 26.

Three issues remain. Each is diagnosed to root cause below; none is a mystery, all three need a design
decision rather than a patch, which is why they were left rather than guessed at.

---

## How to reproduce any of this

The harness lives in `jfocus-refactor-service`, module `codelaser-refactor-extractmodule`, package
`io.codelaser.jfocus.refactor.plan.splitmethod.run` (`ClosedCoreIsolates`, `TestIsolateclosed-coreMethods`,
`TestSplitIsolatedMethods`). It self-skips without the commercial closed-core corpus.

```bash
# produce the corpus: parses 3M lines, ~85 s, needs 16G. Explicitly gated -- it will OOM the
# module's ordinary 8G test task otherwise.
gradle :codelaser-refactor-extractmodule:test --tests '*TestIsolateclosed-coreMethods' \
    -Dtest.split.isolate=1 -PtestMaxHeap=16G

# the isolates land in codelaser-refactor-extractmodule/build/closed-core-isolates/isolate/
```

The `VERIFICATION` section of that run lists every frame that did not parse back. The *reasons* are in the
scanner's warnings, which are captured in the JUnit XML rather than printed to the console:

```bash
python3 -c "
import re,html,glob
out=html.unescape(re.search(r'<system-out>(.*)</system-out>', open(glob.glob(
  'codelaser-refactor-extractmodule/build/test-results/test/*TestIsolateclosed-coreMethods.xml')[0]).read(), re.S).group(1))
for l in out.split('\n'):
    if 'Dropping compilation unit' in l:
        m=re.search(r'/([A-Za-z0-9_]+)\.java \(unresolved symbol\): (.*)', l)
        if m: print(f'{m.group(1):58s} {m.group(2)[:55]}')
"
```

Note two things the corpus taught us the hard way, both already encoded in the harness:

- A compilation unit whose symbols do not resolve is **dropped with a warning**, not a parse exception.
  Counting `summary.parseExceptions()` reports a clean corpus that has silently lost files; set-difference
  between what was written and what parsed back is the only check that catches it.
- The frame names carry an ordinal (`..._11`, `..._31`) from the size ranking. It has been stable across
  every run so far, but it is derived, not intrinsic — confirm by the class name, not the number.

---

## 1. Two JDK types with the same simple name; the wrong one wins the import

**Symptom.** `ExportJob_insertRecords_11` is dropped: `Unresolved method call 'setDate'`.

**What the frame contains.**

```java
package isolate;
import java.io.Serializable;
import java.sql.*;          // collapsed by the ImportComputer (threshold 4)
import java.util.Date;      // single-type
...
    Date dateValue;                                              // line 777, pasted verbatim
    dateValue = new Date(CustomDateUtil.parseDate(...).getTime());  // line 999
    pstmt.setDate(valueCtr++, dateValue);                        // line 1000
```

`pstmt` is a **real** `java.sql.PreparedStatement` — line 983, `try (PreparedStatement pstmt =
this.connection.prepareStatement(query))`, not a stub, because `isJdkType` correctly keeps it.

**Root cause.** A single-type import beats an on-demand import. `import java.util.Date` therefore makes the
verbatim `Date dateValue` mean `java.util.Date`, and `PreparedStatement.setDate(int, java.sql.Date)` does not
accept it. Only one of the two `Date`s can own that simple name in the frame; nothing currently arbitrates.

**What was tried and reverted** (deliberately not committed — it did not work and 54 lines of unverified
machinery is worse than none):

- `Data.jdkImportBySimpleName` — which JDK type owns each simple name, first writer wins.
- `Data.jdkNamedSimplyInSource` — populated in `ensureType` when `ds != null && ds.detail(packageName) ==
  null`, i.e. the pasted text names the type by its simple name, so its spelling is fixed source text and it
  must hold the slot; a type reached only through a reconstructed signature can be printed fully qualified
  instead.
- Tie-break on `originalType.compilationUnit().importStatements()` filtered to `!isStar()`: the original file
  compiled, so whatever it single-imported is what its `Date` meant.

It compiled, passed 46/46 `IsolateMethod` tests, and `import java.util.Date` **still** won.

**The unanswered question, and how to answer it in one run.** Whether `java.sql.Date` is ever a competitor at
all, or whether `java.util.Date` arrives from a reconstructed signature and is simply never challenged.
Dump `jdkTypesToImport` and `jdkNamedSimplyInSource` at the end of `isolate(...)` for this one method
(`-Dtest.split.isolateTop=40` produces it) and the answer is immediate. Bear in mind the `java.sql.*` in the
output is a *collapse* of ≥4 individually collected `java.sql` types — `java.sql.Date` may well be inside it,
in which case it is competing and losing, and the tie-break is what needs fixing rather than the collection.

**Where.** `IsolateMethod.Data.isJdkType` (collection), `IsolateMethod.addJdkImports` (outer class, hands the
set to the `ImportComputer`).

---

## 2. A stub in a namespace chain, referenced both fully-qualified and simply

**Symptom.** `RecordDTO_set_31` is dropped: `Type ParamDouble not found`.

**The conflict, precisely.**

| | where | how it is spelled |
|---|---|---|
| declaration | line 186, indent 20 | nested in the namespace chain `frame → be → closed-core → legacy → parameter` |
| use A — pasted body, ×18 | e.g. `this.chargingSpeedAC = (com.example.legacy.parameter.ParamDistance)value;` | fully qualified |
| use B — reconstructed field | line 7, indent 8, inside the frame-level `RecordDTO` stub | `ParamDouble acceleration;` |

Use A is verbatim source text and **requires** the namespace chain: that is exactly what `namespaceStub` is
for, and placing the type there is correct. Use B is generated by `ensureField` and printed with
`runtime.qualificationSimpleNames()` — which emits the bare simple name, and a type nested five levels deep
in a sibling branch is not in scope there.

So both spellings must reach one declaration, and Java gives no way to declare a type twice. **The fix is in
printing, not in placement.** A reconstructed signature that refers to a namespace-nested stub has to be
printed qualified.

**Precedent to build on.** `IsolateMethod.print(Result, methodString)` already varies qualification per
element: it passes `runtime.qualificationQualifyFromPrimaryType()` for the methods of the `@Override`
supertype while everything else uses `qualificationSimpleNames()`. The same per-element hook takes field and
type printers (`runtime::newFieldPrinter`, `runtime::newTypePrinter`), so the shape of the fix already exists.

Two candidate designs, in the order I would try them:

1. **Per-element**: give fields and stub-method signatures a printer that qualifies from the primary type,
   but only when the referenced stub is inside a namespace chain (`Data.namespaceMap` knows which those are).
   Narrow; should leave every existing expected-output test untouched.
2. **A custom `Qualification`** that qualifies exactly the namespace-nested types and nothing else. Cleaner
   conceptually, more surface area.

Do not "fix" this by flattening the type into the frame — that breaks the 18 fully-qualified casts, which are
source text and cannot be rewritten.

**Where.** `IsolateMethod.print(Result, String)`, `Data.namespaceStub`, `Data.namespaceMap`,
`Data.ensureType`'s namespace branch (taken when `ds != null && ds.detail(packageName) != null`, or on a
simple-name collision).

---

## 3. `MethodPrinterImpl` NPE on a bare `return;`

**Symptom.** One method cannot be isolated at all, so the corpus is 39 files for 40 requested:

```
isolation FAILED for com.example.legacy.datasource.legacyDataSourceGenerator
    .generateDataSourceXMLForPanel(com.example.core.parameter.CustomToken,String)
NullPointerException at MethodPrinterImpl.print:124
  | Cannot invoke "org.e2immu.language.cst.api.statement.ReturnStatement.expression()"
    because the return value of "..." is null
```

This is in **`maddi-cst-print`**, not in `IsolateMethod`, and it fires while printing — the frame is built
fine. A value-less `return;` yields a null expression that line 124 dereferences.

Cheapest reproduction is a unit test in `maddi-cst-print` over a method whose body ends in a bare `return;`,
rather than going through the corpus. Note the sibling defect that was already fixed downstream for the same
shape: `AddMethod.collectExitPointEdits` in jfocus-refactor-service used to NPE on a value-less return
(recorded in `TestSplitBiggestMethods.KNOWN_FAILURES`, now empty).

---

## State, so a later run can tell drift from regression

Baseline of 2026-07-28, all measured on this machine:

| | |
|---|---|
| isolates written / requested | 39 / 40 (issue 3 blocks one) |
| parse back cleanly | **37** (issues 1 and 2 block two) |
| parse errors in the corpus | 0 |
| split attempts / applied | 9,573 / 8,863 |
| logged-not-thrown splitter errors | 0 |
| splits written and re-parsed | 36, **0 parse errors** |
| `IsolateMethod` unit tests | 46, 0 failures |
| `splitmethod` package | 74 tests, 0 failures, at default heap |

One splitter bug is knowingly tolerated, in `TestSplitIsolatedMethods.KNOWN_FAILURES` with its reproducer:
`AddMethod.returnStatementAtEnd:335` asserts `recordType != null` on exactly the path taken when a record IS
needed to carry several values out. Reproduce with `-Dtest.split.only=Factory_parse_29
-Dtest.split.perMethod=0`, candidate 13. That entry comes out when the record-type decision is fixed.

## A note for IsolateClass

Ten of the eleven fixes landed in the reusable core — `Data` (`ensureType`, `ensureTypes`,
`ensureMethodInfo`, `ensureField`, `addDummyInterfaceMethods`) and `MyVisitor` — so they carry over. The
`OwnedMethod` dedup key matters more for a class isolate, where many kept methods reach the same declared
method through different receivers. The only method-shaped code is `isolate(MethodInfo, String)`: frame
naming, mapping the declaring type's type parameters onto the frame, the single `_method_to_be_replaced_`
marker, and the `@Override` supertype. That is the seam to generalise — seed `visit` per kept member, and
grow the printer to a marker per method.

Issue 2 will get *worse* for `IsolateClass`, not better: a class isolate reconstructs many more signatures,
so many more references print with simple names. Worth fixing before that work starts.
