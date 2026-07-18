package org.e2immu.analyzer.modification.link.impl.graph;

import org.e2immu.analyzer.modification.link.impl.Gate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
Features:

1. label function has a reverse and combine; these do not play well together
   e.g. rev(∋ + ∈) = rev(~) != rev(∋)+rev(∈) == X
   it also has a test for valid labels (X is never stored) and a score function (see further)
2. LabeledGraph is symmetric: if a B c is present, then c rev(B) a is present as well
   we do not need to store the reverse graph for efficiency reasons
3. ClosureGraph is symmetric: if a B c is present, then c rev(B) a is present as well
   we do not need to store the reverse graph for efficiency reasons
4. Witnesses keep support history to avoid cyclic reasoning
5. The incremental fixpoint algorithm must have a forward and a backward phase, because the lack of operator symmetry.
   Each phase must do one direction at a time!
6. we prefer direct high value edges over lower value edges, e.g. ← + ∈ is preferred over ∈ + ~
   Their outcome is different!
   As a consequence, the forward propagation must go over the closure rather
   than the graph, because otherwise it cannot find the best combination (it will find 'a' combination).
   See TestDependent,2 for an example, where ∈? finally gets replaced by ∈ because of an already existing combination
   in the closure.
7. the Graph and MakeGraph classes try to keep this graph as simple as possible, whilst generally being
   the cause for this graph system being ways too large in complex situations.
8. modifications on certain variables can cause a selection of edges to be removed. This class provides a
   'repair' function to recompute the closure.
9. return values cannot be used to create composite facts, see acceptForComposite, TestSharedVariables

 */
public final class IncrementalFixpointEngine<V, L> {
    private static final Logger LOGGER = LoggerFactory.getLogger(IncrementalFixpointEngine.class);

    private final LabeledGraph<V, L> graph;
    private final Closure<V, L> closure;
    private final WitnessIndex<V, L> witnessIndex;
    private final BinaryOperator<L> combine;
    private final BinaryOperator<L> best;
    private final Predicate<L> valid;
    private final UnaryOperator<L> reverse;
    private final Function<V, String> vertexPrinter;
    private final Comparator<V> vertexComparator;
    private final Predicate<V> acceptForComposite;

    public IncrementalFixpointEngine(BinaryOperator<L> combine,
                                     BinaryOperator<L> best,
                                     Predicate<L> valid,
                                     Function<L, Integer> scoreFunction,
                                     UnaryOperator<L> reverse,
                                     Function<V, String> vertexPrinter,
                                     Comparator<V> vertexComparator,
                                     Predicate<V> acceptForComposite) {
        this.graph = new LabeledGraph<>();
        this.closure = new Closure<>(best);
        this.witnessIndex = new WitnessIndex<>(scoreFunction, vertexComparator);
        this.combine = Objects.requireNonNull(combine);
        this.valid = Objects.requireNonNull(valid);
        this.best = Objects.requireNonNull(best);
        this.reverse = Objects.requireNonNull(reverse);
        this.vertexPrinter = vertexPrinter;
        this.vertexComparator = vertexComparator;
        this.acceptForComposite = acceptForComposite;
    }

    public Iterable<Map.Entry<V, L>> edges(V v) {
        return graph.successors(v);
    }

    public boolean isKnown(V from) {
        return graph.isKnown(from);
    }

    public int sizeOfClosure() {
        return closure.countFacts();
    }

    public int sizeOfWitnesses() {
        return witnessIndex.size();
    }

    public Stream<Map.Entry<V, L>> successorStream(V variable) {
        return closure.successorStream(variable);
    }

    public Iterable<Map.Entry<V, L>> successors(V variable) {
        return closure.successors(variable);
    }

    public Iterable<Map.Entry<V, Map<V, L>>> edges() {
        return graph.edges();
    }

    public String print() {
        return graph.print(vertexPrinter, vertexComparator);
    }

    public String print(Function<V, String> vertexPrinter, Comparator<V> comparator) {
        return graph.print(vertexPrinter, comparator);
    }

    public String printEdges() {
        return graph.printEdges(vertexComparator);
    }

    public String printClosure() {
        return closure.print(vertexPrinter, vertexComparator, witnessIndex);
    }

    public boolean addVertex(V v) {
        return graph.addVertex(v);
    }

