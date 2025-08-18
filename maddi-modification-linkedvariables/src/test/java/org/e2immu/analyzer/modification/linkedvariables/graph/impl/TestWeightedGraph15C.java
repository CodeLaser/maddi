package org.e2immu.analyzer.modification.linkedvariables.graph.impl;

import org.e2immu.analyzer.modification.linkedvariables.graph.ShortestPath;
import org.e2immu.analyzer.modification.linkedvariables.graph.WeightedGraph;
import org.e2immu.analyzer.modification.linkedvariables.lv.LVImpl;
import org.e2immu.analyzer.modification.linkedvariables.lv.LinkImpl;
import org.e2immu.analyzer.modification.linkedvariables.lv.LinksImpl;
import org.e2immu.analyzer.modification.prepwork.hcs.IndicesImpl;
import org.e2immu.analyzer.modification.prepwork.variable.LV;
import org.e2immu.language.cst.api.variable.Variable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.e2immu.analyzer.modification.prepwork.hcs.IndicesImpl.ALL_INDICES;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestWeightedGraph15C extends CommonWG {

    /*
       record R(M a) { }
       M a = r.a;

       a  *M----2----0M r
       |
       1
       |
       r.a

     */
    Variable r, a, ra;
    WeightedGraph wg;
    ShortestPath shortestPath;

    @BeforeEach
    public void beforeEach() {
        r = makeVariable("r");
        a = makeVariable("a");
        ra = makeVariable("ra");

        wg = new WeightedGraphImpl();

        LV linkAR = LVImpl.createDependent(new LinksImpl(Map.of(ALL_INDICES, new LinkImpl(i0, true)),
                ALL_INDICES, new IndicesImpl(0)));
        assertEquals("*M-2-0M|*-0", linkAR.toString());

        wg.addNode(a, Map.of(r, linkAR));
        wg.addNode(a, Map.of(ra, v1));
        shortestPath = wg.shortestPath();
        assertEquals("0(1:1;2:*M-2-0M|*-0)1(0:1)2(0:0M-2-*M|0-*)",
                ((ShortestPathImpl) shortestPath).getCacheKey());

    }

    @Test
    @DisplayName("start in a")
    public void testA() {
        Map<Variable, LV> startAt = shortestPath.links(a, null);
        assertEquals(3, startAt.size());
        assertEquals(v0, startAt.get(a));
        assertEquals("*M-2-0M|*-0", startAt.get(r).toString());
        assertEquals("-1-", startAt.get(ra).toString());
    }

    @Test
    @DisplayName("start in r")
    public void testR() {
        Map<Variable, LV> startAt = shortestPath.links(r, null);
        assertEquals(3, startAt.size());
        assertEquals(v0, startAt.get(r));
        assertEquals("0M-2-*M|0-*", startAt.get(a).toString());
        assertEquals("0M-2-*M|0-*", startAt.get(ra).toString()); // should be identical
    }

    @Test
    @DisplayName("start in r.a")
    public void testRA() {
        Map<Variable, LV> startAt = shortestPath.links(ra, null);
        assertEquals(3, startAt.size());
        assertEquals(v0, startAt.get(ra));
        assertEquals("*M-2-0M|*-0", startAt.get(r).toString());
        assertEquals("-1-", startAt.get(a).toString());
    }
}
