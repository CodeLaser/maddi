package org.e2immu.language.cst.api.expression;

import org.e2immu.language.cst.api.info.MethodInfo;

public interface UnaryOperator extends Expression {
    Expression expression();

    MethodInfo operator();

    Precedence precedence();

    String NAME = "unaryOperator";

    @Override
    default String name() {
        return NAME;
    }
}
