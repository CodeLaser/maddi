package org.e2immu.analyzer.modification.link.impl.basics;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.link.impl.linkgraph.Graph;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
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
            if("1".equals(statementIndex)) {
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
            if("1".equals(statementIndex)) {
                Assertions.assertEquals("""
                        first ∈ $__sv_copy.§$s   1(first ∈ $__sv_copy.§$s)
                        $__sv_copy.§$s ∋ first   1($__sv_copy.§$s ∋ first)
                        """, graph.printClosure());
            }
            if("2".equals(statementIndex)) {
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
}
