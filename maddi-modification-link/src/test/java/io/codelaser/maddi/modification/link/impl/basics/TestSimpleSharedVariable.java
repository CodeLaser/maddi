package io.codelaser.maddi.modification.link.impl.basics;

import io.codelaser.maddi.modification.link.CommonTest;
import io.codelaser.maddi.modification.link.LinkComputer;
import io.codelaser.maddi.modification.link.impl.LinkComputerImpl;
import io.codelaser.maddi.modification.link.impl.linkgraph.Graph;
import io.codelaser.maddi.modification.prepwork.PrepAnalyzer;
import io.codelaser.maddi.modification.prepwork.variable.MethodLinkedVariables;
import io.codelaser.maddi.modification.prepwork.variable.VariableData;
import io.codelaser.maddi.modification.prepwork.variable.VariableInfo;
import io.codelaser.maddi.modification.prepwork.variable.impl.VariableDataImpl;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.codelaser.maddi.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


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
        TypeInfo X = javaInspector.parse("a.b.X", INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector, (statementIndex, graph) -> {
            if ("1".equals(statementIndex)) {
                Assertions.assertEquals("""
                        $__sv_copy ≻ $__sv_copy.§$s   1($__sv_copy ≻ $__sv_copy.§$s)
                        return method ≤ $__sv_copy   *[return method ∈ $__sv_copy.§$s, $__sv_copy.§$s ≺ $__sv_copy]
                        return method ∈ $__sv_copy.§$s   1(return method ∈ $__sv_copy.§$s)
                        $__sv_copy.§$s ≺ $__sv_copy   1($__sv_copy.§$s ≺ $__sv_copy)
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
        TypeInfo X = javaInspector.parse("a.b.X", INPUT2);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector, (statementIndex, graph) -> {
            if ("1".equals(statementIndex)) {
                Assertions.assertEquals("""
                        $__sv_copy ≥ first   *[$__sv_copy ≻ $__sv_copy.§$s, $__sv_copy.§$s ∋ first]
                        $__sv_copy ≻ $__sv_copy.§$s   1($__sv_copy ≻ $__sv_copy.§$s)
                        first ≤ $__sv_copy   *[first ∈ $__sv_copy.§$s, $__sv_copy.§$s ≺ $__sv_copy]
                        first ∈ $__sv_copy.§$s   1(first ∈ $__sv_copy.§$s)
                        $__sv_copy.§$s ≺ $__sv_copy   1($__sv_copy.§$s ≺ $__sv_copy)
                        $__sv_copy.§$s ∋ first   1($__sv_copy.§$s ∋ first)
                        """, graph.printClosure());
            }
            if ("2".equals(statementIndex)) {
                Assertions.assertEquals("""
                        $__sv_copy ≥ $__sv_return method   *[$__sv_copy ≻ $__sv_copy.§$s, $__sv_copy.§$s ∋ $__sv_return method]
                        $__sv_copy ≻ $__sv_copy.§$s   1($__sv_copy ≻ $__sv_copy.§$s)
                        $__sv_return method ≤ $__sv_copy   *[$__sv_return method ∈ $__sv_copy.§$s, $__sv_copy.§$s ≺ $__sv_copy]
                        $__sv_return method ∈ $__sv_copy.§$s   2($__sv_return method ∈ $__sv_copy.§$s)
                        $__sv_copy.§$s ≺ $__sv_copy   1($__sv_copy.§$s ≺ $__sv_copy)
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
        TypeInfo X = javaInspector.parse("a.b.X", INPUT3);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector, (statementIndex, graph) -> {
            if ("0".equals(statementIndex)) {
                assertEquals("""
                        0:in ∩ copy   *[0:in ≥ copy.§$s, copy.§$s ≺ copy] support: 0:in ≻ 0:in.§$s, 0:in.§$s ⊇ copy.§$s
                        0:in ≻ 0:in.§$s   0(0:in ≻ 0:in.§$s)
                        0:in ≥ copy.§$s   *[0:in ≻ 0:in.§$s, 0:in.§$s ⊇ copy.§$s]
                        copy ∩ 0:in   *[copy ≻ copy.§$s, copy.§$s ≤ 0:in] support: 0:in.§$s ≺ 0:in, copy.§$s ⊆ 0:in.§$s
                        copy ∩ 0:in.§$s   *[copy ≻ copy.§$s, copy.§$s ⊆ 0:in.§$s]
                        copy ≻ copy.§$s   0(copy ≻ copy.§$s)
                        0:in.§$s ≺ 0:in   0(0:in.§$s ≺ 0:in)
                        0:in.§$s ∩ copy   *[0:in.§$s ⊇ copy.§$s, copy.§$s ≺ copy]
                        0:in.§$s ⊇ copy.§$s   0(0:in.§$s ⊇ copy.§$s)
                        copy.§$s ≤ 0:in   *[copy.§$s ⊆ 0:in.§$s, 0:in.§$s ≺ 0:in]
                        copy.§$s ≺ copy   0(copy.§$s ≺ copy)
                        copy.§$s ⊆ 0:in.§$s   0(copy.§$s ⊆ 0:in.§$s)
                        """, graph.printClosure());
            }
            // (3 choose 2) = 3 combinations; always x2 because of symmetry
            if ("1".equals(statementIndex)) {
                assertEquals("""
                        0:in ∩ copy   *[0:in ≥ copy.§$s, copy.§$s ≺ copy] support: 0:in ≻ 0:in.§$s, 0:in.§$s ⊇ copy.§$s
                        0:in ≥ first   *[0:in ≻ 0:in.§$s, 0:in.§$s ∋ first] support: 0:in.§$s ⊇ copy.§$s, copy.§$s ∋ first
                        0:in ≻ 0:in.§$s   0(0:in ≻ 0:in.§$s)
                        0:in ≥ copy.§$s   *[0:in ≥ first, first ∈ copy.§$s] support: 0:in ≻ 0:in.§$s, 0:in.§$s ⊇ copy.§$s, copy.§$s ∋ first
                        copy ∩ 0:in   *[copy ≻ copy.§$s, copy.§$s ≤ 0:in] support: 0:in.§$s ≺ 0:in, copy.§$s ⊆ 0:in.§$s
                        copy ≥ first   *[copy ≻ copy.§$s, copy.§$s ∋ first]
                        copy ∩ 0:in.§$s   *[copy ≥ first, first ∈ 0:in.§$s] support: copy ≻ copy.§$s, copy.§$s ∋ first, copy.§$s ⊆ 0:in.§$s, first ∈ copy.§$s
                        copy ≻ copy.§$s   0(copy ≻ copy.§$s)
                        first ≤ 0:in   *[first ∈ 0:in.§$s, 0:in.§$s ≺ 0:in] support: copy.§$s ⊆ 0:in.§$s, first ∈ copy.§$s
                        first ≤ copy   *[first ∈ copy.§$s, copy.§$s ≺ copy]
                        first ∈ 0:in.§$s   *[first ∈ copy.§$s, copy.§$s ⊆ 0:in.§$s]
                        first ∈ copy.§$s   1(first ∈ copy.§$s)
                        0:in.§$s ≺ 0:in   0(0:in.§$s ≺ 0:in)
                        0:in.§$s ∩ copy   *[0:in.§$s ∋ first, first ≤ copy] support: 0:in.§$s ⊇ copy.§$s, copy.§$s ∋ first, copy.§$s ≺ copy, first ∈ copy.§$s
                        0:in.§$s ∋ first   *[0:in.§$s ⊇ copy.§$s, copy.§$s ∋ first]
                        0:in.§$s ⊇ copy.§$s   0(0:in.§$s ⊇ copy.§$s)
                        copy.§$s ≤ 0:in   *[copy.§$s ∋ first, first ≤ 0:in] support: 0:in.§$s ≺ 0:in, copy.§$s ⊆ 0:in.§$s, first ∈ copy.§$s
                        copy.§$s ≺ copy   0(copy.§$s ≺ copy)
                        copy.§$s ∋ first   1(copy.§$s ∋ first)
                        copy.§$s ⊆ 0:in.§$s   0(copy.§$s ⊆ 0:in.§$s)
                        """, graph.printClosure());
            }
            // now comes a new variable, but because it goes into an equivalence group, it remains (3 choose 2)
            // the alternative was 6 instead of 2 related to first+return
            if ("2".equals(statementIndex)) {
                assertEquals("""
                        $__sv_return method ≤ 0:in   *[$__sv_return method ∈ 0:in.§$s, 0:in.§$s ≺ 0:in] support: $__sv_return method ∈ copy.§$s, copy.§$s ⊆ 0:in.§$s
                        $__sv_return method ≤ copy   *[$__sv_return method ∈ copy.§$s, copy.§$s ≺ copy]
                        $__sv_return method ∈ 0:in.§$s   *[$__sv_return method ∈ copy.§$s, copy.§$s ⊆ 0:in.§$s]
                        $__sv_return method ∈ copy.§$s   2($__sv_return method ∈ copy.§$s)
                        0:in ≥ $__sv_return method   *[0:in ≻ 0:in.§$s, 0:in.§$s ∋ $__sv_return method] support: 0:in.§$s ⊇ copy.§$s, copy.§$s ∋ $__sv_return method
                        0:in ∩ copy   *[0:in ≥ copy.§$s, copy.§$s ≺ copy] support: 0:in ≻ 0:in.§$s, 0:in.§$s ⊇ copy.§$s
                        0:in ≻ 0:in.§$s   0(0:in ≻ 0:in.§$s)
                        0:in ≥ copy.§$s   mat(0:in ≥ copy.§$s)
                        copy ≥ $__sv_return method   *[copy ≻ copy.§$s, copy.§$s ∋ $__sv_return method]
                        copy ∩ 0:in   *[copy ≻ copy.§$s, copy.§$s ≤ 0:in] support: 0:in.§$s ≺ 0:in, copy.§$s ⊆ 0:in.§$s
                        copy ∩ 0:in.§$s   mat(copy ∩ 0:in.§$s)
                        copy ≻ copy.§$s   0(copy ≻ copy.§$s)
                        0:in.§$s ∋ $__sv_return method   *[0:in.§$s ⊇ copy.§$s, copy.§$s ∋ $__sv_return method]
                        0:in.§$s ≺ 0:in   0(0:in.§$s ≺ 0:in)
                        0:in.§$s ∩ copy   mat(0:in.§$s ∩ copy)
                        0:in.§$s ⊇ copy.§$s   0(0:in.§$s ⊇ copy.§$s)
                        copy.§$s ∋ $__sv_return method   2(copy.§$s ∋ $__sv_return method)
                        copy.§$s ≤ 0:in   mat(copy.§$s ≤ 0:in)
                        copy.§$s ≺ copy   0(copy.§$s ≺ copy)
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
        TypeInfo X = javaInspector.parse("a.b.X", INPUT4);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        LinkComputerImpl linkComputer = new LinkComputerImpl(javaInspector, (statementIndex, graph) -> {
            if ("1".equals(statementIndex) || "2".equals(statementIndex)) {
                Assertions.assertEquals("""
                        $__sv_copy ≥ this.field   *[$__sv_copy ≻ $__sv_copy.§$s, $__sv_copy.§$s ∋ this.field]
                        $__sv_copy ≻ $__sv_copy.§$s   1($__sv_copy ≻ $__sv_copy.§$s)
                        this.field ≤ $__sv_copy   *[this.field ∈ $__sv_copy.§$s, $__sv_copy.§$s ≺ $__sv_copy]
                        this.field ∈ $__sv_copy.§$s   1(this.field ∈ $__sv_copy.§$s)
                        $__sv_copy.§$s ≺ $__sv_copy   1($__sv_copy.§$s ≺ $__sv_copy)
                        $__sv_copy.§$s ∋ this.field   1($__sv_copy.§$s ∋ this.field)
                        """, graph.printClosure());
            }
            if ("3".equals(statementIndex)) {
                Assertions.assertEquals("""
                        $__sv_copy ≥ this.field   *[$__sv_copy ≻ $__sv_copy.§$s, $__sv_copy.§$s ∋ this.field]
                        $__sv_copy ≻ $__sv_copy.§$s   1($__sv_copy ≻ $__sv_copy.§$s)
                        this.field ≤ $__sv_copy   *[this.field ∈ $__sv_copy.§$s, $__sv_copy.§$s ≺ $__sv_copy]
                        this.field ∈ $__sv_copy.§$s   1(this.field ∈ $__sv_copy.§$s)
                        this.second ≤ copy   *[this.second ∈ copy.§$s, copy.§$s ≺ copy]
                        this.second ∈ copy.§$s   3(this.second ∈ copy.§$s)
                        copy ≥ this.second   *[copy ≻ copy.§$s, copy.§$s ∋ this.second]
                        copy ≻ copy.§$s   3(copy ≻ copy.§$s)
                        $__sv_copy.§$s ≺ $__sv_copy   1($__sv_copy.§$s ≺ $__sv_copy)
                        $__sv_copy.§$s ∋ this.field   1($__sv_copy.§$s ∋ this.field)
                        copy.§$s ∋ this.second   3(copy.§$s ∋ this.second)
                        copy.§$s ≺ copy   3(copy.§$s ≺ copy)
                        """, graph.printClosure());
            }
            if ("4".equals(statementIndex)) {
                Assertions.assertEquals("""
                        $__sv_copy ≥ this.field   *[$__sv_copy ≻ $__sv_copy.§$s, $__sv_copy.§$s ∋ this.field]
                        $__sv_copy ≻ $__sv_copy.§$s   1($__sv_copy ≻ $__sv_copy.§$s)
                        this.field ≤ $__sv_copy   *[this.field ∈ $__sv_copy.§$s, $__sv_copy.§$s ≺ $__sv_copy]
                        this.field ∈ $__sv_copy.§$s   1(this.field ∈ $__sv_copy.§$s)
                        this.second ∈ $__sv_return method.§$s   4(this.second ∈ $__sv_return method.§$s)
                        $__sv_copy.§$s ≺ $__sv_copy   1($__sv_copy.§$s ≺ $__sv_copy)
                        $__sv_copy.§$s ∋ this.field   1($__sv_copy.§$s ∋ this.field)
                        $__sv_return method.§$s ∋ this.second   4($__sv_return method.§$s ∋ this.second)
                        """, graph.printClosure());
            }
        });
        MethodInfo method = X.findUniqueMethod("method", 1);
        ParameterInfo in = method.parameters().getFirst();
        MethodLinkedVariables mlvListAdd = method.analysis().getOrCreate(METHOD_LINKS,
                () -> linkComputer.doMethod(method));

        VariableData vd1 = VariableDataImpl.of(method.methodBody().statements().get(1));
        VariableInfo copy1 = vd1.variableInfo("copy");
        assertTrue(copy1.isModified());
        VariableInfo in1 = vd1.variableInfo(in);
        assertTrue(in1.isModified());

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
        // also important is that 0:in remains modified...
        assertEquals("[0:in*.§$s∋this.field] --> method.§$s∋this.second", mlvListAdd.toString());
    }

}
