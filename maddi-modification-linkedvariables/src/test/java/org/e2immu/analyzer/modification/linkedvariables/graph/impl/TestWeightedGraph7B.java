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
import org.e2immu.analyzer.modification.prepwork.hcs.IndexImpl;
import org.e2immu.analyzer.modification.prepwork.hcs.IndicesImpl;
import org.e2immu.analyzer.modification.linkedvariables.lv.LVImpl;
import org.e2immu.analyzer.modification.linkedvariables.lv.LinkImpl;
import org.e2immu.analyzer.modification.linkedvariables.lv.LinksImpl;
import org.e2immu.analyzer.modification.prepwork.variable.Indices;
import org.e2immu.analyzer.modification.prepwork.variable.LV;
import org.e2immu.analyzer.modification.prepwork.variable.Links;
import org.e2immu.language.cst.api.variable.Variable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.e2immu.analyzer.modification.prepwork.hcs.IndexImpl.ALL;
import static org.junit.jupiter.api.Assertions.*;

public class TestWeightedGraph7B extends CommonWG {

    /*
    Very similar to 7, except that, as of 20240416, the link between entries and map
    goes over 0.0, 0.1; similarly, entries to entry.
    Note that the link to 'this' remains 0-4-1.
     */
    static class X<T> {
        final Map<Long, T> map = Map.of();

        void method() {
            Set<Map.Entry<Long, T>> entries = map.entrySet();
            for (Map.Entry<Long, T> entry : entries) {
                Long l = entry.getKey();
                T t = entry.getValue();
                System.out.println(l + " -> " + t);
            }
        }
    }

    /*
    this <0> ----4----- <1> map                   Type parameter 0 corresponds to type parameter 1 in Map<K, V>
    map <0,1> -----4----- <0.0,0.1> entries       All hidden content of Map (type parameters 0 and 1) correspond to
                                                  type parameter 0.0 and 0.1 in Set<Map.Entry<K,V>>
    entries <0.0,0.1> -- 4 -- <0,1> entry         ~ get() method, is part of HC
    l * ------------ 4 ------ <0> entry           ~ get() method, is part of HC, parameter 0
    t * ------------ 4 ------ <1> entry           ~ get() method, is part of HC, parameter 1

     */

    Variable thisVar, map, entries, entry, l, t;
    WeightedGraph wg;
    ShortestPath shortestPath;

    @BeforeEach
    public void beforeEach() {
        thisVar = makeVariable("thisVar");
        map = makeVariable("map");
        entries = makeVariable("entries");
        entry = makeVariable("entry");
        l = makeVariable("l");
        t = makeVariable("t");

        wg = new WeightedGraphImpl();

        LV thisVar_4_map = LVImpl.createHC(new LinksImpl(Map.of(i0, new LinkImpl(i1, false))));
        assertEquals("0-4-1", thisVar_4_map.toString());
        assertEquals("1-4-0", thisVar_4_map.reverse().toString());

        Indices i00 = new IndicesImpl(Set.of(new IndexImpl(List.of(0, 0))));
        Indices i01 = new IndicesImpl(Set.of(new IndexImpl(List.of(0, 1))));

        wg.addNode(thisVar, Map.of(map, thisVar_4_map));
        Links l01to01 = new LinksImpl(Map.of(i0, new LinkImpl(i00, false), i1, new LinkImpl(i01, false)));
        LV map_4_entries = LVImpl.createHC(l01to01);
        assertEquals("0,1-4-0.0,0.1", map_4_entries.toString());
        wg.addNode(map, Map.of(thisVar, thisVar_4_map.reverse(), entries, map_4_entries));

        wg.addNode(entries, Map.of(map, map_4_entries.reverse(), entry, map_4_entries.reverse()));

        LV entry_4_l = LVImpl.createHC(new LinksImpl(0, ALL, true));
        LV entry_4_t = LVImpl.createHC(new LinksImpl(1, ALL, true));
        assertEquals("0-4-*", entry_4_l.toString());
        assertEquals("1-4-*", entry_4_t.toString());
        wg.addNode(entry, Map.of(entries, map_4_entries, l, entry_4_l, t, entry_4_t));

        wg.addNode(l, Map.of(entry, entry_4_l.reverse()));
        wg.addNode(t, Map.of(entry, entry_4_t.reverse()));

        shortestPath = wg.shortestPath();

        ShortestPathImpl spi = (ShortestPathImpl) shortestPath;
        assertEquals("0(5:*-4-0)1(2:1-4-0;3:0,1-4-0.0,0.1)2(1:0-4-1)3(1:0.0,0.1-4-0,1;5:0.0,0.1-4-0,1)4(5:*-4-1)5(0:0-4-*;3:0,1-4-0.0,0.1;4:1-4-*)",
                spi.getCacheKey());
        assertSame(l, spi.variablesGet(0));
        assertSame(map, spi.variablesGet(1));
        assertSame(thisVar, spi.variablesGet(2));
        assertSame(entries, spi.variablesGet(3));
        assertSame(t, spi.variablesGet(4));
        assertSame(entry, spi.variablesGet(5));
    }

