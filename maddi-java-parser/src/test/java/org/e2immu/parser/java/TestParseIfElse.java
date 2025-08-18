package org.e2immu.parser.java;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ExpressionAsStatement;
import org.e2immu.language.cst.api.statement.IfElseStatement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestParseIfElse extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class C {
              public static void main(String[] args) {
                if(args.length == 0) {
                  System.out.println("Empty");
                } else if(args.length == 1) {
                  System.out.println("One");
                  System.out.println("more");
                }
              }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT);
        MethodInfo main = typeInfo.findUniqueMethod("main", 1);
        if (main.methodBody().statements().get(0) instanceof IfElseStatement ifElse) {
            assertEquals("0", ifElse.source().index());
            if (ifElse.block().statements().get(0) instanceof ExpressionAsStatement eas) {
                assertEquals("0.0.0", eas.source().index());
            } else fail();
            if (ifElse.elseBlock().statements().get(0) instanceof IfElseStatement ifElse2) {
                assertEquals("0.1.0", ifElse2.source().index());
                if (ifElse2.block().statements().get(0) instanceof ExpressionAsStatement eas2) {
                    assertEquals("0.1.0.0.0", eas2.source().index());
                } else fail();
                if (ifElse2.block().statements().get(1) instanceof ExpressionAsStatement eas2) {
                    assertEquals("0.1.0.0.1", eas2.source().index());
                } else fail();
                assertTrue(ifElse2.elseBlock().isEmpty());
            } else fail();
        } else fail();
    }
}
