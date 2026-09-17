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

package io.codelaser.maddi.modification.analyzer.integration;

import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;
import io.codelaser.maddi.modification.analyzer.AnalysisBudgetExceededException;
import io.codelaser.maddi.modification.analyzer.AnalysisValueFeed;
import io.codelaser.maddi.modification.analyzer.CommonTest;
import io.codelaser.maddi.modification.analyzer.impl.IteratingAnalyzerImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Gap {@code G43} (OpenSearch campaign, 2026-08-26): {@code MODIFICATION_ANALYSIS} ran for 1 hour 40 minutes at
 * 100% CPU without converging, and was killed by PID. The iteration cap (30) never fired because the run never
 * finished its FIRST pass. A time budget has to be checked inside a pass, not only between passes.
 * <p>
 * The clock is made to run out by a feed that sleeps, so the fixture needs no slow code. The per-element tick
 * ({@link AnalysisValueFeed#elementCompleted()}) makes one pass slow; the pass boundary
 * ({@link AnalysisValueFeed#passCompleted}) makes the gap between two passes slow.
 * <p>
 * ⚠ The counts in the refusal are asserted against the feed's own tally, not just for being plausible: a refusal
 * that names the wrong count is the G43 log line ("Done 47372 methods") all over again.
 */
public class TestAnalysisTimeBudget extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class X {
                private int i;
                int get() { return i; }
                void set(int i) { this.i = i; }
                static int twice(int j) { return j + j; }
            }
            """;

    private IteratingAnalyzerImpl budgeted(Duration budget) {
        return new IteratingAnalyzerImpl(javaInspector,
                // fault tolerant, as in production: a spent budget must not be swallowed as an element's crash
                new IteratingAnalyzerImpl.ConfigurationBuilder().setMaxIterations(10).setFaultTolerant(true)
                        .setMaxDuration(budget).build());
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private static String nonModifying(TypeInfo typeInfo, String methodName, int params) {
        var v = typeInfo.findUniqueMethod(methodName, params).analysis()
                .getOrNull(PropertyImpl.NON_MODIFYING_METHOD, ValueImpl.BoolImpl.class);
        return v == null ? "null" : v.isTrue() ? "true" : "false";
    }

    @DisplayName("G43: a pass that outruns the budget refuses mid-pass, naming the elements it reached")
    @Test
    public void aPassThatOutrunsItsBudgetRefusesMidPass() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT);
        List<Info> analysisOrder = prepWork(X);
        assertTrue(analysisOrder.size() >= 4, "the fixture needs a pass of several elements: " + analysisOrder);

        AtomicInteger ticks = new AtomicInteger();
        IteratingAnalyzerImpl iterating = budgeted(Duration.ofMillis(1500));
        iterating.setValueFeed(new AnalysisValueFeed() {
            @Override
            public void elementCompleted() {
                ticks.incrementAndGet();
                sleep(1000); // each element costs a second: the budget runs out inside pass 1
            }

            @Override
            public void passCompleted(int iteration, boolean fullPass, Collection<Info> analyzed) {
            }

            @Override
            public void phase(Phase phase, int iteration) {
            }
        });

        AnalysisBudgetExceededException e = assertThrows(AnalysisBudgetExceededException.class,
                () -> iterating.analyze(analysisOrder));

        assertEquals(1, e.iteration(), "the budget ran out in the first pass: " + e.getMessage());
        assertEquals(analysisOrder.size(), e.elementsInPass(), e.getMessage());
        assertEquals(analysisOrder.size(), e.analysisOrderSize(), e.getMessage());
        assertEquals(ticks.get(), e.elementsDone(), "the refusal must name the elements actually processed");
        assertTrue(e.elementsDone() >= 1 && e.elementsDone() < e.elementsInPass(),
                "the refusal must come from INSIDE the pass, not after it: " + e.getMessage());
        assertEquals(Duration.ofMillis(1500), e.budget());
        assertTrue(e.elapsed().compareTo(e.budget()) > 0, e.getMessage());
        assertTrue(e.getMessage().contains("time budget")
                   && e.getMessage().contains("iteration 1")
                   && e.getMessage().contains(e.elementsDone() + " of " + analysisOrder.size()),
                "the message must name the budget, the iteration and the counts: " + e.getMessage());
    }

    @DisplayName("G43: a budget spent between two passes refuses before the next pass starts")
    @Test
    public void aBudgetSpentBetweenPassesRefusesBeforeTheNextOne() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT);
        List<Info> analysisOrder = prepWork(X);

        AtomicInteger passes = new AtomicInteger();
        IteratingAnalyzerImpl iterating = budgeted(Duration.ofSeconds(5));
        iterating.setValueFeed(new AnalysisValueFeed() {
            @Override
            public void passCompleted(int iteration, boolean fullPass, Collection<Info> analyzed) {
                passes.incrementAndGet();
                if (iteration == 1) sleep(6000); // pass 1 fits, the budget is gone before pass 2
            }

            @Override
            public void phase(Phase phase, int iteration) {
            }
        });

        AnalysisBudgetExceededException e = assertThrows(AnalysisBudgetExceededException.class,
                () -> iterating.analyze(analysisOrder));

        assertEquals(1, passes.get(), "exactly one pass completed: " + e.getMessage());
        assertEquals(2, e.iteration(), e.getMessage());
        assertEquals(0, e.elementsDone(), "no element of pass 2 may start: " + e.getMessage());
        assertTrue(e.getMessage().contains("before iteration 2"), e.getMessage());
        assertTrue(iterating.messages().isEmpty(), "a spent budget is not an element's crash: " + iterating.messages());
    }

    @DisplayName("G43 control: a budget larger than the run changes nothing")
    @Test
    public void aBudgetLargerThanTheRunChangesNothing() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT);
        List<Info> analysisOrder = prepWork(X);

        IteratingAnalyzerImpl iterating = budgeted(Duration.ofHours(1));
        iterating.analyze(analysisOrder);

        assertTrue(iterating.certifiedWithoutFrozenValues(), "the budgeted run still certifies");
        assertEquals("true", nonModifying(X, "get", 0));
        assertEquals("false", nonModifying(X, "set", 1));
    }

    @DisplayName("G43 control: the default configuration has no budget")
    @Test
    public void theDefaultConfigurationHasNoBudget() {
        assertNull(new IteratingAnalyzerImpl.ConfigurationBuilder().build().maxDuration(),
                "a library default must not start refusing callers that never asked for a budget");
    }
}
