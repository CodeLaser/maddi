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

import org.junit.jupiter.api.Test;

import static io.codelaser.maddi.callgraph.ComputeCallGraph.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestComputeCallGraph {
    // every count odd and every weight distinct, so no field can be read from its neighbour's bits unnoticed; the
    // test below uses even counts only, and passed while the doc count was read from the reference count's low bit
    @Test
    public void eachKindIsReadFromItsOwnField() {
        long value = 3 * DOC_REFERENCES + 13 * BY_NAME_REFERENCES + 5 * REFERENCES + 7 * TYPES_IN_DECLARATION
                     + TYPE_HIERARCHY + 9 * CODE_STRUCTURE;
        assertEquals(3, docReferenceCount(value));
        assertEquals(13, byNameReferenceCount(value));
        assertEquals(5, referenceCount(value));
        assertEquals(7, declarationCount(value));
        assertEquals(1, hierarchyCount(value));
        assertEquals(9, codeStructureCount(value));
        assertEquals(3 * 2 + 5 * 3 + 7 * 5 + 7 + 9 * 11, weightedSumInteractions(value, 2, 3, 5, 7, 11));
        assertEquals(0, weightedSumInteractions(REFERENCES, 1, 0, 0, 0, 0), "one reference is not a doc reference");
        assertEquals(0, weightedSumInteractions(BY_NAME_REFERENCES, 1, 1, 1, 1, 1),
                "a by-name reference is a compile-time NON-dependency: it is not in the clustering weight");
    }

    /** The two soft lanes are below the threshold, so no consumer that filters on it can see them. */
    @Test
    public void theSoftLanesAreBelowTheThreshold() {
        for (long soft : new long[]{DOC_REFERENCES, BY_NAME_REFERENCES, 255 * DOC_REFERENCES + 255 * BY_NAME_REFERENCES}) {
            assertFalse(isAtLeastReference(soft), edgeValuePrinter(soft));
            assertFalse(isReference(soft), edgeValuePrinter(soft));
        }
        assertTrue(isByName(BY_NAME_REFERENCES));
        assertFalse(isByName(DOC_REFERENCES));
        assertFalse(isByName(REFERENCES));
        assertEquals("n", edgeValuePrinter(BY_NAME_REFERENCES));
        assertEquals("nd", edgeValuePrinter(BY_NAME_REFERENCES + DOC_REFERENCES));
        assertEquals("Rnd", edgeValuePrinter(REFERENCES + BY_NAME_REFERENCES + DOC_REFERENCES));
    }

    /**
     * ⛔ A lane SATURATES; it does not carry. The doc lane is 8 bits, so the 256th javadoc link to one target would,
     * under {@code Long::sum}, have become one by-name reference — and one REFERENCE, before that, when the lane was
     * 16 bits wide. A count that overflows into the lane above does not exaggerate the edge, it re-labels it.
     */
    @Test
    public void aLaneSaturatesRatherThanCarry() {
        long full = 255 * DOC_REFERENCES;
        assertEquals(255, docReferenceCount(mergeWeights(full, DOC_REFERENCES)));
        assertEquals(0, byNameReferenceCount(mergeWeights(full, DOC_REFERENCES)), "no carry into the lane above");
        assertEquals(255, byNameReferenceCount(mergeWeights(255 * BY_NAME_REFERENCES, BY_NAME_REFERENCES)));
        assertEquals(0, referenceCount(mergeWeights(255 * BY_NAME_REFERENCES, BY_NAME_REFERENCES)));
        assertEquals(65535, referenceCount(mergeWeights(65535 * REFERENCES, REFERENCES)));
        assertEquals(0, declarationCount(mergeWeights(65535 * REFERENCES, REFERENCES)));
        assertEquals(255, hierarchyCount(mergeWeights(255 * TYPE_HIERARCHY, TYPE_HIERARCHY)));
        assertEquals(0, codeStructureCount(mergeWeights(255 * TYPE_HIERARCHY, TYPE_HIERARCHY)));

        // below saturation it is plain addition, lane by lane, which is what every existing weight relies on
        long a = 2 * DOC_REFERENCES + 3 * BY_NAME_REFERENCES + 4 * REFERENCES + 5 * TYPES_IN_DECLARATION
                 + 6 * TYPE_HIERARCHY + 7 * CODE_STRUCTURE;
        assertEquals(2 * a, mergeWeights(a, a));
        assertEquals(a, mergeWeights(a, 0));
    }

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
