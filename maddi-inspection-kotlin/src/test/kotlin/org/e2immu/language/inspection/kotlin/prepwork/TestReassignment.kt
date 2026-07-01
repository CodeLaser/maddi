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
import org.junit.jupiter.api.Test

/**
 * Kotlin port of `modification-prepwork` `TestReassignment` (package `…prepwork.variable`). Same method
 * bodies, expressed in Kotlin; the assignment-string oracles are copied verbatim from the Java test.
 */
class TestReassignment : CommonKotlinPrep() {

    // Java INPUT1: `int k = i*j; if(k<0) return -k; k = -k; if(k%2==0) return k; k = k+1; return k/2;`
    private val input1 = """
        class X {
            fun method(i: Int, j: Int): Int {
                var k = i * j
                if (k < 0) return -k
                k = -k
                if (k % 2 == 0) return k
                k = k + 1
                return k / 2
            }
        }
    """.trimIndent()

    @Test
    fun test1() {
        val x = parse("X.kt", input1)
        val method = x.findUniqueMethod("method", 2)
        doMethod(method)

        val s0 = method.methodBody().statements()[0]
        assertEquals("D:0, A:[0]", VariableDataImpl.of(s0).variableInfo("k").assignments().toString())

        val s2 = method.methodBody().statements()[2]
        assertEquals("D:0, A:[0, 2]", VariableDataImpl.of(s2).variableInfo("k").assignments().toString())

        val s5 = method.methodBody().statements()[5]
        assertEquals("D:0, A:[0, 2, 4]", VariableDataImpl.of(s5).variableInfo("k").assignments().toString())
    }

    // Java INPUT2: `int k; if(i<0){k=j+i;} else {k=j-i;} if(k%2==0){k+=1; return k;} return k/2;`
    private val input2 = """
        class X {
            fun method(i: Int, j: Int): Int {
                var k: Int
                if (i < 0) {
                    k = j + i
                } else {
                    k = j - i
                }
                if (k % 2 == 0) {
                    k += 1
                    return k
                }
                return k / 2
            }
        }
    """.trimIndent()

    @Test
    fun test2() {
        val x = parse("X.kt", input2)
        val method = x.findUniqueMethod("method", 2)
        doMethod(method)

        val s1 = method.methodBody().statements()[1]
        assertEquals("D:0, A:[1.0.0, 1.1.0, 1=M]", VariableDataImpl.of(s1).variableInfo("k").assignments().toString())

        val s200 = method.methodBody().statements()[2].block().statements()[0]
        assertEquals("D:0, A:[1.0.0, 1.1.0, 1=M, 2.0.0]", VariableDataImpl.of(s200).variableInfo("k").assignments().toString())

        val s3 = method.methodBody().statements()[3]
        assertEquals("D:0, A:[1.0.0, 1.1.0, 1=M, 2.0.0, 2=M]", VariableDataImpl.of(s3).variableInfo("k").assignments().toString())
    }
}
