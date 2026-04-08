package org.e2immu.analyzer.modification.link.impl.graph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class WitnessIndex<V, L> {
    private final Map<Fact<V, L>, Witness<V, L>> witnesses = new HashMap<>();

    public String print(Fact<V, L> fact) {
        Witness<V, L> w = witnesses.get(fact);
        if (w == null) return "";
        return w.toString();
    }

    public String print(Function<V, String> vertexPrinter, Fact<V, L> fact) {
        Witness<V, L> w = witnesses.get(fact);
        if (w == null) return "";
        return w.print(vertexPrinter);
    }

    public void put(Fact<V, L> fact, Witness<V, L> witness) {
        witnesses.put(fact, witness);
    }

    public Witness<V, L> get(Fact<V, L> fact) {
        return witnesses.get(fact);
    }

    public void removeFact(Fact<V, L> fact) {
        witnesses.remove(fact);
    }

    //NOTE: we leave intermediates (CompositeWitness) in place
    // return extra vertices to remove
    public Set<V> removeVertices(Set<V> vertices) {
        Set<V> extra = new HashSet<>();
        witnesses.entrySet()
                .removeIf(e -> {
                    if (vertices.contains(e.getKey().source())
                        || vertices.contains(e.getKey().target())) return true;
                    if (e.getValue() instanceof Witness.CompositeWitness<V, L>(Fact<V, L> left, Fact<V, L> right)
                        && vertices.contains(left.target())) {
                        extra.add(left.source());
                        extra.add(right.target());
                        return true;
                    }
                    return false;
                });
        return extra;
    }
}