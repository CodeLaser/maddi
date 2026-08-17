package org.e2immu.language.inspection.openjdk;

import org.e2immu.language.cst.api.element.ModuleInfo;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class TestJavaInspector6MultiProject {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestJavaInspector6MultiProject.class);

    private JavaInspector javaInspector;
    private SourceSet maddiSupport;
    private SourceSet maddiUtil;
    private SourceSet cstApi;
    private SourceSet cstAnalysis;
    private SourceSet cstImpl;
    private SourceSet cstImplTest;
    private SourceSet cstIo;

    /**
     * Where one of maddi's own modules lives, as the test JVM sees it: Gradle puts each module's artifact on this
     * test's runtime class path and rebuilds it as a dependency of the test task, so it is always the current one.
     * <p>
     * The alternative — hard-coding {@code build/libs/<module>-<version>.jar} — rots on a version bump, and worse:
     * it kept resolving to unversioned left-overs from before {@code version=0.8.2}, which no task produces any more.
     * This test compiles maddi's own sources against these artifacts, so a stale one means the sources no longer
     * match the API they are compiled against the moment anything changes, and the failure points at maddi's source
     * rather than at the stale jar. Mirrors what this test already does for slf4j, annotations and junit.
     */
    private static URI artifactOf(Class<?> classInThatModule) {
        try {
            return classInThatModule.getProtectionDomain().getCodeSource().getLocation().toURI();
        } catch (URISyntaxException e) {
            throw new AssertionError("Cannot locate the artifact of " + classInThatModule, e);
        }
    }

    @BeforeEach
    public void test() throws IOException, URISyntaxException {
        javaInspector = new JavaInspectorImpl();

        URI slf4jApiUri = Logger.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        SourceSet orgSlf4jApi = new SourceSetImpl.Builder().setName("slf4j-api-2.0.17.jar")
                .setUri(slf4jApiUri)
                .setExternalLibrary(true)
                .setModule(true)
                .build();

        Path maddiSupportSrc = Path.of("../maddi-support/src/main/java");
        maddiSupport = new SourceSetImpl.Builder()
                .setName("maddi-support")
                .setSourceDirectories(List.of(maddiSupportSrc))
                .setUri(artifactOf(org.e2immu.annotation.Container.class))
                .setLibrary(true)
                .setModule(true)
                .build();

        Path maddUtilSrc = Path.of("../maddi-util/src/main/java");
        maddiUtil = new SourceSetImpl.Builder()
                .setName("maddi-util")
                .setSourceDirectories(List.of(maddUtilSrc))
                .setUri(artifactOf(org.e2immu.util.internal.util.GetSetNames.class))
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

        Path cstApiPath = Path.of("../maddi-cst-api/src/main/java");
        cstApi = new SourceSetImpl.Builder()
                .setName("maddi-cst-api")
                .setSourceDirectories(List.of(cstApiPath))
                .setUri(artifactOf(org.e2immu.language.cst.api.element.Element.class))
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

        Path cstAnalysisPath = Path.of("../maddi-cst-analysis/src/main/java");
        cstAnalysis = new SourceSetImpl.Builder()
                .setName("maddi-cst-analysis")
                .setSourceDirectories(List.of(cstAnalysisPath))
                .setUri(artifactOf(org.e2immu.language.cst.impl.analysis.ValueImpl.class))
                .setModule(true)
                .setDependencies(List.of(cstApi, maddiSupport, orgSlf4jApi))
                .build();

        Path cstImplPath = Path.of("../maddi-cst-impl/src/main/java");
        cstImpl = new SourceSetImpl.Builder()
                .setName("maddi-cst-impl")
                .setSourceDirectories(List.of(cstImplPath))
                .setUri(artifactOf(org.e2immu.language.cst.impl.info.TypeInfoImpl.class))
                .setModule(true)
                .setDependencies(List.of(cstApi, cstAnalysis, maddiSupport, maddiUtil, orgSlf4jApi, annotations))
                .build();

        Path cstImplTestPath = Path.of("../maddi-cst-impl/src/test/java");
        // cannot be referred to
        cstImplTest = new SourceSetImpl.Builder()
                .setName("maddi-cst-impl test") // cannot be referred to
                .setSourceDirectories(List.of(cstImplTestPath))
                .setUri(cstImplTestPath.toUri())
                .setModule(false)
                .setDependencies(List.of(cstApi, cstAnalysis, cstImpl, maddiSupport, maddiUtil, orgSlf4jApi,
                        annotations, junitJupiter))
                .build();

        Path cstIoPath = Path.of("../maddi-cst-io/src/main/java");
        cstIo = new SourceSetImpl.Builder()
                .setName("maddi-cst-io")
                .setSourceDirectories(List.of(cstIoPath))
                .setUri(artifactOf(org.e2immu.language.cst.io.CodecImpl.class))
                .setModule(true)
                .setDependencies(List.of(cstApi, cstAnalysis, maddiSupport, orgSlf4jApi, annotations))
                .build();

        // only the source directories need checking; the artifacts come from this test's own class path
        assertTrue(Files.isDirectory(maddUtilSrc));
        assertTrue(Files.isDirectory(maddiSupportSrc));
        assertTrue(Files.isDirectory(cstApiPath));
        assertTrue(Files.isDirectory(cstAnalysisPath));
        assertTrue(Files.isDirectory(cstImplPath));
        assertTrue(Files.isDirectory(cstImplTestPath));
        assertTrue(Files.isDirectory(cstIoPath));

        // this is the short way of writing
        SourceSet openTest = SourceSetImpl.sourceSetOf(AssertionFailedError.class);

        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSourceSets(cstApi, maddiSupport, cstAnalysis, maddiUtil, cstImpl, cstImplTest, cstIo)
                .addClassPath("jmod:java.base")
                .addClassPathParts(orgSlf4jApi, annotations, junitJupiter, openTest)
                .build();
        javaInspector.initialize(inputConfiguration);
    }

    private static final String ELEMENT = "org.e2immu.language.cst.api.element.Element";
    private static final String TYPE_INFO = "org.e2immu.language.cst.api.info.TypeInfo";
    private static final String TYPE_INFO_IMPL = "org.e2immu.language.cst.impl.info.TypeInfoImpl";
    private static final String CONTAINER = "org.e2immu.annotation.Container";

    /**
     * Rewiring per source set, on a real module graph rather than a two-type toy: everything in maddi-cst-api is
     * declared changed, so
     * <ul>
     *     <li>cst-api is re-scanned: new objects, new compilation units;</li>
     *     <li>the four source sets that depend on it are rewired, not re-scanned: new objects on the <em>same</em>
     *     compilation units, reaching the re-parsed cst-api;</li>
     *     <li>maddi-support and maddi-util depend on none of it, and keep the very objects they had.</li>
     * </ul>
     */
    @Test
    public void testRewirePerSourceSet() {
        JavaInspector.ParseOptions options = new JavaInspector.ParseOptions.Builder()
                .setFailFast(true).setDetailedSources(true).build();
        ParseResult pr1 = javaInspector.parse(Map.of(), options).parseResult();

        TypeInfo element1 = pr1.findType(ELEMENT);                 // cst-api      -> INVALID
        TypeInfo typeInfoApi1 = pr1.findType(TYPE_INFO);           // cst-api      -> INVALID
        TypeInfo typeInfoImpl1 = pr1.findType(TYPE_INFO_IMPL);     // cst-impl     -> REWIRE
        TypeInfo container1 = pr1.findType(CONTAINER);             // maddi-support-> UNCHANGED
        assertNotNull(element1);
        assertNotNull(typeInfoApi1);
        assertNotNull(typeInfoImpl1);
        assertNotNull(container1);

        Set<SourceSet> dependOnCstApi = Set.of(cstAnalysis, cstImpl, cstImplTest, cstIo);
        JavaInspector.ParseOptions reparseOptions = new JavaInspector.ParseOptions.Builder()
                .setFailFast(true).setDetailedSources(true)
                .setInvalidated(ti -> {
                    SourceSet sourceSet = ti.compilationUnit().sourceSet();
                    if (cstApi.equals(sourceSet)) return JavaInspector.InvalidationState.INVALID;
                    return dependOnCstApi.contains(sourceSet)
                            ? JavaInspector.InvalidationState.REWIRE
                            : JavaInspector.InvalidationState.UNCHANGED;
                })
                .build();
        ParseResult pr2 = javaInspector.parse(Map.of(), reparseOptions).parseResult();

        // untouched source sets: the very same objects
        assertSame(container1, pr2.findType(CONTAINER), "maddi-support does not depend on cst-api: keep it");

        // re-scanned: new objects on new compilation units
        TypeInfo element2 = pr2.findType(ELEMENT);
        assertNotSame(element1, element2, "cst-api changed: it must be re-scanned");
        assertNotSame(element1.compilationUnit(), element2.compilationUnit());

        // rewired: a new object, but NOT re-parsed -- rewirePhase0 reuses the compilation unit
        TypeInfo typeInfoImpl2 = pr2.findType(TYPE_INFO_IMPL);
        assertNotSame(typeInfoImpl1, typeInfoImpl2, "cst-impl depends on cst-api: it must be rewired");
        assertSame(typeInfoImpl1.compilationUnit(), typeInfoImpl2.compilationUnit(),
                "rewired, not re-scanned: same compilation unit");

        // and the point of it all: the rewired type reaches the RE-PARSED cst-api, not the objects it replaced
        TypeInfo typeInfoApi2 = pr2.findType(TYPE_INFO);
        assertNotSame(typeInfoApi1, typeInfoApi2);
        TypeInfo implemented = typeInfoImpl2.interfacesImplemented().stream()
                .map(ParameterizedType::typeInfo)
                .filter(ti -> TYPE_INFO.equals(ti.fullyQualifiedName()))
                .findFirst().orElseThrow(() -> new AssertionError("TypeInfoImpl should implement TypeInfo"));
        assertSame(typeInfoApi2, implemented,
                "the rewired TypeInfoImpl must implement the re-parsed TypeInfo, not the stale one");
        assertNotSame(typeInfoApi1, implemented);
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
