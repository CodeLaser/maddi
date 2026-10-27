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

public class AnalysisHintsConfigurationImpl implements AnalysisHintsConfiguration {

    // use case 1
    private final List<String> preloadAnalysisResultsDirs;
    // use case 2
    private final String analysisResultsTargetDir;
    // use case 3
    private final List<String> hintsPackages;
    private final String updatedHintsDir;
    private final String updatedHintsPackage;

    private AnalysisHintsConfigurationImpl(List<String> preloadAnalysisResultsDirs,
                                          String analysisResultsTargetDir,
                                          List<String> hintsPackages,
                                          String updatedHintsDir,
                                          String updatedHintsPackage) {
        this.preloadAnalysisResultsDirs = preloadAnalysisResultsDirs;
        this.analysisResultsTargetDir = analysisResultsTargetDir;
        this.updatedHintsDir = updatedHintsDir;
        this.updatedHintsPackage = updatedHintsPackage;
        this.hintsPackages = hintsPackages;
    }

    @Override
    public List<String> preloadAnalysisResultsDirs() {
        return preloadAnalysisResultsDirs;
    }

    @Override
    public String analysisResultsTargetDir() {
        return analysisResultsTargetDir;
    }

    @Override
    public String updatedHintsDir() {
        return updatedHintsDir;
    }

    @Override
    public String updatedHintsPackage() {
        return updatedHintsPackage;
    }

    @Override
    public List<String> hintsPackages() {
        return hintsPackages;
    }

    @Override
    public String toString() {
        return "AnalysisHintsConfigurationImpl{" +
               "preloadAnalysisResultsDirs=" + preloadAnalysisResultsDirs +
               ", analysisResultsTargetDir='" + analysisResultsTargetDir + '\'' +
               ", hintsPackages=" + hintsPackages +
               ", updatedHintsDir='" + updatedHintsDir + '\'' +
               ", updatedHintsPackage='" + updatedHintsPackage + '\'' +
               '}';
    }

    public static class Builder {
        // use case 1
        private final List<String> preloadAnalysisResultsDirs = new ArrayList<>();
        // use case 2
        private String analysisResultsTargetDir;
        // use case 3
        private String updatedHintsDir;
        private final List<String> hintsPackages = new ArrayList<>();
        private String updatedHintsPackage;

        public Builder setAnalysisResultsTargetDir(String analysisResultsTargetDir) {
            this.analysisResultsTargetDir = analysisResultsTargetDir;
            return this;
        }

        public Builder addPreloadAnalysisResultsDirs(String... preloadAnalysisResultsDirs) {
            this.preloadAnalysisResultsDirs.addAll(Arrays.asList(preloadAnalysisResultsDirs));
            return this;
        }

        public Builder addHintsPackages(String... hintsPackages) {
            this.hintsPackages.addAll(Arrays.asList(hintsPackages));
            return this;
        }

        public Builder setUpdatedHintsDir(String updatedHintsDir) {
            this.updatedHintsDir = updatedHintsDir;
            return this;
        }

        public Builder setUpdatedHintsPackage(String updatedHintsPackage) {
            this.updatedHintsPackage = updatedHintsPackage;
            return this;
        }

        public AnalysisHintsConfiguration build() {
            return new AnalysisHintsConfigurationImpl(List.copyOf(preloadAnalysisResultsDirs),
                    analysisResultsTargetDir,
                    List.copyOf(hintsPackages), updatedHintsDir, updatedHintsPackage);
        }
    }
}
