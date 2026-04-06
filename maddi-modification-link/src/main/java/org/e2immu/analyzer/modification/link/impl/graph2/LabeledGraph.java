package org.e2immu.analyzer.modification.link.impl.graph2;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class LabeledGraph<V, L> {
    private final Map<V, Map<V, L>> out = new HashMap<>();
    private final Map<V, Map<V, L>> in = new HashMap<>();

    public void addEdge(V from, V to, L label) {
        out.computeIfAbsent(from, k -> new HashMap<>())
                .put(to, label);

        in.computeIfAbsent(to, k -> new HashMap<>())
                .put(from, label);
    }

    public void removeEdge(V from, V to) {
        Map<V, L> outMap = out.get(from);
        if (outMap != null) {
            outMap.remove(to);
        }

        Map<V, L> inMap = in.get(to);
        if (inMap != null) {
            inMap.remove(from);
        }
    }

    public Map<V, L> successors(V v) {
        return out.getOrDefault(v, Map.of());
    }

    public Map<V, L> predecessors(V v) {
        return in.getOrDefault(v, Map.of());
    }
}