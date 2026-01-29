package org.e2immu.analyzer.modification.link.impl2;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}
