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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static org.e2immu.analyzer.modification.prepwork.io.LoadAnalysisResults.ANALYZED_RESULTS;
import static org.e2immu.language.inspection.resource.SourceSetImpl.testProtocolSourceSet;


public abstract class CommonTest {
    protected JavaInspector javaInspector;
    protected PrepAnalyzer prepAnalyzer;
    protected Runtime runtime;
    protected ModAnalyzerForTesting analyzer;
    // kept for source compatibility with tests that requested extra JDK modules; the openjdk inspector factory
    // already puts java.base/java.desktop/java.net.http on the classpath, so these are currently covered there
    protected final String[] jmods;

    protected CommonTest() {
        this.jmods = new String[0];
    }

    protected CommonTest(String... jmods) {
        this.jmods = jmods;
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
        javaInspector = org.e2immu.analyzer.modification.common.CommonTest.javaInspectorFactory().withSources(testProtocol);
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
