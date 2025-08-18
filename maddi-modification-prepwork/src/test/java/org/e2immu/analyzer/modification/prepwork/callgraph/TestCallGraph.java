package org.e2immu.analyzer.modification.prepwork.callgraph;

import org.e2immu.analyzer.modification.prepwork.CommonTest;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.util.internal.graph.G;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class TestCallGraph extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            class X {
                int unused;
                int j = square(10);
                void m1() {
                    m2();
                }
                void m2() {
                    j++;
                }
                static int square(int i) {
                    return i*i;
                }
                void recursive(int k) {
                    if(k <= 0) {
                        System.out.println("done");
                    } else {
                        System.out.println("k = "+k);
                        recursive(k-1);
                    }
                }
            }
            """;

    @DisplayName("basics of call graph")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        ComputeCallGraph ccg = new ComputeCallGraph(runtime, X);
        G<Info> graph = ccg.go().graph();
        assertEquals("""
                a.b.X->S->a.b.X.<init>(), a.b.X->S->a.b.X.j, a.b.X->S->a.b.X.m1(), a.b.X->S->a.b.X.m2(), \
                a.b.X->S->a.b.X.recursive(int), a.b.X->S->a.b.X.square(int), a.b.X->S->a.b.X.unused, \
                a.b.X.j->R->a.b.X.m2(), a.b.X.j->R->a.b.X.square(int), a.b.X.m1()->R->a.b.X.m2()\
                """, ComputeCallGraph.print(graph));

        FieldInfo unused = X.getFieldByName("unused", true);
        assertNotNull(graph.vertex(unused));

        MethodInfo methodInfo = X.findUniqueMethod("recursive", 1);
        assertTrue(ccg.recursiveMethods().contains(methodInfo));

        ComputeAnalysisOrder cao = new ComputeAnalysisOrder();
        List<Info> analysisOrder = cao.go(graph);
        assertEquals("""
                [a.b.X.<init>(), a.b.X.m2(), a.b.X.recursive(int), a.b.X.square(int), a.b.X.unused, a.b.X.j, a.b.X.m1(), a.b.X]\
                """, analysisOrder.toString());
    }


    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.List;class X {
                static void method(List<String> list) {
                    list.forEach(s -> {
                        if(list.isEmpty()) {
                            System.out.println("empty");
                        } else {
                            System.out.println("s = "+s);
                            method(list.subList(0, list.size()-1));
                        }
                    });
                }
            }
            """;

    @DisplayName("recursion in lambda")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        ComputeCallGraph ccg = new ComputeCallGraph(runtime, X);
        G<Info> graph = ccg.go().graph();
        assertEquals("""
                a.b.X->S->a.b.X.<init>(), a.b.X->S->a.b.X.method(java.util.List<String>), \
                a.b.X.$0->S->a.b.X.$0.accept(String), a.b.X.method(java.util.List<String>)->S->a.b.X.$0\
                """, ComputeCallGraph.print(graph));

        // NOTE: at the moment, both the lambda method and 'method' are marked recursive
        assertEquals("[a.b.X.$0.accept(String), a.b.X.method(java.util.List<String>)]",
                ccg.recursiveMethods().stream().map(MethodInfo::fullyQualifiedName).sorted().toList().toString());

        ComputeAnalysisOrder cao = new ComputeAnalysisOrder();
        List<Info> analysisOrder = cao.go(graph);
        assertEquals("""
                [a.b.X.$0.accept(String), a.b.X.<init>(), a.b.X.$0, a.b.X.method(java.util.List<String>), a.b.X]\
                """, analysisOrder.toString());
    }


    @Language("java")
    static final String INPUT3 = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            
            class X {
                private List<String> list;
                X(int i) {
                    initList(i);
                    print();
                    sleep();
                }
                private void initList(int i) {
                    list = new ArrayList<>();
                    list.add(i);
                }
                private void sleep() {
                    list.clear();
                }
                void print() {
                    System.out.println("print!");
                }
                public void rest() {
                    sleep();
                    System.out.println("awake");
                }
            }
            """;

    @DisplayName("part of construction")
    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse(INPUT3);
        ComputeCallGraph ccg = new ComputeCallGraph(runtime, X);
        G<Info> graph = ccg.go().graph();
        assertEquals("""
                a.b.X->S->a.b.X.<init>(int)
                a.b.X->S->a.b.X.initList(int)
                a.b.X->S->a.b.X.list
                a.b.X->S->a.b.X.print()
                a.b.X->S->a.b.X.rest()
                a.b.X->S->a.b.X.sleep()
                a.b.X.<init>(int)->R->a.b.X.initList(int)
                a.b.X.<init>(int)->R->a.b.X.print()
                a.b.X.<init>(int)->R->a.b.X.sleep()
                a.b.X.list->R->a.b.X.initList(int)
                a.b.X.list->R->a.b.X.sleep()
                a.b.X.rest()->R->a.b.X.sleep()\
                """, graph.toString("\n", ComputeCallGraph::edgeValuePrinter));

        assertTrue(ccg.recursiveMethods().isEmpty());

        ComputeAnalysisOrder cao = new ComputeAnalysisOrder();
        List<Info> analysisOrder = cao.go(graph);
        assertEquals("""
                [a.b.X.initList(int), a.b.X.print(), a.b.X.sleep(), a.b.X.<init>(int), a.b.X.list, a.b.X.rest(), a.b.X]\
                """, analysisOrder.toString());
    }


    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            class X {
                interface I { int i(); }
                record Y(int i) implements I {}
                record Z(Y y) {}
                Z z;
                Z getZ() { return z; }
            }
            """;

    @DisplayName("subtypes in call graph")
    @Test
    public void test4() {
        TypeInfo X = javaInspector.parse(INPUT4);
        ComputeCallGraph ccg = new ComputeCallGraph(runtime, X);
        G<Info> graph = ccg.go().graph();
        assertEquals("""
                a.b.X->S->a.b.X.<init>(), a.b.X->S->a.b.X.I, a.b.X->S->a.b.X.Y, \
                a.b.X->S->a.b.X.Z, a.b.X->S->a.b.X.getZ(), a.b.X->S->a.b.X.z, a.b.X.I->S->a.b.X.I.i(), \
                a.b.X.Y->H->a.b.X.I, a.b.X.Y->S->a.b.X.Y.<init>(int), a.b.X.Y->S->a.b.X.Y.i, a.b.X.Y->S->a.b.X.Y.i(), \
                a.b.X.Y.i()->S->a.b.X.I.i(), \
                a.b.X.Y.i->R->a.b.X.Y.<init>(int), a.b.X.Y.i->R->a.b.X.Y.i(), a.b.X.Z->S->a.b.X.Z.<init>(a.b.X.Y), \
                a.b.X.Z->S->a.b.X.Z.y, a.b.X.Z->S->a.b.X.Z.y(), a.b.X.Z.<init>(a.b.X.Y)->D->a.b.X.Y, \
                a.b.X.Z.y()->D->a.b.X.Y, a.b.X.Z.y->D->a.b.X.Y, a.b.X.Z.y->R->a.b.X.Z.<init>(a.b.X.Y), \
                a.b.X.Z.y->R->a.b.X.Z.y(), a.b.X.getZ()->D->a.b.X.Z, a.b.X.z->D->a.b.X.Z, a.b.X.z->R->a.b.X.getZ()\
                """, ComputeCallGraph.print(graph));

        ComputeAnalysisOrder cao = new ComputeAnalysisOrder();
        List<Info> analysisOrder = cao.go(graph);
        assertEquals("""
                [a.b.X.<init>(), a.b.X.I.i(), a.b.X.Y.<init>(int), a.b.X.I, a.b.X.Y.i(), a.b.X.Y.i, a.b.X.Y, \
                a.b.X.Z.<init>(a.b.X.Y), a.b.X.Z.y(), a.b.X.Z.y, a.b.X.Z, a.b.X.getZ(), a.b.X.z, a.b.X]\
                """, analysisOrder.toString());
    }

    @Language("java")
    private static final String INPUT5 = """
            package a.b;
            class X {
                /**
                 * link to {@link Y}
                 */
                interface I { int i(); }
                record Y(int i) implements I {}
            }
            """;

    @DisplayName("javadoc reference")
    @Test
    public void test5() {
        TypeInfo X = javaInspector.parse(INPUT5);
        ComputeCallGraph ccg = new ComputeCallGraph(runtime, X);
        G<Info> graph = ccg.go().graph();
        assertEquals("""
                a.b.X->S->a.b.X.<init>(), a.b.X->S->a.b.X.I, a.b.X->S->a.b.X.Y, a.b.X.I->S->a.b.X.I.i(), \
                a.b.X.I->d->a.b.X.Y, a.b.X.Y->H->a.b.X.I, a.b.X.Y->S->a.b.X.Y.<init>(int), a.b.X.Y->S->a.b.X.Y.i, \
                a.b.X.Y->S->a.b.X.Y.i(), a.b.X.Y.i()->S->a.b.X.I.i(), a.b.X.Y.i->R->a.b.X.Y.<init>(int), \
                a.b.X.Y.i->R->a.b.X.Y.i()\
                """, ComputeCallGraph.print(graph));

        ComputeAnalysisOrder cao = new ComputeAnalysisOrder();
        List<Info> analysisOrder = cao.go(graph);
        assertEquals("""
                [a.b.X.<init>(), a.b.X.I.i(), a.b.X.Y.<init>(int), a.b.X.I, a.b.X.Y.i(), a.b.X.Y.i, a.b.X.Y, a.b.X]\
                """, analysisOrder.toString());
    }


    @Language("java")
    private static final String INPUT6 = """
            package a.b;
            import java.io.IOException;
            import java.io.InvalidClassException;
            class X {
                public void m() {
                    try {
                        System.out.println("A");
                    } catch(IOException | InvalidClassException ioe) {
                        System.out.println("B");
                    }
                }
            }
            """;

    @DisplayName("catch clauses")
    @Test
    public void test6() {
        TypeInfo X = javaInspector.parse(INPUT6);
        ComputeCallGraph ccg = new ComputeCallGraph(runtime, Set.of(X), _ -> true);
        G<Info> graph = ccg.go().graph();
        assertEquals("""
                a.b.X->H->java.lang.Object, a.b.X->S->a.b.X.<init>(), a.b.X->S->a.b.X.m(), \
                a.b.X.m()->R->java.io.IOException, a.b.X.m()->R->java.io.InvalidClassException, \
                a.b.X.m()->R->java.io.PrintStream.println(String), \
                a.b.X.m()->R->java.lang.System, a.b.X.m()->R->java.lang.System.out\
                """, ComputeCallGraph.print(graph));

        ComputeAnalysisOrder cao = new ComputeAnalysisOrder();
        List<Info> analysisOrder = cao.go(graph);
        assertEquals("""
                [a.b.X.<init>(), a.b.X.m(), a.b.X]\
                """, analysisOrder.toString());
    }

    @Language("java")
    private static final String INPUT7 = """
            package a.b;
            import java.io.IOException;
            import java.io.InvalidClassException;
            class X {
                public void m(Exception e) {
                    switch(e) {
                        case IOException ioe -> System.out.println("A");
                        case InvalidClassException ice -> System.out.println("B");
                        default -> { }
                    }
                }
            }
            """;

    @DisplayName("switch cases")
    @Test
    public void test7() {
        TypeInfo X = javaInspector.parse(INPUT7);
        ComputeCallGraph ccg = new ComputeCallGraph(runtime, Set.of(X), _ -> true);
        G<Info> graph = ccg.go().graph();
        assertEquals("""
                a.b.X->H->java.lang.Object, a.b.X->S->a.b.X.<init>(), a.b.X->S->a.b.X.m(Exception), \
                a.b.X.m(Exception)->D->java.lang.Exception, a.b.X.m(Exception)->R->java.io.IOException, \
                a.b.X.m(Exception)->R->java.io.InvalidClassException, \
                a.b.X.m(Exception)->R->java.io.PrintStream.println(String), a.b.X.m(Exception)->R->java.lang.System, \
                a.b.X.m(Exception)->R->java.lang.System.out\
                """, ComputeCallGraph.print(graph));

        ComputeAnalysisOrder cao = new ComputeAnalysisOrder();
        List<Info> analysisOrder = cao.go(graph);
        assertEquals("""
                [a.b.X.<init>(), a.b.X.m(Exception), a.b.X]\
                """, analysisOrder.toString());
    }
}
