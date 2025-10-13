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
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.e2immu.language.cst.api.variable.Variable;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestGetSet extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            class X {
                record R(int i) {}
                Object[] objects = new Object[10];
                List<String> list = new ArrayList<>();
                double d;
                R r;
                R rr;
            
                Object getO(int i) { return objects[i]; }
                Object getO2() { return objects[2]; } // not @GetSet YET
            
                String getS(int j) { return list.get(j); }
                String getS2() { return list.get(0); } // not @GetSet YET
            
                double d() { return d; }
                double dd() { return this.d; }
            
                int ri() { return r.i; }
                int rri() { return rr.i; }
                int ri2() { return r.i(); } // ri2 is not an accessor! (we must have a field)
            
                void setO(Object o, int i) { objects[i] = o; }
                X setO2(int i, Object o) { this.objects[i] = o; return this; }
                X setO3(Object o) { this.objects[0] = o; return this; } // not @GetSet yet
            
                void setS(Object o, int i) { list.set(i, o); }
                X setS2(int i, Object o) { this.list.set(i, o); return this; }
                X setS3(Object o) { this.list.set(0, o); return this; } // not @GetSet yet
            
                void setD(double d) { this.d = d; }
                void setRI(int i) { this.r.i = i; }
            
                Object callGetO() {
                    return getO(3);
                }
                Object callGetS() {
                    return getS(3);
                }
                void callSetS2(String s) {
                    setS2(3, "abc");
                }
            }
            """;

    @DisplayName("getters and setters")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doPrimaryType(X);

        FieldInfo objects = X.getFieldByName("objects", true);
        FieldInfo list = X.getFieldByName("list", true);
        FieldInfo d = X.getFieldByName("d", true);
        TypeInfo R = X.findSubType("R");
        FieldInfo ri = R.getFieldByName("i", true);

        MethodInfo getO = X.findUniqueMethod("getO", 1);
        assertSame(objects, getO.getSetField().field());
        MethodInfo getO2 = X.findUniqueMethod("getO2", 0);
        assertNull(getO2.getSetField().field());

        MethodInfo setO = X.findUniqueMethod("setO", 2);
        assertSame(objects, setO.getSetField().field());
        MethodInfo setO2 = X.findUniqueMethod("setO2", 2);
        assertSame(objects, setO2.getSetField().field());
        MethodInfo setO3 = X.findUniqueMethod("setO3", 1);
        assertNull(setO3.getSetField().field());

        MethodInfo getS = X.findUniqueMethod("getS", 1);
        assertSame(list, getS.getSetField().field());
        MethodInfo getS2 = X.findUniqueMethod("getS2", 0);
        assertNull(getS2.getSetField().field());

        MethodInfo setS = X.findUniqueMethod("setS", 2);
        assertSame(list, setS.getSetField().field());
        MethodInfo setS2 = X.findUniqueMethod("setS2", 2);
        assertSame(list, setS2.getSetField().field());
        MethodInfo setS3 = X.findUniqueMethod("setS3", 1);
        assertNull(setS3.getSetField().field());

        MethodInfo md = X.findUniqueMethod("d", 0);
        assertSame(d, md.getSetField().field());
        MethodInfo mdd = X.findUniqueMethod("dd", 0);
        assertSame(d, mdd.getSetField().field());

        MethodInfo mri = X.findUniqueMethod("ri", 0);
        assertSame(ri, mri.getSetField().field());
        MethodInfo mRri = X.findUniqueMethod("rri", 0);
        assertSame(ri, mRri.getSetField().field()); // IMPORTANT! we cannot yet tell the difference: are we accessing r or rr??
        MethodInfo mRi2 = X.findUniqueMethod("ri2", 0);
        assertNull(mRi2.getSetField().field());

        MethodInfo setD = X.findUniqueMethod("setD", 1);
        assertSame(d, setD.getSetField().field());
        MethodInfo setRI = X.findUniqueMethod("setRI", 1);
        assertSame(ri, setRI.getSetField().field());

        {
            MethodInfo test1 = X.findUniqueMethod("callGetO", 0);
            Statement s0 = test1.methodBody().statements().get(0);
            MethodCall mc0 = (MethodCall) s0.expression();
            Variable v = runtime.getterVariable(mc0);
            assertEquals("a.b.X.objects[3]", v.fullyQualifiedName());
            assertEquals(runtime.objectParameterizedType(), v.parameterizedType());
            if (v instanceof DependentVariable dv) {
                assertEquals(1, dv.arrayVariable().parameterizedType().arrays());
            } else fail();
        }
        {
            MethodInfo test1 = X.findUniqueMethod("callGetS", 0);
            Statement s0 = test1.methodBody().statements().get(0);
            MethodCall mc0 = (MethodCall) s0.expression();
            Variable v = runtime.getterVariable(mc0);
            assertEquals("java.util.List._synthetic_list#a.b.X.list[3]", v.fullyQualifiedName());
            assertEquals(runtime.stringParameterizedType(), v.parameterizedType());
        }
        {
            MethodInfo test1 = X.findUniqueMethod("callSetS2", 1);
            Statement s0 = test1.methodBody().statements().get(0);
            MethodCall mc0 = (MethodCall) s0.expression();
            Variable v = runtime.setterVariable(mc0);
            assertEquals("java.util.List._synthetic_list#a.b.X.list[3]", v.fullyQualifiedName());
            assertEquals(runtime.stringParameterizedType(), v.parameterizedType());
        }
    }


    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import org.e2immu.annotation.method.GetSet;
            import java.util.ArrayList;
            import java.util.HashSet;
            import java.util.Set;
            import java.util.List;
            class X {
                interface R {
                    @GetSet Set<Integer> set();
                    int i();
                }
            
                void setAdd(R r) {
                    r.set().add(r.i());
                }
            }
            """;

    /*
    why do we want r.set() to leave a variable trace? because the MODIFIED_COMPONENTS_PARAMETER analysis has to
    pick up the change in one of the components of 'r'. This is nigh impossible if we do not leave some variable trace.
     */
    @DisplayName("create variables for @GetSet access")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doPrimaryType(X);

        MethodInfo setAdd = X.findUniqueMethod("setAdd", 1);
        {
            Statement s0 = setAdd.methodBody().statements().get(0);
            VariableData vdSetAdd0 = VariableDataImpl.of(s0);
            //FIXME do we expect r.i to be known?
            assertEquals("a.b.X.R.set#a.b.X.setAdd(a.b.X.R):0:r, a.b.X.setAdd(a.b.X.R):0:r",
                    vdSetAdd0.knownVariableNamesToString());
        }
    }


    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            import org.e2immu.annotation.method.GetSet;
            import java.util.ArrayList;
            import java.util.HashSet;
            import java.util.Set;
            import java.util.List;
            class X {
                record Pair<F, G>(F f, G g) { }
            
                static <F, G> F getF(Pair<F, G> pair) {
                    return pair.f;
                }
                static <F, G> F getF2(Pair<F, G> pair) {
                    return pair.f(); // 1st level
                }
            
                record R<F, G>(Pair<F, G> pair) {
                    public R {
                        assert pair != null;
                    }
                }
                static <X, Y> boolean bothNotNull(R<X, Y> r) {
                    X x = r.pair.f;
                    Y y = r.pair().g(); // 2nd level, relies on correct recursion
                    return x != null && y != null;
                }
            }
            """;

    @DisplayName("create variables for @GetSet access, recursion")
    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse(INPUT3);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doPrimaryType(X);

        MethodInfo getF = X.findUniqueMethod("getF", 1);
        {
            Statement s0 = getF.methodBody().statements().get(0);
            VariableData vd0 = VariableDataImpl.of(s0);
            assertEquals("""
                            a.b.X.Pair.f#a.b.X.getF(a.b.X.Pair<F,G>):0:pair, \
                            a.b.X.getF(a.b.X.Pair<F,G>), \
                            a.b.X.getF(a.b.X.Pair<F,G>):0:pair\
                            """,
                    vd0.knownVariableNamesToString());
        }
        MethodInfo getF2 = X.findUniqueMethod("getF2", 1);
        {
            Statement s0 = getF2.methodBody().statements().get(0);
            VariableData vd0 = VariableDataImpl.of(s0);
            assertEquals("""
                            a.b.X.Pair.f#a.b.X.getF2(a.b.X.Pair<F,G>):0:pair, \
                            a.b.X.getF2(a.b.X.Pair<F,G>), \
                            a.b.X.getF2(a.b.X.Pair<F,G>):0:pair\
                            """,
                    vd0.knownVariableNamesToString());
        }

        MethodInfo bnn = X.findUniqueMethod("bothNotNull", 1);
        {
            Statement s0 = bnn.methodBody().statements().get(0);
            VariableData vd0 = VariableDataImpl.of(s0);
            assertEquals("""
                            a.b.X.Pair.f#a.b.X.R.pair#a.b.X.bothNotNull(a.b.X.R<X,Y>):0:r, \
                            a.b.X.R.pair#a.b.X.bothNotNull(a.b.X.R<X,Y>):0:r, \
                            a.b.X.bothNotNull(a.b.X.R<X,Y>):0:r, \
                            x\
                            """,
                    vd0.knownVariableNamesToString());
        }
        {
            Statement s1 = bnn.methodBody().statements().get(1);
            VariableData vd1 = VariableDataImpl.of(s1);
            assertEquals("""
                            a.b.X.Pair.f#a.b.X.R.pair#a.b.X.bothNotNull(a.b.X.R<X,Y>):0:r, \
                            a.b.X.Pair.g#a.b.X.R.pair#a.b.X.bothNotNull(a.b.X.R<X,Y>):0:r, \
                            a.b.X.R.pair#a.b.X.bothNotNull(a.b.X.R<X,Y>):0:r, \
                            a.b.X.bothNotNull(a.b.X.R<X,Y>):0:r, \
                            x, y\
                            """,
                    vd1.knownVariableNamesToString());
        }
    }


    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            import org.e2immu.annotation.method.GetSet;
            import java.util.function.Function;
            class X {
                interface R {
                    @GetSet Function<String, Integer> function();
                    @GetSet("variables") Object variable(int i);
                }
                record RI(Function<String,Integer> function, Object[] variables) implements R {
                    Object variable(int i) { return variables[i]; }
                }
                static class Builder {
                    Function<String,Integer> function;
                    Object[] variables;
                    Builder setFunction(Function<String, Integer> f) { function = f; return this; }
                    Builder setVariable(int pos, Object value) { variables[pos]=value; return this; }
                    R build() { return new RI(function, variables); }
                }
                Function<String, Integer> method(String s) {
                    Builder b = new Builder().setFunction(String::length).setVariable(0, s);
                    R r = b.build();
                    return r.function();
                }
                // we see that this is an @Identity method!!
                Object method2(String s) {
                    Builder b = new Builder().setFunction(String::length).setVariable(0, s);
                    R r = b.build();
                    return r.variable(0);
                }
            }
            """;

    @DisplayName("array accessor, variable must exist")
    @Test
    public void test4() {
        TypeInfo X = javaInspector.parse(INPUT4);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doPrimaryType(X);

        MethodInfo method2 = X.findUniqueMethod("method2", 1);
        {
            VariableData vd2 = VariableDataImpl.of(method2.methodBody().lastStatement());
            assertEquals("""
                    a.b.X.Builder.function#scope26-21:26-33, a.b.X.Builder.variables#scope26-21:26-33, \
                    a.b.X.Builder.variables#scope26-21:26-33[0], a.b.X.R.variables#r, a.b.X.R.variables#r[0], \
                    a.b.X.method2(String), a.b.X.method2(String):0:s, b, r\
                    """, vd2.knownVariableNamesToString());
        }
    }


    @Language("java")
    private static final String INPUT5 = """
            package a.b;
            import org.e2immu.annotation.method.GetSet;
            import java.util.List;import java.util.function.Function;
            class X {
                List<Integer> intList;
            
                Integer getInt(int index) {
                    return intList.get(index);
                }
            }
            """;

    @DisplayName("list accessor")
    @Test
    public void test5() {
        TypeInfo X = javaInspector.parse(INPUT5);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doPrimaryType(X);

        MethodInfo getInt = X.findUniqueMethod("getInt", 1);
        assertEquals("a.b.X.intList", getInt.getSetField().field().fullyQualifiedName());
        {
            VariableData vdLast = VariableDataImpl.of(getInt.methodBody().lastStatement());
            assertEquals("""
                    a.b.X.getInt(int), a.b.X.getInt(int):0:index, a.b.X.intList, a.b.X.this, \
                    java.util.List._synthetic_list#a.b.X.intList, \
                    java.util.List._synthetic_list#a.b.X.intList[a.b.X.getInt(int):0:index]\
                    """, vdLast.knownVariableNamesToString());
        }
    }


    @Language("java")
    private static final String INPUT5b = """
            package a.b;
            import org.e2immu.annotation.method.GetSet;
            import java.util.ArrayList;
            import java.util.function.Function;
            class X {
                ArrayList<Integer> intList;
            
                Integer getInt(int index) {
                    return intList.get(index);
                }
            }
            """;

    @DisplayName("list accessor, arrayList")
    @Test
    public void test5b() {
        TypeInfo X = javaInspector.parse(INPUT5b);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doPrimaryType(X);

        MethodInfo getInt = X.findUniqueMethod("getInt", 1);
        assertEquals("a.b.X.intList", getInt.getSetField().field().fullyQualifiedName());
        {
            // NO synthetics for ArrayList!!!
            VariableData vdLast = VariableDataImpl.of(getInt.methodBody().lastStatement());
            assertEquals("""
                    a.b.X.getInt(int), a.b.X.getInt(int):0:index, a.b.X.intList, a.b.X.this\
                    """, vdLast.knownVariableNamesToString());
        }
    }


    @Language("java")
    private static final String INPUT6 = """
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
            }
            """;

    @DisplayName("return list.subList(0, 10).get(index)")
    @Test
    public void test6() {
        TypeInfo X = javaInspector.parse(INPUT6);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doPrimaryType(X);

        MethodInfo get = X.findUniqueMethod("get", 2);
        VariableData vdLast = VariableDataImpl.of(get.methodBody().lastStatement());
        assertEquals("""
                a.b.X.get(java.util.List<T>,int), a.b.X.get(java.util.List<T>,int):0:list, \
                a.b.X.get(java.util.List<T>,int):1:i, java.util.List._synthetic_list#scope10-16:10-34, \
                java.util.List._synthetic_list#scope10-16:10-34[a.b.X.get(java.util.List<T>,int):1:i]\
                """, vdLast.knownVariableNamesToString());
    }


    @Language("java")
    private static final String INPUT7 = """
            package a.b;
            import java.util.HashMap;import java.util.List;
            import java.util.Map;
            class X {
                static <T, S> T get(List<T> listIn, Map<T, List<S>> stringMap) {
                    Map<T, S> resultMap = new HashMap<>();
                    for(T t: listIn) {
                        List<S> list = stringMap.get(t);
                        if(!list.isEmpty()) {
                            S s = list.get(0);
                            resultMap.put(t, s);
                        }
                    }
                    if(!resultMap.isEmpty()) {
                        return resultMap.entrySet().stream().findFirst().orElseThrow().getKey();
                    }
                    return null;
                }
            }
            """;

    @DisplayName("get/set variable should disappear after merge")
    @Test
    public void test7() {
        TypeInfo X = javaInspector.parse(INPUT7);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doPrimaryType(X);

        MethodInfo get = X.findUniqueMethod("get", 2);

        Statement s1 = get.methodBody().statements().get(1);
        Statement s101 = s1.block().statements().get(1);
        Statement s10101 = s101.block().statements().get(1);

        VariableData vd10101 = VariableDataImpl.of(s10101);
        assertEquals("""
                a.b.X.get(java.util.List<T>,java.util.Map<T,java.util.List<S>>):0:listIn, \
                a.b.X.get(java.util.List<T>,java.util.Map<T,java.util.List<S>>):1:stringMap, \
                java.util.List._synthetic_list#list, java.util.List._synthetic_list#list[0], \
                list, resultMap, s, t\
                """, vd10101.knownVariableNamesToString());
        VariableInfo vi101010list = vd10101.variableInfo("list");
        assertEquals("D:1.0.0, A:[1.0.0]", vi101010list.assignments().toString());

        VariableInfo vi101010syntheticList = vd10101.variableInfo("java.util.List._synthetic_list#list");
        assertEquals("D:-, A:[]", vi101010syntheticList.assignments().toString());
        VariableInfo vi101010syntheticList0 = vd10101.variableInfo("java.util.List._synthetic_list#list[0]");
        assertEquals("D:-, A:[]", vi101010syntheticList0.assignments().toString());

        VariableData vd101 = VariableDataImpl.of(s101);
        assertEquals("""
                a.b.X.get(java.util.List<T>,java.util.Map<T,java.util.List<S>>):0:listIn, \
                a.b.X.get(java.util.List<T>,java.util.Map<T,java.util.List<S>>):1:stringMap, \
                java.util.List._synthetic_list#list, java.util.List._synthetic_list#list[0], \
                list, resultMap, t\
                """, vd101.knownVariableNamesToString());

        VariableData vd1 = VariableDataImpl.of(s1);
        assertEquals("""
                a.b.X.get(java.util.List<T>,java.util.Map<T,java.util.List<S>>):0:listIn, \
                a.b.X.get(java.util.List<T>,java.util.Map<T,java.util.List<S>>):1:stringMap, \
                resultMap, t\
                """, vd1.knownVariableNamesToString());

        VariableData vdLast = VariableDataImpl.of(get.methodBody().lastStatement());
        assertEquals("""
                        a.b.X.get(java.util.List<T>,java.util.Map<T,java.util.List<S>>), \
                        a.b.X.get(java.util.List<T>,java.util.Map<T,java.util.List<S>>):0:listIn, \
                        a.b.X.get(java.util.List<T>,java.util.Map<T,java.util.List<S>>):1:stringMap, \
                        resultMap\
                        """,
                vdLast.knownVariableNamesToString());
    }


    @Language("java")
    private static final String INPUT8 = """
            package a.b;
            import java.util.*;
            class X {
                static <T, S> T get(List<T> listA, Map<T, List<S>> stringMap) {
                    Map<T, S> resultMap = new HashMap<>();
                    List<T> listB = new ArrayList<>(listA);
                    for(T t: listB) {
                        List<S> listC = stringMap.get(t);
                        if(!listC.isEmpty()) {
                            S s = listC.get(0);
                            resultMap.put(t, s);
                        }
                    }
                    if(!resultMap.isEmpty()) {
                        return resultMap.entrySet().stream().findFirst().orElseThrow().getKey();
                    }
                    return null;
                }
            }
            """;

    @DisplayName("get/set variable should disappear after merge; list is local")
    @Test
    public void test8() {
        TypeInfo X = javaInspector.parse(INPUT8);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doPrimaryType(X);

        MethodInfo get = X.findUniqueMethod("get", 2);

        Statement s1 = get.methodBody().statements().get(2);
        Statement s101 = s1.block().statements().get(1);
        Statement s10101 = s101.block().statements().get(1);

        VariableData vd10101 = VariableDataImpl.of(s10101);
        assertEquals("""
                a.b.X.get(java.util.List<T>,java.util.Map<T,java.util.List<S>>):0:listA, \
                a.b.X.get(java.util.List<T>,java.util.Map<T,java.util.List<S>>):1:stringMap, \
                java.util.List._synthetic_list#listC, java.util.List._synthetic_list#listC[0], \
                listB, listC, resultMap, s, t\
                """, vd10101.knownVariableNamesToString());

        VariableData vd101 = VariableDataImpl.of(s101);
        assertEquals("""
                a.b.X.get(java.util.List<T>,java.util.Map<T,java.util.List<S>>):0:listA, \
                a.b.X.get(java.util.List<T>,java.util.Map<T,java.util.List<S>>):1:stringMap, \
                java.util.List._synthetic_list#listC, java.util.List._synthetic_list#listC[0], \
                listB, listC, resultMap, t\
                """, vd101.knownVariableNamesToString());

        VariableData vd1 = VariableDataImpl.of(s1);
        assertEquals("""
                a.b.X.get(java.util.List<T>,java.util.Map<T,java.util.List<S>>):0:listA, \
                a.b.X.get(java.util.List<T>,java.util.Map<T,java.util.List<S>>):1:stringMap, \
                listB, resultMap, t\
                """, vd1.knownVariableNamesToString());

        VariableData vdLast = VariableDataImpl.of(get.methodBody().lastStatement());
        assertEquals("""
                        a.b.X.get(java.util.List<T>,java.util.Map<T,java.util.List<S>>), \
                        a.b.X.get(java.util.List<T>,java.util.Map<T,java.util.List<S>>):0:listA, \
                        a.b.X.get(java.util.List<T>,java.util.Map<T,java.util.List<S>>):1:stringMap, \
                        listB, resultMap\
                        """,
                vdLast.knownVariableNamesToString());
    }

}
