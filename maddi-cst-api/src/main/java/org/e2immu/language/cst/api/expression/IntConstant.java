package org.e2immu.language.cst.api.expression;

public interface IntConstant extends Numeric, ConstantExpression<Integer> {

    String NAME = "intConstant";

    @Override
    default String name() {
        return NAME;
    }
}
