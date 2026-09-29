# The receiver decides too: level and cone rules at a call site (2026-09-29)

**Status: prototype behind the `RECEIVERLEVEL` gate (environment, presence), off by default.** Measured on vavr;
fast pin `TestReceiverLevel`. The gap it addresses is G1 of `VAVR.md`, F1 of `ECLIPSECOLLECTIONS.md` and the
`Immutable*` rows of `GUAVA.md`: a collection interface shared between stateful and persistent implementations
makes its abstract methods modifying by the union over all implementations, and a persistent type calling such a
method on `this` or on a field of the interface type is charged the modification.

## What the engine did

`MethodModification.go` (link engine) marks the receiver of a call modified when the callee `isModifying()`, and
an argument when the callee's parameter `isModified()`. Both read the callee's verdict alone; for an abstract
callee that verdict is `AbstractMethodAnalyzerImpl.methodNonModifying`'s union over `IMPLEMENTATIONS`. Nothing
about the receiver was consulted: not its static type, not its immutability. `ShadowModificationPass` (MODREACH,
the post-convergence authority in production) mirrors this with an E2 edge from the callee's node to the
receiver's nodes, the callee's node aggregating every implementation through E6.

## The two rules (`ReceiverLevel`)

1. **Level rule.** A call cannot modify a receiver, or an argument, whose object is immutable with hidden
   content or better: its own fields cannot change through any of its methods. The level is the receiver's
   *effective* one: the static type's own `IMMUTABLE_TYPE` (the base type; the deep immutability of the
   parameterized type folds the type arguments in and says nothing about the container), improved by what the
   variable is known to HOLD: `IMMUTABLE_FIELD` / `IMMUTABLE_PARAMETER`, the dynamic immutability produced by
   `DynamicImmutabilityInference` or a contract, consumed until now for independence only (`DynamicImmutability`).
   An undecided level is no evidence: the call is judged as before. No waiting, no optimism.
2. **Cone rule.** The receiver's static type `T` bounds the runtime type, so only the callee's implementations in
   `T`'s cone (T and its subtypes) can run. For a source type `T` strictly below the callee's declaring type,
   the receiver is charged only when a cone implementation is modifying or undecided; the callee's parameter
   likewise over the implementations' parameter at the same index. A contracted callee (`@Modified`/`@NotModified`
   on the declaration; every jar or hint declaration) keeps its declaration; an empty cone gives no evidence. The
   implementations read are recorded as summary consumptions (`LinkComputer.recordSummaryConsumption`), because
   the worklist tracks the callee's summary, which does not change when a cone member decides: without the
   record the caller kept a stale first-pass verdict (seen in the fixture under the optimistic cycle strategy).

The shadow pass mirrors both: a receiver or argument face whose base type is at hc-level contributes no node
(`immutableForModification`), and a call site with a cone is wired to the cone's implementations instead of to
the callee (E2 receiver edge, E1 parameter edges). Without the second mirror MODREACH downgraded every cone
result whose receiver type had not reached hc-level, which is all of them today (see below).

## What it moved: vavr, source run, engine `e48276146`, MODREACH on

| | gate off | gate on |
|---|---:|---:|
| run time / iterations | 13.5 min / 58 | 15.0 min / 58 |
| refused downgrades at certification | 2,338 | 2,234 |
| work-ceiling trips | 0 | 0 |
| methods computed modifying (of 5,477) | 1,952 | 1,784 |
| method verdicts modifying → non-modifying / the reverse | | 168 / 0 |
| parameter verdicts modified → unmodified / the reverse | | 121 / 0 |
| `List` modifying methods (of 149) | 46 | 20 |
| `Vector` (of 148) | 58 | 28 |
| `Array` (of 147) | 31 | 19 |
| `IndexedSeq` (of 100) | 54 | 26 |
| `PriorityQueue` (of 75) | 30 | 10 |
| `BitMappedTrie` (of 36) | 7 | 0 |
| types that changed level | | 0 |

Every change is in the expected direction. No type changes level: the persistent collections stay at
`@FinalFields`, and the fixture shows why.

## What still caps the types: the independence cycle (G2)

In the fixture, with the gate on, `Cons.tailIsEmpty()` is non-modifying and `Cons.tail` unmodified, yet `Cons`
and `Seq` compute `@FinalFields`. `Seq.tail()` returns a `Seq`; `Cons.tail()` hands out the field; the field's
independence waits on `Seq`'s immutability, which waits on `Seq.tail()`'s independence (folded over `Cons.tail()`),
and cycle breaking resolves the wait at `FINAL_FIELDS` (`TypeImmutableAnalyzerImpl`, "a verdict that will never
arrive is pessimistic"). The single-type case has a rule: a self-referencing field (declared type = the type
itself) is skipped in the independence that feeds immutability. The interface/implementation split
(`List`/`Cons`, `Seq`/`Cons`) is the same shape across two types, and has none. That is G2 of `VAVR.md`
(22 persistent types `@Dependent` where `@Independent(hc = true)` is expected) and the next lever: resolve a
cluster of mutually waiting SOURCE types, whose only undecided inputs are each other, at hc-level in the
cycle-breaking pass, and never above it. `CycleBreakingStrategy.INTERNAL_NO_INFORMATION_IS_NON_MODIFYING` names
that idea and is consumed nowhere.

## What still charges the persistent methods: the helper path

`List` keeps 20 modifying methods (`append`, `sorted`, `removeAll`, `patch`, ...). They pass `this` to a
static helper or build through an `Iterator` of `this`; inside the helper the receiver's static type is the
abstract one (`Value`, `Traversable`), whose cone is everything. The cone rule cannot see the caller's type
from inside the callee. A summary of the helper's parameter keyed by the ARGUMENT's level (four keys, not one
per receiver type) would close it, but the argument's level is the type under computation: it needs the G2
resolution first.

## Not done

- `ShadowModificationPass.projectModificationIdenticals` (the ☷ iterator rule) still wires to the callee.
- The level rule reads `IMMUTABLE_FIELD` / `IMMUTABLE_PARAMETER`; nothing produces the parameter one for source
  code yet, and the field inference fires only on copy-factory shapes.
- Eclipse Collections and Guava runs under the gate: not measured.
