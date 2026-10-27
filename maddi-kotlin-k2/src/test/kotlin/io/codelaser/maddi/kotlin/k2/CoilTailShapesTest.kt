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

import io.codelaser.maddi.cst.api.expression.MethodCall
import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.cst.api.statement.ExpressionAsStatement
import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Four shapes from coil's tail: membership in a floating-point range (`percent in 0.0..1.0`, two comparisons as
 * kotlinc compiles it), arithmetic on a boxed primitive (`pair.second + 1`), a `try` alone in a value-`if` branch, and
 * `MutableList.removeAt(i)`, which is `java.util.List.remove(int)`.
 */
class CoilTailShapesTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("ct/Ct.kt", """
            package ct
            class K {
                fun inRange(p: Double): Boolean = p in 0.0..1.0
                fun outOfRange(p: Double): Boolean = p !in 0.0..1.0
                fun next(pair: Pair<String, Int>): Int = pair.second + 1
                fun size(b: Boolean, s: String): Long {
                    val n = if (b) {
                        try {
                            s.toLong()
                        } catch (_: NumberFormatException) {
                            0L
                        }
                    } else {
                        1L
                    }
                    return n
                }
                fun drop(l: MutableList<String>, i: Int) { l.removeAt(i) }
            }
            """.trimIndent() + "\n")
    }

    private fun type(name: String) = types.flatMap { it.recursiveSubTypeStream().toList() }.first { it.simpleName() == name }

    private fun body(name: String): String =
        type("K").methods().first { it.name() == name }.methodBody().statements().joinToString(" ")

    @Test
    fun noPlaceholder() {
        val census = PlaceholderCensus.of(types)
        assertEquals(0, census.total, census.dumpLines().joinToString("\n"))
    }

    @Test
    fun theShapes() {
        assertEquals("""
            inRange: return 0.0<=p&&p<=1.0;
            outOfRange: return !(0.0<=p&&p<=1.0);
            next: return pair.second+1;
            size: long n; if(b){try{n=StringsKt.toLong(s);}catch(NumberFormatException _){n=0L;}}else{n=1L;} return n;
            drop: l.remove(i);
            """.trimIndent(), listOf("inRange", "outOfRange", "next", "size", "drop").joinToString("\n") { "$it: ${body(it)}" })
    }

    @Test
    fun removeAtIsRemoveInt() {
        val statement = type("K").methods().first { it.name() == "drop" }.methodBody().statements().single()
        val call = (statement as ExpressionAsStatement).expression() as MethodCall
        assertEquals("java.util.List.remove(int)", call.methodInfo().fullyQualifiedName())
    }
}
