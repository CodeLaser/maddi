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

package org.e2immu.analyzer.run.openjdkmain;

import org.e2immu.analyzer.modification.analyzer.impl.IteratingAnalyzerImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.callgraph.ComputeAnalysisOrder;
import org.e2immu.analyzer.modification.prepwork.callgraph.ComputeCallGraph;
import org.e2immu.analyzer.modification.prepwork.callgraph.EarlyCutoffWorklist;
import org.e2immu.analyzer.modification.prepwork.callgraph.PrimaryTypeUseGraph;
import org.e2immu.analyzer.modification.prepwork.io.AnalysisFingerprint;
import org.e2immu.analyzer.modification.prepwork.io.IncrementalState;
import org.e2immu.language.cst.api.element.FingerPrint;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.ConsumptionEdgeRecorder;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.api.resource.MD5FingerPrint;
import org.e2immu.language.inspection.openjdk.JavaInspectorImpl;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.e2immu.language.inspection.api.integration.JavaInspector.InvalidationState.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Task #35 phase C: the early-cutoff worklist woken by RECORDED consumption edges instead of the
 * use graph — the giant-SCC composition of DESIGN-incremental-v2 §0. Run 0 records (recorder
 * armed) and leaves an {@link IncrementalState} (per-type output fingerprints + type-level
 * consumer edges); the resume run seeds the worklist with the changed types and walks DIRECT
 * consumers only, with the output fingerprint as the frontier. Same chain as
 * {@link TestEarlyCutoffWorklistDriver}: a semantic edit at Base must wake and recompute Mid
 * through the CONSUMPTION edge; a comment edit must cut off at Base.
 */
public class TestIncrementalConsumptionWake {

    private static final String BASE_FQN = "a.b.Base";
    private static final String MID_FQN = "c.d.Mid";
    private static final String TOP_FQN = "e.f.Top";

    private static final String BASE = """
            package a.b;
            public class Base {
                public String name() { return "base"; }
            }
            """;
    private static final String BASE_SEMANTIC = """
            package a.b;
            public class Base {
                private String n = "base";
                public String name() { return n; }
                public void rename(String x) { this.n = x; }
            }
            """;
    private static final String MID = """
            package c.d;
            import a.b.Base;
            public class Mid {
                private final Base base;
                public Mid(Base b) { this.base = b; }
                public String viaMid() { return base.name(); }
            }
            """;
    private static final String TOP = """
            package e.f;
            import c.d.Mid;
            public class Top {
                private final Mid mid;
                public Top(Mid m) { this.mid = m; }
                public String viaTop() { return mid.viaMid(); }
            }
            """;

    @TempDir
    Path root;
    private JavaInspector javaInspector;
    private Path baseFile;
    private ParseResult pr1;

