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
