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


import org.e2immu.analyzer.modification.linkedvariables.graph.WeightedGraph;
import org.e2immu.analyzer.modification.prepwork.variable.LV;
import org.e2immu.language.cst.api.variable.Variable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.e2immu.analyzer.modification.linkedvariables.lv.LVImpl.LINK_DEPENDENT;
import static org.e2immu.analyzer.modification.linkedvariables.lv.LVImpl.LINK_STATICALLY_ASSIGNED;
import static org.junit.jupiter.api.Assertions.*;

/*
testing the increase in maxIncl
 */
public class TestWeightedGraph4 extends CommonWG {

    Variable x, a, i, y, b, j;
    WeightedGraph wg1, wg2;
    List<WeightedGraph> wgs;

    /*
     x <---0---> a <---4---> i
                 ^           |
                 |2          |4
                 v           v
     y <---4---> b           j
     */
    @BeforeEach
    public void beforeEach() {
        x = makeVariable("x"); // type X
        y = makeVariable("y"); // type X
        a = makeVariable("a"); // type List<X>
        b = makeVariable("b"); // type List<X>
        i = makeVariable("i"); // type List<List<X>>
        j = makeVariable("j"); // type List<List<X>>

        wg1 = new WeightedGraphImpl();
        wg1.addNode(x, Map.of(x, v0, a, v0));
        wg1.addNode(y, Map.of(y, v0, b, v4));
        wg1.addNode(a, Map.of(a, v0, b, v2, i, v4));
        wg1.addNode(b, Map.of(b, v0));
        wg1.addNode(i, Map.of(i, v0, j, v4));
        wg1.addNode(j, Map.of(j, v0));

        wg2 = new WeightedGraphImpl();
        wg2.addNode(x, Map.of(x, v0, a, v0));
        wg2.addNode(a, Map.of(a, v0, b, v2, i, v4));
        wg2.addNode(i, Map.of(i, v0, j, v4));
        wg2.addNode(y, Map.of(y, v0, b, v4));
        wg2.addNode(b, Map.of(b, v0));
        wg2.addNode(j, Map.of(j, v0));

        wgs = List.of(wg1, wg2);
    }

    @Test
    public void test1() {
        for (WeightedGraph wg : wgs) {
            Map<Variable, LV> startAtX = wg.shortestPath().links(x, LINK_STATICALLY_ASSIGNED);
            assertEquals(2, startAtX.size());
            assertEquals(v0, startAtX.get(x));
            assertEquals(v0, startAtX.get(a));
            assertNull(startAtX.get(b));
            assertNull(startAtX.get(i));
        }
    }

    @Test
    public void test2() {
        for (WeightedGraph wg : wgs) {
            Map<Variable, LV> startAtA = wg.shortestPath().links(a, LINK_DEPENDENT);
            assertEquals(3, startAtA.size());
            assertEquals(v0, startAtA.get(x));
            assertEquals(v0, startAtA.get(a));
            assertEquals(v2, startAtA.get(b));
            assertNull(startAtA.get(y));
            assertNull(startAtA.get(i));
        }
    }

    @Test
    public void test3() {
        for (WeightedGraph wg : wgs) {
            Map<Variable, LV> startAtA = wg.shortestPath().links(a, null);
            assertEquals(6, startAtA.size());
            assertEquals(v0, startAtA.get(x));
            assertEquals(v0, startAtA.get(a));
            assertEquals(v2, startAtA.get(b));
            assertTrue(startAtA.get(y).isCommonHC());
            assertTrue(startAtA.get(i).isCommonHC());
            assertTrue(startAtA.get(j).isCommonHC());
        }
    }
}
