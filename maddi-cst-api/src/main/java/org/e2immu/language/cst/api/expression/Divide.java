package org.e2immu.language.cst.api.expression;

public interface Divide extends BinaryOperator {
    String NAME = "divide";

    @Override
    default String name() {
        return NAME;
    }
}
