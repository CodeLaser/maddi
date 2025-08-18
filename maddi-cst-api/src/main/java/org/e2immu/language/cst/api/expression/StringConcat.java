package org.e2immu.language.cst.api.expression;

public interface StringConcat extends BinaryOperator {
    String NAME = "stringConcat";

    @Override
    default String name() {
        return NAME;
    }
}
