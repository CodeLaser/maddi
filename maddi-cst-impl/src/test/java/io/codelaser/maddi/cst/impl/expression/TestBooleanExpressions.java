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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestBooleanExpressions extends CommonTest {

    /* the following test simulates

        if(a && b) return 1;
        if(!a && !b) return 2;
        // where are we here?

     which is identical to

        if(a && b  || !a && !b) return;
        // where are we here?
     */
    @Test
    public void test1() {
        Expression aAndB = r.and(a, b);
        assertEquals("a&&b", aAndB.toString());
        assertEquals(5, aAndB.complexity());

        Expression not_AAndB = r.negate(aAndB);
        assertEquals("!a||!b", not_AAndB.toString());

        Expression notAAndNotB = r.and(r.negate(a), r.negate(b));
        assertEquals("!a&&!b", notAAndNotB.toString());
        assertEquals(7, notAAndNotB.complexity());

        Expression notNotAAndNotB = r.negate(notAAndNotB);
        assertEquals("a||b", notNotAAndNotB.toString());

        // then we combine these two
        Expression n1AndN2 = r.and(not_AAndB, notNotAAndNotB);
        assertEquals("(a||b)&&(!a||!b)", n1AndN2.toString());

        Expression notA_andB = r.and(r.negate(a), b);
        Expression notBOrA = r.negate(notA_andB);
        assertEquals("a||!b", notBOrA.toString());

        Expression n1AndN2AndN3 = r.and(n1AndN2, notBOrA);
        assertEquals("a&&!b", n1AndN2AndN3.toString());
    }

    @Test
    public void testEither() {
        Expression aAndB = r.and(a, b);
        Expression notAandNotB = r.and(r.negate(a), r.negate(b));
        Expression aAndB_or_notAandNotB = r.or(aAndB, notAandNotB);
        assertEquals("a&&b||!a&&!b", aAndB_or_notAandNotB.toString());

        Expression not__aAndB_or_notAandNotB = r.negate(aAndB_or_notAandNotB);
        assertEquals("(a||b)&&(!a||!b)", not__aAndB_or_notAandNotB.toString());

        Expression combined = r.and(not__aAndB_or_notAandNotB, aAndB_or_notAandNotB);
        assertEquals("false", combined.toString());
        Expression combined2 = r.and(aAndB_or_notAandNotB, not__aAndB_or_notAandNotB);
        assertEquals("false", combined2.toString());
    }

    @Test
    public void testEither2() {
        // IMPORTANT: this test assumes that we don't know about nullability
        Expression aNull =r. equals(a, r.nullConstant());
        Expression aNotNull = r.negate(r.equals(a, r.nullConstant()));
        Expression bNull = r.equals(b, r.nullConstant());
        Expression bNotNull = r.negate(r.equals(b, r.nullConstant()));

        Expression aNullOrBNotNull = r.or(aNull, bNotNull);
        assertEquals("null==a||null!=b", aNullOrBNotNull.toString());
        Expression aNotNullOrBNull = r.or(aNotNull, bNull);
        assertEquals("null!=a||null==b", aNotNullOrBNull.toString());
        Expression and = r.and(aNullOrBNotNull, aNotNullOrBNull);
        assertEquals("(null==a||null!=b)&&(null!=a||null==b)", and.toString());
        Expression notAnd = r.negate(and);
        assertEquals("null==a&&null!=b||null!=a&&null==b", notAnd.toString());
        Expression notNotAnd = r.negate(notAnd);
        assertEquals("(null==a||null!=b)&&(null!=a||null==b)", notNotAnd.toString());
    }
}
