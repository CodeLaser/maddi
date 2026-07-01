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

/** Kotlin port of `modification-prepwork` `TestAssignmentsDoWhile`: do-while reads/assignments + merge. */
class TestAssignmentsDoWhile : CommonKotlinPrep() {

    // Java: `int sum=0; int i=0; do { sum += i; i++; } while (i < n); return sum;`
    private val input = """
        package a.b
        class X {
            fun m(n: Int): Int {
                var sum = 0
                var i = 0
                do {
                    sum += i
                    i++
                } while (i < n)
                return sum
            }
        }
    """.trimIndent()

    private fun local(vd: VariableData, simpleName: String): VariableInfo =
        vd.variableInfoStream().filter { it.variable().simpleName() == simpleName }.findFirst().orElseThrow()

    private fun rv(vd: VariableData): VariableInfo =
        vd.variableInfoStream().filter { it.variable() is ReturnVariable }.findFirst().orElseThrow()

    @Test
    fun test() {
        val x = parse("X.kt", input)
        val m = x.findUniqueMethod("m", 1)
        doMethod(m)
        val vd = VariableDataImpl.of(m)

        val sum = local(vd, "sum")
        assertEquals("2.0.0, 3", sum.reads().toString())
        assertEquals("D:0, A:[0, 2.0.0, 2=M]", sum.assignments().toString())

        val i = local(vd, "i")
        // condition 'i < n' is read at 2:E (post-body evaluation); 'i++' reads+assigns at 2.0.1
        assertEquals("2.0.0, 2.0.1, 2:E", i.reads().toString())
        assertEquals("D:1, A:[1, 2.0.1, 2=M]", i.assignments().toString())

        val n = local(vd, "n")
        assertEquals("2:E", n.reads().toString())
        assertEquals("D:-, A:[]", n.assignments().toString())

        assertEquals("D:-, A:[3]", rv(vd).assignments().toString())
    }
}
