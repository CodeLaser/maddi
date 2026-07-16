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
         * When true, a failure (exception, assertion, stack overflow) while analyzing one {@code Info} is recorded
         * as an ERROR finding — category {@code analyzer-crash} / {@code link-crash} — and analysis continues with
         * the remaining {@code Info}s, instead of aborting the whole run. The offending {@code Info} is not retried
         * in later iterations. Default false, so tests and direct callers keep their fail-fast behaviour; the
         * production runners and the real-code survey turn it on. Mirrors {@code PrepAnalyzer.Options.faultTolerant}.
         */
        boolean faultTolerant();
    }

    void analyze(List<Info> analysisOrder);

    /**
     * Like {@link #analyze(List)}, additionally providing the dependency graph (edge from X to Y = X depends
     * on Y, as built by ComputeCallGraph). With the WORKLIST=1 gate, iterations 2+ only re-analyze elements
     * that changed in the previous iteration plus their dependents (reverse edges) — the worklist narrowing.
     */
    default void analyze(List<Info> analysisOrder, org.e2immu.util.internal.graph.G<Info> dependencyGraph) {
        analyze(analysisOrder);
    }

    /** Findings (warnings/errors about the analyzed code) collected across all iterations; empty before analyze(). */
    List<Message> messages();
}
