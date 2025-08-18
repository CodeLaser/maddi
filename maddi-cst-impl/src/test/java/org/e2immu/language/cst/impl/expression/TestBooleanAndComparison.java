/*
 * e2immu: a static code analyser for effective and eventual immutability
 * Copyright 2020-2021, Bart Naudts, https://www.e2immu.org
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
import org.e2immu.language.cst.api.expression.GreaterThanZero;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestBooleanAndComparison extends CommonTest {

    // this test verifies that combining preconditions will work.

    @Test
    public void test1() {
        GreaterThanZero iGe0 = (GreaterThanZero) r.greater(i, r.newInt(0), true);
        GreaterThanZero iLt0 = (GreaterThanZero) r.less(i, r.newInt(0), false);
        GreaterThanZero jGe0 = (GreaterThanZero) r.greater(j, r.newInt(0), true);

        Expression iGe0_and__iLt0_or_jGe0 = r.and(iGe0, r.or(iLt0, jGe0));
        assertEquals("i>=0&&j>=0", iGe0_and__iLt0_or_jGe0.toString());

        Expression addIGe0Again = r.and(iGe0_and__iLt0_or_jGe0, iGe0);
        assertEquals(iGe0_and__iLt0_or_jGe0, addIGe0Again);

        Expression addIGe0Again2 = r.and(iGe0, iGe0_and__iLt0_or_jGe0);
        assertEquals(iGe0_and__iLt0_or_jGe0, addIGe0Again2);
    }

    @Test
    public void test2() {
        Expression iLe3 = r.less(i, r.newInt(3), true);
        Expression iGe4 = r.greater(i, r.newInt(4), true);
        Expression iGe5 = r.greater(i, r.newInt(5), true);
        Expression or = r.or(iGe4, iLe3);
        assertTrue(or.isBoolValueTrue(), "Have " + or);
        Expression or2 = r.or(iLe3, iGe4);
        assertTrue(or2.isBoolValueTrue(), "Have " + or);
        Expression or3 = r.or(iLe3, iGe5);
        assertFalse(or3.isBoolValueTrue());
        Expression or4 = r.or(iLe3, iLe3);
        assertFalse(or4.isBoolValueTrue());
    }

    @Test
    public void test3() {
        // "l0>=11||l0<=2||l0<=10"
        Expression iLe2 = r.less(i, r.newInt(2), true);
        Expression iLe10 = r.less(i, r.newInt(10), true);
        Expression iGe11 = r.greater(i, r.newInt(11), true);
        Expression or1 = r.or(iGe11, iLe10);
        assertTrue(or1.isBoolValueTrue());
        Expression or2 = r.or(iLe2, iLe10);
        assertEquals(iLe10, or2);
        Expression or3 = r.or(iGe11, iLe2, iLe10);
        assertTrue(or3.isBoolValueTrue(), "Got " + or3);
        Expression or4 = r.or(iLe10, iGe11, iLe2);
        assertTrue(or4.isBoolValueTrue(), "Got " + or3);
        Expression or5 = r.or(iLe2, iGe11, iLe10);
        assertTrue(or5.isBoolValueTrue(), "Got " + or3);
    }
}
