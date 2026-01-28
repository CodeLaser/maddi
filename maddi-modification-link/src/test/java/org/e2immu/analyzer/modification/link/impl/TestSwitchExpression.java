package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestSwitchExpression extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.List;
            public class X {
                List<String> list1;
                List<String> list2;
                public List<String> method(List<String> list3) {
                    return switch (list3.getFirst()) {
                        case "a" -> null;
                        case "B" -> list1;
                        case "c" -> {
                            if (list3.size()>1) yield list2;
                            yield list3;
                        }
                        default -> throw new UnsupportedOperationException();
                    };
                }
            }
            """;

    @DisplayName("switch expression with a number of difficulties")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        MethodInfo method = X.findUniqueMethod("method", 1);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        MethodLinkedVariables mlv = tlc.doMethod(method);
        assertEquals("[-] --> method←$_ce3,method←this.list1,method←this.list2,method←0:list3", mlv.toString());
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.List;
            public class X<T> {
                List<String> list1;
                List<String> list2;
                List<T> ints;
                public List<String> method(List<String> list3, T t) {
                    System.out.println(ints + " vs " + t);
                    return switch (list3.getFirst()) {
                        case "a" -> null;
                        case "B" -> list1;
                        case "c" -> {
                            if (list3.size()>1) yield list2;
                            ints.add(t);
                            yield list3;
                        }
                        default -> throw new UnsupportedOperationException();
                    };
                }
            }
            """;

    @DisplayName("switch expression side effects, variables known beforehand")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        MethodInfo method = X.findUniqueMethod("method", 2);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        MethodLinkedVariables mlv = tlc.doMethod(method);
        assertEquals("this, this.ints", mlv.sortedModifiedString());
        assertEquals("""
                [-, 1:t∈this.ints*.§ts] --> method←$_ce5,method←this*.list1,method←this*.list2,method←0:list3\
                """, mlv.toString());
    }


    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            import java.util.List;
            public class X<T> {
                List<String> list1;
                List<String> list2;
                List<T> ints;
                public List<String> method(List<String> list3, T t) {
                    return switch (list3.getFirst()) {
                        case "a" -> null;
                        case "B" -> list1;
                        case "c" -> {
                            if (list3.size()>1) yield list2;
                            ints.add(t);
                            yield list3;
                        }
                        default -> throw new UnsupportedOperationException();
                    };
                }
            }
            """;

    @DisplayName("switch expression side effects, variables not known beforehand")
    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse(INPUT3);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        MethodInfo method = X.findUniqueMethod("method", 2);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        MethodLinkedVariables mlv = tlc.doMethod(method);
        assertEquals("this, this.ints", mlv.sortedModifiedString());
        assertEquals("""
                [-, 1:t∈this.ints*.§ts] --> method←$_ce3,method←this*.list1,method←this*.list2,method←0:list3\
                """, mlv.toString());
    }

}
