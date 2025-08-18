package org.e2immu.language.cst.api.statement;

import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.Expression;

public interface ExpressionAsStatement extends Statement {

    interface Builder extends Statement.Builder<Builder> {

        @Fluent
        Builder setExpression(Expression expression);
        ExpressionAsStatement build();

    }
    String NAME = "expressionAsStatement";

    @Override
    default String name() {
        return NAME;
    }

    ExpressionAsStatement withSource(Source newSource);
}
