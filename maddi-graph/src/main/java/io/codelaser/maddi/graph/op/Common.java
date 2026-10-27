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

import io.codelaser.maddi.graph.G;
import io.codelaser.maddi.graph.V;

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
