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

import io.codelaser.maddi.cst.api.element.Element
import io.codelaser.maddi.cst.api.expression.MethodCall
import io.codelaser.maddi.cst.api.statement.LocalVariableCreation
import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * A local function is a local variable holding its function object (`Function1<X, R> g = x -> { … }`), so a call is
 * `g.invoke(…)` and `::g` is `g`. 27 declarations on detekt were placeholders swallowing their whole bodies.
 */
class LocalFunctionTest : KotlinScanTestBase() {

    private val source = """
        class P {
            fun f(xs: List<String>): Int {
                var n = 0
                fun add(s: String) { n += s.hashCode() }
                fun String.twice(): Int = hashCode() * 2
                fun fact(i: Int): Int = if (i <= 1) 1 else i * fact(i - 1)
                for (x in xs) add(x)
                val r: (String) -> Unit = ::add
                r("b")
                return n + "a".twice() + fact(3)
            }
        }
        """.trimIndent() + "\n"

    @Test
    fun aLocalFunctionIsAFunctionValue() {
        val p = KotlinScan(runtime, sourceSet).parse("P.kt", source).first()
        val census = PlaceholderCensus.of(listOf(p))
        assertEquals(0, census.total, census.dumpLines().joinToString("\n"))
        val f = p.findUniqueMethod("f", 1)
        val declared = f.methodBody().statements().filterIsInstance<LocalVariableCreation>().map { it.localVariable().simpleName() }
        assertTrue(declared.containsAll(listOf("add", "twice", "fact")), declared.toString())
        val invokes = mutableListOf<MethodCall>()
        f.methodBody().visit { e: Element -> if (e is MethodCall && e.methodInfo().name() == "invoke") invokes += e; true }
        // add(x), r("b"), "a".twice(), fact(3), and fact(i - 1) inside fact's own body
        assertEquals(5, invokes.size, invokes.toString())
        // the local extension's receiver is the first argument
        val twice = invokes.single { it.`object`().toString() == "twice" }
        assertEquals(1, twice.parameterExpressions().size)
        assertTrue(twice.parameterExpressions()[0] is io.codelaser.maddi.cst.api.expression.StringConstant,
            twice.parameterExpressions().toString())
    }
}
