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

package org.e2immu.analyzer.modification.analyzer.integration;

import org.e2immu.analyzer.modification.analyzer.AnalysisValueFeed;
import org.e2immu.analyzer.modification.analyzer.CommonTest;
import org.e2immu.analyzer.modification.analyzer.IteratingAnalyzer;
import org.e2immu.analyzer.modification.analyzer.impl.IteratingAnalyzerImpl;
import org.e2immu.analyzer.modification.prepwork.callgraph.ComputeCallGraph;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.util.internal.graph.G;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The incremental early-cutoff entry point {@link IteratingAnalyzer#analyze(List, G, Set)}: when seeded with a
 * dirty subset, iteration 1 analyses only that subset (not the whole order), the worklist propagates from it, and
 * the run stops the moment the worklist is dry — <em>without</em> the full verification / cycle-breaking pass that
 * a normal run appends. Elements the worklist never reaches keep their (carried) values untouched. Here we observe
 * the analysed set directly through {@link AnalysisValueFeed}. See docs/analysis-rewiring.md.
 */
public class TestSeededIncrementalAnalysis extends CommonTest {

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

    record Recording(List<Integer> passSizes, List<Boolean> fullPasses, List<AnalysisValueFeed.Phase> phases,
                     List<Info> lastAnalyzed) {
        Recording() {
            this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }

        AnalysisValueFeed feed() {
            return new AnalysisValueFeed() {
                @Override
                public void passCompleted(int iteration, boolean fullPass, Collection<Info> analyzed) {
                    passSizes.add(analyzed.size());
                    fullPasses.add(fullPass);
                    lastAnalyzed.clear();
                    lastAnalyzed.addAll(analyzed);
                }

                @Override
                public void phase(Phase phase, int iteration) {
                    phases.add(phase);
                }
            };
        }
    }

    private IteratingAnalyzer newAnalyzer(AnalysisValueFeed feed) {
        IteratingAnalyzer a = new IteratingAnalyzerImpl(javaInspector,
                new IteratingAnalyzerImpl.ConfigurationBuilder().setMaxIterations(10).build());
        a.setValueFeed(feed);
        return a;
    }

    @DisplayName("a seeded worklist analyses only the seed (+propagation) and stops without a verification pass")
    @Test
    public void test() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT);
        List<Info> analysisOrder = prepWork(X);
        G<Info> graph = new ComputeCallGraph(runtime, X).go().graph();

        // (1) a normal full run: iteration 1 is a full pass over the whole order, and it certifies.
        Recording full = new Recording();
        newAnalyzer(full.feed()).analyze(analysisOrder, graph);
        assertTrue(full.fullPasses().getFirst(), "the first pass of a normal run analyses everything");
        assertEquals(analysisOrder.size(), full.passSizes().getFirst());
        assertTrue(full.phases().contains(AnalysisValueFeed.Phase.TERMINAL_CERTIFIED));

        // the analysis is now at its fixpoint; the Info objects hold their (final) values. This is the state an
        // incremental reload starts from: most types carried, a dirty subset to re-check.

        // (2) seed with a single element. Iteration 1 analyses ONLY that element; being already at the fixpoint it
        // changes nothing, so the worklist stays dry and the run stops — no full verification pass appended.
        Info seed = analysisOrder.getFirst();
        Recording seeded = new Recording();
        newAnalyzer(seeded.feed()).analyze(analysisOrder, graph, Set.of(seed));

        assertEquals(1, seeded.passSizes().size(), "one pass only: seed re-checked, worklist dry, stop");
        assertFalse(seeded.fullPasses().getFirst(), "the seeded pass is a subset pass, never a full pass");
        assertEquals(1, seeded.passSizes().getFirst(), "exactly the seed was analysed");
        assertEquals(List.of(seed), seeded.lastAnalyzed());
        // crucially, NO full verification pass ever ran: no pass covered the whole order.
        assertTrue(seeded.passSizes().stream().noneMatch(s -> s == analysisOrder.size()),
                "no pass re-analysed the whole order — the carried elements were spared");
        assertTrue(analysisOrder.size() > 1, "sanity: the order has more than the seed, so sparing is meaningful");
    }
}
