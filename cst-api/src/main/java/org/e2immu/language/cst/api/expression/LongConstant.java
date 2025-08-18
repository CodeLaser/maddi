package org.e2immu.language.cst.api.expression;

public interface LongConstant extends Numeric, ConstantExpression<Long> {
    String NAME = "longConstant";

    @Override
    default String name() {
        return NAME;
    }
}
