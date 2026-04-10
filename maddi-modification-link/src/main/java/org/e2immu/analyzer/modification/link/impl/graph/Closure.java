package org.e2immu.analyzer.modification.link.impl.graph;


import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// not symmetric by design!

public class Closure<V, L> {
    private final Map<V, Map<V, L>> reachable = new HashMap<>();
    private final BinaryOperator<L> best;

    public Closure(BinaryOperator<L> best) {
        this.best = best;
    }

    public boolean add(V from, V to, L label) {
        assert !from.equals(to);

        Map<V, L> map = reachable.computeIfAbsent(from, _ -> new HashMap<>());

        L current = map.get(to);
        if (current == null) {
            map.put(to, label);
            return true;
        }
        L newLabel = best.apply(current, label);
        map.put(to, newLabel);
        return !current.equals(newLabel);
    }

    public int countFacts() {
        return reachable.values().stream().mapToInt(Map::size).sum();
    }

    public L label(V from, V to) {
        return reachable.getOrDefault(from, Map.of()).get(to);
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
                .collect(Collectors.joining("\n", "", "\n"));
    }

    public List<Fact<V, L>> removeFacts(Set<V> vertices, Predicate<Fact<V, L>> acceptForRemoval) {
        List<Fact<V, L>> result = new LinkedList<>();
        for (V v : vertices) {
            Map<V, L> map = reachable.get(v);
            if (map != null) {
                map.entrySet().removeIf(entry -> {
                    Fact<V, L> fact = new Fact<>(v, entry.getKey(), entry.getValue());
                    if (acceptForRemoval.test(fact)) {
                        result.add(fact);
                        return true;
                    }
                    return false;
                });
            }
        }
        return result;
    }

    public void removeVertices(Set<V> vertices) {
        reachable.keySet().removeAll(vertices);
        reachable.values().forEach(map -> map.keySet().removeAll(vertices));
    }
}