package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.impl.variable.LocalVariableImpl;

public class FunctionalInterfaceVariable extends LocalVariableImpl {
    private final ExpressionVisitor.Result result;

    public FunctionalInterfaceVariable(String name,
                                       ParameterizedType parameterizedType,
                                       Expression assignmentExpression,
                                       ExpressionVisitor.Result result) {
        super(name, parameterizedType, assignmentExpression);
        this.result = result;
    }

    public ExpressionVisitor.Result result() {
        return result;
    }
}
