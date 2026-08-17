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

package io.codelaser.maddi.run.config.compile;

import io.codelaser.maddi.cst.api.element.SourceSet;
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
     * ⚠ THE FIXTURE IS A WHOLE BUILD, NOT ONE INVOCATION. It has to be: the defect being pinned is that a
     * module's {@code main} and its extra test sets used to collide into {@code main}/{@code main2}, which only
     * one module with several source sets can show. It is also the shape that would have exposed the naming
     * heuristic this class replaced — see {@link #theNameDoesNotDependOnTheRestOfTheRun()}.
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
            "/libs/entitlement/build/classes/java/main",
            "/libs/entitlement/build/classes/java/main25",
            "/libs/entitlement/build/classes/java/main26",
            "/x-pack/plugin/esql/compute/test/build/classes/java/main",
            "/x-pack/plugin/esql/qa/testFixtures/build/classes/java/main",
            "/test/framework/build/classes/java/main",
            "/test/framework/build/classes/java/test",
            "/server/build/classes/java/main",
            "/server/build/classes/java/test");

    private static Map<String, SourceSet> compute(List<String> destinations) {
        return compute(destinations, ROOT);
    }

    private static Map<String, SourceSet> compute(List<String> destinations, String buildRoot) {
        List<Invocation> list = destinations.stream().map(d -> new Invocation(ROOT + d)).toList();
        return new CompileListToSourceSets(buildRoot).compute(list).jSourceSets().stream()
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
            assertEquals("x-pack/plugin/analytics/" + kind, sourceSet.name(),
                    "and the kind must reach the NAME too");
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

        assertEquals(List.of("x-pack/plugin/analytics/internalClusterTest", "x-pack/plugin/analytics/javaRestTest",
                "x-pack/plugin/analytics/main", "x-pack/plugin/analytics/test"), analytics);
        assertEquals(3, SETS.entrySet().stream()
                .filter(e -> e.getKey().contains("/plugin/analytics/"))
                .filter(e -> e.getValue().test()).count());
    }

    /**
     * ⭐⭐ THE PROPERTY THE NAMING RULE EXISTS FOR, and it is asserted rather than assumed: <b>a source set's name
     * does not depend on what else was compiled beside it.</b>
     *
     * <p>What stood here was a frequency heuristic — it walked up the destination until a path suffix was rare
     * across the whole run — so the same output directory was {@code java/internalClusterTest} in a build where
     * it was the only one of its kind and {@code analytics/internalClusterTest} once three projects had one, and
     * on elasticsearch {@code :server} came out named after the CHECKOUT DIRECTORY. {@link SourceSet}'s contract
     * says the name IS the identity; an identity that depends on the rest of the run is not one.
     *
     * <p>⚠ AND THAT IS WHY THE BUILD ROOT IS AN INPUT. Derived, it is the common ancestor of the modules that
     * happened to compile, so narrowing a parse to two sibling modules would shorten it and rename everything it
     * kept — the second half of this test. Given the build directory, nothing moves.
     */
    @DisplayName("a name does not depend on what else was compiled, once the build root is given")
    @Test
    public void theNameDoesNotDependOnTheRestOfTheRun() {
        String destination = "/x-pack/plugin/analytics/build/classes/java/internalClusterTest";

        SourceSet alone = compute(List.of(destination)).get(ROOT + destination);
        assertEquals("x-pack/plugin/analytics/internalClusterTest", alone.name());
        assertEquals(alone.name(), set(destination).name(), "the same set, in a build of twelve");

        // ⚠ THE CONTROL FOR THE INPUT ITSELF: without a build root the name is only as stable as the module set
        SourceSet derived = compute(List.of(destination), null).get(ROOT + destination);
        assertEquals("analytics/internalClusterTest", derived.name());
    }

    /**
     * ⚠ A MIXED MODULE COMPILES ONE GRADLE SOURCE SET TWICE, with two compilers and two parsers, so the two
     * outputs must stay two source sets. Java is the unmarked case — a Java-only build never carries a language
     * segment — and the Kotlin output takes one rather than an arrival-order counter.
     */
    @DisplayName("a mixed module's java and kotlin outputs are distinguished by language, not by a counter")
    @Test
    public void mixedModuleKeepsTwoSourceSets() {
        Map<String, SourceSet> mixed = compute(List.of(
                "/proj/build/classes/java/main", "/proj/build/classes/kotlin/main",
                "/other/build/classes/java/main"));

        assertEquals("proj/main", mixed.get(ROOT + "/proj/build/classes/java/main").name());
        assertEquals("proj/kotlin/main", mixed.get(ROOT + "/proj/build/classes/kotlin/main").name());
    }

    /**
     * ⛔⛔ A MULTI-RELEASE PROJECT'S EXTRA SOURCE SETS ARE SOURCE SETS. Elasticsearch's {@code libs/entitlement}
     * compiles into {@code build/classes/java/main}, {@code main25}, {@code main26} and {@code main27} — four
     * separately compiled Gradle source sets whose output directories are neither {@code main} nor test-shaped.
     * Folding "not a test kind" to {@code main} collided all four and handed out {@code entitlement/main2},
     * {@code main3}, {@code main4} <i>in arrival order</i>, discarding the release the directory named. Found on
     * the real corpus: six source sets across {@code entitlement}, {@code cli-terminal}, {@code foreign-adapter}
     * and {@code native}.
     */
    @DisplayName("a multi-release project's mainNN source sets keep their own names")
    @Test
    public void multiReleaseSourceSets() {
        assertEquals("libs/entitlement/main", set("/libs/entitlement/build/classes/java/main").name());
        assertEquals("libs/entitlement/main25", set("/libs/entitlement/build/classes/java/main25").name());
        assertEquals("libs/entitlement/main26", set("/libs/entitlement/build/classes/java/main26").name());
        // ⚠ and none of them is a test set: `main25` is production code for a newer runtime
        assertFalse(set("/libs/entitlement/build/classes/java/main25").test());
    }
}
