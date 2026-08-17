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

import org.e2immu.language.cst.api.expression.Expression;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestAndComparison extends CommonTest {

    /*
    important, even though vine is nullable, it is of type int

    @Test
    public void test1() {
        Expression inIsNull = r.equals(vine, r.nullConstant());
        assertEquals("null==in", inIsNull.toString());
        Expression inIsNotNull = r.negate(inIsNull);
        assertEquals("null!=in", inIsNotNull.toString());
        Expression inGE0 = r.greater(vine, r.newInt(0), true);
        assertEquals("in>=0", inGE0.toString());
        Expression inLT0 = r.less(vine, r.newInt(0), false);
        assertEquals("in<0", inLT0.toString());

        Expression and1 = r.and(inIsNotNull, inGE0);
        Expression and2 = r.and(inIsNotNull, inLT0);
        Expression and = r.and(and1, and2);
        assertEquals("false", and.toString());
    }
*/
    // NOTE: this test also fails in the old e2immu version
    @Disabled("Current system sees 0.0 as 0, and treats less than as <=; this is not a good combination")
    @Test
    public void test2() {
        Expression lGE0 = r.greater(l, r.newInt(0), true);
        assertEquals("l>=0", lGE0.toString());
        Expression lLT0 = r.less(l, r.newInt(0), false);
        assertEquals("l<0", lLT0.toString());
        Expression and = r.and(lGE0, lLT0);
        assertEquals("false", and.toString());
    }

    @Test
    public void test1() {
        Expression iEq5 = r.equals(i, r.newInt(5));
        assertEquals("5==i", iEq5.toString());
        Expression iGe0 = r.greaterThanZero(i, true);
        assertEquals("i>=0", iGe0.toString());
        Expression and = r.and(iEq5, iGe0);
        assertEquals(iEq5, and);
    }

    @Test
    public void test1b() {
        Expression iEq5 = r.equals(i, r.newInt(5));
        assertEquals("5==i", iEq5.toString());
        Expression iGe0 = r.less(i, r.intZero(), false);
        assertEquals("i<0", iGe0.toString());
        Expression and = r.and(iEq5, iGe0);
        assertEquals(r.constantFalse(), and);
    }
}
