/*
 * e2immu: a static code analyser for effective and eventual immutability
 * Copyright 2020-2021, Bart Naudts, https://www.e2immu.org
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

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestWeightedGraph13 extends CommonWG {

    Variable map, newHashMap;
    WeightedGraph wg;
    ShortestPath shortestPath;

    /*
     map <-0,1M---4---0,1M-> newHashMap
     */
    @BeforeEach
    public void beforeEach() {
        map = makeVariable("map");
        newHashMap = makeVariable("newHashMap"); //List<X>

        wg = new WeightedGraphImpl();

        LV map_4_newHashMap = LVImpl.createHC(new LinksImpl(Map.of(i0, new LinkImpl(i0, false), i1, new LinkImpl(i1, true))));
        assertEquals("0,1M-4-0,1M", map_4_newHashMap.toString());

        wg.addNode(map, Map.of(map, v0, newHashMap, map_4_newHashMap));
        wg.addNode(newHashMap, Map.of(newHashMap, v0));
        shortestPath = wg.shortestPath();
    }

    @Test
    @DisplayName("start at 'newHashMap'")
    public void test1() {
        Map<Variable, LV> startAt = shortestPath.links(newHashMap, null);
        assertEquals(2, startAt.size());
        assertEquals(v0, startAt.get(newHashMap));
        assertEquals("0,1M-4-0,1M", startAt.get(map).toString());
    }
}
