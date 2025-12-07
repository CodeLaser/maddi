package org.e2immu.analyzer.modification.link.vf;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestVirtualFieldComputer extends CommonTest {

    @Test
    public void test() {
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);

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
}
