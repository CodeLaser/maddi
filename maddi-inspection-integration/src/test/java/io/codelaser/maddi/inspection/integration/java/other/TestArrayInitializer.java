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

package io.codelaser.maddi.inspection.integration.java.other;

import io.codelaser.maddi.cst.api.element.DetailedSources;
import io.codelaser.maddi.cst.api.expression.ArrayInitializer;
import io.codelaser.maddi.cst.api.expression.ClassExpression;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.inspection.integration.JavaInspectorImpl;
import io.codelaser.maddi.inspection.integration.java.CommonTest;
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
