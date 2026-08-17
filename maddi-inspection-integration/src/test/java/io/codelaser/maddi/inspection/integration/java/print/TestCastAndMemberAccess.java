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

package org.e2immu.language.inspection.integration.java.print;

import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.Cast;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestCastAndMemberAccess extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package org.e2immu.analyser.resolver.testexample;
            record X(int x) { }
            """;

    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        Runtime r = javaInspector.runtime();

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

        Cast asX = r.newCastBuilder().setExpression(veD1).setParameterizedType(X.asParameterizedType()).setSource(src).build();
        assertEquals("(X)((Object[])v[0])[1]", asX.toString());

        FieldInfo x = X.getFieldByName("x", true);
        FieldReference dotX = r.newFieldReference(x, asX, r.intParameterizedType());
        assertEquals("((org.e2immu.analyser.resolver.testexample.X)((Object[])v[0])[1]).x", dotX.toString());
    }

    private record X(int x) {
    }

    void testCode(Object[] v) {
        Object[] os = (Object[]) v[0];
        Object o = ((Object[]) v[0])[1];

        int x = ((X) ((Object[]) v[0])[1]).x;
    }
}
