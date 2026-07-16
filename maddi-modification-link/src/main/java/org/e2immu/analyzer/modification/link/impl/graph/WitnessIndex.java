package org.e2immu.analyzer.modification.link.impl.graph;

import java.util.*;
import java.util.function.Function;

public final class WitnessIndex<V, L> {
    private final Map<Fact<V, L>, Witness<V, L>> witnesses = new HashMap<>();
    private final Function<L, Integer> scoreFunction;
    private final Comparator<V> vertexComparator;

    public WitnessIndex(Function<L, Integer> scoreFunction, Comparator<V> vertexComparator) {
        this.scoreFunction = scoreFunction;
        this.vertexComparator = vertexComparator;
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
               && (c.inferred() && !e.inferred() || witnessCost(candidate) < witnessCost(existing))
            || equalQuality(candidate, existing)
               && canonicalCompare(candidate, existing) < 0) {
            witnesses.put(fact, candidate);
            return true;
        }
        return false;
    }

    // Equal-quality witnesses used to be resolved first-arrival-wins — but arrival order depends on map iteration
    // over identity-hashed variables (LocalVariableImpl has no hashCode override), which varies per JVM run and
    // made witness choice, and everything downstream of it (fact survival on vertex removal, closure dumps),
    // nondeterministic. Tie-break instead on a canonical textual key so the chosen witness is order-independent.
    private boolean equalQuality(Witness<V, L> candidate, Witness<V, L> existing) {
        if (candidate instanceof Witness.DirectWitness<V, L> && existing instanceof Witness.DirectWitness<V, L>) {
            return witnessCost(candidate) == witnessCost(existing);
        }
        if (candidate instanceof Witness.CompositeWitness<V, L> c
            && existing instanceof Witness.CompositeWitness<V, L> e) {
            return c.inferred() == e.inferred() && witnessCost(candidate) == witnessCost(existing);
        }
        return false;
    }

    /*
    The tie-break must deterministically order EQUAL-QUALITY candidates for ONE fact. The two child facts
    suffice: a composite derivation IS its (left, right) pair — the children's own witnesses are chosen in
    their own putIfBetter calls. Two prior implementations died on merge-heavy shapes (a 100-arm switch in a
    loop, clone-bench Function18752956; ties are the COMMON case there): printing the entire recursive support
    set through the CST output machinery took 583 SECONDS for one 335-line file, and even two-fact string keys
    still dominated the profile. Structural comparison via the engine's vertexComparator (cached-FQN compare)
    builds no strings at all. Labels order by score, then symbol (both cheap); vertices decide almost always.
     */
    private int canonicalCompare(Witness<V, L> candidate, Witness<V, L> existing) {
        if (candidate instanceof Witness.DirectWitness<V, L> dc
            && existing instanceof Witness.DirectWitness<V, L> de) {
            return compareFacts(dc.fact(), de.fact());
        }
        if (candidate instanceof Witness.CompositeWitness<V, L> cc
            && existing instanceof Witness.CompositeWitness<V, L> ce) {
            int c = compareFacts(cc.left(), ce.left());
            if (c != 0) return c;
            return compareFacts(cc.right(), ce.right());
        }
        return 0; // different kinds: handled by the quality rules above
    }

    private int compareFacts(Fact<V, L> a, Fact<V, L> b) {
        int c = vertexComparator.compare(a.source(), b.source());
        if (c != 0) return c;
        c = vertexComparator.compare(a.target(), b.target());
        if (c != 0) return c;
        c = Integer.compare(scoreFunction.apply(a.label()), scoreFunction.apply(b.label()));
        if (c != 0) return c;
        return a.label().toString().compareTo(b.label().toString());
    }

    public Witness<V, L> get(Fact<V, L> fact) {
        return witnesses.get(fact);
    }

    public Set<V> removeFacts(Set<V> vertices, List<Fact<V, L>> removedFacts) {
        Set<V> extra = new HashSet<>();
        for (Fact<V, L> fact : removedFacts) {
            Witness<V, L> witness = witnesses.remove(fact);
            assert witness != null;
            if (witness instanceof Witness.CompositeWitness<V, L> cw
                && vertices.contains(cw.left().target())) {
                extra.add(cw.left().source());
                extra.add(cw.right().target());
            }
        }
        return extra;
    }

    public int size() {
        return witnesses.size();
    }
}