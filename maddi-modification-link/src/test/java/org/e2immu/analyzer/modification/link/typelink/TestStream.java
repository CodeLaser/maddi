package org.e2immu.analyzer.modification.link.typelink;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.Links;
import org.e2immu.analyzer.modification.link.MethodLinkedVariables;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.link.impl.LinksImpl;
import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.AbstractMap;
import java.util.stream.Stream;

import static org.e2immu.analyzer.modification.link.impl.LinkComputerImpl.VARIABLES_LINKED_TO_OBJECT;
import static org.e2immu.analyzer.modification.link.impl.LinksImpl.LINKS;
import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestStream extends CommonTest {
    @Language("java")
    private static final String INPUT1a = """
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
            }
            """;

    @DisplayName("MR identity function")
    @Test
    public void test1a() {
        TypeInfo C = javaInspector.parse(INPUT1a);

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
        assertEquals("stream2.§xs~0:list.§xs,stream2.§xs~stream1.§xs", lvStream21.toString());

        VariableData vd2 = VariableDataImpl.of(method1.methodBody().statements().get(2));
        VariableInfo viResult = vd2.variableInfo("result");
        Links lvResult = viResult.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("result.§xs~0:list.§xs,result.§xs~stream1.§xs,result.§xs~stream2.§xs", lvResult.toString());

        assertEquals("[-] --> method1.§xs~0:list.§xs", mlv1.toString());

        MethodInfo method = C.findUniqueMethod("method", 1);
        MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));
        //FIXME chain fails assertEquals("[-] --> method1.§xs~0:list.§xs", mlv.toString());
    }

    @Language("java")
    private static final String INPUT1b = """
            package a.b;
            import java.util.List;
            import java.util.stream.Stream;
            public class C {
                public <Y> Y first(Y[] ys)  { return ys[0]; }
                public <X> List<X> method(List<X[]> list) {
                    return list.stream().map(this::first).toList();
                }
                public <X> List<X> method1(List<X[]> list) {
                    Stream<X[]> stream1 = list.stream();
                    Stream<X> stream2 = stream1.map(this::first);
                    List<X> result = stream2.toList();
                    return result;
                }
            }
            """;

    @DisplayName("MR take first element")
    @Test
    public void test1b() {
        TypeInfo C = javaInspector.parse(INPUT1b);

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
        assertEquals("result.§xs~stream2.§xs", lvResult.toString());

        assertEquals("[-] --> method1.§xs<0:list.§xss", mlv1.toString());

        MethodInfo method = C.findUniqueMethod("method", 1);
        MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));
        //FIXME chain fails assertEquals("[-] --> method1.§xs~0:list.§xs", mlv.toString());
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
