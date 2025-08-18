package org.e2immu.language.cst.api.statement;

import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.expression.Expression;

public interface SynchronizedStatement extends Statement {
    interface Builder extends Statement.Builder<Builder> {
        @Fluent
        Builder setExpression(Expression expression);

        @Fluent
        Builder setBlock(Block block);

        SynchronizedStatement build();
    }

    String NAME = "synchronized";

    @Override
    default String name() {
        return NAME;
    }
}
