# maddi-cst-print formatter analysis

Analysis of the `Formatter2Impl` pretty-printer (module **maddi-cst-print**), with an eye to
improving output quality. Written 2026-07-11.

## 1. What this module is

`maddi-cst-print` turns a `CompilationUnit` (or any CST element) into formatted source text. It
does **not** parse; it consumes an `OutputBuilder` — a flat list of `OutputElement`s
(`Text`, `Symbol`, `Space`, `Guide`) produced by the CST `*PrinterImpl` classes in
`maddi-cst-impl` (`TypePrinterImpl`, `MethodPrinterImpl`, statement/expression printers, …).

The module is ~1,160 LOC of Java, no Kotlin. Five classes carry the logic:

| Class | Role |
|---|---|
| `Formatter2Impl` | Entry point. Flattens the element list, builds a recursive `Block` tree. |
| `BlockPrinter` | Renders a block; decides per-guide whether to keep inline or split. Core logic. |
| `ElementPrinter` | Renders leaf elements (`Text`/`Symbol`/`Space`); manages spacing + split points. |
| `Line` | The width budget: current column, indent, `available()`, `carryOutSplit`, `ensureSpace`. |
| `WriteTextBlock` / `Util` | Text-block rendering; comment stripping helpers. |

### Pipeline

```
OutputBuilder (flat list)
   │  Formatter2Impl.collectElements / parseBlock
   ▼
Block tree   (Guide start/mid/end markers delimit nested blocks; tab = indent depth)
   │  Block.write → BlockPrinter.write
   ▼
per block: handleGuideBlock (mark split candidates) or handleElements (render leaves)
   │  Line budget decides inline vs. split; two paths: CHOP_DOWN / GREEDY_FILL
   ▼
String
```

## 2. Key design facts (verified)

- **Production is fixed to `CHOP_DOWN` at line length 120.** Every production call site builds
  options with a bare `new FormattingOptionsImpl.Builder().build()`
  (`maddi-inspection-integration/.../JavaInspectorImpl.java:793`,
  `maddi-inspection-openjdk/.../JavaInspectorImpl.java:95`,
  `maddi-modification-common/.../IsolateMethod.java:87`). Default `wrapStyle = CHOP_DOWN`
  (`FormattingOptionsImpl.java:51`).
- **`GREEDY_FILL` exists but is never selected in `src/main`.** It is only exercised by the
  synthetic unit tests in `TestGreedyFill`. It has never seen a real parse→print round-trip.
- **Blocks stay inline whenever they fit** within the remaining line budget. This is the
  formatter's defining behavior and produces the project's characteristic *compact* output.

## 3. Output quality: the central tension

The formatter deliberately produces **compact** output. Real round-trip expectation
`TestVariousPrint2Issues.OUTPUT3` bakes this in:

```java
class AgentLauncher {private String method(File file) { return String.format("%s",file.getName()); } }
```

An entire class + method body on one line, because it fits in 120 columns. This collapsed-body
style appears in **~44 test files**, so it is an *accepted, deliberate* style — not a bug.

### The dead `prioritySplit` machinery

`generatorForBlock()` (`GuideImpl.java:58`) creates block guides with
`prioritySplit = startWithNewLine = endWithNewLine = true` — flags whose entire purpose is to
force a block body onto its own indented lines. But **`BlockPrinter` ignores them**: it consults
`sub.guide().endWithNewLine()` exactly once (`BlockPrinter.java:216`, for the closing brace) and
never reads `prioritySplit()`, `startWithNewLine()`, or `allowNewLineBefore()`. The split
decision is purely budget-driven:

```java
// BlockPrinter.handleBlock
if (output.hasBeenSplit || addToLine > line.available()) { split… } else { inline… }
```

So the flags that would yield conventional "brace on its own line" output are effectively dead
code. This is the **single biggest lever** for conventional formatting — but flipping it is a
large, intentional style change (≈44 round-trip tests would need re-baselining), so it belongs
behind an option, not as a default flip.

