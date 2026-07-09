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
 * Kotlin port of `modification-prepwork` `TestAssignmentsTry`, cases `test1` and `test2`: a local assigned in
 * both the try body and the catch body has both arms plus their merge (`D:0, A:[1.0.0, 1.1.0, 1=M]`); a
 * finally-only local is defined at its declaration and assigned in the finally arm (`D:1, A:[2.2.0, 2=M]`).
 *
 * Two deliberate deviations from the Java source, neither of which touches the try structure the test targets:
 *  - the trailing `System.out.println("… "+c)` becomes a plain local read (`val u = c`). Kotlin's `println` is a
 *    `kotlin.io` top-level function that `KotlinScan` does not resolve here, so its argument reads are not
 *    attributed; a local read records the post-try read of `c`/`d` faithfully.
 *  - the try body assigns `c = in.charAt(i)`; the Kotlin `in[i]` get-operator form does not track `in` as a read
 *    (same get/set convention documented elsewhere), so the `in` read assertion is omitted. The assignment of
 *    `c` at `1.0.0` — the actual subject — is unaffected.
 *
 * The Java suite's remaining cases have no faithful Kotlin form: try-with-resources becomes a `use { }` call, not
 * a resource header; multi-catch resource re-use has no Kotlin equivalent; and the nested-`for` cases use a
 * C-style `for` whose block structure Kotlin's `for (x in …)` does not reproduce.
 */
class TestAssignmentsTry : CommonKotlinPrep() {

    private fun analyse(input: String): VariableData {
        val x = parse("X.kt", input)
        val m = x.findUniqueMethod("method", 2)
        doMethod(m)
        return VariableDataImpl.of(m)
    }

    private fun v(vd: VariableData, simpleName: String): VariableInfo =
        vd.variableInfoStream().filter { it.variable().simpleName() == simpleName }.findFirst().orElseThrow()

    @Test
    fun test1() {
        val vd = analyse("""
            package a.b
            class X {
                fun method(input: String, i: Int): Char {
                    val c: Char
                    try {
                        c = input[i]
                    } catch (e: IndexOutOfBoundsException) {
                        c = '2'
                    }
                    val u: Char = c
                    return c
                }
            }
        """.trimIndent())
        val c = v(vd, "c")
        // 0: val c, 1: try, 1.0.0: try body, 1.1.0: catch body, 1=M: merge; 2: val u (reads c), 3: return
        assertEquals("D:0, A:[1.0.0, 1.1.0, 1=M]", c.assignments().toString())
        assertEquals("2, 3", c.reads().toString())
    }

    @Test
    fun test2() {
        val vd = analyse("""
            package a.b
            class X {
                fun method(input: String, i: Int): Char {
                    val c: Char
                    val d: Char
                    try {
                        c = input[i]
                    } catch (e: IndexOutOfBoundsException) {
                        c = '2'
                    } finally {
                        d = '9'
                    }
                    val u: Boolean = c == d
                    return c
                }
            }
        """.trimIndent())
        val c = v(vd, "c")
        // 0: val c, 1: val d, 2: try, 2.0.0 try / 2.1.0 catch / 2.2.0 finally, 2=M merge; 3: reads c&d, 4: return
        assertEquals("D:0, A:[2.0.0, 2.1.0, 2=M]", c.assignments().toString())
        assertEquals("3, 4", c.reads().toString())
        val d = v(vd, "d")
        assertEquals("D:1, A:[2.2.0, 2=M]", d.assignments().toString())
        assertEquals("3", d.reads().toString())
    }
}
