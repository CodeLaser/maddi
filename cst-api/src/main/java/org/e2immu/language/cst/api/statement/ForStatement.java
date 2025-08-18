package org.e2immu.language.cst.api.statement;

import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.expression.Expression;

import java.util.List;

public interface ForStatement extends LoopStatement {
    // can be either a LocalVariableCreation (Statement) or any expression
    List<Element> initializers();

    List<Expression> updaters();

    ForStatement withInitializers(List<Element> elements);

    interface Builder extends Statement.Builder<Builder> {

        @Fluent
        Builder addInitializer(Element initializer);

        @Fluent
        Builder setExpression(Expression expression);

        @Fluent
        Builder addUpdater(Expression expression);

        @Fluent
        Builder setBlock(Block block);

        ForStatement build();
    }

    String NAME = "for";

    @Override
    default String name() {
        return NAME;
    }
}
