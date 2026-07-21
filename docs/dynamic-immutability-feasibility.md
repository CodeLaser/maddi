# Dynamic immutability: can the consumption path be closed? (spike, 2026-07)

**Status: spike. No candidate change is proposed here; the engine is left untouched.**

Sizing exercise for a three-part feature. Companion notes: `independent-type-optimism.md` (the optimism
defect, which turns out to be coupled to this), `sam-linking-reconciliation.md`.

## Why anyone cares

Dependence is the single root behind every missing verdict in maddi's own CST. An accessor that hands out
its field makes the type `@Dependent`; `@Dependent` caps it at FINAL_FIELDS at the independence gate; and
FINAL_FIELDS counts as mutable (`ImmutableImpl.isMutable()` is `value <= 1`), so the hierarchy rule then
forces every implementation to MUTABLE. `TypeInspectionImpl` has 16 `List.copyOf`/`Set.copyOf` fields with
plain `return field` accessors and gets nothing.

`TestDynamicImmutableReturn` measured the six shapes. The engine already understands a defensive copy in a
method body (`return List.copyOf(items)` ⇒ accessor `@Independent`, no annotation needed). What is missing is
carrying an object's dynamic immutability **across a field assignment**.

## The three parts

| # | part | shape of the work |
|---|---|---|
| 1 | materialize contracts for *source* elements | mechanical |
| 2 | infer the value | inter-procedural — see below |
| 3 | consume it where dependence is decided | the delicate one; **this spike** |

Part 2 is not a local pattern-match. In the CST the copy happens at the **call site**:

```java
// TypeInspectionImpl.Builder.commit()
new TypeInspectionImpl(..., List.copyOf(fields), ...);
// the product constructor
this.fields = fields;                 // plain assignment
```

A "field assigned `List.copyOf(...)`" detector fires on **nothing** in `TypeInspectionImpl`. Concluding the
field holds immutable content requires propagating dynamic immutability from call-site argument → constructor
parameter → field, over every caller of that constructor.

Which is exactly why part 3 was spiked first: it is pointless to build part 2 for a value nothing consumes.

## Where the dependence verdict is decided

`FieldAnalyzerImpl.computeIndependent(FieldInfo, Links)`
(`maddi-modification-analyzer/.../analyzer/impl/FieldAnalyzerImpl.java`, the method beginning at the
`independentOfType` assignment).

The chain, concretely:

1. `computeLinkedVariables` collects the field's links from every method referring to it.
2. `computeIndependent` grades those links. Its first act is
   `analysisHelper.typeIndependentFromImmutableOrNull(owner, fieldInfo.type())` — independence read off the
   field's **declared type**. `AnalysisHelper` (line ~174) maps mutable → DEPENDENT, immutable-HC →
   INDEPENDENT_HC, immutable → INDEPENDENT. For a `List<String>` field this is DEPENDENT regardless of what
   was assigned to it.
3. A direct link (`link.from() == links.primary()`, identical-to/assigned-from) is graded with that same
   `independentOfType`, so `return items;` transmits DEPENDENT.
4. Result written to `INDEPENDENT_FIELD`.
5. `TypeIndependentAnalyzerImpl.loopOverFieldsAndAbstractMethods` mins over `INDEPENDENT_FIELD` of the fields
   and `INDEPENDENT_METHOD` of the **abstract** methods → `INDEPENDENT_TYPE`.
6. `TypeImmutableAnalyzerImpl.computeImmutableType` returns FINAL_FIELDS at `if (independent.isDependent())`.

Note step 5: a *concrete* accessor's dependence never caps its own type. Only fields and abstract methods are
graded. This matters below.

## The spike change

One read, at step 2: consult `IMMUTABLE_FIELD` (which exists, is documented, and has **zero readers**
anywhere in the engine) in preference to the declared type.

```java
Value.Immutable dynamicImmutable = fieldInfo.analysis().getOrNull(PropertyImpl.IMMUTABLE_FIELD, ...);
Value.Independent independentOfType = dynamicImmutable != null
        ? dynamicImmutable.toCorrespondingIndependent()
        : analysisHelper.typeIndependentFromImmutableOrNull(owner, fieldInfo.type());
```

The value is **injected** by the test, not inferred — that is part 2's job, and the point of the spike is to
answer the consumption question without it.

