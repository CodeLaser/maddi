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

import io.codelaser.maddi.cst.api.element.Element
import io.codelaser.maddi.cst.api.expression.MethodCall
import io.codelaser.maddi.cst.api.expression.TypeExpression
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
            enum class Sev {
                LOW, HIGH;
                fun next(): Sev = entries[(ordinal + 1) % entries.size]
                companion object { fun count(): Int = entries.size }
            }
            class K {
                fun source(): Int = Sev.entries.size
                fun library(): Int = LazyThreadSafetyMode.entries.size
            }
            """.trimIndent() + "\n")
    }

    private fun type(name: String) = types.flatMap { it.recursiveSubTypeStream().toList() }.first { it.simpleName() == name }

    private fun getEntries(method: MethodInfo): MethodCall = allGetEntries(method).single()

    private fun allGetEntries(method: MethodInfo): List<MethodCall> {
        val calls = mutableListOf<MethodCall>()
        method.methodBody().visit { e: Element ->
            if (e is MethodCall && e.methodInfo().name() == "getEntries") calls += e
            true
        }
        return calls
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

    /* in a member of the enum itself: on the type, not on `this`, which prep refused (maddi-kotlin-demo Color.next()) */
    @Test
    fun aBareEntriesInAMemberOfTheEnum() {
        val calls = allGetEntries(type("Sev").findUniqueMethod("next", 0))
        assertEquals(2, calls.size)
        calls.forEach { assertTrue(it.`object`() is TypeExpression, "read on the type, not on ${it.`object`()}") }
    }

    @Test
    fun aLibraryEnumHasItToo() {
        val call = getEntries(type("K").findUniqueMethod("library", 0))
        assertEquals("LazyThreadSafetyMode", call.methodInfo().typeInfo().simpleName())
    }
}
