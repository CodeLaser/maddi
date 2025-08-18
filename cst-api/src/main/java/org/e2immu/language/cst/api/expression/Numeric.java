package org.e2immu.language.cst.api.expression;

public interface Numeric extends Expression {
    Number number();

    double doubleValue();

    Expression negate();

    Expression bitwiseNegation();

    @Override
    default boolean isNegatedOrNumericNegative() {
        return doubleValue() < 0;
    }

    @Override
    default boolean isNumeric() {
        return true;
    }

    @Override
    default Double numericValue() {
        return doubleValue();
    }
}
