/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.callgraph;

import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.graph.G;
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
            import io.codelaser.maddi.annotation.Modified;
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
        TypeInfo X = javaInspector.parse(ABX, INPUT1);
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
