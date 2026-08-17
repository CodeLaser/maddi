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

package io.codelaser.maddi.run.config.compile;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ⛔⛔ <b>A REACTOR SIBLING'S PACKAGED JAR ON THE {@code -classpath} IS ITS SOURCE SET'S OUTPUT, AND WAS READ AS
 * AN OPAQUE LIBRARY.</b> {@code createSourceSet} resolves a classpath entry against {@code sourceSetsByPath} and,
 * when that misses, only warns — while the {@code --module-path} branch immediately below it falls back to
 * {@code jarFileToDestination} and recovers the edge. Three ways a build names a sibling's output, two handled:
 *
 * <table><caption></caption>
 * <tr><td>a directory {@code <mod>/target/classes}</td><td>{@code handleDirectoryInClasspath}</td><td>✅</td></tr>
 * <tr><td>a jar on {@code --module-path}</td><td>{@code jarFileToDestination} fallback</td><td>✅</td></tr>
 * <tr><td>a jar on {@code -classpath}</td><td>nothing</td><td>⛔</td></tr>
 * </table>
 *
 * <p>⚠ <b>AND IT HIDES ON EXACTLY THE CORPORA THAT WERE USED TO VALIDATE IT.</b> {@code computeModuleJars} builds
 * that map only from {@code inv.modulePath()}, so a project with no {@code module-info} anywhere never populates
 * it and the classpath branch has nothing to consult even in principle. Measured 2026-08-12, same tool, same
 * build system, both from {@code mvn -X clean install -DskipTests}:
 *
 * <table><caption></caption>
 * <tr><td>timefold (modular)</td><td>60 of 65 source sets carry intra-reactor dependencies</td><td>0 own jars</td></tr>
 * <tr><td>jenkins (no module-info at all)</td><td><b>1 of 9</b></td><td><b>4 own jars</b></td></tr>
 * </table>
 *
 * <p>What it cost: jenkins' {@code test/test-classes} (493 files) bound to {@code jenkins-core-…​.jar} instead of
 * to {@code core/main}, so core's types sat in the parse <em>twice</em> — once as source, once as class files —
 * and every edit to core source was invisible to the module that tests it.
 *
 * <p>⛔ <b>THE MATCH IS BY PATH, NOT BY NAME.</b> {@code computeModuleName} matches a jar's file name against
 * {@code <module>/main}, which works for gradle (artifact = module) and <em>cannot</em> work for maven, where the
 * artifactId and the module directory are different strings: jenkins ships {@code jenkins-core-2.574.jar} out of
 * a module directory named {@code core}. The reliable signal is already in hand — a packaged jar sits in the same
 * directory as its module's compiler destination ({@code <mod>/target/classes} and {@code <mod>/target/x.jar}).
 * That is also what keeps a VENDORED jar under {@code <mod>/lib/} from being mistaken for the module's output.
 */
public class TestReactorJarOnClasspath {

    private static final String ROOT = "/checkout/jenkins";

    private record Invocation(String destination, List<String> sourcePath, List<String> classpath,
                              List<String> modulePath) implements CompileInvocation {
        Invocation(String destination, List<String> sourcePath, List<String> classpath) {
            this(destination, sourcePath, classpath, null);
        }

        @Override
        public List<String> sourceFiles() {
            return List.of();
        }

        @Override
        public String encoding() {
            return null;
        }
    }

    private static final String CLI_MAIN = ROOT + "/cli/target/classes";
    private static final String CLI_JAR = ROOT + "/cli/target/cli-2.574-SNAPSHOT.jar";
    private static final String CORE_MAIN = ROOT + "/core/target/classes";
    private static final String CORE_TEST = ROOT + "/core/target/test-classes";
    private static final String CORE_JAR = ROOT + "/core/target/jenkins-core-2.574-SNAPSHOT.jar";
    private static final String TEST_TEST = ROOT + "/test/target/test-classes";

    /** CONTROL 1: a genuinely external dependency. Must stay a library, or the rule is "map everything". */
    private static final String EXTERNAL_JAR = "/home/u/.m2/repository/com/google/guava/guava-33.6.0-jre.jar";
    /** CONTROL 2: a jar vendored INSIDE the module but not in its output root. Must stay a library. */
    private static final String VENDORED_JAR = ROOT + "/core/lib/vendored-1.0.jar";

    /** The jenkins shape: maven, no modules anywhere, siblings arriving as packaged jars on the classpath. */
    private static CompileListToSourceSets.Result computeJenkinsShape() {
        return new CompileListToSourceSets(ROOT).compute(List.of(
                new Invocation(CLI_MAIN, List.of(ROOT + "/cli/src/main/java"), List.of()),
                new Invocation(CORE_MAIN, List.of(ROOT + "/core/src/main/java"),
                        List.of(CLI_JAR, EXTERNAL_JAR, VENDORED_JAR)),
                new Invocation(CORE_TEST, List.of(ROOT + "/core/src/test/java"), List.of(CORE_MAIN, EXTERNAL_JAR)),
                // the 493-file module: it names core only through the packaged jar
                new Invocation(TEST_TEST, List.of(ROOT + "/test/src/test/java"), List.of(CORE_JAR, CLI_JAR))));
    }

