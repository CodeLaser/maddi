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

import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * A placeholder that is a call's ARGUMENT survives [KotlinBodyConverter.normalizeIndices]. The renumbering translates
 * the body, and MethodCall's translation removed every argument that was an empty expression -- which a placeholder
 * is. Unread code vanished from the CST and from the census with it: `ServiceLoader.load(T::class.java)` (a reified
 * class literal) became `load(getJava())` whenever the body needed renumbering, which every inlined scope function
 * (#88) does, and detekt's `loadExtensions` lost its verdict to the call with one argument too few.
 */
class RenumberedPlaceholderTest : KotlinScanTestBase() {

    @Test
    fun aPlaceholderArgumentIsKeptAndCounted() {
        val types = KotlinScan(runtime, sourceSet).parse("r/R.kt", """
            package r
            inline fun <reified T : Any> plain(): List<T> = java.util.ServiceLoader.load(T::class.java).toList()
            inline fun <reified T : Any> viaAlso(): List<T> = java.util.ServiceLoader.load(T::class.java).toList().also { println(it.size) }
            """.trimIndent() + "\n")
        val body = { name: String -> types.flatMap { it.methods() }.first { it.name() == name }.methodBody().toString() }
        assertEquals("{return CollectionsKt.toList(ServiceLoader.load(JvmClassMappingKt.getJavaClass(k2-unsupported-expr:KtClassLiteralExpression)));}",
            body("plain"))
        assertEquals("{List<Object> it=CollectionsKt.toList(ServiceLoader.load(JvmClassMappingKt.getJavaClass(k2-unsupported-expr:KtClassLiteralExpression)));{System.out.println(it.size);}return it;}",
            body("viaAlso"))
        assertEquals(2, PlaceholderCensus.of(types).total)
    }
}
