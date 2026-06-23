package org.e2immu.language.inspection.openjdk;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.e2immu.support.SetOnce;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.e2immu.language.inspection.resource.SourceSetImpl.sourceSetOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestJavaInspector4RealClasspath {

    private JavaInspector javaInspector;

    @BeforeEach
    public void test() throws IOException, URISyntaxException {
        javaInspector = new JavaInspectorImpl();

        SourceSet javaBase = SourceSetImpl.javaBase();
        SourceSet annotations = sourceSetOf(NotNull.class, javaBase);
        SourceSet maddiSupport = sourceSetOf(SetOnce.class, javaBase);

        Path cstApiPath = Path.of("../maddi-cst-api/src/main/java");
        assertTrue(Files.isDirectory(cstApiPath));
        SourceSet cstApi = new SourceSetImpl.Builder().setName("cst-api")
                .setSourceDirectories(List.of(cstApiPath))
                .setUri(URI.create("file:/")) // not important here
                .setDependencies(List.of(javaBase, annotations, maddiSupport))
                .build();
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSourceSets(cstApi)
                .addClassPath("jmod:java.base")
                .addClassPathParts(maddiSupport, annotations)
                .build();
        assertEquals(3, inputConfiguration.classPathParts().size());
        javaInspector.initialize(inputConfiguration);
    }

    @Test
    public void test1() {
        JavaInspector.ParseOptions options = new JavaInspector.ParseOptions.Builder()
                .setFailFast(true).setDetailedSources(true).setIgnoreModule(true).build();
        ParseResult parseResult = javaInspector.parse(Map.of(), options).parseResult();

        TypeInfo element = parseResult.findType("org.e2immu.language.cst.api.element.Element");
        assertTrue(element.isInterface());
    }
}
