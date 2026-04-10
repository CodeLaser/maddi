package org.e2immu.analyzer.modification.link.impl.graph;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class LabeledGraph<V, L> {
    private final Map<V, Map<V, L>> map = new LinkedHashMap<>();

    public Iterable<Map.Entry<V, Map<V, L>>> edges() {
        return map.entrySet();
    }

    public String printEdges(Comparator<V> comparator) {
        return map.entrySet().stream().sorted(Map.Entry.comparingByKey(comparator))
                .flatMap(e ->
                        e.getValue().entrySet().stream().sorted(Map.Entry.comparingByKey(comparator))
                                .map(e2 ->
                                        e.getKey() + " " + e2.getValue() + " " + e2.getKey()))
                .collect(Collectors.joining(" / "));
    }

    public String print(Comparator<V> comparator) {
        return print(Object::toString, comparator);
    }

    public String print(Function<V, String> vertexPrinter, Comparator<V> comparator) {
        return map.entrySet().stream().sorted(Map.Entry.comparingByKey(comparator))
                .map(e ->
                        e.getValue().isEmpty()
                                ? vertexPrinter.apply(e.getKey())
                                : e.getValue().entrySet().stream().sorted(Map.Entry.comparingByKey(comparator))
                                .map(e2 -> vertexPrinter.apply(e.getKey()) + " " + e2.getValue() + " " + vertexPrinter.apply(e2.getKey()))
                                .collect(Collectors.joining(" / ")))
                .collect(Collectors.joining("\n", "", "\n"));
    }

    public void addSymmetricEdge(V from, V to, L label, L reverseLabel) {
        map.computeIfAbsent(from, _ -> new LinkedHashMap<>()).put(to, label);
        map.computeIfAbsent(to, _ -> new LinkedHashMap<>()).put(from, reverseLabel);
    }

    public boolean addVertex(V v) {
        map.put(v, new LinkedHashMap<>());
        return true;
    }

    public void removeVertices(Set<V> vertices) {
        map.keySet().removeAll(vertices);
        map.values().forEach(map -> map.keySet().removeAll(vertices));
    }

    public boolean replace(V from, V to, L label, L reverseLabel) {
        Map<V, L> outMap = map.computeIfAbsent(from, _ -> new LinkedHashMap<>());
        L prev = outMap.put(to, label);
        Map<V, L> inMap = map.computeIfAbsent(to, _ -> new LinkedHashMap<>());
        inMap.put(from, reverseLabel);
        return !label.equals(prev);
    }

    public Map<V, L> successors(V v) {
        return map.getOrDefault(v, Map.of());
    }

    public Set<V> vertices() {
        return map.keySet();
    }
}