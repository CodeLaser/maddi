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

package org.e2immu.analyzer.modification.prepwork.variable;

import org.e2immu.analyzer.modification.prepwork.CommonTest;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** try/finally with a return crossing the finally, and a multi-catch ({@code A | B}) union catch variable. */
public class TestAssignmentsTryFinally extends CommonTest {

    private VariableData analyse(String input) {
        TypeInfo X = javaInspector.parse(ABX, input);
        MethodInfo m = X.findUniqueMethod("m", 1);
        new PrepAnalyzer(runtime).doMethod(m);
        return VariableDataImpl.of(m);
    }

    private static VariableInfo v(VariableData vd, String simpleName) {
        return vd.variableInfoStream().filter(x -> x.variable().simpleName().equals(simpleName))
                .findFirst().orElseThrow();
    }

    private static VariableInfo rv(VariableData vd) {
        return vd.variableInfoStream().filter(x -> x.variable() instanceof ReturnVariable).findFirst().orElseThrow();
    }

    @Language("java") private static final String RETURN_IN_TRY_FINALLY = """
            package a.b;
            class X {
                static int m(int n) {
                    int x = 0;
                    try {
                        x = n / n;
                        return x;
                    } finally {
                        x = -1;
                    }
                }
            }""";

    @DisplayName("return inside try, with a finally that reassigns")
    @Test
    public void testReturnInTryFinally() {
        VariableData vd = analyse(RETURN_IN_TRY_FINALLY);
        VariableInfo x = v(vd, "x");
        // 0: init, 1.0.0: try body, 1.1.0: finally body, 1=M: merge
        assertEquals("D:0, A:[0, 1.0.0, 1.1.0, 1=M]", x.assignments().toString());
        assertEquals("1.0.1", x.reads().toString()); // 'return x' inside the try
        assertEquals("D:-, A:[1.0.1, 1=M]", rv(vd).assignments().toString());
    }

    @Language("java") private static final String MULTI_CATCH = """
            package a.b;
            class X {
                static int m(String s) {
                    int x;
                    try {
                        x = Integer.parseInt(s);
                    } catch (NumberFormatException | NullPointerException e) {
                        x = -1;
                    }
                    return x;
                }
            }""";

    @DisplayName("multi-catch: try and the union catch both assign -> defined after")
    @Test
    public void testMultiCatch() {
        VariableData vd = analyse(MULTI_CATCH);
        VariableInfo x = v(vd, "x");
        assertEquals("D:0, A:[1.0.0, 1.1.0, 1=M]", x.assignments().toString());
        assertEquals("2", x.reads().toString());
        assertTrue(x.hasBeenDefined("2"));
        assertEquals("1.0.0", v(vd, "s").reads().toString());
    }

    @Language("java") private static final String LABELED_BREAK_THROUGH_FINALLY = """
            package a.b;
            class X {
                static int m(int n) {
                    int r = 0;
                    outer:
                    for (int i = 0; i < n; i++) {
                        try {
                            if (i == 5) break outer;
                            r += i;
                        } finally {
                            r++;
                        }
                    }
                    return r;
                }
            }""";

    @DisplayName("labeled break crossing a try/finally that reassigns")
    @Test
    public void testLabeledBreakThroughFinally() {
        VariableData vd = analyse(LABELED_BREAK_THROUGH_FINALLY);
        VariableInfo r = v(vd, "r");
        // r += i in the try body (1.0.0.0.1), r++ in the finally (1.0.0.1.0), try/finally merge (1.0.0=M)
        assertEquals("D:0, A:[0, 1.0.0.0.1, 1.0.0.1.0, 1.0.0=M]", r.assignments().toString());
        assertEquals("1.0.0.0.1, 1.0.0.1.0, 2", r.reads().toString());
        assertTrue(r.hasBeenDefined("2"));
        assertEquals("1-E, 1;E", v(vd, "n").reads().toString());
    }
}
