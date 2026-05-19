package org.e2immu.language.inspection.openjdk;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.e2immu.language.inspection.openjdk.JavaInspectorImpl.JAR_WITH_PATH_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestJavaInspector3 {

    private JavaInspector javaInspector;
    private Runtime runtime;

    @BeforeEach
    public void test() throws IOException {
        javaInspector = new JavaInspectorImpl();

        Path commonsCliJar = Path.of("testLib/commons-cli-1.11.0.jar");
        assertTrue(Files.isReadable(commonsCliJar));
        SourceSet commonsCli = new SourceSetImpl("commons-cli-1.11.0.jar", List.of(), commonsCliJar.toUri(),
                StandardCharsets.UTF_8, false, true, true, false, false,
                Set.of(), Set.of());

        Path maddiSupportClasses = Path.of("../maddi-support/build/classes/java/main/");
        assertTrue(Files.isDirectory(maddiSupportClasses));
        SourceSet maddiSupport = new SourceSetImpl("maddi-support", List.of(), maddiSupportClasses.toUri(),
                StandardCharsets.UTF_8, false, true, true, false, false,
                Set.of(), Set.of());

        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSourceSets(JavaInspectorImpl.TEST_PROTOCOL_SOURCE_SET)
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .addClassPath(JAR_WITH_PATH_PREFIX + "slf4j-api-2.0.17.jar")
                .addClassPathParts(commonsCli, maddiSupport)
                .build();
        assertEquals(10, inputConfiguration.classPathParts().size());
        javaInspector.initialize(inputConfiguration);
        runtime = javaInspector.runtime();
    }

    @Language("java")
    public static final String INPUT1 = """
            package a.b;
            import org.apache.commons.cli.Option;
            import org.e2immu.annotation.ImmutableContainer;
            import org.slf4j.Logger;
            import org.slf4j.LoggerFactory;
            
            @ImmutableContainer
            record C(int k) {
                private static final Logger LOGGER = LoggerFactory.getLogger(C.class);
            
                int kSquared() {
                    LOGGER.info("Returning k*k = {}", k*k);
                    return k*k;
                }
            
                Option makeOption() {
                    return new Option("-v", "return "+k);
                }
            }
            """;

    @Test
    public void test1() {
        JavaInspector.ParseOptions options = new JavaInspectorImpl.ParseOptionsBuilder()
                .setFailFast(true).setDetailedSources(true).build();
        ParseResult parseResult = javaInspector.parse(Map.of("a.b.C", INPUT1), options).parseResult();
        TypeInfo C = parseResult.findType("a.b.C");
        assertEquals("@ImmutableContainer", C.annotations().getFirst().toString());
        TypeInfo immutableContainer = runtime.getFullyQualified("org.e2immu.annotation.ImmutableContainer",
                true);
        assertTrue(immutableContainer.typeNature().isAnnotation());
    }
}