    public boolean removeVertices(Set<V> vertices) {
        if (!Gate.isSet("NOMAT")) materializeWitnessOrphans(vertices);
        closure.removeVertices(vertices);
        return graph.removeVertices(vertices);
    }

    /*
    A closure fact between two SURVIVING vertices whose (transitive) witness support touches a vertex being
    removed would die in the next recompute cascade — and whether it dies depends on WHICH witness the
    derivation happened to record among equally-valid paths (order-dependent: nondeterministic output).
    Link facts describe object-graph relationships that outlive the local variable that established them
    ('target.§is ∩ collections.§iss' remains true after the loop variable goes out of scope). So before removal,
    promote such facts to raw graph edges with a direct witness, making them self-supporting. (The old engine
    achieved the same by re-materializing derived links into each statement's graph via VariableData.)
    Deliberate fact-removal flows (replaceReturnAffected: ⊆→~ after modification) do not pass through
    removeVertices, so they are unaffected.
     */
    private void materializeWitnessOrphans(Set<V> dead) {
        List<Fact<V, L>> orphans = new ArrayList<>();
        closure.factStream().forEach(fact -> {
            if (dead.contains(fact.source()) || dead.contains(fact.target())) return;
            Witness<V, L> w = witnessIndex.get(fact);
            if (!(w instanceof Witness.CompositeWitness<V, L>)) return;
            boolean orphaned = w.support().stream().anyMatch(f ->
                    dead.contains(f.source()) || dead.contains(f.target()));
            if (orphaned) orphans.add(fact);
        });
        for (Fact<V, L> fact : orphans) {
            // The closure's two directions derive independently (feature #1: rev(combined) != combine(reversed))
            // and may hold different-strength labels (∩ one way, ~ the other). The consistency invariant demands
            // symmetric coherence of graph edges: closure(v,u) >= reverse(graph(u,v)). Facts are semantically
            // symmetric-by-reverse (features #2/3), so upgrade both directions to the strongest coherent label.
            L liveReverse = closure.label(fact.target(), fact.source());
            L strongest = liveReverse == null ? fact.label() : best.apply(fact.label(), reverse.apply(liveReverse));
            L strongestReverse = reverse.apply(strongest);
            graph.addSymmetricEdge(fact.source(), fact.target(), strongest, strongestReverse);
            closure.add(fact.source(), fact.target(), strongest);
            closure.add(fact.target(), fact.source(), strongestReverse);
            witnessIndex.putIfBetter(new Fact<>(fact.source(), fact.target(), strongest),
                    new Witness.DirectWitness<>(new Fact<>(fact.source(), fact.target(), strongest), "mat"));
            witnessIndex.putIfBetter(new Fact<>(fact.target(), fact.source(), strongestReverse),
                    new Witness.DirectWitness<>(new Fact<>(fact.target(), fact.source(), strongestReverse), "mat"));
        }
    }

    public boolean isValid(L label) {
        return valid.test(label);
    }

    public int addSymmetricEdge(V from, V to, L label, String statementIndex) {
        assert valid.test(label) : "invalid label " + label + " on edge " + from + " -> " + to
                                   + " at statement " + statementIndex;
        L reverseLabel = reverse.apply(label);
        graph.addSymmetricEdge(from, to, label, reverseLabel);
        return incrementalUpdate(Set.of(new Fact<>(from, to, label), new Fact<>(to, from, reverseLabel)),
                statementIndex);
    }

