package org.e2immu.language.cst.api.expression;

public interface CharConstant extends ConstantExpression<Character> {
    String NAME = "charConstant";

    @Override
    default String name() {
        return NAME;
    }
}
