package org.e2immu.analyzer.modification.link.impl.graph2;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ClosureIndex<V, L> {
    private final Map<V, Map<V, Set<L>>> reachable = new HashMap<>();
    private final Map<V, Map<V, Set<L>>> reverseReachable = new HashMap<>();

    public boolean add(V from, V to, L label) {
        reverseReachable.computeIfAbsent(to, _ -> new HashMap<>())
                .computeIfAbsent(from, _ -> new HashSet<>())
                .add(label);
        return reachable
                .computeIfAbsent(from, _ -> new HashMap<>())
                .computeIfAbsent(to, _ -> new HashSet<>())
                .add(label);
    }

    public Set<L> labels(V from, V to) {
        return reachable.getOrDefault(from, Map.of()).getOrDefault(to, Set.of());
    }

    public Map<V, Set<L>> predecessors(V target) {
        return reverseReachable.getOrDefault(target, Map.of());
    }
}