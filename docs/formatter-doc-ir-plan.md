# Rewrite plan: a Doc IR for maddi-cst-print

A concrete plan to replace the token-stream + greedy renderer (`Formatter2Impl` / `BlockPrinter`)
with a Wadler/Leijen–Prettier style **document IR** (`Doc`). Written 2026-07-11, grounded in the
bug fixes F1–F8 (see `formatter-analysis.md`) — most of which are direct taxes on the current
model's loss of separator/structure information.

## 1. Goal and non-goals

**Goal.** Replace the rendering layer with an explicit `Doc` IR whose separators and grouping are
first-class, and a single `group`-fits renderer that subsumes both CHOP_DOWN and GREEDY_FILL.

**Retire, by construction, these bug classes:**
- F7 (blank line inside a wrapped chain) and F8 (dropped separator `&&(`, `||len`, `@A@B`) — both
  come from inferring a *join* separator from one block's trailing/leading state. In a Doc, the
  separator between two elements is a node you write down; it can't be mis-inferred.
- F6 (deep-nesting shatter, patched with `MIN_CONTENT_WIDTH`) — a `group` that cannot fit simply
  breaks; degenerate width degrades gracefully with no floor hack.
- The comment/newline `//` and `spaceLevel==NEWLINE` hacks — a line comment is `Text ++ HardLine`;
  the hardline forces the enclosing group to break. No sniffing.

**Non-goals (initially).** Not rewriting the ~60 `*PrinterImpl` producers; not changing the public
`Formatter`/`OutputContext` interfaces; not changing the `FormattingOptions` surface. The producers
keep emitting today's `OutputBuilder` stream; we insert a **lowering** step (§4) that turns that
stream into `Doc`. Migrating producers to emit `Doc` directly is a later, optional phase.

## 2. The Doc IR

Immutable records in a new package `org.e2immu.language.cst.print.doc`. Node set (minimal but
complete for Java/Kotlin output):

| Node | Flat rendering | Broken rendering | Use |
|---|---|---|---|
| `Text(s)` | `s` (no newlines) | same | atomic token |
| `Concat(docs)` | concatenation | concatenation | sequence |
| `Line` | one space | newline + indent | ordinary soft break (`a, b`) |
| `SoftLine` | nothing | newline + indent | glue break (`).foo()`, `>,`) |
| `HardLine` | — (forces break) | newline + indent | statement sep, line-comment end |
| `BlankLine` | — (forces break) | newline+newline+indent | preserved blank line / breathing space |
| `LiteralLine` | — (forces break) | newline, **no reindent** | inside text blocks / multiline comments |
| `Group(doc)` | doc flat **iff it fits** | doc broken | the fundamental fits/break unit |
| `Fill(docs)` | greedy: pack until overflow | per-item | GREEDY_FILL semantics |
| `Indent(doc)` | doc | doc at indent+tab | nesting |
| `IfBreak(brk, flat)` | `flat` | `brk` | trailing comma, operator placement |
| `Trim` | removes trailing spaces on current line | same | safety: never emit trailing ws |

Notes:
- **Spacing is gone as a lattice.** The whole `SpaceLevel {EMPTY,NO_SPACE,SPACE_IS_NICE,SPACE,
  STRONG_NO_SPACE,NEWLINE}` + `max()` machinery is replaced by choosing, at lowering time, between
  `Text(" ")` / `Line` / `SoftLine` / direct concat. `STRONG_NO_SPACE` (`>,`) becomes `SoftLine`
  or plain concat; `isAt`/`strongNoSpace` special cases disappear into the lowering.
- **CHOP_DOWN vs GREEDY_FILL stop being two code paths.** `Group` = "flat if it fits, else break
  every `Line`" = CHOP_DOWN. `Fill` = "pack per line" = GREEDY_FILL. One renderer, two node types.
- `BlankLine` lets us decide the "breathing space" policy explicitly (see §6).

## 3. The renderer

Standard Wadler/Leijen `best` with Prettier's bounded `fits`:

