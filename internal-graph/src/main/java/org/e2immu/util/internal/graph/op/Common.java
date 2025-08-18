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
