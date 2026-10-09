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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

import static io.codelaser.maddi.inspection.api.integration.JavaInspector.TEST_PROTOCOL;
import static org.junit.jupiter.api.Assertions.*;

/**
 * A library type committed twice from the same class file: "type io.micronaut.http.client.HttpClient committed
 * twice from jar:file:…/micronaut-http-client-core-4.9.9.jar!/io/micronaut/http/client/HttpClient.class", a
 * 225-source-set project parsed in a server under {@code -ea} (2026-10-09). Every loader looks the type up before
 * loading it, so one thread cannot do this; two can, when both miss the shared {@link InfoByFqn} and both load.
 * {@link InfoByFqn#putIfAbsentFromSameOrigin} makes the check and the registration one step, and
 * {@code ClassSymbolScanner.lazilyLoadPrimaryTypeFromClassFile} hands back the instance that won.
 */
public class TestConcurrentClassFileLoad {

    private static SourceSet jarSet(URI uri) {
        return new SourceSetImpl.Builder().setName("lib.jar").setUri(uri)
                .setLibrary(true).setExternalLibrary(true)
                .setDependencies(List.of(SourceSetImpl.javaBase())).build();
    }

    private static TypeInfo typeIn(Runtime runtime, SourceSet set, String entryUri) {
        CompilationUnit cu = runtime.newCompilationUnitBuilder().setPackageName("a.b").setSourceSet(set)
                .setURI(URI.create(entryUri)).build();
        return runtime.newTypeInfo(cu, "Api");
    }

    private static SourceSet task(String name) {
        return new SourceSetImpl.Builder().setName(name).setUri(URI.create("file:/" + name + "/"))
                .setDependencies(List.of(SourceSetImpl.javaBase())).build();
    }

    @DisplayName("same origin: the registered instance comes back, the new one is not registered")
    @Test
    public void sameOriginReturnsTheRegisteredType() {
        Runtime runtime = new RuntimeImpl();
        SourceSet jar = jarSet(URI.create("file:/lib/lib.jar"));
        SourceSet main = task("main");
        InfoByFqn registry = new InfoByFqn();

        TypeInfo first = typeIn(runtime, jar, "jar:file:/lib/lib.jar!/a/b/Api.class");
        assertSame(first, registry.putIfAbsentFromSameOrigin("a.b.Api", first, main));
        TypeInfo again = typeIn(runtime, jar, "jar:file:/lib/lib.jar!/a/b/Api.class");
        assertSame(first, registry.putIfAbsentFromSameOrigin("a.b.Api", again, task("test")));
        assertSame(first, registry.getType("a.b.Api", main));
    }

    @DisplayName("another entry of the same jar (multi-release) is still a reload, as put does it")
    @Test
    public void otherOriginIsPut() {
        Runtime runtime = new RuntimeImpl();
        SourceSet jar = jarSet(URI.create("file:/lib/lib.jar"));
        SourceSet main = task("main");
        InfoByFqn registry = new InfoByFqn();

        TypeInfo base = typeIn(runtime, jar, "jar:file:/lib/lib.jar!/a/b/Api.class");
        registry.putIfAbsentFromSameOrigin("a.b.Api", base, main);
        TypeInfo versioned = typeIn(runtime, jar, "jar:file:/lib/lib.jar!/META-INF/versions/11/a/b/Api.class");
        assertSame(versioned, registry.putIfAbsentFromSameOrigin("a.b.Api", versioned, main));
        assertSame(versioned, registry.getType("a.b.Api", main));
    }

