/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
