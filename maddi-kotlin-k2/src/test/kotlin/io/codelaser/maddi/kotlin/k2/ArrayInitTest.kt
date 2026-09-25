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
 * `val a = IntArray(n) { v }` as the filling loop kotlinc inlines: `new int[n]`, a counter, and `a[i] = v` in a
 * `while`. detekt's `IntArray(size) { return@IntArray 1 }`, `IntArray(size) { return@IntArray it }` and
 * `Array(a.length + 1) { IntArray(b.length + 1) }`.
 */
class ArrayInitTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("ai/Ai.kt", """
            package ai
            fun a(n: Int): IntArray { val dp = IntArray(n) { return@IntArray 1 }; return dp }
            fun b(n: Int): IntArray { val bt = IntArray(n) { it }; return bt }
            fun c(n: Int, m: Int): Array<IntArray> { val dp = Array(n) { IntArray(m) }; return dp }
            fun d(n: Int): IntArray { val x = IntArray(n) { i -> i * 2 }; return x }
            """.trimIndent() + "\n")
    }

    private fun body(name: String): String =
        types.first { it.simpleName() == "AiKt" }.methods().first { it.name() == name }.methodBody().statements().joinToString(" ")

    @Test
    fun noPlaceholder() {
        val census = PlaceholderCensus.of(types)
        assertEquals(0, census.total, census.dumpLines().joinToString("\n"))
    }

    @Test
    fun theShapes() {
        assertEquals("""
            a: int[] dp=new int[n]; int ${'$'}i0=0; while(${'$'}i0<dp.length){dp[${'$'}i0]=1;${'$'}i0++;} return dp;
            b: int[] bt=new int[n]; int ${'$'}i1=0; while(${'$'}i1<bt.length){bt[${'$'}i1]=${'$'}i1;${'$'}i1++;} return bt;
            c: int[][] dp=new int[n][]; int ${'$'}i2=0; while(${'$'}i2<dp.length){dp[${'$'}i2]=new int[m];${'$'}i2++;} return dp;
            d: int[] x=new int[n]; int ${'$'}i3=0; while(${'$'}i3<x.length){x[${'$'}i3]=${'$'}i3*2;${'$'}i3++;} return x;
            """.trimIndent(), listOf("a", "b", "c", "d").joinToString("\n") { "$it: ${body(it)}" })
    }
}
