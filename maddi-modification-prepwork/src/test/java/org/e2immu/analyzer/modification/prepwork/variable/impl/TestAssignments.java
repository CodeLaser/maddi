package org.e2immu.analyzer.modification.prepwork.variable.impl;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TestAssignments {

    @Test
    public void test1() {
        Assignments a = new Assignments("0");
        assertEquals(0, a.size());
        Assignments a0 = Assignments.newAssignment("0", a);
        assertEquals("D:0, A:[0]", a0.toString());

        Assignments a101 = Assignments.newAssignment("1.0.1", a0);
        assertEquals("D:0, A:[0, 1.0.1]", a101.toString());

        Assignments a102 = Assignments.newAssignment("1.0.2", a101);
        assertEquals("D:0, A:[0, 1.0.1, 1.0.2]", a102.toString());

        // there have been assignments in 1.0.x, but none in 1.1.x -> keep as is
        Assignments.CompleteMergeByCounting cmA = new Assignments.CompleteMergeByCounting(2);
        Assignments a1 = Assignments.mergeBlocks("1", cmA, Map.of("1.0.0", a102, "1.1.0", a0), Map.of());
        assertEquals("D:0, A:[0, 1.0.1, 1.0.2]", a1.toString());

        Assignments a110 = Assignments.newAssignment("1.1.0", a0);
        assertEquals("D:0, A:[0, 1.1.0]", a110.toString());

        // in this situation, there has been a full merge. we drop information about sub-blocks in the main
        // array, but store it in actualAssignmentIndices
        Assignments.CompleteMergeByCounting cmB = new Assignments.CompleteMergeByCounting(2);
        Assignments b1 = Assignments.mergeBlocks("1", cmB, Map.of("1.0.0", a102, "1.1.0", a110), Map.of());
        assertEquals("D:0, A:[0, 1.0.1, 1.0.2, 1.1.0, 1=M]", b1.toString());

        assertTrue(b1.hasAValueAt("0"));
        assertTrue(b1.hasAValueAt("1"));
        assertTrue(b1.hasAValueAt("1.0.1"));
        assertTrue(b1.hasAValueAt("1.2.0")); // we fall back to the assignment at 0
        assertTrue(b1.hasAValueAt("2"));
        assertTrue(b1.hasAValueAt("2.0.1"));
        assertTrue(b1.hasAValueAt("3.1"));
    }


    @Test
    public void test2() {
        Assignments a = new Assignments("0");
        assertEquals(0, a.size());
        assertEquals("D:0, A:[]", a.toString());

        Assignments a101 = Assignments.newAssignment("1.0.1", a);
        assertEquals("D:0, A:[1.0.1]", a101.toString());

        Assignments a102 = Assignments.newAssignment("1.0.2", a101);
        assertEquals("D:0, A:[1.0.1, 1.0.2]", a102.toString());

        // there have been assignments in 1.0.x, but none in 1.1.x -> keep as is
        Assignments.CompleteMergeByCounting cmA = new Assignments.CompleteMergeByCounting(2);
        Assignments a1 = Assignments.mergeBlocks("1", cmA, Map.of("1.0.0", a102, "1.1.0", a), Map.of());
        assertEquals("D:0, A:[1.0.1, 1.0.2]", a1.toString());

        Assignments a110 = Assignments.newAssignment("1.1.0", a);
        assertEquals("D:0, A:[1.1.0]", a110.toString());

        // in this situation, there has been a full merge. we drop information about sub-blocks in the main
        // array, but store it in actualAssignmentIndices
        Assignments.CompleteMergeByCounting cmB = new Assignments.CompleteMergeByCounting(2);
        Assignments b1 = Assignments.mergeBlocks("1", cmB, Map.of("1.0.0", a102, "1.1.0", a110), Map.of());
        assertEquals("D:0, A:[1.0.1, 1.0.2, 1.1.0, 1=M]", b1.toString());

        assertFalse(b1.hasAValueAt("0"));
        assertFalse(b1.hasAValueAt("1"));
        assertTrue(b1.hasAValueAt("1.0.1"));
        assertTrue(b1.hasAValueAt("1.0.2"));
        assertTrue(b1.hasAValueAt("1.0.3"));
        assertFalse(b1.hasAValueAt("1.2.0"));
        assertTrue(b1.hasAValueAt("2"));
        assertTrue(b1.hasAValueAt("2.0.1"));
        assertTrue(b1.hasAValueAt("3.1"));
    }


    @Test
    public void test3() {
        Assignments a = new Assignments("2.0.1");
        assertEquals(0, a.size());
        assertEquals("D:2.0.1, A:[]", a.toString());

        Assignments a101 = Assignments.newAssignment("2.0.2", a);
        assertEquals("D:2.0.1, A:[2.0.2]", a101.toString());

        Assignments a102 = Assignments.newAssignment("2.0.3.0.0", a101);
        assertEquals("D:2.0.1, A:[2.0.2, 2.0.3.0.0]", a102.toString());

        // there have been assignments in 1.0.x, but none in 1.1.x -> keep as is
        Assignments.CompleteMergeByCounting cmA = new Assignments.CompleteMergeByCounting(2);
        Assignments a1 = Assignments.mergeBlocks("1", cmA, Map.of("2.0.3.0.0", a102, "2.0.3.1.0", a), Map.of());
        assertEquals("D:2.0.1, A:[2.0.2, 2.0.3.0.0]", a1.toString());

        Assignments a110 = Assignments.newAssignment("2.0.3.1.2", a);
        assertEquals("D:2.0.1, A:[2.0.3.1.2]", a110.toString());

        // in this situation, there has been a full merge. we drop information about sub-blocks in the main
        // array, but store it in actualAssignmentIndices
        Assignments.CompleteMergeByCounting cmB = new Assignments.CompleteMergeByCounting(2);
        Assignments b1 = Assignments.mergeBlocks("2.0.3", cmB, Map.of("2.0.3.0.0", a102, "2.0.3.1.0", a110), Map.of());
        assertEquals("D:2.0.1, A:[2.0.2, 2.0.3.0.0, 2.0.3.1.2, 2.0.3=M]", b1.toString());

        assertFalse(b1.hasAValueAt("0"));
        assertFalse(b1.hasAValueAt("2.0.1"));
        assertTrue(b1.hasAValueAt("2.0.2"));
        assertFalse(b1.hasAValueAt("3"));
    }


    Assignments.CompleteMerge TRUE_COMPLETE_MERGE = new Assignments.CompleteMerge() {
        @Override
        public void add(String subIndex) {
            // nothing here
        }

        @Override
        public boolean complete() {
            return true;
        }
    };


    Assignments.CompleteMerge FALSE_COMPLETE_MERGE = new Assignments.CompleteMerge() {
        @Override
        public void add(String subIndex) {
            // nothing here
        }

        @Override
        public boolean complete() {
            return false;
        }
    };

    @Test
    public void test4() {
        Assignments a = new Assignments("0");
        Assignments a0 = Assignments.newAssignment("1.0.1.0.2", a);
        Assignments a1 = Assignments.mergeBlocks("1.0.1", TRUE_COMPLETE_MERGE, Map.of("1.0.1.0.2", a0), Map.of());
        assertEquals("D:0, A:[1.0.1.0.2, 1.0.1=M]", a1.toString());
        Assignments a2 = Assignments.newAssignment("1.1.0", a);
        Assignments a3 = Assignments.mergeBlocks("1", TRUE_COMPLETE_MERGE, Map.of("1.0.1", a1, "1.1.0", a2), Map.of());
        assertEquals("D:0, A:[1.0.1.0.2, 1.0.1=M, 1.1.0, 1=M]", a3.toString());

        assertTrue(a3.hasBeenAssignedAfterFor("1.0.1~", "2", false));
    }


    @Test
    public void test4b() {
        Assignments a = new Assignments("0");
        Assignments a0 = Assignments.newAssignment("1.0.1.0.2", a);
        Assignments a1 = Assignments.mergeBlocks("1.0.1", FALSE_COMPLETE_MERGE, Map.of("1.0.1.0.2", a0), Map.of());
        assertEquals("D:0, A:[1.0.1.0.2]", a1.toString());
        Assignments a2 = Assignments.newAssignment("1.1.0", a);
        Assignments a3 = Assignments.mergeBlocks("1", FALSE_COMPLETE_MERGE, Map.of("1.0.1", a1, "1.1.0", a2), Map.of());
        assertEquals("D:0, A:[1.0.1.0.2, 1.1.0]", a3.toString());

        assertFalse(a3.hasBeenAssignedAfterFor("1.0.1~", "2", false));
    }
}
