package org.e2immu.analyzer.modification.link.impl2;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.language.cst.api.expression.Lambda;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class Test1 extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.net.URI;
            public class C {
                 private static String makeMessage(URI uri, Object where, String msg, Throwable throwable) {
                     return (throwable == null ? "" : "Exception: " + throwable.getClass().getCanonicalName() + "\\n")
                            + "In: " + uri + (uri == where || where == null ? "" : "\\nIn: " + where) + "\\nMessage: " + msg;
                 }
            }
            """;

    @DisplayName("assertion error in LinkImpl constructor")
    @Test
    public void test1() {
        TypeInfo C = javaInspector.parse(INPUT1);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector,
                new LinkComputer.Options.Builder().setRecurse(true).setCheckDuplicateNames(true).build());
        tlc.doPrimaryType(C);
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.Collection;
            import java.util.List;
            import java.util.Map;
            public class C {
                private static final Map<String, List<Integer>> mapList = Map.of("a", List.of(1, 2),
                            "b", List.of(3), "c", List.of(), "d", List.of(4, 5, 6));
            
                private int method() {
                    int sum = 1;
                    sum += mapList.values().stream()
                                .flatMap(Collection::stream)
                                .mapToInt(i -> i)
                                .sum();
                    return sum;
                 }
            }
            """;

    @DisplayName("wrong link in LMC.linksBetweenParameters")
    @Test
    public void test2() {
        TypeInfo C = javaInspector.parse(INPUT2);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector,
                new LinkComputer.Options.Builder().setRecurse(true).setCheckDuplicateNames(true).build());
        tlc.doPrimaryType(C);
    }


    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            import org.e2immu.annotation.Independent;import java.util.HashMap;import java.util.Map;import java.util.Objects;
            public class C<K, V> {
                private final Map<K, V> map = new HashMap<>();
                public void put(K k, V v) {
                    Objects.requireNonNull(k);
                    Objects.requireNonNull(v);
                    map.put(k, v);
                }
                public void putAll(@Independent(hc = true) C<K, V> setOnceMap) {
                    setOnceMap.map.forEach(this::put);
                }
            }
            """;

    @DisplayName("null owner")
    @Test
    public void test3() {
        TypeInfo C = javaInspector.parse(INPUT3);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector,
                new LinkComputer.Options.Builder().setRecurse(true).setCheckDuplicateNames(true).build());
        tlc.doPrimaryType(C);
    }


    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            import java.util.List;
            import java.util.Map;
            import java.util.Stack;
            import java.util.function.BiConsumer;
            public class C<T> {
                private static class TrieNode<T> {
                    List<T> data;
                    Map<String, TrieNode<T>> map;
                }
                private static <T> void recursivelyVisit(TrieNode<T> node,
                                                          Stack<String> strings,
                                                          BiConsumer<String[], List<T>> visitor) {
                    if (node.data != null) {
                        visitor.accept(strings.toArray(String[]::new), node.data);
                    }
                    if (node.map != null) {
                        node.map.forEach((s, n) -> {
                            strings.push(s);
                            recursivelyVisit(n, strings, visitor);
                            strings.pop();
                        });
                    }
                }
            }
            """;

    @DisplayName("recursive method")
    @Test
    public void test4() {
        TypeInfo C = javaInspector.parse(INPUT4);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        MethodInfo recursivelyVisit = C.findUniqueMethod("recursivelyVisit", 3);
        MethodCall forEach = (MethodCall) recursivelyVisit.methodBody().statements().getLast().block().statements()
                .getFirst().expression();
        Lambda lambda = (Lambda) forEach.parameterExpressions().getFirst();
        lambda.methodInfo().analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(lambda.methodInfo()));
        ParameterInfo strings = recursivelyVisit.parameters().get(1);
        assertFalse(strings.isUnmodified());
    }


    @Language("java")
    private static final String INPUT5 = """
            package a.b;
            
            import java.util.Collections;import java.util.List;
            import java.util.Objects;
            
            public class C<T> {
                interface Runtime { }
                interface Expression { }
                interface Operator { }
                interface Parallel<T> { }
                interface CMParSeq<T> { int size(); Expression template(); List<T> toList(); }
                record ParSeqs<T extends Comparable<? super T>>(Runtime runtime,
                                                                List<CMParSeq<T>> parSeqs,
                                                                Expression template,
                                                                Operator operator) implements Parallel<T> {
                    public ParSeqs {
                        assert operator != null;
                        assert template != null;
                        String msg = compatibleWithParSeqs(parSeqs);
                        assert msg == null : msg;
                    }
                    public static <T> String compatibleWithParSeqs(List<CMParSeq<T>> parSeqs) {
                        //... not relevant to this test
                        return null;
                    }
                }
            }
            """;

    @DisplayName("limitation in GenericsHelper.translateMap")
    @Test
    public void test5() {
        TypeInfo C = javaInspector.parse(INPUT5);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(C);
    }
}
