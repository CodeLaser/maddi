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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestReverseStringComparator {
    @Test
    public void test1() {
        assertTrue(WeightedGraphImpl.REVERSE_STRING_COMPARATOR.compare("cba", "cda") < 0);
        assertTrue(WeightedGraphImpl.REVERSE_STRING_COMPARATOR.compare("cda", "cba") > 0);
        assertEquals(0, WeightedGraphImpl.REVERSE_STRING_COMPARATOR.compare("cba", "cba"));
        assertTrue(WeightedGraphImpl.REVERSE_STRING_COMPARATOR.compare("dcba", "cba") > 0);
        assertTrue(WeightedGraphImpl.REVERSE_STRING_COMPARATOR.compare("cba", "dcba") < 0);
    }
}
