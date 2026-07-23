# The two SAM conventions: what actually diverges

**Status: research findings + one measured, rejected repair attempt; no code change.** Follow-on from
[`independent-type-optimism.md`](independent-type-optimism.md), whose recommended option 1 was
"reconcile the two SAM conventions first". This note establishes *what* has to be reconciled, and
retires two plausible-but-wrong theories about it — including the one this investigation started from.

## The contradiction, pinned to one element

For a SAM with no registered `IMPLEMENTATIONS`, the engine holds two contradictory positions:

- `AbstractMethodAnalyzerImpl.doMethodWithoutImplementation` writes `UNMODIFIED_PARAMETER=TRUE` on its
  parameters (optimistic: nothing is known, so nothing is assumed to happen). First iteration only, via
  `TolerantWrite.setOnce`.
- The link computer seeds the *formal* parameter of the applied SAM into the caller's modified set
  (pessimistic).

Measured on E7 `INPUT4` with the independence fix applied (`TypeIndependentAnalyzerImpl`
`return null` when undecided):

```
apply:0:o   UNMODIFIED_PARAMETER=true   IMPLEMENTATIONS.empty=true
run.modified = a.b.X.ThrowingFunction.apply(a.b.X.TryData):0:o, a.b.X.run(...):0:td, run
```

The same parameter is simultaneously asserted unmodified and seeded as modified. Main reports the first,
the shadow pass's reachability walk concludes the second, and `TestShadowModificationPass`'s
"correctly-analyzed code must diff clean" invariant fires. `IMPLEMENTATIONS` comes from
`overrides()`, so a SAM satisfied only by a method reference genuinely has none — this cannot be fixed
by populating it.

## Experiment: three SAM shapes, one program

The E7 `INPUT4` shape was run with three different SAMs, everything else identical, both with and
without the independence fix. `someSetUnmod=false` in every cell — the essential E7 path (modification
reaching `someSet` through the captured Result) works throughout; only the marking differs.

| variant | SAM | `run.modified` (baseline) | `run.modified` (independence fixed) |
|---|---|---|---|
| **A** | custom, non-generic `ThrowingFunction` | `run:0:td, run` | `apply:0:o, run:0:td, run` ← **false positive** |
| **B** | `java.util.function.Consumer<TryData>` | `run:0:td` | `run:0:td` ← **correct** |
| **C** | custom, generic `MyConsumer<T>` | *(empty)* | *(empty)* ← **under-approximates** |

Two things follow.

1. **The `java.util.function` exception does prevent the false positive** — the hypothesis this
   investigation started from is confirmed at the level of observable behaviour.
2. **But the split is not simply package-based.** A custom *generic* SAM also avoids the false positive,
   while losing the correct `run:0:td` marking entirely. Three shapes, three behaviours, one program.
   Only B is right.

Under the independence fix all three reach identical, correct type-level values (`TryData`
`@Dependent`/`@FinalFields`, `TryDataImpl` `@Dependent`/`@Mutable`), so the divergence lives purely in
the link/modification layer, not in the immutability lattice.

## Two theories, both refuted

### It is not the virtual fields (refutes the `§8.3` framing for *this* sighting)

`virtual-fields.md` §8.3 documents that `VirtualFieldComputer.compute()` short-circuits only the
`java.util.function` package while `Util.needsVirtual()` excludes all functional interfaces, and
suggests the concept implies aligning `needsVirtual` *to* `compute`. The natural reading — a custom SAM
gets a `§m` modification component that a `java.util.function` SAM does not, and the seed attaches to it
— predicts that removing those virtual fields removes the false positive.

**Measured: it does not.** Aligning `compute()` in the *other* direction (short-circuit every functional
interface, not just the package) leaves variant A's false positive exactly as it was. Virtual-field
structure is not what produces the seed.

### It is not `$_afi` deferral

