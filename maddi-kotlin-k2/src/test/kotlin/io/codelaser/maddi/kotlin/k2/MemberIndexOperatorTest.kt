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
 * `a[i] = v` / `a[i]` through a MEMBER extension operator: detekt's `private operator fun ByteArray.set(c: Char,
 * value: Byte)` declared inside an `object`. On the JVM it is an instance method of that object with the array first,
 * called on the implicit dispatch receiver. Only a top-level extension operator (a facade static) was read: eight of
 * detekt's placeholders.
 */
class MemberIndexOperatorTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("im/Im.kt", """
            package im
            object Esc {
                private val levels = ByteArray(128)
                fun store(c: Char) { levels[c] = 4 }
                fun load(c: Char): Byte = levels[c]
                private operator fun ByteArray.set(c: Char, value: Byte) { this[0] = value }
                private operator fun ByteArray.get(c: Char): Byte = this[0]
            }
            class Holder(val mapping: Map<String, Int>)
            class Safe {
                fun look(h: Holder?, k: String): Int? = h?.mapping[k]
            }
            """.trimIndent() + "\n")
    }

    private fun type(name: String) = types.flatMap { it.recursiveSubTypeStream().toList() }.first { it.simpleName() == name }

    private fun body(name: String): String =
        type("Esc").methods().first { it.name() == name }.methodBody().statements().joinToString(" ")

    @Test
    fun noPlaceholder() {
        val census = PlaceholderCensus.of(types)
        assertEquals(0, census.total, census.dumpLines().joinToString("\n"))
    }

    /** `h?.mapping[k]` is `h?.(mapping[k])`; detekt SleepInsteadOfDelay's shape, resolved through the call's receiver. */
    @Test
    fun safeQualifiedIndex() {
        assertEquals("return h==null?null:h.mapping.get(k);", type("Safe").methods().first { it.name() == "look" }.methodBody().statements().joinToString(" "))
    }

    @Test
    fun theShapes() {
        assertEquals("store: set(this.levels,c,4);\nload: return get(this.levels,c);", "store: " + body("store") + "\nload: " + body("load"))
    }
}
