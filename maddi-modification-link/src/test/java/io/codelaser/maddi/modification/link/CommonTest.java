package io.codelaser.maddi.modification.link;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.codelaser.maddi.modification.prepwork.PrepAnalyzer;
import io.codelaser.maddi.modification.prepwork.io.LoadAnalysisResults;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.api.statement.Statement;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.codelaser.maddi.modification.prepwork.io.LoadAnalysisResults.ANALYZED_RESULTS;
import static io.codelaser.maddi.inspection.resource.SourceSetImpl.testProtocolSourceSet;
import static org.junit.jupiter.api.Assertions.assertTrue;


public abstract class CommonTest {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CommonTest.class);

    protected JavaInspector javaInspector;
    protected PrepAnalyzer prepAnalyzer;
    protected Runtime runtime;

    @BeforeAll
    public static void beforeAll() {
        ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
        ((Logger) LoggerFactory.getLogger("io.codelaser.maddi.modification.link")).setLevel(Level.DEBUG);
    }

    @BeforeEach
    public void beforeEach() throws IOException {
        SourceSet testProtocol = testProtocolSourceSet();
        javaInspector = io.codelaser.maddi.modification.common.CommonTest.javaInspectorFactory().withSources(testProtocol);
        runtime = javaInspector.runtime();
        javaInspector.setParameterNames(true); // faithful class-file parameter names; must precede any loading
        javaInspector.onlyPreload(); // we'll run more later
        LoadAnalysisResults lar = new LoadAnalysisResults(javaInspector.runtime(), testProtocol);
        lar.go(ANALYZED_RESULTS);

        prepAnalyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
    }

    // the openjdk parser keeps the implicit super() as a (synthetic) first statement of a constructor; the
    // maddi parser omits it. Tests that index into constructor statements use this to skip it.
    protected static List<Statement> realStatements(MethodInfo methodInfo) {
        return methodInfo.methodBody().statements().stream().filter(s -> !s.isSynthetic()).toList();
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
