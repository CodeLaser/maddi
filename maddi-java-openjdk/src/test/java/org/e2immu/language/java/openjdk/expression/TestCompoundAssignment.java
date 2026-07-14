package org.e2immu.language.java.openjdk.expression;

import org.e2immu.language.cst.api.expression.Assignment;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ExpressionAsStatement;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression for the reported CST-completeness bug (notes/compound-assignment-missing-binary-operator.md):
 * {@code visitCompoundAssignment} must set {@link Assignment#binaryOperator()} — the underlying operator for
 * {@code i += x  ≡  i = i <op> x} — exactly as the unary ({@code i++}) path does. Without it, a consumer
 * reconstructing the semantics reads {@code i = x} and drops the accumulated value.
 */
public class TestCompoundAssignment extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class X {
              int m(int a) {
                int i = a;
                i += 3;
                return i;
              }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = scan("a.b.X", INPUT);
        MethodInfo m = typeInfo.findUniqueMethod("m", 1);
        // statements: [0] int i = a;  [1] i += 3;  [2] return i;
        Assignment assignment = (Assignment) ((ExpressionAsStatement) m.methodBody().statements().get(1)).expression();

        assertSame(runtime.assignPlusOperatorInt(), assignment.assignmentOperator(), "the '+=' operator");
        assertNotNull(assignment.binaryOperator(), "a compound assignment must carry its binary operator");
        assertSame(runtime.plusOperatorInt(), assignment.binaryOperator(), "'+=' -> the '+' binary operator");
        assertTrue(assignment.assignmentOperatorIsPlus());
    }
}
