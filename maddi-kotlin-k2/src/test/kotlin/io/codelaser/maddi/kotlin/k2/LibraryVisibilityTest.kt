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
 * A library class this front end loads keeps the visibility its members were declared with. K2 calls Java's
 * `protected` PACKAGE_PROTECTED, not PROTECTED, and every protected library method used to come out package-private;
 * library fields came out public whatever they were.
 */
class LibraryVisibilityTest : KotlinScanTestBase() {

    @Test
    fun aJavaProtectedMemberStaysProtected() {
        val types = KotlinScan(runtime, sourceSet).parse("l/L.kt", """
            package l

            class L {
                fun list(): java.util.AbstractList<String>? = null
            }
            """.trimIndent() + "\n")
        val abstractList = types.first().findUniqueMethod("list", 0).returnType().typeInfo()
        assertEquals("java.util.AbstractList", abstractList.fullyQualifiedName())
        val removeRange = abstractList.methods().first { it.name() == "removeRange" }
        assertEquals(runtime.accessProtected(), removeRange.access(), "$removeRange")
        // ⚠ a Java class's INSTANCE fields (`modCount`) are not loaded by this front end at all, only its static
        // fields and Kotlin properties, so the field half of the fix is not witnessed here
        assertEquals(runtime.accessPublic(), abstractList.methods().first { it.name() == "iterator" }.access())
    }
}
