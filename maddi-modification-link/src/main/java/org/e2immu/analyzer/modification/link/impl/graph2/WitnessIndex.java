package org.e2immu.analyzer.modification.link.impl.graph2;

import java.util.HashMap;
import java.util.Map;

public final class WitnessIndex<V, L> {
    private final Map<Fact<V, L>, Witness<V, L>> witnesses = new HashMap<>();

    public void put(Fact<V, L> fact, Witness<V, L> witness) {
        witnesses.putIfAbsent(fact, witness);
    }

    public Witness<V, L> get(Fact<V, L> fact) {
        return witnesses.get(fact);
    }
}