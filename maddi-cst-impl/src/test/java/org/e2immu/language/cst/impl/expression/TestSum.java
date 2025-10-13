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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestSum extends CommonTest {


    @Test
    public void test1() {
        Expression s = r.sum(r.intOne(), i);
        assertEquals("1+i", s.toString());
        Expression s2 = r.sum(r.newInt(2), s);
        assertEquals("3+i", s2.toString());
        Expression s3 = r.sum(i, r.intOne());
        assertEquals(s, s3);
        assertEquals("1+i", s3.toString());
    }

    @Test
    public void test4() {
        Expression s1 = r.sum(i, j);
        Expression s2 = r.sum(j, i);
        assertEquals(s1, s2);
        assertEquals("i+j", s1.toString());
        assertEquals("i+j", s2.toString());
    }

    @Test
    public void test2() {
        Expression s = r.sum(r.intOne(), r.sum(i, r.newInt(3)));
        assertEquals("4+i", s.toString());
        Expression s2 = r.sum(r.sum(r.newInt(3), r.newInt(2)), r.sum(s, r.newInt(-9)));
        assertEquals("i", s2.toString());
        Expression s3 = r.sum(r.sum(s2, r.newInt(3)), r.negate(s2));
        assertEquals("3", s3.toString());
    }

    @Test
    public void test3() {
        Expression s = r.sum(r.sum(r.intMinusOne(), r.product(j, r.newInt(-3))), r.product(r.newInt(2), i));
        assertEquals("-1+2*i+-3*j", s.toString());
    }
}
