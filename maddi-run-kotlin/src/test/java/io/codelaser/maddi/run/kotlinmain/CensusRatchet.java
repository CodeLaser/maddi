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

import static org.junit.jupiter.api.Assertions.fail;

/**
 * ⭐ A <b>two-sided</b> ratchet for the Kotlin corpus numbers.
 *
 * <p>The Java corpus tests assert floors ("at least 1,000 types"), deliberately, so a detekt version bump
 * does not make them brittle. A floor is the wrong instrument for the quantity this campaign actually
 * moves: the placeholder census went 6,057 → 4,744 over a month of work, and every floor loose enough to
 * survive that is loose enough to miss a regression of several hundred.
 *
 * <p>So: a regression fails, and an IMPROVEMENT beyond {@link #TOLERANCE} fails too. A bound nobody
 * tightens rots into a floor that proves nothing, and the only reliable moment to tighten it is the run
 * that beat it. When this fails for being better, that is the instrument working — record the new number.
 *
 * <p>⚠ A bound here is only as good as the run it came from. Quote the date and what was measured, and
 * never copy one from a cached or skipped run (see {@code AGENTS.md} §Commands).
 */
final class CensusRatchet {
    private CensusRatchet() {
    }

    /** Absolute slack allowed before an improvement must be recorded; also covers small corpus drift. */
    private static final int TOLERANCE = 5;

    /** For a quantity where LOWER is better: placeholders, isolated elements. */
    static void noWorseThan(String what, int actual, int bound) {
        if (actual > bound) {
            fail(what + " regressed: " + actual + " > the recorded " + bound
                 + ". Either fix it, or re-baseline deliberately and say in the commit what moved and why.");
        }
        if (actual < bound - TOLERANCE) {
            fail(what + " IMPROVED to " + actual + ", beating the recorded " + bound
                 + ". Record it here in this commit — an untightened bound stops measuring.");
        }
    }

    /** For a quantity where HIGHER is better: immutable types, primary types. */
    static void noWorseThanAtLeast(String what, int actual, int bound) {
        if (actual < bound) {
            fail(what + " regressed: " + actual + " < the recorded " + bound
                 + ". Either fix it, or re-baseline deliberately and say in the commit what moved and why.");
        }
        if (actual > bound + TOLERANCE) {
            fail(what + " IMPROVED to " + actual + ", beating the recorded " + bound
                 + ". Record it here in this commit — an untightened bound stops measuring.");
        }
    }
}