### CHOP_DOWN explodes lists vertically

When a block *does* overflow, CHOP_DOWN breaks at **every** candidate, one element per line —
even for a short `throws A, B` or a 3-argument call that only slightly overran. Example
(`Test6.test1`, width 120): the signature overflows, so `throws` is left dangling at the end of
a line and each exception drops onto its own line even though `throws A, B` fits easily. This is
"working as designed" for CHOP_DOWN; `GREEDY_FILL` is the intended cure but isn't wired up.

## 4. Findings, prioritized

### F1 — (FIXED) `Builder` copy constructor dropped 4 fields
`FormattingOptionsImpl.Builder(FormattingOptions)` copied only 5 of 9 fields, silently reverting
`compact`, `allFieldsRequireThis`, `allStaticFieldsRequireType`, `skipComments` to defaults.
Currently uncalled → no production impact, but a latent landmine. **Fixed on branch
`formatter-analysis`** + regression test `TestFormattingOptionsBuilder`.

### F2 — (IMPLEMENTED, opt-in) `prioritySplit`/`startWithNewLine` were ignored (style lever)
See §3. Implemented on branch `formatter-analysis` as a new `FormattingOptions`
flag `alwaysBreakPriorityBlocks` (default **off**, so the compact style and all existing tests
are preserved). When on, `BlockPrinter.handleBlock` forces a break for any priority guide block
(class/method/if bodies), giving conventional "brace on its own line" layout. Test
`TestBreakPriorityBlocks` covers both flag states. Blast radius if ever made default: ~44 test files.

### F3 — (BUG FOUND + FIXED) `GREEDY_FILL` dropped the separator space before an inline block
Driving the realistic `Test6.create1` class through `GREEDY_FILL` surfaced a real defect: when
greedy keeps a guide block on the current line (position-0 boundary does not wrap), the separator
space was lost — producing `throwsMalformedURLException` (width 120/60) and `{buff.append(...)`
(width 60). Root cause: `splitOutputOfBlock` never emits the pending space level (only the inline
path in `handleBlock` did), and `greedyFill` deliberately skips the position-0 boundary. **Fixed**
by threading the pending space level into `greedyFill` and emitting it at position 0 when it stays
inline; a wrap there consumes it into the newline, so chop-down and wrapped cases are unaffected.
Regression test `TestGreedySpacing`.

`GREEDY_FILL` is still unproven on real parse→print round-trips (production `print2` hardcodes
default options, so greedy cannot be reached without a signature change). Remaining recommendation:
add round-trip coverage in `maddi-inspection-integration` and decide whether to expose greedy as a
production-selectable option.

### F4 — (FIXED) Fragile string-sniffing for single-line comments
`ElementPrinter.handleNonSpaceNonSymbol` decided whether a forced split keeps the continuation at
comment level by sniffing `line.stringBuilder.substring(0, 2).equals("//")` — which throws
`StringIndexOutOfBounds` when the builder holds <2 chars and is a positional string hack.
**Fixed** with a block-scoped `Line.singleLineComment` flag, set when the `SINGLE_LINE_COMMENT`
symbol is written at the start of a line (mirroring the existing
`isLeftBlockComment`/`isRightBlockComment` `protectSpaces` pattern) and read via
`Line.isSingleLineComment()`. Behaviour-preserving — the flag is true under exactly the condition
the substring check was. Covered by `TestBlockPrinter2.test3c`, `Test7`, and the round-trip
`TestComments`/`TestJavaDoc` suites.

Two related spots in `BlockPrinter` (the `output.spaceLevel().isNewLine()` "NEWLINE of `//`" check
at `handleGuideBlock`, and the SPACE/NEWLINE handling in `handleBlock`) are labelled "is this a
hack?" but are *type-based*, not string sniffs, and carry no crash risk — left as-is intentionally.

