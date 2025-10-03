package org.e2immu.parser.java;

import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ExpressionAsStatement;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

public class TestParseLiteral extends CommonTestParse {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestParseLiteral.class);

    @Language("java")
    public static final String INPUT1 = """
            package a.b;
            public class X {
                public double parse() {
                    return 0xF + 001 + 0.01e-1f + 2L + 1.0e2d;
                }
            }
            """;

    @Test
    public void test1() {
        parse(INPUT1);
    }

    @Language("java")
    public static final String INPUT2 = """
            package a.b;
            public class X {
                public double parse() {
                   int a = 0xFF_FF_FF_FF;
                   int b = 0xFFFFFFFE;
                   int i = 0xFF000000;
                   int j = 0xcafebabe;
                   return i+j;
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo X = parse(INPUT2);
        MethodInfo parse = X.findUniqueMethod("parse", 0);
        LocalVariableCreation a = (LocalVariableCreation) parse.methodBody().statements().get(0);
        IntConstant ac = (IntConstant) a.localVariable().assignmentExpression();
        assertEquals(-1, ac.constant());
        LocalVariableCreation b = (LocalVariableCreation) parse.methodBody().statements().get(1);
        IntConstant bc = (IntConstant) b.localVariable().assignmentExpression();
        assertEquals(-2, bc.constant());
        LocalVariableCreation i = (LocalVariableCreation) parse.methodBody().statements().get(2);
        IntConstant ic = (IntConstant) i.localVariable().assignmentExpression();
        assertEquals(-16777216, ic.constant());
        LocalVariableCreation j = (LocalVariableCreation) parse.methodBody().statements().get(3);
        IntConstant jc = (IntConstant) j.localVariable().assignmentExpression();
        assertEquals(-889275714, jc.constant());
        assertEquals(-9.0605293E8, ic.constant() + jc.constant());
    }

    @Test
    public void test() {
        assertEquals(0xFF000000, -16777216);
        assertEquals(0xcafebabe, -889275714);
    }

    @Language("java")
    public static final String INPUT3 = """
            package a.b;
            public class X {
                public long parse(long l) {
                    long ll = l;
                    ll &= 0x00000000FFFFFFFFl;
                    return ll;
                }
            }
            """;

    @Test
    public void test3() {
        TypeInfo typeInfo = parse(INPUT3);
        MethodInfo parse = typeInfo.findUniqueMethod("parse", 1);
        if (parse.methodBody().statements().get(1) instanceof ExpressionAsStatement eas
            && eas.expression() instanceof Assignment a) {
            assertInstanceOf(LongConstant.class, a.value());
            assertEquals("ll&=4294967295L", a.toString());
        } else fail();
    }

    @Language("java")
    public static final String INPUT3b = """
            package a.b;
            public class X {
                public long parse(long l) {
                    long m = 0xC6a4_A793_5BD1_E99L;
                    return m & l;
                }
            }
            """;

    @Test
    public void test3b() {
        TypeInfo typeInfo = parse(INPUT3b);
        MethodInfo parse = typeInfo.findUniqueMethod("parse", 1);
        if (parse.methodBody().statements().getFirst() instanceof LocalVariableCreation lvc
            && lvc.localVariable().assignmentExpression() instanceof LongConstant a) {
            assertEquals(0xc6a4a7935bd1e99L, a.constant());
        } else fail();
    }

    @Language("java")
    public static final String INPUT3c = """
            package a.b;
            public class X {
                public long parse(long l) {
                    long m = 0xC6A4_A793_5BD1_E995L;
                    return m & l;
                }
            }
            """;

    @Test
    public void test3c() {
        TypeInfo typeInfo = parse(INPUT3c);
        MethodInfo parse = typeInfo.findUniqueMethod("parse", 1);
        if (parse.methodBody().statements().getFirst() instanceof LocalVariableCreation lvc
            && lvc.localVariable().assignmentExpression() instanceof LongConstant a) {
            assertEquals(0xc6a4a7935bd1e995L, a.constant());
            assertEquals(-4132994306676758123L, a.constant());
        } else fail();
    }


    @Language("java")
    public static final String INPUT3d = """
            package a.b;
            public class X {
                public long parse() {
                    long l = +0xC6A4_A793_5BD1_E99L;
                    long m = -0xC6A4_A793_5BD1_E99L;
                    return m & l;
                }
            }
            """;

    @Test
    public void test3d() {
        TypeInfo typeInfo = parse(INPUT3d);
        MethodInfo parse = typeInfo.findUniqueMethod("parse", 0);
        if (parse.methodBody().statements().getFirst() instanceof LocalVariableCreation lvc
            && lvc.localVariable().assignmentExpression() instanceof LongConstant a) {
            assertEquals(0xc6a4a7935bd1e99L, a.constant());
        } else fail();
    }

    public static final String INPUT4 = "package a.b; public class X { public void parse() { String s = \"a \\\" and \\\" b\"; } }";

    @Test
    public void test4() {
        LOGGER.debug(INPUT4);
        TypeInfo typeInfo = parse(INPUT4);
        MethodInfo parse = typeInfo.findUniqueMethod("parse", 0);
        if (parse.methodBody().statements().get(0) instanceof LocalVariableCreation lvc) {
            if (lvc.localVariable().assignmentExpression() instanceof StringConstant sc) {
                assertEquals("a \" and \" b", sc.constant());
            } else fail();
        } else fail();
    }

    @Language("java")
    public static final String INPUT5 = """
            package a.b;
            public class X {
                public void parse() {
                    String s = \"""
                        abc
                           12"3"
                        \""";
                }
            }
            """;

    @DisplayName("text block, basics")
    @Test
    public void test5() {
        LOGGER.debug(INPUT5);
        TypeInfo typeInfo = parse(INPUT5);
        MethodInfo parse = typeInfo.findUniqueMethod("parse", 0);
        if (parse.methodBody().statements().get(0) instanceof LocalVariableCreation lvc) {
            if (lvc.localVariable().assignmentExpression() instanceof TextBlock tb) {
                assertEquals("abc\n   12\"3\"\n", tb.constant());
                assertEquals("TextBlockFormattingImpl[lineBreaks=[], optOutWhiteSpaceStripping=false, trailingClosingQuotes=false]",
                        tb.textBlockFormatting().toString());
            } else fail();
        } else fail();
    }


    @Language("java")
    public static final String INPUT5B = """
            package a.b;
            public class X {
                public void parse() {
                    String s = \"""  
                        abc
            
                        def
            
                           123
                        \""";
                }
            }
            """;

    @DisplayName("text block, extra spacing and blank lines")
    @Test
    public void test5B() {
        TypeInfo typeInfo = parse(INPUT5B);
        MethodInfo parse = typeInfo.findUniqueMethod("parse", 0);
        if (parse.methodBody().statements().get(0) instanceof LocalVariableCreation lvc) {
            if (lvc.localVariable().assignmentExpression() instanceof TextBlock tb) {
                assertEquals("abc\n\ndef\n\n   123\n", tb.constant());
                assertEquals("TextBlockFormattingImpl[lineBreaks=[], optOutWhiteSpaceStripping=false, trailingClosingQuotes=false]",
                        tb.textBlockFormatting().toString());
            } else fail();
        } else fail();
    }


    @Language("java")
    public static final String INPUT6 = """
            package a.b;
            public class X {
                public void parse() {
                    String s = \"""
                        abc
                        def
                           123\""";
                }
            }
            """;

    @DisplayName("text block, trailing quotes")
    @Test
    public void test6() {
        LOGGER.debug(INPUT6);
        TypeInfo typeInfo = parse(INPUT6);
        MethodInfo parse = typeInfo.findUniqueMethod("parse", 0);
        if (parse.methodBody().statements().get(0) instanceof LocalVariableCreation lvc) {
            if (lvc.localVariable().assignmentExpression() instanceof TextBlock tb) {
                assertEquals("abc\ndef\n   123", tb.constant());
                assertEquals("TextBlockFormattingImpl[lineBreaks=[], optOutWhiteSpaceStripping=false, trailingClosingQuotes=true]",
                        tb.textBlockFormatting().toString());
            } else fail();
        } else fail();
    }


    @Language("java")
    public static final String INPUT7 = """
            package a.b;
            public class X {
                public void parse() {
                    String s = \"""
                        abc\\
                        def
                        123
                        \""";
                }
            }
            """;

    @DisplayName("text block, backslash")
    @Test
    public void test7() {
        assertTrue(INPUT7.contains("\\"));
        TypeInfo typeInfo = parse(INPUT7);
        MethodInfo parse = typeInfo.findUniqueMethod("parse", 0);
        if (parse.methodBody().statements().get(0) instanceof LocalVariableCreation lvc) {
            if (lvc.localVariable().assignmentExpression() instanceof TextBlock tb) {
                assertEquals("abcdef\n123\n", tb.constant());
                assertEquals("TextBlockFormattingImpl[lineBreaks=[3], optOutWhiteSpaceStripping=false, trailingClosingQuotes=false]",
                        tb.textBlockFormatting().toString());
            } else fail();
        } else fail();
    }

    @Language("java")
    public static final String INPUT7B = """
            package a.b;
            public class X {
                public void parse() {
                    String s = \"""
                        abc
                        def
                           123\\
                        \""";
                }
            }
            """;

    @DisplayName("text block, trailing quotes with backslash")
    @Test
    public void test6B() {
        assertTrue(INPUT7B.contains("\\"));
        TypeInfo typeInfo = parse(INPUT7B);
        MethodInfo parse = typeInfo.findUniqueMethod("parse", 0);
        if (parse.methodBody().statements().get(0) instanceof LocalVariableCreation lvc) {
            if (lvc.localVariable().assignmentExpression() instanceof TextBlock tb) {
                assertEquals("abc\ndef\n   123", tb.constant());
                assertEquals("TextBlockFormattingImpl[lineBreaks=[14], optOutWhiteSpaceStripping=false, trailingClosingQuotes=false]",
                        tb.textBlockFormatting().toString());
            } else fail();
        } else fail();
    }

    private static final String S1 = """
            abc
            """;
    private static final String S2 = """
             abc
            """;

    @Language("java")
    private static final String INPUT9 = """
            package a.b;
            class C {
                private static final String S1 = \"""
                    abc
                    \""";
                private static final String S2 = ""\"
                     abc
                    ""\";
            }
            """;

    @DisplayName("Tests 'last' in parseTextBlock")
    @Test
    public void test9() {
        assertEquals("abc\n", S1);
        assertEquals(" abc\n", S2);
        TypeInfo typeInfo = parse(INPUT9);
        FieldInfo S1 = typeInfo.getFieldByName("S1", true);
        if (S1.initializer() instanceof TextBlock tb) {
            assertEquals("abc\n", tb.constant());
        } else fail();
        FieldInfo S2 = typeInfo.getFieldByName("S2", true);
        if (S2.initializer() instanceof TextBlock tb) {
            assertEquals(" abc\n", tb.constant());
        } else fail();
    }


    @Language("java")
    private static final String INPUT10 = """
            package a.b;
            class C {
                private static final float F1 = -0.03F;
            }
            """;

    @DisplayName("Literal negative float")
    @Test
    public void test10() {
        TypeInfo typeInfo = parse(INPUT10);
        FieldInfo F1 = typeInfo.getFieldByName("F1", true);
        if (F1.initializer() instanceof FloatConstant fc) {
            assertEquals(-0.03f, fc.constant());
            assertEquals("3-37:3-42", fc.source().compact2());
        } else fail("Is of " + F1.initializer().getClass());
    }


    @Language("java")
    public static final String INPUT11 = """
            package a.b;
            public class X {
               void method() {
                   System.out.println('\\r' + "abc" + '\\15');
               }
            }
            """;

    @Test
    public void test11() {
        TypeInfo X = parse(INPUT11);
        MethodInfo methodInfo = X.findUniqueMethod("method", 0);
        MethodCall mc = (MethodCall) methodInfo.methodBody().lastStatement().expression();
        BinaryOperator bo = (BinaryOperator) mc.parameterExpressions().getFirst();
        assertEquals("'\\r'+\"abc\"", bo.lhs().toString());
        CharConstant cc = (CharConstant) bo.rhs();
        assertEquals(13, cc.constant().charValue());
    }


    @Language("java")
    public static final String INPUT12 = """
            package a.b;
            public class X {
               Class<?> method() {
                  return X[].class;
               }
            }
            """;

    @Test
    public void test12() {
        TypeInfo X = parse(INPUT12);
        MethodInfo methodInfo = X.findUniqueMethod("method", 0);
        assertEquals("Type Class<?>", methodInfo.returnType().toString());
        assertEquals("Type Class<a.b.X[]>", methodInfo.methodBody().lastStatement().expression().parameterizedType().toString());
    }


}
