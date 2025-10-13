/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.e2immu.util.internal.graph;

import java.util.*;
import java.util.function.Function;
import java.util.function.LongBinaryOperator;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface G<T> {

    G<T> immutableCopy();

    Map<V<T>, Long> removeVertex(T t);

    Map<V<T>, Long> ensureVertex(T t);

    void mergeEdge(T from, T to, long weight);

    G<T> reverse(Predicate<T> predicate);

    default int size() {
        return vertices().size();
    }

    V<T> vertex(T t);

    interface Builder<T> {
        void addAll(Builder<T> other);

        void addVertex(T t);

        Map<T, Long> ensureVertex(T t);

        Map<T, Long> edges(T t);

        void mergeEdge(T from, T to, long weight);

        G<T> build();

        Iterable<Map.Entry<T, Map<T, Long>>> edges();

        void add(T from, Iterable<? extends T> tos);

        int size();
    }

    String toString(String delimiter);

    String toString(String delimiter, Function<Long, String> edgeValuePrinter);

    G<T> withFewerEdges(Map<V<T>, Set<V<T>>> edgesToRemove);

    G<T> withFewerEdgesMap(Map<V<T>, Map<V<T>, Long>> edgesToRemove);

    G<T> subGraph(Set<V<T>> subSet);

    G<T> subGraph(Set<V<T>> subSet, Predicate<Long> acceptEdgePredicate);

    G<T> mutableReverseSubGraph(Set<V<T>> subSet, LongBinaryOperator sum);

    Collection<V<T>> vertices();

    Map<V<T>, Long> edges(V<T> v);

    Iterable<Map.Entry<V<T>, Map<V<T>, Long>>> edges();

    Map<V<T>, Map<V<T>, Long>> edgeMap();

    Stream<E<T>> edgeStream();

    Iterator<Map<V<T>, Map<V<T>, Long>>> edgeIterator(Comparator<Long> comparator, Long limit);

    Map<V<T>, Long> incomingVertexWeight(LongBinaryOperator sum);
}
