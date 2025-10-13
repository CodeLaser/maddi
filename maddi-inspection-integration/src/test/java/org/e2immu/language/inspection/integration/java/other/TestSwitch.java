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

package org.e2immu.language.inspection.integration.java.other;

import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.expression.SwitchExpression;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.SwitchEntry;
import org.e2immu.language.cst.api.statement.SwitchStatementNewStyle;
import org.e2immu.language.cst.api.statement.SwitchStatementOldStyle;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestSwitch extends CommonTest {


    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.List;
            class X {
                int method(Object o) {
                    switch(o) {
                        case String s:  return s.length();
                        case List<?> list: return list.size();
                        case int i when i > 10: return i;
                        default: throw new UnsupportedOperationException();
                    }
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo typeInfo = javaInspector.parse(INPUT2, JavaInspectorImpl.DETAILED_SOURCES);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("method", 1);
    }


    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            import java.util.List;
            class X {
                record R(int i, int j) { }
                record S(String s, R r) { }
                int method(Object o) {
                    switch(o) {
                        case R(int i, int j): return i + j;
                        case S(String s, R(int i, int j)): return s.length() - i; 
                        default: throw new UnsupportedOperationException();
                    }
                }
            }
            """;

    @Test
    public void test3() {
        TypeInfo typeInfo = javaInspector.parse(INPUT3, JavaInspectorImpl.DETAILED_SOURCES);
    }

    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            import java.util.List;
            class X {
                int method(Object o) {
                    switch(o) {
                        case String s -> { return s.length(); }
                        case List<?> list -> { return list.size(); }
                        case int i when i > 10 -> { return i; }
                        default -> throw new UnsupportedOperationException();
                    }
                }
            }
            """;

    @Test
    public void test4() {
        TypeInfo typeInfo = javaInspector.parse(INPUT4, JavaInspectorImpl.DETAILED_SOURCES);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("method", 1);
        SwitchStatementNewStyle ns = (SwitchStatementNewStyle) methodInfo.methodBody().lastStatement();
        SwitchEntry s0 = ns.entries().getFirst();
        assertEquals("String s", s0.patternVariable().toString());

        SwitchEntry s1 = ns.entries().get(1);
        assertEquals("List<?> list", s1.patternVariable().toString());
        assertEquals("<empty>", s1.whenExpression().toString());
        assertEquals("7-13:7-56", s1.source().compact2());
        assertEquals("7-18:7-29", s1.patternVariable().source().compact2());

        SwitchEntry s2 = ns.entries().get(2);
        assertEquals("int i", s2.patternVariable().toString());
        assertEquals("i>10", s2.whenExpression().toString());
        assertEquals("8-13:8-51", s2.source().compact2());
        assertEquals("8-18:8-22", s2.patternVariable().source().compact2());
        assertEquals("8-29:8-34", s2.whenExpression().source().compact2());
    }

    @Language("java")
    private static final String INPUT5 = """
            package a.b;
            import java.util.List;
            class X {
                int method(Object o) {
                    return switch(o) {
                        case String s -> s.length();
                        case List<?> list -> list.size();
                        case int i when i > 10 -> i;
                        case null, default -> 0;
                    };
                }
            }
            """;

    @Test
    public void test5() {
        javaInspector.parse(INPUT5);
    }


    @Language("java")
    private static final String INPUT6 = """
            package a.b;
            import java.util.List;
            class X {
                record R(int i, int j) { }
                record S(String s, R r) { }
                int method(Object o) {
                    switch(o) {
                        case R(int i, int j) -> { return i + j; }
                        case S(String s, R(int i, int j)) -> { return s.length() - i; }
                        default -> throw new UnsupportedOperationException();
                    }
                }
            }
            """;

    @Test
    public void test6() {
        TypeInfo typeInfo = javaInspector.parse(INPUT6, JavaInspectorImpl.DETAILED_SOURCES);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("method", 1);
        SwitchStatementNewStyle ns = (SwitchStatementNewStyle) methodInfo.methodBody().lastStatement();
        SwitchEntry s0 = ns.entries().getFirst();
        assertEquals("R(int i,int j)", s0.patternVariable().toString());
        SwitchEntry s1 = ns.entries().get(1);
        assertEquals("S(String s,R(int i,int j))", s1.patternVariable().toString());
    }


    @Language("java")
    private static final String INPUT7 = """
            package a.b;
            import java.util.List;
            class X {
                record R(int i, int j) { }
                record S(String s, R r) { }
                int method(Object o) {
                    return switch(o) {
                        case R(int i, int j) -> i + j;
                        case S(String s, R(int i, int j)) -> s.length() - i;
                        default -> throw new UnsupportedOperationException();
                    };
                }
            }
            """;

    @Test
    public void test7() {
        javaInspector.parse(INPUT7);
    }


    @Language("java")
    private static final String INPUT8 = """
            package a.b;
            
            class X {
                static final int A = 3;
                static final int B = 4;
                enum E { A, B, C }
                void method(E e) {
                    switch(e) {
                        case A -> go(A);
                        case B -> go(B);
                        default -> go(0);
                    }
                }
                void go(int i) {
                    System.out.println("Have "+i);
                }
            }
            """;

    @DisplayName("Field name vs switch constant")
    @Test
    public void test8() {
        TypeInfo X = javaInspector.parse(INPUT8);
        MethodInfo method = X.findUniqueMethod("method", 1);
        SwitchStatementNewStyle ssns = (SwitchStatementNewStyle) method.methodBody().statements().getFirst();
        SwitchEntry se = ssns.entries().getFirst();
        // ensure E.A, and not X.A, while, at the same time, go(A) uses X.A
        assertEquals("E.A", se.conditions().getFirst().toString());
        MethodCall mc = (MethodCall) se.statement().expression();
        assertEquals("this.go(X.A)", mc.toString());
    }

    @Language("java")
    private static final String INPUT8B = """
            package a.b;
            
            class X {
                static final int A = 3;
                static final int B = 4;
                enum E { A, B, C }
                void method(E e) {
                    switch(e) {
                        case A: go(A); break;
                        case B: go(B); break;
                        default: go(0);
                    }
                }
                void go(int i) {
                    System.out.println("Have "+i);
                }
            }
            """;

    @DisplayName("Field name vs switch constant, old style")
    @Test
    public void test8B() {
        TypeInfo X = javaInspector.parse(INPUT8B);
        MethodInfo method = X.findUniqueMethod("method", 1);
        SwitchStatementOldStyle ssos = (SwitchStatementOldStyle) method.methodBody().statements().getFirst();
        SwitchStatementOldStyle.SwitchLabel sl = ssos.switchLabels().getFirst();
        // ensure E.A, and not X.A, while, at the same time, go(A) uses X.A
        assertEquals("E.A", sl.literal().toString());
        MethodCall mc = (MethodCall) ssos.block().statements().getFirst().expression();
        assertEquals("this.go(X.A)", mc.toString());
    }


    @Language("java")
    private static final String INPUT8C = """
            package a.b;
            
            class X {
                static final int A = 3;
                static final int B = 4;
                enum E { A, B, C }
                int method(E e) {
                    return switch(e) {
                        case A -> go(A);
                        case B -> go(B);
                        default -> go(0);
                    };
                }
                int go(int i) {
                    System.out.println("Have "+i);
                    return i+1;
                }
            }
            """;

    @DisplayName("Field name vs switch constant, expression")
    @Test
    public void test8C() {
        TypeInfo X = javaInspector.parse(INPUT8C);
        MethodInfo method = X.findUniqueMethod("method", 1);
        SwitchExpression sw = (SwitchExpression) method.methodBody().statements().getFirst().expression();
        SwitchEntry se = sw.entries().getFirst();
        // ensure E.A, and not X.A, while, at the same time, go(A) uses X.A
        assertEquals("E.A", se.conditions().getFirst().toString());
        MethodCall mc = (MethodCall) se.statement().expression();
        assertEquals("this.go(X.A)", mc.toString());
    }
}