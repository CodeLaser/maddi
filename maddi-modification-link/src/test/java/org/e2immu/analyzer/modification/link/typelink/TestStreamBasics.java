package org.e2immu.analyzer.modification.link.typelink;


import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.Links;
import org.e2immu.analyzer.modification.link.MethodLinkedVariables;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.link.impl.LinksImpl;
import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.statement.Statement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.e2immu.analyzer.modification.link.impl.LinksImpl.LINKS;
import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled
public class TestStreamBasics extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.List;
            import java.util.function.Predicate;
            import java.util.stream.Stream;
            public class C<X> {
                public List<X> method(List<X> in, Predicate<X> predicate) {
                    return in.stream().filter(predicate).toList();
                }
                public List<X> method1(List<X> in, Predicate<X> predicate) {
                    Stream<X> stream = in.stream();
                    Stream<X> stream1 = stream.filter(predicate);
                    List<X> stream2 = stream1.toList();
                    return stream2;
                }
            }
            """;

    @DisplayName("filter")
    @Test
    public void test1() {
        TypeInfo C = javaInspector.parse(INPUT1);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(C);
        {
            MethodInfo method = C.findUniqueMethod("method1", 2);

            {
                Statement statement = method.methodBody().statements().getFirst();
                VariableData vd = VariableDataImpl.of(statement);
                VariableInfo vi = vd.variableInfo("stream");
                Links tlv = vi.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
                assertEquals("""
                        in(*[Type java.util.List<X>]:*[Type java.util.List<X>])\
                        """, tlv.toString());
            }
            {
                Statement statement = method.methodBody().statements().get(1);
                VariableData vd = VariableDataImpl.of(statement);
                VariableInfo vi = vd.variableInfo("stream1");
                Links tlv = vi.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
                assertEquals("""
                        in(*[Type java.util.List<X>]:*[Type java.util.List<X>]);\
                        stream(*[Type java.util.stream.Stream<T>]:*[Type java.util.stream.Stream<X>])\
                        """, tlv.toString());
            }

            MethodLinkedVariables tlv = method.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
            assertEquals("""
                    in(*[Type java.util.List<X>]:*[Type java.util.List<X>])\
                    """, tlv.toString());
        }
        {
            MethodInfo method = C.findUniqueMethod("method", 2);
            MethodLinkedVariables tlv = method.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
            assertEquals("""
                    in(*[Type java.util.stream.Stream<T>]:*[Type java.util.List<X>])\
                    """, tlv.toString());
        }
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.List;
            import java.util.Optional;
            import java.util.function.Predicate;
            import java.util.stream.Stream;
            public class C<X> {
                public X method(List<X> in, Predicate<X> predicate) {
                    return in.stream().filter(predicate).findFirst().orElseThrow();
                }
                public List<X> method1(List<X> in, Predicate<X> predicate) {
                    Stream<X> stream = in.stream();
                    Stream<X> stream1 = stream.filter(predicate);
                    Optional<X> optional = stream1.findFirst();
                    X x = optional.orElseThrow();
                    return x;
                }
            }
            """;

    @DisplayName("find first")
    @Test
    public void test2() {
        TypeInfo C = javaInspector.parse(INPUT2);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(C);
        String METHOD = """
                in(*[Type param X]:0[Type java.util.List<X>])\
                """;
        {
            MethodInfo method = C.findUniqueMethod("method1", 2);

            {
                Statement statement = method.methodBody().statements().get(2);
                VariableData vd = VariableDataImpl.of(statement);
                VariableInfo vi = vd.variableInfo("optional");
                Links tlv = vi.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
                assertEquals("""
                        in(*[Type java.util.List<X>]:*[Type java.util.List<X>]);\
                        stream(*[Type java.util.stream.Stream<T>]:*[Type java.util.stream.Stream<X>]);\
                        stream1(*[Type java.util.stream.Stream<X>]:*[Type java.util.stream.Stream<X>])\
                        """, tlv.toString());
            }
            {
                Statement statement = method.methodBody().statements().get(3);
                VariableData vd = VariableDataImpl.of(statement);
                VariableInfo vi = vd.variableInfo("x");
                Links tlv = vi.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
                assertEquals("""
                        in(*[Type param X]:0[Type java.util.List<X>]);\
                        optional(*[Type param X]:0[Type java.util.Optional<X>]);\
                        stream(*[Type param T]:0[Type java.util.stream.Stream<T>]);\
                        stream1(*[Type param X]:0[Type java.util.stream.Stream<X>])\
                        """, tlv.toString());
            }
            {
                MethodLinkedVariables tlv = method.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
                assertEquals(METHOD, tlv.toString());
            }
        }
        {
            MethodInfo method = C.findUniqueMethod("method", 2);
            MethodLinkedVariables tlv = method.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
            // FIXME this would be better  assertEquals(METHOD, tlv.toString());
            assertEquals("in(*[Type param T]:0[Type java.util.stream.Stream<T>])", tlv.toString());
        }
    }


    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            import java.util.List;
            import java.util.Optional;
            import java.util.Set;
            import java.util.function.Predicate;
            import java.util.stream.Stream;
            public class C<X extends Comparable<? super X>> {
                public X method(Set<X> in) {
                    return in.stream().sorted().toList();
                }
                public List<X> method1(Set<X> in) {
                   Stream<X> stream = in.stream();
                   Stream<X> sorted = stream.sorted();
                   List<X> list = sorted.toList();
                   return list;
                }
            }
            """;

    @DisplayName("sort + toList")
    @Test
    public void test3() {
        TypeInfo C = javaInspector.parse(INPUT3);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(C);
        // TODO List would be better on LHS
        String METHOD = """
                in(*[Type java.util.Set<X extends Comparable<? super X>>]:*[Type java.util.Set<X extends Comparable<? super X>>])\
                """;
        {
            MethodInfo method = C.findUniqueMethod("method1", 1);

            {
                Statement statement = method.methodBody().statements().get(1);
                VariableData vd = VariableDataImpl.of(statement);
                VariableInfo vi = vd.variableInfo("sorted");
                Links tlv = vi.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
                assertEquals("""
                        in(*[Type java.util.Set<X extends Comparable<? super X>>]:*[Type java.util.Set<X extends Comparable<? super X>>]);\
                        stream(*[Type java.util.stream.Stream<X extends Comparable<? super X>>]:*[Type java.util.stream.Stream<X extends Comparable<? super X>>])\
                        """, tlv.toString());
            }
            {
                Statement statement = method.methodBody().statements().get(2);
                VariableData vd = VariableDataImpl.of(statement);
                VariableInfo vi = vd.variableInfo("list");
                Links tlv = vi.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
                assertEquals("""
                        in(*[Type java.util.Set<X extends Comparable<? super X>>]:*[Type java.util.Set<X extends Comparable<? super X>>]);\
                        sorted(*[Type java.util.stream.Stream<X extends Comparable<? super X>>]:*[Type java.util.stream.Stream<X extends Comparable<? super X>>]);\
                        stream(*[Type java.util.stream.Stream<X extends Comparable<? super X>>]:*[Type java.util.stream.Stream<X extends Comparable<? super X>>])\
                        """, tlv.toString());
            }
            {
                MethodLinkedVariables tlv = method.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
                assertEquals(METHOD, tlv.toString());
            }
        }
        {
            MethodInfo method = C.findUniqueMethod("method", 1);
            MethodLinkedVariables tlv = method.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
            assertEquals(METHOD, tlv.toString());
        }
    }


    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            import java.util.List;
            import java.util.stream.Stream;
            public class C {
                interface I { }
                // method2 plays the role of the offending method
                static class II implements I { String method2(String s) { return s+"?"; } }
                void method(List<II> in, String s) {
                    boolean b = in.stream().anyMatch(ii -> ii.method2(s).length()>5);
                }
                void method1(List<II> in, String s) {
                    Stream<II> stream = in.stream();
                    boolean b = stream.anyMatch(ii -> ii.method2(s).length()>5);
                }
            }
            """;

    @DisplayName("anyMatch")
    @Test
    public void test4() {
        TypeInfo C = javaInspector.parse(INPUT4);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(C);
        {
            MethodInfo method = C.findUniqueMethod("method1", 2);
            {
                Statement statement = method.methodBody().statements().getFirst();
                VariableData vd = VariableDataImpl.of(statement);
                VariableInfo vi = vd.variableInfo("stream");
                Links tlv = vi.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
                assertEquals("""
                        in(*[Type java.util.List<a.b.C.II>]:*[Type java.util.List<a.b.C.II>])\
                        """, tlv.toString());
            }
            {
                Statement statement = method.methodBody().statements().get(1);
                VariableData vd = VariableDataImpl.of(statement);
                VariableInfo vi = vd.variableInfo("stream");
                Links tlv = vi.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
                assertEquals("""
                        ii(0[Type java.util.stream.Stream<a.b.C.II>]:*[Type a.b.C.II]);\
                        in(*[Type java.util.List<a.b.C.II>]:*[Type java.util.List<a.b.C.II>])\
                        """, tlv.toString());

                MethodCall anyMatch = (MethodCall) ((LocalVariableCreation) statement).localVariable().assignmentExpression();
                Links tlvEntry = anyMatch.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
                assertEquals("""
                        ii(0[Type java.util.stream.Stream<a.b.C.II>]:*[Type a.b.C.II]);\
                        in(*[Type java.util.List<a.b.C.II>]:*[Type java.util.List<a.b.C.II>]);\
                        stream(*[Type java.util.stream.Stream<a.b.C.II>]:*[Type java.util.stream.Stream<a.b.C.II>])\
                        """, tlvEntry.toString());
            }
        }
        {
            MethodInfo method = C.findUniqueMethod("method", 2);
            {
                Statement statement = method.methodBody().statements().getFirst();

                MethodCall anyMatch = (MethodCall) ((LocalVariableCreation) statement).localVariable().assignmentExpression();
                Links tlvEntry = anyMatch.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
                assertEquals("""
                        ii(0[Type java.util.List<a.b.C.II>]:*[Type a.b.C.II]);\
                        in(*[Type java.util.List<a.b.C.II>]:*[Type java.util.List<a.b.C.II>])\
                        """, tlvEntry.toString());
            }

        }

    }


    @Language("java")
    private static final String INPUT5 = """
            package a.b;
            import java.util.Set;
            import java.util.stream.Stream;
            public class C {
                public String[] method(Set<String> in) {
                    return in.stream().sorted().toArray(String[]::new);
                }
                public String[] method1(Set<String> in) {
                   Stream<String> stream = in.stream();
                   Stream<String> sorted = stream.sorted();
                   String[] array = sorted.toArray(String[]::new);
                   return array;
                }
            }
            """;

    @DisplayName("sort + toArray")
    @Test
    public void test5() {
        TypeInfo C = javaInspector.parse(INPUT5);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(C);
        // TODO List would be better on LHS
        String METHOD = """
                in(*[Type java.util.Set<String>]:*[Type java.util.Set<String>])\
                """;
        {
            MethodInfo method = C.findUniqueMethod("method1", 1);

            {
                Statement statement = method.methodBody().statements().get(1);
                VariableData vd = VariableDataImpl.of(statement);
                VariableInfo vi = vd.variableInfo("sorted");
                Links tlv = vi.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
                assertEquals("""
                        in(*[Type java.util.Set<String>]:*[Type java.util.Set<String>]);\
                        stream(*[Type java.util.stream.Stream<String>]:*[Type java.util.stream.Stream<String>])\
                        """, tlv.toString());
            }
            {
                Statement statement = method.methodBody().statements().get(2);
                VariableData vd = VariableDataImpl.of(statement);
                VariableInfo vi = vd.variableInfo("sorted");
                Links tlv = vi.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
                assertEquals("""
                        in(*[Type java.util.Set<String>]:*[Type java.util.Set<String>]);\
                        stream(*[Type java.util.stream.Stream<String>]:*[Type java.util.stream.Stream<String>])\
                        """, tlv.toString());
            }
            {
                Statement statement = method.methodBody().statements().get(3);
                VariableData vd = VariableDataImpl.of(statement);
                VariableInfo vi = vd.variableInfo("array");
                Links tlv = vi.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
                assertEquals("""
                        in(*[Type java.util.Set<String>]:*[Type java.util.Set<String>]);\
                        sorted(*[Type param A[]]:*[Type java.util.stream.Stream<String>]);\
                        stream(*[Type java.util.stream.Stream<String>]:*[Type java.util.stream.Stream<String>])\
                        """, tlv.toString());
            }
            {
                MethodLinkedVariables tlv = method.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
                assertEquals(METHOD, tlv.toString());
            }
        }
        {
            MethodInfo method = C.findUniqueMethod("method", 1);
            MethodLinkedVariables tlv = method.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
            // FIXME this would be better! assertEquals(METHOD, tlv.toString());
            assertEquals("in(*[Type param A[]]:*[Type java.util.Set<String>])", tlv.toString());
        }
    }


}
