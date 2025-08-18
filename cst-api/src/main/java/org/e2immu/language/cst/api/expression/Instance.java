package org.e2immu.language.cst.api.expression;

// expression type used in e2immu analyzer

public interface Instance extends Expression {
    String NAME = "instance";

    @Override
    default String name() {
        return NAME;
    }
}
