package org.e2immu.language.java.openjdk.statement;

import org.e2immu.language.cst.api.expression.ArrayLength;
import org.e2immu.language.cst.api.expression.Assignment;
import org.e2immu.language.cst.api.expression.BinaryOperator;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ExpressionAsStatement;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.statement.WhileStatement;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestWhile extends CommonTest {
    @Language("java")
    private static final String INPUT = """
            package a.b;
            // some comment
            class C {
              static void main(String[] args) {
                int i=0;
                while(i<args.length) {
                  System.out.println(i+"="+args[i]);
                  i++;
                }
              }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = scan("a.b.C", INPUT);
        assertEquals(1, typeInfo.comments().size());
        assertEquals(" some comment", typeInfo.comments().getFirst().comment());
        MethodInfo main = typeInfo.findUniqueMethod("main", 1);
        if (main.methodBody().statements().getFirst() instanceof LocalVariableCreation lvc) {
            assertEquals("0", lvc.source().index());
        }
        if (main.methodBody().statements().get(1) instanceof WhileStatement w) {
            assertEquals("1", w.source().index());
            assertEquals("i<args.length", w.expression().toString());
            if (w.expression() instanceof BinaryOperator lt) {
                assertSame(runtime.lessOperatorInt(), lt.operator());
                assertInstanceOf(ArrayLength.class, lt.rhs());
            } else fail();
            if(w.block().statements().getFirst() instanceof ExpressionAsStatement eas) {
                assertEquals("1.0.0", eas.source().index());
            }
            if (w.block().statements().get(1) instanceof ExpressionAsStatement eas) {
                assertEquals("1.0.1", eas.source().index());
                if (eas.expression() instanceof Assignment assignment) {
                    assertEquals("i++", assignment.toString());
                } else fail();
            } else fail();
        } else fail();
    }
}
