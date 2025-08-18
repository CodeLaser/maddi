package org.e2immu.language.cst.api.statement;

import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.Expression;

public interface ThrowStatement extends Statement {
    interface Builder extends Statement.Builder<Builder> {

        @Fluent
        Builder setExpression(Expression expression);

        ThrowStatement build();

    }

    String NAME = "throw";

    @Override
    default String name() {
        return NAME;
    }

    ThrowStatement withSource(Source newSource);
}
