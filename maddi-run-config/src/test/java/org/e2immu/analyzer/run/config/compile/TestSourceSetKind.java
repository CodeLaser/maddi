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
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ⛔⛔ A SOURCE SET MUST SAY IT IS A TEST SET (`#89`), AND AN ABSENT BOOLEAN IS THE HARDEST KIND OF WRONG: every
 * consumer downstream has a defensible default, and none of them can tell {@code false} from <i>not stated</i>.
 * The first elasticsearch configuration declared 3 244 test files and marked none of them, which cost three
 * wrong answers at once — a graph edge separation that never fired, a build-unit dependency that could not tell
 * a test dependent from a production one, and a splitter that assigned no tests to any part.
 * <p>
 * This pins the two ways {@link CompileListToSourceSets} used to get the kind wrong, measured on elasticsearch's
 * 348 source sets, where the old rule gets <b>49</b> of them wrong:
 * <ul>
 *   <li><b>47 tests as production</b> — {@code internalClusterTest} was not in the literal list of test names,
 *       and a literal list cannot be complete: a Gradle source set's output directory <i>is</i> its name, and a
 *       build declares as many as it likes ({@code internalClusterTest}, {@code javaRestTest},
 *       {@code yamlRestTest});</li>
 *   <li><b>2 production as tests</b> — the scan ran over every component of the destination path, so a
 *       <i>project</i> directory called {@code test} or {@code testFixtures} decided the question for a
 *       {@code main} source set underneath it.</li>
 * </ul>
 */
public class TestSourceSetKind {

    private record Invocation(String destination) implements CompileInvocation {
        @Override
        public List<String> classpath() {
            return List.of();
        }

        @Override
        public List<String> modulePath() {
            return null;
        }

