package org.e2immu.util.internal.graph.op;

import org.e2immu.util.internal.graph.G;
import org.e2immu.util.internal.graph.V;

import java.util.*;
import java.util.stream.Stream;

public class ShortestCycleDijkstra {

    public record Cycle<T>(List<V<T>> vertices, long distance) {
    }

    private record PathState<T>(V<T> vertex, long distance, List<V<T>> path, V<T> firstDestination) {
    }

    private record TwoVertices<T>(V<T> v1, V<T> v2) {
    }

    private static final int MIN_CYCLE_SIZE = 3;

    public static <T> Cycle<T> shortestCycle(G<T> graph, V<T> startVertex) {
        PriorityQueue<PathState<T>> pq = new PriorityQueue<>(Comparator.comparing(ps -> ps.distance));
        Map<TwoVertices<T>, Long> visited = new HashMap<>();
        Cycle<T> shortest = null;
        for (Map.Entry<V<T>, Long> edge : graph.edges(startVertex).entrySet()) {
            List<V<T>> initialPath = List.of(startVertex, edge.getKey());
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
            if (startVertex.equals(current.vertex) && current.path.size() >= MIN_CYCLE_SIZE) {
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
            if(edges != null) {
                for (Map.Entry<V<T>, Long> edge : edges.entrySet()) {
                    // ensure that we don't visit one of the vertices we've already visited; exception: start
                    V<T> to = edge.getKey();
                    boolean isStart = to.equals(startVertex);
                    if (!isStart && current.path.contains(to)) continue;
                    if (isStart && current.path.size() < MIN_CYCLE_SIZE) continue; //don't bother with small cycles
                    long newDistance = current.distance + edge.getValue();
                    List<V<T>> newPath = Stream.concat(current.path.stream(), Stream.of(to)).toList();
                    PathState<T> newState = new PathState<>(to, newDistance, newPath, current.firstDestination);
                    pq.offer(newState);
                }
            }
        }
        return shortest;
    }
}
