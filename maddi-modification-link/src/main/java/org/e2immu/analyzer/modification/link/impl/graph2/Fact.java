package org.e2immu.analyzer.modification.link.impl.graph2;

import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public record Fact<V, L>(V source, V target, L label) {
    public Fact {
        assert !source.equals(target);
    }

    public String print(Function<V, String> vertexPrinter) {
        return vertexPrinter.apply(source) + " " + label + " " + vertexPrinter.apply(target);
    }

    @Override
    public @NotNull String toString() {
        return source + " " + label + " " + target;
    }
}
