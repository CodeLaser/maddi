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
import org.e2immu.analyzer.modification.prepwork.variable.LV;
import org.e2immu.language.cst.api.variable.Variable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestWeightedGraph12 extends CommonWG {

    Variable returnVariable, list;
    WeightedGraph wg;
    ShortestPath shortestPath;

    /*
    Linking_0.m8

     rv 0 <-0---4---0-> list 0
     */
    @BeforeEach
    public void beforeEach() {
        returnVariable = makeVariable("rv"); // List<X>
        list = makeVariable("list"); //List<X>

        wg = new WeightedGraphImpl();

        LV rv_4_list = v4;
        assertEquals("0-4-0", rv_4_list.toString());

        wg.addNode(returnVariable, Map.of(returnVariable, v0, list, rv_4_list));
        wg.addNode(list, Map.of(list, v0));
        shortestPath = wg.shortestPath();
    }

    @Test
    @DisplayName("start at 'list'")
    public void test1() {
        Map<Variable, LV> startAt = shortestPath.links(list, null);
        assertEquals(2, startAt.size());
        assertEquals(v0, startAt.get(list));
        assertEquals("0-4-0", startAt.get(returnVariable).toString());
    }

    @Test
    @DisplayName("start at 'rv'")
    public void test2() {
        Map<Variable, LV> startAt = shortestPath.links(returnVariable, null);
        assertEquals(2, startAt.size());
        assertEquals(v0, startAt.get(returnVariable));
        assertEquals("0-4-0", startAt.get(list).toString());
    }


}
