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

package io.codelaser.maddi.inspection.integration.java.lombok;

import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

public class TestConstructor extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package io.codelaser.maddi.test;
            
            import lombok.NoArgsConstructor;
            
            @NoArgsConstructor
            public class X {
                private String s;
                public X(String s) {
                    this.s = s;
                }
                void method(int i) {
                    // do nothing
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1,
                new JavaInspector.ParseOptions.Builder().setLombok(true).build());
        MethodInfo nac = typeInfo.findConstructor(0);
        assertTrue(nac.isSynthetic());
    }

    @Test
    public void test1neg() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1,
                new JavaInspector.ParseOptions.Builder().setLombok(false).build());
        assertThrows(NoSuchElementException.class, () -> typeInfo.findConstructor(0));
    }

    @Language("java")
    private static final String INPUT2 = """
            package io.codelaser.maddi.test;
            
            import lombok.RequiredArgsConstructor;
            import lombok.NonNull;
            
            @RequiredArgsConstructor
            public class X {
                private final String s; // yes
                private final int k; // yes
                private final String t = "T"; // no
                private int l; // no
                private int m = 3; // no
                private Class<?> variableClazz; // no
                @NonNull private Class<?> clazz; // yes
            }
            """;

    @Test
    public void test2() {
        TypeInfo typeInfo = javaInspector.parse(INPUT2,
                new JavaInspector.ParseOptions.Builder().setLombok(true).build());
        FieldInfo variableClazz = typeInfo.getFieldByName("variableClazz", true);
        assertFalse(variableClazz.isPropertyNotNull());
        FieldInfo clazz = typeInfo.getFieldByName("clazz", true);
        assertTrue(clazz.isPropertyNotNull());

        MethodInfo rac = typeInfo.findConstructor(3);
        assertTrue(rac.isSynthetic());
        assertEquals("io.codelaser.maddi.test.X.<init>(String,int,Class)", rac.fullyQualifiedName());
        ParameterInfo p0 = rac.parameters().get(0);
        assertEquals("s", p0.name());
        ParameterInfo p1 = rac.parameters().get(1);
        assertEquals("k", p1.name());
        ParameterInfo p2 = rac.parameters().get(2);
        assertEquals("Type Class<?>", p2.parameterizedType().toString());

        assertEquals("{this.s=s;this.k=k;this.clazz=clazz;}", rac.methodBody().toString());
        MethodInfo nac = typeInfo.findConstructor(0);
        assertEquals("io.codelaser.maddi.test.X.<init>()", nac.fullyQualifiedName());
    }

    @Test
    public void test2neg() {
        TypeInfo typeInfo = javaInspector.parse(INPUT2,
                new JavaInspector.ParseOptions.Builder().setLombok(false).build());
        assertThrows(NoSuchElementException.class, () -> typeInfo.findConstructor(2));
    }


    @Language("java")
    private static final String INPUT3 = """
            package io.codelaser.maddi.test;
            
            import lombok.AllArgsConstructor;
            import lombok.NonNull;
            
            @AllArgsConstructor
            public class X {
                private final String s; // yes
                private final int k; // yes
                private final String t = "T"; // no
                private int l; // yes
                private int m = 3; // yes
                private Class<?> variableClazz; // yes
                @NonNull private Class<?> clazz; // yes
            }
            """;

    @Test
    public void test3() {
        TypeInfo typeInfo = javaInspector.parse(INPUT3,
                new JavaInspector.ParseOptions.Builder().setLombok(true).build());

        MethodInfo aac = typeInfo.findConstructor(6);
        assertTrue(aac.isSynthetic());
        assertEquals("io.codelaser.maddi.test.X.<init>(String,int,int,int,Class,Class)",
                aac.fullyQualifiedName());
        assertEquals("{this.s=s;this.k=k;this.l=l;this.m=m;this.variableClazz=variableClazz;this.clazz=clazz;}",
                aac.methodBody().toString());
        MethodInfo nac = typeInfo.findConstructor(0);
        assertEquals("io.codelaser.maddi.test.X.<init>()", nac.fullyQualifiedName());
    }
}
