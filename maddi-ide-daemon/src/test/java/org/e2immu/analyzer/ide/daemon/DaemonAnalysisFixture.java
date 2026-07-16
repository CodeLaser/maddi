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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Test harness for daemon analysis: write a Java snippet into a temp project, run the real pipeline, and
 * return the plain-JSON result — plus small query helpers so a fixture test is a few lines.
 * <p>
 * The e2immu annotations come from the daemon's own classpath, so snippets need no classpath of their own;
 * only {@code java.base} is on the module list. {@code sdkHome} is the test JVM (JDK 25+).
 */
public final class DaemonAnalysisFixture {
    private DaemonAnalysisFixture() {
    }

    /** Analyze a single source file (path relative to the source root, e.g. {@code "x/Foo.java"}). */
    public static DaemonProtocol.Result analyze(Path projectDir, String relativePath, String source)
            throws Exception {
        Path file = projectDir.resolve("src").resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, source);
        DaemonProtocol.AnalyzeConfig config = new DaemonProtocol.AnalyzeConfig(
                projectDir.toAbsolutePath().toString(),
                System.getProperty("java.home"),
                "UTF-8",
                List.of("java.base"),
                List.of(new DaemonProtocol.SourceRoot("main", "src", false)),
                List.of(),
                List.of(),
                false);
        return new WarmAnalysisService().analyze(new DaemonProtocol.AnalyzeProject("test", config), s -> { });
    }

    /** The display annotations computed for the first element of {@code kind} whose fqn contains {@code fqnPart}. */
    public static List<String> displayFor(DaemonProtocol.Result result, String kind, String fqnPart) {
        return result.elementAnnotations().stream()
                .filter(e -> kind.equals(e.kind()) && e.fqn() != null && e.fqn().contains(fqnPart))
                .flatMap(e -> e.displayAnnotations().stream())
                .toList();
    }

    /** The first finding of the given category, if any. */
    public static Optional<DaemonProtocol.Finding> finding(DaemonProtocol.Result result, String category) {
        return result.findings().stream().filter(f -> category.equals(f.category())).findFirst();
    }
}
