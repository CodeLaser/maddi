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

import io.codelaser.maddi.cst.api.expression.DoubleConstant;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.GreaterThanZero;
import io.codelaser.maddi.cst.api.expression.IntConstant;
import io.codelaser.maddi.util.IntUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestInequality extends CommonTest {

    @DisplayName("basics")
    @Test
    public void test1() {
        IntConstant one = r.newInt(1 );
        assertTrue(one.parameterizedType().isMathematicallyInteger());
        assertTrue(IntUtil.isMathematicalInteger(one.constant()));

        Expression kTimesI = multiply(k, i);
        Expression kTimesILtOne = r.less(kTimesI, one, true);
        assertEquals("k*i<2", kTimesILtOne.toString());
        if(kTimesILtOne instanceof GreaterThanZero gt) {
            assertFalse(gt.allowEquals()); // because 1.0 is an integer, we follow integer conventions
        } else fail();

        Expression and1 = r.and(kTimesILtOne, r.negate(a));
        assertEquals("!a&&k*i<2", and1.toString());
        Expression gt = r.greater(i, n, true);
        assertEquals("i>=n", gt.toString());
        Expression and2 = r.and(and1, gt);
        assertEquals("!a&&k*i<2&&i>=n", and2.toString());
    }

    @DisplayName("compare int to double, 1.0 is mathematically integer")
    @Test
    public void test2() {
        DoubleConstant onePointZero = r.newDouble(1.0);
        assertFalse(onePointZero.parameterizedType().isMathematicallyInteger());
        assertTrue(IntUtil.isMathematicalInteger(onePointZero.constant()));

        Expression lTimesM = multiply(l, m);
        Expression lTimesMLtOne = r.less(lTimesM, onePointZero, true);
        assertEquals("l*m<=1.0", lTimesMLtOne.toString());
        if(lTimesMLtOne instanceof GreaterThanZero gt) {
            assertTrue(gt.allowEquals());
        } else fail();

        Expression and1 = r.and(lTimesMLtOne, r.negate(a));
        assertEquals("!a&&l*m<=1.0", and1.toString());
        Expression gt = r.greater(i, n, true);
        assertEquals("i>=n", gt.toString());
        Expression and2 = r.and(and1, gt);
        assertEquals("!a&&i>=n&&l*m<=1.0", and2.toString());
    }

}
