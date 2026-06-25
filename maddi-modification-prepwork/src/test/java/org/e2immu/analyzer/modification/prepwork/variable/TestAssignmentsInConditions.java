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

/**
 * Assignment used as a side effect inside a condition: the assignment index falls on the condition's evaluation
 * stage ({@code -E}/{@code ;E} for a while, {@code -E} for an if), not on a statement of its own.
 */
public class TestAssignmentsInConditions extends CommonTest {

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

    @Language("java") private static final String WHILE_COND = """
            package a.b;
            class X {
                static int m(int[] arr) {
                    int i = 0;
                    int sum = 0;
                    int v;
                    while ((v = arr[i]) > 0) {
                        sum += v;
                        i++;
                    }
                    return sum;
                }
            }""";

    @DisplayName("assignment in a while condition (v = arr[i]) evaluated at -E and ;E")
    @Test
    public void testWhileCondition() {
        VariableData vd = analyse(WHILE_COND);
        VariableInfo varV = v(vd, "v");
        assertEquals("D:2, A:[3-E, 3;E]", varV.assignments().toString()); // assigned in both condition evaluations
        assertEquals("3.0.0", varV.reads().toString());

        assertEquals("D:0, A:[0, 3.0.1]", v(vd, "i").assignments().toString());
        assertEquals("3-E, 3.0.1, 3;E", v(vd, "i").reads().toString());
        assertEquals("D:1, A:[1, 3.0.0]", v(vd, "sum").assignments().toString());
    }

    @Language("java") private static final String IF_COND = """
            package a.b;
            class X {
                static int m(int x) {
                    int y;
                    if ((y = x * 2) > 0) {
                        return y;
                    }
                    return 0;
                }
            }""";

    @DisplayName("assignment in an if condition (y = x*2) evaluated at -E")
    @Test
    public void testIfCondition() {
        VariableData vd = analyse(IF_COND);
        VariableInfo y = v(vd, "y");
        assertEquals("D:0, A:[1-E]", y.assignments().toString()); // assigned in the if condition
        assertEquals("1.0.0", y.reads().toString());              // read in the then-block
        assertEquals("1-E", v(vd, "x").reads().toString());
    }
}
