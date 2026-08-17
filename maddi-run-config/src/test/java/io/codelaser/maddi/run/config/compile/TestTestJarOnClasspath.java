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

package org.e2immu.analyzer.run.config.compile;

import org.e2immu.language.cst.api.element.SourceSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ⛔⛔ <b>A MODULE'S TEST-JAR SHARES AN OUTPUT ROOT WITH ITS MAIN JAR, AND WAS HANDED TO THE MAIN SOURCE SET.</b>
 * {@link TestReactorJarOnClasspath} established that a reactor sibling's packaged jar is that sibling's source
 * set, matched BY PATH because the artifactId and the module directory are different strings on maven. The match
 * was to the output root alone — and {@code maven-jar-plugin:test-jar} puts {@code <artifact>-tests.jar} in the
 * very same {@code <mod>/target/} as {@code <artifact>.jar}.
 *
 * <p>So a sibling that publishes test fixtures resolved to {@code <mod>/main}, and every fixture it carries left
 * the parse without a word. The failure surfaces far from its cause:
 *
 * <table><caption></caption>
 * <tr><td>what actually happened</td><td>{@code core/test-classes} never became a dependency</td></tr>
 * <tr><td>what javac said</td><td>{@code package ai.timefold.solver.core.testutil does not exist}</td></tr>
 * <tr><td>what the scanner then said, 100 lines later</td>
 *     <td>{@code Unexpected symbol for unqualified call to 'assertCode'}</td></tr>
 * </table>
 *
 * <p>Measured on timefold 2026-08-14: <b>79 such messages over 9 compilation units</b>, in
 * {@code collectors}, {@code search}, {@code benchmark}, both {@code persistence} adapters and
 * {@code quarkus-jackson} — every module whose tests statically import {@code core.testutil}. The parse refused
 * to produce a usable result, so no DSL verb could run against the corpus at all.
 *
 * <p>⚠ <b>THE CLASSIFIER IS A SAFE SIGNAL ONLY BECAUSE MAVEN PUTS IT AFTER THE VERSION.</b> That is what
 * separates a test-jar from a module whose artifactId merely ends in {@code -test}, and it is CONTROL 1 below.
 */
public class TestTestJarOnClasspath {

    private static final String ROOT = "/checkout/timefold";

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

    private static final String CORE_MAIN = ROOT + "/core/target/classes";
    private static final String CORE_TEST = ROOT + "/core/target/test-classes";
    private static final String CORE_JAR = ROOT + "/core/target/timefold-solver-core-999-SNAPSHOT.jar";
    private static final String CORE_TESTS_JAR = ROOT + "/core/target/timefold-solver-core-999-SNAPSHOT-tests.jar";

    private static final String COLLECTORS_MAIN = ROOT + "/collectors/target/classes";
    private static final String COLLECTORS_TEST = ROOT + "/collectors/target/test-classes";

    /** CONTROL 1: an artifactId that ENDS IN {@code -test}. Its main jar must stay main. */
    private static final String IT_MAIN = ROOT + "/integration-test/target/classes";
    private static final String IT_JAR =
            ROOT + "/integration-test/target/timefold-solver-integration-test-999-SNAPSHOT.jar";

    /** CONTROL 2: a module with NO test destination at all, whose output root nonetheless holds a test-jar. */
    private static final String LEGACY_MAIN = ROOT + "/legacy/target/classes";
    private static final String LEGACY_TESTS_JAR = ROOT + "/legacy/target/legacy-1.0-tests.jar";

    private static CompileListToSourceSets.Result computeTimefoldShape() {
        return new CompileListToSourceSets(ROOT).compute(List.of(
                new Invocation(CORE_MAIN, List.of(ROOT + "/core/src/main/java"), List.of()),
                new Invocation(CORE_TEST, List.of(ROOT + "/core/src/test/java"), List.of(CORE_MAIN)),
                new Invocation(LEGACY_MAIN, List.of(ROOT + "/legacy/src/main/java"), List.of()),
                new Invocation(IT_MAIN, List.of(ROOT + "/integration-test/src/main/java"), List.of()),
                new Invocation(COLLECTORS_MAIN, List.of(ROOT + "/collectors/src/main/java"), List.of(CORE_JAR)),
                // the real shape: main jar AND test-jar of the same sibling, side by side
                new Invocation(COLLECTORS_TEST, List.of(ROOT + "/collectors/src/test/java"),
                        List.of(COLLECTORS_MAIN, CORE_JAR, CORE_TESTS_JAR, IT_JAR, LEGACY_TESTS_JAR))));
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
     * ⚠ CONTROL FIRST. Without it the defect reads as "collectors/test-classes depends on core/main", which is
     * true and looks complete — the fixtures are missing, not the module.
     */
    @DisplayName("CONTROL: the sibling's MAIN jar still resolves to its main source set")
    @Test
    public void theMainJarStillResolvesToMain() {
        List<String> deps = names(named(computeTimefoldShape(), "collectors/test-classes").dependencies());
        assertTrue(deps.contains("core/main"),
                "the main jar must keep mapping to core/main: " + deps);
    }

    @DisplayName("a sibling's TEST-jar resolves to that sibling's test source set, not to its main one")
    @Test
    public void theTestJarResolvesToTheTestSourceSet() {
        List<String> deps = names(named(computeTimefoldShape(), "collectors/test-classes").dependencies());
        assertTrue(deps.contains("core/test-classes"),
                "collectors/test-classes reads core's fixtures through timefold-solver-core-999-SNAPSHOT-tests.jar,"
                + " so core/test-classes must be a dependency: " + deps);
    }

    @DisplayName("CONTROL 1: an artifactId ending in -test is not a test-jar, because the classifier follows the version")
    @Test
    public void anArtifactIdEndingInTestIsNotATestJar() {
        List<String> deps = names(named(computeTimefoldShape(), "collectors/test-classes").dependencies());
        assertTrue(deps.contains("integration-test/main"),
                "timefold-solver-integration-test-999-SNAPSHOT.jar is a MAIN jar: " + deps);
        assertFalse(deps.contains("integration-test/test-classes"),
                "there is no such source set, and inventing one would be the mirror of the bug: " + deps);
    }

    @DisplayName("CONTROL 2: with no test destination in the output root, a test-jar falls back to main")
    @Test
    public void withoutATestDestinationTheFallbackIsMain() {
        List<String> deps = names(named(computeTimefoldShape(), "collectors/test-classes").dependencies());
        assertTrue(deps.contains("legacy/main"),
                "legacy publishes no test source set, so the previous behaviour must be preserved: " + deps);
    }
}
