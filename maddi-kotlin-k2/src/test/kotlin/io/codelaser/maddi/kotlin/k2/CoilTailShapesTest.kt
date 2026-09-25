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
            size: long n; if(b){try{n=StringsKt__StringNumberConversionsJVMKt.toLong(s);}catch(NumberFormatException _){n=0L;}}else{n=1L;} return n;
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
