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

package io.codelaser.maddi.modification.prepwork.callgraph;

import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.graph.G;
import io.codelaser.maddi.graph.V;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
import io.codelaser.maddi.inspection.api.parser.Summary;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.integration.JavaInspectorImpl;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.modification.prepwork.CommonTest;
import io.codelaser.maddi.modification.prepwork.PrepAnalyzer;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.codelaser.maddi.inspection.integration.JavaInspectorImpl.JAR_WITH_PATH_PREFIX;
import static io.codelaser.maddi.inspection.integration.JavaInspectorImpl.TEST_PROTOCOL_PREFIX;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <b>A LOCAL CLASS IS A TYPE DECLARED INSIDE A BODY, like an anonymous class and a lambda, and the call graph
 * has to walk it like one.</b> GAP {@code G15} of the OpenSearch modularization campaign, split out of {@code G9}
 * and pinned from the query side by {@code TestGap9LambdaWrittenRequirement}; this is the pin at the source.
 * <p>
 * Edge type A in {@link ComputeCallGraph}'s class comment has always read "from a method or field into its
 * anonymous, lambda, <b>local</b> types". The first two arrive as EXPRESSIONS ({@code ConstructorCall
 * .anonymousClass()}, {@code Lambda}) and the visitor's chain tests expressions; a local class arrives as the
 * STATEMENT {@code LocalTypeDeclaration}, and had no arm at all.
 * <p>
 * ⚠ <b>What that cost, measured before the fix</b> — and it is not quite what the gap record describes, which
 * says the structural edge existed:
 * <pre>
 *   b.LocalClass.go()   -R-> b.LocalClass.0$go$Helper            (from addType on `new Helper()`)
 *   b.LocalClass.go()   -R-> …Helper.&lt;init&gt;(), …Helper.run()     (real calls)
 *   b.LocalClass.0$go$Helper                                     ← no OUTGOING edge whatsoever
 * </pre>
 * The local type was a vertex reachable only as a target: no {@code S} edge declaring it, no {@code S} edges to
 * its own members, and nothing its body references. The anonymous control in the identical shape yielded
 * {@code b.AnonBody.$0.run() -R-> a.Made}. So every consumer of the type graph — {@code requiredTypes},
 * {@code buildUnitDependencies}, {@code cyclicTypeComponents}, the package graph — was short by whatever a local
 * class body uses, silently. {@link #localClassBodyIsAUse()} is the consumer-level statement of that.
 */
public class TestCallGraphLocalClass extends CommonTest {

    @BeforeEach
    @Override
    public void beforeEach() {
        // this test builds its own inspector: every source must be registered up front
    }

    @Language("java")
    private static final String MADE = """
            package a;
            public class Made {
                public void go() { }
            }
            """;

    /** The subject: {@code a.Made} is used ONLY inside the local class body. */
    @Language("java")
    private static final String LOCAL_CLASS = """
            package b;
            import a.Made;
            public class LocalClass {
                public void go() {
                    class Helper {
                        void run() { new Made().go(); }
                    }
                    new Helper().run();
                }
            }
            """;

    /** The control: same shape, anonymous instead of local. It has always worked. */
    @Language("java")
    private static final String ANON_BODY = """
            package b;
            import a.Made;
            public class AnonBody {
                public Runnable make() {
                    return new Runnable() {
                        @Override public void run() { new Made().go(); }
                    };
                }
            }
            """;

    private G<Info> graph;
    private TypeInfo made, localClass, anonBody;

    private void init() throws IOException {
        ParseResult parseResult = parse(Map.of("a.Made", MADE, "b.LocalClass", LOCAL_CLASS, "b.AnonBody", ANON_BODY));
        made = parseResult.findType("a.Made");
        localClass = parseResult.findType("b.LocalClass");
        anonBody = parseResult.findType("b.AnonBody");
        graph = new PrepAnalyzer(runtime).doPrimaryTypesReturnGraph(Set.of(made, localClass, anonBody));
    }

    @DisplayName("G15: the local class body's references are in the graph, exactly as an anonymous body's are")
    @Test
    public void localClassBodyContributesEdges() throws IOException {
        init();
        TypeInfo helper = hiddenType(localClass, "Helper");
        MethodInfo run = helper.findUniqueMethod("run", 0);

        // the declaring member declares it: edge type A, which did not exist at all before
        assertTrue(hasEdge(localClass.findUniqueMethod("go", 0), helper),
                "the enclosing method must declare the local type (S)");
        // ...and go(TypeInfo) then walked it: its own members are attached
        assertTrue(hasEdge(helper, run), "the local type must be attached to its own members (S)");

        // the point of the whole gap: what the body references
        assertTrue(hasEdge(run, made), "the local class body's reference to a.Made must be an edge");
        assertTrue(hasEdge(run, made.findUniqueMethod("go", 0)), "and so must the call it writes");

        // the control, unchanged by this fix
        TypeInfo anon = hiddenType(anonBody, "$0");
        MethodInfo anonRun = anon.findUniqueMethod("run", 0);
        assertTrue(hasEdge(anonRun, made), "control: the anonymous body has always contributed");
        assertTrue(hasEdge(anonRun, made.findUniqueMethod("go", 0)), "control");
    }

    /**
     * ⛔ <b>THE WEIGHT PIN.</b> The builder SUMS edge values, so walking a declared type twice does not produce a
     * duplicate line one could notice — it doubles the reference counts every weighting consumer reads. The walk
     * is safe only because a local class is <em>not</em> among its enclosing type's {@code subTypes()}, so
     * {@code go(TypeInfo)}'s subtype loop never reaches it and {@code handleTypeDeclaredInBody} is its only visit.
     * That is an assumption about the CST, so it is asserted rather than trusted.
     */
    @DisplayName("G15: the local type is walked exactly once — subTypes() does not contain it, and the counts are 1")
    @Test
    public void theLocalTypeIsWalkedExactlyOnce() throws IOException {
        init();
        TypeInfo helper = hiddenType(localClass, "Helper");
        assertFalse(localClass.subTypes().contains(helper),
                "a local class must not be among the enclosing type's subTypes(): if it ever is, go(TypeInfo)"
                + " walks it too and handleTypeDeclaredInBody doubles every weight below it");

        MethodInfo run = helper.findUniqueMethod("run", 0);
        assertEquals(1, ComputeCallGraph.referenceCount(edge(run, made.findUniqueMethod("go", 0))),
                "one call written, one reference counted");
        // the control carries the same count, which is what "exactly as an anonymous body's are" means
        TypeInfo anon = hiddenType(anonBody, "$0");
        assertEquals(1, ComputeCallGraph.referenceCount(
                        edge(anon.findUniqueMethod("run", 0), made.findUniqueMethod("go", 0))),
                "control: the anonymous body counts one too");
    }

    /**
     * The consumer-level statement of the gap, in maddi's own terms: {@code a.Made} is used by {@code b.LocalClass}
     * and by nothing else in it but the local class body. Before the fix the projection did not report it — which
     * is the same "silent under-reporting by a read verb" the querymodule pin describes one layer up.
     */
    @DisplayName("G15: a primary type whose ONLY use of another is inside a local class body still depends on it")
    @Test
    public void localClassBodyIsAUse() throws IOException {
        init();
        PrimaryTypeUseGraph useGraph = new PrimaryTypeUseGraph(graph);
        assertEquals(Set.of(localClass, anonBody), useGraph.dependentsOf(Set.of(made)),
                "b.LocalClass uses a.Made only from inside the local class body; b.AnonBody is the control");
    }

    /** The hidden type (anonymous, lambda or local) declared inside {@code owner}, by simple name. */
    private TypeInfo hiddenType(TypeInfo owner, String simpleName) {
        return graph.vertices().stream().map(V::t)
                .filter(i -> i instanceof TypeInfo ti && simpleName.equals(ti.simpleName())
                             && owner.equals(ti.primaryType()))
                .map(i -> (TypeInfo) i).findFirst()
                .orElseThrow(() -> new AssertionError("no type " + simpleName + " declared in " + owner
                                                      + "; graph=" + ComputeCallGraph.print(graph)));
    }

    private boolean hasEdge(Info from, Info to) {
        Map<V<Info>, Long> edges = graph.edges(new V<>(from));
        return edges != null && edges.containsKey(new V<>(to));
    }

    private long edge(Info from, Info to) {
        Map<V<Info>, Long> edges = graph.edges(new V<>(from));
        assertNotNull(edges, from + " has no outgoing edges at all");
        Long value = edges.get(new V<>(to));
        assertNotNull(value, from + " has no edge to " + to);
        return value;
    }

    private ParseResult parse(Map<String, String> sourcesByFqn) throws IOException {
        Map<String, String> sourcesByURIString = sourcesByFqn.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(e -> TEST_PROTOCOL_PREFIX + e.getKey(), Map.Entry::getValue));
        javaInspector = new JavaInspectorImpl();
        InputConfigurationImpl.Builder builder = new InputConfigurationImpl.Builder()
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .addClassPath(JavaInspectorImpl.MADDI_SUPPORT)
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/junit/jupiter/api")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/junit/platform/commons")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/opentest4j");
        sourcesByURIString.keySet().forEach(builder::addSources);
        InputConfiguration inputConfiguration = builder.build();
        javaInspector.initialize(inputConfiguration);
        runtime = javaInspector.runtime();
        JavaInspector.ParseOptions parseOptions = new JavaInspector.ParseOptions.Builder()
                .setFailFast(true).setDetailedSources(true).build();
        Summary summary = javaInspector.parse(sourcesByURIString, parseOptions);
        return summary.parseResult();
    }
}
