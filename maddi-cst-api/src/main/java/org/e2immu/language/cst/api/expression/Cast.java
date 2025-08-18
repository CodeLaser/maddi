package org.e2immu.language.cst.api.expression;

import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.type.ParameterizedType;

public interface Cast extends Expression {
    Expression expression();

    interface Builder extends Element.Builder<Builder> {
        @Fluent
        Builder setExpression(Expression expression);

        @Fluent
        Builder setParameterizedType(ParameterizedType parameterizedType);

        Cast build();
    }

    String NAME = "cast";

    @Override
    default String name() {
        return NAME;
    }
}
