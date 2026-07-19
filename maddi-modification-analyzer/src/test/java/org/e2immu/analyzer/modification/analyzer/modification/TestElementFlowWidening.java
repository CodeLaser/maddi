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

package org.e2immu.analyzer.modification.analyzer.modification;

import org.e2immu.analyzer.modification.analyzer.CommonTest;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.info.Info;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Characterization of the CONTENT-TIER WIDENING that drives the dominant fernflower divergence
 * cascades (2026-07-19 investigation; the gatherGenerics shape): an ELEMENT of a parameter's
 * hidden content flows into the receiver's own container, and the receiver's container is later
 * modified. The element's own object graph is never touched — modification of the DESTINATION
 * container must not travel back through the shared element to the SOURCE container.
 * <p>
 * Shape (fernflower's Exprent.gatherGenerics + NewExprent.inferExprType, minimized):
 * {@code String left = upper.get(0); this.args.add(left);} — and elsewhere {@code args.clear()}.
 * The engine claims {@code upper} modified; the precise verdict is unmodified. This is the §7.1
 * element-natures policy of PLAN-modification-reachability and the VL2O content-tier family
 * (task #37) in one concrete pin. The CONTROL asserts that modifying the element itself DOES
 * modify {@code upper} — the widening is only wrong for container-level modification of the
 * destination.
 */
public class TestElementFlowWidening extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.List;
            import java.util.ArrayList;
            class X {
                final List<StringBuilder> args = new ArrayList<>();
                StringBuilder take(List<StringBuilder> upper) {
                    StringBuilder left = upper.get(0);
                    this.args.add(left);
                    return left;
                }
                void reset() { this.args.clear(); }
            }
            """;

    @DisplayName("element flow + destination-container modification: does 'upper' get widened to modified?")
    @Test
    public void test() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT);
        List<Info> ao = prepWork(X);
        analyzer.go(ao, 5);

        MethodInfo take = X.findUniqueMethod("take", 1);
        MethodInfo reset = X.findUniqueMethod("reset", 0);

        // ground truth on the parts that are uncontroversial:
        assertFalse(reset.isNonModifying(), "reset() clears this.args: modifying");
        assertFalse(X.getFieldByName("args", true).isUnmodified(), "args is cleared: modified");

        // THE PIN: 'upper' — an element was read out of it and stored; upper's own object graph
        // (the list) was never modified. Precise verdict: unmodified. If this assertion FAILS,
        // the content-tier widening is live (the engine propagates the destination container's
        // modification back through the shared element) — flip the assertion, keep the comment,
        // and link the fix to PLAN §7.1 / task #37's tier cut.
        boolean upperUnmodified = take.parameters().getFirst().isUnmodified();
        assertTrue(upperUnmodified,
                "upper's container was only read; destination-container modification must not widen back");
    }

    /**
     * The FAITHFUL fernflower shape (Exprent.gatherGenerics called from NewExprent.inferExprType):
     * the element crossing happens in a HELPER whose destination parameter is aliased to a FIELD at
     * the call site, and the field is modified (clear) in the caller. The widening locus is then
     * TypeModIndyAnalyzerImpl.handleParameter's "parameter linked to a modified field" rule firing
     * on the content-tier chain upper ∋ left ∈ out ≡ this.args.
     */
    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.List;
            import java.util.ArrayList;
            class Y {
                final List<StringBuilder> args = new ArrayList<>();
                StringBuilder infer(List<StringBuilder> upper) {
                    this.args.clear();
                    return gather(upper, this.args);
                }
                private static StringBuilder gather(List<StringBuilder> upperBound, List<StringBuilder> out) {
                    StringBuilder left = upperBound.get(0);
                    out.add(left);
                    return left;
                }
            }
            """;

    @DisplayName("helper + field-aliased destination: the gatherGenerics shape")
    @Test
    public void testHelperFieldAlias() {
        TypeInfo Y = javaInspector.parse("a.b.Y", INPUT2);
        List<Info> ao = prepWork(Y);
        analyzer.go(ao, 5);

        MethodInfo gather = Y.findUniqueMethod("gather", 2);
        MethodInfo infer = Y.findUniqueMethod("infer", 1);

        assertFalse(Y.getFieldByName("args", true).isUnmodified(), "args is cleared and added to: modified");
        assertFalse(gather.parameters().get(1).isUnmodified(), "out receives an element: modified");

        // THE PIN, twice: neither the helper's source parameter nor the caller's 'upper' is
        // modified — elements were read OUT of them, never written through them. If either
        // assertion fails, the content-tier widening of the fernflower cascades is reproduced
        // here (upper ∋ left ∈ out ≡ this.args, args modified => upper claimed modified).
        assertTrue(gather.parameters().get(0).isUnmodified(),
                "gather's upperBound is read-only; element flow out is not modification");
        assertTrue(infer.parameters().getFirst().isUnmodified(),
                "infer's upper is read-only; the field-aliased destination must not widen back");
    }

    /**
     * The last two fernflower ingredients: the source's list is reached through an ACCESSOR
     * (a dependent alias of the source's field) opened by a DOWNCAST of the parameter
     * (VarType -> GenericType in the original; the #42 downcast family).
     */
    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            import java.util.List;
            import java.util.ArrayList;
            class Z {
                static class Base { }
                static class Gen extends Base {
                    private final List<StringBuilder> arguments = new ArrayList<>();
                    List<StringBuilder> getArguments() { return arguments; }
                }
                final List<StringBuilder> args = new ArrayList<>();
                StringBuilder infer(Base upper) {
                    this.args.clear();
                    if (upper instanceof Gen) {
                        List<StringBuilder> leftArgs = ((Gen) upper).getArguments();
                        StringBuilder left = leftArgs.get(0);
                        this.args.add(left);
                        return left;
                    }
                    return null;
                }
            }
            """;

    @DisplayName("accessor + downcast source: the full gatherGenerics chain")
    @Test
    public void testAccessorDowncast() {
        TypeInfo Z = javaInspector.parse("a.b.Z", INPUT3);
        List<Info> ao = prepWork(Z);
        analyzer.go(ao, 5);

        MethodInfo infer = Z.findUniqueMethod("infer", 1);
        assertFalse(Z.getFieldByName("args", true).isUnmodified(), "args is cleared and added to: modified");

        // THE PIN: upper is read through a downcast-opened accessor; nothing in its object graph
        // is written. If this fails, the widening needs the downcast/accessor chain — link the
        // diagnosis to the mediated-link (#39) and virtual-field machinery.
        assertTrue(infer.parameters().getFirst().isUnmodified(),
                "upper is read-only through the downcast accessor; no widening");
    }
}
