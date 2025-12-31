package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.variable.LocalVariableImpl;

import java.util.List;

public class AppliedFunctionalInterfaceVariable extends LocalVariableImpl {
    private final List<ExpressionVisitor.Result> params;

    public AppliedFunctionalInterfaceVariable(String name,
                                              ParameterizedType parameterizedType,
                                              Expression assignmentExpression,
                                              List<ExpressionVisitor.Result> params) {
        super(name, parameterizedType, assignmentExpression);
        this.params = params;
    }

    public boolean containsNoLocalVariables() {
        return params().stream().allMatch(AppliedFunctionalInterfaceVariable::containsNoLocalVariables);
    }

    private static boolean containsNoLocalVariables(ExpressionVisitor.Result p) {
        return p.links() == null
               || p.links().primary() == null
               || p.links().isEmpty() && containsNoLocalVariable(p.links().primary())
               || p.links().stream().allMatch(l -> containsNoLocalVariable(l.to()));
    }

    private static boolean containsNoLocalVariable(Variable variable) {
        return variable.variableStreamDescend().noneMatch(v -> v instanceof LocalVariable);
    }

    public List<ExpressionVisitor.Result> params() {
        return params;
    }
}
