package org.e2immu.language.cst.impl.expression.util;

import org.e2immu.language.cst.api.expression.Precedence;

// from https://introcs.cs.princeton.edu/java/11precedence/

public enum PrecedenceEnum implements Precedence {

    TOP(17), // constants
    ACCESS(16), // method invoke, object member access [] () .
    POST_INCREMENT(15), // post ++, --
    UNARY(14), // ! ~  + - pre ++ --
    CAST(13), // cast
    MULTIPLICATIVE(12), // * % /
    ADDITIVE(11), // + - string concat
    SHIFT(10), // << >> >>>
    RELATIONAL(9), // < <= > >= instanceof
    EQUALITY(8), // == !=
    AND(7), // &
    XOR(6), // ^
    OR(5), // |
    LOGICAL_AND(4), // &&
    LOGICAL_OR(3), // ||
    TERNARY(2), // ?:
    ASSIGNMENT(1), // assignment  = += -= *= %= /= &= ^= |= <<= >>= >>>=
    BOTTOM(0) // lambda, switch
    ;

    private final int value;


    PrecedenceEnum(int value) {
        this.value = value;
    }

    @Override
    public int value() {
        return value;
    }
}
