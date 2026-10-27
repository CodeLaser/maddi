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
        // a sole `is T` stays a type pattern; an `is Int` tests Integer, never the primitive; a when used as a value
        // yields its arms' values (WhenValueTest), so an arm's sole expression is the arm's value, not a block
        assertEquals("""
            several: return switch(o){case o instanceof String,o instanceof Integer->1;default->0;};
            single: return switch(o){case Integer it->1;default->0;};
            mixed: return switch(o){case o instanceof String,5->1;case !(o instanceof Integer)->2;default->0;};
            """.trimIndent(), listOf("several", "single", "mixed").joinToString("\n") { "$it: ${body(it)}" })
    }
}
