package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.MethodLinkedVariables;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestImmutable extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.List;
            public interface X {
                record R(String s, int i) { }

                R get(int index);
                void set(int i, R r);
            }
            """;

    @DisplayName("Analyze 'get', 'set' of immutable type")
    @Test
    public void test1a() {
        TypeInfo X = javaInspector.parse(INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        TypeInfo R = X.findSubType("R");
        R.analysis().set(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.IMMUTABLE);

        MethodInfo get = X.findUniqueMethod("get", 1);
        LinkComputer tlc = new LinkComputerImpl(javaInspector, false, false);
        MethodLinkedVariables mlvGet = tlc.doMethod(get);
        assertEquals("[-] --> -", mlvGet.toString());

        MethodInfo set = X.findUniqueMethod("set", 2);
        MethodLinkedVariables mlvSet = tlc.doMethod(set);
        assertEquals("[-, -] --> -", mlvSet.toString());
    }

}
