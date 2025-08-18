package org.e2immu.language.cst.api.expression;

import org.e2immu.language.cst.api.element.Element;

public interface EnclosedExpression extends Expression, ExpressionWrapper {
    Expression inner();

    interface Builder extends Element.Builder<Builder> {
        Builder setExpression(Expression expression);

        EnclosedExpression build();
    }

    String NAME = "enclosedExpression";

    @Override
    default String name() {
        return NAME;
    }
}
