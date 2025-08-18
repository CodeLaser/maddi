package org.e2immu.language.cst.api.expression;

public interface Remainder extends BinaryOperator {
    String NAME = "remainder";

    @Override
    default String name() {
        return NAME;
    }
}
