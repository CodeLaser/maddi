package org.e2immu.language.cst.api.statement;

import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.Expression;

public interface ReturnStatement extends Statement {

    ReturnStatement withSource(Source newSource);

    interface Builder extends Statement.Builder<Builder> {
        @Fluent
        Builder setExpression(Expression expression);

        ReturnStatement build();
    }

    String NAME = "return";

    @Override
    default String name() {
        return NAME;
    }
}
