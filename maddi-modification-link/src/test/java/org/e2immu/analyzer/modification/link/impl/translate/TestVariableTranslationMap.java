package org.e2immu.analyzer.modification.link.impl.translate;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.impl.translate.VariableTranslationMap;
import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.link.vf.VirtualFields;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.api.variable.Variable;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class TestVariableTranslationMap extends CommonTest {

    @Test
    public void test() {
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        TypeInfo set = javaInspector.compiledTypesManager().getOrLoad(Set.class);
        This thisSet = runtime.newThis(set.asSimpleParameterizedType());
        TypeInfo map = javaInspector.compiledTypesManager().getOrLoad(Map.class);
        This thisMap = runtime.newThis(map.asSimpleParameterizedType());

        VirtualFields vfSet = vfc.compute(set);
        assertEquals("§m - E[] §es", vfSet.toString());
        VariableTranslationMap vtm = new VariableTranslationMap(runtime);
        vtm.put(thisSet, thisMap);

        FieldReference mSet = runtime.newFieldReference(vfSet.mutable());
        Variable mSetTranslated = vtm.translateVariableRecursively(mSet);
        if (mSetTranslated instanceof FieldReference mSetTFr) {
            assertEquals("this.§m", mSetTFr.toString());
            assertEquals("Type java.util.concurrent.atomic.AtomicBoolean",
                    mSetTFr.parameterizedType().toString());
            assertEquals("Type java.util.Map",
                    mSetTFr.scopeVariable().parameterizedType().toString());
            assertSame(map, mSetTFr.fieldInfo().owner());
        } else fail();
    }


    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.HashSet;
            import java.util.List;
            import java.util.Set;
            public class X {
                 interface I { }
                 class II implements I { }
                 Set<II> set = new HashSet<>();
                 void add(II ii) {
                     set.add(ii);
                 }
                 void method(List<II> list, X x) {
                    list.forEach(x::add);
                 }
            }
            """;

    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        MethodInfo method = X.findUniqueMethod("method", 2);
        ParameterInfo x = method.parameters().getLast();
        FieldInfo set = X.getFieldByName("set", true);
        FieldReference setFr = runtime.newFieldReference(set);
        assertEquals("this.set", setFr.toString());
        VariableTranslationMap vtm = new VariableTranslationMap(runtime).put(setFr, x);

        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        VirtualFields vfSet = vfc.compute(set.type().typeInfo());
        assertEquals("§m - E[] §es", vfSet.toString());

        FieldInfo mutable = vfSet.mutable().withOwner(setFr.parameterizedType().typeInfo());
        FieldReference mSet = runtime.newFieldReference(mutable, runtime.newVariableExpression(setFr),
                vfSet.mutable().type());
        assertEquals("this.set.§m", mSet.toString());
        Variable mSetTranslated = vtm.translateVariableRecursively(mSet);
        if (mSetTranslated instanceof FieldReference mSetTFr) {
            assertEquals("x.§m", mSetTFr.toString());
            assertEquals("Type java.util.concurrent.atomic.AtomicBoolean",
                    mSetTFr.parameterizedType().toString());
            assertEquals("Type a.b.X", mSetTFr.scopeVariable().parameterizedType().toString());
        } else fail();
    }

    @DisplayName("replace this")
    @Test
    public void test2b() {
        TypeInfo X = javaInspector.parse(INPUT2);
        MethodInfo method = X.findUniqueMethod("method", 2);
        ParameterInfo x = method.parameters().getLast();
        FieldInfo set = X.getFieldByName("set", true);
        FieldReference setFr = runtime.newFieldReference(set);
        assertEquals("this.set", setFr.toString());

        This thisVar = runtime.newThis(X.asSimpleParameterizedType());
        VariableTranslationMap vtm = new VariableTranslationMap(runtime).put(thisVar, x);

        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        VirtualFields vfSet = vfc.compute(set.type().typeInfo());
        assertEquals("§m - E[] §es", vfSet.toString());

        FieldInfo mutable = vfSet.mutable().withOwner(setFr.parameterizedType().typeInfo());
        FieldReference mSet = runtime.newFieldReference(mutable, runtime.newVariableExpression(setFr),
                vfSet.mutable().type());
        assertEquals("this.set.§m", mSet.toString());
        Variable mSetTranslated = vtm.translateVariableRecursively(mSet);
        if (mSetTranslated instanceof FieldReference mSetTFr) {
            assertEquals("x.set.§m", mSetTFr.toString());
            assertEquals("Type java.util.concurrent.atomic.AtomicBoolean",
                    mSetTFr.parameterizedType().toString());
            assertEquals("Type java.util.Set<a.b.X.II>",
                    mSetTFr.scopeVariable().parameterizedType().toString());
            assertEquals("java.util.Set", mSetTFr.fieldInfo().owner().toString());
        } else fail();
    }


    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            public class X {
                record S(String c) { }
                record R(S b) { }
            }
            """;

    // base a, sub a.b.c; tm = a -> d; result of translating should be d.b.c;
    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse(INPUT3);
        TypeInfo R = X.findSubType("R");
        FieldInfo b = R.getFieldByName("b", true);
        TypeInfo S = X.findSubType("S");
        FieldInfo c = S.getFieldByName("c", true);

        VariableTranslationMap vtm = new VariableTranslationMap(runtime);
        LocalVariable a = runtime.newLocalVariable("a", R.asParameterizedType());
        LocalVariable d = runtime.newLocalVariable("d", R.asParameterizedType());
        vtm.put(a, d);

        FieldReference ab = runtime.newFieldReference(b, runtime.newVariableExpression(a), b.type());
        FieldReference abc = runtime.newFieldReference(c, runtime.newVariableExpression(ab), c.type());
        Variable abcTranslated = vtm.translateVariableRecursively(abc);
        assertEquals("d.b.c", abcTranslated.toString());
    }


    @Language("java")
    private static final String INPUT3b = """
            package a.b;
            public class X {
                record S(String c) { }
                record R(S b) { }
                R method(R r) {
                    return r;
                }
            }
            """;

    // base a, sub a.b.§c; tm = a -> d; result of translating should be d.b.§c;
    // test originally failed because of code in VariableTranslationMap related to assignmentExpression of local variables
    // NOTE: this bit has been removed from the original code in TranslationMapImpl as well
    @Test
    public void test3b() {
        TypeInfo X = javaInspector.parse(INPUT3b);
        TypeInfo R = X.findSubType("R");
        FieldInfo b = R.getFieldByName("b", true);
        MethodInfo method = X.findUniqueMethod("method", 1);

        VirtualFieldComputer virtualFieldComputer = new VirtualFieldComputer(javaInspector);
        FieldInfo c = virtualFieldComputer.newMField(R);
        VariableTranslationMap vtm = new VariableTranslationMap(runtime);
        LocalVariable d = runtime.newLocalVariable("d", R.asParameterizedType());
        LocalVariable a = runtime.newLocalVariable("a", R.asParameterizedType(),
                runtime.newMethodCallBuilder()
                        .setMethodInfo(method)
                        .setConcreteReturnType(R.asParameterizedType())
                        .setObject(runtime.newVariableExpression(runtime.newThis(X.asParameterizedType())))
                        .setParameterExpressions(List.of(runtime.newVariableExpression(d)))
                        .build());
        vtm.put(a, d);

        FieldReference ab = runtime.newFieldReference(b, runtime.newVariableExpression(a), b.type());
        FieldReference abc = runtime.newFieldReference(c, runtime.newVariableExpression(ab), c.type());
        assertEquals("a.b.§m", abc.toString());
        Variable abcTranslated = vtm.translateVariableRecursively(abc);
        assertEquals("d.b.§m", abcTranslated.toString());
    }

}