    private static List<String> names(List<SourceSet> sets) {
        return sets.stream().map(SourceSet::name).sorted().toList();
    }

    private static SourceSet named(CompileListToSourceSets.Result r, String name) {
        return r.jSourceSets().stream().map(CompileListToSourceSets.JSourceSet::sourceSet)
                .filter(s -> name.equals(s.name())).findFirst()
                .orElseThrow(() -> new AssertionError(name + " is nowhere: "
                        + names(r.jSourceSets().stream().map(CompileListToSourceSets.JSourceSet::sourceSet).toList())));
    }

    /**
     * ⚠ CONTROL FIRST, and it is the control that makes the defect legible at all: jenkins on its own reads as
     * "1 of 9 source sets have intra-reactor dependencies", which is a plausible fact ABOUT JENKINS. The directory
     * form of the very same edge working is what turns it into a defect with a mechanism.
     */
    @DisplayName("CONTROL: the same edge named as a DIRECTORY has always resolved")
    @Test
    public void theDirectoryFormResolves() {
        CompileListToSourceSets.Result result = computeJenkinsShape();
        assertTrue(names(named(result, "core/test-classes").dependencies()).contains("core/main"),
                "core/test-classes names core/target/classes directly, so this edge never depended on the fix");
    }

    @DisplayName("a sibling's packaged jar on the classpath becomes a dependency on that sibling's source set")
    @Test
    public void theJarFormResolvesToo() {
        CompileListToSourceSets.Result result = computeJenkinsShape();

        assertTrue(names(named(result, "core/main").dependencies()).contains("cli/main"),
                "core/main reads cli through cli-2.574-SNAPSHOT.jar: "
                + names(named(result, "core/main").dependencies()));
        assertTrue(names(named(result, "test/test-classes").dependencies()).contains("core/main"),
                "test/test-classes reads core through jenkins-core-2.574-SNAPSHOT.jar — the 493-file case: "
                + names(named(result, "test/test-classes").dependencies()));
    }

    @DisplayName("and the reactor's own jars stop being libraries, so no type is in the parse twice")
    @Test
    public void theReactorJarsAreNotLibraries() {
        List<String> jars = names(computeJenkinsShape().jars());

        assertFalse(jars.contains("cli-2.574-SNAPSHOT.jar"), "still a library: " + jars);
        assertFalse(jars.contains("jenkins-core-2.574-SNAPSHOT.jar"), "still a library: " + jars);
    }

    @DisplayName("CONTROL: an external jar, and one vendored inside the module, both stay libraries")
    @Test
    public void externalAndVendoredJarsAreUntouched() {
        List<String> jars = names(computeJenkinsShape().jars());

        assertTrue(jars.contains("guava-33.6.0-jre.jar"), "an external dependency must stay a library: " + jars);
        assertTrue(jars.contains("vendored-1.0.jar"),
                "a jar under core/lib is not core's compiler output, and must stay a library: " + jars);
    }

    /**
     * ⛔ The rule keys on the module's OUTPUT ROOT (the destination's own directory), never on the file name —
     * so a maven artifactId that differs from the module directory resolves, which is the jenkins case and the
     * one {@code computeModuleName}'s name matching cannot reach.
     */
    @DisplayName("the artifactId need not resemble the module directory: jenkins-core-*.jar out of core/")
    @Test
    public void theArtifactIdNeedNotMatchTheModuleName() {
        SourceSet testSet = named(computeJenkinsShape(), "test/test-classes");
        SourceSet core = testSet.dependencies().stream().filter(d -> "core/main".equals(d.name()))
                .findFirst().orElseThrow(() -> new AssertionError("no core/main edge"));
        assertEquals("file:" + CORE_MAIN, core.uri().toString(),
                "the edge must point at the compiled output, not at the jar");
        assertFalse(core.library(), "core/main is parsed from source, not read as a library");
    }

    @DisplayName("end to end: build() accepts it, and the dependency is in the written configuration")
    @Test
    public void theConfigurationResolves() {
        InputConfiguration ic = CompileListToInputConfiguration.build(computeJenkinsShape(), List.of());

        SourceSet test = ic.sourceSets().stream().filter(s -> "test/test-classes".equals(s.name()))
                .findFirst().orElseThrow();
        assertTrue(names(test.dependencies()).contains("core/main"), names(test.dependencies()).toString());
        assertTrue(ic.classPathParts().stream().noneMatch(p -> p.name().endsWith("-SNAPSHOT.jar")),
                "no reactor jar survives as a classpath part: "
                + ic.classPathParts().stream().map(SourceSet::name).filter(n -> n.endsWith(".jar")).toList());
    }
}
