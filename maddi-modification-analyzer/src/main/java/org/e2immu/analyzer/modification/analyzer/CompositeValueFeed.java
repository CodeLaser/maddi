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

package org.e2immu.analyzer.modification.analyzer;

import org.e2immu.language.cst.api.info.Info;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

/**
 * The analyzer takes a single {@link AnalysisValueFeed} ({@code setValueFeed}); a long run wants two at
 * once — the {@link CheckpointWriter} (durability) and the {@link AnalysisProgressFeed} (observability).
 * This fans each event out to every delegate, guarding each call so one misbehaving feed cannot silence
 * the others: a checkpoint IO failure must not stop the progress heartbeat, and vice versa.
 */
public class CompositeValueFeed implements AnalysisValueFeed {
    private static final Logger LOGGER = LoggerFactory.getLogger(CompositeValueFeed.class);

    private final List<AnalysisValueFeed> delegates;

    public CompositeValueFeed(AnalysisValueFeed... delegates) {
        this.delegates = List.of(delegates);
    }

    @Override
    public void passCompleted(int iteration, boolean fullPass, Collection<Info> analyzed) {
        for (AnalysisValueFeed d : delegates) {
            try {
                d.passCompleted(iteration, fullPass, analyzed);
            } catch (RuntimeException | AssertionError e) {
                LOGGER.warn("value feed {} threw on passCompleted; ignoring", d.getClass().getSimpleName(), e);
            }
        }
    }

    @Override
    public void waveCompleted(int iteration, int wave, Collection<Info> analyzed) {
        for (AnalysisValueFeed d : delegates) {
            try {
                d.waveCompleted(iteration, wave, analyzed);
            } catch (RuntimeException | AssertionError e) {
                LOGGER.warn("value feed {} threw on waveCompleted; ignoring", d.getClass().getSimpleName(), e);
            }
        }
    }

    @Override
    public void elementCompleted() {
        // per-element (hot): most delegates leave this a no-op; only the progress feed ticks a counter
        for (AnalysisValueFeed d : delegates) {
            try {
                d.elementCompleted();
            } catch (RuntimeException | AssertionError e) {
                LOGGER.warn("value feed {} threw on elementCompleted; ignoring", d.getClass().getSimpleName(), e);
            }
        }
    }

    @Override
    public void phase(Phase phase, int iteration) {
        for (AnalysisValueFeed d : delegates) {
            try {
                d.phase(phase, iteration);
            } catch (RuntimeException | AssertionError e) {
                LOGGER.warn("value feed {} threw on phase; ignoring", d.getClass().getSimpleName(), e);
            }
        }
    }
}
