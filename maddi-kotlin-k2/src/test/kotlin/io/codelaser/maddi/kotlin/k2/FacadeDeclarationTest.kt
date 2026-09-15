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
