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

import static org.junit.jupiter.api.Assertions.*;

public class TestWeightedGraph8 extends CommonWG {


    Variable map, keys, values;
    WeightedGraph wg;
    ShortestPath shortestPath;

    /*
     Map<K,V> map = ...; Set<K> keys = map.keySet(); Collection<V> values = map.values()

     map 1--4--0 values
     0
     |
     4
     |
     0
     keys

     */

    @BeforeEach
    public void beforeEach() {
        keys = makeVariable("keys");
        map = makeVariable("map");
        values = makeVariable("values");

        wg = new WeightedGraphImpl();

        LV map_4_keys = v4;
        LV map_4_values = LVImpl.createHC(new LinksImpl(Map.of(i1, new LinkImpl(i0, false))));
        assertEquals("1-4-0", map_4_values.toString());
        assertEquals("0-4-0", map_4_keys.reverse().toString());

        wg.addNode(map, Map.of(keys, map_4_keys, values, map_4_values));
        wg.addNode(keys, Map.of(map, map_4_keys.reverse()));
        wg.addNode(values, Map.of(map, map_4_values.reverse()));

        shortestPath = wg.shortestPath();
    }

    @Test
    @DisplayName("start in keys")
    public void testK() {
        Map<Variable, LV> startAt = shortestPath.links(keys, null);
        assertEquals(2, startAt.size());
        assertEquals(v0, startAt.get(keys));
        assertTrue(startAt.get(map).isCommonHC());
        assertNull(startAt.get(values));
    }

    @Test
    @DisplayName("start in values")
    public void testV() {
        Map<Variable, LV> startAt = shortestPath.links(values, null);
        assertEquals(2, startAt.size());
        assertEquals(v0, startAt.get(values));
        assertTrue(startAt.get(map).isCommonHC());
        assertNull(startAt.get(keys));
    }

    @Test
    @DisplayName("start in map")
    public void testM() {
        Map<Variable, LV> startAt = shortestPath.links(map, null);
        assertEquals(3, startAt.size());
        assertEquals(v0, startAt.get(map));
        assertTrue(startAt.get(keys).isCommonHC());
        assertTrue(startAt.get(values).isCommonHC());
    }
}
