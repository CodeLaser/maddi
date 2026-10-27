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

package io.codelaser.maddi.graph.op;

import io.codelaser.maddi.graph.V;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record Cycles<T>(Set<Cycle<T>> cycles) implements Iterable<Cycle<T>> {

    public int maxCycleSize(Set<T> startingPoints) {
        Collection<V<T>> vertices = startingPoints.stream().map(V::new).collect(Collectors.toUnmodifiableSet());
        return cycles.stream()
                .filter(cycle -> !Collections.disjoint(cycle.vertices(), vertices))
                .mapToInt(Cycle::size).max().orElse(0);
    }

    public int maxCycleSize() {
        return cycles.stream().mapToInt(Cycle::size).max().orElse(0);
    }

    @Override
    public String toString() {
        return cycles.stream().map(cycle -> "[" + cycle + "]").sorted().collect(Collectors.joining("; "));
    }

    public Stream<T> sortedStream(Comparator<T> comparator) {
        CycleComparator cc = new CycleComparator(comparator);
        return cycles.stream().sorted(cc).flatMap(cycle -> cycle.sortedStream(comparator));
    }

    // used in JFocus
    public <S> Stream<S> sortedStream(Comparator<T> comparator, Function<T, S> addGroup) {
        CycleComparator cc = new CycleComparator(comparator);
        return cycles.stream().sorted(cc).flatMap(cycle -> cycle.sortedStream(comparator)).map(addGroup::apply);
    }


    public boolean isEmpty() {
        return cycles.isEmpty();
    }

    public int size() {
        return cycles.size();
    }

    @Override
    public Iterator<Cycle<T>> iterator() {
        return cycles.iterator();
    }

    class CycleComparator implements Comparator<Cycle<T>> {
        private final Comparator<T> comparator;

        CycleComparator(Comparator<T> comparator) {
            this.comparator = comparator;
        }

        @Override
        public int compare(Cycle<T> o1, Cycle<T> o2) {
            int c = o2.size() - o1.size();
            if (c != 0) return c;
            T t1 = o1.first(comparator);
            T t2 = o2.first(comparator);
            assert !t1.equals(t2); // cycles should be disjoint
            return comparator.compare(t1, t2);
        }
    }
}
