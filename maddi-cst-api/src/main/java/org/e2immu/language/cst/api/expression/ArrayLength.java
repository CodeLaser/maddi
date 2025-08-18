package org.e2immu.language.cst.api.expression;

import org.e2immu.language.cst.api.element.Element;

public interface ArrayLength extends Expression {
    Expression scope();

    interface Builder extends Element.Builder<Builder> {
        Builder setExpression(Expression e);

        ArrayLength build();
    }

    String NAME = "arrayLength";

    @Override
    default String name() {
        return NAME;
    }
}
