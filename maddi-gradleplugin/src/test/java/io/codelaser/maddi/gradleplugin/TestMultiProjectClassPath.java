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

package io.codelaser.maddi.gradleplugin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A MULTI-PROJECT build, which is the only shape in which the class-path defect below exists.
 *
 * <p>⚠ <b>THIS IS A FUNCTIONAL TEST BECAUSE IT HAS TO BE.</b> The seam is Gradle's own resolution: a project
 * dependency on a compile class path resolves to the producing project's <em>classes directory</em> rather than to
 * a jar (compile avoidance). A {@code ProjectBuilder} fixture cannot show that -- it resolves no artifacts at all,
 * so the first attempt at this test passed a configuration with zero class-path parts and asserted against
 * nothing. Whichever input a fixture supplies, production computes; here production computes the thing under test.
 */
public class TestMultiProjectClassPath {

    /**
     * ⛔⛔ TWO SIBLING PROJECTS MUST YIELD TWO CLASS-PATH PARTS.
     *
     * <p>Every sibling arrives as {@code <project>/build/classes/java/main}, so {@code file.getName()} is
     * {@code "main"} for all of them. Naming parts by the file name gave them one name, and the "already have it"
     * guard then dropped every one after the first -- with no log line, because the guard had no {@code else}.
     *
     * <p>⚠ MEASURED on pulsar (2026-08-19): {@code :managed-ledger} has 7 sibling projects on its class path and
     * the configuration contained ONE, called {@code main}. Its own sources then failed to parse with 100 errors
     * naming packages that were simply no longer on the class path.
     */
    @Test
    public void siblingProjectsEachGetTheirOwnClassPathPart(@TempDir Path dir) throws IOException {
        writeMultiProject(dir);

        BuildResult result = GradleRunner.create()
                .withProjectDir(dir.toFile())
                .withPluginClasspath()
                .withArguments(":consumer:e2immu-write-input-configuration", "--stacktrace")
                .forwardOutput()
                .build();
        assertEquals(TaskOutcome.SUCCESS,
                result.task(":consumer:e2immu-write-input-configuration").getOutcome());

        JsonNode config = new ObjectMapper()
                .readTree(dir.resolve("consumer/build/inputConfiguration.json").toFile());

        List<String> partNames = new ArrayList<>();
        for (JsonNode part : config.get("classPathParts")) partNames.add(part.get("name").asText());

        // BOTH siblings, under names that tell them apart. Before the fix this list held one entry, "main".
        // The name is asserted by its project-path prefix rather than in full: whether Gradle hands over the
        // classes directory or the jar is the producer's business, and the prefix is what makes either unique.
        assertEquals(1, partNames.stream().filter(n -> n.startsWith(":alpha/")).count(),
                "expected exactly one class-path part for :alpha; got " + partNames);
        assertEquals(1, partNames.stream().filter(n -> n.startsWith(":beta/")).count(),
                "expected exactly one class-path part for :beta; got " + partNames);
        assertFalse(partNames.contains("main"),
                "'main' is a directory name, not an identity: " + partNames);

        // ...and the consumer must actually depend on both, since a part nothing names is a part nothing reads
        List<String> consumerDeps = new ArrayList<>();
        for (JsonNode set : config.get("sourceSets")) {
            if ("consumer/main".equals(set.get("name").asText())) {
                for (JsonNode d : set.get("dependencies")) consumerDeps.add(d.asText());
            }
        }
        assertTrue(consumerDeps.stream().anyMatch(n -> n.startsWith(":alpha/"))
                   && consumerDeps.stream().anyMatch(n -> n.startsWith(":beta/")),
                "consumer/main must depend on both siblings; got " + consumerDeps);
    }

    /**
     * Two java subprojects, both on the consumer's compile class path, each with a type the consumer uses.
     *
     * <p>⚠ {@code java-library}, not {@code java}, and that is the whole fixture. Only the library plugin
     * publishes a {@code classes} variant, which is what makes Gradle put the producing project's
     * {@code build/classes/java/main} DIRECTORY on the consumer's compile class path instead of its jar --
     * compile avoidance. With the plain {@code java} plugin the siblings arrive as {@code alpha.jar} and
     * {@code beta.jar}, whose file names differ, so the collision this test exists for never happens and the
     * test passes against the defect.
     */
    private static void writeMultiProject(Path dir) throws IOException {
        Files.writeString(dir.resolve("settings.gradle.kts"), """
                rootProject.name = "mp"
                include("alpha", "beta", "consumer")
                """);
        Files.writeString(dir.resolve("build.gradle.kts"), """
                subprojects {
                    apply(plugin = "java-library")
                    repositories { mavenCentral() }
                }
                """);
        for (String p : new String[]{"alpha", "beta"}) {
            Path src = dir.resolve(p + "/src/main/java/" + p);
            Files.createDirectories(src);
            Files.writeString(src.resolve("T.java"), "package " + p + ";\npublic class T { public int i; }\n");
            Files.writeString(dir.resolve(p + "/build.gradle.kts"), "");
        }
        Path src = Files.createDirectories(dir.resolve("consumer/src/main/java/consumer"));
        Files.writeString(dir.resolve("consumer/build.gradle.kts"), """
                plugins { id("io.codelaser.maddi.analyzer") }
                dependencies {
                    implementation(project(":alpha"))
                    implementation(project(":beta"))
                }
                """);
        Files.writeString(src.resolve("C.java"), """
                package consumer;
                public class C {
                    public int sum(alpha.T a, beta.T b) { return a.i + b.i; }
                }
                """);
    }
}
