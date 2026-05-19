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
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestJavaInspector4RealClasspath {

    private JavaInspector javaInspector;
    private Runtime runtime;

    @BeforeEach
    public void test() throws IOException, URISyntaxException {
        javaInspector = new JavaInspectorImpl();

        Path maddiSupportJar = Path.of("../maddi-support/build/libs/maddi-support-0.8.2.jar").toRealPath();
        assertTrue(Files.isReadable(maddiSupportJar));
        SourceSet maddiSupport = new SourceSetImpl("maddi-support-0.8.2.jar", List.of(), maddiSupportJar.toUri(),
                StandardCharsets.UTF_8, false, true, true, false, false,
                Set.of(), Set.of());

        Path cstApiPath = Path.of("../maddi-cst-api/src/main/java");
        assertTrue(Files.isDirectory(cstApiPath));
        SourceSet cstApi = new SourceSetImpl("cst-api", List.of(), cstApiPath.toUri(),
                StandardCharsets.UTF_8, false, false, false, false, false,
                Set.of(), Set.of());
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSourceSets(cstApi)
                .addClassPath("jmod:java.base")
                .addClassPathParts(maddiSupport)
                .build();
        assertEquals(2, inputConfiguration.classPathParts().size());
        javaInspector.initialize(inputConfiguration);
        runtime = javaInspector.runtime();
    }

    @Test
    public void test1() {
        JavaInspector.ParseOptions options = new JavaInspectorImpl.ParseOptionsBuilder()
                .setFailFast(true).setDetailedSources(true).build();
        ParseResult parseResult = javaInspector.parse(Map.of(), options).parseResult();

        TypeInfo element = parseResult.findType("org.e2immu.language.cst.api.element.Element");
        assertTrue(element.isInterface());
    }
}
