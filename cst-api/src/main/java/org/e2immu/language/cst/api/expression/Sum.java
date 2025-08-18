package org.e2immu.language.cst.api.expression;


import org.e2immu.language.cst.api.runtime.Runtime;

public interface Sum extends BinaryOperator {

    Double numericPartOfLhs();

    Expression nonNumericPartOfLhs(Runtime runtime);

    Expression isZero(Runtime runtime);

    String NAME = "sum";

    @Override
    default String name() {
        return NAME;
    }
}
