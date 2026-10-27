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
