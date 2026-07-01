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

import org.e2immu.analyzer.modification.prepwork.escape.ComputeAlwaysEscapes
import org.e2immu.language.cst.api.statement.IfElseStatement
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Kotlin port of `modification-prepwork` `escape/TestAlwaysEscapes`: control-flow escape analysis
 * (`Statement.alwaysEscapes()`). A different analyzer dimension from the variable-data ports. Java's
 * C-style `for(int i=0; ; i++)` becomes Kotlin `while (true)`; the escape SEMANTICS (does control always
 * leave the method here?) are what the test asserts. `in` is a Kotlin keyword, so the param is `input`.
 */
class TestAlwaysEscapes : CommonKotlinPrep() {

    private val input1 = """
        package a.b
        class X {
            fun method1(input: String): Int {
                val i = input.length
                return i
            }
            fun method2(input: String): Int {
                if (input.isEmpty()) {
                    return 1
                } else {
                    throw UnsupportedOperationException()
                }
            }
            fun method3(input: String): Int {
                if (input.isEmpty()) {
                    return 1
                } else {
                    val n = input.length
                }
                return 0
            }
        }
    """.trimIndent()

    @Test
    fun test1() {
        val x = parse("X.kt", input1)

        val m1 = x.findUniqueMethod("method1", 1)
        ComputeAlwaysEscapes.go(m1)
        assertFalse(m1.methodBody().statements()[0].alwaysEscapes())
        assertTrue(m1.methodBody().statements()[1].alwaysEscapes())

        val m2 = x.findUniqueMethod("method2", 1)
        ComputeAlwaysEscapes.go(m2)
        val m2s0 = m2.methodBody().statements()[0]
        assertTrue(m2s0.block().statements()[0].alwaysEscapes())
        assertTrue((m2s0 as IfElseStatement).elseBlock().statements()[0].alwaysEscapes())
        assertTrue(m2s0.alwaysEscapes())

        val m3 = x.findUniqueMethod("method3", 1)
        ComputeAlwaysEscapes.go(m3)
        val m3s0 = m3.methodBody().statements()[0]
        assertTrue(m3s0.block().statements()[0].alwaysEscapes())
        assertFalse((m3s0 as IfElseStatement).elseBlock().statements()[0].alwaysEscapes())
        assertFalse(m3s0.alwaysEscapes())
    }

    private val input2 = """
        package a.b
        abstract class X {
            fun method1(input: String): Int { while (true) { } }
            fun method2(input: String): Int { while (true) { break }; return 1 }
            fun method3(input: String): Int { while (true) { val n = input.length; return 0 } }
            abstract fun read(): Int
            abstract fun write(i: Int)
            fun method4() {
                while (true) {
                    val r = read()
                    if (r == -1) { break }
                    write(r)
                }
            }
            fun method5() {
                while (true) {
                    val r = read()
                    if (r == -1) { return }
                    write(r)
                }
            }
        }
    """.trimIndent()

    @Test
    fun test2() {
        val x = parse("X.kt", input2)

        val m1 = x.findUniqueMethod("method1", 1)
        ComputeAlwaysEscapes.go(m1)
        assertTrue(m1.methodBody().statements()[0].alwaysEscapes()) // infinite loop, no break

        val m2 = x.findUniqueMethod("method2", 1)
        ComputeAlwaysEscapes.go(m2)
        val m2s0 = m2.methodBody().statements()[0]
        assertFalse(m2s0.block().statements()[0].alwaysEscapes()) // the `break` does not escape the method
        assertFalse(m2s0.alwaysEscapes())                          // loop can be left via the break

        val m3 = x.findUniqueMethod("method3", 1)
        ComputeAlwaysEscapes.go(m3)
        assertTrue(m3.methodBody().statements()[0].alwaysEscapes()) // loop body always returns

        val m4 = x.findUniqueMethod("method4", 0)
        ComputeAlwaysEscapes.go(m4)
        val m4s0 = m4.methodBody().statements()[0]
        assertFalse(m4s0.block().statements()[1].alwaysEscapes()) // `if (r==-1) break` (one-armed)
        assertFalse(m4s0.alwaysEscapes())

        val m5 = x.findUniqueMethod("method5", 0)
        ComputeAlwaysEscapes.go(m5)
        assertTrue(m5.methodBody().statements()[0].alwaysEscapes()) // infinite loop leaves only via return
    }
}
