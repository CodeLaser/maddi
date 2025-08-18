package org.e2immu.analyzer.modification.prepwork;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.e2immu.language.inspection.integration.JavaInspectorImpl.JAR_WITH_PATH_PREFIX;

public class CommonTest {
    protected JavaInspector javaInspector;
    protected Runtime runtime;
    protected final String[] extraClassPath;

    protected CommonTest() {
        this(new String[]{});
    }

    protected CommonTest(String... extraClassPath) {
        this.extraClassPath = extraClassPath;
    }

    @BeforeAll
    public static void beforeAll() {
        ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
        ((Logger) LoggerFactory.getLogger(MethodAnalyzer.class)).setLevel(Level.DEBUG);
        ((Logger) LoggerFactory.getLogger(PrepAnalyzer.class)).setLevel(Level.DEBUG);
    }

    @BeforeEach
    public void beforeEach() throws IOException {
        javaInspector = new JavaInspectorImpl();
        InputConfigurationImpl.Builder builder = new InputConfigurationImpl.Builder()
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .addClassPath(JavaInspectorImpl.E2IMMU_SUPPORT)
                // NOTE: no access to ToolChain
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/junit/jupiter/api")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/apiguardian/api")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/junit/platform/commons")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/slf4j/event")
                .addClassPath(JAR_WITH_PATH_PREFIX + "ch/qos/logback/core")
                .addClassPath(JAR_WITH_PATH_PREFIX + "ch/qos/logback/classic")
                .addClassPath(JAR_WITH_PATH_PREFIX + "io/codelaser/jfocus/transform/support")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/opentest4j");
        for (String extra : extraClassPath) {
            builder.addClassPath(extra);
        }
        InputConfiguration inputConfiguration = builder.build();
        javaInspector.initialize(inputConfiguration);
        javaInspector.preload("java.util");
        javaInspector.parse(JavaInspectorImpl.FAIL_FAST);
        runtime = javaInspector.runtime();
    }
}