## What it moved

`TestSpikeImmutableField`, shape D (copy in, plain accessor — `TypeInspectionImpl`'s shape):

| | without spike | with spike |
|---|---|---|
| `INDEPENDENT_FIELD` | `@Dependent` | **`@Independent(hc=true)`** |
| `INDEPENDENT_TYPE` | `@Dependent` | **`@Independent(hc=true)`** |
| `IMMUTABLE_TYPE` | `@FinalFields` | **`@Immutable(hc=true)`** |
| accessor `INDEPENDENT_METHOD` | `@Dependent` | `@Dependent` (unchanged) |

So **the consumption path can be closed**, and cheaply: one read in one place takes the real-world shape from
`@FinalFields` to `@Immutable(hc=true)`.

## The finding that matters more: parts 3 and the optimism defect are coupled

The interface variant (`I` declaring `items()`, `Impl` implementing it) is the shape the CST actually has.
With the spike, `Impl` lifts to independent-HC/immutable-HC. But measuring the same fixture with the spike
**reverted** shows `I` is *already* `@Independent` / `@Immutable(hc=true)` while its own abstract `items()` is
`@Dependent` — that is `independent-type-optimism.md`'s defect, not an effect of this change.

Here that defect happens to **help**: an interface honestly graded `@FinalFields` would cap `Impl` at MUTABLE
through the hierarchy rule. So:

> A production version of dynamic-immutability tracking must also lift the **abstract accessor's**
> `INDEPENDENT_METHOD`. A field-only change works today only because the interface is optimistically
> mis-graded; fixing the optimism defect would immediately re-break this shape — which is maddi's CST shape.

That coupling is not visible from either investigation alone, and it means these two pieces of work have to be
planned together rather than sequenced independently.

## Corpus A/B

Baseline (HEAD, `-Dtestarchive.root=...`): 13 tests / 2 skipped / 0 failed; `TestShadowCloneBench` pins **215**
`unmodifiedParameter` divergences; `TestCloneBench` analyzes **9306** types.

With the spike, forced re-run (`--rerun-tasks`): **identical on every number.** 13 tests / 2 skipped / 0
failed; `TestCloneBench` 9306 types; `TestShadowCloneBench`

```
224 divergences {nonModifyingMethod=1, unmodifiedField=8, unmodifiedParameter=215} {propagated=12, seed=212},
0 reverse, in 133 of 9306 types
```

all pinned assertions passing. This is the expected result and worth stating plainly: the spike is **inert on
real code**, because nothing writes `IMMUTABLE_FIELD` — the read simply never fires. It confirms the change
introduces no unintended read path; it is *not* evidence that the feature is safe, which can only be measured
once part 2 populates the property.

(A caution for whoever repeats this: counting `unmodifiedParameter` occurrences in the JUnit XML gives 216,
not 215 — the stdout stream is duplicated in the report and the summary line mentions the property once more.
Read the `SHADOWBENCH totals:` line, or just trust the pinned assertions.)

## Size estimate

- **Part 1, materialize contracts (small).** Same shape as the fix the eventual analyzer already applies to
  `EVENTUAL_METHOD`: write the contracted value into `analysis()` for source elements, which
  `ShallowMethodAnalyzer`/`ShallowTypeAnalyzer` only do for non-source ones. Useful on its own and independent
  of the rest.
- **Part 2, inference (large, and the real cost).** Inter-procedural: dynamic immutability has to flow from a
  call-site argument through a constructor parameter to a field, over all callers. Needs a parameter-level
  notion of dynamic immutability that does not exist, and is the same class of problem as static-value
  propagation. This is the part that decides whether the feature is a week or a quarter.
- **Part 3, consumption (small in the field, unknown at the abstract accessor).** The field read above is one
  line. Lifting the abstract accessor's `INDEPENDENT_METHOD` is not sized here, and it is entangled with the
  optimism defect and with the 215 pinned shadow divergences; that is where the risk sits, not in part 3's
  field half.

**Recommendation.** The consumption question is answered: yes, and cheaply. Do not start with part 2. The next
decision is whether to take on abstract-accessor independence together with the optimism fix as one piece of
work, because the spike shows they cannot be separated for the shape we care about.
