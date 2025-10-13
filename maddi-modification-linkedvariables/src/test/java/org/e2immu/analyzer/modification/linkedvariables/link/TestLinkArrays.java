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

package org.e2immu.analyzer.modification.linkedvariables.link;

import org.e2immu.analyzer.modification.linkedvariables.CommonTest;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.e2immu.language.cst.impl.analysis.PropertyImpl.IMMUTABLE_TYPE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.ImmutableImpl.MUTABLE;
import static org.junit.jupiter.api.Assertions.*;

public class TestLinkArrays extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            class X {
                static <T> void method(T t, T[] array) {
                    T t0 = array[0];
                    array[1] = t;
                }
            }
            """;

    @DisplayName("basics")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        MethodInfo method = X.findUniqueMethod("method", 2);
        ParameterInfo t = method.parameters().getFirst();
        ParameterInfo array = method.parameters().get(1);
        {
            VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getFirst());
            VariableInfo vi0T0 = vd0.variableInfo("t0");
            assertEquals("*-4-0:array, -1-:array[0]", vi0T0.linkedVariables().toString());
            VariableInfo vi0Array = vd0.variableInfo(array);
            assertEquals("0-4-*:array[0], 0-4-*:t0", vi0Array.linkedVariables().toString());
            assertFalse(vi0Array.isModified());
        }
        {
            VariableData vd1 = VariableDataImpl.of(method.methodBody().statements().get(1));
            VariableInfo vi1T0 = vd1.variableInfo("t0");
            assertEquals("*-4-0:array, -1-:array[0]", vi1T0.linkedVariables().toString());
            VariableInfo vi1T = vd1.variableInfo(t);
            assertEquals("*-4-0:array, -1-:array[1]", vi1T.linkedVariables().toString());
            VariableInfo vi1Array = vd1.variableInfo(array);
            assertEquals("0-4-*:array[0], 0-4-*:array[1], 0-4-*:t, 0-4-*:t0", vi1Array.linkedVariables().toString());
            assertTrue(vi1Array.isModified());
        }
    }

    @Language("java")
    private static final String INPUT1B = """
            package a.b;
            class X {
                static class M { int i; void setI(int i) { this.i = i; } }
                static void method(M t, M[] array) {
                    M t0 = array[0];
                    array[1] = t;
                }
            }
            """;

    @DisplayName("basics, mutable content")
    @Test
    public void test1B() {
        TypeInfo X = javaInspector.parse(INPUT1B);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);
        TypeInfo M = X.findSubType("M");
        assertSame(MUTABLE, M.analysis().getOrDefault(IMMUTABLE_TYPE, MUTABLE));

        MethodInfo method = X.findUniqueMethod("method", 2);
        ParameterInfo t = method.parameters().getFirst();
        ParameterInfo array = method.parameters().get(1);
        {
            VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getFirst());
            VariableInfo vi0T0 = vd0.variableInfo("t0");
            assertEquals("*M-2-0M|*-0:array, -1-:array[0]", vi0T0.linkedVariables().toString());
            VariableInfo vi0Array = vd0.variableInfo(array);
            assertEquals("0M-2-*M|0-*:array[0], 0M-2-*M|0-*:t0", vi0Array.linkedVariables().toString());
            assertFalse(vi0Array.isModified());
        }
        {
            VariableData vd1 = VariableDataImpl.of(method.methodBody().statements().get(1));
            VariableInfo vi1T0 = vd1.variableInfo("t0");
            assertEquals("*M-2-0M|*-0:array, -1-:array[0]", vi1T0.linkedVariables().toString());
            VariableInfo vi1T = vd1.variableInfo(t);
            assertEquals("*M-2-0M|*-1:array, -1-:array[1]", vi1T.linkedVariables().toString());
            VariableInfo vi1Array = vd1.variableInfo(array);
            assertEquals("0M-2-*M|0-*:array[0], 0M-2-*M|1-*:array[1], 0M-2-*M|1-*:t, 0M-2-*M|0-*:t0",
                    vi1Array.linkedVariables().toString());
            assertTrue(vi1Array.isModified());
        }
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import org.e2immu.annotation.Independent;
            import org.e2immu.annotation.method.GetSet;
            class X {
                @Independent(hc = true)
                interface I<T> {
                    @GetSet("variables")
                    T get(int i);
                    @GetSet("variables")
                    void set(int i, T t);
                }
                static <T> void method(T t, I<T> i) {
                    T t0 = i.get(0);
                    i.set(1, t);
                }
            }
            """;

    @DisplayName("getter/setter")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);
        TypeInfo I = X.findSubType("I");
        assertSame(MUTABLE, I.analysis().getOrDefault(IMMUTABLE_TYPE, MUTABLE));

        FieldInfo fieldInfo = I.getFieldByName("variables", true);
        assertTrue(fieldInfo.isSynthetic());
        {
            MethodInfo iGet = I.findUniqueMethod("get", 1);
            Value.FieldValue iGetFieldValue = iGet.getSetField();
            assertSame(fieldInfo, iGetFieldValue.field());
            assertNotNull(iGetFieldValue);
            assertEquals("a.b.X.I.variables", iGetFieldValue.field().fullyQualifiedName());
            assertFalse(iGetFieldValue.setter());
        }
        {
            MethodInfo iSet = I.findUniqueMethod("set", 2);
            Value.FieldValue iSetFieldValue = iSet.getSetField();
            assertSame(fieldInfo, iSetFieldValue.field());
            assertNotNull(iSetFieldValue);
            assertEquals("a.b.X.I.variables", iSetFieldValue.field().fullyQualifiedName());
            assertTrue(iSetFieldValue.setter());
        }

        // NOTE: if you remove the annotation "@Independent(hc = true)" from I, there will be no immutability info
        ValueImpl.ImmutableImpl immutable = I.analysis().getOrNull(IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.class);
        assertSame(MUTABLE, immutable);

        MethodInfo method = X.findUniqueMethod("method", 2);
        testCommon23(method);
    }

    private void testCommon23(MethodInfo method) {
        ParameterInfo t = method.parameters().getFirst();
        ParameterInfo i = method.parameters().get(1);
        {
            VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getFirst());

            VariableInfo vi0T0 = vd0.variableInfo("t0");
            assertEquals("E=i.variables[0]", vi0T0.staticValues().toString());
            assertEquals("*M-4-0M:i, *-4-0:variables, -1-:variables[0]", vi0T0.linkedVariables().toString());

            VariableInfo vi0Array = vd0.variableInfo(i);
            // variables is a field inside I, so I and variables share mutable, accessible content. This warrants -2-.
            // further, variables is part of I, so the * is warranted. Finally, the modification area is given.
            assertEquals("0M-4-*M:t0, 0M-2-*M|0-*:variables, 0M-4-*M:variables[0]", vi0Array.linkedVariables().toString());
            assertFalse(vi0Array.isModified());
        }
        {
            VariableData vd1 = VariableDataImpl.of(method.methodBody().statements().get(1));
            VariableInfo vi1T0 = vd1.variableInfo("t0");
            assertEquals("*M-4-0M:i, *-4-0:variables, -1-:variables[0]", vi1T0.linkedVariables().toString());
            assertFalse(vi1T0.isModified());

            VariableInfo vi1T = vd1.variableInfo(t);
            assertEquals("*M-4-0M:i, *-4-0:variables, -1-:variables[1]", vi1T.linkedVariables().toString());
            assertFalse(vi1T.isModified());

            VariableInfo vi1Array = vd1.variableInfo(i);
            assertEquals("0M-4-*M:t, 0M-4-*M:t0, 0M-2-*M|0-*:variables, 0M-4-*M:variables[0], 0M-4-*M:variables[1]",
                    vi1Array.linkedVariables().toString());
            assertTrue(vi1Array.isModified());
        }
    }


    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            import org.e2immu.annotation.method.GetSet;
            class X {
                interface I {
                    @GetSet("variables")
                    Object get(int i);
                    @GetSet("variables")
                    void set(int i, Object t);
                }
                static void method1(Object t, I i) {
                    Object t0 = i.get(0);
                    i.set(1, t);
                }
                static void method2(int t, I i) {
                    int t0 = (int) i.get(0);
                    i.set(1, t);
                }
            }
            """;

    @DisplayName("getter/setter, Object, cast in between")
    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse(INPUT3);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        MethodInfo method1 = X.findUniqueMethod("method1", 2);
        testCommon23(method1);

        MethodInfo method2 = X.findUniqueMethod("method2", 2);
        ParameterInfo t = method2.parameters().getFirst();
        ParameterInfo i = method2.parameters().get(1);
        {
            VariableData vd0 = VariableDataImpl.of(method2.methodBody().statements().getFirst());
            VariableInfo vi0T0 = vd0.variableInfo("t0");
            assertEquals("E=i.variables[0]", vi0T0.staticValues().toString());
            assertEquals("-1-:variables[0]", vi0T0.linkedVariables().toString());
            VariableInfo vi0Array = vd0.variableInfo(i);
            assertEquals("0M-2-*M|0-*:variables", vi0Array.linkedVariables().toString());
            assertFalse(vi0Array.isModified());
        }
        {
            VariableData vd1 = VariableDataImpl.of(method2.methodBody().statements().get(1));
            VariableInfo vi1T0 = vd1.variableInfo("t0");
            assertEquals("-1-:variables[0]", vi1T0.linkedVariables().toString());
            assertFalse(vi1T0.isModified());

            VariableInfo vi1T = vd1.variableInfo(t);
            assertEquals("*M-4-0M:i, *-4-0:variables, -1-:variables[1]", vi1T.linkedVariables().toString());
            assertFalse(vi1T.isModified());

            VariableInfo vi1Array = vd1.variableInfo(i);
            assertEquals("0M-4-*M:t, 0M-2-*M|0-*:variables, 0M-4-*M:variables[1]",
                    vi1Array.linkedVariables().toString());
            assertTrue(vi1Array.isModified());
        }
    }

    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            class B {
                static class M { private int i; public int get() { return i; } public void set(int i) { this.i = i; } }
                public static M[][] transpose(M[][] a) {
                    int m = a.length;
                    int n = a[0].length;
                    M[][] t = new M[n][m];
                    for (int i = 0; i < m; i++) {
                        for (int j = 0; j < n; j++) {
                            t[j][i] = a[i][j];
                        }
                    }
                    return t;
                }
            }
            """;

    @DisplayName("double array of mutable objects")
    @Test
    public void test4() {
        TypeInfo B = javaInspector.parse(INPUT4);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
        MethodInfo transpose = B.findUniqueMethod("transpose", 1);
        ParameterInfo a = transpose.parameters().getFirst();
        {
            VariableData vd = VariableDataImpl.of(transpose.methodBody().statements().getFirst());
            VariableInfo viA = vd.variableInfo(a);
            assertEquals("", viA.linkedVariables().toString());
            VariableInfo viM = vd.variableInfo("m");
            assertEquals("", viM.linkedVariables().toString());
        }
        {
            VariableData vd = VariableDataImpl.of(transpose.methodBody().statements().get(1));
            assertEquals("a.b.B.transpose(a.b.B.M[][]):0:a, a.b.B.transpose(a.b.B.M[][]):0:a[0], m, n",
                    vd.knownVariableNamesToString());
            VariableInfo viN = vd.variableInfo("n");
            assertEquals("", viN.linkedVariables().toString());

            VariableInfo viA = vd.variableInfo(a);
            assertEquals("0M-2-*M|0-*:a[0]", viA.linkedVariables().toString());
            VariableInfo viA0 = vd.variableInfo("a.b.B.transpose(a.b.B.M[][]):0:a[0]");
            assertEquals("*M-2-0M|*-0:a", viA0.linkedVariables().toString());
        }

        {
            VariableData vd = VariableDataImpl.of(transpose.methodBody().statements().get(2));
            assertEquals("a.b.B.transpose(a.b.B.M[][]):0:a, a.b.B.transpose(a.b.B.M[][]):0:a[0], m, n, t",
                    vd.knownVariableNamesToString());
            VariableInfo viN = vd.variableInfo("n");
            assertEquals("", viN.linkedVariables().toString());

            VariableInfo viT = vd.variableInfo("t");
            assertEquals("", viT.linkedVariables().toString());

            VariableInfo viA = vd.variableInfo(a);
            assertEquals("0M-2-*M|0-*:a[0]", viA.linkedVariables().toString());
            VariableInfo viA0 = vd.variableInfo("a.b.B.transpose(a.b.B.M[][]):0:a[0]");
            assertEquals("*M-2-0M|*-0:a", viA0.linkedVariables().toString());
        }
        {
            Statement s30000 = transpose.methodBody().statements().get(3).block()
                    .statements().getFirst().block().statements().getFirst();
            assertEquals("t[j][i]=a[i][j];", s30000.toString());
            VariableData vd = VariableDataImpl.of(s30000);
            assertEquals("""
                            a.b.B.transpose(a.b.B.M[][]):0:a, a.b.B.transpose(a.b.B.M[][]):0:a[0], \
                            a.b.B.transpose(a.b.B.M[][]):0:a[i], a.b.B.transpose(a.b.B.M[][]):0:a[i][j], \
                            i, j, m, n, t, t[j], t[j][i]\
                            """,
                    vd.knownVariableNamesToString());
            VariableInfo viTJI = vd.variableInfo("t[j][i]");
            assertEquals("*M-2-0M|*-?.?:a, *M-2-0M|*-?:a[i], -1-:a[i][j], *M-2-0M|*-?.?:t, *M-2-0M|*-?:t[j]",
                    viTJI.linkedVariables().toString());

            VariableInfo viT = vd.variableInfo("t");
            assertEquals("""
                    0M-2-0M:a, 0M-2-*M|0-*:a[0], 0-2-0:a[i], 0M-2-*M|?.?-*:a[i][j], 0M-2-*M|?-*:t[j], 0M-2-*M|?.?-*:t[j][i]\
                    """, viT.linkedVariables().toString());

            VariableInfo viA = vd.variableInfo(a);
            assertEquals("""
                    0M-2-*M|0-*:a[0], 0M-2-*M|?-*:a[i], 0M-2-*M|?.?-*:a[i][j], 0M-2-0M:t, 0-2-0:t[j], 0M-2-*M|?.?-*:t[j][i]\
                    """, viA.linkedVariables().toString());

            VariableInfo viA0 = vd.variableInfo("a.b.B.transpose(a.b.B.M[][]):0:a[0]");
            assertEquals("*M-2-0M|*-0:a", viA0.linkedVariables().toString());
        }
    }
}
