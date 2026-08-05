package io.codelaser.maddi.inspection.openjdk;

import io.codelaser.maddi.annotation.ImmutableContainer;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import static io.codelaser.maddi.inspection.api.integration.JavaInspector.TEST_PROTOCOL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestJavaInspector3RealClasspath {

    public static final String COMMONS_CLI_JAR = "commons-cli-1.11.0.jar"; // as a file in the project
    public static final String SLF4J_API_JAR = "slf4j-api-2.0.17.jar"; // on maddi classpath

    private JavaInspector javaInspector;
    private Runtime runtime;

    @BeforeEach
    public void test() throws IOException, URISyntaxException {
        javaInspector = new JavaInspectorImpl();

        Path commonsCliJar = Path.of("src/test/resources/" + COMMONS_CLI_JAR);
        assertTrue(Files.isReadable(commonsCliJar));
        SourceSet commonsCli = new SourceSetImpl.Builder().setName(COMMONS_CLI_JAR).setUri(commonsCliJar.toUri())
                .setLibrary(true).setExternalLibrary(true).build();

        // Two parts since the 0.9.1 split: annotations in maddi-annotation, SetOnce/Either in maddi-support.
        Path maddiAnnotationClasses = Path.of("../maddi-annotation/build/classes/java/main/");
        assertTrue(Files.isDirectory(maddiAnnotationClasses));
        SourceSet maddiAnnotation = new SourceSetImpl.Builder().setName("maddi-annotation")
                .setUri(maddiAnnotationClasses.toUri())
                .setLibrary(true).setExternalLibrary(true).build();

        Path maddiSupportClasses = Path.of("../maddi-support/build/classes/java/main/");
        assertTrue(Files.isDirectory(maddiSupportClasses));
        SourceSet maddiSupport = new SourceSetImpl.Builder().setName("maddi-support")
                .setUri(maddiSupportClasses.toUri())
                .setLibrary(true).setExternalLibrary(true).build();
        // note: no need to set module!

        URL slf4jJar = findJarInClassPath("org/slf4j/event");
        SourceSet slf4j = new SourceSetImpl.Builder().setName(SLF4J_API_JAR).setUri(slf4jJar.toURI())
                .setLibrary(true).setExternalLibrary(true).build();

        SourceSet sourceSet = new SourceSetImpl.Builder()
                .setName(TEST_PROTOCOL).setUri(URI.create("file:/"))
                .setDependencies(List.of(commonsCli, maddiAnnotation, maddiSupport, slf4j))
                .build();

        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSourceSets(sourceSet)
                .addClassPath("jmod:java.base", "jmod:java.sql")
                .addClassPathParts(commonsCli, maddiSupport, slf4j)
                .build();
        assertEquals(5, inputConfiguration.classPathParts().size());
        javaInspector.initialize(inputConfiguration);
        runtime = javaInspector.runtime();
    }

    @Language("java")
    public static final String INPUT1 = """
            package a.b;
            import org.apache.commons.cli.Option;
            import io.codelaser.maddi.annotation.ImmutableContainer;
            import org.slf4j.Logger;
            import org.slf4j.LoggerFactory;
            import java.sql.Date;
            
            @ImmutableContainer
            record C(int k) {
                private static final Logger LOGGER = LoggerFactory.getLogger(C.class);
            
                int kSquared() {
                    LOGGER.info("Returning k*k = {}", k*k);
                    return k*k;
                }
            
                Option makeOption(Date date) {
                    return new Option("-v", "return "+k+" at "+date);
                }
            }
            """;

    @Test
    public void test1() {
        JavaInspector.ParseOptions options = new JavaInspector.ParseOptions.Builder()
                .setFailFast(true).setDetailedSources(true).build();
        ParseResult parseResult = javaInspector.parse(Map.of("a.b.C", INPUT1), options).parseResult();
        TypeInfo C = parseResult.findType("a.b.C");
        assertEquals("@ImmutableContainer", C.annotations().getFirst().toString());

        TypeInfo option = C.findUniqueMethod("makeOption", 1).returnType().typeInfo();
        assertEquals(COMMONS_CLI_JAR, option.compilationUnit().sourceSet().name());

        TypeInfo logger = C.getFieldByName("LOGGER", true).type().typeInfo();
        assertEquals(SLF4J_API_JAR, logger.compilationUnit().sourceSet().name());

        TypeInfo immutableContainer = runtime.getFullyQualified("io.codelaser.maddi.annotation.ImmutableContainer",
                true);
        assertTrue(immutableContainer.typeNature().isAnnotation());
        // @ImmutableContainer moved to maddi-annotation in the 0.9.1 split
        assertEquals("maddi-annotation", immutableContainer.compilationUnit().sourceSet().name());
    }

    public URL findJarInClassPath(String prefix) throws IOException {
        Enumeration<URL> roots = getClass().getClassLoader().getResources(prefix);
        if (roots.hasMoreElements()) {
            URL url = roots.nextElement();
            String urlString = url.toString();
            int bangSlash = urlString.indexOf("!/");
            if (bangSlash < 0) {
                throw new UnsupportedEncodingException("? expect !/ in " + urlString);
            }
            String strippedUrlString = urlString.substring("jar:".length(), bangSlash);
            return new URL(strippedUrlString);
        }
        return null;
    }
}
