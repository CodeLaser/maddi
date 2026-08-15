package org.e2immu.language.inspection.openjdk;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.Summary;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.e2immu.language.inspection.api.integration.JavaInspector.TEST_PROTOCOL;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <b>{@code --jdk-internals} must not depend on HOW the configuration was authored.</b>
 * <p>
 * {@code jdkInternalsJavacOptions} emits one {@code --add-exports} per non-exported package of the JDK modules it
 * is given, and that loop is the ONLY thing that opens such a package. It used to read the list from
 * {@code sourceSet.dependencies()} alone — so for a configuration whose JDK modules are declared as CLASS PATH
 * PARTS the list was empty, the flag dropped {@code --release}/{@code --system} and then added nothing, and the
 * parse failed anyway.
 * <p>
 * ⛔ <b>The failure mode is what makes this worth a test: it does not look like a missing option, it looks like a
 * refusal.</b> Without the flag javac says {@code package sun.security.jca does not exist} (ct.sym filtering);
 * with the flag but no module list it says <i>{@code package sun.security.jca is not visible — declared in module
 * java.base, which does not export it to the unnamed module}</i>, which reads as a deliberate decision about
 * module boundaries rather than an {@code --add-exports} that was never emitted.
 * <p>
 * Both authoring styles are real and both are in the corpus catalogue: the plugin routes wire every source set to
 * the jmods, {@code CompileListToInputConfiguration} adds them through {@code addClassPathParts} only. Measured
 * over the 14 generated configurations there, 10 have 20-21 {@code partOfJdk} class path parts and ZERO source
 * sets referencing one. ⚠ It does not split cleanly by route — guava, jenkins, activemq, camel and langchain4j
 * carry them while elasticsearch (0 of 27), pulsar (0 of 90) and timefold-solver (0 of 65) do not — which is why
 * the fallback belongs in the inspector rather than in one generator.
 */
public class TestJdkInternalsWithoutJdkDependencies {

    /** {@code sun.security.jca} is in java.base and is NOT exported: reachable only via --add-exports. */
    @Language("java")
    private static final String USES_INTERNAL = """
            package a.b;
            import sun.security.jca.Providers;
            public class UsesInternal {
                public static String go() {
                    return String.valueOf(Providers.getProviderList());
                }
            }
            """;

    /** The JDK modules this fixture declares; java.base is the one that owns sun.security.jca. */
    private static List<SourceSet> jdkModules() {
        return List.of(SourceSetImpl.javaBase(), SourceSetImpl.jdkModule("java.logging"));
    }

    private record Outcome(boolean parsed, List<String> messages, List<String> warnings, boolean typePresent) {
    }

    /**
     * @param declareJdkAsDependencies the PLUGIN shape when true (jmods reachable via dependencies()), the
     *                                 COMPILE-LOG shape when false (jmods only on the class path)
     */
    private Outcome run(boolean declareJdkAsDependencies) {
        List<SourceSet> jdk = jdkModules();
        SourceSet main = new SourceSetImpl.Builder().setName(TEST_PROTOCOL)
                .setUri(URI.create("file:/main/"))
                .setDependencies(declareJdkAsDependencies ? jdk : List.of())
                .build();

        InputConfiguration ic = new InputConfigurationImpl.Builder()
                .addClassPathParts(jdk)
                .addSourceSets(main)
                .build();

        JavaInspector javaInspector = new JavaInspectorImpl();
        javaInspector.setJdkInternals(true);

        try {
            javaInspector.initialize(ic);
            Summary summary = javaInspector.parseMultiSourceSet(Map.of(main, Map.of("a.b.UsesInternal",
                    USES_INTERNAL)), JavaInspectorImpl.DETAILED_SOURCES);
            List<String> messages = summary.parseExceptions().stream()
                    .map(e -> String.valueOf(e.getMessage())).toList();
            List<String> warnings = summary.parseWarnings().stream()
                    .map(e -> String.valueOf(e.getMessage())).toList();
            TypeInfo t = summary.parseResult().findType("a.b.UsesInternal");
            return new Outcome(messages.isEmpty(), messages, warnings, t != null);
        } catch (RuntimeException | java.io.IOException re) {
            return new Outcome(false, List.of(re.getClass().getSimpleName() + ": " + re.getMessage()),
                    List.of(), false);
        }
    }

    @DisplayName("the plugin shape: JDK modules reachable through dependencies() — the case that already worked")
    @Test
    public void jdkModulesAsDependencies() {
        Outcome o = run(true);
        System.out.println("#jdk-internals dependencies-shape >>> " + o);
        assertTrue(o.parsed(), "a non-exported JDK package must be reachable: " + o.messages());
        assertTrue(o.typePresent(), "and the type must survive into the result");
    }

    /**
     * ⛔ THE REGRESSION TEST, and it was run against the pre-fix build, where it fails.
     * <p>
     * ⚠ <b>It fails on {@code typePresent}, NOT on any message — and that is the finding.</b> Pre-fix the outcome
     * is {@code parsed=true, messages=[], typePresent=false}: javac's <i>"package sun.security.jca is not
     * visible"</i> reaches neither {@code parseExceptions()} nor {@code parseWarnings()}, so the whole
     * compilation unit is dropped while the parse reports success. An assertion written only against the
     * expected message would have passed here vacuously; asserting that the TYPE ARRIVED is what catches it.
     */
    @DisplayName("the compile-log shape: JDK modules only on the class path — must work identically")
    @Test
    public void jdkModulesOnlyOnTheClassPath() {
        Outcome o = run(false);
        System.out.println("#jdk-internals classpath-shape >>> " + o);
        assertTrue(o.typePresent(), "the fallback must open the configuration's JDK modules too, and the type"
                                    + " must reach the result; pre-fix it is silently absent: " + o);
        assertTrue(o.parsed(), o.messages().toString());
    }

    /**
     * The two shapes must agree, which is the whole point: the same sources and the same JDK, described two ways.
     */
    @DisplayName("the two authoring shapes produce the same outcome")
    @Test
    public void bothShapesAgree() {
        assertEquals(run(true).typePresent(), run(false).typePresent(),
                "--jdk-internals must not depend on whether the jmods were spelled as dependencies or as class"
                + " path parts");
    }
}
