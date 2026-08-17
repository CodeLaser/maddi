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

package org.e2immu.util.internal.graph.op;

import org.e2immu.util.internal.graph.G;
import org.e2immu.util.internal.graph.V;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RemoveEdgesByVertexWeight<T> implements BreakCycles.ActionComputer<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GreedyEdgeRemoval.class);
    private final Map<V<T>, Long> vertexWeights;

    public RemoveEdgesByVertexWeight(Map<V<T>, Long> vertexWeights) {
        this.vertexWeights = vertexWeights;
    }

    @Override
    public BreakCycles.Action<T> compute(G<T> g, Cycle<T> cycle) {
        long lowestWeight = cycle.vertices().stream().mapToLong(vertexWeights::get).min().orElseThrow();
        Map<V<T>, Set<V<T>>> edgesToRemove = cycle.vertices().stream()
                .filter(v -> vertexWeights.get(v) == lowestWeight)
                .collect(Collectors.toUnmodifiableMap(v -> v, v -> g.edges(v).keySet(),
                        (s1, s2) -> Stream.concat(s1.stream(), s2.stream()).collect(Collectors.toUnmodifiableSet())));
        LOGGER.debug("Lowest weight {}, remove {}", lowestWeight, edgesToRemove);
        BreakCycles.EdgeRemoval2<T> info = new BreakCycles.EdgeRemoval2<>(edgesToRemove);
        G<T> subGraph = g.subGraph(cycle.vertices()).withFewerEdges(edgesToRemove);
        return new BreakCycles.Action<T>() {
            @Override
            public G<T> apply() {
                return subGraph;
            }

            @Override
            public BreakCycles.ActionInfo info() {
                return info;
            }
        };
    }
}
