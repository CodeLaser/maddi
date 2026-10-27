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
                    // don't bother with small cycles: the path to 'to' has one more vertex than the current path
                    if (isStart && current.path.size() + 1 < minPathSize) continue;
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
