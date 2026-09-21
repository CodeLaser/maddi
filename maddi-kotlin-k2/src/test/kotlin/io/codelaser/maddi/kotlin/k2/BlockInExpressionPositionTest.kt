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
package io.codelaser.maddi.kotlin.k2

import io.codelaser.maddi.cst.api.expression.InlineConditional
import io.codelaser.maddi.cst.api.statement.LocalVariableCreation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * `val v = if (c) { a() } else { b() }` is ONE expression to Kotlin and TWO blocks to the PSI, and the
 * converter's expression dispatch had no arm for a block — so it produced two placeholders per if. At 287
 * sites it was the biggest construct kind on detekt.
 *
 * ⚠ A block of SEVERAL statements is a different problem and still keeps a placeholder: representing it as
 * an expression needs a temporary and a statement context — the lowering `convertTry(returning = true)`
 * performs — which the expression path does not have. The placeholder now says which case it is.
 */
class BlockInExpressionPositionTest : KotlinScanTestBase() {

    @Test
    fun aSingleExpressionBlockIsThatExpression() {
        val types = KotlinScan(runtime, sourceSet).parse("P.kt", """
            class P {
                fun f(c: Boolean): Int { val v = if (c) { 1 } else { 2 }; return v }
            }
            """.trimIndent() + "\n")
        assertEquals(0, PlaceholderCensus.of(types).total, PlaceholderCensus.of(types).byKind.toString())
        val declaration = types.first().findUniqueMethod("f", 1).methodBody().statements().first()
        val initializer = (declaration as LocalVariableCreation).localVariable().assignmentExpression()
        assertTrue(initializer is InlineConditional, "expected `c ? 1 : 2`, got ${initializer.javaClass}")
    }

    /** ⭐ The control: a block that is not one expression keeps a placeholder, and one that NAMES the shape. */
    @Test
    fun aMultiStatementBlockKeepsANamedPlaceholder() {
        val types = KotlinScan(runtime, sourceSet).parse("P.kt", """
            class P {
                fun f(c: Boolean): Int { val v = if (c) { val a = 1; a + 1 } else { 2 }; return v }
            }
            """.trimIndent() + "\n")
        val census = PlaceholderCensus.of(types)
        assertEquals(1, census.total, census.byKind.toString())
        assertEquals(setOf("k2-block-not-a-single-expression"), census.byKind.keys)
    }

    /** A block in STATEMENT position was never affected: that is `convertBlock`'s business, not this arm's. */
    @Test
    fun aBlockInStatementPositionIsUnchanged() {
        val types = KotlinScan(runtime, sourceSet).parse("P.kt", """
            class P {
                fun f(c: Boolean): Int { var v = 0; if (c) { v = 1; v += 1 } else { v = 2 }; return v }
            }
            """.trimIndent() + "\n")
        assertEquals(0, PlaceholderCensus.of(types).total, PlaceholderCensus.of(types).byKind.toString())
    }
}
