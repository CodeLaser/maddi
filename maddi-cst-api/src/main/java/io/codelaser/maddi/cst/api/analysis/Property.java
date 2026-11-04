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

package io.codelaser.maddi.cst.api.analysis;

public interface Property {
    Class<? extends Value> classOfValue();

    Value defaultValue();

    String key();

    /**
     * Whether this property's value survives a rewire (see {@code docs/design/rewiring.md}).
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
     * (see {@code docs/design/analysis-rewiring.md}). Three tiers, by <em>who recomputes the value on a reload</em>:
     * <ul>
     *   <li>{@link AnalysisTier#PARSE_TIME} — written when the source is parsed. A rewired type is never re-parsed,
     *       so the value is carried by the rewire phase or lost. Equivalent to {@link #carryOnRewire()}.</li>
     *   <li>{@link AnalysisTier#INTRINSIC} — derived from the type's <em>own body</em> by prepwork
     *       ({@code VARIABLE_DATA}, {@code PART_OF_CONSTRUCTION}, {@code FINAL_FIELD}, …). Per-type output that a
     *       property opts into {@link #carryOnRewire()} ({@code VARIABLE_DATA}, {@code ALWAYS_ESCAPES}, the prep
     *       marker) is carried onto a rewired type, which prep then skips as it skips a kept one; the rest, the
     *       graph-wide values ({@code PART_OF_CONSTRUCTION}, {@code FINAL_FIELD}, {@code RECURSIVE_METHOD}), is
     *       recomputed. So are {@code IMPLEMENTATIONS} and {@code EXTERNAL_IMPLEMENTATIONS}, which prep writes onto
     *       the overridden methods and repeats on every run.</li>
     *   <li>{@link AnalysisTier#CROSS_TYPE_DERIVED} — derived <em>across types</em> by the link computer and
     *       modification analyzer ({@code IMMUTABLE_*}, {@code INDEPENDENT_*}, {@code LINKS}, {@code METHOD_LINKS},
     *       …). A seeded analysis clears this tier on an element before it recomputes it. The expensive tier a reload exists to avoid; the early-cutoff skip
     *       <em>carries</em> these onto a fingerprint-stable rewired type instead of recomputing them.</li>
     * </ul>
     * The default classifies by {@link #carryOnRewire()} (parse-time when true, cross-type-derived otherwise);
     * the intrinsic-tier properties declare {@code INTRINSIC} explicitly, carried or not.
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
