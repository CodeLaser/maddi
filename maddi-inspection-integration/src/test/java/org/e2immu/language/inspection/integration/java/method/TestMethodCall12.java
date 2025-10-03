package org.e2immu.language.inspection.integration.java.method;

import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.integration.java.CommonTest2;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        init(Map.of("b.B", bB, "c.C", cC));
    }

    @Language("java")
    private static final String aA2 = """
            package a;
            public class ArrayList {
            }
            """;

    @Language("java")
    private static final String bB2 = """
            package b;
            import java.util.ArrayList;
            public class B extends ArrayList<String> {
                public B() { super(); }
            }
            """;

    @Language("java")
    private static final String cC2 = """
            package c;
            import a.ArrayList;
            import b.B;
            class C extends B {
                ArrayList arrayList;
            }
            """;

    @Test
    public void test2() throws IOException {
        ParseResult parseResult = init(Map.of("a.ArrayList", aA2, "b.B", bB2, "c.C", cC2));
        TypeInfo C = parseResult.findType("c.C");
        FieldInfo arrayList = C.getFieldByName("arrayList", true);
        assertEquals("Type a.ArrayList", arrayList.type().toString());
    }
}
