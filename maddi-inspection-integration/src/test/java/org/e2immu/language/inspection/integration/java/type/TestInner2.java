package org.e2immu.language.inspection.integration.java.type;

import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.expression.TypeExpression;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.integration.java.CommonTest2;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestInner2 extends CommonTest2 {
    @Language("java")
    private static final String aA = """
            package a;
            class A {
                public static class Inner {
                    public static void m() {}
                }
            }
            """;
    @Language("java")
    private static final String aB = """
            package a;
            class B {
                void m() {
                    A.Inner.m();
                }
            }
            """;

    @Test
    public void test() throws IOException {
        ParseResult parseResult = init(Map.of("a.A", aA, "a.B", aB));
        TypeInfo b = parseResult.findType("a.B");
        MethodInfo m = b.findUniqueMethod("m", 0);
        MethodCall mc = (MethodCall) m.methodBody().statements().getFirst().expression();
        if(mc.object() instanceof TypeExpression te) {
            assertEquals("4-9:4-15", te.source().compact2());
            //noinspection ALL
            List<DetailedSources.Builder.TypeInfoSource> tis = (List<DetailedSources.Builder.TypeInfoSource>)
                    te.source().detailedSources().associatedObject(te.parameterizedType().typeInfo());
            assertEquals(1, tis.size());
            assertEquals("""
                    [TypeInfoSource[typeInfo=a.A, source=@4:9-4:9]]\
                    """, tis.toString());
        }
    }
}
