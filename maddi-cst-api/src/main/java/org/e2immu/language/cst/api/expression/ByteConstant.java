package org.e2immu.language.cst.api.expression;

public interface ByteConstant extends Numeric, ConstantExpression<Byte> {
    String NAME = "byteConstant";

    @Override
    default String name() {
        return NAME;
    }
}
