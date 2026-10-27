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

package io.codelaser.maddi.java.openjdk.other;

import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.ExplicitConstructorInvocation;
import io.codelaser.maddi.cst.api.statement.IfElseStatement;
import io.codelaser.maddi.cst.api.statement.LocalTypeDeclaration;
import io.codelaser.maddi.cst.api.statement.Statement;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestRecord extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            public record R(int a, String b) {
                record S(int a, String b) { }
            }
            """;

    @DisplayName("record")
    @Test
    public void test1() {
        TypeInfo R = scan("a.b.R", INPUT1);
        MethodInfo syntheticConstructor = R.findConstructor(2);
        assertTrue(syntheticConstructor.isSyntheticConstructor());
        assertTrue(syntheticConstructor.isSynthetic());
        assertEquals(3, syntheticConstructor.methodBody().statements().size());
        Statement s1 = syntheticConstructor.methodBody().statements().get(2);
        assertEquals("this.b=b;", s1.toString());
        assertEquals("1", s1.source().index());
        FieldInfo a = R.getFieldByName("a", true);
        assertTrue(a.hasBeenInspected());
        assertTrue(a.access().isPrivate());
        assertEquals("<empty>", a.initializer().toString());

        TypeInfo S = R.findSubType("S");
        FieldInfo b = S.getFieldByName("b", true);
        assertTrue(b.hasBeenInspected());
        assertTrue(b.access().isPrivate());

        assertEquals("Type Record", R.parentClass().toString());
        assertEquals("Type Record", S.parentClass().toString());
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            public record R(int a, String b) {
                public R {
                    if(a < 0) System.out.println("a is too small");
                }
            }
            """;

    @DisplayName("record with compact constructor")
    @Test
    public void test2() {
        TypeInfo R = scan("a.b.R", INPUT2);
        assertEquals(1, R.constructors().size());
        MethodInfo cc = R.findConstructor(2);
        assertTrue(cc.methodType().isCompactConstructor());
        assertEquals(4, cc.methodBody().statements().size());
        assertInstanceOf(ExplicitConstructorInvocation.class, cc.methodBody().statements().getFirst());
        assertInstanceOf(IfElseStatement.class, cc.methodBody().statements().get(1));
        Statement s2 = cc.methodBody().statements().get(2);
        assertEquals("this.a=a;", s2.toString());
        assertEquals("1", s2.source().index());
    }


    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            public class C {
                S s;
                String method() {
                    return s.b; // ensure that b is accessed before it has been declared
                }
                record S(int a, String b) { }
            }
            """;

    @DisplayName("record")
    @Test
    public void test3() {
        TypeInfo C = scan("a.b.C", INPUT3);

        TypeInfo S = C.findSubType("S");
        FieldInfo b = S.getFieldByName("b", true);
        assertTrue(b.hasBeenInspected());
        assertTrue(b.access().isPrivate());
        assertEquals(2, S.fields().size());
    }

    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            public record C(String output) {
               @Override
               public String output() {
                   System.out.println("print!");
                   return output;
               }
            }
            """;

    @DisplayName("override of record accessor")
    @Test
    public void test4() {
        TypeInfo C = scan("a.b.C", INPUT4);
        MethodInfo output = C.findUniqueMethod("output", 0);
        assertEquals("java.lang.Override", output.annotations().getFirst().typeInfo().fullyQualifiedName());
        assertTrue(output.hasBeenInspected());
        // NOTE: contrary to the Java spec, we're not overriding
        assertTrue(output.overrides().isEmpty());
        assertFalse(output.isSynthetic());

        MethodInfo toString = C.findUniqueMethod("toString", 0);
        assertTrue(toString.isSynthetic());
        assertEquals("[java.lang.Object.toString()]", toString.overrides().toString());

        MethodInfo equals = C.findUniqueMethod("equals", 1);
        assertTrue(equals.isSynthetic());
        assertEquals("[java.lang.Object.equals(Object)]", equals.overrides().toString());

        MethodInfo hashCode = C.findUniqueMethod("hashCode", 0);
        assertTrue(hashCode.isSynthetic());
        assertEquals("[java.lang.Object.hashCode()]", hashCode.overrides().toString());
    }


    @Language("java")
    private static final String INPUT5 = """
            package a.b;
            public record R(int a, String b) {
               void method() {
                   record X(int c, int d) {
                       record Y(int e) { }
                   }
                   X x = new X(a, b.length());
                   System.out.println("x");
               }
            }
            """;

    @DisplayName("inline record")
    @Test
    public void test5() {
        TypeInfo R = scan("a.b.R", INPUT5);
        MethodInfo method = R.findUniqueMethod("method", 0);
        LocalTypeDeclaration ltd = (LocalTypeDeclaration) method.methodBody().statements().getFirst();
        assertTrue(ltd.typeInfo().typeNature().isRecord());
        FieldInfo c = ltd.typeInfo().getFieldByName("c", true);
        assertEquals("<empty>", c.initializer().toString());
        TypeInfo Y = ltd.typeInfo().findSubType("Y");
        FieldInfo e = Y.getFieldByName("e", true);
        assertEquals("<empty>", e.initializer().toString());
    }
}
