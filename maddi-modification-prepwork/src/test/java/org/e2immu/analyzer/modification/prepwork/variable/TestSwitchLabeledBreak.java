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
import org.e2immu.language.cst.api.statement.BreakStatement;
import org.e2immu.language.cst.api.statement.Statement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * An old-style switch creates a synthetic boolean break variable {@code bv-<switchIndex>}; a {@code break}
 * statement assigns it only when it actually breaks out of THAT switch. A labeled {@code break} to an outer loop,
 * and an unlabeled {@code break} that targets a loop nested inside the switch, must NOT be attributed to the
 * switch break variable. (The break's target is {@code goToLabel()}, not {@code label()}.)
 * See {@code MethodAnalyzer.analyzeEval} / {@code InternalVariables.breakTargetsNearestOldStyleSwitch}.
 */
public class TestSwitchLabeledBreak extends CommonTest {

    private static void collectBreaks(Statement s, List<BreakStatement> out) {
        if (s instanceof BreakStatement bs) out.add(bs);
        s.subBlocks().forEach(b -> b.statements().forEach(sub -> collectBreaks(sub, out)));
    }

    // map: break-statement index -> is the switch break variable assigned at that break?
    private TreeMap<String, Boolean> breakVarAssignedPerBreak(@Language("java") String input) {
        TypeInfo X = javaInspector.parse(ABX, input);
        MethodInfo m = X.findUniqueMethod("m", 1);
        new PrepAnalyzer(runtime).doMethod(m);

        List<BreakStatement> breaks = new ArrayList<>();
        m.methodBody().statements().forEach(s -> collectBreaks(s, breaks));

        TreeMap<String, Boolean> result = new TreeMap<>();
        for (BreakStatement bs : breaks) {
            VariableData vd = VariableDataImpl.of(bs);
            boolean assigned = vd.variableInfoStream()
                    .filter(vi -> vi.variable().simpleName().startsWith("bv-"))
                    .anyMatch(vi -> !vi.assignments().isEmpty());
            result.put(bs.source().index(), assigned);
        }
        return result;
    }

    @Language("java") private static final String LABELED_BREAK_TO_LOOP = """
            package a.b;
            class X {
                static int m(int x) {
                    int r = 0;
                    outer:
                    for (int i = 0; i < 3; i++) {
                        switch (x) {
                            case 1: break outer;
                            case 2: r = 2; break;
                            default: r = 3; break;
                        }
                    }
                    return r;
                }
            }""";

    @DisplayName("labeled break to outer loop is not attributed to the switch break variable")
    @Test
    public void testLabeledBreakToLoop() {
        TreeMap<String, Boolean> assigned = breakVarAssignedPerBreak(LABELED_BREAK_TO_LOOP);
        // 1.0.0.0.0 = 'break outer' (targets loop, NOT switch); 1.0.0.0.2 and 1.0.0.0.4 = plain 'break' (switch)
        assertEquals("{1.0.0.0.0=false, 1.0.0.0.2=true, 1.0.0.0.4=true}", assigned.toString());
    }

    @Language("java") private static final String UNLABELED_BREAK_TO_NESTED_LOOP = """
            package a.b;
            class X {
                static int m(int x) {
                    int r = 0;
                    switch (x) {
                        case 1:
                            for (int i = 0; i < 3; i++) {
                                if (i == x) break;
                            }
                            r = 1;
                            break;
                        default:
                            r = 0;
                    }
                    return r;
                }
            }""";

    @DisplayName("unlabeled break to a loop nested in the switch is not attributed to the switch break variable")
    @Test
    public void testUnlabeledBreakToNestedLoop() {
        TreeMap<String, Boolean> assigned = breakVarAssignedPerBreak(UNLABELED_BREAK_TO_NESTED_LOOP);
        // the break inside the for-loop targets the loop (false); only the case-level break targets the switch (true)
        assertEquals("{1.0.0.0.0.0.0=false, 1.0.2=true}", assigned.toString());
    }
}
