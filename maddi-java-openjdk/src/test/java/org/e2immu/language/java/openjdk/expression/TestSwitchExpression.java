package org.e2immu.language.java.openjdk.expression;

import org.e2immu.language.cst.api.expression.SwitchExpression;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.statement.YieldStatement;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class TestSwitchExpression extends CommonTest {
    @Language("java")
    public static final String INPUT1 = """
            import java.util.List;
            record X(List<String> list, int k) {
                int method() {
                    return switch(k) {
                        case 0 -> list.getFirst().length();
                        case 1 -> {
                            System.out.println("?");
                            yield k+3;
                        }
                        case 2 -> {
                            int v = 5;
                            System.out.println(v);
                            yield v;
                        }
                        default -> throw new UnsupportedOperationException();
                    };
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo X = scan("X", INPUT1);
        MethodInfo method = X.findUniqueMethod("method", 0);
        Statement returnStatement = method.methodBody().statements().getFirst();
        SwitchExpression switchExpression = (SwitchExpression) returnStatement.expression();
        Statement s0 = switchExpression.entries().getFirst().statement();
        assertEquals("0.0.0", s0.source().index());
        Statement s11 = switchExpression.entries().get(1).statementAsBlock().statements().get(1);
        assertEquals("0.1.1", s11.source().index());
        assertInstanceOf(YieldStatement.class, s11);
    }

    // JEP 456: a case label may carry several patterns as long as none binds a variable, e.g. 'case A _, B _ ->'.
    // Mirrors com.sun.tools.jdeps.Dependencies ('case BaseTypeSig _, TypeVarSig _ ->'), which crashed the scan.
    @Language("java")
    public static final String INPUT2 = """
            class X {
                sealed interface S permits A, B, C {}
                record A() implements S {}
                record B() implements S {}
                record C() implements S {}
                int method(S s) {
                    return switch (s) {
                        case A _, B _ -> 1;
                        case C _ -> 2;
                    };
                }
            }
            """;

    @DisplayName("multiple binding-free type patterns in one case label")
    @Test
    public void test2() {
        TypeInfo X = scan("X", INPUT2);
        MethodInfo method = X.findUniqueMethod("method", 1);
        Statement returnStatement = method.methodBody().statements().getFirst();
        SwitchExpression switchExpression = (SwitchExpression) returnStatement.expression();
        assertEquals(2, switchExpression.entries().size());
    }
}
