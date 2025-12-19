package org.e2immu.analyzer.modification.link.typelink;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.prepwork.variable.Links;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.link.impl.LinksImpl;
import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.link.vf.VirtualFields;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.AbstractMap;
import java.util.stream.Stream;

import static org.e2immu.analyzer.modification.link.impl.LinkComputerImpl.VARIABLES_LINKED_TO_OBJECT;
import static org.e2immu.analyzer.modification.link.impl.LinksImpl.LINKS;
import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestStream extends CommonTest {
    @Language("java")
    private static final String INPUT_IDENTITY = """
            package a.b;
            import java.util.List;
            import java.util.stream.Stream;
            public class C {
                public <Y> Y identity(Y y)  { return y; }
                public <X> List<X> method(List<X> list) {
                    return list.stream().map(this::identity).toList();
                }
                public <X> List<X> method1(List<X> list) {
                    Stream<X> stream1 = list.stream();
                    Stream<X> stream2 = stream1.map(this::identity);
                    List<X> result = stream2.toList();
                    return result;
                }
                public <X> List<X> method2(List<X> list) {
                    Stream<X> xStream = list.stream().map(this::identity);
                    return xStream.toList();
                }
                public <X> List<X> method3(List<X> list) {
                    List<X> res = list.stream().map(this::identity).toList();
                    return res;
                }
            }
            """;

    @DisplayName("identity function")
    @Test
    public void testIdentity() {
        TypeInfo C = javaInspector.parse(INPUT_IDENTITY);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);

        MethodInfo method1 = C.findUniqueMethod("method1", 1);
        MethodLinkedVariables mlv1 = method1.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method1));

        VariableData vd0 = VariableDataImpl.of(method1.methodBody().statements().getFirst());
        VariableInfo viStream10 = vd0.variableInfo("stream1");
        Links lvStream10 = viStream10.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("stream1.§xs~0:list.§xs", lvStream10.toString());

        VariableData vd1 = VariableDataImpl.of(method1.methodBody().statements().get(1));
        VariableInfo viStream21 = vd1.variableInfo("stream2");
        Links lvStream21 = viStream21.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("stream2.§xs==stream1.§xs,stream2.§xs~0:list.§xs,stream2~stream1", lvStream21.toString());

        VariableData vd2 = VariableDataImpl.of(method1.methodBody().statements().get(2));
        VariableInfo viResult = vd2.variableInfo("result");
        Links lvResult = viResult.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("result.§xs~0:list.§xs,result.§xs~stream1.§xs,result.§xs~stream2.§xs", lvResult.toString());

        assertEquals("[-] --> method1.§xs~0:list.§xs", mlv1.toString());

        MethodInfo method2 = C.findUniqueMethod("method2", 1);
        MethodLinkedVariables mlv2 = method2.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method2));
        assertEquals("[-] --> method2.§xs~0:list.§xs", mlv2.toString());

        MethodInfo method3 = C.findUniqueMethod("method3", 1);
        {
            LocalVariableCreation lvc = (LocalVariableCreation) method3.methodBody().statements().getFirst();
            MethodCall mc = (MethodCall) lvc.localVariable().assignmentExpression();
            assertNotNull(mc);
            assertEquals("java.util.List<X>", mc.parameterizedType().detailedString());
            assertEquals("Type java.util.List<X>", mc.concreteReturnType().toString());
            MethodCall mc2 = (MethodCall) mc.object();
            assertEquals("Type java.util.stream.Stream<X>", mc2.concreteReturnType().toString());
        }
        MethodLinkedVariables mlv3 = method3.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method3));

        VariableData vd30 = VariableDataImpl.of(method3.methodBody().statements().getFirst());
        VariableInfo viRes = vd30.variableInfo("res");
        Links lvRes = viRes.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("res.§xs~0:list.§xs", lvRes.toString());

        assertEquals("[-] --> method3.§xs~0:list.§xs", mlv3.toString());

        MethodInfo method = C.findUniqueMethod("method", 1);
        MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));
        assertEquals("[-] --> method.§xs~0:list.§xs", mlv.toString());
    }

    @Language("java")
    private static final String INPUT_TAKE_FIRST = """
            package a.b;
            import java.util.List;
            import java.util.stream.Stream;
            abstract class C {
                <Y> Y first(Y[] ys)  { return ys[0]; }
                <X> List<X> method(List<X[]> list) {
                    return list.stream().map(this::first).toList();
                }
                <X> List<X> method1(List<X[]> list) {
                    Stream<X[]> stream1 = list.stream();
                    Stream<X> stream2 = stream1.map(this::first);
                    List<X> result = stream2.toList();
                    return result;
                }
                 <X> List<X> method3(List<X[]> list) {
                    Stream<X> stream = list.stream().map(this::first);
                    return stream.toList();
                }
                abstract <H> H[] hiddenContent(Stream<H> h);
                abstract <H> Stream<H> streamFromHiddenContent(H[] h);
                abstract <H> H[] createHcArray(int n);
            
                <X> List<X> method2(List<X[]> list) {
                    Stream<X[]> stream1 = list.stream();
                    // here starts replacement code
                    X[][] hcSource = hiddenContent(stream1);         // stream1.§xss
                    X[] hcTarget = createHcArray(hcSource.length);   // stream2.§xs
                    int i=0;
                    for(X[] element: hcSource) {
                        hcTarget[i] = first(element); // stream2.§xs == stream1.§xss[-1] OR stream2.§xs < stream1.§xss
                        ++i;
                    }
                    Stream<X> stream2 = streamFromHiddenContent(hcTarget);
                    // here ends replacement code
                    List<X> result = stream2.toList();
                    return result;
                }
            
                // mlv: mapApply.§hs < 0:in.§hss
                <H> Stream<H> mapApply(Stream<H[]> in) {
                    H[][] hcSource = hiddenContent(in); // stream1.§xss
                    H[] hcTarget = createHcArray(hcSource.length);   // stream2.§xs
                    int i=0;
                    for(H[] element: hcSource) {
                        hcTarget[i] = first(element); // stream2.§xs == stream1.§xss[-1] OR stream2.§xs < stream1.§xss
                        ++i;
                    }
                    return streamFromHiddenContent(hcTarget);
                }
            }
            """;

    @DisplayName("T[]->T, take first element")
    @Test
    public void testTakeFirst() {
        TypeInfo C = javaInspector.parse(INPUT_TAKE_FIRST);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);

        MethodInfo method1 = C.findUniqueMethod("method1", 1);
        MethodLinkedVariables mlv1 = method1.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method1));

        VariableData vd0 = VariableDataImpl.of(method1.methodBody().statements().getFirst());
        VariableInfo viStream10 = vd0.variableInfo("stream1");
        Links lvStream10 = viStream10.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("stream1.§xss~0:list.§xss", lvStream10.toString());

        VariableData vd1 = VariableDataImpl.of(method1.methodBody().statements().get(1));
        VariableInfo viStream21 = vd1.variableInfo("stream2");
        Links lvStream21 = viStream21.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("stream2.§xs<0:list.§xss,stream2.§xs<stream1.§xss", lvStream21.toString());

        VariableData vd2 = VariableDataImpl.of(method1.methodBody().statements().get(2));
        VariableInfo viResult = vd2.variableInfo("result");
        Links lvResult = viResult.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("result.§xs<0:list.§xss,result.§xs<stream1.§xss,result.§xs~stream2.§xs,result<0:list,result<stream1", lvResult.toString());

        assertEquals("[-] --> method1.§xs<0:list.§xss,method1<0:list", mlv1.toString());

        MethodInfo method3 = C.findUniqueMethod("method3", 1);
        MethodLinkedVariables mlv3 = method3.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method3));

        VariableData vd30 = VariableDataImpl.of(method3.methodBody().statements().getFirst());
        VariableInfo vi3Stream = vd30.variableInfo("stream");
        Links lv3Stream = vi3Stream.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("stream.§xs<0:list.§xss", lv3Stream.toString());
        assertEquals("[-] --> method3.§xs<0:list.§xss,method3<0:list", mlv3.toString());

        MethodInfo method = C.findUniqueMethod("method", 1);
        MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));

        assertEquals("[-] --> method.§xs<0:list.§xss", mlv.toString());
    }

    @Language("java")
    private static final String INPUT_WRAP = """
            package a.b;
            import java.util.List;
            import java.util.stream.Stream;
            class C<X> {
                record R<V>(V v) { }
                <Y> R<Y> wrap(Y y)  { return new R<>(y); }
                List<R<X>> method(List<X> list) {
                    return list.stream().map(this::wrap).toList();
                }
                List<R<X>> method1(List<X> list) {
                    Stream<X> stream1 = list.stream();
                    Stream<R<X>> stream2 = stream1.map(this::wrap);
                    List<R<X>> result = stream2.toList();
                    return result;
                }
            }
            """;

    // because the type parameters of R<X> and X[] are the same, we do not introduce a container type
    // stream2.§xs is of type X[]
    @DisplayName("T->R(T), wrap")
    @Test
    public void testWrap() {
        TypeInfo C = javaInspector.parse(INPUT_WRAP);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);

        MethodInfo wrap = C.findUniqueMethod("wrap", 1);
        MethodLinkedVariables mlvWrap = wrap.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(wrap));
        assertEquals("[-] --> wrap.§y==0:y", mlvWrap.toString());

        MethodInfo method1 = C.findUniqueMethod("method1", 1);
        MethodLinkedVariables mlv1 = method1.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method1));

        VariableData vd0 = VariableDataImpl.of(method1.methodBody().statements().getFirst());
        VariableInfo viStream10 = vd0.variableInfo("stream1");
        Links lvStream10 = viStream10.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("stream1.§xs~0:list.§xs", lvStream10.toString());

        VariableData vd1 = VariableDataImpl.of(method1.methodBody().statements().get(1));
        VariableInfo viStream21 = vd1.variableInfo("stream2");
        Links lvStream21 = viStream21.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        // wrapping in R is invisible
        assertEquals("stream2.§xs==stream1.§xs,stream2.§xs~0:list.§xs,stream2~stream1", lvStream21.toString());

        VariableData vd2 = VariableDataImpl.of(method1.methodBody().statements().get(2));
        VariableInfo viResult = vd2.variableInfo("result");
        Links lvResult = viResult.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("result.§xs~0:list.§xs,result.§xs~stream1.§xs,result.§xs~stream2.§xs", lvResult.toString());

        assertEquals("[-] --> method1.§xs~0:list.§xs", mlv1.toString());

        MethodInfo method = C.findUniqueMethod("method", 1);
        MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));
        assertEquals("[-] --> method.§xs~0:list.§xs", mlv.toString());
    }


    @Language("java")
    private static final String INPUT_WRAP2 = """
            package a.b;
            import java.util.List;
            import java.util.stream.Stream;
            class C<X> {
                record R<V>(V v) { }
                <Y> List<R<Y>> wrap(Y y)  { return List.of(new R<>(y)); }
                List<List<R<X>>> method(List<X> list) {
                    return list.stream().map(this::wrap).toList();
                }
                List<List<R<X>>> method1(List<X> list) {
                    Stream<X> stream1 = list.stream();
                    Stream<List<R<X>>> stream2 = stream1.map(this::wrap);
                    List<List<R<X>>> result = stream2.toList();
                    return result;
                }
            }
            """;

    @DisplayName("T->R[](T), wrapped 2x")
    @Test
    public void testWrap2() {
        TypeInfo C = javaInspector.parse(INPUT_WRAP2);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);

        MethodInfo wrap = C.findUniqueMethod("wrap", 1);
        MethodLinkedVariables mlvWrap = wrap.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(wrap));
        assertEquals("[-] --> wrap.§es>0:y", mlvWrap.toString());

        MethodInfo method1 = C.findUniqueMethod("method1", 1);
        MethodLinkedVariables mlv1 = method1.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method1));

        VariableData vd0 = VariableDataImpl.of(method1.methodBody().statements().getFirst());
        VariableInfo viStream10 = vd0.variableInfo("stream1");
        Links lvStream10 = viStream10.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("stream1.§xs~0:list.§xs", lvStream10.toString());

        VariableData vd1 = VariableDataImpl.of(method1.methodBody().statements().get(1));
        VariableInfo viStream21 = vd1.variableInfo("stream2");

        VirtualFieldComputer virtualFieldComputer = new VirtualFieldComputer(javaInspector);
        VirtualFields vfStream2 = virtualFieldComputer.compute(viStream21.variable().parameterizedType(), false).virtualFields();
        assertEquals("§m - X[][] §xss", vfStream2.toString());

        Links lvStream21 = viStream21.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        // wrapping in another list is visible!
        assertEquals("stream2.§xss>0:list.§xs,stream2.§xss>stream1.§xs", lvStream21.toString());

        VariableData vd2 = VariableDataImpl.of(method1.methodBody().statements().get(2));
        VariableInfo viResult = vd2.variableInfo("result");
        Links lvResult = viResult.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("result.§xss>0:list.§xs,result.§xss>stream1.§xs,result.§xss~stream2.§xss", lvResult.toString());

        assertEquals("[-] --> method1.§xss>0:list.§xs,method1>0:list", mlv1.toString());

        MethodInfo method = C.findUniqueMethod("method", 1);
        MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));
        assertEquals("[-] --> method.§xss>0:list.§xs", mlv.toString());
    }

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.AbstractMap;
            import java.util.Map;
            import java.util.Set;import java.util.stream.Collectors;
            import java.util.stream.Stream;
            public class C<X, Y> {
                private Map.Entry<Y, X> swap(Map.Entry<X, Y> entry) {
                    return new AbstractMap.SimpleEntry<>(entry.getValue(), entry.getKey());
                }
                public void reverse(Map<X, Y> map) {
                    Set<Map.Entry<X,Y>> entries = map.entrySet();
                    Stream<Map.Entry<X,Y>> stream1 = entries.stream();
                    Stream<Map.Entry<Y,X>> stream2 = stream1.map(this::swap);
                }
            }
            """;

    @Disabled
    @DisplayName("MR to instance function swap")
    @Test
    public void test1() {
        TypeInfo C = javaInspector.parse(INPUT1);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(C);

        MethodInfo swap = C.findUniqueMethod("swap", 1);

        // test SimpleEntry constructor x, y
        TypeInfo simpleEntry = javaInspector.compiledTypesManager().get(AbstractMap.SimpleEntry.class);
        MethodInfo constructor1 = simpleEntry.findConstructor(2);
        assertEquals("java.util.AbstractMap.SimpleEntry.<init>(K,V)", constructor1.fullyQualifiedName());
        MethodLinkedVariables tlvConstructor1 = constructor1.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[0:key==this.§kv.§k, 1:value==this.§kv.§v] --> -", tlvConstructor1.toString());

        MethodLinkedVariables tlvSwap = swap.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[-] --> swap.§yx.§x==0:entry.§xy.§x,swap.§yx.§y==0:entry.§xy.§y", tlvSwap.toString());

        // start reverse
        MethodInfo reverse = C.findUniqueMethod("reverse", 1);

        VariableData vd0 = VariableDataImpl.of(reverse.methodBody().statements().getFirst());
        VariableInfo viEntries0 = vd0.variableInfo("entries");
        Links tlvEntries0 = viEntries0.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("entries.§m==0:map.§m,entries.§xys~0:map.§xys,entries~0:map", tlvEntries0.toString());

        Statement reverse1 = reverse.methodBody().statements().get(1);
        VariableData vd1 = VariableDataImpl.of(reverse1);
        VariableInfo viStream1 = vd1.variableInfo("stream1");

        MethodCall mcReverse1 = (MethodCall) ((LocalVariableCreation) reverse1).localVariable().assignmentExpression();
        Value.VariableBooleanMap tlvMcReverse1 = mcReverse1.analysis().getOrDefault(VARIABLES_LINKED_TO_OBJECT,
                ValueImpl.VariableBooleanMapImpl.EMPTY);
        assertEquals("a.b.C.reverse(java.util.Map<X,Y>):0:map=false, entries=true, stream1=false",
                nice(tlvMcReverse1.map()));

        Links tlvStream1 = viStream1.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("stream1.§xys~0:map.§xys,stream1.§xys~entries.§xys", tlvStream1.toString());

        TypeInfo stream = javaInspector.compiledTypesManager().get(Stream.class);
        MethodInfo map = stream.findUniqueMethod("map", 1);
        MethodLinkedVariables tlvMap = map.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(map));
        assertEquals("[-] --> map.§rs~Λ0:function", tlvMap.toString());

        Statement reverse2 = reverse.methodBody().statements().get(2);
        VariableData vd2 = VariableDataImpl.of(reverse2);
        VariableInfo viStream2 = vd2.variableInfo("stream2");
        Links tlvStream2 = viStream2.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("""
                the result of "upscaling"
                
                §yx.§x == §xy.§x   ->    $yx[-2].§xs == §xy[-1].§xs
                
                and combining these 2 gives   $yxs == $xys
                
                so we want, eventually,
                
                stream2.§yxs[-2].§xs~stream1.§xys[-1].§xs,\
                stream2.§yxs[-1].§ys~stream1.§xys[-2].§ys,\
                stream2.§yxs~stream1.§xys
                """, tlvStream2.toString());

        MethodCall mcReverse2 = (MethodCall) ((LocalVariableCreation) reverse2).localVariable().assignmentExpression();
        Value.VariableBooleanMap tlvMcReverse2 = mcReverse2.analysis().getOrDefault(VARIABLES_LINKED_TO_OBJECT,
                ValueImpl.VariableBooleanMapImpl.EMPTY);
        // These are the OBJECTS of the function
        assertEquals("""
                a.b.C.reverse(java.util.Map<X,Y>):0:map=false, entries=false, stream1=true, stream2=false\
                """, tlvMcReverse2.toString());

        MethodLinkedVariables mlvReverse = reverse.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("stream2.§yxs~0:map.§xys", mlvReverse.toString());
    }
}