    private int incrementalUpdate(Collection<Fact<V, L>> seeds, String statementIndex) {
        int newFacts = 0;
        Deque<Fact<V, L>> queue = new ArrayDeque<>();
        for (Fact<V, L> fact : seeds) {
            boolean added = closure.add(fact.source(), fact.target(), fact.label());
            // a seed IS a graph edge: register its direct witness even when the fact was already derived
            // (closure.add false). A direct witness beats any composite (putIfBetter), makes the fact robust
            // against removal of the intermediates the composite depended on, and makes witness choice
            // independent of whether the direct edge arrived before or after the derivation.
            witnessIndex.putIfBetter(fact, new Witness.DirectWitness<>(fact, statementIndex));
            if (added) {
                newFacts++;
                queue.add(fact);
            }
        }
        if (queue.isEmpty()) {
            return 0;
        }
        LOGGER.debug("New information, statement {}", statementIndex);
        Deque<Fact<V, L>> queueCopy = new ArrayDeque<>(queue);

        while (!queue.isEmpty()) {
            Fact<V, L> fact = queue.removeFirst();
            if (LOGGER.isDebugEnabled()) LOGGER.debug("-- inference phase: process {}", fact.print(vertexPrinter));
            propagateForward(fact, queue, false, null);
            propagateBackward(fact, queue, false, null);
        }
        // see e.g. TestDependent,2,3 -- they need this optimization phase
        Set<Fact<V, L>> history = new HashSet<>();
        while (!queueCopy.isEmpty()) {
            Fact<V, L> fact = queueCopy.removeFirst();
            if (history.add(fact)) {
                if (LOGGER.isDebugEnabled()) LOGGER.debug("-- optimization phase: process {}", fact.print(vertexPrinter));
                propagateForward(fact, queueCopy, true, history);
                propagateBackward(fact, queueCopy, true, history);
            }
        }
        LOGGER.debug("End of update, statement {}", statementIndex);
        assert consistencyCheckSucceeds();
        return newFacts;
    }

    private void propagateForward(Fact<V, L> fact, Deque<Fact<V, L>> queue, boolean optimize, Set<Fact<V, L>> history) {
        Iterable<Map.Entry<V, L>> successors = optimize
                ? closure.successors(fact.target())
                : graph.successors(fact.target());
        for (Map.Entry<V, L> edge : successors) {
            L nextLabel = combine.apply(fact.label(), edge.getValue());
            V source = fact.source();
            V target = edge.getKey();
            if (acceptForComposite.test(target) && !source.equals(target) && valid.test(nextLabel)) {
                Fact<V, L> next = new Fact<>(source, target, nextLabel);
                if (history == null || addToHistory(history, next)) {
                    Fact<V, L> newFact = new Fact<>(fact.target(), target, edge.getValue());
                    Witness<V, L> leftW = witnessIndex.get(fact);
                    Witness<V, L> rightW = witnessIndex.get(newFact);
                    if (leftW != null && rightW != null && doesNotCreateCycle(next, leftW, rightW)) {
                        Witness.CompositeWitness<V, L> candidate = Witness.CompositeWitness.of(leftW, rightW, fact,
                                newFact, !optimize);
                        boolean improved = witnessIndex.putIfBetter(next, candidate);
                        boolean added = closure.add(next.source(), next.target(), next.label());
                        if (added || improved) {
                            if (LOGGER.isDebugEnabled()) LOGGER.debug(" -- -- forward, {} {} {} witness {}",
                                    next.print(vertexPrinter),
                                    added ? "added" : "",
                                    improved ? "improved" : "",
                                    candidate.print(vertexPrinter));
                            queue.addLast(next);
                            if (added) completeSymmetrically(next, fact, newFact, candidate, queue, optimize);
                        }
                    }
                }
            }
        }
    }

    /*
    Facts are semantically symmetric-by-reverse (features #2/3; materializeWitnessOrphans already upgrades both
    directions on removal). But the closure's two directions DERIVE independently, so whether the mirror of a
    derived fact exists depended on whether the insertion order happened to enable its own derivation path — a
    fact-level order dependence (the m∩copy flake: 'copy ∩ list' derivable in one order only). Complete the
    mirror immediately. Its witness is the naturally-oriented reversed composition (rev(f∘g) = rev(g)∘rev(f)),
    whose sub-facts exist because edges and mirrors are themselves symmetric — so witness choice stays canonical
    across insertion orders (the diamond pin). acceptForComposite guards feature #9 (no composites TARGETING a
    return variable or a someValue marker).
     */
    private void completeSymmetrically(Fact<V, L> next, Fact<V, L> left, Fact<V, L> right,
                                       Witness<V, L> fallback, Deque<Fact<V, L>> queue, boolean optimize) {
        L revLabel = reverse.apply(next.label());
        if (!valid.test(revLabel) || !acceptForComposite.test(next.source())) return;
        Fact<V, L> mirror = new Fact<>(next.target(), next.source(), revLabel);
        boolean added = closure.add(mirror.source(), mirror.target(), mirror.label());
        if (added) {
            Fact<V, L> revRight = new Fact<>(right.target(), right.source(), reverse.apply(right.label()));
            Fact<V, L> revLeft = new Fact<>(left.target(), left.source(), reverse.apply(left.label()));
            Witness<V, L> leftW = witnessIndex.get(revRight);
            Witness<V, L> rightW = witnessIndex.get(revLeft);
            Witness<V, L> witness = leftW != null && rightW != null
                    ? Witness.CompositeWitness.of(leftW, rightW, revRight, revLeft, !optimize)
                    : fallback;
            witnessIndex.putIfBetter(mirror, witness);
            if (LOGGER.isDebugEnabled()) LOGGER.debug(" -- -- symmetric completion, {}", mirror.print(vertexPrinter));
            queue.addLast(mirror);
        }
    }

