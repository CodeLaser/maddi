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

import io.codelaser.maddi.cst.api.info.TypeInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * A file facade is the only primary type without a declaration of its own: every class, interface, object, enum
 * and annotation has a declaration source, the facade (a JVM artefact holding the top-level declarations) has none.
 * A consumer tells a top-level function from a member by this -- jfocus's Kotlin rename, whose overload set for a
 * top-level function is package-wide, as `import p.name` is -- so it is a contract, not an accident.
 */
class FacadeDeclarationTest : KotlinScanTestBase() {

    private val source = """
        package f

        class C
        interface I
        object O
        enum class E { A }
        annotation class N
        data class D(val x: Int)
        fun top(): Int = 1
        val prop: Int = 2
        """.trimIndent() + "\n"

    private fun hasDeclaration(type: TypeInfo) = type.source() != null && type.source().beginLine() > 0

    @Test
    fun theFacadeIsThePrimaryTypeWithoutADeclaration() {
        val primaries = KotlinScan(runtime, sourceSet).parse("f/F.kt", source).filter { it.isPrimaryType() }
        assertEquals(listOf("FKt"), primaries.filterNot { hasDeclaration(it) }.map { it.simpleName() })
        assertEquals(listOf("C", "D", "E", "I", "N", "O"), primaries.filter { hasDeclaration(it) }.map { it.simpleName() }.sorted())
    }
}
