package org.e2immu.language.inspection.integration.java.method;

import org.e2immu.language.cst.api.expression.BinaryOperator;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.statement.ExpressionAsStatement;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestOverload0 extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package org.e2immu.analyser.resolver.testexample;
            public class Overload_7 {
                public static final class Statement {
                }
                public static void replace(Statement statement){
                }
                public static <S> S replace(S t){
                    return t;
                }
                public static <T> T someExpression() {
                    return (T) new Object();
                }
                public static <U> void test1() {
                    U expression = someExpression();
                    replace(expression);
                }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1);
        MethodInfo test1 = typeInfo.findUniqueMethod("test1", 0);
        Block block = test1.methodBody();
        Statement s1 = block.statements().get(1);
        if (s1 instanceof ExpressionAsStatement eas) {
            if (eas.expression() instanceof MethodCall mc) {
                // ensure we have the method with the type parameter!
                assertEquals("org.e2immu.analyser.resolver.testexample.Overload_7.replace(S)",
                        mc.methodInfo().fullyQualifiedName());
            } else fail();
        } else fail();
    }


    @Language("java")
    private static final String INPUT2 = """
            package org.e2immu.analyser.resolver.testexample;

            public class Overload_6 {
                boolean test() {
                    StringBuilder sb = new StringBuilder("abc").append(3).append("-");
                    CharSequence cs = sb;
                    return cs.length() == 3;
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo typeInfo = javaInspector.parse(INPUT2);
        MethodInfo test1 = typeInfo.findUniqueMethod("test", 0);
        Block block = test1.methodBody();
        Statement s1 = block.statements().get(2);
        if (s1 instanceof ReturnStatement rs) {
            if (rs.expression() instanceof BinaryOperator eq && eq.lhs() instanceof MethodCall mc) {
                // ensure we have the method with the type parameter!
                assertEquals("java.lang.CharSequence.length()", mc.methodInfo().fullyQualifiedName());
            } else fail();
        } else fail();
    }

    @Language("java")
    private static final String INPUT3 = """
            package org.e2immu.analyser.resolver.testexample;

            public class Overload_5 {
                boolean test() {
                    StringBuilder sb = new StringBuilder("abc").append(3).append("-");
                    if (sb.length() == 3) { //evaluates to constant
                        throw new UnsupportedOperationException(); // unreachable code
                    }
                    return sb.toString().length() == 5; // constant true
                }
            }
            """;

    @Test
    public void test3() {
        TypeInfo typeInfo = javaInspector.parse(INPUT3);
        MethodInfo test1 = typeInfo.findUniqueMethod("test", 0);
        Block block = test1.methodBody();
        Statement s1 = block.statements().get(2);
        if (s1 instanceof ReturnStatement rs) {
            if (rs.expression() instanceof BinaryOperator eq && eq.lhs() instanceof MethodCall mc) {
                // ensure we have the method with the type parameter!
                assertEquals("java.lang.String.length()", mc.methodInfo().fullyQualifiedName());
            } else fail();
        } else fail();
    }


    @Language("java")
    private static final String INPUT4 = """
            package org.e2immu.analyser.resolver.testexample;

            public class Overload_4 {

                interface I1 {
                    int f1(String s);

                    void consume(double d);

                    String test();
                }

                interface I2 extends I1 {
                    String test();

                    default int f1(String t) {
                        return t.length();
                    }

                    default void consume(double d) {
                        System.out.println(d);
                    }
                }

                I2 i2 = () -> "3";
            }
            """;

    @Test
    public void test4() {
        javaInspector.parse(INPUT4);
    }

    @Language("java")
    private static final String INPUT5 = """
            package org.e2immu.analyser.resolver.testexample;

            public class Overload_3 {

                interface I1<T> {
                    int f1(T s);

                    void consume(double d);
                }

                interface I2 extends I1<String> {
                    String test();

                    default int f1(String t) {
                        return t.length();
                    }

                    default void consume(double d) {
                        System.out.println(d);
                    }
                }

                I2 i2 = () -> "3";
            }
            """;

    @Test
    public void test5() {
        javaInspector.parse(INPUT5);
    }


    @Language("java")
    private static final String INPUT6 = """
            package org.e2immu.analyser.resolver.testexample;

            public class Overload_2 {

                interface I1 {
                    int f1(String s);

                    void consume(double d);
                }

                interface I2 extends I1 {
                    String test();

                    default int f1(String t) {
                        return t.length();
                    }

                    default void consume(double d) {
                        System.out.println(d);
                    }
                }

                I2 i2 = () -> "3";
            }
            """;

    @Test
    public void test6() {
        javaInspector.parse(INPUT6);
    }

    @Language("java")
    private static final String INPUT7 = """
            package org.e2immu.analyser.resolver.testexample;

            public class Overload_1 {

                private static void method(int p1) {
                    System.out.println(p1);
                }

                private static void method(int p1, String s) {
                    System.out.println(p1+": "+s);
                }

                private static void method(int p1, String... args) {
                    System.out.println(p1 + " " + args.length);
                }

                public void test(String s) {
                    method(0);
                    method(1);
                    method(2, "");
                    method(3, "a");
                    method(2, "a", "b", "c");
                    method(4, new String[]{"a", "b"});
                }
            }""";

    @Test
    public void test7() {
        javaInspector.parse(INPUT7);
    }

    @Language("java")
    private static final String INPUT8 = """
            package org.e2immu.analyser.resolver.testexample;

            public class Overload_0 {

                public boolean test(String s) {
                    return new StringBuilder(s).length() > 0;
                }
            }""";

    @Test
    public void test8() {
        javaInspector.parse(INPUT8);
    }

    private static final String INPUT9 = """
            package a.b;
            class X {
                public String toString() {
                  return "X";
                }
                class MyClass {
                    public String toString() { return ""; }
                }
                public String test(MyClass s) {
                    return s.toString();
                }
            }
            """;

    @Test
    public void test9() {
        TypeInfo X = javaInspector.parse(INPUT9);
        MethodInfo toString = X.findUniqueMethod("toString", 0);
        assertEquals("a.b.X.toString()", toString.fullyQualifiedName());
        assertEquals(1, toString.overrides().size());

        MethodInfo test = X.findUniqueMethod("test", 1);
        if (test.methodBody().statements().get(0) instanceof ReturnStatement rs) {
            if (rs.expression() instanceof MethodCall mc) {
                MethodInfo myClassToString = mc.methodInfo();
                assertEquals("a.b.X.MyClass.toString()", myClassToString.fullyQualifiedName());
                MethodInfo override = myClassToString.overrides().stream().findFirst().orElseThrow();
                assertEquals("java.lang.Object.toString()", override.fullyQualifiedName());
            } else fail();
        } else fail();
    }
}
