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
