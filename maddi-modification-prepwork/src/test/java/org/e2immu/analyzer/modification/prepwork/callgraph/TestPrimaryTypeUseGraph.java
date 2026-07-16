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

import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.CommonTest;
import org.e2immu.language.cst.api.element.ModuleInfo;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.api.parser.Summary;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.util.internal.graph.G;
import org.e2immu.util.internal.graph.ImmutableGraph;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.e2immu.language.inspection.integration.JavaInspectorImpl.JAR_WITH_PATH_PREFIX;
import static org.e2immu.language.inspection.integration.JavaInspectorImpl.TEST_PROTOCOL_PREFIX;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The reverse reachability the reload flow runs on. A chain {@code Top -> Mid -> Base} (each using the next): a change
 * in {@code Base} must make both {@code Mid} and {@code Top} dependent, while a change in {@code Top} must make
 * nothing dependent — upstream stays put, which is the whole point of REWIRE.
 */
public class TestPrimaryTypeUseGraph extends CommonTest {

    @BeforeEach
    @Override
    public void beforeEach() {
        // this test builds its own inspector: the in-house parser needs every testprotocol: source registered
    }

    @Language("java")
    private static final String BASE = """
            package a.b;
            public class Base {
                public String name() { return "base"; }
            }
            """;

    @Language("java")
    private static final String MID = """
            package a.b;
            public class Mid {
                private final Base base = new Base();
                public String name() { return base.name(); }
            }
            """;

    @Language("java")
    private static final String TOP = """
            package a.b;
            public class Top {
                private final Mid mid = new Mid();
                public String describe() { return mid.name(); }
            }
            """;

    // no relation to the other three: it must never show up as a dependent
    @Language("java")
    private static final String LONER = """
            package a.b;
            public class Loner {
                public int i() { return 1; }
            }
            """;

    private G<Info> graph;
    private TypeInfo base, mid, top, loner;

    private void init() throws IOException {
        ParseResult parseResult = parse(Map.of("a.b.Base", BASE, "a.b.Mid", MID, "a.b.Top", TOP, "a.b.Loner", LONER));
        base = parseResult.findType("a.b.Base");
        mid = parseResult.findType("a.b.Mid");
        top = parseResult.findType("a.b.Top");
        loner = parseResult.findType("a.b.Loner");
        graph = new PrepAnalyzer(runtime).doPrimaryTypesReturnGraph(Set.of(base, mid, top, loner));
    }

    @DisplayName("a change downstream pulls in everything that uses it, transitively")
    @Test
    public void testDependentsOfBase() throws IOException {
        init();
        PrimaryTypeUseGraph useGraph = new PrimaryTypeUseGraph(graph);
        assertEquals(Set.of(mid, top), useGraph.dependentsOf(Set.of(base)),
                "Mid uses Base directly, Top reaches it through Mid");
    }

    @DisplayName("a change upstream pulls in nothing: UNCHANGED types never reach an INVALID one")
    @Test
    public void testDependentsOfTop() throws IOException {
        init();
        PrimaryTypeUseGraph useGraph = new PrimaryTypeUseGraph(graph);
        assertEquals(Set.of(), useGraph.dependentsOf(Set.of(top)), "nobody uses Top");
        assertEquals(Set.of(top), useGraph.dependentsOf(Set.of(mid)), "only Top uses Mid");
        assertEquals(Set.of(), useGraph.dependentsOf(Set.of(loner)), "nobody uses Loner");
    }

    @DisplayName("the result excludes the changed set itself, so the two are disjoint")
    @Test
    public void testDisjoint() throws IOException {
        init();
        PrimaryTypeUseGraph useGraph = new PrimaryTypeUseGraph(graph);
        Set<TypeInfo> changed = Set.of(base, mid);
        Set<TypeInfo> dependents = useGraph.dependentsOf(changed);
        // Mid uses Base, so Mid is reachable from the changed set -- but it is changed, so it must not be reported
        assertEquals(Set.of(top), dependents);
        assertTrue(java.util.Collections.disjoint(changed, dependents));
    }

    @DisplayName("module vertices are skipped rather than projected: ModuleInfo.typeInfo() throws")
    @Test
    public void testModuleInfoIsSkipped() throws IOException {
        init();
        // ComputeCallGraph adds a vertex per ModuleInfo, and an edge module->type for every uses/provides it
        // resolves; the projection has to skip those, since a module has no primary type to be projected onto.
        ModuleInfo moduleInfo = runtime.newModuleInfoBuilder()
                .setName("a.b.mod")
                .setCompilationUnit(base.compilationUnit())
                .build();
        assertThrows(UnsupportedOperationException.class, moduleInfo::typeInfo,
                "the hazard this guards against");

        G.Builder<Info> builder = new ImmutableGraph.Builder<>(Long::sum);
        graph.edgeStream().forEach(e -> builder.mergeEdge(e.from().t(), e.to().t(), 1L));
        builder.mergeEdge(moduleInfo, base, ComputeCallGraph.REFERENCES);

        PrimaryTypeUseGraph useGraph = new PrimaryTypeUseGraph(builder.build());
        assertEquals(Set.of(mid, top), useGraph.dependentsOf(Set.of(base)),
                "the module edge must be dropped, and the rest of the projection unaffected");
    }

    private ParseResult parse(Map<String, String> sourcesByFqn) throws IOException {
        Map<String, String> sourcesByURIString = sourcesByFqn.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(e -> TEST_PROTOCOL_PREFIX + e.getKey(), Map.Entry::getValue));
        javaInspector = new JavaInspectorImpl();
        InputConfigurationImpl.Builder builder = new InputConfigurationImpl.Builder()
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .addClassPath(JavaInspectorImpl.E2IMMU_SUPPORT)
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/junit/jupiter/api")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/apiguardian/api")
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
