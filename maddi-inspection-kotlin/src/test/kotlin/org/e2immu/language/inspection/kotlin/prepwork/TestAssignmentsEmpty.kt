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

import org.e2immu.analyzer.modification.prepwork.variable.Stage
import org.e2immu.analyzer.modification.prepwork.variable.VariableData
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Kotlin port of `modification-prepwork` `TestAssignmentsEmpty`: a for-each whose body declares a local and
 * contains an empty `if` block. The merge of the empty block still tracks the local `n`. (The only deviation
 * from the Java oracle is the parameter type spelling: a Kotlin `Array<String>` renders as `kotlin.Array` in the
 * method FQN where Java has `String[]`; the VariableData structure is identical.)
 */
class TestAssignmentsEmpty : CommonKotlinPrep() {

    private val input1 = """
        package a.b
        class X {
            fun method(strings: Array<String>) {
                for (s in strings) {
                    val n = s.length
                    if (n > 0) { }
                }
            }
        }
    """.trimIndent()

    @Test
    fun test1() {
        val x = parse("X.kt", input1)
        val method = x.findUniqueMethod("method", 1)
        doMethod(method)
        val vdMethod: VariableData = VariableDataImpl.of(method)
        assertNotNull(vdMethod)

        assertEquals("a.b.X.method(kotlin.Array):0:strings, s", vdMethod.knownVariableNamesToString())

        val s0 = method.methodBody().statements().first()
        val vd0 = VariableDataImpl.of(s0)
        assertEquals("a.b.X.method(kotlin.Array):0:strings, s", vd0.knownVariableNamesToString())

        val s001 = s0.block().statements()[1]
        val vd001 = VariableDataImpl.of(s001)
        assertEquals("a.b.X.method(kotlin.Array):0:strings, n, s", vd001.knownVariableNamesToString())
        val vicN = vd001.variableInfoContainerOrNull("n")
        assertTrue(vicN.hasMerge())
        assertTrue(vicN.hasEvaluation())
        assertSame(vicN.best(Stage.EVALUATION), vicN.best(Stage.MERGE))
    }
}
