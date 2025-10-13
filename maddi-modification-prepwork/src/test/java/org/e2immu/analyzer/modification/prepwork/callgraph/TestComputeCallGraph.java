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

package org.e2immu.analyzer.modification.prepwork.callgraph;

import org.junit.jupiter.api.Test;

import static org.e2immu.analyzer.modification.prepwork.callgraph.ComputeCallGraph.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestComputeCallGraph {
    @Test
    public void test() {
        long twoR = 2 * REFERENCES;
        assertEquals(2, weightedSumInteractions(twoR, 1, 1, 1,
                1, 1));
        assertTrue(isReference(twoR));
        long twoRtwoH = twoR + 2 * TYPE_HIERARCHY;
        assertEquals(4, weightedSumInteractions(twoRtwoH, 1, 1, 1,
                1, 1));
        assertEquals("HR", edgeValuePrinter(twoRtwoH));
        assertTrue(isReference(twoRtwoH));
        long twoRtwoHoneC = twoRtwoH + CODE_STRUCTURE;
        assertEquals(5, weightedSumInteractions(twoRtwoHoneC, 1, 1, 1,
                1, 1));
        assertEquals("SHR", edgeValuePrinter(twoRtwoHoneC));
        assertTrue(isReference(twoRtwoHoneC));

        long oneC = CODE_STRUCTURE;
        assertFalse(isReference(oneC));
        assertEquals("S", edgeValuePrinter(oneC));
        long oneConeD = CODE_STRUCTURE + TYPES_IN_DECLARATION;
        assertFalse(isReference(oneConeD));
        assertEquals("SD", edgeValuePrinter(oneConeD));
    }
}
