package org.e2immu.language.cst.api.expression;

public interface DoubleConstant extends Numeric, ConstantExpression<Double> {

    String NAME = "doubleConstant";

    @Override
    default String name() {
        return NAME;
    }
}
