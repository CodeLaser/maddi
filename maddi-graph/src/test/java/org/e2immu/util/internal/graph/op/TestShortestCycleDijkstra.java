package org.e2immu.util.internal.graph.op;

import org.e2immu.util.internal.graph.G;
import org.e2immu.util.internal.graph.V;
import org.e2immu.util.internal.graph.analyser.TypeGraphIO;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TestShortestCycleDijkstra {

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
        G.Builder<Integer> gb = new G.Builder<>(Long::sum);
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

        TypeGraphIO.dumpGraph(new File("build/cycle.gml"), graph);

        Linearize.Result<Integer> res = Linearize.linearize(graph, Linearize.LinearizationMode.ALL);
        assertEquals(1, res.remainingCycles().size());
        Cycle<Integer> cycle = res.remainingCycles().iterator().next();
        assertEquals(4, cycle.size());
        assertEquals(1, res.attachedToCycles().size());
        assertEquals("[1, 2, 3, 4]", cycle.vertices().stream().map(Object::toString).sorted().toList().toString());
        assertEquals("[0]", res.attachedToCycles().sortedStream(Integer::compareTo).toList().toString());
    }
}
