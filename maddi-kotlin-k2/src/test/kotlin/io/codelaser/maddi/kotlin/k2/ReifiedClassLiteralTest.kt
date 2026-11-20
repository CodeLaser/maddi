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

import io.codelaser.maddi.cst.api.expression.ClassExpression
import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * <b>`T::class` on a reified type parameter</b> is, in the inline function's own body, the class of T's erasure:
 * what kotlinc compiles there (each call site gets a copy with the real type). It was a placeholder (Exposed: 19).
 */
class ReifiedClassLiteralTest : KotlinScanTestBase() {

    private fun parse(source: String): List<TypeInfo> = KotlinScan(runtime, sourceSet).parse("a/P.kt", source)

    private fun classLiterals(types: List<TypeInfo>, method: String): List<String> {
        val found = mutableListOf<String>()
        types.first { it.simpleName() == "P" }.findUniqueMethod(method, 0).methodBody().visit { e ->
            if (e is ClassExpression) found.add(e.type().typeInfo().fullyQualifiedName())
            true
        }
        return found
    }

    @Test
    fun aReifiedClassLiteralIsTheErasure() {
        val types = parse("""
            package a
            class P {
                inline fun <reified T : Any> kotlinClass() = T::class
                inline fun <reified T : CharSequence> javaClass() = T::class.java
            }
        """.trimIndent())
        assertEquals(0, PlaceholderCensus.of(types).total, PlaceholderCensus.of(types).byKind.toString())
        assertEquals(listOf("java.lang.Object"), classLiterals(types, "kotlinClass"))
        assertEquals(listOf("java.lang.CharSequence"), classLiterals(types, "javaClass"))
    }
}
