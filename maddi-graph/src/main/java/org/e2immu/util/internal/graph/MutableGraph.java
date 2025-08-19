package org.e2immu.util.internal.graph;

import java.util.*;
import java.util.function.LongBinaryOperator;
import java.util.function.Predicate;

public class MutableGraph<T> extends GraphImpl<T> implements G<T> {
    private final LongBinaryOperator sum;

    public MutableGraph(LongBinaryOperator sum) {
        this(null, new HashMap<>(), sum);
    }

    public MutableGraph(Map<T, V<T>> vertices, Map<V<T>, Map<V<T>, Long>> edges, LongBinaryOperator sum) {
        super(vertices, edges);
        this.sum = sum;
    }

    private MutableGraph<T> factory(Map<T, V<T>> vertices, Map<V<T>, Map<V<T>, Long>> edges) {
        return new MutableGraph<>(vertices, edges, sum);
    }

    @Override
    public V<T> vertex(T t) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<V<T>> vertices() {
        return edges.keySet();
    }

    @Override
    public G<T> reverse(Predicate<T> predicate) {
        return reverse(predicate, this::factory);
    }

    @Override
    public G<T> withFewerEdges(Map<V<T>, Set<V<T>>> edgesToRemove) {
        return internalWithFewerEdges(edgesToRemove::get, this::factory);
    }

    @Override
    public G<T> withFewerEdgesMap(Map<V<T>, Map<V<T>, Long>> edgesToRemove) {
        return internalWithFewerEdges(v -> edgesToRemove.getOrDefault(v, Map.of()).keySet(), this::factory);
    }

    @Override
    public G<T> subGraph(Set<V<T>> subSet) {
        return subGraph(subSet, null, this::factory);
    }

    @Override
    public G<T> subGraph(Set<V<T>> subSet, Predicate<Long> acceptEdgePredicate) {
        return subGraph(subSet, acceptEdgePredicate, this::factory);
    }

    @Override
    public G<T> mutableReverseSubGraph(Set<V<T>> subSet, LongBinaryOperator sum) {
        return internalMutableReverseSubGraph(subSet, this::factory);
    }

    // modification

    @Override
    public Map<V<T>, Long> removeVertex(T t) {
        assert t != null;
        return edges.remove(new V<>(t));
    }

    @Override
    public Map<V<T>, Long> ensureVertex(T t) {
        assert t != null;
        return edges.computeIfAbsent(new V<>(t), f -> new LinkedHashMap<>());
    }

    @Override
    public void mergeEdge(T from, T to, long weight) {
        ensureVertex(to);
        ensureVertex(from).merge(new V<>(to), weight, sum::applyAsLong);
    }

}
