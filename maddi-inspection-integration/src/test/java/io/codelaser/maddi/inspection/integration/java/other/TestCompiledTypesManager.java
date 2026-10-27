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

import io.codelaser.maddi.inspection.integration.JavaInspectorImpl;
import io.codelaser.maddi.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestCompiledTypesManager extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package veryunlikely.b;
            import java.io.OutputStream;
            class C {
                private String s;
                C(Object o) {
                    if(o instanceof String str) {
                        this.s = str;
                    }
                }
            }
            """;

    @Test
    public void test1() {
        javaInspector.parse(INPUT1, JavaInspectorImpl.DETAILED_SOURCES);
        assertTrue(javaInspector.compiledTypesManager().isPackagePart("veryunlikely"));
        assertFalse(javaInspector.compiledTypesManager().isPackagePart("String"));
        assertTrue(javaInspector.compiledTypesManager().isPackagePart("java"));
        assertTrue(javaInspector.compiledTypesManager().isPackagePart("io"));
        assertFalse(javaInspector.compiledTypesManager().isPackagePart("*"));
    }

}
