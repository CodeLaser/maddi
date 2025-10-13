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
import org.e2immu.language.cst.api.expression.IntConstant;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.e2immu.language.cst.api.variable.Variable;
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
