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

import static org.junit.jupiter.api.Assertions.*;

public class TestWeightedGraph8C extends CommonWG {


    Variable r, a, b;
    WeightedGraph wg;
    ShortestPath shortestPath;

    /*
     r is a record
     a and b are fields in this record
     their hidden content types are not that relevant to this test
     their modification areas are.

     r 1--2--0 a
     0
     |
     2
     |
     0
     b

     */

    @BeforeEach
    public void beforeEach() {
        a = makeVariable("a");
        r = makeVariable("r");
        b = makeVariable("b");

        wg = new WeightedGraphImpl();

        LV r_2_a = LVImpl.createDependent(new LinksImpl(Map.of(i0, new LinkImpl(i0, false)),
                new IndicesImpl(0), IndicesImpl.ALL_INDICES));
        LV r_2_b = LVImpl.createDependent(new LinksImpl(Map.of(i1, new LinkImpl(i0, false)),
                new IndicesImpl(1), IndicesImpl.ALL_INDICES));
        assertEquals("1-2-0|1-*", r_2_b.toString());
        assertEquals("0-2-0|*-0", r_2_a.reverse().toString());

        wg.addNode(r, Map.of(a, r_2_a, b, r_2_b));
        wg.addNode(a, Map.of(r, r_2_a.reverse()));
        wg.addNode(b, Map.of(r, r_2_b.reverse()));

        shortestPath = wg.shortestPath();

        assertEquals("0(2:0-2-0|*-0)1(2:0-2-1|*-1)2(0:0-2-0|0-*;1:1-2-0|1-*)",
                ((ShortestPathImpl) shortestPath).getCacheKey());
        assertSame(b, ((ShortestPathImpl) shortestPath).variablesGet(1));
    }

    @Test
    @DisplayName("start in a")
    public void testK() {
        Map<Variable, LV> startAt = shortestPath.links(a, null);
        assertEquals(2, startAt.size());
        assertEquals(v0, startAt.get(a));
        assertTrue(startAt.get(r).isDependent());
        assertEquals("0-2-0|*-0", startAt.get(r).toString());
        assertFalse(startAt.containsKey(b));
    }

    @Test
    @DisplayName("start in b")
    public void testV() {
        Map<Variable, LV> startAt = shortestPath.links(b, null);
        assertEquals(2, startAt.size());
        assertEquals(v0, startAt.get(b));
        assertEquals("0-2-1|*-1", startAt.get(r).toString());
        assertFalse(startAt.containsKey(a));
    }

    @Test
    @DisplayName("start in r")
    public void testM() {
        Map<Variable, LV> startAt = shortestPath.links(r, null);
        assertEquals(3, startAt.size());
        assertEquals(v0, startAt.get(r));
        assertEquals("0-2-0|0-*", startAt.get(a).toString());
        assertEquals("1-2-0|1-*", startAt.get(b).toString());
    }
}
