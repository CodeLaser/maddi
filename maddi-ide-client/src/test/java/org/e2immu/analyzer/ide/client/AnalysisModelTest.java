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

package org.e2immu.analyzer.ide.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link AnalysisModel#isNearMiss} is the one definition both front-ends use to decide whether a finding is
 * a defect or a suggestion, and it drives how they highlight it. The categories it matches come from the
 * analyzer's {@code GuardAnalyzerImpl}.
 */
public class AnalysisModelTest {

    private static AnalysisModel.Finding withCategory(String category) {
        return new AnalysisModel.Finding("file:///X.java", 1, 1, 1, 2, "WARN", category, "m", List.of());
    }

    @DisplayName("every near-miss category the analyzer emits is recognised")
    @Test
    public void nearMissCategoriesAreRecognised() {
        for (String category : List.of("near-miss-container", "near-miss-not-modified",
                "near-miss-independent", "near-miss-immutable")) {
            assertTrue(AnalysisModel.isNearMiss(withCategory(category)), category);
        }
    }

    @DisplayName("defects and parse errors are not near misses")
    @Test
    public void otherCategoriesAreNot() {
        assertFalse(AnalysisModel.isNearMiss(withCategory("contract-violation")));
        assertFalse(AnalysisModel.isNearMiss(withCategory("parse")));
        assertFalse(AnalysisModel.isNearMiss(withCategory("general")));
    }

    @DisplayName("a missing category never trips the check")
    @Test
    public void nullsAreSafe() {
        assertFalse(AnalysisModel.isNearMiss(withCategory(null)));
        assertFalse(AnalysisModel.isNearMiss(null));
    }
}
