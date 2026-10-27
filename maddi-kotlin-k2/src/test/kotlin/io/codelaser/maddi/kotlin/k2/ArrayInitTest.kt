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
 * `val a = IntArray(n) { v }` as the filling loop kotlinc inlines: `new int[n]`, a counter, and `a[i] = v` in a
 * `while`. detekt's `IntArray(size) { return@IntArray 1 }`, `IntArray(size) { return@IntArray it }` and
 * `Array(a.length + 1) { IntArray(b.length + 1) }`. In EXPRESSION position too, on the statement's spine: javalin's
 * `Array(n) { "0" }.joinToString()` fills a temporary first.
 */
class ArrayInitTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("ai/Ai.kt", """
            package ai
            fun a(n: Int): IntArray { val dp = IntArray(n) { return@IntArray 1 }; return dp }
            fun b(n: Int): IntArray { val bt = IntArray(n) { it }; return bt }
            fun c(n: Int, m: Int): Array<IntArray> { val dp = Array(n) { IntArray(m) }; return dp }
            fun d(n: Int): IntArray { val x = IntArray(n) { i -> i * 2 }; return x }
            fun e(n: Int): String { val s = Array(n) { "0" }.joinToString(); return s }
            fun f(n: Int): Int = IntArray(n) { it }.sum()
            fun g(n: Int): Any { return (Array(n) { "x" }) as Any }
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
            e: String[] ${'$'}array4=new String[n]; int ${'$'}i5=0; while(${'$'}i5<${'$'}array4.length){${'$'}array4[${'$'}i5]="0";${'$'}i5++;} String s=ArraysKt.joinToString(${'$'}array4,null,null,null,0,null,null); return s;
            f: int[] ${'$'}array6=new int[n]; int ${'$'}i7=0; while(${'$'}i7<${'$'}array6.length){${'$'}array6[${'$'}i7]=${'$'}i7;${'$'}i7++;} return ArraysKt.sum(${'$'}array6);
            g: String[] ${'$'}array8=new String[n]; int ${'$'}i9=0; while(${'$'}i9<${'$'}array8.length){${'$'}array8[${'$'}i9]="x";${'$'}i9++;} return (Object)${'$'}array8;
            """.trimIndent(), listOf("a", "b", "c", "d", "e", "f", "g").joinToString("\n") { "$it: ${body(it)}" })
    }
}
