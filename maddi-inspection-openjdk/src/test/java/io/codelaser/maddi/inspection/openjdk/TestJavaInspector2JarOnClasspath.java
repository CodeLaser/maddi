package org.e2immu.language.inspection.openjdk;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.api.parser.Summary;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Map;

import static org.e2immu.language.inspection.api.integration.JavaInspector.TEST_PROTOCOL;
import static org.e2immu.language.inspection.openjdk.JavaInspectorImpl.JAR_WITH_PATH_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestJavaInspector2JarOnClasspath {

    private JavaInspector javaInspector;
    private Runtime runtime;

    @BeforeEach
    public void test() throws IOException {
        javaInspector = new JavaInspectorImpl();
        SourceSet sourceSet = new SourceSetImpl.Builder()
                .setName(TEST_PROTOCOL).setUri(URI.create("file:/")).build();

        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSourceSets(sourceSet)
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                // The openjdk front end matches `jar-on-classpath:` names against real jar FILE names
                // (ClassSymbolScanner#sourceSetMap), so this needs the file name -- but not a hard-coded
                // version: "maddi-support-0.8.2.jar" silently stopped matching when the release train
                // moved to 0.9.0, and the unresolved annotations looked like a parser bug.
                .addClassPath(JAR_WITH_PATH_PREFIX + maddiSupportJarName())
                .addClassPath(JAR_WITH_PATH_PREFIX + "slf4j-api-2.0.17.jar")
                .build();
        javaInspector.initialize(inputConfiguration);
        runtime = javaInspector.runtime();
    }

    /** The maddi-support jar as it appears on this test's runtime classpath, version and all. */
    private static String maddiSupportJarName() {
        for (String entry : System.getProperty("java.class.path").split(File.pathSeparator)) {
            String name = Path.of(entry).getFileName().toString();
            if (name.startsWith("maddi-support-") && name.endsWith(".jar")) return name;
        }
        throw new IllegalStateException("maddi-support jar not on the test classpath");
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
        Summary summary = javaInspector.parse(Map.of("a.b.C", INPUT1), JavaInspectorImpl.DETAILED_SOURCES);
        // the unresolved 'org.apache.commons.cli' import (not on the partial classpath) is surfaced as a
        // non-fatal warning, not a parse error, so parsing still yields a ParseResult
        assertFalse(summary.haveErrors());
        assertFalse(summary.parseWarnings().isEmpty(), "expected a warning for the unresolved commons-cli import");
        assertTrue(summary.parseWarnings().getFirst().level().isWarning(), "diagnostic must carry WARN severity");
        ParseResult parseResult = summary.parseResult();
        TypeInfo C = parseResult.findType("a.b.C");
        assertEquals("@ImmutableContainer", C.annotations().getFirst().toString());
        TypeInfo immutableContainer = runtime.getFullyQualified("org.e2immu.annotation.ImmutableContainer",
                true);
        assertTrue(immutableContainer.typeNature().isAnnotation());
    }
}
