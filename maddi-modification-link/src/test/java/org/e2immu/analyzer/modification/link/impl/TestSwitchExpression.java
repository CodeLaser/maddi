package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestSwitchExpression extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.List;
            public class X {
                List<String> list1;
                List<String> list2;
                public List<String> method(List<String> list3) {
                    return switch (list3.getFirst()) {
                        case "a" -> null;
                        case "B" -> list1;
                        case "c" -> {
                            if (list3.size()>1) yield list2;
                            yield list3;
                        }
                        default -> throw new UnsupportedOperationException();
                    };
                }
            }
            """;

    @DisplayName("switch expression with a number of difficulties")
    @Test
    public void test1a() {
        TypeInfo X = javaInspector.parse(INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        MethodInfo method = X.findUniqueMethod("method", 1);
        method.analysis().set(PropertyImpl.INDEPENDENT_METHOD, ValueImpl.IndependentImpl.INDEPENDENT);

        LinkComputer tlc = new LinkComputerImpl(javaInspector,
                new LinkComputer.Options.Builder().setCheckDuplicateNames(true).build());
        MethodLinkedVariables mlvGet = tlc.doMethod(method);
        assertEquals("[-] --> method←$_ce2,method←this.list1,method←this.list2,method←0:list3",
                mlvGet.toString());
    }

}
