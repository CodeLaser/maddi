package org.e2immu.language.inspection.integration.java.method;

import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.integration.java.CommonTest2;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestOverride2 extends CommonTest2 {
    @Language("java")
    private static final String aA = """
            package a;
            import c.C;
            class A extends C {
            }
            """;
    @Language("java")
    private static final String bB = """
            package b;
            import a.A;
            class B {
                void m() {
                    A.m();
                }
            }
            """;
    @Language("java")
    private static final String cC = """
            package c;
            class C {
                public static void m();
            }
            """;

    @Test
    public void test() throws IOException {
        ParseResult parseResult = init(Map.of("a.A", aA, "b.B", bB, "c.C", cC));
        TypeInfo B = parseResult.findType("b.B");
        MethodInfo methodInfo = B.findUniqueMethod("m", 0);
        MethodCall mc = (MethodCall) methodInfo.methodBody().lastStatement().expression();
        assertEquals("c.C.m()", mc.methodInfo().fullyQualifiedName());
        assertEquals("A", mc.object().toString());
    }
}
