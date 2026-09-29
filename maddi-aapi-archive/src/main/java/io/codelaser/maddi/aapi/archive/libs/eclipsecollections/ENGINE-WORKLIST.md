# Eclipse Collections: blatantly wrong computed verdicts (engine work list)

Status: 2026-09-25, re-measured 2026-09-29 on the current engine (`b6fcd70b4` plus the results-writer fix, see
ECLIPSECOLLECTIONS.md F5): source run of EC 13.0.0, API + implementation as source, 4 minutes, 0 ceiling trips.
Produced by `corpus/scripts/ec-wrong-verdicts.py <results> --src <checkout> --tsv <out>`, which prints the counts
below and writes every element (about 19,000 rows) to a TSV. One movement since 09-25 worth knowing before reading
the families: the object `Immutable*` and `Mutable*` API interfaces (`ImmutableList`, `MutableList`, `ImmutableMap`,
...) now compute MUTABLE outright where they computed `@FinalFields`; the hints state their contract regardless
(`ECLIPSECOLLECTIONS.md`, the EXPECTED table), so this changes the engine's distance, not the shipped verdicts.

"Blatant" means contradicted by EC's own contract and readable from names alone: an `Immutable*` type below
`@Immutable(hc)`, a mutator (`add`, `put`, `clear`, `sortThis`, ...) computed `@NotModified`, a pure query
(`size`, `isEmpty`, `contains`, `toList`, ...) computed `@Modified`, and so on. No judgement calls. Every method
row is also classified by its SOURCE body:

- **delegates**: the body calls the same-named method on a field or getter (`this.delegate.size()`). The wrong
  verdict is a consequence of the callee's.
- **abstract**: an interface or abstract method, whose verdict is the union over its implementations.
- **own**: anything else. After fixing the classifier, there is **no leaf at all**: no wrong verdict in a body
  that calls nothing. Every wrong verdict flows through a call, so the work list below is organised by the
  MECHANISM that corrupts the flow, each with a verified example and a fixture-sized reproducer.

## Debugging status (2026-09-27)

Fixtures: `maddi-modification-analyzer/.../modification/TestOptimisticModificationShapes` (production configuration,
MODREACH on).

- **O1 FIXED** (`79fd32723`). Not a linking gap: the link summary had `MOD[this, this.items]`. The MODREACH cutover
  overwrote it. `ShadowModificationPass` cached receiver projections by expression identity; a lambda body is
  walked as its own method and inside its enclosing method, and the first walk owned the nodes. Broader than EC:
  plain `list.forEach(s -> this.add(s))` was @NotModified in every production run.
- **O2 CONFIRMED, a rule decision.** Prepwork records IMPLEMENTATIONS only on abstract overrides, and the shadow
  pass's E6 mirrors that deliberately (a default's verdict is its own body; `Element.annotations()` is the recorded
  reason). A default that only throws then reads @NotModified. Narrow option: treat a throw-only default body as
  abstract (union over overrides).
- **O4 CONFIRMED in reduction, cause found, a decision.** The JDK hint makes `java.lang.Iterable`
  `@ImmutableContainer(hc = true)`; an immutable type gets no `§m` modification component, so `iterator()`'s
  `except = "remove"` link (`it.§m ☷ this.§m`) is never created: `this.iterator().remove()` in a type that only
  extends `Iterable` is lost. Over `Collection` the same code is correct. EC's own shape (`booleanIterator()` on
  its own interfaces) is not yet reduced and may be P1 instead.
- **O2 FIXED by option B** (`26d024d1b`): a throw-only placeholder's modification is the union over its overrides.
  **Option D** in the same commit: `override-weakens-computed` warnings, dogfood 14, EC 486. EC, before/after:
  M1 76 -> 34, M2 +153 and M3 +64 (placeholders' unions and lambdas now carry the existing P1/functional
  pessimism). Every EC run (baseline included) re-derives after 3 MODREACH rounds and ends with ~11k refused
  `unmodifiedVariable` downgrades: pre-existing, not from these fixes.
