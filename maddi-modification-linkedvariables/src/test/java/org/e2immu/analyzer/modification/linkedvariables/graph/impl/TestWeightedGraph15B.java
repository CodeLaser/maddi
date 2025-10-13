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
import org.e2immu.analyzer.modification.prepwork.hcs.IndicesImpl;
import org.e2immu.analyzer.modification.prepwork.variable.LV;
import org.e2immu.language.cst.api.variable.Variable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.e2immu.analyzer.modification.prepwork.hcs.IndicesImpl.ALL_INDICES;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestWeightedGraph15B extends CommonWG {

    /*
       a *--2--0 r *--2--0 s
         *----1     *----3
       result should be
       a *--2--0 s
         *----3.1
     */
    Variable r, a, s;
    WeightedGraph wg;
    ShortestPath shortestPath;

    @BeforeEach
    public void beforeEach() {
        r = makeVariable("r");
        a = makeVariable("a");
        s = makeVariable("s");

        wg = new WeightedGraphImpl();

        LV linkAR = LVImpl.createDependent(new LinksImpl(Map.of(ALL_INDICES, new LinkImpl(i0, false)),
                ALL_INDICES, new IndicesImpl(1)));
        assertEquals("*-2-0|*-1", linkAR.toString());
        LV linkRS = LVImpl.createDependent(new LinksImpl(Map.of(ALL_INDICES, new LinkImpl(i0, false)),
                ALL_INDICES, new IndicesImpl(3)));
        assertEquals("*-2-0|*-3", linkRS.toString());


        wg.addNode(r, Map.of(s, linkRS));
        wg.addNode(a, Map.of(r, linkAR));
        shortestPath = wg.shortestPath();
        assertEquals("0(1:*-2-0|*-1)1(0:0-2-*|1-*;2:*-2-0|*-3)2(1:0-2-*|3-*)",
                ((ShortestPathImpl) shortestPath).getCacheKey());

    }

    @Test
    @DisplayName("start in a")
    public void testA() {
        Map<Variable, LV> startAt = shortestPath.links(a, null);
        assertEquals(3, startAt.size());
        assertEquals(v0, startAt.get(a));
        assertEquals("*-2-0|*-1", startAt.get(r).toString());
        assertEquals("*-2-0|*-3.1", startAt.get(s).toString());
    }

    @Test
    @DisplayName("start in r")
    public void testR() {
        Map<Variable, LV> startAt = shortestPath.links(r, null);
        assertEquals(3, startAt.size());
        assertEquals(v0, startAt.get(r));
        assertEquals("0-2-*|1-*", startAt.get(a).toString());
        assertEquals("*-2-0|*-3", startAt.get(s).toString());
    }

    @Test
    @DisplayName("start in s")
    public void testS() {
        Map<Variable, LV> startAt = shortestPath.links(s, null);
        assertEquals(3, startAt.size());
        assertEquals(v0, startAt.get(s));
        assertEquals("0-2-*|3-*", startAt.get(r).toString());
        assertEquals("0-2-*|3.1-*", startAt.get(a).toString());
    }
}
