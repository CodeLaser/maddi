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

package org.e2immu.analyzer.modification.prepwork.callgraph;

import org.e2immu.language.cst.api.element.ModuleInfo;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.util.internal.graph.G;
import org.e2immu.util.internal.graph.ImmutableGraph;
import org.e2immu.util.internal.graph.V;
import org.e2immu.util.internal.graph.op.Common;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The {@link ComputeCallGraph} projected down to primary types, and <em>transposed</em>: the answer to "who depends on
 * this type?".
 * <p>
 * Mind the direction. The call graph's own convention is <em>I need you to exist first</em>: an edge {@code from -> to}
 * means {@code from} needs {@code to}. Here that is flipped, so an edge {@code X -> Y} means <em>Y uses X</em>. An
 * ordinary forward walk from a set of changed types then yields everything that (transitively) uses them — which is
 * exactly the set that must be rewired when those types are re-parsed. ({@link G#reverse} would be the other way to
 * do this; transposing while projecting is cheaper, and is what the run modules already did.)
 * <p>
 * This is the reverse-reachability the reload flow runs on: changed types become {@code INVALID}, everything
 * {@link #dependentsOf} returns becomes {@code REWIRE}, the rest stays {@code UNCHANGED}. Note the closure property
 * that follows from the definition: an {@code UNCHANGED} type cannot reach an {@code INVALID} one, or it would have
 * been reachable here and hence {@code REWIRE}. See {@code rewiring.md}.
 */
public class PrimaryTypeUseGraph {

    private final G<TypeInfo> graph;

    public PrimaryTypeUseGraph(G<Info> callGraph) {
        G.Builder<TypeInfo> builder = new ImmutableGraph.Builder<>(Long::sum);
        callGraph.edgeStream().forEach(e -> {
            Info from = e.from().t();
            Info to = e.to().t();
            // ModuleInfo.typeInfo() throws; module vertices carry no primary type to project onto. ComputeAnalysisOrder
            // filters them the same way. They only appear when the parse does not ignore modules.
            if (from instanceof ModuleInfo || to instanceof ModuleInfo) return;
            TypeInfo ptFrom = from.typeInfo().primaryType();
            TypeInfo ptTo = to.typeInfo().primaryType();
            if (ptFrom != ptTo) builder.mergeEdge(ptTo, ptFrom, 1L);
        });
        this.graph = builder.build();
    }

    /**
     * Edge {@code X -> Y}: Y uses X.
     */
    public G<TypeInfo> graph() {
        return graph;
    }

    /**
     * Every primary type that transitively uses one of {@code changed}, excluding {@code changed} itself. A type in
     * {@code changed} can be reached from another one (they may use each other), which is why the result is filtered
     * rather than merely started without its seeds: the two sets must be disjoint for the caller to map them onto
     * distinct states.
     */
    public Set<TypeInfo> dependentsOf(Set<TypeInfo> changed) {
        List<V<TypeInfo>> startingPoints = changed.stream().map(V::new).toList();
        return Common.follow(graph, startingPoints, false).stream()
                .map(V::t)
                .filter(t -> !changed.contains(t))
                .collect(Collectors.toUnmodifiableSet());
    }
}
