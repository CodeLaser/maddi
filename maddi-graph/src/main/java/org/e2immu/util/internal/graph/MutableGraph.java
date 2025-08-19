package org.e2immu.util.internal.graph;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class MutableGraph<T> extends GraphImpl<T> implements G<T> {

    public MutableGraph() {
        super(new HashMap<>(), new HashMap<>());
    }

    protected MutableGraph(Map<T, V<T>> vertices, Map<V<T>, Map<V<T>, Long>> edges) {
        super(vertices, edges);
    }

    @Override
    public G<T> reverse(Predicate<T> predicate) {
        return reverse(predicate, MutableGraph::new);
    }

    @Override
    public G<T> withFewerEdges(Map<V<T>, Set<V<T>>> edgesToRemove) {
        return internalWithFewerEdges(edgesToRemove::get, MutableGraph::new);
    }

    @Override
    public G<T> withFewerEdgesMap(Map<V<T>, Map<V<T>, Long>> edgesToRemove) {
        return internalWithFewerEdges(v -> edgesToRemove.getOrDefault(v, Map.of()).keySet(),
                MutableGraph::new);
    }

    @Override
    public G<T> subGraph(Set<V<T>> subSet) {
        return subGraph(subSet, null, MutableGraph::new);
    }

    @Override
    public G<T> subGraph(Set<V<T>> subSet, Predicate<Long> acceptEdgePredicate) {
        return subGraph(subSet, acceptEdgePredicate, MutableGraph::new);
    }

    @Override
    public G<T> mutableReverseSubGraph(Set<V<T>> subSet) {
        return mutableReverseSubGraph(subSet, MutableGraph::new);
    }
}
