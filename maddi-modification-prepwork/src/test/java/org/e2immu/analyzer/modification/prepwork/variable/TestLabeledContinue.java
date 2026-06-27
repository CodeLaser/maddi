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
import org.e2immu.language.cst.api.statement.LoopStatement;
import org.e2immu.language.cst.api.statement.Statement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * An infinite loop ({@code while(true)} / {@code for(;;)}) with no break "never exits normally", so a variable
 * assigned in its body is treated as definitely assigned afterwards (a {@code <index>=M} merge). But a labeled
 * {@code continue} that targets an OUTER loop abruptly completes the inner loop, so the inner-loop body may be
 * cut short and that "definite" merge must NOT be produced. See {@code MethodAnalyzer.handleStatement}
 * (breakCountsInLoop) and the {@code noBreakStatementsInside} loop-merge path.
 */
public class TestLabeledContinue extends CommonTest {

    // returns the inner (second-encountered) loop statement
    private static Statement findInnerLoop(Statement s, boolean[] seenOuter) {
        if (s instanceof LoopStatement) {
            if (seenOuter[0]) return s;
            seenOuter[0] = true;
        }
        for (var b : s.subBlocks()) {
            for (Statement sub : b.statements()) {
                Statement r = findInnerLoop(sub, seenOuter);
                if (r != null) return r;
            }
        }
        return null;
    }

    private String innerLoopAssignmentsOfR(@Language("java") String input) {
        TypeInfo X = javaInspector.parse(ABX, input);
        MethodInfo m = X.findUniqueMethod("m", 1);
        new PrepAnalyzer(runtime).doMethod(m);
        Statement inner = null;
        boolean[] seen = {false};
        for (Statement s : m.methodBody().statements()) {
            inner = findInnerLoop(s, seen);
            if (inner != null) break;
        }
        VariableData vd = VariableDataImpl.of(inner);
        return vd.variableInfoStream()
                .filter(vi -> vi.variable().simpleName().equals("r"))
                .map(vi -> vi.assignments().toString())
                .findFirst().orElseThrow();
    }

    @DisplayName("labeled continue to outer loop suppresses the inner infinite-loop definite-assignment merge")
    @Test
    public void testContinueOuter() {
        // r=1 can be skipped by 'continue outer', so r is NOT definitely assigned when the inner loop is left:
        // no '1.0.0=M' merge
        String withContinue = innerLoopAssignmentsOfR("""
                package a.b;
                class X {
                    static void m(boolean cond) {
                        int r;
                        outer:
                        while (true) {
                            while (true) {
                                if (cond) continue outer;
                                r = 1;
                            }
                        }
                    }
                }""");
        assertEquals("D:0, A:[1.0.0.0.1]", withContinue);
    }

    @DisplayName("truly infinite inner loop (no abrupt exit) keeps the definite-assignment merge")
    @Test
    public void testTrulyInfinite() {
        String noContinue = innerLoopAssignmentsOfR("""
                package a.b;
                class X {
                    static void m(boolean cond) {
                        int r;
                        outer:
                        while (true) {
                            while (true) {
                                r = 1;
                            }
                        }
                    }
                }""");
        assertEquals("D:0, A:[1.0.0.0.0, 1.0.0=M]", noContinue);
    }
}
