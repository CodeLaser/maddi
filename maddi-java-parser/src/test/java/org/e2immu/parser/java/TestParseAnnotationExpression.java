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

package org.e2immu.parser.java;

import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.expression.IntConstant;
import org.e2immu.language.cst.api.expression.StringConstant;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestParseAnnotationExpression extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            @SuppressWarnings(4)
            class C {

              @SuppressWarnings
              private static int K = 3;

              @SuppressWarnings("on Method")
              void method() {

              }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT);
        AnnotationExpression aeType = typeInfo.annotations().get(0);
        assertSame(runtime.getFullyQualified("java.lang.SuppressWarnings", true), aeType.typeInfo());
        AnnotationExpression.KV kv0Type = aeType.keyValuePairs().get(0);
        assertEquals("4", kv0Type.value().toString());
        assertInstanceOf(IntConstant.class, kv0Type.value());

        FieldInfo fieldInfo = typeInfo.getFieldByName("K", true);
        assertEquals(1, fieldInfo.annotations().size());
        assertTrue(fieldInfo.annotations().get(0).keyValuePairs().isEmpty());

        MethodInfo method = typeInfo.findUniqueMethod("method", 0);
        AnnotationExpression aeMethod = method.annotations().get(0);
        AnnotationExpression.KV kv0 = aeMethod.keyValuePairs().get(0);
        assertEquals("stringConstant@8-21:8-31", kv0.value().toString());
        assertInstanceOf(StringConstant.class, kv0.value());
    }
}
