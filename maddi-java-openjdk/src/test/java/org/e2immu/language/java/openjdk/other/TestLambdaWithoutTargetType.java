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

package org.e2immu.language.java.openjdk.other;

import org.e2immu.language.java.openjdk.CommonTest;
import org.e2immu.language.java.openjdk.ScanCompilationUnits;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A lambda passed to a call that does not resolve. {@code JCLambda.target} — the functional interface javac
 * inferred — is <b>null</b> there: the call's method symbol is an error symbol, so there was nothing to infer
 * from. {@code visitLambdaExpression} dereferences it immediately ({@code convert(lambda.target)}, then
 * {@code findInstantiatedSAM}).
 * <p>
 * ⛔ The null used to reach {@code ClassSymbolScanner.convert}, fall through every {@code instanceof} case and
 * surface at the bottom as {@code Cannot invoke "Type.toString()" because "type" is null} — a javac internal,
 * one frame past the site, naming neither the lambda nor the unresolved call. Because the message carried no
 * {@link org.e2immu.language.java.openjdk.UnresolvedSymbolException}, fault isolation graded it a hard
 * <em>error</em> rather than the tolerable partial-classpath miss it is, so a single such lambda failed the
 * whole run.
 * <p>
 * Found on a timefold corpus whose parse configuration predated a module split:
 * {@code TimefoldSolverEnterpriseService.loadOrNull(b -> ...)} where that service was no longer on the
 * classpath. Two of the run's three fatal units were this, and both are recoverable — the other 11 units with
 * the same root cause were already being dropped as unresolved symbols.
 */
public class TestLambdaWithoutTargetType extends CommonTest {

    private static final String MESSAGE = "No target type for lambda";

    private void assertToleratedLambdaDrop(ScanCompilationUnits.Result result) {
        assertEquals(1, result.failures().size());
        ScanCompilationUnits.CompilationUnitFailure failure = result.failures().getFirst();
        assertTrue(failure.tolerable(), "an unresolved call is a partial-classpath miss, not a hard error: "
                                       + failure.detail());
        assertTrue(failure.detail().startsWith(MESSAGE), "the message must name the lambda, not a javac "
                                                         + "internal one frame on: " + failure.detail());
    }

    @DisplayName("a lambda argument of an unresolved call, in a class")
    @Test
    public void testInAClass() {
        assertToleratedLambdaDrop(scan(true, Map.of("a.b.E", """
                package a.b;
                public class E {
                    public E() {
                    }
                    void m() {
                        var v = a.b.gone.Svc.loadOrNull(x -> x);
                        System.out.println(v);
                    }
                }
                """)));
    }

    /**
     * The same in an interface, which has no constructor: without one, nothing else in the unit can fail first,
     * so this pins the lambda as the cause rather than relying on scan order.
     */
    @DisplayName("a lambda argument of an unresolved call, in an interface")
    @Test
    public void testInAnInterface() {
        assertToleratedLambdaDrop(scan(true, Map.of("a.b.G", """
                package a.b;
                public interface G {
                    static void m() {
                        var v = a.b.gone.Svc.loadOrNull(x -> x);
                        System.out.println(v);
                    }
                }
                """)));
    }
}
