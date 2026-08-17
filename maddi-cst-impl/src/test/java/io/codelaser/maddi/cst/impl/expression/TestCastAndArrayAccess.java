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

import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.Cast;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestCastAndArrayAccess extends CommonTest {

    @Test
    public void test1() {
        ParameterizedType objectArray = r.objectParameterizedType().copyWithArrays(1);
        LocalVariable v = r.newLocalVariable("v", objectArray);
        DependentVariable v0 = r.newDependentVariable(r.newVariableExpression(v), r.intZero(), r.objectParameterizedType());
        assertEquals("v[0]", v0.toString());
        VariableExpression veV0 = r.newVariableExpression(v0);
        Source src = r.newParserSource("-", 0, 0, 1, 1);
        Cast asObjectArray = r.newCastBuilder().setExpression(veV0).setParameterizedType(objectArray).setSource(src).build();
        assertEquals("(Object[])v[0]", asObjectArray.toString());

        DependentVariable d1 = r.newDependentVariable(asObjectArray, r.intOne());
        VariableExpression veD1 = r.newVariableExpression(d1);
        assertEquals("((Object[])v[0])[1]", veD1.toString());

        ParameterizedType byteArray = r.byteParameterizedType().copyWithArrays(1);
        Cast asByteArray = r.newCastBuilder().setExpression(veD1).setParameterizedType(byteArray).setSource(src).build();
        assertEquals("(byte[])((Object[])v[0])[1]", asByteArray.toString());
    }

    void testCode(Object[] v) {
        Object[] os = (Object[]) v[0];
        Object o = ((Object[]) v[0])[1];
        byte[] bytes = (byte[]) ((Object[]) v[0])[1];
    }
}
