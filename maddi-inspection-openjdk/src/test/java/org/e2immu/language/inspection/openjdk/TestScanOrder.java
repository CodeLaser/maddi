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

package org.e2immu.language.inspection.openjdk;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@code JavaInspectorImpl.computeScanOrder}: a source set must be scanned after everything it depends on.
 * <p>
 * The dependency graph carries that when the caller expresses a dependency as another <em>source set</em>. A
 * build tool exporting a multi-module project usually does not: a sibling module arrives as its artifact
 * ({@code b-1.0.jar}), an external library part, which is filtered out — leaving a graph with no edges at all,
 * where the tie-breaker decides everything. The tie-breaker is therefore the caller's own order.
 */
public class TestScanOrder {

    private static SourceSet artifact(String name) {
        return new SourceSetImpl.Builder().setName(name).setUri(URI.create("file:/" + name))
                .setExternalLibrary(true).setLibrary(true).build();
    }

    private static SourceSet sources(String name, SourceSet... dependencies) {
        return new SourceSetImpl.Builder().setName(name).setUri(URI.create("file:/" + name))
                .setDependencies(List.of(dependencies)).build();
    }

    private static List<String> names(List<SourceSet> sets) {
        return sets.stream().map(SourceSet::name).toList();
    }

    /**
     * ⛔ The regression. Three modules of one reactor, each depending on the previous one's JAR: the graph has no
     * edges, and sorting by name would scan {@code constraint-streams} — which reads {@code core} — first. That is
     * how it went wrong on the timefold corpus: {@code core}'s types were materialized from its class files for
     * {@code constraint-streams}, and the later scan of {@code core}'s own sources dropped two compilation units,
     * both at a nested type whose members the class-file pass had already created.
     */
    @Test
    public void declarationOrderWinsWhenSiblingsArriveAsArtifacts() {
        SourceSet coreJar = artifact("core-1.0.jar");
        SourceSet utilJar = artifact("util-1.0.jar");
        SourceSet util = sources("util/main");
        SourceSet core = sources("core/main", utilJar);
        SourceSet constraintStreams = sources("constraint-streams/main", coreJar, utilJar);

        // declared in reactor order, which is NOT alphabetical
        List<SourceSet> declared = List.of(util, core, constraintStreams);
        assertEquals(List.of("util/main", "core/main", "constraint-streams/main"),
                names(JavaInspectorImpl.computeScanOrder(declared)));
    }

    /** A real edge still outranks the declaration order: the graph is consulted first, the tie-breaker second. */
    @Test
    public void realDependenciesOutrankDeclarationOrder() {
        SourceSet base = sources("z-base/main");
        SourceSet onTop = sources("a-on-top/main", base);

        // declared the wrong way round, and the alphabetical order agrees with the declaration; only the edge
        // between them says otherwise, and it must win
        List<SourceSet> declared = List.of(onTop, base);
        assertEquals(List.of("z-base/main", "a-on-top/main"),
                names(JavaInspectorImpl.computeScanOrder(declared)));
    }

    /** With neither edges nor a meaningful declaration order the result is still total and deterministic. */
    @Test
    public void independentSetsKeepTheirDeclaredOrder() {
        SourceSet b = sources("b/main");
        SourceSet a = sources("a/main");
        SourceSet c = sources("c/main");
        assertEquals(List.of("b/main", "a/main", "c/main"),
                names(JavaInspectorImpl.computeScanOrder(List.of(b, a, c))));
    }
}
