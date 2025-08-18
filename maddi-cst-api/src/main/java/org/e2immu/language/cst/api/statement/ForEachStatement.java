package org.e2immu.language.cst.api.statement;

import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.expression.Expression;

public interface ForEachStatement extends LoopStatement {
    LocalVariableCreation initializer();

    interface Builder extends Statement.Builder<Builder> {

        @Fluent
        Builder setInitializer(LocalVariableCreation localVariableCreation);

        @Fluent
        Builder setExpression(Expression expression);

        @Fluent
        Builder setBlock(Block block);

        ForEachStatement build();
    }

    String NAME = "forEach";

    @Override
    default String name() {
        return NAME;
    }
}
