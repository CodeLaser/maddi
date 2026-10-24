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

package io.codelaser.maddi.cst.impl.expression;

import io.codelaser.maddi.cst.api.expression.Expression;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/*
NOTE: there are some rules commented out in EvalOr
 */
public class TestGreaterThanAndOr extends CommonTest {

    // l<m  || -m<=l<=m  === l<=|m|
    @Test
    public void test5() {
        Expression e1 = r.less(l, m, false);
        assertEquals("m>l", e1.toString());
        Expression e2 = r.less(l, m, true);
        Expression e3 = r.greater(l, r.negate(m), true);
        Expression e23 = r.and(e2, e3);
        assertEquals("m>=l&&l+m>=0", e23.toString());
      // FIXME  assertEquals("m>=l", r.or(e1, e23).toString());
    }

    // l<m  && -m<=l<=m  === -m<=l<m
    @Test
    public void test5b() {
        Expression e1 = r.less(l, m, false);
        assertEquals("m>l", e1.toString());
        Expression e2 = r.less(l, m, true);
        Expression e3 = r.greater(l, r.negate(m), true);
        Expression e23 = r.and(e2, e3);
        assertEquals("m>=l&&l+m>=0", e23.toString());
        assertEquals("m>l&&l+m>=0", r.and(e1, e23).toString());
    }

    // l<=m  || -m<=l<=m  === l<=m
    @Test
    public void test5c() {
        Expression e1 = r.less(l, m, true);
        assertEquals("m>=l", e1.toString());
        Expression e2 = r.less(l, m, true);
        Expression e3 = r.greater(l, r.negate(m), true);
        Expression e23 = r.and(e2, e3);
        assertEquals("m>=l&&l+m>=0", e23.toString());
        assertEquals("m>=l", r.or(e1, e23).toString());
    }

    // l<m  || -m<=l<m  === l<m
    @Test
    public void test5d() {
        Expression e1 = r.less(l, m, false);
        assertEquals("m>l", e1.toString());
        Expression e2 = r.less(l, m, false);
        Expression e3 = r.greater(l, r.negate(m), true);
        Expression e23 = r.and(e2, e3);
        assertEquals("m>l&&l+m>=0", e23.toString());
        assertEquals("m>l", r.or(e1, e23).toString());
    }

    // l>m  || -m<=l<=m  === -|m|<=l
    @Test
    public void test5e() {
        Expression e1 = r.greater(l, m, false);
        assertEquals("l>m", e1.toString());
        Expression e2 = r.less(l, m, true);
        Expression e3 = r.greater(l, r.negate(m), true);
        Expression e23 = r.and(e2, e3);
        assertEquals("m>=l&&l+m>=0", e23.toString());
      // FIXME  assertEquals("l+m>=0", r.or(e1, e23).toString());
    }

    // l>m  || l<=m  === true
    @Test
    public void test5f() {
        Expression e1 = r.greater(l, m, false);
        assertEquals("l>m", e1.toString());
        Expression e2 = r.less(l, m, true);
        assertEquals("m>=l", e2.toString());
        assertEquals("true", r.or(e1, e2).toString());
    }

    // l>m  && l<=m  === false
    @Test
    public void test5g() {
        Expression e1 = r.greater(l, m, false);
        assertEquals("l>m", e1.toString());
        Expression e2 = r.less(l, m, true);
        assertEquals("m>=l", e2.toString());
        assertEquals("false", r.and(e1, e2).toString());
    }

    // l>m  && l<m  === false
    @Test
    public void test5h() {
        Expression e1 = r.greater(l, m, false);
        assertEquals("l>m", e1.toString());
        Expression e2 = r.less(l, m, false);
        assertEquals("m>l", e2.toString());
        assertEquals("false", r.and(e1, e2).toString());
    }

    // l>=m  && l<=m  === l==m
    @Test
    public void test5i() {
        Expression e1 = r.greater(l, m, true);
        assertEquals("l>=m", e1.toString());
        Expression e2 = r.less(l, m, true);
        assertEquals("m>=l", e2.toString());
        assertEquals("l==m", r.and(e1, e2).toString());
    }

