package org.e2immu.analyzer.modification.link.impl.graph;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    // DAG of explanations for the reduction engine. NOT a record: the support set — the RAW facts underpinning
    // the derivation — is materialized LAZILY and memoized. Eager materialization built a fresh union set for
    // EVERY candidate composite (including the many that lose the putIfBetter race); on merge-heavy shapes
    // (clone-bench Function18752956's 100-arm switch) that was a dominant cost. The child WITNESSES are held
    // instead: memory is shared across the DAG, and only witnesses whose support is actually queried
    // (doesNotCreateCycle on winners, orphan materialization, dumps) pay for it, once.
    final class CompositeWitness<V, L> implements Witness<V, L> {
        private final Fact<V, L> left;
        private final Fact<V, L> right;
        private final boolean inferred;
        private final Witness<V, L> leftWitness;
        private final Witness<V, L> rightWitness;
        private Set<Fact<V, L>> support;

        private CompositeWitness(Witness<V, L> leftWitness, Witness<V, L> rightWitness,
                                 Fact<V, L> left, Fact<V, L> right, boolean inferred) {
            this.left = left;
            this.right = right;
            this.inferred = inferred;
            this.leftWitness = leftWitness;
            this.rightWitness = rightWitness;
        }

        public static <V, L> CompositeWitness<V, L> of(Witness<V, L> leftWitness,
                                                       Witness<V, L> rightWitness,
                                                       Fact<V, L> left,
                                                       Fact<V, L> right,
                                                       boolean inferred) {
            return new CompositeWitness<>(leftWitness, rightWitness, left, right, inferred);
        }

        public Fact<V, L> left() {
            return left;
        }

        public Fact<V, L> right() {
            return right;
        }

        public boolean inferred() {
            return inferred;
        }

        @Override
        public Set<Fact<V, L>> support() {
            if (support == null) {
                Set<Fact<V, L>> s = new HashSet<>(leftWitness.support());
                s.addAll(rightWitness.support());
                support = s;
            }
            return support;
        }

        @Override
        public @NotNull String toString() {
            return (inferred ? "*" : "") + "[" + left + ", " + right + "]";
        }

        @Override
        public String print(Function<V, String> vertexPrinter) {
            Set<Fact<V, L>> support = support();
            return (inferred ? "*" : "")
                   + "[" + left.print(vertexPrinter) + ", " + right.print(vertexPrinter) + "]"
                   + (support.size() > 2 ? support.stream()
                    .filter(f -> !f.equals(left) && !f.equals(right))
                    .map(f -> f.print(vertexPrinter))
                    .sorted() // the support set is unordered; sort for deterministic output (dumps, tie-break keys)
                    .collect(Collectors.joining(", ", " support: ", "")) : "");
        }
    }
}
