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

package org.e2immu.language.cst.impl.expression;

import org.e2immu.language.cst.api.expression.BinaryOperator;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.StringConcat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class TestStringConcat extends CommonTest {
    @Test
    public void test0() {
        Expression e1 = make(r.negate(a));
        assertEquals("\"abc\"+!a", e1.toString());
        Expression e2 = make(r.negate(k));
        assertEquals("\"abc\"+-k", e2.toString());
        Expression e3 = r.binaryOperator((BinaryOperator) e2);
        assertEquals("\"abc\"+-k", e3.toString());
        assertInstanceOf(StringConcat.class, e3);
    }

    private Expression make(Expression rhs) {
        return r.newBinaryOperatorBuilder()
                .setParameterizedType(r.stringParameterizedType())
                .setOperator(r.plusOperatorString())
                .setLhs(r.newStringConstant("abc"))
                .setRhs(rhs)
                .setPrecedence(r.precedenceAdditive()).build();
    }

}
