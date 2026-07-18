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

package org.e2immu.analyzer.modification.analyzer;

import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.language.cst.api.analysis.Message;
import org.e2immu.language.cst.api.info.Info;

import java.util.List;

public interface IteratingAnalyzer {

    interface Configuration {
        default LinkComputer.Options linkComputerOptions() {
            return new LinkComputer.Options.Builder()
                    .setRecurse(true)
                    .setTrackObjectCreations(trackObjectCreations())
                    .build();
        }

        int maxIterations();

        // the alternative is: set all to non-modifying
        boolean stopWhenCycleDetectedAndNoImprovements();

        boolean trackObjectCreations();

        CycleBreakingStrategy cycleBreakingStrategy();

        /** When true (the default), verify user-written contracts against computed values after the fixed
         *  point, emitting explanatory findings; see {@link GuardAnalyzer}. */
        boolean guardContracts();

        /**
         * When true, emit advisory WARN findings for types that narrowly miss a property — e.g. a type that
         * would satisfy {@code @Container} but for a single modifying parameter. The mirror image of the guard:
         * no contract is written, the computed property is decided FALSE, but one culprit away from holding.
         * Off by default (opt-in for refactoring surveys), so ordinary runs are unaffected. See
         * {@link GuardAnalyzer} and {@link #nearMissPolicy()}.
         */
        default boolean warnNearMisses() {
            return false;
        }

        /** Thresholds gating {@link #warnNearMisses()}; only consulted when that is true. */
        default NearMissPolicy nearMissPolicy() {
            return NearMissPolicy.STRICT;
        }

        /**
         * When true, a failure (exception, assertion, stack overflow) while analyzing one {@code Info} is recorded
         * as an ERROR finding — category {@code analyzer-crash} / {@code link-crash} — and analysis continues with
         * the remaining {@code Info}s, instead of aborting the whole run. The offending {@code Info} is not retried
         * in later iterations. Default false, so tests and direct callers keep their fail-fast behaviour; the
         * production runners and the real-code survey turn it on. Mirrors {@code PrepAnalyzer.Options.faultTolerant}.
         */
        boolean faultTolerant();
    }

    /**
     * Thresholds gating container near-miss warnings ({@link Configuration#warnNearMisses()}). A type is a
     * container near-miss only when it has at least {@code minParameterSlots} parameter slots (one per parameter
     * of a non-private constructor/method), at most {@code maxBlockingSlots} of them are modified, and — for a
     * blocking slot on an <em>abstract</em> method — the modification is attributable to between 1 and
     * {@code maxBlockingImplementations} implementations out of at least {@code minImplementations}. For the
     * type-level {@code @Immutable}/{@code @Independent} near-misses, the surface is the field count (at least
     * {@code minFields}) and {@code maxBlockingSlots} caps the blocking fields. The absolute caps plus the surface
     * floors keep this to the compelling "one culprit" cases; see the design note in {@code guard-mode-analysis.md}.
     */
    record NearMissPolicy(int minParameterSlots, int maxBlockingSlots, int minImplementations,
                          int maxBlockingImplementations, int minFields) {
        /** The strict defaults: a single blocking member, on a surface of at least 7 parameter slots / 3 fields,
         *  attributable to a single implementation out of at least 3. */
        public static final NearMissPolicy STRICT = new NearMissPolicy(7, 1, 3, 1, 3);
    }

    void analyze(List<Info> analysisOrder);

    /**
     * Register a streaming consumer of established analysis values (see {@link AnalysisValueFeed}).
     * Optional; call before {@link #analyze}. Default: no-op for implementations without streaming.
     */
    default void setValueFeed(AnalysisValueFeed feed) {
        // no-op
    }

    /**
     * Like {@link #analyze(List)}, additionally providing the dependency graph (edge from X to Y = X depends
     * on Y, as built by ComputeCallGraph). Worklist narrowing (default ON; opt out with NOWORKLIST=1) then makes iterations 2+ only re-analyze elements
     * that changed in the previous iteration plus their dependents (reverse edges) — the worklist narrowing.
     */
    default void analyze(List<Info> analysisOrder, org.e2immu.util.internal.graph.G<Info> dependencyGraph) {
        analyze(analysisOrder, dependencyGraph, null);
    }

    /**
     * Incremental entry point for the early-cutoff skip (analysis-rewiring.md). When {@code initialDirty} is
     * non-null, iteration 1 analyses only those elements (instead of the whole order), the worklist propagates from
     * them through the dependency graph on summary change, and — crucially — the run <em>stops when the worklist is
     * dry</em>, with no full verification/cycle-breaking pass (which would re-touch the carried, untouched elements
     * and defeat the skip). Elements the worklist never reaches keep whatever analysis they already hold (their
     * carried values). {@code null} ⇒ the normal full analysis.
     */
    default void analyze(List<Info> analysisOrder, org.e2immu.util.internal.graph.G<Info> dependencyGraph,
                         java.util.Set<Info> initialDirty) {
        analyze(analysisOrder, dependencyGraph, initialDirty, null);
    }

    /**
     * As {@link #analyze(List, org.e2immu.util.internal.graph.G, java.util.Set)}, with a clear-before-recompute hook.
     * In incremental mode the worklist discovers the dirty frontier dynamically: a <em>carried</em> type is pulled in
     * only when a dependency's summary changes. Before such a type is re-analysed, its carried cross-type-derived
     * values must be cleared, or the monotonic overwrite guard rejects a value the fresh analysis <em>lowers</em>
     * (unlike a normal run, where iteration 1 computes the value from nothing and later iterations only refine it
     * upward). {@code beforeFirstRecompute} is invoked once per element, the first time it is about to be analysed
     * <em>after</em> the seed round (the seed is fresh, never carried, so it is skipped). The driver's callback clears
     * the {@code CROSS_TYPE_DERIVED} tier of a carried element (and drops it from its carried set). {@code null} ⇒ no
     * hook. Only consulted when {@code initialDirty != null}.
     */
    default void analyze(List<Info> analysisOrder, org.e2immu.util.internal.graph.G<Info> dependencyGraph,
                         java.util.Set<Info> initialDirty, java.util.function.Consumer<Info> beforeFirstRecompute) {
        analyze(analysisOrder, dependencyGraph);
    }

    /** Findings (warnings/errors about the analyzed code) collected across all iterations; empty before analyze(). */
    List<Message> messages();
}
