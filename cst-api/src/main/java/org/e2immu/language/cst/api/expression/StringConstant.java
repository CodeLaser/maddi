package org.e2immu.language.cst.api.expression;

public interface StringConstant extends ConstantExpression<String> {
    String NAME = "stringConstant";

    @Override
    default String name() {
        return NAME;
    }

}