    private boolean addToHistory(Set<Fact<V, L>> history, Fact<V, L> fact) {
        return history.add(fact) && (!reverse.apply(fact.label()).equals(fact.label())
                                     || history.add(new Fact<>(fact.target(), fact.source(), fact.label())));
    }

    private boolean doesNotCreateCycle(Fact<V, L> target, Witness<V, L> left, Witness<V, L> right) {
        return !left.support().contains(target) && !right.support().contains(target);
    }

    private void propagateBackward(Fact<V, L> fact, Deque<Fact<V, L>> queue, boolean optimize, Set<Fact<V, L>> history) {
        V source = fact.source();
        for (Map.Entry<V, L> edge : closure.successors(source)) {
            V p = edge.getKey();
            V target = fact.target();
            if (acceptForComposite.test(target) && !p.equals(source) && !p.equals(target)) {
                L label = edge.getValue();
                L predLabel = reverse.apply(label); // because we're following the successors!
                L combined = combine.apply(predLabel, fact.label());
                if (valid.test(combined)) {
                    Fact<V, L> next = new Fact<>(p, target, combined);
                    if (history == null || addToHistory(history, next)) {
                        Fact<V, L> newFact = new Fact<>(p, source, predLabel);
                        Witness<V, L> leftW = witnessIndex.get(newFact);
                        Witness<V, L> rightW = witnessIndex.get(fact);
                        if (leftW != null && rightW != null && doesNotCreateCycle(next, leftW, rightW)) {
                            Witness.CompositeWitness<V, L> candidate = Witness.CompositeWitness.of(leftW, rightW, newFact,
                                    fact, !optimize);
                            // (the doesNotCreateCycle check above IS 'candidate.support() does not contain next';
                            // asserting it again would materialize every candidate's lazy support)
                            boolean added = closure.add(next.source(), next.target(), next.label());
                            boolean improved = witnessIndex.putIfBetter(next, candidate);
                            if (added || improved) {
                                if (LOGGER.isDebugEnabled()) LOGGER.debug(" -- -- backward, {} {} {} witness {}",
                                        added ? "added" : "",
                                        improved ? "improved" : "",
                                        next.print(vertexPrinter), candidate.print(vertexPrinter));
                                queue.addLast(next);
                                if (added) completeSymmetrically(next, newFact, fact, candidate, queue, optimize);
                            }
                        }
                    }
                }
            }
        }
    }

    // meant for assertions only!
    boolean consistencyCheckSucceeds() {
        boolean success = true;
        for (Map.Entry<V, Map<V, L>> entry : graph.edges()) {
            for (Map.Entry<V, L> entry2 : entry.getValue().entrySet()) {
                if (singleEdgeConsistencyCheckFails(entry.getKey(), entry2.getKey(), entry2.getValue())) {
                    success = false;
                    // keep going to produce more error messages
                }
                if (singleEdgeConsistencyCheckFails(entry2.getKey(), entry.getKey(), reverse.apply(entry2.getValue()))) {
                    success = false;
                }
            }
        }
        return success;
    }

    private boolean singleEdgeConsistencyCheckFails(V from, V to, L label) {
        L inClosure = closure.label(from, to);
        if (inClosure == null) {
            LOGGER.error("Not in closure: {} {} {}", from, label, to);
            return true;
        }
        if (!betterThanOrEqual(inClosure, label)) {
            LOGGER.error("Worse! {} {} {} but have {} in closure", from, label, to, inClosure);
            return true;
        }
        Fact<V, L> fact = new Fact<>(from, to, label);
        Witness<V, L> witness = witnessIndex.get(fact);
        // edges of the label graph must be witnessed as such
        if (witness instanceof Witness.DirectWitness<V, L> dw) {
            boolean different = !(dw.fact().equals(fact));
            if (different) {
                LOGGER.error("Direct witness different! {} vs {} {} {}", dw, from, label, to);
            }
            return different;
        }
        return false;
    }

