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

import org.e2immu.language.cst.api.expression.BinaryOperator;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestEnum extends CommonTest {


    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            
            public class X {
                @interface FieldAnnotation { }
                enum State { START,
                 @FieldAnnotation
                 BUSY,
                 END }
            
                int method(State state) {
                   return switch(state) {
                       case START -> 3;
                       case END -> 4;
                       default -> 0;
                   };
                }
            
                int method2(State state) {
                   switch(state) {
                       case START:
                           System.out.println("start");
                           break;
                       case END:
                           System.out.println("end");
                           break;
                   }
                   return -1;
                }
            
                int method3(State state) {
                    if (State.BUSY.equals(state)) {
                        return 3;
                    }
                    return 2;
                }
            
                private <T extends Enum<T>> T getEnum(Class<T> enumType, String string) {
                	if (string == null || string.isEmpty()) {
                		return null;
                	}
                	return T.valueOf(enumType, string.toUpperCase());
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = scan("a.b.X", INPUT1);
        assertTrue(typeInfo.hasImplicitParent());
        TypeInfo state = typeInfo.findSubType("State");
        assertTrue(state.hasImplicitParent());
        assertEquals("[BUSY, END, START]", state.fields().stream().map(FieldInfo::name).sorted().toList().toString());
        FieldInfo BUSY = state.getFieldByName("BUSY", true);
        assertEquals(1, BUSY.annotations().size());
        assertEquals("Type Enum<a.b.X.State>", state.parentClass().toString());
        TypeInfo enumType = state.parentClass().typeInfo();
        assertEquals("Type Enum<E extends Enum<E>>", enumType.asParameterizedType().toString());

        FieldInfo start = state.getFieldByName("START", true);
        assertEquals("Type a.b.X.State", start.type().toString());
    }


    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            enum E {
              A, B, C;
              public boolean isA() {
                return this == A;
              }
              public boolean isB() {
                 return "B".equals(name());
              }
              public static E[] all() {
                 return values();
              }
              public static E make(String s) {
                 return valueOf(s);
              }
            }
            """;

    @Test
    public void test2() {
        TypeInfo typeInfo = scan("a.b.E", INPUT2);
        assertTrue(typeInfo.typeNature().isEnum());

        assertEquals("Type Enum<a.b.E>", typeInfo.parentClass().toString());

        FieldInfo a = typeInfo.getFieldByName("A", true);
        assertTrue(a.isStatic());

        // TODO assertTrue(a.access().isPublic());
        assertTrue(a.isFinal());
        assertTrue(a.isPropertyFinal());
        assertEquals(3, typeInfo.fields().size());

        MethodInfo isA = typeInfo.findUniqueMethod("isA", 0);
        if (isA.methodBody().statements().getFirst() instanceof ReturnStatement rs
            && rs.expression() instanceof BinaryOperator bo && bo.rhs() instanceof VariableExpression ve &&
            ve.variable() instanceof FieldReference fr) {
            assertSame(a, fr.fieldInfo());
        } else fail();

        MethodInfo values = typeInfo.findUniqueMethod("values", 0);
        assertEquals("Type a.b.E[]", values.returnType().toString());
        MethodInfo valueOf = typeInfo.findUniqueMethod("valueOf", 1);
        assertEquals("Type a.b.E", valueOf.returnType().toString());
    }

    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            enum E {
              A(true, "s"), B(false), C(false, "d");
              private final boolean b;
              private final String s;
            
              E(boolean b) { this(b, ""); }
            
              E(boolean b, String s) {
                 this.b = b;
                 this.s = s;
              }
              public String s() {
                return s;
              }
            }
            """;

    @Test
    public void test3() {
        TypeInfo typeInfo = scan("a.b.E", INPUT3);
        assertTrue(typeInfo.typeNature().isEnum());
        assertFalse(typeInfo.isExtensible());

        FieldInfo a = typeInfo.getFieldByName("A", true);
        assertTrue(a.isStatic());
        assertEquals("new E(true,\"s\")", a.initializer().toString());

        FieldInfo b = typeInfo.getFieldByName("B", true);
        assertTrue(b.isStatic());
        assertEquals("new E(false)", b.initializer().toString());
    }

    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            public class X {
                public enum Effective {
                    E1, E2;
                    public static Effective of(int index) {
                        return index == 1 ? E1: E2;
                    }
                }
                public enum Level {
                    ONE, TWO, THREE
                }
            }
            """;

    @Test
    public void test4() {
        TypeInfo typeInfo = scan("a.b.X", INPUT4);
        TypeInfo effective = typeInfo.findSubType("Effective");
        assertTrue(effective.typeNature().isEnum());
        assertEquals(2, effective.fields().size());
        assertEquals(3, effective.methods().size());
        assertEquals(2, effective.methods().stream().filter(MethodInfo::isSynthetic).count());
        TypeInfo level = typeInfo.findSubType("Level");
        assertTrue(level.typeNature().isEnum());
        assertEquals(3, level.fields().size());
    }

}
