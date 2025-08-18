package org.e2immu.language.cst.api.statement;

import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.expression.Expression;

public interface DoStatement extends LoopStatement {

    interface Builder extends Statement.Builder<Builder> {
        @Fluent
        Builder setExpression(Expression expression);

        @Fluent
        Builder setBlock(Block block);

        DoStatement build();
    }

    String NAME = "do";

    @Override
    default String name() {
        return NAME;
    }
}
