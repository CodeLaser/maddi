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

package org.e2immu.analyzer.modification.analyzer.nolink;

import org.e2immu.analyzer.modification.analyzer.CommonTest;
import org.e2immu.analyzer.modification.analyzer.SingleIterationAnalyzer;
import org.e2immu.analyzer.modification.analyzer.impl.IteratingAnalyzerImpl;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the iterative fixed-point of the analyzer. Each pass of {@link SingleIterationAnalyzer} reports how many
 * properties it changed; the {@link org.e2immu.analyzer.modification.analyzer.IteratingAnalyzer} re-runs single passes
 * until that count reaches zero (a fixed point). An acyclic type reaches its fixed point after a single computing pass;
 * a type/method dependency <em>cycle</em> that a single pass cannot topologically order needs at least one extra
 * refinement pass. These tests drive the single-iteration analyzer directly so they observe the convergence itself,
 * rather than a linked-variable representation.
 */
public class TestIteration extends CommonTest {

    // cross-type modification cycle: Ping.hit -> Pong.receive (modifies), Pong.hit -> Ping.receive (modifies)
    @Language("java")
    private static final String CYCLIC = """
            package a.b;
            class X {
                static class Ping {
                    Pong pong;
                    int count;
                    void receive() { count++; }
                    void hit() { pong.receive(); }
                }
                static class Pong {
                    Ping ping;
                    int count;
                    void receive() { count++; }
                    void hit() { ping.receive(); }
                }
            }
            """;

    @Language("java")
    private static final String ACYCLIC = """
            package a.b;
            class X {
                static class A {
                    int i;
                    int get() { return i; }
                    void set(int v) { this.i = v; }
                }
            }
            """;

    // The dependency chain that genuinely drives iteration: constructor parameter -> field -> accessor method ->
    // type-level immutability/independence, closed into a cross-type cycle. CHAIN2 is a 2-type loop, CHAIN4 a 4-type
    // loop; a deeper loop touches more of these per pass.
    @Language("java")
    private static final String CHAIN2 = """
            package a.b;
            class X {
                static class A { final B b; A(B b){this.b=b;} B getB(){return b;} }
                static class B { final A a; B(A a){this.a=a;} A getA(){return a;} }
            }
            """;

    @Language("java")
    private static final String CHAIN4 = """
            package a.b;
            class X {
                static class A { final B b; A(B b){this.b=b;} B getB(){return b;} }
                static class B { final C c; B(C c){this.c=c;} C getC(){return c;} }
                static class C { final D d; C(D d){this.d=d;} D getD(){return d;} }
                static class D { final A a; D(A a){this.a=a;} A getA(){return a;} }
            }
            """;

    /** Runs single passes until the analysis stops changing; returns the (1-based) pass at which it converged. */
    private int passesUntilFixedPoint(String input, int cap) throws IOException {
        AnalyzerBundle bundle = buildAnalyzerBundle();
        TypeInfo X = bundle.javaInspector().parse("a.b.X", input);
        List<Info> ao = bundle.prepAnalyzer().doPrimaryType(X);
        SingleIterationAnalyzer single = (SingleIterationAnalyzer) bundle.analyzer();
        for (int pass = 1; pass <= cap; pass++) {
            single.go(ao, false, pass == 1);
            if (single.propertiesChanged() == 0) return pass;
        }
        return fail("did not converge within " + cap + " passes");
    }

    /** propertiesChanged per pass, up to and including the first converged (0) pass. */
    private List<Integer> convergenceSequence(String input, int cap) throws IOException {
        AnalyzerBundle bundle = buildAnalyzerBundle();
        TypeInfo X = bundle.javaInspector().parse("a.b.X", input);
        List<Info> ao = bundle.prepAnalyzer().doPrimaryType(X);
        SingleIterationAnalyzer single = (SingleIterationAnalyzer) bundle.analyzer();
        List<Integer> seq = new java.util.ArrayList<>();
        for (int pass = 1; pass <= cap; pass++) {
            single.go(ao, false, pass == 1);
            int changed = single.propertiesChanged();
            seq.add(changed);
            if (changed == 0) return seq;
        }
        return fail("did not converge within " + cap + " passes");
    }

    @DisplayName("a dependency cycle needs more passes to reach the fixed point than an acyclic type")
    @Test
    public void testConvergence() throws IOException {
        int acyclicPasses = passesUntilFixedPoint(ACYCLIC, 20);
        int cyclicPasses = passesUntilFixedPoint(CYCLIC, 20);

        // an acyclic type is fully computed in one pass; the second pass only confirms nothing changed
        assertEquals(2, acyclicPasses, "an acyclic type reaches its fixed point after one computing pass");
        // the cross-type cycle cannot be settled in a single pass: at least one refinement pass follows
        assertTrue(cyclicPasses > acyclicPasses,
                "the cross-type modification cycle needs an extra refinement pass, have " + cyclicPasses);
    }

    @DisplayName("cycle depth (through ctor-param -> field -> accessor -> type-level) shows up as more properties "
                + "refined in the single refinement pass, not more passes")
    @Test
    public void testDepthInvariant() throws IOException {
        List<Integer> shallow = convergenceSequence(CHAIN2, 20);  // 2-type loop
        List<Integer> deep = convergenceSequence(CHAIN4, 20);     // 4-type loop

        // Each pass re-processes the whole analysis order with the previous pass's complete results, so cross-pass
        // staleness is always depth-1: one computing pass, one refinement pass, one confirming (0) pass -- regardless
        // of how many types the constructor-parameter/field/type-level dependency loop spans.
        assertEquals(3, shallow.size(), "a cycle converges with exactly one refinement pass, have " + shallow);
        assertEquals(3, deep.size(), "a deeper cycle still converges with one refinement pass, have " + deep);
        assertEquals(0, shallow.get(2));
        assertEquals(0, deep.get(2));

        // depth manifests as MORE properties changed in that single refinement pass (index 1), never as more passes
        assertTrue(deep.get(1) > shallow.get(1),
                "the deeper loop refines more properties in the refinement pass: " + deep + " vs " + shallow);
    }

    @DisplayName("the iterating analyzer reaches the correct fixed point for the cycle")
    @Test
    public void testCorrectFixedPoint() throws IOException {
        AnalyzerBundle bundle = buildAnalyzerBundle();
        TypeInfo X = bundle.javaInspector().parse("a.b.X", CYCLIC);
        List<Info> ao = bundle.prepAnalyzer().doPrimaryType(X);
        // enough iterations for the fixed point to be reached (it converges early on its own)
        new IteratingAnalyzerImpl(bundle.javaInspector(),
                new IteratingAnalyzerImpl.ConfigurationBuilder().setMaxIterations(10).build()).analyze(ao);

        TypeInfo ping = X.findSubType("Ping");
        // receive() increments its own field -> modifying; hit() is modifying because it calls the other type's
        // modifying receive() around the cycle
        assertTrue(ping.findUniqueMethod("receive", 0).isModifying());
        assertTrue(ping.findUniqueMethod("hit", 0).isModifying(), "modification propagates around the Ping/Pong cycle");
    }
}
