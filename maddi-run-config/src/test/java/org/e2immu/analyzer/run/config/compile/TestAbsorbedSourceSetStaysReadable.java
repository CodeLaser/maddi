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
 * ⛔⛔ <b>TWO INVOCATIONS OVER ONE SOURCE TREE HAVE TWO DESTINATIONS, AND ONLY ONE OF THEM SURVIVES.</b>
 * {@code compute} drops a source set whose source directories another invocation also compiles. That rule is
 * right — the same tree parsed twice is a duplicate — but it used to drop the <em>output directory</em> with it,
 * and everything compiled against the loser's output still names it.
 * <p>
 * ⚠ <b>THE RULE WAS ALREADY WRITTEN DOWN — AS A FIXTURE NUISANCE.</b> {@code TestSourceSetKind.Invocation}
 * carries a javadoc explaining that every invocation needs its own source root because {@code containsAll} of an
 * empty set is true and the fixture would otherwise collapse. The same sentence describes a production defect,
 * and nothing followed it there.
 * <p>
 * ⛔⛔ <b>MEASURED, ON ELASTICSEARCH, 2026-08-08.</b> {@code libs/native} is compiled twice from the same 38
 * files — once for real, once with {@code -proc:only} into {@code generated-foreign-library-classes}. The second
 * absorbed the first, so {@code libs/native/main} — named as a dependency by <b>208 of 348</b> source sets —
 * existed nowhere in the configuration. Six reconciliation checks passed over it. What said so, a day later and
 * 214 s into a run, was <i>"package org.elasticsearch.nativeaccess does not exist"</i>, one dropped compilation
 * unit, and {@code Summary.parseResult()} refusing the entire {@code ParseResult} over it.
 */
public class TestAbsorbedSourceSetStaysReadable {

    private static final String ROOT = "/checkout/es";

    /** classpath and sourcePath are the two axes this test varies; everything else is fixture. */
    private record Invocation(String destination, List<String> sourcePath, List<String> classpath)
            implements CompileInvocation {
        @Override
        public List<String> modulePath() {
            return null;
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

    private static final String NATIVE_MAIN = ROOT + "/libs/native/build/classes/java/main";
    private static final String NATIVE_PROC = ROOT + "/libs/native/build/generated-foreign-library-classes";
    private static final String NATIVE_SOURCES = ROOT + "/libs/native/src/main/java";
    private static final String SERVER_MAIN = ROOT + "/server/build/classes/java/main";

    /** The real compile, the {@code -proc:only} pass over the SAME sources, and a consumer of the first. */
    private static CompileListToSourceSets.Result computeElasticsearchShape() {
        return new CompileListToSourceSets(ROOT).compute(List.of(
                new Invocation(NATIVE_MAIN, List.of(NATIVE_SOURCES), List.of()),
                new Invocation(NATIVE_PROC, List.of(NATIVE_SOURCES), List.of()),
                new Invocation(SERVER_MAIN, List.of(ROOT + "/server/src/main/java"), List.of(NATIVE_MAIN))));
    }

    private static List<String> names(List<SourceSet> sets) {
        return sets.stream().map(SourceSet::name).sorted().toList();
    }

    /**
     * ⚠ CONTROL FIRST. Two invocations with their OWN source roots are not duplicates, so nothing is absorbed
     * and nothing is demoted — otherwise every assertion below could be explained by "it demotes everything".
     */
    @DisplayName("CONTROL: distinct source roots, so nothing is absorbed and no library is invented")
    @Test
    public void distinctSourceRootsAbsorbNothing() {
        CompileListToSourceSets.Result result = new CompileListToSourceSets(ROOT).compute(List.of(
                new Invocation(NATIVE_MAIN, List.of(NATIVE_SOURCES), List.of()),
                new Invocation(SERVER_MAIN, List.of(ROOT + "/server/src/main/java"), List.of(NATIVE_MAIN))));

        assertEquals(List.of("libs/native/main", "server/main"),
                names(result.jSourceSets().stream().map(CompileListToSourceSets.JSourceSet::sourceSet).toList()));
        assertEquals(List.of(), names(result.jars()));
    }

    @DisplayName("the absorbed source set comes back as a library, under its own name")
    @Test
    public void theAbsorbedSetBecomesALibrary() {
        CompileListToSourceSets.Result result = computeElasticsearchShape();

        // the containment rule still fires: one destination per source tree
        assertEquals(List.of("libs/native/generated-foreign-library-classes", "server/main"),
                names(result.jSourceSets().stream().map(CompileListToSourceSets.JSourceSet::sourceSet).toList()));

        // ⛔ and the loser is READABLE rather than absent -- this is the whole fix
        SourceSet library = result.jars().stream().filter(j -> "libs/native/main".equals(j.name()))
                .findFirst().orElseThrow(() -> new AssertionError("libs/native/main is nowhere: " + names(result.jars())));
        assertTrue(library.library());
        assertTrue(library.externalLibrary());
        assertEquals(List.of(), library.sourceDirectories(), "nothing parses it");
        assertEquals("file:" + NATIVE_MAIN, library.uri().toString(),
                "the same output directory the dependents were compiled against");
    }

    @DisplayName("end to end: the consumer's dependency resolves, so build() accepts the configuration")
    @Test
    public void theDependencyResolves() {
        InputConfiguration ic = CompileListToInputConfiguration.build(computeElasticsearchShape(), List.of());

        SourceSet server = ic.sourceSets().stream().filter(s -> "server/main".equals(s.name()))
                .findFirst().orElseThrow();
        assertTrue(names(server.dependencies()).contains("libs/native/main"),
                "the edge is still there: " + names(server.dependencies()));
        assertTrue(ic.classPathParts().stream().anyMatch(p -> "libs/native/main".equals(p.name())),
                "and it points at something");
    }

    /**
     * ⛔⛔ THE GATE ITSELF, tested apart from the defect it was written for. A configuration whose edges point at
     * nothing must be refused where it is BUILT — the alternative is what actually happened: a clean generation,
     * a clean reconciliation, and a parse failure a day later naming the victim rather than the cause.
     */
    @DisplayName("a dependency that names nothing is refused, with the name and how many sets rely on it")
    @Test
    public void aDanglingDependencyIsRefused() {
        SourceSet ghost = new SourceSetImpl.Builder().setName("libs/native/main")
                .setSourceDirectories(List.of(Path.of("/gone")))
                .setUri(URI.create("file:/gone")).build();
        SourceSet consumer = new SourceSetImpl.Builder().setName("server/main")
                .setSourceDirectories(List.of(Path.of("/p/server")))
                .setUri(URI.create("file:/p/server/classes"))
                .setDependencies(List.of(ghost)).build();

        IllegalStateException e = assertThrows(IllegalStateException.class, () ->
                CompileListToInputConfiguration.build(new CompileListToSourceSets.Result(
                        List.of(new CompileListToSourceSets.JSourceSet(null, consumer)), List.of(), null),
                        List.of()));

        assertTrue(e.getMessage().contains("libs/native/main"), e.getMessage());
        assertTrue(e.getMessage().contains("will not resolve"), e.getMessage());
        assertTrue(e.getMessage().contains("server/main"), e.getMessage());
    }
}
