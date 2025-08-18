package org.e2immu.parser.java;

import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ExpressionAsStatement;
import org.e2immu.language.cst.api.statement.TryStatement;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestParseTryCatch extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class C {
              public static void main(String[] args) {
                try {
                   System.out.println(args[0]);
                } catch(Exception e) {
                   System.out.println("exception "+e);
                } finally {
                   System.out.println("bye");
                }
              }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT);
        MethodInfo main = typeInfo.findUniqueMethod("main", 1);
        if (main.methodBody().statements().get(0) instanceof TryStatement tryStatement) {
            assertTrue(tryStatement.resources().isEmpty());
            if (tryStatement.block().statements().get(0) instanceof ExpressionAsStatement eas) {
                if (eas.expression() instanceof MethodCall mc) {
                    assertEquals("args[0]", mc.parameterExpressions().get(0).toString());
                    if (mc.parameterExpressions().get(0) instanceof VariableExpression ve
                        && ve.variable() instanceof DependentVariable dv) {
                        assertEquals("args", dv.arrayVariable().simpleName());
                    } else fail();
                }
            } else fail();
            assertEquals(1, tryStatement.catchClauses().size());
            assertEquals(1, tryStatement.catchClauses().get(0).block().size());
            assertEquals(1, tryStatement.finallyBlock().size());

            assertEquals("""
                    try{System.out.println(args[0]);}catch(Exception e){System.out.println("exception "+e);}finally{System.out.println("bye");}\
                    """, tryStatement.toString());
        } else fail();
    }
}
