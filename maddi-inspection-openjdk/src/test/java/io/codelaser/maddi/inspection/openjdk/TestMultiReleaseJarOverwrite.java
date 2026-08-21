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

import io.codelaser.maddi.cst.api.element.CompilationUnit;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.impl.runtime.RuntimeImpl;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.Summary;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.InfoByFqn;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;

import static io.codelaser.maddi.inspection.api.integration.JavaInspector.TEST_PROTOCOL;
import static org.junit.jupiter.api.Assertions.*;

/**
 * A multi-release jar presents the same FQN through TWO class-file entries — {@code x/Y.class} and
 * {@code META-INF/versions/N/x/Y.class} — and javac selects between them PER SOURCE SET, because
 * {@code --release} is per source set since {@link SourceSet#sourceRelease()}. So two source sets with
 * different release levels legitimately resolve one external type to two different URIs in one jar, the
 * reload in {@code ClassSymbolScanner.classTypeInfo} ("the javac analysis will have selected the first")
 * fires, and {@code InfoByFqn.put}'s overwrite branch sees prev and next in the SAME source set — the
 * state its assert declared impossible.
 * <p>
 * Found by the first corpus parse ever run with {@code -ea} (opensearch, {@code bc-fips-2.1.3.jar},
 * 2026-08-21): three units dropped, the parse refused, before assertions had run over any corpus the
 * assert had only ever seen unit-test geometry. The rule: a same-set overwrite is legal exactly when the
 * compilation-unit URIs differ (same jar, different entry); a same-set same-URI re-commit stays a defect.
 */
public class TestMultiReleaseJarOverwrite {

    // ------------------------------------------------------------------ unit level: InfoByFqn.put

    private static SourceSet externalJarSet(URI uri) {
        // ⚠ the name IS the lookup key: ensureSourceSet resolves a lazily met jar type by the jar FILENAME
        return new SourceSetImpl.Builder().setName("mr-lib.jar").setUri(uri)
                .setLibrary(true).setExternalLibrary(true)
                .setDependencies(List.of(SourceSetImpl.javaBase())).build();
    }

    private static TypeInfo typeIn(Runtime runtime, SourceSet set, String entryUri) {
        CompilationUnit cu = runtime.newCompilationUnitBuilder().setPackageName("a.b").setSourceSet(set)
                .setURI(URI.create(entryUri)).build();
        return runtime.newTypeInfo(cu, "Api");
    }

    @DisplayName("same source set, different class-file entry: the multi-release overwrite is tolerated")
    @Test
    public void sameSetDifferentUriIsTheMultiReleaseOverwrite() {
        Runtime runtime = new RuntimeImpl();
        SourceSet jarSet = externalJarSet(URI.create("file:/lib/mr-lib.jar"));
        SourceSet task = new SourceSetImpl.Builder().setName("main").setUri(URI.create("file:/main/"))
                .setDependencies(List.of(SourceSetImpl.javaBase())).build();
        InfoByFqn registry = new InfoByFqn();

        TypeInfo base = typeIn(runtime, jarSet, "jar:file:/lib/mr-lib.jar!/a/b/Api.class");
        registry.put(base.fullyQualifiedName(), base, task);
        TypeInfo versioned = typeIn(runtime, jarSet, "jar:file:/lib/mr-lib.jar!/META-INF/versions/9/a/b/Api.class");
        registry.put(versioned.fullyQualifiedName(), versioned, task);

        assertSame(versioned, registry.getType("a.b.Api", task), "last write wins, as it always has with -da");
    }

    @DisplayName("same source set, SAME entry: re-committing the identical type is still a defect")
    @Test
    public void sameSetSameUriIsStillADefect() {
        // this test only tests something when -ea is on; Gradle's Test task enables it by default
        boolean assertionsOn = false;
        assert assertionsOn = true;
        assertTrue(assertionsOn, "run with -ea, or this test asserts nothing");

        Runtime runtime = new RuntimeImpl();
        SourceSet jarSet = externalJarSet(URI.create("file:/lib/mr-lib.jar"));
        SourceSet task = new SourceSetImpl.Builder().setName("main").setUri(URI.create("file:/main/"))
                .setDependencies(List.of(SourceSetImpl.javaBase())).build();
        InfoByFqn registry = new InfoByFqn();

        TypeInfo first = typeIn(runtime, jarSet, "jar:file:/lib/mr-lib.jar!/a/b/Api.class");
        registry.put(first.fullyQualifiedName(), first, task);
        TypeInfo again = typeIn(runtime, jarSet, "jar:file:/lib/mr-lib.jar!/a/b/Api.class");
        assertThrows(AssertionError.class, () -> registry.put(again.fullyQualifiedName(), again, task),
                "nothing decides a reload for an identical entry, so someone committed the same type twice");
    }

