/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.codelaser.maddi.kotlin.k2

import io.codelaser.maddi.kotlin.api.PlaceholderCensus
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
 * ⚠ A block of SEVERAL statements is a different problem: representing it as an expression needs a
 * temporary and a statement context. Where the statement supplies both — a local declaration — it is now
 * lowered (see `StatementAsValueTest`); anywhere else it still keeps a placeholder that NAMES the shape.
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

    /**
     * ⭐ The control: a multi-statement block that is NOT a local's initializer — here an argument — has no
     * statement context to hoist a temporary into, so it keeps a placeholder, and one that names the shape.
     * (As the initializer of a `val` it is lowered instead: `StatementAsValueTest`.)
     */
    @Test
    fun aMultiStatementBlockOutsideADeclarationKeepsANamedPlaceholder() {
        val types = KotlinScan(runtime, sourceSet).parse("P.kt", """
            class P {
                fun g(i: Int): Int = i
                fun f(c: Boolean): Int = g(if (c) { val a = 1; a + 1 } else { 2 })
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
