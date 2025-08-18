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
import static org.junit.jupiter.api.Assertions.*;

public class TestWeightedGraph16 extends CommonWG {

    /*
       Linking_0P.reverse new Pair(pair.g, pair.f)

       rv 1--4--* f *--4--0 pair
       rv 0--4--* g *--4--1 pair

     */
    Variable f, g, rv, pair;
    WeightedGraph wg;
    ShortestPath shortestPath;

    @BeforeEach
    public void beforeEach() {
        f = makeVariable("f");
        g = makeVariable("g");
        rv = makeVariable("rv");
        pair = makeVariable("pair");

        wg = new WeightedGraphImpl();

        LV link0 = LVImpl.createHC(new LinksImpl(Map.of(ALL_INDICES, new LinkImpl(i0, false))));
        LV link1 = LVImpl.createHC(new LinksImpl(Map.of(ALL_INDICES, new LinkImpl(i1, false))));

        wg.addNode(f, Map.of(rv, link1, pair, link0));
        wg.addNode(g, Map.of(rv, link0, pair, link1));
        shortestPath = wg.shortestPath();
        assertEquals("0(2:*-4-0;3:*-4-1)1(2:*-4-1;3:*-4-0)2(0:0-4-*;1:1-4-*)3(0:1-4-*;1:0-4-*)",
                ((ShortestPathImpl) shortestPath).getCacheKey());

    }

    @Test
    @DisplayName("start in f")
    public void testF() {
        Map<Variable, LV> startAt = shortestPath.links(f, null);
        assertEquals(3, startAt.size());
        assertEquals(v0, startAt.get(f));
        assertTrue(startAt.get(rv).isCommonHC());
        assertEquals("*-4-1", startAt.get(rv).toString());
        assertNull(startAt.get(g));
        assertTrue(startAt.get(pair).isCommonHC());
        assertEquals("*-4-0", startAt.get(pair).toString());
    }


    @Test
    @DisplayName("start in rv")
    public void testN() {
        Map<Variable, LV> startAt = shortestPath.links(rv, null);
        assertEquals(4, startAt.size());
        assertEquals(v0, startAt.get(rv));
        assertEquals("0-4-*", startAt.get(g).toString());
        assertEquals("1-4-*", startAt.get(f).toString());
        assertEquals("0,1-4-1,0", startAt.get(pair).toString());
    }
}