    private boolean betterThanOrEqual(L l1, L l2) {
        assert l1 != null;
        assert l2 != null;
        return l1.equals(best.apply(l1, l2));
    }

    public Set<V> replaceReturnAffected(V from, V to, L currentLabel, L newLabel, String skipStatementIndex,
                                        Predicate<Fact<V, L>> acceptRaw) {
        return replaceReturnAffected(new Fact<>(from, to, currentLabel), newLabel, reverse.apply(newLabel),
                skipStatementIndex, acceptRaw, new HashSet<>());
    }

    private Set<V> replaceReturnAffected(Fact<V, L> fact, L newLabel, L reverseNewLabel, String skipStatementIndex,
                                         Predicate<Fact<V, L>> acceptRaw, Set<Fact<V, L>> visited) {
        // the witness DAG is meant to be acyclic, but on deeply self-referential structures it can contain a cycle
        // (io.codelaser...parseq); guard against re-entering a fact to avoid unbounded recursion. Fact equality is
        // on source+target, and graph.replace is idempotent, so skipping an already-visited fact is safe.
        if (!visited.add(fact)) return Set.of();
        Witness<V, L> witness = witnessIndex.get(fact);
        if (witness instanceof Witness.DirectWitness<V, L> dw) {
            // a raw edge inserted at skipStatementIndex is the POST-STATE of the very evaluation whose
            // modification triggered this replacement ('1:rr.§$s ⊇ 0:in.§$s' established by the modifying
            // 'rr.embed(in)' itself) — not invalidated knowledge; leave it in place. acceptRaw restricts the
            // rewrite to raw edges the modification actually owns (a composite entailed by intact edges of
            // OTHER variables is not invalidated; see WriteLinksAndModification, gate NOFLIPOWN).
            if (skipStatementIndex != null && skipStatementIndex.equals(dw.statementIndex())) return Set.of();
            if (!acceptRaw.test(fact)) return Set.of();
            return graph.replace(fact.source(), fact.target(), newLabel, reverseNewLabel)
                    ? Set.of(fact.source(), fact.target()) : Set.of();
        }
        if (witness instanceof Witness.CompositeWitness<V, L> cw) {
            Fact<V, L> left = cw.left();
            Fact<V, L> right = cw.right();
            Set<V> set1 = left.label().equals(fact.label()) ? replaceReturnAffected(left, newLabel, reverseNewLabel, skipStatementIndex, acceptRaw, visited) : Set.of();
            Set<V> set2 = right.label().equals(fact.label()) ? replaceReturnAffected(right, newLabel, reverseNewLabel, skipStatementIndex, acceptRaw, visited) : Set.of();
            return Stream.concat(Stream.concat(Stream.of(fact.source(), fact.target()), set1.stream()), set2.stream())
                    .collect(Collectors.toUnmodifiableSet());
        }
        return Set.of();
    }

    public void recompute(Set<V> affected, String statementIndex, Predicate<Fact<V, L>> acceptRemoval) {
        // remove affected region
        Set<V> remove = new HashSet<>(affected);
        while (true) {
            List<Fact<V, L>> removedFacts = closure.removeFacts(remove, acceptRemoval);
            Set<V> extra = witnessIndex.removeFacts(remove, removedFacts);
            if (!remove.addAll(extra)) break;
        }
        // rebuild it. NOTE: seed from the full 'remove' set, not just 'affected': removeFacts above grows 'remove'
        // to include vertices ('extra') whose facts were removed because their witnesses depended on removed facts.
        // Re-seeding only 'affected' left those extra vertices' base edges out of the closure (a base edge present
        // in the graph but missing from the closure -- consistencyCheck failure on '$__sv_ ⊇ $__rv.§ts').
        List<Fact<V, L>> seeds = new ArrayList<>();
        for (V u : remove) {
            for (var edge : graph.successors(u)) {
                seeds.add(new Fact<>(u, edge.getKey(), edge.getValue()));
                seeds.add(new Fact<>(edge.getKey(), u, reverse.apply(edge.getValue())));
            }
        }
        incrementalUpdate(seeds, statementIndex);
    }

    public Set<V> vertices() {
        return graph.vertices();
    }
}