- **O4 (JDK shape) FIXED, two parts.** (1) `java.lang.Iterable` is back to `@Container` in the JDK hints
  (`76b4c1e33`'s JSON reverse-applied; `openjdk.jar` byte-identical to before the lift). Price: source types
  implementing `Iterable` floor at FINAL_FIELDS again (vavr: 27 types, e.g. `Option`); the vavr hints do not
  move, they state semantics. (2) With the link back, MODREACH still overwrote the fixpoint's @Modified
  (`MODREACH_REVERSE nonModifyingMethod a.b.O4.removeIf2`): `ShadowModificationPass` projected a receiver only
  along whole-object assignment links. It now also follows a `☷` link (identical-to between `§m` faces with a
  NON-EMPTY pass set) when the pass set holds the called method. First tried with empty pass sets too (the
  engine's full rule in `WriteLinksAndModification`): the dogfood has ~1,900 such `≡` edges (`codec.§m ≡
  context.§m`), 39 CST methods went modifying and 22 types (`Value`, `Property`, ...) dropped from unconditional
  immutable-hc to eventual; the ratchet printed them as NEW "progress" because it compares names. Narrowed:
  dogfood method and type verdicts identical to the pass without the projection; 2 edges, both `it.remove()` on
  a local list. Fixture `TestOptimisticModificationShapes.o4` green with MODREACH on.
- **O4, EC's own shape found by D** (still open): `AbstractMutableCollection.remove(Object)` removes through `this.iterator()`
  (EC's abstract iterator(), whose independence is the implementation union) and is computed @NotModified.
- **F5 crash FIXED** (`Trying to overwrite ... variablesLinkedToObject`, `AbstractMutable<P>KeySet.*Satisfy`, 2 of 4
  EC runs, exit 5): a race. Two worker threads may link the same method (ExpressionVisitor's LOCK branch), and the
  per-call-site write in `writeOutMethodCallAnalysis` was check-then-set. Now `getOrCreate` (first writer wins, as
  before); `markDegraded` likewise. `TestConcurrentMethodCallWrite` reproduced it in 9 s before the fix. EC after:
  4 of 4 runs clean.
- **O3 NOT REPRODUCED** in reduction (over `AbstractSet`). EC's `remove` binds to EC's own interfaces: reduce
  further, or it is P1/O2.
- **O4 (EC shape) FIXED, four parts (2026-09-26, uncommitted on ws/dsl).** (1) An inner class's `this.§m` is linked
  `☷{P}` to the outer instance's `this.§m` at `new Inner()`, P = the inner methods that modify the outer `this`
  (`ExpressionVisitor.linkInnerClassToOuterInstance`); (2) a source method's `except` is COMPUTED
  (`TypeModIndyAnalyzerImpl.withDependentExceptions`: the pass of the `☷` links from `§m` to `this`, filtered to
  the declared return type), so `booleanIterator()` reads `@Independent(except = "remove")` without a hint;
  (3) a source abstract method's links follow its decided independence (`ShallowMethodLinkComputer.
  modificationExceptLink`); (4) retention: a statement's links (`VariableInfoImpl.setLinkedVariables`) and a
  source method's links (`TolerantWrite.setLatestWins`, the analysis-order slot only) take the LATEST
  computation when the key is equal and the content differs -- before, both froze at the first computation
  (`Links` equality is primary-only), so a callee decided `@Independent` never propagated upward. Also
  `AbstractMethodAnalyzerImpl.methodIndependent` skips implementations that only delegate back into the
  union (`delegatesWithinFamily`). Fixtures: `TestOptimisticModificationShapes.o4ec/o4ecVeto`,
  `TestDefaultMethodModification.inheritedDefaultMethodIterating`, `TestDelegatingUnion`.
  EC (engine = this chain, before devel's 09-26 merge): M1 49 -> 24, the 8 `Mutable<P>Collection.removeIf`
  rows gone, M5 217 -> 0, M3 3782 -> 3350, 7 min. Dogfood: 32 independence verdicts up, none down, three
  identical runs.
- **INTERACTION with devel's 09-26 linker (ws/server): FIXED 2026-09-27 (two engine rules), the two-cycle
  gone with it.** Merged with devel (`0d755fabd`, the if/else fork/join of the link graph), EC took 1h40
  instead of 9 min: five `[work ceiling]` degradations, `CollectionAdapter.wrapList(Iterable)` alone
  grinding 72 min to 30M work, field paths `iterable.delegate.delegate.delegate`. Attribution runs: devel
  alone 8m52 (no trips); this chain alone 6m58 (no trips); both + `NOFORK=1`: 3m41, no trips. Two mechanisms,
  both in the composition step of the link closure (`IncrementalFixpointEngine`), both amplified by the join
  (a union of the alternatives' facts) and exposed by part (4) (the first computation's small links no
  longer freeze):
  1. *Own field path.* `rv ← rep` (`return iterable`) and `rv.delegate ← rep` (`return new Adapter(iterable)`)
     compose ('← ∘ → = ≡') to `rv ≡ rv.delegate`, a fact no execution makes; each further alternative and
     each recomputation adds a `.delegate` level. Rule: no derived identity/assignment between a variable
     and its own real field path (engine predicate + the reconstructed edges in
     `WriteLinksAndModification`). Fixture `TestAdapterChainGrowth`. This rule ALONE moved the ceiling grind
     to `UnifiedSet.remove(Object)` (20 trips, 7m03): it had removed the shortcut that hid mechanism 2.
  2. *Constant middle.* `ChainedBucket.removeLast`'s `return null` is one constant marker (`$_ce12`) in its
     summary, hence the same vertex at each of the four call sites of `removeFromChain`;
     `bucket.zero ← $_ce12` + `$_ce12 → bucket.one` composed to `bucket.zero ≡ bucket.one` — four slots
     "identical" because each was once assigned null, 64,586 such composites in one method, and mirror
     faces from there (`bucket.one → this.occupied.zero.two`). Rule: a constant marker is never the middle
     vertex of a composition. Fixture `TestRemoveChainWork` (`removeFromChain` 2,503 → 115 summary links,
     4.4M → 15k work). Pre-existing: F5's results held 12,212 constant-marker references in summaries,
     devel's 20,530, now 6,501.
  With both: EC 4m03 (devel alone 8m52), 0 trips, 150 iterations with NO one-change iterations — the
  `ifPresentApply` two-cycle (`--> ifPresentApply∋Λ1:function*` ↔ `--> -`, ~120 iterations of one
  methodLinks change) is gone; M1 24, M5 1, T1 401 unchanged; 77 verdict changes vs rule 1 alone, all
  upward (19 methods and 6 parameters to independent, among them the `UnifiedSet.remove*` family; the
  primitive `*ObjectMap` types lose a spurious `retainAll(prim[])` from their dependence list). Fast suites
  green (link 424, analyzer 346, prepwork 245); ratchet survivors identical (317).

## The numbers

| | rule | elements | direction |
|---|---|---:|---|
| T1 | `Immutable*` type computed below `@Immutable(hc)` | 410 | pessimistic |
| T2 | mutable type computed `@Immutable(hc)` or better | 2 by the name rule (+1 false positive); 30 frozen, both directions (O7) | **optimistic** |
| T3 | eventual type verdict, 0 eventual methods in the run | 18 | **unsound** |
| T4 | stateless factory implementation below `@Immutable(hc)` | 1 | pessimistic |
| M1 | mutator computed `@NotModified` | 76 (39 own, 29 delegates, 8 abstract) | **optimistic** |
| M2 | pure query computed `@Modified` | 11,480 (9,967 delegates, 1,185 own, 328 abstract) | pessimistic |
| M3 | other method of an `Immutable*` type computed `@Modified` | 3,715 | pessimistic |
| M4 | read-only argument (`addAll`'s source, `equals`' other, ...) computed `@Modified` | 3,063 | pessimistic |
| M5 | copying method (`toArray`, `newWith`, ...) on a concrete type computed `@Dependent` | 217 | pessimistic |

The pessimistic families are large but have one dominant root (P1). The optimistic families are small, and each
is a soundness defect. They come first.

## Optimistic: the analysis claims less modification than there is

### O1: modification through a lambda capturing the outer `this` is lost (14 rows)

```java
// impl.map.mutable.primitive.ByteObjectHashMap (and the 6 other <P>ObjectHashMap)
public void putAll(ByteObjectMap<? extends V> map) {
    map.forEachKeyValue((byte key, V value) -> ByteObjectHashMap.this.put(key, value));
}
```
`putAll` is computed `@NotModified`. The lambda calls `put` (computed `@Modified`) on the captured outer instance
and is handed to the ARGUMENT's `forEachKeyValue`. Fixture: a class whose method passes `x -> Outer.this.add(x)`
to a parameter's `forEach`. Expected: the method is `@Modified`.

### O2: a default method that only throws is taken as the method's contract (verified on `sortThis`)

```java
// api.list.primitive.MutableByteList
default MutableByteList sortThis(ByteComparator comparator) {
    throw new UnsupportedOperationException("sortThis(ByteComparator comparator) is not supported on " + ...);
}
```
Computed `@NotModified`, with NO implementations recorded (`implementations` is absent), although `ByteArrayList`
overrides it and sorts. So a call through the interface is non-modifying, and
`SynchronizedByteList.sortThis(comparator)` (= `this.getMutableByteList().sortThis(comparator)`) inherits the
optimism. A default method's verdict must also be the union with its overrides. Fixture: an interface with a
throwing default `m()`, one class overriding `m()` to modify a field, and a caller of `I.m()`.

### O3: `this.remove(x)` in a for-each over an argument is lost

```java
// impl.set.AbstractUnifiedSet (abstract; does not declare remove: the call binds to java.util.Collection.remove)
public boolean removeAllIterable(Iterable<?> iterable) {
    boolean changed = false;
    for (Object each : iterable) { changed |= this.remove(each); }
    return changed;
}
```
Computed `@NotModified`. Likely also `retainAll` → `this.retainAllIterable(...)` in the same class. The callee
here is a JDK hint (`Collection.remove`), so this is either the call from an abstract class to an inherited
library method, or the loop. Fixture: an abstract class implementing `Set<Object>` with that method.

### O4: modification through an iterator obtained from `this` is lost (22 rows)

```java
// api.collection.primitive.MutableBooleanCollection (and the 7 other primitives), a default method
default boolean removeIf(BooleanPredicate predicate) {
    boolean changed = false;
    MutableBooleanIterator iterator = this.booleanIterator();
    while (iterator.hasNext()) { if (predicate.accept(iterator.next())) { iterator.remove(); changed = true; } }
    return changed;
}
```
Computed `@NotModified`. `iterator.remove()` modifies the collection the iterator came from. Either the iterator
is not linked to `this`, or the link does not carry modification back. Compare the JDK `Iterable` hint's
`@Independent(hc = true, except = "remove")`, which is how that link is meant to be stated.

### O5: modification through a getter's result (3 + the synchronized wrappers' delegating mutators)

```java
// impl.map.mutable.SynchronizedMutableMap, SynchronizedSortedMap, SynchronizedBiMap
public void putAllMapIterable(MapIterable<? extends K, ? extends V> mapIterable) {
    synchronized (this.lock) { mapIterable.forEachKeyValue(this.getDelegate()::put); }
}
```
Computed `@NotModified`: a method reference bound to a getter's result, `this.getDelegate()::put`. The 29
"delegates" rows of M1 (`this.getDelegate().removeAll(key)`, `this.getMutableByteList().sortThis(...)`) are partly
this (modification of a getter's result not charged to the field behind it) and partly O2 (the callee is
optimistic). Separate the two before fixing either.

### O6: 18 eventual type verdicts without a single mark (T3)

`@Immutable(hc = true, after = "collection")` and similar on the primitive `Synchronized*Collection` /
`Unmodifiable*Collection` wrappers, `UnmodifiableMutableCollection`, `SynchronizedRichIterable`. The run has 0
eventual methods. The labels are field names: vavr's `MatchError` shape, which `EventualCluster.isGroundedInMark`
(`5e7f2cd58`) was meant to close. A path that writes an eventual TYPE verdict bypasses the gate. Extend
`TestEventualNeedsAMark` with a wrapper: a final field of an interface type, delegating methods, no mark. The
44 method-level labels left in vavr's hints are probably the same writer's twin.

### O7: frozen type verdicts: the engine disagrees with itself (30 types)

The first certification (before the MODREACH rounds) had 244 refused downgrades, 56 of them on `immutableType`. For 30 types a later iteration wanted
`@FinalFields` and the write-once guard kept an earlier `@Immutable(hc = true)`. Which of the two is wrong varies:

- **the frozen value is wrong (optimistic):** `Partition*` (`PartitionFastList`, `PartitionUnifiedSet`,
  `PartitionTreeSortedSet`, `PartitionHashBag`, the `api.partition` interfaces). `PartitionUnifiedSet` holds
  `private final MutableSet<T> selected, rejected`, so `@FinalFields` is right. T2 finds two of them by name.
- **the refused downgrade is wrong (pessimistic):** `FixedSizeSetFactoryImpl`, `FixedSizeMapFactoryImpl` have no
  instance fields: stateless, so `@Immutable(hc = true)` is right, and something made a later iteration want less.
- **convention-dependent:** the `impl.parallel.*ProcedureFactory` types hold a final `Predicate`/`Function`.

Either way, a refused downgrade on a type verdict means two iterations computed different answers for the same
type, which a converged analysis should not do. Trace `PartitionUnifiedSet` and `FixedSizeSetFactoryImpl`
(`EC_TYPE_DEBUG`; mind the F5 crash in `ECLIPSECOLLECTIONS.md`, seen on such a run).

T2's third row, `impl.utility.internal.MutableCollectionIterate` `@Immutable`, is a utility class (private
constructor, static methods only), so it is a false positive of the name rule.

## Pessimistic: the analysis claims more modification or dependence than there is

### P1: delegation cycles through the implementation union (the dominant root)

`RichIterable.size()` has 161 implementations. 102 are computed `@NotModified`; of the 59 computed `@Modified`,
**54 are delegators** (`this.delegate.size()`, `this.getDelegate().size()`: the `Synchronized*`, `Unmodifiable*`,
`MultiReader*`, `AbstractBiMap`, the adapters). A delegator's verdict is the verdict of the abstract method it
calls, which is the union that contains the delegator: a cycle, broken at "modifying". From there it spreads.
Every abstract query of the read-only API is computed `@Modified` (all 181 methods of `RichIterable`, including
`size`, `isEmpty`, `contains`), then every default and concrete method calling them (M2: 9,967 delegates and most
of the 1,185 "own"), every `Immutable*` method (M3), every argument passed to one (M4: `containsAll(source)` is
`source.isEmpty()`), and the `Immutable*` types (T1: a type whose methods modify cannot be immutable).

The most-blamed implementations: `AbstractMultiReaderMutableCollection` (82 abstract methods),
`AbstractBiMap` (52), `SynchronizedStack` (47), `UnmodifiableStack` (47), `UnmodifiableBiMap` (45),
`UnmodifiableMutableOrderedMap` (45), `ImmutableByteObjectHashMap` (43, an immutable map delegating to a
mutable one).

The fixture is small:
```java
interface I { int size(); }
class A implements I { private final int n; A(int n) { this.n = n; } public int size() { return n; } }
class W implements I { private final I d; W(I d) { this.d = d; } public int size() { return d.size(); } }
```
Expected: `I.size()`, `A.size()` and `W.size()` all `@NotModified`. This is the same lever as vavr's G1
(per-receiver summaries). Here the question is narrower: what the cycle should be broken AT. A union in which
the only modifying contributions are cycle members is not modifying.

### P2: `new Proxy(this)` makes the receiver modified (152 `writeReplace`)

```java
// impl.map.mutable.primitive.ImmutableIntMapKeySet (and every Immutable* with a serialization proxy)
private Object writeReplace() { return new ImmutableIntSetSerializationProxy(this); }
```
Computed `@Modified`. Probably P1 again, one step removed: the proxy's constructor parameter is `@Modified`
because its `writeExternal` iterates the set via a union-modifying method. Confirm by reading the proxy
constructor's parameter verdict before treating P2 as separate.

### P3: a fresh result's modification is charged to the receiver (504 methods)

```java
// api.factory.map.primitive.MutableIntIntMapFactory (all 504 with/of overloads of the primitive map factories)
default MutableIntIntMap with(int key, int value) { return this.with().withKeyValue(key, value); }
```
`with()` is computed `@Independent @NotModified` (a new map), yet adding to that new map makes `with(k, v)`
`@Modified`. Fixture: `default M with(int k) { return this.with().add(k); }` with an independent, non-modifying
`with()`. Not in the rule-based table above: the factory interfaces themselves are (correctly) `@Immutable(hc)`,
which is why the hints compiler flagged all 504 as "@Modified method in @Immutable type".

### P4: copying methods computed `@Dependent` (M5: 217, 51 own)

`ImmutableIntSingletonBag.newWith(int)`, `Immutable<P>SingletonSet.toBag()`, `toArray()`/`toSortedArray()` on the
immutable primitive hash bags and sets. The "own" ones build their result through a static factory
(`IntBags.immutable.with(this.element1, element)`): the varargs array or the factory's result is linked back to
the receiver. The 166 delegating ones copy from a mutable delegate field (P1's `ImmutableByteObjectHashMap` shape)
and are expected to follow P1.

### Not yet placed

- `impl.map.mutable.ConcurrentHashMap.size()` reads fields and an atomic array and is computed `@Modified`. It
  is one of the 5 non-delegating implementations that make `RichIterable.size()` modifying. The other 4 are
  lazy views counting through `forEach`/`count`, which is P1 again. `ConcurrentHashMap.size()` may be a genuine
  seed (an `AtomicReferenceArray`/`AtomicIntegerArray` hint?).
- `ImmutableIntArrayList` is `@FinalFields` with all methods non-modifying and independent, while
  `ImmutableIntSingletonList` (same interfaces, one `int` field) is `@Immutable`. The difference is the
  constructor-shared `int[] items`. The type-debug run that would have shown it crashed (F5 in
  `ECLIPSECOLLECTIONS.md`: `Trying to overwrite a value for property variablesLinkedToObject`, not reproduced).

## Suggested order

1. **O2 and O1**: optimistic, small, fixture-sized, and each hides modification from every caller.
2. **O6**: unsound, a known gate with a known fixture to extend.
3. **P1**: one fix should move thousands of verdicts. Measure with this script: it is the before/after.
4. **O3, O4, O5, O7**, then P3, P4, P2 (probably subsumed by P1).

Every fix should be re-run against vavr too (the eventual-cluster lesson: a change validated on one corpus is
unvalidated), and this script's counts should be recorded before and after.
