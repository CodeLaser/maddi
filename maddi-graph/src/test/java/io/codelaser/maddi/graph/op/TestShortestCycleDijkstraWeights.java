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


package io.codelaser.maddi.graph.op;

import io.codelaser.maddi.graph.G;
import io.codelaser.maddi.graph.ImmutableGraph;
import io.codelaser.maddi.graph.V;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The minimal cycle size counts the vertices of the cycle: 2 for two vertices that depend on each other. Until
 * 2026-10-26 the expansion compared the path's length before the closing edge was added, so a cycle of exactly the
 * minimal size was never closed; with the default of 2, two packages depending on each other made no cycle.
 */
public class TestShortestCycleDijkstraWeights {

    private static G<Integer> graph() {
        // 1 <-> 2 costs 6; 1 -> 3 -> 4 -> 1 costs 3; 5 -> 5 is a self-loop
        Map<Integer, Map<Integer, Long>> edges = Map.of(
                1, Map.of(2, 3L, 3, 1L),
                2, Map.of(1, 3L),
                3, Map.of(4, 1L),
                4, Map.of(1, 1L),
                5, Map.of(5, 1L));
        G.Builder<Integer> gb = new ImmutableGraph.Builder<>(Long::sum);
        edges.forEach((from, m) -> m.forEach((to, d) -> gb.mergeEdge(from, to, d)));
        return gb.build();
    }

    private static String vertices(ShortestCycleDijkstraWeights.Cycle<Integer> cycle) {
        return cycle.vertices().stream().map(d -> d.vertex().t().toString()).toList().toString();
    }

    @Test
    public void twoVertices() {
        ShortestCycleDijkstraWeights.Cycle<Integer> cycle
                = ShortestCycleDijkstraWeights.shortestCycle(graph(), new V<>(2), 2);
        assertEquals("[2, 1, 2]", vertices(cycle));
        assertEquals(6, cycle.distance());
    }

    @Test
    public void cheapestOfAtLeastTheMinimalSize() {
        assertEquals("[1, 3, 4, 1]", vertices(ShortestCycleDijkstraWeights.shortestCycle(graph(), new V<>(1), 2)));
        assertEquals("[1, 3, 4, 1]", vertices(ShortestCycleDijkstraWeights.shortestCycle(graph(), new V<>(1), 3)));
        assertNull(ShortestCycleDijkstraWeights.shortestCycle(graph(), new V<>(2), 3));
        assertNull(ShortestCycleDijkstraWeights.shortestCycle(graph(), new V<>(1), 4));
    }

    @Test
    public void aSelfLoopIsNoCycleOfTwo() {
        assertNull(ShortestCycleDijkstraWeights.shortestCycle(graph(), new V<>(5), 2));
    }
}
