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

import org.e2immu.util.internal.graph.G;
import org.e2immu.util.internal.graph.V;

import java.util.*;

public class Common {

    public static <T> Set<V<T>> follow(G<T> g, V<T> startingPoint) {
        return follow(g, List.of(startingPoint), true);
    }

    public static <T> Set<V<T>> follow(G<T> g, Collection<V<T>> startingPoints, boolean includeStartingPoints) {
        assert startingPoints != null;
        List<V<T>> toDo = new LinkedList<>(startingPoints);
        Set<V<T>> connected = includeStartingPoints ? new LinkedHashSet<>(startingPoints) : new LinkedHashSet<>();
        while (!toDo.isEmpty()) {
            V<T> v = toDo.removeFirst();
            Map<V<T>, Long> edges = g.edges(v);
            if (edges != null) {
                for (V<T> to : edges.keySet()) {
                    if (connected.add(to)) {
                        toDo.add(to);
                    }
                }
            }
        }
        return connected;
    }
}
