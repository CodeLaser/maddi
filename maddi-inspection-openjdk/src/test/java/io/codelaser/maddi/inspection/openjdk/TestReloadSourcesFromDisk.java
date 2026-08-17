package org.e2immu.language.inspection.openjdk;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@code reloadSources} over real files on disk — the path {@code RunRewireTests} drives, and the one that matters
 * in production: it edits a source file, calls reloadSources, and expects the edited type back. The sibling
 * {@link TestReloadSources} covers the in-memory path used by tests.
 */
public class TestReloadSourcesFromDisk {

    @Language("java")
    private static final String A = """
            package a.b;
            public class A {
                public int method() { return 1; }
            }
            """;

    @Language("java")
    private static final String B = """
            package a.b;
            public class B {
                public A a() { return new A(); }
            }
            """;

    @TempDir
    Path sourceDir;

    private JavaInspector javaInspector;
    private Path fileA;

    @BeforeEach
    public void before() throws IOException {
        Path pkg = Files.createDirectories(sourceDir.resolve("a/b"));
        fileA = Files.writeString(pkg.resolve("A.java"), A);
        Files.writeString(pkg.resolve("B.java"), B);
        javaInspector = new JavaInspectorImpl(true, false);
        javaInspector.initialize(inputConfiguration());
    }

    private InputConfiguration inputConfiguration() {
        SourceSet sourceSet = new SourceSetImpl.Builder()
                .setName("main")
                .setSourceDirectories(List.of(sourceDir))
                .setUri(sourceDir.toUri())
                .build();
        return new InputConfigurationImpl.Builder()
                .addSourceSets(sourceSet)
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .build();
    }

    // no in-memory sources: the inspector walks the source set's directories
    private ParseResult parseFromDisk() {
        return javaInspector.parse(Map.of(), JavaInspectorImpl.DETAILED_SOURCES).parseResult();
    }

    @DisplayName("untouched files on disk: nothing reported as changed")
    @Test
    public void testUnchangedOnDisk() throws IOException {
        ParseResult pr = parseFromDisk();
        assertEquals(2, pr.primaryTypes().size(), "expected A and B from disk, got " + pr.primaryTypes());
        assertEquals(2, javaInspector.sourceFiles().size());

        JavaInspector.ReloadResult rr = javaInspector.reloadSources(inputConfiguration(), Map.of());
        assertEquals(0, rr.problems().size(), rr.problems().toString());
        assertTrue(rr.sourceHasChanged().isEmpty(), "expected nothing changed, got " + rr.sourceHasChanged());
    }

    @DisplayName("a file edited on disk: exactly its type comes back, as RunRewireTests expects")
    @Test
    public void testEditedOnDisk() throws IOException {
        ParseResult pr = parseFromDisk();
        TypeInfo a = pr.findType("a.b.A");
        assertNotNull(a);

        // exactly what RunRewireTests does to trigger a fingerprint change
        Files.writeString(fileA, "// some comment\n\n" + A);

        JavaInspector.ReloadResult rr = javaInspector.reloadSources(inputConfiguration(), Map.of());
        assertEquals(0, rr.problems().size(), rr.problems().toString());
        assertEquals(1, rr.sourceHasChanged().size(), "expected only A, got " + rr.sourceHasChanged());
        assertSame(a, rr.sourceHasChanged().iterator().next(),
                "RunRewireTests asserts rr.sourceHasChanged().contains(the type it edited)");
    }

    @DisplayName("a file deleted from disk drops out of sourceFiles")
    @Test
    public void testDeletedOnDisk() throws IOException {
        parseFromDisk();
        assertEquals(2, javaInspector.sourceFiles().size());

        Files.delete(fileA);

        JavaInspector.ReloadResult rr = javaInspector.reloadSources(inputConfiguration(), Map.of());
        assertTrue(rr.sourceHasChanged().isEmpty(), rr.sourceHasChanged().toString());
        assertEquals(1, javaInspector.sourceFiles().size(), "the deleted file must be gone");
        assertEquals("a/b/B.java", javaInspector.sourceFiles().iterator().next().path());
    }

    @DisplayName("a file added on disk is registered for the next parse")
    @Test
    public void testAddedOnDisk() throws IOException {
        parseFromDisk();
        Files.writeString(sourceDir.resolve("a/b/C.java"), "package a.b;\npublic class C { }\n");

        JavaInspector.ReloadResult rr = javaInspector.reloadSources(inputConfiguration(), Map.of());
        assertTrue(rr.sourceHasChanged().isEmpty(), "a new file has no types to report: " + rr.sourceHasChanged());
        assertEquals(3, javaInspector.sourceFiles().size());
        assertTrue(javaInspector.sourceFiles().stream().anyMatch(sf -> "a/b/C.java".equals(sf.path())),
                javaInspector.sourceFiles().toString());
    }
}
