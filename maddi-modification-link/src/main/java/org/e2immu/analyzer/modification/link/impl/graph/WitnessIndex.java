package org.e2immu.analyzer.modification.link.impl.graph;

import java.util.*;
import java.util.function.Function;

public final class WitnessIndex<V, L> {
    private final Map<Fact<V, L>, Witness<V, L>> witnesses = new HashMap<>();
    private final Function<L, Integer> scoreFunction;

    public WitnessIndex(Function<L, Integer> scoreFunction) {
        this.scoreFunction = scoreFunction;
    }

    public String print(Fact<V, L> fact) {
        return print(Object::toString, fact);
    }

    public String print(Function<V, String> vertexPrinter, Fact<V, L> fact) {
        Witness<V, L> w = witnesses.get(fact);
        if (w == null) return "";
        return w.print(vertexPrinter);
    }

    private int witnessCost(Witness<V, L> witness) {
        return switch (witness) {
            case Witness.DirectWitness<V, L> _ -> 0;
            case Witness.CompositeWitness<V, L> cw ->
                    1 + scoreFunction.apply(cw.left().label()) + scoreFunction.apply(cw.right().label());
        };
    }

    public boolean putIfBetter(Fact<V, L> fact, Witness<V, L> candidate) {
        Witness<V, L> existing = witnesses.get(fact);
        if (existing == null
            || candidate instanceof Witness.DirectWitness<V, L> && existing instanceof Witness.CompositeWitness<V, L>
            || candidate instanceof Witness.DirectWitness<V, L> && existing instanceof Witness.DirectWitness<V, L>
               && witnessCost(candidate) < witnessCost(existing)
            || candidate instanceof Witness.CompositeWitness<V, L> c && existing instanceof Witness.CompositeWitness<V, L> e
               && (c.inferred() && !e.inferred() || witnessCost(candidate) < witnessCost(existing))) {
            witnesses.put(fact, candidate);
            return true;
        }
        return false;
    }

    public Witness<V, L> get(Fact<V, L> fact) {
        return witnesses.get(fact);
    }

    public Set<V> removeFacts(Set<V> vertices, List<Fact<V, L>> removedFacts) {
        Set<V> extra = new HashSet<>();
        for (Fact<V, L> fact : removedFacts) {
            Witness<V, L> witness = witnesses.remove(fact);
            assert witness != null;
            if (witness instanceof Witness.CompositeWitness<V, L>(Fact<V, L> left, Fact<V, L> right, _)
                && vertices.contains(left.target())) {
                extra.add(left.source());
                extra.add(right.target());
            }
        }
        return extra;
    }

    public int size() {
        return witnesses.size();
    }
}