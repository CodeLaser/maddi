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

import java.util.*;
import java.util.stream.Stream;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
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


    @DisplayName("Analyze 'Optional', multiplicity 1, 1 type parameter")
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

        MethodInfo orElseThrow = optional.findUniqueMethod("orElseThrow", 0);
        MethodLinkedVariables mlvOrElseThrow = linkComputer.doMethod(orElseThrow);
        assertEquals("[] --> orElseThrow==this.t", mlvOrElseThrow.toString());

        MethodInfo stream = optional.findUniqueMethod("stream", 0);
        MethodLinkedVariables mlvStream = linkComputer.doMethod(stream);
        assertEquals("[] --> stream.ts>this.t", mlvStream.toString());

        MethodInfo of = optional.findUniqueMethod("of", 1);
        MethodLinkedVariables mlvOf = linkComputer.doMethod(of);
        assertEquals("[-] --> of.t==0:value", mlvOf.toString());
    }

    @DisplayName("Analyze 'List', multiplicity 2, 1 type parameter")
    @Test
    public void test3() {
        LinkComputer linkComputer = new LinkComputerImpl(javaInspector);
        TypeInfo list = javaInspector.compiledTypesManager().getOrLoad(List.class);

        MethodInfo toArrayTs = list.methodStream()
                .filter(mi -> "toArray".equals(mi.name()) && mi.parameters().size() == 1
                              && mi.parameters().getFirst().parameterizedType().arrays() == 1)
                .findFirst().orElseThrow();
        assertEquals("java.util.List.toArray(T[])", toArrayTs.fullyQualifiedName());
        MethodLinkedVariables mlvToArray = linkComputer.doMethod(toArrayTs);
        assertEquals("[-] --> toArray.$mT==this.$m", mlvToArray.toString());

        MethodInfo addAll = list.findUniqueMethod("addAll", 1);
        MethodLinkedVariables mlvAddAll = linkComputer.doMethod(addAll);
        assertEquals("[0:collection.ts~this.ts] --> -", mlvAddAll.toString());

        MethodInfo ofVarargs = list.methodStream().filter(mi ->
                        "of".equals(mi.name()) && mi.parameters().size() == 1 && mi.parameters().getFirst().isVarArgs())
                .findFirst().orElseThrow();
        assertEquals("java.util.List.of(E...)", ofVarargs.fullyQualifiedName());
        MethodLinkedVariables mlvOfVarargs = linkComputer.doMethod(ofVarargs);
        assertEquals("[-] --> of.ts~0:elements", mlvOfVarargs.toString());

        MethodInfo of2 = list.findUniqueMethod("of", 2);
        MethodLinkedVariables mlvOf2 = linkComputer.doMethod(of2);
        assertEquals("[-, -] --> of.ts>0:e1,of.ts>1:e2", mlvOf2.toString());

        MethodInfo add = list.findUniqueMethod("add", 1);
        MethodLinkedVariables mlvAdd = linkComputer.doMethod(add);
        assertEquals("[0:e<this.ts] --> -", mlvAdd.toString());

        MethodInfo set = list.findUniqueMethod("set", 2);
        MethodLinkedVariables mlvSet = linkComputer.doMethod(set);
        assertEquals("[-, 1:e<this.ts] --> set<this.ts", mlvSet.toString());

        MethodInfo subList = list.findUniqueMethod("subList", 2);
        MethodLinkedVariables mlvSubList = linkComputer.doMethod(subList);
        assertEquals("[-, -] --> subList.$m==this.$m,subList.ts~this.ts", mlvSubList.toString());

        MethodInfo get = list.findUniqueMethod("get", 1);
        MethodLinkedVariables mlvGet = linkComputer.doMethod(get);
        assertEquals("[-] --> get<this.ts", mlvGet.toString());

        MethodInfo contains = list.findUniqueMethod("contains", 1);
        MethodLinkedVariables mlvContains = linkComputer.doMethod(contains);
        assertEquals("[-] --> -", mlvContains.toString());
    }

    @DisplayName("Analyze 'Map', multiplicity 2, 2 type parameters")
    @Test
    public void test4() {
        LinkComputer linkComputer = new LinkComputerImpl(javaInspector);
        TypeInfo map = javaInspector.compiledTypesManager().getOrLoad(Map.class);

        MethodInfo entrySet = map.findUniqueMethod("entrySet", 0);
        MethodLinkedVariables mlvEntrySet = linkComputer.doMethod(entrySet);
        assertEquals("[] --> entrySet.$m==this.$m,entrySet.ts~this.kvs", mlvEntrySet.toString());

        MethodInfo getOrDefault = map.findUniqueMethod("getOrDefault", 2);
        MethodLinkedVariables mlvGetOrDefault = linkComputer.doMethod(getOrDefault);
        assertEquals("[-, -] --> getOrDefault<this.kvs[-1].v,getOrDefault==1:defaultValue",
                mlvGetOrDefault.toString());

        MethodInfo get = map.findUniqueMethod("get", 1);
        MethodLinkedVariables mlvGet = linkComputer.doMethod(get);
        assertEquals("[-] --> get<this.kvs[-1].v", mlvGet.toString());

        MethodInfo keySet = map.findUniqueMethod("keySet", 0);
        MethodLinkedVariables mlvKeySet = linkComputer.doMethod(keySet);
        assertEquals("[] --> keySet.$m==this.$m,keySet.ts~this.kvs[-1].k", mlvKeySet.toString());

        MethodInfo values = map.findUniqueMethod("values", 0);
        MethodLinkedVariables mlvValues = linkComputer.doMethod(values);
        assertEquals("[] --> values.$m==this.$m,values.ts~this.kvs[-1].v", mlvValues.toString());

    }

    @DisplayName("Analyze 'Stream', multiplicity 2, 1 type parameter")
    @Test
    public void test5() {
        LinkComputer linkComputer = new LinkComputerImpl(javaInspector);
        TypeInfo stream = javaInspector.compiledTypesManager().getOrLoad(Stream.class);
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        assertEquals("$m - T[] ts", vfc.compute(stream).toString());

        MethodInfo findFirst = stream.findUniqueMethod("findFirst", 0);
        MethodLinkedVariables mlvFindFirst = linkComputer.doMethod(findFirst);
        assertEquals("[] --> findFirst.t<this.ts", mlvFindFirst.toString());

        MethodInfo filter = stream.findUniqueMethod("filter", 1);
        MethodLinkedVariables mlvFilter = linkComputer.doMethod(filter);
        assertEquals("[-] --> filter.ts~this.ts", mlvFilter.toString());
    }

    @DisplayName("Analyze 'ArrayList' constructors, multiplicity 2, 1 type parameter")
    @Test
    public void test6() {
        LinkComputer linkComputer = new LinkComputerImpl(javaInspector);
        TypeInfo collection = javaInspector.compiledTypesManager().getOrLoad(Collection.class);
        TypeInfo arrayList = javaInspector.compiledTypesManager().getOrLoad(ArrayList.class);
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        assertEquals("$m - T[] ts", vfc.compute(arrayList).toString());

        MethodInfo c1 = arrayList.findConstructor(collection);
        MethodLinkedVariables mlvC1 = linkComputer.doMethod(c1);
        assertEquals("[0:c.ts~this.ts] --> -", mlvC1.toString());

    }

    @Language("java")
    private static final String INPUT7 = """
            package a.b;
            import org.e2immu.annotation.Independent;
            import java.util.Collection;
            public interface X {
                void add(@Independent(hcParameters = {1}) Collection<X> xs, X x);
            }
            """;

    @DisplayName("Analyze simpler version of Collections.addAll")
    @Test
    public void test7() {
        TypeInfo X = javaInspector.parse(INPUT7);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        // run the shallow analyzer to detect the annotations
        AnnotatedApiParser annotatedApiParser = new AnnotatedApiParser();
        ShallowAnalyzer shallowAnalyzer = new ShallowAnalyzer(runtime, annotatedApiParser,
                true);
        shallowAnalyzer.go(List.of(X));

        LinkComputer linkComputer = new LinkComputerImpl(javaInspector);

        MethodInfo add = X.findUniqueMethod("add", 2);
        MethodLinkedVariables mlvAdd = linkComputer.doMethod(add);
        assertEquals("[-] --> -", mlvAdd.toString());
    }

    @DisplayName("Analyze 'Collections.addAll(...)")
    @Test
    public void test8() {
        LinkComputer linkComputer = new LinkComputerImpl(javaInspector);
        TypeInfo collections = javaInspector.compiledTypesManager().getOrLoad(Collections.class);
        MethodInfo addAll = collections.findUniqueMethod("addAll", 2);
        assertEquals("java.util.Collections.addAll(java.util.Collection<? super T>,T...)",
                addAll.fullyQualifiedName());
        MethodLinkedVariables mlvC1 = addAll.analysis().getOrCreate(METHOD_LINKS, () -> linkComputer.doMethod(addAll));
        assertEquals("[0:c.ts~this.ts] --> -", mlvC1.toString());
    }
}