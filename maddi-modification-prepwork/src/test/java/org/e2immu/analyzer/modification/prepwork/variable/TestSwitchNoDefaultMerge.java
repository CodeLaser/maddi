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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * New-style (arrow) switch completeness must be exhaustiveness-aware. A variable assigned in every arm is
 * definitely assigned after the switch (gets the {@code =M} merge marker) only if the switch is exhaustive:
 * it has an explicit {@code default}, or it is a pattern switch (which only compiles when exhaustive). A classic
 * constant switch (int/String/enum labels) without {@code default} is not exhaustive, so the variable is NOT
 * definitely assigned afterwards. See {@code Assignments.assignmentsRequiredForMerge}.
 */
public class TestSwitchNoDefaultMerge extends CommonTest {

    private VariableData analyse(@Language("java") String input) {
        TypeInfo X = javaInspector.parse(ABX, input);
        MethodInfo m = X.findUniqueMethod("m", 1);
        new PrepAnalyzer(runtime).doMethod(m);
        return VariableDataImpl.of(m);
    }

    private static VariableInfo local(VariableData vd, String simpleName) {
        return vd.variableInfoStream().filter(x -> x.variable().simpleName().equals(simpleName))
                .findFirst().orElseThrow();
    }

    @Language("java") private static final String NO_DEFAULT = """
            package a.b;
            class X {
                static int m(int x) {
                    int r;
                    switch (x) {
                        case 1 -> r = 10;
                        case 2 -> r = 20;
                    }
                    return r;
                }
            }""";

    @DisplayName("classic switch WITHOUT default: r is NOT definitely assigned (no =M merge)")
    @Test
    public void testNoDefault() {
        VariableData vd = analyse(NO_DEFAULT);
        VariableInfo r = local(vd, "r");
        assertEquals("D:0, A:[1.0.0, 1.1.0]", r.assignments().toString());
        assertFalse(r.hasBeenDefined("2"));
    }

    @Language("java") private static final String WITH_DEFAULT = """
            package a.b;
            class X {
                static int m(int x) {
                    int r;
                    switch (x) {
                        case 1 -> r = 10;
                        case 2 -> r = 20;
                        default -> r = 0;
                    }
                    return r;
                }
            }""";

    @DisplayName("classic switch WITH default: r IS definitely assigned")
    @Test
    public void testWithDefault() {
        VariableData vd = analyse(WITH_DEFAULT);
        VariableInfo r = local(vd, "r");
        assertEquals("D:0, A:[1.0.0, 1.1.0, 1.2.0, 1=M]", r.assignments().toString());
        assertTrue(r.hasBeenDefined("2"));
    }

    @Language("java") private static final String EXHAUSTIVE_SEALED = """
            package a.b;
            class X {
                sealed interface S permits A, B {}
                record A() implements S {}
                record B() implements S {}
                static int m(S s) {
                    int r;
                    switch (s) {
                        case A a -> r = 1;
                        case B b -> r = 2;
                    }
                    return r;
                }
            }""";

    @DisplayName("exhaustive pattern switch without explicit default: r IS definitely assigned")
    @Test
    public void testExhaustiveSealed() {
        VariableData vd = analyse(EXHAUSTIVE_SEALED);
        VariableInfo r = local(vd, "r");
        assertEquals("D:0, A:[1.0.0, 1.1.0, 1=M]", r.assignments().toString());
        assertTrue(r.hasBeenDefined("2"));
    }
}
