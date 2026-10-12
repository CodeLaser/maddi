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
     * AssertJ, hamcrest, JUnit-params and Mockito libraries, the Kotlin stdlib, and Guava's collections).
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
