package org.e2immu.language.cst.api.expression;

import org.e2immu.language.cst.api.info.InfoMap;

public interface ConstantExpression<T> extends Expression {

    @Override
    default boolean isConstant() {
        return true;
    }

    T constant();

    @Override
    default Expression rewire(InfoMap infoMap) {
        return this;
    }
}