    @Test
    @DisplayName("starting in thisVar")
    public void testTV() {
        Map<Variable, LV> links = shortestPath.links(thisVar, null);
        assertEquals(v0, links.get(thisVar)); // start all
        assertEquals(5, links.size());
        assertEquals("0-4-1", links.get(map).toString());
        assertEquals("0-4-0.1", links.get(entries).toString());
        assertEquals("0-4-1", links.get(entry).toString());
        assertNull(links.get(l));
        assertEquals("0-4-*", links.get(t).toString());
    }

    @Test
    @DisplayName("starting in l")
    public void testL() {
        Map<Variable, LV> links = shortestPath.links(l, null);
        assertEquals(4, links.size());
        assertEquals(v0, links.get(l)); // start all
        assertEquals("*-4-0", links.get(entry).toString());
        assertNull(links.get(t)); // not reachable
        assertEquals("*-4-0.0", links.get(entries).toString());
        assertEquals("*-4-0", links.get(map).toString()); // <0> of map
        assertNull(links.get(thisVar));  // nothing, because map links to this via 1, not via 0
    }

    @Test
    @DisplayName("starting in t")
    public void testT() {
        Map<Variable, LV> links = shortestPath.links(t, null);
        assertEquals(5, links.size());
        assertEquals(v0, links.get(t)); // start all
        assertEquals("*-4-1", links.get(entry).toString());
        assertNull(links.get(l)); // not reachable
        assertEquals("*-4-0.1", links.get(entries).toString());
        assertEquals("*-4-1", links.get(map).toString());
        assertEquals("*-4-0", links.get(thisVar).toString());
    }

    @Test
    @DisplayName("starting in map")
    public void testMap() {
        Map<Variable, LV> links = shortestPath.links(map, null);
        assertEquals(v0, links.get(map)); // start all
        assertEquals("1-4-0", links.get(thisVar).toString());
        assertEquals("0,1-4-0.0,0.1", links.get(entries).toString());
        assertEquals("0,1-4-0,1", links.get(entry).toString());
        assertEquals("1-4-*", links.get(t).toString());
        assertEquals("0-4-*", links.get(l).toString());
        assertEquals(6, links.size());
    }

    @Test
    @DisplayName("starting in entry")
    public void testEntry() {
        Map<Variable, LV> links = shortestPath.links(entry, null);
        assertEquals(v0, links.get(entry)); // start all
        assertEquals("0,1-4-0.0,0.1", links.get(entries).toString());
        assertEquals("0,1-4-0,1", links.get(map).toString());
        assertEquals("1-4-*", links.get(t).toString());
        assertEquals("0-4-*", links.get(l).toString());
        assertEquals("1-4-0", links.get(thisVar).toString());
        assertEquals(6, links.size());
    }
}