### F5 — Naming debt
The `formatter2` package and `Formatter2Impl` were never renamed after the old formatter was
removed (commit `26d8b553`). 18 references across 5 files. Low risk, mechanical, do with an IDE
refactor when convenient.

### F6 — (FIXED) deep nesting on a narrow page went off the rails
For a block at depth `t`, the content budget is `maxAvailable = lengthOfLine - t*spacesInTab`. In
very deep blocks (e.g. 10 nested `if`s) this collapses to ~0 or negative, so **every** candidate
split position "overflows" and the formatter breaks at all of them. Reproduced with the real
parser at widths 40/30/20: `if(` dangling at the end of a line, expressions shattered
one-token-per-line (`p =` / `p +` / `9;`), and spurious blank lines (the `baseSplitLevel`
DOUBLE_NEWLINE "both neighbours wrapped" heuristic firing on every pair). **Fixed** with a
`MIN_CONTENT_WIDTH = 16` floor on the budget in `BlockPrinter.write`: deeply-indented lines may
now exceed `lengthOfLine`, but each statement stays intact instead of degenerating. The floor only
engages once indentation exceeds `lengthOfLine - 16` — shallow blocks (all existing narrow tests)
and all width-120 round trips never reach it. Regression test `TestDeepNestingPrint`
(maddi-java-openjdk, real parser). Follow-up option: make the floor a `FormattingOptions` field
rather than a constant if callers want to tune it.

### F7 — (OPEN) spurious blank line inside a wrapped method chain
When two adjacent sub-blocks both wrap, `baseSplitLevel` promotes their boundary to
`DOUBLE_NEWLINE` (a blank line) *before* it checks whether the boundary is a no-space one. In a
fluent chain whose links each wrap, this inserts a blank line **inside** the chain:
```
r
    .computeIfAbsent(s, k -> new ArrayList<>())

    .add(in.get(i % in.size()));
```
A blank line mid-chain is wrong (though still valid Java — `TestFormatterRoundTripStable` passes).
The obvious fix (check `isNoSpace()` first) is **too blunt**: the same predicate governs a
legitimately-wanted blank line between imports and a comment block (`TestBlockPrinter2.test3c`), so
it regresses that test. A correct fix needs a more precise "this boundary is glue" signal than the
current `spaceLevel`/`hasBeenSplit` interplay exposes — deferred rather than shipped half-understood.
Guard test to keep green when addressing this: `TestBlockPrinter2.test3c`.

### Robustness verification (no defects found)
Two stress guards were added (`TestFormatterStress`, `TestFormatterRoundTripStable`, both real
parser). Across widths 8–160 and both wrap styles: no exceptions, no trailing whitespace, no
whitespace-only lines, and — critically — **no token corruption** (format → re-parse → re-print is
stable). The commented-out "head of split must not be blank" assertion (`Line.java:164`,
FIXME `TestSwitchFor,1`) could not be reproduced with rich switch/chain/generic inputs at any width.

## 5. Strengths (worth preserving)

- The recursive `Block`/`Guide` model is clean and the `minimal()` debug rendering
  (`Formatter2Impl.Block.minimal`) makes block structure directly testable (`TestCollectElements`).
- The `SplitLevel` ladder (`NONE_IF_COMPACT → SINGLE_NEWLINE → FORCED_NEWLINE → DOUBLE_NEWLINE`)
  is a good abstraction for expressing "how hard to break here", well documented in `BlockPrinter`.
- Width accounting in `Line` is careful about first-line-vs-continuation indent.

## 6. Recommended sequence

1. **(done)** F1 safe fix.
2. F2 behind an opt-in flag → lets you *see* conventional output without breaking the compact
   baseline; decide later whether to make it the default.
3. F3 harden GREEDY_FILL with round-trip tests → the cure for F3's vertical explosion.
4. F4 de-hack comment handling → pure robustness, no output change intended.
5. F5 rename when convenient.
