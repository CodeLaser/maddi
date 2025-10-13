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
import org.e2immu.analyzer.modification.prepwork.hcs.HiddenContentSelector;
import org.e2immu.analyzer.modification.prepwork.hct.HiddenContentTypes;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.e2immu.analyzer.modification.linkedvariables.lv.LinkedVariablesImpl.EMPTY;
import static org.e2immu.analyzer.modification.linkedvariables.lv.LinkedVariablesImpl.LINKED_VARIABLES_METHOD;
import static org.e2immu.analyzer.modification.prepwork.hcs.HiddenContentSelector.HCS_PARAMETER;
import static org.e2immu.analyzer.modification.prepwork.hct.HiddenContentTypes.HIDDEN_CONTENT_TYPES;
import static org.junit.jupiter.api.Assertions.*;

public class TestLinkToReturnValueListGet extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.List;
            class X {
                final static class M {
                    private int i;
                    public int getI() { return i; }
                    public void setI(int i) { this.i = i; }
                }
                static <T> T get(List<T> list, int i) {
                    return list.get(i);
                }
                static M get2(List<M> list, int i) {
                    return list.get(i);
                }
                static String get3(List<String> list, int i) {
                    return list.get(i);
                }
                static Object get4(List<Object> list, int i) {
                    return list.get(i);
                }
            }
            """;

    @DisplayName("return list.get(index)")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        MethodInfo listGet = X.findUniqueMethod("get", 2);

        Statement s0 = listGet.methodBody().statements().getFirst();
        VariableData vd0 = VariableDataImpl.of(s0);
        assertNotNull(vd0);
        VariableInfo viRv = vd0.variableInfo(listGet.fullyQualifiedName());
        assertEquals("*-4-0:_synthetic_list, -1-:_synthetic_list[i], *M-4-0M:list", viRv.linkedVariables().toString());

        assertEquals(viRv.linkedVariables(), listGet.analysis().getOrDefault(LINKED_VARIABLES_METHOD, EMPTY));

        MethodInfo listGet2 = X.findUniqueMethod("get2", 2);
        assertEquals("*M-2-0M|*-?:_synthetic_list, -1-:_synthetic_list[i], *M-2-0M|*-0.?:list",
                listGet2.analysis().getOrDefault(LINKED_VARIABLES_METHOD, EMPTY).toString());

        MethodInfo listGet3 = X.findUniqueMethod("get3", 2);
        assertEquals("-1-:_synthetic_list[i]", listGet3.analysis().getOrDefault(LINKED_VARIABLES_METHOD,
                EMPTY).toString());

        MethodInfo listGet4 = X.findUniqueMethod("get4", 2);
        assertEquals("*-4-0:_synthetic_list, -1-:_synthetic_list[i], *M-4-0M:list",
                listGet4.analysis().getOrDefault(LINKED_VARIABLES_METHOD, EMPTY).toString());
    }


    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            class X {
                final static class M {
                    private int i;
                    public int getI() { return i; }
                    public void setI(int i) { this.i = i; }
                }
                static <T> T get(List<T> list, int i) {
                    return new ArrayList<>(list).get(i);
                }
                static M get2(List<M> list, int i) {
                    return new ArrayList<>(list).get(i);
                }
                static M get2b(List<M> list, int i) {
                    ArrayList<M> arrayList = new ArrayList<>(list);
                    return arrayList.get(i);
                }
                static M get2c(List<M> list, int i) {
                    List<M> arrayList = new ArrayList<>(list);
                    return arrayList.get(i);
                }
                static String get3(List<String> list, int i) {
                    return new ArrayList<>(list).get(i);
                }
                static Object get4(List<Object> list, int i) {
                    return new ArrayList<>(list).get(i);
                }
            }
            """;

    @DisplayName("return new ArrayList<>(list).get(index)")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);

        TypeInfo typeInfo = javaInspector.compiledTypesManager().get(ArrayList.class);
        TypeInfo collectionTypeInfo = javaInspector.compiledTypesManager().get(Collection.class);
        MethodInfo arrayListConstructor = typeInfo.findConstructor(collectionTypeInfo);
        assertEquals("java.util.ArrayList.<init>(java.util.Collection<? extends E>)", arrayListConstructor.fullyQualifiedName());
        HiddenContentTypes methodHct = arrayListConstructor.analysis().getOrDefault(HIDDEN_CONTENT_TYPES, HiddenContentTypes.NO_VALUE);
        assertEquals("0=E - 1=Collection", methodHct.detailedSortedTypes());
        assertEquals("ArrayList:E - <init>:Collection", methodHct.toString());

        ParameterInfo p0 = arrayListConstructor.parameters().getFirst();
        HiddenContentSelector paramHcs = p0.analysis().getOrDefault(HCS_PARAMETER, HiddenContentSelector.NONE);
        assertEquals("0=0,1=*", paramHcs.detailed());
        assertSame(ValueImpl.IndependentImpl.INDEPENDENT_HC, p0.analysis()
                .getOrDefault(PropertyImpl.INDEPENDENT_PARAMETER, ValueImpl.IndependentImpl.DEPENDENT));
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        MethodInfo listGet = X.findUniqueMethod("get", 2);
        {
            Statement s0 = listGet.methodBody().statements().getFirst();
            VariableData vd0 = VariableDataImpl.of(s0);
            assertNotNull(vd0);
            VariableInfo viRv = vd0.variableInfo(listGet.fullyQualifiedName());
            assertEquals("*-4-0:_synthetic_list, -1-:_synthetic_list[i], *M-4-0M:list",
                    viRv.linkedVariables().toString());

            assertEquals(viRv.linkedVariables(), listGet.analysis().getOrDefault(LINKED_VARIABLES_METHOD,
                    EMPTY));
        }
        {
            MethodInfo listGet2c = X.findUniqueMethod("get2c", 2);
            VariableData vd0 = VariableDataImpl.of(listGet2c.methodBody().statements().getFirst());
            VariableInfo viAL0 = vd0.variableInfo("arrayList");
            assertEquals("0M-4-0M:list", viAL0.linkedVariables().toString());

            VariableData vd1 = VariableDataImpl.of(listGet2c.methodBody().lastStatement());
            VariableInfo viRv1 = vd1.variableInfo(listGet2c.fullyQualifiedName());
            assertEquals("""
                    *M-2-0M|*-?:_synthetic_list, -1-:_synthetic_list[i], *M-2-0M|*-0.?:arrayList, *M-4-0M:list\
                    """, viRv1.linkedVariables().toString());

            assertEquals("""
                    *M-2-0M|*-?:_synthetic_list, -1-:_synthetic_list[i], *M-4-0M:list\
                    """, listGet2c.analysis().getOrDefault(LINKED_VARIABLES_METHOD, EMPTY).toString());
        }
        {
            MethodInfo listGet2b = X.findUniqueMethod("get2b", 2);
            VariableData vd0 = VariableDataImpl.of(listGet2b.methodBody().statements().getFirst());
            VariableInfo viAL0 = vd0.variableInfo("arrayList");
            assertEquals("0M-4-0M:list", viAL0.linkedVariables().toString());

            VariableData vd1 = VariableDataImpl.of(listGet2b.methodBody().lastStatement());
            VariableInfo viRv1 = vd1.variableInfo(listGet2b.fullyQualifiedName());
            assertEquals("""
                    *M-2-0M|*-?:_synthetic_list, -1-:_synthetic_list[i], *M-2-0M|*-0.?:arrayList, *M-4-0M:list\
                    """, viRv1.linkedVariables().toString());

            assertEquals("*M-2-0M|*-?:_synthetic_list, -1-:_synthetic_list[i], *M-4-0M:list",
                    listGet2b.analysis().getOrDefault(LINKED_VARIABLES_METHOD, EMPTY).toString());
        }
        MethodInfo listGet2 = X.findUniqueMethod("get2", 2);
        assertEquals("*M-2-0M|*-?:_synthetic_list, -1-:_synthetic_list[i], *M-4-0M:list",
                listGet2.analysis().getOrDefault(LINKED_VARIABLES_METHOD, EMPTY).toString());

        MethodInfo listGet3 = X.findUniqueMethod("get3", 2);
        assertEquals("-1-:_synthetic_list[i]",
                listGet3.analysis().getOrDefault(LINKED_VARIABLES_METHOD, EMPTY).toString());

        MethodInfo listGet4 = X.findUniqueMethod("get4", 2);
        assertEquals("*-4-0:_synthetic_list, -1-:_synthetic_list[i], *M-4-0M:list",
                listGet4.analysis().getOrDefault(LINKED_VARIABLES_METHOD, EMPTY).toString());
    }

    private void testLinks3(TypeInfo X) {
        MethodInfo listGet = X.findUniqueMethod("get", 2);

        Statement s0 = listGet.methodBody().statements().getFirst();
        VariableData vd0 = VariableDataImpl.of(s0);
        assertNotNull(vd0);
        VariableInfo viRv = vd0.variableInfo(listGet.fullyQualifiedName());
        assertEquals("*-4-0:_synthetic_list, -1-:_synthetic_list[i], *M-4-0M:list",
                viRv.linkedVariables().toString());

        assertEquals(viRv.linkedVariables(), listGet.analysis().getOrDefault(LINKED_VARIABLES_METHOD,
                EMPTY));

        MethodInfo listGet2 = X.findUniqueMethod("get2", 2);
        assertEquals("*M-2-0M|*-?:_synthetic_list, -1-:_synthetic_list[i], *M-2-0M|*-*.0.?:list",
                listGet2.analysis().getOrDefault(LINKED_VARIABLES_METHOD, EMPTY).toString());

        MethodInfo listGet3 = X.findUniqueMethod("get3", 2);
        assertEquals("-1-:_synthetic_list[i]", listGet3.analysis().getOrDefault(LINKED_VARIABLES_METHOD,
                EMPTY).toString());

        MethodInfo listGet4 = X.findUniqueMethod("get4", 2);
        assertEquals("*-4-0:_synthetic_list, -1-:_synthetic_list[i], *M-4-0M:list",
                listGet4.analysis().getOrDefault(LINKED_VARIABLES_METHOD, EMPTY).toString());
    }


    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            import java.util.List;
            class X {
                final static class M {
                    private int i;
                    public int getI() { return i; }
                    public void setI(int i) { this.i = i; }
                }
                static <T> T get(List<T> list, int i) {
                    return list.subList(0, 10).get(i);
                }
                static M get2(List<M> list, int i) {
                    return list.subList(0, 10).get(i);
                }
                static String get3(List<String> list, int i) {
                    return list.subList(0, 10).get(i);
                }
                static Object get4(List<Object> list, int i) {
                    return list.subList(0, 10).get(i);
                }
            }
            """;

    @DisplayName("return list.subList(0, 10).get(index)")
    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse(INPUT3);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);
        testLinks3(X);
    }


    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            import java.util.List;
            class X {
                final static class M {
                    private int i;
                    public int getI() { return i; }
                    public void setI(int i) { this.i = i; }
                }
                static <T> T get(List<T> list, int i) {
                    return list.subList(0, 10).subList(1, 5).get(i);
                }
                static M get2(List<M> list, int i) {
                    return list.subList(0, 10).subList(1, 5).get(i);
                }
                static String get3(List<String> list, int i) {
                    return list.subList(0, 10).subList(1, 5).get(i);
                }
                static Object get4(List<Object> list, int i) {
                    return list.subList(0, 10).subList(1, 5).get(i);
                }
            }
            """;

    @DisplayName("return list.subList(0, 10).subList(1, 5).get(index)")
    @Test
    public void test4() {
        TypeInfo X = javaInspector.parse(INPUT4);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);
        testLinks3(X);
    }

}
