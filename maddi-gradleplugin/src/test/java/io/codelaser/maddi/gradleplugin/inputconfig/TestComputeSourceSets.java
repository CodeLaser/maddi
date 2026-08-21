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
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestComputeSourceSets {

    /**
     * The order configurations are read in, which is the order a shared class-path part is CLAIMED in:
     * first sighting wins, and the recorder's name is where {@code test}/{@code runtimeOnly} come from.
     *
     * <p>⛔⛔ THE TIEBREAK COMPARED {@code n1} TWICE AND NEVER FIRED. It fell through to alphabetical, which
     * agrees for the standard names and disagrees for every non-test configuration sorting after "test" --
     * OpenSearch's {@code zip}, {@code jarHell}, {@code jdkJarHell}, {@code jacocoAgent},
     * {@code missingdoclet}. Priced before repairing: 93 of 214 projects change order, 0 parts change flags.
     */
    @Test
    public void aNonTestConfigurationIsReadFirstEvenWhenItSortsLast() {
        List<String> names = new ArrayList<>(List.of("testCompileClasspath", "zip", "compileClasspath",
                "testRuntimeClasspath", "runtimeClasspath", "jarHell", "annotationProcessor"));
        names.sort(ComputeSourceSets.CONFIGURATION_ORDER);

        assertEquals(List.of(
                // production, non-test, alphabetical -- `zip` before `testCompileClasspath` is the repair
                "annotationProcessor", "compileClasspath", "jarHell", "zip", "testCompileClasspath",
                // ...then everything runtime-only, non-test first
                "runtimeClasspath", "testRuntimeClasspath"), names);
    }


    /**
     * ⛔⛔ THE ANALYSED PROJECT'S OWN VIEW OF A SOURCE SET MUST WIN OVER A DEPENDENT'S VIEW OF THE SAME SET.
     * The two records describe one set and only one of them can see the compile task: the project's own carries
     * {@code buildUnit}, {@code sourceRelease}, {@code addModules}, {@code warningFlags} and a class-OUTPUT uri,
     * while {@code dependentProjectResult} passes {@code null}, {@code 0} and empty lists BY DESIGN, because a
     * sibling's compile task belongs to another project. Merging the dependents last let the sibling-shaped
     * record overwrite the real one -- and the project is one of its OWN dependents, because with the plugin
     * applied it publishes {@code maddiSourceElements} and then resolves that variant through its own
     * configurations.
     * <p>
     * ⚠ MEASURED on OpenSearch {@code :libs:opensearch-common} (2026-08-20): its {@code uri} came out as
     * {@code build/distributions/opensearch-common-3.9.0-SNAPSHOT.jar}, which does not exist, so the test set
     * could not resolve into main at all -- "112 type(s) were parsed from it ... the compilation units holding
     * them are dropped". None of the 11 tests in this module could see it; only a corpus could.
     */
    @Test
    public void theProjectsOwnSourceSetWinsOverADependentsViewOfIt() {
        SourceSet own = new SourceSetImpl.Builder()
                .setName("p/main")
                .setUri(URI.create("file:/p/build/classes/java/main/"))
                .setBuildUnit(":p")
                .setSourceRelease(21)
                .setAddModules(List.of("jdk.incubator.vector"))
                .setWarningFlags(List.of("-Xlint:cast"))
                .build();
        // exactly what dependentProjectResult produces for a SIBLING: it cannot reach another project's task
        SourceSet asSeenByADependent = new SourceSetImpl.Builder()
                .setName("p/main")
                .setUri(URI.create("file:/p/build/distributions/p-1.0.jar"))
                .build();

        ComputeSourceSets.Result dependent = new ComputeSourceSets.Result("p/main",
                Map.of("p/main", asSeenByADependent), List.of(), Map.of());
        ComputeSourceSets.Result result = new ComputeSourceSets.Result("p/main",
                Map.of("p/main", own), List.of(dependent), Map.of());

        SourceSet merged = result.allSourceSetsByName().get("p/main");
        assertNotNull(merged);
        assertEquals(":p", merged.buildUnit(), "the project's own record must win");
        assertEquals(21, merged.sourceRelease());
        assertEquals(List.of("jdk.incubator.vector"), merged.addModules(),
                "losing this loses the flag, and every type in that module stops resolving");
        assertEquals(List.of("-Xlint:cast"), merged.warningFlags());
        assertTrue(merged.uri().toString().endsWith("/classes/java/main/"),
                "the uri must be the class output, not the distribution jar");
    }

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


    /**
     * ⛔ EACH SOURCE SET STATES ITS OWN RELEASE, AND THE PLUGIN STATED NONE. Without it the parse runs on
     * whatever JDK maddi happens to be rather than on the level the corpus targets, and every API removed since
     * reads as "cannot find symbol" -- measured on pulsar (release 17) as {@code Thread.suspend()} vanishing
     * under JDK 26.
     *
     * <p>Asked per source set because Gradle answers per source set: this fixture is fernflower's shape, where
     * {@code compileJava} pins {@code sourceCompatibility} and {@code compileTestJava} says nothing and inherits
     * the project-wide level. A single global answer cannot express that.
     */
    @Test
    public void sourceReleaseComesFromEachSourceSetsOwnCompileTask() {
        Project project = ProjectBuilder.builder().withName("p").build();
        project.getPluginManager().apply("java");
        new File(project.getProjectDir(), "src/main/java").mkdirs();
        new File(project.getProjectDir(), "src/test/java").mkdirs();
        project.getExtensions().getByType(JavaPluginExtension.class)
                .setSourceCompatibility(JavaVersion.VERSION_21);
        ((JavaCompile) project.getTasks().getByName("compileJava")).getOptions().getRelease().set(17);

        ComputeSourceSets css = new ComputeSourceSets(project.getProjectDir().toPath().toAbsolutePath());
        ComputeSourceSets.Result result = css.compute(project, null, null, Set.of());

        assertEquals(17, result.sourceSetsByName().get("p/main").sourceRelease(),
                "main states --release explicitly, and --release wins");
        assertEquals(21, result.sourceSetsByName().get("p/test").sourceRelease(),
                "test states nothing of its own and falls back to the project-wide sourceCompatibility");
    }
}