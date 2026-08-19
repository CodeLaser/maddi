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

package io.codelaser.maddi.gradleplugin.inputconfig;

import io.codelaser.maddi.cst.api.element.SourceSet;
import org.gradle.api.Project;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestComputeSourceSets {

    @Test
    public void test() {
        ComputeSourceSets css = new ComputeSourceSets(Path.of(".").toAbsolutePath());
        Path workingDirectory = css.getWorkingDirectory();
        assertTrue(workingDirectory.isAbsolute());
        assertTrue(workingDirectory.toString().endsWith("/maddi-gradleplugin/."));
        Path srcMainJava = Path.of("src/main/java");
        assertTrue(Files.isDirectory(srcMainJava));
        assertFalse(srcMainJava.isAbsolute());
        Path absSrcMainJava = srcMainJava.toAbsolutePath();
        assertTrue(absSrcMainJava.isAbsolute());
    }

    /**
     * Kotlin-awareness: a Gradle {@code SourceSet} is {@link ExtensionAware}, and the Kotlin JVM plugin registers a
     * {@code kotlin} {@link SourceDirectorySet} per source set. We attach one by hand (so the test needs no
     * dependency on the Kotlin Gradle plugin) and assert {@link ComputeSourceSets} folds its directory into the
     * computed maddi source set alongside {@code src/main/java}.
     */
    @Test
    public void collectsKotlinSourceDirs() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply("java");

        new File(project.getProjectDir(), "src/main/java").mkdirs();
        new File(project.getProjectDir(), "src/main/kotlin").mkdirs();

        var main = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets().getByName("main");
        SourceDirectorySet kotlin = project.getObjects().sourceDirectorySet("kotlin", "Kotlin sources");
        kotlin.srcDir("src/main/kotlin");
        ((ExtensionAware) main).getExtensions().add("kotlin", kotlin);

        ComputeSourceSets css = new ComputeSourceSets(project.getProjectDir().toPath().toAbsolutePath());
        ComputeSourceSets.Result result = css.compute(project, null, null, Set.of());

        SourceSet mainSet = result.sourceSetsByName().get(project.getName() + "/main");
        assertNotNull(mainSet, "expected a maddi source set for main; got " + result.sourceSetsByName().keySet());
        boolean hasKotlin = mainSet.sourceDirectories().stream()
                .anyMatch(p -> p.toString().replace('\\', '/').endsWith("src/main/kotlin"));
        assertTrue(hasKotlin, "expected the kotlin source directory; got " + mainSet.sourceDirectories());
    }

    /**
     * Every source set of one Gradle project must record the same build unit, and that build unit must be derived
     * from the project's path rather than its name: sibling projects ':a:util' and ':b:util' share the leaf name
     * 'util', so the name cannot identify a build unit. Source set names stay leaf-based, as before.
     */
    @Test
    public void recordsTheProjectPathAsBuildUnit() {
        Project root = ProjectBuilder.builder().withName("root").build();
        Project util = ProjectBuilder.builder().withName("util").withParent(root).build();
        util.getPluginManager().apply("java");
        new File(util.getProjectDir(), "src/main/java").mkdirs();
        new File(util.getProjectDir(), "src/test/java").mkdirs();

        ComputeSourceSets css = new ComputeSourceSets(util.getProjectDir().toPath().toAbsolutePath());
        ComputeSourceSets.Result result = css.compute(util, null, null, Set.of());

        SourceSet main = result.sourceSetsByName().get("util/main");
        SourceSet test = result.sourceSetsByName().get("util/test");
        assertNotNull(main, "got " + result.sourceSetsByName().keySet());
        assertNotNull(test, "got " + result.sourceSetsByName().keySet());

        assertEquals(":util", main.buildUnit());
        assertEquals(main.buildUnit(), test.buildUnit(), "main and test must share one build unit");
        assertTrue(test.test());
    }

    /**
     * ⛔ A SOURCE SET'S {@code uri} IS ITS CLASS OUTPUT, NOT ITS FIRST SOURCE DIRECTORY -- the plugin-side half of
     * {@code TestPluginSourceSets#uriIsTheClassOutput}. It is what {@code JavaInspectorImpl} hands javac as the
     * class path entry for a {@code test -> main} edge, so a source directory there makes javac silently
     * recompile main while it resolves test, and drops any type whose directory does not match its package.
     * <p>
     * ⚠ Asserted on a project whose {@code build/} does not exist: the configuration is computed before the
     * compile tasks it depends on have run, so a class output that is merely DECLARED must still be recorded.
     * Probing it here is what broke {@code TestAnalyzerPluginFunctional#configurationCacheCompatible}.
     */
    @Test
    public void uriIsTheClassOutputNotTheSourceDirectory() {
        Project project = ProjectBuilder.builder().withName("p").build();
        project.getPluginManager().apply("java");
        new File(project.getProjectDir(), "src/main/java").mkdirs();
        new File(project.getProjectDir(), "src/test/java").mkdirs();
        assertFalse(new File(project.getProjectDir(), "build").exists(), "nothing is compiled yet, on purpose");

        ComputeSourceSets css = new ComputeSourceSets(project.getProjectDir().toPath().toAbsolutePath());
        ComputeSourceSets.Result result = css.compute(project, null, null, Set.of());

        for (String name : new String[]{"p/main", "p/test"}) {
            SourceSet set = result.sourceSetsByName().get(name);
            assertNotNull(set, "got " + result.sourceSetsByName().keySet());
            String uri = set.uri().toString();
            String expected = name.endsWith("/test") ? "build/classes/java/test" : "build/classes/java/main";
            assertTrue(uri.endsWith(expected + "/") || uri.endsWith(expected),
                    name + ": expected the class output " + expected + ", got " + uri);
            assertFalse(set.sourceDirectories().isEmpty(), name + " must still carry its source directories");
            assertTrue(set.sourceDirectories().stream().noneMatch(p -> p.toUri().equals(set.uri())),
                    name + ": the uri must not be one of the source directories");
        }
    }
}
