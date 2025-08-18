package org.e2immu.language.cst.api.expression;

import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.variable.Variable;

public interface Assignment extends Expression {
    VariableExpression target();

    Expression value();

    Variable variableTarget();

    MethodInfo assignmentOperator();

    Boolean prefixPrimitiveOperator();

    boolean assignmentOperatorIsPlus();

    MethodInfo binaryOperator();

    Assignment withValue(Expression ve);

    interface Builder extends Element.Builder<Assignment.Builder> {
        @Fluent
        Builder setTarget(VariableExpression target);

        @Fluent
        Builder setValue(Expression value);

        @Fluent
        Builder setAssignmentOperator(MethodInfo assignmentOperator);

        @Fluent
        Builder setPrefixPrimitiveOperator(Boolean prefixPrimitiveOperator);

        @Fluent
        Builder setBinaryOperator(MethodInfo binaryOperator);

        @Fluent
        Builder setAssignmentOperatorIsPlus(boolean assignmentOperatorIsPlus);

        Assignment build();
    }

    String NAME = "assignment";

    @Override
    default String name() {
        return NAME;
    }
}
