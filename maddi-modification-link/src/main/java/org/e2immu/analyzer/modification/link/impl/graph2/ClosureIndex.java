package org.e2immu.analyzer.modification.link.impl.graph2;


import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

public class ClosureIndex<V, L> {
    private final Map<V, Map<V, L>> reachable = new HashMap<>();
    private final Map<V, Map<V, L>> reverseReachable = new HashMap<>();
    private final BinaryOperator<L> best;

    public ClosureIndex(BinaryOperator<L> best) {
        this.best = best;
    }

    public boolean add(V from, V to, L label) {
        assert !from.equals(to);
        reverseReachable.computeIfAbsent(to, _ -> new HashMap<>())
                .merge(from, label, best);
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

    public L label(V from, V to) {
        return reachable.getOrDefault(from, Map.of()).get(to);
    }

    public Map<V, L> predecessors(V target) {
        return reverseReachable.getOrDefault(target, Map.of());
    }

    public String print(Comparator<V> vComparator, Comparator<L> lComparator) {
        return reachable.entrySet().stream().sorted(Map.Entry.comparingByKey(vComparator))
                .flatMap(e ->
                        e.getValue().entrySet().stream().sorted(Map.Entry.comparingByKey(vComparator))
                                .map(e2 -> e.getKey() + " " + e2.getValue() + " " + e2.getKey()))
                .collect(Collectors.joining(" / "));
    }

}