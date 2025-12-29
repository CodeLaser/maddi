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
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.statement.Statement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestModificationBasics extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
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
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
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
    private static final String INPUT2 = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            class Test {
                List<String> list = new ArrayList<>();
                public void add(String s) {
                    list.add(s);
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        MethodInfo add = X.findUniqueMethod("add", 1);
        Statement s0 = add.methodBody().statements().getFirst();
        MethodCall listAddCall = (MethodCall) s0.expression();
        MethodInfo listAdd = listAddCall.methodInfo();
        assertEquals("java.util.List.add(E)", listAdd.fullyQualifiedName());
        assertTrue(listAdd.isModifying());

        VariableData vd = VariableDataImpl.of(s0);
        VariableInfo viList = vd.variableInfo("a.b.Test.list");
        assertTrue(viList.isModified());

        FieldInfo list = X.getFieldByName("list", true);
        assertTrue(list.isModified());

        assertTrue(add.isModifying());
        assertFalse(add.parameters().getFirst().isModified());
    }

    @Language("java")
    private static final String INPUT3 = """
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
    public void test3() {
        TypeInfo X = javaInspector.parse(INPUT3);
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
            VariableData vd0 = VariableDataImpl.of(m.methodBody().statements().getFirst());
            VariableInfo vi0m = vd0.variableInfo("m");
            assertEquals("m∈0:list.§$s", vi0m.linkedVariables().toString());
        }
        {
            MethodInfo m = X.findUniqueMethod("modifyParam3", 2);
            assertFalse(m.isModifying());
            ParameterInfo pi0 = m.parameters().getFirst();
            assertFalse(pi0.isUnmodified());
            {
                VariableData vd1 = VariableDataImpl.of(m.methodBody().statements().get(1));
                VariableInfo vi1m = vd1.variableInfo("m");
                // copy ⊆ list, so m is not an element of copy!
                assertEquals("m∈0:list.§$s", vi1m.linkedVariables().toString());
                VariableInfo vi1list = vd1.variableInfo(pi0);
                assertFalse(vi1list.isModified());
            }
            {
                VariableData vd2 = VariableDataImpl.of(m.methodBody().statements().get(2));
                VariableInfo vi2m = vd2.variableInfo("m");
                assertEquals("m.i≡1:k,m.i≤0:list.§$s,m.i∩copy.§$s,m∈0:list.§$s", vi2m.linkedVariables().toString());
                VariableInfo vi1list = vd2.variableInfo(pi0);
                assertTrue(vi1list.isModified());
            }
        }
    }

    @Language("java")
    private static final String INPUT4 = """
            public class X {
              public static void insertionSort(Comparable[] a, int first, int last) {
                for (int unsorted = first + 1; unsorted <= last; unsorted++) {
                  Comparable firstUnsorted = a[unsorted];
                  insertInOrder(firstUnsorted, a, first, unsorted - 1);
                }
              }
            
              private static void insertInOrder(Comparable element, Comparable[] a, int first, int last) {
                if (element.compareTo(a[last]) >= 0) a[last + 1] = element;
                else if (first < last) {
                  a[last + 1] = a[last];
                  insertInOrder(element, a, first, last - 1);
                } else {
                  a[last + 1] = a[last];
                  a[last] = element;
                }
              }
            }
            """;

    @DisplayName("recursive method")
    @Test
    public void test4() {
        TypeInfo B = javaInspector.parse(INPUT4);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);

        MethodInfo insertInOrder = B.findUniqueMethod("insertInOrder", 4);
        assertTrue(insertInOrder.parameters().get(1).isModified());

        MethodInfo insertionSort = B.findUniqueMethod("insertionSort", 3);
        assertTrue(insertionSort.parameters().getFirst().isModified());
    }


    @Language("java")
    private static final String INPUT5 = """
            package a.b;
            import java.util.ArrayList;
            import java.util.HashSet;
            import java.util.List;
            import java.util.Set;
            class X {
                record R<T>(Set<T> s, List<T> l) {}
                static <T> void method(T t) {
                    Set<T> set = new HashSet<>();
                    List<T> list = new ArrayList<>();
                    R<T> r = new R<>(set, list);
                    Set<T> set2 = r.s;
                    set2.add(t); // assert that set has been modified, but not list
                }
            }
            """;

    // follows TestStaticValuesRecord,8
    @DisplayName("pack and unpack, with local variables")
    @Test
    public void test5() {
        TypeInfo X = javaInspector.parse(INPUT5);
        List<Info> ao = prepWork(X);
        analyzer.go(ao);
        MethodInfo method = X.findUniqueMethod("method", 1);

        Statement s2 = method.methodBody().statements().get(2);
        VariableData vd2 = VariableDataImpl.of(s2);

        VariableInfo vi2Set = vd2.variableInfo("set");
        assertFalse(vi2Set.isModified());

        VariableInfo vi2List = vd2.variableInfo("list");
        assertFalse(vi2List.isModified());

        Statement s4 = method.methodBody().statements().get(4);
        VariableData vd4 = VariableDataImpl.of(s4);

        VariableInfo vi4Set = vd4.variableInfo("set");
        assertTrue(vi4Set.isModified());

        VariableInfo vi4List = vd4.variableInfo("list");
        assertFalse(vi4List.isModified());
    }

}
