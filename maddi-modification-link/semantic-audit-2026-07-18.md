# Semantic verification round — modification & immutability verdicts (2026-07-18)

Scope: are the analyzer's verdicts semantically correct, judged against the road-to-immutability
definitions? Corpora: fernflower, guava, activemq-broker, camel-core-model (+ fernflower re-run with
analysis hints). Method: (1) full-dump distributions; (2) syntactic oracles (trivial getters/setters,
constructor rule, immutable-type consistency); (3) stratified 96-element sample (24 per corpus:
8 nonModifying=true, 8 =false, 4 unmodified=true, 4 =false), judged against source by parallel reader
agents with the spec, every disagreement re-verified by hand.

## Headline numbers (96-element stratified sample)

| outcome | count | share |
|---|---|---|
| semantically correct | 83 | 86% |
| over-conservative (safe direction) | 11 | 11.5% |
| **unsound (wrong in the dangerous direction)** | **2** | **2%** |

Methods (48 sampled): 1 unsound, 8 over-conservative. Fields (48): 1 unsound, 3 over-conservative,
8 "disagreements" that turned out to be intentional property semantics (below).

## The two unsound mechanisms (filed as tasks)

1. **Modification does not propagate through `Outer.this`** (task #28). Guava:
   `CompactHashMap.removeHelper` is correctly modifying, but `EntrySetView.remove` /
   `KeySetView.remove`, which call it on the enclosing instance, are nonModifying=TRUE. Calling a
   modifying method through the qualified outer reference loses the modification. Clean suite-test
   repro shape: inner class calling `Outer.this.modifyingMethod()`.
2. **Aliased static singleton content mutation missed** (fernflower): `BytecodeMappingTracer.DUMMY`
   is unmodified=TRUE although `toString`/`toJava` chains pass DUMMY as the tracer argument and
   `addMapping` mutates its map. The mutation flows through an alias (static constant passed as an
   argument), which the linking approximation does not track back to the field. This is the
   same-object-in-many-places phenomenon (see the tier-3 canonicalization discussion): identity is
   tracked per spelling, and the write lands on a different spelling than the field.

## Intentional-semantics finding: `unmodifiedField` is CONTENT-only

7 sampled fields (activemq PolicyEntry.durableTopicPrefetch a.o., camel RouteDefinition.kamelet
a.o.) are unmodified=TRUE while public setters assign them — and in every case the setter itself is
correctly judged modifying. The engine splits the doc's field-modification definition ("assignment
or modifying method") into two properties: assignment is tracked via effectively-final/setField
(immutability rule 0 consumes it), content modification via unmodifiedField (rule 1). Internally
consistent; the doc's single-definition phrasing diverges from the property granularity. Suggested:
one clarifying sentence in 030-modification.adoc.

## Over-conservative cluster map (where linking degrades, by mechanism)

| mechanism | examples | expected fix |
|---|---|---|
| external callee without annotations | `JdkPattern.flags()` (Pattern.flags is a field read), `Monitor.hasQueuedThread` (AQS traversal) | analysis hints, once loading works (#29) |
| writes to parameters/global context pulled into the receiver verdict | fernflower `ConstExprent.createFloat`, `FunctionExprent.match`, `InvocationExprent.equals`, `FastFixedSet.toString` | linking approximation; receiver-only attribution refinement |
| derived abstract verdicts poisoned by one modifying implementation | `Destination.getName` (all impls read-only — verdict false anyway), `ForwardingMultimap.size` (correctly false: a lazy-init impl exists) | case 1 is an abstract-derivation bug worth a look; case 2 is spec-correct |
| fresh-object construction misattributed | `ImmutableMapEntrySet.writeReplace` | linking approximation |
| builder/fluent chains over-linking fields | camel `TemplatedRouteBuilder.routeTemplateId` (final String judged content-modified) | link over-approximation on fluent flows |

## Immutability: structurally blocked, not imprecise (task #29)

Across ALL corpora, not a single type reaches a positive immutability level (guava: 1,614 undecided +
521 @Mutable, 0 @FinalFields/@ImmutableHC/@Immutable — despite UnsignedInteger having every
prerequisite green). Root cause chain, fully traced:
- TypeImmutableAnalyzer waits (returns null) whenever a supertype or relevant external lacks
  IMMUTABLE_TYPE (`stopExternal`), and `activateCycleBreaking` is never enabled (TODO in
  IteratingAnalyzerImpl) — so the wait never resolves.
- The annotated-API data that would supply those externals EXISTS (aapi archive: Object=@ImmutableHC,
  String=@Immutable, ...) and `--preload-analysis-results-dirs` loads and parses it — but
  LoadAnalysisResults resolves **0 of 249** primary types ("module not on the classpath") in a
  corpus run, so nothing attaches; the with/without-hints dumps are bit-identical.
- Consequently only the negative path (mutable-by-own-fields) ever concludes: the @Mutable verdicts
  sampled are correct; everything else is undecided.
So immutability attribution is currently: sound but only one-sided. Fixing the hints resolution
(#29) + a cycle-breaking activation strategy should unlock the positive side; re-measure then.

## Also confirmed en route

- The constructor non-confluence (parallel scheduling concluding nonModifying=true on
  field-assigning constructors, e.g. `StatEdge.EdgeType.<init>`) violates the spec's "non-trivial
  constructors are modifying": the sequential/conservative verdict is the correct one. The
  fernflower hints-run reproduced the flip as its only dump difference.
- Zero trivial setters judged nonModifying corpus-wide (syntactic oracle over 4 corpora): the
  method-side soundness baseline is strong.

Artifacts: scratchpad sem-*.txt dumps, sample-*.txt, semantic_audit.py, ff-hints-run.log.
