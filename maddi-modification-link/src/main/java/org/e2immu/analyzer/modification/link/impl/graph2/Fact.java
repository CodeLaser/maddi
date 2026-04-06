package org.e2immu.analyzer.modification.link.impl.graph2;

public record Fact<V, L>(V source, V target, L label) {
    public Fact {
        assert !source.equals(target);
    }
}
