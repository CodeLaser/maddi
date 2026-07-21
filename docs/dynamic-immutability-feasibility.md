# Dynamic immutability: closing the consumption path (2026-07)

**Status: parts 1 and 3 IMPLEMENTED (2026-07), together with a local guard check. Part 2 (inference) was
attempted and NOT built: for maddi's own CST the value it would have to infer is not merely unknown, it is not
guaranteed to be true. See "Part 2: why it was not built" below.**
The measurements below were the spike that sized it; the "what it moved" and coupling sections have been updated
to what the implemented version actually does.

Originally a sizing exercise for a three-part feature. Companion notes: `independent-type-optimism.md` (the optimism
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

## The change, as implemented

Three reads, not one — see below. The first is at step 2: consult `IMMUTABLE_FIELD` (which exists, is documented, and has **zero readers**
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

## The coupling with the optimism defect — found by the spike, CLOSED by the implementation

The interface variant (`I` declaring `items()`, `Impl` implementing it) is the shape the CST actually has.
With the spike, `Impl` lifts to independent-HC/immutable-HC. But measuring the same fixture with the spike
**reverted** shows `I` is *already* `@Independent` / `@Immutable(hc=true)` while its own abstract `items()` is
`@Dependent` — that is `independent-type-optimism.md`'s defect, not an effect of this change.

Here that defect happens to **help**: an interface honestly graded `@FinalFields` would cap `Impl` at MUTABLE
through the hierarchy rule. So:

> A production version of dynamic-immutability tracking must also lift the **abstract accessor's**
> `INDEPENDENT_METHOD`. A field-only change works today only because the interface is optimistically
> mis-graded; fixing the optimism defect would immediately re-break this shape — which is maddi's CST shape.

**The implementation does exactly that, so the coupling is gone.** Three sites grade a field by its declared
type and now all consult `DynamicImmutability`:

| site | what it decides |
|---|---|
| `FieldAnalyzerImpl.computeIndependent` | the field's own `INDEPENDENT_FIELD` |
| `TypeModIndyAnalyzerImpl.handleGetterSetter` | a getter's `INDEPENDENT_METHOD` |
| `LinkToField.immutableOfLinkedField` | a return value's link to a field (and the guard's blame) |

With the getter lifted, `AbstractMethodAnalyzerImpl.methodIndependent` copies the value up to the abstract
accessor, so the interface is graded **honestly** rather than optimistically. Verified by temporarily applying
the optimism fix (`return null` in `computeIndependentType`) and re-measuring the interface fixture:

| | before the optimism fix | with it applied |
|---|---|---|
| `I` | `@Independent` / `@Immutable(hc=true)` | `@Independent(hc=true)` / `@Immutable(hc=true)` |
| `Impl` | `@Independent(hc=true)` / `@Immutable(hc=true)` | `@Independent(hc=true)` / `@Immutable(hc=true)` |

`I` drops to its honest value and nothing breaks. So the two pieces of work no longer have to be sequenced
together — fixing `independent-type-optimism.md` will not re-break this shape.

### Whole object only

The dynamic value is applied only where the whole field object is reached — never on a content tier (`§es`,
`∋`, `∈`, or an indexed getter). `@Immutable(hc=true)` on a field says the *container* cannot change; the
elements are its hidden content. Reading it more broadly would turn a promise about the container into a
promise about everything inside it.

## The guard check

Because the contract now moves verdicts, `GuardAnalyzerImpl.guardDynamicImmutableFields` checks it against
every assignment the type itself makes to the field (initializer, constructors, methods):

- **refutable lie** — a freshly constructed mutable object (`new ArrayList<>(in)`) — is a `contract-violation`
  error;
- **proven** — a call whose `IMMUTABLE_METHOD` says it returns an immutable object — is silent;
- **unverifiable** — `this.items = items` from a parameter — is a `contract-unverifiable` *warning*.

The parameter case is the one that matters and the one a local check cannot settle: `TypeInspectionImpl`'s
contract is true (callers pass `List.copyOf(...)` from `Builder.commit()`) while a false one is
character-for-character identical. Refuting would accuse correct code; silence would let a load-bearing promise
pass unremarked. The warning says precisely what is true — *this is trusted, not checked, and the obligation
has moved to the callers* — and part 2 is what turns it into a verdict.

Proof of "produces an immutable object" is the AAPI's own `IMMUTABLE_METHOD`, never a hand-written list of
factory names: one definition instead of two that can drift, and more accurate than a name list. `List.copyOf`
is annotated and genuinely copies; `Collections.unmodifiableList` is deliberately not annotated, because it
returns a **view** — a caller holding the original can still change what the field sees, so treating it as
proof would bless a false contract.

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
- **Part 3, consumption — DONE.** Three reads sharing one rule (`DynamicImmutability`), plus the guard check.
  The abstract accessor lifts via the existing implementation-to-abstract copy, so the optimism coupling is
  closed rather than inherited. Corpus A/B identical (13 tests / 2 skipped / 0 failed, `TestCloneBench` 9306
  types, `TestShadowCloneBench` 224 divergences {nonModifyingMethod=1, unmodifiedField=8,
  unmodifiedParameter=215}) — expected, because nothing writes `IMMUTABLE_FIELD` on unannotated code.

**Where this leaves the feature.** Parts 1 and 3 are in: a hand-written `@Immutable` on a field is materialized,
consumed, and checked as far as a local check can. That is already enough to certify a type by annotating it —
at the cost of a promise the guard can only warn about rather than verify. Part 2 remains the large piece, and
remains the thing that would make the CST certify itself with no annotations at all.

## Part 2: why it was not built (2026-07)

Part 2 was authorized with an explicit soundness gate: a constructor parameter's dynamic immutability may only
be inferred when *every* caller can be enumerated. Applying that gate to the motivating example ends the
exercise before any code is written.

```
public class TypeInspectionImpl ...                 // public type
    public TypeInspectionImpl(Inspection, Set<TypeModifier>, List<MethodInfo>, ...)   // public constructor
exports org.e2immu.language.cst.impl.info;          // exported package
```

There is exactly **one** caller inside the analyzed source set (`Builder.commit()`, which does pass
`List.copyOf(...)`). But the constructor is public on an exported package, so any consumer —
`maddi-inspection-*`, a plugin, user code — may legally write

```java
new TypeInspectionImpl(inspection, modifiers, myMutableList, ...);
```

and the field then *genuinely holds a mutable list*. Inferring `IMMUTABLE_FIELD` from the single visible caller
would not be an approximation; it would be false. **The `contract-unverifiable` warning part 3 emits for these
fields is therefore correct and not removable by any amount of analysis** — it is a true statement about the
API, not a limitation of the analyzer. "Every caller must pass an immutable object" is an obligation the type
does not enforce.

### The distribution says the ceiling is low

Collection-typed fields in `maddi-cst-impl`, by the shape of their assignment (regex survey, indicative — nested
builders make exact attribution awkward):

| shape | fields | soundly inferable? |
|---|---|---|
| `this.f = List.copyOf(...)` locally in ctor/initializer | 6 | yes — purely local, no call-site walk |
| `this.f = param`, constructor **private** | 14 | yes — all callers are inside the primary type, which prepwork already scans whole |
| `this.f = param`, constructor **public/protected** | 80 | **no** — callers are unbounded |

So a sound part 2 reaches roughly 20 of ~100 fields and, decisively, **none of the 16 in `TypeInspectionImpl`**
that motivated it. Building it would deliver a minority payoff while leaving the headline goal exactly where it
is — and it would require the fixpoint integration that is the risky part of the work.

`private` is the natural boundary and it aligns with the machinery that exists: all callers of a private member
are necessarily within the same primary type, and `ComputeCallGraph` is built per primary type. Package-private
would already exceed that graph's reach.

### What would actually certify the CST

Three options, none of them part 2:

1. **Narrow the constructors.** Make the product constructors private/package-private with the `Builder` as the
   only route in. That makes the 80 become the 14, and a sound part 2 would then reach them. It is also the
   change that makes the promise *true* rather than merely believed.
2. **Copy at both ends.** Add `List.copyOf` to the accessors as well; certifies today with no analyzer change,
   at a second copy per call on a hot path (`fields()`, `methods()`, `parameters()`).
3. **Annotate and accept the warning.** Parts 1 and 3 already support this: the contract is materialized,
   consumed, and reported as trusted-not-verified. Defensible while the annotations stay inside code whose
   callers you control.

There is no closed-world/whole-program configuration in `IteratingAnalyzer.Configuration`, so option 1's
soundness cannot be bought with a flag.
