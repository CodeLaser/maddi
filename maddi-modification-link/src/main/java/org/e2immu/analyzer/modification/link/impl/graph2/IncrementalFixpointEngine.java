package org.e2immu.analyzer.modification.link.impl.graph2;

import java.util.*;
import java.util.function.BinaryOperator;

public final class IncrementalFixpointEngine<V, L> {

    private final LabeledGraph<V, L> graph;
    private final ClosureIndex<V, L> closureIndex;
    private final WitnessIndex<V, L> witnessIndex;
    private final BinaryOperator<L> combine;

    public IncrementalFixpointEngine(BinaryOperator<L> combine) {
        this.graph = new LabeledGraph<>();
        this.closureIndex = new ClosureIndex<>();
        this.witnessIndex = new WitnessIndex<>();
        this.combine = combine;
    }

    public String print(Comparator<V> comparator) {
        return graph.print(comparator);
    }

    public String printClosure(Comparator<V> vertexComparator, Comparator<L> labelComparator) {
        return closureIndex.print(vertexComparator, labelComparator);
    }


    public void addVertex(V v) {
        graph.addVertex(v);
    }

    public UpdateResult<V> addEdge(V from, V to, L label) {
        graph.addEdge(from, to, label);
        return incrementalUpdate(from, to, label);
    }

    private UpdateResult<V> incrementalUpdate(V from, V to, L label) {

        Deque<Fact<V, L>> queue = new ArrayDeque<>();
        Set<V> affected = new HashSet<>();
        int newFacts = 0;

        Fact<V, L> seed = new Fact<>(from, to, label);
        queue.add(seed);

        while (!queue.isEmpty()) {
            Fact<V, L> fact = queue.removeFirst();

            if (closureIndex.add(fact.source(), fact.target(), fact.label())) {

                newFacts++;
                affected.add(fact.source());
                affected.add(fact.target());

                witnessIndex.put(fact, new Witness.DirectWitness<>(fact.source(), fact.target(), fact.label()));

                propagateForward(fact, queue);
                propagateBackward(fact, queue);
            }
        }

        int removed = reduceLocally(affected);

        return new UpdateResult<>(affected, newFacts, removed);
    }

    private void propagateForward(Fact<V, L> fact, Deque<Fact<V, L>> queue) {
        V u = fact.source();
        V v = fact.target();
        L currentLabel = fact.label();

        for (Map.Entry<V, L> edge : graph.successors(v).entrySet()) {
            V w = edge.getKey();
            L edgeLabel = edge.getValue();
            L newLabel = combine.apply(currentLabel, edgeLabel);
            Fact<V, L> newFact = new Fact<>(u, w, newLabel);
            witnessIndex.put(newFact, new Witness.CompositeWitness<>(fact, new Fact<>(v, w, edgeLabel)));
            queue.addLast(newFact);
        }
    }

    private void propagateBackward(Fact<V, L> fact, Deque<Fact<V, L>> queue) {

        V u = fact.source();
        V v = fact.target();
        L currentLabel = fact.label();

        for (Map.Entry<V, Set<L>> predEntry : closureIndex.predecessors(u).entrySet()) {
            V p = predEntry.getKey();

            for (L predLabel : predEntry.getValue()) {
                L newLabel = combine.apply(predLabel, currentLabel);
                Fact<V, L> newFact = new Fact<>(p, v, newLabel);

                witnessIndex.put(newFact, new Witness.CompositeWitness<>(new Fact<>(p, u, predLabel), fact));

                queue.addLast(newFact);
            }
        }
    }

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

                boolean reconstructable = closureIndex.labels(u, v).contains(directLabel)
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
    }

    public Set<L> query(V from, V to) {
        return closureIndex.labels(from, to);
    }

    public LabeledGraph<V, L> reducedGraph() {
        return graph;
    }
}