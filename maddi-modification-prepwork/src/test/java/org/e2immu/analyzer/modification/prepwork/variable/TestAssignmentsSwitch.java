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

package org.e2immu.analyzer.modification.prepwork.variable;

import org.e2immu.analyzer.modification.prepwork.CommonTest;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.*;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestAssignmentsSwitch extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            class X {
                static int method(char c, boolean b) {
                    int i;
                    switch (c) {
                        case 'a':
                            if (b) i = 3;
                            else i = 1;
                            break;
                        case 'b':
                            i = 2;
                            break;
                        default:
                            i = 4;
                    }
                    return i;
                }
            }
            """;

    @Language("java")
    private static final String INPUT1b = """
            package a.b;
            class X {
                static int method(char c, boolean b) {
                    int i;
                    switch (c) {
                        case 'a':
                            if (b) {
                                i = 3;
                            } else {
                                i = 1;
                            }
                            break;
                        case 'b':
                            i = 2;
                            break;
                        default:
                            i = 4;
                            break;
                    }
                    return i;
                }
            }
            """;

    @DisplayName("clean old-style switch")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        test1Code(X);
    }

    @DisplayName("clean old-style switch, unnecessary break after default")
    @Test
    public void test1b() {
        TypeInfo X = javaInspector.parse(INPUT1b);
        test1Code(X);
    }

    private void test1Code(TypeInfo X) {
        MethodInfo method = X.findUniqueMethod("method", 2);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doMethod(method);
        VariableData vdMethod = VariableDataImpl.of(method);
        assertNotNull(vdMethod);
        assertEquals("a.b.X.method(char,boolean), a.b.X.method(char,boolean):0:c, a.b.X.method(char,boolean):1:b, i",
                vdMethod.knownVariableNamesToString());

        VariableInfo inVi = vdMethod.variableInfo(method.parameters().get(0).fullyQualifiedName());
        assertEquals("c", inVi.variable().simpleName());
        assertEquals("1-E", inVi.reads().toString()); 
        assertEquals("D:-, A:[]", inVi.assignments().toString());

        VariableInfo bVi = vdMethod.variableInfo(method.parameters().get(1).fullyQualifiedName());
        assertEquals("b", bVi.variable().simpleName());
        assertEquals("1.0.0-E", bVi.reads().toString()); 
        assertEquals("D:-, A:[]", bVi.assignments().toString());

        VariableInfo iVi = vdMethod.variableInfo("i");
        assertEquals("2", iVi.reads().toString());
        assertEquals("D:0, A:[1.0.0.0.0, 1.0.0.1.0, 1.0.0=M, 1.0.2, 1.0.4, 1=M]", iVi.assignments().toString());

        Statement ifElse = method.methodBody().statements().get(1).block().statements().get(0);
        VariableData vdIfElse = VariableDataImpl.of(ifElse);
        VariableInfo iViIfElse = vdIfElse.variableInfo("i");
        assertEquals("D:0, A:[1.0.0.0.0, 1.0.0.1.0, 1.0.0=M]", iViIfElse.assignments().toString());
    }


    @Language("java")
    private static final String INPUT1c = """
            package a.b;
            class X {
                static int method(char c, boolean b) {
                    int i;
                    switch (c) {
                        case 'a':
                            if (b) {
                                i = 3;
                                break;
                            } else {
                                i = 1;
                                break;
                            }
                        case 'b':
                            break;
                        default:
                            i = 4;
                            break;
                    }
                    return i;
                }
            }
            """;


    @DisplayName("clean old-style switch, break hidden in if-statement")
    @Test
    public void test1c() {
        TypeInfo X = javaInspector.parse(INPUT1c);
        MethodInfo method = X.findUniqueMethod("method", 2);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doMethod(method);
        VariableData vdMethod = VariableDataImpl.of(method);
        assertNotNull(vdMethod);
        assertEquals("a.b.X.method(char,boolean), a.b.X.method(char,boolean):0:c, a.b.X.method(char,boolean):1:b, i",
                vdMethod.knownVariableNamesToString());

        VariableInfo iVi = vdMethod.variableInfo("i");
        assertEquals("2", iVi.reads().toString()); 
        assertEquals("D:0, A:[1.0.0.0.0, 1.0.0.1.0, 1.0.0=M, 1.0.2]", iVi.assignments().toString());
        assertFalse(iVi.hasBeenDefined("2"));
    }


    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            class X {
                static int method(char c, boolean b) {
                    int i;
                    switch (c) {
                        case 'a':
                            if (b) i = 3;
                            else i = 1;
                            break;
                        case 'b':
                            return -1;
                        default:
                            i = 4;
                    }
                    return i;
                }
            }
            """;


    @DisplayName("clean old-style switch, break and return")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        MethodInfo method = X.findUniqueMethod("method", 2);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doMethod(method);
        VariableData vdMethod = VariableDataImpl.of(method);
        assertNotNull(vdMethod);
        assertEquals("a.b.X.method(char,boolean), a.b.X.method(char,boolean):0:c, a.b.X.method(char,boolean):1:b, i",
                vdMethod.knownVariableNamesToString());

        VariableInfo iVi = vdMethod.variableInfo("i");
        assertEquals("2", iVi.reads().toString()); 
        assertEquals("D:0, A:[1.0.0.0.0, 1.0.0.1.0, 1.0.0=M, 1.0.3, 1=M]", iVi.assignments().toString());
        assertTrue(iVi.hasBeenDefined("2"));
    }

    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            class X {
                static int method(char c, boolean b) {
                    int i;
                    switch (c) {
                        case 'a':
                            if (b) i = 3;
                            else i = 1;
                            break;
                        case 'b':
                            System.out.println("b!!");
                        default:
                            i = 4;
                    }
                    return i;
                }
            }
            """;


    @DisplayName("old-style switch with irrelevant fall-through")
    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse(INPUT3);
        MethodInfo method = X.findUniqueMethod("method", 2);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doMethod(method);
        VariableData vdMethod = VariableDataImpl.of(method);
        assertNotNull(vdMethod);

        VariableInfo iVi = vdMethod.variableInfo("i");
        assertEquals("2", iVi.reads().toString()); 
        assertEquals("D:0, A:[1.0.0.0.0, 1.0.0.1.0, 1.0.0=M, 1.0.3, 1=M]", iVi.assignments().toString());
        assertTrue(iVi.hasBeenDefined("2"));
    }

    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            class X {
                static int method(char c, boolean b) {
                    int i;
                    switch (c) {
                        case 'a':
                            if (b) i = 3;
                            else i = 1;
                        case 'b':
                            System.out.println("b!!");
                            break;
                        default:
                            i = 4;
                    }
                    return i; // i has not been defined! illegal java
                }
            }
            """;

    @DisplayName("old-style switch with real fall-through and overwrite")
    @Test
    public void test4() {
        TypeInfo X = javaInspector.parse(INPUT4);
        MethodInfo method = X.findUniqueMethod("method", 2);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doMethod(method);
        VariableData vdMethod = VariableDataImpl.of(method);
        assertNotNull(vdMethod);

        VariableInfo iVi = vdMethod.variableInfo("i");
        assertEquals("2", iVi.reads().toString()); 
        assertEquals("D:0, A:[1.0.0.0.0, 1.0.0.1.0, 1.0.0=M, 1.0.3]", iVi.assignments().toString());
        assertFalse(iVi.hasBeenDefined("2"));
    }


    @Language("java")
    public static final String INPUT5 = """
            package a.b;
            import java.io.IOException;
            import java.io.Reader;
            class X {
                 private String parse(Reader in, int state) throws IOException {
                     in.mark(1);
                     int c = in.read();
                     StringBuilder str = new StringBuilder();
                     switch(c) {
                        case -1:
                            switch(state) {
                                case 0:
                                    return null;
                                default:
                                    return str.toString();
                            }
                        case '\\n':
                            return str.toString();
                        default:
                            switch(state) {
                                case 2:
                                case 3:
                                    in.reset();
                                    return str.toString();
                                default:
                                    state = 1;
                                    str.append((char) c);
                            }
                    }
                    return "?";
                }
            }
            """;


    @DisplayName("nested old-style switches, almost return everywhere")
    @Test
    public void test5() {
        TypeInfo X = javaInspector.parse(INPUT5);
        MethodInfo method = X.findUniqueMethod("parse", 2);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doMethod(method);
        VariableData vdMethod = VariableDataImpl.of(method);
        assertNotNull(vdMethod);

        VariableInfo iVi = vdMethod.variableInfo(method.fullyQualifiedName());
        assertEquals("D:-, A:[3.0.0.0.0, 3.0.0.0.1, 3.0.0=M, 3.0.1, 3.0.2.0.1, 4]", iVi.assignments().toString());
        assertFalse(iVi.hasBeenDefined("3"));
        assertTrue(iVi.hasBeenDefined("4"));
        assertTrue(iVi.hasBeenDefined("5"));
        assertEquals("[3.0.0.0.0, 3.0.0.0.1, 3.0.0, 3.0.1, 3.0.2.0.1, 4]",
                iVi.assignments().allIndicesStripStage().toString());
    }

    @Language("java")
    public static final String INPUT6 = """
            package a.b;
            class X {
                 private String parse(String str) {
                     final char[] trans2 = "bcdef".toCharArray();
                     final char[] trans1 = "ghij".toCharArray();
                     int index1 = 0;
                     int index2 = 0;
                     str = str.replaceAll("[\\n\\r]", "");
                     char[] input = str.toCharArray();
                     for (int i = 0; i < 20; i++) {
                         switch(input[i]) {
                             case '4':
                                 input[i] = input[i + 1] = input[i + 4] = input[i + 5] = 'a';
                                 break;
                             case '1':
                                 input[i] = trans1[index1++];
                                 break;
                             case '2':
                                 input[i] = input[i + 4] = trans2[index2++];
                                 break;
                             case '3':
                                 input[i] = input[i + 1] = (char) (trans2[index2++] - 'a' + 'A');
                                 break;
                             case '0':
                                 input[i] = ' ';
                                 break;
                             default:
                         }
                     }
                     return new String(input);
                 }
            }
            """;

    @DisplayName("increments in array indices")
    @Test
    public void test6() {
        TypeInfo X = javaInspector.parse(INPUT6);
        MethodInfo method = X.findUniqueMethod("parse", 1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doMethod(method);
        VariableData vdLast = VariableDataImpl.of(method.methodBody().lastStatement());
        assertNotNull(vdLast);

        VariableInfo index1Vi = vdLast.variableInfo("index1");
        assertEquals("D:2, A:[2, 6.0.0.0.2]", index1Vi.assignments().toString());
        assertEquals("6.0.0.0.2", index1Vi.reads().toString());

        VariableInfo index2Vi = vdLast.variableInfo("index2");
        assertEquals("D:3, A:[3, 6.0.0.0.4, 6.0.0.0.6]", index2Vi.assignments().toString());
        assertEquals("6.0.0.0.4, 6.0.0.0.6", index2Vi.reads().toString());
    }


    @Language("java")
    public static final String INPUT7 = """
            package a.b;
            class X {
                 private void method(Exception e) {
                     switch (e) {
                         case IndexOutOfBoundsException i -> {
                             if (i.getCause() != null) {
                                 System.out.println(i.getMessage());
                             }
                             throw i;
                         }
                         case RuntimeException _ ->{
                             System.out.println("re");
                             return;
                         }
                         default -> throw e;
                     }
                     System.out.println("at end");
                 }
            }
            """;

    @Test
    public void test7() {
        TypeInfo X = javaInspector.parse(INPUT7);
        MethodInfo method = X.findUniqueMethod("method", 1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doMethod(method);
        SwitchStatementNewStyle ns = (SwitchStatementNewStyle) method.methodBody().statements().getFirst();
        SwitchEntry se0 = ns.entries().getFirst();
        IfElseStatement ifElse = (IfElseStatement) ((Block)se0.statement()).statements().getFirst();
        VariableData vd = VariableDataImpl.of(ifElse);
        VariableInfoContainer vicI = vd.variableInfoContainerOrNull("i");
        assertTrue(vicI.hasMerge());
    }
}
