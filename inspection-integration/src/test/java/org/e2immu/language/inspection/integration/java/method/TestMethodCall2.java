package org.e2immu.language.inspection.integration.java.method;

import org.e2immu.language.cst.api.expression.Lambda;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ExpressionAsStatement;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class TestMethodCall2 extends CommonTest {

    @Language("java")
    private static final String INPUT2 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.*;

            public class InspectionGaps_2 {
                private static final Map<String, Integer> PRIORITY = new HashMap<>();

                static {
                    PRIORITY.put("e2container", 1);
                    PRIORITY.put("e2immutable", 2);
                }

                static {
                    PRIORITY.put("e1container", 3);
                    PRIORITY.put("e1immutable", 4);
                }

                private static int priority(String in) {
                    return PRIORITY.getOrDefault(in.substring(0, in.indexOf('-')), 10);
                }

                private static String highestPriority(String[] annotations) {
                    List<String> toSort = new ArrayList<>(Arrays.asList(annotations));
                    toSort.sort(Comparator.comparing(InspectionGaps_2::priority));
                    return toSort.get(0);
                }
            }
            """;

    // more of a method call test
    @Test
    public void test2() {
        javaInspector.parse(INPUT2);
    }

    @Language("java")
    private static final String INPUT23= """
            package org.e2immu.analyser.resolver.testexample;

            import org.e2immu.annotation.NotNull;

            import java.util.List;

            public class MethodCall_20 {

                @NotNull(content = true)
                private final List<String> list;

                public MethodCall_20(@NotNull(content = true) List<String> list) {
                    this.list = list;
                }

                public int method() {
                    int res = 3;
                    for (String s : list.subList(0, 10)) {
                        if (s.length() == 9) {
                            res = 4;
                            break;
                        }
                    }
                    return res;
                }
            }
            """;

    @Test
    public void test23() {
        javaInspector.parse(INPUT23);
    }

    @Language("java")
    private static final String INPUT24 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.function.Consumer;

            public class MethodCall_24 {

                interface Analyser {
                    record SharedState(int iteration, String context) {}
                }

                static class MethodAnalyser implements Analyser {

                    private record SharedState(boolean allowBreaking) {}

                    public void init() {
                        Consumer<SharedState> consumer= sharedState -> method(sharedState);
                    }

                    public boolean method(SharedState sharedState) {
                        return true;
                    }

                    public boolean other() {
                        return false;
                    }
                }
            }
            """;

    @Test
    public void test24() {
        javaInspector.parse(INPUT24);
    }

    @Language("java")
    private static final String INPUT25 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.List;
            import java.util.function.Consumer;
            import java.util.function.Predicate;

            public class MethodCall_25 {

                interface Element {

                    default void visit(Consumer<Element> consumer) {
                        subElements().forEach(element -> element.visit(consumer));
                        consumer.accept(this);
                    }

                    default void visit(Predicate<Element> predicate) {
                        if (predicate.test(this)) {
                            subElements().forEach(element -> element.visit(predicate));
                        }
                    }

                    List<Element> subElements();
                }

                public void method(Element e) {
                    e.visit(element -> {
                        System.out.println("?");
                        return true;
                    });
                }

                /* Compilation error:
                public void method2(Element e) {
                    e.visit(element -> System.out.println("?"));
                }
                */

                public void method2(Element e) {
                    e.visit(element -> {
                        System.out.println("?");
                    });
                }

                public void method3(Element e) {
                    e.visit(element -> {
                        System.out.println("?");
                        if (element == null) {
                            return false;
                        } else {
                            return true;
                        }
                    });
                }

                public void method4(Element e) {
                    e.visit(element -> {
                        try {
                            System.out.println("Hello");
                            return true;
                        } finally {
                            System.out.println("?");
                        }
                    });
                }

                /*
                Compilation error:
                public void method5(Element e, List<String> list) {
                    e.visit(element -> {
                        for (String s : list) {
                           return true;
                        }
                    });
                }
                 */

                /*
                Compilation error: (cannot distinguish!)
                public void method5(Element e) {
                    e.visit(element -> {
                        throw new UnsupportedOperationException("?" + element);
                    });
                }*/
            }
            """;

    @Test
    public void test25() {
        javaInspector.parse(INPUT25);
    }
    @Language("java")
    private static final String INPUT26 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.Map;

            public class MethodCall_26 {

                public void method(Map<String, Integer> map) {
                    map.merge("abc", 4, (i1, i2) -> {
                        throw new UnsupportedOperationException();
                    });
                }
            }
            """;

    @Test
    public void test26() {
        javaInspector.parse(INPUT26);
    }
    @Language("java")
    private static final String INPUT27 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.List;
            import java.util.function.BiConsumer;
            import java.util.function.Consumer;
            import java.util.function.Predicate;

            // see also MethodCall_7
            public class MethodCall_27<A, B> {

                public void method(List<B> list, Predicate<B> b) {
                    b.test(list.get(0));
                }

                public void method(List<B> list, Consumer<B> b) {
                    b.accept(list.get(0));
                }

                public void method(List<A> list, BiConsumer<A, B> a) {
                    a.accept(list.get(0), null);
                }

                public void test(A a, B b) {
                    // COMPILATION ERROR: method(List.of(bb), System.out::println);
                    method(List.of(a), (x, y) -> System.out.println(x + " " + y));
                    method(List.of(b), x -> x.toString().length() > 3);
                }
            }
            """;

    @Test
    public void test27() {
        javaInspector.parse(INPUT27);
    }
    @Language("java")
    private static final String INPUT28 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.Arrays;
            import java.util.function.Supplier;

            public class MethodCall_28 {
                interface HasSize {
                    int size();
                }

                static class ImmutableArrayOfHasSize {

                    public final HasSize[] elements;

                    public ImmutableArrayOfHasSize(int size, Supplier<HasSize> generator) {
                        elements = new HasSize[size];
                        Arrays.setAll(elements, i -> generator.get());
                    }

                }
            }
            """;

    @Test
    public void test28() {
        javaInspector.parse(INPUT28);
    }
    @Language("java")
    private static final String INPUT29 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.Map;
            import java.util.TreeMap;
            import java.util.function.BiConsumer;

            // a variant in Import fails because of a static import, see Import_11
            public class MethodCall_29 {
                interface Variable {
                }

                interface DV {
                }

                private static class Node {
                    Map<Variable, DV> dependsOn;
                    final Variable variable;

                    private Node(Variable v) {
                        variable = v;
                    }
                }

                private final Map<Variable, Node> nodeMap = new TreeMap<>();

                public void visit(BiConsumer<Variable, Map<Variable, DV>> consumer) {
                    nodeMap.values().forEach(n -> consumer.accept(n.variable, n.dependsOn));
                }

            }

            """;

    @Test
    public void test29() {
        javaInspector.parse(INPUT29);
    }

}
