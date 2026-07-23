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

package org.e2immu.analyzer.modification.analyzer.eventual;

import org.e2immu.analyzer.modification.analyzer.impl.EventualClusterContraction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Greatest-fixpoint, step 2: the contraction as pure set logic. {@link EventualClusterContraction#membersToRetract}
 * keeps the largest subset of the verdict-holders closed under "every candidate I assumed is also retained", and
 * returns the rest to be retracted. These cases pin the fixpoint: a self-consistent cycle survives whole, a
 * broken assumption drops (and cascades), and a genuinely-independent verdict is never touched.
 */
public class TestEventualClusterContraction {

    @DisplayName("a self-consistent cycle survives whole: nothing is retracted")
    @Test
    public void testSelfConsistentCycleSurvives() {
        // A and B assume each other, both hold a verdict: the whole SCC is retained (the maddi Info-family shape)
        Set<String> retract = EventualClusterContraction.membersToRetract(
                Set.of("A", "B"),
                Map.of("A", Set.of("B"), "B", Set.of("A")));
        assertEquals(Set.of(), retract);
    }

    @DisplayName("a member that assumed a candidate which never got a verdict is retracted")
    @Test
    public void testBrokenAssumptionRetracted() {
        // A assumed C, but C never obtained a verdict (not in haveVerdict) -> A is unjustified
        Set<String> retract = EventualClusterContraction.membersToRetract(
                Set.of("A"),
                Map.of("A", Set.of("C")));
        assertEquals(Set.of("A"), retract);
    }

    @DisplayName("retraction cascades: whoever leaned on a retracted member is retracted too")
    @Test
    public void testCascade() {
        // A assumed absent C -> A dropped; B assumed A -> B dropped in the next round
        Set<String> retract = EventualClusterContraction.membersToRetract(
                Set.of("A", "B"),
                Map.of("A", Set.of("C"), "B", Set.of("A")));
        assertEquals(Set.of("A", "B"), retract);
    }

    @DisplayName("a genuinely-independent verdict (no assumptions) is never retracted")
    @Test
    public void testIndependentVerdictKept() {
        // G assumed nothing (a proven, non-cluster eventual type); A leaned on absent C
        Set<String> retract = EventualClusterContraction.membersToRetract(
                Set.of("G", "A"),
                Map.of("A", Set.of("C")));
        assertEquals(Set.of("A"), retract);
    }

    @DisplayName("a self-consistent core is kept even when a sibling with a broken assumption is dropped")
    @Test
    public void testMixed() {
        // {A,B} assume each other (kept); X assumed absent Y (dropped); the drop of X does not touch the core
        Set<String> retract = EventualClusterContraction.membersToRetract(
                Set.of("A", "B", "X"),
                Map.of("A", Set.of("B"), "B", Set.of("A"), "X", Set.of("Y")));
        assertEquals(Set.of("X"), retract);
    }
}
