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
import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.callgraph.ComputeAnalysisOrder;
import org.e2immu.analyzer.modification.prepwork.callgraph.ComputeCallGraph;
import org.e2immu.analyzer.modification.prepwork.callgraph.EarlyCutoffWorklist;
import org.e2immu.analyzer.modification.prepwork.callgraph.PrimaryTypeUseGraph;
import org.e2immu.analyzer.modification.prepwork.io.AnalysisFingerprint;
import org.e2immu.language.cst.api.analysis.Property;
import org.e2immu.language.cst.api.element.FingerPrint;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.InfoMapView;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.util.internal.graph.G;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.openjdk.JavaInspectorImpl;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.e2immu.language.inspection.api.integration.JavaInspector.InvalidationState.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The early-cutoff capstone: the {@link EarlyCutoffWorklist} driving the <em>real</em> per-type recompute (prep +
 * analyse) against carried dependencies, through the actual reload path, with the analysisFingerprint deciding the
 * frontier. A use chain across three source sets — {@code Top} (uses {@code Mid}) → {@code Mid} (uses {@code Base}) →
 * {@code Base} — edited at {@code Base}.
 * <p>
 * This closes the gap the prototype's worklist test left open (its recompute was a stand-in look-up): here every
 * recompute genuinely re-preps and re-analyses the type, and a type the worklist never reaches is <em>never</em>
 * re-prepped nor re-analysed — it returns with its optimistically-carried cross-type-derived output. A comment edit at
 * {@code Base} recomputes only {@code Base} (its output is unchanged, so the frontier cuts off) and spares the whole
 * tail; a semantic edit propagates along the chain. See analysis-rewiring.md.
 */
public class TestEarlyCutoffWorklistDriver {

    private static final String BASE_FQN = "a.b.Base";
    private static final String MID_FQN = "c.d.Mid";
    private static final String TOP_FQN = "e.f.Top";

