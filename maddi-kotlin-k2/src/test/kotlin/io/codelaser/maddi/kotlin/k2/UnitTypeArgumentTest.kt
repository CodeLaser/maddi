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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * `Unit` is `void` only in RETURN position. As a type argument it is the class `kotlin.Unit`: kotlinc compiles
 * `(String) -> Unit` to `Function1<String, Unit>`, and that is the signature a Java caller sees. It was mapped to
 * `Function1<String, void>`, which is no Java type at all.
 */
class UnitTypeArgumentTest : KotlinScanTestBase() {

    @Test
    fun unitIsAClassAsATypeArgumentAndVoidAsAReturnType() {
        val u = KotlinScan(runtime, sourceSet).parse("u/U.kt", """
            package u
            class U {
                fun each(action: (String) -> Unit) { action("x") }
                fun later(): List<Unit> = listOf()
                fun done() {}
            }
            """.trimIndent()).first()
        assertEquals("kotlin.jvm.functions.Function1<String,kotlin.Unit>",
            u.findUniqueMethod("each", 1).parameters()[0].parameterizedType().fullyQualifiedName())
        assertEquals("java.util.List<kotlin.Unit>", u.findUniqueMethod("later", 0).returnType().fullyQualifiedName())
        assertEquals("void", u.findUniqueMethod("done", 0).returnType().fullyQualifiedName())
    }
}
