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

package io.codelaser.maddi.run.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Which analysis results an analysis run preloads: the rule behind {@code --preload-analysis-results-dirs} of the
 * command lines and {@code preloadAnalysisResultsDirs} of the Gradle and Maven plugins.
 * <p>
 * ⛔ LOADED BY DEFAULT for an analysis run (CodeLaser/maddi-mod#15). Without results every library type is an
 * unknown, and the run still converges, certifies and reports thousands of verdicts -- detekt read
 * {@code @FinalFields}=1320 with not one immutable type, and {@code @Immutable}=676 once the JDK results were loaded
 * (docs/status/kotlin-corpora.md §3). For Kotlin it is worse: every uncontracted stdlib call modifies its arguments.
 * Nothing about such a run looks wrong, so the results must not depend on a setting nobody knows to state. Not when
 * hints are being compiled or written: those runs produce the results, and what they preload changes what they write.
 * <ul>
 * <li>nothing stated: the shipped results, unless the run produces hints;</li>
 * <li>{@value #NONE}, alone: nothing, not even the shipped results;</li>
 * <li>a list: its entries, where {@value #DEFAULT} stands for the shipped results ({@code default,my/results}).</li>
 * </ul>
 * The command lines applied this since 2026-10-10; the plugins, which reach {@code maddi-run-main} through a
 * key/value map, since 2026-10-11.
 */
public final class ShippedAnalysisResults {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShippedAnalysisResults.class);

    /** The value that loads nothing, not even the shipped results. */
    public static final String NONE = "none";

    /** In a list, the shipped results: {@code default,my/results} adds to them. */
    public static final String DEFAULT = "default";

    /**
     * The analysis results maddi ships (maddi-aapi-archive, on the run-time class path of the command lines and in
     * the plugin jars): the JDK first, as the libraries were compiled against it, then libs.jar (the test, log,
     * AssertJ, hamcrest, JUnit-params, Mockito, commons-io and Jackson 2 libraries, the Kotlin stdlib, and Guava's collections).
     */
    public static final List<String> RESOURCES = List.of(
            "resource:/io/codelaser/maddi/aapi/archive/analyzedPackageFiles/openjdk.jar",
            "resource:/io/codelaser/maddi/aapi/archive/analyzedPackageFiles/libs.jar");

    private ShippedAnalysisResults() {
    }

    /** The shipped results {@code anchor}'s class loader can see, warning when some are missing. */
    public static List<String> available(Class<?> anchor) {
        List<String> shipped = RESOURCES.stream()
                .filter(r -> anchor.getResource(r.substring("resource:".length())) != null).toList();
        if (shipped.size() < RESOURCES.size()) {
            LOGGER.warn("The shipped analysis results are not on the class path; library types will be unknowns"
                        + " (found {} of {})", shipped, RESOURCES);
        }
        return shipped;
    }

    /**
     * The directories and resources to preload.
     *
     * @param stated        the stated entries, already split at the separators; null or empty when nothing was stated
     * @param producesHints the run compiles or writes analysis hints
     * @param anchor        a class whose loader sees the run-time class path
     */
    public static List<String> resolve(List<String> stated, boolean producesHints, Class<?> anchor) {
        if (stated == null || stated.isEmpty()) {
            return producesHints ? List.of() : available(anchor);
        }
        if (stated.size() == 1 && NONE.equals(stated.getFirst().trim())) return List.of();
        List<String> result = new ArrayList<>();
        for (String entry : stated) {
            if (DEFAULT.equals(entry.trim())) result.addAll(available(anchor));
            else result.add(entry);
        }
        return List.copyOf(result);
    }
}
