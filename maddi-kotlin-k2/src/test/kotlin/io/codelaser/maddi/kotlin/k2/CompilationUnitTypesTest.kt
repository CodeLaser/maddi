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
 * A Kotlin compilation unit's types are its TOP-LEVEL types, the facade included, as
 * [io.codelaser.maddi.cst.api.element.CompilationUnit.types] documents and the Java front end sets them. Nested
 * classes -- plain, `inner`, a companion, an `object` -- are reached through their enclosing type, not listed beside
 * it: a consumer that recurses, as it must for Java, would otherwise visit each one twice, and one that reads the list
 * as "what this file declares" sees a file declaring many.
 */
class CompilationUnitTypesTest : KotlinScanTestBase() {

    @Test
    fun onlyTopLevelTypesAndTheFacade() {
        val types = KotlinScan(runtime, sourceSet).parse("r/R.kt", """
            package r

            class A {
                class Nested
                inner class Inner {
                    inner class Deeper
                }
                companion object
            }

            private class B

            object C {
                object D
            }

            fun f(): Int = 1
            """.trimIndent() + "\n")
        val cu = types.first().compilationUnit()
        assertEquals(listOf("A", "B", "C", "RKt"), cu.types().map { it.simpleName() }.sorted())
        // the nested ones still exist, reached through their enclosing type
        val a = cu.types().first { it.simpleName() == "A" }
        assertEquals(listOf("A", "Companion", "Deeper", "Inner", "Nested"),
            a.recursiveSubTypeStream().map { it.simpleName() }.toList().sorted())
    }

    @Test
    fun noFacadeWithoutTopLevelFunctions() {
        val types = KotlinScan(runtime, sourceSet).parse("r/S.kt", """
            package r

            class S {
                class T
            }
            """.trimIndent() + "\n")
        assertEquals(listOf("S"), types.first().compilationUnit().types().map { it.simpleName() })
    }
}
