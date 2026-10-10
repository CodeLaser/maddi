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
 * An @InlineOnly stdlib function whose body is a loop has no method to call or to contract: it becomes the chain of
 * public calls computing the same value, each contracted in the kotlin archive (#15).
 */
class InlineOnlyLoopTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("il/Il.kt", """
            package il
            class K {
                fun fnn(l: List<String>): Int? = l.firstNotNullOfOrNull { it.toIntOrNull() }
                fun last(l: List<String>): String? = l.findLast { it.isEmpty() }
                fun total(l: List<String>): Int = l.sumOf { it.hashCode() }
                fun totalL(l: List<String>): Long = l.sumOf { it.hashCode().toLong() }
                fun least(l: List<String>): Int = l.minOf { it.hashCode() }
                fun most(l: List<String>): Double = l.maxOf { it.hashCode().toDouble() }
                fun arr(a: Array<String>?): Array<String> = a.orEmpty()
            }
            """.trimIndent() + "\n")
    }

    private fun body(name: String): String {
        val k = types.flatMap { it.recursiveSubTypeStream().toList() }.first { it.simpleName() == "K" }
        return k.methods().first { it.name() == name }.methodBody().statements().joinToString(" ")
    }

    @Test
    fun noPlaceholder() {
        val census = PlaceholderCensus.of(types)
        assertEquals(0, census.total, census.dumpLines().joinToString("\n"))
    }

    @Test
    fun theChains() {
        assertEquals("""
            fnn: return CollectionsKt.firstOrNull(CollectionsKt.mapNotNull(l,it->StringsKt.toIntOrNull(it)));
            last: return CollectionsKt.lastOrNull(l,it->StringsKt.isEmpty(it));
            total: return CollectionsKt.sumOfInt(CollectionsKt.map(l,it->it.hashCode()));
            totalL: return CollectionsKt.sumOfLong(CollectionsKt.map(l,it->(long)it.hashCode()));
            least: return CollectionsKt.minOrThrow(CollectionsKt.map(l,it->it.hashCode()));
            most: return CollectionsKt.maxOrThrow(CollectionsKt.map(l,it->(double)it.hashCode()));
            arr: return a==null?new String[0]:a;
            """.trimIndent(), listOf("fnn", "last", "total", "totalL", "least", "most", "arr").joinToString("\n") { "$it: ${body(it)}" })
    }
}
