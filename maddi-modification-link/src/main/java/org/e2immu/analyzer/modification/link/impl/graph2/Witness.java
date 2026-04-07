package org.e2immu.analyzer.modification.link.impl.graph2;

import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

// explains how a certain fact was derived
public sealed interface Witness<V, L>
        permits Witness.DirectWitness, Witness.CompositeWitness {

    String print(Function<V, String> vertexPrinter);

    record DirectWitness<V, L>(V from, V to, L label, String statementIndex) implements Witness<V, L> {
        @Override
        public @NotNull String toString() {
            return statementIndex + "(" + from + " " + label + " " + to + ")";
        }

        @Override
        public String print(Function<V, String> vertexPrinter) {
            return statementIndex + "(" + vertexPrinter.apply(from) + " " + label + " "
                   + vertexPrinter.apply(to) + ")";
        }
    }

    // DAG of explanations for the reduction engine
    record CompositeWitness<V, L>(Fact<V, L> left, Fact<V, L> right) implements Witness<V, L> {
        @Override
        public @NotNull String toString() {
            return "[" + left + ", " + right + "]";
        }

        @Override
        public String print(Function<V, String> vertexPrinter) {
            return "[" + left.print(vertexPrinter) + ", " + right.print(vertexPrinter) + "]";
        }
    }
}
