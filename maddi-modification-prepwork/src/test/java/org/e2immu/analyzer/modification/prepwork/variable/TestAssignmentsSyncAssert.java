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

/** synchronized (lock evaluated, body merged) and assert (condition and message both read). */
public class TestAssignmentsSyncAssert extends CommonTest {

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

    @Language("java") private static final String SYNCHRONIZED =
            "package a.b; class X { static void m(Object lock, int[] a) { synchronized (lock) { a[0] = 1; } } }";

    @DisplayName("synchronized: lock read at eval, body assignment merged")
    @Test
    public void testSynchronized() {
        VariableData vd = analyse(SYNCHRONIZED, 2);
        assertEquals("0-E", v(vd, "lock").reads().toString());
        assertEquals("0.0.0", v(vd, "a").reads().toString());
        assertEquals("D:-, A:[0.0.0, 0=M]", v(vd, "a[0]").assignments().toString());
    }

    @Language("java") private static final String ASSERT =
            "package a.b; class X { static void m(int x) { assert x > 0 : \"msg \" + x; } }";

    @DisplayName("assert: both the condition and the message read the variable")
    @Test
    public void testAssert() {
        VariableData vd = analyse(ASSERT, 1);
        assertEquals("0", v(vd, "x").reads().toString());
    }
}
