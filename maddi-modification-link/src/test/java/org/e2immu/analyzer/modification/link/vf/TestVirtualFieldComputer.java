package org.e2immu.analyzer.modification.link.vf;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestVirtualFieldComputer extends CommonTest {

    @Test
    public void test() {
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        TypeInfo list = javaInspector.compiledTypesManager().getOrLoad(List.class);
        assertEquals("$m - E[] es", vfc.compute(list).toString());
        TypeInfo arrayList = javaInspector.compiledTypesManager().getOrLoad(ArrayList.class);
        assertEquals("$m - E[] es", vfc.compute(arrayList).toString());
        TypeInfo deque = javaInspector.compiledTypesManager().getOrLoad(Deque.class);
        assertEquals("$m - E[] es", vfc.compute(deque).toString());
    }
}
