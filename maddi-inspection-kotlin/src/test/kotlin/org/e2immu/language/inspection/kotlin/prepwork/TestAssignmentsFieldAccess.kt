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

import org.e2immu.analyzer.modification.prepwork.variable.VariableData
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Kotlin port of `modification-prepwork` `TestAssignmentsFieldAccess`: a compound assignment (`+=`) on an
 * instance field reads *and* assigns its target.
 *
 * The Java suite's other cases have no faithful Kotlin form and are not ported: an indexed compound assignment
 * (`a[i] += v`) desugars through Kotlin's `get`/`set` operator convention, so no `a[i]` dependent variable is
 * produced; a nested field assignment (`a.b.c = ...`) resolves through property getter/setter calls, so there is
 * no `a`/`b`/`c` field-variable to track; and the static-field case would need a `companion object`, which
 * changes the enclosing structure.
 */
class TestAssignmentsFieldAccess : CommonKotlinPrep() {

    private fun analyse(input: String, params: Int): VariableData {
        val x = parse("X.kt", input)
        val m = x.findUniqueMethod("m", params)
        doMethod(m)
        return VariableDataImpl.of(m)
    }

    private fun v(vd: VariableData, simpleName: String): VariableInfo =
        vd.variableInfoStream().filter { it.variable().simpleName() == simpleName }.findFirst().orElseThrow()

    @Test
    fun testCompoundField() {
        val vd = analyse("package a.b; class X { var f: Int = 0; fun m(d: Int) { this.f += d } }", 1)
        val f = v(vd, "f")
        assertEquals("0", f.reads().toString())       // += reads f
        assertEquals("D:-, A:[0]", f.assignments().toString())
        assertEquals("0", v(vd, "d").reads().toString())
        assertEquals("0", v(vd, "this").reads().toString())
    }
}
