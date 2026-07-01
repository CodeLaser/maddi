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

package org.e2immu.language.inspection.kotlin.prepwork

import org.e2immu.analyzer.modification.prepwork.variable.ReturnVariable
import org.e2immu.analyzer.modification.prepwork.variable.VariableData
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Kotlin port of `modification-prepwork` `TestAssignmentsExpressionForms` (ternary cases). Java's `?:` is
 * Kotlin's `if/else` expression. Not ported: `a = b = 5` (assignment is not an expression in Kotlin) and
 * `a[i++] = b[j++]` with a multi-declarator `int i=0, j=0` (which would split into two statements and shift
 * every index, breaking the verbatim oracle).
 */
class TestAssignmentsExpressionForms : CommonKotlinPrep() {

    private fun analyse(input: String, params: Int): VariableData {
        val x = parse("X.kt", input)
        val m = x.findUniqueMethod("m", params)
        doMethod(m)
        return VariableDataImpl.of(m)
    }

    private fun v(vd: VariableData, simpleName: String): VariableInfo =
        vd.variableInfoStream().filter { it.variable().simpleName() == simpleName }.findFirst().orElseThrow()

    private fun rv(vd: VariableData): VariableInfo =
        vd.variableInfoStream().filter { it.variable() is ReturnVariable }.findFirst().orElseThrow()

    @Test
    fun testTernary() {
        // `int r = c ? a : b; return r;`
        val vd = analyse("package a.b; class X { fun m(c: Boolean, a: Int, b: Int): Int { val r = if (c) a else b; return r } }", 3)
        assertEquals("0", v(vd, "c").reads().toString())
        assertEquals("0", v(vd, "a").reads().toString())
        assertEquals("0", v(vd, "b").reads().toString())
        assertEquals("D:0, A:[0]", v(vd, "r").assignments().toString())
        assertEquals("D:-, A:[1]", rv(vd).assignments().toString())
    }

    @Test
    fun testNestedTernary() {
        // `return x > 0 ? 1 : x < 0 ? -1 : 0;`
        val vd = analyse("package a.b; class X { fun m(x: Int): Int { return if (x > 0) 1 else if (x < 0) -1 else 0 } }", 1)
        assertEquals("0", v(vd, "x").reads().toString())
        assertEquals("D:-, A:[0]", rv(vd).assignments().toString())
    }
}
