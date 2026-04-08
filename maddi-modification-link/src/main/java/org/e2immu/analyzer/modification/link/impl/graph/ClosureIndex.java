package org.e2immu.analyzer.modification.link.impl.graph;


import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClosureIndex<V, L> {
    private final Map<V, Map<V, L>> reachable = new HashMap<>();
    private final Map<V, Map<V, L>> reverseReachable = new HashMap<>();
    private final BinaryOperator<L> best;

    public ClosureIndex(BinaryOperator<L> best) {
        this.best = best;
    }

    public boolean add(V from, V to, L label) {
        assert !from.equals(to);

        Map<V, L> map = reachable.computeIfAbsent(from, _ -> new HashMap<>());
        Map<V, L> rMap = reverseReachable.computeIfAbsent(to, _ -> new HashMap<>());

        L current = map.get(to);
        if (current == null) {
            map.put(to, label);
            rMap.put(from, label);
            return true;
        }
        L newLabel = best.apply(current, label);
        map.put(to, newLabel);
        rMap.put(from, newLabel);
        return !current.equals(newLabel);
    }

    public L label(V from, V to) {
        return reachable.getOrDefault(from, Map.of()).get(to);
    }

    public Iterable<Map.Entry<V, L>> predecessors(V target) {
        return reverseReachable.getOrDefault(target, Map.of()).entrySet();
    }

    public Stream<Map.Entry<V, L>> successorStream(V target) {
        return reachable.getOrDefault(target, Map.of()).entrySet().stream();
    }

    public Iterable<Map.Entry<V, L>> successors(V target) {
        return reachable.getOrDefault(target, Map.of()).entrySet();
    }

    public String print(Function<V, String> vertexPrinter,
                        Comparator<V> vComparator, WitnessIndex<V, L> witnessIndex) {
        return reachable.entrySet().stream().sorted(Map.Entry.comparingByKey(vComparator))
                .flatMap(e ->
                        e.getValue().entrySet().stream().sorted(Map.Entry.comparingByKey(vComparator))
                                .map(e2 -> vertexPrinter.apply(e.getKey()) + " " + e2.getValue()
                                           + " " + vertexPrinter.apply(e2.getKey()) + "   "
                                           + witnessIndex.print(vertexPrinter,
                                        new Fact<>(e.getKey(), e2.getKey(), e2.getValue()))))
                .collect(Collectors.joining("\n"));
    }

    public void removeFacts(List<Fact<V, L>> factsToRemove) {
    }

    public void removeVertices(Set<V> vertices) {
        reachable.keySet().removeAll(vertices);
        reverseReachable.keySet().removeAll(vertices);
        reachable.values().forEach(map -> map.keySet().removeAll(vertices));
        reverseReachable.values().forEach(map -> map.keySet().removeAll(vertices));
    }
}