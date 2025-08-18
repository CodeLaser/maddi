package org.e2immu.language.cst.api.expression;

public interface Precedence {
    int value();

    default boolean greaterThan(Precedence other) {
        return value() > other.value();
    }
}
