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

package org.e2immu.analyzer.modification.linkedvariables.staticvalues;

import org.e2immu.analyzer.modification.linkedvariables.CommonTest;
import org.e2immu.analyzer.modification.linkedvariables.lv.StaticValuesImpl;
import org.e2immu.analyzer.modification.common.getset.ApplyGetSetTranslation;
import org.e2immu.analyzer.modification.prepwork.variable.StaticValues;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.expression.Assignment;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.Variable;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestStaticValuesHelper extends CommonTest {


    @Language("java")
    private static final String INPUT_1 = """
            package a.b;
            class X {
                interface I { }
                static class R {
                    I i;
                }
                record Wrapper(R r, R[] rs) {}
                void method(Wrapper w, I j) {
                   w.r().i = j;
                   w.rs[3].i = j;
                }
            }
            """;

    @Test
    public void test1a() {
        TypeInfo X = javaInspector.parse(INPUT_1);
        prepWork(X);

        MethodInfo method = X.findUniqueMethod("method", 2);

        Statement s0 = method.methodBody().statements().get(0);
        Assignment a = (Assignment) s0.expression().translate(new ApplyGetSetTranslation(runtime));
        assertEquals("w.r.i", a.target().toString());
        StaticValues sv = new StaticValuesImpl(a.value().parameterizedType(), a.value(), false, Map.of());
        assertEquals("Type a.b.X.I E=j", sv.toString());
        FieldReference fr = (FieldReference) a.variableTarget();
        StaticValuesHelper svh = new StaticValuesHelper(runtime);
        Map<Variable, List<StaticValues>> append = new HashMap<>();
        List<StaticValues> list = new ArrayList<>();
        list.add(sv);
        append.put(fr, list);
        svh.recursivelyAdd1(append, fr, a.source(), list, VariableDataImpl.of(s0), "0");
        assertEquals("""
                a.b.X.method(a.b.X.Wrapper,a.b.X.I):0:w=[this.r.i=j], \
                w.r.i=[Type a.b.X.I E=j], \
                w.r=[this.i=j]\
                """, append.entrySet().stream().map(Object::toString).sorted().collect(Collectors.joining(", ")));
    }

    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT_1);
        prepWork(X);

        MethodInfo method = X.findUniqueMethod("method", 2);

        Statement s1 = method.methodBody().statements().get(1);
        Assignment a = (Assignment) s1.expression().translate(new ApplyGetSetTranslation(runtime));
        assertEquals("w.rs[3].i", a.target().toString());
        StaticValues sv = new StaticValuesImpl(a.value().parameterizedType(), a.value(), false, Map.of());
        assertEquals("Type a.b.X.I E=j", sv.toString());
        FieldReference fr = (FieldReference) a.variableTarget();
        StaticValuesHelper svh = new StaticValuesHelper(runtime);
        Map<Variable, List<StaticValues>> append = new HashMap<>();
        List<StaticValues> list = new ArrayList<>();
        list.add(sv);
        append.put(fr, list);
        svh.recursivelyAdd1(append, fr, a.source(), list, VariableDataImpl.of(s1), "0");
        assertEquals("w.rs[3].i=[Type a.b.X.I E=j], w.rs[3]=[this.i=j]",
                append.entrySet().stream().map(Object::toString).sorted().collect(Collectors.joining(", ")));
    }


    @Language("java")
    private static final String INPUT_2 = """
            package a.b;
            import java.util.Set;
            class X {
                record R(int i, int j) {}
                record S(R r, int k) {}
            
                int method(S s) {
                    s.r().i = 3;
                    s.k = s.r.j;
                    return s.r().i+s.r.j()+s.k;
                }
            }
            """;

    @Test
    public void test2a() {
        TypeInfo X = javaInspector.parse(INPUT_2);
        prepWork(X);

        MethodInfo method = X.findUniqueMethod("method", 1);

        Statement s0 = method.methodBody().statements().get(0);
        Assignment a = (Assignment) s0.expression().translate(new ApplyGetSetTranslation(runtime));
        assertEquals("s.r.i", a.target().toString());
        StaticValues sv = new StaticValuesImpl(a.value().parameterizedType(), a.value(), false, Map.of());
        assertEquals("Type int E=3", sv.toString());
        FieldReference fr = (FieldReference) a.variableTarget();
        StaticValuesHelper svh = new StaticValuesHelper(runtime);
        Map<Variable, List<StaticValues>> append = new HashMap<>();
        List<StaticValues> list = new ArrayList<>();
        list.add(sv);
        append.put(fr, list);
        svh.recursivelyAdd1(append, fr, a.source(), list, VariableDataImpl.of(s0), "0");
        assertEquals("""
                a.b.X.method(a.b.X.S):0:s=[this.r.i=3], s.r.i=[Type int E=3], s.r=[this.i=3]\
                """, append.entrySet().stream().map(Object::toString).sorted().collect(Collectors.joining(", ")));
    }

    @Test
    public void test2b() {
        TypeInfo X = javaInspector.parse(INPUT_2);
        prepWork(X);

        MethodInfo method = X.findUniqueMethod("method", 1);

        Statement s1 = method.methodBody().statements().get(1);
        Assignment a = (Assignment) s1.expression().translate(new ApplyGetSetTranslation(runtime));
        assertEquals("s.k", a.target().toString());
        StaticValues sv = new StaticValuesImpl(a.value().parameterizedType(), a.value(), false, Map.of());
        assertEquals("Type int E=s.r.j", sv.toString());
        FieldReference fr = (FieldReference) a.variableTarget();
        StaticValuesHelper svh = new StaticValuesHelper(runtime);
        Map<Variable, List<StaticValues>> append = new HashMap<>();
        List<StaticValues> list = new ArrayList<>();
        list.add(sv);
        append.put(fr, list);
        svh.recursivelyAdd1(append, fr, a.source(), list, VariableDataImpl.of(s1), "1");
        assertEquals("""
                a.b.X.method(a.b.X.S):0:s=[this.k=s.r.j], s.k=[Type int E=s.r.j]\
                """, append.entrySet().stream().map(Object::toString).sorted().collect(Collectors.joining(", ")));
    }

    @Language("java")
    private static final String INPUT_3 = """
            package a.b;
            import java.util.Set;
            class X {
                int j;
                int k;
                void setJ(int jp) {
                    j=jp;
                }
                void setJK(int jp, int kp) {
                    j=jp;
                    k=kp;
                }
            }
            """;

    @Test
    public void test3a() {
        TypeInfo X = javaInspector.parse(INPUT_3);
        prepWork(X);

        MethodInfo method = X.findUniqueMethod("setJ", 1);
        Statement s0 = method.methodBody().statements().get(0);
        Assignment a = (Assignment) s0.expression().translate(new ApplyGetSetTranslation(runtime));
        assertEquals("this.j", a.target().toString());
        StaticValues sv = new StaticValuesImpl(a.value().parameterizedType(), a.value(), false, Map.of());
        assertEquals("Type int E=jp", sv.toString());
        FieldReference fr = (FieldReference) a.variableTarget();
        StaticValuesHelper svh = new StaticValuesHelper(runtime);
        Map<Variable, List<StaticValues>> append = new HashMap<>();
        List<StaticValues> list = new ArrayList<>();
        list.add(sv);
        append.put(fr, list);
        svh.recursivelyAdd1(append, fr, a.source(), list, VariableDataImpl.of(s0), "0");
        assertEquals("""
                this.j=[Type int E=jp], this=[this.j=jp]\
                """, append.entrySet().stream().map(Object::toString).sorted().collect(Collectors.joining(", ")));
    }


    @Test
    public void test3b() {
        TypeInfo X = javaInspector.parse(INPUT_3);
        prepWork(X);

        MethodInfo method = X.findUniqueMethod("setJK", 2);
        Statement s1 = method.methodBody().statements().get(1);
        Assignment a = (Assignment) s1.expression().translate(new ApplyGetSetTranslation(runtime));
        assertEquals("this.k", a.target().toString());
        StaticValues sv = new StaticValuesImpl(a.value().parameterizedType(), a.value(), false, Map.of());
        assertEquals("Type int E=kp", sv.toString());
        FieldReference fr = (FieldReference) a.variableTarget();
        StaticValuesHelper svh = new StaticValuesHelper(runtime);
        Map<Variable, List<StaticValues>> append = new HashMap<>();
        List<StaticValues> list = new ArrayList<>();
        list.add(sv);
        append.put(fr, list);
        svh.recursivelyAdd1(append, fr, a.source(), list, VariableDataImpl.of(s1), "0");
        assertEquals("""
                this.k=[Type int E=kp], this=[this.k=kp]\
                """, append.entrySet().stream().map(Object::toString).sorted().collect(Collectors.joining(", ")));
    }


    @Language("java")
    private static final String INPUT_4 = """
            package a.b;
            class X {
                void set(int[] v, int k) {
                    v[0] = k;
                }
            }
            """;

    @Test
    public void test4() {
        TypeInfo X = javaInspector.parse(INPUT_4);
        prepWork(X);

        MethodInfo method = X.findUniqueMethod("set", 2);
        Statement s0 = method.methodBody().statements().get(0);
        Assignment a = (Assignment) s0.expression().translate(new ApplyGetSetTranslation(runtime));
        assertEquals("v[0]", a.target().toString());
        StaticValues sv = new StaticValuesImpl(a.value().parameterizedType(), a.value(), false, Map.of());
        assertEquals("Type int E=k", sv.toString());
        DependentVariable dv = (DependentVariable) a.variableTarget();
        StaticValuesHelper svh = new StaticValuesHelper(runtime);
        Map<Variable, List<StaticValues>> append = new HashMap<>();
        List<StaticValues> list = new ArrayList<>();
        list.add(sv);
        append.put(dv, list);
        svh.add(a.source(),  VariableDataImpl.of(s0), "0", dv, list, append);
        assertEquals("""
                a.b.X.set(int[],int):0:v=[this[0]=k], v[0]=[Type int E=k]\
                """, append.entrySet().stream().map(Object::toString).sorted().collect(Collectors.joining(", ")));
    }


}
