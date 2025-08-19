package org.e2immu.util.internal.graph;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.LongBinaryOperator;
import java.util.function.Predicate;

/*
This class uses linked maps and sets (as opposed to Map.copyOf, Set.copyOf, new HashMap(), etc.)
to behave in a consistent way across tests.
 */
public class ImmutableGraph<T> extends GraphImpl<T> implements G<T> {

    public static final String IMMUTABLE_GRAPH = "Immutable graph";

    private ImmutableGraph(Map<T, V<T>> vertices,
                           Map<V<T>, Map<V<T>, Long>> edges) {
        super(vertices, edges);
    }

    public static <T> ImmutableGraph<T> create(Map<T, Map<T, Long>> initialGraph) {
        Map<T, V<T>> vertices = new LinkedHashMap<>();
        Map<V<T>, Map<V<T>, Long>> edges = new LinkedHashMap<>();
        for (T t : initialGraph.keySet()) {
            V<T> v = new V<>(t);
            vertices.put(t, v);
        }
        for (Map.Entry<T, Map<T, Long>> entry : initialGraph.entrySet()) {
            V<T> from = vertices.get(entry.getKey());
            for (Map.Entry<T, Long> e2 : entry.getValue().entrySet()) {
                V<T> to = vertices.get(e2.getKey());
                assert to != null;
                edges.computeIfAbsent(from, f -> new LinkedHashMap<>()).put(to, e2.getValue());
            }
        }
        return new ImmutableGraph<>(vertices, edges);
    }

    @Override
    public G<T> reverse(Predicate<T> predicate) {
        return reverse(predicate, (v, n) -> new ImmutableGraph<T>(v, Map.copyOf(n)));
    }

    // based on a map of T elements
    public static class Builder<T> implements G.Builder<T> {
        private final LongBinaryOperator sum;

        public Builder(LongBinaryOperator sum) {
            this.sum = sum;
        }

        Map<T, Map<T, Long>> map = new LinkedHashMap<>();

        @Override
        public void addAll(G.Builder<T> other) {
            ((ImmutableGraph.Builder<T>) other).map.forEach((from, m) -> {
                Map<T, Long> m2 = ensureVertex(from);
                m.forEach((to, value) -> m2.merge(to, value, Long::sum));
            });
        }

        @Override
        public void addVertex(T t) {
            ensureVertex(t);
        }

        @Override
        public Map<T, Long> ensureVertex(T t) {
            assert t != null;
            return map.computeIfAbsent(t, f -> new LinkedHashMap<>());
        }

        @Override
        public Map<T, Long> edges(T t) {
            return map.get(t);
        }

        @Override
        public void mergeEdge(T from, T to, long weight) {
            ensureVertex(to);
            ensureVertex(from).merge(to, weight, sum::applyAsLong);
        }

        @Override
        public ImmutableGraph<T> build() {
            return create(map);
        }

        @Override
        public Iterable<Map.Entry<T, Map<T, Long>>> edges() {
            return () -> map.entrySet().iterator();
        }

        @Override
        public void add(T from, Iterable<? extends T> tos) {
            Map<T, Long> m = ensureVertex(from);
            tos.forEach(to -> {
                ensureVertex(to);
                m.merge(Objects.requireNonNull(to), 1L, Long::sum);
            });
        }
    }


    @Override
    public G<T> withFewerEdges(Map<V<T>, Set<V<T>>> edgesToRemove) {
        return internalWithFewerEdges(edgesToRemove::get, ImmutableGraph::new);
    }

    @Override
    public G<T> withFewerEdgesMap(Map<V<T>, Map<V<T>, Long>> edgesToRemove) {
        return internalWithFewerEdges(v -> edgesToRemove.getOrDefault(v, Map.of()).keySet(),
                ImmutableGraph::new);
    }

    @Override
    public G<T> subGraph(Set<V<T>> subSet) {
        return subGraph(subSet, null, ImmutableGraph::new);
    }

    @Override
    public G<T> subGraph(Set<V<T>> subSet, Predicate<Long> acceptEdgePredicate) {
        return subGraph(subSet, acceptEdgePredicate, ImmutableGraph::new);
    }

    @Override
    public G<T> mutableReverseSubGraph(Set<V<T>> subSet, LongBinaryOperator sum) {
        Map<T, V<T>> subMap = new LinkedHashMap<>();
        Map<V<T>, Map<V<T>, Long>> newEdges = new LinkedHashMap<>();
        for (V<T> v : subSet) {
            Map<V<T>, Long> localEdges = edges.get(v);
            if (localEdges != null) {
                for (Map.Entry<V<T>, Long> entry : localEdges.entrySet()) {
                    V<T> to = entry.getKey();
                    Map<V<T>, Long> newLocal = newEdges.computeIfAbsent(to, t -> new LinkedHashMap<>());
                    newLocal.put(v, entry.getValue());
                }
            }
            subMap.put(v.t(), v);
        }
        // freeze edge maps
        return new ImmutableGraph<T>(subMap, newEdges);
    }

    @Override
    public Map<V<T>, Long> removeVertex(T t) {
        throw new UnsupportedOperationException(IMMUTABLE_GRAPH);
    }

    @Override
    public Map<V<T>, Long> ensureVertex(T t) {
        throw new UnsupportedOperationException(IMMUTABLE_GRAPH);
    }

    @Override
    public void mergeEdge(T from, T to, long weight) {
        throw new UnsupportedOperationException(IMMUTABLE_GRAPH);
    }
}
