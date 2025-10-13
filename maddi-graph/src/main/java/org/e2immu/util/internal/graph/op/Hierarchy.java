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

package org.e2immu.util.internal.graph.op;

import org.e2immu.util.internal.graph.V;

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