```
render(width, docs):                      // docs: stack of (indent, mode, Doc)
  col = 0
  while docs not empty:
    (i, mode, d) = pop
    switch d:
      Text(s):            out(s); col += s.length
      Concat(xs):         push each x with (i, mode) in reverse
      Indent(x):          push (i + tab, mode, x)
      Line/SoftLine/HardLine/BlankLine/LiteralLine:
          if mode == FLAT and d is Line:      out(" "); col += 1
          elif mode == FLAT and d is SoftLine: /* nothing */
          else: out(newline + indent(i, d));  col = i     // HardLine forces this even in FLAT
      Group(x):           push (i, fits(width-col, FLAT, x, rest) ? FLAT : BREAK, x)
      Fill(xs):           greedy fill (measure each item flat; break before the first that overflows)
      IfBreak(b,f):       push (i, mode, mode==BREAK ? b : f)
```

`fits(remaining, mode, doc, rest)` lays out `doc` (then `rest`) flat and returns false as soon as it
exceeds `remaining` **or** hits a `HardLine`. Capped at `remaining` characters of lookahead → O(n)
overall (the current `updateForSplit` remapping is worse than linear, so this is also a perf win).

Invariants become total, not `assert`: `Trim`/end-of-line handling guarantees no trailing space in
**production** (today's asserts are off with `-ea` disabled).

## 4. Lowering: `OutputBuilder` stream → `Doc` (the bridge)

This is where today's complexity concentrates, and the one genuinely hard module. It consumes the
same flat `OutputElement` list Formatter2 consumes, and reuses the existing guide re-parse
(`collectElements`/`parseBlock`) — but emits `Doc` instead of `Block`. Because the stream already
carries guide structure and per-symbol L/R affinities, the lowering is **faithful**: anything the
current formatter expresses, it can express — while fixing the bug class, because each inter-token
separator becomes an explicit node instead of a reconstructed trailing `spaceLevel`.

Mapping:

| Stream element | Doc |
|---|---|
| `Text` / `Symbol.symbol()` | `Text(...)` |
| `Symbol` L/R space `NONE` | direct concat (no separator) |
| `Symbol` L/R space `ONE`/`ONE_IS_NICE` | `Line` (or `Text(" ")` if non-breakable) |
| `Space.NEWLINE` | `HardLine` |
| `Space` with split rank | `Line` inside the enclosing `Group` |
| Guide `start..mid..end`, `generatorForBlock` | `Group(Indent(HardLine-joined segments))` wrapped `{`…`}` (priority ⇒ hardlines) |
| Guide (default: chains, arg lists) | `Group(Indent(join segments with `Line`/`SoftLine`))` |
| Guide (params) | `Group(Indent(join with `Line`))` |
| single-line comment (`//`, text, NEWLINE) | `Concat(Text("//"+body), HardLine)` |
| block comment `/* … */` | `Text` with embedded `LiteralLine`s |
| text block | `Text` head + `LiteralLine`s (reuse `WriteTextBlock` logic) |
| method chain (`.mid()` + `DOT`) | `Group(Indent(Concat(obj, SoftLine, ".", seg, SoftLine, ".", seg…)))` |

## 5. Options mapping

| FormattingOptions | Doc-world meaning |
|---|---|
| `lengthOfLine` | width passed to `render` |
| `spacesInTab` | indent step in `Indent` |
| `compact` | render nice `Line`s as flat (drop optional spaces); prefer FLAT |
| `binaryOperatorsAtEndOfLine` | operator before `Line` vs after — an `IfBreak`/lowering choice |
| `wrapStyle CHOP_DOWN` | lower guide lists to `Group` |
| `wrapStyle GREEDY_FILL` | lower guide lists to `Fill` |
| `alwaysBreakPriorityBlocks` | block guide lowers to hardlines instead of `Group` (already the F2 behavior) |
| `skipComments` | drop comment nodes at lowering (as today in `Util.removeComments`) |
| `tabsForLineSplit` | continuation-indent depth in `Indent` |

## 6. Policy decisions to make explicit (behavior changes)

These are places where the current output is quirky and the rewrite forces a deliberate choice:

1. **Breathing-space blank lines.** Today `DOUBLE_NEWLINE` synthesizes a blank line "when both
   neighbours wrapped" (the F7 heuristic). The conventional approach is to **preserve blank lines
   from source** (the parser knows them) via `BlankLine`, not synthesize them. Recommend: preserve
   source blanks; drop the synthesized heuristic. This changes output.
2. **Compact-if-it-fits vs conventional.** Today a body inlines when it fits (`class X {…} ` on one
   line). `alwaysBreakPriorityBlocks` already exists to opt into conventional. Recommend making
   conventional the default in the rewrite (block guides ⇒ hardlines) and keeping compact as an
   option — but this is the single biggest output diff, so decide up front.
3. **`if(` vs `if (`** and similar are producer choices, unchanged.

## 7. Migration phases

- **Phase 0 — IR + renderer, standalone.** New `doc` package: node records + `render` + `fits`.
  Pure, no producer dependency. Property tests: idempotence (`render∘parse` stable), never trailing
  space, `Group` fits-monotonicity, `Fill` packing, hardline forces break. ~1–2 days.
- **Phase 1 — lowering.** `OutputBuilder → Doc`, reusing the guide re-parse. The hard part; this is
  where §4/§6 get encoded. Unit-test with the existing hand-built `OutputBuilder` fixtures
  (`Test3`–`Test7`, `TestBlockPrinter*`). ~3–5 days.
- **Phase 2 — new Formatter behind the interface.** `DocFormatterImpl implements Formatter` =
  `render(lower(ob), options)`. Wire via a flag in `OutputContextImpl`; keep `Formatter2Impl`
  alongside for differential testing. ~0.5 day.
- **Phase 3 — differential harness + triage.** Run BOTH formatters over the whole corpus (all
  round-trip tests, `TestCloneBench`, JDK sources) and diff. Every diff is triaged: improvement
  (glued→spaced, no blank-in-chain, graceful deep nesting) vs regression. Tune the lowering to
  minimize accidental diffs; adopt intended improvements by updating goldens in one reviewed pass.
  This is the bulk of the calendar time and the main risk sink. ~1–2 weeks.
- **Phase 4 — flip + cleanup.** Make Doc the default, delete `Formatter2Impl`/`BlockPrinter`/`Line`,
  rename the package (kills the `formatter2` / dead `runtime` debt, F5). ~1 day.

## 8. Risks and mitigations

- **Large golden churn.** Hundreds of frozen `assertEquals(INPUT, reprint)` tests encode current
  output. Mitigation: Phase 3 differential harness; land the golden update as one reviewed commit;
  keep the flag so a regression can be bisected against the old formatter.
- **Faithfulness of lowering.** The stream must carry enough to reproduce intent — it does (it's
  exactly what Formatter2 reads today). The keep-both differential test is the safety net.
- **Two producers of blank lines** (synthesized vs source-preserved) — decided in §6.1 before Phase
  1, not discovered in Phase 3.
- **Performance** — `fits` must be bounded (stop at width / first hardline). Expected net win.
- **Scope creep into producers.** Resist; lowering keeps producers untouched. Migrating them to emit
  `Doc` directly (eliminating the flat stream entirely — improvement #2 from the analysis) is a
  separate, later, incremental effort once Doc is the default.

## 9. What exists at the end

`doc/` package: `Doc` nodes (~10 records, ~150 LOC), `DocRenderer` (~200 LOC), `Lowering`
(~350 LOC), `DocFormatterImpl` (~30 LOC). Deleted: `BlockPrinter` (368), `Line` (187),
`ElementPrinter` (188), `Formatter2Impl` block machinery. Net roughly LOC-neutral but with the
separator/grouping model made explicit — and the F6/F7/F8 bug family structurally impossible.

## 10. Suggested first step

Implement **Phase 0** (IR + renderer + property tests) in isolation — it's self-contained, low
risk, and immediately demonstrates the fits/group behavior on hand-built `Doc`s before any
lowering or producer contact. If it feels right, proceed to the Phase 1 lowering.
