package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.api.variable.Variable;

public interface LinkVariable {

    boolean acceptForLinkedVariables();

    static boolean acceptForLinkedVariables(Variable variable) {
        return variable.variableStreamDescend()
                .allMatch(v -> !(v instanceof LocalVariable lv) ||
                               lv instanceof LinkVariable linkVariable && linkVariable.acceptForLinkedVariables());
    }
}
