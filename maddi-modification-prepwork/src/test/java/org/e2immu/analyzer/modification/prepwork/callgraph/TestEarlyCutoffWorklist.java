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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The early-cutoff worklist, on a use-graph chain {@code A <- B <- C <- D} (B uses A, C uses B, D uses C), edit at A.
 * Demonstrates that cutoff spares the transitive tail: recomputation stops at the first type whose output is unchanged.
 */
public class TestEarlyCutoffWorklist {

    // immediate dependents (who directly uses X): A used by B, B by C, C by D, D by nobody.
    private static final Function<String, Set<String>> DEPENDENTS = x -> switch (x) {
        case "A" -> Set.of("B");
        case "B" -> Set.of("C");
        case "C" -> Set.of("D");
        default -> Set.of();
    };
    private static final Map<String, String> PRIOR = Map.of("A", "a1", "B", "b1", "C", "c1", "D", "d1");
    private static final Function<String, String> PRIOR_FP = PRIOR::get;

    @DisplayName("output unchanged at the seed: nothing downstream is recomputed")
    @Test
    public void testSeedUnchangedCutsOffAll() {
        // A recomputes to the same fingerprint (e.g. a comment edit): no propagation at all.
        EarlyCutoffWorklist.Result<String> r = EarlyCutoffWorklist.run(List.of("A"), DEPENDENTS, PRIOR_FP, PRIOR::get);
        assertEquals(Set.of("A"), r.recomputed(), "only the seed is recomputed; B, C, D are cut off");
    }

    @DisplayName("change propagates only until a type's output stabilises")
    @Test
    public void testCutoffSparesTheTail() {
        // A and B change, C recomputes to its prior fingerprint -> D is spared.
        Map<String, String> fresh = Map.of("A", "a2", "B", "b2", "C", "c1", "D", "d1");
        EarlyCutoffWorklist.Result<String> r = EarlyCutoffWorklist.run(List.of("A"), DEPENDENTS, PRIOR_FP, fresh::get);
        assertEquals(Set.of("A", "B", "C"), r.recomputed(),
                "A changed -> check B; B changed -> check C; C unchanged -> cut off, D spared");
        assertFalse(r.recomputed().contains("D"), "the tail past the firewall is spared");
    }

    @DisplayName("change reaches the end: everything on the path is recomputed")
    @Test
    public void testChangeReachesTheEnd() {
        Map<String, String> fresh = Map.of("A", "a2", "B", "b2", "C", "c2", "D", "d2");
        EarlyCutoffWorklist.Result<String> r = EarlyCutoffWorklist.run(List.of("A"), DEPENDENTS, PRIOR_FP, fresh::get);
        assertEquals(Set.of("A", "B", "C", "D"), r.recomputed());
    }

    @DisplayName("a cycle converges (chaotic iteration stops once fingerprints stabilise)")
    @Test
    public void testCycleConverges() {
        // A <-> B mutually depend; both settle to a new fingerprint on the first recompute and stay there.
        Function<String, Set<String>> cyclic = x -> "A".equals(x) ? Set.of("B") : Set.of("A");
        Map<String, String> fresh = Map.of("A", "a2", "B", "b2");
        EarlyCutoffWorklist.Result<String> r = EarlyCutoffWorklist.run(List.of("A"), cyclic,
                Map.of("A", "a1", "B", "b1")::get, fresh::get);
        assertEquals(Set.of("A", "B"), r.recomputed());
        // A changed -> queue B; B changed -> queue A again; A recomputes to a2 == last a2 -> no further propagation.
        assertTrue(r.recomputeCount() >= 2 && r.recomputeCount() <= 4, "converges, no infinite loop: " + r.recomputeCount());
    }
}
