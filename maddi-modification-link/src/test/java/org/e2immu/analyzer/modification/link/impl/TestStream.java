package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

public class TestStream  extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.List;
            public class X<T> {
                List<T> list;
                T large(int n) {
                    return list.stream().filter(t->t.toString().length()>n).findFirst().orElseThrow();
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
        MethodInfo large = X.findUniqueMethod("large", 1);
        linkComputer.doMethod(large);
    }
}