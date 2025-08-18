package org.e2immu.analyzer.modification.linkedvariables.staticvalues;

import org.e2immu.analyzer.modification.common.getset.ApplyGetSetTranslation;
import org.e2immu.analyzer.modification.linkedvariables.CommonTest;
import org.e2immu.analyzer.modification.linkedvariables.lv.StaticValuesImpl;
import org.e2immu.analyzer.modification.prepwork.variable.StaticValues;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.e2immu.analyzer.modification.linkedvariables.lv.StaticValuesImpl.*;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.IMMUTABLE_TYPE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.FALSE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.TRUE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.ImmutableImpl.FINAL_FIELDS;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.ImmutableImpl.MUTABLE;
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
        List<Info> ao = prepWork(X);
        analyzer.go(ao);
        FieldInfo setField = X.getFieldByName("set", true);
        FieldReference setFr = runtime.newFieldReference(setField);
        FieldInfo nField = X.getFieldByName("n", true);
        FieldReference nFr = runtime.newFieldReference(nField);

        MethodInfo constructor = X.findConstructor(2);
        ParameterInfo setParam = constructor.parameters().getFirst();
        {
            Statement s0 = constructor.methodBody().statements().getFirst();
            VariableData vd0 = VariableDataImpl.of(s0);

            VariableInfo vi0SetField = vd0.variableInfo(setFr);
            assertEquals("-1-:set", vi0SetField.linkedVariables().toString());
            assertEquals("E=set", vi0SetField.staticValues().toString());

            VariableInfo vi0SetParam = vd0.variableInfo(setParam);
            assertEquals("-1-:set", vi0SetParam.linkedVariables().toString());
            assertNull(vi0SetParam.staticValues());
        }
        {
            Statement s1 = constructor.methodBody().statements().get(1);
            VariableData vd1 = VariableDataImpl.of(s1);

            VariableInfo vi1SetField = vd1.variableInfo(setFr);
            assertEquals("-1-:set", vi1SetField.linkedVariables().toString());
            assertEquals("E=set", vi1SetField.staticValues().toString());
            VariableInfo vi1NField = vd1.variableInfo(nFr);
            assertEquals("-1-:n", vi1NField.linkedVariables().toString());
            assertEquals("E=n", vi1NField.staticValues().toString());
        }
        {
            MethodInfo accessorSet = X.findUniqueMethod("set", 0);
            StaticValues svAccessorSet = accessorSet.analysis().getOrNull(STATIC_VALUES_METHOD, StaticValuesImpl.class);
            assertEquals("E=this.set", svAccessorSet.toString());
            FieldValue getSet = accessorSet.analysis().getOrDefault(PropertyImpl.GET_SET_FIELD, ValueImpl.GetSetValueImpl.EMPTY);
            assertEquals(setField, getSet.field());
            VariableData vd = VariableDataImpl.of(accessorSet.methodBody().lastStatement());
            VariableInfo viField = vd.variableInfo(setField.fullyQualifiedName());
            assertNull(viField.staticValues());
        }
        {
            MethodInfo constructorSet = X.findConstructor(2);
            VariableData vd = VariableDataImpl.of(constructorSet.methodBody().lastStatement());
            VariableInfo viField = vd.variableInfo(setField.fullyQualifiedName());
            assertEquals("E=set", viField.staticValues().toString());
        }
        {
            StaticValues svSetField = setField.analysis().getOrNull(STATIC_VALUES_FIELD, StaticValuesImpl.class);
            assertEquals("E=set", svSetField.toString());
            assertSame(setParam, ((VariableExpression) svSetField.expression()).variable());
        }
        {
            StaticValues svSetParam = setParam.analysis().getOrNull(STATIC_VALUES_PARAMETER, StaticValuesImpl.class);
            assertEquals("E=this.set", svSetParam.toString());
            assertSame(setField, ((FieldReference) ((VariableExpression) svSetParam.expression()).variable()).fieldInfo());
        }
        {
            MethodInfo accessorN = X.findUniqueMethod("n", 0);
            StaticValues svAccessorN = accessorN.analysis().getOrNull(STATIC_VALUES_METHOD, StaticValuesImpl.class);
            assertEquals("E=this.n", svAccessorN.toString());
            FieldValue getSet = accessorN.analysis().getOrDefault(PropertyImpl.GET_SET_FIELD, ValueImpl.GetSetValueImpl.EMPTY);
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
            }
            """;

    @DisplayName("values in record")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        List<Info> ao = prepWork(X);

        TypeInfo R = X.findSubType("R");
        FieldInfo RsetField = R.getFieldByName("set", true);
        MethodInfo Rset = R.findUniqueMethod("set", 0);
        assertSame(RsetField, Rset.getSetField().field());
        assertFalse(R.isExtensible());

        assertEquals("""
                [a.b.X.<init>(), a.b.X.R.<init>(java.util.Set<String>,int), a.b.X.R.n(), a.b.X.R.set(), a.b.X.R.n, a.b.X.R.set, a.b.X.R, a.b.X.method(java.util.Set<String>), a.b.X]\
                """, ao.toString());

        analyzer.go(ao);

        MethodInfo method = X.findUniqueMethod("method", 1);
        LocalVariableCreation rLvc = (LocalVariableCreation) method.methodBody().statements().getFirst();
        LocalVariable r = rLvc.localVariable();

        VariableData vd0 = VariableDataImpl.of(rLvc);
        VariableInfo rVi0 = vd0.variableInfo(r);
        assertEquals("Type a.b.X.R E=new R(in,3) this.n=3, this.set=in", rVi0.staticValues().toString());

        ReturnStatement rs = (ReturnStatement) method.methodBody().statements().get(1);
        {
            VariableData vd1 = VariableDataImpl.of(rs);
            VariableInfo rVi1 = vd1.variableInfo(r);
            assertEquals(rVi0.staticValues(), rVi1.staticValues());

            VariableInfo rvVi1 = vd1.variableInfo(method.fullyQualifiedName());
            assertEquals("-1-:n", rvVi1.linkedVariables().toString());
            assertEquals("E=3", rvVi1.staticValues().toString());
        }
        StaticValues methodSv = method.analysis().getOrNull(STATIC_VALUES_METHOD, StaticValuesImpl.class);
        assertEquals("E=3", methodSv.toString());
    }


    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            import java.util.Set;
            class X {
                record R(Set<String> set, int n) {}
                int method(Set<String> in) {
                    R r = new R(in, 3);
                    R s = r;
                    return s.n;
                }
            }
            """;

    @DisplayName("values in record, extra indirection")
    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse(INPUT3);
        List<Info> ao = prepWork(X);
        analyzer.go(ao);
        MethodInfo method = X.findUniqueMethod("method", 1);

        LocalVariableCreation rLvc = (LocalVariableCreation) method.methodBody().statements().getFirst();
        LocalVariable r = rLvc.localVariable();

        VariableData vd0 = VariableDataImpl.of(rLvc);
        VariableInfo rVi0 = vd0.variableInfo(r);
        assertEquals("0M-2-*M|0-*:in", rVi0.linkedVariables().toString());
        assertEquals("Type a.b.X.R E=new R(in,3) this.n=3, this.set=in", rVi0.staticValues().toString());

        {
            LocalVariableCreation sLvc = (LocalVariableCreation) method.methodBody().statements().get(1);
            VariableData vd1 = VariableDataImpl.of(sLvc);
            VariableInfo sVi1 = vd1.variableInfo("s");
            assertEquals("0M-2-*M|0-*:in, -1-:r", sVi1.linkedVariables().toString());
            assertEquals(rVi0.staticValues(), sVi1.staticValues());
        }

        ReturnStatement rs = (ReturnStatement) method.methodBody().statements().get(2);
        {
            VariableData vd2 = VariableDataImpl.of(rs);
            VariableInfo rVi2 = vd2.variableInfo(r);
            assertEquals(rVi0.staticValues(), rVi2.staticValues());

            VariableInfo rvVi2 = vd2.variableInfo(method.fullyQualifiedName());
            assertEquals("-1-:n", rvVi2.linkedVariables().toString());
            assertEquals("E=3", rvVi2.staticValues().toString());
        }
        StaticValues methodSv = method.analysis().getOrNull(STATIC_VALUES_METHOD, StaticValuesImpl.class);
        assertEquals("E=3", methodSv.toString());
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
        List<Info> ao = prepWork(X);
        analyzer.go(ao);
        MethodInfo method = X.findUniqueMethod("method", 1);

        LocalVariableCreation rLvc = (LocalVariableCreation) method.methodBody().statements().getFirst();
        LocalVariable r = rLvc.localVariable();

        VariableData vd0 = VariableDataImpl.of(rLvc);
        VariableInfo rVi0 = vd0.variableInfo(r);
        assertEquals("0,1M-2-0,*M|0-*:in", rVi0.linkedVariables().toString());
        assertEquals("Type a.b.X.R<T> E=new R<>(in,3) this.n=3, this.set=in", rVi0.staticValues().toString());

        {
            LocalVariableCreation sLvc = (LocalVariableCreation) method.methodBody().statements().get(1);
            VariableData vd1 = VariableDataImpl.of(sLvc);
            VariableInfo sVi1 = vd1.variableInfo("s");
            assertEquals("0,1M-2-0,*M|0-*:in, -1-:r", sVi1.linkedVariables().toString());
            assertEquals(rVi0.staticValues(), sVi1.staticValues());
        }

        ReturnStatement rs = (ReturnStatement) method.methodBody().statements().get(2);
        {
            VariableData vd2 = VariableDataImpl.of(rs);
            VariableInfo rVi2 = vd2.variableInfo(r);
            assertEquals(rVi0.staticValues(), rVi2.staticValues());

            VariableInfo rvVi2 = vd2.variableInfo(method.fullyQualifiedName());
            assertEquals("-1-:n", rvVi2.linkedVariables().toString());
            assertEquals("E=3", rvVi2.staticValues().toString());
        }
        StaticValues methodSv = method.analysis().getOrNull(STATIC_VALUES_METHOD, StaticValuesImpl.class);
        assertEquals("E=3", methodSv.toString());
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
        List<Info> ao = prepWork(X);
        analyzer.go(ao);
        MethodInfo method = X.findUniqueMethod("method", 1);
        LocalVariableCreation rLvc = (LocalVariableCreation) method.methodBody().statements().getFirst();
        LocalVariable r = rLvc.localVariable();

        VariableData vd0 = VariableDataImpl.of(rLvc);
        VariableInfo rVi0 = vd0.variableInfo(r);
        assertEquals("Type a.b.X.R<java.util.Set<String>> E=new R<>(in) this.t=in", rVi0.staticValues().toString());
        assertEquals("0.0-4-0:in", rVi0.linkedVariables().toString());

        ReturnStatement rs = (ReturnStatement) method.methodBody().statements().get(1);

        VariableData vd1 = VariableDataImpl.of(rs);
        VariableInfo rVi1 = vd1.variableInfo(r);
        assertEquals(rVi0.staticValues(), rVi1.staticValues());

        VariableInfo rvVi1 = vd1.variableInfo(method.fullyQualifiedName());
        // 4 and M: we have type T, immutable HC, but a concrete choice Set, Mutable
        assertEquals("*M-2-0M|*-0:r, -1-:t", rvVi1.linkedVariables().toString());
        // we don't want E=r.t here, that one can be substituted again because r.t=in
        assertEquals("E=in", rvVi1.staticValues().toString());

        StaticValues methodSv = method.analysis().getOrNull(STATIC_VALUES_METHOD, StaticValuesImpl.class);
        assertEquals("E=in", methodSv.toString());

        // @Identity method, we return the first parameter
        assertSame(TRUE, method.analysis().getOrDefault(PropertyImpl.IDENTITY_METHOD, FALSE));
        assertSame(FALSE, method.analysis().getOrDefault(PropertyImpl.FLUENT_METHOD, FALSE));
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
        List<Info> ao = prepWork(X);

        MethodInfo method = X.findUniqueMethod("method", 1);
        LocalVariableCreation lvc0 = (LocalVariableCreation) method.methodBody().statements().getFirst();
        ApplyGetSetTranslation tm = new ApplyGetSetTranslation(runtime);
        assertEquals("""
                new Builder().j=3,new Builder().intList=List.of(0,1),new Builder().stringSet=in,new Builder()\
                """, lvc0.localVariable().assignmentExpression().translate(tm).toString());

        analyzer.go(ao);

        TypeInfo R = X.findSubType("R");
        MethodInfo constructorR = R.findConstructor(3);
        ParameterInfo p0ConstructorR = constructorR.parameters().getFirst();
        StaticValues svP0 = p0ConstructorR.analysis().getOrDefault(STATIC_VALUES_PARAMETER, NONE);
        assertEquals("E=this.set", svP0.toString());

        {
            LocalVariableCreation rLvc = (LocalVariableCreation) method.methodBody().statements().get(1);
            LocalVariable r = rLvc.localVariable();
            VariableData vd1 = VariableDataImpl.of(rLvc);
            VariableInfo rVi1 = vd1.variableInfo(r);
            assertEquals("Type a.b.X.R E=new Builder() this.i=3, this.list=List.of(0,1), this.set=in", rVi1.staticValues().toString());
        }
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
            }
            """;

    @DisplayName("more complex builder for record: indexed objects")
    @Test
    public void test6() {
        TypeInfo X = javaInspector.parse(INPUT6);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);
        TypeInfo R = X.findSubType("R");
        TypeInfo builder = X.findSubType("Builder");
        MethodInfo build = builder.findUniqueMethod("build", 0);
        StaticValues sv = build.analysis().getOrDefault(STATIC_VALUES_METHOD, NONE);
        assertEquals("Type a.b.X.R E=new R(this.function,this.variables) this.function=this.function, this.variables=this.variables",
                sv.toString());
        Map.Entry<Variable, Expression> e0 = sv.values().entrySet().stream().findFirst().orElseThrow();

        // you can't see it, but the types are correct
        assertSame(R, ((FieldReference) e0.getKey()).fieldInfo().owner());
        assertSame(builder, ((FieldReference) ((VariableExpression) e0.getValue()).variable()).fieldInfo().owner());

        MethodInfo setVariable = builder.findUniqueMethod("setVariable", 2);
        Value.FieldValue fv = setVariable.getSetField();
        assertTrue(fv.setter());
        assertEquals(0, fv.parameterIndexOfIndex());
        assertEquals("a.b.X.Builder.variables", fv.field().toString());

        MethodInfo method = X.findUniqueMethod("method", 1);
        {
            LocalVariableCreation bLvc = (LocalVariableCreation) method.methodBody().statements().getFirst();
            LocalVariable b = bLvc.localVariable();
            VariableData vd0 = VariableDataImpl.of(bLvc);
            VariableInfo bVi0 = vd0.variableInfo(b);
            // code of ExpressionAnalyzer.methodCallStaticValue
            assertEquals("Type a.b.X.Builder E=new Builder() this.function=String::length, variables[0]=stringConstant@14-78:14-80",
                    bVi0.staticValues().toString());
        }
        {
            LocalVariableCreation rLvc = (LocalVariableCreation) method.methodBody().statements().get(1);
            LocalVariable r = rLvc.localVariable();
            VariableData vd1 = VariableDataImpl.of(rLvc);
            VariableInfo rVi1 = vd1.variableInfo(r);
            // code of ExpressionAnalyzer.checkCaseForBuilder
            assertEquals("Type a.b.X.R E=new Builder() this.function=String::length, variables[0]=stringConstant@14-78:14-80",
                    rVi1.staticValues().toString());
        }
        {
            Statement s2 = method.methodBody().statements().get(2);
            VariableData v2 = VariableDataImpl.of(s2);
            VariableInfo vi2Rv = v2.variableInfo(method.fullyQualifiedName());
            assertEquals("E=String::length", vi2Rv.staticValues().toString());
        }
        MethodInfo method2 = X.findUniqueMethod("method2", 1);
        {
            Statement s2 = method2.methodBody().statements().get(2);
            VariableData v2 = VariableDataImpl.of(s2);
            VariableInfo vi2Rv = v2.variableInfo(method2.fullyQualifiedName());
            assertEquals("E=String::length", vi2Rv.staticValues().toString());
        }
    }


    @Language("java")
    private static final String INPUT7 = """
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

    @DisplayName("interface in between")
    @Test
    public void test7() {
        TypeInfo X = javaInspector.parse(INPUT7);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        MethodInfo method = X.findUniqueMethod("method", 1);
        {
            LocalVariableCreation rLvc = (LocalVariableCreation) method.methodBody().statements().get(1);
            LocalVariable r = rLvc.localVariable();
            VariableData vd1 = VariableDataImpl.of(rLvc);
            VariableInfo rVi1 = vd1.variableInfo(r);
            assertEquals("Type a.b.X.RI E=new Builder() this.function=String::length, variables[0]=s",
                    rVi1.staticValues().toString());
        }
        {
            Statement s2 = method.methodBody().statements().get(2);
            VariableData v2 = VariableDataImpl.of(s2);
            VariableInfo vi2Rv = v2.variableInfo(method.fullyQualifiedName());
            assertEquals("E=String::length", vi2Rv.staticValues().toString());
        }
        MethodInfo method2 = X.findUniqueMethod("method2", 1);
        {
            Statement s2 = method2.methodBody().statements().get(2);
            VariableData v2 = VariableDataImpl.of(s2);
            VariableInfo vi2Rv = v2.variableInfo(method2.fullyQualifiedName());
            assertEquals("E=s", vi2Rv.staticValues().toString());
            assertTrue(method2.isIdentity());
        }
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

    @DisplayName("pack and unpack, with local variables")
    @Test
    public void test8() {
        TypeInfo X = javaInspector.parse(INPUT8);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        MethodInfo method = X.findUniqueMethod("method", 1);
        {
            Statement s2 = method.methodBody().statements().get(2);
            VariableData vd2 = VariableDataImpl.of(s2);

            VariableInfo vi2r = vd2.variableInfo("r");
            assertEquals("Type a.b.X.R<T> E=new R<>(set,list) this.l=list, this.s=set",
                    vi2r.staticValues().toString());
            assertEquals("0,2M-2-0,*M|1-*:list, 0,1M-2-0,*M|0-*:set", vi2r.linkedVariables().toString());

            VariableInfo vi2Set = vd2.variableInfo("set");
            assertEquals("*M,0-2-1M,0|*-0:r", vi2Set.linkedVariables().toString());
            assertFalse(vi2Set.isModified());

            VariableInfo vi2List = vd2.variableInfo("list");
            assertEquals("*M,0-2-2M,0|*-1:r", vi2List.linkedVariables().toString());
            assertFalse(vi2List.isModified());
        }
        {
            Statement s4 = method.methodBody().statements().get(4);
            VariableData vd4 = VariableDataImpl.of(s4);

            VariableInfo vi4R = vd4.variableInfo("r");
            assertEquals("Type a.b.X.R<T> E=new R<>(set,list) this.l=list, this.s=set",
                    vi4R.staticValues().toString());
            assertEquals("0,2M-2-0,*M|1-*:list, 1M-2-*M|0-*:s, 0,1M-2-0,*M|0-*:set, 1M-2-*M|0-*:set2, 1M-4-*M:t",
                    vi4R.linkedVariables().toString());

            VariableInfo vi4Set = vd4.variableInfo("set");
            // should never link to 'list'!!
            assertEquals("*M,0-2-1M,0|*-0:r, -2-:s, -2-:set2, 0-4-*:t", vi4Set.linkedVariables().toString());

            assertTrue(vi4Set.isModified());

            VariableInfo vi4List = vd4.variableInfo("list");
            assertEquals("*M,0-2-2M,0|*-1:r", vi4List.linkedVariables().toString());
            assertFalse(vi4List.isModified());
        }
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
        internTest9(X);
        TypeInfo R = X.findSubType("R");
        assertSame(FINAL_FIELDS, R.analysis().getOrDefault(IMMUTABLE_TYPE, MUTABLE));
    }

    private void internTest9(TypeInfo X) {
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        MethodInfo method = X.findUniqueMethod("method", 3);
        ParameterInfo set = method.parameters().getFirst();
        ParameterInfo list = method.parameters().get(1);
        {
            Statement s0 = method.methodBody().statements().getFirst();
            VariableData vd0 = VariableDataImpl.of(s0);

            VariableInfo vi2r = vd0.variableInfo("r");
            assertEquals("Type a.b.X.R<T> E=new R<>(set,list) this.l=list, this.s=set",
                    vi2r.staticValues().toString());
            assertEquals("0,2M-2-0,*M|1-*:list, 0,1M-2-0,*M|0-*:set", vi2r.linkedVariables().toString());

            VariableInfo vi2Set = vd0.variableInfo(set);
            assertEquals("*M,0-2-1M,0|*-0:r", vi2Set.linkedVariables().toString());
            assertFalse(vi2Set.isModified());

            VariableInfo vi2List = vd0.variableInfo(list);
            assertEquals("*M,0-2-2M,0|*-1:r", vi2List.linkedVariables().toString());
            assertFalse(vi2List.isModified());
        }
        {
            Statement s2 = method.methodBody().statements().get(2);
            VariableData vd2 = VariableDataImpl.of(s2);

            VariableInfo vi4R = vd2.variableInfo("r");
            assertEquals("Type a.b.X.R<T> E=new R<>(set,list) this.l=list, this.s=set",
                    vi4R.staticValues().toString());
            assertEquals("0,2M-2-0,*M|1-*:list, 1M-2-*M|0-*:s, 0,1M-2-0,*M|0-*:set, 1M-2-*M|0-*:set2, 1M-4-*M:t",
                    vi4R.linkedVariables().toString());

            VariableInfo vi4Set = vd2.variableInfo(set);
            assertEquals("*M,0-2-1M,0|*-0:r, -2-:s, -2-:set2, 0-4-*:t", vi4Set.linkedVariables().toString());
            assertTrue(vi4Set.isModified());

            VariableInfo vi4List = vd2.variableInfo(list);
            assertEquals("*M,0-2-2M,0|*-1:r", vi4List.linkedVariables().toString());
            assertFalse(vi4List.isModified());
        }
    }
}
