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

import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * #56: named arguments were passed in parameter order with no temporaries, so `namedOrder(b = n++, a = n++)`
 * evaluated `a` first and each parameter got the other's value. They are now bound in the order written.
 */
class NamedArgumentOrderTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("na/Na.kt", """
            package na
            class K {
                var n = 0
                fun namedOrder(a: Int, b: Int): Int = a - b
                fun swapped(): Int = namedOrder(b = n++, a = n++)
                fun inOrder(): Int = namedOrder(a = n++, b = n++)
                fun stable(x: Int, y: Int): Int = namedOrder(b = x, a = y)
                fun statement() { namedOrder(b = n++, a = 1) }
            }
            """.trimIndent() + "\n")
    }

    private fun body(name: String): String = types.first { it.simpleName() == "K" }.methods()
        .first { it.name() == name }.methodBody().statements().joinToString(" ")

    @Test
    fun noPlaceholder() {
        val census = PlaceholderCensus.of(types)
        assertEquals(0, census.total, census.dumpLines().joinToString("\n"))
    }

    @Test
    fun argumentsEvaluateInTheOrderWritten() {
        // in order, or all stable: nothing to bind
        assertEquals("""
            swapped: int ${'$'}arg0=this.n++; int ${'$'}arg1=this.n++; return namedOrder(${'$'}arg1,${'$'}arg0);
            inOrder: return namedOrder(this.n++,this.n++);
            stable: return namedOrder(y,x);
            statement: int ${'$'}arg2=this.n++; int ${'$'}arg3=1; namedOrder(${'$'}arg3,${'$'}arg2);
            """.trimIndent(), listOf("swapped", "inOrder", "stable", "statement").joinToString("\n") { "$it: ${body(it)}" })
    }
}
