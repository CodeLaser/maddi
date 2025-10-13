/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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

public class TestWeightedGraph8B extends CommonWG {


    Variable map, keys, values;
    WeightedGraph wg;
    ShortestPath shortestPath;

    /*
     Compared to test 8, this is the correct scenario, the keys are a view of the key set of the map;
     the values are a view of the values of the map.

     A modification on the keys (removal) has an effect on the values; we must keep the -2- link.

     Map<K,V> map = ...; Set<K> keys = map.keySet(); Collection<V> values = map.values()

     map 1--2--0 values
     0
     |
     2
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

        LV map_2_keys = LVImpl.createDependent(new LinksImpl(Map.of(i0, new LinkImpl(i0, false))));
        LV map_2_values = LVImpl.createDependent(new LinksImpl(Map.of(i1, new LinkImpl(i0, false))));
        assertEquals("1-2-0", map_2_values.toString());
        assertEquals("0-2-0", map_2_keys.reverse().toString());

        wg.addNode(map, Map.of(keys, map_2_keys, values, map_2_values));
        wg.addNode(keys, Map.of(map, map_2_keys.reverse()));
        wg.addNode(values, Map.of(map, map_2_values.reverse()));

        shortestPath = wg.shortestPath();

        assertEquals("0(1:1-2-0;2:0-2-0)1(0:0-2-1)2(0:0-2-0)",
                ((ShortestPathImpl) shortestPath).getCacheKey());
        assertSame(values, ((ShortestPathImpl) shortestPath).variablesGet(1));
    }

    @Test
    @DisplayName("start in keys")
    public void testK() {
        Map<Variable, LV> startAt = shortestPath.links(keys, null);
        assertEquals(3, startAt.size());
        assertEquals(v0, startAt.get(keys));
        assertTrue(startAt.get(map).isDependent());
        assertEquals("0-2-0", startAt.get(map).toString());
        assertTrue(startAt.get(values).isDependent());
        assertEquals("-2-", startAt.get(values).toString());
    }

    @Test
    @DisplayName("start in values")
    public void testV() {
        Map<Variable, LV> startAt = shortestPath.links(values, null);
        assertEquals(3, startAt.size());
        assertEquals(v0, startAt.get(values));
        assertEquals("0-2-1", startAt.get(map).toString());
        assertEquals("-2-", startAt.get(keys).toString());
    }

    @Test
    @DisplayName("start in map")
    public void testM() {
        Map<Variable, LV> startAt = shortestPath.links(map, null);
        assertEquals(3, startAt.size());
        assertEquals(v0, startAt.get(map));
        assertEquals("0-2-0", startAt.get(keys).toString());
        assertEquals("1-2-0", startAt.get(values).toString());
    }
}
