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
import org.e2immu.language.cst.api.statement.LoopStatement
import org.e2immu.language.cst.api.statement.Statement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Kotlin port of `modification-prepwork` `TestLabeledContinue`: a labelled `continue@outer` to the outer
 * loop suppresses the inner infinite-loop definite-assignment merge. Exercises labelled `while` + Kotlin's
 * `continue@label`, and the analyzer's abrupt-exit reasoning.
 */
class TestLabeledContinue : CommonKotlinPrep() {

    /** The inner (second-encountered) loop statement. */
    private fun findInnerLoop(s: Statement, seenOuter: BooleanArray): Statement? {
        if (s is LoopStatement) {
            if (seenOuter[0]) return s
            seenOuter[0] = true
        }
        for (b in s.subBlocks()) for (sub in b.statements()) findInnerLoop(sub, seenOuter)?.let { return it }
        return null
    }

    private fun innerLoopAssignmentsOfR(input: String): String {
        val x = parse("X.kt", input)
        val m = x.findUniqueMethod("m", 1)
        doMethod(m)
        var inner: Statement? = null
        val seen = booleanArrayOf(false)
        for (s in m.methodBody().statements()) {
            inner = findInnerLoop(s, seen)
            if (inner != null) break
        }
        val vd = VariableDataImpl.of(inner)
        return vd.variableInfoStream().filter { it.variable().simpleName() == "r" }
            .map { it.assignments().toString() }.findFirst().orElseThrow()
    }

    @Test
    fun testContinueOuter() {
        // r=1 can be skipped by 'continue@outer', so r is NOT definitely assigned when the inner loop is
        // left: no '1.0.0=M' merge
        val withContinue = innerLoopAssignmentsOfR(
            """
            package a.b
            class X {
                fun m(cond: Boolean) {
                    var r: Int
                    outer@ while (true) {
                        while (true) {
                            if (cond) continue@outer
                            r = 1
                        }
                    }
                }
            }
            """.trimIndent()
        )
        assertEquals("D:0, A:[1.0.0.0.1]", withContinue)
    }

    @Test
    fun testTrulyInfinite() {
        val noContinue = innerLoopAssignmentsOfR(
            """
            package a.b
            class X {
                fun m(cond: Boolean) {
                    var r: Int
                    outer@ while (true) {
                        while (true) {
                            r = 1
                        }
                    }
                }
            }
            """.trimIndent()
        )
        assertEquals("D:0, A:[1.0.0.0.0, 1.0.0=M]", noContinue)
    }
}
