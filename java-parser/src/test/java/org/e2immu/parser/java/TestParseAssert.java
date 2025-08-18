package org.e2immu.parser.java;

import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.AssertStatement;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestParseAssert extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class C {
              public int length(String[] args) {
                assert args != null;
                int a = args.length;
                assert a > 0 : "expect a to be >0, but got "+a;
                return a+1;
              }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT);
        assertEquals("C", typeInfo.simpleName());
        MethodInfo methodInfo = typeInfo.methods().get(0);


        Block block = methodInfo.methodBody();
        assertEquals(4, block.size());
        if (block.statements().get(0) instanceof AssertStatement as) {
            assertTrue(as.message().isEmpty());
            assertEquals("args!=null", as.expression().toString());
        } else fail();
        if (block.statements().get(2) instanceof AssertStatement as) {
            assertInstanceOf(BinaryOperator.class, as.message());
            assertEquals("\"expect a to be >0, but got \"+a", as.message().toString());
            assertEquals("a>0", as.expression().toString());
        } else fail();
    }
}
