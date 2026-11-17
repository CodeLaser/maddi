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

import io.codelaser.maddi.cst.api.expression.MethodCall
import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * <b>`Number.toLong()` is `java.lang.Number.longValue()`</b>, as kotlinc compiles it, on a `Number` or a smart-cast
 * to one. It was a placeholder with its receiver (Exposed's column types: `is Number -> value.toLong()`).
 */
class NumberConversionTest : KotlinScanTestBase() {

    private fun parse(source: String): List<TypeInfo> = KotlinScan(runtime, sourceSet).parse("a/P.kt", source)

    private fun callees(types: List<TypeInfo>, method: String): List<String> {
        val found = mutableListOf<String>()
        types.first { it.simpleName() == "P" }.findUniqueMethod(method, 1).methodBody().visit { e ->
            if (e is MethodCall) found.add(e.methodInfo().fullyQualifiedName())
            true
        }
        return found
    }

    @Test
    fun numberConversionsAreTheJvmValueMethods() {
        val types = parse("""
            package a
            class P {
                fun direct(n: Number): Long = n.toLong()
                fun smartCast(v: Any): Int = if (v is Number) v.toInt() else 0
                fun safeCast(v: Any?): Short? = (v as? Number)?.toShort()
            }
        """.trimIndent())
        assertEquals(0, PlaceholderCensus.of(types).total, PlaceholderCensus.of(types).byKind.toString())
        assertEquals(listOf("java.lang.Number.longValue()"), callees(types, "direct"))
        assertEquals(listOf("java.lang.Number.intValue()"), callees(types, "smartCast"))
        assertEquals(listOf("java.lang.Number.shortValue()"), callees(types, "safeCast"))
    }
}
