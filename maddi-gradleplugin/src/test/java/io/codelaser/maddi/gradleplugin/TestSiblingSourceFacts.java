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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A SIBLING project's compile facts — {@code sourceRelease}, {@code addModules}, {@code warningFlags} —
 * reaching the consumer that co-analyses its sources.
 *
 * <p>⛔⛔ <b>THEY USED TO ARRIVE EMPTY, AND EMPTY IS NOT NEUTRAL.</b> A source set with
 * {@code sourceRelease=0} is compiled against whatever JDK maddi happens to run on, which reinstates every
 * API removed since; a source set with no {@code --add-modules} loses an incubator module's whole package
 * ("package jdk.incubator.vector is not visible") and drops the units that use it. MEASURED on OpenSearch
 * (2026-08-20, {@code applyTo=all}): <b>2 source sets of 21 carried any warning flag, and 19 arrived with
 * {@code sourceRelease=0}</b>.
 *
 * <p>⚠ <b>THE FIXTURE APPLIES THE PLUGIN TO BOTH PROJECTS, WHICH IS THE WHOLE DIFFERENCE</b> from
 * {@code TestMultiProjectClassPath}. A sibling only publishes {@code maddiSourceElements} — and therefore
 * only becomes a source set rather than a jar — when the plugin is applied to it too. That is the
 * {@code applyTo=all} mode; the single-module mode this plugin's users are normally in has no source
 * siblings at all, which is why this path had no test.
 */
public class TestSiblingSourceFacts {

    /**
     * ⭐ The sibling's own numbers, and they are DELIBERATELY DIFFERENT from the consumer's: release 17
     * against 21, a warning policy against none. A fixture where both agree passes against a defect that
     * copies the consumer's facts onto the sibling.
     */
    @Test
    public void aSiblingsCompileFactsReachTheConsumer(@TempDir Path dir) throws IOException {
        writeTwoProjects(dir);

        BuildResult result = GradleRunner.create()
                .withProjectDir(dir.toFile())
                .withPluginClasspath()
                .withArguments(":consumer:maddi-write-input-configuration", "--stacktrace")
                .forwardOutput()
                .build();
        assertEquals(TaskOutcome.SUCCESS,
                result.task(":consumer:maddi-write-input-configuration").getOutcome());

        JsonNode config = new ObjectMapper()
                .readTree(dir.resolve("consumer/build/inputConfiguration.json").toFile());

        JsonNode alpha = null, consumer = null;
        List<String> names = new ArrayList<>();
        for (JsonNode set : config.get("sourceSets")) {
            names.add(set.get("name").asText());
            if ("alpha/main".equals(set.get("name").asText())) alpha = set;
            if ("consumer/main".equals(set.get("name").asText())) consumer = set;
        }
        assertNotNull(alpha, "the sibling must arrive as a SOURCE set, not a jar; got " + names);
        assertNotNull(consumer, names.toString());

        // the sibling's own release, not zero and not the consumer's
        assertEquals(17, alpha.get("sourceRelease").asInt(),
                "sourceRelease=0 means 'whatever JDK maddi runs on'; the sibling says 17");
        assertEquals(21, consumer.get("sourceRelease").asInt());

        List<String> alphaFlags = strings(alpha, "warningFlags");
        assertTrue(alphaFlags.contains("-Werror"),
                "the sibling compiles under -Werror and must say so; got " + alphaFlags);
        assertTrue(alphaFlags.contains("-Xlint:all"), alphaFlags.toString());

        // ...and the consumer's own flags are its own: this is not one list copied onto both
        assertTrue(strings(consumer, "warningFlags").isEmpty(), "the consumer sets no warning flags");
    }

    /**
     * ⚠ <b>AN EMPTY LIST IS OMITTED FROM THE JSON ALTOGETHER</b>, so "this set has no warning flags" and
     * "nobody recorded any" are the same absence in the file. That is the distinction stage 2b had to make
     * downstream ({@code FLAGS_NOT_RECORDED} against {@code EXEMPT}), and it is why this reads a missing
     * node as empty rather than failing on it — the assertion above is about the sibling's flags being
     * PRESENT, which an absence cannot fake.
     */
    private static List<String> strings(JsonNode set, String field) {
        JsonNode node = set.get(field);
        if (node == null) return List.of();
        List<String> values = new ArrayList<>();
        for (JsonNode f : node) values.add(f.asText());
        return values;
    }

    /**
     * {@code java-library} on both, the maddi plugin on both. ⚠ {@code alpha}'s facts are set on its
     * {@code compileJava} task alone, which is the shape that matters: OpenSearch's {@code libs/common}
     * scopes both {@code --add-modules} and its {@code -Werror} exemption to {@code compileJava}, so its
     * main and test sets disagree, and a per-project answer would flatten exactly that.
     */
    private static void writeTwoProjects(Path dir) throws IOException {
        Files.writeString(dir.resolve("settings.gradle.kts"), """
                rootProject.name = "sf"
                include("alpha", "consumer")
                """);
        Files.writeString(dir.resolve("build.gradle.kts"), """
                subprojects {
                    apply(plugin = "java-library")
                    repositories { mavenCentral() }
                }
                """);
        Path alphaSrc = Files.createDirectories(dir.resolve("alpha/src/main/java/alpha"));
        Files.writeString(alphaSrc.resolve("T.java"), "package alpha;\npublic class T { public int i; }\n");
        Files.writeString(dir.resolve("alpha/build.gradle.kts"), """
                plugins { id("io.codelaser.maddi.analyzer") }
                tasks.named<JavaCompile>("compileJava") {
                    options.release.set(17)
                    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
                }
                """);
        Path src = Files.createDirectories(dir.resolve("consumer/src/main/java/consumer"));
        Files.writeString(dir.resolve("consumer/build.gradle.kts"), """
                plugins { id("io.codelaser.maddi.analyzer") }
                tasks.named<JavaCompile>("compileJava") { options.release.set(21) }
                dependencies { implementation(project(":alpha")) }
                """);
        Files.writeString(src.resolve("C.java"), """
                package consumer;
                public class C {
                    public int sum(alpha.T a) { return a.i; }
                }
                """);
    }
}
