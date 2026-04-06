package org.e2immu.analyzer.modification.link.impl.graph;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

class FixpointPropagationAlgorithm {

    static <V, L> Map<V, Set<L>> computePathLabels(
            Function<V, Map<V, L>> graph,
            Set<V> keySet,
            V start,
            L emptyLabel,
            BiFunction<L, L, L> combine) {

        // All labels known per vertex
        Map<V, Set<L>> labels = new HashMap<>();

        // Only labels not yet propagated
        Map<V, Deque<L>> delta = new HashMap<>();

        for (V v : keySet) {
            labels.put(v, new HashSet<>());
            delta.put(v, new ArrayDeque<>());
        }

        labels.computeIfAbsent(start, k -> new HashSet<>()).add(emptyLabel);
        delta.computeIfAbsent(start, k -> new ArrayDeque<>()).add(emptyLabel);

        Deque<V> worklist = new ArrayDeque<>();
        Set<V> inQueue = new HashSet<>();

        worklist.add(start);
        inQueue.add(start);

        while (!worklist.isEmpty()) {
            V u = worklist.removeFirst();
            inQueue.remove(u);

            Deque<L> pending = delta.get(u);

            // Process only newly added labels
            while (!pending.isEmpty()) {
                L lbl = pending.removeFirst();

                for (Map.Entry<V, L> e : graph.apply(u).entrySet()) {
                    V w = e.getKey();
                    L edgeLabel = e.getValue();

                    L newLabel = combine.apply(lbl, edgeLabel);

                    Set<L> wLabels =
                            labels.computeIfAbsent(w, k -> new HashSet<>());

                    if (wLabels.add(newLabel)) {
                        delta.computeIfAbsent(w, k -> new ArrayDeque<>())
                                .addLast(newLabel);

                        if (inQueue.add(w)) {
                            worklist.addLast(w);
                        }
                    }
                }
            }
        }

        return labels;
    }

}
