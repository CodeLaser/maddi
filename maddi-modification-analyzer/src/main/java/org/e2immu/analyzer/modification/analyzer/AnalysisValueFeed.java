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

import org.e2immu.language.cst.api.info.Info;

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
