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

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Finds the {@link AnalysisEngine} on the class path or module path.
 * <p>
 * ⛔ <b>A missing engine is an error, never a no-op.</b> The CLI already exits 0 for
 * {@code --analysis-steps=none} whether or not the sources parsed; an analysis that silently did nothing because
 * a jar was absent would be the same defect one tier up, and would read as "nothing to report". So
 * {@link #require} throws, naming the jar and the reason it was needed.
 */
public final class AnalysisEngines {
    /** The artifact that provides the engine, named in every error. */
    public static final String PROVIDER_ARTIFACT = "io.codelaser:maddi-run-analysis (repository maddi-mod)";

    private AnalysisEngines() {
    }

    /**
     * @param why what the caller was about to do, e.g. {@code "--analysis-steps=modification"}; goes into the
     *            error message
     * @return the one engine on the class path
     * @throws IllegalStateException when there is none, or more than one
     */
    public static AnalysisEngine require(String why) {
        return require(why, AnalysisEngines.class.getClassLoader());
    }

    public static AnalysisEngine require(String why, ClassLoader classLoader) {
        List<AnalysisEngine> engines = new ArrayList<>();
        ServiceLoader.load(AnalysisEngine.class, classLoader).forEach(engines::add);
        if (engines.isEmpty() && classLoader != AnalysisEngines.class.getClassLoader()) {
            ServiceLoader.load(AnalysisEngine.class).forEach(engines::add);
        }
        if (engines.isEmpty()) {
            throw new IllegalStateException("No modification analysis engine on the class path: " + why
                                            + " needs " + PROVIDER_ARTIFACT + " at run time. A parse-only run "
                                            + "(--analysis-steps=none) does not.");
        }
        if (engines.size() > 1) {
            throw new IllegalStateException("More than one modification analysis engine on the class path ("
                                            + engines.stream().map(AnalysisEngine::name).toList() + "); "
                                            + why + " cannot choose.");
        }
        return engines.getFirst();
    }

    /** The engine if one is present, else null: for callers that can do without, and say so. */
    public static AnalysisEngine find() {
        List<AnalysisEngine> engines = new ArrayList<>();
        ServiceLoader.load(AnalysisEngine.class, AnalysisEngines.class.getClassLoader()).forEach(engines::add);
        return engines.size() == 1 ? engines.getFirst() : null;
    }
}
