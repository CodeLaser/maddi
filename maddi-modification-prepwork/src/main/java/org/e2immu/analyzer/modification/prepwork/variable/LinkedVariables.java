package org.e2immu.analyzer.modification.prepwork.variable;

import org.e2immu.language.cst.api.variable.Variable;

import java.util.Map;
import java.util.stream.Stream;

// Primarily a helper type to carry around multiple Links objects. Not a "deliverable".
public interface LinkedVariables extends Iterable<Map.Entry<Variable, Links>> {

    default boolean contains(Variable variable) {
        return map().containsKey(variable);
    }

    boolean isEmpty();

    Map<Variable, Links> map();

    LinkedVariables merge(LinkedVariables other);

    default Stream<Map.Entry<Variable, Links>> stream() {
        return map().entrySet().stream();
    }
}
