package org.e2immu.language.inspection.integration.java.method;

import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.integration.java.CommonTest2;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

public class TestMethodCall12 extends CommonTest2 {

    @Language("java")
    private static final String bB = """
            package b;
            public class B {
                public static String contains(String s) {
                    return s.toLowerCase();
                }
            }
            """;

    @Language("java")
    private static final String cC = """
            package c;
            import static b.B.*;
            import java.util.Set;
            class C {
                public void m(Set<String> set) {
                    if(success(set.contains("abc"))) {
                        System.out.println("yes");
                    }
                }
                private boolean success(boolean b) {
                    return !b;
                }
            }
            """;

    @DisplayName("avoid instance->static if type of object is different")
    @Test
    public void test() throws IOException {
        init(Map.of( "b.B", bB, "c.C", cC));
    }
}
