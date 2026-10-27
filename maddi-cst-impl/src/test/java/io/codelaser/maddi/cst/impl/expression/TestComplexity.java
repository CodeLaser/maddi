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
import io.codelaser.maddi.cst.api.expression.IntConstant;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.variable.DependentVariable;
import io.codelaser.maddi.cst.api.variable.Variable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestComplexity extends CommonTest {

    @Test
    public void test() {
        Variable v1 = createVariable("a", r.intParameterizedType());
        assertEquals(2, v1.complexity());
        VariableExpression ve1 = r.newVariableExpression(v1);
        assertEquals(2, ve1.complexity());
        IntConstant one = r.intOne();
        assertEquals(1, one.complexity());
        Expression sum = r.sum(ve1, one);
        assertEquals("1+a", sum.toString());
        assertEquals(4, sum.complexity());
    }

    @Test
    public void test2() {
        Variable v1 = createVariable("a", r.intParameterizedType().copyWithArrays(1));
        assertEquals(2, v1.complexity());
        VariableExpression ve1 = r.newVariableExpression(v1);
        assertEquals(2, ve1.complexity());

        IntConstant one = r.intOne();
        assertEquals(1, one.complexity());

        DependentVariable a1 = r.newDependentVariable(ve1, one);
        assertEquals(3, a1.complexity());

        Expression sum = r.sum(r.newVariableExpression(a1), one);
        assertEquals("1+a[1]", sum.toString());
        assertEquals(5, sum.complexity());
    }
}
