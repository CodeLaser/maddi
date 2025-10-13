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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestAndSorting extends CommonTest {

    // null checks go to the front
    @Test
    public void test1() {
        Expression sIsNotNull = r.negate(r.equals(s, r.nullConstant()));
        assertEquals("null!=s", sIsNotNull.toString());
        Expression sLength = r.newMethodCallBuilder().setObject(s).setMethodInfo(r.andOperatorBool()).setParameterExpressions(List.of())
                .setConcreteReturnType(r.booleanParameterizedType()).build();
        assertEquals("s.&&()", sLength.toString());
        Expression a1 = r.and(sIsNotNull, sLength);
        assertEquals("null!=s&&s.&&()", a1.toString());
        Expression a2 = r.and(sLength, sIsNotNull);
        assertEquals("null!=s&&s.&&()", a2.toString());
    }

    // method calls remain in the same order
    @Test
    public void test2() {
        Expression sIsNotNull = r.negate(r.equals(s, r.nullConstant()));
        assertEquals("null!=s", sIsNotNull.toString());
        Expression s1 = r.newMethodCallBuilder().setObject(s).setMethodInfo(r.andOperatorBool()).setParameterExpressions(List.of())
                .setConcreteReturnType(r.booleanParameterizedType()).build();
        assertEquals("s.&&()", s1.toString());
        Expression s2 = r.newMethodCallBuilder().setObject(s).setMethodInfo(r.orOperatorBool()).setParameterExpressions(List.of())
                .setConcreteReturnType(r.booleanParameterizedType()).build();
        assertEquals("s.||()", s2.toString());

        Expression a1 = r.and(sIsNotNull, s1, s2);
        assertEquals("null!=s&&s.&&()&&s.||()", a1.toString());
        Expression a2 = r.and(s2, sIsNotNull, s1);
        assertEquals("null!=s&&s.||()&&s.&&()", a2.toString());
    }
}
