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
import io.codelaser.maddi.inspection.api.parser.ParseResult;
import io.codelaser.maddi.inspection.api.parser.Summary;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.intellij.lang.annotations.Language;
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
 * {@link InputConfiguration#withSupportFromClasspath(Map)} and its {@code withMaddiSupportFromClasspath()}
 * shorthand: the API for putting a library that is already on the running process' class path onto the
 * inspected class path, without knowing where it lives or which version it is.
 * <p>
 * It could not work as written. It builds each support set with the {@code jar-on-classpath:} prefix on the
 * <b>URI</b> and the caller's map key as the <b>name</b> — the key being the whole point of the parameter —
 * while all three recognition sites tested the name:
 * {@code ClassSymbolScanner}'s constructor, and {@code JavaInspectorImpl}'s javac class path assembly and
 * {@code resolveJarOnClassPathDependencies}. So the set was never recognised, fell through to
 * {@code Path.of(uri)} on an opaque URI, and died with
 * {@code FileSystemNotFoundException: Provider "jar-on-classpath" not installed} — before a single type was
 * looked at. Both entry points were affected, and neither had a caller in this repository, which is how it
 * survived.
 * <p>
 * The sites now ask {@link InputConfiguration#jarOnClasspathSelector} instead, which accepts the prefix on
 * either the name or the URI, so both constructions work and callers keep their naming freedom.
 */
public class TestWithSupportFromClasspath {

    @Language("java")
    private static final String SOURCE = """
            package a;
            import io.codelaser.maddi.annotation.ImmutableContainer;
            @ImmutableContainer
            public record C(int k) {
                int kSquared() { return k * k; }
            }
            """;

    @TempDir
    Path root;

    /**
     * A source set with real file dependencies, which is what makes this reproduce: the javac class path is
     * only overridden when there is at least one file dependency, and it is that loop which used to call
     * {@code Path.of} on the unrecognised support entry.
     */
    private InputConfiguration base() throws IOException {
        Path src = Files.createDirectories(root.resolve("src/a"));
        Files.writeString(src.resolve("C.java"), SOURCE);
        return new InputConfigurationImpl.Builder()
                .addSource("main", root.resolve("src").toString())
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .build();
    }

    private void assertAnnotationResolves(InputConfiguration inputConfiguration) throws IOException {
        JavaInspector javaInspector = new JavaInspectorImpl();
        javaInspector.initialize(inputConfiguration);
        Summary summary = javaInspector.parse(JavaInspectorImpl.DETAILED_SOURCES);
        assertFalse(summary.haveErrors(), "" + summary.parseWarnings());
        ParseResult parseResult = summary.parseResult();
        TypeInfo c = parseResult.findType("a.C");
        assertEquals("@ImmutableContainer", c.annotations().getFirst().toString(),
                "the annotation resolved through the support set, so it is a type and not a stub");
        assertTrue(javaInspector.runtime()
                        .getFullyQualified("io.codelaser.maddi.annotation.ImmutableContainer", true)
                        .typeNature().isAnnotation(),
                "and it really is the annotation type");
    }

    @DisplayName("withMaddiSupportFromClasspath: the shorthand resolves io.codelaser.maddi.annotation")
    @Test
    public void testMaddiShorthand() throws IOException {
        assertAnnotationResolves(base().withMaddiSupportFromClasspath());
    }

    @DisplayName("withSupportFromClasspath: the caller's own name for the set, and it still resolves")
    @Test
    public void testExplicitMap() throws IOException {
        assertAnnotationResolves(base()
                .withSupportFromClasspath(Map.of("theAnnotations", "io/codelaser/maddi/annotation")));
    }

    /**
     * The name the caller chose is kept — that is the difference between fixing this at the recognition
     * sites and fixing it by overwriting the name with the prefix, which would have made the map key
     * decorative.
     */
    @DisplayName("the map key names the source set; the prefix stays on the URI")
    @Test
    public void testTheKeyIsTheName() throws IOException {
        InputConfiguration ic = base()
                .withSupportFromClasspath(Map.of("theAnnotations", "io/codelaser/maddi/annotation"));
        SourceSet support = ic.classPathParts().stream()
                .filter(s -> "theAnnotations".equals(s.name()))
                .findFirst().orElseThrow(() -> new AssertionError("the set is named after the map key: "
                                                                  + ic.classPathParts()));
        assertEquals(URI.create("jar-on-classpath:io/codelaser/maddi/annotation"), support.uri());
        assertEquals("io/codelaser/maddi/annotation", InputConfiguration.jarOnClasspathSelector(support));
        // and every source set depends on it, which is what put it in front of the javac class path loop
        assertTrue(ic.sourceSets().stream().allMatch(s -> s.dependencies().contains(support)),
                "" + ic.sourceSets());
    }

    /** The other construction, which always worked, must keep working: the prefix on the NAME. */
    @DisplayName("the prefix on the name is still recognised")
    @Test
    public void testPrefixOnTheName() {
        SourceSet onName = new SourceSetImpl.Builder()
                .setName(JavaInspectorImpl.JAR_WITH_PATH_PREFIX + "io/codelaser/maddi/annotation")
                .setUri(URI.create("file:/nowhere")).build();
        assertEquals("io/codelaser/maddi/annotation", InputConfiguration.jarOnClasspathSelector(onName));

        SourceSet neither = new SourceSetImpl.Builder().setName("plain")
                .setUri(URI.create("file:/somewhere.jar")).build();
        assertNull(InputConfiguration.jarOnClasspathSelector(neither));
        assertNull(InputConfiguration.jarOnClasspathSelector(null));
    }

    /** {@code List.of()} would not carry a null, and the JDK entries have no prefix anywhere. */
    @DisplayName("a jmod entry is not a jar-on-classpath entry")
    @Test
    public void testJmodIsNotOne() throws IOException {
        base().classPathParts().forEach(cpp ->
                assertNull(InputConfiguration.jarOnClasspathSelector(cpp), "" + cpp.name()));
    }
}
