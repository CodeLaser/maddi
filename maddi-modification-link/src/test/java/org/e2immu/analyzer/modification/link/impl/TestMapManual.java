package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.prepwork.variable.LinkNature;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.ReturnVariable;
import org.e2immu.analyzer.modification.prepwork.variable.impl.ReturnVariableImpl;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestMapManual extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.HashMap;
            import java.util.Map;
            import java.util.Set;
            public class X<K, V> {
                Map<K, V> map = new HashMap<>();
                public V get(K key) {
                    return map.get(key);
                }
                public Set<K> keySet() {
                    return map.keySet();
                }
                public V getOrDefault(K key, V defaultValue) {
                    V v = map.get(key);
                    return v == null ? defaultValue : v;
                }
                public Set<Map.Entry<K, V>> entrySet() {
                    return map.entrySet();
                }
            }
            """;

    @DisplayName("Analyze 'get', map access, manually inserting links for Map.get(K)")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        TypeInfo atomicBoolean = javaInspector.compiledTypesManager().getOrLoad(AtomicBoolean.class);
        assertNotNull(atomicBoolean);
        TypeInfo map = javaInspector.compiledTypesManager().get(Map.class);
        EInfo eInfo = getThisMapEV(X, map, atomicBoolean);

        setMethodLinkedVariablesOfMapGet(map, eInfo);

        MethodInfo get = X.findUniqueMethod("get", 1);
        LinkComputer tlc = new LinkComputerImpl(javaInspector, false, false);
        MethodLinkedVariables mlv = tlc.doMethod(get);
        assertEquals("get<this.map.eArray[-1].v", mlv.ofReturnValue().toString());
    }

    private static void setMethodLinkedVariablesOfMapGet(TypeInfo map, EInfo eInfo) {
        MethodInfo mapGet = map.findUniqueMethod("get", 1);
        ReturnVariable mapGetRv = new ReturnVariableImpl(mapGet);
        MethodLinkedVariablesImpl mlvGet = new MethodLinkedVariablesImpl(
                new LinksImpl.Builder(mapGetRv)
                        .add(mapGetRv, LinkNature.IS_ELEMENT_OF, eInfo.thisMapEV)
                        .build(),
                List.of());
        assertEquals("[] --> get<this.eArray[-1].v", mlvGet.toString());
        mapGet.analysis().set(METHOD_LINKS, mlvGet);
    }

    @DisplayName("Analyze 'keySet', manually inserting links for Map.keySet()")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        TypeInfo atomicBoolean = javaInspector.compiledTypesManager().getOrLoad(AtomicBoolean.class);
        assertNotNull(atomicBoolean);

        TypeInfo map = javaInspector.compiledTypesManager().get(Map.class);
        EInfo eInfo = getThisMapEV(X, map, atomicBoolean);

        TypeInfo set = javaInspector.compiledTypesManager().get(Set.class);
        FieldInfo setM = runtime.newFieldInfo("M", false, atomicBoolean.asParameterizedType(), set);
        ParameterizedType tsPt = runtime.newParameterizedType(set.typeParameters().getFirst(), 1, null);
        FieldInfo setTArray = runtime.newFieldInfo("tArray", false, tsPt, set);

        MethodInfo mapKeySet = map.findUniqueMethod("keySet", 0);
        ReturnVariable mapKeySetRv = new ReturnVariableImpl(mapKeySet);
        VariableExpression mapKeySetRvVe = runtime.newVariableExpression(mapKeySetRv);
        FieldReference mapKeySetRvM = runtime.newFieldReference(setM, mapKeySetRvVe,
                atomicBoolean.asParameterizedType());
        FieldReference mapKeySetRvTArray = runtime.newFieldReference(setTArray, mapKeySetRvVe, setTArray.type());
        MethodLinkedVariablesImpl mlvGet = new MethodLinkedVariablesImpl(
                new LinksImpl.Builder(mapKeySetRv)
                        .add(mapKeySetRvTArray, LinkNature.INTERSECTION_NOT_EMPTY, eInfo.thisMapEK)
                        .add(mapKeySetRvM, LinkNature.IS_IDENTICAL_TO, runtime.newFieldReference(eInfo.M))
                        .build(),
                List.of());
        assertEquals("[] --> keySet.M==this.M,keySet.tArray~this.eArray[-1].k", mlvGet.toString());
        mapKeySet.analysis().set(METHOD_LINKS, mlvGet);

        MethodInfo keySet = X.findUniqueMethod("keySet", 0);
        LinkComputer tlc = new LinkComputerImpl(javaInspector, false, false);
        MethodLinkedVariables mlv = tlc.doMethod(keySet);
        assertEquals("""
                keySet.M==this.map.M,keySet.tArray~this.map.eArray[-1].k\
                """, mlv.ofReturnValue().toString());
    }

    private record EInfo(TypeInfo e, ParameterizedType eArrayPt, FieldInfo eArray,
                         FieldReference thisMapEK,
                         FieldReference thisMapEV,
                         FieldInfo M) {
    }

    private EInfo getThisMapEV(TypeInfo X, TypeInfo map, TypeInfo atomicBoolean) {
        TypeInfo e = makeRecord(X, X.typeParameters());
        ParameterizedType eArrayPt = runtime.newParameterizedType(e, 1);
        // the synthetic field E e
        FieldInfo eArray = runtime.newFieldInfo("eArray", false, eArrayPt, map);
        assertEquals("Type a.b.X.E[]", eArray.type().toString());
        FieldReference thisMapE = runtime.newFieldReference(eArray);
        DependentVariable thisMapEDot = runtime.newDependentVariable(runtime.newVariableExpression(thisMapE),
                runtime.newInt(-1));
        FieldInfo k = e.getFieldByName("k", true);
        FieldReference thisMapEK = runtime.newFieldReference(k, runtime.newVariableExpression(thisMapEDot), k.type());
        FieldInfo v = e.getFieldByName("v", true);
        FieldReference thisMapEV = runtime.newFieldReference(v, runtime.newVariableExpression(thisMapEDot), v.type());

        FieldInfo M = runtime.newFieldInfo("M", false, atomicBoolean.asParameterizedType(), map);
        return new EInfo(e, eArrayPt, eArray, thisMapEK, thisMapEV, M);
    }

    @DisplayName("Analyze 'getOrDefault', map access, manually inserting links for Map.get(K)")
    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse(INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        TypeInfo atomicBoolean = javaInspector.compiledTypesManager().getOrLoad(AtomicBoolean.class);
        assertNotNull(atomicBoolean);
        TypeInfo map = javaInspector.compiledTypesManager().get(Map.class);
        EInfo eInfo = getThisMapEV(X, map, atomicBoolean);

        setMethodLinkedVariablesOfMapGet(map, eInfo);

        MethodInfo getOrDefault = X.findUniqueMethod("getOrDefault", 2);
        LinkComputer tlc = new LinkComputerImpl(javaInspector, false, false);
        MethodLinkedVariables mlv = tlc.doMethod(getOrDefault);
        assertEquals("""
                getOrDefault<this.map.eArray[-1].v,getOrDefault==1:defaultValue\
                """, mlv.ofReturnValue().toString());
    }


    @DisplayName("Analyze 'entrySet', map access, manually inserting links for Map.entrySet()")
    @Test
    public void test4() {
        TypeInfo X = javaInspector.parse(INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        TypeInfo atomicBoolean = javaInspector.compiledTypesManager().getOrLoad(AtomicBoolean.class);
        assertNotNull(atomicBoolean);
        TypeInfo map = javaInspector.compiledTypesManager().get(Map.class);
        EInfo eInfo = getThisMapEV(X, map, atomicBoolean);

        TypeInfo set = javaInspector.compiledTypesManager().get(Set.class);
        FieldInfo setM = runtime.newFieldInfo("M", false, atomicBoolean.asParameterizedType(), set);
        ParameterizedType tsPt = runtime.newParameterizedType(set.typeParameters().getFirst(), 1, null);
        FieldInfo setTArray = runtime.newFieldInfo("tArray", false, tsPt, set);

        MethodInfo mapEntrySet = map.findUniqueMethod("entrySet", 0);
        ReturnVariable mapEntrySetRv = new ReturnVariableImpl(mapEntrySet);
        VariableExpression mapEntrySetRvVe = runtime.newVariableExpression(mapEntrySetRv);
        FieldReference mapEntrySetRvM = runtime.newFieldReference(setM, mapEntrySetRvVe,
                atomicBoolean.asParameterizedType());
        FieldReference eArray = runtime.newFieldReference(eInfo.eArray);
        FieldReference mapEntrySetRvEArray = runtime.newFieldReference(eInfo.eArray, mapEntrySetRvVe, eInfo.eArrayPt);
        MethodLinkedVariablesImpl mlvGet = new MethodLinkedVariablesImpl(
                new LinksImpl.Builder(mapEntrySetRv)
                        .add(mapEntrySetRvEArray, LinkNature.INTERSECTION_NOT_EMPTY, eArray)
                        .add(mapEntrySetRvM, LinkNature.IS_IDENTICAL_TO, runtime.newFieldReference(eInfo.M))
                        .build(),
                List.of());
        assertEquals("[] --> entrySet.M==this.M,entrySet.eArray~this.eArray", mlvGet.toString());
        mapEntrySet.analysis().set(METHOD_LINKS, mlvGet);


        MethodInfo entrySet = X.findUniqueMethod("entrySet", 0);
        LinkComputer tlc = new LinkComputerImpl(javaInspector, false, false);
        MethodLinkedVariables mlv = tlc.doMethod(entrySet);
        assertEquals("""
                entrySet.M==this.map.M,entrySet.eArray~this.map.eArray\
                """, mlv.ofReturnValue().toString());
    }

    private TypeInfo makeRecord(TypeInfo X, List<TypeParameter> typeParameters) {
        TypeInfo typeInfo = runtime.newTypeInfo(X, "E");
        typeInfo.builder().setTypeNature(runtime.typeNatureClass())
                .setParentClass(runtime.objectParameterizedType())
                .setAccess(runtime.accessPublic());
        for (TypeParameter tp : typeParameters) {
            FieldInfo fieldInfo = runtime.newFieldInfo(tp.simpleName().toLowerCase(), false,
                    runtime.newParameterizedType(tp, 0, null), typeInfo);
            fieldInfo.builder().addFieldModifier(runtime.fieldModifierFinal())
                    .addFieldModifier(runtime.fieldModifierPublic())
                    .setInitializer(runtime.newEmptyExpression())
                    .computeAccess().commit();
            typeInfo.builder().addField(fieldInfo);
        }
        typeInfo.builder().commit();
        return typeInfo;
    }

}
