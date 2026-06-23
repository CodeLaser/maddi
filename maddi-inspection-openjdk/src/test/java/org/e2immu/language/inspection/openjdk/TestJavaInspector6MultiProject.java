package org.e2immu.language.inspection.openjdk;

import org.e2immu.language.cst.api.element.ModuleInfo;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.jetbrains.annotations.NotNull;
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

        URI slf4jApiUri = Logger.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        SourceSet orgSlf4jApi = new SourceSetImpl.Builder().setName("slf4j-api-2.0.17.jar")
                .setUri(slf4jApiUri)
                .setExternalLibrary(true)
                .setModule(true)
                .build();

        Path maddiSupportJar = Path.of("../maddi-support/build/libs/maddi-support-0.8.2.jar").toRealPath();
        Path maddiSupportSrc = Path.of("../maddi-support/src/main/java");
        SourceSet maddiSupport = new SourceSetImpl.Builder()
                .setName("maddi-support-0.8.2.jar")
                .setSourceDirectories(List.of(maddiSupportSrc))
                .setUri(maddiSupportJar.toUri())
                .setLibrary(true)
                .setModule(true)
                .build();

        Path maddiUtilJar = Path.of("../maddi-util/build/libs/maddi-util.jar").toRealPath();
        Path maddUtilSrc = Path.of("../maddi-util/src/main/java");
        SourceSet maddiUtil = new SourceSetImpl.Builder()
                .setName("maddi-util.jar")
                .setSourceDirectories(List.of(maddUtilSrc))
                .setUri(maddiUtilJar.toUri())
                .setLibrary(true)
                .setModule(true)
                .setDependencies(List.of(maddiSupport, orgSlf4jApi))
                .build();


        URI annotationsUri = NotNull.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        SourceSet annotations = new SourceSetImpl.Builder().setName("annotations-26.1.0.jar")
                .setUri(annotationsUri)
                .setExternalLibrary(true)
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
                .setDependencies(List.of(maddiSupport, annotations))
                .build();


        URI junitJupiterApi = Test.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        SourceSet junitJupiter = new SourceSetImpl.Builder().setName("junit-jupiter-api-6.0.3.jar")
                .setUri(junitJupiterApi)
                .setExternalLibrary(true)
                .setModule(true)
                .build();

        Path cstAnalysisJar = Path.of("../maddi-cst-analysis/build/libs/maddi-cst-analysis.jar").toRealPath();
        Path cstAnalysisPath = Path.of("../maddi-cst-analysis/src/main/java");
        cstAnalysis = new SourceSetImpl.Builder()
                .setName("maddi-cst-analysis.jar")
                .setSourceDirectories(List.of(cstAnalysisPath))
                .setUri(cstAnalysisJar.toUri())
                .setModule(true)
                .setDependencies(List.of(cstApi, maddiSupport, orgSlf4jApi))
                .build();

        Path cstImplJar = Path.of("../maddi-cst-impl/build/libs/maddi-cst-impl.jar").toRealPath();
        Path cstImplPath = Path.of("../maddi-cst-impl/src/main/java");
        SourceSet cstImpl = new SourceSetImpl.Builder()
                .setName("maddi-cst-impl.jar")
                .setSourceDirectories(List.of(cstImplPath))
                .setUri(cstImplJar.toUri())
                .setModule(true)
                .setDependencies(List.of(cstApi, cstAnalysis, maddiSupport, maddiUtil, orgSlf4jApi, annotations))
                .build();

        Path cstImplTestPath = Path.of("../maddi-cst-impl/src/test/java");
        // cannot be referred to
        SourceSet cstImplTest = new SourceSetImpl.Builder()
                .setName("maddi-cst-impl test") // cannot be referred to
                .setSourceDirectories(List.of(cstImplTestPath))
                .setUri(cstImplTestPath.toUri())
                .setModule(false)
                .setDependencies(List.of(cstApi, cstAnalysis, cstImpl, maddiSupport, maddiUtil, orgSlf4jApi,
                        annotations, junitJupiter))
                .build();

        Path cstIoJar = Path.of("../maddi-cst-io/build/libs/maddi-cst-io.jar").toRealPath();
        Path cstIoPath = Path.of("../maddi-cst-io/src/main/java");
        SourceSet cstIo = new SourceSetImpl.Builder()
                .setName("maddi-cst-io.jar")
                .setSourceDirectories(List.of(cstIoPath))
                .setUri(cstImplJar.toUri())
                .setModule(true)
                .setDependencies(List.of(cstApi, cstAnalysis, maddiSupport, orgSlf4jApi, annotations))
                .build();

        assertTrue(Files.isReadable(maddiUtilJar));
        assertTrue(Files.isDirectory(maddUtilSrc));
        assertTrue(Files.isReadable(maddiSupportJar));
        assertTrue(Files.isDirectory(maddiSupportSrc));
        assertTrue(Files.isReadable(cstApiJar));
        assertTrue(Files.isDirectory(cstApiPath));
        assertTrue(Files.isReadable(cstAnalysisJar));
        assertTrue(Files.isDirectory(cstAnalysisPath));
        assertTrue(Files.isReadable(cstImplJar));
        assertTrue(Files.isDirectory(cstImplPath));
        assertTrue(Files.isDirectory(cstImplTestPath));
        assertTrue(Files.isReadable(cstIoJar));
        assertTrue(Files.isDirectory(cstIoPath));

        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSourceSets(cstApi, maddiSupport, cstAnalysis, maddiUtil, cstImpl, cstImplTest, cstIo)
                .addClassPath("jmod:java.base")
                .addClassPathParts(orgSlf4jApi, annotations, junitJupiter)
                .build();
        javaInspector.initialize(inputConfiguration);
    }

    @Test
    public void test1() {
        JavaInspector.ParseOptions options = new JavaInspector.ParseOptions.Builder()
                .setFailFast(true).setDetailedSources(true)
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

        TypeInfo typeInfoImpl = parseResult.findType("org.e2immu.language.cst.impl.info.TypeInfoImpl");
        assertTrue(typeInfoImpl.typeNature().isClass());
    }
}
