package org.e2immu.analyzer.modification.link.impl.graph2;

import java.util.*;
import java.util.stream.Collectors;

public final class LabeledGraph<V, L> {
    private final Map<V, Map<V, L>> out = new HashMap<>();
    private final Map<V, Map<V, L>> in = new HashMap<>();

    public Iterable<Map.Entry<V, Map<V, L>>> edges() {
        return out.entrySet();
    }

    public String printEdges(Comparator<V> comparator) {
        return out.entrySet().stream().sorted(Map.Entry.comparingByKey(comparator))
                .flatMap(e ->
                        e.getValue().entrySet().stream().sorted(Map.Entry.comparingByKey(comparator))
                                .map(e2 ->
                                        e.getKey() + " " + e2.getValue() + " " + e2.getKey()))
                .collect(Collectors.joining(" / "));
    }

    public String print(Comparator<V> comparator) {
        return out.entrySet().stream().sorted(Map.Entry.comparingByKey(comparator))
                .map(e -> e.getKey() + ":" + (e.getValue().isEmpty() ? "" : " ")
                          + e.getValue().entrySet().stream().sorted(Map.Entry.comparingByKey(comparator))
                                  .map(e2 -> " " + e2.getValue() + " " + e2.getKey())
                                  .collect(Collectors.joining(", ")))
                .collect(Collectors.joining("\n"));
    }

    public void addEdge(V from, V to, L label) {
        out.computeIfAbsent(from, k -> new HashMap<>()).put(to, label);
        in.computeIfAbsent(to, k -> new HashMap<>()).put(from, label);
    }

    public boolean addVertex(V v) {
        if (in.containsKey(v)) return false;
        in.put(v, new HashMap<>());
        out.put(v, new HashMap<>());
        return true;
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

    public void removeFacts(List<Fact<V, L>> factsToRemove) {
    }

    public void removeVertices(Set<V> vertices) {
        in.keySet().removeAll(vertices);
        out.keySet().removeAll(vertices);
        in.values().forEach(map -> map.keySet().removeAll(vertices));
        out.values().forEach(map -> map.keySet().removeAll(vertices));
    }

    public Map<V, L> successors(V v) {
        return out.getOrDefault(v, Map.of());
    }

    public Map<V, L> predecessors(V v) {
        return in.getOrDefault(v, Map.of());
    }

    public Set<V> vertices() {
        return out.keySet();
    }
}