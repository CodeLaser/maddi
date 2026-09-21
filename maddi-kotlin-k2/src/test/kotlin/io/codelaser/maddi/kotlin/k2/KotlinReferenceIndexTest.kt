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

import io.codelaser.maddi.kotlin.api.KotlinReferenceIndex.DeclarationKey
import io.codelaser.maddi.kotlin.api.KotlinReferenceIndex.Occurrence
import io.codelaser.maddi.cst.api.info.TypeInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KotlinReferenceIndexTest : KotlinScanTestBase() {

    private val base = """
        package a

        open class Base {
            open fun greet(): String = "b"
        }
        class Sub : Base() {
            override fun greet(): String = "s" + super.greet()
        }
        class Task : Runnable {
            override fun run() {}
        }
        fun String.path(): String = this
        fun Int.path(): String = toString()
        fun take(f: (item: String) -> Unit) = f("x")
        interface HasName { val name: String }
        class Named(override val name: String) : HasName
        """.trimIndent() + "\n"

    private val user = """
        package b

        import a.Base
        import a.Sub
        import a.path

        fun use(b: Base, s: Sub): String = b.greet() + s.greet() + "x".path() + 1.path()
        """.trimIndent() + "\n"

    private lateinit var types: List<TypeInfo>

    private fun index(): K2ReferenceIndex {
        val index = K2ReferenceIndex()
        types = KotlinScan(runtime, sourceSet).parse(mapOf("a/Base.kt" to base, "b/User.kt" to user), emptyMap(),
            listOf(index))
        assertEquals(0, index.unresolvedReferences)
        return index
    }

    private fun method(type: String, name: String) =
        types.first { it.simpleName() == type }.methods().first { it.name() == name }

    private fun where(o: Occurrence) = "${o.uri.substringAfterLast('/')}:${o.beginLine}:${o.beginPos}"

    @Test
    fun aCstMethodFindsItsDeclarationThroughItsOwnNamePosition() {
        val index = index()
        val key = requireNotNull(index.keyOf(method("Base", "greet"))) { "no name position recorded for Base.greet" }
        val declaration = requireNotNull(index.declaration(key)) { "Base.greet not indexed at $key" }
        assertEquals("greet", declaration.name)
        assertEquals("Base.kt:4:14", where(declaration))
        assertEquals(listOf(4, 18), listOf(declaration.endLine, declaration.endPos)) // `greet` is columns 14..18
    }

    @Test
    fun anOverrideFamilyIsRenamedTogether() {
        val index = index()
        val key = index.keyOf(method("Base", "greet"))!!
        assertEquals(setOf(key, index.keyOf(method("Sub", "greet"))!!), index.family(key))
        assertFalse(index.overridesOutsideProject(key))
        // two declarations, `super.greet()`, and the two calls in User.kt
        assertEquals(listOf("Base.kt:4:14", "Base.kt:7:18", "Base.kt:7:48", "User.kt:7:38", "User.kt:7:50"),
            index.occurrencesOfFamily(key).map { where(it) }.sorted())
    }

    @Test
    fun overridingALibraryDeclarationCannotBeRenamed() {
        val index = index()
        assertTrue(index.overridesOutsideProject(index.keyOf(method("Task", "run"))!!))
    }

    @Test
    fun aConstructorPropertyJoinsTheFamilyItOverrides() {
        // building the index at all is half the test: `item` (line 14) is a named parameter of a function type, for
        // which K2 refuses to create a symbol
        val index = index()
        val uri = index.keyOf(method("Base", "greet"))!!.uri
        val inInterface = DeclarationKey(uri, 15, 25)
        val inConstructor = DeclarationKey(uri, 16, 26)
        assertEquals("name", index.declaration(inInterface)?.name)
        assertEquals("name", index.declaration(inConstructor)?.name)
        assertEquals(setOf(inInterface, inConstructor), index.family(inConstructor))
    }

    @Test
    fun overloadsAreSeparateDeclarations() {
        val index = index()
        val facade = types.first { it.simpleName() == "BaseKt" }
        val (stringPath, intPath) = facade.methods().filter { it.name() == "path" }
            .partition { it.parameters().first().parameterizedType().typeInfo()?.fullyQualifiedName() == "java.lang.String" }
            .let { it.first.single() to it.second.single() }
        val stringKey = index.keyOf(stringPath)!!
        val intKey = index.keyOf(intPath)!!
        fun calls(key: DeclarationKey) = index.references(key).filter { it.beginLine == 7 }.map { where(it) }
        assertEquals(listOf("User.kt:7:64"), calls(stringKey))
        assertEquals(listOf("User.kt:7:75"), calls(intKey))
        // `import a.path` names both overloads: it is recorded for each, and says it is shared with the other
        val import = index.references(stringKey).single { it.beginLine == 5 }
        assertEquals(setOf(intKey), import.sharedWith)
        assertEquals(setOf(stringKey), index.references(intKey).single { it.beginLine == 5 }.sharedWith)
    }
}
