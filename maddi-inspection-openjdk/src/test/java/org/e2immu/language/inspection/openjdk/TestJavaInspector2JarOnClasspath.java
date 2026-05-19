package org.e2immu.language.inspection.openjdk;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.e2immu.language.inspection.openjdk.JavaInspectorImpl.JAR_WITH_PATH_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestJavaInspector2JarOnClasspath {

    private JavaInspector javaInspector;
    private Runtime runtime;

    @BeforeEach
    public void test() throws IOException {
        javaInspector = new JavaInspectorImpl();
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSourceSets(JavaInspectorImpl.TEST_PROTOCOL_SOURCE_SET)
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .addClassPath(JAR_WITH_PATH_PREFIX + "maddi-support-0.8.2.jar")
                .addClassPath(JAR_WITH_PATH_PREFIX + "slf4j-api-2.0.17.jar")
                .build();
        javaInspector.initialize(inputConfiguration);
        runtime = javaInspector.runtime();
    }

    @Language("java")
    public static final String INPUT1 = """
            package a.b;
            import org.e2immu.annotation.ImmutableContainer;
            import org.slf4j.Logger;
            import org.slf4j.LoggerFactory;
            import org.apache.commons.cli.Util;
            
            @ImmutableContainer
            record C(int k) {
                private static final Logger LOGGER = LoggerFactory.getLogger(C.class);
            
                int kSquared() {
                    LOGGER.info("Returning k*k = {}", k*k);
                    return k*k;
                }
            }
            """;

    @Test
    public void test1() {
        ParseResult parseResult = javaInspector.parse(Map.of("a.b.C", INPUT1),
                JavaInspectorImpl.DETAILED_SOURCES).parseResult();
        TypeInfo C = parseResult.findType("a.b.C");
        assertEquals("@ImmutableContainer", C.annotations().getFirst().toString());
        TypeInfo immutableContainer = runtime.getFullyQualified("org.e2immu.annotation.ImmutableContainer",
                true);
        assertTrue(immutableContainer.typeNature().isAnnotation());
    }
}
