/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.codelaser.maddi.inspection.openjdk;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.Summary;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

import static io.codelaser.maddi.inspection.api.integration.JavaInspector.TEST_PROTOCOL;
import static org.junit.jupiter.api.Assertions.*;

/**
 * A Lombok processor that cannot run makes the parse fall back to parsing WITHOUT Lombok -- and that fallback is a
 * parse ERROR, not a log line. On JDK 27, Lombok 1.18.42 and 1.18.46 fail to initialise (they need
 * {@code com.sun.tools.javac.tree.EndPosTable}, which JDK 27 removed); the fallback used to be one WARN per source
 * set while the parse reported success, so a corpus either failed far downstream ("Type 'log' not found") or went
 * green on a model with Lombok's members missing.
 * <p>
 * The broken processor is built here: a {@code lombok-0.0.0-broken.jar} whose
 * {@code lombok.launch.AnnotationProcessorHider$AnnotationProcessor} -- the class maddi asks javac for -- fails in
 * {@code init()} with an ExceptionInInitializerError, as the real ones do when they load their javac handlers (a
 * failure while CONSTRUCTING the processor is not the same path: javac reports that as a diagnostic). So the test does not depend on which JDK it runs on, nor on a Lombok
 * version that happens to be broken on it.
 */
public class TestLombokFallback {

    @Language("java")
    private static final String BROKEN_PROCESSOR = """
            package lombok.launch;
            import java.util.Set;
            import javax.annotation.processing.AbstractProcessor;
            import javax.annotation.processing.ProcessingEnvironment;
            import javax.annotation.processing.RoundEnvironment;
            import javax.lang.model.element.TypeElement;
            public class AnnotationProcessorHider {
                public static class AnnotationProcessor extends AbstractProcessor {
                    @Override
                    public synchronized void init(ProcessingEnvironment processingEnv) {
                        super.init(processingEnv);
                        Handlers.load();
                    }
                    @Override
                    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
                        return false;
                    }
                }
                static class Handlers {
                    static {
                        if (true) throw new IllegalStateException("simulated: a javac class this Lombok needs is gone");
                    }
                    static void load() {
                    }
                }
            }
            """;

    // annotated: javac only instantiates (and so initialises) processors in a round that has annotations to offer;
    // a source set that uses Lombok always does
    @Language("java")
    private static final String INPUT = """
            package io.codelaser.maddi.test;
            @SuppressWarnings("unused")
            public class X {
                private int t;
            }
            """;

    @TempDir
    Path tempDir;

    private JavaInspector javaInspector;
    private SourceSet sourceSet;

    @BeforeEach
    public void before() throws IOException {
        Path jar = brokenLombokJar();
        SourceSet javaBase = SourceSetImpl.javaBase();
        SourceSet lombok = LombokJar.sourceSet(jar, javaBase);
        sourceSet = new SourceSetImpl.Builder().setName(TEST_PROTOCOL + "1").setUri(URI.create("file:/"))
                .setDependencies(List.of(javaBase, lombok)).build();
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSourceSets(sourceSet)
                .addClassPath("jmod:java.base")
                .addClassPathParts(lombok)
                .build();
        javaInspector = new JavaInspectorImpl();
        javaInspector.initialize(inputConfiguration);
    }

    private Path brokenLombokJar() throws IOException {
        Path src = tempDir.resolve("src/lombok/launch/AnnotationProcessorHider.java");
        Files.createDirectories(src.getParent());
        Files.writeString(src, BROKEN_PROCESSOR);
        Path classes = Files.createDirectories(tempDir.resolve("classes"));
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        assertEquals(0, javac.run(null, null, null, "-d", classes.toString(), src.toString()),
                "the broken processor must compile");
        Path jar = tempDir.resolve("lombok-0.0.0-broken.jar");
        try (OutputStream os = Files.newOutputStream(jar); JarOutputStream jos = new JarOutputStream(os);
             Stream<Path> files = Files.walk(classes)) {
            for (Path f : files.filter(Files::isRegularFile).toList()) {
                jos.putNextEntry(new JarEntry(classes.relativize(f).toString().replace('\\', '/')));
                jos.write(Files.readAllBytes(f));
                jos.closeEntry();
            }
        }
        return jar;
    }

    private Summary parse(boolean failFast) {
        JavaInspector.ParseOptions options = new JavaInspector.ParseOptions.Builder()
                .setFailFast(failFast).setDetailedSources(true).setLombok(true).build();
        return javaInspector.parseMultiSourceSet(Map.of(sourceSet, Map.of("io.codelaser.maddi.test.X", INPUT)),
                options);
    }

    @Test
    public void theFallbackIsAParseError() {
        Summary summary = parse(false);
        assertTrue(summary.haveErrors(), "a parse without its declared Lombok must not report success");
        List<JavaInspectorImpl.LombokFallback> fallbacks = summary.parseExceptions().stream()
                .filter(e -> e instanceof JavaInspectorImpl.LombokFallback)
                .map(e -> (JavaInspectorImpl.LombokFallback) e).toList();
        assertEquals(1, fallbacks.size(), String.valueOf(summary.parseExceptions()));
        JavaInspectorImpl.LombokFallback fallback = fallbacks.getFirst();
        assertEquals(TEST_PROTOCOL + "1", fallback.sourceSetName());
        assertEquals("lombok-0.0.0-broken.jar", fallback.lombokJar());
        // the root cause, not the ExceptionInInitializerError that wraps it
        assertTrue(fallback.rootCause().contains("simulated: a javac class this Lombok needs is gone"),
                fallback.rootCause());
        assertTrue(fallback.getMessage().contains("parsed WITHOUT Lombok"), fallback.getMessage());

        // the policy refusal, as for any parse error...
        assertThrows(UnsupportedOperationException.class, summary::parseResult);
        // ...while the retry's types are still there for a caller that accepts a partial result (the IDE)
        assertNotNull(summary.parseResultIgnoringErrors().findType("io.codelaser.maddi.test.X"));
    }

    @Test
    public void failFastStopsAtTheFallback() {
        assertThrows(Summary.FailFastException.class, () -> parse(true));
    }
}
