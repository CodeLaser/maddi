package org.e2immu.language.inspection.integration.java;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

import static org.e2immu.language.inspection.integration.JavaInspectorImpl.JAR_WITH_PATH_PREFIX;
import static org.e2immu.language.inspection.integration.JavaInspectorImpl.TEST_PROTOCOL_PREFIX;


public abstract class CommonTest2 {
    protected JavaInspector javaInspector;

    @BeforeAll
    public static void beforeAll() {
        ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
    }

    public Map<String, String> sourcesByURIString(Map<String, String> sourcesByFqn) {
        return sourcesByFqn.entrySet().stream().collect(Collectors.toUnmodifiableMap(
                e -> TEST_PROTOCOL_PREFIX + e.getKey(), Map.Entry::getValue));
    }

    public ParseResult init(Map<String, String> sourcesByFqn) throws IOException {
        Map<String, String> sourcesByURIString = sourcesByURIString(sourcesByFqn);
        InputConfiguration inputConfiguration = makeInputConfiguration(sourcesByURIString);
        javaInspector = new JavaInspectorImpl(true, false);
        javaInspector.initialize(inputConfiguration);
        return javaInspector.parse(sourcesByURIString,
                        new JavaInspectorImpl.ParseOptionsBuilder().setFailFast(true).setDetailedSources(true).build())
                .parseResult();
    }

    public static InputConfiguration makeInputConfiguration(Map<String, String> sourcesByURIString) {
        InputConfiguration.Builder inputConfigurationBuilder = new InputConfigurationImpl.Builder()
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                // NOTE: no access to ToolChain here; this is rather exceptional
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/junit/jupiter/api")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/apiguardian/api")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/junit/platform/commons")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/slf4j/event")
                .addClassPath(JAR_WITH_PATH_PREFIX + "ch/qos/logback/core")
                .addClassPath(JAR_WITH_PATH_PREFIX + "ch/qos/logback/classic")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/opentest4j");
        sourcesByURIString.keySet().forEach(inputConfigurationBuilder::addSources);
        return inputConfigurationBuilder.build();
    }
}
