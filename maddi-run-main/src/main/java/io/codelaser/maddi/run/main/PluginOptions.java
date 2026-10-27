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

package io.codelaser.maddi.run.main;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The option surface both build plugins expose, reduced to the two things they actually do with it: split a
 * delimited string, and turn the values into the key/value maps {@link Main} parses.
 * <p>
 * ⛔ Both plugins had their own copy of all of this, and the copies had drifted in ways nothing could catch:
 * one guarded {@code != null} and the other {@code != null && !isBlank()}, so the same unset option reached
 * {@link Main} as an absent key from Maven and as a <b>null value</b> from Gradle.
 */
public class PluginOptions {

    /**
     * ⚠ ONE SEPARATOR CLASS, DELIBERATELY. The two plugins wrote {@code [;,]} for {@code excludeFromClasspath}
     * and {@code [,;]} for the package options -- the same set, spelled two ways, which is the fingerprint of a
     * copy rather than of a decision. Both accept comma and semicolon; there was never a difference to preserve.
     */
    private static final String SEPARATORS = "[,;]\\s*";

    /** Empty when the option says nothing; blanks between separators are dropped rather than kept as "". */
    public static Set<String> splitToSet(String option) {
        if (option == null || option.isBlank()) return Set.of();
        return Arrays.stream(option.split(SEPARATORS))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * As {@link #splitToSet}, but {@code null} when the option says nothing.
     * <p>
     * ⚠ The difference is not stylistic: {@code SourceSet.restrictToPackages()} reads {@code null} as "no
     * restriction" and an EMPTY set as "restrict to nothing", so the two cannot be collapsed.
     */
    public static Set<String> splitToSetOrNull(String option) {
        Set<String> set = splitToSet(option);
        return set.isEmpty() ? null : set;
    }

    /**
     * The {@code GeneralConfiguration} key/value map.
     *
     * @param analysisResultsDir where results go; when blank, {@code <buildDirectory>/e2immu}, which is the
     *                           default both plugins computed separately and identically.
     */
    public static Map<String, String> generalConfigMap(boolean incrementalAnalysis,
                                                       String analysisResultsDir,
                                                       File defaultAnalysisResultsDir,
                                                       boolean parallel,
                                                       String analysisSteps,
                                                       String debugTargets,
                                                       boolean quiet,
                                                       boolean warnNearMisses,
                                                       boolean skipKotlinSources) {
        Map<String, String> map = new HashMap<>();
        map.put(Main.INCREMENTAL_ANALYSIS, "" + incrementalAnalysis);
        map.put(Main.ANALYSIS_RESULTS_DIR, analysisResultsDir == null || analysisResultsDir.isBlank()
                ? defaultAnalysisResultsDir.getAbsolutePath() : analysisResultsDir);
        map.put(Main.PARALLEL, "" + parallel);
        map.put(Main.QUIET, "" + quiet);
        map.put(Main.WARN_NEAR_MISSES, "" + warnNearMisses);
        map.put(Main.SKIP_KOTLIN_SOURCES, "" + skipKotlinSources);
        // ⛔ OMITTED WHEN UNSET, never present-and-null. The Gradle plugin put both of these in unconditionally,
        // so an unconfigured extension handed Main a map with null values in it.
        putIfStated(map, Main.ANALYSIS_STEPS, analysisSteps);
        putIfStated(map, Main.DEBUG, debugTargets);
        return map;
    }

    /** The {@code AnalysisHintsConfiguration} key/value map: its three use cases, all optional. */
    public static Map<String, String> analysisHintsMap(String preloadAnalysisResultsDirs,
                                                       String analysisResultsTargetDir,
                                                       String updatedHintsDir,
                                                       String updatedHintsPackage,
                                                       String hintsPackages) {
        Map<String, String> map = new HashMap<>();
        putIfStated(map, Main.PRELOAD_ANALYSIS_RESULTS_DIRS, preloadAnalysisResultsDirs);
        putIfStated(map, Main.ANALYSIS_RESULTS_TARGET_DIR, analysisResultsTargetDir);
        putIfStated(map, Main.UPDATED_HINTS_DIR, updatedHintsDir);
        putIfStated(map, Main.UPDATED_HINTS_PACKAGE, updatedHintsPackage);
        putIfStated(map, Main.HINTS_PACKAGES, hintsPackages);
        return map;
    }

    private static void putIfStated(Map<String, String> map, String key, String value) {
        if (value != null && !value.isBlank()) map.put(key, value);
    }
}
