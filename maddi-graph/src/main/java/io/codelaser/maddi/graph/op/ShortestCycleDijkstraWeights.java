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
import java.util.stream.Stream;

public class ShortestCycleDijkstraWeights {

    /* the weight is the original weight in the graph */
    public record Dependency<T>(V<T> vertex, long weight) {
    }

    public record Cycle<T>(List<Dependency<T>> vertices, long distance) {
    }

    private record PathState<T>(V<T> vertex, long distance, List<Dependency<T>> path, V<T> firstDestination) {
        public boolean pathContains(V<T> to) {
            return path.stream().anyMatch(d -> to.equals(d.vertex));
        }
    }

    private record TwoVertices<T>(V<T> v1, V<T> v2) {
    }

    public static <T> Cycle<T> shortestCycle(G<T> graph, V<T> startVertex, int minCycleSize) {
        int minPathSize = minCycleSize + 1; // end vertex == begin vertex, so a cycle of 2 types is a path of 3
        PriorityQueue<PathState<T>> pq = new PriorityQueue<>(Comparator.comparing(ps -> ps.distance));
        Map<TwoVertices<T>, Long> visited = new HashMap<>();
        Cycle<T> shortest = null;
        for (Map.Entry<V<T>, Long> edge : graph.edges(startVertex).entrySet()) {
            List<Dependency<T>> initialPath = List.of(new Dependency<>(startVertex, 0L),
                    new Dependency<>(edge.getKey(), edge.getValue()));
            PathState<T> initialState = new PathState<>(edge.getKey(), edge.getValue(), initialPath, edge.getKey());
            pq.add(initialState);
        }
        while (!pq.isEmpty()) {
            PathState<T> current = pq.poll();
            TwoVertices<T> stateKey = new TwoVertices<>(current.vertex, current.firstDestination);
            Long inVisited = visited.get(stateKey);
            if (inVisited != null && inVisited <= current.distance) continue;
            visited.put(stateKey, current.distance);

            // Check if we've found a cycle back to start
            if (startVertex.equals(current.vertex) && current.path.size() >= minPathSize) {
                // we've found a cycle
                if (shortest == null || current.distance < shortest.distance) {
                    // no need to copy the path, it is immutable
                    shortest = new Cycle<>(current.path, current.distance);
                }
                // don't expand further
                continue;
            }
            // Try to expand
            Map<V<T>, Long> edges = graph.edges(current.vertex);
            if (edges != null) {
                for (Map.Entry<V<T>, Long> edge : edges.entrySet()) {
                    // ensure that we don't visit one of the vertices we've already visited; exception: start
                    V<T> to = edge.getKey();
                    boolean isStart = to.equals(startVertex);
                    if (!isStart && current.pathContains(to)) continue;
                    if (isStart && current.path.size() < minPathSize) continue; //don't bother with small cycles
                    long newDistance = current.distance + edge.getValue();
                    Dependency<T> d = new Dependency<>(to, edge.getValue());
                    List<Dependency<T>> newPath = Stream.concat(current.path.stream(), Stream.of(d)).toList();
                    PathState<T> newState = new PathState<>(to, newDistance, newPath, current.firstDestination);
                    pq.offer(newState);
                }
            }
        }
        return shortest;
    }
}
