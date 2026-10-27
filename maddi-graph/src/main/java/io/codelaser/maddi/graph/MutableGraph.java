/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.graph;

import java.util.*;
import java.util.function.LongBinaryOperator;
import java.util.function.Predicate;

public class MutableGraph<T> extends GraphImpl<T> implements G<T> {
    private final LongBinaryOperator sum;

    public MutableGraph(LongBinaryOperator sum) {
        this(null, new LinkedHashMap<>(), sum);
    }

    public MutableGraph(Map<T, V<T>> vertices, Map<V<T>, Map<V<T>, Long>> edges, LongBinaryOperator sum) {
        super(vertices, edges);
        this.sum = sum;
    }

    private MutableGraph<T> factory(Map<T, V<T>> vertices, Map<V<T>, Map<V<T>, Long>> edges) {
        return new MutableGraph<>(vertices, edges, sum);
    }

    @Override
    public G<T> immutableCopy() {
        return ImmutableGraph.createFromMutable(edges);
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
