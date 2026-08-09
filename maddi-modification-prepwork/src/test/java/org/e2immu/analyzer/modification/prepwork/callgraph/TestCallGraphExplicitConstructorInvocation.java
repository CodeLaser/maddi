/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.e2immu.analyzer.modification.prepwork.callgraph;

import org.e2immu.analyzer.modification.prepwork.CommonTest;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.util.internal.graph.G;
import org.e2immu.util.internal.graph.V;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@code this(..)} and {@code super(..)} are calls, and the graph says so with the R bit as well as the S one.
 * <p>
 * The S bit alone is what the analysis ORDER needs; it is not what the edge IS. While it was the only bit set,
 * every consumer asking "is this a genuine call" through {@link ComputeCallGraph#isReference} was blind to
 * constructor delegation — measured downstream as a call-site count that was short by one sibling constructor.
 */
public class TestCallGraphExplicitConstructorInvocation extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            class X {
                final String name;
                final int n;
                X(int n) {
                    this("n", n);
                }
                X(String name, int n) {
                    this.name = name;
                    this.n = n;
                }
                static class Sub extends X {
                    Sub(int n) {
                        super("sub", n);
                    }
                }
            }
            """;

    private static long edge(G<Info> graph, Info from, Info to) {
        Map<V<Info>, Long> edges = graph.edges(new V<>(from));
        assertNotNull(edges, from + " has no outgoing edges at all");
        Long value = edges.get(new V<>(to));
        assertNotNull(value, from + " has no edge to " + to);
        return value;
    }

    private static MethodInfo constructor(TypeInfo typeInfo, int parameters, String firstParameterType) {
        return typeInfo.constructors().stream()
                .filter(c -> c.parameters().size() == parameters
                             && firstParameterType.equals(c.parameters().getFirst().parameterizedType()
                        .typeInfo().fullyQualifiedName()))
                .findFirst().orElseThrow();
    }

    @DisplayName("this(..) is recorded as a call, not only as code structure")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(ABX, INPUT1);
        G<Info> graph = new ComputeCallGraph(runtime, X).go().graph();

        MethodInfo delegating = constructor(X, 1, "int");
        MethodInfo target = constructor(X, 2, "java.lang.String");
        long thisCall = edge(graph, delegating, target);
        assertEquals("SR", ComputeCallGraph.edgeValuePrinter(thisCall));
        assertTrue(ComputeCallGraph.isReference(thisCall),
                "gap #124: a consumer filtering on isReference() must see the delegation");

        TypeInfo sub = X.findSubType("Sub");
        MethodInfo subConstructor = constructor(sub, 1, "int");
        long superCall = edge(graph, subConstructor, target);
        assertEquals("SR", ComputeCallGraph.edgeValuePrinter(superCall),
                "a subclass's super(..) is the same shape and gets the same treatment");
        assertTrue(ComputeCallGraph.isReference(superCall));
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.ArrayList;
            class X {
                static class L extends ArrayList<String> {
                    L(int initialCapacity) {
                        super(initialCapacity);
                    }
                }
            }
            """;

    /**
     * The R bit is added under {@code handleMethodCall}'s rules, so it obeys {@code accept()} exactly like every
     * other call: an invocation of a constructor OUTSIDE the parse keeps the structure edge it always had and
     * gains nothing. Without this, a reference-filtered walk would start reporting out-of-parse constructors.
     */
    @DisplayName("super(..) into a type outside the parse stays structure-only")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(ABX, INPUT2);
        G<Info> graph = new ComputeCallGraph(runtime, X).go().graph();

        TypeInfo l = X.findSubType("L");
        MethodInfo constructor = constructor(l, 1, "int");
        MethodInfo arrayListConstructor = javaInspector.compiledTypesManager()
                .get(java.util.ArrayList.class).constructors().stream()
                .filter(c -> c.parameters().size() == 1
                             && "int".equals(c.parameters().getFirst().parameterizedType()
                        .typeInfo().fullyQualifiedName()))
                .findFirst().orElseThrow();
        long superCall = edge(graph, constructor, arrayListConstructor);
        assertEquals("S", ComputeCallGraph.edgeValuePrinter(superCall));
        assertFalse(ComputeCallGraph.isReference(superCall));
    }

    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            class X {
                X(int n) {
                }
                static class Sub extends X {
                    Sub() {
                        super(1);
                    }
                }
                static class Implicit extends Sub {
                    Implicit() {
                    }
                }
            }
            """;

    /**
     * ⛔⛔ <b>ONLY WHAT THE AUTHOR WROTE IS A CALL.</b> A constructor that writes no {@code super(..)} gets one
     * synthesised, so counting synthetic invocations as references would hand EVERY subclass a use-edge to its
     * parent — 182 unjustified edges on the 500-type clustering stress model, and 334 weights moved with them.
     * The structure edge is recorded for both kinds, because the analysis order needs it either way.
     */
    @DisplayName("a synthesised super() is structure, never a reference")
    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse(ABX, INPUT3);
        G<Info> graph = new ComputeCallGraph(runtime, X).go().graph();

        MethodInfo xConstructor = constructor(X, 1, "int");
        MethodInfo subConstructor = X.findSubType("Sub").constructors().getFirst();
        assertEquals("SR", ComputeCallGraph.edgeValuePrinter(edge(graph, subConstructor, xConstructor)),
                "written super(1)");

        MethodInfo implicit = X.findSubType("Implicit").constructors().getFirst();
        long synthesised = edge(graph, implicit, subConstructor);
        assertEquals("S", ComputeCallGraph.edgeValuePrinter(synthesised),
                "Implicit() writes no super(), so its invocation is synthesised: structure, not a call");
        assertFalse(ComputeCallGraph.isReference(synthesised));
    }
}
