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

package org.e2immu.analyzer.aapi.parser;

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
