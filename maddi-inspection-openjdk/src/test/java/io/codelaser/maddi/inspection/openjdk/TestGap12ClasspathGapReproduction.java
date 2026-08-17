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

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.e2immu.language.inspection.api.integration.JavaInspector.TEST_PROTOCOL;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <b>What a MISSING CLASSPATH ENTRY does to an annotated record — measured, with its control.</b>
 * <p>
 * Context: a corpus parse refused with GAP #12's message (<i>"already committed, and a second definition is now
 * arriving FROM A COMPILED ARTIFACT"</i>) for a five-component record, having modelled only constructors of 1, 2
 * and 3 parameters. The obvious reading — a compact constructor loses its canonical parameters — is WRONG, and
 * {@code TestCompactConstructorProbe} (maddi-java-openjdk) pins the refutation at scan level. The real cause was
 * {@code jackson-annotations} absent from that source set's parse classpath. This test asks what that gap does
 * end to end, through the real multi-source-set parse.
 * <p>
 * ⭐ <b>WHAT IT ESTABLISHES</b>, from the two runs below:
 * <ul>
 *     <li><b>The control (annotation PRESENT)</b> — {@code a.b.Cfg} parses to {@code [3-arg, 1-arg]}: the compact
 *     canonical constructor of a 3-component annotated record really does carry all three components. This is
 *     {@code TestCompactConstructorProbe}'s claim, re-established at INTEGRATION level rather than scan level.</li>
 *     <li><b>With the gap</b> — {@code a.b.Cfg} is <b>absent from the parse result entirely</b>, reported only as
 *     an {@code UnresolvedSymbolException} on the WARNING channel, with {@code haveErrors()==false} and
 *     {@code parseResult()} handed over as a success. A type vanishes and the parse calls itself fine.</li>
 * </ul>
 * <p>
 * ⛔ <b>WHAT IT DOES NOT DO, STATED SO THE NEXT READER DOES NOT ASSUME IT: this fixture does NOT reach the #12
 * refusal, so it does not exercise the message.</b> Here the unresolvable annotation leaves a dangling symbol and
 * the whole type is DROPPED; in the corpus javac's recovery instead committed a TRUNCATED type, which then met
 * the class file and collided. Both come from one classpath gap, and which of the two you get depends on what
 * javac's error recovery leaves behind — a mix of resolvable and unresolvable annotations in the corpus, all
 * unresolvable here. Reproducing the truncating variant synthetically was not achieved;
 * {@code TestGap12StaleArtifactDiagnosis} covers the message at unit level only.
 */
public class TestGap12ClasspathGapReproduction {

    /** Lives in its own artifact, and in the gap run the parse is deliberately NOT given it. */
    @Language("java")
    private static final String MARKER = """
            package com.example.ann;
            import java.lang.annotation.*;
            @Retention(RetentionPolicy.RUNTIME)
            @Target({ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.TYPE, ElementType.METHOD})
            public @interface Marker {
                String value() default "";
            }
            """;

    /** The corpus shape: an annotated record header, a compact canonical constructor, a convenience one. */
    @Language("java")
    private static final String CFG = """
            package a.b;
            import com.example.ann.Marker;
            @Marker("type")
            public record Cfg(@Marker("a") String alpha, @Marker("b") String beta, @Marker("c") String gamma) {
                public Cfg {
                    if (alpha == null) throw new IllegalArgumentException();
                }
                public Cfg(String alpha) {
                    this(alpha, "b", "c");
                }
            }
            """;

    /** Sees a.b.Cfg from the compiled classes directory, and calls the full canonical constructor. */
    @Language("java")
    private static final String USER = """
            package c.d;
            import a.b.Cfg;
            public class User {
                public String go() { return new Cfg("a", "b", "c").alpha(); }
            }
            """;

    private static Path compile(Path tmp, String name, Map<String, String> sources, Path... classpath)
            throws IOException {
        Path src = Files.createDirectories(tmp.resolve(name + "-src"));
        Path classes = Files.createDirectories(tmp.resolve(name + "-classes"));
        for (Map.Entry<String, String> e : sources.entrySet()) {
            Path javaFile = src.resolve(e.getKey().replace('.', '/') + ".java");
            Files.createDirectories(javaFile.getParent());
            Files.writeString(javaFile, e.getValue());
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "this test needs a JDK, not a JRE");
        List<String> args = new ArrayList<>(List.of("-d", classes.toString()));
        if (classpath.length > 0) {
            args.add("-cp");
            args.add(Stream.of(classpath).map(Path::toString)
                    .reduce((x, y) -> x + File.pathSeparator + y).orElseThrow());
        }
        try (Stream<Path> walk = Files.walk(src)) {
            walk.filter(p -> p.toString().endsWith(".java")).map(Path::toString).forEach(args::add);
        }
        int rc = compiler.run(null, null, null, args.toArray(String[]::new));
        assertEquals(0, rc, "the fixture's own compilation must succeed: " + name);
        return classes;
    }

