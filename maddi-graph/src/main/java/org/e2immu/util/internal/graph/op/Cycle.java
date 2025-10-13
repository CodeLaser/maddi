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

import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record Cycle<T>(Set<V<T>> vertices) {
    public int size() {
        return vertices().size();
    }

    @Override
    public String toString() {
        return vertices.stream().map(V::toString).sorted().collect(Collectors.joining(", "));
    }

    public Stream<T> sortedStream(Comparator<T> comparator) {
        return vertices.stream().map(V::t).sorted(comparator);
    }

    public T first(Comparator<T> comparator) {
        return vertices().stream().map(V::t).min(comparator).orElseThrow();
    }

    public boolean contains(V<T> v) {
        return vertices.contains(v);
    }
}
