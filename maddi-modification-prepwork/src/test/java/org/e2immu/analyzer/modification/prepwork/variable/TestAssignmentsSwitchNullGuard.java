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
 * {@code case null} and guarded ({@code when}) patterns. Note these use block-bodied or expression arms in an
 * actual switch <em>expression</em>; the block arms index correctly ({@code 0.0.0} ...), in contrast with the
 * malformed expression-arm index bug captured in {@code TestAssignmentsSwitchNewStyle}.
 */
public class TestAssignmentsSwitchNullGuard extends CommonTest {

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

    @Language("java") private static final String CASE_NULL = """
            package a.b;
            class X {
                static int m(String s) {
                    switch (s) {
                        case null -> { return -1; }
                        case "x" -> { return 1; }
                        default -> { return 0; }
                    }
                }
            }""";

    @DisplayName("switch statement with a case null arm (block-bodied arrows)")
    @Test
    public void testCaseNull() {
        VariableData vd = analyse(CASE_NULL);
        assertEquals("0-E", v(vd, "s").reads().toString());
        // three arms each return; block arms get well-formed indices and a merge
        assertEquals("D:-, A:[0.0.0, 0.1.0, 0.2.0, 0=M]", rv(vd).assignments().toString());
    }

    @Language("java") private static final String GUARDED = """
            package a.b;
            class X {
                static int m(Object o) {
                    return switch (o) {
                        case Integer i when i > 0 -> i;
                        case Integer i -> -i;
                        default -> 0;
                    };
                }
            }""";

    @DisplayName("guarded (when) pattern in a switch expression")
    @Test
    public void testGuardedPattern() {
        VariableData vd = analyse(GUARDED);
        assertEquals("0", v(vd, "o").reads().toString());
        assertEquals("D:-, A:[0]", rv(vd).assignments().toString());
    }
}
