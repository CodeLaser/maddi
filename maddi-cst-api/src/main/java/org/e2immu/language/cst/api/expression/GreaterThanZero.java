package org.e2immu.language.cst.api.expression;

import org.e2immu.language.cst.api.runtime.Runtime;

public interface GreaterThanZero extends Expression {
    Expression expression();

    boolean allowEquals();

    XB extract(Runtime runtime);

    interface XB {
        Expression x();

        double b();

        boolean lessThan();
    }

    String NAME = "greaterThanZero";

    @Override
    default String name() {
        return NAME;
    }
}
