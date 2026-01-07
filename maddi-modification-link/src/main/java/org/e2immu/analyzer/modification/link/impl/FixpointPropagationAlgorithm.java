package org.e2immu.analyzer.modification.link.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public class FixpointPropagationAlgorithm {
    private static final Logger LOGGER = LoggerFactory.getLogger(FixpointPropagationAlgorithm.class);

    public static <V, L> Map<V, Set<L>> computePathLabels(
            Function<V, Map<V, L>> graph,
            Set<V> keySet,
            V start,
            L emptyLabel,
            BiFunction<L, L, L> combine) {
        // Map from vertex to all reachable labels
        Map<V, Set<L>> labels = new HashMap<>();
        for (V v : keySet) {
            labels.put(v, new HashSet<>());
        }

        // Start node has the "empty path" label
        labels.get(start).add(emptyLabel);

        // Worklist BFS/DFS-style queue
        Deque<V> worklist = new ArrayDeque<>();
        worklist.add(start);

        while (!worklist.isEmpty()) {
            V u = worklist.removeFirst();

            for (Map.Entry<V, L> e : graph.apply(u).entrySet()) {
                V w = e.getKey();

                assert !u.equals(w) : "graph has a self-loop, may cause concurrent modification exceptions";

                for (L lbl : labels.get(u)) {
                    L newLabel = combine.apply(lbl, e.getValue());

                    // If this label is new for w, add & propagate
                    if (labels.computeIfAbsent(w, _ -> new HashSet<>()).add(newLabel)) {
                        worklist.add(w);
                        //LOGGER.debug("Start {}: {} -> {} add {}", start, u, w, newLabel);
                    }
                }
            }
        }

        return labels;
    }

}
