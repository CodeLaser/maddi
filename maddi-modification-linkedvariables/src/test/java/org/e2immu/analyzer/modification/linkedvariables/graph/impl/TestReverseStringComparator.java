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
