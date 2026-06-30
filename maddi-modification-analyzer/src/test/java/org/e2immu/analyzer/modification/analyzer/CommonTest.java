package org.e2immu.analyzer.modification.analyzer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.e2immu.analyzer.modification.analyzer.impl.IteratingAnalyzerImpl;
import org.e2immu.analyzer.modification.analyzer.impl.ModAnalyzerForTesting;
import org.e2immu.analyzer.modification.analyzer.impl.SingleIterationAnalyzerImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.io.LoadAnalysisResults;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.e2immu.analyzer.modification.prepwork.io.LoadAnalysisResults.ANALYZED_RESULTS;
import static org.e2immu.language.inspection.resource.SourceSetImpl.testProtocolSourceSet;


public abstract class CommonTest {
    protected JavaInspector javaInspector;
    protected PrepAnalyzer prepAnalyzer;
    protected Runtime runtime;
    protected ModAnalyzerForTesting analyzer;
    // extra JDK modules a test needs on the classpath, given as "jmod:java.sql" etc.
    protected final String[] jmods;
    // per-directory source sets a clone-bench style test needs registered for openjdk single-file parsing; each
    // must be distinct so identically-named types in different directories do not collide (keyed by (fqn, set))
    protected final Map<String, SourceSet> openJdkSourceSetsByName = new HashMap<>();

    protected CommonTest() {
        this.jmods = new String[0];
    }

    protected CommonTest(String... jmods) {
        this.jmods = jmods;
    }

    // names of extra (per-directory) source sets a subclass needs registered for openjdk single-file parsing
    protected List<String> openJdkExtraSourceSetNames() {
        return List.of();
    }

    @BeforeAll
    public static void beforeAll() {
        ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
        ((Logger) LoggerFactory.getLogger("org.e2immu.analyzer.modification.link")).setLevel(Level.DEBUG);
        ((Logger) LoggerFactory.getLogger("org.e2immu.analyzer.modification.analyzer")).setLevel(Level.DEBUG);
    }

    @BeforeEach
    public void beforeEach() throws IOException {
        SourceSet testProtocol = testProtocolSourceSet();
        List<String> extraSourceSetNames = openJdkExtraSourceSetNames();
        if (extraSourceSetNames.isEmpty()) {
            javaInspector = org.e2immu.analyzer.modification.common.CommonTest.javaInspectorFactory().withSources(testProtocol);
        } else {
            // clone-bench style test: register one source set per directory (JDK types resolve via the global
            // input configuration; the per-directory sets keep identically-named types apart) and add the
            // requested extra JDK modules (jmods entries look like "jmod:java.sql")
            List<SourceSet> extraSets = new ArrayList<>();
            for (String name : extraSourceSetNames) {
                SourceSet set = new SourceSetImpl.Builder().setName(name).setUri(URI.create("file:/")).build();
                openJdkSourceSetsByName.put(name, set);
                extraSets.add(set);
            }
            List<String> jdkModules = Arrays.stream(jmods)
                    .map(s -> s.startsWith("jmod:") ? s.substring("jmod:".length()) : s).toList();
            javaInspector = org.e2immu.analyzer.modification.common.CommonTest
                    .javaInspectorWithExtras(testProtocol, extraSets, jdkModules);
            extraSets.forEach(SourceSet::computePriorityDependencies);
        }
        runtime = javaInspector.runtime();
        javaInspector.setParameterNames(true); // faithful class-file parameter names; must precede any loading
        javaInspector.onlyPreload(); // we'll run more later
        LoadAnalysisResults lar = new LoadAnalysisResults(javaInspector.runtime(), testProtocol);
        lar.go(ANALYZED_RESULTS);

        prepAnalyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());

        IteratingAnalyzer.Configuration configuration = new IteratingAnalyzerImpl.ConfigurationBuilder().build();
        analyzer = new SingleIterationAnalyzerImpl(javaInspector, configuration);
    }

    // the openjdk parser keeps the implicit super() as a (synthetic) first statement of a constructor; the
    // maddi parser omits it. Tests that index into constructor statements use this to skip it.
    protected static List<Statement> realStatements(MethodInfo methodInfo) {
        return methodInfo.methodBody().statements().stream().filter(s -> !s.isSynthetic()).toList();
    }

    protected List<Info> prepWork(TypeInfo typeInfo) {
        List<Info> analysisOrder = prepAnalyzer.doPrimaryType(typeInfo);
        assert analysisOrder.stream().noneMatch(i -> i instanceof TypeInfo ti && ti.simpleName().endsWith("$"))
                : "It looks like annotated API types are part of the analysis info list.";
        return analysisOrder;
    }
}
