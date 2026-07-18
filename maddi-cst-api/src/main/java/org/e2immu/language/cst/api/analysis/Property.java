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

package org.e2immu.language.cst.api.analysis;

public interface Property {
    Class<? extends Value> classOfValue();

    Value defaultValue();

    String key();

    /**
     * Whether this property's value survives a rewire (see {@code docs/rewiring.md}).
     * <p>
     * A rewired type is one whose own source did not change, but which reaches a type that did — so anything
     * <em>derived across types</em> (links, immutability, independence) was computed against source that no longer
     * exists, and must be recomputed rather than carried: for those, dropping is the correct answer, not merely the
     * safe one. Hence the default of {@code false}.
     * <p>
     * Two kinds do want carrying. Data <em>intrinsic</em> to the type's own body — prepwork's, chiefly
     * {@code VARIABLE_DATA} — is still valid, because the body did not change, and recomputing it is most of the
     * cost a reload exists to avoid. Data written at <em>parse</em> time is stronger still: a rewired type is never
     * re-parsed, so if it is not carried it is simply lost.
     * <p>
     * Saying true here is a claim that the value is one of those two, and that its {@link Value#rewire} maps every
     * Info and Variable reference it holds.
     */
    default boolean carryOnRewire() {
        return false;
    }

    /**
     * Which stage produces this property's value — the classification the incremental early-cutoff skip is built on
     * (see {@code docs/analysis-rewiring.md}). Three tiers, by <em>who recomputes the value on a reload</em>:
     * <ul>
     *   <li>{@link AnalysisTier#PARSE_TIME} — written when the source is parsed. A rewired type is never re-parsed,
     *       so the value is carried by the rewire phase or lost. Equivalent to {@link #carryOnRewire()}.</li>
     *   <li>{@link AnalysisTier#INTRINSIC} — derived from the type's <em>own body</em> by prepwork
     *       ({@code VARIABLE_DATA}, {@code PART_OF_CONSTRUCTION}, {@code FINAL_FIELD}, …). Prepwork re-derives it on
     *       every run, so it is <em>recomputed, never carried</em>: carrying it onto a type that prep then re-visits
     *       would double-set.</li>
     *   <li>{@link AnalysisTier#CROSS_TYPE_DERIVED} — derived <em>across types</em> by the link computer and
     *       modification analyzer ({@code IMMUTABLE_*}, {@code INDEPENDENT_*}, {@code LINKS}, {@code METHOD_LINKS},
     *       {@code IMPLEMENTATIONS}, …). The expensive tier a reload exists to avoid; the early-cutoff skip
     *       <em>carries</em> these onto a fingerprint-stable rewired type instead of recomputing them.</li>
     * </ul>
     * The default classifies by {@link #carryOnRewire()} (parse-time when true, cross-type-derived otherwise);
     * the intrinsic-tier properties declare {@code INTRINSIC} explicitly.
     */
    default AnalysisTier analysisTier() {
        return carryOnRewire() ? AnalysisTier.PARSE_TIME : AnalysisTier.CROSS_TYPE_DERIVED;
    }

    enum AnalysisTier {
        PARSE_TIME,
        INTRINSIC,
        CROSS_TYPE_DERIVED
    }
}
