package io.codelaser.maddi.modification.link.impl;

import io.codelaser.maddi.cst.api.variable.LocalVariable;
import io.codelaser.maddi.cst.api.variable.Variable;

public interface LinkVariable {

    boolean acceptForLinkedVariables();

    static boolean acceptForLinkedVariables(Variable variable) {
        return variable.variableStreamDescend()
                .allMatch(v -> !(v instanceof LocalVariable lv) ||
                               lv instanceof LinkVariable linkVariable && linkVariable.acceptForLinkedVariables());
    }
}
