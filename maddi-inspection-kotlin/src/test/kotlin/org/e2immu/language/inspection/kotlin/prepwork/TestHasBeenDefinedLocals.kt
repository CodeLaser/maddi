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
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Kotlin port of `modification-prepwork` `TestHasBeenDefinedLocals`. The control-flow shapes (if/else, do-while,
 * while, try/catch) translate structurally 1:1 to Kotlin, so the assignment-index and `hasBeenDefined` oracle
 * strings copy over verbatim.
 */
class TestHasBeenDefinedLocals : CommonKotlinPrep() {

    private fun analyse(input: String): VariableData {
        val x = parse("X.kt", input)
        val m = x.findUniqueMethod("m", 1)
        doMethod(m)
        return VariableDataImpl.of(m)
    }

    // assigned in both if-branches -> defined after the merge
    private val bothBranches = """
        package a.b
        class X {
            fun m(c: Boolean): Int {
                var x: Int
                if (c) x = 1
                else x = 2
                return x
            }
        }
    """.trimIndent()

    @Test
    fun testBothBranches() {
        val vd = analyse(bothBranches)
        val x = vd.variableInfo("x")
        assertEquals("D:0, A:[1.0.0, 1.1.0, 1=M]", x.assignments().toString())
        assertFalse(x.hasBeenDefined("0"))     // declared, not yet assigned
        assertTrue(x.hasBeenDefined("1.0.0"))  // inside then-branch
        assertTrue(x.hasBeenDefined("1.1.0"))  // inside else-branch
        assertTrue(x.hasBeenDefined("2"))      // after if: both branches assign
    }

    // assigned in one if-branch only -> NOT defined after
    private val oneBranch = """
        package a.b
        class X {
            fun m(c: Boolean) {
                var x: Int
                if (c) {
                    x = 1
                    println(x)
                }
            }
        }
    """.trimIndent()

    @Test
    fun testOneBranch() {
        val vd = analyse(oneBranch)
        val x = vd.variableInfo("x")
        assertEquals("D:0, A:[1.0.0]", x.assignments().toString())
        assertTrue(x.hasBeenDefined("1.0.1"))   // inside then-branch, after the assignment
        assertFalse(x.hasBeenDefined("2"))      // fictitious index after the if: not defined (no else)
    }

    // assigned in do-while body -> defined after (body runs at least once)
    private val doWhile = """
        package a.b
        class X {
            fun m(c: Boolean): Int {
                var x: Int
                do {
                    x = 1
                } while (c)
                return x
            }
        }
    """.trimIndent()

    @Test
    fun testDoWhileBody() {
        val vd = analyse(doWhile)
        val x = vd.variableInfo("x")
        assertEquals("D:0, A:[1.0.0, 1=M]", x.assignments().toString())
        assertTrue(x.hasBeenDefined("2")) // contrast with while: do-body is unconditional, hence a merge
    }

    // assigned in while body -> NOT defined after (body may run zero times)
    private val whileBody = """
        package a.b
        class X {
            fun m(c: Boolean) {
                var x: Int
                while (c) {
                    x = 1
                    println(x)
                }
            }
        }
    """.trimIndent()

    @Test
    fun testWhileBody() {
        val vd = analyse(whileBody)
        val x = vd.variableInfo("x")
        assertEquals("D:0, A:[1.0.0]", x.assignments().toString()) // no merge: conditional loop
        assertTrue(x.hasBeenDefined("1.0.1"))  // inside the body, after the assignment
        assertFalse(x.hasBeenDefined("2"))     // fictitious index after the loop: not defined
    }

    // assigned in try and in all catch clauses -> defined after
    private val tryCatch = """
        package a.b
        class X {
            fun m(n: Int): Int {
                var x: Int
                try {
                    x = n / n
                } catch (e: RuntimeException) {
                    x = 0
                }
                return x
            }
        }
    """.trimIndent()

    @Test
    fun testTryCatch() {
        val vd = analyse(tryCatch)
        val x = vd.variableInfo("x")
        assertEquals("D:0, A:[1.0.0, 1.1.0, 1=M]", x.assignments().toString())
        assertTrue(x.hasBeenDefined("2"))
    }
}
