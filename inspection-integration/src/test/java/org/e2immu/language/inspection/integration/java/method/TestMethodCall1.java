package org.e2immu.language.inspection.integration.java.method;

import org.e2immu.language.cst.api.expression.Lambda;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class TestMethodCall1 extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.Arrays;

            public class MethodCall_11 {

                enum Choice {ONE, TWO, THREE,}

                private final Choice[] choices;

                public MethodCall_11(int n) {
                    choices = new Choice[n];
                    Arrays.fill(choices, Choice.ONE);
                }

                Choice getChoice(int i) {
                    return choices[i];
                }
            }
            """;

    @Test
    public void test1() {
        javaInspector.parse(INPUT1);
    }

    @Language("java")
    private static final String INPUT2 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.List;

            public class MethodCall_12 {

                record Pair<K, V>(K k, V v) {
                }

                public void method() {
                    List<Pair<String, Integer>> list = List.of(new Pair<>("abc", 3));
                    for (Pair<String, Integer> pair : list) {
                        if (pair.k.length() > 0) {
                            System.out.println(pair.v.byteValue());
                        }
                    }
                }
            }
            """;

    @Test
    public void test2() {
        javaInspector.parse(INPUT2);
    }

    @Language("java")
    private static final String INPUT3 = """
            package org.e2immu.analyser.resolver.testexample;

            public class MethodCall_13 {

                public void method(String target, int n) {
                    int blockIndex = Integer.parseInt(target.substring(n + 1, target.indexOf('.', n + 1)));
                }
            }
            """;

    @Test
    public void test3() {
        javaInspector.parse(INPUT3);
    }

    @Language("java")
    private static final String INPUT4 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.Arrays;

            public class MethodCall_14 {
                record Pair<K, V>(K k, V v) {
                }

                interface VI {
                    E make();
                }

                static class VII implements VI {

                    @Override
                    public E make() {
                        return null;
                    }
                }

                interface E {
                    boolean test();
                }

                static class EE implements E {

                    @Override
                    public boolean test() {
                        return false;
                    }
                }

                public void accept(boolean b, E... values) {
                }

                // sort of mirrors the notNullValuesAsExpression method in StatementAnalysis
                public void method(VI b, E e, VI... vis) {
                    accept(true, Arrays.stream(vis)
                            .filter(vi -> vi.getClass().toString().contains("e"))
                            .map(vi -> {
                                if (b instanceof VII vii) {
                                    if (e instanceof EE ee) {
                                        return new Pair<>(vi, e.test() + "xxx");
                                    }
                                    String s = "ab";
                                    return new Pair<>(vi, s);
                                }
                                return null;
                            })
                            .filter(p -> p != null && p.k.make() != null && p.v.length() == 4)
                            .map(p -> p.k.make())
                            .toArray(E[]::new));
                }
            }
            """;

    @Test
    public void test4() {
        javaInspector.parse(INPUT4);
    }

    @Language("java")
    private static final String INPUT5 = """
            package org.e2immu.analyser.resolver.testexample;

            public record MethodCall_15<T extends Comparable, S>(S s) {

                public S accept(T t) {
                    System.out.println(t);
                    return s;
                }

                public S accept(String string) {
                    System.out.println(string);
                    return s;
                }

                public void test() {
                    accept("a");
                }

                public void test(T t) {
                    accept(t);
                }

                public void test2(T t1, T t2) {
                    accept(t1);
                    accept(t2);
                }
            }
            """;

    @Test
    public void test5() {
        javaInspector.parse(INPUT5);
    }

    @Language("java")
    private static final String INPUT7 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.function.Supplier;

            public class MethodCall_17 {

                private void log(String msg, Object... objects) {
                    System.out.println(msg + ": " + objects.length);
                }

                private void log(String msg, Object object, Supplier<Object> supplier) {
                    System.out.println(msg + ": " + object + " = " + supplier.get());
                }

                public void method(int x) {
                    log("Hello!", x, () -> "Return a string");
                    log("Hello?", x, "Return a string");
                }
            }
            """;

    @Test
    public void test7() {
        javaInspector.parse(INPUT7);
    }

    @Language("java")
    private static final String INPUT8 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.Arrays;
            import java.util.function.Function;
            import java.util.stream.Collectors;

            public class MethodCall_18 {

                interface AnnotationExpression {
                    <T> T extract(String s, T t);
                }

                private final AnnotationExpression ae = new AnnotationExpression() {
                    @Override
                    public <T> T extract(String s, T t) {
                        return s.length() > 0 ? null : t;
                    }
                };

                // int
                public String method1() {
                    Function<AnnotationExpression, String> f1 = ae -> {
                        Integer i = ae.extract("level", 3);
                        return i == null ? null : Integer.toString(i);
                    };
                    return f1.apply(ae);
                }

                // string[]
                public String method2() {
                    Function<AnnotationExpression, String> f2 = ae -> {
                        String[] inspected = ae.extract("to", new String[]{});
                        return Arrays.stream(inspected).sorted().collect(Collectors.joining(","));
                    };
                    return f2.apply(ae);
                }

                // Integer
                public String method3() {
                    Function<AnnotationExpression, String> f3 = ae -> {
                        Integer i = ae.extract("level", null);
                        return Integer.toString(i);
                    };
                    return f3.apply(ae);
                }
            }
            """;

    @Test
    public void test8() {
        TypeInfo typeInfo = javaInspector.parse(INPUT8);
        MethodInfo method2 = typeInfo.findUniqueMethod("method2", 0);
        if (method2.methodBody().statements().get(0) instanceof LocalVariableCreation lvc
            && lvc.localVariable().assignmentExpression() instanceof Lambda lambda) {
            assertEquals("Type java.util.function.Function<org.e2immu.analyser.resolver.testexample.MethodCall_18.AnnotationExpression,String>",
                    lambda.concreteFunctionalType().toString());
            if (lambda.methodBody().statements().get(0) instanceof LocalVariableCreation lvc2
                && lvc2.localVariable().assignmentExpression() instanceof MethodCall mc) {
                assertEquals("Type String[]", mc.concreteReturnType().toString());
            } else fail();
        } else fail();
    }

    @Language("java")
    private static final String INPUT9 = """
            package org.e2immu.analyser.resolver.testexample;


            import java.util.Objects;

            // reflects an inspection problem with code in StatementAnalysisImpl, around line 2000
            public class MethodCall_19 {
                interface EvaluationContext {
                }

                interface Precondition {
                    static Precondition empty(EvaluationContext evaluationContext) {
                        return new Precondition() {
                        };
                    }
                }

                public static void method(Precondition precondition, EvaluationContext evaluationContext) {
                    setPreconditionFromMethodCalls(Objects.requireNonNullElseGet(precondition,
                            () -> Precondition.empty(evaluationContext)));
                }

                public static void setPreconditionFromMethodCalls(Precondition precondition) {
                    // don't need anything here
                }
            }
            """;

    @Test
    public void test9() {
        javaInspector.parse(INPUT9);
    }

}
