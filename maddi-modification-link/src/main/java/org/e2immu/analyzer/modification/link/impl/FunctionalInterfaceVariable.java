package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.impl.variable.LocalVariableImpl;

public class FunctionalInterfaceVariable extends LocalVariableImpl {
    private final Result result;

    public FunctionalInterfaceVariable(String name,
                                       ParameterizedType parameterizedType,
                                       Expression assignmentExpression,
                                       Result result) {
        super(name, parameterizedType, assignmentExpression);
        this.result = result;
    }

    public Result result() {
        return result;
    }
}
