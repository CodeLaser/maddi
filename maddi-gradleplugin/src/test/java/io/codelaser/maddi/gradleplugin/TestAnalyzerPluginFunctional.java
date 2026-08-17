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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end reactivation test: apply the plugin to a generated Java project and run the {@code e2immu-analyzer}
 * task. It must parse (openjdk front-end, in a forked worker), run the modification analysis, and write a result
 * JSON per package to {@code build/e2immu}.
 */
public class TestAnalyzerPluginFunctional {

    @Test
    public void analyzerTaskRunsAndWritesResults(@TempDir Path projectDir) throws IOException {
        writeSimpleJavaProject(projectDir);

        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments("e2immu-analyzer", "--stacktrace", "--info")
                .forwardOutput()
                .build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":e2immu-analyzer").getOutcome());
        // toRealPath() resolves the macOS /var -> /private/var symlink, so the results directory the analyzer
        // wrote (canonical path) matches what we check here
        Path results = projectDir.toRealPath().resolve("build/e2immu");
        assertTrue(Files.isDirectory(results), "expected results directory " + results);
        try (var stream = Files.walk(results)) {
            assertTrue(stream.anyMatch(p -> p.getFileName().toString().endsWith(".json")),
                    "expected at least one analysis-result .json in " + results);
        }
    }

    /**
     * Modernization proof: with the configuration cache enabled and {@code problems=fail}, the task must store the
     * entry on the first run (a hard failure on any configuration-cache incompatibility) and reuse it on the second.
     */
    @Test
    public void configurationCacheCompatible(@TempDir Path projectDir) throws IOException {
        writeSimpleJavaProject(projectDir);
        Files.writeString(projectDir.resolve("gradle.properties"), """
                org.gradle.configuration-cache=true
                org.gradle.configuration-cache.problems=fail
                """);

        GradleRunner runner = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments("e2immu-analyzer", "--stacktrace")
                .forwardOutput();

        BuildResult first = runner.build();
        assertEquals(TaskOutcome.SUCCESS, first.task(":e2immu-analyzer").getOutcome());

        BuildResult second = runner.build();
        assertTrue(second.getOutput().contains("Reusing configuration cache")
                   || second.getOutput().contains("Configuration cache entry reused"),
                "expected the configuration cache to be reused on the second run:\n" + second.getOutput());
        // and, unchanged inputs + present outputs => the analyzer is not re-executed (up-to-date / from cache)
        TaskOutcome secondOutcome = second.task(":e2immu-analyzer").getOutcome();
        assertTrue(secondOutcome == TaskOutcome.UP_TO_DATE || secondOutcome == TaskOutcome.FROM_CACHE,
                "expected the second run to be incremental, was " + secondOutcome);
    }

    private static void writeSimpleJavaProject(Path projectDir) throws IOException {
        Files.writeString(projectDir.resolve("settings.gradle.kts"), "rootProject.name = \"tp\"\n");
        Files.writeString(projectDir.resolve("build.gradle.kts"), """
                plugins {
                    java
                    id("org.e2immu.analyzer-plugin")
                }
                e2immu {
                    jmods = "java.base"
                    analysisSteps = "modification"
                    sourcePackages = "com.example"
                }
                """);
        Path pkg = Files.createDirectories(projectDir.resolve("src/main/java/com/example"));
        Files.writeString(pkg.resolve("Counter.java"), """
                package com.example;
                public class Counter {
                    private int count;
                    public void increment() { count++; }
                    public int get() { return count; }
                }
                """);
    }
}
