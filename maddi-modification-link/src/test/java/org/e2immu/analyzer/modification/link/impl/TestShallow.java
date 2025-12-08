package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.aapi.parser.AnnotatedApiParser;
import org.e2immu.analyzer.modification.common.defaults.ShallowAnalyzer;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestShallow extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import org.e2immu.annotation.Independent;
            public interface X<T> {
                @Independent(hc = true)
                T get();
                void set(@Independent(hc = true) T t);
                String label(int k);
            }
            """;

    @DisplayName("Analyze 'get/set', multiplicity 1")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        // run the shallow analyzer to detect the annotations
        AnnotatedApiParser annotatedApiParser = new AnnotatedApiParser();
        ShallowAnalyzer shallowAnalyzer = new ShallowAnalyzer(runtime, annotatedApiParser,
                true);
        shallowAnalyzer.go(List.of(X));

        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        VirtualFields vfX = vfc.compute(X);
        assertEquals("$m - T t", vfX.toString());

        LinkComputer linkComputer = new LinkComputerImpl(javaInspector);

        MethodInfo get = X.findUniqueMethod("get", 0);
        MethodLinkedVariables mlvGet = linkComputer.doMethod(get);
        assertEquals("[] --> get==this.t", mlvGet.toString());

        MethodInfo set = X.findUniqueMethod("set", 1);
        MethodLinkedVariables mlvSet = linkComputer.doMethod(set);
        assertEquals("[0:t==this.t] --> -", mlvSet.toString());

        MethodInfo label = X.findUniqueMethod("label", 1);
        MethodLinkedVariables mlvLabel = linkComputer.doMethod(label);
        assertEquals("[-] --> -", mlvLabel.toString());
    }


    @DisplayName("Analyze 'Optional', multiplicity 1")
    @Test
    public void test2() {
        TypeInfo optional = javaInspector.compiledTypesManager().getOrLoad(Optional.class);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(optional);
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        VirtualFields vfX = vfc.compute(optional);
        assertEquals("/ - T t", vfX.toString());

        LinkComputer linkComputer = new LinkComputerImpl(javaInspector);

        MethodInfo get = optional.findUniqueMethod("get", 0);
        MethodLinkedVariables mlvGet = linkComputer.doMethod(get);
        assertEquals("[] --> get==this.t", mlvGet.toString());

        MethodInfo set = optional.findUniqueMethod("orElse", 1);
        MethodLinkedVariables mlvSet = linkComputer.doMethod(set);
        assertEquals("[-] --> orElse==0:other,orElse==this.t", mlvSet.toString());

        MethodInfo label = optional.findUniqueMethod("stream", 0);
        MethodLinkedVariables mlvLabel = linkComputer.doMethod(label);
        assertEquals("[] --> stream>this.t", mlvLabel.toString());
    }

    @DisplayName("Analyze 'List', multiplicity 2")
    @Test
    public void test3() {
        LinkComputer linkComputer = new LinkComputerImpl(javaInspector);
        TypeInfo list = javaInspector.compiledTypesManager().getOrLoad(List.class);

        MethodInfo subList = list.findUniqueMethod("subList", 2);
        MethodLinkedVariables mlvSubList = linkComputer.doMethod(subList);
        assertEquals("[-, -] --> subList~this.ts", mlvSubList.toString());

        MethodInfo get = list.findUniqueMethod("get", 1);
        MethodLinkedVariables mlvGet = linkComputer.doMethod(get);
        assertEquals("[-] --> get<this.ts", mlvGet.toString());

    }


}