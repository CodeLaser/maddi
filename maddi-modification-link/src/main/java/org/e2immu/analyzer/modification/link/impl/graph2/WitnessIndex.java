package org.e2immu.analyzer.modification.link.impl.graph2;

import java.util.HashMap;
import java.util.List;
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
        witnesses.putIfAbsent(fact, witness);
    }

    public Witness<V, L> get(Fact<V, L> fact) {
        return witnesses.get(fact);
    }

    // returns a list of facts that rely on these vertices; these facts must be removed as well
    public List<Fact<V, L>> removeVertices(Set<V> vertices) {
        return List.of(); // FIXME
    }
}