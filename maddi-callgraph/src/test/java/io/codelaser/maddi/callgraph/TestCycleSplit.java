/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.callgraph;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The chunks a large cycle group is cut into for the parallel first iteration (ComputeAnalysisOrder.waves). */
public class TestCycleSplit {

    @DisplayName("chunks of at least the size, cut only where the group changes, order kept")
    @Test
    public void chunks() {
        // the group is the first letter: "a1" and "a2" belong to the same primary type
        List<String> list = List.of("a1", "a2", "a3", "b1", "b2", "c1", "d1", "d2", "d3", "d4", "e1");
        List<List<String>> chunks = ComputeAnalysisOrder.chunks(list, 2, s -> s.charAt(0));
        assertEquals("[[a1, a2, a3], [b1, b2], [c1, d1, d2, d3, d4], [e1]]", chunks.toString());
    }

    @DisplayName("a list no larger than the size is one chunk")
    @Test
    public void small() {
        assertEquals("[[x1, y1]]", ComputeAnalysisOrder.chunks(List.of("x1", "y1"), 2, s -> s.charAt(0)).toString());
    }
}
