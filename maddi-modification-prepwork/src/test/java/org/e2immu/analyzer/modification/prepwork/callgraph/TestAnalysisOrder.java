package org.e2immu.analyzer.modification.prepwork.callgraph;

import org.e2immu.analyzer.modification.prepwork.CommonTest;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.util.internal.graph.G;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestAnalysisOrder extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import org.e2immu.annotation.Modified;
            class X {
                interface I {}
            
                interface J extends Comparable<J> {}
            
                // modifying
                interface K {
                    @Modified
                    void add(String s);
                }
            
                // modifying, because K is
                interface KK extends K {
                    int get();
                }
            
                interface L {
                    int get();
                }
            
                //modifying (implicit in abstract void methods)
                interface M extends L {
                    void set(int i);
                }
            
                class Nested {
                    int n;
                }
            }
            """;

    @DisplayName("interfaces")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        ComputeCallGraph ccg = new ComputeCallGraph(runtime, X);
        G<Info> graph = ccg.go().graph();
        assertEquals("""
                a.b.X->S->a.b.X.<init>(), a.b.X->S->a.b.X.I, a.b.X->S->a.b.X.J, a.b.X->S->a.b.X.K, a.b.X->S->a.b.X.KK, \
                a.b.X->S->a.b.X.L, a.b.X->S->a.b.X.M, a.b.X->S->a.b.X.Nested, a.b.X.K->S->a.b.X.K.add(String), \
                a.b.X.KK->H->a.b.X.K, a.b.X.KK->S->a.b.X.KK.get(), a.b.X.L->S->a.b.X.L.get(), a.b.X.M->H->a.b.X.L, \
                a.b.X.M->S->a.b.X.M.set(int), a.b.X.Nested->S->a.b.X.Nested.<init>(), a.b.X.Nested->S->a.b.X.Nested.n\
                """, ComputeCallGraph.print(graph));
        List<Info> analysisOrder = new ComputeAnalysisOrder().go(graph);
        assertEquals("""
                <init>, I, J, add, get, get, set, <init>, n, K, L, Nested, KK, M, X\
                """, analysisOrder.stream().map(Info::simpleName).collect(Collectors.joining(", ")));
    }

}
