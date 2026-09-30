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

package io.codelaser.maddi.ide.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The hand-over an IDE performs for Kotlin: a real daemon process, launched by {@link MaddiDaemonProcess} with a
 * directory of K2 jars (the shape of a download), analyses a {@code .kt} file. Without the directory, the same
 * daemon analyses nothing of it and says so.
 */
public class KotlinRoundTripTest {

    private static final String BAG = """
            package k

            class Bag {
                private val items = ArrayList<String>()
                fun add(s: String) { items.add(s) }
                fun size(): Int = items.size
            }
            """;

    @Test
    public void withAndWithoutTheFrontEnd(@TempDir Path projectDir) throws Exception {
        Path installDir = Path.of(System.getProperty("maddi.daemon.install"));
        Path jdkHome = Path.of(System.getProperty("maddi.test.jdkHome", System.getProperty("java.home")));
        Path k2Home = Path.of(System.getProperty("maddi.test.k2Home"));
        Path src = projectDir.resolve("src");
        Files.createDirectories(src.resolve("k"));
        Files.writeString(src.resolve("k/Bag.kt"), BAG);

        String stdlib;
        try (Stream<Path> jars = Files.list(installDir.resolve("lib"))) {
            stdlib = jars.filter(p -> p.getFileName().toString().startsWith("kotlin-stdlib-"))
                    .findFirst().orElseThrow().toString();
        }
        AnalysisModel.AnalyzeConfig config = new AnalysisModel.AnalyzeConfig(
                projectDir.toString(), jdkHome.toString(), "UTF-8", List.of("java.base"),
                List.of(), List.of(), List.of(), false, false,
                List.of(new AnalysisModel.ModuleSourceSet("k/main", List.of(src.toString()), null, false, 0,
                        List.of(), List.of(stdlib))));

        try (MaddiDaemonProcess daemon = new MaddiDaemonProcess()) {
            daemon.ensureStarted(installDir, jdkHome, 0, k2Home, projectDir.resolve("daemon.log"));
            AnalysisModel.Result result = analyze(daemon, config);
            assertTrue(result.initializationProblems().stream().noneMatch(p -> p.contains("NOT analyzed")),
                    () -> "problems: " + result.initializationProblems());
            List<String> add = display(result, "k.Bag.add");
            assertTrue(add.contains("@Modified"), () -> "Bag.add: " + add);
            assertTrue(result.elementAnnotations().stream().anyMatch(e -> e.uri().endsWith("k/Bag.kt")));

            // the front end is a launch parameter: dropping it relaunches, and the Kotlin is reported as skipped
            daemon.ensureStarted(installDir, jdkHome, 0, null, projectDir.resolve("daemon.log"));
            AnalysisModel.Result without = analyze(daemon, config);
            assertTrue(without.initializationProblems().stream().anyMatch(p -> p.contains("NOT analyzed")),
                    () -> "problems: " + without.initializationProblems());
            assertFalse(without.elementAnnotations().stream().anyMatch(e -> e.uri().endsWith(".kt")));
        }
    }

    private static AnalysisModel.Result analyze(MaddiDaemonProcess daemon, AnalysisModel.AnalyzeConfig config)
            throws Exception {
        JsonNode node = daemon.analyze("req", config, frame -> { });
        assertEquals("result", node.path("type").asText(), () -> "expected a result, got " + node);
        return daemon.client().objectMapper().treeToValue(node, AnalysisModel.Result.class);
    }

    private static List<String> display(AnalysisModel.Result result, String fqn) {
        return result.elementAnnotations().stream().filter(e -> e.fqn() != null && e.fqn().startsWith(fqn + "("))
                .flatMap(e -> e.displayAnnotations().stream()).toList();
    }
}
