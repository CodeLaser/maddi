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
            
            import java.util.Collections;
            import java.util.List;
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


    @Language("java")
    private static final String INPUT6 = """
            package a.b;
            
            import java.util.Collections;
            import java.util.List;
            import java.util.Objects;
            
            public class C<T> {
                interface Runtime { }
                interface Expression { }
                interface Operator { }
                interface Parallel<T> { }
                interface CMParSeq<T> { int size(); Expression template(); List<T> toList(); }
                interface SeqPars<T> { }
                List<T> elements;
                Runtime runtime;
                record ParSeqElement<T>(Runtime runtime, T t) implements Parallel<T> { }
                void combineWithSeqPars(SeqPars<T> seqPars, Operator operator) {
                       List<Parallel<T>> converted = elements.stream()
                                    .map(e -> new ParSeqElement<>(runtime, e))
                                    .map(e -> (Parallel<T>) e).toList();
                }
            }
            """;

    @DisplayName("null virtual field")
    @Test
    public void test6() {
        TypeInfo C = javaInspector.parse(INPUT6);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(C);
    }


    @Language("java")
    private static final String INPUT7 = """
            package a.b;
            
            import java.util.ArrayList;
            import java.util.Collections;
            import java.util.List;
            import java.util.Objects;
            
            class C  {
                interface Runtime { }
                interface Expression { }
                interface Operator { }
                interface CMParSeq<T> { int size(); Expression template(); List<T> toList(); }
                interface Parallel<T> extends CMParSeq<T> { }
                interface EmptyParSeq<T> extends CMParSeq<T> { }
                record EmptyParSeqImpl(Runtime runtime) implements EmptyParSeq<?> { }
                interface SeqPars<T> { }
                static class SeqElements<T> implements CMParSeq<T> {
                    List<T> elements;
                    Runtime runtime;
                    Expression template;
                    record ParSeqElement<T>(Runtime runtime, T t) implements Parallel<T> { }
                    SeqElements(Runtime runtime, List<T> intersection, Expression template) {
                        this.elements = intersection;
                        this.runtime = runtime;
                        this.template = template;
                    }
                    void inParallelWith(CMParSeq<T> other, Operator operator) {
                        if (other instanceof SeqElements<T> ets) {
                            CMParSeq<T> intersection = intersection(ets);
                        }
                    }
                    private CMParSeq<T> intersection(CMParSeq<T> other) {
                        if (other instanceof EmptyParSeq<T>) return other;
                        if (other instanceof ParSeqElement<T> e) return contains(e.t()) ? other : new EmptyParSeqImpl<>(runtime);
                        if (other instanceof SeqElements<T> seq) {
                            List<T> intersection = new ArrayList<>(elements);
                            intersection.retainAll(seq.elements);
                            if (intersection.isEmpty()) return new EmptyParSeqImpl<>(runtime);
                            if (intersection.size() == 1) return new ParSeqElement<>(runtime, intersection.get(0));
                            return new SeqElements<>(runtime, intersection, template);
                        }
                        throw new UnsupportedOperationException();
                    }
                    private boolean contains(T t) {
                        return elements.contains(t);
                    }
                }
            }
            """;

    @DisplayName("could not reproduce the bug")
    @Test
    public void test7() {
        TypeInfo C = javaInspector.parse(INPUT7);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector,
                new LinkComputer.Options.Builder().setRecurse(true).setCheckDuplicateNames(false).build());
        tlc.doPrimaryType(C);
    }

    @Language("java")
    private static final String INPUT8 = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            class JSONParser {
                interface Node extends List<Node> { }
                NodeScope currentNodeScope;
                class NodeScope extends ArrayList<Node> {
                    NodeScope parentScope;
                    NodeScope() {
                        this.parentScope = JSONParser.this.currentNodeScope;
                        JSONParser.this.currentNodeScope = this;
                    }
                }
            }
            """;

    @DisplayName("cycle protection")
    @Test
    public void test8() {
        TypeInfo C = javaInspector.parse(INPUT8);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector,
                new LinkComputer.Options.Builder().setRecurse(true).setCheckDuplicateNames(false).build());
        tlc.doPrimaryType(C);
    }


    @Language("java")
    private static final String INPUT9 = """
            package a.b;
            import java.util.ArrayList;
            import java.util.HashMap;import java.util.List;import java.util.Map;import java.util.stream.Stream;
            class X {
                static <T> List<T> concatImmutable(List<T> l1, List<T> l2) {
                    return Stream.concat(l1.stream(), l2.stream()).toList();
                }
                interface Variable { }
                interface CMParSeq<T> { Template template(); }
                interface Template { }
                interface Expression { }
                record ByTemplate(Template template, int pos, List<CMParSeq<Variable>> list) {
                    ByTemplate merge(ByTemplate other) {
                        assert template.equals(other.template);
                        return new ByTemplate(template, Math.min(pos, other.pos), concatImmutable(list, other.list));
                    }
                }
                private static List<ByTemplate> groupByTemplate(List<CMParSeq<Variable>> parSeqs) {
                    Map<Expression, ByTemplate> map = new HashMap<>();
                    int index = 0;
                    for (CMParSeq<Variable> ps : parSeqs) {
                        map.merge(ps.template(), new ByTemplate(ps.template(), index, List.of(ps)), ByTemplate::merge);
                        index++;
                    }
                    return map.values().stream().filter(bt -> bt.list.size() > 1).toList();
                }
            }
            """;

    @DisplayName("§m mixes with other virtual fields")
    @Test
    public void test9() {
        TypeInfo C = javaInspector.parse(INPUT9);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector,
                new LinkComputer.Options.Builder().setRecurse(true).setCheckDuplicateNames(false).build());
        tlc.doPrimaryType(C);
    }
}
