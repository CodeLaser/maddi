# sv engine — differential worklist (194 assertion failures)

Baseline: `sv-integration` big-bang, `maddi-modification-link` at 196/383 failing
(15 skipped). This groups the **194 assertion diffs** by *what the new engine gets
wrong* versus the old fixpoint, so the engine phase has a prioritized to-do.

Method: for each failing `expected: <…> but was: <…>`, tokenize both link sets and
diff them. **Dropped** = in expected, missing from engine output (engine
under-produces). **Spurious** = engine emitted, not expected (engine over-produces).

> Caveat: a `?` bucket (no nature symbol) is mostly non-link tokens — method
> summaries like `C<init>(0)` and modification-set entries — i.e. tokenizer noise,
> not real link differences. Ignore it; focus on the nature-bearing rows.

## Headline: the engine drops assignment & identity links

**Dropped (engine MISSING), by nature:**

| count | nature | meaning |
|------:|:--|:--|
| **177** | `←` | **is-assigned-from — by far the dominant failure** |
| 31 | `→` | is-assigned-to |
| 27 | `≡` | identical-to (incl. §m mutation links) |
| 26 | `∈` | is-element-of |
| 16 | `⊆` | is-subset-of |
| 11 | `∩` | object-graph-overlaps |
| 8 | `~` | shares-elements |
| 8 | `∋` | contains-as-member |
| 7 | `≺` | is-field-of |
| 6 | `≥` | object-graph-contains |
| 1 | `⊇` | is-superset-of |

**Spurious (engine EXTRA), by nature:** `∋` 13, `≻` 13, `≺` 10, `∩` 6, `∈` 6, `⊆` 5,
`~` 4, `≡` 3, `←` 1.

**Read:** the engine **loses `←`/`→`/`≡`** (the assignment-flow and identity edges)
and, where it does emit something, tends to substitute **field-of / contains
(`≺`/`≻`/`∋`)**. That is consistent with the label algebra's `combine`/`best`
demoting or discarding the high-value assignment/identity edges during closure and
reduction — exactly the "prefer high-value direct edges (`←`+`∈`)" invariant the
engine's own doc says it must preserve but evidently doesn't yet.

**Structural:** ~113 of the 194 are near-total drops — the engine emits **nothing**
(or only `-`) where the old fixpoint had a full link set. So this is not fine
mis-labeling at the margins; whole link sets are collapsing.

## Prioritized attack plan

1. **`←` assignment-flow drop (177) — do this first.** One root cause almost
   certainly explains the bulk. Likely in the closure/reduction: assignment edges
   (`IS_ASSIGNED_FROM`/`_TO`, score = top) are being combined-away or reduced-away
   instead of preserved. Sharpest probes (produce *nothing*):
   - `TestSharedVariable :: direct assignment`
   - `TestGetSet :: modification in @Fluent setter`, `:: getS←this.s`
   - `TestList :: Analyze direct link from constructor parameter`
   Fix and re-measure — expect a large cascade.
2. **`≡` identity / §m drop (27)** — the mutation-identity edges. Probes:
   `TestCast :: links of cast` (`§m≡`), `TestRedundantModificationLinks :: simple chain`.
3. **`∈`/`⊆`/`~` element & subset drops (50 total)** — the stream/collection HC flow.
   Probes: `TestList :: 'sub'` (`⊆`), `TestMap` (`~`,`∩`,`≥`), `TestStreamMapSpec`.
4. **Spurious `≺`/`≻`/`∋` (36)** — usually the *flip side* of 1–3: once the correct
   `←`/`∈`/`~` survive closure, these field-of/contains substitutions should vanish.
   Re-check after 1–3 rather than chasing separately.
5. **`SharedVariable.acceptForLinkedVariables` (2 crashes)** — implement the
   unfinished path (return the intended boolean rather than throwing).
6. **Re-enable the `@Disabled impl/large` stress tests** once the mocks are made
   javac-compliant and 1–4 land — they are the engine-stability regression net.

## Highest-value test classes (dropped-link count — use as the differential targets)

| drops | class |
|------:|:--|
| 83 | `io.TestWriteAnalysis2` |
| 60 | `impl.TestLinkMethodCall` |
| 38 | `staticvalues.TestStaticValuesRecord` |
| 32 | `typelink.TestLanguageConstructs` |
| 24 | `typelink.TestSupplier` |
| 19 | `typelink.TestSupplierSpec` |
| 11 | `typelink.TestList`, `typelink.TestStreamMapSpec` |
| 10 | `impl.TestLinkModificationArea`, `typelink.TestStaticBiFunction` |

Start with `TestLinkMethodCall` / `TestGetSet` / `TestList` for the `←` root cause
(small, direct, assignment-centric), then widen to the stream/map classes.

## How to work it
- Keep the differential harness: `./gradlew :maddi-modification-link:test`, diff
  expected-vs-got per class.
- After each engine fix, re-run this analysis to watch the nature histogram shrink —
  the `←` row collapsing is the success signal.
- Each resolved diff is a real engine fix (correct link now produced), **not** a
  re-baseline — the old fixpoint's output is the oracle.
