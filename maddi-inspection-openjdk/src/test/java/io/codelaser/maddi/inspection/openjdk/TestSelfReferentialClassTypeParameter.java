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
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.Summary;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

import static io.codelaser.maddi.inspection.api.integration.JavaInspector.TEST_PROTOCOL;
import static org.junit.jupiter.api.Assertions.*;

/**
 * A class-file type whose class type parameter is bounded by its own type,
 * {@code AbstractClassLoaderValue<CLV extends AbstractClassLoaderValue<CLV, V>, V>}, and whose member class
 * {@code Sub} mentions the enclosing type's {@code CLV}.
 * <p>
 * Measured on Apache Ignite (2026-09-14), whose source sets export {@code java.base/jdk.internal.loader}:
 * committing {@code jdk.internal.loader.ClassLoaderValue} from the JDK failed with
 * <i>"Type parameter 'CLV' not found in jdk.internal.loader.AbstractClassLoaderValue, nor in any enclosing type"</i>.
 */
public class TestSelfReferentialClassTypeParameter {

    /** The JDK's shape, verbatim from {@code javap} (JDK 17), minus the bodies that do not matter. */
    @Language("java")
    private static final String ABSTRACT_VALUE = """
            package lib;
            public abstract class AbstractValue<CLV extends AbstractValue<CLV, V>, V> {
                AbstractValue() { }
                public abstract Object key();
                public <K> Sub<K> sub(K key) { return new Sub<>(key); }
                public abstract boolean isEqualOrDescendantOf(AbstractValue<?, V> clv);
                public V get(ClassLoader cl) { return null; }
                public V computeIfAbsent(ClassLoader cl,
                        java.util.function.BiFunction<? super ClassLoader, ? super CLV, ? extends V> f) { return null; }
                private static <CLV extends AbstractValue<CLV, ?>> java.util.Map<CLV, Object> map(ClassLoader cl) {
                    return null;
                }
                public final class Sub<K> extends AbstractValue<Sub<K>, V> {
                    private final K key;
                    Sub(K key) { this.key = key; }
                    public AbstractValue<CLV, V> parent() { return AbstractValue.this; }
                    @Override public K key() { return key; }
                    @Override public boolean isEqualOrDescendantOf(AbstractValue<?, V> clv) { return false; }
                }
            }
            """;

    @Language("java")
    private static final String VALUE = """
            package lib;
            public final class Value<V> extends AbstractValue<Value<V>, V> {
                public Value() { }
                @Override public Value<V> key() { return this; }
                @Override public boolean isEqualOrDescendantOf(AbstractValue<?, V> clv) { return equals(clv); }
            }
            """;

    @Language("java")
    private static final String USER = """
            package p;
            public class User {
                private final lib.Value<String> value = new lib.Value<>();
                String get(ClassLoader cl) { return value.get(cl); }
            }
            """;

    @Language("java")
    private static final String JDK_USER = """
            package p;
            import jdk.internal.loader.ClassLoaderValue;
            public class JdkUser {
                private final ClassLoaderValue<String> value = new ClassLoaderValue<>();
                String get(ClassLoader cl) { return value.get(cl); }
            }
            """;

    @TempDir
    Path tmp;

