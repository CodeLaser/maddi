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

### F2 — `prioritySplit`/`startWithNewLine` are ignored (style lever)
See §3. Recommendation: honor them behind a new `FormattingOptions` flag (default off, so the
compact style and all existing tests are preserved), giving callers a conventional-layout mode.
Blast radius if made default: ~44 test files.

### F3 — `GREEDY_FILL` is unproven end-to-end
Only synthetic unit tests. Recommendation: add real round-trip coverage and harden the fallback
(`BlockPrinter.java:282` already degrades to chop-down when a sub-block contains newlines) before
offering it as a production option.

### F4 — Fragile string-sniffing hacks (robustness)
The author's own TODOs mark these:
- `ElementPrinter.java:137` — `line.stringBuilder.substring(0, 2).equals("//")` throws
  `StringIndexOutOfBounds` if the builder holds <2 chars and mis-classifies any content that
  merely starts with `//`. It is coupled to a matching hack in `BlockPrinter.java:136-138` and
  `:253`. These drive comment/newline handling and are the most likely source of future
  regressions. Recommendation: carry an explicit "is single-line comment" signal on the element
  instead of sniffing the rendered string.

### F5 — Naming debt
The `formatter2` package and `Formatter2Impl` were never renamed after the old formatter was
removed (commit `26d8b553`). 18 references across 5 files. Low risk, mechanical, do with an IDE
refactor when convenient.

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