`LinkMethodCall.samOfFunctionalInterface` already implements deferral: when the SAM's receiver resolves
to a parameter of the current method it decorates it with an `AppliedFunctionalInterfaceVariable`
(`$_afi`) "to be resolved once the concrete lambda bound to that parameter is known at the outer call
site". That is exactly the shape a "defer to the caller" redesign would want.

But it is not what discriminates A from B: the receiver (`td.throwingFunction()`) resolves the same way
in all three variants, and `TryData.throwingFunction()` is `@Dependent` in all three once independence is
fixed. Deferral is orthogonal to the divergence.

## What actually discriminates: contract versus inference

`java.util.function.Consumer.accept` is annotated in the AAPI archive
(`maddi-aapi-archive/.../jdk/JavaUtilFunction.java`):

```java
void accept(/*@Independent(hc=true)[T]*/ @Modified T t) { }
```

With that contract the engine knows the SAM modifies its argument and translates the effect onto the
concrete argument — `run:0:td`, clean, no formal parameter in sight. It never needs to seed, and
`doMethodWithoutImplementation` never gets a say, because `Consumer.accept` is external and already
decided.

A custom SAM has no such contract. The engine must infer, and the two conventions above then contradict
each other on the same element. **So "`java.util.function` is special" is true, but it is special
because it is *annotated*, not because of any package check in the virtual-field logic.** The package
check in `compute()` is a separate matter that happens to correlate.

Variant C shows genericity is a third, independent axis: a generic custom SAM produces no marking at
all, losing even the correct `run:0:td`. That is an under-approximation and arguably its own defect;
it is not the subject of this note.

## Does one redesign cover both sightings?

**No — the evidence says they are distinct.**

- The 2026-06 sighting (`virtual-fields.md` §8.3) is about *carriage*: aligning `needsVirtual` to
  `compute` moves functional-interface values off the Λ (SAM/lambda) path onto `§m` virtual-field
  content. Reproduced here: exactly five `TestModificationFunctional` failures, four of them literally
  "ThrowingFunction: propagation part 1–4", with links changing from
  `build.throwingFunction←Λthis.bodyThrowingFunction` to `build.throwingFunction.§m≡…`.
- Our sighting is about *contract versus inference* for the SAM's parameter, plus the
  optimistic/pessimistic contradiction that inference triggers.

They share a cast of characters (custom SAMs, `ThrowingFunction`) but not a mechanism — demonstrated by
the fact that changing the virtual-field predicate does not move our false positive. Fixing §8.3 will
not fix this, and fixing this will not fix §8.3.

## Candidate designs

1. **Stop the optimistic write for a SAM that is actually applied.** `doMethodWithoutImplementation`
   would not write `UNMODIFIED_PARAMETER=TRUE` for a functional interface's SAM, leaving the
   conservative seed uncontradicted. Smallest change; makes main agree with shadow by conceding to the
   pessimistic side. Cost: every custom SAM parameter becomes potentially-modified, which will ripple —
   wants the corpus.
2. **Stop the pessimistic seed when the callee's parameter is contractually unmodified.** The inverse:
   the link computer would consult `UNMODIFIED_PARAMETER` before seeding a formal parameter. Makes main
   and shadow agree on the optimistic side, and keeps E7's current pinned expectations. Risk: the
   optimism is unfounded — it comes from `doMethodWithoutImplementation`'s "nothing is known", not from
   evidence — so this may hide real modification through lambdas.
3. **Give custom SAMs the same treatment as annotated ones** by deriving the SAM parameter's
   modification from the lambdas/method references actually bound to it at capture sites. Principled and
   matches what `$_afi` does for the receiver case, but it is the real redesign, and the capture site is
   not always in the same compilation unit.

### Option 1, measured against the corpus (2026-07) — rejected

