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
