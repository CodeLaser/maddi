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

package io.codelaser.maddi.cst.impl.expression;

import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.GreaterThanZero;
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
