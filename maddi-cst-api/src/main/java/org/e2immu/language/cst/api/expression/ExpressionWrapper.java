package org.e2immu.language.cst.api.expression;

public interface ExpressionWrapper {
    Expression expression();

    int wrapperOrder();
}
