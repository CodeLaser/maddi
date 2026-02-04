package org.e2immu.analyzer.modification.link.typelink;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestConsumers extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.function.BiConsumer;
            import java.util.function.Supplier;
            public class C {
                interface SourceProvider extends Supplier<String> { }
                static class Builder {
                    void setSource(String source);
                 }
                void method(String in, Builder b, BiConsumer<SourceProvider, Builder> consumer) {
                    consumer.accept(in, b);
                }
                void call(Builder builder) {
                    method("a", builder, (sp, b)-> {
                        b.setSource(sp.get());
                    });
                }
            }
            """;

    @DisplayName("example of bi-consumer that does not 'receive' a stream, but only one element")
    @Test
    public void test1() {
        TypeInfo C = javaInspector.parse(INPUT1);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);

        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(C);

        MethodInfo method = C.findUniqueMethod("method", 3);
        assertEquals("[-, -, 2:consumer*↗$_afi0] --> -",
                method.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class).toString());

    }


    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.Map;
            import java.util.stream.Stream;
            public class C {
                interface VariableInfo { String variableName(); String links(); } // not a functional interface
                interface VariableInfoContainer { VariableInfo variableInfo(); String name(); }
                static class VariableData {
                    Map<String, VariableInfoContainer> map;
                    Stream<VariableInfo> variableInfoStream() {
                        return map.entrySet().stream()
                        .filter(e -> e.getKey().startsWith("$"))
                        .map(e -> e.getValue().variableInfo());
                    }
                     Stream<VariableInfo> variableInfoStream2() {
                        Stream<Map.Entry<String,VariableInfoContainer>> stream = map.entrySet().stream();
                        Stream<Map.Entry<String,VariableInfoContainer>> filtered = stream.filter(e -> e.getKey().startsWith("$"));
                        Stream<VariableInfo> mapped = filtered.map(e -> e.getValue().variableInfo());
                        return mapped;
                    }
                 }
                void method(VariableData vd) {
                   vd.variableInfoStream().forEach(vi -> {
                       System.out.println(vi);
                       System.out.println("?");
                   });
                }
            }
            """;

    @Disabled("working on it")
    @DisplayName("example of consumer with ∩ rather than ∈ link")
    @Test
    public void test2() {
        TypeInfo C = javaInspector.parse(INPUT2);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);

        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(C);

        TypeInfo vd = C.findSubType("VariableData");
        MethodInfo variableInfoStream2 = vd.findUniqueMethod("variableInfoStream2", 0);
        VariableData vd0 = VariableDataImpl.of(variableInfoStream2.methodBody().statements().getFirst());
        VariableInfo stream0 = vd0.variableInfo("stream");
        assertEquals("stream.§$$s⊆this.map.§$$s", stream0.linkedVariables().toString());
        VariableData vd1 = VariableDataImpl.of(variableInfoStream2.methodBody().statements().get(1));
        VariableInfo filtered1 = vd1.variableInfo("filtered");
        assertEquals("filtered.§$$s⊆stream.§$$s,filtered∩0:e", filtered1.linkedVariables().toString());
        // filtered.§$$s⊆this.map.§$$s,filtered.§$$s⊆stream.§$$s,filtered∩0:e would be without efficiency dropping
        VariableData vd2 = VariableDataImpl.of(variableInfoStream2.methodBody().statements().get(2));
        VariableInfo mapped2 = vd2.variableInfo("mapped");
        assertEquals("""
                mapped.§$s≤this.map.§$$s,mapped.§$s≤filtered.§$$s,mapped.§$s≤stream.§$$s\
                """, mapped2.linkedVariables().toString());

        VariableData vd3 = VariableDataImpl.of(variableInfoStream2.methodBody().statements().get(3));
        VariableInfo rv = vd3.variableInfo("a.b.C.VariableData.variableInfoStream2()");
        assertEquals("variableInfoStream2.§$s≤this.map.§$$s,variableInfoStream2.§$s∩0:e",
                rv.linkedVariables().toString());

        assertEquals("[] --> variableInfoStream2.§$s≤this.map.§$$s",
                variableInfoStream2.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class).toString());


        MethodInfo variableInfoStream = vd.findUniqueMethod("variableInfoStream", 0);
        assertEquals("[] --> variableInfoStream.§$s≤this.map.§$$s",
                variableInfoStream.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class).toString());

        MethodInfo method = C.findUniqueMethod("method", 1);
        assertEquals("[-, -, 2:consumer*↗$_afi0] --> -",
                method.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class).toString());
    }

}
