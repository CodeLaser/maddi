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
import io.codelaser.maddi.kotlin.api.PlaceholderCensus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * #53: every enum entry's field was initialized with a bare empty expression, so `A(1)` lost its argument and
 * `code` was never assigned for any entry. Each entry is now `new E(args)`, as the Java front end builds a Java
 * enum constant, with arguments ordered and defaulted as any call's.
 */
class EnumEntryTest : KotlinScanTestBase() {

    private val types: List<TypeInfo> by lazy {
        KotlinScan(runtime, sourceSet).parse("en/En.kt", """
            package en
            enum class E(val code: Int) { A(1), B(2) { override fun g() = 9 }; open fun g() = code }
            enum class F { X, Y }
            enum class G(val a: Int = 7, val b: String = "s") { P, Q(b = "t"), R(1) }
            """.trimIndent() + "\n")
    }

    private fun entries(name: String): String = types.first { it.simpleName() == name }.fields()
        .filter { it.isStatic && it.type().typeInfo()?.simpleName() == name }
        .joinToString(" ") { "${it.name()}=${it.initializer()}" }

    @Test
    fun noPlaceholder() {
        val census = PlaceholderCensus.of(types)
        assertEquals(0, census.total, census.dumpLines().joinToString("\n"))
    }

    @Test
    fun anEntryBodyIsASubtypeThatOverrides() {
        val b = types.first { it.simpleName() == "E" }.getFieldByName("B", true).initializer()
            as io.codelaser.maddi.cst.api.expression.ConstructorCall
        val g = b.anonymousClass().findUniqueMethod("g", 0)
        assertEquals("en.E", b.anonymousClass().parentClass().typeInfo().fullyQualifiedName())
        assertEquals("[en.E.g()]", g.overrides().map { it.fullyQualifiedName() }.toString())
        assertEquals("[return 9;]", g.methodBody().statements().toString())
    }

    @Test
    fun eachEntryConstructsItsValue() {
        // G: an omitted argument goes through the `$default` constructor with its bit mask, as kotlinc compiles it
        assertEquals("""
            E: A=new E(1) B=new E(2){public int g(){return 9;}}
            F: X=new F() Y=new F()
            G: P=new G(0,null,3,null) Q=new G(0,"t",1,null) R=new G(1,null,2,null)
            """.trimIndent(), listOf("E", "F", "G").joinToString("\n") { "$it: ${entries(it)}" })
    }
}
