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
 * Field and array references (the existing suite is almost entirely local/parameter centric): a compound
 * assignment reads <em>and</em> assigns its target; a nested field assignment {@code a.b.c = ...} reads the
 * intermediate {@code a} and {@code a.b} and assigns {@code a.b.c}.
 */
public class TestAssignmentsFieldAccess extends CommonTest {

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

    @Language("java") private static final String COMPOUND_FIELD =
            "package a.b; class X { int f; void m(int d) { this.f += d; } }";

    @DisplayName("compound assignment on an instance field reads and assigns it")
    @Test
    public void testCompoundField() {
        VariableData vd = analyse(COMPOUND_FIELD, 1);
        VariableInfo f = v(vd, "f");
        assertEquals("0", f.reads().toString());       // += reads f
        assertEquals("D:-, A:[0]", f.assignments().toString());
        assertEquals("0", v(vd, "d").reads().toString());
        assertEquals("0", v(vd, "this").reads().toString());
    }

    @Language("java") private static final String COMPOUND_ARRAY =
            "package a.b; class X { static void m(int[] a, int i, int v) { a[i] += v; } }";

    @DisplayName("compound assignment on an array element reads and assigns it")
    @Test
    public void testCompoundArray() {
        VariableData vd = analyse(COMPOUND_ARRAY, 3);
        VariableInfo ai = v(vd, "a[i]");
        assertEquals("0", ai.reads().toString());
        assertEquals("D:-, A:[0]", ai.assignments().toString());
        assertEquals("0", v(vd, "a").reads().toString());
        assertEquals("0", v(vd, "i").reads().toString());
        assertEquals("0", v(vd, "v").reads().toString());
    }

    @Language("java") private static final String NESTED_CHAIN =
            "package a.b; class X { static class A { B b; } static class B { int c; } void m(A a) { a.b.c = 5; } }";

    @DisplayName("nested field assignment a.b.c = 5 reads a and a.b, assigns a.b.c")
    @Test
    public void testNestedFieldChain() {
        VariableData vd = analyse(NESTED_CHAIN, 1);
        assertEquals("D:-, A:[0]", v(vd, "c").assignments().toString()); // a.b.c assigned
        assertEquals("0", v(vd, "b").reads().toString());                // a.b read
        assertEquals("0", v(vd, "a").reads().toString());                // a read
    }

    @Language("java") private static final String STATIC_FIELD =
            "package a.b; class X { static int s; static int m() { s = 1; int t = s; return t; } }";

    @DisplayName("static field assignment and read")
    @Test
    public void testStaticField() {
        VariableData vd = analyse(STATIC_FIELD, 0);
        VariableInfo s = v(vd, "s");
        assertEquals("D:-, A:[0]", s.assignments().toString());
        assertEquals("1", s.reads().toString());
        assertEquals("D:1, A:[1]", v(vd, "t").assignments().toString());
        assertEquals("2", v(vd, "t").reads().toString());
    }
}
