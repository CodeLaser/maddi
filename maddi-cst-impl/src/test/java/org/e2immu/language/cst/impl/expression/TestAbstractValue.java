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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestAbstractValue extends CommonTest {

    @Test
    public void test() {
        Expression notA = r.negate(a);
        assertEquals("!a", notA.toString());
        Expression notA2 = r.negate(a);
        assertEquals(notA, notA2);
        assertEquals(a, r.negate(notA));

        assertEquals(a, r.and(a, a));
        assertEquals(notA, r.and(notA, notA));
        assertEquals(FALSE, r.and(a, notA));

        // A && A, !A && !A
        assertEquals(a, r.and(a, a));
        assertEquals(notA, r.and(notA, notA));
        // A && !A, !A && A
        assertEquals(FALSE, r.and(a, notA));
        assertEquals(FALSE, r.and(notA, a));

        // F || T
        assertEquals(TRUE, r.or(FALSE, TRUE));
        // A || A, !A || !A
        assertEquals(a, r.or(a, a));
        assertEquals(notA, r.or(notA, notA));
        // A || !A, !A || A
        assertEquals(TRUE, r.or(a, notA));
        assertEquals(TRUE, r.or(notA, a));
    }


    @Test
    public void testAndOfTrues() {
        Expression v = r.and(TRUE, TRUE);
        assertEquals(TRUE, v);
    }

    @Test
    public void testMoreComplicatedAnd() {
        Expression aAndAOrB = r.and(a, r.or(a, b));
        assertEquals(a, aAndAOrB);

        Expression aAndNotAOrB = r.and(a, r.or(r.negate(a), b));
        assertEquals("a&&b", aAndNotAOrB.toString());

        //D && A && !B && (!A || B) && C (the && C, D is there just for show)
        Expression v = r.and(d, a, r.negate(b), r.or(r.negate(a), b), c);
        assertEquals(FALSE, v);
    }

    @Test
    public void testExpandAndInOr() {
        // A || (B && C): as of 202501, we're not expanding anymore
        Expression v = r.or(a, r.and(b, c));
        assertEquals("a||b&&c", v.toString());
    }

    @Test
    public void testInstanceOf() {
        Expression iva = newInstanceOf(a, r.stringParameterizedType());
        assertEquals("a instanceof String", iva.toString());
        Expression ivb = newInstanceOf(b, r.stringParameterizedType());
        Expression or = r.or(ivb, iva);
        assertEquals("a instanceof String||b instanceof String", or.toString());
        Expression iva2 = newInstanceOf(a, r.objectParameterizedType());
        Expression or2 = r.or(iva, iva2);
        assertEquals("a instanceof Object||a instanceof String", or2.toString());
    }

    /*
        Map<Variable, Boolean> nullClauses(Expression v, boolean accept) {
            Filter filter = new Filter(context, filterMode);
            return filter.filter(v, filter.individualNullOrNotNullClause()).accepted()
                    .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                            e -> e.getValue() instanceof Equals Equals && Equals.lhs == NullConstant.NULL_CONSTANT));
        }

        // note: van and vbn are nullable, va and vb are NOT (see CommonAbstractValue)
        @Test
        public void testIsNull() {
            Expression v = r.equals(an, r.nullConstant());
            assertEquals("null==an", v.toString());
            Map<Variable, Boolean> nullClauses = nullClauses(v, Filter.FilterMode.ACCEPT);
            assertEquals(1, nullClauses.size());
            assertEquals(true, nullClauses.get(van));

            Expression v2 = r.equals(bn, r.nullConstant());
            assertEquals("null==bn", v2.toString());
            Map<Variable, Boolean> nullClauses2 = nullClauses(v2, Filter.FilterMode.ACCEPT);
            assertEquals(1, nullClauses2.size());
            assertEquals(true, nullClauses2.get(vbn));

            Expression orValue = r.or(v, r.negate(v2));
            assertEquals("null==an||null!=bn", orValue.toString());
            Map<Variable, Boolean> nullClausesAnd = nullClauses(orValue, Filter.FilterMode.REJECT);
            assertEquals(2, nullClausesAnd.size());
            assertEquals(true, nullClausesAnd.get(van));
            assertEquals(false, nullClausesAnd.get(vbn));
        }

        @Test
        public void testIsNotNull() {
            Expression v = r.negate(r.equals(r.nullConstant(), a));
            assertEquals("null!=a", v.toString());
            Map<Variable, Boolean> nullClauses = nullClauses(v, Filter.FilterMode.REJECT);
            assertEquals(1, nullClauses.size());
            assertEquals(false, nullClauses.get(va));
        }
    */

    // as of 202501, we're not expanding an or of ands.
    public static final String EXPECTED = "a&&b||c&&d";
    public static final String EXPECTED2 = "a&&!b||!c&&d";

    @Test
    public void testCNF() {
        // (a && b) || (c && d)
        Expression and1 = r.and(a, b);
        assertEquals("a&&b", and1.toString());
        Expression and2 = r.and(c, d);
        assertEquals("c&&d", and2.toString());
        Expression or = r.or(and1, and2);
        assertEquals(EXPECTED, or.toString());
        Expression or2 = r.or(r.and(b, a), r.and(d, c));
        assertEquals(EXPECTED, or2.toString());
        Expression or3 = r.or(r.and(d, c), r.and(b, a));
        assertEquals(EXPECTED, or3.toString());
    }

    @Test
    public void testCNFWithNot() {
        Expression notB = r.negate(b);
        Expression notC = r.negate(c);
        Expression or = r.or(r.and(a, notB), r.and(notC, d));
        assertEquals(EXPECTED2, or.toString());
        Expression or2 = r.or(r.and(notB, a), r.and(d, notC));
        assertEquals(EXPECTED2, or2.toString());
        Expression or3 = r.or(r.and(d, notC), r.and(notB, a));
        assertEquals(EXPECTED2, or3.toString());
    }

    // (not ('a' == c (parameter 0)) and not ('b' == c (parameter 0)) and ('a' == c (parameter 0) or 'b' == c (parameter 0)))
    // not a and not b and (a or b)

    @Test
    public void testForSwitchStatement() {
        Expression v = r.and(r.negate(a), r.negate(b), r.or(a, b));
        assertEquals(FALSE, v);

        Expression cIsA = r.equals(r.newChar('a'), c);
        Expression cIsABis = r.equals(r.newChar('a'), c);
        assertEquals(cIsA, cIsABis);

        Expression cIsB = r.equals(r.newChar('b'), c);

        Expression v2 = r.and(r.negate(cIsA), r.negate(cIsB), r.or(cIsA, cIsB));
        assertEquals(FALSE, v2);
    }

    @Test
    public void testCompare() {
        Expression aGt4 = r.greater(i, r.newInt(4), true);
        assertEquals("i>=4", aGt4.toString());

        Expression n4ltB = r.less(r.newInt(4), i, false);
        assertEquals("i>=5", n4ltB.toString());

        Expression n4lt8 = r.less(r.newInt(4), r.newInt(8), false);
        assertEquals(TRUE, n4lt8);
    }

    @Test
    public void testSumProduct() {
        Expression aa = r.sum(a, a);
        assertEquals("2*a", aa.toString());
        Expression a0 = r.sum(a, r.newInt(0));
        assertEquals(a, a0);
        Expression aTimes0 = r.product(a, r.newInt(0));
        assertEquals(r.newInt(0), aTimes0);

        Expression a3a = r.sum(a,
                r.product(r.newInt(3), a));
        assertEquals("4*a", a3a.toString());
        Expression b2 = r.product(b, r.newInt(2));
        Expression b4 = r.product(r.newInt(4), b);
        Expression b4b2 = r.sum(b4, b2);
        assertEquals("6*b", b4b2.toString());
    }
}
