package org.e2immu.analyzer.modification.linkedvariables.graph.impl;

import org.e2immu.analyzer.modification.linkedvariables.lv.LVImpl;
import org.e2immu.analyzer.modification.prepwork.variable.LV;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestShortestPathImpl extends CommonWG {

    @Test
    public void testAssigned() {
        long l = ShortestPathImpl.toDistanceComponent(LVImpl.LINK_ASSIGNED);
        assertEquals(ShortestPathImpl.ASSIGNED, l);
    }

    @Test
    public void testCommonHC() {
        long l = ShortestPathImpl.toDistanceComponent(v4);
        assertEquals(ShortestPathImpl.COMMON_HC, l);
    }
}
