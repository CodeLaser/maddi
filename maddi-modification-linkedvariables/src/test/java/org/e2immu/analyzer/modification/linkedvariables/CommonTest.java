package org.e2immu.analyzer.modification.linkedvariables;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.e2immu.analyzer.modification.io.LoadAnalyzedPackageFiles;
import org.e2immu.analyzer.modification.linkedvariables.impl.CommonAnalyzerImpl;
import org.e2immu.analyzer.modification.linkedvariables.impl.IteratingAnalyzerImpl;
import org.e2immu.analyzer.modification.linkedvariables.impl.ModAnalyzerForTesting;
import org.e2immu.analyzer.modification.linkedvariables.impl.SingleIterationAnalyzerImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
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

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CommonTest {
    protected JavaInspector javaInspector;
    protected Runtime runtime;
    protected final String[] extraClassPath;
    protected ModAnalyzerForTesting analyzer;
    protected PrepAnalyzer prepAnalyzer;
    protected final boolean storeErrors;

    protected CommonTest(String... extraClassPath) {
        this(false, extraClassPath);
    }

    protected CommonTest(boolean storeErrors, String... extraClassPath) {
        this.extraClassPath = extraClassPath;
        this.storeErrors = storeErrors;
    }

    @BeforeAll
    public static void beforeAll() {
        ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
        ((Logger) LoggerFactory.getLogger("graph-algorithm")).setLevel(Level.DEBUG);
        ((Logger) CommonAnalyzerImpl.DECIDE).setLevel(Level.DEBUG);
        ((Logger) CommonAnalyzerImpl.UNDECIDED).setLevel(Level.DEBUG);
    }

    @BeforeEach
    public void beforeEach() throws IOException {
        javaInspector = new JavaInspectorImpl();
        InputConfigurationImpl.Builder builder = new InputConfigurationImpl.Builder()
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .addClassPath(JavaInspectorImpl.E2IMMU_SUPPORT)
                .addClassPath(ToolChain.CLASSPATH_JUNIT)
                .addClassPath(ToolChain.CLASSPATH_SLF4J_LOGBACK)
                .addSources("src/main/java");
        for (String extra : extraClassPath) {
            builder.addClassPath(extra);
        }
        InputConfiguration inputConfiguration = builder.build();
        javaInspector.initialize(inputConfiguration);
        javaInspector.preload("java.util", inputConfiguration.javaBase());

        new LoadAnalyzedPackageFiles(javaInspector.mainSources()).go(javaInspector,
                List.of(ToolChain.currentJdkAnalyzedPackages(), ToolChain.commonLibsAnalyzedPackages()));

        javaInspector.parse(JavaInspectorImpl.FAIL_FAST);
        runtime = javaInspector.runtime();
        prepAnalyzer = new PrepAnalyzer(runtime);

        IteratingAnalyzer.Configuration configuration = new IteratingAnalyzerImpl.ConfigurationBuilder()
                .setStoreErrors(storeErrors)
                .build();
        analyzer = new SingleIterationAnalyzerImpl(runtime, configuration);
    }

    protected List<Info> prepWork(TypeInfo typeInfo) {
        List<TypeInfo> typesLoaded = javaInspector.compiledTypesManager().typesLoaded(true);
        assertTrue(typesLoaded.stream().anyMatch(ti -> "java.util.ArrayList".equals(ti.fullyQualifiedName())));
        prepAnalyzer.initialize(typesLoaded);

        return prepAnalyzer.doPrimaryType(typeInfo);
    }
}
