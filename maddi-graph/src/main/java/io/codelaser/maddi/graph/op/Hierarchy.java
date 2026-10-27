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
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
Leaves are in list.getFirst()
Vertices with an edge to leaves only are in list.get(1)
Vertices with edges to those in list.getFirst() and list.get(1) are in list.get(2), etc.
 */
public record Hierarchy<T>(List<Set<V<T>>> list) {
    @Override
    public String toString() {
        return list.stream()
                .map(s -> "[" + s.stream().map(V::toString).sorted().collect(Collectors.joining(", ")) + "]")
                .collect(Collectors.joining("; "));
    }

    public Stream<T> sortedStream(Comparator<T> comparator) {
        return list.stream().flatMap(set -> set.stream().map(V::t).sorted(comparator));
    }

    // used in JFocus
    public <S> Stream<S> sortedStream(Comparator<T> comparator, BiFunction<T, Integer, S> addGroup) {
        int group = 0;
        Stream<S> concat = Stream.of();
        for (Set<V<T>> set : list) {
            int fGroup = group;
            Stream<S> s = set.stream().map(V::t).sorted(comparator).map(t -> addGroup.apply(t, fGroup));
            concat = Stream.concat(concat, s);
            group++;
        }
        return concat;
    }

    public void append(Hierarchy<T> other) {
        list.addAll(other.list);
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public int size() {
        return list.size();
    }

    public Hierarchy<T> reversed() {
        List<Set<V<T>>> reverse = new ArrayList<>(list.size());
        ListIterator<Set<V<T>>> iterator = list.listIterator(list.size());
        while (iterator.hasPrevious()) {
            reverse.add(iterator.previous());
        }
        return new Hierarchy<>(reverse);
    }
}
