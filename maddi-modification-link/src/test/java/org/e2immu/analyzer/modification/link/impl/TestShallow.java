package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.aapi.parser.AnnotatedApiParser;
import org.e2immu.analyzer.modification.common.defaults.ShallowAnalyzer;
import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.link.vf.VirtualFields;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.Link;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Stream;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.IndependentImpl.INDEPENDENT_HC;
import static org.junit.jupiter.api.Assertions.*;

public class TestShallow extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import org.e2immu.annotation.Independent;
            import org.e2immu.annotation.NotModified;
            public interface X<T> {
                @Independent(hc = true)
                @NotModified
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

        ShallowAnalyzer shallowAnalyzer = new ShallowAnalyzer(runtime, Element::annotations, true);
        shallowAnalyzer.go(List.of(X));

        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        VirtualFields vfX = vfc.compute(X);
        assertEquals("§m - T §t", vfX.toString());

        LinkComputer linkComputer = new LinkComputerImpl(javaInspector);

        MethodInfo get = X.findUniqueMethod("get", 0);
        assertTrue(get.isNonModifying());
        MethodLinkedVariables mlvGet = linkComputer.doMethod(get);
        assertEquals("[] --> get←this.§t", mlvGet.toString());

        MethodInfo set = X.findUniqueMethod("set", 1);
        assertTrue(set.isModifying());
        MethodLinkedVariables mlvSet = linkComputer.doMethod(set);
        assertEquals("[0:t→this*.§t] --> -", mlvSet.toString());

        MethodInfo label = X.findUniqueMethod("label", 1);
        MethodLinkedVariables mlvLabel = linkComputer.doMethod(label);
        assertEquals("[-] --> -", mlvLabel.toString());
    }


    @Language("java")
    private static final String INPUT1b = """
            package a.b;
            import org.e2immu.annotation.Independent;
            import org.e2immu.annotation.NotModified;
            public interface X<T extends Comparable<? super T>> {
                @Independent(hc = true)
                @NotModified
                T get();
                void set(@Independent(hc = true) T t);
                String label(int k);
            }
            """;

    @DisplayName("Analyze 'get/set', multiplicity 1, with bound type parameter")
    @Test
    public void test1b() {
        TypeInfo X = javaInspector.parse(INPUT1b);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        // run the shallow analyzer to detect the annotations
        AnnotatedApiParser annotatedApiParser = new AnnotatedApiParser();
        ShallowAnalyzer shallowAnalyzer = new ShallowAnalyzer(runtime, Element::annotations, true);
        shallowAnalyzer.go(List.of(X));

        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        VirtualFields vfX = vfc.compute(X);
        assertEquals("§m - T §t", vfX.toString());

        LinkComputer linkComputer = new LinkComputerImpl(javaInspector);

        MethodInfo get = X.findUniqueMethod("get", 0);
        MethodLinkedVariables mlvGet = linkComputer.doMethod(get);
        assertEquals("[] --> get←this.§t", mlvGet.toString());

        MethodInfo set = X.findUniqueMethod("set", 1);
        MethodLinkedVariables mlvSet = linkComputer.doMethod(set);
        assertEquals("[0:t→this*.§t] --> -", mlvSet.toString());

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
        assertEquals("/ - T §t", vfX.toString());

        LinkComputer linkComputer = new LinkComputerImpl(javaInspector);

        MethodInfo get = optional.findUniqueMethod("get", 0);
        MethodLinkedVariables mlvGet = linkComputer.doMethod(get);
        assertEquals("[] --> get←this.§t", mlvGet.toString());

        MethodInfo set = optional.findUniqueMethod("orElse", 1);
        MethodLinkedVariables mlvSet = linkComputer.doMethod(set);
        assertEquals("[-] --> orElse←this.§t,orElse←0:other", mlvSet.toString());

        MethodInfo orElseThrow = optional.findUniqueMethod("orElseThrow", 0);
        MethodLinkedVariables mlvOrElseThrow = linkComputer.doMethod(orElseThrow);
        assertEquals("[] --> orElseThrow←this.§t", mlvOrElseThrow.toString());

        MethodInfo stream = optional.findUniqueMethod("stream", 0);
        MethodLinkedVariables mlvStream = linkComputer.doMethod(stream);
        assertEquals("[] --> stream.§ts∋this.§t", mlvStream.toString());

        MethodInfo of = optional.findUniqueMethod("of", 1);
        MethodLinkedVariables mlvOf = linkComputer.doMethod(of);
        assertEquals("[-] --> of.§t←0:value", mlvOf.toString());

        MethodInfo orElseGet = optional.findUniqueMethod("orElseGet", 1);
        MethodLinkedVariables mlvOrElseGet = linkComputer.doMethod(orElseGet);
        assertEquals("[-] --> orElseGet←this.§t,orElseGet←Λ0:supplier", mlvOrElseGet.toString());
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
        MethodLinkedVariables mlvToArrayTs = linkComputer.doMethod(toArrayTs);
        assertEquals("[-] --> toArray.§ts⊆this.§es", mlvToArrayTs.toString());

        MethodInfo toArray = list.findUniqueMethod("toArray", 0);
        MethodLinkedVariables mlvToArray = linkComputer.doMethod(toArray);
        assertEquals("[] --> toArray.§$s⊆this.§es", mlvToArray.toString());

        MethodInfo addAll = list.findUniqueMethod("addAll", 1);
        MethodLinkedVariables mlvAddAll = linkComputer.doMethod(addAll);
        assertEquals("[0:collection.§es~this*.§es] --> -", mlvAddAll.toString());

        MethodInfo ofVarargs = list.methodStream().filter(mi ->
                        "of".equals(mi.name()) && mi.parameters().size() == 1 && mi.parameters().getFirst().isVarArgs())
                .findFirst().orElseThrow();
        assertEquals("java.util.List.of(E...)", ofVarargs.fullyQualifiedName());
        MethodLinkedVariables mlvOfVarargs = linkComputer.doMethod(ofVarargs);
        assertEquals("[-] --> of.§es⊆0:elements.§es", mlvOfVarargs.toString());

        MethodInfo of1 = list.methodStream().filter(mi ->
                        "of".equals(mi.name()) && mi.parameters().size() == 1 && !mi.parameters().getFirst().isVarArgs())
                .findFirst().orElseThrow();
        assertEquals("java.util.List.of(E)", of1.fullyQualifiedName());
        MethodLinkedVariables mlvOf1 = linkComputer.doMethod(of1);
        assertEquals("[-] --> of.§es∋0:e1", mlvOf1.toString());
        assertTrue(mlvOf1.modified().isEmpty());

        MethodInfo of2 = list.findUniqueMethod("of", 2);
        MethodLinkedVariables mlvOf2 = linkComputer.doMethod(of2);
        assertEquals("[-, -] --> of.§es∋0:e1,of.§es∋1:e2", mlvOf2.toString());

        MethodInfo add = list.findUniqueMethod("add", 1);
        MethodLinkedVariables mlvAdd = linkComputer.doMethod(add);
        assertEquals("[0:e∈this*.§es] --> -", mlvAdd.toString());

        MethodInfo set = list.findUniqueMethod("set", 2);
        MethodLinkedVariables mlvSet = linkComputer.doMethod(set);
        assertEquals("[-, 1:e∈this*.§es] --> set∈this*.§es", mlvSet.toString());

        MethodInfo subList = list.findUniqueMethod("subList", 2);
        MethodLinkedVariables mlvSubList = linkComputer.doMethod(subList);
        assertEquals("[-, -] --> subList.§es⊆this.§es,subList.§m≡this.§m", mlvSubList.toString());

        MethodInfo get = list.findUniqueMethod("get", 1);
        MethodLinkedVariables mlvGet = linkComputer.doMethod(get);
        assertEquals("[-] --> get∈this.§es", mlvGet.toString());

        MethodInfo contains = list.findUniqueMethod("contains", 1);
        MethodLinkedVariables mlvContains = linkComputer.doMethod(contains);
        assertEquals("[-] --> -", mlvContains.toString());

        // TODO should we make a distinction between getFirst and getLast?

        MethodInfo getFirst = list.findUniqueMethod("getFirst", 0);
        MethodLinkedVariables mlvGetFirst = linkComputer.doMethod(getFirst);
        assertEquals("[] --> getFirst∈this.§es", mlvGetFirst.toString());

        MethodInfo getLast = list.findUniqueMethod("getLast", 0);
        MethodLinkedVariables mlvGetLast = linkComputer.doMethod(getLast);
        assertEquals("[] --> getLast∈this.§es", mlvGetLast.toString());
    }

    @DisplayName("Analyze 'Collection', multiplicity 2, 1 type parameter")
    @Test
    public void test3b() {
        LinkComputer linkComputer = new LinkComputerImpl(javaInspector);
        TypeInfo collection = javaInspector.compiledTypesManager().getOrLoad(Collection.class);

        MethodInfo toArrayFunction = collection.methodStream()
                .filter(mi -> "toArray".equals(mi.name()) && mi.parameters().size() == 1
                              && mi.parameters().getFirst().parameterizedType().isFunctionalInterface())
                .findFirst().orElseThrow();
        assertEquals("java.util.Collection.toArray(java.util.function.IntFunction<T[]>)",
                toArrayFunction.fullyQualifiedName());
        MethodLinkedVariables mlvToArrayFunction = linkComputer.doMethod(toArrayFunction);
        // NOTE: this.§es rather than §ts, because of "force"  @Independent(hcReturnValue = true) on the method
        assertEquals("[-] --> toArray.§ts⊆this.§es", mlvToArrayFunction.toString());
    }

    @Test
    public void test3c() {
        LinkComputer linkComputer = new LinkComputerImpl(javaInspector);
        TypeInfo iterable = javaInspector.compiledTypesManager().getOrLoad(Iterable.class);

        MethodInfo forEach = iterable.findUniqueMethod("forEach", 1);
        MethodLinkedVariables mlvForEach = linkComputer.doMethod(forEach);
        assertEquals("[this.§ts⊇Λ0:action] --> -", mlvForEach.toString());
    }

    @DisplayName("Analyze 'Map', multiplicity 2, 2 type parameters")
    @Test
    public void test4() {
        LinkComputer linkComputer = new LinkComputerImpl(javaInspector);
        TypeInfo map = javaInspector.compiledTypesManager().getOrLoad(Map.class);

        // ---

        MethodInfo entrySet = map.findUniqueMethod("entrySet", 0);
        MethodLinkedVariables mlvEntrySet = linkComputer.doMethod(entrySet);
        assertEquals("[] --> entrySet.§kvs⊆this.§kvs,entrySet.§m≡this.§m", mlvEntrySet.toString());
        Link l1 = mlvEntrySet.ofReturnValue().stream().findFirst().orElseThrow();
        assertEquals("Type java.util.Map.Entry.KV[]", l1.from().parameterizedType().toString());

        // ---

        MethodInfo getOrDefault = map.findUniqueMethod("getOrDefault", 2);
        MethodLinkedVariables mlvGetOrDefault = linkComputer.doMethod(getOrDefault);
        assertEquals("[-, -] --> getOrDefault∈this.§kvs[-2],getOrDefault←1:defaultValue",
                mlvGetOrDefault.toString());

        Link l2 = mlvGetOrDefault.ofReturnValue().stream().findFirst().orElseThrow();
        assertEquals("Type param V", l2.from().parameterizedType().toString());
        assertEquals("this.§kvs[-2]", l2.to().toString());
        assertEquals("Type param V[]", l2.to().parameterizedType().toString());

        // ---

        MethodInfo get = map.findUniqueMethod("get", 1);
        MethodLinkedVariables mlvGet = linkComputer.doMethod(get);
        assertEquals("[-] --> get∈this.§kvs[-2]", mlvGet.toString());

        // ---

        MethodInfo keySet = map.findUniqueMethod("keySet", 0);
        MethodLinkedVariables mlvKeySet = linkComputer.doMethod(keySet);
        assertEquals("[] --> keySet.§ks⊆this.§kvs[-1],keySet.§m≡this.§m", mlvKeySet.toString());
        Link l3 = mlvKeySet.ofReturnValue().stream().findFirst().orElseThrow();
        assertEquals("this.§kvs[-1]", l3.to().toString());
        assertEquals("Type param K[]", l3.to().parameterizedType().toString());

        // ---

        MethodInfo values = map.findUniqueMethod("values", 0);
        MethodLinkedVariables mlvValues = linkComputer.doMethod(values);
        assertEquals("[] --> values.§vs⊆this.§kvs[-2],values.§m≡this.§m", mlvValues.toString());

        // ---

        MethodInfo forEachBi = map.findUniqueMethod("forEach", 1);
        assertEquals("java.util.Map.forEach(java.util.function.BiConsumer<? super K,? super V>)",
                forEachBi.fullyQualifiedName());
        MethodLinkedVariables mlvForEachBi = linkComputer.doMethod(forEachBi);
        assertEquals("[this.§kvs⊇Λ0:action] --> -", mlvForEachBi.toString());
    }

    @DisplayName("Analyze 'Stream', multiplicity 2, 1 type parameter")
    @Test
    public void test5() {
        LinkComputer linkComputer = new LinkComputerImpl(javaInspector);
        TypeInfo stream = javaInspector.compiledTypesManager().getOrLoad(Stream.class);
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        assertEquals("§m - T[] §ts", vfc.compute(stream).toString());

        MethodInfo findFirst = stream.findUniqueMethod("findFirst", 0);
        MethodLinkedVariables mlvFindFirst = linkComputer.doMethod(findFirst);
        assertEquals("[] --> findFirst.§t∈this.§ts", mlvFindFirst.toString());

        MethodInfo of = stream.methodStream()
                .filter(mi -> "of".equals(mi.name())
                              && mi.parameters().getFirst().parameterizedType().arrays() == 0)
                .findFirst().orElseThrow();
        MethodLinkedVariables mlvOf = linkComputer.doMethod(of);
        assertEquals("[-] --> of.§ts∋0:t", mlvOf.toString());

        MethodInfo collect = stream.findUniqueMethod("collect", 1);
        MethodLinkedVariables mlvCollect = linkComputer.doMethod(collect);
        // too complicated, not a functional interface
        assertEquals("[0:collector.§tars[-1]⊆this.§ts] --> -", mlvCollect.toString());

        MethodInfo generate = stream.findUniqueMethod("generate", 1);
        MethodLinkedVariables mlvGenerate = linkComputer.doMethod(generate);
        // Should we interpret the supplier as a multiplicity 1 or 2 source? Let's take 2 ~ Stream.map
        assertEquals("[-] --> generate.§ts⊆Λ0:s", mlvGenerate.toString());

        MethodInfo filter = stream.findUniqueMethod("filter", 1);
        MethodLinkedVariables mlvFilter = linkComputer.doMethod(filter);
        // there should be no lambda here, the filter cannot produce T elements
        // there is a lambda here, because the filter SHOULD not produce T elements (but it technically CAN)
        assertEquals("[this.§ts⊇Λ0:predicate] --> filter.§ts⊆this.§ts", mlvFilter.toString());

        MethodInfo toArrayFunction = stream.methodStream()
                .filter(mi -> "toArray".equals(mi.name()) && mi.parameters().size() == 1
                              && mi.parameters().getFirst().parameterizedType().isFunctionalInterface())
                .findFirst().orElseThrow();
        assertEquals("java.util.stream.Stream.toArray(java.util.function.IntFunction<A[]>)",
                toArrayFunction.fullyQualifiedName());
        MethodLinkedVariables mlvToArrayFunction = linkComputer.doMethod(toArrayFunction);
        // NOTE: this.§ts rather than §as, because of "force"  @Independent(hcReturnValue = true) on the method
        assertEquals("[-] --> toArray.§as⊆this.§ts", mlvToArrayFunction.toString());

        MethodInfo map = stream.findUniqueMethod("map", 1);
        MethodLinkedVariables mlvMap = linkComputer.doMethod(map);
        assertEquals("[-] --> map.§rs⊆Λ0:function", mlvMap.toString());
    }

    @DisplayName("Analyze 'ArrayList' constructors, multiplicity 2, 1 type parameter")
    @Test
    public void test6() {
        LinkComputer linkComputer = new LinkComputerImpl(javaInspector);
        TypeInfo collection = javaInspector.compiledTypesManager().getOrLoad(Collection.class);
        TypeInfo arrayList = javaInspector.compiledTypesManager().getOrLoad(ArrayList.class);
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        assertEquals("§m - E[] §es", vfc.compute(arrayList).toString());

        MethodInfo c1 = arrayList.findConstructor(collection);
        MethodLinkedVariables mlvC1 = linkComputer.doMethod(c1);
        assertEquals("[0:c.§es⊇this*.§es] --> -", mlvC1.toString());

    }

    @Language("java")
    private static final String INPUT7 = """
            package a.b;
            import org.e2immu.annotation.Independent;
            import java.util.Collection;
            public interface X {
                <T> void add(@Independent(hcParameters = {1}) Collection<T> c, T t);
            
                void test(); // for this example, X should not be a functional interface
            }
            """;

    @DisplayName("Analyze simpler version of Collections.addAll")
    @Test
    public void test7() {
        TypeInfo X = javaInspector.parse(INPUT7);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        ShallowAnalyzer shallowAnalyzer = new ShallowAnalyzer(runtime, Element::annotations, true);
        shallowAnalyzer.go(List.of(X));

        MethodInfo add = X.findUniqueMethod("add", 2);

        LinkComputer linkComputer = new LinkComputerImpl(javaInspector);

        MethodLinkedVariables mlvAdd = linkComputer.doMethod(add);
        assertEquals("[-, 1:t∈0:c*.§ts] --> -", mlvAdd.toString());
    }

    @DisplayName("Analyze 'Collections.addAll(...), extra complication: varargs")
    @Test
    public void test8() {
        LinkComputer linkComputer = new LinkComputerImpl(javaInspector);
        TypeInfo collections = javaInspector.compiledTypesManager().getOrLoad(Collections.class);
        MethodInfo addAll = collections.findUniqueMethod("addAll", 2);
        assertEquals("java.util.Collections.addAll(java.util.Collection<? super T>,T...)",
                addAll.fullyQualifiedName());
        MethodLinkedVariables mlvC1 = addAll.analysis().getOrCreate(METHOD_LINKS, () -> linkComputer.doMethod(addAll));
        assertEquals("[-, 1:elements.§ts⊆0:c*.§ts] --> -", mlvC1.toString());
    }

    @Language("java")
    private static final String INPUT9 = """
            package a.b;
            import org.e2immu.annotation.Independent;
            import java.util.List;
            public class C<X> {
                record R<V>(V v) { }
                @Independent(hc = true)
                abstract List<R<X>> method(@Independent(hc = true) List<X> list);
            }
            """;

    @DisplayName("wrapped")
    @Test
    public void test9() {
        TypeInfo C = javaInspector.parse(INPUT9);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        ShallowAnalyzer shallowAnalyzer = new ShallowAnalyzer(runtime, Element::annotations, false);
        shallowAnalyzer.go(List.of(C));

        MethodInfo method = C.findUniqueMethod("method", 1);
        assertSame(INDEPENDENT_HC, method.analysis().getOrNull(PropertyImpl.INDEPENDENT_METHOD, ValueImpl.IndependentImpl.class));
        assertSame(INDEPENDENT_HC, method.parameters().getFirst().analysis().getOrNull(PropertyImpl.INDEPENDENT_PARAMETER,
                ValueImpl.IndependentImpl.class));

        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        assertEquals("§m - X[] §xs", vfc.compute(C).toString());

        LinkComputer linkComputer = new LinkComputerImpl(javaInspector);

        MethodLinkedVariables mlv = linkComputer.doMethod(method);
        assertEquals("[0:list*.§xs~this*.§xs] --> method.§xs⊆this*.§xs", mlv.toString());
    }


    @Language("java")
    private static final String INPUT9b = """
            package a.b;
            import org.e2immu.annotation.Independent;import org.e2immu.annotation.NotModified;
            import java.util.List;
            public abstract class C {
                record R<V>(V v) { }
                @Independent(hc = true)
                abstract <X> List<R<X>> method(@Independent(hcReturnValue = true) @NotModified List<X> list);
            }
            """;

    @DisplayName("wrapped, method type parameter")
    @Test
    public void test9b() {
        TypeInfo C = javaInspector.parse(INPUT9b);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        ShallowAnalyzer shallowAnalyzer = new ShallowAnalyzer(runtime, Element::annotations, false);
        shallowAnalyzer.go(List.of(C));

        MethodInfo method = C.findUniqueMethod("method", 1);
        assertSame(INDEPENDENT_HC, method.analysis().getOrNull(PropertyImpl.INDEPENDENT_METHOD, ValueImpl.IndependentImpl.class));
        //method.parameters().getFirst().analysis().set(PropertyImpl.INDEPENDENT_PARAMETER,
         //       new ValueImpl.IndependentImpl(1, Map.of(-1, 1)));

        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        assertEquals("§m - C §0", vfc.compute(C).toString());
        // the §0 is "some anonymous virtual field"
        LinkComputer linkComputer = new LinkComputerImpl(javaInspector);

        MethodLinkedVariables mlv = linkComputer.doMethod(method);
        assertEquals("[-] --> method.§xs⊆0:list.§xs", mlv.toString());
    }
}