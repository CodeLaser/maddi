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

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.impl.element.SourceImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestParseAnnotationDeclaration extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            public @interface Annot {
              String value() default "";
              int n();
              String k() default "k";
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT);
        assertEquals("Annot", typeInfo.simpleName());
        assertEquals("a.b.Annot", typeInfo.fullyQualifiedName());
        assertTrue(typeInfo.typeNature().isAnnotation());

        assertEquals(3, typeInfo.methods().size());
        MethodInfo methodInfo = typeInfo.methods().getFirst();
        assertEquals("value", methodInfo.name());
        assertEquals("a.b.Annot.value()", methodInfo.fullyQualifiedName());
        if (methodInfo.source() instanceof SourceImpl source) {
            assertEquals(3, source.beginLine());
            assertEquals(3, source.endLine());
        }
    }
}
