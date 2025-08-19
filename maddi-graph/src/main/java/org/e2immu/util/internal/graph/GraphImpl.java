package org.e2immu.util.internal.graph;

import java.util.*;
import java.util.function.Function;
import java.util.function.LongBinaryOperator;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
This class uses linked maps and sets (as opposed to Map.copyOf, Set.copyOf, new HashMap(), etc.)
to behave in a consistent way across tests.
 */
public abstract class GraphImpl<T> implements G<T> {
    protected final Map<T, V<T>> vertices;
    protected final Map<V<T>, Map<V<T>, Long>> edges;

    protected GraphImpl(Map<T, V<T>> vertices,
                        Map<V<T>, Map<V<T>, Long>> edges) {
        this.vertices = vertices;
        this.edges = edges;
    }

    protected interface Factory<T> {
        G<T> create(Map<T, V<T>> vertices, Map<V<T>, Map<V<T>, Long>> edges);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(edges);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof G<?> g && edges.equals(g.edgeMap());
    }

    protected G<T> reverse(Predicate<T> predicate, Factory<T> factory) {
        Map<V<T>, Map<V<T>, Long>> newEdges = new LinkedHashMap<>();
        for (Map.Entry<V<T>, Map<V<T>, Long>> e : edges.entrySet()) {
            Map<V<T>, Long> localEdges = e.getValue();
            for (Map.Entry<V<T>, Long> entry : localEdges.entrySet()) {
                V<T> to = entry.getKey();
                if (predicate.test(to.t())) {
                    Map<V<T>, Long> newLocal = newEdges.computeIfAbsent(to, t -> new LinkedHashMap<>());
                    newLocal.put(e.getKey(), entry.getValue());
                }
            }
        }
        newEdges.replaceAll((k, v) -> Map.copyOf(v));
        return factory.create(vertices, newEdges);
    }

    @Override
    public V<T> vertex(T t) {
        return vertices.get(t);
    }

    @Override
    public String toString() {
        return toString(", ");
    }

    @Override
    public String toString(String delimiter) {
        return toString(delimiter, l -> "" + l);
    }

    @Override
    public String toString(String delimiter, Function<Long, String> edgeValuePrinter) {
        return edges.entrySet().stream()
                .flatMap(e -> e.getValue().entrySet().stream()
                        .map(e2 -> new E<>(e.getKey(), e2.getKey(), e2.getValue())))
                .map(e -> e.toString(edgeValuePrinter)).sorted().collect(Collectors.joining(delimiter));
    }

    protected G<T> withFewerEdges(Map<V<T>, Set<V<T>>> edgesToRemove, Factory<T> factory) {
        return internalWithFewerEdges(edgesToRemove::get, factory);
    }

    protected G<T> withFewerEdgesMap(Map<V<T>, Map<V<T>, Long>> edgesToRemove, Factory<T> factory) {
        return internalWithFewerEdges(v -> edgesToRemove.getOrDefault(v, Map.of()).keySet(), factory);
    }

    protected G<T> internalWithFewerEdges(Function<V<T>, Set<V<T>>> edgesToRemove, Factory<T> factory) {
        Map<V<T>, Map<V<T>, Long>> newEdges = new LinkedHashMap<>();
        for (Map.Entry<V<T>, Map<V<T>, Long>> entry : edges.entrySet()) {
            Set<V<T>> toRemove = edgesToRemove.apply(entry.getKey());
            Map<V<T>, Long> newEdgesOfV;
            if (toRemove != null && !toRemove.isEmpty()) {
                Map<V<T>, Long> map = new LinkedHashMap<>(entry.getValue());
                map.keySet().removeAll(toRemove);
                newEdgesOfV = map;
            } else {
                newEdgesOfV = entry.getValue();
            }
            if (newEdgesOfV.isEmpty()) {
                newEdges.remove(entry.getKey());
            } else {
                newEdges.put(entry.getKey(), newEdgesOfV);
            }
        }
        return factory.create(vertices, newEdges);
    }

    protected G<T> subGraph(Set<V<T>> subSet, Factory<T> factory) {
        return subGraph(subSet, null, factory);
    }

    protected G<T> subGraph(Set<V<T>> subSet, Predicate<Long> acceptEdgePredicate, Factory<T> factory) {
        Map<T, V<T>> subMap = new LinkedHashMap<>();
        Map<V<T>, Map<V<T>, Long>> newEdges = new LinkedHashMap<>();
        for (V<T> v : subSet) {
            Map<V<T>, Long> localEdges = edges.get(v);
            if (localEdges != null) {
                Map<V<T>, Long> newLocal = new LinkedHashMap<>();
                for (Map.Entry<V<T>, Long> entry : localEdges.entrySet()) {
                    V<T> to = entry.getKey();
                    if ((acceptEdgePredicate == null || acceptEdgePredicate.test(entry.getValue())) && subSet.contains(to)) {
                        newLocal.put(to, entry.getValue());
                    }
                }
                if (!newLocal.isEmpty()) {
                    newEdges.put(v, newLocal);
                }
            }
            subMap.put(v.t(), v);
        }
        return factory.create(subMap, newEdges);
    }


    protected G<T> internalMutableReverseSubGraph(Set<V<T>> subSet, Factory<T> factory) {
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
        return factory.create(subMap, newEdges);
    }

    @Override
    public Collection<V<T>> vertices() {
        return vertices.values();
    }

    @Override
    public Map<V<T>, Long> edges(V<T> v) {
        assert v != null;
        return edges.get(v);
    }

    @Override
    public Iterable<Map.Entry<V<T>, Map<V<T>, Long>>> edges() {
        return () -> edges.entrySet().iterator();
    }

    @Override
    public Map<V<T>, Map<V<T>, Long>> edgeMap() {
        return edges;
    }

    @Override
    public Stream<E<T>> edgeStream() {
        return edges.entrySet().stream()
                .flatMap(e -> e.getValue().entrySet().stream()
                        .map(e2 -> new E<>(e.getKey(), e2.getKey(), e2.getValue())));
    }

    @Override
    public Iterator<Map<V<T>, Map<V<T>, Long>>> edgeIterator(Comparator<Long> comparator, Long limit) {
        List<E<T>> edges = edgeStream()
                .filter(e -> limit == null || e.weight() < limit)
                .sorted((e1, e2) -> comparator.compare(e1.weight(), e2.weight()))
                .toList();
        return edges.stream().map(e -> Map.of(e.from(), Map.of(e.to(), e.weight()))).iterator();
    }

    @Override
    public Map<V<T>, Long> incomingVertexWeight(LongBinaryOperator sum) {
        Map<V<T>, Long> map = new HashMap<>();
        for (Map<V<T>, Long> targets : edges.values()) {
            for (Map.Entry<V<T>, Long> entry : targets.entrySet()) {
                map.merge(entry.getKey(), entry.getValue(), sum::applyAsLong);
            }
        }
        return map;
    }
}
