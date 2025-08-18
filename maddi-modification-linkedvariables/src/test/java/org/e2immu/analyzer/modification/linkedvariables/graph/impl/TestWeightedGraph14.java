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

public class TestWeightedGraph14 extends CommonWG {

    /*
       list1.add(m)
       list2.add(m)

       m  *M ----4---- 0M list1
       *M
       |
       4
       |
       0M
       list2
     */
    Variable m, list1, list2;
    WeightedGraph wg;
    ShortestPath shortestPath;

    @BeforeEach
    public void beforeEach() {
        m = makeVariable("m");
        list1 = makeVariable("list1");
        list2 = makeVariable("list2");

        wg = new WeightedGraphImpl();

        LV link = LVImpl.createHC(new LinksImpl(Map.of(ALL_INDICES, new LinkImpl(i0, true))));
        assertEquals("*M-4-0M", link.toString());

        wg.addNode(m, Map.of(list1, link, list2, link));
        shortestPath = wg.shortestPath();
        assertEquals("0(2:0M-4-*M)1(2:0M-4-*M)2(0:*M-4-0M;1:*M-4-0M)",
                ((ShortestPathImpl) shortestPath).getCacheKey());

    }

    @Test
    @DisplayName("start in m")
    public void testM() {
        Map<Variable, LV> startAt = shortestPath.links(m, null);
        assertEquals(3, startAt.size());
        assertEquals(v0, startAt.get(m));
        assertEquals("*M-4-0M", startAt.get(list1).toString());
        assertEquals("*M-4-0M", startAt.get(list2).toString());
    }

    @Test
    @DisplayName("start in list1")
    public void testMS() {
        Map<Variable, LV> startAt = shortestPath.links(list1, null);
        assertEquals(3, startAt.size());
        assertEquals(v0, startAt.get(list1));
        assertEquals("0M-4-*M", startAt.get(m).toString());
        assertEquals("0-4-0", startAt.get(list2).toString());
    }
}
