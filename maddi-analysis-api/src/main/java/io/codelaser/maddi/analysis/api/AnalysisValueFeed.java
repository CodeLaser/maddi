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

package io.codelaser.maddi.analysis.api;

import io.codelaser.maddi.cst.api.info.Info;

import java.util.Collection;

/**
 * Streaming view of analysis values as the fixpoint iteration progresses, for consumers (the IDE daemon)
 * that want to display established values before certification. The design rests on the property
 * discipline: published values are write-once and refine monotonically, so a consumer never needs
 * retraction — a displayed value can only get stronger.
 *
 * <p>The engine emits at PASS BOUNDARIES, from the coordinator thread, piggybacking on the worklist: the
 * {@code analyzed} collection of {@link #passCompleted} is the set of elements analyzed in that pass — all
 * of them on the first (full) pass, the shrinking dirty set afterwards. It over-approximates the changed
 * set; consumers read {@code info.analysis()} directly (all properties, current values — Info objects are
 * shared in-process) and may diff against their previous state. Values read this way are safe: the workers
 * are quiescent between passes.
 *
 * <p>Status ladder for display: a value seen in pass p is PROVISIONAL; once its element stops appearing in
 * subsequent passes it is quiet (in practice the vast majority after 2-3 passes); the
 * {@link Phase#TERMINAL_CERTIFIED} event upgrades everything to final. Note that positive type-immutability
 * conclusions frequently arrive only in the cycle-breaking pass, i.e. LAST — after
 * {@link Phase#CYCLE_BREAKING_ACTIVATED}.
 *
 * <p>Feed exceptions are caught and logged; they never disturb the analysis.
 */
public interface AnalysisValueFeed {

    /**
     * @param iteration 1-based pass number
     * @param fullPass  true when the whole analysis order was (re-)analyzed, false for a worklist subset
     * @param analyzed  the elements analyzed in this pass; read {@code info.analysis()} for their values.
     *                  The collection is valid only for the duration of the call — copy if retained.
     */
    void passCompleted(int iteration, boolean fullPass, Collection<Info> analyzed);

    /**
     * Emitted at each wave barrier of a strata-parallel FIRST pass (workers quiescent, coordinator
     * thread — same safety as a pass boundary). At monorepo scale the first pass runs for hours;
     * pass-boundary consumers see nothing during that stretch. The wave's elements have been analyzed
     * for this pass, but later waves and the post-loop analyzers may still refine their values —
     * treat these as provisional snapshots, superseded by the pass-boundary emission.
     */
    default void waveCompleted(int iteration, int wave, Collection<Info> analyzed) {
    }

    /**
     * Emitted once per element as its {@code processElement} completes, on the (possibly parallel) worker
     * thread — so it must be cheap and thread-safe. Its purpose is intra-wave progress: {@link #waveCompleted}
     * only fires at a barrier, and a giant single SCC is ONE wave, so committed progress can pin for the whole
     * multi-hour stretch. Counting completed elements lets progress advance smoothly inside that wave. It
     * carries no values (the element's analysis may still be refined this pass); it is a counter tick only.
     */
    default void elementCompleted() {
    }

    void phase(Phase phase, int iteration);

    enum Phase {
        /** certification reached with immutability-undecided types: one more full pass runs with breaking. */
        CYCLE_BREAKING_ACTIVATED,
        /** the fixpoint is certified: all values are final. */
        TERMINAL_CERTIFIED,
        /** stopped at the iteration cap: values are the best available, not certified. */
        TERMINAL_MAX_ITERATIONS,
        /** stopped on the oscillation plateau: values are the best available, not certified. */
        TERMINAL_PLATEAU
    }
}
