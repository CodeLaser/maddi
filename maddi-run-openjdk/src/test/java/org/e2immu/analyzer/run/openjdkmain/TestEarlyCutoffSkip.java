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

import org.e2immu.analyzer.modification.analyzer.IteratingAnalyzer;
import org.e2immu.analyzer.modification.analyzer.impl.IteratingAnalyzerImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.callgraph.ComputeAnalysisOrder;
import org.e2immu.analyzer.modification.prepwork.callgraph.ComputeCallGraph;
import org.e2immu.analyzer.modification.prepwork.io.AnalysisFingerprint;
import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
import org.e2immu.language.cst.api.analysis.Property;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.InfoMapView;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.openjdk.JavaInspectorImpl;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.intellij.lang.annotations.Language;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static org.e2immu.language.inspection.api.integration.JavaInspector.InvalidationState.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The early-cutoff skip, running end to end and saving work. Two source sets: {@code User} (dependent) uses
 * {@code Base} (main). Analyse, edit {@code Base} with a comment, reload — {@code Base} is INVALID, {@code User} is
 * REWIRE. Then: carry {@code User}'s derived analyzer output onto its rewired object via the exposed view, and
 * <strong>re-analyse only {@code Base}</strong> (the INVALID set). {@code Base}'s output is unchanged, so {@code User}
 * is spared: it is never re-prepped nor re-analysed, keeping its carried result. The saving is exactly that — the
 * analysis order fed to the analyzer contains {@code Base}'s members and not {@code User}'s.
 * <p>
 * Prep runs on the INVALID set only, so it never touches the carried REWIRE types (no double-set); this is why the
 * demonstration does not yet need the per-Property tier flag (see {@code analysis-rewiring.md}).
 */
public class TestEarlyCutoffSkip {

    private static final String USER_FQN = "c.d.User";
    private static final String BASE_FQN = "a.b.Base";

    @Language("java")
    private static final String BASE = """
            package a.b;
            public class Base {
                public String name() { return "base"; }
            }
            """;
    @Language("java")
    private static final String USER = """
            package c.d;
            import a.b.Base;
            public class User {
                private final Base base;
                public User(Base b) { this.base = b; }
                public Base get() { return base; }
            }
            """;

    // the derived (cross-type) tier: exactly Property.AnalysisTier.CROSS_TYPE_DERIVED -- analyzer output minus both
    // the parse-time tier (rewire-carried) and the intrinsic tier (prepwork-recomputed).
    private static final Predicate<Property> DERIVED_OUTPUT = AnalysisFingerprint.CROSS_TYPE_DERIVED_ONLY;

    @TempDir
    Path root;
    private JavaInspector javaInspector;
    private Path baseFile;

