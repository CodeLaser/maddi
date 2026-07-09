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
 * Kotlin port of `modification-prepwork` `TestAssignmentsSwitchNullGuard`, `testCaseNull`: a `when` on a nullable
 * selector with a `null` arm, a value arm, and an `else`, each arm block-bodied and returning. The three block
 * arms index `0.0.0` / `0.1.0` / `0.2.0` and merge at `0=M`; the selector is read once at evaluation (`0-E`).
 *
 * The Java suite's `testGuardedPattern` case (`case Integer i when i > 0`) is N/A: it uses an instanceof-pattern
 * variable, which Kotlin's `is` does not bind (it smart-casts the selector instead).
 */
class TestWhenCaseNull : CommonKotlinPrep() {

    private fun analyse(input: String): VariableData {
        val x = parse("X.kt", input)
        val m = x.findUniqueMethod("m", 1)
        doMethod(m)
        return VariableDataImpl.of(m)
    }

    private fun v(vd: VariableData, simpleName: String): VariableInfo =
        vd.variableInfoStream().filter { it.variable().simpleName() == simpleName }.findFirst().orElseThrow()

    private fun rv(vd: VariableData): VariableInfo =
        vd.variableInfoStream().filter { it.variable() is ReturnVariable }.findFirst().orElseThrow()

    @Test
    fun testCaseNull() {
        val vd = analyse("""
            package a.b
            class X {
                fun m(s: String?): Int {
                    when (s) {
                        null -> { return -1 }
                        "x" -> { return 1 }
                        else -> { return 0 }
                    }
                }
            }
        """.trimIndent())
        assertEquals("0-E", v(vd, "s").reads().toString())
        assertEquals("D:-, A:[0.0.0, 0.1.0, 0.2.0, 0=M]", rv(vd).assignments().toString())
    }
}
