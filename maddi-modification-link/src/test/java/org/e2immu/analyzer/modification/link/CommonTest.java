package org.e2immu.analyzer.modification.link;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.io.LoadAnalysisResults;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.e2immu.analyzer.modification.prepwork.io.LoadAnalysisResults.ANALYZED_RESULTS;
import static org.e2immu.language.inspection.resource.SourceSetImpl.testProtocolSourceSet;
import static org.junit.jupiter.api.Assertions.assertTrue;


public abstract class CommonTest {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CommonTest.class);

    protected JavaInspector javaInspector;
    protected PrepAnalyzer prepAnalyzer;
    protected Runtime runtime;

    @BeforeAll
    public static void beforeAll() {
        ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
        ((Logger) LoggerFactory.getLogger("org.e2immu.analyzer.modification.link")).setLevel(Level.DEBUG);
    }

    @BeforeEach
    public void beforeEach() throws IOException {
        SourceSet testProtocol = testProtocolSourceSet();
        javaInspector = org.e2immu.analyzer.modification.common.CommonTest.javaInspectorFactory().withSources(testProtocol);
        runtime = javaInspector.runtime();
        javaInspector.onlyPreload(); // we'll run more later
        LoadAnalysisResults lar = new LoadAnalysisResults(javaInspector.runtime(), testProtocol);
        lar.go(ANALYZED_RESULTS);

        prepAnalyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
    }

    protected static <K, V> String nice(Map<K, V> map) {
        return map.entrySet().stream().map(Object::toString).sorted().collect(Collectors.joining(", "));
    }

    protected void prepWork(TypeInfo typeInfo) {
        List<TypeInfo> typesLoaded = javaInspector.compiledTypesManager().typesLoaded(true);
        assertTrue(typesLoaded.stream().anyMatch(ti -> "java.util.ArrayList".equals(ti.fullyQualifiedName())));

        prepAnalyzer.doPrimaryType(typeInfo);
    }
}
