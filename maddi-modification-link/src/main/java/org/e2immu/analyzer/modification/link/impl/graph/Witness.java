package org.e2immu.analyzer.modification.link.impl.graph;

import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// explains how a certain fact was derived
public sealed interface Witness<V, L>
        permits Witness.DirectWitness, Witness.CompositeWitness {

    Set<Fact<V, L>> support();

    String print(Function<V, String> vertexPrinter);

    record DirectWitness<V, L>(Fact<V, L> fact, String statementIndex) implements Witness<V, L> {

        @Override
        public String print(Function<V, String> vertexPrinter) {
            return statementIndex + "(" + fact.print(vertexPrinter) + ")";
        }

        @Override
        public Set<Fact<V, L>> support() {
            return Set.of(fact);
        }
    }

    // DAG of explanations for the reduction engine
    record CompositeWitness<V, L>(Fact<V, L> left, Fact<V, L> right, boolean inferred,
                                  Set<Fact<V, L>> support) implements Witness<V, L> {

        public static <V, L> CompositeWitness<V, L> of(Witness<V, L> leftWitness,
                                                       Witness<V, L> rightWitness,
                                                       Fact<V, L> left,
                                                       Fact<V, L> right,
                                                       boolean inferred) {
            Stream<Fact<V, L>> leftSupport = leftWitness.support().stream();
            Stream<Fact<V, L>> rightSupport = rightWitness.support().stream();
            Set<Fact<V, L>> newSupport = Stream.concat(leftSupport, rightSupport)
                    .collect(Collectors.toUnmodifiableSet());
            return new CompositeWitness<>(left, right, inferred, newSupport);
        }

        @Override
        public @NotNull String toString() {
            return (inferred ? "*" : "") + "[" + left + ", " + right + "]";
        }

        @Override
        public String print(Function<V, String> vertexPrinter) {
            return (inferred ? "*" : "")
                   + "[" + left.print(vertexPrinter) + ", " + right.print(vertexPrinter) + "]"
                   + (support.size() > 2 ? support.stream()
                    .filter(f -> !f.equals(left) && !f.equals(right))
                    .map(f -> f.print(vertexPrinter))
                    .collect(Collectors.joining(", ", " support: ", "")) : "");
        }
    }
}
