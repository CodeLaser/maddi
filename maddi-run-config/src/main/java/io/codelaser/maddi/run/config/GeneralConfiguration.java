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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @param translate       the language to translate the main sources to ({@code kotlin}, {@code csharp}); null: none
 * @param translateOutput the directory the translation is written to
 */
public record GeneralConfiguration(boolean incrementalAnalysis,
                                   String analysisResultsDir,
                                   List<String> analysisSteps,
                                   List<String> debugTargets,
                                   boolean quiet,
                                   boolean parallel,
                                   boolean jdkInternals,
                                   boolean warnNearMisses,
                                   boolean skipKotlinSources,
                                   String translate,
                                   String translateOutput) {

    public static class Builder {
        private boolean incrementalAnalysis;
        private final List<String> analysisSteps = new ArrayList<>();
        private final List<String> debugTargets = new ArrayList<>();
        private boolean quiet;
        private boolean parallel;
        private boolean jdkInternals;
        private boolean warnNearMisses;
        private boolean skipKotlinSources;
        private String analysisResultsDir;
        private String translate;
        private String translateOutput;

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

        public Builder setJdkInternals(boolean jdkInternals) {
            this.jdkInternals = jdkInternals;
            return this;
        }

        public Builder setWarnNearMisses(boolean warnNearMisses) {
            this.warnNearMisses = warnNearMisses;
            return this;
        }

        /**
         * Analyze the Java sources of a project that also holds Kotlin, accepting that the Kotlin is not read.
         * Unset (the default) the run REFUSES rather than reporting success over a partly-read tree; see
         * {@code DetectKotlinSources}.
         */
        public Builder setSkipKotlinSources(boolean skipKotlinSources) {
            this.skipKotlinSources = skipKotlinSources;
            return this;
        }

        public Builder setAnalysisResultsDir(String analysisResultsDir) {
            this.analysisResultsDir = analysisResultsDir;
            return this;
        }

        public Builder setTranslate(String translate) {
            this.translate = translate;
            return this;
        }

        public Builder setTranslateOutput(String translateOutput) {
            this.translateOutput = translateOutput;
            return this;
        }

        public GeneralConfiguration build() {
            return new GeneralConfiguration(incrementalAnalysis, analysisResultsDir, List.copyOf(analysisSteps),
                    List.copyOf(debugTargets), quiet, parallel, jdkInternals, warnNearMisses, skipKotlinSources,
                    translate, translateOutput);
        }
    }
}
