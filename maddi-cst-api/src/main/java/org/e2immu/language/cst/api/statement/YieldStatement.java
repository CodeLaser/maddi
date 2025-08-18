package org.e2immu.language.cst.api.statement;

import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.Expression;

public interface YieldStatement extends Statement {

    YieldStatement withSource(Source newSource);

    interface Builder extends Statement.Builder<Builder> {
        @Fluent
        Builder setExpression(Expression expression);

        YieldStatement build();
    }

    String NAME = "yield";

    @Override
    default String name() {
        return NAME;
    }
}
