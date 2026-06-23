package org.e2immu.analyzer.modification.link;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.io.LoadAnalysisResults;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.ToolChain;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.e2immu.support.SetOnce;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.e2immu.language.inspection.api.integration.JavaInspector.TEST_PROTOCOL;
import static org.e2immu.language.inspection.resource.SourceSetImpl.sourceSetOf;


public abstract class CommonTest {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CommonTest.class);

    protected JavaInspector javaInspector;
    protected PrepAnalyzer prepAnalyzer;
    protected Runtime runtime;

    @BeforeAll
    public static void beforeAll() {
        ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
        ((Logger) LoggerFactory.getLogger("org.e2immu.analyzer.modification.link"))
                .setLevel(Level.DEBUG);
    }

    @BeforeEach
    public void beforeEach() throws IOException, URISyntaxException {
        String impl = System.getProperty("maddi_parser", "maddi");
        LOGGER.info("Parsing with {}", impl);
        if ("maddi".equalsIgnoreCase(impl)) {
            maddiParser();
        } else if ("openJdk".equalsIgnoreCase(impl)) {
            openJdkParser();
        } else throw new UnsupportedEncodingException("Unknown parser " + impl);
        runtime = javaInspector.runtime();
    }

    protected void openJdkParser() throws URISyntaxException, IOException {
        SourceSet javaBase = SourceSetImpl.javaBase();
        SourceSet orgSlf4j = sourceSetOf(org.slf4j.Logger.class, javaBase);
        SourceSet annotations = sourceSetOf(NotNull.class, javaBase);
        SourceSet maddiSupport = sourceSetOf(SetOnce.class, javaBase);
        SourceSet junitJupiter = sourceSetOf(Assertions.class, javaBase);

        SourceSet sources = new SourceSetImpl.Builder().setName(TEST_PROTOCOL).setUri(URI.create("file:/"))
                .setDependencies(List.of(javaBase, orgSlf4j, annotations, maddiSupport, junitJupiter))
                .build();

        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSourceSets(sources)
                .addClassPath("jmod:java.base")
                .addClassPathParts(maddiSupport, orgSlf4j, annotations, junitJupiter)
                .build();
        javaInspector = new org.e2immu.language.inspection.openjdk.JavaInspectorImpl();
        javaInspector.initialize(inputConfiguration);
        javaInspector.preload("java.base::java.util");
        javaInspector.preload("java.base::java.util.concurrent.atomic");

    }

    private void maddiParser() throws IOException {
        javaInspector = new JavaInspectorImpl();
        InputConfigurationImpl.Builder builder = new InputConfigurationImpl.Builder()
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .addClassPath(JavaInspectorImpl.E2IMMU_SUPPORT)
                .addClassPath(ToolChain.CLASSPATH_JUNIT)
                .addClassPath(ToolChain.CLASSPATH_SLF4J_LOGBACK)
                .addSources("none");

        InputConfiguration inputConfiguration = builder.build();
        javaInspector.initialize(inputConfiguration);
        javaInspector.preload("java.util");
        javaInspector.parse(JavaInspectorImpl.FAIL_FAST);
        runtime = javaInspector.runtime();

        new LoadAnalysisResults(javaInspector.mainSources()).go(javaInspector,
                List.of(ToolChain.currentJdkAnalyzedPackages(), ToolChain.commonLibsAnalyzedPackages()));

        prepAnalyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
    }

    protected static <K, V> String nice(Map<K, V> map) {
        return map.entrySet().stream().map(Object::toString).sorted().collect(Collectors.joining(", "));
    }
}