    private InputConfiguration inputConfiguration() {
        var main = new SourceSetImpl.Builder().setName("main")
                .setSourceDirectories(List.of(root.resolve("main-src")))
                .setUri(root.resolve("main-src").toUri()).build();
        var mid = new SourceSetImpl.Builder().setName("mid")
                .setSourceDirectories(List.of(root.resolve("mid-src")))
                .setUri(root.resolve("mid-src").toUri())
                .setDependencies(List.of(main)).build();
        var top = new SourceSetImpl.Builder().setName("top")
                .setSourceDirectories(List.of(root.resolve("top-src")))
                .setUri(root.resolve("top-src").toUri())
                .setDependencies(List.of(mid)).build();
        return new InputConfigurationImpl.Builder().addSourceSets(main, mid, top)
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES).build();
    }

    private PrepAnalyzer prep() {
        return new PrepAnalyzer(javaInspector.runtime(), new PrepAnalyzer.Options.Builder().setFaultTolerant(true).build());
    }

    private void analyze(List<Info> order, org.e2immu.util.internal.graph.G<Info> graph) {
        var analyzer = new IteratingAnalyzerImpl(javaInspector, new IteratingAnalyzerImpl.ConfigurationBuilder()
                .setMaxIterations(20).setStopWhenCycleDetectedAndNoImprovements(true).build());
        if (graph == null) analyzer.analyze(order);
        else analyzer.analyze(order, graph);
    }

    /** run 0: write sources, parse, prep+analyze with the recorder armed, capture + save + reload the state */
    private IncrementalState run0(ComputeCallGraph[] ccgOut) throws IOException {
        Path mainSrc = Files.createDirectories(root.resolve("main-src/a/b"));
        baseFile = Files.writeString(mainSrc.resolve("Base.java"), BASE);
        Files.writeString(Files.createDirectories(root.resolve("mid-src/c/d")).resolve("Mid.java"), MID);
        Files.writeString(Files.createDirectories(root.resolve("top-src/e/f")).resolve("Top.java"), TOP);

        javaInspector = new JavaInspectorImpl(true, false);
        javaInspector.initialize(inputConfiguration());
        ParseResult pr0 = javaInspector.parse(Map.of(), JavaInspectorImpl.DETAILED_SOURCES).parseResult();

        ConsumptionEdgeRecorder.arm();
        ConsumptionEdgeRecorder.reset();
        ComputeCallGraph ccg = prep().doPrimaryTypesReturnComputeCallGraph(Set.copyOf(pr0.primaryTypes()),
                pr0.sourceSetToModuleInfoMap().values(), t -> false, false);
        analyze(new ComputeAnalysisOrder().go(ccg.graph(), false), ccg.graph());
        ccgOut[0] = ccg;

        IncrementalState captured = IncrementalState.capture(javaInspector.runtime(), pr0.primaryTypes(),
                ConsumptionEdgeRecorder.edgesSnapshot());
        Path stateDir = Files.createDirectories(root.resolve("state"));
        captured.save(stateDir.toFile());
        IncrementalState loaded = IncrementalState.load(stateDir.toFile());
        assertEquals(captured.analysisFingerprints(), loaded.analysisFingerprints(), "fingerprint round-trip");
        assertEquals(captured.consumers(), loaded.consumers(), "consumer-edge round-trip");
        return loaded;
    }

    /** reload after an edit and drive the worklist with CONSUMPTION-edge wake */
    private Set<String> reloadAndDrive(ComputeCallGraph ccg0, IncrementalState state) throws IOException {
        JavaInspector.ReloadResult rr = javaInspector.reloadSources(inputConfiguration(), Map.of());
        Set<TypeInfo> changed = rr.sourceHasChanged();
        Set<TypeInfo> dependents = new PrimaryTypeUseGraph(ccg0.graph()).dependentsOf(changed);
        JavaInspector.Invalidated inv = ti ->
                changed.contains(ti) ? INVALID : dependents.contains(ti) ? REWIRE : UNCHANGED;
        pr1 = javaInspector.parse(new JavaInspector.ParseOptions.Builder()
                .setDetailedSources(true).setFailFast(true).setInvalidated(inv).build()).parseResult();

        // the wake relation: DIRECT consumers from the recorded state, mapped onto the fresh parse
        Function<TypeInfo, Set<TypeInfo>> consumersOf = t -> state.consumers()
                .getOrDefault(t.fullyQualifiedName(), Set.of()).stream()
                .map(fqn -> pr1.findType(fqn))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        Set<TypeInfo> seed = changed.stream().map(t -> pr1.findType(t.fullyQualifiedName()))
                .collect(Collectors.toSet());
        Set<String> recomputed = new TreeSet<>();
        Function<TypeInfo, FingerPrint> recompute = t -> {
            recomputed.add(t.fullyQualifiedName());
            analyze(prep().doPrimaryType(t), null);
            return AnalysisFingerprint.of(javaInspector.runtime(), t);
        };
        EarlyCutoffWorklist.run(seed, consumersOf,
                t -> {
                    String fp = state.analysisFingerprints().get(t.fullyQualifiedName());
                    return fp == null ? null : MD5FingerPrint.from(fp);
                },
                recompute);
        return recomputed;
    }

    @DisplayName("recorded consumption edges: Base->Mid exists, semantic edit wakes Mid, comment edit cuts off")
    @Test
    public void test() throws IOException {
        ComputeCallGraph[] ccg = new ComputeCallGraph[1];
        IncrementalState state = run0(ccg);

        assertFalse(state.analysisFingerprints().isEmpty(), "run 0 must fingerprint the chain");
        Set<String> baseConsumers = state.consumers().getOrDefault(BASE_FQN, Set.of());
        assertTrue(baseConsumers.contains(MID_FQN),
                "Mid consumed Base's summaries during analysis: " + state.consumers());

        // comment edit: recompute Base only; its output fingerprint is unchanged, the frontier cuts off
        Files.writeString(baseFile, "// a comment\n\n" + BASE);
        Set<String> afterComment = reloadAndDrive(ccg[0], state);
        assertEquals(Set.of(BASE_FQN), afterComment, "comment edit: consumption wake spares Mid and Top");

        // semantic edit: Base's output moves; the consumption edge wakes Mid
        Files.writeString(baseFile, BASE_SEMANTIC);
        Set<String> afterSemantic = reloadAndDrive(ccg[0], state);
        assertTrue(afterSemantic.contains(BASE_FQN), "the edited type is recomputed");
        assertTrue(afterSemantic.contains(MID_FQN),
                "Base's output moved, so its DIRECT consumer Mid is woken: " + afterSemantic);
    }
}
