package org.e2immu.analyzer.modification.link.impl.graph;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class LabeledGraph<V, L> {

    // fired exactly when a vertex enters or leaves the vertex set (this map's key set) — the single
    // ownership point. Consumers maintain derived indexes (Graph's scope-part index); without a listener
    // every "which vertices touch X" question is a full vertex scan inside per-statement loops.
    public interface VertexListener<V> {
        void vertexAdded(V v);

        void vertexRemoved(V v);
    }

    private final Map<V, Map<V, L>> map = new LinkedHashMap<>();
    private VertexListener<V> listener;

    public void setListener(VertexListener<V> listener) {
        this.listener = listener;
    }

    // all vertex creation routes through here so the listener never misses an implicit
    // edge-endpoint creation
    private Map<V, L> row(V v) {
        Map<V, L> existing = map.get(v);
        if (existing != null) return existing;
        Map<V, L> fresh = new LinkedHashMap<>();
        map.put(v, fresh);
        if (listener != null) listener.vertexAdded(v);
        return fresh;
    }

    public Iterable<Map.Entry<V, Map<V, L>>> edges() {
        return map.entrySet();
    }

    public Map<V, L> edges(V v) {
        return map.getOrDefault(v, Map.of());
    }

    public boolean isKnown(V v) {
        return map.containsKey(v);
    }

    public String printEdges(Comparator<V> comparator) {
        return map.entrySet().stream().sorted(Map.Entry.comparingByKey(comparator))
                .flatMap(e ->
                        e.getValue().entrySet().stream().sorted(Map.Entry.comparingByKey(comparator))
                                .map(e2 ->
                                        e.getKey() + " " + e2.getValue() + " " + e2.getKey()))
                .collect(Collectors.joining(" / "));
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
        row(from).put(to, label);
        row(to).put(from, reverseLabel);
    }

    public boolean addVertex(V v) {
        // must be idempotent: an unconditional put CLOBBERED an existing vertex's successors, and the
        // unconditional 'true' made mergeEdgeBi's self-loop guards report change forever — the MakeGraph
        // expand loop then never converged ('cycle protection' on deep structures, TestParSeqLinkBench).
        if (map.containsKey(v)) return false;
        row(v);
        return true;
    }

    public boolean removeVertices(Set<V> vertices) {
        map.values().forEach(map -> map.keySet().removeAll(vertices));
        boolean change = false;
        for (V v : vertices) {
            if (map.remove(v) != null) {
                change = true;
                if (listener != null) listener.vertexRemoved(v);
            }
        }
        return change;
    }

    public boolean replace(V from, V to, L label, L reverseLabel) {
        L prev = row(from).put(to, label);
        row(to).put(from, reverseLabel);
        return !label.equals(prev);
    }

    public Iterable<Map.Entry<V, L>> successors(V v) {
        return map.getOrDefault(v, Map.of()).entrySet();
    }

    public Set<V> vertices() {
        return map.keySet();
    }
}