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

import io.codelaser.maddi.cst.api.element.Element
import io.codelaser.maddi.cst.api.expression.MethodCall
import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * `map.keys` and `map.entries` are `keySet()` and `entrySet()` on the JVM: kotlinc maps the property of the Kotlin
 * built-in to the Java method, and the getter-name rule finds neither. 40 of detekt's unresolved accesses.
 */
class MappedPropertyTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("mp/Mp.kt", """
            package mp
            class K {
                fun explicit(m: Map<String, Int>): Int = m.keys.size + m.entries.size
                fun implicit(m: Map<String, Int>): Int = with(m) { keys.size }
            }
            """.trimIndent() + "\n")
    }

    private fun calledIn(method: String): List<String> {
        val names = mutableListOf<String>()
        types.first { it.simpleName() == "K" }.findUniqueMethod(method, 1).methodBody().visit { e: Element ->
            if (e is MethodCall) names += e.methodInfo().typeInfo().simpleName() + "." + e.methodInfo().name()
            true
        }
        return names
    }

    @Test
    fun keysAndEntriesAreTheJavaMethods() {
        assertEquals(0, PlaceholderCensus.of(types).total, PlaceholderCensus.of(types).dumpLines().joinToString("\n"))
        val explicit = calledIn("explicit")
        assertEquals(listOf("Map.keySet", "Map.entrySet"), explicit.filter { it.startsWith("Map.") }, "$explicit")
    }

    @Test
    fun onAnImplicitReceiverToo() {
        val implicit = calledIn("implicit")
        assertEquals(listOf("Map.keySet"), implicit.filter { it.startsWith("Map.") }, "$implicit")
    }
}
