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

import org.e2immu.analyzer.modification.analyzer.CommonTest;
import org.e2immu.analyzer.modification.analyzer.IteratingAnalyzer;
import org.e2immu.analyzer.modification.analyzer.impl.IteratingAnalyzerImpl;
import org.e2immu.analyzer.modification.prepwork.callgraph.ComputeCallGraph;
import org.e2immu.language.cst.api.analysis.Property;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.util.internal.graph.G;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The clear-before-recompute hook of {@link IteratingAnalyzer#analyze(List, G, Set, Consumer)}: in incremental mode
 * the worklist pulls a carried type into the dirty frontier only when a dependency's summary changes, and the hook
 * fires once for each such element — before its first re-analysis — so the driver can clear its stale
 * cross-type-derived values (else the monotonic guard would reject a lowered value). The seed itself is fresh
 * (INVALID) source, never carried, so the hook never fires for it. See docs/analysis-rewiring.md.
 */
public class TestClearBeforeRecomputeHook extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class X {
                private int i;
                int get() { return i; }
                void set(int j) { this.i = j; }
                int getTwice() { return get() + get(); }
            }
            """;

    private static final Predicate<Property> CROSS_TYPE_DERIVED =
            p -> p.analysisTier() == Property.AnalysisTier.CROSS_TYPE_DERIVED;

    private IteratingAnalyzer newAnalyzer() {
        return new IteratingAnalyzerImpl(javaInspector,
                new IteratingAnalyzerImpl.ConfigurationBuilder().setMaxIterations(10).build());
    }

    @DisplayName("the hook fires for propagated (carried) elements, once each, never for the seed")
    @Test
    public void testHookFiresForPropagatedNotSeed() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT);
        List<Info> analysisOrder = prepWork(X);
        G<Info> graph = new ComputeCallGraph(runtime, X).go().graph();
        newAnalyzer().analyze(analysisOrder, graph); // to fixpoint

        // simulate every element being carried-but-stale: drop the cross-type-derived tier everywhere, so re-analysis
        // genuinely recomputes and propagates (an intrinsic-only reload state). Seed a single element.
        analysisOrder.forEach(i -> i.analysis().removeIf(CROSS_TYPE_DERIVED));
        Info seed = analysisOrder.getFirst();

        List<Info> fired = new ArrayList<>();
        newAnalyzer().analyze(analysisOrder, graph, Set.of(seed), fired::add);

        assertFalse(fired.isEmpty(), "propagation past the seed occurred, so the hook fired");
        assertFalse(fired.contains(seed), "the seed is fresh, never carried: the hook must not fire for it");
        assertEquals(fired.size(), Set.copyOf(fired).size(), "the hook fires at most once per element");
        assertTrue(analysisOrder.containsAll(fired), "every fired element is an analysis-order element");
    }

    @DisplayName("nothing propagates from a converged seed: the hook stays silent")
    @Test
    public void testHookSilentWhenNothingPropagates() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT);
        List<Info> analysisOrder = prepWork(X);
        G<Info> graph = new ComputeCallGraph(runtime, X).go().graph();
        newAnalyzer().analyze(analysisOrder, graph); // to fixpoint; nothing is stale

        List<Info> fired = new ArrayList<>();
        newAnalyzer().analyze(analysisOrder, graph, Set.of(analysisOrder.getFirst()), fired::add);

        assertTrue(fired.isEmpty(), "a converged seed changes no summary, so nothing is pulled in and the hook is silent");
    }
}
