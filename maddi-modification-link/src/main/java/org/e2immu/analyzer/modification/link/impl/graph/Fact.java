package org.e2immu.analyzer.modification.link.impl.graph;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Function;

/*
IMPORTANT: equality is on source+target, because of the map in WitnessIndex
If we don't do that, we cannot find a "better" witness.

Not a record: the hash is CACHED. Facts are the keys of every engine map (closure, witness index, history
sets), and hashing recomputes the source/target hash tuple on each probe — on merge-heavy shapes
(clone-bench Function18752956) Fact.hashCode was a top frame. The vertex hashes themselves are cheap
(cached FQN strings), but the per-probe recomputation adds up across millions of map operations.
 */
public final class Fact<V, L> implements Comparable<Fact<V, L>> {
    private final V source;
    private final V target;
    private final L label;
    private final int hashCode;

    public Fact(V source, V target, L label) {
        assert !source.equals(target);
        this.source = source;
        this.target = target;
        this.label = label;
        this.hashCode = Objects.hash(source, target);
    }

    public V source() {
        return source;
    }

    public V target() {
        return target;
    }

    public L label() {
        return label;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof Fact<?, ?> fact)) return false;
        return hashCode == fact.hashCode
               && Objects.equals(source(), fact.source()) && Objects.equals(target(), fact.target());
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    /**
     * Comparable for one load-bearing reason: {@code java.util.HashMap} treeifies buckets with 8+
     * hash-colliding keys, and inside a treeified bin, keys that are NOT mutually Comparable have their
     * insertion/iteration ties broken by {@code System.identityHashCode} ({@code HashMap.tieBreakOrder})
     * — per-JVM-run order. Facts key every engine map (closure, witness index, history sets); an
     * identity tie-break there is exactly the "identical event trace, different surviving instance"
     * nondeterminism measured on the composed dogfood (docs/eventual-info-hierarchy.md §"The trace
     * round"). Order on (source, target), consistent with {@link #equals} (label excluded). V is a
     * {@code Variable} in every engine instantiation, and Variable is Comparable by FQN.
     */
    @Override
    @SuppressWarnings("unchecked")
    public int compareTo(@NotNull Fact<V, L> o) {
        int c = ((Comparable<V>) source).compareTo(o.source);
        if (c != 0) return c;
        return ((Comparable<V>) target).compareTo(o.target);
    }

    public String print(Function<V, String> vertexPrinter) {
        return vertexPrinter.apply(source) + " " + label + " " + vertexPrinter.apply(target);
    }

    @Override
    public @NotNull String toString() {
        return source + " " + label + " " + target;
    }
}
