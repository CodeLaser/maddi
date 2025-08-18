package org.e2immu.language.cst.api.expression;

public interface FloatConstant extends Numeric, ConstantExpression<Float> {
    String NAME = "floatConstant";

    @Override
    default String name() {
        return NAME;
    }
}
