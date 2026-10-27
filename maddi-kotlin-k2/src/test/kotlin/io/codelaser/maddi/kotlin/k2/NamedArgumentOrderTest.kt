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
