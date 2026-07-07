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

package org.e2immu.analyzer.modification.analyzer.impl;

import org.e2immu.analyzer.modification.analyzer.CycleBreakingStrategy;
import org.e2immu.analyzer.modification.analyzer.IteratingAnalyzer;
import org.e2immu.analyzer.modification.analyzer.SingleIterationAnalyzer;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class IteratingAnalyzerImpl extends CommonAnalyzerImpl implements IteratingAnalyzer {
    private static final Logger LOGGER = LoggerFactory.getLogger(IteratingAnalyzerImpl.class);

    private final JavaInspector javaInspector;

    public IteratingAnalyzerImpl(JavaInspector javaInspector, Configuration configuration) {
        super(configuration, null);
        this.javaInspector = javaInspector;
    }

    public record ConfigurationImpl(int maxIterations,
                                    boolean stopWhenCycleDetectedAndNoImprovements,
                                    CycleBreakingStrategy cycleBreakingStrategy,
                                    boolean trackObjectCreations) implements Configuration {
    }

    public static class ConfigurationBuilder {
        private int maxIterations = 1;
        private boolean stopWhenCycleDetectedAndNoImprovements;
        private boolean trackObjectCreations;
        private CycleBreakingStrategy cycleBreakingStrategy = CycleBreakingStrategy.NONE;

        public ConfigurationBuilder setCycleBreakingStrategy(CycleBreakingStrategy cycleBreakingStrategy) {
            this.cycleBreakingStrategy = cycleBreakingStrategy;
            return this;
        }

        public ConfigurationBuilder setStopWhenCycleDetectedAndNoImprovements(boolean stopWhenCycleDetectedAndNoImprovements) {
            this.stopWhenCycleDetectedAndNoImprovements = stopWhenCycleDetectedAndNoImprovements;
            return this;
        }

        public ConfigurationBuilder setMaxIterations(int maxIterations) {
            this.maxIterations = maxIterations;
            return this;
        }

        public ConfigurationBuilder setTrackObjectCreations(boolean trackObjectCreations) {
            this.trackObjectCreations = trackObjectCreations;
            return this;
        }

        public Configuration build() {
            return new ConfigurationImpl(maxIterations, stopWhenCycleDetectedAndNoImprovements, cycleBreakingStrategy,
                    trackObjectCreations);
        }
    }

    @Override
    public void analyze(List<Info> analysisOrder) {
        int iterations = 0;
        SingleIterationAnalyzer singleIterationAnalyzer = new SingleIterationAnalyzerImpl(javaInspector, configuration);
        boolean cycleBreakingActive = false;
        while (true) {
            ++iterations;
            LOGGER.info("{}, cycle breaking active? {}", highlight("Start iteration " + iterations),
                    cycleBreakingActive);
            Instant start = Instant.now();
            singleIterationAnalyzer.go(analysisOrder, cycleBreakingActive, iterations == 1);
            Instant end = Instant.now();
            Duration duration = Duration.between(start, end);
            LOGGER.info("Duration of single iteration: {} min {} sec {} ms", duration.toMinutesPart(),
                    duration.toSecondsPart(), duration.toMillisPart());
            int propertiesChanged = singleIterationAnalyzer.propertiesChanged();
            boolean done = propertiesChanged == 0;
            if (iterations == configuration.maxIterations() || done) {
                LOGGER.info("Stop iterating after {} iterations, done? {}", iterations, done);
                return;
            }
            LOGGER.info("Run again, properties changed {}", propertiesChanged);
            // TODO any strategy, e.g. after 3 iterations, activate cycle breaking
        }
    }
}