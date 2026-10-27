/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.cst.impl.expression.util;

import io.codelaser.maddi.cst.api.expression.Precedence;

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
