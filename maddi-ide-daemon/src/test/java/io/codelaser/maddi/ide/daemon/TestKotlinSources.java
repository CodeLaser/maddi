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

package io.codelaser.maddi.ide.daemon;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A project with Kotlin sources is analysed through the mixed parse, not skipped. The build hands this JVM the
 * K2 jars as {@code -Dmaddi.k2.classpath} (see {@code build.gradle.kts}), the way an IDE hands a daemon the
 * directory it downloaded them to.
 * <p>
 * The configuration is the per-module shape IntelliJ sends: one {@code ModuleSourceSet} per module, each with its
 * own class path, here kotlin-stdlib (taken from this JVM, which has it through the mixed inspector).
 */
public class TestKotlinSources {

    private static final String POINT = """
            package k

            class Point(val x: Int, val y: Int) {
                fun plus(other: Point): Point = Point(x + other.x, y + other.y)
            }
            """;

    private static final String BAG = """
            package k

            class Bag {
                private val items = ArrayList<String>()
                fun add(s: String) { items.add(s) }
                fun size(): Int = items.size
            }
            """;

    // Java, depending on the Kotlin module
    private static final String USER = """
            package j;
            import k.Point;
            public class User {
                private final Point origin;
                public User(Point origin) {
                    this.origin = origin;
                }
                public Point origin() {
                    return origin;
                }
            }
            """;

    @Test
    public void kotlinOnly(@TempDir Path projectDir) throws Exception {
        Path kDir = write(projectDir, "k/src", "k/Point.kt", POINT);
        write(projectDir, "k/src", "k/Bag.kt", BAG);
        DaemonProtocol.Result result = analyze(projectDir, List.of(set("k/main", kDir, List.of())));

        assertNoKotlinSkipped(result);
        DaemonProtocol.ElementAnnotation point = element(result, "TYPE", "k.Point");
        assertTrue(point.uri().endsWith("k/Point.kt"), point.uri());
        assertEquals(3, (int) point.beginLine(), "class Point is on line 3 of Point.kt");
        assertFalse(point.displayAnnotations().isEmpty(), "Point has a verdict");

        // a modifying and a non-modifying method, told apart
        assertTrue(DaemonAnalysisFixture.displayFor(result, "METHOD", "k.Bag.add").contains("@Modified"),
                () -> "Bag.add: " + DaemonAnalysisFixture.displayFor(result, "METHOD", "k.Bag.add"));
        assertTrue(DaemonAnalysisFixture.displayFor(result, "METHOD", "k.Bag.size").contains("@NotModified"),
                () -> "Bag.size: " + DaemonAnalysisFixture.displayFor(result, "METHOD", "k.Bag.size"));
    }

    @Test
    public void javaDependsOnKotlin(@TempDir Path projectDir) throws Exception {
        Path kDir = write(projectDir, "k/src", "k/Point.kt", POINT);
        Path jDir = write(projectDir, "j/src", "j/User.java", USER);
        DaemonProtocol.Result result = analyze(projectDir, List.of(
                set("k/main", kDir, List.of()),
                set("j/main", jDir, List.of("k/main"))));

        assertNoKotlinSkipped(result);
        assertEquals(0, result.parseErrorCount(), () -> "findings: " + result.findings());
        assertTrue(element(result, "TYPE", "k.Point").uri().endsWith("k/Point.kt"));
        DaemonProtocol.ElementAnnotation user = element(result, "TYPE", "j.User");
        assertTrue(user.uri().endsWith("j/User.java"), user.uri());
        assertEquals(3, (int) user.beginLine());
    }

    /**
     * A data class's synthesized copy() takes parameters named and positioned like the constructor's, each with its
     * own verdict. Only the constructor's may reach the editor: two elements of one kind over one range left the
     * choice to list order.
     */
    @Test
    public void dataClassCopyParametersAreNotShown(@TempDir Path projectDir) throws Exception {
        Path kDir = write(projectDir, "k/src", "k/Pair2.kt", """
                package k

                data class Pair2(val a: String, val b: List<String>)
                """);
        DaemonProtocol.Result result = analyze(projectDir, List.of(set("k/main", kDir, List.of())));
        List<String> parameters = result.elementAnnotations().stream()
                .filter(e -> "PARAMETER".equals(e.kind())).map(DaemonProtocol.ElementAnnotation::fqn).toList();
        assertFalse(parameters.isEmpty());
        assertTrue(parameters.stream().allMatch(fqn -> fqn.contains("<init>")), () -> "parameters: " + parameters);
    }

    private static void assertNoKotlinSkipped(DaemonProtocol.Result result) {
        assertTrue(result.initializationProblems().stream().noneMatch(p -> p.contains("NOT analyzed")),
                () -> "problems: " + result.initializationProblems());
    }

    private static DaemonProtocol.ElementAnnotation element(DaemonProtocol.Result result, String kind, String fqn) {
        return result.elementAnnotations().stream()
                .filter(e -> kind.equals(e.kind()) && fqn.equals(e.fqn()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no " + kind + " " + fqn + " among "
                        + result.elementAnnotations().stream().map(DaemonProtocol.ElementAnnotation::fqn).toList()));
    }

    private static Path write(Path projectDir, String root, String relativePath, String source) throws Exception {
        Path dir = projectDir.resolve(root);
        Path file = dir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, source);
        return dir;
    }

    private static DaemonProtocol.ModuleSourceSet set(String name, Path dir, List<String> dependencies)
            throws Exception {
        String stdlib = Path.of(Class.forName("kotlin.Unit").getProtectionDomain().getCodeSource().getLocation().toURI())
                .toString();
        return new DaemonProtocol.ModuleSourceSet(name, List.of(dir.toString()), null, false, 0, dependencies,
                List.of(stdlib));
    }

    private static DaemonProtocol.Result analyze(Path projectDir, List<DaemonProtocol.ModuleSourceSet> sets)
            throws Exception {
        DaemonProtocol.AnalyzeConfig config = new DaemonProtocol.AnalyzeConfig(
                projectDir.toAbsolutePath().toString(),
                System.getProperty("java.home"),
                "UTF-8",
                List.of("java.base"),
                List.of(),
                List.of(),
                List.of(),
                false,
                false,
                sets);
        return new WarmAnalysisService().analyze(new DaemonProtocol.AnalyzeProject("test", config), status -> { });
    }
}
