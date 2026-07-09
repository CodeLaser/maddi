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
 * Kotlin port of `modification-prepwork` `TestAssignmentsTryFinally`, `return`-in-try-with-`finally` case: the
 * finally reassigns `x`, so try body (1.0.0), finally body (1.1.0) and the merge (1=M) all appear, and the
 * return variable is assigned at the `return x` inside the try (1.0.1) plus the finally merge.
 *
 * The Java suite's other two cases are Kotlin-inapplicable: multi-catch (`catch (A | B e)`) has no Kotlin form,
 * and the labeled-break case uses a C-style `for (int i = 0; ...)` whose block structure Kotlin's `for (i in …)`
 * does not reproduce.
 */
class TestAssignmentsTryFinally : CommonKotlinPrep() {

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

    private val returnInTryFinally = """
        package a.b
        class X {
            fun m(n: Int): Int {
                var x = 0
                try {
                    x = n / n
                    return x
                } finally {
                    x = -1
                }
            }
        }
    """.trimIndent()

    @Test
    fun testReturnInTryFinally() {
        val vd = analyse(returnInTryFinally)
        val x = v(vd, "x")
        // 0: init, 1.0.0: try body, 1.1.0: finally body, 1=M: merge
        assertEquals("D:0, A:[0, 1.0.0, 1.1.0, 1=M]", x.assignments().toString())
        assertEquals("1.0.1", x.reads().toString()) // 'return x' inside the try
        assertEquals("D:-, A:[1.0.1, 1=M]", rv(vd).assignments().toString())
    }
}
