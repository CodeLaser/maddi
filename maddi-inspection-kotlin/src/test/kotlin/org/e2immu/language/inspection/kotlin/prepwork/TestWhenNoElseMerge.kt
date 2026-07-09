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
 * Kotlin port of `modification-prepwork` `TestSwitchNoDefaultMerge`: `when` completeness must be
 * exhaustiveness-aware. A variable assigned in every arm is definitely assigned after the `when` (gets the `1=M`
 * merge marker) only if the `when` is exhaustive — it has an explicit `else`, or it is an exhaustive `is`-`when`
 * over a `sealed` hierarchy. A classic constant `when` (int labels) without `else` is not exhaustive, so the
 * variable is NOT definitely assigned afterwards.
 */
class TestWhenNoElseMerge : CommonKotlinPrep() {

    private fun analyse(input: String): VariableData {
        val x = parse("X.kt", input)
        val m = x.findUniqueMethod("m", 1)
        doMethod(m)
        return VariableDataImpl.of(m)
    }

    private fun local(vd: VariableData, simpleName: String): VariableInfo =
        vd.variableInfoStream().filter { it.variable().simpleName() == simpleName }.findFirst().orElseThrow()

    // r is pre-initialised so that the source compiles; the point is the ABSENCE of the '1=M' merge marker.
    private val noElse = """
        package a.b
        class X {
            fun m(x: Int): Int {
                var r = 0
                when (x) {
                    1 -> r = 10
                    2 -> r = 20
                }
                return r
            }
        }
    """.trimIndent()

    @Test
    fun testNoElse() {
        val vd = analyse(noElse)
        val r = local(vd, "r")
        assertEquals("D:0, A:[0, 1.0.0, 1.1.0]", r.assignments().toString())
    }

    private val withElse = """
        package a.b
        class X {
            fun m(x: Int): Int {
                var r: Int
                when (x) {
                    1 -> r = 10
                    2 -> r = 20
                    else -> r = 0
                }
                return r
            }
        }
    """.trimIndent()

    @Test
    fun testWithElse() {
        val vd = analyse(withElse)
        val r = local(vd, "r")
        assertEquals("D:0, A:[1.0.0, 1.1.0, 1.2.0, 1=M]", r.assignments().toString())
        assertTrue(r.hasBeenDefined("2"))
    }

    private val exhaustiveSealed = """
        package a.b
        class X {
            sealed interface S
            class A : S
            class B : S
            fun m(s: S): Int {
                var r: Int
                when (s) {
                    is A -> r = 1
                    is B -> r = 2
                }
                return r
            }
        }
    """.trimIndent()

    @Test
    fun testExhaustiveSealed() {
        val vd = analyse(exhaustiveSealed)
        val r = local(vd, "r")
        assertEquals("D:0, A:[1.0.0, 1.1.0, 1=M]", r.assignments().toString())
        assertTrue(r.hasBeenDefined("2"))
    }
}
