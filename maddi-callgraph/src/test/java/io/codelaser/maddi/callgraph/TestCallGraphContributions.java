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

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.openjdk.JavaInspectorImpl;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.codelaser.maddi.inspection.api.integration.JavaInspector.InvalidationState.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The call graph per primary type (docs/design/incremental-and-overlay.md §7 item 5): after a partial re-parse only
 * the re-parsed and rewired types are walked again, and the union is the graph a full walk builds.
 */
public class TestCallGraphContributions {

    @TempDir
    Path root;

    @Language("java")
    private static final String A = """
            package p;
            public class A {
                private final B b = new B();
                private int count;
                public int m() { count++; return b.v() + B.K; }
                public int fact(int n) { return n <= 1 ? 1 : n * fact(n - 1); }
                public Runnable r() { return () -> b.v(); }
            }
            """;

    @Language("java")
    private static final String B = """
            package p;
            public class B {
                public static final int K = 3;
                public int v() { return K; }
            }
            """;

    @Language("java")
    private static final String C = """
            package p;
            public class C extends java.util.ArrayList<String> {
                public C() { super(4); }
                public String s() { return "c"; }
            }
            """;

    @DisplayName("reused contributions: the same graph as a full walk, before and after a partial re-parse")
    @Test
    public void test() throws IOException {
        Path src = root.resolve("src");
        write(src, "p/A.java", A);
        write(src, "p/B.java", B);
        write(src, "p/C.java", C);
        SourceSet set = new SourceSetImpl.Builder().setName("main").setSourceDirectories(List.of(src))
                .setUri(src.toUri()).build();
        JavaInspector inspector = inspector(set);
        ParseResult pr = inspector.parse(Map.of(), JavaInspectorImpl.DETAILED_SOURCES).parseResult();

        CallGraphContributions contributions = new CallGraphContributions();
        // one predicate object for the whole sequence: another one clears the contributions (they were filtered by it)
        java.util.function.Predicate<TypeInfo> noExternals = _ -> false;
        ComputeCallGraph first = new ComputeCallGraph(inspector.runtime(), pr, noExternals).go(contributions);
        assertEquals(3, contributions.computed());
        assertAsFullWalk(first, pr);
        assertTrue(names(first).contains("p.A.fact(int)"), "recursion is part of the contribution");

        ComputeCallGraph second = new ComputeCallGraph(inspector.runtime(), pr, noExternals).go(contributions);
        assertEquals(3, contributions.reused());
        assertEquals(0, contributions.computed());
        assertAsFullWalk(second, pr);

        write(src, "p/B.java", "// edited\n" + B);
        JavaInspector.ParseOptions options = new JavaInspector.ParseOptions.Builder()
                .setDetailedSources(true).setFailFast(true)
                .setInvalidated(ti -> switch (ti.fullyQualifiedName()) {
                    case "p.B" -> INVALID;
                    case "p.A" -> REWIRE;
                    default -> UNCHANGED;
                })
                .build();
        TypeInfo c0 = pr.findType("p.C");
        pr = inspector.parse(Map.of(), options).parseResult();
        assertSame(c0, pr.findType("p.C"), "C is kept");
        ComputeCallGraph third = new ComputeCallGraph(inspector.runtime(), pr, noExternals).go(contributions);
        assertEquals(1, contributions.reused(), "C");
        assertEquals(2, contributions.computed(), "A, rewired, and B, re-parsed");
        assertEquals(0, contributions.stale());
        assertAsFullWalk(third, pr);
        assertEquals(3, contributions.size(), "the previous generation's contributions are dropped");

        JavaInspector cold = inspector(set);
        ParseResult coldResult = cold.parse(Map.of(), JavaInspectorImpl.DETAILED_SOURCES).parseResult();
        ComputeCallGraph coldGraph = new ComputeCallGraph(cold.runtime(), coldResult, _ -> false).go();
        assertEquals(ComputeCallGraph.print(coldGraph.graph()), ComputeCallGraph.print(third.graph()),
                "as a cold parse");
        assertEquals(names(coldGraph), names(third));

        new ComputeCallGraph(inspector.runtime(), pr, _ -> false).go(contributions);
        assertEquals(3, contributions.computed(), "another external predicate: nothing is reused");
    }

    /*
     A kept type whose walk pointed into a type that was replaced: the re-parse below keeps A while it re-parses the B
     A calls, which a correct invalidation never does, so A's CST, and its contribution, still hold the old B. The
     contribution must not be reused.
     */
    @DisplayName("a contribution that points into a replaced type is walked again")
    @Test
    public void testStaleTarget() throws IOException {
        Path src = root.resolve("src2");
        write(src, "p/A.java", A);
        write(src, "p/B.java", B);
        write(src, "p/C.java", C);
        SourceSet set = new SourceSetImpl.Builder().setName("main").setSourceDirectories(List.of(src))
                .setUri(src.toUri()).build();
        JavaInspector inspector = inspector(set);
        ParseResult pr = inspector.parse(Map.of(), JavaInspectorImpl.DETAILED_SOURCES).parseResult();
        CallGraphContributions contributions = new CallGraphContributions();
        java.util.function.Predicate<TypeInfo> noExternals = _ -> false;
        new ComputeCallGraph(inspector.runtime(), pr, noExternals).go(contributions);

        write(src, "p/B.java", "// edited\n" + B);
        JavaInspector.ParseOptions options = new JavaInspector.ParseOptions.Builder()
                .setDetailedSources(true).setFailFast(true)
                .setInvalidated(ti -> ti.fullyQualifiedName().equals("p.B") ? INVALID : UNCHANGED)
                .build();
        pr = inspector.parse(Map.of(), options).parseResult();
        new ComputeCallGraph(inspector.runtime(), pr, noExternals).go(contributions);
        assertEquals(2, contributions.computed(), "B, re-parsed, and A, which points into the old B");
        assertEquals(1, contributions.reused(), "C: its edges into ArrayList are not stale");
        assertEquals(1, contributions.stale(), "A");
    }

    /* the same graph, vertex and edge order included, and the same recursive methods, as a full walk over pr */
    private static void assertAsFullWalk(ComputeCallGraph incremental, ParseResult pr) {
        ComputeCallGraph full = new ComputeCallGraph(null, pr, _ -> false).go();
        assertEquals(ComputeCallGraph.print(full.graph()), ComputeCallGraph.print(incremental.graph()));
        assertEquals(List.copyOf(full.graph().vertices()).toString(),
                List.copyOf(incremental.graph().vertices()).toString(), "vertex order");
        assertEquals(names(full), names(incremental));
    }

    private static Set<String> names(ComputeCallGraph ccg) {
        return ccg.recursiveMethods().stream().map(mi -> mi.fullyQualifiedName())
                .collect(Collectors.toCollection(java.util.TreeSet::new));
    }

    private static JavaInspector inspector(SourceSet set) {
        try {
            JavaInspector inspector = new JavaInspectorImpl(true, false);
            InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                    .addSourceSets(set)
                    .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                    .build();
            inspector.initialize(inputConfiguration);
            return inspector;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void write(Path sourceRoot, String relative, String content) throws IOException {
        Path file = sourceRoot.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}
