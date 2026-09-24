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
import io.codelaser.maddi.cst.api.info.MethodInfo
import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * `E.entries` is the static `E.getEntries(): EnumEntries<E>` kotlinc gives every Kotlin enum. K2 models it as a
 * static property, neither the source nor the library enum model had the method, and the qualified form was read
 * on the enum's companion (or on the class name, typed `Unit`): 20 of detekt's placeholders.
 */
class EnumEntriesTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("ee/Ee.kt", """
            package ee
            enum class Sev { LOW, HIGH; companion object { fun count(): Int = entries.size } }
            class K {
                fun source(): Int = Sev.entries.size
                fun library(): Int = LazyThreadSafetyMode.entries.size
            }
            """.trimIndent() + "\n")
    }

    private fun type(name: String) = types.flatMap { it.recursiveSubTypeStream().toList() }.first { it.simpleName() == name }

    private fun getEntries(method: MethodInfo): MethodCall {
        val calls = mutableListOf<MethodCall>()
        method.methodBody().visit { e: Element ->
            if (e is MethodCall && e.methodInfo().name() == "getEntries") calls += e
            true
        }
        return calls.single()
    }

    @Test
    fun aSourceEnumHasTheStaticGetter() {
        assertEquals(0, PlaceholderCensus.of(types).total, PlaceholderCensus.of(types).dumpLines().joinToString("\n"))
        val call = getEntries(type("K").findUniqueMethod("source", 0))
        assertEquals("Sev", call.methodInfo().typeInfo().simpleName())
        assertTrue(call.methodInfo().isStatic)
        assertEquals("EnumEntries", call.methodInfo().returnType().typeInfo().simpleName())
    }

    @Test
    fun aBareEntriesInsideTheEnum() {
        assertEquals("Sev", getEntries(type("Companion").findUniqueMethod("count", 0)).methodInfo().typeInfo().simpleName())
    }

    @Test
    fun aLibraryEnumHasItToo() {
        val call = getEntries(type("K").findUniqueMethod("library", 0))
        assertEquals("LazyThreadSafetyMode", call.methodInfo().typeInfo().simpleName())
    }
}
