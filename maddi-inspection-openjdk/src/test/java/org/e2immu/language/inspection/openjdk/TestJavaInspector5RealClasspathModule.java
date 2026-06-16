package org.e2immu.language.inspection.openjdk;

import org.e2immu.language.cst.api.element.ModuleInfo;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestJavaInspector5RealClasspathModule {

    private JavaInspector javaInspector;
    private SourceSet cstApi;

    @BeforeEach
    public void test() throws IOException {
        javaInspector = new JavaInspectorImpl();

        Path maddiSupportJar = Path.of("../maddi-support/build/libs/maddi-support-0.8.2.jar").toRealPath();
        assertTrue(Files.isReadable(maddiSupportJar));
        SourceSet maddiSupport = new SourceSetImpl.Builder().setName("maddi-support-0.8.2.jar")
                .setUri(maddiSupportJar.toUri()).setExternalLibrary(true).setLibrary(true)
                .setModule(true)
                .build();

        Path cstApiPath = Path.of("../maddi-cst-api/src/main/java");
        assertTrue(Files.isDirectory(cstApiPath));
        cstApi = new SourceSetImpl.Builder().setName("cst-api")
                .setSourceDirectories(List.of(cstApiPath))
                .setUri(URI.create("file:/")) // not important here
                .setModule(true)
                .setDependencies(List.of(maddiSupport))
                .build();
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSourceSets(cstApi)
                .addClassPath("jmod:java.base")
                .addClassPathParts(maddiSupport)
                .build();
        assertEquals(2, inputConfiguration.classPathParts().size());
        javaInspector.initialize(inputConfiguration);
    }

    @Test
    public void test1() {
        JavaInspector.ParseOptions options = new JavaInspectorImpl.ParseOptionsBuilder()
                .setFailFast(true).setDetailedSources(true).build(); // not ignoring module here!
        ParseResult parseResult = javaInspector.parse(Map.of(), options).parseResult();

        ModuleInfo moduleInfo = parseResult.moduleInfo(cstApi);
        assertEquals(13, moduleInfo.exports().size());

        TypeInfo element = parseResult.findType("org.e2immu.language.cst.api.element.Element");
        assertTrue(element.isInterface());
    }
}