Option 1 was implemented and evidence-tested. It was scoped as narrowly as the diagnosis justifies:
`doMethodWithoutImplementation` skips *only* the `UNMODIFIED_PARAMETER=TRUE` write, and only when
`methodInfo == methodInfo.typeInfo().singleAbstractMethod()`. The structural test was chosen over
`isSAMOfStandardFunctionalInterface()` because a lambda can target any structurally-functional
interface, annotated or not, so the narrower test would miss exactly the un-annotated custom SAMs that
have this problem. `INDEPENDENT_METHOD`, `IMMUTABLE_METHOD` and `INDEPENDENT_PARAMETER` were left alone.
The independence fix from `independent-type-optimism.md` was applied at the same time, as the two are
entangled.

What it achieved:

- **The main-vs-shadow contradiction on the E7 fixture is resolved.** `TestShadowModificationPass`
  "shadow agrees on the forwarding-hop callback shape" passes. That is what option 1 promised.
- Ripple across the unit suite is tiny: **exactly one** test fails build-wide.

Why it was rejected anyway:

- **The false positive survives.** The one failing test is `TestModificationFunctionalE7.test4`, whose
  expectation is ground truth: `run.modified` becomes
  `apply:0:o, run:0:td, run` where `run:0:td, run` is correct, because `apply` is realized only by
  `this::methodBody` and `methodBody` never touches its parameter. Option 1 makes main *agree* with
  shadow by conceding to the pessimistic side — it buys consistency by adopting the wrong answer, and
  passing it would require editing the expectation that encodes the right one.
- **At corpus scale it makes the divergence worse, not better.** `TestShadowCloneBench` over the
  clone-bench corpus (9306 types) pins a known main-vs-shadow divergence count; option 1 moves
  `unmodifiedParameter` from **215 to 216**. It removes *none* of the 215. That is the decisive
  measurement: if SAM-parameter optimism were the cause of the main/shadow disagreement, fixing it
  should have reduced that count. It did not. Option 1 is therefore not the reconciliation — it is a
  separate behaviour change that happens to move the one fixture.

Everything else in the corpus was identical to baseline: `TestCloneBench` 9306 types green, and
ActiveMQ, Fernflower, Guava, JenkinsCore, LangChain4j, TimefoldSolver, TypeDependencies, FormatterStress
and CloneBenchMethodHistogram all unchanged (13 tests / 2 skipped / 1 failed vs. baseline 13 / 2 / 0).

The eventual-immutability dogfood measurement was also unchanged (30 `eventualMethod`, no
`eventuallyImmutableType`), confirming that the missing type-level verdict is structural — `TypeInfo`
declares `setOnDemandInspection` and `builder()` but not `commit()`, so the interface carries no
`@Mark` — and not a consequence of the independence gate.

**Revised recommendation: option 3.** The measurement above removes option 1 from contention and, by
showing the 215 existing divergences are untouched by SAM-parameter optimism, suggests option 2 would
not reach them either. Deriving the SAM parameter's modification from the lambdas and method references
actually bound at capture sites is the remaining candidate that could be *correct* rather than merely
consistent. It is the real redesign, and it should be attempted only with the corpus harness in place —
which it now is.

---

**Original recommendation (superseded): option 1**, measured against the corpus. It resolves the contradiction in the
direction of soundness (a SAM whose implementations are invisible *may* modify its argument), and it is
the one that does not require inventing new machinery. Option 3 is the right long-term answer and should
be revisited if option 1's ripple proves too coarse.

Note for whoever runs that measurement: `TestShadowCloneBench` is the invariant this whole question
surfaced through, and it is now corpus-covered — pass
`-Dtestarchive.root=<path>` (or `TESTARCHIVE_ROOT`) so it does not skip, and force a genuine re-run.
It is the test that decided the option 1 measurement above.

## Reproducing

The three-variant fixture is not checked in (it was scratch). To rebuild it: copy
`TestModificationFunctionalE7.INPUT4`, and swap `ThrowingFunction` for `java.util.function.Consumer<TryData>`
(variant B) and for a custom `interface MyConsumer<T> { void accept(T o); }` (variant C). Apply the
independence fix from `independent-type-optimism.md` and print `mlv(run).sortedModifiedString()`.
