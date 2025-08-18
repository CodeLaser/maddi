package org.e2immu.analyzer.modification.linkedvariables.graph.impl;

import org.e2immu.analyzer.modification.linkedvariables.graph.ShortestPath;
import org.e2immu.analyzer.modification.linkedvariables.graph.WeightedGraph;
import org.e2immu.analyzer.modification.linkedvariables.lv.LVImpl;
import org.e2immu.analyzer.modification.linkedvariables.lv.LinkImpl;
import org.e2immu.analyzer.modification.linkedvariables.lv.LinksImpl;
import org.e2immu.analyzer.modification.prepwork.variable.LV;
import org.e2immu.language.cst.api.variable.Variable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.e2immu.analyzer.modification.prepwork.hcs.IndicesImpl.ALL_INDICES;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestWeightedGraph15E extends CommonWG {

    /*
       r -> a 0,1M-2-0,*M|0-*
       r -> b 0,2M-2-0,*M|1-*

       result should keep a and b separate
     */

    Variable r, a, b, s;
    WeightedGraph wg;
    ShortestPath shortestPath;

    @BeforeEach
    public void beforeEach() {
        r = makeVariable("r");
        a = makeVariable("a"); // set
        b = makeVariable("b"); // list

        wg = new WeightedGraphImpl();

        LV linkAR = LVImpl.createDependent(new LinksImpl(Map.of(ALL_INDICES, new LinkImpl(i1, true),
                i0, new LinkImpl(i0, false)), ALL_INDICES, i0));
        assertEquals("*M,0-2-1M,0|*-0", linkAR.toString());
        LV linkBR = LVImpl.createDependent(new LinksImpl(Map.of(ALL_INDICES, new LinkImpl(i2, true),
                i0, new LinkImpl(i0, false)), ALL_INDICES, i1));
        assertEquals("*M,0-2-2M,0|*-1", linkBR.toString());


        wg.addNode(a, Map.of(r, linkAR));
        wg.addNode(b, Map.of(r, linkBR));
        shortestPath = wg.shortestPath();
        assertEquals("0(2:*M,0-2-1M,0|*-0)1(2:*M,0-2-2M,0|*-1)2(0:0,1M-2-0,*M|0-*;1:0,2M-2-0,*M|1-*)",
                ((ShortestPathImpl) shortestPath).getCacheKey());

    }

    @Test
    @DisplayName("start in a")
    public void testA() {
        Map<Variable, LV> startAt = shortestPath.links(a, null);
        assertEquals(v0, startAt.get(a));
        assertEquals("*M,0-2-1M,0|*-0", startAt.get(r).toString());
        assertEquals(2, startAt.size());
    }

    @Test
    @DisplayName("start in b")
    public void testB() {
        Map<Variable, LV> startAt = shortestPath.links(b, null);
        assertEquals(v0, startAt.get(b));
        assertEquals("*M,0-2-2M,0|*-1", startAt.get(r).toString());
        assertEquals(2, startAt.size());
    }

    @Test
    @DisplayName("start in r")
    public void testR() {
        Map<Variable, LV> startAt = shortestPath.links(r, null);
        assertEquals(3, startAt.size());
        assertEquals(v0, startAt.get(r));
        assertEquals("0,1M-2-0,*M|0-*", startAt.get(a).toString());
        assertEquals("0,2M-2-0,*M|1-*", startAt.get(b).toString());
    }
}
