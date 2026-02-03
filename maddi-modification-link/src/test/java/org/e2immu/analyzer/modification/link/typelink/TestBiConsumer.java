package org.e2immu.analyzer.modification.link.typelink;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TestBiConsumer extends CommonTest {

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

        MethodInfo method = C.findUniqueMethod("method", 2);

    }

}
