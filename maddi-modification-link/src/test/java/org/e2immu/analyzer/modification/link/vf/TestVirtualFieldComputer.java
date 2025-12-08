package org.e2immu.analyzer.modification.link.vf;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestVirtualFieldComputer extends CommonTest {

    @DisplayName("list hierarchy")
    @Test
    public void test1() {
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);

        // start with List. This will recursively compute Collection and Iterable
        TypeInfo list = javaInspector.compiledTypesManager().getOrLoad(List.class);
        VirtualFields vfList = vfc.compute(list);
        assertEquals("$m - T[] ts", vfList.toString());
        assertEquals("java.lang.Iterable", vfList.mutable().owner().toString());
        assertEquals("java.lang.Iterable", vfList.hiddenContent().type().typeParameter()
                .getOwner().getLeft().toString());

        // test that along the way, Collection has been analysed as well
        TypeInfo collection = javaInspector.compiledTypesManager().getOrLoad(Collection.class);
        VirtualFields vfCollection = collection.analysis().getOrNull(VirtualFields.VIRTUAL_FIELDS, VirtualFields.class);
        assertEquals("$m - T[] ts", vfCollection.toString());

        // ensure that Object has VF computed to NONE
        TypeInfo object = javaInspector.compiledTypesManager().getOrLoad(Object.class);
        VirtualFields vfObject = vfc.compute(object);
        assertEquals(VirtualFields.NONE, vfObject);

        TypeInfo arrayList = javaInspector.compiledTypesManager().getOrLoad(ArrayList.class);
        assertEquals("$m - T[] ts", vfc.compute(arrayList).toString());
        TypeInfo deque = javaInspector.compiledTypesManager().getOrLoad(Deque.class);
        assertEquals("$m - T[] ts", vfc.compute(deque).toString());
    }

    @DisplayName("map hierarchy")
    @Test
    public void test2() {
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);

        // start with TreeMap. This will recursively compute NavigableMap, SequencedMap, Map, ...
        TypeInfo list = javaInspector.compiledTypesManager().getOrLoad(TreeMap.class);
        VirtualFields vfList = vfc.compute(list);
        assertEquals("$m - KV[] kvs", vfList.toString());
        assertEquals("java.util.Map", vfList.mutable().owner().toString());
        assertEquals("java.util.Map.KV", vfList.hiddenContent().type().typeInfo().toString());
        FieldInfo k = vfList.hiddenContent().type().typeInfo().getFieldByName("k", true);
        assertEquals("java.util.Map.KV", k.owner().toString());
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
    }
}
