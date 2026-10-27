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
