package org.e2immu.analyzer.modification.link.impl2;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class Test1 extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.net.URI;
            public class C {
                 private static String makeMessage(URI uri, Object where, String msg, Throwable throwable) {
                     return (throwable == null ? "" : "Exception: " + throwable.getClass().getCanonicalName() + "\\n")
                            + "In: " + uri + (uri == where || where == null ? "" : "\\nIn: " + where) + "\\nMessage: " + msg;
                 }
            }
            """;

    @DisplayName("assertion error in LinkImpl constructor")
    @Test
    public void test1() {
        TypeInfo C = javaInspector.parse(INPUT1);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector,
                new LinkComputer.Options.Builder().setRecurse(true).setCheckDuplicateNames(true).build());
        tlc.doPrimaryType(C);
    }
}
