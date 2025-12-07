package org.e2immu.analyzer.modification.link;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.e2immu.analyzer.modification.io.LoadAnalyzedPackageFiles;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.ToolChain;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;


public abstract class CommonTest {
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
    public void beforeEach() throws IOException {
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

        new LoadAnalyzedPackageFiles(javaInspector.mainSources()).go(javaInspector,
                List.of(ToolChain.currentJdkAnalyzedPackages(), ToolChain.commonLibsAnalyzedPackages()));

        prepAnalyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
    }
}
