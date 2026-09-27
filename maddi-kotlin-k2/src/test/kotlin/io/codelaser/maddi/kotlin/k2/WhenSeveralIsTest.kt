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
 * #57: a `when` arm with several `is` conditions kept only the last, as its single type pattern: `is String, is Int`
 * became `case int it`, and a String fell to `else`. Java has no multi-type pattern, so such an arm tests each type.
 */
class WhenSeveralIsTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("ws/Ws.kt", """
            package ws
            class K {
                fun several(o: Any): Int = when (o) { is String, is Int -> 1; else -> 0 }
                fun single(o: Any): Int = when (o) { is Int -> 1; else -> 0 }
                fun mixed(o: Any): Int = when (o) { is String, 5 -> 1; !is Int -> 2; else -> 0 }
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
    fun eachTypeIsTested() {
        // a sole `is T` stays a type pattern; an `is Int` tests Integer, never the primitive
        assertEquals("""
            several: return switch(o){case o instanceof String,o instanceof Integer->{1;}default->{0;}};
            single: return switch(o){case Integer it->{1;}default->{0;}};
            mixed: return switch(o){case o instanceof String,5->{1;}case !(o instanceof Integer)->{2;}default->{0;}};
            """.trimIndent(), listOf("several", "single", "mixed").joinToString("\n") { "$it: ${body(it)}" })
    }
}
