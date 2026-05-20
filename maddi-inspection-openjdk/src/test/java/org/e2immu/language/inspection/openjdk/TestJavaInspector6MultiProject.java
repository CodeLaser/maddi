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
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestJavaInspector6MultiProject {

    private JavaInspector javaInspector;
    private SourceSet cstApi;
    private SourceSet cstAnalysis;

    @BeforeEach
    public void test() throws IOException, URISyntaxException {
        javaInspector = new JavaInspectorImpl();

        Path maddiSupportJar = Path.of("../maddi-support/build/libs/maddi-support-0.8.2.jar").toRealPath();
        Path maddiSupportSrc = Path.of("../maddi-support/src/main/java");
        SourceSet maddiSupport = new SourceSetImpl.Builder()
                .setName("maddi-support-0.8.2.jar")
                .setSourceDirectories(List.of(maddiSupportSrc))
                .setUri(maddiSupportJar.toUri())
                .setLibrary(true)
                .setModule(true)
                .build();

        Path cstApiJar = Path.of("../maddi-cst-api/build/libs/maddi-cst-api.jar").toRealPath();
        Path cstApiPath = Path.of("../maddi-cst-api/src/main/java");
        cstApi = new SourceSetImpl.Builder()
                .setName("maddi-cst-api.jar")
                .setSourceDirectories(List.of(cstApiPath))
                .setUri(cstApiJar.toUri())
                .setLibrary(true)
                .setModule(true)
                .setDependencies(Set.of(maddiSupport))
                .build();

        URI slf4jApiUri = Logger.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        SourceSet orgSlf4jApi = new SourceSetImpl.Builder().setName("slf4j-api-2.0.17.jar")
                .setUri(slf4jApiUri)
                .setExternalLibrary(true)
                .setModule(true).build();

        Path cstAnalysisJar = Path.of("../maddi-cst-analysis/build/libs/maddi-cst-analysis.jar").toRealPath();
        Path cstAnalysisPath = Path.of("../maddi-cst-analysis/src/main/java");
        cstAnalysis = new SourceSetImpl.Builder()
                .setName("maddi-cst-analysis.jar")
                .setSourceDirectories(List.of(cstAnalysisPath))
                .setUri(cstAnalysisJar.toUri())
                .setModule(true)
                .setDependencies(Set.of(cstApi, maddiSupport, orgSlf4jApi))
                .build();

        assertTrue(Files.isDirectory(maddiSupportSrc));
        assertTrue(Files.isReadable(cstApiJar));
        assertTrue(Files.isDirectory(cstApiPath));
        assertTrue(Files.isReadable(maddiSupportJar));
        assertTrue(Files.isReadable(cstAnalysisJar));
        assertTrue(Files.isDirectory(cstAnalysisPath));

        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSourceSets(cstApi, maddiSupport, cstAnalysis)
                .addClassPath("jmod:java.base")
                .addClassPathParts(orgSlf4jApi)
                .build();
        assertEquals(2, inputConfiguration.classPathParts().size());
        javaInspector.initialize(inputConfiguration);
    }

    @Test
    public void test1() {
        JavaInspector.ParseOptions options = new JavaInspectorImpl.ParseOptionsBuilder()
                .setFailFast(true).setDetailedSources(true)
                .setIgnoreModule(false)
                .build(); // not ignoring module here!
        ParseResult parseResult = javaInspector.parse(Map.of(), options).parseResult();

        ModuleInfo moduleInfoApi = parseResult.moduleInfo(cstApi);
        assertEquals(13, moduleInfoApi.exports().size());

        TypeInfo element = parseResult.findType("org.e2immu.language.cst.api.element.Element");
        assertTrue(element.isInterface());

        ModuleInfo moduleInfoAnalysis = parseResult.moduleInfo(cstAnalysis);
        assertEquals(1, moduleInfoAnalysis.exports().size());
        assertEquals(3, moduleInfoAnalysis.requires().size());
        assertEquals(moduleInfoApi.name(), moduleInfoAnalysis.requires().get(1).name());

        TypeInfo valueImpl = parseResult.findType("org.e2immu.language.cst.impl.analysis.ValueImpl");
        assertTrue(valueImpl.isAbstract());
    }
}
