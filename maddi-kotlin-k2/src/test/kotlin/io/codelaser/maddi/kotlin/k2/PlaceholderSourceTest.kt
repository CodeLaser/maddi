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

import io.codelaser.maddi.cst.api.expression.EmptyExpression
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * A placeholder for code the front end does not convert keeps the range of that code. The code is not in the CST,
 * so its range is the only trace of it: a consumer that must know every call spelled in the source (jfocus's rename
 * clash check) scans those ranges.
 *
 * ⚠ This used to pin the SWALLOW as well — an operator whose operand is unresolved replaces both operands, so the
 * outer range has to cover the inner one. The fixture no longer produces that shape (`hashCode() + 1` converts
 * around the class literal rather than over it), and the swallow is now measured where it is bigger than a
 * fixture: fixing one on a corpus REVEALS the holes it hid, accounted site by site in the campaign's
 * kotlin-gap-analysis.
 *
 * ⚠ The unconverted operand has had to move twice, which is the healthy direction: it was a library extension
 * call until maddi#43 converted those, then `s.length` until `bootstrapString` started loading String's
 * PROPERTIES. It is now `String::class`, a class literal, which the converter's dispatch still has no arm for.
 * The test is about the RANGE mechanism, not about which construct is missing.
 */
class PlaceholderSourceTest : KotlinScanTestBase() {

    @Test
    fun anUnconvertedExpressionKeepsItsRange() {
        val types = KotlinScan(runtime, sourceSet).parse("u/U.kt", """
            package u
            inline fun <reified T> use(): Int = T::class.hashCode() + 1
            """.trimIndent() + "\n")
        val use = types.single { it.simpleName() == "UKt" }.methods().single { it.name() == "use" }
        val placeholders = mutableListOf<EmptyExpression>()
        use.methodBody().visit { e -> if (e is EmptyExpression && e.msg()?.startsWith("k2-") == true) placeholders += e; true }
        assertEquals(1, placeholders.size, placeholders.map { it.msg() }.toString())
        val s = placeholders.single().source()
        // `T::class`, columns 37-44 of line 2: the placeholder's range is the construct it stands for
        // (a reified type parameter's class literal: no Java spelling, so still a placeholder)
        assertEquals(listOf(2, 37, 2, 44), listOf(s.beginLine(), s.beginPos(), s.endLine(), s.endPos()),
            placeholders.single().msg())
    }
}
