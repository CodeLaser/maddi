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

package org.e2immu.analyzer.modification.analyzer.modification;


import org.e2immu.analyzer.modification.analyzer.CommonTest;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestModificationBasics extends CommonTest {
    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.List;
            import java.util.Iterator;
            class Test {
                public Iterator<String> m(List<String> items) {
                    return items.iterator();
                }
            }
            """;

    @Test
    public void test() {
        TypeInfo X = javaInspector.parse(INPUT);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        MethodInfo m = X.findUniqueMethod("m", 1);
        MethodCall iteratorCall = (MethodCall) m.methodBody().statements().getFirst().expression();
        MethodInfo iterator = iteratorCall.methodInfo();
        assertEquals("java.util.List.iterator()", iterator.fullyQualifiedName());
        assertFalse(iterator.isModifying());

        assertFalse(m.isModifying());
        assertFalse(m.parameters().getFirst().isModified());
    }

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.ArrayList;import java.util.List;
            class X {
                static class M {
                    int i;
                    void setI(int i) { this.i = i; }
                }
                void modifyParam(List<M> list, int k) {
                    list.get(0).setI(k);
                }
                void modifyParam2(List<M> list, int k) {
                    M m = list.get(0);
                    m.setI(k);
                }
                void modifyParam3(List<M> list, int k) {
                    List<M> copy = new ArrayList<>(list);
                    M m = list.get(0);
                    m.setI(k);
                }
            }
            """;


    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);
        {
            MethodInfo m = X.findUniqueMethod("modifyParam", 2);
            assertFalse(m.isModifying());
            ParameterInfo pi0 = m.parameters().getFirst();
            assertFalse(pi0.isUnmodified());
        }
        {
            MethodInfo m = X.findUniqueMethod("modifyParam2", 2);
            assertFalse(m.isModifying());
            ParameterInfo pi0 = m.parameters().getFirst();
            assertFalse(pi0.isUnmodified());
            VariableData vd0 = VariableDataImpl.of( m.methodBody().statements().getFirst());
            VariableInfo vi0m = vd0.variableInfo("m");
            assertEquals("*M-2-0M|*-0:_synthetic_list, -1-:_synthetic_list[0], *M-2-0M|*-0.0:list",
                    vi0m.linkedVariables().toString());
        }
        {
            MethodInfo m = X.findUniqueMethod("modifyParam3", 2);
            assertFalse(m.isModifying());
            ParameterInfo pi0 = m.parameters().getFirst();
            assertFalse(pi0.isUnmodified());
            {
                VariableData vd1 = VariableDataImpl.of(m.methodBody().statements().get(1));
                VariableInfo vi1m = vd1.variableInfo("m");
                assertEquals("*M-2-0M|*-0:_synthetic_list, -1-:_synthetic_list[0], *M-4-0M:copy, *M-2-0M|*-0.0:list",
                        vi1m.linkedVariables().toString());
                VariableInfo vi1list = vd1.variableInfo(pi0);
                assertFalse(vi1list.isModified());
            }
            {
                VariableData vd2 = VariableDataImpl.of(m.methodBody().statements().get(2));
                VariableInfo vi2m = vd2.variableInfo("m");
                assertEquals("""
                                *M-2-0M|*-0:_synthetic_list, -1-:_synthetic_list[0], *M-4-0M:copy, -2-|*-0:i, \
                                -2-|*-0:k, *M-2-0M|*-0.0:list\
                                """,
                        vi2m.linkedVariables().toString());
                VariableInfo vi1list = vd2.variableInfo(pi0);
                assertTrue(vi1list.isModified());
            }
        }
    }
}
