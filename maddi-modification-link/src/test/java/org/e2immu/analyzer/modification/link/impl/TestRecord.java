package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.MethodLinkedVariables;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestRecord extends CommonTest {

    @Language("java")
    private static final String INPUT_WRAP2 = """
            package a.b;
            import java.util.List;
            class C<X> {
                record R<V>(V v) { }
                <Y> List<R<Y>> wrap(Y y)  { return List.of(new R<>(y)); }
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
        assertEquals("[-] --> -?", mlvWrap.toString()); // FIXME
    }
}
