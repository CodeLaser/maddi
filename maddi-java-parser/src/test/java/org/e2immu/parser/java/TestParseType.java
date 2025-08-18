package org.e2immu.parser.java;

import org.e2immu.language.cst.api.expression.Assignment;
import org.e2immu.language.cst.api.expression.BinaryOperator;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ExpressionAsStatement;
import org.e2immu.language.cst.api.statement.WhileStatement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestParseType extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class C {
              Object[] toArray;
              abstract Object[] toArray();
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT);
        FieldInfo fieldInfo = typeInfo.getFieldByName("toArray", true);
        assertEquals("Type Object[]", fieldInfo.type().toString());
        MethodInfo methodInfo = typeInfo.findUniqueMethod("toArray", 0);
        assertEquals("Type Object[]", methodInfo.returnType().toString());
    }
}
