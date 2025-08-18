package org.e2immu.language.inspection.integration.java.print;

import org.e2immu.language.cst.api.expression.TextBlock;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestTextBlockFormatting extends CommonTest {


    @Language("java")
    public static final String INPUT1 = """
            package a.b;
            public class X {
                public void method() {
                    String s = \"""
                        abc\\
                        def
                        123
                        \""";
                }
            }
            """;
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        MethodInfo methodInfo = X.findUniqueMethod("method", 0);
        LocalVariableCreation lvc  = (LocalVariableCreation)  methodInfo.methodBody().statements().get(0);
        TextBlock tb = (TextBlock) lvc.localVariable().assignmentExpression();
        assertNotNull(tb.textBlockFormatting());
        String s = javaInspector.print2(X);
        @Language("java")
        String expect = """
            package a.b;
            public class X {
                public void method() {
                    String s = \"""
                            abc\\
                            def
                            123
                            \""";
                }
            }
            """;
        assertEquals(expect, s);
    }

}
