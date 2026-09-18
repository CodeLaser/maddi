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

import io.codelaser.maddi.cst.api.expression.EmptyExpression
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * A placeholder for code the front end does not convert keeps the range of that code. The code is not in the CST,
 * so its range is the only trace of it: a consumer that must know every call spelled in the source (jfocus's rename
 * clash check) scans those ranges. An operator whose operand is unresolved replaces both operands, so the inner
 * placeholder is gone and the outer range must cover it.
 *
 * The unconverted operand here is `String.length`: `kotlin.String` maps to the predefined `java.lang.String`, whose
 * property accesses do not resolve to its `length()` (the other half of bootstrapString). It used to be a library
 * extension call, which maddi#43 converts.
 */
class PlaceholderSourceTest : KotlinScanTestBase() {

    @Test
    fun anUnconvertedExpressionKeepsItsRange() {
        val types = KotlinScan(runtime, sourceSet).parse("u/U.kt", """
            package u
            fun use(s: String) = s.length + 1
            """.trimIndent() + "\n")
        val use = types.single { it.simpleName() == "UKt" }.methods().single { it.name() == "use" }
        val placeholders = mutableListOf<EmptyExpression>()
        use.methodBody().visit { e -> if (e is EmptyExpression && e.msg()?.startsWith("k2-") == true) placeholders += e; true }
        assertEquals(1, placeholders.size, placeholders.map { it.msg() }.toString())
        val s = placeholders.single().source()
        // `s.length + 1`, columns 22-33 of line 2
        assertEquals(listOf(2, 22, 2, 33), listOf(s.beginLine(), s.beginPos(), s.endLine(), s.endPos()),
            placeholders.single().msg())
    }
}
