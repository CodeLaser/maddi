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

package org.e2immu.analyzer.modification.prepwork.callgraph;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * The early-cutoff orchestration (docs/analysis-rewiring.md): the "red-green" worklist that decides the minimal set of
 * types to recompute after an edit, and cuts off propagation where a recomputed type's <em>output</em> is unchanged.
 * <p>
 * Seed the worklist with the types whose source changed (the {@code INVALID} set). Recompute a type, hash its output
 * (its {@code analysisFingerprint}), and compare to the previous run's: only if it <em>changed</em> do its dependents
 * become candidates. A type whose output is unchanged is a firewall — its dependents are spared. Everything never
 * reached (never recomputed) keeps its prior analysis (carried). The saving is the difference between the whole set
 * and {@link Result#recomputed()}.
 * <p>
 * Generic over the node type {@code <T>} (a primary type) and the fingerprint type {@code <F>}, and over the
 * recompute step, so the algorithm is testable without the analyzer. Chaotic iteration with per-node last-fingerprint
 * tracking, so it converges on a use graph with cycles (mutually-referential types): propagation stops once
 * fingerprints stabilise. Intra-type fixpoints are the analyzer's own concern, not this worklist's.
 */
public class EarlyCutoffWorklist {

    /** {@code recomputed}: the types that were (re)computed at least once; {@code recomputeCount}: total recompute
     *  steps (≥ recomputed.size() when the graph has cycles). Everything outside {@code recomputed} was cut off. */
    public record Result<T>(Set<T> recomputed, int recomputeCount) {
    }

    private EarlyCutoffWorklist() {
    }

    /**
     * @param seed                 the types whose source changed (INVALID); the worklist starts here.
     * @param immediateDependents  one hop in the use graph: the types that directly use {@code t}.
     * @param priorFingerprint     {@code t}'s analysisFingerprint from the previous run, or {@code null} if new.
     * @param recompute            recompute {@code t}'s analysis and return its fresh analysisFingerprint.
     */
    public static <T, F> Result<T> run(Collection<T> seed,
                                       Function<T, Set<T>> immediateDependents,
                                       Function<T, F> priorFingerprint,
                                       Function<T, F> recompute) {
        Set<T> inWorklist = new LinkedHashSet<>(seed);
        Deque<T> worklist = new ArrayDeque<>(inWorklist);
        Map<T, F> lastFingerprint = new HashMap<>();
        Set<T> recomputed = new LinkedHashSet<>();
        int recomputeCount = 0;
        while (!worklist.isEmpty()) {
            T t = worklist.poll();
            inWorklist.remove(t);
            F newFingerprint = recompute.apply(t);
            recomputeCount++;
            recomputed.add(t);
            F previous = lastFingerprint.containsKey(t) ? lastFingerprint.get(t) : priorFingerprint.apply(t);
            lastFingerprint.put(t, newFingerprint);
            if (!Objects.equals(newFingerprint, previous)) {
                // output changed -> its dependents may change too; otherwise cut off (spare the dependents)
                for (T dependent : immediateDependents.apply(t)) {
                    if (inWorklist.add(dependent)) worklist.add(dependent);
                }
            }
        }
        return new Result<>(recomputed, recomputeCount);
    }
}
