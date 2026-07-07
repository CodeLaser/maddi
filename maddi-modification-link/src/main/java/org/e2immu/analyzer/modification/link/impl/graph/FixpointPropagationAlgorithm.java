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

    static <V, L> Map<V, Map<V, L>> reduceLabelPreserving(
            Map<V, Map<V, L>> graph,
            Set<V> vertices,
            BiFunction<L, L, L> combine) {

        Map<V, Map<V, L>> reduced = new HashMap<>();

        // Deep copy
        for (V u : vertices) {
            reduced.put(u, new HashMap<>(graph.getOrDefault(u, Map.of())));
        }

        for (V u : vertices) {
            Map<V, L> outgoing = new HashMap<>(reduced.getOrDefault(u, Map.of()));

            for (Map.Entry<V, L> edge : outgoing.entrySet()) {
                V v = edge.getKey();
                L directLabel = edge.getValue();

                // Temporarily remove edge
                reduced.get(u).remove(v);

                boolean reconstructable =
                        canReconstructLabel(
                                reduced,
                                u,
                                v,
                                directLabel,
                                combine);

                if (!reconstructable) {
                    reduced.get(u).put(v, directLabel);
                }
            }
        }

        return reduced;
    }

    private record VL<V, L>(V vertex, L label) {
    }

    static <V, L> boolean canReconstructLabel(
            Map<V, Map<V, L>> graph,
            V source,
            V target,
            L expected,
            BiFunction<L, L, L> combine) {

        Deque<VL<V, L>> q = new ArrayDeque<>();
        Set<V> visited = new HashSet<>();

        q.add(new VL<>(source, null));

        while (!q.isEmpty()) {
            var current = q.removeFirst();
            V u = current.vertex();
            L currentLabel = current.label();

            if (!visited.add(u)) continue;

            for (var e : graph.getOrDefault(u, Map.of()).entrySet()) {
                V w = e.getKey();

                L nextLabel = currentLabel == null
                        ? e.getValue()
                        : combine.apply(currentLabel, e.getValue());

                if (w.equals(target) &&
                    Objects.equals(nextLabel, expected)) {
                    return true;
                }

                q.add(new VL<>(w, nextLabel));
            }
        }

        return false;
    }
}
