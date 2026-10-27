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
package io.codelaser.maddi.run.kotlinmain;

import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ⚠ The ratchet is an assertion, and an assertion nobody has seen fail is a guess. The corpus runs can only
 * ever exercise its PASSING side (the numbers are, by construction, the recorded ones), so both failing
 * sides are proved here instead — for free, with no checkout.
 */
public class TestCensusRatchet {

    @Test
    public void aRegressionFails() {
        AssertionFailedError e = assertThrows(AssertionFailedError.class,
                () -> CensusRatchet.noWorseThan("placeholders", 4_800, 4_704));
        assertTrue(e.getMessage().contains("regressed"), e.getMessage());
        AssertionFailedError up = assertThrows(AssertionFailedError.class,
                () -> CensusRatchet.noWorseThanAtLeast("immutable types", 600, 668));
        assertTrue(up.getMessage().contains("regressed"), up.getMessage());
    }

    /** ⭐ The half that makes it a ratchet rather than a floor: getting better without saying so also fails. */
    @Test
    public void anUnrecordedImprovementAlsoFails() {
        AssertionFailedError e = assertThrows(AssertionFailedError.class,
                () -> CensusRatchet.noWorseThan("placeholders", 4_600, 4_704));
        assertTrue(e.getMessage().contains("IMPROVED"), e.getMessage());
        assertTrue(e.getMessage().contains("stops measuring"), e.getMessage());
        AssertionFailedError up = assertThrows(AssertionFailedError.class,
                () -> CensusRatchet.noWorseThanAtLeast("immutable types", 700, 668));
        assertTrue(up.getMessage().contains("IMPROVED"), up.getMessage());
    }

    /** Small drift either way is tolerated, so a corpus is not re-baselined for noise. */
    @Test
    public void theRecordedValueAndSmallDriftPass() {
        assertDoesNotThrow(() -> CensusRatchet.noWorseThan("placeholders", 4_704, 4_704));
        assertDoesNotThrow(() -> CensusRatchet.noWorseThan("placeholders", 4_701, 4_704));
        assertDoesNotThrow(() -> CensusRatchet.noWorseThanAtLeast("immutable types", 670, 668));
        // ...and an exact zero is a legitimate bound: prep isolation has been 0 for the whole campaign
        assertDoesNotThrow(() -> CensusRatchet.noWorseThan("isolated by prep", 0, 0));
        assertThrows(AssertionFailedError.class, () -> CensusRatchet.noWorseThan("isolated by prep", 1, 0));
    }
}
