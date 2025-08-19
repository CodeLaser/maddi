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

    Stream<E<T>> edgeStream();

    Iterator<Map<V<T>, Map<V<T>, Long>>> edgeIterator(Comparator<Long> comparator, Long limit);

    Map<V<T>, Long> incomingVertexWeight(LongBinaryOperator sum);
}
