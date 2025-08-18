package org.e2immu.language.cst.api.expression;

public interface Equals extends BinaryOperator {
    String NAME = "equals";

    @Override
    default String name() {
        return NAME;
    }
}
