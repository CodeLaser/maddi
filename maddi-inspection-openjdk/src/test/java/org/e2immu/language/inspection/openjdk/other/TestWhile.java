package org.e2immu.language.inspection.openjdk.other;

import org.e2immu.language.cst.api.expression.ArrayLength;
import org.e2immu.language.cst.api.expression.Assignment;
import org.e2immu.language.cst.api.expression.BinaryOperator;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ExpressionAsStatement;
import org.e2immu.language.cst.api.statement.WhileStatement;
import org.e2immu.language.inspection.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

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
        TypeInfo typeInfo = scan(Map.of("a.b.C", INPUT), List.of()).getFirst();
        assertEquals(1, typeInfo.comments().size());
        assertEquals(" some comment", typeInfo.comments().getFirst().comment());
        MethodInfo main = typeInfo.findUniqueMethod("main", 1);
        if (main.methodBody().statements().get(1) instanceof WhileStatement w) {
            assertEquals("i<args.length", w.expression().toString());
            if (w.expression() instanceof BinaryOperator lt) {
                assertSame(runtime.lessOperatorInt(), lt.operator());
                assertInstanceOf(ArrayLength.class, lt.rhs());
            } else fail();
            if (w.block().statements().get(1) instanceof ExpressionAsStatement eas) {
                if (eas.expression() instanceof Assignment assignment) {
                    assertEquals("i++", assignment.toString());
                } else fail();
            } else fail();
        } else fail();
    }
}
