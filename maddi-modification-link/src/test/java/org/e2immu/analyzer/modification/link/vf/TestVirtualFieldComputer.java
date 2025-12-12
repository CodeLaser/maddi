package org.e2immu.analyzer.modification.link.vf;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;
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
        assertEquals("$m - E[] es", vfList.toString());
        assertEquals("java.util.List", vfList.mutable().owner().toString());
        assertEquals("java.util.List", vfList.hiddenContent().type().typeParameter()
                .getOwner().getLeft().toString());

        // TypeInfo collection = javaInspector.compiledTypesManager().getOrLoad(Collection.class);
        // VirtualFields vfCollection = collection.analysis().getOrNull(VirtualFields.VIRTUAL_FIELDS, VirtualFields.class);
        // assertEquals("$m - T[] ts", vfCollection.toString());

        // ensure that Object has VF computed to NONE
        TypeInfo object = javaInspector.compiledTypesManager().getOrLoad(Object.class);
        VirtualFields vfObject = vfc.compute(object);
        assertEquals(VirtualFields.NONE, vfObject);

        TypeInfo arrayList = javaInspector.compiledTypesManager().getOrLoad(ArrayList.class);
        assertEquals("$m - E[] es", vfc.compute(arrayList).toString());
        TypeInfo deque = javaInspector.compiledTypesManager().getOrLoad(Deque.class);
        assertEquals("$m - E[] es", vfc.compute(deque).toString());

        assertEquals(2, vfc.maxMultiplicityFromMethods(arrayList));
    }

    @DisplayName("map hierarchy")
    @Test
    public void test2() {
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);

        TypeInfo treeMap = javaInspector.compiledTypesManager().getOrLoad(TreeMap.class);
        VirtualFields vfList = vfc.compute(treeMap);
        assertEquals("$m - KV[] kvs", vfList.toString());
        assertEquals("java.util.TreeMap", vfList.mutable().owner().toString());
        assertEquals("java.util.TreeMap.KV", vfList.hiddenContent().type().typeInfo().toString());
        FieldInfo k = vfList.hiddenContent().type().typeInfo().getFieldByName("k", true);
        assertEquals("java.util.TreeMap.KV", k.owner().toString());

        assertEquals(2, vfc.maxMultiplicityFromMethods(treeMap));
    }

    @DisplayName("stream hierarchy")
    @Test
    public void test3() {
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);

        // we want to end up "with $m - T[] ts", multiplicity 2 rather than "$m - TS ts", multiplicity 1
        // how? Iterator<T> is also multi 2
        // recursively defined type parameters to be IGNORED

        TypeInfo stream = javaInspector.compiledTypesManager().getOrLoad(Stream.class);
        VirtualFields vfStream = vfc.compute(stream);
        assertEquals("$m - T[] ts", vfStream.toString());
        assertEquals(2, vfc.maxMultiplicityFromMethods(stream));
    }

    @DisplayName("optional")
    @Test
    public void test4() {
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);

        TypeInfo optional = javaInspector.compiledTypesManager().getOrLoad(Optional.class);
        VirtualFields vfStream = vfc.compute(optional);
        assertEquals("/ - T t", vfStream.toString());
        assertEquals(1, vfc.maxMultiplicityFromMethods(optional));
    }


    @DisplayName("object array")
    @Test
    public void test5() {
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        ParameterizedType objectArray = runtime.objectParameterizedType().copyWithArrays(1);
        VirtualFieldComputer.VfTm vfTm = vfc.computeAllowTypeParameterArray(objectArray, true);
        VirtualFields vfStream = vfTm.virtualFields();
        assertEquals("$mObject - Object[] objects", vfStream.toString());
        VirtualFields vfFormal = vfc.compute(runtime.objectTypeInfo());
        assertEquals("/ - /", vfFormal.toString());
        assertNull(vfTm.formalToConcrete());
    }

    @DisplayName("comparable")
    @Test
    public void test5b() {
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        TypeInfo comparable = javaInspector.compiledTypesManager().getOrLoad(Comparable.class);
        VirtualFields vf = vfc.compute(comparable);
        assertEquals("/ - /", vf.toString());
    }

    @DisplayName("computeAllowTypeParameterArray TP[]")
    @Test
    public void test6() {
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        TypeInfo optional = javaInspector.compiledTypesManager().getOrLoad(Optional.class);
        ParameterizedType tpArray = optional.asParameterizedType().parameters().getFirst().copyWithArrays(1);
        VirtualFieldComputer.VfTm vfTmTpArray = vfc.computeAllowTypeParameterArray(tpArray, true);
        VirtualFields vfTpArray = vfTmTpArray.virtualFields();
        assertEquals("$mT - T[] ts", vfTpArray.toString());
        assertNull(vfTmTpArray.formalToConcrete());

        ParameterizedType tpArray2 = optional.asParameterizedType().parameters().getFirst().copyWithArrays(2);
        VirtualFields vfTpArray2 = vfc.computeAllowTypeParameterArray(tpArray2, true).virtualFields();
        assertEquals("$mT - T[][] tss", vfTpArray2.toString());
    }

    @DisplayName("computeAllowTypeParameterArray List<TP[]>")
    @Test
    public void test7() {
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        TypeInfo optional = javaInspector.compiledTypesManager().getOrLoad(Optional.class);
        TypeInfo list = javaInspector.compiledTypesManager().getOrLoad(List.class);
        ParameterizedType tpArray = optional.asParameterizedType().parameters().getFirst().copyWithArrays(1);
        ParameterizedType listTpArray = runtime.newParameterizedType(list, List.of(tpArray));
        assertEquals("java.util.List<T[]>", listTpArray.descriptor());
        VirtualFieldComputer.VfTm vfTmListTpArray = vfc.computeAllowTypeParameterArray(listTpArray, true);
        VirtualFields vfListTpArray = vfTmListTpArray.virtualFields();
        assertEquals("$m - T[][] tss", vfListTpArray.toString());
        assertEquals("""
                E=TP#0 in Collection [] --> T=TP#0 in Optional []
                E=TP#0 in List [] --> T=TP#0 in Optional []
                E=TP#0 in SequencedCollection [] --> T=TP#0 in Optional []
                T=TP#0 in Iterable [] --> T=TP#0 in Optional []\
                """, vfTmListTpArray.formalToConcrete().toString());

        ParameterizedType tpArray2 = optional.asParameterizedType().parameters().getFirst().copyWithArrays(2);
        ParameterizedType listTpArray2 = runtime.newParameterizedType(list, List.of(tpArray2));
        assertEquals("java.util.List<T[][]>", listTpArray2.descriptor());
        VirtualFieldComputer.VfTm vfTmListTpArray2 = vfc.computeAllowTypeParameterArray(listTpArray2, true);
        VirtualFields vfTpArray2 = vfTmListTpArray2.virtualFields();
        assertEquals("$m - T[][][] tsss", vfTpArray2.toString());
        assertEquals("""
                E=TP#0 in Collection [] --> T=TP#0 in Optional []
                E=TP#0 in List [] --> T=TP#0 in Optional []
                E=TP#0 in SequencedCollection [] --> T=TP#0 in Optional []
                T=TP#0 in Iterable [] --> T=TP#0 in Optional []\
                """, vfTmListTpArray2.formalToConcrete().toString());
    }

    private String txDetails(Map<FieldInfo, FieldInfo> map) {
        return map.entrySet().stream()
                .map(e -> txDetail(e.getKey())
                          + " --> " + txDetail(e.getValue()))
                .sorted()
                .collect(Collectors.joining("\n"));
    }

    private String txDetail(FieldInfo v) {
        String type = v.type().detailedString();
        String owner = v.type().typeParameter() != null
                ? " " + v.type().typeParameter().toStringWithTypeBounds() : "";
        String fields = v.type().typeInfo() == null ? ""
                : " " + v.type().typeInfo().fields().stream()
                .map(f -> f.type().typeParameter().toStringWithTypeBounds())
                .collect(Collectors.joining(", "));
        return v.name() + " [" + type + owner + fields + "]";
    }


    @DisplayName("computeAllowTypeParameterArray map")
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
        VirtualFieldComputer.VfTm vfTmMapTE = vfc.computeAllowTypeParameterArray(mapTE, true);
        VirtualFields vfMapTE = vfTmMapTE.virtualFields();
        assertEquals("$m - TE[] tes", vfMapTE.toString());
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
}
