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
 *                          process-wide default (on)
 * @param eventualCluster   the eventual-immutability cluster on or off for this run; null keeps the analyzer's
 *                          process-wide default (on unless {@code EVENTUALCLUSTER=0})
 * @param nullability       the analyzer computes the facts the nullability pass reads (argument links at every call
 *                          site), so that {@link AnalysisEngine#nullabilityFacts} can run after it; null: off
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
                                  Boolean eventualCluster,
                                  Boolean nullability) {

    /** Both feature switches at the analyzer's process-wide default. */
    public ModificationOptions(Integer maxIterations, Boolean stopWhenCycleDetectedAndNoImprovements,
                               Boolean trackObjectCreations, Boolean modificationViaReachability,
                               Boolean faultTolerant, Boolean warnNearMisses, boolean environmentGates,
                               boolean storeFingerprints) {
        this(maxIterations, stopWhenCycleDetectedAndNoImprovements, trackObjectCreations,
                modificationViaReachability, faultTolerant, warnNearMisses, environmentGates, storeFingerprints,
                null, null, null);
    }

    /** Without the nullability pass's facts. */
    public ModificationOptions(Integer maxIterations, Boolean stopWhenCycleDetectedAndNoImprovements,
                               Boolean trackObjectCreations, Boolean modificationViaReachability,
                               Boolean faultTolerant, Boolean warnNearMisses, boolean environmentGates,
                               boolean storeFingerprints, Boolean staticSideEffects, Boolean eventualCluster) {
        this(maxIterations, stopWhenCycleDetectedAndNoImprovements, trackObjectCreations,
                modificationViaReachability, faultTolerant, warnNearMisses, environmentGates, storeFingerprints,
                staticSideEffects, eventualCluster, null);
    }

    public ModificationOptions withNullability(boolean nullability) {
        return new ModificationOptions(maxIterations, stopWhenCycleDetectedAndNoImprovements, trackObjectCreations,
                modificationViaReachability, faultTolerant, warnNearMisses, environmentGates, storeFingerprints,
                staticSideEffects, eventualCluster, nullability);
    }
}