    // ------------------------------------------------------- end to end: the jar, two release levels

    @Language("java")
    private static final String API = """
            package a.b;
            public class Api {
                public String name() { return "api"; }
            }
            """;

    @Language("java")
    private static final String USE_A = """
            package p.q;
            public class UseA {
                public a.b.Api hold(a.b.Api x) { return x; }
            }
            """;

    @Language("java")
    private static final String USE_B = """
            package r.s;
            public class UseB {
                public a.b.Api hold(a.b.Api x) { return x; }
            }
            """;

    /** base entries compiled at --release 9, {@code META-INF/versions/11} entries at --release 11. */
    private static Path multiReleaseJar(Path tmp) throws IOException {
        Path javaFile = tmp.resolve("src/a/b/Api.java");
        Files.createDirectories(javaFile.getParent());
        Files.writeString(javaFile, API);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "this test needs a JDK, not a JRE");
        Path base = Files.createDirectories(tmp.resolve("base"));
        Path v11 = Files.createDirectories(tmp.resolve("v11"));
        assertEquals(0, compiler.run(null, null, null,
                "--release", "9", "-d", base.toString(), javaFile.toString()));
        assertEquals(0, compiler.run(null, null, null,
                "--release", "11", "-d", v11.toString(), javaFile.toString()));

        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().putValue("Multi-Release", "true");
        Path jar = tmp.resolve("mr-lib.jar");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jar), manifest)) {
            addTree(jos, base, "");
            addTree(jos, v11, "META-INF/versions/11/");
        }
        return jar;
    }

    private static void addTree(JarOutputStream jos, Path classes, String prefix) throws IOException {
        try (Stream<Path> walk = Files.walk(classes)) {
            for (Path p : walk.filter(Files::isRegularFile).toList()) {
                jos.putNextEntry(new JarEntry(prefix + classes.relativize(p).toString().replace('\\', '/')));
                jos.write(Files.readAllBytes(p));
                jos.closeEntry();
            }
        }
    }

    @DisplayName("one MR jar, two source sets at two release levels: the parse must survive it")
    @Test
    public void multiReleaseJarAcrossTwoReleaseLevels() throws Exception {
        Path tmp = Files.createTempDirectory("mrjar-");
        try {
            Path jar = multiReleaseJar(tmp);

            SourceSet javaBase = SourceSetImpl.javaBase();
            SourceSet mrJar = externalJarSet(jar.toUri());
            // A resolves a.b.Api to the BASE entry, B to META-INF/versions/11 -- same jar, two URIs
            SourceSet setA = new SourceSetImpl.Builder().setName(TEST_PROTOCOL).setUri(URI.create("file:/a/"))
                    .setSourceRelease(9)
                    .setDependencies(List.of(javaBase, mrJar)).build();
            SourceSet setB = new SourceSetImpl.Builder().setName(TEST_PROTOCOL + "B").setUri(URI.create("file:/b/"))
                    .setSourceRelease(17)
                    .setDependencies(List.of(javaBase, mrJar)).build();

            InputConfiguration ic = new InputConfigurationImpl.Builder()
                    .addClassPathParts(javaBase, mrJar)
                    .addSourceSets(setA, setB)
                    .build();
            JavaInspector javaInspector = new JavaInspectorImpl();
            javaInspector.initialize(ic);

            Map<SourceSet, Map<String, String>> sources = new LinkedHashMap<>();
            sources.put(setA, Map.of("p.q.UseA", USE_A));
            sources.put(setB, Map.of("r.s.UseB", USE_B));

            Summary summary = javaInspector.parseMultiSourceSet(sources, JavaInspectorImpl.DETAILED_SOURCES);
            assertTrue(summary.parseExceptions().isEmpty(),
                    "an MR jar on the classpath of two release levels must not drop units: "
                    + summary.parseExceptions().stream().map(e -> String.valueOf(e.getMessage())).toList());
            assertDoesNotThrow(summary::parseResult);
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