    // l>m  || l<m  === l!=m
    @Test
    public void test5j() {
        Expression e1 = r.greater(l, m, false);
        assertEquals("l>m", e1.toString());
        Expression e2 = r.less(l, m, false);
        assertEquals("m>l", e2.toString());
        assertEquals("l!=m", r.or(e1, e2).toString());
    }

    // l>m || l>=m   ===   l>=m
    @Test
    public void test5k() {
        Expression e1 = r.greater(l, m, false);
        assertEquals("l>m", e1.toString());
        Expression e2 = r.greater(l, m, true);
        assertEquals("l>=m", e2.toString());
        assertEquals("l>=m", r.or(e1, e2).toString());
    }

    // l>=m || l>m   ===   l>=m   (solved by sorting wrt 5k)
    @Test
    public void test5l() {
        Expression e1 = r.greater(l, m, false);
        assertEquals("l>m", e1.toString());
        Expression e2 = r.greater(l, m, true);
        assertEquals("l>=m", e2.toString());
        assertEquals("l>=m", r.or(e2, e1).toString());
    }

    // l>m  || l+m>=0 === l>m || l>=-m  === l>=-|m|
    // FIXME commented out at the moment in EvalOr
    @Test
    public void test5m() {
        Expression e1 = r.greater(l, m, false);
        assertEquals("l>m", e1.toString());
        Expression e2 = r.greater(l, r.negate(m), true);
        assertEquals("l+m>=0", e2.toString());
        assertEquals("l>m||l+m>=0", r.or(e1, e2).toString());
    }

    // l>m && l>=m  === l > m
    @Test
    public void test5n() {
        Expression e1 = r.greater(l, m, false);
        assertEquals("l>m", e1.toString());
        Expression e2 = r.greater(l, m, true);
        assertEquals("l>=m", e2.toString());
        assertEquals("l>m", r.and(e1, e2).toString());
    }

    // maddi#170: two strict comparisons on one bound miss the bound itself
    @Test
    public void strictPairOnDoubleIsNotATautology() {
        Expression lt = r.less(l, r.newDouble(2.5), false);
        Expression gt = r.greater(l, r.newDouble(2.5), false);
        assertEquals("l<2.5||l>2.5", r.or(lt, gt).toString());
        Expression lt3 = r.less(l, r.newDouble(3), false);
        Expression gt3 = r.greater(l, r.newDouble(3), false);
        assertEquals("l<3.0||l>3", r.or(lt3, gt3).toString());
    }

    // maddi#170: one side admitting equality closes the gap
    @Test
    public void nonStrictPairOnDoubleIsATautology() {
        assertEquals("true", r.or(r.less(l, r.newDouble(2.5), true), r.greater(l, r.newDouble(2.5), false)).toString());
        assertEquals("true", r.or(r.less(l, r.newDouble(2.5), false), r.greater(l, r.newDouble(2.5), true)).toString());
        assertEquals("true", r.or(r.less(l, r.newDouble(2.5), true), r.greater(l, r.newDouble(2.5), true)).toString());
    }

    // maddi#170: integer-valued bounds on a double do not get the integer rule: 3.5 lies between 3.0 and 4.0
    @Test
    public void integerBoundsOnDoubleLeaveAGap() {
        Expression or = r.or(r.less(l, r.newDouble(3), true), r.greater(l, r.newDouble(4), true));
        assertNotEquals("true", or.toString());
    }

    // the integer rule itself is unchanged
    @Test
    public void integerPairs() {
        assertEquals("k<3||k>=4", r.or(r.less(k, r.newInt(3), false), r.greater(k, r.newInt(3), false)).toString());
        assertEquals("true", r.or(r.less(k, r.newInt(3), true), r.greater(k, r.newInt(3), true)).toString());
        assertEquals("true", r.or(r.less(k, r.newInt(4), true), r.greater(k, r.newInt(5), true)).toString());
        assertEquals("true", r.or(r.less(k, r.newInt(5), false), r.greater(k, r.newInt(4), false)).toString());
        assertEquals("true", r.or(r.less(k, r.newInt(5), false), r.greater(k, r.newInt(5), true)).toString());
    }
}
