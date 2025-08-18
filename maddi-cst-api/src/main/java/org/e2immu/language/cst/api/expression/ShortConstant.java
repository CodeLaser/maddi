package org.e2immu.language.cst.api.expression;

public interface ShortConstant extends Numeric, ConstantExpression<Short> {
    String NAME = "shortConstant";

    @Override
    default String name() {
        return NAME;
    }
}
