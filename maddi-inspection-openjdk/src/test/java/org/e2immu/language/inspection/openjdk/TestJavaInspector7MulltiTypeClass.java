package org.e2immu.language.inspection.openjdk;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.e2immu.language.inspection.api.integration.JavaInspector.TEST_PROTOCOL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestJavaInspector7MulltiTypeClass {
    public static final String COMMONS_CLI_1_11_0_JAR = "commons-cli-1.11.0.jar"; // as a file in the project
    public static final String COMMONS_CLI_1_2_JAR = "commons-cli-1.2.jar"; // as a file in the project

    private JavaInspector javaInspector;
    private SourceSet sourceSet1;
    private SourceSet sourceSet2;

    @BeforeEach
    public void test() throws IOException {
        javaInspector = new JavaInspectorImpl();

        Path commonsCli2Jar = Path.of("src/test/resources/" + COMMONS_CLI_1_2_JAR);
        assertTrue(Files.isReadable(commonsCli2Jar));
        SourceSet commonsCli2 = new SourceSetImpl.Builder()
                .setName(COMMONS_CLI_1_2_JAR)
                .setUri(commonsCli2Jar.toUri())
                .setLibrary(true).setExternalLibrary(true).build();

        sourceSet1 = new SourceSetImpl.Builder().setName(TEST_PROTOCOL + "1").setUri(URI.create("file:/"))
                .setDependencies(List.of(commonsCli2))
                .build();

        Path commonsCli11Jar = Path.of("src/test/resources/" + COMMONS_CLI_1_11_0_JAR);
        assertTrue(Files.isReadable(commonsCli11Jar));
        SourceSet commonsCli11 = new SourceSetImpl.Builder()
                .setName(COMMONS_CLI_1_11_0_JAR)
                .setUri(commonsCli11Jar.toUri())
                .setLibrary(true).setExternalLibrary(true).build();

        sourceSet2 = new SourceSetImpl.Builder().setName(TEST_PROTOCOL + "2").setUri(URI.create("file:/"))
                .setDependencies(List.of(sourceSet1, commonsCli11))
                .build();

        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSourceSets(sourceSet1, sourceSet2)
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .addClassPathParts(commonsCli2, commonsCli11)
                .build();
        javaInspector.initialize(inputConfiguration);
    }

    @Language("java")
    private static final String INPUTX1 = """
            package a.b;
            import org.apache.commons.cli.Option;
            class X1 {
                void method(Option option) {
                    System.out.println("Received "+option);
                }
            }
            """;

    @Language("java")
    private static final String INPUTX1_2 = """
            package a.b;
            import org.apache.commons.cli.Option;
            class X1 {
                void method() {
                    Option.builder(); // this method does not exist in 1.2
                }
            }
            """;

    @Language("java")
    private static final String INPUTY = """
            package a.b;
            class Y {
                void method(String s) {
                    new X1().method();
                }
            }
            """;

    @Test
    public void test1() {
        ParseResult parseResult = javaInspector.parseMultiSourceSet(Map.of(sourceSet1, Map.of("a.b.X1", INPUTX1),
                                sourceSet2, Map.of("a.b.X1", INPUTX1_2, "a.b.Y", INPUTY)),
                        JavaInspectorImpl.DETAILED_SOURCES)
                .parseResult();

        List<TypeInfo> list = parseResult.typeByFullyQualifiedName("a.b.X1");
        assertEquals(2, list.size());
        assertEquals("test-protocol1::a.b.X1", list.getFirst().descriptor());
        assertEquals("test-protocol2::a.b.X1", list.getLast().descriptor());

        TypeInfo Y = parseResult.findType("a.b.Y");
        MethodInfo method = Y.findUniqueMethod("method", 1);
        MethodCall mc = ((MethodCall) method.methodBody().statements().getFirst().expression());
        assertEquals("test-protocol2::a.b.X1.method(java.lang.String)", mc.methodInfo().descriptor());
    }
}
