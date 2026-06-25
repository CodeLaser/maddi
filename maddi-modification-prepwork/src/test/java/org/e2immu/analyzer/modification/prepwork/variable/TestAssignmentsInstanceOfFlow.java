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
import org.e2immu.language.cst.api.statement.IfElseStatement;
import org.e2immu.language.cst.api.statement.Statement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * instanceof-pattern flow scoping beyond the cases already covered by {@code TestAssignmentsInstanceOf}: the
 * pattern visible in the right-hand side of {@code &&}, nested record deconstruction, and — importantly — the
 * guard-clause idiom whose pattern flows into the rest of the block, which is currently BROKEN (see the bug test).
 */
public class TestAssignmentsInstanceOfFlow extends CommonTest {

    private MethodInfo analyse(String input) {
        TypeInfo X = javaInspector.parse(ABX, input);
        MethodInfo m = X.findUniqueMethod("m", 1);
        new PrepAnalyzer(runtime).doMethod(m);
        return m;
    }

    private static VariableInfo v(VariableData vd, String simpleName) {
        return vd.variableInfoStream().filter(x -> x.variable().simpleName().equals(simpleName))
                .findFirst().orElseThrow();
    }

    private static boolean has(VariableData vd, String simpleName) {
        return vd.variableInfoStream().anyMatch(x -> x.variable().simpleName().equals(simpleName));
    }

    @Language("java") private static final String AND = """
            package a.b;
            class X {
                static boolean m(Object o) {
                    return o instanceof String s && s.isEmpty();
                }
            }""";

    @DisplayName("instanceof pattern usable in the right-hand side of &&")
    @Test
    public void testAnd() {
        MethodInfo m = analyse(AND);
        VariableData vd = VariableDataImpl.of(m.methodBody().lastStatement());
        VariableInfo s = v(vd, "s");
        assertEquals("D:0, A:[0]", s.assignments().toString());
        assertEquals("0", s.reads().toString()); // s.isEmpty()
        assertEquals("0", v(vd, "o").reads().toString());
    }

    @Language("java") private static final String NESTED_RECORD = """
            package a.b;
            class X {
                record Point(int x, int y) {}
                record Line(Point a, Point b) {}
                static int m(Object o) {
                    if (o instanceof Line(Point(int x1, int y1), Point b)) {
                        return x1 + y1;
                    }
                    return 0;
                }
            }""";

    @DisplayName("nested record deconstruction binds all pattern variables in the then-block")
    @Test
    public void testNestedRecordPattern() {
        MethodInfo m = analyse(NESTED_RECORD);
        IfElseStatement ifElse = (IfElseStatement) m.methodBody().statements().get(0);
        VariableData vd = VariableDataImpl.of(ifElse.block().statements().get(0)); // 'return x1 + y1;'
        assertEquals("D:0-E, A:[0-E]", v(vd, "x1").assignments().toString());
        assertEquals("0.0.0", v(vd, "x1").reads().toString());
        assertEquals("D:0-E, A:[0-E]", v(vd, "y1").assignments().toString());
        assertEquals("0.0.0", v(vd, "y1").reads().toString());
        assertEquals("D:0-E, A:[0-E]", v(vd, "b").assignments().toString()); // bound but unused
    }

    @Language("java") private static final String GUARD_CLAUSE = """
            package a.b;
            class X {
                static int m(Object o) {
                    if (!(o instanceof String s)) return -1;
                    return s.length();
                }
            }""";

    @DisplayName("guard-clause pattern (negative instanceof + early return) flows into the rest of the block")
    @Test
    public void testGuardClause() {
        MethodInfo m = analyse(GUARD_CLAUSE);
        IfElseStatement ifElse = (IfElseStatement) m.methodBody().statements().get(0);

        // the pattern variable 's' is created in the condition of the if statement
        VariableData vdIf = VariableDataImpl.of(ifElse);
        assertTrue(has(vdIf, "s"));
        assertEquals("D:0-E, A:[0-E]", v(vdIf, "s").assignments().toString());

        // since the then-branch exits, Java flow-scopes 's' to the rest of the block: it is present at
        // 'return s.length();' and its read is recorded there (regression test for the Util.beyond scope fix)
        VariableData vdUse = VariableDataImpl.of(m.methodBody().statements().get(1));
        assertTrue(has(vdUse, "s"));
        assertEquals("1", v(vdUse, "s").reads().toString());
        assertEquals("D:0-E, A:[0-E]", v(vdUse, "s").assignments().toString());
    }
}
