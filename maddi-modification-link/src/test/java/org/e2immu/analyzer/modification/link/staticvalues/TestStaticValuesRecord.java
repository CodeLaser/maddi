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

package org.e2immu.analyzer.modification.link.staticvalues;

import org.e2immu.analyzer.modification.common.getset.ApplyGetSetTranslation;
import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.*;

public class TestStaticValuesRecord extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.Set;
            record X(Set<String> set, int n) {}
            """;

    @DisplayName("record")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);

        FieldInfo setField = X.getFieldByName("set", true);
        FieldReference setFr = runtime.newFieldReference(setField);
        FieldInfo nField = X.getFieldByName("n", true);
        FieldReference nFr = runtime.newFieldReference(nField);

        MethodInfo constructor = X.findConstructor(2);
        MethodLinkedVariables mlvConstructor = constructor.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(constructor));
        ParameterInfo setParam = constructor.parameters().getFirst();
        {
            Statement s0 = constructor.methodBody().statements().getFirst();
            VariableData vd0 = VariableDataImpl.of(s0);

            VariableInfo vi0SetField = vd0.variableInfo(setFr);
            assertEquals("this.set←0:set", vi0SetField.linkedVariables().toString());

            VariableInfo vi0SetParam = vd0.variableInfo(setParam);
            assertEquals("0:set→this.set", vi0SetParam.linkedVariables().toString());
        }
        {
            Statement s1 = constructor.methodBody().statements().get(1);
            VariableData vd1 = VariableDataImpl.of(s1);

            VariableInfo vi1SetField = vd1.variableInfo(setFr);
            assertEquals("this.set←0:set", vi1SetField.linkedVariables().toString());
            VariableInfo vi1NField = vd1.variableInfo(nFr);
            assertEquals("this.n←1:n", vi1NField.linkedVariables().toString());
        }
        assertEquals("[0:set→this.set, 1:n→this.n] --> -", mlvConstructor.toString());
        {
            MethodInfo accessorSet = X.findUniqueMethod("set", 0);
            MethodLinkedVariables mlvAccessorSet = accessorSet.analysis().getOrCreate(METHOD_LINKS,
                    () -> tlc.doMethod(accessorSet));
            assertEquals("[] --> set←this.set", mlvAccessorSet.toString());
            Value.FieldValue getSet = accessorSet.analysis().getOrDefault(PropertyImpl.GET_SET_FIELD,
                    ValueImpl.GetSetValueImpl.EMPTY);
            assertEquals(setField, getSet.field());
            VariableData vd = VariableDataImpl.of(accessorSet.methodBody().lastStatement());
            VariableInfo viField = vd.variableInfo(setField.fullyQualifiedName());
            assertEquals("-", viField.linkedVariables().toString());
        }
        {
            MethodInfo accessorN = X.findUniqueMethod("n", 0);
            MethodLinkedVariables mlvAccessorSet = accessorN.analysis().getOrCreate(METHOD_LINKS,
                    () -> tlc.doMethod(accessorN));
            assertEquals("[] --> n←this.n", mlvAccessorSet.toString());
            Value.FieldValue getSet = accessorN.analysis().getOrDefault(PropertyImpl.GET_SET_FIELD, ValueImpl.GetSetValueImpl.EMPTY);
            assertEquals(nField, getSet.field());
        }
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.Set;
            class X {
                record R(Set<String> set, int n) {}
                int method(Set<String> in) {
                    R r = new R(in, 3);
                    return r.n;
                }
                R method2(Set<String> in) {
                    R r = new R(in, 3);
                    return r;
                }
            }
            """;

    @DisplayName("values in record")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        TypeInfo R = X.findSubType("R");
        FieldInfo RsetField = R.getFieldByName("set", true);
        MethodInfo Rset = R.findUniqueMethod("set", 0);
        assertSame(RsetField, Rset.getSetField().field());
        assertFalse(R.isExtensible());

        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        {
            MethodInfo method = X.findUniqueMethod("method", 1);
            MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));
            LocalVariableCreation rLvc = (LocalVariableCreation) method.methodBody().statements().getFirst();
            LocalVariable r = rLvc.localVariable();
            VariableData vd0 = VariableDataImpl.of(rLvc);
            VariableInfo rVi0 = vd0.variableInfo(r);
            assertEquals("r.n←$_ce1,r.set←0:in", rVi0.linkedVariables().toString());
            assertEquals("[-] --> -", mlv.toString());
        }
        {
            MethodInfo method = X.findUniqueMethod("method2", 1);
            MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));
            LocalVariableCreation rLvc = (LocalVariableCreation) method.methodBody().statements().getFirst();
            LocalVariable r = rLvc.localVariable();
            VariableData vd0 = VariableDataImpl.of(rLvc);
            VariableInfo rVi0 = vd0.variableInfo(r);
            assertEquals("r.n←$_ce1,r.set←0:in", rVi0.linkedVariables().toString());
            assertEquals("[-] --> method2.set←0:in", mlv.toString());
        }
    }


    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            import java.util.Set;
            class X {
                record R(Set<String> set, int n) {}
                int method(Set<String> in, int k) {
                    R r = new R(in, k);
                    R s = r;
                    return s.n;
                }
            }
            """;

    @DisplayName("values in record, extra indirection")
    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse(INPUT3);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        MethodInfo method = X.findUniqueMethod("method", 2);
        MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));

        LocalVariableCreation rLvc = (LocalVariableCreation) method.methodBody().statements().getFirst();
        LocalVariable r = rLvc.localVariable();
        VariableData vd0 = VariableDataImpl.of(rLvc);
        VariableInfo rVi0 = vd0.variableInfo(r);
        assertEquals("r.n←1:k,r.set←0:in", rVi0.linkedVariables().toString());

        LocalVariableCreation sLvc = (LocalVariableCreation) method.methodBody().statements().get(1);
        VariableData vd1 = VariableDataImpl.of(sLvc);
        VariableInfo sVi1 = vd1.variableInfo("s");
        assertEquals("s.n←r.n,s.n←1:k,s.set←r.set,s.set←0:in,s←r", sVi1.linkedVariables().toString());

        assertEquals("[-, -] --> method←1:k", mlv.toString());
    }


    @Language("java")
    private static final String INPUT3B = """
            package a.b;
            import java.util.Set;
            class X {
                record R<T>(Set<T> set, int n) {}
                static <T> T method(Set<T> in) {
                    R<T> r = new R<>(in, 3);
                    R<T> s = r;
                    return s.n;
                }
            }
            """;

    @DisplayName("values in record, extra indirection; type parameter")
    @Test
    public void test3b() {
        TypeInfo X = javaInspector.parse(INPUT3B);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);

        MethodInfo method = X.findUniqueMethod("method", 1);
        MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));

        LocalVariableCreation rLvc = (LocalVariableCreation) method.methodBody().statements().getFirst();
        LocalVariable r = rLvc.localVariable();

        VariableData vd0 = VariableDataImpl.of(rLvc);
        VariableInfo rVi0 = vd0.variableInfo(r);
        assertEquals("r.n←$_ce1,r.set←0:in", rVi0.linkedVariables().toString());

        LocalVariableCreation sLvc = (LocalVariableCreation) method.methodBody().statements().get(1);
        VariableData vd1 = VariableDataImpl.of(sLvc);
        VariableInfo sVi1 = vd1.variableInfo("s");
        assertEquals("s.n←r.n,s.n←$_ce1,s.set←r.set,s.set←0:in,s←r", sVi1.linkedVariables().toString());

        assertEquals("[-] --> -", mlv.toString());
        // anything related to the constant 3 is lost
    }


    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            import java.util.Set;
            class X {
                record R<T>(T t) {}
                int method(Set<String> in) {
                    R<Set<String>> r = new R<>(in);
                    return r.t();
                }
            }
            """;

    @DisplayName("values in record, @Identity, accessor")
    @Test
    public void test4() {
        TypeInfo X = javaInspector.parse(INPUT4);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        LinkComputer tlc = new LinkComputerImpl(javaInspector);

        MethodInfo method = X.findUniqueMethod("method", 1);
        MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));
        LocalVariableCreation rLvc = (LocalVariableCreation) method.methodBody().statements().getFirst();
        LocalVariable r = rLvc.localVariable();

        VariableData vd0 = VariableDataImpl.of(rLvc);
        VariableInfo rVi0 = vd0.variableInfo(r);

        // IMPORTANT: R is not an abstract type, it is very concrete and can be analyzed!
        // but, because 't' is an unbound type parameter, it can gain virtual fields
        assertEquals("r.t←0:in", rVi0.linkedVariables().toString());

        assertEquals("[-] --> method←0:in", mlv.toString());
        ReturnStatement rs = (ReturnStatement) method.methodBody().statements().get(1);

        // TODO after TypeModIndyAnalyzer, we should have:
        // @Identity method, we return the first parameter
        //assertSame(TRUE, method.analysis().getOrDefault(PropertyImpl.IDENTITY_METHOD, FALSE));
        //assertSame(FALSE, method.analysis().getOrDefault(PropertyImpl.FLUENT_METHOD, FALSE));
    }


    @Language("java")
    private static final String INPUT4b = """
            package a.b;
            import java.util.Set;
            class X {
                interface R<T> { T t(); R<T> embed(T t); }
                Set<String> method(Set<String> in, R<Set<String>> rr) {
                    R<Set<String>> r = rr.embed(in);
                    return r.t();
                }
            }
            """;

    @DisplayName("values in record, embed in abstract type")
    @Test
    public void test4b() {
        TypeInfo X = javaInspector.parse(INPUT4b);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);

        TypeInfo R = X.findSubType("R");
        assertEquals("§m - T §t", vfc.compute(R).toString());

        MethodInfo method = X.findUniqueMethod("method", 2);
        MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));
        LocalVariableCreation rLvc = (LocalVariableCreation) method.methodBody().statements().getFirst();
        LocalVariable r = rLvc.localVariable();

        VariableData vd0 = VariableDataImpl.of(rLvc);
        VariableInfo rVi0 = vd0.variableInfo(r);
        assertEquals("r.§m≡1:rr.§m,r.§$s←1:rr.§$s,r.§$s~0:in.§$s", rVi0.linkedVariables().toString());

        assertEquals("""
                [0:in.§$s~1:rr.§$s,0:in.§m←1:rr.§m, 1:rr.§$s~0:in.§$s,1:rr.§m→0:in.§m] --> \
                method.§$s⊆1:rr.§$s,method.§$s∩0:in.§$s\
                """, mlv.toString());
    }


    @Language("java")
    private static final String INPUT5 = """
            package a.b;
            import java.util.Set;
            import java.util.List;
            class X {
                record R(Set<String> set, List<Integer> list, int i) {}
                static class Builder {
                    Set<String> stringSet;
                    List<Integer> intList;
                    int j;
                    Builder setStringSet(Set<String> set) { stringSet = set; return this; }
                    Builder setIntList(List<Integer>list) { intList = list; return this; }
                    Builder setJ(int k) { j = k; return this; }
                    R build() { return new R(stringSet, intList, j); }
                }
                R method(Set<String> in) {
                    Builder b = new Builder().setJ(3).setIntList(List.of(0, 1)).setStringSet(in);
                    R r = b.build();
                    return r;
                }
            }
            """;

    @DisplayName("simple builder for record")
    @Test
    public void test5() {
        TypeInfo X = javaInspector.parse(INPUT5);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        MethodInfo method = X.findUniqueMethod("method", 1);
        MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));

        LocalVariableCreation lvc0 = (LocalVariableCreation) method.methodBody().statements().getFirst();
        ApplyGetSetTranslation tm = new ApplyGetSetTranslation(runtime);
        assertEquals("""
                new Builder().j=3,new Builder().intList=List.of(0,1),new Builder().stringSet=in,new Builder()\
                """, lvc0.localVariable().assignmentExpression().translate(tm).toString());
        assertEquals("[-] --> method.set←0:in", mlv.toString());


        TypeInfo R = X.findSubType("R");
        MethodInfo constructorR = R.findConstructor(3);

        MethodLinkedVariables mlvConstructorR = constructorR.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(constructorR));
        assertEquals("[0:set→this.set, 1:list→this.list, 2:i→this.i] --> -", mlvConstructorR.toString());

        LocalVariableCreation rLvc = (LocalVariableCreation) method.methodBody().statements().get(1);
        LocalVariable r = rLvc.localVariable();
        VariableData vd1 = VariableDataImpl.of(rLvc);
        VariableInfo rVi1 = vd1.variableInfo(r);
        assertEquals("""
                        r.i←b.j,r.list←b.intList,r.list≥$_ce3,r.list≥$_ce4,r.set←b.stringSet,r.set←0:in\
                        """,
                //"Type a.b.X.R E=new Builder() this.i=3, this.list=List.of(0,1), this.set=in",
                rVi1.linkedVariables().toString());
        // we keep 1: this.set = in; ignore the constants
    }


    @Language("java")
    private static final String INPUT6 = """
            package a.b;
            import java.util.Set;
            import java.util.List;import java.util.function.Function;
            class X {
                record R(Function<String,Integer> function, Object[] variables) {}
                static class Builder {
                    Function<String,Integer> function;
                    Object[] variables;
                    Builder setFunction(Function<String, Integer> f) { function = f; return this; }
                    Builder setVariable(int pos, Object value) { variables[pos]=value; return this; }
                    R build() { return new R(function, variables); }
                }
                Function<String, Integer> method(Set<String> in) {
                    Builder b = new Builder().setFunction(String::length).setVariable(0, "a");
                    R r = b.build();
                    return r.function;
                }
                Function<String, Integer> method2(Set<String> in) {
                    Builder b = new Builder().setFunction(String::length).setVariable(0, "a");
                    R r = b.build();
                    return r.function();
                }
                Object[] method3(String in) {
                    Builder b = new Builder().setFunction(String::length).setVariable(0, in);
                    R r = b.build();
                    return r.variables();
                }
            }
            """;

    @DisplayName("more complex builder for record: indexed objects")
    @Test
    public void test6() {
        TypeInfo X = javaInspector.parse(INPUT6);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);

        TypeInfo R = X.findSubType("R");
        MethodInfo constructorR = R.findConstructor(2);
        MethodLinkedVariables mlvCR = constructorR.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(constructorR));
        assertEquals("[0:function→Λthis.function, 1:variables→this.variables] --> -",
                mlvCR.toString());

        TypeInfo builder = X.findSubType("Builder");

        MethodInfo build = builder.findUniqueMethod("build", 0);
        MethodLinkedVariables mlvBuild = build.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(build));

        assertEquals("[] --> build.function←Λthis.function,build.variables←this.variables",
                mlvBuild.toString());

        MethodInfo setVariable = builder.findUniqueMethod("setVariable", 2);
        MethodLinkedVariables mlvSetVariable = setVariable.analysis().getOrCreate(METHOD_LINKS,
                () -> tlc.doMethod(setVariable));
        assertEquals("""
                [-, 1:value→this.variables[0:pos],1:value∈this.variables] --> \
                setVariable←this,setVariable.variables[0:pos]←1:value,\
                setVariable.variables[0:pos]∈this.variables,\
                setVariable.variables∋this.variables[0:pos],\
                setVariable.variables∋1:value\
                """, mlvSetVariable.toString());

        Value.FieldValue fv = setVariable.getSetField();
        assertTrue(fv.setter());
        assertEquals(0, fv.parameterIndexOfIndex());
        assertEquals("a.b.X.Builder.variables", fv.field().toString());

        MethodInfo method3 = X.findUniqueMethod("method3", 1);
        MethodLinkedVariables mlvMethod3 = method3.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method3));
        {
            LocalVariableCreation bLvc = (LocalVariableCreation) method3.methodBody().statements().getFirst();
            LocalVariable b = bLvc.localVariable();
            VariableData vd0 = VariableDataImpl.of(bLvc);
            VariableInfo bVi0 = vd0.variableInfo(b);
            // code of ExpressionAnalyzer.methodCallStaticValue
            assertEquals("b.function←length,b.variables[0]←0:in,b.variables[0:pos]←0:in,b.variables∋0:in",
                    bVi0.linkedVariables().toString());
        }
        {
            LocalVariableCreation rLvc = (LocalVariableCreation) method3.methodBody().statements().get(1);
            LocalVariable r = rLvc.localVariable();
            VariableData vd1 = VariableDataImpl.of(rLvc);
            VariableInfo rVi1 = vd1.variableInfo(r);
            // code of ExpressionAnalyzer.checkCaseForBuilder
            assertEquals("""
                    r.function←Λb.function,r.function←length,\
                    r.variables←b.variables,r.variables∋b.variables[0],\
                    r.variables∋b.variables[0:pos],r.variables∋0:in\
                    """, rVi1.linkedVariables().toString());
        }
        assertEquals("[-] --> method3∋0:in", mlvMethod3.toString());

        MethodInfo method2 = X.findUniqueMethod("method2", 1);
        MethodLinkedVariables mlvMethod2 = method2.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method2));
        {
            Statement s2 = method2.methodBody().statements().get(2);
            VariableData v2 = VariableDataImpl.of(s2);
            VariableInfo vi2Rv = v2.variableInfo(method2.fullyQualifiedName());
            assertEquals("-", vi2Rv.linkedVariables().toString());
        }
        assertEquals("[-] --> method2←length", mlvMethod2.toString());

        MethodInfo method = X.findUniqueMethod("method", 1);
        MethodLinkedVariables mlvMethod = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));
        {
            Statement s2 = method.methodBody().statements().get(2);
            VariableData v2 = VariableDataImpl.of(s2);
            VariableInfo vi2Rv = v2.variableInfo(method.fullyQualifiedName());
            assertEquals("-", vi2Rv.linkedVariables().toString());
        }
        assertEquals("[-] --> method←length", mlvMethod.toString());
    }

    @Language("java")
    private static final String INPUT7 = """
            package a.b;
            import org.e2immu.annotation.Independent;
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
                    Builder b = new Builder().setFunction(String::length);
                    b.setVariable(0, s);
                    R r = b.build();
                    return r.function();
                }
                // we see that this is an @Identity method!!
                Object method2(String s) {
                    Builder b = new Builder().setFunction(String::length).setVariable(0, s);
                    R r = b.build();
                    Object o = r.variable(0);
                    return o;
                }
            }
            """;

    @DisplayName("interface in between")
    @Test
    public void test7() {
        TypeInfo X = javaInspector.parse(INPUT7);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        LinkComputer tlc = new LinkComputerImpl(javaInspector);

        TypeInfo R = X.findSubType("RI");
        MethodInfo RConstructor = R.findConstructor(2);
        MethodLinkedVariables mlvRi = RConstructor.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(RConstructor));
        assertEquals("[0:function→Λthis.function, 1:variables→this.variables] --> -",
                mlvRi.toString());

        TypeInfo B = X.findSubType("Builder");
        MethodInfo build = B.findUniqueMethod("build", 0);
        MethodLinkedVariables mlvBuild = build.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(build));
        assertEquals("[] --> build.function←Λthis.function,build.variables←this.variables",
                mlvBuild.toString());

        MethodInfo method = X.findUniqueMethod("method", 1);
        MethodLinkedVariables mlvMethod = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));
        {
            LocalVariableCreation bLvc = (LocalVariableCreation) method.methodBody().statements().getFirst();
            VariableData vd0 = VariableDataImpl.of(bLvc);
            VariableInfo bVi0 = vd0.variableInfo("b");
            assertEquals("b.function←length", bVi0.linkedVariables().toString());
        }
        {
            VariableData vd1 = VariableDataImpl.of(method.methodBody().statements().get(1));
            VariableInfo bVi1 = vd1.variableInfo("b");
            assertEquals("b.function←length,b.variables[0]←0:s,b.variables∋0:s",
                    bVi1.linkedVariables().toString());
        }
        {
            LocalVariableCreation rLvc = (LocalVariableCreation) method.methodBody().statements().get(2);
            VariableData vd2 = VariableDataImpl.of(rLvc);
            VariableInfo rVi2 = vd2.variableInfo("r");
            assertEquals("""
                    r.function←Λb.function,r.function←length,\
                    r.variables[0]←b.variables[0],r.variables[0]←0:s,\
                    r.variables[0]∈b.variables,r.variables←b.variables,r.variables∋b.variables[0],r.variables∋0:s\
                    """, rVi2.linkedVariables().toString());
        }
        assertEquals("[-] --> ?", mlvMethod.toString());

        MethodInfo method2 = X.findUniqueMethod("method2", 1);
        MethodInfo variable = R.findUniqueMethod("variable", 1);
        variable.analysis().set(PropertyImpl.INDEPENDENT_METHOD, ValueImpl.IndependentImpl.INDEPENDENT_HC);

        MethodLinkedVariables mlvVariable = variable.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(variable));
        // interpret the @GetSet(string)!!
        assertEquals("[-] --> variable∈this.variables,variable←this.variables[0:i]", mlvVariable.toString());

        MethodLinkedVariables mlvMethod2 = method2.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method2));
        {
            VariableData vd0 = VariableDataImpl.of(method2.methodBody().statements().get(1));
            VariableInfo vi0B = vd0.variableInfo("b");
            // FIXME the 0:pos must go, we can only keep parameters of the correct method
            assertEquals("""
                    b.variables[0]←0:s,\
                    b.variables[0]∈r.variables,\
                    b.variables[0:pos]←0:s,\
                    b.variables[0:pos]∈r.variables,\
                    b.variables→r.variables,\
                    b.variables∋0:s\
                    """, vi0B.linkedVariables().toString());
        }
        {
            Statement s1 = method2.methodBody().statements().get(1);
            VariableData v1 = VariableDataImpl.of(s1);
            VariableInfo vi2Rv = v1.variableInfo("r");
            assertEquals("""
                    r.variables←b.variables,\
                    r.variables∋b.variables[0],\
                    r.variables∋b.variables[0:pos],\
                    r.variables∋0:s\
                    """, vi2Rv.linkedVariables().toString());
            // FIXME not making the connection b.v[0] ← s, r.v ← b.v  ---> r.v[0] ← s
            //  because if we do that, it will follow from o ← r.v[0] ← s that o ← s
        }
        {
            Statement s2 = method2.methodBody().statements().get(2);
            VariableData v2 = VariableDataImpl.of(s2);
            VariableInfo vi2Rv = v2.variableInfo("o");
            assertEquals("o←r.variables[0],o∈r.variables, ...", vi2Rv.linkedVariables().toString());
        }
        assertEquals("[-] --> method2←0:s", mlvMethod2.toString());
    }

    @Language("java")
    private static final String INPUT8 = """
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

    // note: modification of of set2/set vs list in TestModificationBasics,5
    @DisplayName("pack and unpack, with local variables")
    @Test
    public void test8() {
        TypeInfo X = javaInspector.parse(INPUT8);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        MethodInfo method = X.findUniqueMethod("method", 1);
        MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));

        Statement s2 = method.methodBody().statements().get(2);
        VariableData vd2 = VariableDataImpl.of(s2);

        VariableInfo vi2r = vd2.variableInfo("r");
        assertEquals("r.l←list,r.s←set", vi2r.linkedVariables().toString());

        VariableInfo vi2Set = vd2.variableInfo("set");
        assertEquals("set→r.s", vi2Set.linkedVariables().toString());

        VariableInfo vi2List = vd2.variableInfo("list");
        assertEquals("list→r.l", vi2List.linkedVariables().toString());

        Statement s4 = method.methodBody().statements().get(4);
        VariableData vd4 = VariableDataImpl.of(s4);

        VariableInfo vi4R = vd4.variableInfo("r");
        // old version of "Util.isPartOf"
        //        r.l←list,r.s→set2,r.s←set,r.s≥0:t,r.s∩set.§ts,r.s∩set2.§ts,\
        //        r.s.§ts→set2.§ts,r.s.§ts∋0:t,r.s.§ts~set.§ts,r≈set\
        assertEquals("r.l←list,r.s.§ts→set2.§ts,r.s.§ts∋0:t,r.s.§ts~set.§ts,r.s→set2,r.s←set,r≈set",
                vi4R.linkedVariables().toString());
        VariableInfo vi4Set = vd4.variableInfo("set");
        // should never link to 'list'!!
        assertEquals("set.§ts→set2.§ts,set.§ts∋0:t,set.§ts~r.s.§ts,set→r.s,set→set2",
                vi4Set.linkedVariables().toString());

        VariableInfo vi4List = vd4.variableInfo("list");
        assertEquals("list→r.l", vi4List.linkedVariables().toString());

        assertEquals("[-] --> -", mlv.toString());
    }

    @Language("java")
    private static final String INPUT9 = """
            package a.b;
            import java.util.ArrayList;
            import java.util.HashSet;
            import java.util.List;
            import java.util.Set;
            class X {
                record R<T>(Set<T> s, List<T> l) {}
                static <T> void method(Set<T> set, List<T> list, T t) {
                    R<T> r = new R<>(set, list);
                    Set<T> set2 = r.s;
                    set2.add(t); // assert that set has been modified, but not list
                }
            }
            """;

    @DisplayName("pack and unpack, with parameters")
    @Test
    public void test9() {
        TypeInfo X = javaInspector.parse(INPUT9);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        MethodInfo method = X.findUniqueMethod("method", 3);
        MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));

        ParameterInfo set = method.parameters().getFirst();
        ParameterInfo list = method.parameters().get(1);
        {
            Statement s0 = method.methodBody().statements().getFirst();
            VariableData vd0 = VariableDataImpl.of(s0);

            VariableInfo vi2r = vd0.variableInfo("r");
            assertEquals("r.l←1:list,r.s←0:set", vi2r.linkedVariables().toString());

            VariableInfo vi2Set = vd0.variableInfo(set);
            assertEquals("0:set→r.s", vi2Set.linkedVariables().toString());
            assertFalse(vi2Set.isModified());

            VariableInfo vi2List = vd0.variableInfo(list);
            assertEquals("1:list→r.l", vi2List.linkedVariables().toString());
            assertFalse(vi2List.isModified());
        }
        {
            Statement s2 = method.methodBody().statements().get(2);
            VariableData vd2 = VariableDataImpl.of(s2);

            VariableInfo vi4R = vd2.variableInfo("r");
            assertEquals("r.l←1:list,r.s.§ts→set2.§ts,r.s.§ts∋2:t,r.s.§ts~0:set.§ts,r.s→set2,r.s←0:set,r≈0:set",
                    vi4R.linkedVariables().toString());

            VariableInfo vi4Set = vd2.variableInfo(set);
            assertEquals("0:set.§ts→set2.§ts,0:set.§ts∋2:t,0:set.§ts~r.s.§ts,0:set→r.s,0:set→set2",
                    vi4Set.linkedVariables().toString());
            assertTrue(vi4Set.isModified());

            VariableInfo vi4List = vd2.variableInfo(list);
            assertEquals("1:list→r.l", vi4List.linkedVariables().toString());
            assertFalse(vi4List.isModified());
        }
        assertEquals("[0:set.§ts∋2:t, -, 2:t∈0:set.§ts] --> -", mlv.toString());
    }
}
