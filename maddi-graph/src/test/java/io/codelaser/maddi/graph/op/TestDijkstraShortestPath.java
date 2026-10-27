/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.graph.op;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class TestDijkstraShortestPath {

    @Test
    public void test() {
        DijkstraShortestPath d = new DijkstraShortestPath();
        Map<Integer, Map<Integer, Long>> edges = Map.of(
                0, Map.of(1, 1L, 2, 4L, 3, 5L),
                1, Map.of(2, 2L),
                2, Map.of(3, 1L),
                3, Map.of()
        );
        DijkstraShortestPath.EdgeProvider edgeProvider = i -> edges.get(i).entrySet().stream()
                .map(e -> new DijkstraShortestPath.DCPEntry(e));
        long[] dist0 = d.shortestPath(4, edgeProvider, 0);
        assertArrayEquals(new long[]{0, 1, 3, 4}, dist0);
        long[] dist1 = d.shortestPath(4, edgeProvider, 1);
        assertArrayEquals(new long[]{Long.MAX_VALUE, 0, 2, 3}, dist1);
        long[] dist2 = d.shortestPath(4, edgeProvider, 2);
        assertArrayEquals(new long[]{Long.MAX_VALUE, Long.MAX_VALUE, 0, 1}, dist2);
    }
}
