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
