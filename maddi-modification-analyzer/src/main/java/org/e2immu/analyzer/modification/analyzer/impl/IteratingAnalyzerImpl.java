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

import org.e2immu.analyzer.modification.common.util.TolerantWrite;
import org.e2immu.analyzer.modification.analyzer.CycleBreakingStrategy;
import org.e2immu.analyzer.modification.analyzer.IteratingAnalyzer;
import org.e2immu.analyzer.modification.analyzer.SingleIterationAnalyzer;
import org.e2immu.language.cst.api.analysis.Message;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class IteratingAnalyzerImpl extends CommonAnalyzerImpl implements IteratingAnalyzer {
    private static final Logger LOGGER = LoggerFactory.getLogger(IteratingAnalyzerImpl.class);

    private final JavaInspector javaInspector;
    private SingleIterationAnalyzer lastRun;
    private final List<Message> guardMessages = new ArrayList<>();

    public IteratingAnalyzerImpl(JavaInspector javaInspector, Configuration configuration) {
        super(configuration, null, null);
        this.javaInspector = javaInspector;
    }

    public record ConfigurationImpl(int maxIterations,
                                    boolean stopWhenCycleDetectedAndNoImprovements,
                                    CycleBreakingStrategy cycleBreakingStrategy,
                                    boolean trackObjectCreations,
                                    boolean guardContracts,
                                    boolean faultTolerant) implements Configuration {
    }

    public static class ConfigurationBuilder {
        private int maxIterations = 1;
        private boolean stopWhenCycleDetectedAndNoImprovements;
        private boolean trackObjectCreations;
        private boolean guardContracts = true;
        private boolean faultTolerant;
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

        public ConfigurationBuilder setGuardContracts(boolean guardContracts) {
            this.guardContracts = guardContracts;
            return this;
        }

        public ConfigurationBuilder setFaultTolerant(boolean faultTolerant) {
            this.faultTolerant = faultTolerant;
            return this;
        }

        public Configuration build() {
            return new ConfigurationImpl(maxIterations, stopWhenCycleDetectedAndNoImprovements, cycleBreakingStrategy,
                    trackObjectCreations, guardContracts, faultTolerant);
        }
    }

    @Override
    public List<Message> messages() {
        return Stream.concat(lastRun == null ? Stream.of() : lastRun.messages().stream(),
                guardMessages.stream()).toList();
    }

    @Override
    public void analyze(List<Info> analysisOrder) {
        int iterations = 0;
        SingleIterationAnalyzer singleIterationAnalyzer = new SingleIterationAnalyzerImpl(javaInspector, configuration);
        this.lastRun = singleIterationAnalyzer;
        boolean cycleBreakingActive = false;
        int previousPropertiesChanged = Integer.MAX_VALUE;
        while (true) {
            ++iterations;
            LOGGER.info("{}, cycle breaking active? {}", highlight("Start iteration " + iterations),
                    cycleBreakingActive);
            TolerantWrite.resetChangeCounts();
            Instant start = Instant.now();
            singleIterationAnalyzer.go(analysisOrder, cycleBreakingActive, iterations == 1);
            Instant end = Instant.now();
            Duration duration = Duration.between(start, end);
            LOGGER.info("Duration of single iteration: {} min {} sec {} ms", duration.toMinutesPart(),
                    duration.toSecondsPart(), duration.toMillisPart());
            int propertiesChanged = singleIterationAnalyzer.propertiesChanged();
            // convergence diagnosis: which properties are still moving? (value-changing writes per property)
            String topChanges = TolerantWrite.changeCounts().entrySet().stream()
                    .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                    .limit(10)
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .reduce((a, b) -> a + ", " + b).orElse("-");
            LOGGER.info("Iteration {} property changes (top 10): {}", iterations, topChanges);
            boolean done = propertiesChanged == 0;
            // plateau: the change count no longer meaningfully decreases (an oscillation floor); further full
            // re-analyses only pay for the same flips again. Opt-in via stopWhenCycleDetectedAndNoImprovements.
            boolean plateau = configuration.stopWhenCycleDetectedAndNoImprovements()
                              && iterations >= 3 && propertiesChanged >= 0.95 * previousPropertiesChanged;
            if (iterations == configuration.maxIterations() || done || plateau) {
                LOGGER.info("Stop iterating after {} iterations, done? {}{}", iterations, done,
                        plateau ? " (plateau: " + propertiesChanged + " vs " + previousPropertiesChanged + ")" : "");
                if (configuration.guardContracts()) {
                    // values are final now: verify user-written contracts, emit explanatory findings
                    new GuardAnalyzerImpl(javaInspector.runtime(), configuration, guardMessages).go(analysisOrder);
                }
                return;
            }
            previousPropertiesChanged = propertiesChanged;
            LOGGER.info("Run again, properties changed {}", propertiesChanged);
            // TODO any strategy, e.g. after 3 iterations, activate cycle breaking
        }
    }
}