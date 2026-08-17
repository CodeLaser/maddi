package io.codelaser.maddi.inspection.openjdk;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import io.codelaser.maddi.support.SetOnce;
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

import static io.codelaser.maddi.inspection.resource.SourceSetImpl.sourceSetOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestJavaInspector4RealClasspath {

    private JavaInspector javaInspector;

    @BeforeEach
    public void test() throws IOException, URISyntaxException {
        javaInspector = new JavaInspectorImpl();

        SourceSet javaBase = SourceSetImpl.javaBase();
        SourceSet annotations = sourceSetOf(NotNull.class, javaBase);
        SourceSet maddiAnnotation = sourceSetOf(io.codelaser.maddi.annotation.Immutable.class, javaBase);
        SourceSet maddiSupport = sourceSetOf(SetOnce.class, javaBase, maddiAnnotation);

        Path cstApiPath = Path.of("../maddi-cst-api/src/main/java");
        assertTrue(Files.isDirectory(cstApiPath));
        SourceSet cstApi = new SourceSetImpl.Builder().setName("cst-api")
                .setSourceDirectories(List.of(cstApiPath))
                .setUri(URI.create("file:/")) // not important here
                // maddiAnnotation named beside maddiSupport, not reached through it: the parser's
                // source-set dependencies are direct, not transitive, so `requires transitive
                // io.codelaser.maddi.annotation` does not carry the annotations to cst-api here.
                .setDependencies(List.of(javaBase, annotations, maddiAnnotation, maddiSupport))
                .build();
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSourceSets(cstApi)
                .addClassPath("jmod:java.base")
                .addClassPathParts(maddiAnnotation, maddiSupport, annotations)
                .build();
        assertEquals(4, inputConfiguration.classPathParts().size());
        javaInspector.initialize(inputConfiguration);
    }

    @Test
    public void test1() {
        JavaInspector.ParseOptions options = new JavaInspector.ParseOptions.Builder()
                .setFailFast(true).setDetailedSources(true).setIgnoreModule(true).build();
        ParseResult parseResult = javaInspector.parse(Map.of(), options).parseResult();

        TypeInfo element = parseResult.findType("io.codelaser.maddi.cst.api.element.Element");
        assertTrue(element.isInterface());
    }
}
