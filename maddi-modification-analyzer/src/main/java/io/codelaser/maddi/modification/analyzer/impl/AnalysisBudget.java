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

package io.codelaser.maddi.modification.analyzer.impl;

import io.codelaser.maddi.modification.analyzer.AnalysisBudgetExceededException;

import java.time.Duration;

/**
 * The wall-clock deadline of one {@code IteratingAnalyzer.analyze} call (gap {@code G43}). The coordinator names
 * each pass before it starts; the workers check it per element, so {@link #check} must stay cheap and thread-safe.
 */
final class AnalysisBudget {
    private final Duration budget;
    private final long budgetNanos;
    private final long startNanos;
    private final int analysisOrderSize;
    private volatile int iteration;
    private volatile int elementsInPass;

    private AnalysisBudget(Duration budget, int analysisOrderSize) {
        this.budget = budget;
        this.budgetNanos = budget.toNanos();
        this.startNanos = System.nanoTime();
        this.analysisOrderSize = analysisOrderSize;
    }

    /** @return the running budget, or {@code null} when {@code budget} is null (unlimited) */
    static AnalysisBudget start(Duration budget, int analysisOrderSize) {
        return budget == null ? null : new AnalysisBudget(budget, analysisOrderSize);
    }

    /** Name the pass about to run, for the refusal's message. Its first element's check refuses a spent budget. */
    void startPass(int iteration, int elementsInPass) {
        this.iteration = iteration;
        this.elementsInPass = elementsInPass;
    }

    /** @throws AnalysisBudgetExceededException when the budget is spent; {@code elementsDone} is of the current pass */
    void check(int elementsDone) {
        long elapsed = System.nanoTime() - startNanos;
        if (elapsed > budgetNanos) {
            throw new AnalysisBudgetExceededException(budget, Duration.ofNanos(elapsed), iteration, elementsDone,
                    elementsInPass, analysisOrderSize);
        }
    }
}
