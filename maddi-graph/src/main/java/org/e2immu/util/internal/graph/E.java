package org.e2immu.util.internal.graph;

import java.util.function.Function;

public record E<T>(V<T> from, V<T> to, long weight) {
    @Override
    public String toString() {
        return from + "->" + weight + "->" + to;
    }

    public String toString(Function<Long, String> edgeValuePrinter) {
        return from + "->" + edgeValuePrinter.apply(weight) + "->" + to;
    }
}
