package org.e2immu.analyzer.modification.link.vf;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.Variable;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TestVirtualFieldComputer extends CommonTest {

    @DisplayName("list hierarchy")
    @Test
    public void test1() {
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);

        TypeInfo list = javaInspector.compiledTypesManager().getOrLoad(List.class);
        VirtualFields vfList = vfc.compute(list);
        assertEquals("§m - E[] §es", vfList.toString());
        assertEquals("java.util.List", vfList.mutable().owner().toString());
        assertEquals("java.util.List", vfList.hiddenContent().type().typeParameter()
                .getOwner().getLeft().toString());

        // TypeInfo collection = javaInspector.compiledTypesManager().getOrLoad(Collection.class);
        // VirtualFields vfCollection = collection.analysis().getOrNull(VirtualFields.VIRTUAL_FIELDS, VirtualFields.class);
        // assertEquals("§m - T[] ts", vfCollection.toString());

        TypeInfo object = javaInspector.compiledTypesManager().getOrLoad(Object.class);
        VirtualFields vfObject = vfc.compute(object);
        assertEquals("/ - Object §0", vfObject.toString());

        TypeInfo arrayList = javaInspector.compiledTypesManager().getOrLoad(ArrayList.class);
        assertEquals("§m - E[] §es", vfc.compute(arrayList).toString());
        TypeInfo deque = javaInspector.compiledTypesManager().getOrLoad(Deque.class);
        assertEquals("§m - E[] §es", vfc.compute(deque).toString());

        assertEquals(2, vfc.maxMultiplicityFromMethods(arrayList));
    }

    @DisplayName("map hierarchy")
    @Test
    public void test2() {
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);

        TypeInfo treeMap = javaInspector.compiledTypesManager().getOrLoad(TreeMap.class);
        VirtualFields vfList = vfc.compute(treeMap);
        assertEquals("§m - KV[] §kvs", vfList.toString());
        assertEquals("java.util.TreeMap", vfList.mutable().owner().toString());
        assertEquals("java.util.TreeMap.KV", vfList.hiddenContent().type().typeInfo().toString());
        FieldInfo k = vfList.hiddenContent().type().typeInfo().getFieldByName("§k", true);
        assertEquals("java.util.TreeMap.KV", k.owner().toString());

        assertEquals(2, vfc.maxMultiplicityFromMethods(treeMap));
    }

    @DisplayName("stream hierarchy")
    @Test
    public void test3() {
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);

        // we want to end up "with $m - T[] ts", multiplicity 2 rather than "§m - TS ts", multiplicity 1
        // how? Iterator<T> is also multi 2
        // recursively defined type parameters to be IGNORED

        TypeInfo stream = javaInspector.compiledTypesManager().getOrLoad(Stream.class);
        VirtualFields vfStream = vfc.compute(stream);
        assertEquals("§m - T[] §ts", vfStream.toString());
        assertEquals(2, vfc.maxMultiplicityFromMethods(stream));
    }

    @DisplayName("optional")
    @Test
    public void test4() {
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);

        TypeInfo optional = javaInspector.compiledTypesManager().getOrLoad(Optional.class);
        VirtualFields vfStream = vfc.compute(optional);
        assertEquals("/ - T §t", vfStream.toString());
        assertEquals(1, vfc.maxMultiplicityFromMethods(optional));
    }

    @DisplayName("Optional<String>")
    @Test
    public void test4b() {
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);

        TypeInfo optional = javaInspector.compiledTypesManager().getOrLoad(Optional.class);
        ParameterizedType os = runtime.newParameterizedType(optional, List.of(runtime.stringParameterizedType()));
        VirtualFieldComputer.VfTm vfTm = vfc.compute(os, true);
        assertEquals("/ - String §0", vfTm.virtualFields().toString());
        assertEquals("T=TP#0 in Optional [] --> String", vfTm.formalToConcrete().toString());
    }

    // even if Optional is immutable HC, its concrete HC is mutable, so the type has a §m field
    @DisplayName("Optional<StringBuilder>")
    @Test
    public void test4c() {
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        TypeInfo stringBuilder = javaInspector.compiledTypesManager().getOrLoad(StringBuilder.class);
        TypeInfo optional = javaInspector.compiledTypesManager().getOrLoad(Optional.class);
        ParameterizedType os = runtime.newParameterizedType(optional, List.of(stringBuilder.asParameterizedType()));
        VirtualFieldComputer.VfTm vfTm = vfc.compute(os, true);
        assertEquals("§m - StringBuilder §0", vfTm.virtualFields().toString());
        assertEquals("T=TP#0 in Optional [] --> StringBuilder", vfTm.formalToConcrete().toString());
    }


    @DisplayName("object array")
    @Test
    public void test5() {
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        ParameterizedType objectArray = runtime.objectParameterizedType().copyWithArrays(1);
        VirtualFieldComputer.VfTm vfTm = vfc.compute(objectArray, true);
        VirtualFields vfStream = vfTm.virtualFields();
        assertEquals("§m - Object[] §$s", vfStream.toString());
        VirtualFields vfFormal = vfc.compute(runtime.objectTypeInfo());
        assertEquals("/ - Object §0", vfFormal.toString());
        assertEquals("", vfTm.formalToConcrete().toString());
    }

    @DisplayName("comparable")
    @Test
    public void test5b() {
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        TypeInfo comparable = javaInspector.compiledTypesManager().getOrLoad(Comparable.class);
        VirtualFields vf = vfc.compute(comparable);
        assertEquals("/ - /", vf.toString());
    }

    @DisplayName("TP[]")
    @Test
    public void test6() {
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        TypeInfo optional = javaInspector.compiledTypesManager().getOrLoad(Optional.class);
        ParameterizedType tpArray = optional.asParameterizedType().parameters().getFirst().copyWithArrays(1);
        VirtualFieldComputer.VfTm vfTmTpArray = vfc.compute(tpArray, true);
        VirtualFields vfTpArray = vfTmTpArray.virtualFields();
        assertEquals("§mT - T[] §ts", vfTpArray.toString());
        assertNull(vfTmTpArray.formalToConcrete());

        ParameterizedType tpArray2 = optional.asParameterizedType().parameters().getFirst().copyWithArrays(2);
        VirtualFields vfTpArray2 = vfc.compute(tpArray2, true).virtualFields();
        assertEquals("§mT - T[][] §tss", vfTpArray2.toString());
    }

    @DisplayName("List<TP[]>")
    @Test
    public void test7() {
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        TypeInfo optional = javaInspector.compiledTypesManager().getOrLoad(Optional.class);
        TypeInfo list = javaInspector.compiledTypesManager().getOrLoad(List.class);
        ParameterizedType tpArray = optional.asParameterizedType().parameters().getFirst().copyWithArrays(1);
        ParameterizedType listTpArray = runtime.newParameterizedType(list, List.of(tpArray));
        assertEquals("java.util.List<T[]>", listTpArray.descriptor());
        VirtualFieldComputer.VfTm vfTmListTpArray = vfc.compute(listTpArray, true);
        VirtualFields vfListTpArray = vfTmListTpArray.virtualFields();
        assertEquals("§m - T[][] §tss", vfListTpArray.toString());
        assertEquals("""
                E=TP#0 in Collection [] --> T=TP#0 in Optional [] dim 1
                E=TP#0 in List [] --> T=TP#0 in Optional [] dim 1
                E=TP#0 in SequencedCollection [] --> T=TP#0 in Optional [] dim 1
                T=TP#0 in Iterable [] --> T=TP#0 in Optional [] dim 1\
                """, vfTmListTpArray.formalToConcrete().toString());

        ParameterizedType tpArray2 = optional.asParameterizedType().parameters().getFirst().copyWithArrays(2);
        ParameterizedType listTpArray2 = runtime.newParameterizedType(list, List.of(tpArray2));
        assertEquals("java.util.List<T[][]>", listTpArray2.descriptor());
        VirtualFieldComputer.VfTm vfTmListTpArray2 = vfc.compute(listTpArray2, true);
        VirtualFields vfTpArray2 = vfTmListTpArray2.virtualFields();
        assertEquals("§m - T[][][] §tsss", vfTpArray2.toString());
        assertEquals("""
                E=TP#0 in Collection [] --> T=TP#0 in Optional [] dim 2
                E=TP#0 in List [] --> T=TP#0 in Optional [] dim 2
                E=TP#0 in SequencedCollection [] --> T=TP#0 in Optional [] dim 2
                T=TP#0 in Iterable [] --> T=TP#0 in Optional [] dim 2\
                """, vfTmListTpArray2.formalToConcrete().toString());
    }


    @DisplayName("map")
    @Test
    public void test8() {
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);

        TypeInfo treeMap = javaInspector.compiledTypesManager().getOrLoad(TreeMap.class);
        TypeInfo optional = javaInspector.compiledTypesManager().getOrLoad(Optional.class);
        TypeInfo list = javaInspector.compiledTypesManager().getOrLoad(List.class);
        ParameterizedType mapTE = runtime.newParameterizedType(treeMap, List.of(
                optional.asParameterizedType().parameters().getFirst(),
                list.asParameterizedType().parameters().getFirst()
        ));
        assertEquals("java.util.TreeMap<T,E>", mapTE.descriptor());
        VirtualFieldComputer.VfTm vfTmMapTE = vfc.compute(mapTE, true);
        VirtualFields vfMapTE = vfTmMapTE.virtualFields();
        assertEquals("§m - TE[] §tes", vfMapTE.toString());
        assertEquals("java.util.Optional[T]", vfMapTE.hiddenContent().type().typeInfo()
                .fields().getFirst().type().typeParameter().descriptor());
        assertEquals("""
                K=TP#0 in AbstractMap [] --> T=TP#0 in Optional []
                K=TP#0 in Map [] --> T=TP#0 in Optional []
                K=TP#0 in NavigableMap [] --> T=TP#0 in Optional []
                K=TP#0 in SequencedMap [] --> T=TP#0 in Optional []
                K=TP#0 in SortedMap [] --> T=TP#0 in Optional []
                K=TP#0 in TreeMap [] --> T=TP#0 in Optional []
                V=TP#1 in AbstractMap [] --> E=TP#0 in List []
                V=TP#1 in Map [] --> E=TP#0 in List []
                V=TP#1 in NavigableMap [] --> E=TP#0 in List []
                V=TP#1 in SequencedMap [] --> E=TP#0 in List []
                V=TP#1 in SortedMap [] --> E=TP#0 in List []
                V=TP#1 in TreeMap [] --> E=TP#0 in List []\
                """, vfTmMapTE.formalToConcrete().toString());
    }

    @DisplayName("Map.Entry<Stream<X>, Stream<Y>>")
    @Test
    public void test9() {
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);

        TypeInfo map = javaInspector.compiledTypesManager().getOrLoad(Map.class);
        TypeInfo mapEntry = map.findSubType("Entry");
        TypeInfo optional = javaInspector.compiledTypesManager().getOrLoad(Optional.class);
        TypeInfo list = javaInspector.compiledTypesManager().getOrLoad(List.class);
        TypeInfo stream = javaInspector.compiledTypesManager().getOrLoad(Stream.class);
        ParameterizedType streamT = runtime.newParameterizedType(stream, List.of(optional.asParameterizedType().parameters().getFirst()));
        ParameterizedType streamE = runtime.newParameterizedType(stream, List.of(list.asParameterizedType().parameters().getFirst()));

        ParameterizedType mapEntryStreamTStreamE = runtime.newParameterizedType(mapEntry, List.of(streamT, streamE));
        assertEquals("java.util.Map.Entry<java.util.stream.Stream<T>,java.util.stream.Stream<E>>",
                mapEntryStreamTStreamE.descriptor());

        VirtualFieldComputer.VfTm vfTm = vfc.compute(mapEntryStreamTStreamE, false);
        assertEquals("§tses", vfTm.virtualFields().hiddenContent().name());
        ParameterizedType hcType = vfTm.virtualFields().hiddenContent().type();
        assertEquals("java.util.Map.Entry.TSES", hcType.detailedString());
        assertEquals(2, hcType.typeInfo().fields().size());
        FieldInfo ts = hcType.typeInfo().fields().getFirst();
        assertEquals("§ts", ts.name());
        assertEquals("T[]", ts.type().detailedString());
    }

    @Language("java")
    private static final String INPUT10 = """
            package a.b;
            import java.util.AbstractMap;
            import java.util.Map;
            import java.util.stream.Stream;
            public class C<X, Y> {
                public Map.Entry<Stream<X>, Stream<Y>> oneInstance(X x, Y y) {
                    return new AbstractMap.SimpleEntry<>(Stream.of(x), Stream.of(y));
                }
            }
            """;

    @DisplayName("C<X,Y>")
    @Test
    public void test10() {
        TypeInfo C = javaInspector.parse(INPUT10);
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        VirtualFields vf = vfc.compute(C);
        // this could be correct, could be wrong, depending on other methods being present or absent
        assertEquals("§m - XY §xy", vf.toString());
        MethodInfo oneInstance = C.findUniqueMethod("oneInstance", 2);
        VirtualFields vf2 = vfc.compute(oneInstance.returnType(), false).virtualFields();
        // but this one is correct
        assertEquals("§m - XSYS §xsys", vf2.toString());
    }


    @Language("java")
    private static final String INPUT11 = """
            package a.b;
            import org.e2immu.annotation.Independent;
            import java.util.List;
            public class C<X> {
                record R<V>(V v) { }
                @Independent(hc = true)
                abstract List<R<X>> method(List<X> list);
            }
            """;

    @DisplayName("wrapped")
    @Test
    public void test11() {
        TypeInfo C = javaInspector.parse(INPUT11);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        TypeInfo R = C.findSubType("R");
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        // NOTE: the §m is there because we have not done the @Final analysis
        assertEquals("§m - V §v", vfc.compute(R).toString());
        assertEquals("§m - X[] §xs", vfc.compute(C).toString());
    }


    @DisplayName("translation map for container VF")
    @Test
    public void test12() {
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);

        TypeInfo map = javaInspector.compiledTypesManager().getOrLoad(Map.class);
        VirtualFields vfMap = vfc.compute(map);
        assertEquals("§m - KV[] §kvs", vfMap.toString());

        TypeInfo stringBuilder = javaInspector.compiledTypesManager().getOrLoad(StringBuilder.class);
        TypeInfo list = javaInspector.compiledTypesManager().getOrLoad(List.class);
        ParameterizedType mapSE = runtime.newParameterizedType(map, List.of(
                stringBuilder.asParameterizedType(),
                list.asParameterizedType().parameters().getFirst()
        ));
        assertEquals("java.util.Map<java.lang.StringBuilder,E>", mapSE.descriptor());
        VirtualFieldComputer.VfTm vfTmMapTE = vfc.compute(mapSE, true);
        VirtualFields vfMapTE = vfTmMapTE.virtualFields();
        assertEquals("§m - $E[] §$es", vfMapTE.toString());

        assertEquals("""
                K=TP#0 in Map [] --> StringBuilder
                V=TP#1 in Map [] --> E=TP#0 in List []\
                """, vfTmMapTE.formalToConcrete().toString());

        FieldReference fr = runtime.newFieldReference(vfMap.hiddenContent());
        Variable translated = vfTmMapTE.formalToConcrete().translateVariableRecursively(fr);
        assertEquals("this.§$es", translated.toString());
    }


    @Language("java")
    private static final String INPUT13 = """
            package a.b;
            import java.util.Iterator;
            public class C<S> {
                void method(Iterable<S> iterable) {
                    Iterator<S> it = iterable.iterator();
                }
            }
            """;

    @DisplayName("iterable")
    @Test
    public void test13() {
        TypeInfo C = javaInspector.parse(INPUT13);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        MethodInfo method = C.findUniqueMethod("method", 1);
        ParameterInfo iterable = method.parameters().getFirst();

        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        VirtualFieldComputer.VfTm vfTm = vfc.compute(iterable.parameterizedType(), true);
        assertEquals("""
                VfTm[virtualFields=§m - S[] §ss, formalToConcrete=T=TP#0 in Iterable [] --> S=TP#0 in C []]\
                """, vfTm.toString());

    }

}
