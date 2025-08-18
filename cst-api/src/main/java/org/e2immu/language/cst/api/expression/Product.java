package org.e2immu.language.cst.api.expression;


public interface Product extends BinaryOperator {
    String NAME = "product";

    @Override
    default String name() {
        return NAME;
    }
}
