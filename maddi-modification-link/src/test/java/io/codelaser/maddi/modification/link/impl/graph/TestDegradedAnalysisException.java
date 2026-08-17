package org.e2immu.analyzer.modification.link.impl.graph;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/*
 The two guards that abandon a method used to throw the SAME UnsupportedOperationException("cycle protection"),
 so a corpus degrading in bulk could not be attributed to either: on elasticsearch, 111 degraded methods and no
 way to tell a structural non-convergence from a spent budget. They want opposite answers (raise the ceiling vs
 fix the graph), so the reason must survive to the log.
 */
public class TestDegradedAnalysisException {

    @DisplayName("the two guards are distinguishable, and both keep the 'cycle protection' contract")
    @Test
    public void test() {
        DegradedAnalysisException rounds =
                new DegradedAnalysisException(DegradedAnalysisException.Reason.EXPANSION_ROUNDS);
        DegradedAnalysisException ceiling =
                new DegradedAnalysisException(DegradedAnalysisException.Reason.WORK_CEILING);

        assertNotEquals(rounds.getMessage(), ceiling.getMessage());
        assertEquals("cycle protection: expansion rounds", rounds.getMessage());
        assertEquals("cycle protection: work ceiling", ceiling.getMessage());
        assertEquals(DegradedAnalysisException.Reason.WORK_CEILING, ceiling.reason());

        // LinkComputerImpl.doMethod recognizes the degradation contract by TYPE and rethrows anything else;
        // every reason must therefore stay an UnsupportedOperationException.
        for (DegradedAnalysisException.Reason reason : DegradedAnalysisException.Reason.values()) {
            DegradedAnalysisException e = new DegradedAnalysisException(reason);
            assertInstanceOf(UnsupportedOperationException.class, e);
            assertTrue(e.getMessage().startsWith(DegradedAnalysisException.PREFIX));
        }
    }
}
