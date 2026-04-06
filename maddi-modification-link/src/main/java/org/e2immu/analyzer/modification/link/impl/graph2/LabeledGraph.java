package org.e2immu.analyzer.modification.link.impl.graph2;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class LabeledGraph<V, L> {
    private final Map<V, Map<V, L>> out = new HashMap<>();
    private final Map<V, Map<V, L>> in = new HashMap<>();

    public String print(Comparator<V> comparator) {
        return out.entrySet().stream().sorted(Map.Entry.comparingByKey(comparator))
                .flatMap(e ->
                        e.getValue().entrySet().stream().sorted(Map.Entry.comparingByKey(comparator))
                                .map(e2 ->
                                        e.getKey() + " " + e2.getValue() + " " + e2.getKey()))
                .collect(Collectors.joining(" / "));
    }

    public void addEdge(V from, V to, L label) {
        out.computeIfAbsent(from, k -> new HashMap<>()).put(to, label);
        in.computeIfAbsent(to, k -> new HashMap<>()).put(from, label);
    }

    public void addVertex(V v) {
        in.computeIfAbsent(v, _ -> new HashMap<>());
        out.computeIfAbsent(v, _ -> new HashMap<>());
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