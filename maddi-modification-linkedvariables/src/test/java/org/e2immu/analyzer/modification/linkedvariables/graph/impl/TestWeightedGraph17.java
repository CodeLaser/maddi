package org.e2immu.analyzer.modification.linkedvariables.graph.impl;

import org.e2immu.analyzer.modification.linkedvariables.graph.ShortestPath;
import org.e2immu.analyzer.modification.linkedvariables.graph.WeightedGraph;
import org.e2immu.analyzer.modification.linkedvariables.lv.LVImpl;
import org.e2immu.analyzer.modification.linkedvariables.lv.LinkImpl;
import org.e2immu.analyzer.modification.linkedvariables.lv.LinksImpl;
import org.e2immu.analyzer.modification.prepwork.variable.LV;
import org.e2immu.language.cst.api.variable.Variable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.e2immu.analyzer.modification.prepwork.hcs.IndicesImpl.ALL_INDICES;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestWeightedGraph17 extends CommonWG {

    static <T> void listAdd(List<T> list, T t1, T t2) {
        List<T> l = list.subList(0, 10);
        List<T> l2 = l;
        l2.add(t1);
        l2.add(t2);
    }

    /*

    Situation: at statement 3, we expect t2 to be linked *-4-0:l2,*-4-0:list. We do not want it linked to t
    This link to t does not occur when the intermediate variable l2 is absent.

    At this point, we're not implementing cluster of -1- completeness.

    t1 -> *-4-0 l, list, l2 (all 3 connected by assignment, 0 and 1)
    t2 -> *-4-0 l2.

    we're not expecting t2 to link to t1
     */
    Variable l, l2, list, t1, t2;
    WeightedGraph wg;
    ShortestPath shortestPath;

    @BeforeEach
    public void beforeEach() {
        l = makeVariable("l");
        l2 = makeVariable("l2");
        list = makeVariable("list");
        t1 = makeVariable("t1");
        t2 = makeVariable("t2");

        wg = new WeightedGraphImpl();

        LV link0 = LVImpl.createHC(new LinksImpl(Map.of(ALL_INDICES, new LinkImpl(i0, false))));

        wg.addNode(t1, Map.of(l, link0, list, link0, l2, link0));
        wg.addNode(list, Map.of(l, v1, l2, v1));
        wg.addNode(l, Map.of(l2, v0));
        wg.addNode(t2, Map.of(l2, link0));

        // order: t1 l2 t2 l list (reverse alphabetic)
        shortestPath = wg.shortestPath();
        assertEquals("0(1:*-4-0;3:*-4-0;4:*-4-0)1(0:0-4-*;2:0-4-*;3:0;4:1)2(1:*-4-0)3(0:0-4-*;1:0;4:1)4(0:0-4-*;1:1;3:1)",
                ((ShortestPathImpl) shortestPath).getCacheKey());
    }

    @Test
    public void test() {
        Map<Variable, LV> startAtT1 = shortestPath.links(t1, null);
        assertEquals("l2:*-4-0, l:*-4-0, list:*-4-0, t1:-0-", print(startAtT1));
        Map<Variable, LV> startAtList = shortestPath.links(list, null);
        assertEquals("l2:-1-, l:-1-, list:-0-, t1:0-4-*, t2:0-4-*", print(startAtList));
        Map<Variable, LV> startAtL = shortestPath.links(l, null);
        assertEquals("l2:-0-, l:-0-, list:-1-, t1:0-4-*, t2:0-4-*", print(startAtL));
        Map<Variable, LV> startAtL2 = shortestPath.links(l2, null);
        assertEquals("l2:-0-, l:-0-, list:-1-, t1:0-4-*, t2:0-4-*", print(startAtL2));
        Map<Variable, LV> startAtT2 = shortestPath.links(t2, null);
        assertEquals("l2:*-4-0, l:*-4-0, list:*-4-0, t2:-0-", print(startAtT2)); // should not contain t1!
    }

    private String print(Map<Variable, LV> map) {
        return map.entrySet().stream().map(e -> e.getKey().simpleName()
                                                + ":"
                                                + e.getValue()).sorted().collect(Collectors.joining(", "));
    }
}
