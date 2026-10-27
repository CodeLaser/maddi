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

import io.codelaser.maddi.cst.api.expression.BinaryOperator;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.StringConcat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class TestStringConcat extends CommonTest {
    @Test
    public void test0() {
        Expression e1 = make(r.negate(a));
        assertEquals("\"abc\"+!a", e1.toString());
        Expression e2 = make(r.negate(k));
        assertEquals("\"abc\"+-k", e2.toString());
        Expression e3 = r.binaryOperator((BinaryOperator) e2);
        assertEquals("\"abc\"+-k", e3.toString());
        assertInstanceOf(StringConcat.class, e3);
    }

    private Expression make(Expression rhs) {
        return r.newBinaryOperatorBuilder()
                .setParameterizedType(r.stringParameterizedType())
                .setOperator(r.plusOperatorString())
                .setLhs(r.newStringConstant("abc"))
                .setRhs(rhs)
                .setPrecedence(r.precedenceAdditive()).build();
    }

}