        /**
         * ⚠ EVERY INVOCATION NEEDS ITS OWN SOURCE ROOT, and that is not fixture decoration.
         * {@code compute} drops a source set whose source directories are contained in a later one's, and
         * {@code containsAll} of an EMPTY set is true — so a fixture with no source roots collapses to a single
         * source set and every assertion below would be about the wrong object.
         */
        @Override
        public List<String> sourcePath() {
            return List.of(destination.replace("/build/classes/java/", "/src/").replace("/target/", "/src/"));
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

    private static final String ROOT = "/checkout/elasticsearch";

    /**
     * ⚠ THE FIXTURE IS A WHOLE BUILD, NOT ONE INVOCATION, because {@code computeName} is a function of the whole
     * run: it walks up the destination until a path suffix is rare (frequency ≤ 2) across ALL destinations. Ask
     * it about a single project and {@code .../build/classes/java/internalClusterTest} is already unique at
     * {@code java/}, so the name comes out as {@code java/internalClusterTest}. These are the destinations of a
     * plausible multi-project build, so the heuristic climbs to the project directory as it does on a real one.
     */
    private static final List<String> BUILD = List.of(
            "/x-pack/plugin/analytics/build/classes/java/main",
            "/x-pack/plugin/analytics/build/classes/java/test",
            "/x-pack/plugin/analytics/build/classes/java/internalClusterTest",
            "/x-pack/plugin/analytics/build/classes/java/javaRestTest",
            "/x-pack/plugin/security/build/classes/java/main",
            "/x-pack/plugin/security/build/classes/java/test",
            "/x-pack/plugin/security/build/classes/java/internalClusterTest",
            "/x-pack/plugin/security/build/classes/java/javaRestTest",
            "/x-pack/plugin/ccr/build/classes/java/main",
            "/x-pack/plugin/ccr/build/classes/java/internalClusterTest",
            "/x-pack/plugin/ccr/build/classes/java/javaRestTest",
            "/x-pack/plugin/esql/compute/test/build/classes/java/main",
            "/x-pack/plugin/esql/qa/testFixtures/build/classes/java/main",
            "/test/framework/build/classes/java/main",
            "/test/framework/build/classes/java/test",
            "/server/build/classes/java/main",
            "/server/build/classes/java/test");

    private static Map<String, SourceSet> compute(List<String> destinations) {
        List<Invocation> list = destinations.stream().map(d -> new Invocation(ROOT + d)).toList();
        return new CompileListToSourceSets().compute(list).jSourceSets().stream()
                .collect(Collectors.toMap(js -> js.invocation().destination(),
                        CompileListToSourceSets.JSourceSet::sourceSet));
    }

    private static final Map<String, SourceSet> SETS = compute(BUILD);

    private static SourceSet set(String destination) {
        SourceSet sourceSet = SETS.get(ROOT + destination);
        assertNotNull(sourceSet, destination + " is not in the fixture");
        return sourceSet;
    }

    /** ⚠ CONTROL FIRST, or nothing below proves anything: the two kinds that always worked still do. */
    @DisplayName("CONTROL: gradle main/test and maven classes/test-classes")
    @Test
    public void control() {
        assertFalse(set("/server/build/classes/java/main").test());
        assertTrue(set("/server/build/classes/java/test").test());

        Map<String, SourceSet> maven = compute(List.of("/core/target/classes", "/core/target/test-classes",
                "/persistence/target/classes", "/persistence/target/test-classes"));
        assertFalse(maven.get(ROOT + "/core/target/classes").test());
        assertTrue(maven.get(ROOT + "/core/target/test-classes").test());
    }

    /**
     * ⛔⛔ 47 OF ELASTICSEARCH'S 348 SOURCE SETS. A Gradle build declares whatever test source sets it wants, and
     * the output directory carries the name, so the rule is the naming convention rather than a list.
     */
    @DisplayName("a gradle test source set outside the literal list is still a test source set")
    @Test
    public void extraGradleTestSourceSets() {
        for (String kind : List.of("internalClusterTest", "javaRestTest")) {
            SourceSet sourceSet = set("/x-pack/plugin/analytics/build/classes/java/" + kind);
            assertTrue(sourceSet.test(), kind + " must be a test source set");
            assertEquals("analytics/" + kind, sourceSet.name(), "and the kind must reach the NAME too");
        }
    }

    /**
     * ⛔⛔ THE OTHER DIRECTION, AND IT IS THE ONE THAT LOOKS HARMLESS. A project living under a directory named
     * {@code test} does not thereby become test code — {@code test/framework} and {@code esql/compute/test} are
     * production libraries.
     */
    @DisplayName("a project directory named test does not make a main source set a test source set")
    @Test
    public void projectDirectoryNamedTest() {
        assertFalse(set("/test/framework/build/classes/java/main").test());
        assertFalse(set("/x-pack/plugin/esql/compute/test/build/classes/java/main").test());
        assertFalse(set("/x-pack/plugin/esql/qa/testFixtures/build/classes/java/main").test());
        // ...and the same module's own test set still is one
        assertTrue(set("/test/framework/build/classes/java/test").test());
    }

    /**
     * ⛔ THE NAME COLLISION THE MISSING KIND CAUSED, which is how it became visible at all: with no kind, an
     * {@code internalClusterTest} set falls back to {@code /main} and collides with its own module's main set,
     * so {@code duplicateNamePrevention} hands out {@code main2} — an arbitrary, order-dependent name for a
     * source set that had a perfectly good one. On elasticsearch that was 45 of the 54 counter-suffixed names.
     */
    @DisplayName("a module's main and extra test sets no longer collide into main/main2")
    @Test
    public void noCollisionBetweenMainAndExtraTestSet() {
        List<String> analytics = SETS.entrySet().stream()
                .filter(e -> e.getKey().contains("/plugin/analytics/"))
                .map(e -> e.getValue().name()).sorted().toList();

        assertEquals(List.of("analytics/internalClusterTest", "analytics/javaRestTest", "analytics/main",
                "analytics/test"), analytics);
        assertEquals(3, SETS.entrySet().stream()
                .filter(e -> e.getKey().contains("/plugin/analytics/"))
                .filter(e -> e.getValue().test()).count());
    }

    /**
     * ⛔⛔ PINNED BECAUSE IT IS A DEFECT, NOT BECAUSE IT IS RIGHT — and it is the reason the two tests above need
     * a whole build rather than one invocation.
     *
     * <p>{@code computeName} walks up the destination until a path suffix is rare across <b>all</b> destinations
     * in the run (frequency ≤ 2). So <b>a source set's name is a function of which other projects were compiled
     * beside it</b>: the very same output directory is {@code java/internalClusterTest} in a build where it is
     * the only one of its kind, and {@code analytics/internalClusterTest} once three projects have one. Adding an
     * unrelated project renames an existing source set.
     *
     * <p>⚠ {@code SourceSet}'s own contract says the name IS the identity — <i>"source sets are identified by
     * their name() throughout the system, including in serialized dependency references"</i>. An identity that
     * depends on the rest of the run is not an identity. This test exists so the day that is fixed, it fails and
     * says what changed.
     */
    @DisplayName("PINNED DEFECT: the same output directory gets a different name in a different build")
    @Test
    public void theNameIsAFunctionOfTheWholeRun() {
        String destination = "/x-pack/plugin/analytics/build/classes/java/internalClusterTest";

        SourceSet alone = compute(List.of(destination)).get(ROOT + destination);
        assertEquals("java/internalClusterTest", alone.name());

        assertEquals("analytics/internalClusterTest", set(destination).name());
        assertTrue(alone.test() && set(destination).test(), "the KIND, at least, does not drift");
    }
}
