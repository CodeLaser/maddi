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
import org.e2immu.analyzer.modification.prepwork.callgraph.ComputePartOfConstructionFinalField;
import org.e2immu.analyzer.modification.prepwork.callgraph.PrimaryTypeUseGraph;
import org.e2immu.analyzer.modification.prepwork.io.AnalysisFingerprint;
import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.element.FingerPrint;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.InfoMapView;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
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

import static org.e2immu.language.inspection.api.integration.JavaInspector.InvalidationState.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The payoff of the exposed {@link InfoMapView}: an <em>outside-reload</em> carry of a spared REWIRE type's analyzer
 * output. Two source sets (dependent {@code User} uses {@code Base} in main); analyse, edit {@code Base}, reload so
 * {@code User} is REWIRE (its analysis dropped), then carry {@code User}'s prior analyzer output onto the rewired
 * object via {@code javaInspector.lastRewireInfoMap()} — no re-analysis. The carried type's analysisFingerprint must
 * equal its pre-reload fingerprint: the carry faithfully reproduces the output with references re-pointed. This is
 * exactly what the early-cutoff skip does for a type the fingerprint says is unchanged.
 */
public class TestOutsideReloadCarry {

    private static final String USER_FQN = "c.d.User";

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
                public String use() { return base.name(); }
                public Base get() { return base; }
            }
            """;

    @TempDir
    Path root;
    private JavaInspector javaInspector;
    private Path baseFile;

    private InputConfiguration inputConfiguration() {
        SourceSet main = new SourceSetImpl.Builder().setName("main")
                .setSourceDirectories(List.of(root.resolve("main-src")))
                .setUri(root.resolve("main-classes").toUri()).build();
        SourceSet dependent = new SourceSetImpl.Builder().setName("dependent")
                .setSourceDirectories(List.of(root.resolve("dep-src")))
                .setUri(root.resolve("dep-src").toUri())
                .setDependencies(List.of(main)).build();
        return new InputConfigurationImpl.Builder()
                .addSourceSets(main, dependent)
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

    private ComputeCallGraph analyze(ParseResult pr) {
        PrepAnalyzer prep = new PrepAnalyzer(javaInspector.runtime(),
                new PrepAnalyzer.Options.Builder().setFaultTolerant(true).build());
        ComputeCallGraph ccg = prep.doPrimaryTypesReturnComputeCallGraph(Set.copyOf(pr.primaryTypes()),
                pr.sourceSetToModuleInfoMap().values(), t -> false, false);
        List<Info> order = new ComputeAnalysisOrder().go(ccg.graph(), false);
        new IteratingAnalyzerImpl(javaInspector,
                new IteratingAnalyzerImpl.ConfigurationBuilder().setMaxIterations(20)
                        .setStopWhenCycleDetectedAndNoImprovements(true).build()).analyze(order, ccg.graph());
        return ccg;
    }

    // the DERIVED analyzer output: verdicts + link summaries, excluding VARIABLE_DATA (recomputed) and the
    // carryOnRewire properties the rewire phase already carried onto the rewired object (e.g. GET_SET_FIELD) -- so the
    // outside carry does not double-set them. Together, phase-carry + this = the full analyzer output.
    private static final java.util.function.Predicate<org.e2immu.language.cst.api.analysis.Property> DERIVED_OUTPUT =
            p -> AnalysisFingerprint.ANALYZER_OUTPUT_ONLY.test(p) && !p.carryOnRewire();

    private static boolean containsIdentity(Iterable<? extends Info> set, Info target) {
        for (Info i : set) {
            if (i == target) return true;
        }
        return false;
    }

    /** Carry a type's derived analyzer output onto its rewired copy, via the read-only view. */
    private static void carryAnalyzerOutput(TypeInfo oldType, InfoMapView view) {
        view.typeInfo(oldType).analysis().setAll(oldType.analysis().rewire(view, DERIVED_OUTPUT));
        oldType.constructorAndMethodStream().forEach(oldMethod -> {
            view.methodInfo(oldMethod).analysis().setAll(oldMethod.analysis().rewire(view, DERIVED_OUTPUT));
            for (ParameterInfo oldParam : oldMethod.parameters()) {
                view.parameterInfo(oldParam).analysis().setAll(oldParam.analysis().rewire(view, DERIVED_OUTPUT));
            }
        });
        for (FieldInfo oldField : oldType.fields()) {
            view.fieldInfo(oldField).analysis().setAll(oldField.analysis().rewire(view, DERIVED_OUTPUT));
        }
    }

    @DisplayName("carry a spared REWIRE type's analyzer output onto its rewired object via the exposed view")
    @Test
    public void testOutsideReloadCarry() throws IOException {
        Path mainSrc = Files.createDirectories(root.resolve("main-src/a/b"));
        baseFile = Files.writeString(mainSrc.resolve("Base.java"), BASE);
        Path depSrc = Files.createDirectories(root.resolve("dep-src/c/d"));
        Files.writeString(depSrc.resolve("User.java"), USER);
        compile(List.of(baseFile), Files.createDirectories(root.resolve("main-classes")));

        javaInspector = new JavaInspectorImpl(true, false);
        javaInspector.initialize(inputConfiguration());
        ParseResult pr0 = javaInspector.parse(Map.of(), JavaInspectorImpl.DETAILED_SOURCES).parseResult();
        ComputeCallGraph ccg0 = analyze(pr0);
        TypeInfo oldUser = pr0.findType(USER_FQN);

        // edit Base (a comment): User's analyzer output is unchanged, but User is REWIRE (it uses Base)
        Files.writeString(baseFile, "// a comment\n\n" + BASE);
        JavaInspector.ReloadResult rr = javaInspector.reloadSources(inputConfiguration(), Map.of());
        Set<TypeInfo> changed = rr.sourceHasChanged();
        // dependents from the call graph of the first (pre-reload) analysis: here User depends on Base
        Set<TypeInfo> dependents = new PrimaryTypeUseGraph(ccg0.graph()).dependentsOf(changed);
        JavaInspector.Invalidated inv = ti ->
                changed.contains(ti) ? INVALID : dependents.contains(ti) ? REWIRE : UNCHANGED;
        ParseResult pr1 = javaInspector.parse(new JavaInspector.ParseOptions.Builder()
                .setDetailedSources(true).setInvalidated(inv).build()).parseResult();

        TypeInfo newUser = pr1.findType(USER_FQN);
        assertNotSame(oldUser, newUser, "User was rewired");
        MethodInfo newGet = newUser.findUniqueMethod("get", 0);
        // REWIRE dropped the derived output: the rewired get() has no METHOD_LINKS yet
        assertNull(newGet.analysis().getOrNull(MethodLinkedVariablesImpl.METHOD_LINKS, MethodLinkedVariablesImpl.class),
                "REWIRE dropped the derived analyzer output");

        // the payoff: carry the prior analyzer output onto the rewired object via the EXPOSED view — no re-analysis
        InfoMapView view = javaInspector.lastRewireInfoMap();
        assertNotNull(view, "the reload must expose its rewire");
        assertSame(newUser, view.typeInfo(oldUser));
        carryAnalyzerOutput(oldUser, view);

        // carried: the verdicts and link summaries are back on the rewired objects
        assertNotNull(newUser.analysis().getOrNull(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.class),
                "the type verdict was carried");
        assertNotNull(newGet.analysis().getOrNull(MethodLinkedVariablesImpl.METHOD_LINKS,
                MethodLinkedVariablesImpl.class), "METHOD_LINKS was carried onto the rewired get()");

        // re-pointed: PART_OF_CONSTRUCTION holds the rewired constructor object, not the one it replaced
        MethodInfo newCtor = oldUser.constructorAndMethodStream().filter(MethodInfo::isConstructor)
                .map(view::methodInfo).findFirst().orElseThrow();
        MethodInfo oldCtor = oldUser.constructorAndMethodStream().filter(MethodInfo::isConstructor)
                .findFirst().orElseThrow();
        assertNotSame(oldCtor, newCtor);
        Value.SetOfInfo poc = newUser.analysis()
                .getOrNull(ComputePartOfConstructionFinalField.PART_OF_CONSTRUCTION, ValueImpl.SetOfInfoImpl.class);
        assertNotNull(poc, "PART_OF_CONSTRUCTION was carried");
        assertTrue(containsIdentity(poc.infoSet(), newCtor), "carried PART_OF_CONSTRUCTION points at the rewired ctor");
        assertFalse(containsIdentity(poc.infoSet(), oldCtor), "not the replaced ctor");
    }
}
