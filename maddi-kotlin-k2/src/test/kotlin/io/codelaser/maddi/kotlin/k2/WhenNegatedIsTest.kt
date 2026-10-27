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
import io.codelaser.maddi.cst.api.expression.InstanceOf
import io.codelaser.maddi.cst.api.expression.SwitchExpression
import io.codelaser.maddi.cst.api.expression.UnaryOperator
import io.codelaser.maddi.cst.api.statement.ReturnStatement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * ⛔ A `!is T` arm used to be DROPPED: not representable as a type pattern, it was left with no pattern and
 * no condition, which reads downstream as an arm that is always taken. Unlike a `k2-…` placeholder it left
 * no trace at all — not in the tree, not in a census, not in a range a consumer could re-read.
 *
 * It is the switch-entry spelling of `o !is T`, which the expression path already converts, so it becomes the
 * same negated [InstanceOf].
 */
class WhenNegatedIsTest : KotlinScanTestBase() {

    @Test
    fun aNegatedIsArmIsAConditionAndNotSilence() {
        val m = KotlinScan(runtime, sourceSet).parse(
            "N.kt",
            """
            class N {
                fun describe(o: Any): String = when (o) {
                    !is String -> "not a string"
                    else -> "a string"
                }
            }
            """.trimIndent() + "\n"
        ).first()

        val sw = (m.findUniqueMethod("describe", 1).methodBody().statements().first() as ReturnStatement)
            .expression() as SwitchExpression
        val entry = sw.entries()[0]
        assertEquals(1, entry.conditions().size, "the arm must carry a condition, not nothing")
        val condition = entry.conditions().first()
        assertTrue(condition is UnaryOperator, "expected a logical not, got ${condition.javaClass}")
        assertTrue((condition as UnaryOperator).expression() is InstanceOf,
            "expected !(o instanceof String), got ${condition.expression().javaClass}")

        // ⭐ and it is a real conversion, not a marked hole: the census stays at zero
        assertEquals(0, PlaceholderCensus.of(listOf(m)).total)
    }

    /** The positive arm is unchanged: `is T` remains a type pattern, which is what the analyzer reads. */
    @Test
    fun aPlainIsArmIsStillATypePattern() {
        val m = KotlinScan(runtime, sourceSet).parse(
            "P.kt",
            """
            class P {
                fun describe(o: Any): String = when (o) {
                    is String -> "a string"
                    else -> "other"
                }
            }
            """.trimIndent() + "\n"
        ).first()

        val sw = (m.findUniqueMethod("describe", 1).methodBody().statements().first() as ReturnStatement)
            .expression() as SwitchExpression
        val entry = sw.entries()[0]
        assertTrue(entry.conditions().isEmpty())
        assertEquals(runtime.stringParameterizedType(), entry.patternVariable().localVariable().parameterizedType())
    }
}
