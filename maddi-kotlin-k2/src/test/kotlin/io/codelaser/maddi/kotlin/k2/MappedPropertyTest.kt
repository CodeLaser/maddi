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
