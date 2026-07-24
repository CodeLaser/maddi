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

package org.e2immu.gradleplugin;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proof that the <em>shaded</em> plugin jar is self-contained.
 * <p>
 * Unlike {@link TestAnalyzerPluginFunctional}, which uses {@code withPluginClasspath()} (the plugin's full
 * runtime classpath, with every analyzer module present), this test resolves the plugin <em>from a local
 * Maven repository</em> — the {@code localPluginRepo} the build publishes the shadow jar into — and applies
 * it by id and version. None of the {@code maddi-*} analyzer modules are on any classpath here; the only
 * thing that carries the analyzer is the shaded jar. If a class were missing from the shadow jar (or a
 * dependency were left in the POM but not bundled), the run would fail with {@code NoClassDefFoundError} or
 * an unresolved dependency. A green run means the jar stands alone.
 */
public class TestAnalyzerPluginShadedJarIsolation {

    @Test
    public void shadedPluginResolvesAndRunsFromLocalRepo(@TempDir Path projectDir) throws IOException {
        String localRepo = System.getProperty("e2immu.localPluginRepo");
        assertNotNull(localRepo, "system property e2immu.localPluginRepo must be set by the build");
        String pluginVersion = System.getProperty("e2immu.pluginVersion");
        assertNotNull(pluginVersion, "system property e2immu.pluginVersion must be set by the build");

        String repoUri = Path.of(localRepo).toUri().toString();
        // Resolve the plugin ONLY from the local repo (no withPluginClasspath, no other repositories),
        // so the shaded jar is the only thing on the plugin classpath.
        Files.writeString(projectDir.resolve("settings.gradle.kts"), """
                pluginManagement {
                    repositories {
                        maven { url = uri("%s") }
                    }
                }
                rootProject.name = "iso"
                """.formatted(repoUri));
        Files.writeString(projectDir.resolve("build.gradle.kts"), """
                plugins {
                    java
                    id("org.e2immu.analyzer-plugin") version "%s"
                }
                e2immu {
                    jmods = "java.base"
                    analysisSteps = "modification"
                    sourcePackages = "com.example"
                }
                """.formatted(pluginVersion));
        Path pkg = Files.createDirectories(projectDir.resolve("src/main/java/com/example"));
        Files.writeString(pkg.resolve("Counter.java"), """
                package com.example;
                public class Counter {
                    private int count;
                    public void increment() { count++; }
                    public int get() { return count; }
                }
                """);

        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withArguments("e2immu-analyzer", "--stacktrace", "--info")
                .forwardOutput()
                .build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":e2immu-analyzer").getOutcome());
        Path results = projectDir.toRealPath().resolve("build/e2immu");
        assertTrue(Files.isDirectory(results), "expected results directory " + results);
        try (var stream = Files.walk(results)) {
            assertTrue(stream.anyMatch(p -> p.getFileName().toString().endsWith(".json")),
                    "expected at least one analysis-result .json in " + results);
        }
    }
}
