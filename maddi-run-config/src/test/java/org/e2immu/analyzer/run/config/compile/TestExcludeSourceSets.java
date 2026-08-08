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
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ⛔⛔ A COMPILE TASK LIST CANNOT KEEP A SOURCE SET OUT OF THE PARSE, which is the entire reason this exists.
 * Gradle compiles a requested task's dependencies whether you asked for them or not, and every compile emits a
 * javac line, so every dependency becomes a source set. Measured on elasticsearch: a task list naming exactly
 * the 348 wanted source sets brought <b>23 unwanted ones</b> in as dependencies — {@code repository-gcs}, whose
 * jars carry four classes that cannot be committed and where one bad compilation unit refuses the whole
 * {@code ParseResult}, arrived via {@code x-pack/plugin/stateless}.
 * <p>
 * ⭐ AND THE EXCLUDED SET IS DEMOTED, NOT DROPPED. Its output directory becomes a library under the same name,
 * so its types still resolve for its dependents — an exclusion therefore does <b>not</b> have to be closed under
 * "who depends on this", which is the trap the hand-written generator this replaces kept falling into.
 */
public class TestExcludeSourceSets {

    private static SourceSet sourceSet(String name, List<SourceSet> dependencies) {
        return new SourceSetImpl.Builder()
                .setName(name)
                .setSourceDirectories(List.of(Path.of("/p/" + name)))
                .setUri(URI.create("file:/p/" + name + "/classes"))
                .setDependencies(dependencies)
                .build();
    }

    /** producer <- consumer, plus an unrelated set, which is the shape the elasticsearch case has. */
    private static final SourceSet PRODUCER = sourceSet("modules/repository-gcs/main", List.of());
    private static final SourceSet CONSUMER = sourceSet("x-pack/plugin/stateless/test", List.of(PRODUCER));
    private static final SourceSet OTHER = sourceSet("server/main", List.of());

    private static InputConfiguration build(List<String> excluded) {
        return CompileListToInputConfiguration.build(
                new CompileListToSourceSets.Result(
                        List.of(PRODUCER, CONSUMER, OTHER).stream()
                                .map(s -> new CompileListToSourceSets.JSourceSet(null, s)).toList(),
                        List.of()),
                List.of(), excluded);
    }

    private static List<String> names(List<SourceSet> sourceSets) {
        return sourceSets.stream().map(SourceSet::name).sorted().toList();
    }

    /** ⚠ CONTROL FIRST: with no exclusions all three are source sets, or nothing below distinguishes anything. */
    @DisplayName("CONTROL: no exclusions, every source set stays")
    @Test
    public void noExclusions() {
        InputConfiguration ic = build(List.of());

        assertEquals(List.of("modules/repository-gcs/main", "server/main", "x-pack/plugin/stateless/test"),
                names(ic.sourceSets()));
    }

    /** ⛔⛔ THE HEADLINE: excluded, and its CONSUMER stays — pointed at the library it became. */
    @DisplayName("an excluded source set becomes a library, and its dependent still resolves it")
    @Test
    public void excludedBecomesALibrary() {
        InputConfiguration ic = build(List.of("modules/repository-gcs/main"));

        assertEquals(List.of("server/main", "x-pack/plugin/stateless/test"), names(ic.sourceSets()));

        SourceSet library = ic.classPathParts().stream()
                .filter(p -> "modules/repository-gcs/main".equals(p.name())).findFirst().orElseThrow();
        assertTrue(library.library());
        assertTrue(library.externalLibrary());
        assertFalse(library.parsedFromSource(), "nothing parses it, so no lever can edit it");
        assertEquals(List.of(), library.sourceDirectories());
        assertEquals("file:/p/modules/repository-gcs/main/classes", library.uri().toString(),
                "the same output directory: its types are still readable");

        // ⭐ THE EDGE IS RE-POINTED, which is what makes closure under "who depends on this" unnecessary
        SourceSet consumer = ic.sourceSets().stream()
                .filter(s -> "x-pack/plugin/stateless/test".equals(s.name())).findFirst().orElseThrow();
        assertEquals(List.of("modules/repository-gcs/main"), names(consumer.dependencies()));
        assertTrue(consumer.dependencies().getFirst().library());
    }

    /**
     * ⛔⛔ AN EXCLUSION THAT MATCHES NOTHING IS REFUSED. It would give a WIDER parse than was asked for, with a
     * zero exit — and after the elasticsearch switch renamed 338 of 348 source sets in one step, a stale
     * exclusion list is the likeliest way to get one. A warning in a log is not enough for that.
     */
    @DisplayName("an exclusion that matches no source set is refused, with the likely rename named")
    @Test
    public void unmatchedExclusionIsRefused() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> build(List.of("repository-gcs/main")));

        assertTrue(e.getMessage().contains("repository-gcs/main"), e.getMessage());
        assertTrue(e.getMessage().contains("widens the parse silently"), e.getMessage());
        // the message has to carry the answer, because the cause is nearly always a rename
        assertTrue(e.getMessage().contains("modules/repository-gcs/main"), e.getMessage());
    }

    /** Excluding everything a set depends on leaves it standing: the point of demoting rather than deleting. */
    @DisplayName("a source set whose every dependency is excluded is still parsed")
    @Test
    public void consumerSurvivesLosingAllDependencies() {
        InputConfiguration ic = build(List.of("modules/repository-gcs/main"));

        assertTrue(names(ic.sourceSets()).contains("x-pack/plugin/stateless/test"));
    }
}
