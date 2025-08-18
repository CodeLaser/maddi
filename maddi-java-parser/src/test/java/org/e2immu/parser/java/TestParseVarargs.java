package org.e2immu.parser.java;

import org.e2immu.language.cst.api.expression.Assignment;
import org.e2immu.language.cst.api.expression.BinaryOperator;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ExpressionAsStatement;
import org.e2immu.language.cst.api.statement.WhileStatement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestParseVarargs extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package org.e2immu.analyser.resolver.testexample;

            public class Varargs_0 {

                private static void method(int p1, String... args) {
                    System.out.println(p1 + " " + args.length);
                }

                public void test(String s) {
                    method(0);
                    method(1);
                    method(2, "");
                    method(3, "a");
                    method(2, "a", "b", "c");
                    method(4, new String[]{"a", "b"});
                }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT);
        MethodInfo method = typeInfo.findUniqueMethod("method", 2);
        assertEquals("Type String[]", method.parameters().get(1).parameterizedType().toString());
        assertTrue(method.parameters().get(1).isVarArgs());
        MethodInfo test = typeInfo.findUniqueMethod("test", 1);
        for (int i = 0; i < 6; i++) {
            if (test.methodBody().statements().get(i) instanceof ExpressionAsStatement eas
                && eas.expression() instanceof MethodCall mc) {
                assertSame(method, mc.methodInfo());
            }
        }
    }
}
