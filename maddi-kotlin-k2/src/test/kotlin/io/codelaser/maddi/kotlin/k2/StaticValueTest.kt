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

import io.codelaser.maddi.cst.api.expression.VariableExpression
import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.cst.api.statement.ReturnStatement
import io.codelaser.maddi.cst.api.variable.FieldReference
import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * A value named through a TYPE: an enum constant or a nested object, qualified by more than one type
 * (`Notification.Level.Warning`, `RulesSpec.RunPolicy.NoRestrictions`) or imported and written bare (`IGNORE_CASE`,
 * `NONE`). Each is a static field on the class file: the constant itself, or the object's `INSTANCE`. The lookup
 * accepted a receiver that is a single name only; on detekt these were 21 placeholders.
 */
class StaticValueTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("sv/Sv.kt", """
            package sv
            import sv.Mode.SHOW
            import kotlin.LazyThreadSafetyMode.NONE
            class Note { enum class Level { Info, Warning } }
            interface Spec { sealed interface Policy { object Open : Policy; object Closed : Policy } }
            enum class Mode { SHOW, HIDE }
            class K {
                fun nestedEnum(): Note.Level = Note.Level.Warning
                fun nestedObject(): Spec.Policy = Spec.Policy.Open
                fun importedEnum(): Mode = SHOW
                fun importedLibraryEnum(): LazyThreadSafetyMode = NONE
                fun qualifiedLibraryEnum(): RegexOption = RegexOption.IGNORE_CASE
                fun fullyQualified(): Note.Level = sv.Note.Level.Info
            }
            """.trimIndent() + "\n")
    }

    private fun type(name: String) = types.flatMap { it.recursiveSubTypeStream().toList() }.first { it.simpleName() == name }

    private fun returned(name: String): FieldReference {
        val body = type("K").findUniqueMethod(name, 0).methodBody()
        val value = (body.statements().single() as ReturnStatement).expression()
        return (value as VariableExpression).variable() as FieldReference
    }

    @Test
    fun noPlaceholder() {
        val census = PlaceholderCensus.of(types)
        assertEquals(0, census.total, census.dumpLines().joinToString("\n"))
    }

    @Test
    fun anEnumConstantOfANestedEnum() {
        val ref = returned("nestedEnum")
        assertEquals("Warning", ref.fieldInfo().name())
        assertEquals("Level", ref.fieldInfo().owner().simpleName())
        assertTrue(ref.fieldInfo().isStatic)
    }

    @Test
    fun anObjectNestedTwoDeep() {
        val ref = returned("nestedObject")
        assertEquals("INSTANCE", ref.fieldInfo().name())
        assertEquals("Open", ref.fieldInfo().owner().simpleName())
    }

    @Test
    fun anImportedEnumConstant() {
        assertEquals("SHOW", returned("importedEnum").fieldInfo().name())
        val library = returned("importedLibraryEnum")
        assertEquals("NONE", library.fieldInfo().name())
        assertEquals("LazyThreadSafetyMode", library.fieldInfo().owner().simpleName())
    }

    @Test
    fun aQualifiedLibraryEnumConstant() {
        assertEquals("IGNORE_CASE", returned("qualifiedLibraryEnum").fieldInfo().name())
    }

    @Test
    fun aFullyQualifiedName() {
        assertEquals("Info", returned("fullyQualified").fieldInfo().name())
    }
}
