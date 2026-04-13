package org.e2immu.analyzer.modification.link.impl.basics;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.link.impl.linkgraph.Graph;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class TestSimpleSharedVariable extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.List;
            class X {
                String method(List<String> in) {
                    List<String> copy = in;
                    return copy.getFirst();
                }
            }
            """;

    @DisplayName("direct assignment")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector, (statementIndex, graph) -> {
            if ("1".equals(statementIndex)) {
                Assertions.assertEquals("""
                        return method ∈ $__sv_copy.§$s   1(return method ∈ $__sv_copy.§$s)
                        $__sv_copy.§$s ∋ return method   1($__sv_copy.§$s ∋ return method)
                        """, graph.printClosure());
            }
        });
        MethodInfo method = X.findUniqueMethod("method", 1);
        MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));
        assertEquals("[-] --> method∈0:in.§$s", mlv.toString());
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.List;
            class X {
                String method(List<String> in) {
                    List<String> copy = in;
                    String first = copy.getFirst();
                    return first;
                }
            }
            """;

    @DisplayName("away from return")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector, (statementIndex, graph) -> {
            if ("1".equals(statementIndex)) {
                Assertions.assertEquals("""
                        first ∈ $__sv_copy.§$s   1(first ∈ $__sv_copy.§$s)
                        $__sv_copy.§$s ∋ first   1($__sv_copy.§$s ∋ first)
                        """, graph.printClosure());
            }
            if ("2".equals(statementIndex)) {
                Assertions.assertEquals("""
                        $__sv_return method ∈ $__sv_copy.§$s   2($__sv_return method ∈ $__sv_copy.§$s)
                        $__sv_copy.§$s ∋ $__sv_return method   2($__sv_copy.§$s ∋ $__sv_return method)
                        """, graph.printClosure());
            }
        });
        MethodInfo method = X.findUniqueMethod("method", 1);
        MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));
        assertEquals("[-] --> method∈0:in.§$s", mlv.toString());
    }


    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            class X {
                String method(List<String> in) {
                    List<String> copy = new ArrayList<>(in);
                    String first = copy.getFirst();
                    return first;
                }
            }
            """;

    @DisplayName("add new ArrayList")
    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse(INPUT3);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector, (statementIndex, graph) -> {
            if ("0".equals(statementIndex)) {
                assertEquals("""
                        0:in.§$s ⊇ copy.§$s   0(0:in.§$s ⊇ copy.§$s)
                        copy.§$s ⊆ 0:in.§$s   0(copy.§$s ⊆ 0:in.§$s)
                        """, graph.printClosure());
            }
            // (3 choose 2) = 3 combinations; always x2 because of symmetry
            if ("1".equals(statementIndex)) {
                assertEquals("""
                        first ∈ 0:in.§$s   *[first ∈ copy.§$s, copy.§$s ⊆ 0:in.§$s]
                        first ∈ copy.§$s   1(first ∈ copy.§$s)
                        0:in.§$s ∋ first   *[0:in.§$s ⊇ copy.§$s, copy.§$s ∋ first]
                        0:in.§$s ⊇ copy.§$s   0(0:in.§$s ⊇ copy.§$s)
                        copy.§$s ∋ first   1(copy.§$s ∋ first)
                        copy.§$s ⊆ 0:in.§$s   0(copy.§$s ⊆ 0:in.§$s)
                        """, graph.printClosure());
            }
            // now comes a new variable, but because it goes into an equivalence group, it remains (3 choose 2)
            // the alternative was 6 instead of 2 related to first+return
            if ("2".equals(statementIndex)) {
                assertEquals("""
                        $__sv_return method ∈ 0:in.§$s   *[$__sv_return method ∈ copy.§$s, copy.§$s ⊆ 0:in.§$s]
                        $__sv_return method ∈ copy.§$s   2($__sv_return method ∈ copy.§$s)
                        0:in.§$s ∋ $__sv_return method   *[0:in.§$s ⊇ copy.§$s, copy.§$s ∋ $__sv_return method]
                        0:in.§$s ⊇ copy.§$s   0(0:in.§$s ⊇ copy.§$s)
                        copy.§$s ∋ $__sv_return method   2(copy.§$s ∋ $__sv_return method)
                        copy.§$s ⊆ 0:in.§$s   0(copy.§$s ⊆ 0:in.§$s)
                        """, graph.printClosure());
            }
        });
        MethodInfo method = X.findUniqueMethod("method", 1);
        MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));
        assertEquals("[-] --> method∈0:in.§$s", mlv.toString());
    }


    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            import java.util.ArrayList;import java.util.Collections;
            import java.util.List;
            class X {
                String field;
                String second;
                List<String> method(List<String> in) {
                    List<String> copy = in;
                    copy.add(field);
                    copy = new ArrayList<>();
                    copy.add(second);
                    return copy;
                }
            }
            """;

    @DisplayName("Simple reassignment")
    @Test
    public void test4() {
        TypeInfo X = javaInspector.parse(INPUT4);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        LinkComputerImpl linkComputer = new LinkComputerImpl(javaInspector, new LinkComputerImpl.TestVisitor() {
            @Override
            public void visit(String statementIndex, Graph graph) {
                if ("1".equals(statementIndex)) {
                    assertEquals("""
                            this.field ∈ $__sv_copy.§$s   1(this.field ∈ $__sv_copy.§$s)
                            $__sv_copy.§$s ∋ this.field   1($__sv_copy.§$s ∋ this.field)
                            """, graph.printClosure());
                }
            }
        });
        MethodInfo method = X.findUniqueMethod("method", 1);
        MethodLinkedVariables mlvListAdd = method.analysis().getOrCreate(METHOD_LINKS,
                () -> linkComputer.doMethod(method));

        VariableData vd2 = VariableDataImpl.of(method.methodBody().statements().get(2));
        VariableInfo field2 = vd2.variableInfo("a.b.X.field");
        assertEquals("this.field∈0:in.§$s", field2.linkedVariables().toString());
        VariableInfo copy2 = vd2.variableInfo("copy");
        // '-' because we're not tracking the intermediary variable
        assertEquals("-", copy2.linkedVariables().toString());

        VariableData vd3 = VariableDataImpl.of(method.methodBody().statements().get(3));
        VariableInfo copy3 = vd3.variableInfo("copy");
        assertEquals("copy.§$s∋this.second", copy3.linkedVariables().toString());

        // important that this.second not part of 0:in
        assertEquals("[0:in*.§$s∋this.field] --> method.§$s∋this.second", mlvListAdd.toString());
    }

}