    /** What one run of the fixture produced, so the two runs can be compared field by field. */
    private record Observation(boolean haveErrors, List<String> parseExceptions, List<String> warnings,
                               String cfgConstructors) {
    }

    @DisplayName("#12b control: with the annotation present, the compact constructor carries all 3 components")
    @Test
    public void controlWithTheAnnotationPresent() throws Exception {
        Observation o = run(false);
        assertEquals("[3-arg, 1-arg]", o.cfgConstructors(),
                "the compact canonical constructor has the record's 3 components, plus the convenience one");
        assertTrue(o.warnings().isEmpty(), "a complete classpath parses clean: " + o.warnings());
        assertTrue(o.parseExceptions().isEmpty(), o.parseExceptions().toString());
        assertFalse(o.haveErrors());
    }

    /**
     * ⛔ The measurement that matters, and it is asserted UNCONDITIONALLY — a branch that accepts every outcome
     * is an escape hatch, not a control ({@code TestGap12StaleJarReproduction} learned that the hard way).
     */
    @DisplayName("#12b: with the classpath gap, the type is dropped — on the warning channel, parse 'successful'")
    @Test
    public void withTheGapTheTypeIsDroppedAndTheParseStillSucceeds() throws Exception {
        Observation o = run(true);
        assertEquals("(absent)", o.cfgConstructors(),
                "the whole type is gone from the parse result, not merely truncated");
        assertFalse(o.warnings().isEmpty(), "it is NOT silent: the drop is reported");
        assertTrue(o.warnings().toString().contains("UnresolvedSymbolException"), o.warnings().toString());
        // ⚠ and this is the uncomfortable half: nothing on the error channel, and parseResult() is handed over
        assertFalse(o.haveErrors(), "haveErrors() is false even though a type vanished");
        assertTrue(o.parseExceptions().isEmpty(), "no parse exception either: " + o.parseExceptions());
    }

    private Observation run(boolean withGap) throws Exception {
        Path tmp = Files.createTempDirectory("gap12b-");
        try {
            Path annClasses = compile(tmp, "ann", Map.of("com.example.ann.Marker", MARKER));
            // a.b.Cfg compiles cleanly WITH the annotation; that is what target/classes holds
            Path cfgClasses = compile(tmp, "cfg", Map.of("a.b.Cfg", CFG), annClasses);

            SourceSet javaBase = SourceSetImpl.javaBase();
            // the module's own exploded output, on the classpath — NOT a jar, which is the point
            SourceSet moduleClasses = new SourceSetImpl.Builder()
                    .setName("definition-target-classes").setUri(cfgClasses.toUri())
                    .setLibrary(true).setExternalLibrary(true)
                    .setDependencies(List.of(javaBase)).build();
            SourceSet annLib = new SourceSetImpl.Builder()
                    .setName("example-ann").setUri(annClasses.toUri())
                    .setLibrary(true).setExternalLibrary(true)
                    .setDependencies(List.of(javaBase)).build();
            // ⛔ THE GAP: com.example.ann is absent from this source set's dependencies and from the classpath.
            SourceSet definitionMain = new SourceSetImpl.Builder().setName(TEST_PROTOCOL)
                    .setUri(URI.create("file:/definition/"))
                    .setDependencies(withGap ? List.of(javaBase) : List.of(javaBase, annLib)).build();
            SourceSet consumer = new SourceSetImpl.Builder().setName(TEST_PROTOCOL + "B")
                    .setUri(URI.create("file:/consumer/"))
                    .setDependencies(List.of(javaBase, moduleClasses)).build();

            InputConfigurationImpl.Builder icb = new InputConfigurationImpl.Builder();
            if (withGap) icb.addClassPathParts(javaBase, moduleClasses);
            else icb.addClassPathParts(javaBase, moduleClasses, annLib);
            InputConfiguration ic = icb.addSourceSets(definitionMain, consumer).build();

            JavaInspector javaInspector = new JavaInspectorImpl();
            javaInspector.initialize(ic);

            Map<SourceSet, Map<String, String>> sources = new LinkedHashMap<>();
            sources.put(definitionMain, Map.of("a.b.Cfg", CFG));
            sources.put(consumer, Map.of("c.d.User", USER));

            Summary summary = javaInspector.parseMultiSourceSet(sources, JavaInspectorImpl.DETAILED_SOURCES);
            TypeInfo cfg = summary.parseResult().findType("a.b.Cfg");
            Observation o = new Observation(summary.haveErrors(),
                    summary.parseExceptions().stream().map(e -> String.valueOf(e.getMessage())).toList(),
                    summary.parseWarnings().stream().map(e -> String.valueOf(e.getMessage())).toList(),
                    cfg == null ? "(absent)"
                            : cfg.constructors().stream().map(c -> c.parameters().size() + "-arg").toList()
                            .toString());
            System.out.println("#12b " + (withGap ? "WITH THE GAP" : "CONTROL, no gap") + " >>> " + o + "\n<<<");
            return o;
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
