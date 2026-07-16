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

package org.e2immu.analyzer.ide.daemon;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M1 end-to-end: run the real analysis pipeline over a small on-disk Java project with a deliberate
 * {@code @Container} contract violation, and assert the JSON result carries the guard finding (with
 * a why-chain) and the computed {@code @Container} element annotation.
 * <p>
 * The example is self-contained (no {@code java.util}) so no preloaded JDK analysis hints are needed;
 * the only classpath entry is the e2immu annotations jar (maddi-support), passed by the Gradle test task.
 */
public class WarmAnalysisServiceTest {

    @Test
    public void containerViolationIsFoundAndExplained(@TempDir Path projectDir) throws Exception {

        // A @Container interface whose implementation modifies its argument → contract violation.
        Path pkg = Files.createDirectories(projectDir.resolve("src/x"));
        Files.writeString(pkg.resolve("Example.java"), """
                package x;
                import org.e2immu.annotation.Container;

                class Mutable {
                    private int value;
                    void set(int v) { this.value = v; } // modifying: assigns a field
                    int get() { return value; }
                }

                @Container
                interface HasAdd {
                    void add(Mutable m);
                }

                class BadImpl implements HasAdd {
                    @Override
                    public void add(Mutable m) { m.set(3); } // modifies the argument → violates @Container
                }

                // Uses java.util: count() only reads the list. Computing it @NotModified requires knowing
                // java.util.List.size() is @NotModified — i.e. the preloaded JDK analysis hints.
                class Holder {
                    private final java.util.List<String> items = new java.util.ArrayList<>();
                    int count() { return items.size(); }
                }
                """);

        DaemonProtocol.AnalyzeConfig config = new DaemonProtocol.AnalyzeConfig(
                projectDir.toAbsolutePath().toString(),
                System.getProperty("java.home"),
                "UTF-8",
                List.of("java.base"),
                List.of(new DaemonProtocol.SourceRoot("main", "src", false)),
                List.of(), // no user classpath: the e2immu annotations come from the daemon's own classpath
                List.of(),
                false);

        DaemonProtocol.Result result = new WarmAnalysisService()
                .analyze(new DaemonProtocol.AnalyzeProject("req-1", config), status -> { });

        assertNotNull(result);
        assertTrue(result.parseErrorCount() == 0,
                "unexpected parse errors: " + describeFindings(result.findings()));

        // (a) the guard finding
        DaemonProtocol.Finding violation = result.findings().stream()
                .filter(f -> "contract-violation".equals(f.category()))
                .findFirst()
                .orElse(null);
        assertNotNull(violation, "expected a contract-violation; got: " + describeFindings(result.findings()));
        assertTrue("ERROR".equals(violation.severity()), "contract violation should be an ERROR");
        assertNotNull(violation.uri(), "violation should be located in a source file");
        assertFalse(violation.causes() == null || violation.causes().isEmpty(),
                "violation should carry a why-chain (causes)");

        // (b) the computed @Container element annotation (on the contracted interface)
        boolean containerShown = result.elementAnnotations().stream()
                .flatMap(e -> e.displayAnnotations().stream())
                .anyMatch(a -> a.contains("@Container"));
        assertTrue(containerShown, "expected a @Container element annotation in the results");

        // element annotations should be located and typed
        assertTrue(result.elementAnnotations().stream().anyMatch(e -> "TYPE".equals(e.kind())),
                "expected at least one TYPE element annotation");

        // (c) the JDK/library analysis hints were preloaded
        assertTrue(result.hintsLoaded() > 0, "expected JDK/library analysis hints to be preloaded");

        // (c2) a modified parameter is rendered @Modified explicitly (BadImpl.add(m) modifies m)
        boolean modifiedParamShown = result.elementAnnotations().stream()
                .filter(e -> "PARAMETER".equals(e.kind()))
                .flatMap(e -> e.displayAnnotations().stream())
                .anyMatch(a -> a.equals("@Modified"));
        assertTrue(modifiedParamShown, "expected a @Modified parameter annotation (BadImpl.add's argument)");

        // (d) hints make library-dependent analysis correct: count() only reads list.size() → @NotModified
        //     (requires the java.util hints to be loaded and decoded)
        boolean countNotModified = result.elementAnnotations().stream()
                .filter(e -> "METHOD".equals(e.kind()) && e.fqn() != null && e.fqn().contains("count"))
                .flatMap(e -> e.displayAnnotations().stream())
                .anyMatch(a -> a.contains("@NotModified"));
        assertTrue(countNotModified,
                "expected Holder.count() @NotModified (needs java.util.List.size() hints); annotations: "
                        + result.elementAnnotations().stream()
                        .filter(e -> e.fqn() != null && e.fqn().contains("count"))
                        .map(e -> e.fqn() + " " + e.displayAnnotations()).toList());
    }

    private static String describeFindings(List<DaemonProtocol.Finding> findings) {
        StringBuilder sb = new StringBuilder();
        for (DaemonProtocol.Finding f : findings) {
            sb.append("\n  [").append(f.severity()).append("/").append(f.category()).append("] ")
                    .append(f.message());
        }
        return sb.toString();
    }
}
