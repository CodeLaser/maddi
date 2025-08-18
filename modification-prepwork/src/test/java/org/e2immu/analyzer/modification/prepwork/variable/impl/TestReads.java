package org.e2immu.analyzer.modification.prepwork.variable.impl;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestReads {

    @Test
    public void test() {
        Reads reads = new Reads(List.of("2", "4", "5"));
        assertTrue(reads.between("1", "3"));
        assertTrue(reads.between("1", "4"));
        assertTrue(reads.between("1", "4.0.1"));
        assertTrue(reads.between("3.0", "4.0.1"));
        assertFalse(reads.between("2.0.0", "3"));
    }
}
