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
