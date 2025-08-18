package org.e2immu.analyzer.modification.prepwork.hcs;

import org.e2immu.analyzer.modification.prepwork.CommonTest;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.hct.ComputeHiddenContent;
import org.e2immu.analyzer.modification.prepwork.hct.HiddenContentTypes;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestStream extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.stream.Stream;
            import java.util.Optional;
            class X {
                static class M { private int i; int getI() { return i; } void setI(int i) { this.i = i; } }
            
                static Stream<M> m3(Stream<M> stream) {
                    return stream.filter(m -> m.i == 3);
                }
            }
            """;

    @DisplayName("basics")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        PrepAnalyzer prepAnalyzer = new PrepAnalyzer(runtime);
        prepAnalyzer.initialize(javaInspector.compiledTypesManager().typesLoaded());
        prepAnalyzer.doPrimaryType(X);

        TypeInfo stream = javaInspector.compiledTypesManager().get(Stream.class);
        HiddenContentTypes hctStream = stream.analysis().getOrDefault(HiddenContentTypes.HIDDEN_CONTENT_TYPES, HiddenContentTypes.NO_VALUE);
        assertEquals("0=T, 1=Stream", hctStream.detailedSortedTypes());
        MethodInfo filter = stream.findUniqueMethod("filter", 1);

        HiddenContentTypes hctFilter = filter.analysis().getOrDefault(HiddenContentTypes.HIDDEN_CONTENT_TYPES, HiddenContentTypes.NO_VALUE);
        assertEquals("0=T, 1=Stream - 2=Predicate", hctFilter.detailedSortedTypes());
        HiddenContentSelector hcsFilter = filter.analysis().getOrDefault(HiddenContentSelector.HCS_METHOD, HiddenContentSelector.NONE);
        assertEquals("0=0,1=*", hcsFilter.detailed());
    }
}
