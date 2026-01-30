package org.e2immu.analyzer.modification.link.impl2;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class Test2 extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;

            class X {
                interface Runtime { }
                static class PA {
                    MA methodAnalyzer;
                    PA(Runtime runtime) {
                        methodAnalyzer = new MA(runtime, this);
                    }
                }
                static class MA {
                    PA prepAnalyzer;
                    Runtime runtime;
                    MA(Runtime runtime, PA prepAnalyzer) {
                        this.runtime = runtime;
                        this.prepAnalyzer = prepAnalyzer;
                    }
                }
            }
            """;

    @DisplayName("cycle protection")
    @Test
    public void test1() {
        TypeInfo C = javaInspector.parse(INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector,
                new LinkComputer.Options.Builder().setRecurse(true).setCheckDuplicateNames(false).build());
        tlc.doPrimaryType(C);
    }


    @Language("java")
    private static final String INPUT2 = """
            package a.b;

            class X {
                
            }
            """;

    @DisplayName("more modifications in 2nd iteration")
    @Test
    public void test2() {
        TypeInfo C = javaInspector.parse(INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector,
                new LinkComputer.Options.Builder().setRecurse(true).setCheckDuplicateNames(false).build());
        tlc.doPrimaryType(C);
    }
}
