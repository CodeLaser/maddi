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

import org.e2immu.language.cst.api.analysis.Message;
import org.e2immu.language.cst.api.info.Info;

import java.util.List;

public interface SingleIterationAnalyzer {

    default void go(List<Info> analysisOrder, boolean activateCycleBreaking) {
        go(analysisOrder, activateCycleBreaking, true);
    }

    void go(List<Info> analysisOrder, boolean activateCycleBreaking, boolean firstIteration);

    /**
     * Like the 3-arg {@code go}, additionally providing dependency waves for a strata-parallel FIRST
     * iteration (see ComputeAnalysisOrder.waves): each wave's units run concurrently, units are sequential
     * inside, waves are barriers. Ignored (delegates) by default or when null.
     */
    default void go(List<Info> analysisOrder, boolean activateCycleBreaking, boolean firstIteration,
                    List<List<List<Info>>> firstIterationWaves) {
        go(analysisOrder, activateCycleBreaking, firstIteration);
    }

    int propertiesChanged();

    /**
     * Summary-consumption edges discovered by the link computer (consumer read consumed's METHOD_LINKS
     * while computing its own links) — see LinkComputer.recordSummaryConsumption. Accumulated over the run.
     */
    default java.util.Map<org.e2immu.language.cst.api.info.MethodInfo,
            java.util.Set<org.e2immu.language.cst.api.info.MethodInfo>> consumedSummaries() {
        return java.util.Map.of();
    }

    /**
     * Worklist support: the elements whose analysis values changed during the most recent {@code go} call --
     * per-element counter-delta attribution united with the write-target attribution from TolerantWrite (which
     * also catches the link computer's on-demand recursion writing a CALLEE's summary mid-caller).
     */
    java.util.Set<Info> changedInfos();

    /**
     * Worklist v2: only elements whose EXTERNALLY VISIBLE summary changed (method links/modification verdicts,
     * field/type verdicts) — the set whose dependents must be re-analyzed. Element-internal (statement-level)
     * changes are excluded: dependents cannot observe them.
     */
    java.util.Set<Info> summaryChangedInfos();

    /** Findings (warnings/errors about the analyzed code) collected by all analyzers of this iteration. */
    List<Message> messages();
}
