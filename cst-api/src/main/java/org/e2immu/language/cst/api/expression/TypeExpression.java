package org.e2immu.language.cst.api.expression;

import org.e2immu.language.cst.api.type.Diamond;
import org.e2immu.language.cst.api.type.ParameterizedType;

public interface TypeExpression extends Expression {
    ParameterizedType parameterizedType();

    String NAME = "typeExpression";

    @Override
    default String name() {
        return NAME;
    }


    interface Builder extends Expression.Builder<Builder> {
        Builder setParameterizedType(ParameterizedType parameterizedType);

        Builder setDiamond(Diamond diamond);

        TypeExpression build();
    }
}
