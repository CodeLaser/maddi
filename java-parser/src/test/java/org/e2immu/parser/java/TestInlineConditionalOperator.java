package org.e2immu.parser.java;

import org.e2immu.language.cst.api.expression.InlineConditional;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestInlineConditionalOperator extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class C {
              int method(boolean b, int i, int j) {
                return b?i:j-1;
              }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT);

        MethodInfo methodInfo = typeInfo.methods().get(0);
        if (methodInfo.methodBody().statements().get(0) instanceof ReturnStatement rs
            && rs.expression() instanceof InlineConditional ic) {
            assertEquals("b?i:j-1", ic.toString());
            assertNotNull(ic.source());
        } else fail();
    }
}
