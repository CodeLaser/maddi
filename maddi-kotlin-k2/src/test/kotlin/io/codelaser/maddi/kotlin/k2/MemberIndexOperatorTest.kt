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

    @Test
    fun theShapes() {
        assertEquals("store: set(this.levels,c,4);\nload: return get(this.levels,c);", "store: " + body("store") + "\nload: " + body("load"))
    }
}
