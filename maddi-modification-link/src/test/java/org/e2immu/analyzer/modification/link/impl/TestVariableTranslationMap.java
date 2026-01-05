package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.link.vf.VirtualFields;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.api.variable.Variable;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

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
}
