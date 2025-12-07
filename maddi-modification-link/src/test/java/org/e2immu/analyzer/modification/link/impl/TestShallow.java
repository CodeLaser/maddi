package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.MethodLinkedVariables;
import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.link.vf.VirtualFields;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestShallow extends CommonTest {

    @Test
    public void test() {
        LinkComputer lc = new LinkComputerImpl(javaInspector);
        TypeInfo list = javaInspector.compiledTypesManager().getOrLoad(List.class);
        MethodInfo get = list.findUniqueMethod("get", 1);
        lc.doMethod(get);
    }


    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            public interface X<T> {
                T get();
            }
            """;

    @DisplayName("Analyze 'get', map access, manually inserting links for Map.get(K)")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        VirtualFields vfX = vfc.compute(X);
        assertEquals("$m - T t", vfX.toString());
        LinkComputer linkComputer = new LinkComputerImpl(javaInspector);
        MethodInfo get = X.findUniqueMethod("get", 0);
        MethodLinkedVariables mlv = linkComputer.doMethod(get);
        assertEquals("[] --> get==this.t", mlv.toString());
    }
}