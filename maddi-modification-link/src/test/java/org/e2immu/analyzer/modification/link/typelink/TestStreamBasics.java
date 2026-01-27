package org.e2immu.analyzer.modification.link.typelink;


import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.prepwork.variable.Links;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.e2immu.analyzer.modification.link.impl.LinkComputerImpl.VARIABLES_LINKED_TO_OBJECT;
import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
                Links tlv = vi.linkedVariablesOrEmpty();
                assertEquals("stream.§xs⊆0:in.§xs", tlv.toString());
            }
            {
                Statement statement = method.methodBody().statements().get(1);
                VariableData vd = VariableDataImpl.of(statement);
                VariableInfo vi = vd.variableInfo("stream1");
                Links tlv = vi.linkedVariablesOrEmpty();
                assertEquals("stream1.§xs⊆0:in.§xs,stream1.§xs⊆stream.§xs", tlv.toString());
            }

            MethodLinkedVariables tlv = method.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
            assertEquals("[-, -] --> method1.§xs⊆0:in.§xs", tlv.toString());
        }
        {
            MethodInfo method = C.findUniqueMethod("method", 2);
            MethodLinkedVariables tlv = method.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
            assertEquals("[-, -] --> method.§xs⊆0:in.§xs", tlv.toString());
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
        {
            MethodInfo method = C.findUniqueMethod("method1", 2);

            {
                Statement statement = method.methodBody().statements().get(2);
                VariableData vd = VariableDataImpl.of(statement);
                VariableInfo vi = vd.variableInfo("optional");
                Links tlv = vi.linkedVariablesOrEmpty();
                assertEquals("optional.§x∈0:in.§xs,optional.§x∈stream.§xs,optional.§x∈stream1.§xs",
                        tlv.toString());
            }
            {
                Statement statement = method.methodBody().statements().get(3);
                VariableData vd = VariableDataImpl.of(statement);
                VariableInfo vi = vd.variableInfo("x");
                Links tlv = vi.linkedVariablesOrEmpty();
                assertEquals("x←optional.§x,x∈0:in.§xs,x∈stream.§xs,x∈stream1.§xs", tlv.toString());
            }
            {
                MethodLinkedVariables tlv = method.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
                assertEquals("[-, -] --> method1∈0:in.§xs", tlv.toString());
            }
        }
        {
            MethodInfo method = C.findUniqueMethod("method", 2);
            MethodLinkedVariables tlv = method.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
            assertEquals("[-, -] --> method∈0:in.§xs", tlv.toString());
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

        {
            MethodInfo method = C.findUniqueMethod("method1", 1);
            MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));
            {
                Statement statement = method.methodBody().statements().getFirst();
                VariableData vd = VariableDataImpl.of(statement);
                VariableInfo vi = vd.variableInfo("stream");
                Links tlv = vi.linkedVariablesOrEmpty();
                assertEquals("stream.§xs⊆0:in.§xs", tlv.toString());
            }
            {
                Statement statement = method.methodBody().statements().get(1);
                VariableData vd = VariableDataImpl.of(statement);
                VariableInfo vi = vd.variableInfo("sorted");
                Links tlv = vi.linkedVariablesOrEmpty();
                assertEquals("sorted.§xs⊆0:in.§xs,sorted.§xs⊆stream.§xs", tlv.toString());
            }
            {
                Statement statement = method.methodBody().statements().get(2);
                VariableData vd = VariableDataImpl.of(statement);
                VariableInfo vi = vd.variableInfo("list");
                Links tlv = vi.linkedVariablesOrEmpty();
                assertEquals("list.§xs⊆0:in.§xs,list.§xs⊆sorted.§xs,list.§xs⊆stream.§xs", tlv.toString());
            }

            assertEquals("[-] --> method1.§xs⊆0:in.§xs", mlv.toString());
        }
        {
            MethodInfo method = C.findUniqueMethod("method", 1);
            MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));
            assertEquals("[-] --> method.§xs⊆0:in.§xs", mlv.toString());
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
                Links tlv = vi.linkedVariablesOrEmpty();
                assertEquals("stream.§$s⊆0:in.§$s", tlv.toString());
            }
            {
                Statement statement = method.methodBody().statements().get(1);
                VariableData vd = VariableDataImpl.of(statement);
                VariableInfo vi = vd.variableInfo("stream");
                Links tlv = vi.linkedVariablesOrEmpty();
                assertEquals("stream.§$s∋0:ii,stream.§$s⊆0:in.§$s", tlv.toString());

                MethodCall anyMatch = (MethodCall) ((LocalVariableCreation) statement).localVariable().assignmentExpression();
                Value.VariableBooleanMap tlvEntry = anyMatch.analysis().getOrDefault(VARIABLES_LINKED_TO_OBJECT,
                        ValueImpl.VariableBooleanMapImpl.EMPTY);
                // NOTE: ii not present
                assertEquals("""
                        a.b.C.method1(java.util.List<a.b.C.II>,String):0:in=false, \
                        stream=true\
                        """, tlvEntry.toString());
            }
        }
        {
            MethodInfo method = C.findUniqueMethod("method", 2);
            {
                Statement statement = method.methodBody().statements().getFirst();

                MethodCall anyMatch = (MethodCall) ((LocalVariableCreation) statement).localVariable().assignmentExpression();
                Value.VariableBooleanMap tlvEntry = anyMatch.analysis().getOrDefault(VARIABLES_LINKED_TO_OBJECT,
                        ValueImpl.VariableBooleanMapImpl.EMPTY);
                // NOTE: ii not present
                assertEquals("a.b.C.method(java.util.List<a.b.C.II>,String):0:in=true", tlvEntry.toString());
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
        {
            MethodInfo method = C.findUniqueMethod("method1", 1);
            MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));
            {
                Statement statement = method.methodBody().statements().get(1);
                VariableData vd = VariableDataImpl.of(statement);
                VariableInfo vi = vd.variableInfo("sorted");
                Links tlv = vi.linkedVariablesOrEmpty();
                assertEquals("sorted.§$s⊆0:in.§$s,sorted.§$s⊆stream.§$s", tlv.toString());
            }
            {
                Statement statement = method.methodBody().statements().get(2);
                VariableData vd = VariableDataImpl.of(statement);
                VariableInfo vi = vd.variableInfo("sorted");
                Links tlv = vi.linkedVariablesOrEmpty();
                assertEquals("sorted.§$s⊇array.§$s,sorted.§$s⊆0:in.§$s,sorted.§$s⊆stream.§$s", tlv.toString());
            }
            {
                Statement statement = method.methodBody().statements().get(3);
                VariableData vd = VariableDataImpl.of(statement);
                VariableInfo vi = vd.variableInfo("array");
                Links tlv = vi.linkedVariablesOrEmpty();
                assertEquals("""
                        array.§$s→method1.§$s,array.§$s⊆0:in.§$s,array.§$s⊆sorted.§$s,array.§$s⊆stream.§$s,array→method1\
                        """, tlv.toString());
            }
            // NOTE: because of the "@Independent(hcReturnValue = true)" force annotation, we lose the information of $
            assertEquals("[-] --> method1.§$s⊆0:in.§$s", mlv.toString());
        }

        // NOTE: because of the "@Independent(hcReturnValue = true)" force annotation, we lose the information of $
        MethodInfo method = C.findUniqueMethod("method", 1);
        MethodLinkedVariables tlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));
        assertEquals("[-] --> method.§$s⊆0:in.§$s", tlv.toString());
    }


}
