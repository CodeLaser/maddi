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

package org.e2immu.analyzer.modification.prepwork.io;

/**
 * A single normalization applied to the serialised analysis dump before it is hashed into an
 * {@link AnalysisFingerprint}. Each normalizer widens the class of source edits the fingerprint is invariant to,
 * by erasing from the dump some detail that does not affect what a <em>dependent</em> observes:
 * <ul>
 *   <li>{@link SourcePositionNormalizer} — source line:col coordinates (a line-shifting edit must not dirty an
 *       otherwise-unchanged type);</li>
 *   <li>(later) variable-name blanking for rename-invariance; canonicalisation of orderings; etc.</li>
 * </ul>
 * <p>
 * Normalizers compose as an ordered pipeline (see {@link AnalysisFingerprint}); many are expected over time, so
 * this is deliberately a small, open interface. The substrate is the serialised dump ({@code String}) — the one
 * representation every normalizer can share; a structurally-aware normalizer may parse and re-serialise internally.
 * <p>
 * <b>Soundness contract:</b> a normalizer may only erase detail a dependent cannot read. Erasing more than that
 * would make two genuinely-different results hash equal and cut off a recomputation that was actually needed.
 * Erasing positions is sound (no analysis reads a callee's source coordinates); erasing a verdict would not be.
 */
public interface FingerprintNormalizer {

    /** A short, stable, kebab-case identifier (for logging and profile composition). */
    String name();

    /** Return {@code encoded} with this normalizer's target detail erased; must be deterministic and idempotent. */
    String normalize(String encoded);
}
