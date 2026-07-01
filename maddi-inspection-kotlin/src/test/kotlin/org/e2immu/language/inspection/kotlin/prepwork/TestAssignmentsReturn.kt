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

import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/**
 * Kotlin port of `modification-prepwork` `TestAssignmentsReturn`. The Kotlin source uses `package a.b`, so
 * the FQN-bearing oracle strings (`a.b.X.method(char)`, …) copy over verbatim from the Java test.
 */
class TestAssignmentsReturn : CommonKotlinPrep() {

    // Java: `int i; if(c=='?'){ return 0; } else { i = 1; } return i;`
    private val input1 = """
        package a.b
        class X {
            fun method(c: Char): Int {
                var i: Int
                if (c == '?') {
                    return 0
                } else {
                    i = 1
                }
                return i
            }
        }
    """.trimIndent()

    @Test
    fun test1() {
        val x = parse("X.kt", input1)
        val method = x.findUniqueMethod("method", 1)
        doMethod(method)

        val vdMethod = VariableDataImpl.of(method)
        assertNotNull(vdMethod)
        assertEquals("a.b.X.method(char), a.b.X.method(char):0:c, i", vdMethod.knownVariableNamesToString())
        assertEquals("[i, a.b.X.method(char):0:c, a.b.X.method(char)]", vdMethod.knownVariableNames().toString())

        val iVi = vdMethod.variableInfo("i")
        assertEquals("2", iVi.reads().toString())
        assertEquals("D:0, A:[1.1.0, 1=M]", iVi.assignments().toString())
    }
}