    private InputConfiguration inputConfiguration() {
        var main = new SourceSetImpl.Builder().setName("main")
                .setSourceDirectories(List.of(root.resolve("main-src")))
                .setUri(root.resolve("main-classes").toUri()).build();
        var dependent = new SourceSetImpl.Builder().setName("dependent")
                .setSourceDirectories(List.of(root.resolve("dep-src")))
                .setUri(root.resolve("dep-src").toUri())
                .setDependencies(List.of(main)).build();
        return new InputConfigurationImpl.Builder().addSourceSets(main, dependent)
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES).build();
    }

    private static void compile(List<Path> files, Path outputDir) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null)) {
            fm.setLocation(StandardLocation.CLASS_OUTPUT, List.of(outputDir.toFile()));
            Iterable<? extends JavaFileObject> units = fm.getJavaFileObjectsFromPaths(files);
            assertTrue(compiler.getTask(null, fm, null, List.of(), null, units).call(), "could not compile main");
        }
    }

    private PrepAnalyzer prep() {
        return new PrepAnalyzer(javaInspector.runtime(), new PrepAnalyzer.Options.Builder().setFaultTolerant(true).build());
    }

    private void analyze(List<Info> order, ComputeCallGraph ccg) {
        new IteratingAnalyzerImpl(javaInspector,
                new IteratingAnalyzerImpl.ConfigurationBuilder().setMaxIterations(20)
                        .setStopWhenCycleDetectedAndNoImprovements(true).build()).analyze(order, ccg.graph());
    }

    private static void carryDerivedOutput(TypeInfo oldType, InfoMapView view) {
        view.typeInfo(oldType).analysis().setAll(oldType.analysis().rewire(view, DERIVED_OUTPUT));
        oldType.constructorAndMethodStream().forEach(m -> {
            view.methodInfo(m).analysis().setAll(m.analysis().rewire(view, DERIVED_OUTPUT));
            for (ParameterInfo p : m.parameters()) {
                view.parameterInfo(p).analysis().setAll(p.analysis().rewire(view, DERIVED_OUTPUT));
            }
        });
        for (FieldInfo f : oldType.fields()) {
            view.fieldInfo(f).analysis().setAll(f.analysis().rewire(view, DERIVED_OUTPUT));
        }
    }

    @DisplayName("edit Base -> re-analyse only Base, carry+spare User: fewer types analysed")
    @Test
    public void testSkipSparesTheDependent() throws IOException {
        Path mainSrc = Files.createDirectories(root.resolve("main-src/a/b"));
        baseFile = Files.writeString(mainSrc.resolve("Base.java"), BASE);
        Path depSrc = Files.createDirectories(root.resolve("dep-src/c/d"));
        Files.writeString(depSrc.resolve("User.java"), USER);
        compile(List.of(baseFile), Files.createDirectories(root.resolve("main-classes")));

        javaInspector = new JavaInspectorImpl(true, false);
        javaInspector.initialize(inputConfiguration());
        ParseResult pr0 = javaInspector.parse(Map.of(), JavaInspectorImpl.DETAILED_SOURCES).parseResult();
        ComputeCallGraph ccg0 = prep().doPrimaryTypesReturnComputeCallGraph(Set.copyOf(pr0.primaryTypes()),
                pr0.sourceSetToModuleInfoMap().values(), t -> false, false);
        analyze(new ComputeAnalysisOrder().go(ccg0.graph(), false), ccg0);
        TypeInfo oldUser = pr0.findType(USER_FQN);

        // edit Base (comment) and reload: Base INVALID, User REWIRE (it uses Base)
        Files.writeString(baseFile, "// a comment\n\n" + BASE);
        JavaInspector.ReloadResult rr = javaInspector.reloadSources(inputConfiguration(), Map.of());
        Set<TypeInfo> changed = rr.sourceHasChanged();
        Set<TypeInfo> dependents = new org.e2immu.analyzer.modification.prepwork.callgraph.PrimaryTypeUseGraph(
                ccg0.graph()).dependentsOf(changed);
        JavaInspector.Invalidated inv = ti ->
                changed.contains(ti) ? INVALID : dependents.contains(ti) ? REWIRE : UNCHANGED;
        ParseResult pr1 = javaInspector.parse(new JavaInspector.ParseOptions.Builder()
                .setDetailedSources(true).setInvalidated(inv).build()).parseResult();

        // 1) carry User's derived output onto the rewired object -- no re-analysis of User
        InfoMapView view = javaInspector.lastRewireInfoMap();
        assertNotNull(view);
        carryDerivedOutput(oldUser, view);

        // 2) re-analyse ONLY the INVALID set (Base); User is not in the order -> spared
        TypeInfo newBase = pr1.findType(BASE_FQN);
        TypeInfo newUser = pr1.findType(USER_FQN);
        List<Info> baseOrder = prep().doPrimaryType(newBase);
        assertTrue(baseOrder.stream().noneMatch(i -> USER_FQN.equals(i.typeInfo().primaryType().fullyQualifiedName())),
                "the saving: the re-analysis order contains Base's members, not User's");
        new IteratingAnalyzerImpl(javaInspector,
                new IteratingAnalyzerImpl.ConfigurationBuilder().setMaxIterations(20).build()).analyze(baseOrder);

        // Base was re-analysed (has its verdict); User was spared but has its carried output
        assertNotNull(newBase.analysis().getOrNull(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.class),
                "Base was re-analysed");
        assertNotNull(newUser.analysis().getOrNull(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.class),
                "User kept its carried type verdict without being re-analysed");
        assertNotNull(newUser.findUniqueMethod("get", 0).analysis()
                        .getOrNull(MethodLinkedVariablesImpl.METHOD_LINKS, MethodLinkedVariablesImpl.class),
                "User kept its carried METHOD_LINKS without being re-analysed");
    }
}
