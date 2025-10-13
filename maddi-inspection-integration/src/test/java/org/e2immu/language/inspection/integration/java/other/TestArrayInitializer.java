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

package org.e2immu.language.inspection.integration.java.other;

import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.expression.ArrayInitializer;
import org.e2immu.language.cst.api.expression.ClassExpression;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TestArrayInitializer extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            class X {
                public String[] names = { java.util.List.class.getName() };
            }
            """;

    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1, JavaInspectorImpl.DETAILED_SOURCES);
        FieldInfo names = X.getFieldByName("names", true);
        ArrayInitializer ai = (ArrayInitializer) names.initializer();
        MethodCall mc = (MethodCall) ai.expressions().getFirst();
        ClassExpression classExpression = (ClassExpression) mc.object();
        assertEquals("Type Class<java.util.List>", classExpression.parameterizedType().toString());
        ParameterizedType pt = classExpression.type();
        assertEquals("Type java.util.List", pt.toString());
        assertEquals("3-31:3-44", classExpression.source().detailedSources().detail(pt).compact2());
        //noinspection ALL
        List<DetailedSources.Builder.TypeInfoSource> tis = (List<DetailedSources.Builder.TypeInfoSource>)
                classExpression.source().detailedSources().associatedObject(pt.typeInfo());
        // there are no qualification objects, so the associated object is not present
        assertNull(tis);
        // but we do have a package string
        assertEquals("3-31:3-39", classExpression.source().detailedSources()
                .detail(pt.typeInfo().packageName()).compact2());
    }

}
