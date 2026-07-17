package org.e2immu.language.java.openjdk.expression;

import org.e2immu.language.cst.api.expression.Assignment;
import org.e2immu.language.cst.api.expression.BinaryOperator;
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

    @Language("java")
    private static final String STRING_INPUT = """
            package a.b;
            class X {
              String concat(String s) {
                s += "x";
                return s;
              }
              String plain(String s) {
                return s + "y";
              }
            }
            """;

    /**
     * Regression for notes/compound-assignment-string-uses-int-operator.md: {@code s += "x"} on a String is
     * concatenation, so the operator must be the String {@code +=} / {@code +}, not the numeric ones the switch
     * used to pick from the syntactic kind alone. Otherwise a consumer reconstructing {@code s = s + "x"} routes a
     * String into {@code runtime.sum()}, which asserts numeric operands.
     */
    @Test
    public void stringCompoundAddIsConcat() {
        TypeInfo typeInfo = scan("a.b.X", STRING_INPUT);
        MethodInfo concat = typeInfo.findUniqueMethod("concat", 1);
        Assignment assignment = (Assignment)
                ((ExpressionAsStatement) concat.methodBody().statements().get(0)).expression();

        assertSame(runtime.assignPlusOperatorString(), assignment.assignmentOperator(),
                "String '+=' must be the String operator, not the int one");
        assertSame(runtime.plusOperatorString(), assignment.binaryOperator(),
                "and its binary operator must be String '+', so s = s + x concatenates");
        assertTrue(assignment.assignmentOperatorIsPlus());
    }

    /** The plain binary path was already correct; assert it here so the two forms are known to agree. */
    @Test
    public void stringPlainAddIsConcat() {
        TypeInfo typeInfo = scan("a.b.X", STRING_INPUT);
        MethodInfo plain = typeInfo.findUniqueMethod("plain", 1);
        // return s + "y";
        var ret = (org.e2immu.language.cst.api.statement.ReturnStatement) plain.methodBody().statements().get(0);
        BinaryOperator binary = (BinaryOperator) ret.expression();
        assertSame(runtime.plusOperatorString(), binary.operator(), "plain String '+' is concatenation");
    }
}
