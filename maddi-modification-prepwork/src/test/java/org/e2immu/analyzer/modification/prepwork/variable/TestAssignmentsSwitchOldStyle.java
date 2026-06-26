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
 * Old-style switch, focused on reads/assignments: fall-through that reads a previous case's assignment, a switch
 * without a default, multiple labels sharing one body, and a default placed in the middle. (Complements
 * {@code TestAssignmentsSwitch}, which covers the clean/break/return/nested cases.)
 */
public class TestAssignmentsSwitchOldStyle extends CommonTest {

    private VariableData analyse(String input, int params) {
        TypeInfo X = javaInspector.parse(ABX, input);
        MethodInfo m = X.findUniqueMethod("m", params);
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

    @Language("java") private static final String FALL_THROUGH = """
            package a.b;
            class X {
                static int m(int x) {
                    int r = 0;
                    switch (x) {
                        case 1: r = 1;          // falls through (no break)
                        case 2: r = r + 10; break;
                        default: r = -1;
                    }
                    return r;
                }
            }""";

    @DisplayName("fall-through: case 2 reads the value assigned in case 1 (and at definition)")
    @Test
    public void testFallThrough() {
        VariableData vd = analyse(FALL_THROUGH, 1);
        VariableInfo r = v(vd, "r");
        // r=0 (0), case1 r=1 (1.0.0), case2 r=r+10 reads+assigns (1.0.1), default r=-1 (1.0.3), merge (1=M)
        assertEquals("D:0, A:[0, 1.0.0, 1.0.1, 1.0.3, 1=M]", r.assignments().toString());
        assertEquals("1.0.1, 2", r.reads().toString());
        assertTrue(r.hasBeenDefined("1.0.1")); // the read in 'r = r + 10' sees a value (init or fall-through)
        assertEquals("D:-, A:[2]", rv(vd).assignments().toString());
    }

    @Language("java") private static final String NO_DEFAULT = """
            package a.b;
            class X {
                static void m(int x) {
                    int r;
                    switch (x) {
                        case 1: r = 1; System.out.println(r); break;
                        case 2: r = 2; System.out.println(r); break;
                    }
                }
            }""";

    @DisplayName("switch without default: in-case reads are defined; a merge is still produced")
    @Test
    public void testNoDefault() {
        VariableData vd = analyse(NO_DEFAULT, 1);
        VariableInfo r = v(vd, "r");
        assertEquals("D:0, A:[1.0.0, 1.0.3, 1=M]", r.assignments().toString());
        assertEquals("1.0.1, 1.0.4", r.reads().toString());
        assertTrue(r.hasBeenDefined("1.0.1")); // r assigned before println within case 1
        assertTrue(r.hasBeenDefined("1.0.4")); // within case 2
        // NOTE: a 1=M merge is produced even though there is no default; hence hasBeenDefined is true after the
        // switch too. This is sound for legal Java (a local could not be read there without definite assignment),
        // but means the merge does not by itself prove all paths assign. Flagging for awareness.
        assertTrue(r.hasBeenDefined("9"));
    }

    @Language("java") private static final String MULTIPLE_LABELS = """
            package a.b;
            class X {
                static int m(int x) {
                    int r;
                    switch (x) {
                        case 1:
                        case 2: r = 10; break;
                        default: r = 0;
                    }
                    return r;
                }
            }""";

    @DisplayName("multiple labels on one case share a single body/index")
    @Test
    public void testMultipleLabels() {
        VariableData vd = analyse(MULTIPLE_LABELS, 1);
        VariableInfo r = v(vd, "r");
        // case 1 + case 2 share the single assignment at 1.0.0; default at 1.0.2
        assertEquals("D:0, A:[1.0.0, 1.0.2, 1=M]", r.assignments().toString());
        assertEquals("2", r.reads().toString());
        assertTrue(r.hasBeenDefined("2")); // all paths (incl. default) assign
    }

    @Language("java") private static final String DEFAULT_IN_MIDDLE = """
            package a.b;
            class X {
                static int m(int x) {
                    int r;
                    switch (x) {
                        case 1: r = 1; break;
                        default: r = 0; break;
                        case 2: r = 2; break;
                    }
                    return r;
                }
            }""";

    @DisplayName("default placed in the middle of the cases")
    @Test
    public void testDefaultInMiddle() {
        VariableData vd = analyse(DEFAULT_IN_MIDDLE, 1);
        VariableInfo r = v(vd, "r");
        // case1 (1.0.0), default (1.0.2), case2 (1.0.4), merge (1=M)
        assertEquals("D:0, A:[1.0.0, 1.0.2, 1.0.4, 1=M]", r.assignments().toString());
        assertTrue(r.hasBeenDefined("2"));
    }
}
