package org.e2immu.analyzer.modification.link.impl.graph2;

import org.jetbrains.annotations.NotNull;

// explains how a certain fact was derived
public sealed interface Witness<V, L>
        permits Witness.DirectWitness, Witness.CompositeWitness {

    record DirectWitness<V, L>(V from, V to, L label, String statementIndex) implements Witness<V, L> {
        @Override
        public @NotNull String toString() {
            return statementIndex + "(" + from + " " + label + " " + to + ")";
        }
    }

    // DAG of explanations for the reduction engine
    record CompositeWitness<V, L>(Fact<V, L> left, Fact<V, L> right) implements Witness<V, L> {
        @Override
        public @NotNull String toString() {
            return "[" + left + ", " + right + "]";
        }
    }
}
