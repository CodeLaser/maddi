package io.codelaser.maddi.inspection.openjdk;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;

import static io.codelaser.maddi.inspection.api.integration.JavaInspector.TEST_PROTOCOL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * {@link SourceSet#sourceRelease()}: a source set compiles against ITS OWN Java API level.
 * <p>
 * The global {@code InputConfiguration.sourceRelease()} states one answer for the whole configuration, so it
 * abstains for a reactor that states several — see {@code CompileListToInputConfiguration#setSourceRelease}:
 * picking the maximum hides an API removed after it from the module that still uses it, picking the minimum
 * invents compile errors in the module that does not. OpenSearch states three (44 sets at 21, buildSrc/reaper
 * at 11, libs/common at 8) and paid for the abstention with a dropped unit and a refused ParseResult. Per set
 * the question has an answer: the set's own.
 * <p>
 * <b>The probe is {@link TestAlternativeJRE}'s, reached the cheap way.</b> {@code java.applet.Applet} was
 * removed in JDK 26 (JEP 504), so on a JDK 26 runner it cannot resolve at the default release — that sibling
 * test proves the point by pointing at an installed JDK 21 and skips when none is configured. Through
 * {@code --release} no second JDK is needed: {@code ct.sym} ships with the running one. A pass therefore says
 * {@code --release=21} reached javac for this set.
 * <p>
 * ⭐ <b>And it says it came from the SET, because the configuration's global release is 0.</b> Nothing else in
 * the chain could have supplied 21 — that is the whole assertion, and it is why this test does not need a
 * second source set to demonstrate per-set-ness.
 */
public class TestPerSourceSetRelease {

    @Language("java")
    private static final String INPUT = """
            package p;
            import java.applet.Applet;
            public class UsesApplet {
                Applet applet;
            }
            """;

    @DisplayName("a set states release 21 and resolves a type the running JDK removed; the global release is 0")
    @Test
    public void perSetReleaseReachesJavac() throws java.io.IOException {
        int running = Runtime.version().feature();
        Assumptions.assumeTrue(running >= 26,
                "the probe is java.applet.Applet, removed in JDK 26; below that the default release resolves it"
                + " anyway and the test would pass without proving anything (running on " + running + ")");

        JavaInspector javaInspector = new JavaInspectorImpl();
        SourceSet sourceSet = new SourceSetImpl.Builder()
                .setName(TEST_PROTOCOL)
                .setUri(URI.create("file:/"))
                .setSourceRelease(21)
                .build();
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSourceSets(sourceSet)
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES) // java.desktop carries java.applet
                .build();
        // 0: the mixed-reactor case, where the global field has nothing it can honestly say
        org.junit.jupiter.api.Assertions.assertEquals(0, inputConfiguration.sourceRelease());
        javaInspector.initialize(inputConfiguration);

        ParseResult parseResult = javaInspector
                .parse(Map.of("p.UsesApplet", INPUT), JavaInspectorImpl.DETAILED_SOURCES).parseResult();
        TypeInfo usesApplet = parseResult.findType("p.UsesApplet");
        assertNotNull(usesApplet, "the set states release 21, where java.applet.Applet still exists");

        // Control, run 2026-08-17: with setSourceRelease(21) removed, the failure is the assertNotNull ABOVE --
        // "expected: not <null>" -- because the unit whose field cannot resolve is dropped whole, so findType
        // alone is already a real probe here.
        // This second assertion is kept anyway, for the regression findType cannot see: a unit KEPT with the
        // field left unresolved. That is the vacuous-green shape, and it costs one line to refuse it.
        FieldInfo applet = usesApplet.getFieldByName("applet", true);
        assertEquals("java.applet.Applet", applet.type().typeInfo().fullyQualifiedName(),
                "the field must resolve against release 21's platform, not the running JDK's");
    }
}
