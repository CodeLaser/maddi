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

import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import io.codelaser.maddi.cst.api.expression.MethodCall
import io.codelaser.maddi.cst.api.info.TypeInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * <b>An extension property is not a member of the receiver's type</b>, so neither the field lookup nor the
 * accessor lookup could ever find one: `o.doubled` failed for a property declared in the SAME FILE, and
 * `c.java` / `s.lastIndex` for every library one. Kotlin compiles it to a static getter on a facade, exactly
 * as it compiles an extension function to a static function, so it resolves the same way.
 *
 * ⚠ The library half needed the facade loader to carry property getters at all: it was built from a package's
 * FUNCTIONS, so `getLastIndex` was not on it to be found.
 */
class ExtensionPropertyTest : KotlinScanTestBase() {

    private fun callee(types: List<TypeInfo>, method: String): String {
        val found = mutableListOf<String>()
        types.single { it.simpleName() == "P" }.findUniqueMethod(method, 1).methodBody().visit { e ->
            if (e is MethodCall) found.add(e.methodInfo().fullyQualifiedName())
            true
        }
        return found.single()
    }

    @Test
    fun aSourceExtensionPropertyResolvesToItsFacadeGetter() {
        val types = KotlinScan(runtime, sourceSet).parse("P.kt", """
            class Own { val x: Int = 1 }
            val Own.doubled: Int get() = x * 2
            class P { fun c(o: Own) = o.doubled }
            """.trimIndent() + "\n")
        assertEquals(0, PlaceholderCensus.of(types).total, PlaceholderCensus.of(types).byKind.toString())
        assertEquals("PKt.getDoubled(Own)", callee(types, "c"))
    }

    @Test
    fun aLibraryExtensionPropertyResolvesToItsFacadeGetter() {
        val types = KotlinScan(runtime, sourceSet).parse("P.kt", """
            class P {
                fun a(s: String) = s.lastIndex
                fun b(x: List<String>) = x.indices
            }
            """.trimIndent() + "\n")
        assertEquals(0, PlaceholderCensus.of(types).total, PlaceholderCensus.of(types).byKind.toString())
        assertTrue(callee(types, "a").endsWith("getLastIndex(CharSequence)"), callee(types, "a"))
        assertTrue(callee(types, "b").endsWith("getIndices(java.util.Collection)"), callee(types, "b"))
    }

    /** ⭐ The control: a member property is still a member, not a facade call. */
    @Test
    fun aMemberPropertyIsUnaffected() {
        val types = KotlinScan(runtime, sourceSet).parse("P.kt", """
            class Own { val x: Int = 1 }
            class P { fun c(o: Own) = o.x }
            """.trimIndent() + "\n")
        assertEquals(0, PlaceholderCensus.of(types).total)
        val found = mutableListOf<String>()
        types.single { it.simpleName() == "P" }.findUniqueMethod("c", 1).methodBody().visit { e ->
            if (e is MethodCall) found.add(e.methodInfo().fullyQualifiedName())
            true
        }
        assertEquals(listOf<String>(), found, "a member property read is a field access, not a call")
    }
}
