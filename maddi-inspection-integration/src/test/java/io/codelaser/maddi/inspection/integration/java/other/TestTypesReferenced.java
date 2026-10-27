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

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestTypesReferenced extends CommonTest {

    @Language("java")
    public static final String INPUT1 = """
            package a.b;
            import java.lang.annotation.Annotation;
            class X {
                interface Y<A extends Annotation> {
                   A supply();
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1);
        assertEquals("""
                [java.lang.annotation.Annotation[I], a.b.X.Y[I], java.lang.annotation.Annotation[E], a.b.X.Y[I]]\
                """, typeInfo.typesReferenced(null).toList().toString());
        // FIXME PRINT space in front of {A
        String expect = """
                package a.b;
                import java.lang.annotation.Annotation;
                class X { interface Y<A extends Annotation> { A supply(); } }
                """;
        assertEquals(expect, javaInspector.print2(typeInfo.compilationUnit()));
    }
}
