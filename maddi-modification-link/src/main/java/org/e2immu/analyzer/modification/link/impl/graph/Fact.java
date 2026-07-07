package org.e2immu.analyzer.modification.link.impl.graph;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Function;

/*
IMPORTANT: equality is on source+target, because of the map in WitnessIndex
If we don't do that, we cannot find a "better" witness.
 */
public record Fact<V, L>(V source, V target, L label) {
    public Fact {
        assert !source.equals(target);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Fact<?, ?> fact)) return false;
        return Objects.equals(source(), fact.source()) && Objects.equals(target(), fact.target());
    }

    @Override
    public int hashCode() {
        return Objects.hash(source(), target());
    }

    public String print(Function<V, String> vertexPrinter) {
        return vertexPrinter.apply(source) + " " + label + " " + vertexPrinter.apply(target);
    }

    @Override
    public @NotNull String toString() {
        return source + " " + label + " " + target;
    }
}
