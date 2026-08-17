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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Kotlin port of `modification-prepwork` `TestAssignmentsSwitchNewStyle`: a Kotlin `when` is the analogue of the
 * Java arrow switch. Assignment in every arm (including `else`) yields a `1=M` merge, so the variable is
 * definitely assigned afterwards; expression arms (`1 -> r = 10`) and block arms (`2 -> { r = 20 }`) index
 * consistently as statement 0 of their entry (`1.0.0` / `1.1.0` / `1.2.0`).
 */
class TestAssignmentsWhen : CommonKotlinPrep() {

    private fun analyse(input: String): VariableData {
        val x = parse("X.kt", input)
        val m = x.findUniqueMethod("m", 1)
        doMethod(m)
        return VariableDataImpl.of(m)
    }

    private fun local(vd: VariableData, simpleName: String): VariableInfo =
        vd.variableInfoStream().filter { it.variable().simpleName() == simpleName }.findFirst().orElseThrow()

    private val whenInt = """
        package a.b
        class X {
            fun m(x: Int): Int {
                var r: Int
                when (x) {
                    1 -> r = 10
                    2 -> { r = 20 }
                    else -> r = 0
                }
                return r
            }
        }
    """.trimIndent()

    @Test
    fun testWhen() {
        val vd = analyse(whenInt)
        val r = local(vd, "r")
        assertEquals("D:0, A:[1.0.0, 1.1.0, 1.2.0, 1=M]", r.assignments().toString())
        assertEquals("2", r.reads().toString())
        assertTrue(r.hasBeenDefined("2")) // all arms (incl. else) assign -> defined after

        val x = local(vd, "x")
        assertEquals("1-E", x.reads().toString())
    }

    // NOTE: the Java original's second case is a *pattern* switch (`case Integer i -> s = "int" + i`), which
    // binds a fresh pattern variable. Kotlin's `is Int` smart-casts the selector `o` itself instead of
    // introducing a new variable, so `o` picks up extra reads inside the arms — the VariableData is no longer
    // identical, so that sub-test is intentionally not ported (Kotlin smart-cast != Java pattern binding).
}
