package org.e2immu.language.cst.api.expression;

import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.runtime.Factory;

public interface InlineConditional extends Expression {
    Expression ifTrue();

    Expression ifFalse();

    Expression condition();

    interface Builder extends Element.Builder<Builder> {
        @Fluent
        Builder setIfTrue(Expression ifTrue);

        @Fluent
        Builder setIfFalse(Expression ifFalse);

        @Fluent
        Builder setCondition(Expression condition);

        InlineConditional build(Factory factory);
    }

    String NAME = "inlineConditional";

    @Override
    default String name() {
        return NAME;
    }
}
