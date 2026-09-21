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

import io.codelaser.maddi.cst.api.expression.BinaryOperator
import io.codelaser.maddi.cst.api.expression.MethodCall
import io.codelaser.maddi.cst.api.statement.ReturnStatement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * ⛔ Parentheses were not in the expression dispatch at all, so `(a + b).toString()` became a placeholder —
 * and a placeholder swallows everything inside it, so the addition, its operands and any call within went
 * with it. The corpus census named this the day it existed: **349 of detekt's 6,057** and **20 of coil's
 * 437**, the second-biggest kind on either corpus, for something that is not a language feature.
 *
 * Kotlin's parentheses carry no semantics beyond grouping, which the CST expresses by its shape, so the
 * inner expression is the result — what javac's parser does with a `JCParens`.
 */
class ParenthesizedExpressionTest : KotlinScanTestBase() {

    @Test
    fun parenthesesAreNotAHole() {
        val m = KotlinScan(runtime, sourceSet).parse(
            "Q.kt",
            """
            class Q {
                fun add(a: Int, b: Int): Int = a + b
                fun render(a: Int, b: Int): Int = (this).add((a + b), b)
            }
            """.trimIndent() + "\n"
        ).first()

        val body = (m.findUniqueMethod("render", 2).methodBody().statements().first() as ReturnStatement)
            .expression()
        assertTrue(body is MethodCall, "expected the call to survive, got ${body.javaClass}")
        // ⭐ the grouped ARGUMENT survives too: a placeholder used to eat the whole subtree it stood for
        val argument = (body as MethodCall).parameterExpressions().first()
        assertTrue(argument is BinaryOperator, "expected (a + b) as the argument, got ${argument.javaClass}")
        assertEquals(0, PlaceholderCensus.of(listOf(m)).total, PlaceholderCensus.of(listOf(m)).byKind.toString())
    }

    /** Nested and redundant parentheses collapse the same way, and a census over them stays at zero. */
    @Test
    fun nestedParenthesesCollapse() {
        val m = KotlinScan(runtime, sourceSet).parse(
            "R.kt",
            """
            class R {
                fun f(a: Int, b: Int, c: Int): Int = ((a + b)) * (c)
            }
            """.trimIndent() + "\n"
        ).first()
        assertEquals(0, PlaceholderCensus.of(listOf(m)).total, PlaceholderCensus.of(listOf(m)).byKind.toString())
    }
}
