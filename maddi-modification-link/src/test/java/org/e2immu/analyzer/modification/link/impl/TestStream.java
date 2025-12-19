package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestStream extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.List;
            import java.util.Optional;import java.util.stream.Stream;
            
            public class X<T> {
                List<T> list;
                T large1(int n) {
                    return list.stream().filter(t->t.toString().length()>n).findFirst().orElseThrow();
                }
                T large2(int n) {
                    Stream<T> stream = list.stream();
                    Stream<T> filtered = stream.filter(t->t.toString().length()>n);
                    Optional<T> first = filtered.findFirst();
                    T orElse = first.orElseThrow();
                    return orElse;
                }
                T large3(int n) {
                    Stream<T> stream = list.stream();
                    Stream<T> filtered = stream.filter(t->t.toString().length()>n);
                    Optional<T> first = filtered.findFirst();
                    return first.orElseThrow();
                }
                T large4(int n) {
                    Stream<T> stream = list.stream();
                    Stream<T> filtered = stream.filter(t->t.toString().length()>n);
                    return filtered.findFirst().orElseThrow();
                }
            }
            """;

    @DisplayName("first stream chain")
    @Test
    public void test1() {
        LinkComputer linkComputer = new LinkComputerImpl(javaInspector);
        TypeInfo list = javaInspector.compiledTypesManager().getOrLoad(List.class);
        linkComputer.doPrimaryType(list);
        TypeInfo stream = javaInspector.compiledTypesManager().getOrLoad(Stream.class);
        linkComputer.doPrimaryType(stream);

        TypeInfo X = javaInspector.parse(INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        MethodInfo large2 = X.findUniqueMethod("large2", 1);
        MethodLinkedVariables mlvLarge2 = linkComputer.doMethod(large2);
        VariableData vd0 = VariableDataImpl.of(large2.methodBody().statements().getFirst());
        VariableInfo viStream = vd0.variableInfo("stream");
        assertEquals("stream.§ts~this.list.§ts", viStream.linkedVariables().toString());

        VariableData vd1 = VariableDataImpl.of(large2.methodBody().statements().get(1));
        VariableInfo viFiltered = vd1.variableInfo("filtered");
        assertEquals("filtered.§ts~stream.§ts,filtered.§ts~this.list.§ts",
                viFiltered.linkedVariables().toString());

        VariableData vd2 = VariableDataImpl.of(large2.methodBody().statements().get(2));
        VariableInfo viFirst = vd2.variableInfo("first");
        assertEquals("""
                first.§t<filtered.§ts,first.§t<stream.§ts,first.§t<this.list.§ts\
                """, viFirst.linkedVariables().toString());

        VariableData vd3 = VariableDataImpl.of(large2.methodBody().statements().get(3));
        VariableInfo viOrElse = vd3.variableInfo("orElse");
        assertEquals("orElse<filtered.§ts,orElse<stream.§ts,orElse<this.list.§ts,orElse==first.§t",
                viOrElse.linkedVariables().toString());

        assertEquals("[-] --> large2<this.list.§ts", mlvLarge2.toString());

        MethodInfo large1 = X.findUniqueMethod("large1", 1);
        MethodLinkedVariables mlvLarge1 = linkComputer.doMethod(large1);
        assertEquals("[-] --> large1<this.list.§ts", mlvLarge1.toString());

        MethodInfo large4 = X.findUniqueMethod("large4", 1);
        MethodLinkedVariables mlvLarge4 = linkComputer.doMethod(large4);
        assertEquals("[-] --> large4<this.list.§ts", mlvLarge4.toString());

        MethodInfo large3 = X.findUniqueMethod("large3", 1);
        MethodLinkedVariables mlvLarge3 = linkComputer.doMethod(large3);
        assertEquals("[-] --> large3<this.list.§ts", mlvLarge3.toString());

    }
}