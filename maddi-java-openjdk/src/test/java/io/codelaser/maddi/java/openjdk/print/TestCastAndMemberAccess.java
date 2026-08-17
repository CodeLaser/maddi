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

package io.codelaser.maddi.java.openjdk.print;

import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.expression.Cast;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.DependentVariable;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.api.variable.LocalVariable;
import io.codelaser.maddi.java.openjdk.CommonTest;
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
        TypeInfo X = scan("org.e2immu.analyser.resolver.testexample.X", INPUT1);

        ParameterizedType objectArray = runtime.objectParameterizedType().copyWithArrays(1);
        LocalVariable v = runtime.newLocalVariable("v", objectArray);
        DependentVariable v0 = runtime.newDependentVariable(runtime.newVariableExpression(v), runtime.intZero(), runtime.objectParameterizedType());
        assertEquals("v[0]", v0.toString());
        VariableExpression veV0 = runtime.newVariableExpression(v0);
        Source src = runtime.newParserSource("-", 0, 0, 1, 1);
        Cast asObjectArray = runtime.newCastBuilder().setExpression(veV0).setParameterizedType(objectArray).setSource(src).build();
        assertEquals("(Object[])v[0]", asObjectArray.toString());

        DependentVariable d1 = runtime.newDependentVariable(asObjectArray, runtime.intOne());
        VariableExpression veD1 = runtime.newVariableExpression(d1);
        assertEquals("((Object[])v[0])[1]", veD1.toString());

        Cast asX = runtime.newCastBuilder().setExpression(veD1).setParameterizedType(X.asParameterizedType()).setSource(src).build();
        assertEquals("(X)((Object[])v[0])[1]", asX.toString());

        FieldInfo x = X.getFieldByName("x", true);
        FieldReference dotX = runtime.newFieldReference(x, asX, runtime.intParameterizedType());
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
