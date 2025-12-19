package org.e2immu.analyzer.modification.prepwork.variable;

import org.e2immu.language.cst.api.variable.Variable;

import java.util.Map;

// Primarily a helper type to carry around multiple Links objects. Not a "deliverable".
public interface LinkedVariables extends Iterable<Map.Entry<Variable, Links>> {

    default boolean contains(Variable variable) {
        return map().containsKey(variable);
    }

    default boolean isDelayed() { throw new UnsupportedOperationException(); }

    boolean isEmpty();

    default boolean isNotYetSet() { throw new UnsupportedOperationException(); }

    Map<Variable, Links> map();

    LinkedVariables merge(LinkedVariables other);

   default  boolean overwriteAllowed(LinkedVariables linkedVariables) { throw new UnsupportedOperationException(); }
}
