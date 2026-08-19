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

package io.codelaser.maddi.run.mvnplugin;

import io.codelaser.maddi.cst.api.element.SourceSet;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Maven plugin's dependency walk: which resolved artifacts end up on which of the two class paths, and under
 * what name.
 *
 * <p><b>The plugin had no tests at all</b> until this file. What it is measured against is not this fixture but
 * {@code --compile-log}: the same corpus, both routes, and the source set's dependency list diffed. That A/B is
 * what found both defects below; these tests exist so a fix cannot silently come undone between corpus runs.
 *
 * <p>⚠ <b>WHAT THIS FIXTURE SUPPLIES, PRODUCTION COMPUTES</b> -- the graph, with a scope on every node. Aether
 * derives that scope from a node's whole path (a compile dependency of a test dependency is itself test-scoped),
 * and no fixture can prove it does. Only the corpus can, and does: with the filter applied, timefold-solver's
 * {@code core/main} comes back to the 12 dependencies javac's own {@code -classpath} names, from 60.
 */
public class TestComputeClassPathParts {

    /**
     * ⛔⛔ THE TEST TOOLCHAIN IS NOT ON THE MAIN CLASS PATH.
     *
     * <p>The scope filter was computed and thrown away, so every scope pass walked the same unfiltered graph and
     * main received the union of all of them.
     *
     * <p>⚠ MEASURED on timefold-solver (2026-08-19): {@code core/main} carried 60 dependencies against javac's
     * 12 -- junit, mockito, assertj, awaitility, hamcrest, logback and the JAXB runtime, none of which the module
     * is compiled against. It cost no error, which is why it survived: a class path that is too wide only lets a
     * type resolve that a stricter build would have refused.
     */
    @Test
    public void scopeDecidesWhichClassPathAnArtifactReaches(@TempDir Path dir) throws IOException {
        DependencyNode root = root(
                jarNode(dir, "org.example:api:1.0", JavaScopes.COMPILE),
                jarNode(dir, "org.example:servlet:1.0", JavaScopes.PROVIDED),
                jarNode(dir, "org.example:logback:1.0", JavaScopes.RUNTIME),
                jarNode(dir, "org.example:junit:1.0", JavaScopes.TEST));

        assertEquals(Set.of("api-1.0.jar", "servlet-1.0.jar"), names(walk(root, JavaScopes.COMPILE, false)),
                "provided is on javac's compile class path; runtime and test are not");
        assertEquals(Set.of("api-1.0.jar", "servlet-1.0.jar", "logback-1.0.jar", "junit-1.0.jar"),
                names(walk(root, JavaScopes.TEST, true)),
                "the test class path is everything -- maven-compiler-plugin's getTestClasspathElements");
    }

    /**
     * ⚠ AND THE ORDINARY CASE MUST NOT MOVE. Every checked-in corpus configuration names its jars by file name,
     * and maddi's own {@code --write-input-configuration} writes them that way, so the coordinate prefix above
     * has to stay off a jar.
     */
    @Test
    public void jarsKeepTheirFileName(@TempDir Path dir) throws IOException {
        DependencyNode root = root(jarNode(dir, "org.example:api:1.0", JavaScopes.COMPILE));
        assertEquals(Set.of("api-1.0.jar"), names(walk(root, JavaScopes.COMPILE, false)));
    }

    /**
     * A class path is flat. A transitive dependency is a direct dependency of the source set, because nesting it
     * under its parent -- with the dedup by name -- would drop it from that parent's child set the second time it
     * is reached, leaving it unreachable when maddi walks the graph.
     */
    @Test
    public void transitiveDependenciesAreFlattened(@TempDir Path dir) throws IOException {
        DefaultDependencyNode parent = jarNode(dir, "org.example:parent:1.0", JavaScopes.COMPILE);
        parent.setChildren(List.of(jarNode(dir, "org.example:child:1.0", JavaScopes.COMPILE)));

        assertEquals(Set.of("parent-1.0.jar", "child-1.0.jar"), names(walk(root(parent), JavaScopes.COMPILE, false)));
    }

    // ------------------------------------------------------------------ fixture

    private static Set<String> names(Set<SourceSet> parts) {
        Set<String> names = new TreeSet<>();
        for (SourceSet part : parts) names.add(part.name());
        return names;
    }

    private static Set<SourceSet> walk(DependencyNode root, String scope, boolean test) {
        return ComputeClassPath.run(new SystemStreamLog(), root, scope, test);
    }

    /** A synthetic root, as {@code DependencyResolutionResult#getDependencyGraph} returns: the project itself. */
    private static DependencyNode root(DependencyNode... children) {
        DefaultDependencyNode root = new DefaultDependencyNode((Dependency) null);
        root.setChildren(List.of(children));
        return root;
    }

    private static DefaultDependencyNode jarNode(Path dir, String coordinates, String scope) throws IOException {
        DefaultArtifact artifact = new DefaultArtifact(coordinates);
        File jar = dir.resolve(artifact.getArtifactId() + "-" + artifact.getVersion() + ".jar").toFile();
        // a real (empty) jar: PluginSourceSets opens it to ask whether it is a JPMS module
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar.toPath()))) {
            out.flush();
        }
        return node(artifact, jar, scope);
    }

    private static DefaultDependencyNode node(Artifact artifact, File file, String scope) {
        return new DefaultDependencyNode(new Dependency(artifact.setFile(file), scope));
    }

    /** Named so the call site reads as one thing; the map is per-run state, not fixture input. */
    private static class ComputeClassPath {
        static Set<SourceSet> run(org.apache.maven.plugin.logging.Log log, DependencyNode root, String scope,
                                  boolean test) {
            Map<String, SourceSet> byName = new HashMap<>();
            return ComputeSourceSets.computeClassPathParts(log, root, scope, test, byName, Set.of());
        }
    }
}