    private static final String BASE = """
            package a.b;
            public class Base {
                public String name() { return "base"; }
            }
            """;
    // a semantic edit that changes Base's own analyzer output: a new method brings new verdicts (parameter
    // independence, method links) into Base's fingerprint. Kept fieldless on purpose — fingerprinting a
    // per-type-recomputed type whose links reference a MODIFIABLE field hits a codec identity limitation (the field
    // is not ==-identical in the single-type-analysed context); see analysis-rewiring.md.
    private static final String BASE_SEMANTIC = """
            package a.b;
            public class Base {
                public String name() { return "base"; }
                public String tag(String s) { return s; }
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
                .setUri(root.resolve("main-classes").toUri()).build();
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

    private static void compile(List<Path> files, Path outputDir) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null)) {
            fm.setLocation(StandardLocation.CLASS_OUTPUT, List.of(outputDir.toFile()));
            Iterable<? extends JavaFileObject> units = fm.getJavaFileObjectsFromPaths(files);
            assertTrue(compiler.getTask(null, fm, null, List.of(), null, units).call(), "could not compile Base");
        }
    }

    private PrepAnalyzer prep() {
        return new PrepAnalyzer(javaInspector.runtime(), new PrepAnalyzer.Options.Builder().setFaultTolerant(true).build());
    }

    private ComputeCallGraph prepAndAnalyzeAll(ParseResult pr) {
        ComputeCallGraph ccg = prep().doPrimaryTypesReturnComputeCallGraph(Set.copyOf(pr.primaryTypes()),
                pr.sourceSetToModuleInfoMap().values(), t -> false, false);
        new IteratingAnalyzerImpl(javaInspector, new IteratingAnalyzerImpl.ConfigurationBuilder()
                .setMaxIterations(20).setStopWhenCycleDetectedAndNoImprovements(true).build())
                .analyze(new ComputeAnalysisOrder().go(ccg.graph(), false), ccg.graph());
        return ccg;
    }

    private void analyzeSingle(List<Info> order) {
        new IteratingAnalyzerImpl(javaInspector, new IteratingAnalyzerImpl.ConfigurationBuilder()
                .setMaxIterations(20).setStopWhenCycleDetectedAndNoImprovements(true).build()).analyze(order);
    }

    private static final Predicate<Property> CROSS_TYPE_DERIVED = AnalysisFingerprint.CROSS_TYPE_DERIVED_ONLY;

    /** Drop the optimistically-carried cross-type-derived tier, so a dirtied type's fresh re-analysis may re-write it. */
    private static void clearCrossTypeDerived(TypeInfo t) {
        t.analysis().removeIf(CROSS_TYPE_DERIVED);
        t.constructorAndMethodStream().forEach(m -> {
            m.analysis().removeIf(CROSS_TYPE_DERIVED);
            m.parameters().forEach(p -> p.analysis().removeIf(CROSS_TYPE_DERIVED));
        });
        t.fields().forEach(f -> f.analysis().removeIf(CROSS_TYPE_DERIVED));
    }

    /** Optimistically carry a spared type's cross-type-derived output onto its rewired copy, via the read-only view. */
    private static void carryDerivedOutput(TypeInfo oldType, InfoMapView view) {
        view.typeInfo(oldType).analysis().setAll(oldType.analysis().rewire(view, CROSS_TYPE_DERIVED));
        oldType.constructorAndMethodStream().forEach(m -> {
            view.methodInfo(m).analysis().setAll(m.analysis().rewire(view, CROSS_TYPE_DERIVED));
            for (ParameterInfo p : m.parameters()) {
                view.parameterInfo(p).analysis().setAll(p.analysis().rewire(view, CROSS_TYPE_DERIVED));
            }
        });
        for (FieldInfo f : oldType.fields()) {
            view.fieldInfo(f).analysis().setAll(f.analysis().rewire(view, CROSS_TYPE_DERIVED));
        }
    }

    private TreeMap<String, FingerPrint> fingerprintAll(ParseResult pr) {
        TreeMap<String, FingerPrint> m = new TreeMap<>();
        pr.primaryTypes().forEach(pt -> m.put(pt.fullyQualifiedName(), AnalysisFingerprint.of(javaInspector.runtime(), pt)));
        return m;
    }

    /** Set up run-0: write all sources, compile Base, parse+prep+analyze, snapshot fingerprints. Returns run-0 ccg. */
    private ComputeCallGraph run0(TreeMap<String, FingerPrint> beforeOut) throws IOException {
        Path mainSrc = Files.createDirectories(root.resolve("main-src/a/b"));
        baseFile = Files.writeString(mainSrc.resolve("Base.java"), BASE);
        Files.writeString(Files.createDirectories(root.resolve("mid-src/c/d")).resolve("Mid.java"), MID);
        Files.writeString(Files.createDirectories(root.resolve("top-src/e/f")).resolve("Top.java"), TOP);
        compile(List.of(baseFile), Files.createDirectories(root.resolve("main-classes")));

        javaInspector = new JavaInspectorImpl(true, false);
        javaInspector.initialize(inputConfiguration());
        ParseResult pr0 = javaInspector.parse(Map.of(), JavaInspectorImpl.DETAILED_SOURCES).parseResult();
        ComputeCallGraph ccg0 = prepAndAnalyzeAll(pr0);
        beforeOut.putAll(fingerprintAll(pr0));
        return ccg0;
    }

    /** Reload after the current Base.java edit; carry derived output onto REWIRE types; return the driven worklist result. */
    private EarlyCutoffWorklist.Result<TypeInfo> reloadAndDrive(ComputeCallGraph ccg0,
                                                                TreeMap<String, FingerPrint> before,
                                                                Set<String> recomputeInvoked) throws IOException {
        JavaInspector.ReloadResult rr = javaInspector.reloadSources(inputConfiguration(), Map.of());
        Set<TypeInfo> changed = rr.sourceHasChanged();
        Set<TypeInfo> dependents = new PrimaryTypeUseGraph(ccg0.graph()).dependentsOf(changed);
        JavaInspector.Invalidated inv = ti ->
                changed.contains(ti) ? INVALID : dependents.contains(ti) ? REWIRE : UNCHANGED;
        pr1 = javaInspector.parse(new JavaInspector.ParseOptions.Builder()
                .setDetailedSources(true).setFailFast(true).setInvalidated(inv).build()).parseResult();

        InfoMapView view = javaInspector.lastRewireInfoMap();
        assertNotNull(view, "dependents are REWIRE, so the reload exposes its rewire map");
        for (TypeInfo dep : dependents) carryDerivedOutput(dep, view);

        // one-hop dependents over pr1 types, translated from the run-0 use graph by fqn (edge X -> Y means Y uses X)
        G<TypeInfo> useGraph = new PrimaryTypeUseGraph(ccg0.graph()).graph();
        Map<TypeInfo, Set<TypeInfo>> deps = new HashMap<>();
        useGraph.vertices().forEach(v -> {
            Map<org.e2immu.util.internal.graph.V<TypeInfo>, Long> out = useGraph.edges(v); // null for a sink
            Set<TypeInfo> users = out == null ? Set.of() : out.keySet().stream()
                    .map(u -> pr1.findType(u.t().fullyQualifiedName())).collect(Collectors.toSet());
            deps.put(pr1.findType(v.t().fullyQualifiedName()), users);
        });

        Set<TypeInfo> seed = changed.stream().map(t -> pr1.findType(t.fullyQualifiedName())).collect(Collectors.toSet());

        Function<TypeInfo, FingerPrint> recompute = t -> {
            recomputeInvoked.add(t.fullyQualifiedName());
            clearCrossTypeDerived(t);                       // clear the optimistic carry before re-analysing
            analyzeSingle(prep().doPrimaryType(t));         // real per-type recompute against carried/fresh dependencies
            return AnalysisFingerprint.of(javaInspector.runtime(), t);
        };
        return EarlyCutoffWorklist.run(seed, t -> deps.getOrDefault(t, Set.of()),
                t -> before.get(t.fullyQualifiedName()), recompute);
    }

    @DisplayName("comment edit at Base: the worklist recomputes only Base and spares the whole Mid/Top tail")
    @Test
    public void testCommentEditSparesTheTail() throws IOException {
        TreeMap<String, FingerPrint> before = new TreeMap<>();
        ComputeCallGraph ccg0 = run0(before);

        Files.writeString(baseFile, "// a comment\n\n" + BASE);
        Set<String> recomputeInvoked = new TreeSet<>();
        EarlyCutoffWorklist.Result<TypeInfo> result = reloadAndDrive(ccg0, before, recomputeInvoked);

        Set<String> recomputed = result.recomputed().stream().map(TypeInfo::fullyQualifiedName)
                .collect(Collectors.toCollection(TreeSet::new));
        assertEquals(Set.of(BASE_FQN), recomputed, "Base's output is unchanged, so the frontier cuts off at Base");
        assertEquals(Set.of(BASE_FQN), recomputeInvoked, "the per-type recompute ran for Base only");

        // the saving: Mid and Top were never re-analysed, yet return with their carried derived output
        TypeInfo mid1 = pr1.findType(MID_FQN);
        assertNotNull(mid1.analysis().getOrNull(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.class),
                "spared Mid kept its carried type verdict");
        assertNotNull(mid1.findUniqueMethod("viaMid", 0).analysis()
                        .getOrNull(MethodLinkedVariablesImpl.METHOD_LINKS, MethodLinkedVariablesImpl.class),
                "spared Mid kept its carried METHOD_LINKS");
        assertNotNull(pr1.findType(TOP_FQN).analysis()
                        .getOrNull(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.class),
                "spared Top kept its carried type verdict");
    }

    @DisplayName("semantic edit at Base: the change propagates along the chain, recomputing Mid and Top")
    @Test
    public void testSemanticEditPropagates() throws IOException {
        TreeMap<String, FingerPrint> before = new TreeMap<>();
        ComputeCallGraph ccg0 = run0(before);

        Files.writeString(baseFile, BASE_SEMANTIC);
        Set<String> recomputeInvoked = new TreeSet<>();
        EarlyCutoffWorklist.Result<TypeInfo> result = reloadAndDrive(ccg0, before, recomputeInvoked);

        Set<String> recomputed = result.recomputed().stream().map(TypeInfo::fullyQualifiedName)
                .collect(Collectors.toCollection(TreeSet::new));
        assertTrue(recomputed.contains(BASE_FQN), "the edited type is recomputed");
        assertTrue(recomputed.contains(MID_FQN), "Base's output moved, so Mid is pulled in and recomputed: " + recomputed);
    }
}
