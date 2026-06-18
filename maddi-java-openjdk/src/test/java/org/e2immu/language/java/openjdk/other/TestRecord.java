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

package org.e2immu.language.java.openjdk.other;

import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ExplicitConstructorInvocation;
import org.e2immu.language.cst.api.statement.IfElseStatement;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.java.openjdk.CommonTest;
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
}
