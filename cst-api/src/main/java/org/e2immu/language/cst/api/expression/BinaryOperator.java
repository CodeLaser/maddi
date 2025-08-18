package org.e2immu.language.cst.api.expression;

import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.type.ParameterizedType;

public interface BinaryOperator extends Expression {

    Expression lhs();

    Expression rhs();

    MethodInfo operator();

    Precedence precedence();

    interface Builder extends Expression.Builder<Builder> {
        @Fluent
        Builder setLhs(Expression lhs);

        @Fluent
        Builder setRhs(Expression rhs);

        @Fluent
        Builder setOperator(MethodInfo operator);

        @Fluent
        Builder setPrecedence(Precedence precedence);

        @Fluent
        Builder setParameterizedType(ParameterizedType parameterizedType);

        BinaryOperator build();
    }

    String NAME = "binaryOperator";

    @Override
    default String name() {
        return NAME;
    }
}