    @DisplayName("many threads load one class file at once: one instance, no 'committed twice'")
    @Test
    public void concurrentLoadersAgreeOnOneInstance() throws Exception {
        Runtime runtime = new RuntimeImpl();
        SourceSet jar = jarSet(URI.create("file:/lib/lib.jar"));
        int threads = 16;
        int rounds = 200;
        try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
            for (int round = 0; round < rounds; round++) {
                InfoByFqn registry = new InfoByFqn();
                CountDownLatch start = new CountDownLatch(1);
                List<Future<TypeInfo>> futures = new ArrayList<>();
                for (int t = 0; t < threads; t++) {
                    SourceSet task = task("set" + t);
                    TypeInfo mine = typeIn(runtime, jar, "jar:file:/lib/lib.jar!/a/b/Api.class");
                    futures.add(executor.submit(() -> {
                        start.await();
                        // each thread also registers types of its own, so the maps resize under the race
                        for (int i = 0; i < 50; i++) {
                            TypeInfo other = runtime.newTypeInfo(runtime.newCompilationUnitBuilder()
                                    .setPackageName("x" + i).setSourceSet(jar)
                                    .setURI(URI.create("jar:file:/lib/lib.jar!/x" + i + "/" + task.name() + ".class"))
                                    .build(), task.name());
                            registry.putIfAbsentFromSameOrigin(other.fullyQualifiedName(), other, task);
                        }
                        return registry.putIfAbsentFromSameOrigin("a.b.Api", mine, task);
                    }));
                }
                start.countDown();
                Set<TypeInfo> winners = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
                for (Future<TypeInfo> f : futures) winners.add(f.get()); // an AssertionError surfaces here
                assertEquals(1, winners.size(), "every loader must get the same instance");
                assertSame(winners.iterator().next(), registry.getType("a.b.Api", task("set0")));
            }
        }
    }

    // ------------------------------------------------------- end to end: one jar, two source sets, -ea

    @Language("java")
    private static final String API = """
            package a.b;
            public interface Api {
                String name();
            }
            """;

    @Language("java")
    private static final String USE_A = """
            package p.q;
            public class UseA {
                public String hold(a.b.Api x) { return x.name(); }
            }
            """;

    @Language("java")
    private static final String USE_B = """
            package r.s;
            public class UseB {
                public a.b.Api hold(a.b.Api x) { return x; }
            }
            """;

    private static Path jar(Path tmp) throws IOException {
        Path javaFile = tmp.resolve("src/a/b/Api.java");
        Files.createDirectories(javaFile.getParent());
        Files.writeString(javaFile, API);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "this test needs a JDK, not a JRE");
        Path classes = Files.createDirectories(tmp.resolve("classes"));
        assertEquals(0, compiler.run(null, null, null, "-d", classes.toString(), javaFile.toString()));
        Path jar = tmp.resolve("lib.jar");
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

    @DisplayName("two source sets using one jar type, parsed under -ea: one TypeInfo, no parse error")
    @Test
    public void twoSourceSetsOneJarType() throws Exception {
        boolean assertionsOn = false;
        assert assertionsOn = true;
        assertTrue(assertionsOn, "run with -ea, or this test asserts nothing");

        Path tmp = Files.createTempDirectory("onejar-");
        try {
            SourceSet javaBase = SourceSetImpl.javaBase();
            SourceSet lib = jarSet(jar(tmp).toUri());
            SourceSet setA = new SourceSetImpl.Builder().setName(TEST_PROTOCOL).setUri(URI.create("file:/a/"))
                    .setDependencies(List.of(javaBase, lib)).build();
            SourceSet setB = new SourceSetImpl.Builder().setName(TEST_PROTOCOL + "B").setUri(URI.create("file:/b/"))
                    .setDependencies(List.of(javaBase, lib)).build();
            InputConfiguration ic = new InputConfigurationImpl.Builder()
                    .addClassPathParts(javaBase, lib)
                    .addSourceSets(setA, setB)
                    .build();
            JavaInspector javaInspector = new JavaInspectorImpl();
            javaInspector.initialize(ic);

            Map<SourceSet, Map<String, String>> sources = new LinkedHashMap<>();
            sources.put(setA, Map.of("p.q.UseA", USE_A));
            sources.put(setB, Map.of("r.s.UseB", USE_B));
            Summary summary = javaInspector.parseMultiSourceSet(sources, JavaInspectorImpl.DETAILED_SOURCES);
            assertTrue(summary.parseExceptions().isEmpty(), () -> summary.parseExceptions().stream()
                    .map(e -> String.valueOf(e.getMessage())).toList().toString());

            TypeInfo useA = summary.parseResult().findType("p.q.UseA");
            TypeInfo useB = summary.parseResult().findType("r.s.UseB");
            TypeInfo apiA = useA.findUniqueMethod("hold", 1).parameters().getFirst().parameterizedType().typeInfo();
            TypeInfo apiB = useB.findUniqueMethod("hold", 1).returnType().typeInfo();
            assertSame(apiA, apiB, "both source sets must share the one TypeInfo of a.b.Api");
            assertTrue(apiA.hasBeenInspected());
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