    private Path libraryJar() throws IOException {
        Path src = Files.createDirectories(tmp.resolve("src/lib"));
        Files.writeString(src.resolve("AbstractValue.java"), ABSTRACT_VALUE);
        Files.writeString(src.resolve("Value.java"), VALUE);
        Path classes = Files.createDirectories(tmp.resolve("classes"));
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertEquals(0, compiler.run(null, null, null, "-d", classes.toString(),
                src.resolve("AbstractValue.java").toString(), src.resolve("Value.java").toString()));
        Path jar = tmp.resolve("lib.jar");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jar));
             Stream<Path> walk = Files.walk(classes)) {
            for (Path file : walk.filter(Files::isRegularFile).toList()) {
                jos.putNextEntry(new JarEntry(classes.relativize(file).toString().replace('\\', '/')));
                jos.write(Files.readAllBytes(file));
                jos.closeEntry();
            }
        }
        return jar;
    }

    private static List<String> problems(Summary summary) {
        List<String> all = new ArrayList<>();
        summary.parseExceptions().forEach(e -> all.add(String.valueOf(e.getMessage())));
        summary.parseWarnings().forEach(e -> all.add(String.valueOf(e.getMessage())));
        return all;
    }

    @DisplayName("a library type extending a self-bounded generic parent commits, with its members")
    @Test
    public void libraryAnalogue() throws IOException {
        Path jar = libraryJar();
        SourceSet javaBase = SourceSetImpl.javaBase();
        SourceSet lib = new SourceSetImpl.Builder().setName("lib.jar").setUri(jar.toUri())
                .setLibrary(true).setExternalLibrary(true).setDependencies(List.of(javaBase)).build();
        SourceSet set = new SourceSetImpl.Builder().setName(TEST_PROTOCOL).setUri(URI.create("file:/a/"))
                .setDependencies(List.of(javaBase, lib)).build();
        InputConfiguration ic = new InputConfigurationImpl.Builder().addClassPathParts(javaBase, lib)
                .addSourceSets(set).build();
        JavaInspector javaInspector = new JavaInspectorImpl();
        javaInspector.initialize(ic);

        Summary summary = javaInspector.parse(Map.of("p.User", USER),
                new JavaInspector.ParseOptions.Builder().setFailFast(false).setDetailedSources(true).build());

        assertEquals(List.of(), problems(summary));
        TypeInfo value = javaInspector.compiledTypesManager().type("lib.Value", null);
        assertNotNull(value);
        assertEquals(List.of("CLV", "V"), value.parentClass().typeInfo().typeParameters().stream()
                .map(tp -> tp.simpleName()).toList());
    }

    @DisplayName("the JDK's own jdk.internal.loader.ClassLoaderValue commits, with the package exported")
    @Test
    public void theJdkType() throws IOException {
        SourceSet set = new SourceSetImpl.Builder().setName(TEST_PROTOCOL).setUri(URI.create("file:/"))
                .setAddExports(List.of("java.base/jdk.internal.loader=ALL-UNNAMED")).build();
        InputConfiguration ic = new InputConfigurationImpl.Builder().addSourceSets(set)
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES).build();
        JavaInspector javaInspector = new JavaInspectorImpl();
        javaInspector.initialize(ic);

        Summary summary = javaInspector.parse(Map.of("p.JdkUser", JDK_USER),
                new JavaInspector.ParseOptions.Builder().setFailFast(false).setDetailedSources(true).build());

        assertEquals(List.of(), problems(summary), "neither the unit nor the JDK type may fail");
        assertTrue(summary.types().stream().anyMatch(t -> "p.JdkUser".equals(t.fullyQualifiedName())),
                "the unit that uses the exported internal type must not be dropped");
        TypeInfo clv = javaInspector.compiledTypesManager().type("jdk.internal.loader.ClassLoaderValue", null);
        assertNotNull(clv);
        assertEquals(List.of("V"), clv.typeParameters().stream().map(tp -> tp.simpleName()).toList(),
                "a stub has no type parameters, which is what 'V not found' was");
        assertEquals(List.of("CLV", "V"), clv.parentClass().typeInfo().typeParameters().stream()
                .map(tp -> tp.simpleName()).toList());
    }

    @Language("java")
    private static final String JFR_USER = """
            package p;
            public class Recorded extends jdk.jfr.Event { }
            """;

    /** Only the stub branch of {@code lazilyLoadPrimaryTypeFromClassFile} gives a type this URI. */
    private static final URI STUB = URI.create("jrt:/internal/");

    /**
     * CONTROL: the stub policy for internal packages NOBODY exports is unchanged — loading every {@code jdk.internal}
     * type is what {@code --jdk-internals} is for. {@code jdk.jfr.Event} extends {@code jdk.internal.event.Event}
     * (the pulsar shape in {@code lazilyLoadPrimaryTypeFromClassFile}), so that internal type is certainly loaded,
     * and with only {@code jdk.internal.loader} exported it must still be a stub. ⚠ An earlier version of this
     * control probed a type the parse never loaded, and passed on {@code null}.
     */
    @DisplayName("CONTROL: an internal package no source set exports is still left as a stub")
    @Test
    public void anUnexportedInternalPackageStaysAStub() throws IOException {
        SourceSet set = new SourceSetImpl.Builder().setName(TEST_PROTOCOL).setUri(URI.create("file:/"))
                .setAddExports(List.of("java.base/jdk.internal.loader=ALL-UNNAMED")).build();
        InputConfiguration ic = new InputConfigurationImpl.Builder().addSourceSets(set)
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES).addClassPath("jmod:jdk.jfr").build();
        JavaInspector javaInspector = new JavaInspectorImpl();
        javaInspector.initialize(ic);
        Summary summary = javaInspector.parse(Map.of("p.JdkUser", JDK_USER, "p.Recorded", JFR_USER),
                new JavaInspector.ParseOptions.Builder().setFailFast(false).setDetailedSources(true).build());
        assertEquals(List.of(), problems(summary));

        TypeInfo internalEvent = javaInspector.compiledTypesManager().typeIfLoaded("jdk.internal.event.Event", null);
        assertNotNull(internalEvent, "the control only means something if the internal type was loaded");
        assertEquals(STUB, internalEvent.compilationUnit().uri(), "jdk.internal.event is not exported: a stub");
        TypeInfo clv = javaInspector.compiledTypesManager().typeIfLoaded("jdk.internal.loader.ClassLoaderValue", null);
        assertNotNull(clv);
        assertNotEquals(STUB, clv.compilationUnit().uri(), "jdk.internal.loader IS exported: loaded, not stubbed");
    }
}
