# Handoff: link-computer recursion hits statements without VariableData; downstream, 208 methods lost

**Written 2026-08-02.** Found chasing the dominant standardize-failure bucket of the jfocus dedup M2 run on
closed-core. Status: **open**; this is the root of a three-layer collateral chain, and the fix belongs here in
the link engine (or in the guarantee its callers rely on).

## The chain, outermost symptom first

1. **jfocus M2 recall stuck at 66%**: 208 of 225 standardize failures are
   `AssertionError: Assigning to local variable, but not yet defined: <lv>` at
   `codelaser-stdbase-parser …/BlockData.findForAssignment` — the variable names are the query-condition
   locals (`condition`, `conditionItem`, `item`, …). A separate, genuine stdbase defect with the same
   surface (pop-time reassignment misclassification under escape-evolved block conditions) was fixed and
   pinned on 2026-08-02 (`TestQueryConditionRebinding`, jfocus-stdbase commit "freeze the declaring block's
   condition at reassignment registration") — the closed-core bucket did not move: same count, same examples.
2. **The parse assertion is collateral**: the declaration of the failing local was never registered, because
   the maddi analysis of the method is incomplete — prepwork/link data missing on its statements.
3. **The root**: during the intake's analysis, `LinkComputerImpl` aborts these methods:
   ```
   ERROR LinkComputerImpl -- Caught exception in statement 2@1028:9-1028:85 of
       com.example.app.iface.items.ItemService.checkDuplicatesForItems(Try.TryData): null
   ERROR LinkComputerImpl -- Caught exception recursively computing …checkDuplicatesForItems(…)
   ERROR LinkComputerImpl -- Caught exception computing …checkDuplicates(String)
   java.lang.AssertionError
       at LinkComputerImpl$SourceMethodComputer.doStatement(LinkComputerImpl.java:711)   ← assert vd != null
       at LinkComputerImpl$SourceMethodComputer.doBlock(:616)
       at LinkComputerImpl$SourceMethodComputer.go(:438)
       at LinkComputerImpl.doMethod(:264) / doMethod(:215)
       at ExpressionVisitor.lambda$recurseIntoLinkComputer$1(:801)
       at PropertyValueMapImpl.getOrCreate(:92)
       at ExpressionVisitor.recurseIntoLinkComputer(:801)
       at ExpressionVisitor.constructorCall(:505)
       at ExpressionVisitor.visit(:67)
       at LinkComputerImpl$SourceMethodComputer.handleSingleLvc(:1052)
       …
   ```
   `assert vd != null` at `doStatement:711`: the link computer recursed into a source method whose
   statements carry **no VariableData**.

## Context dependence (why it looked mysterious)

The same method analyzes cleanly or fatally depending on **which other types are in the parsed slice**:

| slice | result for `checkDuplicates` |
|---|---|
| `com.example.app.iface.items` only (7 types; `SqlUtil` from bytecode) | clean |
| + `com.example.core.general.util` (SqlUtil et al. as **source**) | LinkComputer aborts (stack above) |
| the 357-type fill-type slice | 208 methods, same family |

With the callee in bytecode there is nothing to recurse into; with it in source, the recursion happens and
hits the vd-less statement. The jfocus intake (`ProjectIntake`, codelaser-standardize-deduplication) already
runs a **two-phase analysis** — prepwork over ALL types before any linking — precisely to guarantee the
recursion always finds prep-analyzed statements, and `TestIntakeAttrition.crossTypeStaticCall` pins the
naive ordering case as fixed. Some path escapes that guarantee. Note the recursion target here is a
**Try-transform-generated sibling** (`…Resources(Try.TryData)`) reached through `constructorCall` →
`recurseIntoLinkComputer` (a `this::checkResources`-style reference inside
`new Try.TryDataImpl.Builder()…`), which may matter: these synthetic methods and their compilation units are
products of the jfocus transform's re-parse, not of the original source scan.

## Reproduction (fast, machine-local)

- 3-minute live lab: `gradle :codelaser-standardize-deduplication:closedCoreSweep -Dtest.closedcore.sweep=false
  -Dtest.closedcore.intake=1 -Dtest.closedcore.dedup=1
  "-Dtest.closedcore.packages=com.example.app.iface.items,com.example.core.general.util"
  -Dtest.closedcore.stacks=3` (jfocus-standardize; needs the closed-core config, see `ClosedCore`). The items-only
  variant is the green control.
- No small self-contained repro yet: the shape needs a caller + a source callee reached via a functional
  reference inside a builder-style constructor chain, where the recursion target's statements lack
  VariableData. `TestIntakeAttrition` (same module) shows the harness pattern to distill into.

## Impact and priority

Each aborted method loses its analysis, and downstream every one of them is a standardize failure — on the
`ConditionItem` flagship family this is **the** remaining recall blocker (66.4% vs the 90% M2 gate;
intake tail is the only other loss of size). Fix directions, in preference order: make the recursion
lazily prep the target (or skip-and-degrade EXPLICITLY, marking the summary degraded rather than throwing);
or extend the callers' two-phase guarantee to cover transform-generated siblings; or catch the assert at the
recursion boundary and continue with a degraded callee summary — never a silent abort of the whole caller.
