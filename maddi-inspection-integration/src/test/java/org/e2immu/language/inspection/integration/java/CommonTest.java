package org.e2immu.language.inspection.integration.java;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.e2immu.language.inspection.integration.JavaInspectorImpl.JAR_WITH_PATH_PREFIX;


public abstract class CommonTest {
    protected JavaInspector javaInspector;
    protected final boolean allowCreationOfStubTypes;

    protected CommonTest() {
        this(false);
    }

    protected CommonTest(boolean allowCreationOfStubTypes) {
        this.allowCreationOfStubTypes = allowCreationOfStubTypes;
    }

    @BeforeAll
    public static void beforeAll() {
        ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
    }

    @BeforeEach
    public void beforeEach() throws IOException {
        javaInspector = new JavaInspectorImpl(false, allowCreationOfStubTypes);
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSources(InputConfigurationImpl.MAVEN_TEST)
                .addRestrictSourceToPackages("org.e2immu.language.inspection.integration.java.importhelper.")
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .addClassPath("../maddi-support/build/classes/java/main")
                // NOTE: no access to ToolChain here; this is rather exceptional
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/junit/jupiter/api")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/apiguardian/api")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/junit/platform/commons")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/slf4j/event")
                .addClassPath(JAR_WITH_PATH_PREFIX + "ch/qos/logback/core")
                .addClassPath(JAR_WITH_PATH_PREFIX + "ch/qos/logback/classic")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/opentest4j")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/assertj/core")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/springframework/core")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/springframework/test")
                .addClassPath(JAR_WITH_PATH_PREFIX + "lombok")
                .build();
        javaInspector.initialize(inputConfiguration);
        javaInspector.parse(new JavaInspectorImpl.ParseOptionsBuilder()
                .setFailFast(true)
                .setLombok(false)
                .setDetailedSources(true).build());
        javaInspector.javaBase().computePriorityDependencies();
    }
}
