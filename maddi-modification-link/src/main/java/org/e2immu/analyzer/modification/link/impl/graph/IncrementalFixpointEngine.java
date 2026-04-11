package org.e2immu.analyzer.modification.link.impl.graph;

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

0. label function has a reverse and combine; these do not play well together
   e.g. rev(∋ + ∈) = rev(~) != rev(∋)+rev(∈) == X
   it also has a test for valid labels (X is never stored) and a score function (see 4)
1. LabeledGraph is symmetric: if a B c is present, then c rev(B) a is present as well
   we do not need to store the reverse graph for efficiency reasons
2. ClosureGraph is symmetric: if a B c is present, then c rev(B) a is present as well
   we do not need to store the reverse graph for efficiency reasons
3. The incremental fixpoint algorithm must have a forward and a backward phase, because the lack of operator symmetry.
   Each phase must do one direction at a time!
4. we prefer direct high value edges over lower value edges, e.g. ← + ∈ is preferred over ∈ + ~
   Their outcome is different!
   As a consequence, the forward propagation must go over the closure rather
   than the graph, because otherwise it cannot find the best combination (it will find 'a' combination).
   See TestDependent,2 for an example, where ∈? finally gets replaced by ∈ because of an already existing combination
   in the closure.
5. the Graph and MakeGraph classes try to keep this graph as simple as possible, whilst generally being
   the cause for this graph system being ways too large in complex situations.
6. modifications on certain variables can cause a selection of edges to be removed. This class provides a
   'repair' function to recompute the closure.


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

    public IncrementalFixpointEngine(BinaryOperator<L> combine,
                                     BinaryOperator<L> best,
                                     Predicate<L> valid,
                                     Function<L, Integer> scoreFunction,
                                     UnaryOperator<L> reverse,
                                     Function<V, String> vertexPrinter,
                                     Comparator<V> vertexComparator) {
        this.graph = new LabeledGraph<>();
        this.closure = new Closure<>(best);
        this.witnessIndex = new WitnessIndex<>(scoreFunction);
        this.combine = Objects.requireNonNull(combine);
        this.valid = Objects.requireNonNull(valid);
        this.best = Objects.requireNonNull(best);
        this.reverse = Objects.requireNonNull(reverse);
        this.vertexPrinter = vertexPrinter;
        this.vertexComparator = vertexComparator;
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

    public void removeVertices(Set<V> vertices) {
        graph.removeVertices(vertices);
        closure.removeVertices(vertices);
    }

    public int addSymmetricEdge(V from, V to, L label, String statementIndex) {
        assert valid.test(label);
        L reverseLabel = reverse.apply(label);
        graph.addSymmetricEdge(from, to, label, reverseLabel);
        return incrementalUpdate(Set.of(new Fact<>(from, to, label), new Fact<>(to, from, reverseLabel)),
                statementIndex);
    }

    private int incrementalUpdate(Collection<Fact<V, L>> seeds, String statementIndex) {
        int newFacts = 0;
        Deque<Fact<V, L>> queue = new ArrayDeque<>();
        for (Fact<V, L> fact : seeds) {
            if (closure.add(fact.source(), fact.target(), fact.label())) {
                newFacts++;
                witnessIndex.putIfBetter(fact,
                        new Witness.DirectWitness<>(fact.source(), fact.target(), fact.label(), statementIndex));
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
            LOGGER.debug("-- inference phase: process {}", fact.print(vertexPrinter));
            propagateForward(fact, queue, false, null);
            propagateBackward(fact, queue, false, null);
        }
        // see e.g. TestDependent,2,3 -- they need this optimization phase
        Set<Fact<V, L>> history = new HashSet<>();
        while (!queueCopy.isEmpty()) {
            Fact<V, L> fact = queueCopy.removeFirst();
            if (history.add(fact)) {
                LOGGER.debug("-- optimization phase: process {}", fact.print(vertexPrinter));
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
            if (!source.equals(target) && valid.test(nextLabel)) {
                Fact<V, L> next = new Fact<>(source, target, nextLabel);
                if (history == null || addToHistory(history, next)) {
                    Fact<V, L> newFact = new Fact<>(fact.target(), target, edge.getValue());
                    Witness<V, L> leftW = witnessIndex.get(fact);
                    Witness<V, L> rightW = witnessIndex.get(newFact);
                    if (leftW != null && rightW != null && doesNotCreateCycle(next, leftW, rightW)) {
                        Witness.CompositeWitness<V, L> candidate = Witness.CompositeWitness.of(leftW, rightW, fact,
                                newFact, !optimize);
                        boolean improved = witnessIndex.putIfBetter(next, candidate);
                        if (closure.add(next.source(), next.target(), next.label()) || improved) {
                            queue.addLast(next);
                        }
                    }
                }
            }
        }
    }

    private boolean addToHistory(Set<Fact<V, L>> history, Fact<V, L> fact) {
        return history.add(fact) && (!reverse.apply(fact.label()).equals(fact.label())
                                     || history.add(new Fact<>(fact.target(), fact.source(), fact.label())));
    }

    private boolean doesNotCreateCycle(Fact<V, L> target, Witness<V, L> left, Witness<V, L> right) {
        return (left == null || !left.support().contains(target))
               && (right == null || !right.support().contains(target));
    }

    private void propagateBackward(Fact<V, L> fact, Deque<Fact<V, L>> queue, boolean optimize, Set<Fact<V, L>> history) {
        V source = fact.source();
        for (Map.Entry<V, L> edge : closure.successors(source)) {
            V p = edge.getKey();
            V target = fact.target();
            if (!p.equals(source) && !p.equals(target)) {
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
                            boolean improved = witnessIndex.putIfBetter(next, candidate);
                            if (closure.add(next.source(), next.target(), next.label()) || improved) {
                                queue.addLast(next);
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
            boolean different = !(dw.from().equals(from)) || !(dw.to().equals(to)) || !dw.label().equals(label);
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

    public Set<V> replaceReturnAffected(V from, V to, L currentLabel, L newLabel) {
        return replaceReturnAffected(new Fact<>(from, to, currentLabel), newLabel, reverse.apply(newLabel));
    }

    private Set<V> replaceReturnAffected(Fact<V, L> fact, L newLabel, L reverseNewLabel) {
        Witness<V, L> witness = witnessIndex.get(fact);
        if (witness instanceof Witness.DirectWitness<V, L>) {
            return graph.replace(fact.source(), fact.target(), newLabel, reverseNewLabel)
                    ? Set.of(fact.source(), fact.target()) : Set.of();
        }
        if (witness instanceof Witness.CompositeWitness<V, L>(Fact<V, L> left, Fact<V, L> right, _, _)) {
            Set<V> set1 = left.label().equals(fact.label()) ? replaceReturnAffected(left, newLabel, reverseNewLabel) : Set.of();
            Set<V> set2 = right.label().equals(fact.label()) ? replaceReturnAffected(right, newLabel, reverseNewLabel) : Set.of();
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
        // rebuild it
        List<Fact<V, L>> seeds = new ArrayList<>();
        for (V u : affected) {
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