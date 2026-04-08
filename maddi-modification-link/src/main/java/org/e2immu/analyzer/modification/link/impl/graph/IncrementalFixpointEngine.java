package org.e2immu.analyzer.modification.link.impl.graph;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class IncrementalFixpointEngine<V, L> {

    private final LabeledGraph<V, L> graph;
    private final ClosureIndex<V, L> closureIndex;
    private final WitnessIndex<V, L> witnessIndex;
    private final BinaryOperator<L> combine;
    private final BinaryOperator<L> best;
    private final Predicate<L> valid;

    public IncrementalFixpointEngine(BinaryOperator<L> combine, BinaryOperator<L> best,
                                     Predicate<L> valid) {
        this.graph = new LabeledGraph<>();
        this.closureIndex = new ClosureIndex<>(best);
        this.witnessIndex = new WitnessIndex<>();
        this.combine = combine;
        this.valid = valid;
        this.best = best;
    }

    public L label(V from, V to) {
        return graph.successors(from).get(to);
    }

    public Stream<Map.Entry<V, L>> successorStream(V variable) {
        return closureIndex.successorStream(variable);
    }

    public Stream<Map.Entry<V, L>> successorsInGraphStream(V v) {
        return graph.successors(v).entrySet().stream();
    }

    public Iterable<Map.Entry<V, L>> successorsInGraph(V v) {
        return graph.successors(v).entrySet();
    }

    public Stream<Map.Entry<V, L>> predecessorsInGraphStream(V v) {
        return graph.predecessors(v).entrySet().stream();
    }

    public Iterable<Map.Entry<V, L>> successors(V variable) {
        return closureIndex.successors(variable);
    }

    public Iterable<Map.Entry<V, Map<V, L>>> edges() {
        return graph.edges();
    }

    public String print(Comparator<V> comparator) {
        return graph.print(comparator);
    }

    public String print(Function<V, String> vertexPrinter, Comparator<V> comparator) {
        return graph.print(vertexPrinter, comparator);
    }

    public String printEdges(Comparator<V> comparator) {
        return graph.printEdges(comparator);
    }

    public String printClosure(Comparator<V> vertexComparator) {
        return closureIndex.print(Object::toString, vertexComparator, witnessIndex);
    }

    public String printClosure(Function<V, String> vertexPrinter, Comparator<V> vertexComparator) {
        return closureIndex.print(vertexPrinter, vertexComparator, witnessIndex);
    }

    public boolean addVertex(V v) {
        return graph.addVertex(v);
    }

    public void removeVertex(V v) {
        removeVertices(Set.of(v));
    }

    public void removeVertices(Set<V> vertices) {
        graph.removeVertices(vertices);
        closureIndex.removeVertices(vertices);
    }

    public int addEdge(V from, V to, L label, String statementIndex) {
        assert valid.test(label);
        graph.addEdge(from, to, label);
        return incrementalUpdate(from, to, label, statementIndex);
    }

    private record FactW<V, L>(Fact<V, L> fact, Witness<V, L> witness, boolean force) {
    }

    private int incrementalUpdate(V from, V to, L label, String statementIndex) {
        assert !from.equals(to);

        Deque<FactW<V, L>> queue = new ArrayDeque<>();
        int newFacts = 0;

        FactW<V, L> seed = new FactW<>(new Fact<>(from, to, label),
                new Witness.DirectWitness<>(from, to, label, statementIndex), true);
        queue.add(seed);

        while (!queue.isEmpty()) {
            FactW<V, L> factW = queue.removeFirst();

            boolean added = closureIndex.add(factW.fact.source(), factW.fact.target(), factW.fact.label());
            if (added) newFacts++;
            if (added || factW.force) {
                witnessIndex.put(factW.fact, factW.witness);
                propagateForward(factW.fact, queue);
                propagateBackward(factW.fact, queue);
            }
        }
        // TODO this would be the place to remove redundant edges in the labelGraph
        //  (redundant with respect to the overall result in closureIndex)

        assert betterThanOrEqual(closureIndex.label(from, to), label);
        assert allEdgesOfLabelGraphAreInClosure();

        return newFacts;
    }

    boolean allEdgesOfLabelGraphAreInClosure() {
        for (Map.Entry<V, Map<V, L>> entry : graph.edges()) {
            for (Map.Entry<V, L> entry2 : entry.getValue().entrySet()) {
                L inClosure = closureIndex.label(entry.getKey(), entry2.getKey());
                if (inClosure == null || !betterThanOrEqual(inClosure, entry2.getValue())) {
                    return false;
                }
                Fact<V, L> fact = new Fact<>(entry.getKey(), entry2.getKey(), entry2.getValue());
                Witness<V, L> witness = witnessIndex.get(fact);
                // edges of the label graph must be witnessed as such
                if (!(witness instanceof Witness.DirectWitness<V, L>)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean betterThanOrEqual(L l1, L l2) {
        assert l1 != null;
        assert l2 != null;
        return l1.equals(best.apply(l1, l2));
    }

    private void propagateForward(Fact<V, L> fact, Deque<FactW<V, L>> queue) {
        V u = fact.source();
        V v = fact.target();
        L currentLabel = fact.label();

        for (Map.Entry<V, L> edge : graph.successors(v).entrySet()) {
            V w = edge.getKey();
            if (!v.equals(w) && !u.equals(w)) {
                L edgeLabel = edge.getValue();
                L newLabel = combine.apply(currentLabel, edgeLabel);
                if (valid.test(newLabel)) {
                    Fact<V, L> newFact = new Fact<>(u, w, newLabel);
                    Fact<V, L> newFact2 = new Fact<>(v, w, edgeLabel);
                    Witness.CompositeWitness<V, L> witness = new Witness.CompositeWitness<>(fact, newFact2);
                    queue.addLast(new FactW<>(newFact, witness, false));
                }
            }
        }
    }

    private void propagateBackward(Fact<V, L> fact, Deque<FactW<V, L>> queue) {
        V u = fact.source();
        V v = fact.target();
        L currentLabel = fact.label();

        for (Map.Entry<V, L> predEntry : closureIndex.predecessors(u)) {
            V p = predEntry.getKey();
            if (!p.equals(v) && !p.equals(u)) {
                L predLabel = predEntry.getValue();

                L newLabel = combine.apply(predLabel, currentLabel);
                if (valid.test(newLabel)) {
                    Fact<V, L> newFact = new Fact<>(p, v, newLabel);
                    Fact<V, L> newFact2 = new Fact<>(p, u, predLabel);
                    Witness.CompositeWitness<V, L> witness = new Witness.CompositeWitness<>(newFact2, fact);
                    queue.addLast(new FactW<>(newFact, witness, false));
                }
            }
        }
    }

    /*
    private int reduceLocally(Set<V> affected) {
        int removed = 0;

        for (V u : affected) {
            Map<V, L> successors =
                    new HashMap<>(graph.successors(u));

            for (Map.Entry<V, L> edge : successors.entrySet()) {
                V v = edge.getKey();
                L directLabel = edge.getValue();

                // Temporarily remove edge
                graph.removeEdge(u, v);

                boolean reconstructable = directLabel.equals(closureIndex.label(u, v))
                                          // without the 2nd clause: too aggressive; with the 2nd clause: too weak?
                                          && witnessIndex.get(new Fact<>(u, v, directLabel))
                                                  instanceof Witness.CompositeWitness<V, L>;

                if (reconstructable) {
                    removed++;
                } else {
                    graph.addEdge(u, v, directLabel);
                }
            }
        }

        return removed;
    }*/

    public Set<V> replaceReturnAffected(V from, V to, L currentLabel, L newLabel) {
        return replaceReturnAffected(new Fact<>(from, to, currentLabel), newLabel);
    }

    private Set<V> replaceReturnAffected(Fact<V, L> fact, L newLabel) {
        Witness<V, L> witness = witnessIndex.get(fact);
        if (witness instanceof Witness.DirectWitness<V, L>) {
            return graph.replace(fact.source(), fact.target(), newLabel)
                    ? Set.of(fact.source(), fact.target()) : Set.of();
        }
        if (witness instanceof Witness.CompositeWitness<V, L>(Fact<V, L> left, Fact<V, L> right)) {
            Set<V> set1 = left.label().equals(fact.label()) ? replaceReturnAffected(left, newLabel) : Set.of();
            Set<V> set2 = right.label().equals(fact.label()) ? replaceReturnAffected(right, newLabel) : Set.of();
            return Stream.concat(Stream.concat(Stream.of(fact.source(), fact.target()), set1.stream()), set2.stream())
                    .collect(Collectors.toUnmodifiableSet());
        }
        return Set.of();
    }

    public void recompute(Set<V> affected, String statementIndex) {
        closureIndex.removeVertices(affected);
        witnessIndex.removeVertices(affected);
        rebuildAffectedRegion(affected, statementIndex);
        assert allEdgesOfLabelGraphAreInClosure();
    }

    // TODO the core 'while' loop is identical to that in 'incrementalUpdate'
    private void rebuildAffectedRegion(Set<V> affected, String statementIndex) {
        Deque<FactW<V, L>> queue = new ArrayDeque<>();

        for (V u : affected) {
            for (var edge : graph.successors(u).entrySet()) {
                V v = edge.getKey();
                L label = edge.getValue();
                queue.add(new FactW<>(new Fact<>(u, v, label),
                        new Witness.DirectWitness<>(u, v, label, statementIndex), true));
            }
            for (var edge : graph.predecessors(u).entrySet()) {
                V v = edge.getKey();
                L label = edge.getValue();
                queue.add(new FactW<>(new Fact<>(v, u, label),
                        new Witness.DirectWitness<>(v, u, label, statementIndex), true));
            }
        }

        while (!queue.isEmpty()) {
            FactW<V, L> factW = queue.removeFirst();

            if (closureIndex.add(factW.fact.source(), factW.fact.target(), factW.fact.label())) {
                witnessIndex.put(factW.fact, factW.witness);
                propagateForward(factW.fact, queue);
                propagateBackward(factW.fact, queue);
            }
        }
    }

    public Set<V> vertices() {
        return graph.vertices();
    }
}