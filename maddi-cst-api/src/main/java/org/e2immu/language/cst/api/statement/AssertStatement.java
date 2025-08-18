package org.e2immu.language.cst.api.statement;

import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.Expression;

public interface AssertStatement extends Statement {

    Expression message();

    AssertStatement withSource(Source newSource);

    interface Builder extends Statement.Builder<Builder> {
        @Fluent
        Builder setExpression(Expression expression);

        @Fluent
        Builder setMessage(Expression message);

        AssertStatement build();
    }

    String NAME = "assert";

    @Override
    default String name() {
        return NAME;
    }
}
