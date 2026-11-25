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

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.graph.G;
import io.codelaser.maddi.graph.V;

/**
 * CodeLaser/maddi#214: {@code new X(6) { }} calls {@code X(int)}, as {@code new X(6)} does. The call site is written,
 * with written arguments, so a lever deleting or changing {@code X(int)} has to see it; before, the anonymous
 * creation reached the graph only through its own members and the reference to its type.
 */
public class TestCallGraphAnonymousSuperConstructor extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class X {
                X(int n) {
                }
                X() {
                }
                static Object anonymous() {
                    return new X(6) { };
                }
                static Object plain() {
                    return new X(7);
                }
                static Runnable implementation() {
                    return new Runnable() { public void run() { } };
                }
            }
            """;

    @DisplayName("an anonymous creation is a call of the superclass constructor its arguments go to")
    @Test
    public void test() {
        TypeInfo X = javaInspector.parse(ABX, INPUT);
        G<Info> graph = new ComputeCallGraph(runtime, X).go().graph();
        MethodInfo xInt = X.constructors().stream().filter(c -> c.parameters().size() == 1).findFirst().orElseThrow();

        long plain = edge(graph, X.findUniqueMethod("plain", 0), xInt);
        long anonymous = edge(graph, X.findUniqueMethod("anonymous", 0), xInt);
        assertTrue(ComputeCallGraph.isReference(anonymous), "a consumer filtering on isReference() must see it");
        assertEquals(ComputeCallGraph.edgeValuePrinter(plain), ComputeCallGraph.edgeValuePrinter(anonymous),
                "the same call as 'new X(7)'");

        Map<V<Info>, Long> fromImplementation = graph.edges(new V<>(X.findUniqueMethod("implementation", 0)));
        assertNotNull(fromImplementation);
        assertTrue(fromImplementation.keySet().stream().noneMatch(v -> v.t() instanceof MethodInfo m
                                                                       && m.isConstructor()
                                                                       && m.typeInfo().isJavaLangObject()),
                "an anonymous implementation of an interface calls nothing written: no edge to Object()");
    }

    private static long edge(G<Info> graph, Info from, Info to) {
        Map<V<Info>, Long> edges = graph.edges(new V<>(from));
        assertNotNull(edges, from + " has no outgoing edges at all");
        Long value = edges.get(new V<>(to));
        assertNotNull(value, from + " has no edge to " + to);
        return value;
    }
}
