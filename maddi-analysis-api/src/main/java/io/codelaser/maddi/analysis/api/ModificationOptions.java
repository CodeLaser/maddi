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

package io.codelaser.maddi.analysis.api;

/**
 * The analyzer's configuration. A null field keeps the analyzer's own default, so each caller states exactly
 * what it stated before the split and nothing more.
 *
 * @param environmentGates honour the experimental environment gates of the CLI: {@code SHADOWDIFF},
 *                         {@code MODREACH}, {@code CHECKPOINT}, {@code CHECKPOINT_RESTORE}, {@code INCREMENTAL},
 *                         {@code INCREMENTAL_FILL}. When true they decide {@code trackObjectCreations} and
 *                         {@code modificationViaReachability}, whatever those fields say.
 * @param storeFingerprints after the analysis, store each source set's analysis fingerprint rollup
 *                          (incremental early cut-off, docs/design/analysis-rewiring.md), BEFORE the environment
 *                          gates' incremental state is captured -- the order the CLI always had.
 * @param staticSideEffects the static-side-effect analysis on or off for this run; null keeps the analyzer's
 *                          process-wide default (on when the environment variable {@code SSE} is set)
 * @param eventualCluster   the eventual-immutability cluster on or off for this run; null keeps the analyzer's
 *                          process-wide default (on unless {@code EVENTUALCLUSTER=0})
 */
public record ModificationOptions(Integer maxIterations,
                                  Boolean stopWhenCycleDetectedAndNoImprovements,
                                  Boolean trackObjectCreations,
                                  Boolean modificationViaReachability,
                                  Boolean faultTolerant,
                                  Boolean warnNearMisses,
                                  boolean environmentGates,
                                  boolean storeFingerprints,
                                  Boolean staticSideEffects,
                                  Boolean eventualCluster) {

    /** Both feature switches at the analyzer's process-wide default. */
    public ModificationOptions(Integer maxIterations, Boolean stopWhenCycleDetectedAndNoImprovements,
                               Boolean trackObjectCreations, Boolean modificationViaReachability,
                               Boolean faultTolerant, Boolean warnNearMisses, boolean environmentGates,
                               boolean storeFingerprints) {
        this(maxIterations, stopWhenCycleDetectedAndNoImprovements, trackObjectCreations,
                modificationViaReachability, faultTolerant, warnNearMisses, environmentGates, storeFingerprints,
                null, null);
    }
}
