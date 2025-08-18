package org.e2immu.language.cst.api.expression;

public interface Negation extends UnaryOperator, ExpressionWrapper {

    @Override
    default boolean isNegatedOrNumericNegative() {
        return true;
    }

    String NAME = "negation";

    @Override
    default String name() {
        return NAME;
    }
}
