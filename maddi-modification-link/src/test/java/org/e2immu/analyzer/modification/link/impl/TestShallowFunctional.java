package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestShallowFunctional extends CommonTest {

    // any functional interface will show the same behaviour

    @DisplayName("Analyze 'BiFunction'")
    @Test
    public void test5() {
        LinkComputer linkComputer = new LinkComputerImpl(javaInspector);
        TypeInfo stream = javaInspector.compiledTypesManager().getOrLoad(BiFunction.class);
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        assertEquals("/ - /", vfc.compute(stream).toString());

        MethodInfo apply = stream.findUniqueMethod("apply", 2);
        MethodLinkedVariables mlvFindFirst = linkComputer.doMethod(apply);
        assertEquals("[-, -] --> -", mlvFindFirst.toString());
    }
}
