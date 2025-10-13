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

package org.e2immu.language.inspection.integration.java.lombok;

import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

public class TestConstructor extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package org.e2immu.test;
            
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
                new JavaInspectorImpl.ParseOptionsBuilder().setLombok(true).build());
        MethodInfo nac = typeInfo.findConstructor(0);
        assertTrue(nac.isSynthetic());
    }

    @Test
    public void test1neg() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1,
                new JavaInspectorImpl.ParseOptionsBuilder().setLombok(false).build());
        assertThrows(NoSuchElementException.class, () -> typeInfo.findConstructor(0));
    }

    @Language("java")
    private static final String INPUT2 = """
            package org.e2immu.test;
            
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
                new JavaInspectorImpl.ParseOptionsBuilder().setLombok(true).build());
        FieldInfo variableClazz = typeInfo.getFieldByName("variableClazz", true);
        assertFalse(variableClazz.isPropertyNotNull());
        FieldInfo clazz = typeInfo.getFieldByName("clazz", true);
        assertTrue(clazz.isPropertyNotNull());

        MethodInfo rac = typeInfo.findConstructor(3);
        assertTrue(rac.isSynthetic());
        assertEquals("org.e2immu.test.X.<init>(String,int,Class<?>)", rac.fullyQualifiedName());
        ParameterInfo p0 = rac.parameters().get(0);
        assertEquals("s", p0.name());
        ParameterInfo p1 = rac.parameters().get(1);
        assertEquals("k", p1.name());
        ParameterInfo p2 = rac.parameters().get(2);
        assertEquals("Type Class<?>", p2.parameterizedType().toString());

        assertEquals("{this.s=s;this.k=k;this.clazz=clazz;}", rac.methodBody().toString());
        MethodInfo nac = typeInfo.findConstructor(0);
        assertEquals("org.e2immu.test.X.<init>()", nac.fullyQualifiedName());
    }

    @Test
    public void test2neg() {
        TypeInfo typeInfo = javaInspector.parse(INPUT2,
                new JavaInspectorImpl.ParseOptionsBuilder().setLombok(false).build());
        assertThrows(NoSuchElementException.class, () -> typeInfo.findConstructor(2));
    }


    @Language("java")
    private static final String INPUT3 = """
            package org.e2immu.test;
            
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
                new JavaInspectorImpl.ParseOptionsBuilder().setLombok(true).build());

        MethodInfo aac = typeInfo.findConstructor(6);
        assertTrue(aac.isSynthetic());
        assertEquals("org.e2immu.test.X.<init>(String,int,int,int,Class<?>,Class<?>)",
                aac.fullyQualifiedName());
        assertEquals("{this.s=s;this.k=k;this.l=l;this.m=m;this.variableClazz=variableClazz;this.clazz=clazz;}",
                aac.methodBody().toString());
        MethodInfo nac = typeInfo.findConstructor(0);
        assertEquals("org.e2immu.test.X.<init>()", nac.fullyQualifiedName());
    }
}
