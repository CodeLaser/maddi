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

package org.e2immu.analyzer.run.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public record GeneralConfiguration(boolean incrementalAnalysis,
                                   String analysisResultsDir,
                                   List<String> analysisSteps,
                                   List<String> debugTargets,
                                   boolean quiet,
                                   boolean parallel) {

    public static class Builder {
        private boolean incrementalAnalysis;
        private final List<String> analysisSteps = new ArrayList<>();
        private final List<String> debugTargets = new ArrayList<>();
        private boolean quiet;
        private boolean parallel;
        private String analysisResultsDir;

        public Builder setIncrementalAnalysis(boolean incrementalAnalysis) {
            this.incrementalAnalysis = incrementalAnalysis;
            return this;
        }

        public Builder addAnalysisSteps(String... analysisSteps) {
            this.analysisSteps.addAll(Arrays.asList(analysisSteps));
            return this;
        }

        public Builder addDebugTargets(String... debugTargets) {
            this.debugTargets.addAll(Arrays.asList(debugTargets));
            return this;
        }

        public Builder setQuiet(boolean quiet) {
            this.quiet = quiet;
            return this;
        }

        public Builder setParallel(boolean parallel) {
            this.parallel = parallel;
            return this;
        }

        public Builder setAnalysisResultsDir(String analysisResultsDir) {
            this.analysisResultsDir = analysisResultsDir;
            return this;
        }

        public GeneralConfiguration build() {
            return new GeneralConfiguration(incrementalAnalysis, analysisResultsDir, List.copyOf(analysisSteps),
                    List.copyOf(debugTargets), quiet, parallel);
        }
    }
}
