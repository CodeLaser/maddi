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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Definite-assignment of <em>local</em> variables via {@link VariableInfo#hasBeenDefined(String)}. The existing
 * suite only asserts this on the return variable; here we pin it down for ordinary locals across the control-flow
 * shapes that decide whether a local is definitely assigned at a given point.
 */
public class TestHasBeenDefinedLocals extends CommonTest {

    private VariableData analyse(String input) {
        TypeInfo X = javaInspector.parse(ABX, input);
        MethodInfo m = X.findUniqueMethod("m", 1);
        new PrepAnalyzer(runtime).doMethod(m);
        return VariableDataImpl.of(m);
    }

    @Language("java") private static final String BOTH_BRANCHES = """
            package a.b;
            class X {
                static int m(boolean c) {
                    int x;
                    if (c) x = 1;
                    else x = 2;
                    return x;
                }
            }""";

    @DisplayName("assigned in both if-branches -> defined after the merge")
    @Test
    public void testBothBranches() {
        VariableData vd = analyse(BOTH_BRANCHES);
        VariableInfo x = vd.variableInfo("x");
        assertEquals("D:0, A:[1.0.0, 1.1.0, 1=M]", x.assignments().toString());
        assertFalse(x.hasBeenDefined("0"));     // declared, not yet assigned
        assertTrue(x.hasBeenDefined("1.0.0"));  // inside then-branch
        assertTrue(x.hasBeenDefined("1.1.0"));  // inside else-branch
        assertTrue(x.hasBeenDefined("2"));      // after if: both branches assign
    }

    @Language("java") private static final String ONE_BRANCH = """
            package a.b;
            class X {
                static void m(boolean c) {
                    int x;
                    if (c) {
                        x = 1;
                        System.out.println(x);
                    }
                }
            }""";

    @DisplayName("assigned in one if-branch only -> NOT defined after")
    @Test
    public void testOneBranch() {
        VariableData vd = analyse(ONE_BRANCH);
        VariableInfo x = vd.variableInfo("x");
        assertEquals("D:0, A:[1.0.0]", x.assignments().toString());
        assertTrue(x.hasBeenDefined("1.0.1"));   // inside then-branch, after the assignment
        assertFalse(x.hasBeenDefined("2"));      // fictitious index after the if: not defined (no else)
    }

    @Language("java") private static final String DO_WHILE = """
            package a.b;
            class X {
                static int m(boolean c) {
                    int x;
                    do {
                        x = 1;
                    } while (c);
                    return x;
                }
            }""";

    @DisplayName("assigned in do-while body -> defined after (body runs at least once)")
    @Test
    public void testDoWhileBody() {
        VariableData vd = analyse(DO_WHILE);
        VariableInfo x = vd.variableInfo("x");
        assertEquals("D:0, A:[1.0.0, 1=M]", x.assignments().toString());
        assertTrue(x.hasBeenDefined("2")); // contrast with while: do-body is unconditional, hence a merge
    }

    @Language("java") private static final String WHILE = """
            package a.b;
            class X {
                static void m(boolean c) {
                    int x;
                    while (c) {
                        x = 1;
                        System.out.println(x);
                    }
                }
            }""";

    @DisplayName("assigned in while body -> NOT defined after (body may run zero times)")
    @Test
    public void testWhileBody() {
        VariableData vd = analyse(WHILE);
        VariableInfo x = vd.variableInfo("x");
        assertEquals("D:0, A:[1.0.0]", x.assignments().toString()); // no merge: conditional loop
        assertTrue(x.hasBeenDefined("1.0.1"));  // inside the body, after the assignment
        assertFalse(x.hasBeenDefined("2"));     // fictitious index after the loop: not defined
    }

    @Language("java") private static final String TRY_CATCH = """
            package a.b;
            class X {
                static int m(int n) {
                    int x;
                    try {
                        x = n / n;
                    } catch (RuntimeException e) {
                        x = 0;
                    }
                    return x;
                }
            }""";

    @DisplayName("assigned in try and in all catch clauses -> defined after")
    @Test
    public void testTryCatch() {
        VariableData vd = analyse(TRY_CATCH);
        VariableInfo x = vd.variableInfo("x");
        assertEquals("D:0, A:[1.0.0, 1.1.0, 1=M]", x.assignments().toString());
        assertTrue(x.hasBeenDefined("2"));
    }
}
