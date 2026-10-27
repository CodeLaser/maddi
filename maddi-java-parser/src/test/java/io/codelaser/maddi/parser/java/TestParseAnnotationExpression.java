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

package io.codelaser.maddi.parser.java;

import io.codelaser.maddi.cst.api.element.DetailedSources;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.expression.AnnotationExpression;
import io.codelaser.maddi.cst.api.expression.IntConstant;
import io.codelaser.maddi.cst.api.expression.StringConstant;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;

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
              @SuppressWarnings(value = "on Method", x = 3, y = 5)
              void method2() {
              }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT, true);
        AnnotationExpression aeType = typeInfo.annotations().getFirst();
        assertSame(runtime.getFullyQualified("java.lang.SuppressWarnings", true), aeType.typeInfo());
        AnnotationExpression.KV kv0Type = aeType.keyValuePairs().getFirst();
        assertEquals("4", kv0Type.value().toString());
        assertInstanceOf(IntConstant.class, kv0Type.value());

        FieldInfo fieldInfo = typeInfo.getFieldByName("K", true);
        assertEquals(1, fieldInfo.annotations().size());
        assertTrue(fieldInfo.annotations().getFirst().keyValuePairs().isEmpty());

        MethodInfo method = typeInfo.findUniqueMethod("method", 0);
        AnnotationExpression aeMethod = method.annotations().getFirst();
        AnnotationExpression.KV kv0 = aeMethod.keyValuePairs().getFirst();
        assertEquals("stringConstant@8-21:8-31", kv0.value().toString());
        assertInstanceOf(StringConstant.class, kv0.value());

        MethodInfo method2 = typeInfo.findUniqueMethod("method2", 0);
        AnnotationExpression aeMethod2 = method2.annotations().getFirst();
        assertEquals("11-40:11-40, 11-47:11-47",
                aeMethod2.source().detailedSources().details(DetailedSources.ARGUMENT_COMMAS)
                .stream().map(Source::compact2).collect(Collectors.joining(", ")));
        assertEquals("11-54:11-54",
                aeMethod2.source().detailedSources().detail(DetailedSources.END_OF_ARGUMENT_LIST).compact2());
    }
}
