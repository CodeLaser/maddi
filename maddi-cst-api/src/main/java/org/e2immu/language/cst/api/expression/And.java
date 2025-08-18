package org.e2immu.language.cst.api.expression;

import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.element.Element;

import java.util.List;

public interface And extends Expression {
    List<Expression> expressions();

    interface Builder extends Element.Builder<Builder> {
        @Fluent
        Builder addExpressions(List<Expression> expressions);

        @Fluent
        Builder addExpression(Expression expression);

        And build();
    }

    String NAME = "and";

    @Override
    default String name() {
        return NAME;
    }
}
