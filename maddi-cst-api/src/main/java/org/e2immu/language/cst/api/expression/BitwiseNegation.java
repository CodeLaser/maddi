package org.e2immu.language.cst.api.expression;

public interface BitwiseNegation extends UnaryOperator, ExpressionWrapper {

    String NAME = "bitwiseNegation";

    @Override
    default String name() {
        return NAME;
    }
}
