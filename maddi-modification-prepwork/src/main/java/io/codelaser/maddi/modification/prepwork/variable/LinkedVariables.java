package io.codelaser.maddi.modification.prepwork.variable;

import io.codelaser.maddi.cst.api.variable.Variable;

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
