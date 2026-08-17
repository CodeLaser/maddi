package org.e2immu.language.inspection.openjdk;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.Summary;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

import static org.e2immu.language.inspection.api.integration.JavaInspector.TEST_PROTOCOL;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <b>GAP #12, the REPRODUCTION attempt</b> — a real stale jar, not a hand-written message.
 * <p>
 * The corpus shape, from the ES three-module carve: a distribution build ({@code localDistro}) left a plugin
 * bundle jar behind; the next parse read it as a <b>second, stale definition</b> of a type that had since been
 * edited, and the scanner met that type twice — committed once, then again with a different constructor
 * descriptor. 18 units dropped, <b>10 never touched by the edit</b>, javac green throughout. The rule is
 * <b>"rung 6 poisons rung 1"</b>.
 * <p>
 * This models it with the mechanism the scanner's own comment names: <i>"the TypeInfo is shared across
 * source-set scans via InfoByFqn, but each scan has its own javac context"</i>. So: source set A declares
 * {@code a.b.Widget} with a one-argument constructor; a jar holds a DIFFERENT {@code a.b.Widget} with a
 * two-argument one; source set B sees only the jar and calls the two-argument form.
 * <p>
 * ⚠ <b>WHETHER THIS REPRODUCES IS ITSELF THE RESULT.</b> After #150 and #163 today — a minimisation that
 * parsed clean, and a filed symptom that was unreachable the filed way — this test asserts what it OBSERVES
 * and says so either way, rather than being written to confirm what I expect.
 */
public class TestGap12StaleJarReproduction {

    /** The version that goes into the jar: TWO constructor parameters. */
    @Language("java")
    private static final String WIDGET_IN_JAR = """
            package a.b;
            public class Widget {
                public Widget(String name, int size) { }
                public String describe() { return "jar"; }
            }
            """;

    /** The version in source, i.e. after the edit: ONE constructor parameter. The disagreement. */
    @Language("java")
    private static final String WIDGET_IN_SOURCE = """
            package a.b;
            public class Widget {
                public Widget(String name) { }
                public String describe() { return "source"; }
            }
            """;

    /** Sees the jar only, and calls the constructor that exists ONLY in the stale jar. */
    @Language("java")
    private static final String USER = """
            package c.d;
            import a.b.Widget;
            public class User {
                public String go() { return new Widget("w", 3).describe(); }
            }
            """;

    private static Path compileToJar(Path tmp, String fqn, String source) throws IOException {
        Path src = tmp.resolve("src");
        Path classes = Files.createDirectories(tmp.resolve("classes"));
        Path javaFile = src.resolve(fqn.replace('.', '/') + ".java");
        Files.createDirectories(javaFile.getParent());
        Files.writeString(javaFile, source);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "this test needs a JDK, not a JRE");
        int rc = compiler.run(null, null, null, "-d", classes.toString(), javaFile.toString());
        assertEquals(0, rc, "the jar's version of the type must compile");

        Path jar = tmp.resolve("stale-plugin-bundle.jar");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jar));
             Stream<Path> walk = Files.walk(classes)) {
            for (Path p : walk.filter(Files::isRegularFile).toList()) {
                jos.putNextEntry(new JarEntry(classes.relativize(p).toString().replace('\\', '/')));
                jos.write(Files.readAllBytes(p));
                jos.closeEntry();
            }
        }
        return jar;
    }

    @DisplayName("#12: a stale jar defining the same type as source — what does the parse actually say?")
    @Test
    public void staleJarBesideSource() throws Exception {
        Path tmp = Files.createTempDirectory("gap12-");
        try {
            Path jar = compileToJar(tmp, "a.b.Widget", WIDGET_IN_JAR);

            SourceSet javaBase = SourceSetImpl.javaBase();
            SourceSet staleJar = new SourceSetImpl.Builder()
                    .setName("stale-plugin-bundle").setUri(jar.toUri())
                    .setLibrary(true).setExternalLibrary(true)
                    .setDependencies(List.of(javaBase)).build();
            // A: declares a.b.Widget from source (the edited version)
            SourceSet setA = new SourceSetImpl.Builder().setName(TEST_PROTOCOL).setUri(URI.create("file:/a/"))
                    .setDependencies(List.of(javaBase)).build();
            // B: sees the JAR's a.b.Widget, and calls the constructor only the jar has
            SourceSet setB = new SourceSetImpl.Builder().setName(TEST_PROTOCOL + "B").setUri(URI.create("file:/b/"))
                    .setDependencies(List.of(javaBase, staleJar)).build();

            InputConfiguration ic = new InputConfigurationImpl.Builder()
                    .addClassPathParts(javaBase, staleJar)
                    .addSourceSets(setA, setB)
                    .build();
            JavaInspector javaInspector = new JavaInspectorImpl();
            javaInspector.initialize(ic);

            Map<SourceSet, Map<String, String>> sources = new LinkedHashMap<>();
            sources.put(setA, Map.of("a.b.Widget", WIDGET_IN_SOURCE));
            sources.put(setB, Map.of("c.d.User", USER));

            String observed;
            try {
                Summary summary = javaInspector.parseMultiSourceSet(sources, JavaInspectorImpl.DETAILED_SOURCES);
                observed = summary.parseExceptions().isEmpty() ? "PARSED"
                        : "PARSE EXCEPTIONS: " + summary.parseExceptions().stream()
                        .map(e -> String.valueOf(e.getMessage())).toList();
                try {
                    summary.parseResult();
                    observed += " | parseResult() OK";
                } catch (RuntimeException re) {
                    observed += " | parseResult() refused: " + re.getMessage();
                }
            } catch (RuntimeException re) {
                observed = "THREW " + re.getClass().getSimpleName() + ": " + re.getMessage();
            }
            System.out.println("#12 STALE-JAR OBSERVATION >>>\n" + observed + "\n<<<");

            // ⛔ ASSERTED UNCONDITIONALLY, AND THAT IS THE SECOND LESSON THIS TEST TAUGHT.
            // Its first version branched: "if the definitions collided, assert the jar is named; otherwise
            // record that the fixture did not reproduce". Run against the PRE-FIX code that branch took the
            // else -- the old exception message was null, so neither marker matched -- and the test PASSED
            // while the diagnosis did not exist. ▶▶ A BRANCH THAT ACCEPTS EVERY OUTCOME IS NOT A CONTROL, IT
            // IS AN ESCAPE HATCH; keeping "whether it reproduces is the result" honest meant running it against
            // the broken build FIRST, and once the answer is known the test has to commit to it.
            assertTrue(observed.contains("already committed"),
                    "the collision must be reported at all: " + observed);
            assertTrue(observed.contains("a.b.Widget"), observed);
            assertTrue(observed.contains("stale-plugin-bundle.jar"),
                    "it must name the ARTIFACT, which is the whole point of #12: " + observed);
            assertTrue(observed.contains("THE JAR IS STALE"), "and state the cause: " + observed);
            assertTrue(observed.contains("last modified"),
                    "with the mtime, so a reader can see it predates the edit: " + observed);
            assertTrue(observed.contains("REBUILD OR DELETE IT"), "and the remedy: " + observed);
            // the top-level verdict is the line the caller actually sees, and it used to name nothing at all
            assertTrue(observed.contains("parse error(s) in 1 compilation unit(s)"), observed);
            assertTrue(observed.contains("need not be the ones you edited"), observed);
        } finally {
            try (Stream<Path> walk = Files.walk(tmp)) {
                walk.sorted((x, y) -> y.getNameCount() - x.getNameCount()).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {
                    }
                });
            }
        }
    }
}
