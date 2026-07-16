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

package org.e2immu.analyzer.ide.plugin.daemon;

import com.fasterxml.jackson.databind.JsonNode;
import org.e2immu.analyzer.ide.plugin.model.AnalysisModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M2 end-to-end (plugin ↔ daemon): launch the real daemon, send an {@code analyzeProject} for a small
 * on-disk {@code @Container} violation, and map the JSON {@code result} back into the plugin's DTOs —
 * exactly the round-trip {@code MaddiAnalysisService} performs, but without a live IDE {@code Project}.
 * The e2immu annotations come from the daemon distribution's own {@code lib/} (its "hot class files").
 */
public class DaemonAnalyzeRoundTripTest {

    @Test
    public void analyzeContainerViolationOverSocket(@TempDir Path projectDir) throws Exception {
        String install = System.getProperty("maddi.daemon.install");
        assertTrue(install != null && !install.isBlank(), "maddi.daemon.install must be set by the test task");
        Path installDir = Path.of(install);
        // A JDK 25+ home for the daemon to run on and to read java.base from (the test JVM itself is JBR 21).
        String jdkHome = System.getProperty("maddi.test.jdkHome", System.getProperty("java.home"));

        Path pkg = Files.createDirectories(projectDir.resolve("src/x"));
        Files.writeString(pkg.resolve("Example.java"), """
                package x;
                import org.e2immu.annotation.Container;

                class Mutable {
                    private int value;
                    void set(int v) { this.value = v; }
                    int get() { return value; }
                }

                @Container
                interface HasAdd {
                    void add(Mutable m);
                }

                class BadImpl implements HasAdd {
                    @Override
                    public void add(Mutable m) { m.set(3); } // violates @Container
                }
                """);

        AnalysisModel.AnalyzeConfig config = new AnalysisModel.AnalyzeConfig(
                projectDir.toAbsolutePath().toString(),
                jdkHome,
                "UTF-8",
                List.of("java.base"),
                List.of(new AnalysisModel.SourceRoot("main", "src", false)),
                List.of(), // no user classpath: the e2immu annotations come from the daemon's own classpath
                List.of(),
                false);

        DaemonLauncher launcher = new DaemonLauncher();
        try (DaemonLauncher.Handle handle = launcher.launch(installDir, Path.of(jdkHome), List.of(), 30_000);
             DaemonClient client = new DaemonClient(handle.port(), 120_000)) {

            assertEquals("handshakeAck", client.handshake(1).path("type").asText());

            JsonNode resultNode = client.analyze("req-1", config, status ->
                    System.out.println("status: " + status.path("phase").asText()));
            assertEquals("result", resultNode.path("type").asText(),
                    "expected a result, got: " + resultNode);

            AnalysisModel.Result result = client.objectMapper().treeToValue(resultNode, AnalysisModel.Result.class);
            assertEquals(0, result.parseErrorCount(), "unexpected parse errors");

            AnalysisModel.Finding violation = result.findings().stream()
                    .filter(f -> "contract-violation".equals(f.category()))
                    .findFirst().orElse(null);
            assertNotNull(violation, "expected a contract-violation over the wire");
            assertEquals("ERROR", violation.severity());
            assertNotNull(violation.uri());
            assertNotNull(violation.beginLine());
            assertFalse(violation.causes() == null || violation.causes().isEmpty(), "expected a why-chain");

            boolean containerShown = result.elementAnnotations().stream()
                    .flatMap(e -> e.displayAnnotations().stream())
                    .anyMatch(a -> a.contains("@Container"));
            assertTrue(containerShown, "expected a @Container element annotation");

            client.shutdown();
        }
    }
}
