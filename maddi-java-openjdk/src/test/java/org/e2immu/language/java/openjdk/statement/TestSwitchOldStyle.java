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

package org.e2immu.language.java.openjdk.statement;

import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.SwitchStatementOldStyle;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class TestSwitchOldStyle extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            class C {
              static void main(String[] args) {
                switch(args.length) {
                  case 0:
                    System.out.println("zero!");
                  case 1, 2:
                  case 3:
                    System.out.println("less than 3");
                    return;
                  default:
                    System.out.println("all the rest");
                }
              }
            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = scan("a.b.C", INPUT1);
        MethodInfo main = typeInfo.findUniqueMethod("main", 1);
        if (main.methodBody().statements().getFirst() instanceof SwitchStatementOldStyle sso) {
            assertEquals("switch(args.length){case 0:System.out.println(\"zero!\");case 1:case 2:case 3:System.out.println(\"less than 3\");return;default:System.out.println(\"all the rest\");}",
                    sso.print(runtime.qualificationQualifyFromPrimaryType()).toString());
        } else fail();
    }


    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            public class C {
            
                public static String method(int dataType) {
                    String s;
                    a:
                    switch (dataType) {
            
                        case 3: {
                            s = "x";
                            break;
                        }
            
                        case 4:
                            s = "z";
                            b:
                            break a;
            
                        default:
                            s = "y";
            
                    }
                    return s;
                }
            
            }
            """;

    @Test
    public void test2() {
        TypeInfo typeInfo = scan("a.b.C", INPUT2);
        MethodInfo main = typeInfo.findUniqueMethod("method", 1);
        if (main.methodBody().statements().get(1) instanceof SwitchStatementOldStyle sso) {
            assertEquals("""
                            a: switch(dataType){case 3:{s="x";break;}case 4:s="z";b: break a;default:s="y";}\
                            """,
                    sso.print(runtime.qualificationQualifyFromPrimaryType()).toString());
        } else fail();
    }

    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            class C {
              static void main(String[] args) {
                switch(args.length) {
                  case 0:
                  case 1, 2:
                  case 3:
                  default:
                }
              }
            }
            """;

    // NOTE: differs from maddi's own parser, which returns an empty new-style switch
    @Test
    public void test3() {
        TypeInfo typeInfo = scan("a.b.C", INPUT3);
        MethodInfo main = typeInfo.findUniqueMethod("main", 1);
        if (main.methodBody().statements().getFirst() instanceof SwitchStatementOldStyle ssns) {
            assertEquals("switch(args.length){}",
                    ssns.print(runtime.qualificationQualifyFromPrimaryType()).toString());
        } else fail();
    }


    @Language("java")
    private static final String INPUT4 = """
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
    public void test4() {
        TypeInfo typeInfo = scan("a.b.X", INPUT4);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("method", 1);
        SwitchStatementOldStyle statment = (SwitchStatementOldStyle) methodInfo.methodBody().statements().getFirst();
        assertEquals("String s", statment.switchLabels().getFirst().patternVariable().toString());
        assertEquals("List<?> list", statment.switchLabels().get(1).patternVariable().toString());
        SwitchStatementOldStyle.SwitchLabel label2 = statment.switchLabels().get(2);
        assertEquals("int i", label2.patternVariable().toString());
        assertEquals("i>10", label2.whenExpression().toString());
    }


    @Language("java")
    private static final String INPUT5 = """
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
    public void test5() {
        TypeInfo X = scan("a.b.X", INPUT5);
    }


    @Language("java")
    private static final String INPUT6 = """
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
    public void test6() {
        TypeInfo X = scan("a.b.X", INPUT6);
        MethodInfo method = X.findUniqueMethod("method", 1);
        SwitchStatementOldStyle ssos = (SwitchStatementOldStyle) method.methodBody().statements().getFirst();
        SwitchStatementOldStyle.SwitchLabel sl = ssos.switchLabels().getFirst();
        // ensure E.A, and not X.A, while, at the same time, go(A) uses X.A
        assertEquals("E.A", sl.literal().toString());
        MethodCall mc = (MethodCall) ssos.block().statements().getFirst().expression();
        assertEquals("go(X.A)", mc.toString());
    }


    @Language("java")
    private static final String INPUT7 = """
            package a;
            public class A {
                public void m(Object o) {
                    switch(o) {
                        case String b:
                            System.out.println("It's a string");
                        default:
                    }
                }
            }
            """;

    @Language("java")
    private static final String INPUT8 = """
            package a.b;
            class C {
              int state;
              void doPaint() {
                switch(state) {
                }
              }
            }
            """;

    // completely empty switch body (no cases at all), as generated by Nimbus *Painter.doPaint()
    @DisplayName("Empty switch body")
    @Test
    public void test8() {
        TypeInfo typeInfo = scan("a.b.C", INPUT8);
        MethodInfo doPaint = typeInfo.findUniqueMethod("doPaint", 0);
        if (doPaint.methodBody().statements().getFirst() instanceof SwitchStatementOldStyle sso) {
            assertEquals("switch(state){}",
                    sso.print(runtime.qualificationQualifyFromPrimaryType()).toString());
        } else fail();
    }

    @DisplayName("Types referenced")
    @Test
    public void test7() {
        TypeInfo X = scan("a.A", INPUT7);
        MethodInfo method = X.findUniqueMethod("m", 1);

        assertEquals("""
                java.io.PrintStream[I], java.lang.Object[E], java.lang.Object[I], java.lang.String[E], \
                java.lang.System[E], java.lang.System[I], void[E]\
                """, method.typesReferenced(null)
                .map(Object::toString).sorted().collect(Collectors.joining(", ")));
    }

}
