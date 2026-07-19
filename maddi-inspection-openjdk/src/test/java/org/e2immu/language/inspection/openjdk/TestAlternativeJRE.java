package org.e2immu.language.inspection.openjdk;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.e2immu.language.inspection.api.integration.JavaInspector.TEST_PROTOCOL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The parser loads platform (java.*) types from the JDK running the analyzer by default. When
 * {@link InputConfiguration#alternativeJREDirectory()} is set (the {@code --jre} option), it must instead load
 * them from that JDK.
 * <p>
 * Probe: {@code java.applet.Applet} was removed in JDK 26 (JEP 504). This test runs on JDK 26, where the type no
 * longer exists, and points the parser at an older JDK (JDK 21). A pass therefore proves the alternative JRE was
 * honoured — the default {@code --release=26} could not resolve {@code Applet} at all. Skips when no JDK &le; 25
 * home is supplied via {@code -Dtest.jdk21.home} or {@code $JDK21_HOME}.
 */
public class TestAlternativeJRE {

    @Language("java")
    private static final String INPUT = """
            package p;
            import java.applet.Applet;
            public class UsesApplet {
                Applet applet;
            }
            """;

    private static String alternativeJre() {
        String p = System.getProperty("test.jdk21.home");
        if (p == null || p.isBlank()) p = System.getenv("JDK21_HOME");
        return p;
    }

    @Test
    public void alternativeJreResolvesTypeRemovedInRunningJdk() throws IOException {
        String jdk = alternativeJre();
        Assumptions.assumeTrue(jdk != null && !jdk.isBlank() && Files.isDirectory(Path.of(jdk)),
                "requires a JDK <= 25 home in -Dtest.jdk21.home or $JDK21_HOME (java.applet.Applet was removed in JDK 26)");

        JavaInspector javaInspector = new JavaInspectorImpl();
        SourceSet sourceSet = new SourceSetImpl.Builder().setName(TEST_PROTOCOL).setUri(URI.create("file:/")).build();
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSourceSets(sourceSet)
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES) // java.desktop carries java.applet
                .setAlternativeJREDirectory(jdk)
                .build();
        javaInspector.initialize(inputConfiguration);

        ParseResult parseResult = javaInspector.parse(Map.of("p.UsesApplet", INPUT), JavaInspectorImpl.DETAILED_SOURCES)
                .parseResult();
        TypeInfo usesApplet = parseResult.findType("p.UsesApplet");
        assertNotNull(usesApplet);

        FieldInfo applet = usesApplet.getFieldByName("applet", true);
        assertEquals("java.applet.Applet", applet.type().typeInfo().fullyQualifiedName(),
                "the field type must resolve to the alternative JDK's java.applet.Applet, removed in the JDK 26 running this test");
    }
}
