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

/** Constructor bodies: this(...)/super(...) argument reads and field assignments — none previously covered. */
public class TestAssignmentsConstructor extends CommonTest {

    private VariableData analyse(MethodInfo constructor) {
        new PrepAnalyzer(runtime).doMethod(constructor);
        return VariableDataImpl.of(constructor);
    }

    private static VariableInfo v(VariableData vd, String simpleName) {
        return vd.variableInfoStream().filter(x -> x.variable().simpleName().equals(simpleName))
                .findFirst().orElseThrow();
    }

    @Language("java") private static final String THIS_DELEGATION =
            "package a.b; class X { X(int a) { this(a, 0); } X(int a, int b) { } }";

    @DisplayName("this(...) delegation reads the forwarded argument")
    @Test
    public void testThisDelegation() {
        TypeInfo X = javaInspector.parse(ABX, THIS_DELEGATION);
        VariableData vd = analyse(X.findConstructor(1));
        assertEquals("0", v(vd, "a").reads().toString());
    }

    @Language("java") private static final String FIELD_ASSIGNS =
            "package a.b; class X { int a, b; X(int x, int y) { a = x; b = y; } }";

    @DisplayName("field assignments in a constructor (this.a = x, this.b = y)")
    @Test
    public void testFieldAssignments() {
        TypeInfo X = javaInspector.parse(ABX, FIELD_ASSIGNS);
        VariableData vd = analyse(X.findConstructor(2));
        assertEquals("D:-, A:[0]", v(vd, "a").assignments().toString()); // field a
        assertEquals("D:-, A:[1]", v(vd, "b").assignments().toString()); // field b
        assertEquals("0", v(vd, "x").reads().toString());
        assertEquals("1", v(vd, "y").reads().toString());
        assertEquals("0, 1", v(vd, "this").reads().toString()); // implicit this on each field assignment
    }

    @Language("java") private static final String SUPER_CALL =
            "package a.b; class X { static class P { P(int x) {} } static class C extends P { C(int a) { super(a); } } }";

    @DisplayName("super(...) reads the forwarded argument")
    @Test
    public void testSuperCall() {
        TypeInfo X = javaInspector.parse(ABX, SUPER_CALL);
        VariableData vd = analyse(X.findSubType("C", true).findConstructor(1));
        assertEquals("0", v(vd, "a").reads().toString());
    }
}
