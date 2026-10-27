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

import io.codelaser.maddi.graph.G;
import io.codelaser.maddi.graph.ImmutableGraph;
import io.codelaser.maddi.graph.V;
import io.codelaser.maddi.graph.analyzer.TypeGraphIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TestShortestCycleDijkstra {

    /**
     * The per-call temp dir now lives INSIDE a JUnit-managed root, so each call still gets its own unique
     * directory and JUnit deletes the whole tree afterwards. At top level these accumulated across runs
     * until /tmp's tmpfs ran out of INODES and createTempDirectory itself began failing.
     */
    @TempDir
    private Path tempRoot;

    @Test
    public void test() throws IOException {
        // 0 ->1-> 1, 0 ->4-> 2, 0 ->5-> 3
        // 1 ->2-> 2
        // 2 ->1-> 3
        // 3 ->1-> 4
        // 4 ->4-> 1

        // so 0 does not participate in the cycle, but the other nodes do

        Map<Integer, Map<Integer, Long>> edges = Map.of(
                0, Map.of(1, 1L, 2, 4L, 3, 5L),
                1, Map.of(2, 2L),
                2, Map.of(3, 1L),
                3, Map.of(4, 1L),
                4, Map.of(1, 4L)
        );
        G.Builder<Integer> gb = new ImmutableGraph.Builder<>(Long::sum);
        edges.forEach((from, m) ->
                m.forEach((to, d) -> gb.mergeEdge(from, to, d)));
        G<Integer> graph = gb.build();
        assertEquals("""
                0->1->1
                0->4->2
                0->5->3
                1->2->2
                2->1->3
                3->1->4
                4->4->1\
                """, graph.toString("\n"));
        ShortestCycleDijkstra.Cycle<Integer> cycle1 = ShortestCycleDijkstra.shortestCycle(graph, new V<>(1));
        assertEquals("Cycle[vertices=[1, 2, 3, 4, 1], distance=8]", cycle1.toString());
        ShortestCycleDijkstra.Cycle<Integer> cycle0 = ShortestCycleDijkstra.shortestCycle(graph, new V<>(0));
        assertNull(cycle0);

        Path tempDir = Files.createTempDirectory(tempRoot, "test");
        Path outputFile = tempDir.resolve("cycle.gml");
        TypeGraphIO.dumpGraph(outputFile.toFile(), graph);

        Linearize.Result<Integer> res = Linearize.linearize(graph, Linearize.LinearizationMode.ALL);
        assertEquals(1, res.remainingCycles().size());
        Cycle<Integer> cycle = res.remainingCycles().iterator().next();
        assertEquals(4, cycle.size());
        assertEquals(1, res.attachedToCycles().size());
        assertEquals("[1, 2, 3, 4]", cycle.vertices().stream().map(Object::toString).sorted().toList().toString());
        assertEquals("[0]", res.attachedToCycles().sortedStream(Integer::compareTo).toList().toString());
    }
}
